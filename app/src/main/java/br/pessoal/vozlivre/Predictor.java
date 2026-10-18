package br.pessoal.vozlivre;

import java.text.Normalizer;
import java.util.*;
import org.json.*;

/**
 * Local next-word suggestions. Learns word and word-pair counts per profile from spoken messages;
 * nothing leaves the device.
 */
public final class Predictor {
  static final int MAX_ENTRIES = 4000;

  private Predictor() {}

  static String fold(String s) {
    return Normalizer.normalize(s, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }

  public static List<String> words(String text) {
    ArrayList<String> out = new ArrayList<>();
    for (String w : text.toLowerCase(new Locale("pt", "BR")).split("[^\\p{L}\\p{N}'-]+"))
      if (!w.isEmpty() && w.length() <= 40) out.add(w);
    return out;
  }

  static JSONObject table(JSONObject usage, String name) {
    JSONObject t = usage.optJSONObject(name);
    if (t == null) {
      t = new JSONObject();
      Model.put(usage, name, t);
    }
    return t;
  }

  static JSONObject usage(JSONObject profile) {
    JSONObject u = profile.optJSONObject("usage");
    if (u == null) {
      u = new JSONObject();
      Model.put(profile, "usage", u);
    }
    return u;
  }

  static void add(JSONObject t, String key) {
    Model.put(t, key, Math.min(1000000, t.optInt(key) + 1));
  }

  /** Records a message the person actually said. */
  public static void learn(JSONObject profile, String text) {
    List<String> w = words(text);
    if (w.isEmpty()) return;
    JSONObject u = usage(profile), single = table(u, "w"), pairs = table(u, "b");
    String previous = "";
    for (String word : w) {
      add(single, word);
      add(pairs, previous + " " + word);
      previous = word;
    }
    prune(single);
    prune(pairs);
  }

  static void prune(JSONObject t) {
    if (t.length() <= MAX_ENTRIES) return;
    ArrayList<String> keys = new ArrayList<>();
    Iterator<String> it = t.keys();
    while (it.hasNext()) keys.add(it.next());
    keys.sort(Comparator.comparingInt(t::optInt));
    for (int i = 0; i < keys.size() - MAX_ENTRIES * 3 / 4; i++) t.remove(keys.get(i));
  }

  public static void forget(JSONObject profile) {
    profile.remove("usage");
  }

  /**
   * @param previous last word already in the message, or "" at the start
   * @param prefix letters typed for the current word, or ""
   * @param vocabulary words available on the person's pages, in display form
   * @param seed starter pairs: {"next": {"word": ["follower", ...]}}
   */
  public static List<String> suggest(
      JSONObject profile,
      String previous,
      String prefix,
      Collection<String> vocabulary,
      JSONObject seed,
      int limit) {
    String prev = previous.toLowerCase(new Locale("pt", "BR")).trim();
    String start = fold(prefix.trim());
    JSONObject u = profile.optJSONObject("usage");
    JSONObject single = u == null ? null : u.optJSONObject("w");
    JSONObject pairs = u == null ? null : u.optJSONObject("b");
    Map<String, Double> score = new HashMap<>();
    Map<String, String> display = new HashMap<>();
    for (String v : vocabulary) display.putIfAbsent(fold(v), v);
    if (pairs != null) {
      Iterator<String> it = pairs.keys();
      String head = prev + " ";
      while (it.hasNext()) {
        String k = it.next();
        if (k.startsWith(head)) bump(score, display, k.substring(head.length()), 4.0 * pairs.optInt(k));
      }
    }
    JSONObject next = seed == null ? null : seed.optJSONObject("next");
    JSONArray starters = next == null ? null : next.optJSONArray(prev);
    if (starters != null)
      for (int i = 0; i < starters.length(); i++)
        bump(score, display, starters.optString(i), 3.0 * (starters.length() - i) / starters.length());
    if (single != null && (!start.isEmpty() || score.size() < limit)) {
      Iterator<String> it = single.keys();
      while (it.hasNext()) {
        String k = it.next();
        bump(score, display, k, Math.min(2.5, .25 * single.optInt(k)));
      }
    }
    if (!start.isEmpty()) for (String v : vocabulary) if (!v.contains(" ")) bump(score, display, v, .1);
    ArrayList<String> keys = new ArrayList<>();
    for (String k : score.keySet())
      if (k.startsWith(start) && !k.equals(start) && !k.equals(fold(prev))) keys.add(k);
    keys.sort(
        (a, b) -> {
          int c = Double.compare(score.get(b), score.get(a));
          return c != 0 ? c : a.compareTo(b);
        });
    ArrayList<String> out = new ArrayList<>();
    for (String k : keys) {
      if (out.size() >= limit) break;
      out.add(display.get(k));
    }
    return out;
  }

  static void bump(Map<String, Double> score, Map<String, String> display, String word, double n) {
    if (word.trim().isEmpty() || n <= 0) return;
    String k = fold(word.trim());
    score.merge(k, n, Double::sum);
    display.putIfAbsent(k, word.trim());
  }

  /** The last word of a message, for next-word context. */
  public static String last(String text) {
    List<String> w = words(text);
    return w.isEmpty() ? "" : w.get(w.size() - 1);
  }
}
