package ai.catheu.notebook.jshell;

import java.util.ArrayDeque;
import java.util.Deque;

public class ShellProvider {

  private final Deque<PowerJShell> preparedShells;

  public ShellProvider() {
    this.preparedShells = new ArrayDeque<>(2);
    // pre-init two shells to make the first rendering feel faster
    this.preparedShells.add(internalNewShell());
    this.preparedShells.add(internalNewShell());
  }

  public PowerJShell getShell() {
    if (!preparedShells.isEmpty()) {
      return preparedShells.pop();
    }
    return internalNewShell();
  }

  private static PowerJShell internalNewShell() {
    return new PowerJShell();
  }
}
