/*
 * Copyright (c) 2002-2019, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.reader.impl;

import java.util.regex.Pattern;

import jdk.internal.org.jline.reader.LineReader;
import jdk.internal.org.jline.reader.LineReader.RegionType;
import jdk.internal.org.jline.reader.Highlighter;
import jdk.internal.org.jline.utils.AttributedString;
import jdk.internal.org.jline.utils.AttributedStringBuilder;
import jdk.internal.org.jline.utils.AttributedStyle;
import jdk.internal.org.jline.utils.WCWidth;

public class DefaultHighlighter implements Highlighter {
    private Pattern errorPattern;
    private int errorIndex = -1;

    @Override
    public void setErrorPattern(Pattern errorPattern) {
        this.errorPattern = errorPattern;
    }

    @Override
    public void setErrorIndex(int errorIndex) {
        this.errorIndex = errorIndex;
    }

    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        int underlineStart = -1;
        int underlineEnd = -1;
        int negativeStart = -1;
        int negativeEnd = -1;
        String search = reader.getSearchTerm();
        if (search != null && search.length() > 0) {
            underlineStart = buffer.indexOf(search);
            if (underlineStart >= 0) {
                underlineEnd = underlineStart + search.length() - 1;
            }
        }
        if (reader.getRegionActive() != RegionType.NONE) {
            negativeStart = reader.getRegionMark();
            negativeEnd = reader.getBuffer().cursor();
            if (negativeStart > negativeEnd) {
                int x = negativeEnd;
                negativeEnd = negativeStart;
                negativeStart = x;
            }
            if (reader.getRegionActive() == RegionType.LINE) {
                while (negativeStart > 0 && reader.getBuffer().atChar(negativeStart - 1) != '\n') {
                    negativeStart--;
                }
                while (negativeEnd < reader.getBuffer().length() - 1 && reader.getBuffer().atChar(negativeEnd + 1) != '\n') {
                    negativeEnd++;
                }
            }
        }

        AttributedStringBuilder sb = new AttributedStringBuilder();
        for (int i = 0; i < buffer.length(); i++) {
            if (i == underlineStart) {
                sb.style(AttributedStyle::underline);
            }
            if (i == negativeStart) {
                sb.style(AttributedStyle::inverse);
            }
            if (i == errorIndex) {
                sb.style(AttributedStyle::inverse);
            }

            char c = buffer.charAt(i);
            if (c == '\t' || c == '\n') {
                sb.append(c);
            } else if (c < 32) {
                sb.style(AttributedStyle::inverseNeg)
                        .append('^')
                        .append((char) (c + '@'))
                        .style(AttributedStyle::inverseNeg);
            } else {
                int w = WCWidth.wcwidth(c);
                if (w > 0) {
                    sb.append(c);
                }
            }
            if (i == underlineEnd) {
                sb.style(AttributedStyle::underlineOff);
            }
            if (i == negativeEnd) {
                sb.style(AttributedStyle::inverseOff);
            }
            if (i == errorIndex) {
                sb.style(AttributedStyle::inverseOff);
            }
        }
        if (errorPattern != null) {
            sb.styleMatches(errorPattern, AttributedStyle.INVERSE);
        }
        return sb.toAttributedString();
    }

}
