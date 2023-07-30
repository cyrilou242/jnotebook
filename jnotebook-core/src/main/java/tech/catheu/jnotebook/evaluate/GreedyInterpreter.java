/*
 * Copyright Cyril de Catheu, 2023
 *
 * Licensed under the JNOTEBOOK LICENSE 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at https://raw.githubusercontent.com/cyrilou242/jnotebook/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT * WARRANTIES OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and limitations under
 * the License.
 */
package tech.catheu.jnotebook.evaluate;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import jdk.jshell.ErroneousSnippet;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;
import spoon.support.compiler.VirtualFile;
import tech.catheu.jnotebook.ExecutionStatus;
import tech.catheu.jnotebook.jshell.EvalResult;
import tech.catheu.jnotebook.jshell.PowerJShell;
import tech.catheu.jnotebook.jshell.ShellProvider;
import tech.catheu.jnotebook.parse.StaticParsing;
import tech.catheu.jnotebook.parse.StaticSnippet;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;

import static jdk.jshell.Snippet.Kind.*;

@SuppressWarnings("UnstableApiUsage")
public class GreedyInterpreter implements Interpreter {

  private static final Logger LOG = LoggerFactory.getLogger(GreedyInterpreter.class);

  // the synthetic names are complex to avoid collision with user defined types
  private static final String SYNTHETIC_CLASS_NAME = "B9fe3d5Synth";
  private static final String SYNTHETIC_METHOD_NAME = "ce75c1cSynth";
  private static final String CLASS_PREFIX = "class " + SYNTHETIC_CLASS_NAME + " { \n";
  private static final String BLOCK_SUFFIX = "}";
  public static final CtScanner FINGERPRINT_PREPARATOR = new CtScanner() {
    @Override
    protected void enter(CtElement e) {
      e.setComments(null);
      super.enter(e);
    }

    @Override
    public <T> void visitCtMethod(CtMethod<T> m) {
      if (m.getSimpleName().startsWith(SYNTHETIC_METHOD_NAME)) {
        m.setSimpleName(SYNTHETIC_METHOD_NAME);
      }
      super.visitCtMethod(m);
    }
  };

  final Map<Path, PowerJShell> fileToShell = new HashMap<>();
  final Map<Path, State> fileToState = new HashMap<>();
  private final ShellProvider shellProvider;

  public GreedyInterpreter(final ShellProvider shellProvider) {
    this.shellProvider = shellProvider;
  }

  @Override
  public Interpreted interpret(final StaticParsing staticParsing) {
    if (!staticParsing.executionStatus().isOk()) {
      return new Interpreted(staticParsing.path(),
                             Collections.emptyList(),
                             Collections.emptyList(),
                             staticParsing.executionStatus());
    }

    try {
      return internalInterpret(staticParsing);
    } catch (Exception e) {
      final String errorMessage = String.format(
              "Error during interpretation of file %s:\n%s",
              staticParsing.path(),
              e.getMessage());
      final ExecutionStatus errorStatus = ExecutionStatus.failure(errorMessage, e);
      LOG.error(errorMessage);
      return new Interpreted(staticParsing.path(),
                             Collections.emptyList(),
                             Collections.emptyList(),
                             errorStatus);
    }
  }

