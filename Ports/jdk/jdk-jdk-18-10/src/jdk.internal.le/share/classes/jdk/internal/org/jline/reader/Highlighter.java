/*
 * Copyright (c) 2002-2019, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.reader;

import java.util.regex.Pattern;

import jdk.internal.org.jline.utils.AttributedString;

public interface Highlighter {

    AttributedString highlight(LineReader reader, String buffer);
    public void setErrorPattern(Pattern errorPattern);
    public void setErrorIndex(int errorIndex);
}
