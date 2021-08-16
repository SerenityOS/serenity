/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing;

import java.awt.Component;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;

/**
 * A text component that can be marked up with attributes that are
 * represented graphically.
 * You can find how-to information and examples of using text panes in
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/text.html">Using Text Components</a>,
 * a section in <em>The Java Tutorial.</em>
 *
 * <p>
 * This component models paragraphs
 * that are composed of runs of character level attributes.  Each
 * paragraph may have a logical style attached to it which contains
 * the default attributes to use if not overridden by attributes set
 * on the paragraph or character run.  Components and images may
 * be embedded in the flow of text.
 *
 * <dl>
 * <dt><b>Newlines</b>
 * <dd>
 * For a discussion on how newlines are handled, see
 * <a href="text/DefaultEditorKit.html">DefaultEditorKit</a>.
 * </dl>
 *
 * <p>
 * <strong>Warning:</strong> Swing is not thread safe. For more
 * information see <a
 * href="package-summary.html#threading">Swing's Threading
 * Policy</a>.
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
 * @author  Timothy Prinzing
 * @see javax.swing.text.StyledEditorKit
 * @since 1.2
 */
@JavaBean(description = "A text component that can be marked up with attributes that are graphically represented.")
@SwingContainer
@SuppressWarnings("serial") // Same-version serialization only
public class JTextPane extends JEditorPane {

    /**
     * Creates a new <code>JTextPane</code>.  A new instance of
     * <code>StyledEditorKit</code> is
     * created and set, and the document model set to <code>null</code>.
     */
    public JTextPane() {
        super();
        EditorKit editorKit = createDefaultEditorKit();
        String contentType = editorKit.getContentType();
        if (contentType != null
            && getEditorKitClassNameForContentType(contentType) ==
                 defaultEditorKitMap.get(contentType)) {
            setEditorKitForContentType(contentType, editorKit);
        }
        setEditorKit(editorKit);
    }

    /**
     * Creates a new <code>JTextPane</code>, with a specified document model.
     * A new instance of <code>javax.swing.text.StyledEditorKit</code>
     *  is created and set.
     *
     * @param doc the document model
     */
    public JTextPane(StyledDocument doc) {
        this();
        setStyledDocument(doc);
    }

    /**
     * Returns the class ID for the UI.
     *
     * @return the string "TextPaneUI"
     *
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false)
    public String getUIClassID() {
        return uiClassID;
    }

    /**
     * Associates the editor with a text document.  This
     * must be a <code>StyledDocument</code>.
     *
     * @param doc  the document to display/edit
     * @exception IllegalArgumentException  if <code>doc</code> can't
     *   be narrowed to a <code>StyledDocument</code> which is the
     *   required type of model for this text component
     */
    public void setDocument(Document doc) {
        if (doc instanceof StyledDocument) {
            super.setDocument(doc);
        } else {
            throw new IllegalArgumentException("Model must be StyledDocument");
        }
    }

    /**
     * Associates the editor with a text document.
     * The currently registered factory is used to build a view for
     * the document, which gets displayed by the editor.
     *
     * @param doc  the document to display/edit
     */
    public void setStyledDocument(StyledDocument doc) {
        super.setDocument(doc);
    }

    /**
     * Fetches the model associated with the editor.
     *
     * @return the model
     */
    public StyledDocument getStyledDocument() {
        return (StyledDocument) getDocument();
    }

    /**
     * Replaces the currently selected content with new content
     * represented by the given string.  If there is no selection
     * this amounts to an insert of the given text.  If there
     * is no replacement text this amounts to a removal of the
     * current selection.  The replacement text will have the
     * attributes currently defined for input at the point of
     * insertion.  If the document is not editable, beep and return.
     *
     * @param content  the content to replace the selection with
     */
    @Override
    public void replaceSelection(String content) {
        replaceSelection(content, true);
    }

    private void replaceSelection(String content, boolean checkEditable) {
        if (checkEditable && !isEditable()) {
            UIManager.getLookAndFeel().provideErrorFeedback(JTextPane.this);
            return;
        }
        Document doc = getStyledDocument();
        if (doc != null) {
            try {
                Caret caret = getCaret();
                boolean composedTextSaved = saveComposedText(caret.getDot());
                int p0 = Math.min(caret.getDot(), caret.getMark());
                int p1 = Math.max(caret.getDot(), caret.getMark());
                AttributeSet attr = getInputAttributes().copyAttributes();
                if (doc instanceof AbstractDocument) {
                    ((AbstractDocument)doc).replace(p0, p1 - p0, content,attr);
                }
                else {
                    if (p0 != p1) {
                        doc.remove(p0, p1 - p0);
                    }
                    if (content != null && content.length() > 0) {
                        doc.insertString(p0, content, attr);
                    }
                }
                if (composedTextSaved) {
                    restoreComposedText();
                }
            } catch (BadLocationException e) {
                UIManager.getLookAndFeel().provideErrorFeedback(JTextPane.this);
            }
        }
    }

