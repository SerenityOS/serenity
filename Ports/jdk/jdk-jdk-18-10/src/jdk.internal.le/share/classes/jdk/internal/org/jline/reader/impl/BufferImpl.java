/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.reader.impl;

import java.util.Objects;

import jdk.internal.org.jline.reader.Buffer;

/**
 * A holder for a {@link StringBuilder} that also contains the current cursor position.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.0
 */
public class BufferImpl implements Buffer
{
    private int cursor = 0;
    private int cursorCol = -1;
    private int[] buffer;
    private int g0;
    private int g1;

    public BufferImpl() {
        this(64);
    }

    public BufferImpl(int size) {
        buffer = new int[size];
        g0 = 0;
        g1 = buffer.length;
    }

    private BufferImpl(BufferImpl buffer) {
        this.cursor = buffer.cursor;
        this.cursorCol = buffer.cursorCol;
        this.buffer = buffer.buffer.clone();
        this.g0 = buffer.g0;
        this.g1 = buffer.g1;
    }

    public BufferImpl copy () {
        return new BufferImpl(this);
    }

    public int cursor() {
        return cursor;
    }

    public int length() {
        return buffer.length - (g1 - g0);
    }

    public boolean currChar(int ch) {
        if (cursor == length()) {
            return false;
        } else {
            buffer[adjust(cursor)] = ch;
            return true;
        }
    }

    public int currChar() {
        if (cursor == length()) {
            return 0;
        } else {
            return atChar(cursor);
        }
    }

    public int prevChar() {
        if (cursor <= 0) {
            return 0;
        }
        return atChar(cursor - 1);
    }

    public int nextChar() {
        if (cursor >= length() - 1) {
            return 0;
        }
        return atChar(cursor + 1);
    }

    public int atChar(int i) {
        if (i < 0 || i >= length()) {
            return 0;
        }
        return buffer[adjust(i)];
    }

    private int adjust(int i) {
        return (i >= g0) ? i + g1 - g0 : i;
    }

    /**
     * Write the specific character into the buffer, setting the cursor position
     * ahead one.
     *
     * @param c the character to insert
     */
    public void write(int c) {
        write(new int[] { c });
    }

    /**
     * Write the specific character into the buffer, setting the cursor position
     * ahead one. The text may overwrite or insert based on the current setting
     * of {@code overTyping}.
     *
     * @param c the character to insert
     */
    public void write(int c, boolean overTyping) {
        if (overTyping) {
            delete(1);
        }
        write(new int[] { c });
    }

    /**
     * Insert the specified chars into the buffer, setting the cursor to the end of the insertion point.
     */
    public void write(CharSequence str) {
        Objects.requireNonNull(str);
        write(str.codePoints().toArray());
    }

    public void write(CharSequence str, boolean overTyping) {
        Objects.requireNonNull(str);
        int[] ucps = str.codePoints().toArray();
        if (overTyping) {
            delete(ucps.length);
        }
        write(ucps);
    }

    private void write(int[] ucps) {
        moveGapToCursor();
        int len = length() + ucps.length;
        int sz = buffer.length;
        if (sz < len) {
            while (sz < len) {
                sz *= 2;
            }
            int[] nb = new int[sz];
            System.arraycopy(buffer, 0, nb, 0, g0);
            System.arraycopy(buffer, g1, nb, g1 + sz - buffer.length, buffer.length - g1);
            g1 += sz - buffer.length;
            buffer = nb;
        }
        System.arraycopy(ucps, 0, buffer, cursor, ucps.length);
        g0 += ucps.length;
        cursor += ucps.length;
        cursorCol = -1;
    }

    public boolean clear() {
        if (length() == 0) {
            return false;
        }
        g0 = 0;
        g1 = buffer.length;
        cursor = 0;
        cursorCol = -1;
        return true;
    }

    public String substring(int start) {
        return substring(start, length());
    }

    public String substring(int start, int end) {
        if (start >= end || start < 0 || end > length()) {
            return "";
        }
        if (end <= g0) {
            return new String(buffer, start, end - start);
        } else if (start > g0) {
            return new String(buffer, g1 - g0 + start, end - start);
        } else {
            int[] b = buffer.clone();
            System.arraycopy(b, g1, b, g0, b.length - g1);
            return new String(b, start, end - start);
        }
    }

