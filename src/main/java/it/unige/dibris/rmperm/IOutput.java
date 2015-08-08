package it.unige.dibris.rmperm;

public interface IOutput {
    enum Level {
        DEBUG(0),
        VERBOSE(1),
        NORMAL(2),
        ERROR(3);
        public final int priority;

        Level(int priority) {
            this.priority = priority;
        }
    }

    void printf(Level l, String format, Object... args);
}
