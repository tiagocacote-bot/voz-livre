package br.pessoal.vozlivre;

import java.util.*;

/**
 * Brazilian Portuguese word forms for AAC messages: verb conjugation by subject and tense, and
 * gender/number of adjectives and nouns. Rule-based with the irregular verbs most used in AAC; it
 * suggests forms and never changes a message by itself unless the person enables it.
 */
public final class Grammar {
  /** Persons: eu, você/ele/ela/a gente, nós, vocês/eles/elas. */
  public static final int EU = 0, ELE = 1, NOS = 2, ELES = 3;

  public static final String[] TENSES = {"Presente", "Passado", "Imperfeito", "Futuro"};
  static final String[] PERSON_NAMES = {"eu", "você/ele/ela", "nós", "vocês/eles/elas"};

  static final Map<String, String[]> IRREGULAR = new HashMap<>();
  static final Map<String, String> GERUND = new HashMap<>(), PARTICIPLE = new HashMap<>();
  /** Common verbs recognized from conjugated forms, in addition to the board's infinitives. */
  static final String[] COMMON = (
      "abraçar abrir acabar acordar ajudar almoçar amar andar apertar aprender arrumar assistir"
          + " beber beijar brincar buscar cair cantar chamar chegar chorar comer começar comprar"
          + " conhecer conseguir contar conversar correr cortar cozinhar dançar dar decidir"
          + " desenhar descansar desligar dizer doer dormir entender entrar escolher escovar"
          + " escrever escutar esperar esquecer estar estudar falar fazer fechar ficar gostar"
          + " gritar guardar ir jantar jogar lavar lembrar levantar ler ligar limpar machucar"
          + " mandar mexer morar mostrar mudar nadar odiar olhar ouvir pagar parar passear pedir"
          + " pegar pensar perder perguntar pintar poder pôr precisar preferir pular querer"
          + " receber repetir respirar responder rir saber sair sentar sentir ser servir subir"
          + " sujar tirar tocar tomar trabalhar trazer trocar usar ver vestir viajar vir voltar")
      .split(" ");

  static {
    irregular("ser", "sou é somos são", "fui foi fomos foram", "era era éramos eram", null);
    irregular("estar", "estou está estamos estão", "estive esteve estivemos estiveram", null, null);
    irregular("ter", "tenho tem temos têm", "tive teve tivemos tiveram", "tinha tinha tínhamos tinham", null);
    irregular("ir", "vou vai vamos vão", "fui foi fomos foram", "ia ia íamos iam", null);
    irregular("querer", "quero quer queremos querem", "quis quis quisemos quiseram", null, null);
    irregular("poder", "posso pode podemos podem", "pude pôde pudemos puderam", null, null);
    irregular("fazer", "faço faz fazemos fazem", "fiz fez fizemos fizeram", null, "farei fará faremos farão");
    irregular("dizer", "digo diz dizemos dizem", "disse disse dissemos disseram", null, "direi dirá diremos dirão");
    irregular("trazer", "trago traz trazemos trazem", "trouxe trouxe trouxemos trouxeram", null, "trarei trará traremos trarão");
    irregular("dar", "dou dá damos dão", "dei deu demos deram", null, null);
    irregular("ver", "vejo vê vemos veem", "vi viu vimos viram", "via via víamos viam", null);
    irregular("vir", "venho vem vimos vêm", "vim veio viemos vieram", "vinha vinha vínhamos vinham", null);
    irregular("saber", "sei sabe sabemos sabem", "soube soube soubemos souberam", null, null);
    irregular("pôr", "ponho põe pomos põem", "pus pôs pusemos puseram", "punha punha púnhamos punham", "porei porá poremos porão");
    irregular("ler", "leio lê lemos leem", null, null, null);
    irregular("rir", "rio ri rimos riem", null, null, null);
    irregular("sair", "saio sai saímos saem", "saí saiu saímos saíram", "saía saía saíamos saíam", null);
    irregular("cair", "caio cai caímos caem", "caí caiu caímos caíram", "caía caía caíamos caíam", null);
    irregular("doer", "doo dói doemos doem", "doí doeu doemos doeram", "doía doía doíamos doíam", null);
    irregular("odiar", "odeio odeia odiamos odeiam", null, null, null);
    irregular("dormir", "durmo dorme dormimos dormem", null, null, null);
    irregular("ouvir", "ouço ouve ouvimos ouvem", null, null, null);
    irregular("pedir", "peço pede pedimos pedem", null, null, null);
    irregular("sentir", "sinto sente sentimos sentem", null, null, null);
    irregular("preferir", "prefiro prefere preferimos preferem", null, null, null);
    irregular("vestir", "visto veste vestimos vestem", null, null, null);
    irregular("servir", "sirvo serve servimos servem", null, null, null);
    irregular("repetir", "repito repete repetimos repetem", null, null, null);
    irregular("subir", "subo sobe subimos sobem", null, null, null);
    irregular("perder", "perco perde perdemos perdem", null, null, null);
    irregular("conseguir", "consigo consegue conseguimos conseguem", null, null, null);
    irregular("seguir", "sigo segue seguimos seguem", null, null, null);
    GERUND.put("pôr", "pondo");
    for (String[] p :
        new String[][] {
          {"fazer", "feito"}, {"dizer", "dito"}, {"ver", "visto"}, {"vir", "vindo"},
          {"pôr", "posto"}, {"abrir", "aberto"}, {"escrever", "escrito"}, {"pagar", "pago"},
          {"ganhar", "ganho"}, {"gastar", "gasto"}
        }) PARTICIPLE.put(p[0], p[1]);
  }

