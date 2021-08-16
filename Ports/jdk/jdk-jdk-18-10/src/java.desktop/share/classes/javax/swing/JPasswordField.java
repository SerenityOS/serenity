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

import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Arrays;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleText;
import javax.accessibility.AccessibleTextSequence;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;

/**
 * <code>JPasswordField</code> is a lightweight component that allows
 * the editing of a single line of text where the view indicates
 * something was typed, but does not show the original characters.
 * You can find further information and examples in
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/textfield.html">How to Use Text Fields</a>,
 * a section in <em>The Java Tutorial.</em>
 * <p>
 * <code>JPasswordField</code> is intended
 * to be source-compatible with <code>java.awt.TextField</code>
 * used with <code>echoChar</code> set.  It is provided separately
 * to make it easier to safely change the UI for the
 * <code>JTextField</code> without affecting password entries.
 * <p>
 * <strong>NOTE:</strong>
 * By default, JPasswordField disables input methods; otherwise, input
 * characters could be visible while they were composed using input methods.
 * If an application needs the input methods support, please use the
 * inherited method, <code>enableInputMethods(true)</code>.
 * <p>
 * <strong>Warning:</strong> The {@code JPasswordField} will not show the
 * original characters that were typed, instead displaying alternative text or
 * graphics. However this doesn't prevent the password from appearing in the
 * system memory. For handling confidential information such as the password
 * text, refer to the relevant section at
 * <a href="https://www.oracle.com/java/technologies/javase/seccodeguide.html">
 * Secure Coding Guidelines</a>.
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
 * @since 1.2
 */
@JavaBean(description = "Allows the editing of a line of text but doesn't show the characters.")
@SwingContainer(false)
@SuppressWarnings("serial") // Same-version serialization only
public class JPasswordField extends JTextField {

    /**
     * Constructs a new <code>JPasswordField</code>,
     * with a default document, <code>null</code> starting
     * text string, and 0 column width.
     */
    public JPasswordField() {
        this(null,null,0);
    }

    /**
     * Constructs a new <code>JPasswordField</code> initialized
     * with the specified text.  The document model is set to the
     * default, and the number of columns to 0.
     *
     * @param text the text to be displayed, <code>null</code> if none
     */
    public JPasswordField(String text) {
        this(null, text, 0);
    }

    /**
     * Constructs a new empty <code>JPasswordField</code> with the specified
     * number of columns.  A default model is created, and the initial string
     * is set to <code>null</code>.
     *
     * @param columns the number of columns &gt;= 0
     */
    public JPasswordField(int columns) {
        this(null, null, columns);
    }

    /**
     * Constructs a new <code>JPasswordField</code> initialized with
     * the specified text and columns.  The document model is set to
     * the default.
     *
     * @param text the text to be displayed, <code>null</code> if none
     * @param columns the number of columns &gt;= 0
     */
    public JPasswordField(String text, int columns) {
        this(null, text, columns);
    }

    /**
     * Constructs a new <code>JPasswordField</code> that uses the
     * given text storage model and the given number of columns.
     * This is the constructor through which the other constructors feed.
     * The echo character is set to '*', but may be changed by the current
     * Look and Feel.  If the document model is
     * <code>null</code>, a default one will be created.
     *
     * @param doc  the text storage to use
     * @param txt the text to be displayed, <code>null</code> if none
     * @param columns  the number of columns to use to calculate
     *   the preferred width &gt;= 0; if columns is set to zero, the
     *   preferred width will be whatever naturally results from
     *   the component implementation
     */
    public JPasswordField(Document doc, String txt, int columns) {
        super(doc, txt, columns);
        // We could either leave this on, which wouldn't be secure,
        // or obscure the composted text, which essentially makes displaying
        // it useless. Therefore, we turn off input methods.
        enableInputMethods(false);
    }

