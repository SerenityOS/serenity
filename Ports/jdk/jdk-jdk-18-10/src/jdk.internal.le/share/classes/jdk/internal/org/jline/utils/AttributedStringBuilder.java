/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Attributed string builder
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class AttributedStringBuilder extends AttributedCharSequence implements Appendable {

    private char[] buffer;
    private int[] style;
    private int length;
    private TabStops tabs = new TabStops(0);
    private int lastLineLength = 0;
    private AttributedStyle current = AttributedStyle.DEFAULT;

    public static AttributedString append(CharSequence... strings) {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        for (CharSequence s : strings) {
            sb.append(s);
        }
        return sb.toAttributedString();
    }

    public AttributedStringBuilder() {
        this(64);
    }

    public AttributedStringBuilder(int capacity) {
        buffer = new char[capacity];
        style = new int[capacity];
        length = 0;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        return buffer[index];
    }

    @Override
    public AttributedStyle styleAt(int index) {
        return new AttributedStyle(style[index], style[index]);
    }

    @Override
    int styleCodeAt(int index) {
        return style[index];
    }

    @Override
    protected char[] buffer() {
        return buffer;
    }

    @Override
    protected int offset() {
        return 0;
    }

    @Override
    public AttributedString subSequence(int start, int end) {
        return new AttributedString(
                Arrays.copyOfRange(buffer, start, end),
                Arrays.copyOfRange(style, start, end),
                0,
                end - start);
    }

    @Override
    public AttributedStringBuilder append(CharSequence csq) {
        return append(new AttributedString(csq, current));
    }

    @Override
    public AttributedStringBuilder append(CharSequence csq, int start, int end) {
        return append(csq.subSequence(start, end));
    }

    @Override
    public AttributedStringBuilder append(char c) {
        return append(Character.toString(c));
    }

    public AttributedStringBuilder append(CharSequence csq, AttributedStyle style) {
        return append(new AttributedString(csq, style));
    }

    public AttributedStringBuilder style(AttributedStyle style) {
        current = style;
        return this;
    }

    public AttributedStringBuilder style(Function<AttributedStyle,AttributedStyle> style) {
        current = style.apply(current);
        return this;
    }

    public AttributedStringBuilder styled(Function<AttributedStyle,AttributedStyle> style, CharSequence cs) {
        return styled(style, sb -> sb.append(cs));
    }

    public AttributedStringBuilder styled(AttributedStyle style, CharSequence cs) {
        return styled(s -> style, sb -> sb.append(cs));
    }

    public AttributedStringBuilder styled(Function<AttributedStyle,AttributedStyle> style, Consumer<AttributedStringBuilder> consumer) {
        AttributedStyle prev = current;
        current = style.apply(prev);
        consumer.accept(this);
        current = prev;
        return this;
    }

    public AttributedStyle style() {
        return current;
    }

    public AttributedStringBuilder append(AttributedString str) {
        return append((AttributedCharSequence) str, 0, str.length());
    }

    public AttributedStringBuilder append(AttributedString str, int start, int end) {
        return append((AttributedCharSequence) str, start, end);
    }

    public AttributedStringBuilder append(AttributedCharSequence str) {
        return append(str, 0, str.length());
    }

    public AttributedStringBuilder append(AttributedCharSequence str, int start, int end) {
        ensureCapacity(length + end - start);
        for (int i = start; i < end; i++) {
            char c = str.charAt(i);
            int s = str.styleCodeAt(i) & ~current.getMask() | current.getStyle();
            if (tabs.defined() && c == '\t') {
                insertTab(new AttributedStyle(s, 0));
            } else {
                ensureCapacity(length + 1);
                buffer[length] = c;
                style[length] = s;
                if (c == '\n') {
                    lastLineLength = 0;
                } else {
                    lastLineLength++;
                }
                length++;
            }
        }
        return this;
    }

    protected void ensureCapacity(int nl) {
        if (nl > buffer.length) {
            int s = Math.max(buffer.length, 1);
            while (s <= nl) {
                s *= 2;
            }
            buffer = Arrays.copyOf(buffer, s);
            style = Arrays.copyOf(style, s);
        }
    }

    public void appendAnsi(String ansi) {
        ansiAppend(ansi);
    }

    public AttributedStringBuilder ansiAppend(String ansi) {
        int ansiStart = 0;
        int ansiState = 0;
        ensureCapacity(length + ansi.length());
        for (int i = 0; i < ansi.length(); i++) {
            char c = ansi.charAt(i);
            if (ansiState == 0 && c == 27) {
                ansiState++;
            } else if (ansiState == 1 && c == '[') {
                ansiState++;
                ansiStart = i + 1;
            } else if (ansiState == 2) {
                if (c == 'm') {
                    String[] params = ansi.substring(ansiStart, i).split(";");
                    int j = 0;
                    while (j < params.length) {
                        int ansiParam = params[j].isEmpty() ? 0 : Integer.parseInt(params[j]);
                        switch (ansiParam) {
                            case 0:
                                current = AttributedStyle.DEFAULT;
                                break;
                            case 1:
                                current = current.bold();
                                break;
                            case 2:
                                current = current.faint();
                                break;
                            case 3:
                                current = current.italic();
                                break;
                            case 4:
                                current = current.underline();
                                break;
                            case 5:
                                current = current.blink();
                                break;
                            case 7:
                                current = current.inverse();
                                break;
                            case 8:
                                current = current.conceal();
                                break;
                            case 9:
                                current = current.crossedOut();
                                break;
                            case 22:
                                current = current.boldOff().faintOff();
                                break;
                            case 23:
                                current = current.italicOff();
                                break;
                            case 24:
                                current = current.underlineOff();
                                break;
                            case 25:
                                current = current.blinkOff();
                                break;
                            case 27:
                                current = current.inverseOff();
                                break;
                            case 28:
                                current = current.concealOff();
                                break;
                            case 29:
                                current = current.crossedOutOff();
                                break;
                            case 30:
                            case 31:
                            case 32:
                            case 33:
                            case 34:
                            case 35:
                            case 36:
                            case 37:
                                current = current.foreground(ansiParam - 30);
                                break;
                            case 39:
                                current = current.foregroundOff();
                                break;
                            case 40:
                            case 41:
                            case 42:
                            case 43:
                            case 44:
                            case 45:
                            case 46:
                            case 47:
                                current = current.background(ansiParam - 40);
                                break;
                            case 49:
                                current = current.backgroundOff();
                                break;
                            case 38:
                            case 48:
                                if (j + 1 < params.length) {
                                    int ansiParam2 = Integer.parseInt(params[++j]);
                                    if (ansiParam2 == 2) {
                                        if (j + 3 < params.length) {
                                            int r = Integer.parseInt(params[++j]);
                                            int g = Integer.parseInt(params[++j]);
                                            int b = Integer.parseInt(params[++j]);
                                            // convert to 256 colors
                                            int col = 16 + (r >> 3) * 36 + (g >> 3) * 6 + (b >> 3);
                                            if (ansiParam == 38) {
                                                current = current.foreground(col);
                                            } else {
                                                current = current.background(col);
                                            }
                                        }
                                    } else if (ansiParam2 == 5) {
                                        if (j + 1 < params.length) {
                                            int col = Integer.parseInt(params[++j]);
                                            if (ansiParam == 38) {
                                                current = current.foreground(col);
                                            } else {
                                                current = current.background(col);
                                            }
                                        }
                                    }
                                }
                                break;
                            case 90:
                            case 91:
                            case 92:
                            case 93:
                            case 94:
                            case 95:
                            case 96:
                            case 97:
                                current = current.foreground(ansiParam - 90 + 8);
                                break;
                            case 100:
                            case 101:
                            case 102:
                            case 103:
                            case 104:
                            case 105:
                            case 106:
                            case 107:
                                current = current.background(ansiParam - 100 + 8);
                                break;
                        }
                        j++;
                    }
                    ansiState = 0;
                } else if (!(c >= '0' && c <= '9' || c == ';')) {
                    // This is not a SGR code, so ignore
                    ansiState = 0;
                }
            } else if (c == '\t' && tabs.defined()) {
                insertTab(current);
            } else {
                ensureCapacity(length + 1);
                buffer[length] = c;
                style[length] = this.current.getStyle();
                if (c == '\n') {
                    lastLineLength = 0;
                } else {
                    lastLineLength++;
                }
                length++;
            }
        }
        return this;
    }

    protected void insertTab(AttributedStyle s) {
        int nb = tabs.spaces(lastLineLength);
        ensureCapacity(length + nb);
        for (int i = 0; i < nb; i++) {
            buffer[length] = ' ';
            style[length] = s.getStyle();
            length++;
        }
        lastLineLength += nb;
    }

    public void setLength(int l) {
        length = l;
    }

    /**
     * Set the number of spaces a tab is expanded to. Tab size cannot be changed
     * after text has been added to prevent inconsistent indentation.
     *
     * If tab size is set to 0, tabs are not expanded (the default).
     * @param tabsize Spaces per tab or 0 for no tab expansion. Must be non-negative
     * @return this
     */
    public AttributedStringBuilder tabs(int tabsize) {
        if (tabsize < 0) {
            throw new IllegalArgumentException("Tab size must be non negative");
        }
        return tabs(Arrays.asList(tabsize));
    }

    public AttributedStringBuilder tabs(List<Integer> tabs) {
        if (length > 0) {
            throw new IllegalStateException("Cannot change tab size after appending text");
        }
        this.tabs = new TabStops(tabs);
        return this;
    }

    public AttributedStringBuilder styleMatches(Pattern pattern, AttributedStyle s) {
        Matcher matcher = pattern.matcher(this);
        while (matcher.find()) {
            for (int i = matcher.start(); i < matcher.end(); i++) {
                style[i] = (style[i] & ~s.getMask()) | s.getStyle();
            }
        }
        return this;
    }

    public AttributedStringBuilder styleMatches(Pattern pattern, List<AttributedStyle> styles) {
        Matcher matcher = pattern.matcher(this);
        while (matcher.find()) {
            for (int group = 0; group < matcher.groupCount(); group++) {
                AttributedStyle s = styles.get(group);
                for (int i = matcher.start(group + 1); i < matcher.end(group + 1); i++) {
                    style[i] = (style[i] & ~s.getMask()) | s.getStyle();
                }
            }
        }
        return this;
    }

    private class TabStops {
        private List<Integer> tabs = new ArrayList<>();
        private int lastStop = 0;
        private int lastSize = 0;

        public TabStops(int tabs) {
            this.lastSize = tabs;
        }

        public TabStops(List<Integer> tabs) {
            this.tabs = tabs;
            int p = 0;
            for (int s: tabs) {
                if (s <= p) {
                    continue;
                }
                lastStop = s;
                lastSize = s - p;
                p = s;
            }
        }

        boolean defined() {
            return lastSize > 0;
        }

        int spaces(int lastLineLength) {
            int out = 0;
            if (lastLineLength >= lastStop) {
                out = lastSize - (lastLineLength - lastStop) % lastSize;
            } else {
                for (int s: tabs) {
                    if (s > lastLineLength) {
                        out = s - lastLineLength;
                        break;
                    }
                }
            }
            return out;
        }

    }

}