  @NotNull
  private Interpreted internalInterpret(StaticParsing staticParsing) {
    final PowerJShell shell =
            fileToShell.computeIfAbsent(staticParsing.path(), this::newShell);
    final State state = fileToState.computeIfAbsent(
            staticParsing.path(),
            ignored -> new State(new HashMap<>(), new HashMap<>()));

    final SourceClass source =
            buildSourceClass(staticParsing, shell.sourceCodeAnalysis());
    final CtClass<?> ast = parseClassCode(source.classCode());
    final DependencyGraph depGraph = buildDependenciesGraph(ast);

    // resolve diff and dependencies
    final BiMap<String, Integer> fingerprintToSnippetIdx = HashBiMap.create();
    final Set<Integer> snippetsIdxToRun = new HashSet<>();
    final HashMap<String, String> newSimpleNameToFingerprint = new HashMap<>();
    for (final String simpleName : depGraph.dependencies.nodes()) {
      final CtTypeMember ctMember = depGraph.simpleNameToMember.get(simpleName);
      final Integer snippetId = Integer.valueOf(ctMember.getComments().get(0).getContent());
      ctMember.accept(FINGERPRINT_PREPARATOR);
      final Set<String> predecessors = depGraph.dependencies.predecessors(simpleName);
      final String fingerprint = ctMember.toString() + predecessors.hashCode();
      fingerprintToSnippetIdx.put(fingerprint, snippetId);
      newSimpleNameToFingerprint.put(simpleName, fingerprint);
      if (!state.fingerprintToEvalResult.containsKey(fingerprint)) {
        snippetsIdxToRun.add(snippetId);
        addAllDependencies(snippetsIdxToRun,
                           depGraph,
                           depGraph.dependencies.successors(simpleName));
      } else if (depGraph.forwardReferences.contains(simpleName)) {
        snippetsIdxToRun.add(snippetId);
      } else if (fingerprintToSnippetIdx.containsKey(fingerprint)) {
        // with the current architecture, it's not possible to know what to do when a fingerprint is duplicated
        // I don't think it is possible without a perfect diff computer - because of java mutability
        // a duplicated fingerprint should always be re-run
        snippetsIdxToRun.add(snippetId);
        addAllDependencies(snippetsIdxToRun,
                           depGraph,
                           depGraph.dependencies.successors(simpleName));
        snippetsIdxToRun.add(fingerprintToSnippetIdx.get(fingerprint));
      }
    }

    // clean outdated jshell snippets
    final List<String> toRemove = new ArrayList<>(state.fingerprintToEvalResult.keySet()
                                             .stream()
                                             .filter(fingerprint -> !fingerprintToSnippetIdx.containsKey(fingerprint))
                                             .toList());
    depGraph.forwardReferences.forEach(f -> toRemove.add(state.simpleNameToFingerprint.get(f)));
    for (final String fingerprint : toRemove) {
      final EvalResult evalResult = state.fingerprintToEvalResult.get(fingerprint);
      if (evalResult != null) {
        for (SnippetEvent s : evalResult.events()) {
          // fixme cyril ? this uses jshell dependency mechanism but does not delete according to computed dependencies
          LOG.debug("Dropping outdated snippet: {}", s.snippet().source().trim());
          shell.drop(s.snippet());
        }
        state.fingerprintToEvalResult.remove(fingerprint);
      }
    }
    state.simpleNameToFingerprint.clear(); // not the cleanest way to implement this
    state.simpleNameToFingerprint.putAll(newSimpleNameToFingerprint);

    // build result snippets
    final List<InterpretedSnippet> interpretedSnippets = new ArrayList<>();
    for (int i = 0; i < staticParsing.snippets().size(); i++) {
      final StaticSnippet s = staticParsing.snippets().get(i);
      final String fingerprint = fingerprintToSnippetIdx.inverse().get(i);
      if (s.type().equals(StaticSnippet.Type.JAVA)) {
        if (fingerprint == null) {
          // imports are not fingerprinted and always re-evaluated for the moment
          LOG.debug("Evaluating: " + s.completionInfo().source().strip());
          final EvalResult res = shell.eval(s.completionInfo().source());
          interpretedSnippets.add(new InterpretedSnippet(s, res));
        } else if (snippetsIdxToRun.contains(i)) {
          LOG.debug("Evaluating: " + s.completionInfo().source().strip());
          final EvalResult res = shell.eval(s.completionInfo().source());
          state.fingerprintToEvalResult.put(fingerprint, res);
          interpretedSnippets.add(new InterpretedSnippet(s, res));
        } else {
          // use cached result
          LOG.debug("Using cache for: " + s.completionInfo().source().strip());
          final EvalResult res = state.fingerprintToEvalResult.get(fingerprint);
          interpretedSnippets.add(new InterpretedSnippet(s, res));
        }
      } else {
        interpretedSnippets.add(new InterpretedSnippet(s, null));
      }
    }

    return new Interpreted(staticParsing.path(),
                           staticParsing.lines(),
                           interpretedSnippets,
                           ExecutionStatus.ok());
  }

  private void addAllDependencies(final Set<Integer> snippetsIdxToRerun,
                                  final DependencyGraph depGraph,
                                  final Set<String> successors) {
    for (final String s : successors) {
      final CtTypeMember ctTypeMember = depGraph.simpleNameToMember().get(s);
      final Integer snippetId =
              Integer.valueOf(ctTypeMember.getComments().get(0).getContent());
      snippetsIdxToRerun.add(snippetId);
      addAllDependencies(snippetsIdxToRerun,
                         depGraph,
                         depGraph.dependencies.successors(s));
    }
  }

