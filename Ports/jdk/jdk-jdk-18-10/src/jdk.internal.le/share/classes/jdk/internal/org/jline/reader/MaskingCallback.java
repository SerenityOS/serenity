/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.reader;

/**
 * Callback used to mask parts of the line
 */
public interface MaskingCallback {

    /**
     * Transforms the line before it is displayed so that
     * some parts can be hidden.
     *
     * @param line the current line being edited
     * @return the modified line to display
     */
    String display(String line);

    /**
     * Transforms the line before storing in the history.
     * If the return value is empty or null, it will not be saved
     * in the history.
     *
     * @param line the line to be added to history
     * @return the modified line
     */
    String history(String line);

}
