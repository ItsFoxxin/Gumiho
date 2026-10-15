package dev.gumiho.client.module.combat;

/** A challenger must win consecutive tick samples before replacing a valid lock. */
public final class StableTarget<T> {
    private T current, challenger;
    private int samples;
    public T current() { return current; }
    public T update(T best, boolean currentValid, boolean pinnedOverride) {
        if (!currentValid || current == null || pinnedOverride) {
            current = best; challenger = null; samples = 0;
        } else if (best == null || best == current) {
            challenger = null; samples = 0;
        } else {
            if (challenger != best) { challenger = best; samples = 0; }
            if (++samples >= 4) { current = best; challenger = null; samples = 0; }
        }
        return current;
    }
    public void reset() { current = null; challenger = null; samples = 0; }
}
