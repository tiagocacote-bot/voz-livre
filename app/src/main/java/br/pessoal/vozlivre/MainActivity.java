package br.pessoal.vozlivre;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.speech.tts.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class MainActivity extends Activity {
  Model model;
  Repository repo;
  TextToSpeech tts;
  boolean voiceReady = false, edit = false, paused = false, stopped = false;
  LinearLayout root, header, messageItems;
  TextView messageText, pageTitle, status;
  ScrollView pageScroll;
  GridLayout grid;
  HorizontalScrollView messageScroll;
  String current;
  Deque<String> back = new ArrayDeque<>();
  final Handler handler = new Handler(Looper.getMainLooper());
  final ScanEngine scanner = new ScanEngine();
  final ArrayList<View> targets = new ArrayList<>();
  final ArrayList<Runnable> targetActions = new ArrayList<>();
  final Map<View, Integer> colors = new HashMap<>();
  final ExecutorService io = Executors.newSingleThreadExecutor();
  final Map<String, Bitmap> bitmapCache =
      new LinkedHashMap<String, Bitmap>(32, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, Bitmap> e) {
          return size() > 80;
        }
      };
  int dialogCount = 0;
  View focused = null, touchTarget = null;
  Runnable dwell = null;
  JSONObject photoDraft = null;
  TextView photoLabel = null;
  String search = "";
  Button moreButton;
  LinearLayout suggestionRow;
  final Button[] suggestions = new Button[4];
  int fixedTargets = 8;
  JSONObject seed = null;
  static final int EXPORT = 10, IMPORT = 11, PHOTO = 12;
  final int ink = Color.rgb(21, 48, 65), teal = Color.rgb(8, 127, 140);
  final Runnable scanTick =
      new Runnable() {
        public void run() {
          if (activeScan() && "auto".equals(model.settings().optString("scanType", "auto"))) {
            focus(nextTarget());
            handler.postDelayed(this, clamp(model.settings().optInt("scanMs", 1200), 400, 8000));
          }
        }
      };

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    repo = new Repository(this);
    try {
      model = repo.load();
    } catch (Exception e) {
      new AlertDialog.Builder(this)
          .setTitle(t("Não foi possível abrir o comunicador"))
          .setMessage(e.toString())
          .setPositiveButton(t("Fechar"), (d, w) -> finish())
          .show();
      return;
    }
    applyLanguage();
    current = model.profile().optString("home");
    if (state != null) {
      String p = state.getString("page");
      if (model.page(p) != null) current = p;
    }
    build();
    tts =
        new TextToSpeech(
            this,
            result -> {
              voiceReady = result == TextToSpeech.SUCCESS;
              if (voiceReady) configureVoice();
              runOnUiThread(
                  () -> {
                    if (status != null)
                      status.setText(
                          voiceReady ? t("Pronto para comunicar") : t("Configure uma voz em Ajustes"));
                  });
            });
    if (repo.recovered)
      show(
          new AlertDialog.Builder(this)
              .setTitle(t("Dados recuperados"))
              .setMessage(
                  t("Foi necessário recuperar seus dados. Uma cópia do arquivo anterior foi preservada no aparelho. Exporte um backup em Ajustes."))
              .setPositiveButton(t("Entendi"), null));
  }

  static String t(String pt) {
    return I18n.t(pt);
  }

  /** Interface language from the app setting (or the device), loaded from assets/i18n. */
  void applyLanguage() {
    String lang =
        I18n.resolve(model.root.optString("uiLanguage", "auto"), Locale.getDefault().getLanguage());
    JSONObject dictionary = null;
    if (!"pt".equals(lang))
      try {
        dictionary =
            new JSONObject(
                new String(
                    Repository.read(getAssets().open("i18n/" + lang + ".json"), 2000000),
                    StandardCharsets.UTF_8));
      } catch (Exception ignored) {
      }
    I18n.use(lang, dictionary);
  }

  /** Language of the active profile's board and voice: "pt", "en" or "es". */
  String lang() {
    return model.settings().optString("language", "pt");
  }

  boolean portuguese() {
    return "pt".equals(lang());
  }

  Locale voiceLocale() {
    switch (lang()) {
      case "en":
        return new Locale("en", "US");
      case "es":
        return new Locale("es", "ES");
      default:
        return new Locale("pt", "BR");
    }
  }

  void languageSettings() {
    String[] codes = {"auto", "pt", "en", "es"};
    String[] names = {
      t("Automático (idioma do aparelho)"), "Português (Brasil)", "English", "Español"
    };
    int selected = Math.max(0, Arrays.asList(codes).indexOf(model.root.optString("uiLanguage", "auto")));
    show(
        new AlertDialog.Builder(this)
            .setTitle(t("Idioma do aplicativo"))
            .setSingleChoiceItems(
                names,
                selected,
                (d, i) -> {
                  Model.put(model.root, "uiLanguage", codes[i]);
                  save();
                  applyLanguage();
                  d.dismiss();
                  build();
                })
            .setNegativeButton(t("Cancelar"), null));
  }

  int dp(float n) {
    return (int) (n * getResources().getDisplayMetrics().density + .5f);
  }

  int clamp(int n, int min, int max) {
    return Math.min(max, Math.max(min, n));
  }

  boolean contrast() {
    return model.settings().optBoolean("contrast");
  }

  int bg() {
    return contrast() ? Color.BLACK : Color.rgb(243, 247, 249);
  }

  int fg() {
    return contrast() ? Color.WHITE : ink;
  }

  GradientDrawable shape(int color) {
    GradientDrawable g = new GradientDrawable();
    g.setColor(color);
    g.setCornerRadius(dp(12));
    g.setStroke(dp(1), contrast() ? Color.LTGRAY : Color.rgb(208, 222, 229));
    return g;
  }

  LinearLayout row() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.HORIZONTAL);
    l.setGravity(Gravity.CENTER_VERTICAL);
    return l;
  }

  LinearLayout column() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  TextView text(String s, int size) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextSize(size);
    t.setTextColor(fg());
    return t;
  }

  void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_LONG).show();
  }

  void error(Exception e) {
    runOnUiThread(
        () ->
            show(
                new AlertDialog.Builder(this)
                    .setTitle(t("Não foi possível concluir"))
                    .setMessage(e.getMessage() == null ? e.toString() : e.getMessage())
                    .setPositiveButton(t("Entendi"), null)));
  }

  void save() {
    try {
      repo.save(model);
    } catch (Exception e) {
      status.setText(t("Não foi possível salvar — exporte um backup"));
      error(e);
    }
  }

  AlertDialog show(AlertDialog.Builder b) {
    cancelTouch();
    dialogCount++;
    handler.removeCallbacks(scanTick);
    AlertDialog d = b.create();
    d.setOnDismissListener(x -> closed(d));
    d.setOnKeyListener((x, code, event) -> dialogKey(code, event));
    d.show();
    openDialogs.add(d);
    restartDialogScan();
    return d;
  }

  /** Every dialog's dismiss path ends here, including dialogs with their own listener. */
  void closed(Dialog d) {
    openDialogs.remove(d);
    clearDialogFocus();
    dialogCount = Math.max(0, dialogCount - 1);
    if (topDialog() != null) restartDialogScan();
    else startScan();
  }

  // ----- Scanning inside dialogs: linear, or by groups of 8 when a dialog has many choices -----

  final ArrayList<Dialog> openDialogs = new ArrayList<>();
  final Map<View, android.graphics.drawable.Drawable> savedForegrounds = new HashMap<>();
  final ArrayList<View> dialogFocus = new ArrayList<>();
  int dialogIndex = -1, dialogGroup = -1;
  static final int GROUP = 8;
  final Runnable dialogTick =
      new Runnable() {
        public void run() {
          if (dialogScanActive() && "auto".equals(model.settings().optString("scanType", "auto"))) {
            stepDialog();
            handler.postDelayed(this, clamp(model.settings().optInt("scanMs", 1200), 400, 8000));
          }
        }
      };

  Dialog topDialog() {
    for (int i = openDialogs.size() - 1; i >= 0; i--)
      if (openDialogs.get(i).isShowing()) return openDialogs.get(i);
    return null;
  }

  boolean dialogScanActive() {
    return model != null
        && !stopped
        && !paused
        && topDialog() != null
        && "scan".equals(model.settings().optString("method"));
  }

  void restartDialogScan() {
    handler.removeCallbacks(dialogTick);
    clearDialogFocus();
    dialogIndex = -1;
    dialogGroup = -1;
    if (!dialogScanActive()) return;
    // Wait for the dialog's first layout so its targets are on screen.
    handler.postDelayed(
        () -> {
          if (!dialogScanActive()) return;
          stepDialog();
          if ("auto".equals(model.settings().optString("scanType", "auto")))
            handler.postDelayed(
                dialogTick, clamp(model.settings().optInt("scanMs", 1200), 400, 8000));
        },
        250);
  }

  List<View> dialogTargets() {
    ArrayList<View> out = new ArrayList<>();
    Dialog d = topDialog();
    if (d != null && d.getWindow() != null) collect(d.getWindow().getDecorView(), out);
    return out;
  }

  void collect(View v, List<View> out) {
    if (!v.isShown()) return;
    if (v instanceof ListView) {
      ListView list = (ListView) v;
      for (int i = 0; i < list.getChildCount(); i++)
        if (list.getChildAt(i).isShown()) out.add(list.getChildAt(i));
      return;
    }
    if (v instanceof EditText || v instanceof Drawing) return;
    if (v.isEnabled() && (v.isClickable() || v instanceof Spinner)) {
      out.add(v);
      return;
    }
    if (v instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++)
        collect(((ViewGroup) v).getChildAt(i), out);
  }

  /** Advances the highlight: groups first when there are many targets, then items. */
  void stepDialog() {
    List<View> t = dialogTargets();
    if (t.isEmpty()) return;
    boolean grouped = t.size() > GROUP + 2;
    if (grouped && dialogGroup < 0) {
      dialogIndex++;
      int groups = (t.size() + GROUP - 1) / GROUP;
      if (dialogIndex >= groups) {
        if (pageLists()) return;
        dialogIndex = 0;
      }
      int from = dialogIndex * GROUP;
      focusDialog(t.subList(from, Math.min(t.size(), from + GROUP)));
      return;
    }
    int from = grouped ? dialogGroup * GROUP : 0, to = grouped ? Math.min(t.size(), from + GROUP) : t.size();
    dialogIndex++;
    if (from + dialogIndex >= to) {
      if (grouped) {
        // Leaving the group: back to choosing groups, starting after this one.
        dialogIndex = dialogGroup;
        dialogGroup = -1;
        focusDialog(t.subList(from, to));
        return;
      }
      if (pageLists()) return;
      dialogIndex = 0;
    }
    focusDialog(Collections.singletonList(t.get(from + dialogIndex)));
  }

  /** Scrolls long lists to show their next items; returns true when it scrolled. */
  boolean pageLists() {
    Dialog d = topDialog();
    if (d == null || d.getWindow() == null) return false;
    ArrayList<ListView> lists = new ArrayList<>();
    findLists(d.getWindow().getDecorView(), lists);
    boolean moved = false;
    for (ListView l : lists)
      if (l.getLastVisiblePosition() < l.getCount() - 1) {
        l.setSelection(l.getLastVisiblePosition());
        moved = true;
      } else if (l.getFirstVisiblePosition() > 0) {
        l.setSelection(0);
        moved = true;
      }
    if (moved) {
      dialogIndex = -1;
      clearDialogFocus();
    }
    return moved;
  }

  void findLists(View v, List<ListView> out) {
    if (v instanceof ListView) out.add((ListView) v);
    else if (v instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++)
        findLists(((ViewGroup) v).getChildAt(i), out);
  }

  void focusDialog(List<View> views) {
    clearDialogFocus();
    for (View v : views) {
      savedForegrounds.put(v, v.getForeground());
      GradientDrawable ring = new GradientDrawable();
      ring.setColor(Color.TRANSPARENT);
      ring.setCornerRadius(dp(8));
      ring.setStroke(dp(5), Color.rgb(239, 111, 25));
      v.setForeground(ring);
      dialogFocus.add(v);
    }
    if (!views.isEmpty()) {
      View first = views.get(0);
      first.requestRectangleOnScreen(new Rect(0, 0, first.getWidth(), first.getHeight()), false);
    }
  }

  void clearDialogFocus() {
    for (View v : dialogFocus) v.setForeground(savedForegrounds.remove(v));
    dialogFocus.clear();
  }

  void selectDialog() {
    if (dialogFocus.isEmpty()) return;
    List<View> t = dialogTargets();
    if (dialogFocus.size() > 1) {
      // A group is highlighted: scan inside it.
      dialogGroup = Math.max(0, t.indexOf(dialogFocus.get(0)) / GROUP);
      dialogIndex = -1;
      stepDialog();
      restartTick();
      return;
    }
    View v = dialogFocus.get(0);
    clearDialogFocus();
    if (v.getParent() instanceof ListView) {
      ListView list = (ListView) v.getParent();
      int position = list.getFirstVisiblePosition() + list.indexOfChild(v);
      list.performItemClick(v, position, list.getItemIdAtPosition(position));
    } else v.performClick();
    // Dialogs that stay open (checkboxes, keyboards) keep scanning from the start.
    dialogIndex = -1;
    dialogGroup = -1;
    if (dialogScanActive())
      handler.postDelayed(
          () -> {
            if (dialogScanActive() && dialogFocus.isEmpty()) stepDialog();
          },
          300);
    restartTick();
  }

  void restartTick() {
    handler.removeCallbacks(dialogTick);
    if (dialogScanActive() && "auto".equals(model.settings().optString("scanType", "auto")))
      handler.postDelayed(dialogTick, clamp(model.settings().optInt("scanMs", 1200), 400, 8000));
  }

  boolean dialogKey(int code, KeyEvent event) {
    if (model == null || !"scan".equals(model.settings().optString("method"))) return false;
    if (code != KeyEvent.KEYCODE_SPACE
        && code != KeyEvent.KEYCODE_ENTER
        && code != KeyEvent.KEYCODE_DPAD_CENTER) return false;
    if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
      if (paused) {
        paused = false;
        restartDialogScan();
      } else if ("step".equals(model.settings().optString("scanType"))
          && code == KeyEvent.KEYCODE_SPACE) stepDialog();
      else selectDialog();
    }
    return true;
  }

  Button plain(String title, Runnable action) {
    Button b = new Button(this);
    b.setText(title);
    b.setAllCaps(false);
    b.setTextSize(15);
    b.setTextColor(fg());
    b.setMinHeight(dp(48));
    b.setPadding(dp(9), 0, dp(9), 0);
    b.setBackground(shape(contrast() ? Color.rgb(25, 25, 25) : Color.WHITE));
    b.setOnClickListener(v -> action.run());
    return b;
  }

  Button action(String title, Runnable action) {
    Button b = plain(title, action);
    register(b, action, contrast() ? Color.rgb(25, 25, 25) : Color.WHITE);
    return b;
  }

  void addRowButton(LinearLayout row, View v, float weight) {
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(50), weight);
    p.setMargins(dp(3), dp(3), dp(3), dp(3));
    row.addView(v, p);
  }

  void build() {
    handler.removeCallbacks(scanTick);
    cancelTouch();
    targets.clear();
    targetActions.clear();
    colors.clear();
    focused = null;
    root = column();
    root.setBackgroundColor(bg());
    root.setPadding(dp(10), dp(6), dp(10), dp(8));
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          root.setPadding(
              dp(10) + insets.getSystemWindowInsetLeft(),
              dp(6) + insets.getSystemWindowInsetTop(),
              dp(10) + insets.getSystemWindowInsetRight(),
              dp(8) + insets.getSystemWindowInsetBottom());
          return insets;
        });
    setContentView(root);
    root.requestApplyInsets();
    header = row();
    TextView brand = text(t("Voz Livre"), 23);
    brand.setTypeface(null, Typeface.BOLD);
    header.addView(brand, new LinearLayout.LayoutParams(0, dp(50), 1));
    Button profiles = action(t("Perfil ▾"), () -> profiles());
    header.addView(profiles);
    Button settings = action(t("Ajustes"), () -> settings());
    header.addView(settings);
    root.addView(header);
    LinearLayout titleRow = row();
    pageTitle = text("", 18);
    pageTitle.setTypeface(null, Typeface.BOLD);
    titleRow.addView(pageTitle, new LinearLayout.LayoutParams(0, dp(40), 1));
    Button ed = action(edit ? t("Concluir edição") : t("Editar"), () -> toggleEdit());
    titleRow.addView(ed);
    boolean compact = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    if (compact) {
      header.removeView(brand);
      titleRow.removeView(pageTitle);
      titleRow.removeView(ed);
      header.addView(pageTitle, 0, new LinearLayout.LayoutParams(0, dp(50), 1));
      header.addView(ed);
    } else root.addView(titleRow);
    messageScroll = new HorizontalScrollView(this);
    messageScroll.setBackground(shape(contrast() ? Color.rgb(20, 20, 20) : Color.WHITE));
    messageItems = row();
    messageItems.setPadding(dp(8), dp(6), dp(8), dp(6));
    messageScroll.addView(messageItems);
    root.addView(messageScroll, new LinearLayout.LayoutParams(-1, dp(compact ? 72 : 92)));
    LinearLayout speech = row();
    Button speak =
        action(
            t("▶ Falar"),
            () -> {
              learn(model.text());
              speak(model.text(), false);
            });
    speak.setTextColor(Color.WHITE);
    speak.setBackground(shape(teal));
    colors.put(speak, teal);
    addRowButton(speech, speak, 1.5f);
    addRowButton(
        speech,
        action(
            t("Parar"),
            () -> {
              if (tts != null) tts.stop();
            }),
        1);
    addRowButton(
        speech,
        action(
            t("Apagar"),
            () -> {
              model.remove();
              changed();
            }),
        1);
    addRowButton(
        speech,
        action(
            t("Limpar"),
            () -> {
              model.clear();
              changed();
            }),
        1);
    root.addView(speech);
    LinearLayout nav = row();
    // Landscape has little height: suggestions share the navigation row.
    suggestionRow = compact ? null : row();
    LinearLayout hints = compact ? nav : suggestionRow;
    for (int i = 0; i < suggestions.length; i++) {
      Button[] self = new Button[1];
      self[0] = action("", () -> pick(String.valueOf(self[0].getTag())));
      int tint = contrast() ? Color.rgb(10, 45, 50) : Color.rgb(222, 242, 243);
      self[0].setBackground(shape(tint));
      colors.put(self[0], tint);
      self[0].setContentDescription(t("Sugestão"));
      suggestions[i] = self[0];
      addRowButton(hints, self[0], 1);
    }
    if (suggestionRow != null) root.addView(suggestionRow);
    addRowButton(nav, action(t("⌂ Início"), () -> home()), 1);
    addRowButton(nav, action(t("‹ Voltar"), () -> goBack()), 1);
    addRowButton(nav, action(t("Teclado"), () -> keyboard()), 1);
    moreButton = action(t("Mais ▾"), () -> tools());
    addRowButton(nav, moreButton, 1);
    root.addView(nav);
    fixedTargets = targets.size();
    pageScroll = new ScrollView(this);
    pageScroll.setFillViewport(false);
    grid = new GridLayout(this);
    grid.setPadding(0, dp(4), 0, dp(8));
    pageScroll.addView(grid);
    root.addView(pageScroll, new LinearLayout.LayoutParams(-1, 0, 1));
    status =
        text(
            edit
                ? t("Toque para editar; mantenha pressionado para mover ou copiar")
                : t("Salvo neste aparelho"),
            12);
    status.setPadding(dp(6), dp(4), 0, 0);
    root.addView(status);
    renderMessage();
    renderPage();
  }

  void changed() {
    renderMessage();
    save();
  }

  void renderMessage() {
    messageItems.removeAllViews();
    JSONArray a = model.message();
    if (a.length() == 0) {
      TextView hint = text(t("Escolha palavras para montar sua mensagem"), 17);
      hint.setPadding(dp(8), 0, dp(8), 0);
      messageItems.addView(hint);
    } else
      for (int i = 0; i < a.length(); i++) {
        JSONObject token = a.optJSONObject(i);
        LinearLayout tile = column();
        tile.setGravity(Gravity.CENTER);
        String symbol = token.optString("symbol");
        if (!symbol.isEmpty()) {
          ImageView image = image(symbol, 36);
          if (image != null) tile.addView(image);
        }
        TextView t = text(token.optString("text"), 18);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(7), 0, dp(7), 0);
        tile.addView(t);
        tile.setContentDescription(token.optString("text") + t(", mudar forma"));
        final int index = i;
        tile.setOnClickListener(v -> wordForms(index));
        messageItems.addView(tile);
      }
    messageScroll.post(() -> messageScroll.fullScroll(View.FOCUS_RIGHT));
    renderSuggestions();
  }

  boolean predict() {
    return model.settings().optBoolean("predict", true);
  }

  String seedLanguage = null;

  JSONObject seed() {
    if (seed == null || !lang().equals(seedLanguage))
      try {
        seedLanguage = lang();
        seed =
            new JSONObject(
                new String(
                    Repository.read(
                        getAssets().open(portuguese() ? "prediction.json" : "prediction-" + lang() + ".json"),
                        1000000),
                    StandardCharsets.UTF_8));
      } catch (Exception e) {
        seed = new JSONObject();
      }
    return seed;
  }

  /** Word buttons of the active profile, keyed by folded message. */
  Map<String, JSONObject> vocabulary() {
    Map<String, JSONObject> words = new LinkedHashMap<>();
    JSONArray pages = model.pages();
    for (int i = 0; i < pages.length(); i++) {
      JSONArray a = pages.optJSONObject(i).optJSONArray("buttons");
      for (int j = 0; j < a.length(); j++) {
        JSONObject b = a.optJSONObject(j);
        String m = b.optString("message", b.optString("label")).trim();
        if ("word".equals(b.optString("type", "word")) && !b.optBoolean("hidden") && !m.isEmpty())
          words.putIfAbsent(fold(m), b);
      }
    }
    return words;
  }

  List<String> predictions(String previous, String prefix) {
    ArrayList<String> words = new ArrayList<>();
    for (JSONObject b : vocabulary().values()) words.add(b.optString("message", b.optString("label")));
    List<String> raw = Predictor.suggest(model.profile(), previous, prefix, words, seed(), suggestions.length);
    if (!portuguese() || Grammar.person(previous) < 0) return raw;
    // After a subject, offer verbs already conjugated: "eu" → "brinco", not "brincar".
    LinkedHashSet<String> agreed = new LinkedHashSet<>();
    for (String w : raw) agreed.add(Grammar.agree(previous, w, verbs()));
    return new ArrayList<>(agreed);
  }

  /** Infinitives on the person's pages, used to recognize conjugated forms. */
  List<String> verbs() {
    ArrayList<String> out = new ArrayList<>();
    for (JSONObject b : vocabulary().values()) {
      String m = b.optString("message", b.optString("label")).trim();
      if (Grammar.infinitive(m)) out.add(m);
    }
    return out;
  }

  /** Word in the message as the person chose it, conjugated when automatic grammar is on. */
  String agreed(String text) {
    if (!portuguese() || !model.settings().optBoolean("grammarAuto")) return text;
    return Grammar.agree(Predictor.last(model.text()), text, verbs());
  }

  void wordForms(int index) {
    JSONArray message = model.message();
    if (index < 0 || index >= message.length()) return;
    if (!portuguese()) {
      toast(t("As formas das palavras estão disponíveis para pranchas em português."));
      return;
    }
    String text = message.optJSONObject(index).optString("text");
    List<String> words = Predictor.words(text);
    if (words.isEmpty()) return;
    String word = words.get(words.size() - 1);
    String before =
        words.size() > 1
            ? words.get(words.size() - 2)
            : index > 0 ? Predictor.last(message.optJSONObject(index - 1).optString("text")) : "";
    List<Grammar.Form> forms = Grammar.forms(word, before, verbs());
    if (forms.isEmpty()) {
      toast(t("Esta palavra não tem outras formas."));
      return;
    }
    String[] items = new String[forms.size()];
    for (int i = 0; i < items.length; i++)
    {
      StringBuilder label = new StringBuilder();
      for (String part : forms.get(i).description.split(" · "))
        label.append(label.length() == 0 ? "" : " · ").append(t(part));
      items[i] = forms.get(i).text + "  ·  " + label;
    }
    int cut = text.toLowerCase(new Locale("pt", "BR")).lastIndexOf(word);
    show(
        new AlertDialog.Builder(this)
            .setTitle(t("Formas de “") + word + "”")
            .setItems(
                items,
                (d, i) -> {
                  String chosen = forms.get(i).text;
                  String replaced =
                      cut >= 0 ? text.substring(0, cut) + chosen + text.substring(cut + word.length()) : chosen;
                  model.replace(index, replaced);
                  changed();
                  if (model.settings().optBoolean("speakWords")) speak(chosen, false);
                })
            .setNegativeButton(t("Fechar"), null));
  }

  void renderSuggestions() {
    if (suggestions[0] == null) return;
    boolean on = predict();
    if (suggestionRow != null) suggestionRow.setVisibility(on ? View.VISIBLE : View.GONE);
    if (!on) {
      for (Button b : suggestions) b.setVisibility(View.GONE);
      return;
    }
    List<String> next = predictions(Predictor.last(model.text()), "");
    for (int i = 0; i < suggestions.length; i++) {
      String word = i < next.size() ? next.get(i) : "";
      suggestions[i].setText(word);
      suggestions[i].setTag(word);
      suggestions[i].setContentDescription(t("Sugestão: ") + word);
      suggestions[i].setVisibility(word.isEmpty() ? View.INVISIBLE : View.VISIBLE);
    }
  }

  void pick(String word) {
    if (word == null || word.trim().isEmpty()) return;
    JSONObject b = vocabulary().get(fold(word.trim()));
    try {
      if (b != null) model.append(agreed(b.optString("message", b.optString("label"))), b.optString("symbol"));
      else model.append(agreed(word), "");
      changed();
      if (model.settings().optBoolean("speakWords")) speak(word, false);
    } catch (Exception e) {
      error(e);
    }
  }

  void learn(String text) {
    if (predict() && !text.trim().isEmpty()) {
      Predictor.learn(model.profile(), text);
      save();
    }
  }

  int categoryColor(String c) {
    if (contrast()) return Color.rgb(20, 20, 20);
    switch (c) {
      case "yellow":
        return Color.rgb(255, 241, 190);
      case "green":
        return Color.rgb(214, 239, 219);
      case "orange":
        return Color.rgb(255, 227, 196);
      case "red":
        return Color.rgb(255, 216, 218);
      case "pink":
        return Color.rgb(244, 220, 238);
      case "purple":
        return Color.rgb(230, 224, 248);
      default:
        return Color.rgb(221, 238, 250);
    }
  }

  ImageView image(String name, int size) {
    if (!Repository.safe(name)) return null;
    try {
      Bitmap bitmap = bitmapCache.get(name);
      if (bitmap == null) {
        InputStream in =
            name.startsWith("user/")
                ? new FileInputStream(new File(getFilesDir(), name))
                : getAssets().open(name);
        try (InputStream data = in) {
          BitmapFactory.Options options = new BitmapFactory.Options();
          options.inSampleSize = name.startsWith("symbols/") ? 4 : 2;
          bitmap = BitmapFactory.decodeStream(data, null, options);
        }
        if (bitmap == null) return null;
        bitmapCache.put(name, bitmap);
      }
      ImageView v = new ImageView(this);
      v.setImageBitmap(bitmap);
      v.setAdjustViewBounds(true);
      v.setScaleType(ImageView.ScaleType.FIT_CENTER);
      v.setLayoutParams(new LinearLayout.LayoutParams(dp(size), dp(size)));
      v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
      return v;
    } catch (Exception e) {
      return null;
    }
  }

  void renderPage() {
    int cardBase = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 64 : 90;
    float cardScale = cardBase == 64 ? 3f : 3.5f;
    cancelTouch();
    while (targets.size() > fixedTargets) {
      int i = targets.size() - 1;
      colors.remove(targets.remove(i));
      targetActions.remove(i);
    }
    grid.removeAllViews();
    JSONObject page = model.page(current);
    if (page == null) {
      current = model.profile().optString("home");
      page = model.page(current);
    }
    pageTitle.setText(page.optString("title") + (edit ? t(" · edição") : ""));
    // Core pages keep their own grid so every word stays in the same place (motor planning).
    int cols =
        clamp(
            page.optBoolean("fixedLayout")
                ? page.optInt("columns", 6)
                : model.settings().optInt("columns", 3),
            2,
            8);
    grid.setColumnCount(cols);
    JSONArray buttons = page.optJSONArray("buttons");
    for (int i = 0; i < buttons.length(); i++) {
      final JSONObject b = buttons.optJSONObject(i);
      if (b.optBoolean("hidden") && !edit) {
        View space = new View(this);
        space.setVisibility(View.INVISIBLE);
        space.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        GridLayout.LayoutParams pos = new GridLayout.LayoutParams();
        pos.width = 0;
        pos.height = dp(cardBase + clamp(model.settings().optInt("font", 19), 15, 30) * cardScale);
        pos.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        pos.setMargins(dp(3), dp(3), dp(3), dp(3));
        grid.addView(space, pos);
        continue;
      }
      if (!search.isEmpty()
          && !fold(b.optString("label") + " " + b.optString("message")).contains(fold(search)))
        continue;
      LinearLayout card = column();
      card.setGravity(Gravity.CENTER);
      card.setPadding(dp(4), dp(8), dp(4), dp(6));
      int color = categoryColor(b.optString("color"));
      card.setBackground(shape(color));
      card.setAlpha(b.optBoolean("hidden") ? .4f : 1f);
      ImageView img = image(b.optString("symbol"), 60);
      if (img != null) card.addView(img);
      else {
        TextView icon =
            text(b.optString("emoji", b.optString("type").equals("link") ? "↗" : "●"), 29);
        icon.setGravity(Gravity.CENTER);
        card.addView(icon, new LinearLayout.LayoutParams(-1, dp(55)));
      }
      String label =
          (b.optBoolean("disabled") ? "⚠ " : "")
              + b.optString("label")
              + ("link".equals(b.optString("type")) || "wordlink".equals(b.optString("type")) ? " ›" : "");
      TextView t = text(label, clamp(model.settings().optInt("font", 19), 15, 30));
      t.setGravity(Gravity.CENTER);
      t.setMaxLines(3);
      t.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
      card.addView(t, new LinearLayout.LayoutParams(-1, -2));
      card.setContentDescription(
          b.optString("label") + (b.optString("type").equals("link") ? t(", abrir página") : ""));
      card.setFocusable(true);
      card.setClickable(true);
      Runnable click =
          () -> {
            if (edit) editButton(b, false);
            else activate(b);
          };
      card.setOnClickListener(v -> click.run());
      card.setOnLongClickListener(
          v -> {
            if (edit) {
              organize(b);
              return true;
            }
            return false;
          });
      register(card, click, color);
      GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
      lp.width = 0;
      lp.height = dp(cardBase + clamp(model.settings().optInt("font", 19), 15, 30) * cardScale);
      lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
      lp.setMargins(dp(3), dp(3), dp(3), dp(3));
      grid.addView(card, lp);
    }
    if (edit) {
      Button add = action(t("+ Novo botão"), () -> editButton(model.newButton(), true));
      GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
      lp.width = 0;
      lp.height = dp(125);
      lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
      lp.setMargins(dp(3), dp(3), dp(3), dp(3));
      grid.addView(add, lp);
    }
    if (!edit && grid.getChildCount() == 0) {
      TextView empty = text(t("Nenhuma palavra aqui. Use Editar para adicionar."), 18);
      grid.addView(empty);
    }
    scanner.reset(targets.size());
    startScan();
  }

  void activate(JSONObject b) {
    if (b.optBoolean("disabled")) {
      toast(t("Ação importada não reconhecida. Revise este botão em Editar antes de ativar."));
      return;
    }
    try {
      switch (b.optString("type", "word")) {
        case "link":
          navigate(b.optString("target"));
          break;
        case "speak":
          speak(b.optString("message", b.optString("label")), false);
          break;
        case "wordlink":
          // Sentence starter: add its words, then open the page with what usually follows.
          model.append(b.optString("message", b.optString("label")), b.optString("symbol"));
          changed();
          if (model.settings().optBoolean("speakWords")) speak(b.optString("message"), false);
          if (model.page(b.optString("target")) != null) navigate(b.optString("target"));
          break;
        default:
          model.append(agreed(b.optString("message", b.optString("label"))), b.optString("symbol"));
          changed();
          if (model.settings().optBoolean("speakWords")) speak(b.optString("message"), false);
      }
    } catch (Exception e) {
      error(e);
    }
  }

  void navigate(String page) {
    if (model.page(page) == null) {
      toast(t("Esta página não foi encontrada. Você pode corrigir o destino em Editar."));
      return;
    }
    back.push(current);
    current = page;
    search = "";
    pageScroll.scrollTo(0, 0);
    renderPage();
  }

  void home() {
    current = model.profile().optString("home");
    back.clear();
    search = "";
    pageScroll.scrollTo(0, 0);
    renderPage();
  }

  void goBack() {
    if (!back.isEmpty() && model.page(back.peek()) != null) {
      current = back.pop();
      search = "";
      renderPage();
    } else home();
  }

  @Override
  public void onBackPressed() {
    if (edit) {
      edit = false;
      build();
    } else if (!back.isEmpty()) goBack();
    else super.onBackPressed();
  }

  void toggleEdit() {
    String pin = model.settings().optString("pin");
    if (!edit && !pin.isEmpty()) {
      EditText input =
          field(
              t("Código de edição"),
              "",
              android.text.InputType.TYPE_CLASS_NUMBER
                  | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
      show(
          new AlertDialog.Builder(this)
              .setTitle(t("Entrar na edição"))
              .setView(input)
              .setPositiveButton(
                  t("Entrar"),
                  (d, w) -> {
                    if (pin.equals(input.getText().toString())) {
                      edit = true;
                      build();
                    } else toast(t("Código incorreto."));
                  })
              .setNegativeButton(t("Cancelar"), null));
    } else {
      edit = !edit;
      build();
    }
  }

  static String fold(String s) {
    return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }

  void register(View v, Runnable r, int color) {
    targets.add(v);
    targetActions.add(r);
    colors.put(v, color);
    v.setOnTouchListener((view, event) -> touch(view, event));
    v.setOnHoverListener(
        (view, event) -> {
          if (!edit && "mouse".equals(model.settings().optString("method"))) {
            if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
              cancelTouch();
              highlight(view);
              dwell =
                  () -> {
                    if ((!paused || view == moreButton) && dialogCount == 0) {
                      r.run();
                    }
                  };
              handler.postDelayed(dwell, clamp(model.settings().optInt("holdMs", 650), 200, 5000));
            } else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) cancelTouch();
          }
          return false;
        });
  }

  View at(float x, float y) {
    for (View v : targets) {
      int[] pos = new int[2];
      v.getLocationOnScreen(pos);
      if (x >= pos[0] && x < pos[0] + v.getWidth() && y >= pos[1] && y < pos[1] + v.getHeight())
        return v;
    }
    return null;
  }

  boolean touch(View v, MotionEvent e) {
    if (edit || dialogCount > 0) return false;
    String method = model.settings().optString("method", "touch");
    if (paused)
      return v != moreButton;
    if (method.equals("touch") || method.equals("mouse") || method.equals("scan")) return false;
    if (e.getAction() == MotionEvent.ACTION_DOWN) {
      cancelTouch();
      touchTarget = v;
      highlight(v);
      if (method.equals("hold")) {
        dwell =
            () -> {
              if (touchTarget == v) {
                touchTarget = null;
                runTarget(v);
              }
            };
        handler.postDelayed(dwell, clamp(model.settings().optInt("holdMs", 650), 200, 5000));
      } else if (method.equals("auditory")) {
        speak(
            v.getContentDescription() != null
                ? v.getContentDescription().toString()
                : v instanceof Button ? ((Button) v).getText().toString() : "",
            true);
      }
    } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
      View hit = at(e.getRawX(), e.getRawY());
      if (!method.equals("hold") && hit != touchTarget) {
        touchTarget = hit;
        if (hit != null) {
          highlight(hit);
          if (method.equals("auditory"))
            speak(
                hit.getContentDescription() != null
                    ? hit.getContentDescription().toString()
                    : hit instanceof Button ? ((Button) hit).getText().toString() : "",
                true);
        }
      } else if (method.equals("hold") && hit != touchTarget) cancelTouch();
    } else if (e.getAction() == MotionEvent.ACTION_UP) {
      View selected = touchTarget;
      cancelTouch();
      if ((method.equals("release") || method.equals("auditory")) && selected != null)
        runTarget(selected);
    } else if (e.getAction() == MotionEvent.ACTION_CANCEL) cancelTouch();
    return true;
  }

  void runTarget(View v) {
    int i = targets.indexOf(v);
    if (i >= 0) targetActions.get(i).run();
  }

  void cancelTouch() {
    if (dwell != null) handler.removeCallbacks(dwell);
    dwell = null;
    touchTarget = null;
  }

  void highlight(View v) {
    if (focused != null && colors.containsKey(focused))
      focused.setBackground(shape(colors.get(focused)));
    focused = v;
    if (v != null) {
      GradientDrawable g = shape(colors.getOrDefault(v, Color.WHITE));
      g.setStroke(dp(5), Color.rgb(239, 111, 25));
      v.setBackground(g);
      v.requestRectangleOnScreen(new Rect(0, 0, v.getWidth(), v.getHeight()), false);
    }
  }

  boolean activeScan() {
    return model != null
        && !stopped
        && !paused
        && dialogCount == 0
        && "scan".equals(model.settings().optString("method"));
  }

  /** Next scan target that is on screen; skips empty suggestions and hidden rows. */
  int nextTarget() {
    for (int n = 0; n < scanner.size(); n++) {
      int i = scanner.next();
      if (i >= 0 && i < targets.size() && targets.get(i).isShown()) return i;
    }
    return -1;
  }

  void focus(int i) {
    if (i >= 0 && i < targets.size()) highlight(targets.get(i));
  }

  void startScan() {
    handler.removeCallbacks(scanTick);
    if (activeScan()) {
      if (scanner.selected() < 0) focus(nextTarget());
      if ("auto".equals(model.settings().optString("scanType", "auto")))
        handler.postDelayed(scanTick, clamp(model.settings().optInt("scanMs", 1200), 400, 8000));
    }
  }

  void selectScan() {
    if (!activeScan()) return;
    int i = scanner.selected();
    if (i >= 0 && i < targetActions.size()) {
      handler.removeCallbacks(scanTick);
      targetActions.get(i).run();
      startScan();
    }
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    if (paused && dialogCount == 0 && "scan".equals(model.settings().optString("method"))
        && (event.getKeyCode() == KeyEvent.KEYCODE_SPACE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER
            || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER)) {
      if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
        paused = false;
        status.setText(t("Acesso retomado"));
        startScan();
      }
      return true;
    }
    if (activeScan()
        && (event.getKeyCode() == KeyEvent.KEYCODE_SPACE
            || event.getKeyCode() == KeyEvent.KEYCODE_ENTER
            || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER)) {
      if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
        if ("step".equals(model.settings().optString("scanType"))
            && event.getKeyCode() == KeyEvent.KEYCODE_SPACE) focus(nextTarget());
        else selectScan();
      }
      return true;
    }
    return super.dispatchKeyEvent(event);
  }

  void configureVoice() {
    if (!voiceReady || tts == null) return;
    try {
      tts.setLanguage(voiceLocale());
      Set<Voice> voices = tts.getVoices();
      String chosen = model.settings().optString("voice");
      Voice best = null;
      if (voices != null)
        for (Voice v : voices) {
          if (v.getName().equals(chosen)) best = v;
        }
      if (best == null && voices != null)
        for (Voice v : voices)
          if (v.getLocale().getLanguage().equals(voiceLocale().getLanguage())
              && (!portuguese() || v.getLocale().getCountry().equals("BR"))
              && !v.isNetworkConnectionRequired()) {
            best = v;
            break;
          }
      if (best != null) tts.setVoice(best);
      tts.setSpeechRate((float) Math.max(.3, Math.min(2, model.settings().optDouble("rate", 1))));
      tts.setPitch((float) Math.max(.5, Math.min(2, model.settings().optDouble("pitch", 1))));
    } catch (Exception e) {
      runOnUiThread(() -> toast(t("Não foi possível configurar esta voz.")));
    }
  }

  void speak(String s, boolean cue) {
    if (s.trim().isEmpty()) return;
    if (!voiceReady) {
      toast(t("A voz ainda não está pronta. Verifique Ajustes → Voz."));
      return;
    }
    configureVoice();
    String text = s.trim();
    int limit = TextToSpeech.getMaxSpeechInputLength();
    String first = text.substring(0, Math.min(text.length(), limit));
    int result = tts.speak(first, TextToSpeech.QUEUE_FLUSH, null, Model.id());
    for (int offset = first.length(); offset < text.length(); offset += limit)
      tts.speak(
          text.substring(offset, Math.min(text.length(), offset + limit)),
          TextToSpeech.QUEUE_ADD,
          null,
          Model.id());
    if (result == TextToSpeech.ERROR)
      toast(t("A fala falhou. Verifique a voz nas configurações do Android."));
  }

  EditText field(String label, String value, int type) {
    EditText e = new EditText(this);
    e.setHint(label);
    e.setContentDescription(label);
    e.setText(value);
    e.setInputType(type);
    e.setSelectAllOnFocus(false);
    return e;
  }

  void styleForm(View v) {
    if (v instanceof TextView) ((TextView) v).setTextColor(ink);
    if (v instanceof Button) v.setBackground(shape(Color.WHITE));
    if (v instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++)
        styleForm(((ViewGroup) v).getChildAt(i));
  }

  void label(LinearLayout form, String s) {
    TextView t = text(s, 15);
    t.setPadding(dp(4), dp(12), 0, dp(2));
    form.addView(t);
  }

  ScrollView scroll(LinearLayout form) {
    ScrollView s = new ScrollView(this);
    form.setPadding(dp(16), dp(4), dp(16), dp(16));
    form.setBackgroundColor(Color.WHITE);
    styleForm(form);
    s.addView(form);
    return s;
  }

  Spinner spinner(String[] values, int selected) {
    Spinner s = new ScanSpinner(this, values);
    s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
    s.setSelection(Math.max(0, selected));
    return s;
  }

  /** A spinner whose choices open as a regular dialog while scanning, so switches can reach them. */
  class ScanSpinner extends Spinner {
    final String[] values;

    ScanSpinner(Context c, String[] values) {
      super(c, Spinner.MODE_DROPDOWN);
      this.values = values;
    }

    @Override
    public boolean performClick() {
      if (model == null || !"scan".equals(model.settings().optString("method")))
        return super.performClick();
      show(
          new AlertDialog.Builder(MainActivity.this)
              .setTitle(t("Escolha"))
              .setSingleChoiceItems(
                  values,
                  getSelectedItemPosition(),
                  (d, which) -> {
                    setSelection(which);
                    d.dismiss();
                  })
              .setNegativeButton(t("Cancelar"), null));
      return true;
    }
  }

  void editButton(JSONObject original, boolean fresh) {
    if (!edit && !model.settings().optString("pin").isEmpty()) {
      unlock(
          () -> {
            edit = true;
            build();
            editButton(original, fresh);
          });
      return;
    }
    JSONObject draft = Model.copy(original);
    LinearLayout form = column();
    label(form, t("Texto do botão"));
    EditText name = field(t("Rótulo"), draft.optString("label"), 1);
    form.addView(name);
    label(form, t("O que será falado"));
    EditText message =
        field(
            t("Mensagem"),
            draft.optString("message"),
            android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    form.addView(message);
    String[] types = {
      t("Adicionar à mensagem"), t("Falar diretamente"), t("Abrir página"), t("Adicionar e abrir página")
    };
    String type = draft.optString("type");
    label(form, t("Ao tocar"));
    Spinner kind =
        spinner(
            types,
            type.equals("speak") ? 1 : type.equals("link") ? 2 : type.equals("wordlink") ? 3 : 0);
    form.addView(kind);
    String[] names = new String[model.pages().length()];
    int targetIndex = 0;
    for (int i = 0; i < names.length; i++) {
      JSONObject p = model.pages().optJSONObject(i);
      names[i] = p.optString("title");
      if (p.optString("id").equals(draft.optString("target"))) targetIndex = i;
    }
    label(form, t("Página de destino (para abrir página)"));
    Spinner dest = spinner(names, targetIndex);
    form.addView(dest);
    String[] palettes = {t("Azul"), t("Amarelo"), t("Verde"), t("Laranja"), t("Rosa"), t("Roxo"), t("Vermelho")},
        codes = {"blue", "yellow", "green", "orange", "pink", "purple", "red"};
    label(form, t("Cor"));
    Spinner palette = spinner(palettes, Arrays.asList(codes).indexOf(draft.optString("color")));
    form.addView(palette);
    CheckBox hidden = new CheckBox(this);
    hidden.setText(t("Ocultar no modo de comunicação"));
    hidden.setChecked(draft.optBoolean("hidden"));
    form.addView(hidden);
    photoLabel =
        text(draft.optString("symbol").isEmpty() ? t("Sem imagem") : t("Imagem selecionada"), 14);
    form.addView(photoLabel);
    LinearLayout imageRow = row();
    addRowButton(imageRow, plain(t("Escolher símbolo"), () -> symbols(draft)), 1);
    addRowButton(
        imageRow,
        plain(
            t("Usar foto"),
            () -> {
              photoDraft = draft;
              Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
              intent.setType("image/*");
              intent.addCategory(Intent.CATEGORY_OPENABLE);
              startActivityForResult(intent, PHOTO);
            }),
        1);
    addRowButton(
        imageRow,
        plain(
            t("Sem imagem"),
            () -> {
              Model.put(draft, "symbol", "");
              photoLabel.setText(t("Sem imagem"));
            }),
        1);
    form.addView(imageRow);
    AlertDialog dialog =
        show(
            new AlertDialog.Builder(this)
                .setTitle(fresh ? t("Novo botão") : t("Editar botão"))
                .setView(scroll(form))
                .setPositiveButton(t("Salvar"), null)
                .setNegativeButton(t("Cancelar"), null)
                .setNeutralButton(
                    fresh ? "" : t("Excluir"),
                    fresh
                        ? null
                        : (d, w) -> {
                          model.beforeEdit();
                          JSONArray a = model.page(current).optJSONArray("buttons");
                          for (int i = 0; i < a.length(); i++)
                            if (a.optJSONObject(i)
                                .optString("id")
                                .equals(original.optString("id"))) {
                              a.remove(i);
                              break;
                            }
                          save();
                          renderPage();
                        }));
    dialog
        .getButton(AlertDialog.BUTTON_POSITIVE)
        .setOnClickListener(
            v -> {
              if (name.getText().toString().trim().isEmpty()) {
                name.setError(t("Digite um rótulo"));
                return;
              }
              model.beforeEdit();
              Model.put(draft, "label", name.getText().toString().trim());
              Model.put(draft, "message", message.getText().toString());
              Model.put(
                  draft,
                  "type",
                  new String[] {"word", "speak", "link", "wordlink"}[kind.getSelectedItemPosition()]);
              Model.put(
                  draft,
                  "target",
                  model.pages().optJSONObject(dest.getSelectedItemPosition()).optString("id"));
              Model.put(draft, "color", codes[palette.getSelectedItemPosition()]);
              Model.put(draft, "hidden", hidden.isChecked());
              Model.put(draft, "disabled", false);
              JSONArray a = model.page(current).optJSONArray("buttons");
              if (fresh) {
                if (a.length() >= 600) {
                  toast(t("Esta página está cheia."));
                  return;
                }
                a.put(draft);
              } else
                for (int i = 0; i < a.length(); i++)
                  if (a.optJSONObject(i).optString("id").equals(original.optString("id"))) {
                    try {
                      a.put(i, draft);
                    } catch (JSONException e) {
                      error(e);
                    }
                    break;
                  }
              save();
              dialog.dismiss();
              renderPage();
            });
  }

  void organize(JSONObject button) {
    show(
        new AlertDialog.Builder(this)
            .setTitle(button.optString("label"))
            .setItems(
                new String[] {t("Mover posição nesta página"), t("Copiar para outra página")},
                (d, option) -> {
                  JSONArray buttons = model.page(current).optJSONArray("buttons");
                  if (option == 0) {
                    String[] positions = new String[buttons.length()];
                    for (int i = 0; i < positions.length; i++)
                      positions[i] = (i + 1) + " · " + buttons.optJSONObject(i).optString("label");
                    show(
                        new AlertDialog.Builder(this)
                            .setTitle(t("Trocar com qual posição?"))
                            .setItems(
                                positions,
                                (x, pos) -> {
                                  model.beforeEdit();
                                  int old = -1;
                                  for (int i = 0; i < buttons.length(); i++)
                                    if (buttons.optJSONObject(i) == button) old = i;
                                  if (old >= 0)
                                    try {
                                      JSONObject other = buttons.getJSONObject(pos);
                                      buttons.put(pos, button);
                                      buttons.put(old, other);
                                      save();
                                      renderPage();
                                    } catch (Exception e) {
                                      error(e);
                                    }
                                })
                            .setNegativeButton(t("Cancelar"), null));
                  } else {
                    String[] names = new String[model.pages().length()];
                    for (int i = 0; i < names.length; i++)
                      names[i] = model.pages().optJSONObject(i).optString("title");
                    show(
                        new AlertDialog.Builder(this)
                            .setTitle(t("Copiar para"))
                            .setItems(
                                names,
                                (x, pos) -> {
                                  JSONArray a =
                                      model.pages().optJSONObject(pos).optJSONArray("buttons");
                                  if (a.length() >= 600) {
                                    toast(t("Esta página está cheia."));
                                    return;
                                  }
                                  model.beforeEdit();
                                  JSONObject copy = Model.copy(button);
                                  Model.put(copy, "id", Model.id());
                                  a.put(copy);
                                  save();
                                  renderPage();
                                })
                            .setNegativeButton(t("Cancelar"), null));
                  }
                })
            .setNegativeButton(t("Fechar"), null));
  }

  void symbols(JSONObject draft) {
    symbols(draft, null);
  }

  JSONArray symbolRecords = null;

  JSONArray symbolRecords() throws Exception {
    if (symbolRecords == null)
      symbolRecords =
          new JSONObject(
                  new String(
                      Repository.read(getAssets().open("symbols-provenance.json"), 2000000),
                      StandardCharsets.UTF_8))
              .getJSONArray("records");
    return symbolRecords;
  }

  /** Best bundled picture for a text: whole phrase first, then its longest word; ARASAAC first. */
  String findSymbol(String text) {
    try {
      JSONArray all = symbolRecords();
      String phrase = fold(text.trim().replaceAll("[.!?]+$", ""));
      ArrayList<String> words = new ArrayList<>();
      for (String w : phrase.split("[^\\p{L}]+")) if (w.length() > 2) words.add(w);
      words.sort((x, y) -> y.length() - x.length());
      ArrayList<String> wanted = new ArrayList<>();
      wanted.add(phrase);
      wanted.addAll(words);
      for (String w : wanted)
        for (int pass = 0; pass < 2; pass++)
          for (int i = 0; i < all.length(); i++) {
            JSONObject r = all.optJSONObject(i);
            if ((pass == 0) != "ARASAAC".equals(r.optString("provider"))) continue;
            if (fold(r.optString("search")).equals(w)) return r.optString("file");
            JSONArray k = r.optJSONArray("keywords");
            if (k != null)
              for (int j = 0; j < k.length(); j++)
                if (fold(k.optString(j)).equals(w)) return r.optString("file");
          }
    } catch (Exception ignored) {
    }
    return "";
  }

  void symbols(JSONObject draft, Runnable after) {
    try {
      JSONArray all = symbolRecords();
      LinearLayout form = column();
      EditText query = field(t("Buscar símbolo"), "", 1);
      form.addView(query);
      GridLayout choices = new GridLayout(this);
      choices.setColumnCount(3);
      form.addView(choices);
      AlertDialog d =
          show(
              new AlertDialog.Builder(this)
                  .setTitle(t("Símbolos disponíveis"))
                  .setView(scroll(form))
                  .setNegativeButton(t("Fechar"), null));
      Runnable update =
          () -> {
            choices.removeAllViews();
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < all.length(); i++) {
              JSONObject item = all.optJSONObject(i);
              String file = item.optString("file");
              if (!seen.add(file)
                  || !fold(item.optString("search") + item.optJSONArray("keywords"))
                      .contains(fold(query.getText().toString()))) continue;
              LinearLayout tile = column();
              ImageView img = image(file, 60);
              if (img != null) tile.addView(img);
              TextView t = text(item.optString("search"), 14);
              tile.addView(t);
              tile.setPadding(dp(6), dp(6), dp(6), dp(6));
              tile.setContentDescription(item.optString("search"));
              tile.setOnClickListener(
                  v -> {
                    Model.put(draft, "symbol", file);
                    if (photoLabel != null)
                      photoLabel.setText(t("Símbolo: ") + item.optString("search"));
                    d.dismiss();
                    if (after != null) after.run();
                  });
              choices.addView(tile, new GridLayout.LayoutParams());
            }
          };
      query.addTextChangedListener(watcher(update));
      update.run();
    } catch (Exception e) {
      error(e);
    }
  }

  android.text.TextWatcher watcher(Runnable change) {
    return new android.text.TextWatcher() {
      public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

      public void onTextChanged(CharSequence s, int st, int before, int count) {
        change.run();
      }

      public void afterTextChanged(android.text.Editable e) {}
    };
  }

  void keyboard() {
    LinearLayout form = column();
    EditText input =
        field(
            t("Escreva sua mensagem"),
            "",
            android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    input.setMinLines(3);
    form.addView(input);
    if (predict()) {
      LinearLayout hints = row();
      Button[] options = new Button[suggestions.length];
      Runnable refresh =
          () -> {
            String typed = input.getText().toString();
            int cut = typed.lastIndexOf(' ') + 1;
            String before = typed.substring(0, cut);
            String previous = Predictor.last(before.isEmpty() ? model.text() : before);
            List<String> next = predictions(previous, typed.substring(cut));
            for (int i = 0; i < options.length; i++) {
              String word = i < next.size() ? next.get(i) : "";
              options[i].setText(word);
              options[i].setVisibility(word.isEmpty() ? View.INVISIBLE : View.VISIBLE);
            }
          };
      for (int i = 0; i < options.length; i++) {
        Button[] self = new Button[1];
        self[0] =
            plain(
                "",
                () -> {
                  String typed = input.getText().toString();
                  input.setText(
                      typed.substring(0, typed.lastIndexOf(' ') + 1) + self[0].getText() + " ");
                  input.setSelection(input.getText().length());
                });
        options[i] = self[0];
        addRowButton(hints, self[0], 1);
      }
      form.addView(hints);
      input.addTextChangedListener(watcher(refresh));
      refresh.run();
    }
    LinearLayout controls = row();
    addRowButton(
        controls,
        plain(
            t("Falar"),
            () -> {
              learn(input.getText().toString());
              speak(input.getText().toString(), false);
            }),
        1);
    addRowButton(
        controls,
        plain(
            t("Apagar letra"),
            () -> {
              String s = input.getText().toString();
              if (!s.isEmpty()) input.setText(s.substring(0, s.offsetByCodePoints(s.length(), -1)));
            }),
        1);
    form.addView(controls);
    GridLayout keys = new GridLayout(this);
    keys.setColumnCount(6);
    String letters =
        "en".equals(lang())
            ? "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z"
            : "es".equals(lang())
                ? "A B C D E F G H I J K L M N Ñ O P Q R S T U V W X Y Z Á É Í Ó Ú"
                : "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z Ç Ã É Í Ó Ú";
    for (String k : letters.split(" ")) {
      Button b =
          plain(
              k,
              () -> {
                input.append(k.toLowerCase(voiceLocale()));
              });
      keys.addView(b, new GridLayout.LayoutParams());
    }
    form.addView(keys);
    form.addView(plain(t("Espaço"), () -> input.append(" ")));
    show(
        new AlertDialog.Builder(this)
            .setTitle(t("Teclado"))
            .setView(scroll(form))
            .setPositiveButton(
                t("Adicionar à mensagem"),
                (d, w) -> {
                  try {
                    model.append(input.getText().toString(), "");
                    changed();
                  } catch (Exception e) {
                    error(e);
                  }
                })
            .setNegativeButton(t("Fechar"), null));
  }

  void tools() {
    String[] opts = {
      t("Desfazer mensagem"),
      t("Refazer mensagem"),
      t("Pesquisar palavra"),
      t("Salvar mensagem em botão"),
      t("Notas e frases pessoais"),
      t("Rotinas visuais"),
      t("Temporizador"),
      t("Quadro branco"),
      t("Compartilhar mensagem"),
      t("Pausar / retomar acesso"),
      t("Desfazer última edição"),
      t("Páginas"),
      t("Mudar forma da última palavra")
    };
    show(
        new AlertDialog.Builder(this)
            .setTitle(t("Ferramentas"))
            .setItems(
                opts,
                (d, i) -> {
                  switch (i) {
                    case 0:
                      model.undo();
                      changed();
                      break;
                    case 1:
                      model.redo();
                      changed();
                      break;
                    case 2:
                      findWord();
                      break;
                    case 3:
                      if (model.text().isEmpty()) toast(t("Monte uma mensagem primeiro."));
                      else {
                        JSONObject b = model.newButton();
                        Model.put(b, "label", model.text());
                        Model.put(b, "message", model.text());
                        Model.put(b, "type", "speak");
                        editButton(b, true);
                      }
                      break;
                    case 4:
                      notes();
                      break;
                    case 5:
                      routines();
                      break;
                    case 6:
                      timer();
                      break;
                    case 7:
                      whiteboard();
                      break;
                    case 8:
                      Intent share = new Intent(Intent.ACTION_SEND);
                      share.setType("text/plain");
                      share.putExtra(Intent.EXTRA_TEXT, model.text());
                      startActivity(Intent.createChooser(share, t("Compartilhar mensagem")));
                      break;
                    case 9:
                      paused = !paused;
                      status.setText(
                          paused ? t("Acesso pausado — Mais ou acionador para retomar") : t("Acesso retomado"));
                      startScan();
                      break;
                    case 10:
                      if (model.undoEdit()) {
                        save();
                        build();
                      } else toast(t("Nenhuma edição para desfazer nesta sessão."));
                      break;
                    case 11:
                      pagesMenu();
                      break;
                    case 12:
                      if (model.message().length() == 0) toast(t("Monte uma mensagem primeiro."));
                      else wordForms(model.message().length() - 1);
                      break;
                  }
                })
            .setNegativeButton(t("Fechar"), null));
  }

  void findWord() {
    EditText q = field(t("Palavra"), "", 1);
    AlertDialog prompt =
        show(
            new AlertDialog.Builder(this)
                .setTitle(t("Pesquisar em todas as páginas"))
                .setView(q)
                .setPositiveButton(
                    t("Pesquisar"),
                    (d, w) -> {
                      String needle = fold(q.getText().toString());
                      ArrayList<String> labels = new ArrayList<>(), dest = new ArrayList<>();
                      for (int i = 0; i < model.pages().length(); i++) {
                        JSONObject p = model.pages().optJSONObject(i);
                        JSONArray a = p.optJSONArray("buttons");
                        for (int j = 0; j < a.length(); j++) {
                          JSONObject b = a.optJSONObject(j);
                          if (fold(b.optString("label") + " " + b.optString("message"))
                              .contains(needle)) {
                            labels.add(
                                b.optString("label")
                                    + " — "
                                    + p.optString("title")
                                    + (b.optBoolean("hidden") ? t(" (oculto)") : ""));
                            dest.add(p.optString("id"));
                          }
                        }
                      }
                      if (labels.isEmpty()) toast(t("Palavra não encontrada."));
                      else
                        show(
                            new AlertDialog.Builder(this)
                                .setTitle(t("Onde está a palavra"))
                                .setItems(
                                    labels.toArray(new String[0]),
                                    (x, i) -> {
                                      navigate(dest.get(i));
                                    })
                                .setNegativeButton(t("Fechar"), null));
                    })
                .setNegativeButton(t("Cancelar"), null));
  }

  void pagesMenu() {
    if (!edit && !model.settings().optString("pin").isEmpty()) {
      unlock(
          () -> {
            edit = true;
            build();
            pagesMenu();
          });
      return;
    }
    JSONArray pages = model.pages();
    String[] names = new String[pages.length()];
    for (int i = 0; i < names.length; i++) names[i] = pages.optJSONObject(i).optString("title");
    show(
        new AlertDialog.Builder(this)
            .setTitle(t("Suas páginas"))
            .setItems(names, (d, i) -> navigate(pages.optJSONObject(i).optString("id")))
            .setPositiveButton(
                t("+ Nova página"),
                (d, w) -> {
                  EditText input = field(t("Nome da página"), "", 1);
                  show(
                      new AlertDialog.Builder(this)
                          .setTitle(t("Nova página"))
                          .setView(input)
                          .setPositiveButton(
                              t("Criar"),
                              (x, y) -> {
                                String name = input.getText().toString().trim();
                                if (name.isEmpty()) return;
                                JSONObject pg = model.newPage(name);
                                JSONObject link = model.newButton();
                                Model.put(link, "label", name);
                                Model.put(link, "type", "link");
                                Model.put(link, "target", pg.optString("id"));
                                Model.put(link, "color", "pink");
                                model
                                    .page(model.profile().optString("home"))
                                    .optJSONArray("buttons")
                                    .put(link);
                                current = pg.optString("id");
                                edit = true;
                                save();
                                build();
                              })
                          .setNegativeButton(t("Cancelar"), null));
                })
            .setNeutralButton(
                t("Renomear atual"),
                (d, w) -> {
                  EditText input = field(t("Nome"), model.page(current).optString("title"), 1);
                  show(
                      new AlertDialog.Builder(this)
                          .setTitle(t("Renomear página"))
                          .setView(input)
                          .setPositiveButton(
                              t("Salvar"),
                              (x, y) -> {
                                if (!input.getText().toString().trim().isEmpty()) {
                                  model.beforeEdit();
                                  Model.put(
                                      model.page(current),
                                      "title",
                                      input.getText().toString().trim());
                                  save();
                                  renderPage();
                                }
                              })
                          .setNegativeButton(t("Cancelar"), null));
                })
            .setNegativeButton(t("Fechar"), null));
  }

  void profiles() {
    JSONArray a = model.root.optJSONArray("profiles");
    String[] names = new String[a.length()];
    for (int i = 0; i < a.length(); i++) names[i] = a.optJSONObject(i).optString("name");
    show(
        new AlertDialog.Builder(this)
            .setTitle(t("Quem vai se comunicar?"))
            .setItems(
                names,
                (d, i) -> {
                  model.selectProfile(a.optJSONObject(i).optString("id"));
                  current = model.profile().optString("home");
                  back.clear();
                  edit = false;
                  save();
                  build();
                })
            .setPositiveButton(
                t("Novo perfil"),
                (d, w) -> {
                  if (a.length() >= 16) {
                    toast(t("Limite de 16 perfis."));
                    return;
                  }
                  LinearLayout form = column();
                  EditText input = field(t("Nome do perfil"), "", 1);
                  form.addView(input);
                  label(form, t("Prancha inicial e voz"));
                  String[] boards = {"pt", "en", "es"};
                  Spinner board =
                      spinner(
                          new String[] {"Português (Brasil)", "English", "Español"},
                          Math.max(0, Arrays.asList(boards).indexOf(I18n.language())));
                  form.addView(board);
                  show(
                      new AlertDialog.Builder(this)
                          .setTitle(t("Novo perfil"))
                          .setView(scroll(form))
                          .setPositiveButton(
                              t("Criar"),
                              (x, y) -> {
                                if (input.getText().toString().trim().isEmpty()) return;
                                try {
                                  JSONObject template =
                                      new Model(repo.defaults(boards[board.getSelectedItemPosition()]))
                                          .profile();
                                  model.newProfile(input.getText().toString(), template);
                                  current = model.profile().optString("home");
                                  back.clear();
                                  save();
                                  build();
                                } catch (Exception e) {
                                  error(e);
                                }
                              })
                          .setNegativeButton(t("Cancelar"), null));
                })
            .setNeutralButton(
                t("Renomear"),
                (d, w) -> {
                  EditText input = field(t("Nome"), model.profile().optString("name"), 1);
                  show(
                      new AlertDialog.Builder(this)
                          .setTitle(t("Nome do perfil"))
                          .setView(input)
                          .setPositiveButton(
                              t("Salvar"),
                              (x, y) -> {
                                if (!input.getText().toString().trim().isEmpty()) {
                                  Model.put(
                                      model.profile(), "name", input.getText().toString().trim());
                                  save();
                                }
                              })
                          .setNegativeButton(t("Cancelar"), null));
                })
            .setNegativeButton(t("Fechar"), null));
  }

  void settings() {
    if (!model.settings().optString("pin").isEmpty()) {
      unlock(() -> settingsUnlocked());
      return;
    }
    settingsUnlocked();
  }

  void unlock(Runnable task) {
    EditText code =
        field(
            t("Código de edição"),
            "",
            android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
    show(
        new AlertDialog.Builder(this)
            .setTitle(t("Código de edição"))
            .setView(code)
            .setPositiveButton(
                t("Continuar"),
                (d, w) -> {
                  if (model.settings().optString("pin").equals(code.getText().toString()))
                    task.run();
                  else toast(t("Código incorreto."));
                })
            .setNegativeButton(t("Cancelar"), null));
  }

  void settingsUnlocked() {
    String[] items = {
      t("Acesso: toque, permanência e varredura"),
      t("Voz e velocidade"),
      t("Tamanho da grade e letras"),
      t("Exportar backup completo"),
      t("Restaurar backup"),
      t("Importar prancha TD Snap (experimental)"),
      t("Código para proteger a edição"),
      t("Sobre o Voz Livre"),
      t("Idioma do aplicativo")
    };
    show(
        new AlertDialog.Builder(this)
            .setTitle(model.profile().optString("name"))
            .setItems(
                items,
                (d, i) -> {
                  switch (i) {
                    case 0:
                      accessSettings();
                      break;
                    case 1:
                      voiceSettings();
                      break;
                    case 2:
                      displaySettings();
                      break;
                    case 3:
                      Intent out = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                      out.setType("application/json");
                      out.addCategory(Intent.CATEGORY_OPENABLE);
                      out.putExtra(
                          Intent.EXTRA_TITLE,
                          "Voz-Livre-"
                              + new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                                  .format(new Date())
                              + ".vozlivre.json");
                      startActivityForResult(out, EXPORT);
                      break;
                    case 4:
                      chooseImport(false);
                      break;
                    case 5:
                      show(
                          new AlertDialog.Builder(this)
                              .setTitle(t("Importação experimental"))
                              .setMessage(
                                  t("Importa páginas, textos, links, cores, a ordem dos botões e as fotos da prancha de arquivos SPS/SPB. Símbolos de bibliotecas comerciais e ações especiais não são transferidos. O original é preservado."))
                              .setPositiveButton(t("Escolher cópia"), (x, y) -> chooseImport(true))
                              .setNegativeButton(t("Cancelar"), null));
                      break;
                    case 6:
                      pinSettings();
                      break;
                    case 7:
                      about();
                      break;
                    case 8:
                      languageSettings();
                      break;
                  }
                })
            .setNegativeButton(t("Fechar"), null));
  }

  void accessSettings() {
    JSONObject s = model.settings();
    LinearLayout form = column();
    String[]
        methods =
            {
              t("Toque direto"),
              t("Manter pressionado"),
              t("Selecionar ao soltar"),
              t("Toque com pista auditiva"),
              t("Permanência do ponteiro"),
              t("Varredura")
            },
        codes = {"touch", "hold", "release", "auditory", "mouse", "scan"};
    label(form, t("Como selecionar"));
    Spinner method = spinner(methods, Arrays.asList(codes).indexOf(s.optString("method")));
    form.addView(method);
    label(form, t("Tempo de permanência / pressão (milissegundos)"));
    EditText hold = field(t("200 a 5000"), s.optInt("holdMs", 650) + "", 2);
    form.addView(hold);
    label(form, t("Varredura"));
    Spinner scan =
        spinner(
            new String[] {
              t("Automática: Enter ou Espaço confirma"),
              t("Dois acionadores: Espaço avança, Enter confirma")
            },
            s.optString("scanType").equals("step") ? 1 : 0);
    form.addView(scan);
    label(form, t("Velocidade de varredura (milissegundos)"));
    EditText speed = field(t("400 a 8000"), s.optInt("scanMs", 1200) + "", 2);
    form.addView(speed);
    TextView note =
        text(
            t("A varredura percorre a tela principal e também as janelas: ajustes, edição, teclado, rotinas e listas. Em janelas com muitas opções, escolha primeiro o grupo e depois o item. Olhar exige um dispositivo que forneça ponteiro; não há calibração ocular por câmera nesta versão."),
            14);
    form.addView(note);
    show(
        new AlertDialog.Builder(this)
            .setTitle(t("Acesso"))
            .setView(scroll(form))
            .setPositiveButton(
                t("Salvar"),
                (d, w) -> {
                  try {
                    Model.put(
                        s, "holdMs", clamp(Integer.parseInt(hold.getText().toString()), 200, 5000));
                    Model.put(
                        s,
                        "scanMs",
                        clamp(Integer.parseInt(speed.getText().toString()), 400, 8000));
                    Model.put(s, "method", codes[method.getSelectedItemPosition()]);
                    Model.put(s, "scanType", scan.getSelectedItemPosition() == 1 ? "step" : "auto");
                    paused = false;
                    save();
                    build();
                  } catch (NumberFormatException e) {
                    toast(t("Digite os tempos em números."));
                  }
                })
            .setNegativeButton(t("Cancelar"), null));
  }

  void voiceSettings() {
    if (!voiceReady) {
      show(
          new AlertDialog.Builder(this)
              .setTitle(t("Voz não disponível"))
              .setMessage(
                  t("Configure no Android uma voz no idioma deste perfil. Depois, feche e reabra o Voz Livre."))
              .setPositiveButton(
                  t("Configurar no Android"),
                  (d, w) -> startActivity(new Intent("com.android.settings.TTS_SETTINGS")))
              .setNegativeButton(t("Fechar"), null));
      return;
    }
    JSONObject s = model.settings();
    ArrayList<Voice> voices = new ArrayList<>();
    Set<Voice> all = tts.getVoices();
    if (all != null)
      for (Voice v : all) if (v.getLocale().getLanguage().equals(voiceLocale().getLanguage())) voices.add(v);
    voices.sort(
        Comparator.comparing(Voice::isNetworkConnectionRequired).thenComparing(Voice::getName));
    LinearLayout form = column();
    String[] names = new String[Math.max(1, voices.size())];
    int selected = 0;
    for (int i = 0; i < voices.size(); i++) {
      Voice v = voices.get(i);
      names[i] =
          v.getName() + (v.isNetworkConnectionRequired() ? t(" · precisa de internet") : t(" · local"));
      if (v.getName().equals(s.optString("voice"))) selected = i;
    }
    if (voices.isEmpty()) names[0] = t("Voz padrão do sistema");
    label(form, t("Voz disponível no aparelho"));
    Spinner voice = spinner(names, selected);
    form.addView(voice);
    label(form, t("Velocidade (0,3 a 2)"));
    EditText rate = field(t("Velocidade"), s.optDouble("rate", 1) + "", 8194);
    form.addView(rate);
    label(form, t("Altura (0,5 a 2)"));
    EditText pitch = field(t("Altura"), s.optDouble("pitch", 1) + "", 8194);
    form.addView(pitch);
    CheckBox words = new CheckBox(this);
    words.setText(t("Falar cada palavra ao adicionar"));
    words.setChecked(s.optBoolean("speakWords"));
    form.addView(words);
    form.addView(
        plain(t("Ouvir exemplo"), () -> speak(t("Esta é a minha voz. Eu tenho algo a dizer."), false)));
    form.addView(
        plain(
            t("Configurar vozes no Android"),
            () -> {
              try {
                startActivity(new Intent("com.android.settings.TTS_SETTINGS"));
              } catch (Exception e) {
                error(e);
              }
            }));
    show(
        new AlertDialog.Builder(this)
            .setTitle(t("Sua voz"))
            .setView(scroll(form))
            .setPositiveButton(
                t("Salvar"),
                (d, w) -> {
                  try {
                    if (!voices.isEmpty())
                      Model.put(s, "voice", voices.get(voice.getSelectedItemPosition()).getName());
                    Model.put(
                        s,
                        "rate",
                        Math.max(
                            .3,
                            Math.min(
                                2,
                                Double.parseDouble(rate.getText().toString().replace(',', '.')))));
                    Model.put(
                        s,
                        "pitch",
                        Math.max(
                            .5,
                            Math.min(
                                2,
                                Double.parseDouble(pitch.getText().toString().replace(',', '.')))));
                    Model.put(s, "speakWords", words.isChecked());
                    save();
                    configureVoice();
                  } catch (Exception e) {
                    toast(t("Digite valores numéricos para a voz."));
                  }
                })
            .setNegativeButton(t("Cancelar"), null));
  }

  void displaySettings() {
    JSONObject s = model.settings();
    LinearLayout form = column();
    label(form, t("Colunas (mudar pode alterar as posições)"));
    Spinner cols =
        spinner(
            new String[] {t("2 colunas"), t("3 colunas"), t("4 colunas"), t("5 colunas"), t("6 colunas")},
            clamp(s.optInt("columns", 3), 2, 6) - 2);
    form.addView(cols);
    label(form, t("Tamanho das letras"));
    Spinner font =
        spinner(
            new String[] {"17", "19", "22", "26", "30"},
            Math.max(0, Arrays.asList(17, 19, 22, 26, 30).indexOf(s.optInt("font", 19))));
    form.addView(font);
    CheckBox contrast = new CheckBox(this);
    contrast.setText(t("Alto contraste"));
    contrast.setChecked(s.optBoolean("contrast"));
    form.addView(contrast);
    CheckBox predict = new CheckBox(this);
    predict.setText(t("Sugerir próximas palavras"));
    predict.setChecked(predict());
    form.addView(predict);
    CheckBox grammar = new CheckBox(this);
    grammar.setText(t("Conjugar verbos pelo sujeito (eu + brincar → eu brinco)"));
    grammar.setChecked(s.optBoolean("grammarAuto"));
    form.addView(grammar);
    form.addView(
        plain(
            t("Esquecer palavras aprendidas"),
            () ->
                show(
                    new AlertDialog.Builder(this)
                        .setTitle(t("Esquecer palavras aprendidas?"))
                        .setMessage(
                            t("As sugestões deste perfil voltam ao início. Pranchas e mensagens não são alteradas."))
                        .setPositiveButton(
                            t("Esquecer"),
                            (x, y) -> {
                              Predictor.forget(model.profile());
                              save();
                              renderSuggestions();
                            })
                        .setNegativeButton(t("Cancelar"), null))));
    label(form, t("Imagens dos botões iniciais"));
    boolean mulberry = "mulberry".equals(Model.symbolSet(model.root));
    Spinner set =
        spinner(
            new String[] {t("ARASAAC (pictogramas originais)"), t("Mulberry e desenhos próprios")},
            mulberry ? 1 : 0);
    form.addView(set);
    show(
        new AlertDialog.Builder(this)
            .setTitle(t("Grade e aparência"))
            .setView(scroll(form))
            .setPositiveButton(
                t("Salvar"),
                (d, w) -> {
                  Model.put(s, "columns", cols.getSelectedItemPosition() + 2);
                  Model.put(s, "font", Integer.parseInt(font.getSelectedItem().toString()));
                  Model.put(s, "contrast", contrast.isChecked());
                  Model.put(s, "predict", predict.isChecked());
                  Model.put(s, "grammarAuto", grammar.isChecked());
                  if ((set.getSelectedItemPosition() == 1) != mulberry)
                    try {
                      model.beforeEdit();
                      int n =
                          Model.switchSymbols(
                              model.root, repo.legacy(), set.getSelectedItemPosition() == 0);
                      bitmapCache.clear();
                      toast(n + t(" imagens trocadas. Fotos e símbolos escolhidos por você foram mantidos."));
                    } catch (Exception e) {
                      error(e);
                    }
                  save();
                  build();
                })
            .setNegativeButton(t("Cancelar"), null));
  }

  void pinSettings() {
    EditText input =
        field(
            t("Código numérico (vazio remove)"),
            model.settings().optString("pin"),
            android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
    show(
        new AlertDialog.Builder(this)
            .setTitle(t("Proteção da edição"))
            .setMessage(
                t("Este código evita edições por acidente. Ele não criptografa seus dados. Guarde seu backup para recuperação."))
            .setView(input)
            .setPositiveButton(
                t("Salvar"),
                (d, w) -> {
                  Model.put(model.settings(), "pin", input.getText().toString());
                  save();
                })
            .setNegativeButton(t("Cancelar"), null));
  }

  boolean snapImport = false;

  void chooseImport(boolean snap) {
    snapImport = snap;
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.setType("*/*");
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    startActivityForResult(intent, IMPORT);
  }

  void notes() {
    JSONArray notes = model.profile().optJSONArray("notes");
    String[] titles = new String[notes.length()];
    for (int i = 0; i < titles.length; i++) titles[i] = notes.optJSONObject(i).optString("title");
    show(
        new AlertDialog.Builder(this)
            .setTitle(t("Notas pessoais"))
            .setItems(titles, (d, i) -> noteEdit(notes.optJSONObject(i), false))
            .setPositiveButton(
                t("Nova nota"),
                (d, w) -> {
                  JSONObject n = new JSONObject();
                  Model.put(n, "title", t("Nova nota"));
                  Model.put(n, "text", model.text());
                  noteEdit(n, true);
                })
            .setNegativeButton(t("Fechar"), null));
  }

  void noteEdit(JSONObject n, boolean fresh) {
    LinearLayout form = column();
    EditText title = field(t("Título"), n.optString("title"), 1),
        content =
            field(
                t("Texto"),
                n.optString("text"),
                android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    content.setMinLines(5);
    form.addView(title);
    form.addView(content);
    form.addView(plain(t("Falar nota"), () -> speak(content.getText().toString(), false)));
    show(
        new AlertDialog.Builder(this)
            .setTitle(t("Nota"))
            .setView(scroll(form))
            .setPositiveButton(
                t("Salvar"),
                (d, w) -> {
                  Model.put(n, "title", title.getText().toString());
                  Model.put(n, "text", content.getText().toString());
                  if (fresh) model.profile().optJSONArray("notes").put(n);
                  save();
                })
            .setNeutralButton(
                t("Adicionar à mensagem"),
                (d, w) -> {
                  try {
                    model.append(content.getText().toString(), "");
                    changed();
                  } catch (Exception e) {
                    error(e);
                  }
                })
            .setNegativeButton(t("Fechar"), null));
  }

  void routines() {
    JSONArray routines = model.profile().optJSONArray("routines");
    String[] names = new String[routines.length()];
    for (int i = 0; i < names.length; i++) names[i] = routines.optJSONObject(i).optString("title");
    show(
        new AlertDialog.Builder(this)
            .setTitle(t("Rotinas visuais"))
            .setItems(names, (d, i) -> routine(routines.optJSONObject(i)))
            .setPositiveButton(
                t("Criar rotina"),
                (d, w) -> {
                  LinearLayout form = column();
                  EditText title = field(t("Nome"), t("Minha rotina"), 1),
                      steps =
                          field(
                              t("Uma etapa por linha"),
                              t("Primeiro\nDepois"),
                              android.text.InputType.TYPE_CLASS_TEXT
                                  | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                  form.addView(title);
                  form.addView(steps);
                  show(
                      new AlertDialog.Builder(this)
                          .setTitle(t("Nova rotina"))
                          .setView(scroll(form))
                          .setPositiveButton(
                              t("Criar"),
                              (x, y) -> {
                                JSONObject r = new JSONObject();
                                Model.put(r, "title", title.getText().toString());
                                JSONArray a = new JSONArray();
                                for (String t : steps.getText().toString().split("\n"))
                                  if (!t.trim().isEmpty()) {
                                    JSONObject step = new JSONObject();
                                    Model.put(step, "text", t.trim());
                                    Model.put(step, "symbol", findSymbol(t));
                                    a.put(step);
                                  }
                                if (a.length() == 0) return;
                                Model.put(r, "steps", a);
                                Model.put(r, "done", 0);
                                routines.put(r);
                                save();
                                routine(r);
                              })
                          .setNegativeButton(t("Cancelar"), null));
                })
            .setNegativeButton(t("Fechar"), null));
  }

  /** A routine step as {text, symbol}; steps saved before 0.3.0 are plain text. */
  static JSONObject step(JSONArray steps, int i) {
    JSONObject o = steps.optJSONObject(i);
    if (o != null) return o;
    o = new JSONObject();
    Model.put(o, "text", steps.optString(i));
    Model.put(o, "symbol", "");
    try {
      steps.put(i, o);
    } catch (JSONException ignored) {
    }
    return o;
  }

  void routine(JSONObject r) {
    JSONArray steps = r.optJSONArray("steps");
    if (steps == null) return;
    LinearLayout form = column();
    int done = r.optInt("done");
    AlertDialog[] self = new AlertDialog[1];
    for (int i = 0; i < steps.length(); i++) {
      JSONObject step = step(steps, i);
      LinearLayout line = row();
      line.setPadding(dp(4), dp(8), dp(4), dp(8));
      line.setAlpha(i < done ? .45f : 1f);
      TextView mark = text(i < done ? "✓" : i == done ? "→" : "○", 23);
      line.addView(mark, new LinearLayout.LayoutParams(dp(34), -2));
      ImageView img = image(step.optString("symbol"), 56);
      if (img != null) line.addView(img);
      TextView t = text(step.optString("text"), 23);
      t.setPadding(dp(10), 0, 0, 0);
      line.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
      line.setContentDescription(step.optString("text") + t(", trocar imagem"));
      line.setClickable(true);
      line.setOnClickListener(
          v ->
              symbols(
                  step,
                  () -> {
                    save();
                    if (self[0] != null) self[0].dismiss();
                    routine(r);
                  }));
      form.addView(line);
    }
    TextView hint = text(t("Toque em uma etapa para trocar a imagem."), 13);
    hint.setPadding(dp(4), dp(8), 0, 0);
    form.addView(hint);
    self[0] =
        show(
        new AlertDialog.Builder(this)
            .setTitle(r.optString("title"))
            .setView(scroll(form))
            .setPositiveButton(
                done >= steps.length() ? t("Concluído") : t("Próxima etapa"),
                (d, w) -> {
                  if (done < steps.length()) {
                    speak(step(steps, done).optString("text"), false);
                    Model.put(r, "done", done + 1);
                    save();
                    routine(r);
                  }
                })
            .setNeutralButton(
                t("Recomeçar"),
                (d, w) -> {
                  Model.put(r, "done", 0);
                  save();
                  routine(r);
                })
            .setNegativeButton(t("Fechar"), null));
  }

  void timer() {
    EditText minutes = field(t("Minutos"), "2", 2);
    show(
        new AlertDialog.Builder(this)
            .setTitle(t("Temporizador visual"))
            .setView(minutes)
            .setPositiveButton(
                t("Começar"),
                (d, w) -> {
                  try {
                    long duration =
                        clamp(Integer.parseInt(minutes.getText().toString()), 1, 120) * 60000L;
                    LinearLayout form = column();
                    TextView time = text("", 42);
                    time.setGravity(Gravity.CENTER);
                    form.addView(time);
                    ProgressBar progress =
                        new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                    progress.setMax(1000);
                    form.addView(progress);
                    AlertDialog dialog =
                        show(
                            new AlertDialog.Builder(this)
                                .setTitle(t("Tempo da atividade"))
                                .setView(scroll(form))
                                .setNegativeButton(t("Encerrar"), null));
                    CountDownTimer timer =
                        new CountDownTimer(duration, 250) {
                          public void onTick(long ms) {
                            time.setText(
                                String.format(
                                    Locale.ROOT, "%02d:%02d", ms / 60000, (ms / 1000) % 60));
                            progress.setProgress((int) (ms * 1000 / duration));
                          }

                          public void onFinish() {
                            time.setText(t("Terminou"));
                            progress.setProgress(0);
                            speak(t("O tempo terminou"), false);
                          }
                        };
                    dialog.setOnDismissListener(
                        x -> {
                          timer.cancel();
                          closed(dialog);
                        });
                    timer.start();
                  } catch (Exception e) {
                    toast(t("Digite o tempo em minutos."));
                  }
                })
            .setNegativeButton(t("Cancelar"), null));
  }

  void whiteboard() {
    LinearLayout form = column();
    Drawing drawing = new Drawing(this);
    form.addView(drawing, new LinearLayout.LayoutParams(-1, dp(360)));
    form.addView(plain(t("Limpar desenho"), () -> drawing.clear()));
    show(
        new AlertDialog.Builder(this)
            .setTitle(t("Quadro branco"))
            .setView(scroll(form))
            .setPositiveButton(
                t("Salvar como botão"),
                (d, w) -> {
                  try {
                    String file = "user/" + Model.id() + ".png";
                    Bitmap b =
                        Bitmap.createBitmap(
                            Math.max(1, drawing.getWidth()),
                            Math.max(1, drawing.getHeight()),
                            Bitmap.Config.ARGB_8888);
                    drawing.draw(new Canvas(b));
                    try (FileOutputStream out =
                        new FileOutputStream(new File(getFilesDir(), file))) {
                      b.compress(Bitmap.CompressFormat.PNG, 100, out);
                    }
                    JSONObject button = model.newButton();
                    Model.put(button, "label", t("Meu desenho"));
                    Model.put(button, "message", t("Meu desenho"));
                    Model.put(button, "symbol", file);
                    editButton(button, true);
                  } catch (Exception e) {
                    error(e);
                  }
                })
            .setNegativeButton(t("Fechar"), null));
  }

  static class Drawing extends View {
    Paint pen = new Paint(3);
    Path path = new Path();

    Drawing(Context c) {
      super(c);
      pen.setColor(Color.rgb(20, 60, 80));
      pen.setStyle(Paint.Style.STROKE);
      pen.setStrokeWidth(7);
      pen.setStrokeCap(Paint.Cap.ROUND);
      setBackgroundColor(Color.WHITE);
      setContentDescription(t("Área para desenho por toque"));
    }

    void clear() {
      path.reset();
      invalidate();
    }

    protected void onDraw(Canvas c) {
      super.onDraw(c);
      c.drawPath(path, pen);
    }

    public boolean onTouchEvent(MotionEvent e) {
      if (e.getAction() == MotionEvent.ACTION_DOWN) {
        getParent().requestDisallowInterceptTouchEvent(true);
        path.moveTo(e.getX(), e.getY());
      } else if (e.getAction() == MotionEvent.ACTION_MOVE) path.lineTo(e.getX(), e.getY());
      else if (e.getAction() == MotionEvent.ACTION_UP) {
        path.lineTo(e.getX() + .1f, e.getY() + .1f);
        getParent().requestDisallowInterceptTouchEvent(false);
        performClick();
      }
      invalidate();
      return true;
    }

    public boolean performClick() {
      super.performClick();
      return true;
    }
  }

  void about() {
    show(
        new AlertDialog.Builder(this)
            .setTitle(t("Voz Livre ") + BuildInfo.VERSION)
            .setMessage(
                t("Seu comunicador pessoal. Gratuito e de código aberto, sem anúncios. Funciona sem conta e sem conexão para as pranchas e os recursos locais. A voz depende do motor instalado no Android.\n\nAplicativo independente, sem vínculo com Tobii Dynavox. Não inclui PCS, PODD, Gateway ou vozes comerciais.\n\nPictogramas: Sergio Palao. Origem: ARASAAC (arasaac.org). Licença: CC BY-NC-SA 4.0. Propriedade: Governo de Aragão (Espanha). Uso não comercial.\n\nMulberry Symbols by Steve Lee (CC BY-SA 4.0), convertidos para PNG. github.com/mulberrysymbols/mulberry-symbols. Licença: creativecommons.org/licenses/by-sa/4.0/. Desenhos próprios e código: MIT.\n\nVersão alfa: símbolos e vocabulário precisam de revisão individual. Sem validação clínica. Os recursos de acesso precisam de testes em cada aparelho.\n\nEsta versão não inclui calibração ocular, sincronização em nuvem, automação residencial nem atividades Boardmaker. Faça um backup das suas personalizações em Ajustes."))
            .setPositiveButton(t("Fechar"), null));
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (result != RESULT_OK || data == null || data.getData() == null) return;
    Uri uri = data.getData();
    if (request == PHOTO) {
      JSONObject draft = photoDraft;
      io.execute(
          () -> {
            try {
              BitmapFactory.Options options = new BitmapFactory.Options();
              options.inJustDecodeBounds = true;
              try (InputStream in = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, options);
              }
              options.inSampleSize = 1;
              while (options.outWidth / options.inSampleSize > 1024
                  || options.outHeight / options.inSampleSize > 1024) options.inSampleSize *= 2;
              options.inJustDecodeBounds = false;
              Bitmap image;
              try (InputStream in = getContentResolver().openInputStream(uri)) {
                image = BitmapFactory.decodeStream(in, null, options);
              }
              if (image == null) throw new IOException(t("Não foi possível abrir a imagem."));
              String name = "user/" + Model.id() + ".jpg";
              try (FileOutputStream out = new FileOutputStream(new File(getFilesDir(), name))) {
                image.compress(Bitmap.CompressFormat.JPEG, 85, out);
              }
              runOnUiThread(
                  () -> {
                    if (draft != null) {
                      Model.put(draft, "symbol", name);
                      if (photoLabel != null) photoLabel.setText(t("Foto selecionada"));
                    }
                  });
            } catch (Exception e) {
              error(e);
            }
          });
    } else if (request == EXPORT) {
      try {
        Model snapshot = new Model(model.root.toString());
        io.execute(
            () -> {
              try {
                byte[] bytes = repo.backup(snapshot);
                try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                  out.write(bytes);
                }
                runOnUiThread(() -> toast(t("Backup exportado com suas fotos e personalizações.")));
              } catch (Exception e) {
                error(e);
              }
            });
      } catch (Exception e) {
        error(e);
      }
    } else if (request == IMPORT) {
      boolean snap = snapImport;
      io.execute(
          () -> {
            try {
              byte[] bytes = Repository.read(getContentResolver().openInputStream(uri), 32000000);
              if (snap) {
                SnapImporter.Result imported = SnapImporter.read(this, bytes);
                runOnUiThread(
                    () ->
                        show(
                            new AlertDialog.Builder(this)
                                .setTitle(t("Prancha lida"))
                                .setMessage(imported.summary)
                                .setPositiveButton(
                                    t("Adicionar como perfil"),
                                    (d, w) -> {
                                      try {
                                        if (model.root.optJSONArray("profiles").length() >= 16)
                                          throw new IOException(t("Limite de perfis atingido."));
                                        model.newProfile(t("Prancha importada"), imported.profile);
                                        current = model.profile().optString("home");
                                        save();
                                        build();
                                      } catch (Exception e) {
                                        error(e);
                                      }
                                    })
                                .setNegativeButton(t("Cancelar"), null)));
              } else {
                Model loaded = repo.parseBackup(bytes);
                runOnUiThread(
                    () ->
                        show(
                            new AlertDialog.Builder(this)
                                .setTitle(t("Restaurar backup?"))
                                .setMessage(
                                    t("O arquivo contém ")
                                        + loaded.root.optJSONArray("profiles").length()
                                        + t(" perfil(is). A restauração substituirá os dados ativos deste aparelho. Exporte primeiro um backup se desejar conservar as alterações atuais."))
                                .setPositiveButton(
                                    t("Restaurar"),
                                    (d, w) -> {
                                      try {
                                        Model restored = repo.restoreImages(bytes);
                                        repo.save(restored);
                                        model = restored;
                                        current = model.profile().optString("home");
                                        back.clear();
                                        edit = false;
                                        bitmapCache.clear();
                                        build();
                                        toast(t("Backup restaurado."));
                                      } catch (Exception e) {
                                        error(e);
                                      }
                                    })
                                .setNegativeButton(t("Cancelar"), null)));
              }
            } catch (Exception e) {
              error(e);
            }
          });
    }
  }

  @Override
  public void onConfigurationChanged(Configuration config) {
    super.onConfigurationChanged(config);
    build();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    state.putString("page", current);
  }

  @Override
  protected void onPause() {
    super.onPause();
    stopped = true;
    handler.removeCallbacks(scanTick);
    cancelTouch();
    if (tts != null) tts.stop();
    if (model != null && status != null) save();
  }

  @Override
  protected void onResume() {
    super.onResume();
    stopped = false;
    if (model != null) startScan();
  }

  @Override
  protected void onDestroy() {
    stopped = true;
    handler.removeCallbacksAndMessages(null);
    if (tts != null) {
      tts.stop();
      tts.shutdown();
    }
    io.shutdown();
    super.onDestroy();
  }
}
