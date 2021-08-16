/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.netbeans.jemmy.operators;

import java.awt.Container;
import java.util.Hashtable;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;

/**
 *
 * Class provides basic functions to operate with JTextArea (selection, typing,
 * deleting)
 *
 * <BR><BR>Timeouts used: <BR>
 * JTextComponentOperator.PushKeyTimeout - time between key pressing and
 * releasing during text typing <BR>
 * JTextComponentOperator.BetweenKeysTimeout - time to sleep between two chars
 * typing <BR>
 * JTextComponentOperator.ChangeCaretPositionTimeout - maximum time to chenge
 * caret position <BR>
 * JTextComponentOperator.TypeTextTimeout - maximum time to type text <BR>
 * ComponentOperator.WaitComponentTimeout - time to wait component displayed
 * <BR>
 * ComponentOperator.WaitFocusTimeout - time to wait component focus <BR>
 * JScrollBarOperator.OneScrollClickTimeout - time for one scroll click <BR>
 * JScrollBarOperator.WholeScrollTimeout - time for the whole scrolling <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class JTextAreaOperator extends JTextComponentOperator
        implements Timeoutable, Outputable {

    /**
     * Identifier for a "column count" property.
     *
     * @see #getDump
     */
    public static final String COLUMN_COUNT_DPROP = "Column count";

    /**
     * Identifier for a "row count" property.
     *
     * @see #getDump
     */
    public static final String ROW_COUNT_DPROP = "Row count";

    private Timeouts timeouts;
    private TestOut output;

    /**
     * Constructor.
     *
     * @param b a component
     */
    public JTextAreaOperator(JTextArea b) {
        super(b);
    }

    /**
     * Constructs a JTextAreaOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JTextAreaOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((JTextArea) cont.
                waitSubComponent(new JTextAreaFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JTextAreaOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public JTextAreaOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Button text.
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JTextAreaOperator(ContainerOperator<?> cont, String text, int index) {
        this((JTextArea) waitComponent(cont,
                new JTextAreaFinder(new JTextComponentOperator.JTextComponentByTextFinder(text,
                        cont.getComparator())),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Button text.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JTextAreaOperator(ContainerOperator<?> cont, String text) {
        this(cont, text, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param index Ordinal component index.
     * @throws TimeoutExpiredException
     */
    public JTextAreaOperator(ContainerOperator<?> cont, int index) {
        this((JTextArea) waitComponent(cont,
                new JTextAreaFinder(),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @throws TimeoutExpiredException
     */
    public JTextAreaOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches JTextArea in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @param index Ordinal component index.
     * @return JTextArea instance or null if component was not found.
     */
    public static JTextArea findJTextArea(Container cont, ComponentChooser chooser, int index) {
        return (JTextArea) findJTextComponent(cont, new JTextAreaFinder(chooser), index);
    }

    /**
     * Searches JTextArea in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @return JTextArea instance or null if component was not found.
     */
    public static JTextArea findJTextArea(Container cont, ComponentChooser chooser) {
        return findJTextArea(cont, chooser, 0);
    }

    /**
     * Searches JTextArea by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return JTextArea instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JTextArea findJTextArea(Container cont, String text, boolean ce, boolean ccs, int index) {
        return (findJTextArea(cont,
                new JTextAreaFinder(new JTextComponentOperator.JTextComponentByTextFinder(text,
                        new DefaultStringComparator(ce, ccs))),
                index));
    }

    /**
     * Searches JTextArea by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return JTextArea instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JTextArea findJTextArea(Container cont, String text, boolean ce, boolean ccs) {
        return findJTextArea(cont, text, ce, ccs, 0);
    }

    /**
     * Waits JTextArea in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @param index Ordinal component index.
     * @return JTextArea instance.
     * @throws TimeoutExpiredException
     */
    public static JTextArea waitJTextArea(Container cont, ComponentChooser chooser, int index) {
        return (JTextArea) waitJTextComponent(cont, new JTextAreaFinder(chooser), index);
    }

    /**
     * Waits JTextArea in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @return JTextArea instance.
     * @throws TimeoutExpiredException
     */
    public static JTextArea waitJTextArea(Container cont, ComponentChooser chooser) {
        return waitJTextArea(cont, chooser, 0);
    }

    /**
     * Waits JTextArea by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return JTextArea instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JTextArea waitJTextArea(Container cont, String text, boolean ce, boolean ccs, int index) {
        return (waitJTextArea(cont,
                new JTextAreaFinder(new JTextComponentOperator.JTextComponentByTextFinder(text,
                        new DefaultStringComparator(ce, ccs))),
                index));
    }

    /**
     * Waits JTextArea by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return JTextArea instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JTextArea waitJTextArea(Container cont, String text, boolean ce, boolean ccs) {
        return waitJTextArea(cont, text, ce, ccs, 0);
    }

    @Override
    public void setTimeouts(Timeouts times) {
        timeouts = times;
        super.setTimeouts(timeouts);
    }

    @Override
    public Timeouts getTimeouts() {
        return timeouts;
    }

    @Override
    public void setOutput(TestOut out) {
        output = out;
        super.setOutput(output.createErrorOutput());
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    /**
     * Notifies whether "PageUp" and "PageDown" should be used to change caret
     * position. If can be useful if text takes some pages.
     *
     * @param yesOrNo if page navigation keys need to be used.
     * @deprecated All text operations are performed by TextDriver regitered for
     * this operator type.
     */
    @Deprecated
    public void usePageNavigationKeys(boolean yesOrNo) {
    }

    /**
     * Moves caret to line.
     *
     * @param row Line to move caret to.
     * @see JTextComponentOperator#changeCaretPosition(int)
     * @see #changeCaretPosition(int)
     * @see #changeCaretPosition(int, int)
     * @throws TimeoutExpiredException
     */
    public void changeCaretRow(int row) {
        changeCaretPosition(row, getCaretPosition()
                - getLineStartOffset(getLineOfOffset(getCaretPosition())));
    }

    /**
     * Moves caret.
     *
     * @param row Line to move caret to.
     * @param column Column to move caret to.
     * @see JTextComponentOperator#changeCaretPosition(int)
     * @see #changeCaretRow(int)
     * @see #changeCaretPosition(int, int)
     * @throws TimeoutExpiredException
     */
    public void changeCaretPosition(int row, int column) {
        int startOffset = getLineStartOffset(row);
        int endOffset = getLineEndOffset(row);
        super.changeCaretPosition(getLineStartOffset(row)
                + ((column <= (endOffset - startOffset))
                        ? column
                        : (endOffset - startOffset)));
    }

    /**
     * Types text.
     *
     * @param text Text to be typed.
     * @param row Line to type text in.
     * @param column Column to type text from.
     * @see JTextComponentOperator#typeText(String, int)
     * @throws TimeoutExpiredException
     */
    public void typeText(String text, int row, int column) {
        if (!hasFocus()) {
            makeComponentVisible();
        }
        changeCaretPosition(row, column);
        typeText(text);
    }

    /**
     * Select a part of text.
     *
     * @param startRow Start position row.
     * @param startColumn Start position column.
     * @param endRow End position row.
     * @param endColumn End position column.
     * @see JTextComponentOperator#selectText(int, int)
     * @see #selectLines(int, int)
     * @throws TimeoutExpiredException
     */
    public void selectText(int startRow, int startColumn,
            int endRow, int endColumn) {
        int startPos = 0;
        try {
            startPos = getLineStartOffset(startRow) + startColumn;
        } catch (JemmyException e) {
            if (!(e.getInnerThrowable() instanceof BadLocationException)) {
                throw (e);
            }
        }
        int endPos = getText().length();
        try {
            endPos = getLineStartOffset(endRow) + endColumn;
        } catch (JemmyException e) {
            if (!(e.getInnerThrowable() instanceof BadLocationException)) {
                throw (e);
            }
        }
        selectText(startPos, endPos);
    }

    /**
     * Select some text lines.
     *
     * @param startLine start selection
     * @param endLine end selection
     * @see JTextComponentOperator#selectText(int, int)
     * @see #selectText(int, int, int, int)
     * @throws TimeoutExpiredException
     */
    public void selectLines(int startLine, int endLine) {
        if (!hasFocus()) {
            makeComponentVisible();
        }
        selectText(startLine, 0, endLine + 1, 0);
    }

    /**
     * Returns information about component.
     */
    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        result.put(COLUMN_COUNT_DPROP, Integer.toString(((JTextArea) getSource()).getRows()));
        result.put(ROW_COUNT_DPROP, Integer.toString(((JTextArea) getSource()).getColumns()));
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code JTextArea.append(String)} through queue
     */
    public void append(final String string) {
        runMapping(new MapVoidAction("append") {
            @Override
            public void map() {
                ((JTextArea) getSource()).append(string);
            }
        });
    }

    /**
     * Maps {@code JTextArea.getColumns()} through queue
     */
    public int getColumns() {
        return (runMapping(new MapIntegerAction("getColumns") {
            @Override
            public int map() {
                return ((JTextArea) getSource()).getColumns();
            }
        }));
    }

    /**
     * Maps {@code JTextArea.getLineCount()} through queue
     */
    public int getLineCount() {
        return (runMapping(new MapIntegerAction("getLineCount") {
            @Override
            public int map() {
                return ((JTextArea) getSource()).getLineCount();
            }
        }));
    }

    /**
     * Maps {@code JTextArea.getLineEndOffset(int)} through queue
     */
    public int getLineEndOffset(final int i) {
        return (runMapping(new MapIntegerAction("getLineEndOffset") {
            @Override
            public int map() throws BadLocationException {
                return ((JTextArea) getSource()).getLineEndOffset(i);
            }
        }));
    }

    /**
     * Maps {@code JTextArea.getLineOfOffset(int)} through queue
     */
    public int getLineOfOffset(final int i) {
        return (runMapping(new MapIntegerAction("getLineOfOffset") {
            @Override
            public int map() throws BadLocationException {
                return ((JTextArea) getSource()).getLineOfOffset(i);
            }
        }));
    }

    /**
     * Maps {@code JTextArea.getLineStartOffset(int)} through queue
     */
    public int getLineStartOffset(final int i) {
        return (runMapping(new MapIntegerAction("getLineStartOffset") {
            @Override
            public int map() throws BadLocationException {
                return ((JTextArea) getSource()).getLineStartOffset(i);
            }
        }));
    }

    /**
     * Maps {@code JTextArea.getLineWrap()} through queue
     */
    public boolean getLineWrap() {
        return (runMapping(new MapBooleanAction("getLineWrap") {
            @Override
            public boolean map() {
                return ((JTextArea) getSource()).getLineWrap();
            }
        }));
    }

    /**
     * Maps {@code JTextArea.getRows()} through queue
     */
    public int getRows() {
        return (runMapping(new MapIntegerAction("getRows") {
            @Override
            public int map() {
                return ((JTextArea) getSource()).getRows();
            }
        }));
    }

    /**
     * Maps {@code JTextArea.getTabSize()} through queue
     */
    public int getTabSize() {
        return (runMapping(new MapIntegerAction("getTabSize") {
            @Override
            public int map() {
                return ((JTextArea) getSource()).getTabSize();
            }
        }));
    }

    /**
     * Maps {@code JTextArea.getWrapStyleWord()} through queue
     */
    public boolean getWrapStyleWord() {
        return (runMapping(new MapBooleanAction("getWrapStyleWord") {
            @Override
            public boolean map() {
                return ((JTextArea) getSource()).getWrapStyleWord();
            }
        }));
    }

    /**
     * Maps {@code JTextArea.insert(String, int)} through queue
     */
    public void insert(final String string, final int i) {
        runMapping(new MapVoidAction("insert") {
            @Override
            public void map() {
                ((JTextArea) getSource()).insert(string, i);
            }
        });
    }

    /**
     * Maps {@code JTextArea.replaceRange(String, int, int)} through queue
     */
    public void replaceRange(final String string, final int i, final int i1) {
        runMapping(new MapVoidAction("replaceRange") {
            @Override
            public void map() {
                ((JTextArea) getSource()).replaceRange(string, i, i1);
            }
        });
    }

    /**
     * Maps {@code JTextArea.setColumns(int)} through queue
     */
    public void setColumns(final int i) {
        runMapping(new MapVoidAction("setColumns") {
            @Override
            public void map() {
                ((JTextArea) getSource()).setColumns(i);
            }
        });
    }

    /**
     * Maps {@code JTextArea.setLineWrap(boolean)} through queue
     */
    public void setLineWrap(final boolean b) {
        runMapping(new MapVoidAction("setLineWrap") {
            @Override
            public void map() {
                ((JTextArea) getSource()).setLineWrap(b);
            }
        });
    }

    /**
     * Maps {@code JTextArea.setRows(int)} through queue
     */
    public void setRows(final int i) {
        runMapping(new MapVoidAction("setRows") {
            @Override
            public void map() {
                ((JTextArea) getSource()).setRows(i);
            }
        });
    }

    /**
     * Maps {@code JTextArea.setTabSize(int)} through queue
     */
    public void setTabSize(final int i) {
        runMapping(new MapVoidAction("setTabSize") {
            @Override
            public void map() {
                ((JTextArea) getSource()).setTabSize(i);
            }
        });
    }

    /**
     * Maps {@code JTextArea.setWrapStyleWord(boolean)} through queue
     */
    public void setWrapStyleWord(final boolean b) {
        runMapping(new MapVoidAction("setWrapStyleWord") {
            @Override
            public void map() {
                ((JTextArea) getSource()).setWrapStyleWord(b);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * Checks component type.
     */
    public static class JTextAreaFinder extends Finder {

        /**
         * Constructs JTextAreaFinder.
         *
         * @param sf other searching criteria.
         */
        public JTextAreaFinder(ComponentChooser sf) {
            super(JTextArea.class, sf);
        }

        /**
         * Constructs JTextAreaFinder.
         */
        public JTextAreaFinder() {
            super(JTextArea.class);
        }
    }
}
