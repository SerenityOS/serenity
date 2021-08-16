/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.reader;

/**
 * This exception is thrown by {@link LineReader#readLine} when
 * user the user types ctrl-D).
 */
public class EndOfFileException extends RuntimeException {

    private static final long serialVersionUID = 528485360925144689L;

    public EndOfFileException() {
    }

    public EndOfFileException(String message) {
        super(message);
    }

    public EndOfFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public EndOfFileException(Throwable cause) {
        super(cause);
    }

    public EndOfFileException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
