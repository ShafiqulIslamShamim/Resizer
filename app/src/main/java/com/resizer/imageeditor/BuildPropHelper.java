package com.resizer.imageeditor;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

public class BuildPropHelper {

  public static String getBuildPropInfo(String className) {
    StringBuilder sb = new StringBuilder();
    try {
      Class<?> cls = Class.forName(className);
      Field[] fields = cls.getDeclaredFields();

      // Sort fields alphabetically for easier readability
      Arrays.sort(fields, Comparator.comparing(Field::getName));

      for (Field field : fields) {
        try {
          field.setAccessible(true);
          Object value = field.get(null);
          sb.append(field.getName()).append(" : ").append(objectToString(value)).append("\n");
        } catch (Exception e) {
          sb.append(field.getName())
              .append(" : [Error accessing value: ")
              .append(e.getClass().getSimpleName())
              .append("]\n");
        }
      }
    } catch (ClassNotFoundException e) {
      sb.append("Class not found: ").append(className).append("\n");
    } catch (Exception e) {
      sb.append("Unexpected error: ").append(e.toString()).append("\n");
    }
    return sb.toString();
  }

  public static String objectToString(Object obj) {
    if (obj == null) return "null";

    try {
      Class<?> objClass = obj.getClass();

      // Handle primitive or object arrays
      if (objClass.isArray()) {
        int length = Array.getLength(obj);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < length; i++) {
          Object element = Array.get(obj, i);
          sb.append(objectToString(element));
          if (i < length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
      }

      // Handle collections (e.g., List, Set)
      if (obj instanceof Collection) {
        Collection<?> collection = (Collection<?>) obj;
        StringBuilder sb = new StringBuilder("[");
        Iterator<?> it = collection.iterator();
        while (it.hasNext()) {
          sb.append(objectToString(it.next()));
          if (it.hasNext()) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
      }

      // Handle maps (e.g., HashMap)
      if (obj instanceof Map) {
        Map<?, ?> map = (Map<?, ?>) obj;
        StringBuilder sb = new StringBuilder("{");
        Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry<?, ?> entry = it.next();
          sb.append(objectToString(entry.getKey()))
              .append("=")
              .append(objectToString(entry.getValue()));
          if (it.hasNext()) sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
      }

      // Handle everything else via toString()
      return obj.toString();

    } catch (Exception e) {
      return "[objectToString failed: " + e.getClass().getSimpleName() + "]";
    }
  }
}
