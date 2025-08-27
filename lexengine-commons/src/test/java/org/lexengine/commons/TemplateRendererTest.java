/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.lexengine.commons.error.GeneratorException;

public class TemplateRendererTest {

  @Test
  public void testRender() {
    String template = "Hello, ${name}!";
    Map<String, String> model = Map.of("name", "John");
    String expected = "Hello, John!";
    assertEquals(expected, TemplateRenderer.render(template, model));
  }

  @Test
  public void testRenderMultiLineTemplate() {
    String template =
        """
      Hello, ${name}!
      How are you?, ${name}!
      """;
    Map<String, String> model = Map.of("name", "John");
    String expected =
        """
      Hello, John!
      How are you?, John!
      """;
    assertEquals(expected, TemplateRenderer.render(template, model));
  }

  @Test
  public void testRenderMissingAttribute() {
    String template = "Hello, ${name}!";
    Map<String, String> model = Map.of();
    assertThrows(GeneratorException.class, () -> TemplateRenderer.render(template, model));
  }

  @Test
  public void testRenderToFile() throws IOException {
    String template = "Hello, ${name}!";
    Map<String, String> model = Map.of("name", "John");
    Path tempFile = Files.createTempFile("test", ".txt");
    TemplateRenderer renderer = new TemplateRenderer(template, model);
    renderer.renderToFile(tempFile);
    String rendered = Files.readString(tempFile);
    assertEquals("Hello, John!", rendered);
  }
}
