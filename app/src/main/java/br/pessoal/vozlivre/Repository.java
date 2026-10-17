package br.pessoal.vozlivre;

import android.content.Context;
import android.util.AtomicFile;
import android.util.Base64;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

final class Repository {
  final Context context;
  final AtomicFile state;
  boolean recovered = false;

  Repository(Context c) {
    context = c;
    state = new AtomicFile(new File(c.getFilesDir(), "comunicador.json"));
    new File(c.getFilesDir(), "user").mkdirs();
  }

  static byte[] read(InputStream stream, int max) throws IOException {
    try (InputStream in = stream;
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] b = new byte[8192];
      int n;
      while ((n = in.read(b)) != -1) {
        if (out.size() + n > max) throw new IOException("Arquivo muito grande.");
        out.write(b, 0, n);
      }
      return out.toByteArray();
    }
  }

  String defaults() throws IOException {
    return new String(
        read(context.getAssets().open("default.json"), 8000000), StandardCharsets.UTF_8);
  }

  Model load() throws Exception {
    if (!state.getBaseFile().exists() && !new File(state.getBaseFile().getPath() + ".bak").exists())
      return new Model(defaults());
    try {
      return new Model(new String(read(state.openRead(), 32000000), StandardCharsets.UTF_8));
    } catch (Exception e) {
      recovered = true;
      File snapshot =
          new File(
              context.getFilesDir(), "dados-recuperar-" + System.currentTimeMillis() + ".json");
      try {
        write(snapshot, read(state.openRead(), 32000000));
      } catch (Exception ignored) {
      }
      File previous = new File(context.getFilesDir(), "anterior.json");
      if (previous.exists())
        try {
          return new Model(
              new String(read(new FileInputStream(previous), 32000000), StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
      return new Model(defaults());
    }
  }

  synchronized void save(Model m) throws Exception {
    Model.validate(m.root);
    if (state.getBaseFile().exists())
      try {
        write(new File(context.getFilesDir(), "anterior.json"), read(state.openRead(), 32000000));
      } catch (IOException ignored) {
      }
    FileOutputStream out = null;
    try {
      out = state.startWrite();
      out.write(m.root.toString().getBytes(StandardCharsets.UTF_8));
      state.finishWrite(out);
      if (new File(state.getBaseFile().getPath() + ".new").exists())
        throw new IOException("Não foi possível confirmar a gravação dos dados.");
    } catch (Exception e) {
      if (out != null) state.failWrite(out);
      throw e;
    }
  }

  static void write(File f, byte[] data) throws IOException {
    try (FileOutputStream out = new FileOutputStream(f)) {
      out.write(data);
      out.getFD().sync();
    }
  }

  byte[] backup(Model m) throws Exception {
    JSONObject root = Model.copy(m.root);
    JSONObject images = new JSONObject();
    Set<String> needed = new HashSet<>();
    JSONArray profiles = root.getJSONArray("profiles");
    for (int i = 0; i < profiles.length(); i++) {
      JSONObject p = profiles.getJSONObject(i);
      JSONArray pages = p.getJSONArray("pages");
      for (int j = 0; j < pages.length(); j++) {
        JSONArray a = pages.getJSONObject(j).getJSONArray("buttons");
        for (int k = 0; k < a.length(); k++) {
          String s = a.getJSONObject(k).optString("symbol");
          if (s.startsWith("user/")) needed.add(s);
        }
      }
      JSONArray msg = p.optJSONArray("message");
      if (msg != null)
        for (int j = 0; j < msg.length(); j++) {
          String s = msg.getJSONObject(j).optString("symbol");
          if (s.startsWith("user/")) needed.add(s);
        }
    }
    int total = 0;
    for (String s : needed) {
      if (!safe(s)) throw new IOException("Imagem inválida.");
      byte[] b = read(new FileInputStream(new File(context.getFilesDir(), s)), 4000000);
      total += b.length;
      if (total > 16000000)
        throw new IOException(
            "As fotos ultrapassam o limite de 16 MB do backup. Reduza as imagens antes de"
                + " exportar.");
      Model.put(images, s, Base64.encodeToString(b, Base64.NO_WRAP));
    }
    Model.put(root, "images", images);
    return root.toString().getBytes(StandardCharsets.UTF_8);
  }

  static boolean safe(String s) {
    return s.matches("(?:user|symbols)/[a-zA-Z0-9_.-]+\\.(?:png|jpg|jpeg|webp)");
  }

  Model parseBackup(byte[] bytes) throws Exception {
    JSONObject r = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
    JSONObject images = r.optJSONObject("images");
    r.remove("images");
    Model.validate(r);
    if (images != null) {
      Iterator<String> keys = images.keys();
      int total = 0;
      while (keys.hasNext()) {
        String k = keys.next();
        if (!k.startsWith("user/") || !safe(k)) throw new IOException("Caminho de foto inválido.");
        byte[] b = Base64.decode(images.getString(k), Base64.DEFAULT);
        total += b.length;
        if (b.length > 4000000 || total > 16000000)
          throw new IOException("Limite de imagens excedido.");
      }
    }
    return new Model(r.toString());
  }

  Model restoreImages(byte[] bytes) throws Exception {
    Model restored = parseBackup(bytes);
    JSONObject images =
        new JSONObject(new String(bytes, StandardCharsets.UTF_8)).optJSONObject("images");
    if (images == null) return restored;
    Map<String, String> names = new HashMap<>();
    Iterator<String> keys = images.keys();
    while (keys.hasNext()) {
      String k = keys.next();
      if (!k.startsWith("user/") || !safe(k)) throw new IOException("Imagem inválida.");
      String suffix = k.substring(k.lastIndexOf('.'));
      String newName = "user/" + Model.id() + suffix;
      write(
          new File(context.getFilesDir(), newName),
          Base64.decode(images.getString(k), Base64.DEFAULT));
      names.put(k, newName);
    }
    JSONArray profiles = restored.root.getJSONArray("profiles");
    for (int i = 0; i < profiles.length(); i++) {
      JSONObject profile = profiles.getJSONObject(i);
      JSONArray pages = profile.getJSONArray("pages");
      for (int j = 0; j < pages.length(); j++) {
        JSONArray a = pages.getJSONObject(j).getJSONArray("buttons");
        for (int k = 0; k < a.length(); k++) {
          JSONObject b = a.getJSONObject(k);
          String replacement = names.get(b.optString("symbol"));
          if (replacement != null) Model.put(b, "symbol", replacement);
        }
      }
      JSONArray message = profile.optJSONArray("message");
      if (message != null)
        for (int j = 0; j < message.length(); j++) {
          JSONObject b = message.getJSONObject(j);
          String replacement = names.get(b.optString("symbol"));
          if (replacement != null) Model.put(b, "symbol", replacement);
        }
    }
    return restored;
  }
}
