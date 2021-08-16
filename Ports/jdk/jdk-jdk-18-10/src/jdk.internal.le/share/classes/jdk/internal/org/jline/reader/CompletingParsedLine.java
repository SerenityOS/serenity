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
 * An extension of {@link ParsedLine} that, being aware of the quoting and escaping rules
 * of the {@link org.jline.reader.Parser} that produced it, knows if and how a completion candidate
 * should be escaped/quoted.
 *
 * @author Eric Bottard
 */
public interface CompletingParsedLine extends ParsedLine {

    CharSequence escape(CharSequence candidate, boolean complete);

    int rawWordCursor();

    int rawWordLength();

}