    public String upToCursor() {
        return substring(0, cursor);
    }

    /**
     * Move the cursor position to the specified absolute index.
     */
    public boolean cursor(int position) {
        if (position == cursor) {
            return true;
        }
        return move(position - cursor) != 0;
    }

    /**
     * Move the cursor <i>where</i> characters.
     *
     * @param num   If less than 0, move abs(<i>where</i>) to the left, otherwise move <i>where</i> to the right.
     * @return      The number of spaces we moved
     */
    public int move(final int num) {
        int where = num;

        if ((cursor == 0) && (where <= 0)) {
            return 0;
        }

        if ((cursor == length()) && (where >= 0)) {
            return 0;
        }

        if ((cursor + where) < 0) {
            where = -cursor;
        }
        else if ((cursor + where) > length()) {
            where = length() - cursor;
        }

        cursor += where;
        cursorCol = -1;

        return where;
    }

    public boolean up() {
        int col = getCursorCol();
        int pnl = cursor - 1;
        while (pnl >= 0 && atChar(pnl) != '\n') {
            pnl--;
        }
        if (pnl < 0) {
            return false;
        }
        int ppnl = pnl - 1;
        while (ppnl >= 0 && atChar(ppnl) != '\n') {
            ppnl--;
        }
        cursor = Math.min(ppnl + col + 1, pnl);
        return true;
    }

    public boolean down() {
        int col = getCursorCol();
        int nnl = cursor;
        while (nnl < length() && atChar(nnl) != '\n') {
            nnl++;
        }
        if (nnl >= length()) {
            return false;
        }
        int nnnl = nnl + 1;
        while (nnnl < length() && atChar(nnnl) != '\n') {
            nnnl++;
        }
        cursor = Math.min(nnl + col + 1, nnnl);
        return true;
    }

    public boolean moveXY(int dx, int dy) {
        int col = 0;
        while (prevChar() != '\n' && move(-1) == -1) {
            col++;
        }
        cursorCol = 0;
        while (dy < 0) {
            up();
            dy++;
        }
        while (dy > 0) {
            down();
            dy--;
        }
        col = Math.max(col + dx, 0);
        for (int i = 0; i < col; i++) {
            if (move(1) != 1 || currChar() == '\n') {
                break;
            }
        }
        cursorCol = col;
        return true;
    }

    private int getCursorCol() {
        if (cursorCol < 0) {
            cursorCol = 0;
            int pnl = cursor - 1;
            while (pnl >= 0 && atChar(pnl) != '\n') {
                pnl--;
            }
            cursorCol = cursor - pnl - 1;
        }
        return cursorCol;
    }

    /**
     * Issue <em>num</em> backspaces.
     *
     * @return the number of characters backed up
     */
    public int backspace(final int num) {
        int count = Math.max(Math.min(cursor, num), 0);
        moveGapToCursor();
        cursor -= count;
        g0 -= count;
        cursorCol = -1;
        return count;
    }

    /**
     * Issue a backspace.
     *
     * @return true if successful
     */
    public boolean backspace() {
        return backspace(1) == 1;
    }

    public int delete(int num) {
        int count = Math.max(Math.min(length() - cursor, num), 0);
        moveGapToCursor();
        g1 += count;
        cursorCol = -1;
        return count;
    }

    public boolean delete() {
        return delete(1) == 1;
    }

    @Override
    public String toString() {
        return substring(0, length());
    }

    public void copyFrom(Buffer buf) {
        if (!(buf instanceof BufferImpl)) {
            throw new IllegalStateException();
        }
        BufferImpl that = (BufferImpl) buf;
        this.g0 = that.g0;
        this.g1 = that.g1;
        this.buffer = that.buffer.clone();
        this.cursor = that.cursor;
        this.cursorCol = that.cursorCol;
    }

    private void moveGapToCursor() {
        if (cursor < g0) {
            int l = g0 - cursor;
            System.arraycopy(buffer, cursor, buffer, g1 - l, l);
            g0 -= l;
            g1 -= l;
        } else if (cursor > g0) {
            int l = cursor - g0;
            System.arraycopy(buffer, g1, buffer, g0, l);
            g0 += l;
            g1 += l;
        }
    }
}
