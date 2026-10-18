package br.pessoal.vozlivre;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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

  /** Nearest Voz Livre category color for an ARGB background. */
  static String color(long argb) {
    if (argb == 0) return "blue";
    float[] hsv = new float[3];
    Color.colorToHSV((int) argb, hsv);
    if (hsv[1] < .12f) return "blue";
    float h = hsv[0];
    if (h < 15 || h >= 345) return "red";
    if (h < 45) return "orange";
    if (h < 70) return "yellow";
    if (h < 170) return "green";
    if (h < 260) return "blue";
    if (h < 300) return "purple";
    return "pink";
  }

  /** Saves a page-set photo (PNG/JPEG owned by the board author) into the app's photo folder. */
  static String saveImage(Context context, byte[] data) {
    try {
      BitmapFactory.Options bounds = new BitmapFactory.Options();
      bounds.inJustDecodeBounds = true;
      BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
      BitmapFactory.Options o = new BitmapFactory.Options();
      o.inSampleSize = 1;
      while (bounds.outWidth / o.inSampleSize > 1024 || bounds.outHeight / o.inSampleSize > 1024)
        o.inSampleSize *= 2;
      Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length, o);
      if (b == null) return null;
      float scale = Math.min(1f, 512f / Math.max(b.getWidth(), b.getHeight()));
      if (scale < 1f)
        b =
            Bitmap.createScaledBitmap(
                b, Math.max(1, (int) (b.getWidth() * scale)), Math.max(1, (int) (b.getHeight() * scale)), true);
      String name = "user/" + Model.id() + ".png";
      File out = new File(context.getFilesDir(), name);
      out.getParentFile().mkdirs();
      try (FileOutputStream f = new FileOutputStream(out)) {
        if (!b.compress(Bitmap.CompressFormat.PNG, 100, f)) return null;
      }
      if (out.length() > 4000000) {
        out.delete();
        return null;
      }
      return name;
    } catch (Exception e) {
      return null;
    }
  }

  static String str(Cursor c, String name) {
    int i = c.getColumnIndex(name);
    return i < 0 || c.isNull(i) ? "" : c.getString(i);
  }

  static Result read(Context context, byte[] bytes) throws Exception {
    if (bytes.length < 16
        || !new String(bytes, 0, 15, StandardCharsets.US_ASCII).equals("SQLite format 3"))
      throw new IOException(
          I18n.t("Este arquivo não usa o formato SQLite SPS/SPB reconhecido. Backups SUB ainda não são suportados."));
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
            throw new IOException(I18n.t("Esquema de prancha não reconhecido: ") + name);
        }
      JSONArray pages = new JSONArray();
      Map<Long, JSONObject> byId = new LinkedHashMap<>();
      Map<String, String> byUnique = new HashMap<>();
      int count = 0, blocked = 0, combined = 0, missingImages = 0;
      String home = "";
      Map<Long, String> dimensions = new HashMap<>();
      String defaultDimension = "";
      try (Cursor c = db.rawQuery("SELECT GridDimension FROM PageSetProperties LIMIT 1", null)) {
        if (c.moveToFirst()) defaultDimension = str(c, "GridDimension");
      } catch (Exception ignored) {
      }
      try (Cursor c = db.rawQuery("SELECT Id,UniqueId,Title FROM Page LIMIT 1000", null)) {
        while (c.moveToNext()) {
          if (pages.length() >= 999) throw new IOException(I18n.t("Prancha com páginas demais."));
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
      try (Cursor c = db.rawQuery("SELECT Id,GridDimension FROM Page LIMIT 1000", null)) {
        while (c.moveToNext()) dimensions.put(c.getLong(0), str(c, "GridDimension"));
      } catch (Exception ignored) {
      }
      // One layout per page: the one for the page's grid size, else the one placing most buttons.
      Map<Long, long[]> placement = new HashMap<>(); // element reference -> {row, column, visible}
      try (Cursor c =
          db.rawQuery(
              "SELECT pl.Id,pl.PageId,pl.PageLayoutSetting,COUNT(ep.Id) FROM PageLayout pl LEFT JOIN ElementPlacement ep ON ep.PageLayoutId=pl.Id GROUP BY pl.Id LIMIT 10000",
              null)) {
        Map<Long, Long> chosen = new HashMap<>();
        Map<Long, Integer> best = new HashMap<>();
        while (c.moveToNext()) {
          long layout = c.getLong(0), page = c.getLong(1);
          String setting = str(c, "PageLayoutSetting");
          String dim = dimensions.getOrDefault(page, "");
          if (dim.isEmpty()) dim = defaultDimension;
          int score = c.getInt(3) + (!dim.isEmpty() && setting.startsWith(dim + ",") ? 1000000 : 0);
          if (score > best.getOrDefault(page, -1)) {
            best.put(page, score);
            chosen.put(page, layout);
          }
        }
        for (long layout : chosen.values())
          try (Cursor e =
              db.rawQuery(
                  "SELECT ElementReferenceId,GridPosition,Visible FROM ElementPlacement WHERE PageLayoutId=? LIMIT 100001",
                  new String[] {String.valueOf(layout)})) {
            while (e.moveToNext()) {
              String[] xy = str(e, "GridPosition").split(",");
              if (xy.length != 2) continue;
              try {
                placement.put(
                    e.getLong(0),
                    new long[] {
                      Long.parseLong(xy[1].trim()), Long.parseLong(xy[0].trim()), e.isNull(2) ? 1 : e.getLong(2)
                    });
              } catch (NumberFormatException ignored) {
              }
            }
          }
      } catch (Exception ignored) {
      }
      Map<Long, String> photos = new HashMap<>();
      int imported = 0, libraryImages = 0;
      long importedBytes = 0;
      if (pages.length() == 0) throw new IOException(I18n.t("Nenhuma página encontrada."));
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
              "SELECT b.Id,b.Label,b.Message,b.LibrarySymbolId,b.PageSetImageId,er.PageId,er.Id AS RefId,er.BackgroundColor FROM Button b JOIN ElementReference er ON er.Id=b.ElementReferenceId ORDER BY er.PageId,b.Id LIMIT 100001",
              null)) {
        while (c.moveToNext()) {
          if (++count > 100000) throw new IOException(I18n.t("Prancha com botões demais."));
          JSONObject page = byId.get(c.getLong(c.getColumnIndex("PageId")));
          if (page == null) continue;
          JSONArray a = page.optJSONArray("buttons");
          if (a.length() >= 600) throw new IOException(I18n.t("Uma página excede 600 botões."));
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
          int bg = c.getColumnIndex("BackgroundColor");
          if (target == null && bg >= 0 && !c.isNull(bg)) Model.put(b, "color", color(c.getLong(bg)));
          long[] place = placement.get(c.getLong(c.getColumnIndex("RefId")));
          if (place != null) {
            Model.put(b, "row", place[0]);
            Model.put(b, "col", place[1]);
            if (place[2] == 0) Model.put(b, "hidden", true);
          }
          String library = str(c, "LibrarySymbolId"), picture = str(c, "PageSetImageId");
          boolean hasPicture = !picture.isEmpty() && !picture.equals("0");
          String photo = null;
          if (hasPicture) {
            photo = photos.get(Long.parseLong(picture));
            if (photo == null && importedBytes < 12000000)
              try (Cursor d =
                  db.rawQuery(
                      "SELECT Identifier,Data FROM PageSetData WHERE Id=?", new String[] {picture})) {
                if (d.moveToFirst() && str(d, "Identifier").startsWith("IMG:")) {
                  byte[] data = d.getBlob(1);
                  if (data != null && data.length < 8000000) {
                    photo = saveImage(context, data);
                    if (photo != null) {
                      photos.put(Long.parseLong(picture), photo);
                      imported++;
                      importedBytes += new File(context.getFilesDir(), photo).length();
                    }
                  }
                } else libraryImages++;
              } catch (Exception ignored) {
              }
          }
          if (photo != null) Model.put(b, "symbol", photo);
          else if ((!library.isEmpty() && !library.equals("0")) || hasPicture) missingImages++;
          a.put(b);
        }
      }
      // Keep the original reading order: row by row, left to right.
      for (int i = 0; i < pages.length(); i++) {
        JSONArray buttons = pages.optJSONObject(i).optJSONArray("buttons");
        ArrayList<JSONObject> list = new ArrayList<>();
        for (int j = 0; j < buttons.length(); j++) list.add(buttons.optJSONObject(j));
        list.sort(
            (x, y) -> {
              long rx = x.optLong("row", Long.MAX_VALUE), ry = y.optLong("row", Long.MAX_VALUE);
              if (rx != ry) return Long.compare(rx, ry);
              return Long.compare(x.optLong("col", Long.MAX_VALUE), y.optLong("col", Long.MAX_VALUE));
            });
        JSONArray sorted = new JSONArray();
        for (JSONObject b : list) {
          b.remove("row");
          b.remove("col");
          sorted.put(b);
        }
        Model.put(pages.optJSONObject(i), "buttons", sorted);
      }
      JSONObject landing = new JSONObject();
      Model.put(landing, "id", "snap-indice");
      Model.put(landing, "title", I18n.t("Páginas importadas"));
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
      if (menu.length() > 600) throw new IOException(I18n.t("Índice de importação muito grande."));
      pages.put(landing);
      // The index is always available even when an original home page has a proprietary toolbar.
      JSONObject profile = new JSONObject();
      Model.put(profile, "id", Model.id());
      Model.put(profile, "name", I18n.t("Prancha importada"));
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
              + I18n.t(" páginas (incluindo índice), ")
              + count
              + I18n.t(" botões.\n\n")
              + blocked
              + I18n.t(" botões com ações não reconhecidas ficarão desativados até você revisar no editor.\n")
              + combined
              + I18n.t(" botões combinados foram simplificados para navegação.\n")
              + imported
              + I18n.t(" fotos da prancha foram importadas. ")
              + missingImages
              + I18n.t(" imagens não foram transferidas")
              + (libraryImages > 0 || missingImages > 0
                  ? I18n.t(" (símbolos de bibliotecas comerciais, como PCS, não podem ser copiados)")
                  : "")
              + I18n.t(".\n\nA ordem dos botões, as cores e os botões ocultos seguem o original. O tamanho da grade, as vozes e as ações avançadas não são preservados. Você poderá trocar os símbolos no editor. Confira as páginas antes de usar.");
      return result;
    } finally {
      if (db != null) db.close();
      file.delete();
    }
  }
}
