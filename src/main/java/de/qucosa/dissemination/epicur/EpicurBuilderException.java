package de.qucosa.dissemination.epicur;

public class EpicurBuilderException extends Exception {
    public EpicurBuilderException(String msg) {
        super(msg);
    }

    public EpicurBuilderException(String msg, Exception e) {
        super(msg, e);
    }
}
