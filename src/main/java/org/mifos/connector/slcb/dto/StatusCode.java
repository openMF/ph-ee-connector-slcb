package org.mifos.connector.slcb.dto;

public enum StatusCode {

    INVALID(-1);


    private final int value;

    StatusCode(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }
}
