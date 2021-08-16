/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jdk.internal.org.jline.terminal.Terminal;
import jdk.internal.org.jline.utils.InfoCmp.Capability;

/**
 * Handle display and visual cursor.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class Display {

    /**OpenJDK:
     * When true, when the cursor is moved to column 0, carriage_return capability is not used,
     * but rather the cursor is moved left the appropriate number of positions.
     * This is useful when there's something already printed on the first line (which is not
     * specified as a prompt), for example when the user is providing an input.
     */
    public static boolean DISABLE_CR = false;

    protected final Terminal terminal;
    protected final boolean fullScreen;
    protected List<AttributedString> oldLines = Collections.emptyList();
    protected int cursorPos;
    private int columns;
    private int columns1; // columns+1
    protected int rows;
    protected boolean reset;
    protected boolean delayLineWrap;

    protected final Map<Capability, Integer> cost = new HashMap<>();
    protected final boolean canScroll;
    protected final boolean wrapAtEol;
    protected final boolean delayedWrapAtEol;
    protected final boolean cursorDownIsNewLine;

    public Display(Terminal terminal, boolean fullscreen) {
        this.terminal = terminal;
        this.fullScreen = fullscreen;

        this.canScroll = can(Capability.insert_line, Capability.parm_insert_line)
                            && can(Capability.delete_line, Capability.parm_delete_line);
        this.wrapAtEol = terminal.getBooleanCapability(Capability.auto_right_margin);
        this.delayedWrapAtEol = this.wrapAtEol
            && terminal.getBooleanCapability(Capability.eat_newline_glitch);
        this.cursorDownIsNewLine = "\n".equals(Curses.tputs(terminal.getStringCapability(Capability.cursor_down)));
    }

    /**
     * If cursor is at right margin, don't wrap immediately.
     * See <code>org.jline.reader.LineReader.Option#DELAY_LINE_WRAP</code>.
     * @return <code>true</code> if line wrap is delayed, <code>false</code> otherwise
     */
    public boolean delayLineWrap() {
        return delayLineWrap;
    }
    public void setDelayLineWrap(boolean v) { delayLineWrap = v; }

    public void resize(int rows, int columns) {
        if (this.rows != rows || this.columns != columns) {
            this.rows = rows;
            this.columns = columns;
            this.columns1 = columns + 1;
            oldLines = AttributedString.join(AttributedString.EMPTY, oldLines).columnSplitLength(columns, true, delayLineWrap());
        }
    }

    public void reset() {
        oldLines = Collections.emptyList();
    }

    /**
     * Clears the whole screen.
     * Use this method only when using full-screen / application mode.
     */
    public void clear() {
        if (fullScreen) {
            reset = true;
        }
    }

    public void updateAnsi(List<String> newLines, int targetCursorPos) {
        update(newLines.stream().map(AttributedString::fromAnsi).collect(Collectors.toList()), targetCursorPos);
    }

    /**
     * Update the display according to the new lines and flushes the output.
     * @param newLines the lines to display
     * @param targetCursorPos desired cursor position - see Size.cursorPos.
     */
    public void update(List<AttributedString> newLines, int targetCursorPos) {
        update(newLines, targetCursorPos, true);
    }

    /**
     * Update the display according to the new lines.
     * @param newLines the lines to display
     * @param targetCursorPos desired cursor position - see Size.cursorPos.
     * @param flush whether the output should be flushed or not
     */
    public void update(List<AttributedString> newLines, int targetCursorPos, boolean flush) {
        if (reset) {
            terminal.puts(Capability.clear_screen);
            oldLines.clear();
            cursorPos = 0;
            reset = false;
        }

        // If dumb display, get rid of ansi sequences now
        Integer cols = terminal.getNumericCapability(Capability.max_colors);
        if (cols == null || cols < 8) {
            newLines = newLines.stream().map(s -> new AttributedString(s.toString()))
                    .collect(Collectors.toList());
        }

        // Detect scrolling
        if ((fullScreen || newLines.size() >= rows) && newLines.size() == oldLines.size() && canScroll) {
            int nbHeaders = 0;
            int nbFooters = 0;
            // Find common headers and footers
            int l = newLines.size();
            while (nbHeaders < l
                   && Objects.equals(newLines.get(nbHeaders), oldLines.get(nbHeaders))) {
                nbHeaders++;
            }
            while (nbFooters < l - nbHeaders - 1
                    && Objects.equals(newLines.get(newLines.size() - nbFooters - 1), oldLines.get(oldLines.size() - nbFooters - 1))) {
                nbFooters++;
            }
            List<AttributedString> o1 = newLines.subList(nbHeaders, newLines.size() - nbFooters);
            List<AttributedString> o2 = oldLines.subList(nbHeaders, oldLines.size() - nbFooters);
            int[] common = longestCommon(o1, o2);
            if (common != null) {
                int s1 = common[0];
                int s2 = common[1];
                int sl = common[2];
                if (sl > 1 && s1 < s2) {
                    moveVisualCursorTo((nbHeaders + s1) * columns1);
                    int nb = s2 - s1;
                    deleteLines(nb);
                    for (int i = 0; i < nb; i++) {
                        oldLines.remove(nbHeaders + s1);
                    }
                    if (nbFooters > 0) {
                        moveVisualCursorTo((nbHeaders + s1 + sl) * columns1);
                        insertLines(nb);
                        for (int i = 0; i < nb; i++) {
                            oldLines.add(nbHeaders + s1 + sl, new AttributedString(""));
                        }
                    }
                } else if (sl > 1 && s1 > s2) {
                    int nb = s1 - s2;
                    if (nbFooters > 0) {
                        moveVisualCursorTo((nbHeaders + s2 + sl) * columns1);
                        deleteLines(nb);
                        for (int i = 0; i < nb; i++) {
                            oldLines.remove(nbHeaders + s2 + sl);
                        }
                    }
                    moveVisualCursorTo((nbHeaders + s2) * columns1);
                    insertLines(nb);
                    for (int i = 0; i < nb; i++) {
                        oldLines.add(nbHeaders + s2, new AttributedString(""));
                    }
                }
            }
        }

        int lineIndex = 0;
        int currentPos = 0;
        int numLines = Math.max(oldLines.size(), newLines.size());
        boolean wrapNeeded = false;
        while (lineIndex < numLines) {
            AttributedString oldLine =
                lineIndex < oldLines.size() ? oldLines.get(lineIndex)
                : AttributedString.NEWLINE;
            AttributedString newLine =
                 lineIndex < newLines.size() ? newLines.get(lineIndex)
                : AttributedString.NEWLINE;
            currentPos = lineIndex * columns1;
            int curCol = currentPos;
            int oldLength = oldLine.length();
            int newLength = newLine.length();
            boolean oldNL = oldLength > 0 && oldLine.charAt(oldLength-1)=='\n';
            boolean newNL = newLength > 0 && newLine.charAt(newLength-1)=='\n';
            if (oldNL) {
                oldLength--;
                oldLine = oldLine.substring(0, oldLength);
            }
            if (newNL) {
                newLength--;
                newLine = newLine.substring(0, newLength);
            }
            if (wrapNeeded
                && lineIndex == (cursorPos + 1) / columns1
                && lineIndex < newLines.size()) {
                // move from right margin to next line's left margin
                cursorPos++;
                if (newLength == 0 || newLine.isHidden(0)) {
                    // go to next line column zero
                    rawPrint(new AttributedString(" \b"));
                } else {
                    AttributedString firstChar = newLine.substring(0, 1);
                    // go to next line column one
                    rawPrint(firstChar);
                    cursorPos += firstChar.columnLength(); // normally 1
                    newLine = newLine.substring(1, newLength);
                    newLength--;
                    if (oldLength > 0) {
                        oldLine = oldLine.substring(1, oldLength);
                        oldLength--;
                    }
                    currentPos = cursorPos;
                }
            }
            List<DiffHelper.Diff> diffs = DiffHelper.diff(oldLine, newLine);
            boolean ident = true;
            boolean cleared = false;
            for (int i = 0; i < diffs.size(); i++) {
                DiffHelper.Diff diff = diffs.get(i);
                int width = diff.text.columnLength();
                switch (diff.operation) {
                    case EQUAL:
                        if (!ident) {
                            cursorPos = moveVisualCursorTo(currentPos);
                            rawPrint(diff.text);
                            cursorPos += width;
                            currentPos = cursorPos;
                        } else {
                            currentPos += width;
                        }
                        break;
                    case INSERT:
                        if (i <= diffs.size() - 2
                                && diffs.get(i + 1).operation == DiffHelper.Operation.EQUAL) {
                            cursorPos = moveVisualCursorTo(currentPos);
                            if (insertChars(width)) {
                                rawPrint(diff.text);
                                cursorPos += width;
                                currentPos = cursorPos;
                                break;
                            }
                        } else if (i <= diffs.size() - 2
                                && diffs.get(i + 1).operation == DiffHelper.Operation.DELETE
                                && width == diffs.get(i + 1).text.columnLength()) {
                            moveVisualCursorTo(currentPos);
                            rawPrint(diff.text);
                            cursorPos += width;
                            currentPos = cursorPos;
                            i++; // skip delete
                            break;
                        }
                        moveVisualCursorTo(currentPos);
                        rawPrint(diff.text);
                        cursorPos += width;
                        currentPos = cursorPos;
                        ident = false;
                        break;
                    case DELETE:
                        if (cleared) {
                            continue;
                        }
                        if (currentPos - curCol >= columns) {
                            continue;
                        }
                        if (i <= diffs.size() - 2
                                && diffs.get(i + 1).operation == DiffHelper.Operation.EQUAL) {
                            if (currentPos + diffs.get(i + 1).text.columnLength() < columns) {
                                moveVisualCursorTo(currentPos);
                                if (deleteChars(width)) {
                                    break;
                                }
                            }
                        }
                        int oldLen = oldLine.columnLength();
                        int newLen = newLine.columnLength();
                        int nb = Math.max(oldLen, newLen) - (currentPos - curCol);
                        moveVisualCursorTo(currentPos);
                        if (!terminal.puts(Capability.clr_eol)) {
                            rawPrint(' ', nb);
                            cursorPos += nb;
                        }
                        cleared = true;
                        ident = false;
                        break;
                }
            }
            lineIndex++;
            boolean newWrap = ! newNL && lineIndex < newLines.size();
            if (targetCursorPos + 1 == lineIndex * columns1
                && (newWrap || ! delayLineWrap))
                targetCursorPos++;
            boolean atRight = (cursorPos - curCol) % columns1 == columns;
            wrapNeeded = false;
            if (this.delayedWrapAtEol) {
                boolean oldWrap = ! oldNL && lineIndex < oldLines.size();
                if (newWrap != oldWrap && ! (oldWrap && cleared)) {
                    moveVisualCursorTo(lineIndex*columns1-1, newLines);
                    if (newWrap)
                        wrapNeeded = true;
                    else
                        terminal.puts(Capability.clr_eol);
                }
            } else if (atRight) {
                if (this.wrapAtEol) {
                    terminal.writer().write(" \b");
                    cursorPos++;
                } else {
                    terminal.puts(Capability.carriage_return); // CR / not newline.
                    cursorPos = curCol;
                }
                currentPos = cursorPos;
            }
        }
        if (cursorPos != targetCursorPos) {
            moveVisualCursorTo(targetCursorPos < 0 ? currentPos : targetCursorPos, newLines);
        }
        oldLines = newLines;

        if (flush) {
            terminal.flush();
        }
    }

    protected boolean deleteLines(int nb) {
        return perform(Capability.delete_line, Capability.parm_delete_line, nb);
    }

    protected boolean insertLines(int nb) {
        return perform(Capability.insert_line, Capability.parm_insert_line, nb);
    }

    protected boolean insertChars(int nb) {
        return perform(Capability.insert_character, Capability.parm_ich, nb);
    }

    protected boolean deleteChars(int nb) {
        return perform(Capability.delete_character, Capability.parm_dch, nb);
    }

    protected boolean can(Capability single, Capability multi) {
        return terminal.getStringCapability(single) != null
                || terminal.getStringCapability(multi) != null;
    }

    protected boolean perform(Capability single, Capability multi, int nb) {
        boolean hasMulti = terminal.getStringCapability(multi) != null;
        boolean hasSingle = terminal.getStringCapability(single) != null;
        if (hasMulti && (!hasSingle || cost(single) * nb > cost(multi))) {
            terminal.puts(multi, nb);
            return true;
        } else if (hasSingle) {
            for (int i = 0; i < nb; i++) {
                terminal.puts(single);
            }
            return true;
        } else {
            return false;
        }
    }

    private int cost(Capability cap) {
        return cost.computeIfAbsent(cap, this::computeCost);
    }

    private int computeCost(Capability cap) {
        String s = Curses.tputs(terminal.getStringCapability(cap), 0);
        return s != null ? s.length() : Integer.MAX_VALUE;
    }

    private static int[] longestCommon(List<AttributedString> l1, List<AttributedString> l2) {
        int start1 = 0;
        int start2 = 0;
        int max = 0;
        for (int i = 0; i < l1.size(); i++) {
            for (int j = 0; j < l2.size(); j++) {
                int x = 0;
                while (Objects.equals(l1.get(i + x), l2.get(j + x))) {
                    x++;
                    if (((i + x) >= l1.size()) || ((j + x) >= l2.size())) break;
                }
                if (x > max) {
                    max = x;
                    start1 = i;
                    start2 = j;
                }
            }
        }
        return max != 0 ? new int[] { start1, start2, max } : null;
    }

    /*
     * Move cursor from cursorPos to argument, updating cursorPos
     * We're at the right margin if {@code (cursorPos % columns1) == columns}.
     * This method knows how to move both *from* and *to* the right margin.
     */
    protected void moveVisualCursorTo(int targetPos,
                                      List<AttributedString> newLines) {
        if (cursorPos != targetPos) {
            boolean atRight = (targetPos % columns1) == columns;
            moveVisualCursorTo(targetPos - (atRight ? 1 : 0));
            if (atRight) {
                // There is no portable way to move to the right margin
                // except by writing a character in the right-most column.
                int row = targetPos / columns1;
                AttributedString lastChar = row >= newLines.size() ? AttributedString.EMPTY
                    : newLines.get(row).columnSubSequence(columns-1, columns);
                if (lastChar.length() == 0)
                    rawPrint((int) ' ');
                else
                    rawPrint(lastChar);
                cursorPos++;
            }
        }
    }

    /*
     * Move cursor from cursorPos to argument, updating cursorPos
     * We're at the right margin if {@code (cursorPos % columns1) == columns}.
     * This method knows how to move *from* the right margin,
     * but does not know how to move *to* the right margin.
     * I.e. {@code (i1 % columns1) == column} is not allowed.
     */
    protected int moveVisualCursorTo(int i1) {
        int i0 = cursorPos;
        if (i0 == i1) return i1;
        int width = columns1;
        int l0 = i0 / width;
        int c0 = i0 % width;
        int l1 = i1 / width;
        int c1 = i1 % width;
        if (c0 == columns) { // at right margin
            terminal.puts(Capability.carriage_return);
            c0 = 0;
        }
        if (l0 > l1) {
            perform(Capability.cursor_up, Capability.parm_up_cursor, l0 - l1);
        } else if (l0 < l1) {
            // TODO: clean the following
            if (fullScreen) {
                if (!terminal.puts(Capability.parm_down_cursor, l1 - l0)) {
                    for (int i = l0; i < l1; i++) {
                        terminal.puts(Capability.cursor_down);
                    }
                    if (cursorDownIsNewLine) {
                        c0 = 0;
                    }
                }
            } else {
                terminal.puts(Capability.carriage_return);
                rawPrint('\n', l1 - l0);
                c0 = 0;
            }
        }
        if (c0 != 0 && c1 == 0 && !DISABLE_CR) {
            terminal.puts(Capability.carriage_return);
        } else if (c0 < c1) {
            perform(Capability.cursor_right, Capability.parm_right_cursor, c1 - c0);
        } else if (c0 > c1) {
            perform(Capability.cursor_left, Capability.parm_left_cursor, c0 - c1);
        }
        cursorPos = i1;
        return i1;
    }

    void rawPrint(char c, int num) {
        for (int i = 0; i < num; i++) {
            rawPrint(c);
        }
    }

    void rawPrint(int c) {
        terminal.writer().write(c);
    }

    void rawPrint(AttributedString str) {
        str.print(terminal);
    }

    public int wcwidth(String str) {
        return str != null ? AttributedString.fromAnsi(str).columnLength() : 0;
    }

}
