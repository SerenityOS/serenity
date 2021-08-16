/*
 * Copyright (c) 2000, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
package javax.swing.text;

import sun.reflect.misc.ReflectUtil;
import sun.swing.SwingUtilities2;

import java.io.Serializable;
import java.lang.reflect.*;
import java.text.ParseException;
import javax.swing.*;

/**
 * <code>DefaultFormatter</code> formats arbitrary objects. Formatting is done
 * by invoking the <code>toString</code> method. In order to convert the
 * value back to a String, your class must provide a constructor that
 * takes a String argument. If no single argument constructor that takes a
 * String is found, the returned value will be the String passed into
 * <code>stringToValue</code>.
 * <p>
 * Instances of <code>DefaultFormatter</code> can not be used in multiple
 * instances of <code>JFormattedTextField</code>. To obtain a copy of
 * an already configured <code>DefaultFormatter</code>, use the
 * <code>clone</code> method.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @see javax.swing.JFormattedTextField.AbstractFormatter
 *
 * @since 1.4
 */
@SuppressWarnings("serial") // Same-version serialization only
public class DefaultFormatter extends JFormattedTextField.AbstractFormatter
                    implements Cloneable, Serializable {
    /** Indicates if the value being edited must match the mask. */
    private boolean allowsInvalid;

    /** If true, editing mode is in overwrite (or strikethough). */
    private boolean overwriteMode;

    /** If true, any time a valid edit happens commitEdit is invoked. */
    private boolean commitOnEdit;

    /** Class used to create new instances. */
    private Class<?> valueClass;

    /** NavigationFilter that forwards calls back to DefaultFormatter. */
    private NavigationFilter navigationFilter;

    /** DocumentFilter that forwards calls back to DefaultFormatter. */
    private DocumentFilter documentFilter;

    /** Used during replace to track the region to replace. */
    transient ReplaceHolder replaceHolder;


    /**
     * Creates a DefaultFormatter.
     */
    public DefaultFormatter() {
        overwriteMode = true;
        allowsInvalid = true;
    }

    /**
     * Installs the <code>DefaultFormatter</code> onto a particular
     * <code>JFormattedTextField</code>.
     * This will invoke <code>valueToString</code> to convert the
     * current value from the <code>JFormattedTextField</code> to
     * a String. This will then install the <code>Action</code>s from
     * <code>getActions</code>, the <code>DocumentFilter</code>
     * returned from <code>getDocumentFilter</code> and the
     * <code>NavigationFilter</code> returned from
     * <code>getNavigationFilter</code> onto the
     * <code>JFormattedTextField</code>.
     * <p>
     * Subclasses will typically only need to override this if they
     * wish to install additional listeners on the
     * <code>JFormattedTextField</code>.
     * <p>
     * If there is a <code>ParseException</code> in converting the
     * current value to a String, this will set the text to an empty
     * String, and mark the <code>JFormattedTextField</code> as being
     * in an invalid state.
     * <p>
     * While this is a public method, this is typically only useful
     * for subclassers of <code>JFormattedTextField</code>.
     * <code>JFormattedTextField</code> will invoke this method at
     * the appropriate times when the value changes, or its internal
     * state changes.
     *
     * @param ftf JFormattedTextField to format for, may be null indicating
     *            uninstall from current JFormattedTextField.
     */
    public void install(JFormattedTextField ftf) {
        super.install(ftf);
        positionCursorAtInitialLocation();
    }

    /**
     * Sets when edits are published back to the
     * <code>JFormattedTextField</code>. If true, <code>commitEdit</code>
     * is invoked after every valid edit (any time the text is edited). On
     * the other hand, if this is false than the <code>DefaultFormatter</code>
     * does not publish edits back to the <code>JFormattedTextField</code>.
     * As such, the only time the value of the <code>JFormattedTextField</code>
     * will change is when <code>commitEdit</code> is invoked on
     * <code>JFormattedTextField</code>, typically when enter is pressed
     * or focus leaves the <code>JFormattedTextField</code>.
     *
     * @param commit Used to indicate when edits are committed back to the
     *               JTextComponent
     */
    public void setCommitsOnValidEdit(boolean commit) {
        commitOnEdit = commit;
    }

    /**
     * Returns when edits are published back to the
     * <code>JFormattedTextField</code>.
     *
     * @return true if edits are committed after every valid edit
     */
    public boolean getCommitsOnValidEdit() {
        return commitOnEdit;
    }

    /**
     * Configures the behavior when inserting characters. If
     * <code>overwriteMode</code> is true (the default), new characters
     * overwrite existing characters in the model.
     *
     * @param overwriteMode Indicates if overwrite or overstrike mode is used
     */
    public void setOverwriteMode(boolean overwriteMode) {
        this.overwriteMode = overwriteMode;
    }

    /**
     * Returns the behavior when inserting characters.
     *
     * @return true if newly inserted characters overwrite existing characters
     */
    public boolean getOverwriteMode() {
        return overwriteMode;
    }

    /**
     * Sets whether or not the value being edited is allowed to be invalid
     * for a length of time (that is, <code>stringToValue</code> throws
     * a <code>ParseException</code>).
     * It is often convenient to allow the user to temporarily input an
     * invalid value.
     *
     * @param allowsInvalid Used to indicate if the edited value must always
     *        be valid
     */
    public void setAllowsInvalid(boolean allowsInvalid) {
        this.allowsInvalid = allowsInvalid;
    }

    /**
     * Returns whether or not the value being edited is allowed to be invalid
     * for a length of time.
     *
     * @return false if the edited value must always be valid
     */
    public boolean getAllowsInvalid() {
        return allowsInvalid;
    }

    /**
     * Sets that class that is used to create new Objects. If the
     * passed in class does not have a single argument constructor that
     * takes a String, String values will be used.
     *
     * @param valueClass Class used to construct return value from
     *        stringToValue
     */
    public void setValueClass(Class<?> valueClass) {
        this.valueClass = valueClass;
    }

    /**
     * Returns that class that is used to create new Objects.
     *
     * @return Class used to construct return value from stringToValue
     */
    public Class<?> getValueClass() {
        return valueClass;
    }

    /**
     * Converts the passed in String into an instance of
     * <code>getValueClass</code> by way of the constructor that
     * takes a String argument. If <code>getValueClass</code>
     * returns null, the Class of the current value in the
     * <code>JFormattedTextField</code> will be used. If this is null, a
     * String will be returned. If the constructor throws an exception, a
     * <code>ParseException</code> will be thrown. If there is no single
     * argument String constructor, <code>string</code> will be returned.
     *
     * @throws ParseException if there is an error in the conversion
     * @param string String to convert
     * @return Object representation of text
     */
    public Object stringToValue(String string) throws ParseException {
        Class<?> vc = getValueClass();
        JFormattedTextField ftf = getFormattedTextField();

        if (vc == null && ftf != null) {
            Object value = ftf.getValue();

            if (value != null) {
                vc = value.getClass();
            }
        }
        if (vc != null) {
            Constructor<?> cons;

            try {
                ReflectUtil.checkPackageAccess(vc);
                SwingUtilities2.checkAccess(vc.getModifiers());
                cons = vc.getConstructor(new Class<?>[]{String.class});

            } catch (NoSuchMethodException nsme) {
                cons = null;
            }

            if (cons != null) {
                try {
                    SwingUtilities2.checkAccess(cons.getModifiers());
                    return cons.newInstance(new Object[] { string });
                } catch (Throwable ex) {
                    throw new ParseException("Error creating instance", 0);
                }
            }
        }
        return string;
    }

    /**
     * Converts the passed in Object into a String by way of the
     * <code>toString</code> method.
     *
     * @throws ParseException if there is an error in the conversion
     * @param value Value to convert
     * @return String representation of value
     */
    public String valueToString(Object value) throws ParseException {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    /**
     * Returns the <code>DocumentFilter</code> used to restrict the characters
     * that can be input into the <code>JFormattedTextField</code>.
     *
     * @return DocumentFilter to restrict edits
     */
    protected DocumentFilter getDocumentFilter() {
        if (documentFilter == null) {
            documentFilter = new DefaultDocumentFilter();
        }
        return documentFilter;
    }

    /**
     * Returns the <code>NavigationFilter</code> used to restrict where the
     * cursor can be placed.
     *
     * @return NavigationFilter to restrict navigation
     */
    protected NavigationFilter getNavigationFilter() {
        if (navigationFilter == null) {
            navigationFilter = new DefaultNavigationFilter();
        }
        return navigationFilter;
    }

    /**
     * Creates a copy of the DefaultFormatter.
     *
     * @return copy of the DefaultFormatter
     */
    public Object clone() throws CloneNotSupportedException {
        DefaultFormatter formatter = (DefaultFormatter)super.clone();

        formatter.navigationFilter = null;
        formatter.documentFilter = null;
        formatter.replaceHolder = null;
        return formatter;
    }


    /**
     * Positions the cursor at the initial location.
     */
    void positionCursorAtInitialLocation() {
        JFormattedTextField ftf = getFormattedTextField();
        if (ftf != null) {
            ftf.setCaretPosition(getInitialVisualPosition());
        }
    }

    /**
     * Returns the initial location to position the cursor at. This forwards
     * the call to <code>getNextNavigatableChar</code>.
     */
    int getInitialVisualPosition() {
        return getNextNavigatableChar(0, 1);
    }

    /**
     * Subclasses should override this if they want cursor navigation
     * to skip certain characters. A return value of false indicates
     * the character at <code>offset</code> should be skipped when
     * navigating throught the field.
     */
    boolean isNavigatable(int offset) {
        return true;
    }

    /**
     * Returns true if the text in <code>text</code> can be inserted.  This
     * does not mean the text will ultimately be inserted, it is used if
     * text can trivially reject certain characters.
     */
    boolean isLegalInsertText(String text) {
        return true;
    }

    /**
     * Returns the next editable character starting at offset incrementing
     * the offset by <code>direction</code>.
     */
    private int getNextNavigatableChar(int offset, int direction) {
        int max = getFormattedTextField().getDocument().getLength();

        while (offset >= 0 && offset < max) {
            if (isNavigatable(offset)) {
                return offset;
            }
            offset += direction;
        }
        return offset;
    }

    /**
     * A convenience methods to return the result of deleting
     * <code>deleteLength</code> characters at <code>offset</code>
     * and inserting <code>replaceString</code> at <code>offset</code>
     * in the current text field.
     */
    String getReplaceString(int offset, int deleteLength,
                            String replaceString) {
        String string = getFormattedTextField().getText();
        String result;

        result = string.substring(0, offset);
        if (replaceString != null) {
            result += replaceString;
        }
        if (offset + deleteLength < string.length()) {
            result += string.substring(offset + deleteLength);
        }
        return result;
    }

    /*
     * Returns true if the operation described by <code>rh</code> will
     * result in a legal edit.  This may set the <code>value</code>
     * field of <code>rh</code>.
     */
    boolean isValidEdit(ReplaceHolder rh) {
        if (!getAllowsInvalid()) {
            String newString = getReplaceString(rh.offset, rh.length, rh.text);

            try {
                rh.value = stringToValue(newString);

                return true;
            } catch (ParseException pe) {
                return false;
            }
        }
        return true;
    }

    /**
     * Invokes <code>commitEdit</code> on the JFormattedTextField.
     */
    void commitEdit() throws ParseException {
        JFormattedTextField ftf = getFormattedTextField();

        if (ftf != null) {
            ftf.commitEdit();
        }
    }

    /**
     * Pushes the value to the JFormattedTextField if the current value
     * is valid and invokes <code>setEditValid</code> based on the
     * validity of the value.
     */
    void updateValue() {
        updateValue(null);
    }

    /**
     * Pushes the <code>value</code> to the editor if we are to
     * commit on edits. If <code>value</code> is null, the current value
     * will be obtained from the text component.
     */
    void updateValue(Object value) {
        try {
            if (value == null) {
                String string = getFormattedTextField().getText();

                value = stringToValue(string);
            }

            if (getCommitsOnValidEdit()) {
                commitEdit();
            }
            setEditValid(true);
        } catch (ParseException pe) {
            setEditValid(false);
        }
    }

    /**
     * Returns the next cursor position from offset by incrementing
     * <code>direction</code>. This uses
     * <code>getNextNavigatableChar</code>
     * as well as constraining the location to the max position.
     */
    int getNextCursorPosition(int offset, int direction) {
        int newOffset = getNextNavigatableChar(offset, direction);
        int max = getFormattedTextField().getDocument().getLength();

        if (!getAllowsInvalid()) {
            if (direction == -1 && offset == newOffset) {
                // Case where hit backspace and only characters before
                // offset are fixed.
                newOffset = getNextNavigatableChar(newOffset, 1);
                if (newOffset >= max) {
                    newOffset = offset;
                }
            }
            else if (direction == 1 && newOffset >= max) {
                // Don't go beyond last editable character.
                newOffset = getNextNavigatableChar(max - 1, -1);
                if (newOffset < max) {
                    newOffset++;
                }
            }
        }
        return newOffset;
    }

    /**
     * Resets the cursor by using getNextCursorPosition.
     */
    void repositionCursor(int offset, int direction) {
        getFormattedTextField().getCaret().setDot(getNextCursorPosition
                                                  (offset, direction));
    }


    /**
     * Finds the next navigable character.
     */
    int getNextVisualPositionFrom(JTextComponent text, int pos,
                                  Position.Bias bias, int direction,
                                  Position.Bias[] biasRet)
                                           throws BadLocationException {
        int value = text.getUI().getNextVisualPositionFrom(text, pos, bias,
                                                           direction, biasRet);

        if (value == -1) {
            return -1;
        }
        if (!getAllowsInvalid() && (direction == SwingConstants.EAST ||
                                    direction == SwingConstants.WEST)) {
            int last = -1;

            while (!isNavigatable(value) && value != last) {
                last = value;
                value = text.getUI().getNextVisualPositionFrom(
                              text, value, bias, direction,biasRet);
            }
            int max = getFormattedTextField().getDocument().getLength();
            if (last == value || value == max) {
                if (value == 0) {
                    biasRet[0] = Position.Bias.Forward;
                    value = getInitialVisualPosition();
                }
                if (value >= max && max > 0) {
                    // Pending: should not assume forward!
                    biasRet[0] = Position.Bias.Forward;
                    value = getNextNavigatableChar(max - 1, -1) + 1;
                }
            }
        }
        return value;
    }

    /**
     * Returns true if the edit described by <code>rh</code> will result
     * in a legal value.
     */
    boolean canReplace(ReplaceHolder rh) {
        return isValidEdit(rh);
    }

    /**
     * DocumentFilter method, funnels into <code>replace</code>.
     */
    void replace(DocumentFilter.FilterBypass fb, int offset,
                     int length, String text,
                     AttributeSet attrs) throws BadLocationException {
        ReplaceHolder rh = getReplaceHolder(fb, offset, length, text, attrs);

        replace(rh);
    }

    /**
     * If the edit described by <code>rh</code> is legal, this will
     * return true, commit the edit (if necessary) and update the cursor
     * position.  This forwards to <code>canReplace</code> and
     * <code>isLegalInsertText</code> as necessary to determine if
     * the edit is in fact legal.
     * <p>
     * All of the DocumentFilter methods funnel into here, you should
     * generally only have to override this.
     */
    boolean replace(ReplaceHolder rh) throws BadLocationException {
        boolean valid = true;
        int direction = 1;

        if (rh.length > 0 && (rh.text == null || rh.text.length() == 0) &&
               (getFormattedTextField().getSelectionStart() != rh.offset ||
                   rh.length > 1)) {
            direction = -1;
        }

        if (getOverwriteMode() && rh.text != null &&
            getFormattedTextField().getSelectedText() == null)
        {
            rh.length = Math.min(Math.max(rh.length, rh.text.length()),
                                 rh.fb.getDocument().getLength() - rh.offset);
        }
        if ((rh.text != null && !isLegalInsertText(rh.text)) ||
            !canReplace(rh) ||
            (rh.length == 0 && (rh.text == null || rh.text.length() == 0))) {
            valid = false;
        }
        if (valid) {
            int cursor = rh.cursorPosition;

            rh.fb.replace(rh.offset, rh.length, rh.text, rh.attrs);
            if (cursor == -1) {
                cursor = rh.offset;
                if (direction == 1 && rh.text != null) {
                    cursor = rh.offset + rh.text.length();
                }
            }
            updateValue(rh.value);
            repositionCursor(cursor, direction);
            return true;
        }
        else {
            invalidEdit();
        }
        return false;
    }

    /**
     * NavigationFilter method, subclasses that wish finer control should
     * override this.
     */
    void setDot(NavigationFilter.FilterBypass fb, int dot, Position.Bias bias){
        fb.setDot(dot, bias);
    }

    /**
     * NavigationFilter method, subclasses that wish finer control should
     * override this.
     */
    void moveDot(NavigationFilter.FilterBypass fb, int dot,
                 Position.Bias bias) {
        fb.moveDot(dot, bias);
    }


    /**
     * Returns the ReplaceHolder to track the replace of the specified
     * text.
     */
    ReplaceHolder getReplaceHolder(DocumentFilter.FilterBypass fb, int offset,
                                   int length, String text,
                                   AttributeSet attrs) {
        if (replaceHolder == null) {
            replaceHolder = new ReplaceHolder();
        }
        replaceHolder.reset(fb, offset, length, text, attrs);
        return replaceHolder;
    }


    /**
     * ReplaceHolder is used to track where insert/remove/replace is
     * going to happen.
     */
    static class ReplaceHolder {
        /** The FilterBypass that was passed to the DocumentFilter method. */
        DocumentFilter.FilterBypass fb;
        /** Offset where the remove/insert is going to occur. */
        int offset;
        /** Length of text to remove. */
        int length;
        /** The text to insert, may be null. */
        String text;
        /** AttributeSet to attach to text, may be null. */
        AttributeSet attrs;
        /** The resulting value, this may never be set. */
        Object value;
        /** Position the cursor should be adjusted from.  If this is -1
         * the cursor position will be adjusted based on the direction of
         * the replace (-1: offset, 1: offset + text.length()), otherwise
         * the cursor position is adusted from this position.
         */
        int cursorPosition;

        void reset(DocumentFilter.FilterBypass fb, int offset, int length,
                   String text, AttributeSet attrs) {
            this.fb = fb;
            this.offset = offset;
            this.length = length;
            this.text = text;
            this.attrs = attrs;
            this.value = null;
            cursorPosition = -1;
        }
    }


    /**
     * NavigationFilter implementation that calls back to methods with
     * same name in DefaultFormatter.
     */
    private class DefaultNavigationFilter extends NavigationFilter
                             implements Serializable {
        public void setDot(FilterBypass fb, int dot, Position.Bias bias) {
            JTextComponent tc = DefaultFormatter.this.getFormattedTextField();
            if (tc.composedTextExists()) {
                // bypass the filter
                fb.setDot(dot, bias);
            } else {
                DefaultFormatter.this.setDot(fb, dot, bias);
            }
        }

        public void moveDot(FilterBypass fb, int dot, Position.Bias bias) {
            JTextComponent tc = DefaultFormatter.this.getFormattedTextField();
            if (tc.composedTextExists()) {
                // bypass the filter
                fb.moveDot(dot, bias);
            } else {
                DefaultFormatter.this.moveDot(fb, dot, bias);
            }
        }

        public int getNextVisualPositionFrom(JTextComponent text, int pos,
                                             Position.Bias bias,
                                             int direction,
                                             Position.Bias[] biasRet)
                                           throws BadLocationException {
            if (text.composedTextExists()) {
                // forward the call to the UI directly
                return text.getUI().getNextVisualPositionFrom(
                        text, pos, bias, direction, biasRet);
            } else {
                return DefaultFormatter.this.getNextVisualPositionFrom(
                        text, pos, bias, direction, biasRet);
            }
        }
    }


    /**
     * DocumentFilter implementation that calls back to the replace
     * method of DefaultFormatter.
     */
    private class DefaultDocumentFilter extends DocumentFilter implements
                             Serializable {
        public void remove(FilterBypass fb, int offset, int length) throws
                              BadLocationException {
            JTextComponent tc = DefaultFormatter.this.getFormattedTextField();
            if (tc.composedTextExists()) {
                // bypass the filter
                fb.remove(offset, length);
            } else {
                DefaultFormatter.this.replace(fb, offset, length, null, null);
            }
        }

        public void insertString(FilterBypass fb, int offset,
                                 String string, AttributeSet attr) throws
                              BadLocationException {
            JTextComponent tc = DefaultFormatter.this.getFormattedTextField();
            if (tc.composedTextExists() ||
                Utilities.isComposedTextAttributeDefined(attr)) {
                // bypass the filter
                fb.insertString(offset, string, attr);
            } else {
                DefaultFormatter.this.replace(fb, offset, 0, string, attr);
            }
        }

        public void replace(FilterBypass fb, int offset, int length,
                                 String text, AttributeSet attr) throws
                              BadLocationException {
            JTextComponent tc = DefaultFormatter.this.getFormattedTextField();
            if (tc.composedTextExists() ||
                Utilities.isComposedTextAttributeDefined(attr)) {
                // bypass the filter
                fb.replace(offset, length, text, attr);
            } else {
                DefaultFormatter.this.replace(fb, offset, length, text, attr);
            }
        }
    }
}
