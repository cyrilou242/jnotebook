package ai.catheu.notebook.evaluate;

import ai.catheu.notebook.jshell.EvalResult;
import ai.catheu.notebook.jshell.PowerJShell;
import ai.catheu.notebook.jshell.ShellProvider;
import ai.catheu.notebook.parse.StaticParsing;
import ai.catheu.notebook.parse.StaticSnippet;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import jdk.jshell.ErroneousSnippet;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
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

import java.nio.file.Path;
import java.util.*;

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
  final Map<Path, Map<String, EvalResult>> fileToResultCache = new HashMap<>();
  private final ShellProvider shellProvider;

  public GreedyInterpreter(final ShellProvider shellProvider) {
    this.shellProvider = shellProvider;
  }

  @Override
  public Interpreted interpret(final StaticParsing staticParsing) {
    final PowerJShell shell =
            fileToShell.computeIfAbsent(staticParsing.path(), this::newShell);
    final Map<String, EvalResult> resultCache = fileToResultCache.computeIfAbsent(
            staticParsing.path(),
            ignored -> new HashMap<>());

    final SourceClass source =
            buildSourceClass(staticParsing, shell.sourceCodeAnalysis());
    final CtClass<?> ast = parseClassCode(source.classCode());
    final DependencyGraph depGraph = buildDependenciesGraph(ast);

    // resolve diff and dependencies
    final BiMap<String, Integer> fingerprintToSnippetIdx = HashBiMap.create();
    final Set<Integer> snippetsIdxToRun = new HashSet<>();
    for (final String node : depGraph.dependencies.nodes()) {
      final CtTypeMember l = depGraph.graphKeyToMember.get(node);
      final Integer snippetId = Integer.valueOf(l.getComments().get(0).getContent());
      l.accept(FINGERPRINT_PREPARATOR);
      final String fingerprint = l.toString();
      fingerprintToSnippetIdx.put(fingerprint, snippetId);
      if (!resultCache.containsKey(fingerprint)) {
        snippetsIdxToRun.add(snippetId);
        addAllDependencies(snippetsIdxToRun,
                           depGraph,
                           depGraph.dependencies.predecessors(node));
      }
    }

    // clean outdated jshell snippets
    for (final String f : resultCache.keySet()) {
      if (!fingerprintToSnippetIdx.containsKey(f)) {
        // snippets can be dropped
        for (SnippetEvent s : resultCache.get(f).events()) {
          LOG.debug("Dropping outdated snippet: {}", s.snippet().source());
          shell.drop(s.snippet());
        }
        fingerprintToSnippetIdx.remove(f);
      }
    }

    // build result snippets
    final List<InterpretedSnippet> interpretedSnippets1 = new ArrayList<>();
    for (int i = 0; i < staticParsing.snippets().size(); i++) {
      final StaticSnippet s = staticParsing.snippets().get(i);
      final String fingerprint = fingerprintToSnippetIdx.inverse().get(i);
      if (s.type().equals(StaticSnippet.Type.JAVA)) {
        if (fingerprint == null) {
          // imports are not fingerprinted and always re-evaluated for the moment
          LOG.debug("Evaluating: " + s.completionInfo().source().strip());
          final EvalResult res = shell.eval(s.completionInfo().source());
          interpretedSnippets1.add(new InterpretedSnippet(s, res));
        } else if (snippetsIdxToRun.contains(i)) {
          LOG.debug("Evaluating: " + s.completionInfo().source().strip());
          final EvalResult res = shell.eval(s.completionInfo().source());
          resultCache.put(fingerprint, res);
          interpretedSnippets1.add(new InterpretedSnippet(s, res));
        } else {
          // use cached result
          LOG.debug("Using cache for: " + s.completionInfo().source().strip());
          final EvalResult res = resultCache.get(fingerprint);
          interpretedSnippets1.add(new InterpretedSnippet(s, res));
        }
      } else {
        interpretedSnippets1.add(new InterpretedSnippet(s, null));
      }
    }
    final List<InterpretedSnippet> interpretedSnippets = interpretedSnippets1;

    return new Interpreted(staticParsing.path(),
                           staticParsing.lines(),
                           interpretedSnippets);
  }

  private void addAllDependencies(Set<Integer> snippetsIdxToRerun, DependencyGraph depGraph, Set<String> predecessors) {
    for (final String p : predecessors) {
      final CtTypeMember ctTypeMember = depGraph.graphKeyToMember().get(p);
      final Integer snippetId =
              Integer.valueOf(ctTypeMember.getComments().get(0).getContent());
      snippetsIdxToRerun.add(snippetId);
      addAllDependencies(snippetsIdxToRerun,
                         depGraph,
                         depGraph.dependencies.predecessors(p));
    }
  }

  private SourceClass buildSourceClass(final StaticParsing staticParsing, final SourceCodeAnalysis ana) {
    StringBuilder spoonCompatibleSource = new StringBuilder(CLASS_PREFIX);
    Map<Integer, Snippet> staticSnippetIdxToSnippet = new HashMap<>();
    for (int i = 0; i < staticParsing.snippets().size(); i++) {
      final StaticSnippet e = staticParsing.snippets().get(i);
      if (e.type().equals(StaticSnippet.Type.JAVA)) {
        final String snippetString = e.completionInfo().source();
        final Snippet preAnalysis = ana.sourceToSnippets(snippetString).get(0);
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
              LOG.error("unmanaged case when parsing: " + preAnalysis.kind());
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
    MutableGraph<String> dependencies =
            GraphBuilder.directed().allowsSelfLoops(false).build();
    final Map<String, CtTypeMember> simpleNameToMember = new HashMap<>();
    for (int i = 1; i < typeMembers.size(); i++) {
      final CtTypeMember member = typeMembers.get(i);
      dependencies.addNode(member.getSimpleName());
      simpleNameToMember.put(member.getSimpleName(), member);
      member.accept(new CtScanner() {
        @Override
        public <T> void visitCtFieldReference(CtFieldReference<T> reference) {
          if (reference.getDeclaringType() != null) {
            final var declaringType = reference.getDeclaringType();
            if (declaringType.getSimpleName().equals(SYNTHETIC_CLASS_NAME)) {
              dependencies.putEdge(member.getSimpleName(), reference.getSimpleName());
            }
          }
        }

        @Override
        public <T> void visitCtExecutableReference(CtExecutableReference<T> reference) {
          if (reference.getDeclaringType() != null) {
            final var declaringType = reference.getDeclaringType();
            if (declaringType.getSimpleName().equals(SYNTHETIC_CLASS_NAME)) {
              dependencies.putEdge(member.getSimpleName(), reference.getSimpleName());
            }
          }
        }

        @Override
        public <T> void visitCtArrayTypeReference(CtArrayTypeReference<T> reference) {
          if (reference.getDeclaringType() != null) {
            final var declaringType = reference.getDeclaringType();
            if (declaringType.getSimpleName().equals(SYNTHETIC_CLASS_NAME)) {
              dependencies.putEdge(member.getSimpleName(), reference.getSimpleName());
            }
          }
        }

        @Override
        public <T> void visitCtTypeReference(CtTypeReference<T> reference) {
          if (reference.getDeclaringType() != null) {
            final var declaringType = reference.getDeclaringType();
            if (declaringType.getSimpleName().equals(SYNTHETIC_CLASS_NAME)) {
              dependencies.putEdge(member.getSimpleName(), reference.getSimpleName());
            }
          }
        }

        // todo add all other kinds of references...
      });
    }

    return new DependencyGraph(dependencies, simpleNameToMember);
  }

  private PowerJShell newShell(final Path path) {
    LOG.info("Starting new shell for file: {}", path.getFileName());
    return shellProvider.getShell();
  }

  private static CtClass<?> parseClassCode(String classCode) {
    Launcher launcher = new Launcher();
    launcher.getEnvironment().setComplianceLevel(11);
    launcher.getEnvironment().setNoClasspath(true);
    launcher.getEnvironment().setCommentEnabled(true);
    launcher.addInputResource(new VirtualFile(classCode));
    return (CtClass<?>) launcher.buildModel().getAllTypes().iterator().next();
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

  private static StringBuilder methodVariableWrap(final int i, final String snippetString) {
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
                                 Map<String, CtTypeMember> graphKeyToMember) {
  }

  private record SourceClass(String classCode,
                             Map<Integer, Snippet> staticSnippetIdxToSnippet) {
  }
}