  private Grammar() {}

  static void irregular(String verb, String present, String past, String imperfect, String future) {
    String[] forms = new String[16];
    String[][] parts = {split(present), split(past), split(imperfect), split(future)};
    for (int t = 0; t < 4; t++) if (parts[t] != null) System.arraycopy(parts[t], 0, forms, t * 4, 4);
    IRREGULAR.put(verb, forms);
  }

  static String[] split(String s) {
    return s == null ? null : s.split(" ");
  }

  static String lower(String s) {
    return s.trim().toLowerCase(new Locale("pt", "BR"));
  }

  /** Person of a subject word, or -1 when it is not a subject pronoun. */
  public static int person(String word) {
    switch (lower(word)) {
      case "eu":
        return EU;
      case "você":
      case "voce":
      case "ele":
      case "ela":
      case "gente":
        return ELE;
      case "nós":
      case "nos":
        return NOS;
      case "vocês":
      case "voces":
      case "eles":
      case "elas":
        return ELES;
      default:
        return -1;
    }
  }

  public static boolean infinitive(String word) {
    String w = lower(word);
    if (IRREGULAR.containsKey(w)) return true;
    return w.length() > 2 && !w.contains(" ") && w.matches("\\p{L}+(ar|er|ir)");
  }

  public static String conjugate(String verb, int person, int tense) {
    String v = lower(verb);
    String[] irregular = IRREGULAR.get(v);
    if (irregular != null && irregular[tense * 4 + person] != null) return irregular[tense * 4 + person];
    if (v.equals("pôr")) v = "por";
    String stem = v.substring(0, v.length() - 2), end = v.substring(v.length() - 2);
    if (tense == 3) {
      String inf = verb.equals("pôr") ? "por" : v;
      return inf + new String[] {"ei", "á", "emos", "ão"}[person];
    }
    if (tense == 0 && end.equals("ar") && stem.endsWith("e"))
      return stem + new String[] {"io", "ia", "amos", "iam"}[person]; // passear: passeio
    if (tense == 0 && person == EU) {
      if (end.equals("er") && stem.endsWith("c")) return stem.substring(0, stem.length() - 1) + "ço";
      if (!end.equals("ar") && stem.endsWith("g")) return stem.substring(0, stem.length() - 1) + "jo";
      if (end.equals("ir") && stem.endsWith("gu")) return stem.substring(0, stem.length() - 1) + "o";
    }
    if (tense == 1 && person == EU && end.equals("ar")) {
      if (stem.endsWith("c")) return stem.substring(0, stem.length() - 1) + "quei";
      if (stem.endsWith("g")) return stem + "uei";
      if (stem.endsWith("ç")) return stem.substring(0, stem.length() - 1) + "cei";
    }
    String[] e;
    switch (end) {
      case "ar":
        e =
            new String[] {
              "o", "a", "amos", "am", "ei", "ou", "amos", "aram", "ava", "ava", "ávamos", "avam"
            };
        break;
      case "er":
        e =
            new String[] {
              "o", "e", "emos", "em", "i", "eu", "emos", "eram", "ia", "ia", "íamos", "iam"
            };
        break;
      default:
        e =
            new String[] {
              "o", "e", "imos", "em", "i", "iu", "imos", "iram", "ia", "ia", "íamos", "iam"
            };
    }
    return stem + e[tense * 4 + person];
  }

