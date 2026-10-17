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
          "A mensagem está muito longa. Salve em uma nota ou limpe antes de continuar.");
    checkpoint();
    JSONObject t = new JSONObject();
    put(t, "text", text.trim());
    put(t, "symbol", symbol);
    message().put(t);
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
      throw new IllegalArgumentException("Limite de 16 perfis.");
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
    if (pages().length() >= 1000) throw new IllegalArgumentException("Limite de páginas atingido.");
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
    put(b, "label", "Nova palavra");
    put(b, "message", "Nova palavra");
    put(b, "type", "word");
    put(b, "color", "blue");
    put(b, "symbol", "");
    put(b, "hidden", false);
    return b;
  }

  public static void validate(JSONObject r) throws JSONException {
    if (!"voz-livre".equals(r.optString("format")) || r.optInt("version") != 1)
      throw new JSONException("Arquivo não é um backup Voz Livre compatível.");
    JSONArray profiles = r.optJSONArray("profiles");
    if (profiles == null || profiles.length() == 0 || profiles.length() > 16)
      throw new JSONException("Quantidade de perfis inválida.");
    Set<String> profileIds = new HashSet<>();
    boolean active = false;
    for (int p = 0; p < profiles.length(); p++) {
      JSONObject u = profiles.getJSONObject(p);
      String uid = u.getString("id");
      if (uid.isEmpty() || !profileIds.add(uid)) throw new JSONException("Perfil duplicado.");
      active |= uid.equals(r.optString("activeProfile"));
      JSONArray pages = u.getJSONArray("pages");
      if (pages.length() == 0 || pages.length() > 1000)
        throw new JSONException("Quantidade de páginas inválida.");
      Set<String> ids = new HashSet<>();
      for (int i = 0; i < pages.length(); i++) {
        JSONObject pg = pages.getJSONObject(i);
        String pageId = pg.getString("id");
        if (pageId.isEmpty() || !ids.add(pageId)) throw new JSONException("Página duplicada.");
        JSONArray buttons = pg.getJSONArray("buttons");
        if (buttons.length() > 600) throw new JSONException("Página grande demais.");
        for (int j = 0; j < buttons.length(); j++) {
          JSONObject b = buttons.getJSONObject(j);
          if (b.optString("label").length() > 2048 || b.optString("message").length() > 16000)
            throw new JSONException("Texto muito longo.");
          String s = b.optString("symbol");
          if (!s.isEmpty()
              && !s.matches("(?:symbols|user)/[a-zA-Z0-9_.-]+\\.(?:png|jpg|jpeg|webp)"))
            throw new JSONException("Caminho de imagem inválido.");
        }
      }
      if (!ids.contains(u.optString("home")))
        throw new JSONException("Página inicial não encontrada.");
      if (u.optJSONObject("settings") == null) throw new JSONException("Configurações ausentes.");
      JSONArray m = u.optJSONArray("message");
      if (m != null) {
        if (m.length() > 256) throw new JSONException("Mensagem grande demais.");
        int length = 0;
        for (int i = 0; i < m.length(); i++) {
          JSONObject token = m.getJSONObject(i);
          length += token.optString("text").length() + 1;
          String symbol = token.optString("symbol");
          if (length > 16256) throw new JSONException("Texto muito longo.");
          if (!symbol.isEmpty()
              && !symbol.matches("(?:symbols|user)/[a-zA-Z0-9_.-]+\\.(?:png|jpg|jpeg|webp)"))
            throw new JSONException("Caminho de imagem inválido na mensagem.");
        }
      }
      if (u.optJSONArray("notes") == null) put(u, "notes", new JSONArray());
      if (u.optJSONArray("routines") == null) put(u, "routines", new JSONArray());
    }
    if (!active) throw new JSONException("Perfil ativo inexistente.");
    if (r.toString().length() > 24000000) throw new JSONException("Backup grande demais.");
  }
}
