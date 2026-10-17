package br.pessoal.vozlivre;

/** Deterministic focus state for one-switch automatic or two-switch step scanning. */
public final class ScanEngine {
  private int count, index = -1;

  public void reset(int size) {
    count = Math.max(0, size);
    index = -1;
  }

  public int next() {
    if (count == 0) return -1;
    index = (index + 1) % count;
    return index;
  }

  public int selected() {
    return index >= 0 && index < count ? index : -1;
  }

  public int size() {
    return count;
  }
}
