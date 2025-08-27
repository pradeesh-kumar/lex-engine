/*
* Copyright (c) 2025 lex-engine
* Author: Pradeesh Kumar
*/
package org.lexengine.commons;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lexengine.commons.error.ErrorType;
import org.lexengine.commons.error.GeneratorException;
import org.lexengine.commons.logging.Out;

/**
 * The TemplateRenderer class is responsible for rendering templates by replacing placeholders with
 * actual values. It provides methods to render templates to a string or to a file.
 */
public class TemplateRenderer {

  /**
   * Pattern to match placeholders in the template. Placeholders are in the format ${placeholder}.
   */
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

  /** The template string to be rendered. */
  private final String template;

  /** The model containing the values to replace the placeholders in the template. */
  private final Map<String, String> model;

  /**
   * Constructs a TemplateRenderer instance by reading a template from a file.
   *
   * @param templateFile the file containing the template
   * @param model the model containing the values to replace the placeholders
   * @throws GeneratorException if an error occurs while reading the template file
   */
  public TemplateRenderer(Path templateFile, Map<String, String> model) {
    try {
      this.template = Files.readString(templateFile);
      this.model = model;
    } catch (IOException e) {
      Out.error("Error reading template file: " + templateFile, e);
      throw GeneratorException.error(ErrorType.ERR_LEX_TEMPLATE_FILE_READ);
    }
  }

  /**
   * Constructs a TemplateRenderer instance with a given template string.
   *
   * @param template the template string
   * @param model the model containing the values to replace the placeholders
   */
  public TemplateRenderer(String template, Map<String, String> model) {
    this.template = template;
    this.model = model;
  }

  /**
   * Renders the template to a file.
   *
   * @param outFile the file to write the rendered template to
   * @throws IOException if an error occurs while writing to the file
   */
  public void renderToFile(Path outFile) throws IOException {
    Out.info("Generating the file at %s", outFile);
    try (FileWriter outWriter = new FileWriter(outFile.toFile())) {
      outWriter.write(render());
      Out.info("Rendering template to the file %s", outFile);
    }
  }

  /**
   * Renders the template to a string.
   *
   * @return the rendered template
   */
  public String render() {
    return render(this.template, this.model);
  }

  /**
   * Renders a template to a string by replacing placeholders with actual values.
   *
   * @param template the template string
   * @param model the model containing the values to replace the placeholders
   * @return the rendered template
   * @throws GeneratorException if a placeholder is not found in the model
   */
  public static String render(String template, Map<String, String> model) {
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      String placeholder = matcher.group(1);
      String attrVal = model.get(placeholder);
      if (attrVal == null) {
        Out.error("Failed to render template! Attribute %s not found!", placeholder);
        throw GeneratorException.error(ErrorType.ERR_CLASS_GENERATE_ATTR_MISSING);
      }
      matcher.appendReplacement(result, attrVal);
    }
    matcher.appendTail(result);
    return result.toString();
  }
}