    /**
     * Returns the name of the L&amp;F class that renders this component.
     *
     * @return the string "PasswordFieldUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false)
    public String getUIClassID() {
        return uiClassID;
    }


    /**
     * {@inheritDoc}
     * @since 1.6
     */
    public void updateUI() {
        if(!echoCharSet) {
            echoChar = '*';
        }
        super.updateUI();
    }

    /**
     * Returns the character to be used for echoing.  The default is '*'.
     * The default may be different depending on the currently running Look
     * and Feel. For example, Metal/Ocean's default is a bullet character.
     *
     * @return the echo character, 0 if unset
     * @see #setEchoChar
     * @see #echoCharIsSet
     */
    public char getEchoChar() {
        return echoChar;
    }

    /**
     * Sets the echo character for this <code>JPasswordField</code>.
     * Note that this is largely a suggestion, since the
     * view that gets installed can use whatever graphic techniques
     * it desires to represent the field.  Setting a value of 0 indicates
     * that you wish to see the text as it is typed, similar to
     * the behavior of a standard <code>JTextField</code>.
     *
     * @param c the echo character to display
     * @see #echoCharIsSet
     * @see #getEchoChar
     */
    @BeanProperty(bound = false, visualUpdate = true, description
            = "character to display in place of the real characters")
    public void setEchoChar(char c) {
        echoChar = c;
        echoCharSet = true;
        repaint();
        revalidate();
    }

    /**
     * Returns true if this <code>JPasswordField</code> has a character
     * set for echoing.  A character is considered to be set if the echo
     * character is not 0.
     *
     * @return true if a character is set for echoing
     * @see #setEchoChar
     * @see #getEchoChar
     */
    public boolean echoCharIsSet() {
        return echoChar != 0;
    }

    // --- JTextComponent methods ----------------------------------

    /**
     * Invokes <code>provideErrorFeedback</code> on the current
     * look and feel, which typically initiates an error beep.
     * The normal behavior of transferring the
     * currently selected range in the associated text model
     * to the system clipboard, and removing the contents from
     * the model, is not acceptable for a password field.
     */
    public void cut() {
        if (getClientProperty("JPasswordField.cutCopyAllowed") != Boolean.TRUE) {
            UIManager.getLookAndFeel().provideErrorFeedback(this);
        } else {
            super.cut();
        }
    }

    /**
     * Invokes <code>provideErrorFeedback</code> on the current
     * look and feel, which typically initiates an error beep.
     * The normal behavior of transferring the
     * currently selected range in the associated text model
     * to the system clipboard, and leaving the contents from
     * the model, is not acceptable for a password field.
     */
    public void copy() {
        if (getClientProperty("JPasswordField.cutCopyAllowed") != Boolean.TRUE) {
            UIManager.getLookAndFeel().provideErrorFeedback(this);
        } else {
            super.copy();
        }
    }

    /**
     * Returns the text contained in this <code>TextComponent</code>.
     * If the underlying document is <code>null</code>, will give a
     * <code>NullPointerException</code>.
     * <p>
     * For security reasons, this method is deprecated.  Use the
     <code>* getPassword</code> method instead.
     * @deprecated As of Java 2 platform v1.2,
     * replaced by <code>getPassword</code>.
     * @return the text
     */
    @Deprecated
    public String getText() {
        return super.getText();
    }

    /**
     * Fetches a portion of the text represented by the
     * component.  Returns an empty string if length is 0.
     * <p>
     * For security reasons, this method is deprecated.  Use the
     * <code>getPassword</code> method instead.
     * @deprecated As of Java 2 platform v1.2,
     * replaced by <code>getPassword</code>.
     * @param offs the offset &gt;= 0
     * @param len the length &gt;= 0
     * @return the text
     * @exception BadLocationException if the offset or length are invalid
     */
    @Deprecated
    public String getText(int offs, int len) throws BadLocationException {
        return super.getText(offs, len);
    }

