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
    java.util.List<String> verbs = java.util.Arrays.asList("brincar", "escovar");
    check(Grammar.conjugate("brincar", Grammar.EU, 0).equals("brinco"), "conjuga presente regular");
    check(Grammar.conjugate("brincar", Grammar.EU, 1).equals("brinquei"), "passado com troca c→qu");
    check(Grammar.conjugate("pegar", Grammar.EU, 1).equals("peguei"), "passado com troca g→gu");
    check(Grammar.conjugate("ir", Grammar.NOS, 0).equals("vamos"), "verbo irregular ir");
    check(Grammar.conjugate("ter", Grammar.ELES, 0).equals("têm"), "verbo irregular ter");
    check(Grammar.conjugate("fazer", Grammar.EU, 3).equals("farei"), "futuro irregular");
    check(Grammar.conjugate("dormir", Grammar.EU, 0).equals("durmo"), "presente com troca de vogal");
    check(Grammar.agree("eu", "brincar", verbs).equals("brinco"), "concorda verbo com sujeito");
    check(Grammar.agree("mais", "comer", verbs).equals("comer"), "sem sujeito não muda");
    check(Grammar.agree("eu", "mar", verbs).equals("mar"), "não conjuga palavra desconhecida");
    check("querer".equals(Grammar.lemma("quero", verbs)), "reconhece forma conjugada");
    boolean past = false;
    for (Grammar.Form f : Grammar.forms("brinco", "eu", verbs)) past |= f.text.equals("brinquei");
    check(past, "formas do verbo incluem passado");
    boolean fem = false;
    for (Grammar.Form f : Grammar.forms("cansado", "", verbs)) fem |= f.text.equals("cansada");
    check(fem, "formas do adjetivo incluem feminino");
    check(Grammar.forms("água", "", verbs).size() == 1, "substantivo só ganha plural");
    check(Grammar.forms("sim", "", verbs).isEmpty(), "palavra sem flexão");
    check(Grammar.plural("confortável").equals("confortáveis") && Grammar.plural("mão").equals("mãos"), "plurais especiais");
    Model words = new Model(source);
    words.append("eu", "");
    words.append("brincar", "");
    words.replace(1, "brinco");
    check(words.text().equals("eu brinco"), "troca a forma de uma palavra da mensagem");
    words.undo();
    check(words.text().equals("eu brincar"), "troca de forma pode ser desfeita");
    JSONObject defaults = new JSONObject(source);
    JSONObject old020 = new JSONObject(source);
    old020.remove("starterVersion");
    JSONArray oldPages = old020.getJSONArray("profiles").getJSONObject(0).getJSONArray("pages");
    for (int i = oldPages.length() - 1; i >= 0; i--) {
      String id = oldPages.getJSONObject(i).optString("id");
      if (java.util.Arrays.asList("perguntas", "escola", "brincar", "tempo", "descricoes", "lugares").contains(id))
        oldPages.remove(i);
    }
    JSONArray oldHome = oldPages.getJSONObject(0).getJSONArray("buttons");
    for (int i = oldHome.length() - 1; i >= 0; i--)
      if (oldHome.getJSONObject(i).has("target")
          && java.util.Arrays.asList("perguntas", "escola", "brincar", "tempo", "descricoes").contains(oldHome.getJSONObject(i).optString("target")))
        oldHome.remove(i);
    for (int i = 0; i < oldPages.length(); i++) {
      JSONArray bs = oldPages.getJSONObject(i).getJSONArray("buttons");
      for (int j = 0; j < bs.length(); j++)
        if (bs.getJSONObject(j).optString("id").equals("b45"))
          Model.put(bs.getJSONObject(j), "symbol", "symbols/mulberry-delicious_food.png");
    }
    int homeBefore = oldHome.length();
    check(Model.upgradeStarter(old020, defaults, legacy) == 5, "acrescenta as 5 páginas novas");
    check(oldHome.length() == homeBefore + 5, "liga as páginas novas no início");
    boolean placesBack = false, fixedSymbol = false;
    for (int i = 0; i < oldPages.length(); i++) {
      placesBack |= oldPages.getJSONObject(i).optString("id").equals("lugares");
      JSONArray bs = oldPages.getJSONObject(i).getJSONArray("buttons");
      for (int j = 0; j < bs.length(); j++)
        fixedSymbol |= bs.getJSONObject(j).optString("id").equals("b45") && bs.getJSONObject(j).optString("symbol").equals("symbols/32340.png");
    }
    check(!placesBack, "não recria página apagada pela pessoa");
    check(fixedSymbol, "dá pictograma ARASAAC a botão que não tinha");
    check(Model.upgradeStarter(old020, defaults, legacy) == 0, "atualização roda uma vez só");
    Model.validate(old020);
    JSONObject routine = Model.copy(m.root);
    JSONObject plain = new JSONObject();
    Model.put(plain, "title", "Antiga");
    plain.put("steps", new JSONArray().put("Primeiro").put("Depois"));
    routine.getJSONArray("profiles").getJSONObject(0).getJSONArray("routines").put(plain);
    Model.validate(routine);
    check(true, "rotinas antigas só com texto continuam válidas");
    JSONObject bad = new JSONObject();
    Model.put(bad, "text", "x");
    Model.put(bad, "symbol", "user/../../segredo.png");
    plain.put("steps", new JSONArray().put(bad));
    rejected(routine, "rejeitar imagem inválida em rotina");
    JSONObject en = new JSONObject(read(assets.resolve("i18n/en.json")));
    I18n.use("en", en);
    check(I18n.t("Fechar").equals("Close") && I18n.t("texto sem tradução").equals("texto sem tradução"), "tradução com reserva em português");
    I18n.use("pt", null);
    check(I18n.t("Fechar").equals("Fechar"), "volta ao português");
    check(I18n.resolve("auto", "es").equals("es") && I18n.resolve("auto", "fr").equals("pt") && I18n.resolve("en", "es").equals("en"), "idioma automático segue o aparelho");
    for (String lang : new String[] {"en", "es"}) {
      Model other = new Model(read(assets.resolve("default-" + lang + ".json")));
      check(other.settings().optString("language").equals(lang) && other.pages().length() == board.pages().length(), "prancha inicial em " + lang);
    }
    Model.validate(m.root);
    check(true, "estado completo continua válido após fluxos");
    System.out.println("TOTAL " + passed + " verificações aprovadas");
  }
}
