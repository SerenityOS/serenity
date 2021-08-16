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

import java.awt.event.ActionEvent;
import java.io.*;
import java.text.*;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.*;
import javax.swing.*;

/**
 * <code>InternationalFormatter</code> extends <code>DefaultFormatter</code>,
 * using an instance of <code>java.text.Format</code> to handle the
 * conversion to a String, and the conversion from a String.
 * <p>
 * If <code>getAllowsInvalid()</code> is false, this will ask the
 * <code>Format</code> to format the current text on every edit.
 * <p>
 * You can specify a minimum and maximum value by way of the
 * <code>setMinimum</code> and <code>setMaximum</code> methods. In order
 * for this to work the values returned from <code>stringToValue</code> must be
 * comparable to the min/max values by way of the <code>Comparable</code>
 * interface.
 * <p>
 * Be careful how you configure the <code>Format</code> and the
 * <code>InternationalFormatter</code>, as it is possible to create a
 * situation where certain values can not be input. Consider the date
 * format 'M/d/yy', an <code>InternationalFormatter</code> that is always
 * valid (<code>setAllowsInvalid(false)</code>), is in overwrite mode
 * (<code>setOverwriteMode(true)</code>) and the date 7/1/99. In this
 * case the user will not be able to enter a two digit month or day of
 * month. To avoid this, the format should be 'MM/dd/yy'.
 * <p>
 * If <code>InternationalFormatter</code> is configured to only allow valid
 * values (<code>setAllowsInvalid(false)</code>), every valid edit will result
 * in the text of the <code>JFormattedTextField</code> being completely reset
 * from the <code>Format</code>.
 * The cursor position will also be adjusted as literal characters are
 * added/removed from the resulting String.
 * <p>
 * <code>InternationalFormatter</code>'s behavior of
 * <code>stringToValue</code> is  slightly different than that of
 * <code>DefaultTextFormatter</code>, it does the following:
 * <ol>
 *   <li><code>parseObject</code> is invoked on the <code>Format</code>
 *       specified by <code>setFormat</code>
 *   <li>If a Class has been set for the values (<code>setValueClass</code>),
 *       supers implementation is invoked to convert the value returned
 *       from <code>parseObject</code> to the appropriate class.
 *   <li>If a <code>ParseException</code> has not been thrown, and the value
 *       is outside the min/max a <code>ParseException</code> is thrown.
 *   <li>The value is returned.
 * </ol>
 * <code>InternationalFormatter</code> implements <code>stringToValue</code>
 * in this manner so that you can specify an alternate Class than
 * <code>Format</code> may return.
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
 * @see java.text.Format
 * @see java.lang.Comparable
 *
 * @since 1.4
 */
@SuppressWarnings("serial") // Same-version serialization only
public class InternationalFormatter extends DefaultFormatter {
    /**
     * Used by <code>getFields</code>.
     */
    private static final Format.Field[] EMPTY_FIELD_ARRAY =new Format.Field[0];

    /**
     * Object used to handle the conversion.
     */
    private Format format;
    /**
     * Can be used to impose a maximum value.
     */
    private Comparable<?> max;
    /**
     * Can be used to impose a minimum value.
     */
    private Comparable<?> min;

