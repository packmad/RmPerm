package it.saonzo.rmperm;


class BadCommandLineException extends Exception {
    BadCommandLineException() {
        super();
    }

    BadCommandLineException(String message) {
        super(message);
    }
}
