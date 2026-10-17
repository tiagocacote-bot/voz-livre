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
    Model.validate(m.root);
    check(true, "estado completo continua válido após fluxos");
    System.out.println("TOTAL " + passed + " verificações aprovadas");
  }
}