    /**
     * <code>InternationalFormatter</code>'s behavior is dicatated by a
     * <code>AttributedCharacterIterator</code> that is obtained from
     * the <code>Format</code>. On every edit, assuming
     * allows invalid is false, the <code>Format</code> instance is invoked
     * with <code>formatToCharacterIterator</code>. A <code>BitSet</code> is
     * also kept upto date with the non-literal characters, that is
     * for every index in the <code>AttributedCharacterIterator</code> an
     * entry in the bit set is updated based on the return value from
     * <code>isLiteral(Map)</code>. <code>isLiteral(int)</code> then uses
     * this cached information.
     * <p>
     * If allowsInvalid is false, every edit results in resetting the complete
     * text of the JTextComponent.
     * <p>
     * InternationalFormatterFilter can also provide two actions suitable for
     * incrementing and decrementing. To enable this a subclass must
     * override <code>getSupportsIncrement</code> to return true, and
     * override <code>adjustValue</code> to handle the changing of the
     * value. If you want to support changing the value outside of
     * the valid FieldPositions, you will need to override
     * <code>canIncrement</code>.
     */
    /**
     * A bit is set for every index identified in the
     * AttributedCharacterIterator that is not considered decoration.
     * This should only be used if validMask is true.
     */
    private transient BitSet literalMask;
    /**
     * Used to iterate over characters.
     */
    private transient AttributedCharacterIterator iterator;
    /**
     * True if the Format was able to convert the value to a String and
     * back.
     */
    private transient boolean validMask;
    /**
     * Current value being displayed.
     */
    private transient String string;
    /**
     * If true, DocumentFilter methods are unconditionally allowed,
     * and no checking is done on their values. This is used when
     * incrementing/decrementing via the actions.
     */
    private transient boolean ignoreDocumentMutate;


    /**
     * Creates an <code>InternationalFormatter</code> with no
     * <code>Format</code> specified.
     */
    public InternationalFormatter() {
        setOverwriteMode(false);
    }

    /**
     * Creates an <code>InternationalFormatter</code> with the specified
     * <code>Format</code> instance.
     *
     * @param format Format instance used for converting from/to Strings
     */
    public InternationalFormatter(Format format) {
        this();
        setFormat(format);
    }

    /**
     * Sets the format that dictates the legal values that can be edited
     * and displayed.
     *
     * @param format <code>Format</code> instance used for converting
     * from/to Strings
     */
    public void setFormat(Format format) {
        this.format = format;
    }

    /**
     * Returns the format that dictates the legal values that can be edited
     * and displayed.
     *
     * @return Format instance used for converting from/to Strings
     */
    public Format getFormat() {
        return format;
    }

    /**
     * Sets the minimum permissible value. If the <code>valueClass</code> has
     * not been specified, and <code>minimum</code> is non null, the
     * <code>valueClass</code> will be set to that of the class of
     * <code>minimum</code>.
     *
     * @param minimum Minimum legal value that can be input
     * @see #setValueClass
     */
    public void setMinimum(Comparable<?> minimum) {
        if (getValueClass() == null && minimum != null) {
            setValueClass(minimum.getClass());
        }
        min = minimum;
    }

    /**
     * Returns the minimum permissible value.
     *
     * @return Minimum legal value that can be input
     */
    public Comparable<?> getMinimum() {
        return min;
    }

    /**
     * Sets the maximum permissible value. If the <code>valueClass</code> has
     * not been specified, and <code>max</code> is non null, the
     * <code>valueClass</code> will be set to that of the class of
     * <code>max</code>.
     *
     * @param max Maximum legal value that can be input
     * @see #setValueClass
     */
    public void setMaximum(Comparable<?> max) {
        if (getValueClass() == null && max != null) {
            setValueClass(max.getClass());
        }
        this.max = max;
    }

    /**
     * Returns the maximum permissible value.
     *
     * @return Maximum legal value that can be input
     */
    public Comparable<?> getMaximum() {
        return max;
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
        updateMaskIfNecessary();
        // invoked again as the mask should now be valid.
        positionCursorAtInitialLocation();
    }

    /**
     * Returns a String representation of the Object <code>value</code>.
     * This invokes <code>format</code> on the current <code>Format</code>.
     *
     * @throws ParseException if there is an error in the conversion
     * @param value Value to convert
     * @return String representation of value
     */
    public String valueToString(Object value) throws ParseException {
        if (value == null) {
            return "";
        }
        Format f = getFormat();

        if (f == null) {
            return value.toString();
        }
        return f.format(value);
    }

