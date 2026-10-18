package br.pessoal.vozlivre;

import static org.junit.Assert.*;

import android.app.Application;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(
    sdk = {28, 35},
    application = Application.class,
    shadows = RuntimeTest.PosixAtomicRename.class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
public class RuntimeTest {
  /** Android's rename replaces destinations; Windows' renameTo does not. */
  @Implements(value=android.util.AtomicFile.class, minSdk=29)
  public static class PosixAtomicRename {
    @Implementation(minSdk=29)
    protected static void rename(File source,File target) throws IOException {
      Files.move(source.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
    }
  }
  Repository repo;

  @Before
  public void setUp() throws Exception {
    repo = new Repository(RuntimeEnvironment.getApplication());
    repo.save(new Model(repo.defaults()));
  }

  @Test
  public void compose() throws Exception {
    ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
    MainActivity a = c.get();
    assertEquals("inicio", a.current);
    assertEquals(8, a.model.pages().length());
    assertTrue(a.grid.getChildCount() > 10);
    JSONObject page = a.model.page("inicio");
    for (String word : new String[] {"eu", "quero"}) {
      boolean found = false;
      JSONArray b = page.getJSONArray("buttons");
      for (int i = 0; i < b.length(); i++)
        if (b.getJSONObject(i).optString("label").equals(word)) {
          a.activate(b.getJSONObject(i));
          found = true;
          break;
        }
      assertTrue(found);
    }
    assertEquals("eu quero", a.model.text());
    a.model.clear();
    a.model.undo();
    assertEquals("eu quero", a.model.text());
    a.navigate("comida");
    assertEquals("comida", a.current);
    a.goBack();
    assertEquals("inicio", a.current);
    c.pause().stop().destroy();
    assertEquals("eu quero", repo.load().text());
  }

  @Test
  public void hidden() throws Exception {
    ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
    MainActivity a = c.get();
    int n = a.grid.getChildCount();
    Model.put(a.model.page(a.current).getJSONArray("buttons").getJSONObject(0), "hidden", true);
    a.renderPage();
    assertEquals(n, a.grid.getChildCount());
    assertEquals(View.INVISIBLE, a.grid.getChildAt(0).getVisibility());
    a.edit = true;
    a.renderPage();
    assertEquals(View.VISIBLE, a.grid.getChildAt(0).getVisibility());
    c.pause().stop().destroy();
  }

  @Test
  public void backup() throws Exception {
    Model m = repo.load();
    String symbol = "user/photo.jpg";
    byte[] old = {1, 2, 3};
    Repository.write(new File(repo.context.getFilesDir(), symbol), old);
    Model.put(m.page("inicio").getJSONArray("buttons").getJSONObject(0), "symbol", symbol);
    m.append("minha foto", symbol);
    byte[] backup = repo.backup(m);
    Repository.write(new File(repo.context.getFilesDir(), symbol), new byte[] {9, 9, 9});
    Model restored = repo.restoreImages(backup);
    String replacement = restored.message().getJSONObject(0).getString("symbol");
    assertNotEquals(symbol, replacement);
    assertArrayEquals(
        old, Files.readAllBytes(new File(repo.context.getFilesDir(), replacement).toPath()));
    assertArrayEquals(
        new byte[] {9, 9, 9},
        Files.readAllBytes(new File(repo.context.getFilesDir(), symbol).toPath()));
    assertEquals(
        replacement,
        restored.page("inicio").getJSONArray("buttons").getJSONObject(0).getString("symbol"));
    repo.save(restored);
    assertEquals("minha foto", repo.load().text());
  }

  @Test
  public void recovery() throws Exception {
    Model m = repo.load();
    m.append("primeiro", "");
    repo.save(m);
    m.append("segundo", "");
    repo.save(m);
    Repository.write(repo.state.getBaseFile(), "broken".getBytes(StandardCharsets.UTF_8));
    Model recovered = repo.load();
    assertTrue(repo.recovered);
    assertEquals("primeiro", recovered.text());
  }

  @Test
  public void badPath() throws Exception {
    JSONObject data = new JSONObject(repo.defaults());
    JSONObject images = new JSONObject();
    images.put("user/../../fora.jpg", "AQID");
    data.put("images", images);
    try {
      repo.restoreImages(data.toString().getBytes(StandardCharsets.UTF_8));
      fail("invalid path accepted");
    } catch (IOException expected) {
    }
  }

  @Test
  public void symbols() throws Exception {
    ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
    MainActivity a = c.get();
    assertNotNull(a.image("symbols/2248.png", 60));
    assertNull(a.image("user/../../secrets.jpg", 60));
    c.pause().stop().destroy();
  }

  @Test
  public void switches() throws Exception {
    ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
    MainActivity a = c.get();
    Model.put(a.model.settings(), "method", "scan");
    Model.put(a.model.settings(), "scanType", "step");
    a.build();
    int old = a.scanner.selected();
    assertTrue(a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE)));
    assertEquals(old + 1, a.scanner.selected());
    // Select the first word via scanner action, without depending on the current visual card order.
    for (int i = a.fixedTargets; i < a.targets.size(); i++)
      if ("eu".equals(a.targets.get(i).getContentDescription())) {
        while (a.scanner.selected() != i) a.scanner.next();
        a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
        break;
      }
    assertEquals("eu", a.model.text());
    a.paused = true;
    a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
    assertFalse("Acionador deve permitir sair da pausa", a.paused);
    c.pause().stop().destroy();
  }

  @Test
  public void contrast() throws Exception {
    ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
    MainActivity a = c.get();
    Model.put(a.model.settings(), "contrast", true);
    LinearLayout form = a.column();
    TextView label = a.text("teste", 18);
    form.addView(label);
    a.scroll(form);
    assertEquals(a.ink, label.getCurrentTextColor());
    c.pause().stop().destroy();
  }

  @Test
  public void hold() throws Exception {
    ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup();
    MainActivity a=c.get();Model.put(a.model.settings(),"method","hold");Model.put(a.model.settings(),"holdMs",200);
    View button=a.grid.getChildAt(0);
    android.view.MotionEvent down=android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_DOWN,1,1,0);
    a.touch(button,down);
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(199,java.util.concurrent.TimeUnit.MILLISECONDS);
    assertEquals("",a.model.text());
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(1,java.util.concurrent.TimeUnit.MILLISECONDS);
    assertEquals("eu",a.model.text());
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(1000,java.util.concurrent.TimeUnit.MILLISECONDS);
    assertEquals("eu",a.model.text());
    a.model.clear();a.touch(button,down);a.touch(button,android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_CANCEL,1,1,0));
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(300,java.util.concurrent.TimeUnit.MILLISECONDS);
    assertEquals("",a.model.text());down.recycle();c.pause().stop().destroy();
  }

  @Test
  public void release() throws Exception {
    ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup();MainActivity a=c.get();
    Model.put(a.model.settings(),"method","release");View button=a.grid.getChildAt(0);
    android.view.MotionEvent down=android.view.MotionEvent.obtain(0,0,android.view.MotionEvent.ACTION_DOWN,1,1,0);
    android.view.MotionEvent up=android.view.MotionEvent.obtain(0,1,android.view.MotionEvent.ACTION_UP,1,1,0);
    a.touch(button,down);assertEquals("",a.model.text());a.touch(button,up);assertEquals("eu",a.model.text());
    down.recycle();up.recycle();c.pause().stop().destroy();
  }

  static void idle(int ms) {
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
        .idleFor(java.time.Duration.ofMillis(ms));
  }

  static boolean key(MainActivity a, int code) {
    return a.dialogKey(code, new KeyEvent(KeyEvent.ACTION_DOWN, code));
  }

  @Test
  public void scanInsideDialogs() throws Exception {
    ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
    MainActivity a = c.get();
    Model.put(a.model.settings(), "method", "scan");
    Model.put(a.model.settings(), "scanType", "step");
    a.build();
    a.model.append("eu", "");
    a.model.append("quero", "");
    a.changed();
    a.tools();
    idle(400);
    assertNotNull("tools dialog is open", a.topDialog());
    assertFalse("first group is highlighted", a.dialogFocus.isEmpty());
    assertTrue("many items: scanning starts by groups", a.dialogFocus.size() > 1);
    assertTrue(key(a, KeyEvent.KEYCODE_ENTER)); // enter the first group
    assertEquals(1, a.dialogFocus.size());
    assertTrue(key(a, KeyEvent.KEYCODE_ENTER)); // first item: undo message
    idle(400);
    assertEquals("eu", a.model.text());
    assertNull("list dialog closed after choosing", a.topDialog());
    a.settings();
    idle(400);
    for (int i = 0; i < 3; i++) key(a, KeyEvent.KEYCODE_SPACE);
    assertFalse("settings dialog is scannable", a.dialogFocus.isEmpty());
    c.pause().stop().destroy();
  }

  @Test
  public void interfaceLanguage() throws Exception {
    try {
      ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
      MainActivity a = c.get();
      Model.put(a.model.root, "uiLanguage", "en");
      a.applyLanguage();
      a.build();
      assertEquals("Close", MainActivity.t("Fechar"));
      boolean speak = false;
      for (View v : a.targets) speak |= v instanceof Button && ((Button) v).getText().toString().equals("▶ Speak");
      assertTrue("buttons follow the interface language", speak);
      JSONObject english = new Model(repo.defaults("en")).profile();
      a.model.newProfile("Test", english);
      a.build();
      assertEquals("en", a.lang());
      assertEquals("en", a.voiceLocale().getLanguage());
      c.pause().stop().destroy();
    } finally {
      I18n.use("pt", null);
    }
  }

  @Test
  public void wordFormsAndRoutines() throws Exception {
    ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
    MainActivity a = c.get();
    a.model.append("eu", "");
    a.model.append("brincar", "");
    a.changed();
    a.wordForms(1);
    idle(300);
    assertNotNull("word forms dialog opens", a.topDialog());
    assertEquals("symbols/2326.png", a.findSymbol("Escovar os dentes"));
    JSONObject routine = a.model.profile().getJSONArray("routines").getJSONObject(0);
    a.routine(routine);
    idle(300);
    assertNotNull(a.topDialog());
    c.pause().stop().destroy();
  }

  @Test
  public void importSnap() throws Exception {
    byte[] sample = Files.readAllBytes(Paths.get(System.getProperty("snap.sample")));
    byte[] before = sample.clone();
    SnapImporter.Result r = SnapImporter.read(repo.context, sample);
    assertArrayEquals(before, sample);
    assertTrue(r.profile.getJSONArray("pages").length() > 1);
    int blocked = 0, words = 0;
    JSONArray pages = r.profile.getJSONArray("pages");
    for (int i = 0; i < pages.length(); i++) {
      JSONArray buttons = pages.getJSONObject(i).getJSONArray("buttons");
      for (int j = 0; j < buttons.length(); j++) {
        JSONObject b = buttons.getJSONObject(j);
        if (b.optBoolean("disabled")) blocked++;
        else if (b.optString("type").equals("word")) words++;
      }
    }
    assertTrue(blocked > 0);
    assertTrue(words > 0);
    int photos = 0, colored = 0;
    for (int i = 0; i < pages.length(); i++) {
      JSONArray buttons = pages.getJSONObject(i).getJSONArray("buttons");
      for (int j = 0; j < buttons.length(); j++) {
        JSONObject b = buttons.getJSONObject(j);
        if (b.optString("symbol").startsWith("user/")) {
          photos++;
          assertTrue(new java.io.File(repo.context.getFilesDir(), b.optString("symbol")).isFile());
        }
        if (!"blue".equals(b.optString("color")) && !"pink".equals(b.optString("color"))) colored++;
      }
    }
    assertTrue("photos from the page set are imported", photos > 0);
    assertTrue("button colors are kept", colored > 0);
    JSONArray first = null;
    for (int i = 0; i < pages.length(); i++)
      if ("Medical Advocacy".equals(pages.getJSONObject(i).optString("title")))
        first = pages.getJSONObject(i).getJSONArray("buttons");
    assertEquals("original reading order", "Can you hear me?", first.getJSONObject(0).optString("label"));
    Model m = repo.load();
    m.newProfile("Importada", r.profile);
    Model.validate(m.root);
  }
}