    /**
     * Inserts a component into the document as a replacement
     * for the currently selected content.  If there is no
     * selection the component is effectively inserted at the
     * current position of the caret.  This is represented in
     * the associated document as an attribute of one character
     * of content.
     * <p>
     * The component given is the actual component used by the
     * JTextPane.  Since components cannot be a child of more than
     * one container, this method should not be used in situations
     * where the model is shared by text components.
     * <p>
     * The component is placed relative to the text baseline
     * according to the value returned by
     * <code>Component.getAlignmentY</code>.  For Swing components
     * this value can be conveniently set using the method
     * <code>JComponent.setAlignmentY</code>.  For example, setting
     * a value of <code>0.75</code> will cause 75 percent of the
     * component to be above the baseline, and 25 percent of the
     * component to be below the baseline.
     *
     * @param c    the component to insert
     */
    public void insertComponent(Component c) {
        MutableAttributeSet inputAttributes = getInputAttributes();
        inputAttributes.removeAttributes(inputAttributes);
        StyleConstants.setComponent(inputAttributes, c);
        replaceSelection(" ", false);
        inputAttributes.removeAttributes(inputAttributes);
    }

    /**
     * Inserts an icon into the document as a replacement
     * for the currently selected content.  If there is no
     * selection the icon is effectively inserted at the
     * current position of the caret.  This is represented in
     * the associated document as an attribute of one character
     * of content.
     *
     * @param g    the icon to insert
     * @see Icon
     */
    public void insertIcon(Icon g) {
        MutableAttributeSet inputAttributes = getInputAttributes();
        inputAttributes.removeAttributes(inputAttributes);
        StyleConstants.setIcon(inputAttributes, g);
        replaceSelection(" ", false);
        inputAttributes.removeAttributes(inputAttributes);
    }

    /**
     * Adds a new style into the logical style hierarchy.  Style attributes
     * resolve from bottom up so an attribute specified in a child
     * will override an attribute specified in the parent.
     *
     * @param nm   the name of the style (must be unique within the
     *   collection of named styles).  The name may be <code>null</code>
     *   if the style is unnamed, but the caller is responsible
     *   for managing the reference returned as an unnamed style can't
     *   be fetched by name.  An unnamed style may be useful for things
     *   like character attribute overrides such as found in a style
     *   run.
     * @param parent the parent style.  This may be <code>null</code>
     *   if unspecified
     *   attributes need not be resolved in some other style.
     * @return the new <code>Style</code>
     */
    public Style addStyle(String nm, Style parent) {
        StyledDocument doc = getStyledDocument();
        return doc.addStyle(nm, parent);
    }

    /**
     * Removes a named non-<code>null</code> style previously added to
     * the document.
     *
     * @param nm  the name of the style to remove
     */
    public void removeStyle(String nm) {
        StyledDocument doc = getStyledDocument();
        doc.removeStyle(nm);
    }

    /**
     * Fetches a named non-<code>null</code> style previously added.
     *
     * @param nm  the name of the style
     * @return the <code>Style</code>
     */
    public Style getStyle(String nm) {
        StyledDocument doc = getStyledDocument();
        return doc.getStyle(nm);
    }

    /**
     * Sets the logical style to use for the paragraph at the
     * current caret position.  If attributes aren't explicitly set
     * for character and paragraph attributes they will resolve
     * through the logical style assigned to the paragraph, which
     * in term may resolve through some hierarchy completely
     * independent of the element hierarchy in the document.
     *
     * @param s  the logical style to assign to the paragraph,
     *          or <code>null</code> for no style
     */
    public void setLogicalStyle(Style s) {
        StyledDocument doc = getStyledDocument();
        doc.setLogicalStyle(getCaretPosition(), s);
    }

    /**
     * Fetches the logical style assigned to the paragraph represented
     * by the current position of the caret, or <code>null</code>.
     *
     * @return the <code>Style</code>
     */
    public Style getLogicalStyle() {
        StyledDocument doc = getStyledDocument();
        return doc.getLogicalStyle(getCaretPosition());
    }

