package br.pessoal.vozlivre;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Read-only import of the observed SQLite page-bundle schema. Never runs stored actions. */
final class SnapImporter {
  static final class Result {
    JSONObject profile;
    String summary;
  }

  static String str(Cursor c, String name) {
    int i = c.getColumnIndex(name);
    return i < 0 || c.isNull(i) ? "" : c.getString(i);
  }

  static Result read(Context context, byte[] bytes) throws Exception {
    if (bytes.length < 16
        || !new String(bytes, 0, 15, StandardCharsets.US_ASCII).equals("SQLite format 3"))
      throw new IOException(
          "Este arquivo não usa o formato SQLite SPS/SPB reconhecido. Backups SUB ainda não são"
              + " suportados.");
    File file = File.createTempFile("prancha-", ".db", context.getCacheDir());
    Repository.write(file, bytes);
    SQLiteDatabase db = null;
    try {
      db =
          SQLiteDatabase.openDatabase(
              file.getAbsolutePath(),
              null,
              SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
      for (String name : new String[] {"Page", "Button", "ElementReference", "CommandSequence"})
        try (Cursor c =
            db.rawQuery("SELECT type FROM sqlite_master WHERE name=?", new String[] {name})) {
          if (!c.moveToFirst() || !c.getString(0).equals("table"))
            throw new IOException("Esquema de prancha não reconhecido: " + name);
        }
      JSONArray pages = new JSONArray();
      Map<Long, JSONObject> byId = new LinkedHashMap<>();
      Map<String, String> byUnique = new HashMap<>();
      int count = 0, blocked = 0, combined = 0, missingImages = 0;
      String home = "";
      try (Cursor c = db.rawQuery("SELECT Id,UniqueId,Title FROM Page LIMIT 1000", null)) {
        while (c.moveToNext()) {
          if (pages.length() >= 999) throw new IOException("Prancha com páginas demais.");
          JSONObject p = new JSONObject();
          String id = "snap-" + c.getLong(0);
          Model.put(p, "id", id);
          Model.put(p, "title", str(c, "Title"));
          Model.put(p, "columns", 3);
          Model.put(p, "buttons", new JSONArray());
          pages.put(p);
          byId.put(c.getLong(0), p);
          byUnique.put(str(c, "UniqueId"), id);
        }
      }
      if (pages.length() == 0) throw new IOException("Nenhuma página encontrada.");
      try (Cursor c =
          db.rawQuery("SELECT DefaultHomePageUniqueId FROM PageSetProperties LIMIT 1", null)) {
        if (c.moveToFirst()) home = byUnique.getOrDefault(c.getString(0), "");
      } catch (Exception ignored) {
      }
      Map<Long, String> links = new HashMap<>();
      try (Cursor c =
          db.rawQuery("SELECT ButtonId,PageUniqueId FROM ButtonPageLink LIMIT 100001", null)) {
        while (c.moveToNext()) links.put(c.getLong(0), c.getString(1));
      } catch (Exception ignored) {
      }
      Map<Long, String> commands = new HashMap<>();
      try (Cursor c =
          db.rawQuery(
              "SELECT ButtonId,SerializedCommands FROM CommandSequence LIMIT 100001", null)) {
        while (c.moveToNext()) commands.put(c.getLong(0), c.getString(1));
      }
      try (Cursor c =
          db.rawQuery(
              "SELECT b.Id,b.Label,b.Message,b.LibrarySymbolId,b.PageSetImageId,er.PageId FROM"
                  + " Button b JOIN ElementReference er ON er.Id=b.ElementReferenceId ORDER BY"
                  + " er.PageId,b.Id LIMIT 100001",
              null)) {
        while (c.moveToNext()) {
          if (++count > 100000) throw new IOException("Prancha com botões demais.");
          JSONObject page = byId.get(c.getLong(c.getColumnIndex("PageId")));
          if (page == null) continue;
          JSONArray a = page.optJSONArray("buttons");
          if (a.length() >= 600) throw new IOException("Uma página excede 600 botões.");
          JSONObject b = new JSONObject();
          String label = str(c, "Label"), message = str(c, "Message");
          Model.put(b, "id", Model.id());
          Model.put(b, "label", label);
          Model.put(b, "message", message.isEmpty() ? label : message);
          Model.put(b, "type", "word");
          Model.put(b, "color", "blue");
          Model.put(b, "symbol", "");
          Model.put(b, "hidden", false);
          String target = byUnique.get(links.get(c.getLong(0)));
          boolean supported = true;
          int actions = 0;
          String raw = commands.get(c.getLong(0));
          if (raw != null && !raw.isEmpty())
            try {
              JSONArray list = new JSONObject(raw).optJSONArray("$values");
              if (list == null) supported = false;
              else {
                actions = list.length();
                for (int i = 0; i < list.length(); i++) {
                  JSONObject cmd = list.optJSONObject(i);
                  if (cmd == null) {
                    supported = false;
                    break;
                  }
                  String t = cmd.optString("$type");
                  if (t.equals("2")) {
                    if (target == null) target = byUnique.get(cmd.optString("LinkedPageId"));
                  } else if (!t.equals("3") || cmd.optInt("MessageAction", -1) != 0)
                    supported = false;
                }
              }
            } catch (Exception e) {
              supported = false;
            }
          if (target != null) {
            Model.put(b, "type", "link");
            Model.put(b, "target", target);
            Model.put(b, "color", "pink");
            if (actions > 1) combined++;
          } else if (raw == null || actions == 0) supported = false;
          if (!supported) {
            Model.put(b, "disabled", true);
            blocked++;
          }
          String library = str(c, "LibrarySymbolId"), picture = str(c, "PageSetImageId");
          if ((!library.isEmpty() && !library.equals("0"))
              || (!picture.isEmpty() && !picture.equals("0"))) missingImages++;
          a.put(b);
        }
      }
      JSONObject landing = new JSONObject();
      Model.put(landing, "id", "snap-indice");
      Model.put(landing, "title", "Páginas importadas");
      Model.put(landing, "columns", 3);
      JSONArray menu = new JSONArray();
      Model.put(landing, "buttons", menu);
      for (int i = 0; i < pages.length(); i++) {
        JSONObject p = pages.optJSONObject(i);
        if (p.optJSONArray("buttons").length() > 0) {
          JSONObject b = new JSONObject();
          Model.put(b, "id", Model.id());
          Model.put(b, "label", p.optString("title"));
          Model.put(b, "message", "");
          Model.put(b, "type", "link");
          Model.put(b, "target", p.optString("id"));
          Model.put(b, "color", "pink");
          menu.put(b);
        }
      }
      if (menu.length() > 600) throw new IOException("Índice de importação muito grande.");
      pages.put(landing);
      // The index is always available even when an original home page has a proprietary toolbar.
      JSONObject profile = new JSONObject();
      Model.put(profile, "id", Model.id());
      Model.put(profile, "name", "Prancha importada");
      Model.put(profile, "home", "snap-indice");
      Model.put(profile, "pages", pages);
      JSONObject settings = new JSONObject();
      Model.put(settings, "columns", 3);
      Model.put(settings, "font", 19);
      Model.put(settings, "method", "touch");
      Model.put(profile, "settings", settings);
      Model.put(profile, "message", new JSONArray());
      Model.put(profile, "notes", new JSONArray());
      Model.put(profile, "routines", new JSONArray());
      Result result = new Result();
      result.profile = profile;
      result.summary =
          pages.length()
              + " páginas (incluindo índice), "
              + count
              + " botões.\n\n"
              + blocked
              + " botões com ações não reconhecidas ficarão desativados até você revisar no"
              + " editor.\n"
              + combined
              + " botões combinados foram simplificados para navegação.\n"
              + missingImages
              + " referências a imagens não foram transferidas.\n\n"
              + "A posição, as dimensões, as vozes e as ações avançadas do original não são"
              + " preservadas. Você poderá substituir os símbolos no editor. Confira as páginas"
              + " antes de usar.";
      return result;
    } finally {
      if (db != null) db.close();
      file.delete();
    }
  }
}
