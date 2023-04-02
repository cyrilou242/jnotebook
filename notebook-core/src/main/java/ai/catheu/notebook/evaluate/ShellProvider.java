package ai.catheu.notebook.evaluate;

import jdk.jshell.JShell;

import java.util.ArrayDeque;
import java.util.Deque;

public class ShellProvider {

  private final Deque<JShell> preparedShells;

  public ShellProvider() {
    this.preparedShells = new ArrayDeque<>(2);
    // pre-init two shells to make the first rendering feel faster
    this.preparedShells.add(internalNewShell());
    this.preparedShells.add(internalNewShell());
  }

  public JShell getShell() {
    if (!preparedShells.isEmpty()) {
      return preparedShells.pop();
    }
    return internalNewShell();
  }

  private static JShell internalNewShell() {
    final JShell jshell = JShell.builder().build();
    return jshell;
  }
}
