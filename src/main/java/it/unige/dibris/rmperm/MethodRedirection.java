package it.unige.dibris.rmperm;

import org.jf.dexlib2.iface.reference.MethodReference;

public final class MethodRedirection {
    final MethodReference from;
    final MethodReference to;

    public MethodRedirection(MethodReference from, MethodReference to) {
        this.from = from;
        this.to = to;
    }

    public MethodReference getFrom() {

        return from;
    }

    public MethodReference getTo() {
        return to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof MethodRedirection))
            return false;

        MethodRedirection that = (MethodRedirection) o;

        if (from != null ? !from.equals(that.from) : that.from != null)
            return false;
        return !(to != null ? !to.equals(that.to) : that.to != null);

    }

    @Override
    public int hashCode() {
        int result = from != null ? from.hashCode() : 0;
        result = 31 * result + (to != null ? to.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return from + " ---> " + to;
    }
}
