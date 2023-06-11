package tech.catheu.jnotebook.render;

public record Rendering(String html) {

  public static Rendering of(final String html) {
    return new Rendering(html);
  }
}
