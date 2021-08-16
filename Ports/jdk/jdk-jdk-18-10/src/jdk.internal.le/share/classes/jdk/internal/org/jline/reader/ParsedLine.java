/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.reader;

import java.util.List;

/**
 * <code>ParsedLine</code> objects are returned by the {@link Parser}
 * during completion or when accepting the line.
 *
 * The instances should implement the {@link CompletingParsedLine}
 * interface so that escape chars and quotes can be correctly handled.
 *
 * @see Parser
 * @see CompletingParsedLine
 */
public interface ParsedLine {

    /**
     * The current word being completed.
     * If the cursor is after the last word, an empty string is returned.
     *
     * @return the word being completed or an empty string
     */
    String word();

    /**
     * The cursor position within the current word.
     *
     * @return the cursor position within the current word
     */
    int wordCursor();

    /**
     * The index of the current word in the list of words.
     *
     * @return the index of the current word in the list of words
     */
    int wordIndex();

    /**
     * The list of words.
     *
     * @return the list of words
     */
    List<String> words();

    /**
     * The unparsed line.
     *
     * @return the unparsed line
     */
    String line();

    /**
     * The cursor position within the line.
     *
     * @return the cursor position within the line
     */
    int cursor();

}