  private SourceClass buildSourceClass(final StaticParsing staticParsing,
                                       final SourceCodeAnalysis ana) {
    StringBuilder spoonCompatibleSource = new StringBuilder(CLASS_PREFIX);
    Map<Integer, Snippet> staticSnippetIdxToSnippet = new HashMap<>();
    for (int i = 0; i < staticParsing.snippets().size(); i++) {
      final StaticSnippet e = staticParsing.snippets().get(i);
      if (e.type().equals(StaticSnippet.Type.JAVA)) {
        final String snippetString = e.completionInfo().source();
        final Snippet preAnalysis;
        if (snippetString != null) {
          preAnalysis = ana.sourceToSnippets(snippetString).get(0);
        } else {
          // too many cases that are hard to recover from - for the moment surface the error to the top
          throw new IllegalStateException(String.format(
                  "Error trying to interpret JAVA code in lines [%s, %s]:\n%s\n" + "Code completeness: %s",
                  e.start() + 1,
                  // index from 1 for humans
                  e.end(),
                  e.completionInfo().remaining(),
                  e.completionInfo().completeness()));
        }
        staticSnippetIdxToSnippet.put(i, preAnalysis);
        switch (preAnalysis.kind()) {
          case IMPORT:
            StringBuilder previousSource = spoonCompatibleSource;
            spoonCompatibleSource = new StringBuilder("//" + i + "\n");
            spoonCompatibleSource.append(snippetString);
            spoonCompatibleSource.append(previousSource);
            break;
          case METHOD:
          case TYPE_DECL:
            spoonCompatibleSource.append(idComment(i)).append(snippetString);
            break;
          case VAR:
            switch (preAnalysis.subKind()) {
              case VAR_DECLARATION_SUBKIND:
              case VAR_DECLARATION_WITH_INITIALIZER_SUBKIND:
                spoonCompatibleSource.append(idComment(i)).append(snippetString);
                break;
              case TEMP_VAR_EXPRESSION_SUBKIND:
                spoonCompatibleSource.append(methodVariableWrap(i, snippetString));
                break;
            }
            break;
          case EXPRESSION:
            spoonCompatibleSource.append(methodVariableWrap(i, snippetString));
            break;
          case STATEMENT:
            spoonCompatibleSource.append(methodWrap(i, snippetString));
            break;
          case ERRONEOUS:
            // FIXME CYRIL - ensure count of { = count of } for the wrapping to work - see BUG 4
            final Snippet.Kind probableKind =
                    ((ErroneousSnippet) preAnalysis).probableKind();
            if (probableKind.equals(EXPRESSION)) {
              spoonCompatibleSource.append(methodVariableWrap(i, snippetString));
            } else if (probableKind.equals(STATEMENT)) {
              spoonCompatibleSource.append(methodWrap(i, snippetString));
            } else if (probableKind.equals(VAR)) {
              // very fragile way of infering if something is a correct var instantiation or a jshell specific value only
              if (snippetString.contains("=")) {
                spoonCompatibleSource.append(methodWrap(i, snippetString));
              } else {
                spoonCompatibleSource.append(methodVariableWrap(i, snippetString));
              }
            } else {
              LOG.debug("unmanaged case when parsing: " + preAnalysis.kind());
            }
            break;
          default:
            throw new IllegalArgumentException(preAnalysis.kind().toString());
        }
        spoonCompatibleSource.append("\n");
      }
    }
    spoonCompatibleSource.append(BLOCK_SUFFIX);
    final String classCode = spoonCompatibleSource.toString();
    return new SourceClass(classCode, staticSnippetIdxToSnippet);
  }