    /**
     * Returns the <code>Object</code> representation of the
     * <code>String</code> <code>text</code>.
     *
     * @param text <code>String</code> to convert
     * @return <code>Object</code> representation of text
     * @throws ParseException if there is an error in the conversion
     */
    public Object stringToValue(String text) throws ParseException {
        Object value = stringToValue(text, getFormat());

        // Convert to the value class if the Value returned from the
        // Format does not match.
        if (value != null && getValueClass() != null &&
                             !getValueClass().isInstance(value)) {
            value = super.stringToValue(value.toString());
        }
        try {
            if (!isValidValue(value, true)) {
                throw new ParseException("Value not within min/max range", 0);
            }
        } catch (ClassCastException cce) {
            throw new ParseException("Class cast exception comparing values: "
                                     + cce, 0);
        }
        return value;
    }

    /**
     * Returns the <code>Format.Field</code> constants associated with
     * the text at <code>offset</code>. If <code>offset</code> is not
     * a valid location into the current text, this will return an
     * empty array.
     *
     * @param offset offset into text to be examined
     * @return Format.Field constants associated with the text at the
     *         given position.
     */
    public Format.Field[] getFields(int offset) {
        if (getAllowsInvalid()) {
            // This will work if the currently edited value is valid.
            updateMask();
        }

        Map<Attribute, Object> attrs = getAttributes(offset);

        if (attrs != null && attrs.size() > 0) {
            ArrayList<Attribute> al = new ArrayList<Attribute>();

            al.addAll(attrs.keySet());
            return al.toArray(EMPTY_FIELD_ARRAY);
        }
        return EMPTY_FIELD_ARRAY;
    }

    /**
     * Creates a copy of the DefaultFormatter.
     *
     * @return copy of the DefaultFormatter
     */
    public Object clone() throws CloneNotSupportedException {
        InternationalFormatter formatter = (InternationalFormatter)super.
                                           clone();

        formatter.literalMask = null;
        formatter.iterator = null;
        formatter.validMask = false;
        formatter.string = null;
        return formatter;
    }

    /**
     * If <code>getSupportsIncrement</code> returns true, this returns
     * two Actions suitable for incrementing/decrementing the value.
     */
    protected Action[] getActions() {
        if (getSupportsIncrement()) {
            return new Action[] { new IncrementAction("increment", 1),
                                  new IncrementAction("decrement", -1) };
        }
        return null;
    }

    /**
     * Invokes <code>parseObject</code> on <code>f</code>, returning
     * its value.
     */
    Object stringToValue(String text, Format f) throws ParseException {
        if (f == null) {
            return text;
        }
        return f.parseObject(text);
    }

