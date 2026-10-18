package br.pessoal.vozlivre;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.json.*;

public class ModelTest {
  static int passed = 0;

  static void check(boolean ok, String name) {
    if (!ok) throw new AssertionError(name);
    passed++;
    System.out.println("OK " + name);
  }

  static void rejected(JSONObject r, String name) {
    try {
      Model.validate(r);
      throw new AssertionError(name);
    } catch (JSONException expected) {
      passed++;
      System.out.println("OK " + name);
    }
  }

  static String read(Path p) throws Exception {
    return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
  }

  public static void main(String[] args) throws Exception {
    String source = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
    Model m = new Model(source);
    m.append("eu", "");
    m.append("quero", "");
    m.append("água", "symbols/32464.png");
    check(m.text().equals("eu quero água"), "compor frase pt-BR");
    m.clear();
    m.undo();
    check(m.text().equals("eu quero água"), "recuperar limpeza acidental");
    m.redo();
    check(m.text().isEmpty(), "refazer limpeza");
    m.undo();
    m.remove();
    check(m.text().equals("eu quero"), "apagar última palavra");
    m.undo();
    check(m.text().equals("eu quero água"), "desfazer apagar");
    Model restored = new Model(m.root.toString());
    check(restored.text().equals(m.text()), "serialização preserva mensagem e acentos");
    String active = m.profile().optString("id");
    m.newProfile("Outro usuário", new Model(source).profile());
    check(m.text().isEmpty(), "perfil novo não herda mensagem");
    m.append("olá", "");
    m.selectProfile(active);
    check(m.text().equals("eu quero água"), "perfis isolam mensagem");
    m.undo();
    check(m.text().equals("eu quero água"), "histórico não atravessa perfis");
    m.beforeEdit();
    Model.put(m.page("inicio"), "title", "Personalizada");
    check(
        m.undoEdit() && m.page("inicio").optString("title").equals("Palavras principais"),
        "desfazer edição sem perder mensagem");
    m.beforeEdit();
    Model.put(m.page("inicio"), "title", "Outra edição");
    m.append("por favor", "");
    m.undoEdit();
    check(m.text().equals("eu quero água por favor"), "desfazer edição preserva mensagem posterior");
    m.undo();
    check(m.text().equals("eu quero água"), "histórico de mensagem permanece independente da edição");
    JSONObject invalid = Model.copy(m.root);
    Model.put(invalid, "activeProfile", "inexistente");
    rejected(invalid, "rejeitar perfil ativo inexistente");
    invalid = Model.copy(m.root);
    Model.put(invalid.optJSONArray("profiles").optJSONObject(0), "home", "sumiu");
    rejected(invalid, "rejeitar página inicial ausente");
    invalid = Model.copy(m.root);
    JSONObject btn =
        invalid
            .optJSONArray("profiles")
            .optJSONObject(0)
            .optJSONArray("pages")
            .optJSONObject(0)
            .optJSONArray("buttons")
            .optJSONObject(0);
    Model.put(btn, "symbol", "user/../../segredo.jpg");
    rejected(invalid, "rejeitar travessia em caminho de foto");
    invalid = Model.copy(m.root);
    JSONArray pages = invalid.optJSONArray("profiles").optJSONObject(0).optJSONArray("pages");
    pages.put(Model.copy(pages.optJSONObject(0)));
    rejected(invalid, "rejeitar IDs duplicados de páginas");
    Model limit = new Model(source);
    for (int i = 0; i < 256; i++) limit.append("a", "");
    try {
      limit.append("extra", "");
      throw new AssertionError("limite");
    } catch (IllegalArgumentException expected) {
      check(limit.message().length() == 256, "limite não descarta mensagem existente");
    }
    ScanEngine scan = new ScanEngine();
    scan.reset(3);
    check(
        scan.selected() == -1 && scan.next() == 0 && scan.next() == 1,
        "varredura começa em alvo válido");
    scan.next();
    check(scan.next() == 0, "varredura retorna ao início");
    scan.reset(0);
    check(scan.next() == -1 && scan.selected() == -1, "página vazia não seleciona alvo anterior");
    check(
        m.page("comida") != null && m.page("frases") != null, "navegação inicial possui destinos");
    Path assets = Paths.get(args[0]).getParent();
    JSONObject seed = new JSONObject(read(assets.resolve("prediction.json")));
    JSONObject legacy = new JSONObject(read(assets.resolve("legacy-symbols.json")));
    java.util.List<String> vocab = java.util.Arrays.asList("água", "beber", "comer", "suco", "banana");
    JSONObject fresh = new Model(source).profile();
    check(
        Predictor.suggest(fresh, "eu", "", vocab, seed, 4).get(0).equals("quero"),
        "sugestões iniciais sem histórico");
    check(
        Predictor.suggest(fresh, "", "", vocab, seed, 4).size() == 4,
        "sugestões no início da mensagem");
    for (int i = 0; i < 3; i++) Predictor.learn(fresh, "Eu quero suco.");
    check(
        Predictor.suggest(fresh, "quero", "", vocab, seed, 4).get(0).equals("suco"),
        "aprende a próxima palavra mais usada");
    check(
        Predictor.suggest(fresh, "quero", "be", vocab, seed, 4).get(0).equals("beber"),
        "completa palavra digitada");
    check(
        Predictor.suggest(fresh, "quero", "agu", vocab, seed, 4).contains("água"),
        "completa sem exigir acento");
    check(
        !Predictor.suggest(fresh, "suco", "", vocab, seed, 4).contains("suco"),
        "não repete a palavra anterior");
    for (int i = 0; i < Predictor.MAX_ENTRIES + 50; i++) Predictor.learn(fresh, "palavra" + i);
    check(
        fresh.getJSONObject("usage").getJSONObject("w").length() <= Predictor.MAX_ENTRIES,
        "aprendizado tem tamanho limitado");
    Model learned = new Model(m.root.toString());
    Predictor.learn(learned.profile(), "eu preciso");
    check(
        new Model(learned.root.toString()).profile().getJSONObject("usage").getJSONObject("b").has("eu preciso"),
        "aprendizado é salvo no perfil");
    Predictor.forget(learned.profile());
    check(!learned.profile().has("usage"), "esquecer palavras aprendidas");
    JSONObject odd = Model.copy(m.root);
    Model.put(odd.optJSONArray("profiles").optJSONObject(0), "usage", "x");
    Model.validate(odd);
    check(!odd.optJSONArray("profiles").optJSONObject(0).has("usage"), "descarta aprendizado inválido");
    Model board = new Model(source);
    check("arasaac".equals(Model.symbolSet(board.root)), "prancha inicial usa ARASAAC");
    String arasaac = board.root.toString();
    JSONObject eu = board.page("inicio").getJSONArray("buttons").getJSONObject(0);
    JSONObject quero = board.page("inicio").getJSONArray("buttons").getJSONObject(1);
    Model.put(quero, "symbol", "user/foto.jpg");
    board.append("eu", eu.optString("symbol"));
    int changed = Model.switchSymbols(board.root, legacy, false);
    check(
        changed > 150 && eu.optString("symbol").equals("symbols/original-self.png"),
        "trocar para Mulberry");
    check(quero.optString("symbol").equals("user/foto.jpg"), "troca preserva foto pessoal");
    check(
        board.message().getJSONObject(board.message().length() - 1).optString("symbol").equals("symbols/original-self.png"),
        "troca atualiza mensagem em andamento");
    JSONObject old = new JSONObject(arasaac);
    Model.switchSymbols(old, legacy, false);
    old.remove("symbolSet");
    Model upgraded = new Model(old.toString());
    Model.switchSymbols(upgraded.root, legacy, true);
    check(
        upgraded.root.getJSONArray("profiles").toString().equals(new JSONObject(arasaac).getJSONArray("profiles").toString()),
        "dados da 0.1.1 voltam exatamente aos pictogramas ARASAAC");
    Model.validate(m.root);
    check(true, "estado completo continua válido após fluxos");
    System.out.println("TOTAL " + passed + " verificações aprovadas");
  }
}
