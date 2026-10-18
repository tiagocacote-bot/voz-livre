package br.pessoal.vozlivre;

import java.util.*;
import org.json.*;

/** Local communication data. No Android dependencies, network or vendor protocol. */
public final class Model {
  public JSONObject root;
  private final Deque<JSONArray> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
  private final Deque<String> edits = new ArrayDeque<>();

  public Model(String json) throws JSONException {
    root = new JSONObject(json);
    validate(root);
  }

  public static JSONObject copy(JSONObject o) {
    try {
      return new JSONObject(o.toString());
    } catch (JSONException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static JSONArray copy(JSONArray o) {
    try {
      return new JSONArray(o.toString());
    } catch (JSONException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static void put(JSONObject o, String k, Object v) {
    try {
      o.put(k, v);
    } catch (JSONException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static String id() {
    return UUID.randomUUID().toString();
  }

  public JSONObject profile() {
    JSONArray a = root.optJSONArray("profiles");
    for (int i = 0; i < a.length(); i++)
      if (a.optJSONObject(i).optString("id").equals(root.optString("activeProfile")))
        return a.optJSONObject(i);
    return a.optJSONObject(0);
  }

  public JSONObject settings() {
    return profile().optJSONObject("settings");
  }

  public JSONArray pages() {
    return profile().optJSONArray("pages");
  }

  public JSONObject page(String id) {
    JSONArray a = pages();
    for (int i = 0; i < a.length(); i++)
      if (a.optJSONObject(i).optString("id").equals(id)) return a.optJSONObject(i);
    return null;
  }

  public JSONArray message() {
    JSONArray a = profile().optJSONArray("message");
    if (a == null) {
      a = new JSONArray();
      put(profile(), "message", a);
    }
    return a;
  }

  public String text() {
    StringBuilder s = new StringBuilder();
    JSONArray a = message();
    for (int i = 0; i < a.length(); i++) {
      if (s.length() > 0) s.append(' ');
      s.append(a.optJSONObject(i).optString("text"));
    }
    return s.toString();
  }

  private void checkpoint() {
    undo.push(copy(message()));
    while (undo.size() > 80) undo.removeLast();
    redo.clear();
  }

  public void append(String text, String symbol) {
    if (text.trim().isEmpty()) return;
    if (message().length() >= 256 || text().length() + text.length() > 16000)
      throw new IllegalArgumentException(
          I18n.t("A mensagem está muito longa. Salve em uma nota ou limpe antes de continuar."));
    checkpoint();
    JSONObject t = new JSONObject();
    put(t, "text", text.trim());
    put(t, "symbol", symbol);
    message().put(t);
  }

  /** Replaces the text of one message word (word forms); undoable. */
  public void replace(int i, String text) {
    if (i < 0 || i >= message().length() || text.trim().isEmpty()) return;
    checkpoint();
    put(message().optJSONObject(i), "text", text.trim());
  }

  public void remove() {
    if (message().length() > 0) {
      checkpoint();
      message().remove(message().length() - 1);
    }
  }

  public void clear() {
    if (message().length() > 0) {
      checkpoint();
      put(profile(), "message", new JSONArray());
    }
  }

  public void undo() {
    if (!undo.isEmpty()) {
      redo.push(copy(message()));
      put(profile(), "message", undo.pop());
    }
  }

  public void redo() {
    if (!redo.isEmpty()) {
      undo.push(copy(message()));
      put(profile(), "message", redo.pop());
    }
  }

  /** Local history of spoken messages for this profile (newest last, kept on the device). */
  public JSONArray history() {
    JSONArray a = profile().optJSONArray("history");
    if (a == null) {
      a = new JSONArray();
      put(profile(), "history", a);
    }
    return a;
  }

  public void addHistory(String text) {
    if (text == null || text.trim().isEmpty()) return;
    JSONArray a = history();
    try {
      JSONObject e = new JSONObject();
      String trimmed = text.trim();
      put(e, "text", trimmed.length() > 2000 ? trimmed.substring(0, 2000) : trimmed);
      put(e, "time", System.currentTimeMillis());
      a.put(e);
      while (a.length() > 100) a.remove(0);
    } catch (JSONException ignored) {
    }
  }

  public void clearHistory() {
    put(profile(), "history", new JSONArray());
  }

  public void beforeEdit() {
    edits.push(root.toString());
    while (edits.size() > 20) edits.removeLast();
  }

  public boolean undoEdit() {
    if (edits.isEmpty()) return false;
    try {
      JSONArray profiles = new JSONObject(edits.pop()).getJSONArray("profiles");
      for (int i = 0; i < profiles.length(); i++) {
        JSONObject previous = profiles.getJSONObject(i);
        if (previous.optString("id").equals(profile().optString("id"))) {
          put(profile(), "pages", previous.getJSONArray("pages"));
          put(profile(), "home", previous.getString("home"));
          return true;
        }
      }
      return false;
    } catch (JSONException e) {
      return false;
    }
  }

  public void selectProfile(String id) {
    put(root, "activeProfile", id);
    undo.clear();
    redo.clear();
    edits.clear();
  }

  public JSONObject newProfile(String name, JSONObject template) {
    if (root.optJSONArray("profiles").length() >= 16)
      throw new IllegalArgumentException(I18n.t("Limite de 16 perfis."));
    beforeEdit();
    JSONObject p = copy(template);
    put(p, "id", id());
    put(p, "name", name.trim());
    put(p, "message", new JSONArray());
    root.optJSONArray("profiles").put(p);
    selectProfile(p.optString("id"));
    return p;
  }

  public JSONObject newPage(String title) {
    if (pages().length() >= 1000) throw new IllegalArgumentException(I18n.t("Limite de páginas atingido."));
    beforeEdit();
    JSONObject p = new JSONObject();
    put(p, "id", id());
    put(p, "title", title.trim());
    put(p, "columns", 3);
    put(p, "buttons", new JSONArray());
    pages().put(p);
    return p;
  }

  public JSONObject newButton() {
    JSONObject b = new JSONObject();
    put(b, "id", id());
    put(b, "label", I18n.t("Nova palavra"));
    put(b, "message", I18n.t("Nova palavra"));
    put(b, "type", "word");
    put(b, "color", "blue");
    put(b, "symbol", "");
    put(b, "hidden", false);
    return b;
  }

  /** Symbol set applied to the starter buttons: "arasaac" (default) or "mulberry". */
  public static String symbolSet(JSONObject r) {
    return r.optString("symbolSet", "");
  }

  /**
   * Switches starter-button pictures between ARASAAC and Mulberry/original drawings. Buttons are
   * matched by starter id and current picture, so personal photos and edited symbols are kept.
   *
   * @param legacy {"byButton": {id: [mulberry, arasaac]}, "bySymbol": {mulberry: arasaac}}
   * @return number of pictures changed
   */
  public static int switchSymbols(JSONObject r, JSONObject legacy, boolean toArasaac) {
    JSONObject byButton = legacy.optJSONObject("byButton"), bySymbol = legacy.optJSONObject("bySymbol");
    Map<String, String> symbols = new HashMap<>();
    Iterator<String> keys = bySymbol.keys();
    while (keys.hasNext()) {
      String k = keys.next();
      if (toArasaac) symbols.put(k, bySymbol.optString(k));
      else symbols.putIfAbsent(bySymbol.optString(k), k);
    }
    int changed = 0;
    JSONArray profiles = r.optJSONArray("profiles");
    for (int p = 0; p < profiles.length(); p++) {
      JSONObject profile = profiles.optJSONObject(p);
      JSONArray pages = profile.optJSONArray("pages");
      for (int i = 0; i < pages.length(); i++) {
        JSONArray buttons = pages.optJSONObject(i).optJSONArray("buttons");
        for (int j = 0; j < buttons.length(); j++) {
          JSONObject b = buttons.optJSONObject(j);
          String s = b.optString("symbol"), next = null;
          JSONArray pair = byButton.optJSONArray(b.optString("id"));
          if (pair != null && s.equals(pair.optString(toArasaac ? 0 : 1)))
            next = pair.optString(toArasaac ? 1 : 0);
          else if (symbols.containsKey(s)) next = symbols.get(s);
          if (next != null && !next.equals(s)) {
            put(b, "symbol", next);
            changed++;
          }
        }
      }
      JSONArray message = profile.optJSONArray("message");
      if (message != null)
        for (int i = 0; i < message.length(); i++) {
          JSONObject token = message.optJSONObject(i);
          String next = symbols.get(token.optString("symbol"));
          if (next != null) put(token, "symbol", next);
        }
    }
    put(r, "symbolSet", toArasaac ? "arasaac" : "mulberry");
    return changed;
  }

  /**
   * Once per installation (starterVersion 2): adds the starter pages new in 0.3.0 to
   * profiles made from the starter board, links them from the home page, and gives ARASAAC
   * pictures to buttons that had none before. Pages the person already has are left alone.
   *
   * @return number of pages added
   */
  public static int upgradeStarter(JSONObject r, JSONObject defaults, JSONObject legacy) {
    if (r.optInt("starterVersion") >= 2) return 0;
    JSONObject template = defaults.optJSONArray("profiles").optJSONObject(0);
    JSONArray starterPages = template.optJSONArray("pages");
    JSONObject starterHome = null;
    for (int i = 0; i < starterPages.length(); i++)
      if (starterPages.optJSONObject(i).optString("id").equals(template.optString("home")))
        starterHome = starterPages.optJSONObject(i);
    int added = 0;
    JSONArray profiles = r.optJSONArray("profiles");
    for (int p = 0; p < profiles.length(); p++) {
      JSONObject profile = profiles.optJSONObject(p);
      JSONArray pages = profile.optJSONArray("pages");
      Set<String> ids = new HashSet<>();
      JSONObject home = null;
      for (int i = 0; i < pages.length(); i++) {
        ids.add(pages.optJSONObject(i).optString("id"));
        if (pages.optJSONObject(i).optString("id").equals(profile.optString("home")))
          home = pages.optJSONObject(i);
      }
      if (home == null || !ids.contains(template.optString("home"))) continue; // not a starter profile
      JSONObject settings = profile.optJSONObject("settings");
      if (settings != null && !"pt".equals(settings.optString("language", "pt"))) continue;
      JSONArray newPages = legacy.optJSONArray("pages030");
      Set<String> wanted = new HashSet<>();
      for (int i = 0; newPages != null && i < newPages.length(); i++) wanted.add(newPages.optString(i));
      for (int i = 0; i < starterPages.length() && pages.length() < 1000; i++) {
        JSONObject page = starterPages.optJSONObject(i);
        if (ids.contains(page.optString("id")) || !wanted.contains(page.optString("id"))) continue;
        pages.put(copy(page));
        added++;
        if (starterHome != null && home.optJSONArray("buttons").length() < 600) {
          JSONArray links = starterHome.optJSONArray("buttons");
          for (int j = 0; j < links.length(); j++)
            if (page.optString("id").equals(links.optJSONObject(j).optString("target")))
              home.optJSONArray("buttons").put(copy(links.optJSONObject(j)));
        }
      }
      JSONArray fixed = legacy.optJSONArray("since030");
      JSONObject byButton = legacy.optJSONObject("byButton");
      if (fixed != null && !"mulberry".equals(symbolSet(r)))
        for (int i = 0; i < pages.length(); i++) {
          JSONArray buttons = pages.optJSONObject(i).optJSONArray("buttons");
          for (int j = 0; j < buttons.length(); j++) {
            JSONObject b = buttons.optJSONObject(j);
            JSONArray pair = byButton.optJSONArray(b.optString("id"));
            for (int k = 0; k < fixed.length(); k++)
              if (fixed.optString(k).equals(b.optString("id"))
                  && pair != null
                  && b.optString("symbol").equals(pair.optString(0)))
                put(b, "symbol", pair.optString(1));
          }
        }
    }
    put(r, "starterVersion", 2);
    return added;
  }

  public static void validate(JSONObject r) throws JSONException {
    if (!"voz-livre".equals(r.optString("format")) || r.optInt("version") != 1)
      throw new JSONException(I18n.t("Arquivo não é um backup Voz Livre compatível."));
    JSONArray profiles = r.optJSONArray("profiles");
    if (profiles == null || profiles.length() == 0 || profiles.length() > 16)
      throw new JSONException(I18n.t("Quantidade de perfis inválida."));
    Set<String> profileIds = new HashSet<>();
    boolean active = false;
    for (int p = 0; p < profiles.length(); p++) {
      JSONObject u = profiles.getJSONObject(p);
      String uid = u.getString("id");
      if (uid.isEmpty() || !profileIds.add(uid)) throw new JSONException(I18n.t("Perfil duplicado."));
      active |= uid.equals(r.optString("activeProfile"));
      JSONArray pages = u.getJSONArray("pages");
      if (pages.length() == 0 || pages.length() > 1000)
        throw new JSONException(I18n.t("Quantidade de páginas inválida."));
      Set<String> ids = new HashSet<>();
      for (int i = 0; i < pages.length(); i++) {
        JSONObject pg = pages.getJSONObject(i);
        String pageId = pg.getString("id");
        if (pageId.isEmpty() || !ids.add(pageId)) throw new JSONException(I18n.t("Página duplicada."));
        JSONArray buttons = pg.getJSONArray("buttons");
        if (buttons.length() > 600) throw new JSONException(I18n.t("Página grande demais."));
        for (int j = 0; j < buttons.length(); j++) {
          JSONObject b = buttons.getJSONObject(j);
          if (b.optString("label").length() > 2048 || b.optString("message").length() > 16000)
            throw new JSONException(I18n.t("Texto muito longo."));
          String s = b.optString("symbol");
          if (!s.isEmpty()
              && !s.matches("(?:symbols|user)/[a-zA-Z0-9_.-]+\\.(?:png|jpg|jpeg|webp)"))
            throw new JSONException(I18n.t("Caminho de imagem inválido."));
        }
      }
      if (!ids.contains(u.optString("home")))
        throw new JSONException(I18n.t("Página inicial não encontrada."));
      if (u.optJSONObject("settings") == null) throw new JSONException(I18n.t("Configurações ausentes."));
      JSONArray m = u.optJSONArray("message");
      if (m != null) {
        if (m.length() > 256) throw new JSONException(I18n.t("Mensagem grande demais."));
        int length = 0;
        for (int i = 0; i < m.length(); i++) {
          JSONObject token = m.getJSONObject(i);
          length += token.optString("text").length() + 1;
          String symbol = token.optString("symbol");
          if (length > 16256) throw new JSONException(I18n.t("Texto muito longo."));
          if (!symbol.isEmpty()
              && !symbol.matches("(?:symbols|user)/[a-zA-Z0-9_.-]+\\.(?:png|jpg|jpeg|webp)"))
            throw new JSONException(I18n.t("Caminho de imagem inválido na mensagem."));
        }
      }
      if (u.has("usage") && u.optJSONObject("usage") == null) u.remove("usage");
      if (u.optJSONArray("notes") == null) put(u, "notes", new JSONArray());
      if (u.optJSONArray("routines") == null) put(u, "routines", new JSONArray());
      JSONArray routines = u.optJSONArray("routines");
      for (int i = 0; i < routines.length(); i++) {
        JSONArray steps = routines.getJSONObject(i).optJSONArray("steps");
        if (steps == null) continue;
        if (steps.length() > 200) throw new JSONException(I18n.t("Rotina grande demais."));
        for (int j = 0; j < steps.length(); j++) {
          JSONObject step = steps.optJSONObject(j);
          String symbol = step == null ? "" : step.optString("symbol");
          if (!symbol.isEmpty()
              && !symbol.matches("(?:symbols|user)/[a-zA-Z0-9_.-]+\\.(?:png|jpg|jpeg|webp)"))
            throw new JSONException(I18n.t("Caminho de imagem inválido na rotina."));
        }
      }
    }
    if (!active) throw new JSONException(I18n.t("Perfil ativo inexistente."));
    if (r.toString().length() > 24000000) throw new JSONException(I18n.t("Backup grande demais."));
  }
}