    @Override
    @BeanProperty(bound = false, description = "the text of this component")
    public void setText(String t) {
        // overwrite the old data first
        Document doc = getDocument();
        int nleft = doc.getLength();
        Segment text = new Segment();
        // we would like to get direct data array access, not a copy of it
        text.setPartialReturn(true);
        int offs = 0;
        try {
            while (nleft > 0) {
                doc.getText(offs, nleft, text);
                Arrays.fill(text.array, text.offset,
                            text.count + text.offset, '\u0000');
                nleft -= text.count;
                offs += text.count;
            }
        } catch (BadLocationException ignored) {
            // we tried
        }
        super.setText(t);
    }

    /**
     * Returns the text contained in this <code>TextComponent</code>.
     * If the underlying document is <code>null</code>, will give a
     * <code>NullPointerException</code>.  For stronger
     * security, it is recommended that the returned character array be
     * cleared after use by setting each character to zero.
     *
     * @return the text
     */
    @BeanProperty(bound = false)
    public char[] getPassword() {
        Document doc = getDocument();
        Segment txt = new Segment();
        try {
            doc.getText(0, doc.getLength(), txt); // use the non-String API
        } catch (BadLocationException e) {
            return null;
        }
        char[] retValue = new char[txt.count];
        System.arraycopy(txt.array, txt.offset, retValue, 0, txt.count);
        return retValue;
    }

    /**
     * See readObject() and writeObject() in JComponent for more
     * information about serialization in Swing.
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

    // --- variables -----------------------------------------------

    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "PasswordFieldUI";

    private char echoChar;

    private boolean echoCharSet = false;


    /**
     * Returns a string representation of this <code>JPasswordField</code>.
     * This method is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this <code>JPasswordField</code>
     */
    protected String paramString() {
        return super.paramString() +
        ",echoChar=" + echoChar;
    }


    /**
     * This method is a hack to get around the fact that we cannot
     * directly override setUIProperty because part of the inheritance hierarchy
     * goes outside of the javax.swing package, and therefore calling a package
     * private method isn't allowed. This method should return true if the property
     * was handled, and false otherwise.
     */
    boolean customSetUIProperty(String propertyName, Object value) {
        if (propertyName == "echoChar") {
            if (!echoCharSet) {
                setEchoChar((Character)value);
                echoCharSet = false;
            }
            return true;
        }
        return false;
    }

/////////////////
// Accessibility support
////////////////


    /**
     * Returns the <code>AccessibleContext</code> associated with this
     * <code>JPasswordField</code>. For password fields, the
     * <code>AccessibleContext</code> takes the form of an
     * <code>AccessibleJPasswordField</code>.
     * A new <code>AccessibleJPasswordField</code> instance is created
     * if necessary.
     *
     * @return an <code>AccessibleJPasswordField</code> that serves as the
     *         <code>AccessibleContext</code> of this
     *         <code>JPasswordField</code>
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJPasswordField();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JPasswordField</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to password field user-interface
     * elements.
     * <p>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.  As of 1.4, support for long term storage
     * of all JavaBeans
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    protected class AccessibleJPasswordField extends AccessibleJTextField {

        /**
         * Constructs an {@code AccessibleJPasswordField}.
         */
        protected AccessibleJPasswordField() {}

        /**
         * Gets the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         *   object (AccessibleRole.PASSWORD_TEXT)
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PASSWORD_TEXT;
        }

        /**
         * Gets the <code>AccessibleText</code> for the <code>JPasswordField</code>.
         * The returned object also implements the
         * <code>AccessibleExtendedText</code> interface.
         *
         * @return <code>AccessibleText</code> for the JPasswordField
         * @see javax.accessibility.AccessibleContext
         * @see javax.accessibility.AccessibleContext#getAccessibleText
         * @see javax.accessibility.AccessibleText
         * @see javax.accessibility.AccessibleExtendedText
         *
         * @since 1.6
         */
        public AccessibleText getAccessibleText() {
            return this;
        }

        /*
         * Returns a String filled with password echo characters. The String
         * contains one echo character for each character (including whitespace)
         * that the user entered in the JPasswordField.
         */
        private String getEchoString(String str) {
            if (str == null) {
                return null;
            }
            char[] buffer = new char[str.length()];
            Arrays.fill(buffer, getEchoChar());
            return new String(buffer);
        }

