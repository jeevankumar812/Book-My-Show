package com.cfs.BMS.exception;

public class SeatAlreadyBookedException extends ConflictException {
    public SeatAlreadyBookedException(String message) { super(message); }
}