  public static String gerund(String verb) {
    String v = lower(verb);
    if (GERUND.containsKey(v)) return GERUND.get(v);
    return v.substring(0, v.length() - 1) + "ndo";
  }

  public static String participle(String verb) {
    String v = lower(verb);
    if (PARTICIPLE.containsKey(v)) return PARTICIPLE.get(v);
    String stem = v.substring(0, v.length() - 2);
    return stem + (v.endsWith("ar") ? "ado" : "ido");
  }

  /** Infinitive for a known verb form ("quero" → "querer"), or null. */
  public static String lemma(String word, Collection<String> extraVerbs) {
    String w = lower(word);
    if (infinitive(w) && (IRREGULAR.containsKey(w) || known(w, extraVerbs))) return w;
    LinkedHashSet<String> verbs = new LinkedHashSet<>(Arrays.asList(COMMON));
    for (String v : extraVerbs) if (infinitive(v)) verbs.add(lower(v));
    for (String v : verbs) {
      for (int t = 0; t < 4; t++) for (int p = 0; p < 4; p++) if (conjugate(v, p, t).equals(w)) return v;
      if (gerund(v).equals(w) || participle(v).equals(w)) return v;
    }
    return null;
  }

  static boolean known(String w, Collection<String> extraVerbs) {
    if (Arrays.asList(COMMON).contains(w)) return true;
    for (String v : extraVerbs) if (lower(v).equals(w)) return true;
    return false;
  }

  /** Present-tense form after a subject ("eu" + "brincar" → "brinco"); other words unchanged. */
  public static String agree(String subject, String word, Collection<String> extraVerbs) {
    int p = person(subject);
    String w = lower(word);
    if (p < 0 || !infinitive(w) || !(IRREGULAR.containsKey(w) || known(w, extraVerbs))) return word;
    return conjugate(w, p, 0);
  }

  /** Gender/number forms for adjectives and nouns, labeled; empty when not inflectable. */
  public static List<Form> inflections(String word) {
    String w = lower(word);
    ArrayList<Form> out = new ArrayList<>();
    if (w.isEmpty() || w.contains(" ") || !w.matches("\\p{L}+") || person(w) >= 0) return out;
    if (INVARIABLE.contains(w) || lemma(w, Collections.<String>emptyList()) != null) return out;
    String masc = null, fem = null;
    if (w.equals("irmão") || w.equals("irmã") || w.equals("irmãos") || w.equals("irmãs")) {
      masc = "irmão";
      fem = "irmã";
    } else if (!gendered(w)) {
      // Nouns such as "água" or "mão" keep their gender; only offer the plural.
    } else if (w.endsWith("o")) {
      masc = w;
      fem = w.substring(0, w.length() - 1) + "a";
    } else if (w.endsWith("os")) {
      masc = w.substring(0, w.length() - 1);
      fem = w.substring(0, w.length() - 2) + "a";
    } else if (w.endsWith("as") && !w.endsWith("ias")) {
      fem = w.substring(0, w.length() - 1);
      masc = w.substring(0, w.length() - 2) + "o";
    } else if (w.endsWith("a") && !w.endsWith("ia")) {
      fem = w;
      masc = w.substring(0, w.length() - 1) + "o";
    } else if (w.endsWith("or")) {
      masc = w;
      fem = w + "a";
    } else if (w.endsWith("ora")) {
      fem = w;
      masc = w.substring(0, w.length() - 1);
    }
    if (masc != null) {
      out.add(new Form(masc, "Masculino · singular"));
      out.add(new Form(fem, "Feminino · singular"));
      out.add(new Form(plural(masc), "Masculino · plural"));
      out.add(new Form(plural(fem), "Feminino · plural"));
    } else if (!w.endsWith("s")) out.add(new Form(plural(w), "Plural"));
    return out;
  }

