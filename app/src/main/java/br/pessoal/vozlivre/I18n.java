package br.pessoal.vozlivre;

import java.util.*;
import org.json.*;

/**
 * Interface translations. Portuguese is the source text and the key; a dictionary per language
 * (assets/i18n/en.json, es.json) maps it to the translation. Missing entries fall back to Portuguese.
 */
public final class I18n {
  public static final String[] LANGUAGES = {"pt", "en", "es"};
  private static Map<String, String> table = Collections.emptyMap();
  private static String language = "pt";

  private I18n() {}

  public static String t(String pt) {
    String v = table.get(pt);
    return v == null ? pt : v;
  }

  public static String language() {
    return language;
  }

  /** Installs a dictionary ({"strings": {pt: translation}}); null or "pt" restores Portuguese. */
  public static void use(String lang, JSONObject dictionary) {
    language = lang == null ? "pt" : lang;
    if (dictionary == null || "pt".equals(language)) {
      table = Collections.emptyMap();
      return;
    }
    HashMap<String, String> m = new HashMap<>();
    JSONObject s = dictionary.optJSONObject("strings");
    if (s != null) {
      Iterator<String> keys = s.keys();
      while (keys.hasNext()) {
        String k = keys.next();
        m.put(k, s.optString(k));
      }
    }
    table = m;
  }

  /** "auto" follows the device: English or Spanish when the device uses them, else Portuguese. */
  public static String resolve(String setting, String deviceLanguage) {
    if (setting != null && Arrays.asList(LANGUAGES).contains(setting)) return setting;
    return Arrays.asList(LANGUAGES).contains(deviceLanguage) ? deviceLanguage : "pt";
  }
}
