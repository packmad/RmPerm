package it.unige.dibris.rmperm;

public class ConsoleOutput implements IOutput {
    final Level level;

    public ConsoleOutput(Level level) {
        this.level = level;
    }

    @Override
    public void printf(Level msgLevel, String format, Object... args) {
        if (msgLevel == Level.ERROR)
            System.err.printf(format, args);
        else if (msgLevel.priority >= this.level.priority)
            System.out.printf(format, args);
    }
}