  /** Function words that have no plural or gender forms. */
  static final Set<String> INVARIABLE =
      new HashSet<>(
          Arrays.asList(
              ("sim não nao mais menos aqui ali lá agora depois antes ontem hoje amanhã já também"
                      + " bem mal nunca sempre ainda quando onde como quem qual que por para com"
                      + " sem de em e ou mas se então muito pouco tudo nada obrigado")
                  .split(" ")));

  /** Words whose -o/-a ending marks gender: people, feelings and descriptions. */
  static final Set<String> GENDERED_WORDS =
      new HashSet<>(
          Arrays.asList(
              ("amigo menino filho tio primo neto avô vizinho médico aluno colega moço namorado"
                      + " bravo calmo feio bonito lindo pequeno novo velho sozinho confuso branco"
                      + " preto amarelo vermelho roxo sujo limpo frio quieto alto baixo gordo magro"
                      + " rápido lento pronto certo errado tranquilo nervoso ")
                  .split(" ")));

  static boolean gendered(String w) {
    String base = w.endsWith("s") ? w.substring(0, w.length() - 1) : w;
    String masc = base.endsWith("a") ? base.substring(0, base.length() - 1) + "o" : base;
    if (base.endsWith("ão") || base.endsWith("ã")) return false;
    if (GENDERED_WORDS.contains(masc)) return true;
    if (base.endsWith("or") || base.endsWith("ora")) return true;
    return masc.matches(".*(ado|ido|oso|ito|inho|eiro|ivo|ico|ento|ano)$");
  }

  static boolean accented(String s) {
    return s.matches(".*[áéíóúâêô].*");
  }

  public static String plural(String w) {
    if (w.endsWith("s") || w.endsWith("x")) return w;
    if (Arrays.asList("mão", "irmão", "chão", "grão", "cidadão", "órgão", "órfão").contains(w))
      return w + "s";
    if (Arrays.asList("pão", "cão", "capitão", "alemão").contains(w))
      return w.substring(0, w.length() - 2) + "ães";
    if (w.endsWith("ão")) return w.substring(0, w.length() - 2) + "ões";
    if (w.endsWith("el")) {
      String stem = w.substring(0, w.length() - 2);
      return stem + (accented(stem) ? "eis" : "éis");
    }
    if (w.endsWith("il")) {
      String stem = w.substring(0, w.length() - 2);
      return accented(stem) ? stem + "eis" : w.substring(0, w.length() - 1) + "s";
    }
    if (w.endsWith("al") || w.endsWith("ol") || w.endsWith("ul"))
      return w.substring(0, w.length() - 1) + "is";
    if (w.endsWith("m")) return w.substring(0, w.length() - 1) + "ns";
    if (w.endsWith("r") || w.endsWith("z")) return w + "es";
    return w + "s";
  }

  /** One choice for the word-forms dialog. */
  public static final class Form {
    public final String text, description;

    Form(String text, String description) {
      this.text = text;
      this.description = description;
    }
  }

  /**
   * Word forms to offer for a message word.
   *
   * @param subject the word before it in the message, or ""
   */
  public static List<Form> forms(String word, String subject, Collection<String> extraVerbs) {
    ArrayList<Form> out = new ArrayList<>();
    String verb = lemma(word, extraVerbs);
    if (verb != null) {
      int p = person(subject);
      if (p >= 0)
        for (int t = 0; t < 4; t++) out.add(new Form(conjugate(verb, p, t), TENSES[t]));
      else
        for (int i = 0; i < 4; i++)
          out.add(new Form(conjugate(verb, i, 0), "Presente · " + PERSON_NAMES[i]));
      if (p >= 0) out.add(new Form(conjugate(verb, p == EU ? ELE : EU, 0), "Presente · " + PERSON_NAMES[p == EU ? ELE : EU]));
      out.add(new Form(verb, "Infinitivo"));
      out.add(new Form(gerund(verb), "Gerúndio"));
      out.add(new Form(participle(verb), "Particípio"));
    } else out.addAll(inflections(word));
    LinkedHashMap<String, Form> unique = new LinkedHashMap<>();
    for (Form f : out) if (!f.text.equals(lower(word))) unique.putIfAbsent(f.text, f);
    return new ArrayList<>(unique.values());
  }
}
