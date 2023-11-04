/*
 * Copyright 2023 Cyril de Catheu
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package tech.catheu.jnotebook;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

// totally inefficient way to transform jfr logs into d3 format
// some pieces extracted from https://github.com/billybong/JavaFlames and https://github.com/spiermar/node-stack-convert
class FlameGraph {

  private FlameGraph() {
  }

  public static Map<String, Object> flameGraphD3(final Stream<String> flameGraphLog) {
    Node root = new Node("ALL");
    flameGraphLog.forEach(val -> {
      final String[] countSplit = val.split(" ");
      final var stackSplit = countSplit[0].split(";");
      root.add(new ArrayList<>(List.of(stackSplit)),
               Integer.parseInt(countSplit[1].strip()));
    });

    return root.serialize();
  }

  public static Stream<String> flameGraphLog(final Path jfrRecording) {
    try (final RecordingFile recordingFile = new RecordingFile(jfrRecording)) {
      final Map<String, Integer> counts = new HashMap<>();
      while (recordingFile.hasMoreEvents()) {
        final RecordedEvent event = recordingFile.readEvent();
        if ("jdk.ExecutionSample".equalsIgnoreCase(event.getEventType().getName())) {
          final String collapsedFrames =
                  collapseFrames(event.getStackTrace().getFrames());
          counts.merge(collapsedFrames, 1, Integer::sum);
        }
      }
      return counts.entrySet()
                   .stream()
                   .map(e -> "%s %d\n".formatted(e.getKey(), e.getValue()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String collapseFrames(List<RecordedFrame> frames) {
    var methodNames = new ArrayDeque<String>(frames.size());
    for (var frame : frames) {
      final RecordedMethod method = frame.getMethod();
      methodNames.addFirst("%s::%s".formatted(method.getType().getName(),
                                              method.getName()));
    }
    return String.join(";", methodNames);
  }

  private static class Node {
    String name;
    int value;
    Map<String, Node> children;

    public Node(String name) {
      this.name = name;
      this.value = 0;
      this.children = new HashMap<>();
    }

    public void add(List<String> frames, int value) {
      this.value += value;
      if (frames != null && !frames.isEmpty()) {
        String head = frames.get(0);
        Node child = this.children.get(head);
        if (child == null) {
          child = new Node(head);
          this.children.put(head, child);
        }
        frames.remove(0);
        child.add(frames, value);
      }
    }

    public Map<String, Object> serialize() {
      final Map<String, Object> res = new HashMap<>();
      res.put("name", this.name);
      res.put("value", this.value);
      List<Map<String, Object>> children = new ArrayList<>();
      for (Node child : this.children.values()) {
        children.add(child.serialize());
      }
      if (!children.isEmpty()) {
        res.put("children", children);
      }
      return res;
    }
  }

}