    /**
     * Returns true if <code>value</code> is between the min/max.
     *
     * @param wantsCCE If false, and a ClassCastException is thrown in
     *                 comparing the values, the exception is consumed and
     *                 false is returned.
     */
    boolean isValidValue(Object value, boolean wantsCCE) {
        @SuppressWarnings("unchecked")
        Comparable<Object> min = (Comparable<Object>)getMinimum();

        try {
            if (min != null && min.compareTo(value) > 0) {
                return false;
            }
        } catch (ClassCastException cce) {
            if (wantsCCE) {
                throw cce;
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        Comparable<Object> max = (Comparable<Object>)getMaximum();
        try {
            if (max != null && max.compareTo(value) < 0) {
                return false;
            }
        } catch (ClassCastException cce) {
            if (wantsCCE) {
                throw cce;
            }
            return false;
        }
        return true;
    }

    /**
     * Returns a Set of the attribute identifiers at <code>index</code>.
     */
    Map<Attribute, Object> getAttributes(int index) {
        if (isValidMask()) {
            AttributedCharacterIterator iterator = getIterator();

            if (index >= 0 && index <= iterator.getEndIndex()) {
                iterator.setIndex(index);
                return iterator.getAttributes();
            }
        }
        return null;
    }


    /**
     * Returns the start of the first run that contains the attribute
     * <code>id</code>. This will return <code>-1</code> if the attribute
     * can not be found.
     */
    int getAttributeStart(AttributedCharacterIterator.Attribute id) {
        if (isValidMask()) {
            AttributedCharacterIterator iterator = getIterator();

            iterator.first();
            while (iterator.current() != CharacterIterator.DONE) {
                if (iterator.getAttribute(id) != null) {
                    return iterator.getIndex();
                }
                iterator.next();
            }
        }
        return -1;
    }

    /**
     * Returns the <code>AttributedCharacterIterator</code> used to
     * format the last value.
     */
    AttributedCharacterIterator getIterator() {
        return iterator;
    }

    /**
     * Updates the AttributedCharacterIterator and bitset, if necessary.
     */
    void updateMaskIfNecessary() {
        if (!getAllowsInvalid() && (getFormat() != null)) {
            if (!isValidMask()) {
                updateMask();
            }
            else {
                String newString = getFormattedTextField().getText();

                if (!newString.equals(string)) {
                    updateMask();
                }
            }
        }
    }

    /**
     * Updates the AttributedCharacterIterator by invoking
     * <code>formatToCharacterIterator</code> on the <code>Format</code>.
     * If this is successful,
     * <code>updateMask(AttributedCharacterIterator)</code>
     * is then invoked to update the internal bitmask.
     */
    void updateMask() {
        if (getFormat() != null) {
            Document doc = getFormattedTextField().getDocument();

            validMask = false;
            if (doc != null) {
                try {
                    string = doc.getText(0, doc.getLength());
                } catch (BadLocationException ble) {
                    string = null;
                }
                if (string != null) {
                    try {
                        Object value = stringToValue(string);
                        AttributedCharacterIterator iterator = getFormat().
                                  formatToCharacterIterator(value);

                        updateMask(iterator);
                    }
                    catch (ParseException pe) {}
                    catch (IllegalArgumentException iae) {}
                    catch (NullPointerException npe) {}
                }
            }
        }
    }

    /**
     * Returns the number of literal characters before <code>index</code>.
     */
    int getLiteralCountTo(int index) {
        int lCount = 0;

        for (int counter = 0; counter < index; counter++) {
            if (isLiteral(counter)) {
                lCount++;
            }
        }
        return lCount;
    }

    /**
     * Returns true if the character at index is a literal, that is
     * not editable.
     */
    boolean isLiteral(int index) {
        if (isValidMask() && index < string.length()) {
            return literalMask.get(index);
        }
        return false;
    }

    /**
     * Returns the literal character at index.
     */
    char getLiteral(int index) {
        if (isValidMask() && string != null && index < string.length()) {
            return string.charAt(index);
        }
        return (char)0;
    }

    /**
     * Returns true if the character at offset is navigable too. This
     * is implemented in terms of <code>isLiteral</code>, subclasses
     * may wish to provide different behavior.
     */
    boolean isNavigatable(int offset) {
        return !isLiteral(offset);
    }

    /**
     * Overriden to update the mask after invoking supers implementation.
     */
    void updateValue(Object value) {
        super.updateValue(value);
        updateMaskIfNecessary();
    }

    /**
     * Overriden to unconditionally allow the replace if
     * ignoreDocumentMutate is true.
     */
    void replace(DocumentFilter.FilterBypass fb, int offset,
                     int length, String text,
                     AttributeSet attrs) throws BadLocationException {
        if (ignoreDocumentMutate) {
            fb.replace(offset, length, text, attrs);
            return;
        }
        super.replace(fb, offset, length, text, attrs);
    }

    /**
     * Returns the index of the next non-literal character starting at
     * index. If index is not a literal, it will be returned.
     *
     * @param direction Amount to increment looking for non-literal
     */
    private int getNextNonliteralIndex(int index, int direction) {
        int max = getFormattedTextField().getDocument().getLength();

        while (index >= 0 && index < max) {
            if (isLiteral(index)) {
                index += direction;
            }
            else {
                return index;
            }
        }
        return (direction == -1) ? 0 : max;
    }

    /**
     * Overriden in an attempt to honor the literals.
     * <p>If we do not allow invalid values and are in overwrite mode, this
     * {@code rh.length} is corrected as to preserve trailing literals.
     * If not in overwrite mode, and there is text to insert it is
     * inserted at the next non literal index going forward.  If there
     * is only text to remove, it is removed from the next non literal
     * index going backward.
     */
    boolean canReplace(ReplaceHolder rh) {
        if (!getAllowsInvalid()) {
            String text = rh.text;
            int tl = (text != null) ? text.length() : 0;
            JTextComponent c = getFormattedTextField();

            if (tl == 0 && rh.length == 1 && c.getSelectionStart() != rh.offset) {
                // Backspace, adjust to actually delete next non-literal.
                rh.offset = getNextNonliteralIndex(rh.offset, -1);
            } else if (getOverwriteMode()) {
                int pos = rh.offset;
                int textPos = pos;
                boolean overflown = false;

                for (int i = 0; i < rh.length; i++) {
                    while (isLiteral(pos)) pos++;
                    if (pos >= string.length()) {
                        pos = textPos;
                        overflown = true;
                        break;
                    }
                    textPos = ++pos;
                }
                if (overflown || c.getSelectedText() == null) {
                    rh.length = pos - rh.offset;
                }
            }
            else if (tl > 0) {
                // insert (or insert and remove)
                rh.offset = getNextNonliteralIndex(rh.offset, 1);
            }
            else {
                // remove only
                rh.offset = getNextNonliteralIndex(rh.offset, -1);
            }
            ((ExtendedReplaceHolder)rh).endOffset = rh.offset;
            ((ExtendedReplaceHolder)rh).endTextLength = (rh.text != null) ?
                                                    rh.text.length() : 0;
        }
        else {
            ((ExtendedReplaceHolder)rh).endOffset = rh.offset;
            ((ExtendedReplaceHolder)rh).endTextLength = (rh.text != null) ?
                                                    rh.text.length() : 0;
        }
        boolean can = super.canReplace(rh);
        if (can && !getAllowsInvalid()) {
            ((ExtendedReplaceHolder)rh).resetFromValue(this);
        }
        return can;
    }

    /**
     * When in !allowsInvalid mode the text is reset on every edit, thus
     * supers implementation will position the cursor at the wrong position.
     * As such, this invokes supers implementation and then invokes
     * <code>repositionCursor</code> to correctly reset the cursor.
     */
    boolean replace(ReplaceHolder rh) throws BadLocationException {
        int start = -1;
        int direction = 1;
        int literalCount = -1;

        if (rh.length > 0 && (rh.text == null || rh.text.length() == 0) &&
               (getFormattedTextField().getSelectionStart() != rh.offset ||
                   rh.length > 1)) {
            direction = -1;
        }
        if (!getAllowsInvalid()) {
            if ((rh.text == null || rh.text.length() == 0) && rh.length > 0) {
                // remove
                start = getFormattedTextField().getSelectionStart();
            }
            else {
                start = rh.offset;
            }
            literalCount = getLiteralCountTo(start);
        }
        if (super.replace(rh)) {
            if (start != -1) {
                int end = ((ExtendedReplaceHolder)rh).endOffset;

                end += ((ExtendedReplaceHolder)rh).endTextLength;
                repositionCursor(literalCount, end, direction);
            }
            else {
                start = ((ExtendedReplaceHolder)rh).endOffset;
                if (direction == 1) {
                    start += ((ExtendedReplaceHolder)rh).endTextLength;
                }
                repositionCursor(start, direction);
            }
            return true;
        }
        return false;
    }

    /**
     * Repositions the cursor. <code>startLiteralCount</code> gives
     * the number of literals to the start of the deleted range, end
     * gives the ending location to adjust from, direction gives
     * the direction relative to <code>end</code> to position the
     * cursor from.
     */
    private void repositionCursor(int startLiteralCount, int end,
                                  int direction)  {
        int endLiteralCount = getLiteralCountTo(end);

        if (endLiteralCount != end) {
            end -= startLiteralCount;
            for (int counter = 0; counter < end; counter++) {
                if (isLiteral(counter)) {
                    end++;
                }
            }
        }
        repositionCursor(end, 1 /*direction*/);
    }

    /**
     * Returns the character from the mask that has been buffered
     * at <code>index</code>.
     */
    char getBufferedChar(int index) {
        if (isValidMask()) {
            if (string != null && index < string.length()) {
                return string.charAt(index);
            }
        }
        return (char)0;
    }

    /**
     * Returns true if the current mask is valid.
     */
    boolean isValidMask() {
        return validMask;
    }

    /**
     * Returns true if <code>attributes</code> is null or empty.
     */
    boolean isLiteral(Map<?, ?> attributes) {
        return ((attributes == null) || attributes.size() == 0);
    }

    /**
     * Updates the interal bitset from <code>iterator</code>. This will
     * set <code>validMask</code> to true if <code>iterator</code> is
     * non-null.
     */
    private void updateMask(AttributedCharacterIterator iterator) {
        if (iterator != null) {
            validMask = true;
            this.iterator = iterator;

            // Update the literal mask
            if (literalMask == null) {
                literalMask = new BitSet();
            }
            else {
                for (int counter = literalMask.length() - 1; counter >= 0;
                     counter--) {
                    literalMask.clear(counter);
                }
            }

            iterator.first();
            while (iterator.current() != CharacterIterator.DONE) {
                Map<Attribute,Object> attributes = iterator.getAttributes();
                boolean set = isLiteral(attributes);
                int start = iterator.getIndex();
                int end = iterator.getRunLimit();

                while (start < end) {
                    if (set) {
                        literalMask.set(start);
                    }
                    else {
                        literalMask.clear(start);
                    }
                    start++;
                }
                iterator.setIndex(start);
            }
        }
    }

    /**
     * Returns true if <code>field</code> is non-null.
     * Subclasses that wish to allow incrementing to happen outside of
     * the known fields will need to override this.
     */
    boolean canIncrement(Object field, int cursorPosition) {
        return (field != null);
    }

    /**
     * Selects the fields identified by <code>attributes</code>.
     */
    void selectField(Object f, int count) {
        AttributedCharacterIterator iterator = getIterator();

        if (iterator != null &&
                        (f instanceof AttributedCharacterIterator.Attribute)) {
            AttributedCharacterIterator.Attribute field =
                                   (AttributedCharacterIterator.Attribute)f;

            iterator.first();
            while (iterator.current() != CharacterIterator.DONE) {
                while (iterator.getAttribute(field) == null &&
                       iterator.next() != CharacterIterator.DONE);
                if (iterator.current() != CharacterIterator.DONE) {
                    int limit = iterator.getRunLimit(field);

                    if (--count <= 0) {
                        getFormattedTextField().select(iterator.getIndex(),
                                                       limit);
                        break;
                    }
                    iterator.setIndex(limit);
                    iterator.next();
                }
            }
        }
    }

    /**
     * Returns the field that will be adjusted by adjustValue.
     */
    Object getAdjustField(int start, Map<?, ?> attributes) {
        return null;
    }

    /**
     * Returns the number of occurrences of <code>f</code> before
     * the location <code>start</code> in the current
     * <code>AttributedCharacterIterator</code>.
     */
    private int getFieldTypeCountTo(Object f, int start) {
        AttributedCharacterIterator iterator = getIterator();
        int count = 0;

        if (iterator != null &&
                    (f instanceof AttributedCharacterIterator.Attribute)) {
            AttributedCharacterIterator.Attribute field =
                                   (AttributedCharacterIterator.Attribute)f;

            iterator.first();
            while (iterator.getIndex() < start) {
                while (iterator.getAttribute(field) == null &&
                       iterator.next() != CharacterIterator.DONE);
                if (iterator.current() != CharacterIterator.DONE) {
                    iterator.setIndex(iterator.getRunLimit(field));
                    iterator.next();
                    count++;
                }
                else {
                    break;
                }
            }
        }
        return count;
    }

    /**
     * Subclasses supporting incrementing must override this to handle
     * the actual incrementing. <code>value</code> is the current value,
     * <code>attributes</code> gives the field the cursor is in (may be
     * null depending upon <code>canIncrement</code>) and
     * <code>direction</code> is the amount to increment by.
     */
    Object adjustValue(Object value, Map<?, ?> attributes, Object field,
                           int direction) throws
                      BadLocationException, ParseException {
        return null;
    }

    /**
     * Returns false, indicating InternationalFormatter does not allow
     * incrementing of the value. Subclasses that wish to support
     * incrementing/decrementing the value should override this and
     * return true. Subclasses should also override
     * <code>adjustValue</code>.
     */
    boolean getSupportsIncrement() {
        return false;
    }

    /**
     * Resets the value of the JFormattedTextField to be
     * <code>value</code>.
     */
    void resetValue(Object value) throws BadLocationException, ParseException {
        Document doc = getFormattedTextField().getDocument();
        String string = valueToString(value);

        try {
            ignoreDocumentMutate = true;
            doc.remove(0, doc.getLength());
            doc.insertString(0, string, null);
        } finally {
            ignoreDocumentMutate = false;
        }
        updateValue(value);
    }

    /**
     * Subclassed to update the internal representation of the mask after
     * the default read operation has completed.
     */
    @Serial
    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        updateMaskIfNecessary();
    }


    /**
     * Overriden to return an instance of <code>ExtendedReplaceHolder</code>.
     */
    ReplaceHolder getReplaceHolder(DocumentFilter.FilterBypass fb, int offset,
                                   int length, String text,
                                   AttributeSet attrs) {
        if (replaceHolder == null) {
            replaceHolder = new ExtendedReplaceHolder();
        }
        return super.getReplaceHolder(fb, offset, length, text, attrs);
    }


    /**
     * As InternationalFormatter replaces the complete text on every edit,
     * ExtendedReplaceHolder keeps track of the offset and length passed
     * into canReplace.
     */
    static class ExtendedReplaceHolder extends ReplaceHolder {
        /** Offset of the insert/remove. This may differ from offset in
         * that if !allowsInvalid the text is replaced on every edit. */
        int endOffset;
        /** Length of the text. This may differ from text.length in
         * that if !allowsInvalid the text is replaced on every edit. */
        int endTextLength;

        /**
         * Resets the region to delete to be the complete document and
         * the text from invoking valueToString on the current value.
         */
        void resetFromValue(InternationalFormatter formatter) {
            // Need to reset the complete string as Format's result can
            // be completely different.
            offset = 0;
            try {
                text = formatter.valueToString(value);
            } catch (ParseException pe) {
                // Should never happen, otherwise canReplace would have
                // returned value.
                text = "";
            }
            length = fb.getDocument().getLength();
        }
    }


    /**
     * IncrementAction is used to increment the value by a certain amount.
     * It calls into <code>adjustValue</code> to handle the actual
     * incrementing of the value.
     */
    private class IncrementAction extends AbstractAction {
        private int direction;

        IncrementAction(String name, int direction) {
            super(name);
            this.direction = direction;
        }

        public void actionPerformed(ActionEvent ae) {

            if (getFormattedTextField().isEditable()) {
                if (getAllowsInvalid()) {
                    // This will work if the currently edited value is valid.
                    updateMask();
                }

                boolean validEdit = false;

                if (isValidMask()) {
                    int start = getFormattedTextField().getSelectionStart();

                    if (start != -1) {
                        AttributedCharacterIterator iterator = getIterator();

                        iterator.setIndex(start);

                        Map<Attribute,Object> attributes = iterator.getAttributes();
                        Object field = getAdjustField(start, attributes);

                        if (canIncrement(field, start)) {
                            try {
                                Object value = stringToValue(
                                        getFormattedTextField().getText());
                                int fieldTypeCount = getFieldTypeCountTo(
                                        field, start);

                                value = adjustValue(value, attributes,
                                        field, direction);
                                if (value != null && isValidValue(value, false)) {
                                    resetValue(value);
                                    updateMask();

                                    if (isValidMask()) {
                                        selectField(field, fieldTypeCount);
                                    }
                                    validEdit = true;
                                }
                            }
                            catch (ParseException pe) { }
                            catch (BadLocationException ble) { }
                        }
                    }
                }
                if (!validEdit) {
                    invalidEdit();
                }
            }
        }
    }
}
