/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.utils;

/**
 * Text styling.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class AttributedStyle {

    public static final int BLACK =     0;
    public static final int RED =       1;
    public static final int GREEN =     2;
    public static final int YELLOW =    3;
    public static final int BLUE =      4;
    public static final int MAGENTA =   5;
    public static final int CYAN =      6;
    public static final int WHITE =     7;

    public static final int BRIGHT =    8;

    static final int F_BOLD         = 0x00000001;
    static final int F_FAINT        = 0x00000002;
    static final int F_ITALIC       = 0x00000004;
    static final int F_UNDERLINE    = 0x00000008;
    static final int F_BLINK        = 0x00000010;
    static final int F_INVERSE      = 0x00000020;
    static final int F_CONCEAL      = 0x00000040;
    static final int F_CROSSED_OUT  = 0x00000080;
    static final int F_FOREGROUND   = 0x00000100;
    static final int F_BACKGROUND   = 0x00000200;
    static final int F_HIDDEN       = 0x00000400;

    static final int MASK           = 0x000007FF;

    static final int FG_COLOR_EXP    = 16;
    static final int BG_COLOR_EXP    = 24;
    static final int FG_COLOR        = 0xFF << FG_COLOR_EXP;
    static final int BG_COLOR        = 0xFF << BG_COLOR_EXP;

    public static final AttributedStyle DEFAULT = new AttributedStyle();
    public static final AttributedStyle BOLD = DEFAULT.bold();
    public static final AttributedStyle BOLD_OFF = DEFAULT.boldOff();
    public static final AttributedStyle INVERSE = DEFAULT.inverse();
    public static final AttributedStyle INVERSE_OFF = DEFAULT.inverseOff();
    public static final AttributedStyle HIDDEN = DEFAULT.hidden();
    public static final AttributedStyle HIDDEN_OFF = DEFAULT.hiddenOff();

    final int style;
    final int mask;

    public AttributedStyle() {
        this(0, 0);
    }

    public AttributedStyle(AttributedStyle s) {
        this(s.style, s.mask);
    }

    public AttributedStyle(int style, int mask) {
        this.style = style;
        this.mask = mask & MASK | ((style & F_FOREGROUND) != 0 ? FG_COLOR : 0)
                                | ((style & F_BACKGROUND) != 0 ? BG_COLOR : 0);
    }

    public AttributedStyle bold() {
        return new AttributedStyle(style | F_BOLD, mask | F_BOLD);
    }

    public AttributedStyle boldOff() {
        return new AttributedStyle(style & ~F_BOLD, mask | F_BOLD);
    }

    public AttributedStyle boldDefault() {
        return new AttributedStyle(style & ~F_BOLD, mask & ~F_BOLD);
    }

    public AttributedStyle faint() {
        return new AttributedStyle(style | F_FAINT, mask | F_FAINT);
    }

    public AttributedStyle faintOff() {
        return new AttributedStyle(style & ~F_FAINT, mask | F_FAINT);
    }

    public AttributedStyle faintDefault() {
        return new AttributedStyle(style & ~F_FAINT, mask & ~F_FAINT);
    }

    public AttributedStyle italic() {
        return new AttributedStyle(style | F_ITALIC, mask | F_ITALIC);
    }

    public AttributedStyle italicOff() {
        return new AttributedStyle(style & ~F_ITALIC, mask | F_ITALIC);
    }

    public AttributedStyle italicDefault() {
        return new AttributedStyle(style & ~F_ITALIC, mask & ~F_ITALIC);
    }

    public AttributedStyle underline() {
        return new AttributedStyle(style | F_UNDERLINE, mask | F_UNDERLINE);
    }

    public AttributedStyle underlineOff() {
        return new AttributedStyle(style & ~F_UNDERLINE, mask | F_UNDERLINE);
    }

    public AttributedStyle underlineDefault() {
        return new AttributedStyle(style & ~F_UNDERLINE, mask & ~F_UNDERLINE);
    }

    public AttributedStyle blink() {
        return new AttributedStyle(style | F_BLINK, mask | F_BLINK);
    }

    public AttributedStyle blinkOff() {
        return new AttributedStyle(style & ~F_BLINK, mask | F_BLINK);
    }

    public AttributedStyle blinkDefault() {
        return new AttributedStyle(style & ~F_BLINK, mask & ~F_BLINK);
    }

    public AttributedStyle inverse() {
        return new AttributedStyle(style | F_INVERSE, mask | F_INVERSE);
    }

    public AttributedStyle inverseNeg() {
        int s = (style & F_INVERSE) != 0 ? style & ~F_INVERSE : style | F_INVERSE;
        return new AttributedStyle(s, mask | F_INVERSE);
    }

    public AttributedStyle inverseOff() {
        return new AttributedStyle(style & ~F_INVERSE, mask | F_INVERSE);
    }

    public AttributedStyle inverseDefault() {
        return new AttributedStyle(style & ~F_INVERSE, mask & ~F_INVERSE);
    }

    public AttributedStyle conceal() {
        return new AttributedStyle(style | F_CONCEAL, mask | F_CONCEAL);
    }

    public AttributedStyle concealOff() {
        return new AttributedStyle(style & ~F_CONCEAL, mask | F_CONCEAL);
    }

    public AttributedStyle concealDefault() {
        return new AttributedStyle(style & ~F_CONCEAL, mask & ~F_CONCEAL);
    }

    public AttributedStyle crossedOut() {
        return new AttributedStyle(style | F_CROSSED_OUT, mask | F_CROSSED_OUT);
    }

    public AttributedStyle crossedOutOff() {
        return new AttributedStyle(style & ~F_CROSSED_OUT, mask | F_CROSSED_OUT);
    }

    public AttributedStyle crossedOutDefault() {
        return new AttributedStyle(style & ~F_CROSSED_OUT, mask & ~F_CROSSED_OUT);
    }

    public AttributedStyle foreground(int color) {
        return new AttributedStyle(style & ~FG_COLOR | F_FOREGROUND | ((color << FG_COLOR_EXP) & FG_COLOR), mask | F_FOREGROUND);
    }

    public AttributedStyle foregroundOff() {
        return new AttributedStyle(style & ~FG_COLOR & ~F_FOREGROUND, mask | F_FOREGROUND);
    }

    public AttributedStyle foregroundDefault() {
        return new AttributedStyle(style & ~FG_COLOR & ~F_FOREGROUND, mask & ~(F_FOREGROUND | FG_COLOR));
    }

    public AttributedStyle background(int color) {
        return new AttributedStyle(style & ~BG_COLOR | F_BACKGROUND | ((color << BG_COLOR_EXP) & BG_COLOR), mask | F_BACKGROUND);
    }

    public AttributedStyle backgroundOff() {
        return new AttributedStyle(style & ~BG_COLOR & ~F_BACKGROUND, mask | F_BACKGROUND);
    }

    public AttributedStyle backgroundDefault() {
        return new AttributedStyle(style & ~BG_COLOR & ~F_BACKGROUND, mask & ~(F_BACKGROUND | BG_COLOR));
    }

    /**
     * The hidden flag can be used to embed custom escape sequences.
     * The characters are considered being 0-column long and will be printed as-is.
     * The user is responsible for ensuring that those sequences do not move the cursor.
     *
     * @return the new style
     */
    public AttributedStyle hidden() {
        return new AttributedStyle(style | F_HIDDEN, mask | F_HIDDEN);
    }

    public AttributedStyle hiddenOff() {
        return new AttributedStyle(style & ~F_HIDDEN, mask | F_HIDDEN);
    }

    public AttributedStyle hiddenDefault() {
        return new AttributedStyle(style & ~F_HIDDEN, mask & ~F_HIDDEN);
    }

    public int getStyle() {
        return style;
    }

    public int getMask() {
        return mask;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributedStyle that = (AttributedStyle) o;
        if (style != that.style) return false;
        return mask == that.mask;

    }

    @Override
    public int hashCode() {
        int result = style;
        result = 31 * result + mask;
        return result;
    }
}