        /**
         * Returns the <code>String</code> at a given <code>index</code>.
         *
         * @param part the <code>CHARACTER</code>, <code>WORD</code> or
         * <code>SENTENCE</code> to retrieve
         * @param index an index within the text
         * @return a <code>String</code> if <code>part</code> and
         * <code>index</code> are valid.
         * Otherwise, <code>null</code> is returned
         *
         * @see javax.accessibility.AccessibleText#CHARACTER
         * @see javax.accessibility.AccessibleText#WORD
         * @see javax.accessibility.AccessibleText#SENTENCE
         *
         * @since 1.6
         */
        public String getAtIndex(int part, int index) {
            if (part == AccessibleText.CHARACTER) {
                return getEchoString(super.getAtIndex(part, index));
            } else {
                // Treat the text displayed in the JPasswordField
                // as one word and sentence.
                int length = getDocument().getLength();
                if (index < 0 || index >= length) {
                    return null;
                }
                char[] password = new char[length];
                Arrays.fill(password, getEchoChar());
                return new String(password);
            }
        }

        /**
         * Returns the <code>String</code> after a given <code>index</code>.
         *
         * @param part the <code>CHARACTER</code>, <code>WORD</code> or
         * <code>SENTENCE</code> to retrieve
         * @param index an index within the text
         * @return a <code>String</code> if <code>part</code> and
         * <code>index</code> are valid.
         * Otherwise, <code>null</code> is returned
         *
         * @see javax.accessibility.AccessibleText#CHARACTER
         * @see javax.accessibility.AccessibleText#WORD
         * @see javax.accessibility.AccessibleText#SENTENCE
         *
         * @since 1.6
         */
        public String getAfterIndex(int part, int index) {
            if (part == AccessibleText.CHARACTER) {
                return getEchoString(super.getAfterIndex(part, index));
            } else {
                // There is no word or sentence after the text
                // displayed in the JPasswordField.
                return null;
            }
        }

        /**
         * Returns the <code>String</code> before a given <code>index</code>.
         *
         * @param part the <code>CHARACTER</code>, <code>WORD</code> or
         * <code>SENTENCE</code> to retrieve
         * @param index an index within the text
         * @return a <code>String</code> if <code>part</code> and
         * <code>index</code> are valid.
         * Otherwise, <code>null</code> is returned
         *
         * @see javax.accessibility.AccessibleText#CHARACTER
         * @see javax.accessibility.AccessibleText#WORD
         * @see javax.accessibility.AccessibleText#SENTENCE
         *
         * @since 1.6
         */
        public String getBeforeIndex(int part, int index) {
            if (part == AccessibleText.CHARACTER) {
                return getEchoString(super.getBeforeIndex(part, index));
            } else {
                // There is no word or sentence before the text
                // displayed in the JPasswordField.
                return null;
            }
        }

        /**
         * Returns the text between two <code>indices</code>.
         *
         * @param startIndex the start index in the text
         * @param endIndex the end index in the text
         * @return the text string if the indices are valid.
         * Otherwise, <code>null</code> is returned
         *
         * @since 1.6
         */
        public String getTextRange(int startIndex, int endIndex) {
            String str = super.getTextRange(startIndex, endIndex);
            return getEchoString(str);
        }