    /**
     * Fetches the character attributes in effect at the
     * current location of the caret, or <code>null</code>.
     *
     * @return the attributes, or <code>null</code>
     */
    @BeanProperty(bound = false)
    public AttributeSet getCharacterAttributes() {
        StyledDocument doc = getStyledDocument();
        Element run = doc.getCharacterElement(getCaretPosition());
        if (run != null) {
            return run.getAttributes();
        }
        return null;
    }

    /**
     * Applies the given attributes to character
     * content.  If there is a selection, the attributes
     * are applied to the selection range.  If there
     * is no selection, the attributes are applied to
     * the input attribute set which defines the attributes
     * for any new text that gets inserted.
     *
     * @param attr the attributes
     * @param replace if true, then replace the existing attributes first
     */
    public void setCharacterAttributes(AttributeSet attr, boolean replace) {
        int p0 = getSelectionStart();
        int p1 = getSelectionEnd();
        if (p0 != p1) {
            StyledDocument doc = getStyledDocument();
            doc.setCharacterAttributes(p0, p1 - p0, attr, replace);
        } else {
            MutableAttributeSet inputAttributes = getInputAttributes();
            if (replace) {
                inputAttributes.removeAttributes(inputAttributes);
            }
            inputAttributes.addAttributes(attr);
        }
    }

    /**
     * Fetches the current paragraph attributes in effect
     * at the location of the caret, or <code>null</code> if none.
     *
     * @return the attributes
     */
    @BeanProperty(bound = false)
    public AttributeSet getParagraphAttributes() {
        StyledDocument doc = getStyledDocument();
        Element paragraph = doc.getParagraphElement(getCaretPosition());
        if (paragraph != null) {
            return paragraph.getAttributes();
        }
        return null;
    }

    /**
     * Applies the given attributes to paragraphs.  If
     * there is a selection, the attributes are applied
     * to the paragraphs that intersect the selection.
     * If there is no selection, the attributes are applied
     * to the paragraph at the current caret position.
     *
     * @param attr the non-<code>null</code> attributes
     * @param replace if true, replace the existing attributes first
     */
    public void setParagraphAttributes(AttributeSet attr, boolean replace) {
        int p0 = getSelectionStart();
        int p1 = getSelectionEnd();
        StyledDocument doc = getStyledDocument();
        doc.setParagraphAttributes(p0, p1 - p0, attr, replace);
    }

    /**
     * Gets the input attributes for the pane.
     *
     * @return the attributes
     */
    @BeanProperty(bound = false)
    public MutableAttributeSet getInputAttributes() {
        return getStyledEditorKit().getInputAttributes();
    }

    /**
     * Gets the editor kit.
     *
     * @return the editor kit
     */
    protected final StyledEditorKit getStyledEditorKit() {
        return (StyledEditorKit) getEditorKit();
    }

    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "TextPaneUI";


    /**
     * See <code>readObject</code> and <code>writeObject</code> in
     * <code>JComponent</code> for more
     * information about serialization in Swing.
     *
     * @param s the output stream
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        if (getUIClassID().equals(uiClassID)) {
            byte count = JComponent.getWriteObjCounter(this);
            JComponent.setWriteObjCounter(this, --count);
            if (count == 0 && ui != null) {
                ui.installUI(this);
            }
        }
    }


    // --- JEditorPane ------------------------------------

    /**
     * Creates the <code>EditorKit</code> to use by default.  This
     * is implemented to return <code>javax.swing.text.StyledEditorKit</code>.
     *
     * @return the editor kit
     */
    protected EditorKit createDefaultEditorKit() {
        return new StyledEditorKit();
    }

    /**
     * Sets the currently installed kit for handling
     * content.  This is the bound property that
     * establishes the content type of the editor.
     *
     * @param kit the desired editor behavior
     * @exception IllegalArgumentException if kit is not a
     *          <code>StyledEditorKit</code>
     */
    public final void setEditorKit(EditorKit kit) {
        if (kit instanceof StyledEditorKit) {
            super.setEditorKit(kit);
        } else {
            throw new IllegalArgumentException("Must be StyledEditorKit");
        }
    }

    /**
     * Returns a string representation of this <code>JTextPane</code>.
     * This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this <code>JTextPane</code>
     */
    protected String paramString() {
        return super.paramString();
    }

}
