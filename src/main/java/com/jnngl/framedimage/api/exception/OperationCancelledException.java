package com.jnngl.framedimage.api.exception;

public class OperationCancelledException extends Exception {

    private final String operation;

    public OperationCancelledException(String operation) {
        super();
        this.operation = operation;
    }

    public String getExceptionMessage(){
        return operation + " failed: Cancelled by other plugin.";
    }
}