        /**
         * Returns the <code>AccessibleTextSequence</code> at a given
         * <code>index</code>.
         *
         * @param part the <code>CHARACTER</code>, <code>WORD</code>,
         * <code>SENTENCE</code>, <code>LINE</code> or <code>ATTRIBUTE_RUN</code> to
         * retrieve
         * @param index an index within the text
         * @return an <code>AccessibleTextSequence</code> specifying the text if
         * <code>part</code> and <code>index</code> are valid.  Otherwise,
         * <code>null</code> is returned
         *
         * @see javax.accessibility.AccessibleText#CHARACTER
         * @see javax.accessibility.AccessibleText#WORD
         * @see javax.accessibility.AccessibleText#SENTENCE
         * @see javax.accessibility.AccessibleExtendedText#LINE
         * @see javax.accessibility.AccessibleExtendedText#ATTRIBUTE_RUN
         *
         * @since 1.6
         */
        public AccessibleTextSequence getTextSequenceAt(int part, int index) {
            if (part == AccessibleText.CHARACTER) {
                AccessibleTextSequence seq = super.getTextSequenceAt(part, index);
                if (seq == null) {
                    return null;
                }
                return new AccessibleTextSequence(seq.startIndex, seq.endIndex,
                                                  getEchoString(seq.text));
            } else {
                // Treat the text displayed in the JPasswordField
                // as one word, sentence, line and attribute run
                int length = getDocument().getLength();
                if (index < 0 || index >= length) {
                    return null;
                }
                char[] password = new char[length];
                Arrays.fill(password, getEchoChar());
                String text = new String(password);
                return new AccessibleTextSequence(0, password.length - 1, text);
            }
        }

        /**
         * Returns the <code>AccessibleTextSequence</code> after a given
         * <code>index</code>.
         *
         * @param part the <code>CHARACTER</code>, <code>WORD</code>,
         * <code>SENTENCE</code>, <code>LINE</code> or <code>ATTRIBUTE_RUN</code> to
         * retrieve
         * @param index an index within the text
         * @return an <code>AccessibleTextSequence</code> specifying the text if
         * <code>part</code> and <code>index</code> are valid.  Otherwise,
         * <code>null</code> is returned
         *
         * @see javax.accessibility.AccessibleText#CHARACTER
         * @see javax.accessibility.AccessibleText#WORD
         * @see javax.accessibility.AccessibleText#SENTENCE
         * @see javax.accessibility.AccessibleExtendedText#LINE
         * @see javax.accessibility.AccessibleExtendedText#ATTRIBUTE_RUN
         *
         * @since 1.6
         */
        public AccessibleTextSequence getTextSequenceAfter(int part, int index) {
            if (part == AccessibleText.CHARACTER) {
                AccessibleTextSequence seq = super.getTextSequenceAfter(part, index);
                if (seq == null) {
                    return null;
                }
                return new AccessibleTextSequence(seq.startIndex, seq.endIndex,
                                                  getEchoString(seq.text));
            } else {
                // There is no word, sentence, line or attribute run
                // after the text displayed in the JPasswordField.
                return null;
            }
        }

        /**
         * Returns the <code>AccessibleTextSequence</code> before a given
         * <code>index</code>.
         *
         * @param part the <code>CHARACTER</code>, <code>WORD</code>,
         * <code>SENTENCE</code>, <code>LINE</code> or <code>ATTRIBUTE_RUN</code> to
         * retrieve
         * @param index an index within the text
         * @return an <code>AccessibleTextSequence</code> specifying the text if
         * <code>part</code> and <code>index</code> are valid.  Otherwise,
         * <code>null</code> is returned
         *
         * @see javax.accessibility.AccessibleText#CHARACTER
         * @see javax.accessibility.AccessibleText#WORD
         * @see javax.accessibility.AccessibleText#SENTENCE
         * @see javax.accessibility.AccessibleExtendedText#LINE
         * @see javax.accessibility.AccessibleExtendedText#ATTRIBUTE_RUN
         *
         * @since 1.6
         */
        public AccessibleTextSequence getTextSequenceBefore(int part, int index) {
            if (part == AccessibleText.CHARACTER) {
                AccessibleTextSequence seq = super.getTextSequenceBefore(part, index);
                if (seq == null) {
                    return null;
                }
                return new AccessibleTextSequence(seq.startIndex, seq.endIndex,
                                                  getEchoString(seq.text));
            } else {
                // There is no word, sentence, line or attribute run
                // before the text displayed in the JPasswordField.
                return null;
            }
        }
    }
}
