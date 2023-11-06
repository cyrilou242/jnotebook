package tech.catheu.jnotebook.exp;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import org.apache.commons.lang3.StringEscapeUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class PlotlyGen {

  private static final String PLOTLY_SCHEMA_STRING;
  private static final JSONObject PLOTLY_SCHEMA_JSON;
  private static final Converter<String, String> UPPER_CAMEL_CONVERTER =
          CaseFormat.LOWER_UNDERSCORE.converterTo(CaseFormat.UPPER_CAMEL);
  private static final Converter<String, String> LOWER_CAMEL_CONVERTER =
          CaseFormat.LOWER_UNDERSCORE.converterTo(CaseFormat.LOWER_CAMEL);

  static {
    try {
      PLOTLY_SCHEMA_STRING = Files.readString(Paths.get(
              "/Users/cyril/IdeaProjects/notebook/plot-schema.json"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    PLOTLY_SCHEMA_JSON = new JSONObject(PLOTLY_SCHEMA_STRING);
  }

  public static void main(String[] args) throws IOException {
    for (String rootObject : PLOTLY_SCHEMA_JSON.keySet()) {
      String clazz = "";
      if (rootObject.equals("defs")) {
        continue;
      } else if (rootObject.equals("layout")) {
        final JSONObject layoutSchema = PLOTLY_SCHEMA_JSON.getJSONObject(rootObject);
        final JSONObject layoutAttributes =
                layoutSchema.getJSONObject("layoutAttributes");
        clazz = generateClass(rootObject, layoutAttributes, false);
      } else if (rootObject.equals("traces")) {
        // FIXME CYRIL skipping for the moment - need to check the custom keys
        continue;
      } else if (rootObject.equals("transforms")) {
        // FIXME CYRIL skipping for the moment - need to check the custom keys
        continue;
      } else {
        clazz = generateClass(rootObject,
                              PLOTLY_SCHEMA_JSON.getJSONObject(rootObject),
                              false);
      }
      Files.write(Paths.get(UPPER_CAMEL_CONVERTER.convert(rootObject) + ".java"),
                  clazz.getBytes(StandardCharsets.UTF_8));
    }
  }

  private static String generateClass(final String name, JSONObject schema,
                                      final boolean inner) {
    final StringBuilder clazz = new StringBuilder();
    if (!inner) {
      clazz.append("package tech.catheu.jnotebook;\n");
      clazz.append("import java.util.List;\n");
    }
    final String className = UPPER_CAMEL_CONVERTER.convert(name);
    clazz.append("public " + (inner ? "static" : "") + " class " + className + " {\n");

    if (schema.keySet().contains("items")) {
      try {
        checkArgument(schema.keySet().size() == 2,
                      "items only manged when keys are items and role. KeySet: %s",
                      schema.keySet());
      } catch (Exception e) {
        throw e;
      }
      final JSONObject items = schema.getJSONObject("items");
      checkArgument(items.keySet().size() == 1,
                    "items only managed when items have one element");
      final String itemType = items.keys().next();
      clazz.append(generateMember(itemType,
                                  items.getJSONObject(itemType),
                                  className,
                                  true));
    } else {
      for (final String e : schema.keySet()) {
        if (List.of("_deprecated", "role").contains(e)) {
          // TODO add support for deprecated fields - role is skipped on purpose
          continue;
        }
        final JSONObject jsonObject;
        try {
          jsonObject = schema.getJSONObject(e);
        } catch (Exception ex) {
          System.out.println(String.format("%s has role %s", className, e));
          continue;
        }
        clazz.append(generateMember(e, jsonObject, className, false));
      }
    }
    clazz.append("}\n");
    return clazz.toString();
  }

  private static String generateMember(final String name, final JSONObject memberSchema,
                                       final String parentName, boolean isRepeated) {
    final String lowerCamelName = LOWER_CAMEL_CONVERTER.convert(name);
    if (memberSchema.keySet().contains("role")) {
      final String role = memberSchema.getString("role");
      switch (role) {
        case "object":
          final String className = UPPER_CAMEL_CONVERTER.convert(name);
          return memberDeclaration(className,
                                   lowerCamelName,
                                   null,
                                   isRepeated) + memberSetter(className,
                                                              lowerCamelName,
                                                              parentName,
                                                              isRepeated) + generateClass(
                  name,
                  memberSchema,
                  true);
        default:
          throw new UnsupportedOperationException("Unsupported role: " + role);
      }
    }
    if (isRepeated) {
      throw new RuntimeException("Repeated only managed with objects for the moment");
    }

    final String valType = memberSchema.getString("valType");
    switch (valType) {
      case "integer":
        return generateIntegerMember(memberSchema, parentName, lowerCamelName);
      case "string":
        return generateStringMember(memberSchema, parentName, lowerCamelName);
      case "boolean":
        return generateBooleanMember(memberSchema, parentName, lowerCamelName);
      case "number":
        return generateNumberMember(memberSchema, parentName, lowerCamelName);
      case "any":
        return generateAnyMember(memberSchema, parentName, lowerCamelName);
      default:
        System.out.println("Unsupported valType: " + valType);
    }
    return "";
  }

  private static String generateAnyMember(JSONObject memberSchema, String parentName,
                                          String lowerCamelName) {
    String defaultValue = null;
    if (memberSchema.has("dflt") && !memberSchema.isNull("dflt")) {
      final Object o = memberSchema.get("dflt");
      if (o instanceof Boolean) {
        defaultValue = o.toString();
      } else if (o instanceof Integer) {
        defaultValue = o.toString();
      } else if (o instanceof String s) {
        defaultValue = s;
      } else if (o instanceof JSONArray arr) {
        defaultValue = initStatementOf(arr, o);
      } else if (o instanceof JSONObject obj) {
        defaultValue = initStatementOf(obj, o);
      } else {
        throw new UnsupportedOperationException(String.format(
                "Unsupported default for any type: %s, %s, ",
                o.getClass(),
                o.toString()));
      }
      //defaultValue = memberSchema.get("dflt");
    } return memberDeclaration("Object",
                               lowerCamelName,
                               defaultValue,
                               false) + memberSetter("Object",
                                                     lowerCamelName,
                                                     parentName,
                                                     false);
  }

  private static String initStatementOf(JSONObject obj, Object o) {
    if (obj.isEmpty()) {
      return "Map.of()";
    } else {
      throw new UnsupportedOperationException(String.format(
              "TODO implement. Unsupported type for map default in any case: %s, %s, ",
              o.getClass(),
              o.toString()));
    }
  }

  @NotNull
  private static String initStatementOf(JSONArray arr, Object o) {
    if (arr.isEmpty()) {
      return "List.of()";
    } else {
      throw new UnsupportedOperationException(String.format(
              "TODO implement. Unsupported type for list default in any case: %s, %s, ",
              o.getClass(),
              o.toString()));
    }
  }

  private static String generateNumberMember(JSONObject memberSchema, String parentName,
                                             String lowerCamelName) {
    // TODO implement full spec see valObjects
    String defaultValue = null;
    if (memberSchema.has("dflt") && !memberSchema.isNull("dflt")) {
      final Object defaultValueObject = memberSchema.get("dflt");
      if (defaultValueObject instanceof String) {
        // mix of numbers and Strings - use String in java
        return generateStringMember(memberSchema, parentName, lowerCamelName);
      }
      defaultValue = Double.toString(memberSchema.getBigDecimal("dflt").doubleValue());
    }
    return memberDeclaration("Double",
                             lowerCamelName,
                             defaultValue,
                             false) + memberSetter("Double",
                                                   lowerCamelName,
                                                   parentName,
                                                   false);
  }

  private static String generateBooleanMember(JSONObject memberSchema, String parentName,
                                              String lowerCamelName) {
    // TODO implement fullSpec see valObjects
    String defaultValue = null;
    if (memberSchema.has("dflt") && !memberSchema.isNull("dflt")) {
      defaultValue = Boolean.toString(memberSchema.getBoolean("dflt"));
    }
    return memberDeclaration("boolean",
                             lowerCamelName,
                             defaultValue,
                             false) + memberSetter("boolean",
                                                   lowerCamelName,
                                                   parentName,
                                                   false);
  }

  @NotNull
  private static String generateIntegerMember(JSONObject memberSchema, String parentName,
                                              String lowerCamelName) {
    // TODO implement full spec see valObjects
    String defaultValue = null;
    if (memberSchema.has("dflt") && !memberSchema.isNull("dflt")) {
      defaultValue = memberSchema.getBigInteger("dflt").toString();
    }
    return memberDeclaration("int", lowerCamelName, defaultValue, false) + memberSetter(
            "int",
            lowerCamelName,
            parentName,
            false);
  }

  private static String generateStringMember(JSONObject memberSchema, String parentName,
                                             String lowerCamelName) {
    // TODO implement fullSpec see valObjects
    String defaultValue = null;
    if (memberSchema.has("dflt") && !memberSchema.isNull("dflt")) {
      // FIXME CYRIL implement proper escaping
      defaultValue =
              "\"" + StringEscapeUtils.escapeJava(memberSchema.getString("dflt")) + "\"";
    }
    return memberDeclaration("String",
                             lowerCamelName,
                             defaultValue,
                             false) + memberSetter("String",
                                                   lowerCamelName,
                                                   parentName,
                                                   false);
  }

  private static String memberDeclaration(String type, String name,
                                          final @Nullable String defaultValue,
                                          final boolean isRepeated) {
    return String.format("private %s %s%s;\n",
                         listType(type, isRepeated),
                         listName(name, isRepeated),
                         defaultValue != null ? "=" + defaultValue : "");
  }

  private static String memberSetter(final String type, final String name,
                                     final String parentName, final boolean isRepeated) {
    final String firstUpperName = name.substring(0, 1).toUpperCase() + name.substring(1);
    return String.format("""
                                 public %s set%s(final %s %s) {
                                   this.%s=%s;
                                   return this;
                                 }
                                                          
                                 """,
                         parentName,
                         listName(firstUpperName, isRepeated),
                         listType(type, isRepeated),
                         listName(name, isRepeated),
                         listName(name, isRepeated),
                         listName(name, isRepeated));
  }

  private static String listName(String name, boolean isRepeated) {
    return isRepeated ? name + "List" : name;
  }

  private static String listType(String type, boolean isRepeated) {
    return isRepeated ? "List<" + type + ">" : type;
  }
}