  private static DependencyGraph buildDependenciesGraph(CtClass<?> res) {
    final List<CtTypeMember> typeMembers = res.getTypeMembers();
    final MutableGraph<String> dependencies =
            GraphBuilder.directed().allowsSelfLoops(false).build();
    final Map<String, CtTypeMember> simpleNameToMember = new HashMap<>();
    final Set<String> forwardReferences = new HashSet<>();
    // order means top to bottom order
    final BiConsumer<String, String> orderSafePutEdge = (member, reference) -> {
      if (dependencies.nodes().contains(reference)) {
        // no need to add self-references - (eg recursive functions)
        if (!reference.equals(member)) {
          dependencies.putEdge(reference, member);
        }
      } else {
        forwardReferences.add(reference);
      }
    };
    for (int i = 1; i < typeMembers.size(); i++) {
      final CtTypeMember member = typeMembers.get(i);
      final String memberSimpleName = member.getSimpleName();
      dependencies.addNode(memberSimpleName);
      simpleNameToMember.put(memberSimpleName, member);
      member.accept(new CtScanner() {
        @Override
        public <T> void visitCtFieldReference(CtFieldReference<T> reference) {
          if (reference.getDeclaringType() != null) {
            final CtTypeReference<?> declaringType = reference.getDeclaringType();
            if (declaringType.getSimpleName().equals(SYNTHETIC_CLASS_NAME)) {
              orderSafePutEdge.accept(memberSimpleName, reference.getSimpleName());
            }
          }
        }

        @Override
        public <T> void visitCtExecutableReference(CtExecutableReference<T> reference) {
          if (reference.getDeclaringType() != null) {
            final CtTypeReference<?> declaringType = reference.getDeclaringType();
            if (declaringType.getSimpleName().equals(SYNTHETIC_CLASS_NAME)) {
              orderSafePutEdge.accept(memberSimpleName, reference.getSimpleName());
            }
          }
        }

        @Override
        public <T> void visitCtArrayTypeReference(CtArrayTypeReference<T> reference) {
          if (reference.getDeclaringType() != null) {
            final CtTypeReference<?> declaringType = reference.getDeclaringType();
            if (declaringType.getSimpleName().equals(SYNTHETIC_CLASS_NAME)) {
              orderSafePutEdge.accept(memberSimpleName, reference.getSimpleName());
            }
          }
        }

        @Override
        public <T> void visitCtTypeReference(CtTypeReference<T> reference) {
          if (reference.getDeclaringType() != null) {
            final CtTypeReference<?> declaringType = reference.getDeclaringType();
            if (declaringType.getSimpleName().equals(SYNTHETIC_CLASS_NAME)) {
              orderSafePutEdge.accept(memberSimpleName, reference.getSimpleName());
            }
          }
        }

        // fixme cyril add all other kinds of references and tests
      });
    }

    return new DependencyGraph(dependencies, simpleNameToMember, forwardReferences);
  }

  private PowerJShell newShell(final Path path) {
    LOG.info("Starting new shell for file: {}", path.getFileName());
    return shellProvider.getShell();
  }

  private static CtClass<?> parseClassCode(String classCode) {
    Launcher launcher = new Launcher();
    launcher.getEnvironment().setComplianceLevel(getJavaVersion());
    launcher.getEnvironment().setNoClasspath(true);
    launcher.getEnvironment().setCommentEnabled(true);
    launcher.addInputResource(new VirtualFile(classCode));
    return (CtClass<?>) launcher.buildModel().getAllTypes().iterator().next();
  }

  private static int getJavaVersion() {
    if (SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_17)) {
      return 17;
    } else if (SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_16)) {
      LOG.warn("Running on an unsupported version of Java: " + JavaVersion.JAVA_16);
      return 16;
    }
    throw new RuntimeException(
            "Running on an unsupported version of Java. Jnotebook requires Java >=17.");
  }

  private static StringBuilder methodPrefix(final int i) {
    return new StringBuilder("public Object " + SYNTHETIC_METHOD_NAME + i + "(){");
  }

  private static StringBuilder idComment(final int i) {
    return new StringBuilder().append("//").append(i).append("\n");
  }

  private static StringBuilder methodWrap(final int i, final String snippetString) {
    return new StringBuilder().append(idComment(i))
                              .append(methodPrefix(i))
                              .append(snippetString)
                              .append(BLOCK_SUFFIX);
  }

  private static StringBuilder methodVariableWrap(final int i,
                                                  final String snippetString) {
    return new StringBuilder().append(idComment(i))
                              .append(methodPrefix(i))
                              .append("var $reserved$ = ")
                              .append(snippetString)
                              .append(BLOCK_SUFFIX);
  }

  @Override
  public void stop() {
    for (Path path : fileToShell.keySet()) {
      final var sh = fileToShell.get(path);
      if (sh != null) {
        sh.close();
      }
      fileToShell.remove(path);
    }
  }

  private record DependencyGraph(MutableGraph<String> dependencies,
                                 Map<String, CtTypeMember> simpleNameToMember,
                                 Set<String> forwardReferences) {
  }

  private record SourceClass(String classCode,
                             Map<Integer, Snippet> staticSnippetIdxToSnippet) {
  }

  private record State(Map<String, EvalResult> fingerprintToEvalResult, Map<String, String> simpleNameToFingerprint) {}
}
