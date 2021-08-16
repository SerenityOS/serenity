/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.im.InputMethodRequests;
import java.awt.peer.TextComponentPeer;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.text.BreakIterator;
import java.util.EventListener;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleText;
import javax.swing.text.AttributeSet;

import sun.awt.AWTPermissions;
import sun.awt.InputMethodSupport;

/**
 * The {@code TextComponent} class is the superclass of
 * any component that allows the editing of some text.
 * <p>
 * A text component embodies a string of text.  The
 * {@code TextComponent} class defines a set of methods
 * that determine whether or not this text is editable. If the
 * component is editable, it defines another set of methods
 * that supports a text insertion caret.
 * <p>
 * In addition, the class defines methods that are used
 * to maintain a current <em>selection</em> from the text.
 * The text selection, a substring of the component's text,
 * is the target of editing operations. It is also referred
 * to as the <em>selected text</em>.
 *
 * @author      Sami Shaio
 * @author      Arthur van Hoff
 * @since       1.0
 */
public class TextComponent extends Component implements Accessible {

    /**
     * The value of the text.
     * A {@code null} value is the same as "".
     *
     * @serial
     * @see #setText(String)
     * @see #getText()
     */
    String text;

    /**
     * A boolean indicating whether or not this
     * {@code TextComponent} is editable.
     * It will be {@code true} if the text component
     * is editable and {@code false} if not.
     *
     * @serial
     * @see #isEditable()
     */
    boolean editable = true;

    /**
     * The selection refers to the selected text, and the
     * {@code selectionStart} is the start position
     * of the selected text.
     *
     * @serial
     * @see #getSelectionStart()
     * @see #setSelectionStart(int)
     */
    int selectionStart;

    /**
     * The selection refers to the selected text, and the
     * {@code selectionEnd}
     * is the end position of the selected text.
     *
     * @serial
     * @see #getSelectionEnd()
     * @see #setSelectionEnd(int)
     */
    int selectionEnd;

    /**
     * A flag used to tell whether the background has been set by
     * developer code (as opposed to AWT code).  Used to determine
     * the background color of non-editable TextComponents.
     */
    boolean backgroundSetByClientCode = false;

    /**
     * A list of listeners that will receive events from this object.
     */
    protected transient TextListener textListener;

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -2214773872412987419L;

    /**
     * Constructs a new text component initialized with the
     * specified text. Sets the value of the cursor to
     * {@code Cursor.TEXT_CURSOR}.
     * @param      text       the text to be displayed; if
     *             {@code text} is {@code null}, the empty
     *             string {@code ""} will be displayed
     * @exception  HeadlessException if
     *             {@code GraphicsEnvironment.isHeadless}
     *             returns true
     * @see        java.awt.GraphicsEnvironment#isHeadless
     * @see        java.awt.Cursor
     */
    TextComponent(String text) throws HeadlessException {
        GraphicsEnvironment.checkHeadless();
        this.text = (text != null) ? text : "";
        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    }

    private void enableInputMethodsIfNecessary() {
        if (checkForEnableIM) {
            checkForEnableIM = false;
            try {
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                boolean shouldEnable = false;
                if (toolkit instanceof InputMethodSupport) {
                    shouldEnable = ((InputMethodSupport)toolkit)
                      .enableInputMethodsForTextComponent();
                }
                enableInputMethods(shouldEnable);
            } catch (Exception e) {
                // if something bad happens, just don't enable input methods
            }
        }
    }

    /**
     * Enables or disables input method support for this text component. If input
     * method support is enabled and the text component also processes key events,
     * incoming events are offered to the current input method and will only be
     * processed by the component or dispatched to its listeners if the input method
     * does not consume them. Whether and how input method support for this text
     * component is enabled or disabled by default is implementation dependent.
     *
     * @param enable true to enable, false to disable
     * @see #processKeyEvent
     * @since 1.2
     */
    public void enableInputMethods(boolean enable) {
        checkForEnableIM = false;
        super.enableInputMethods(enable);
    }

    boolean areInputMethodsEnabled() {
        // moved from the constructor above to here and addNotify below,
        // this call will initialize the toolkit if not already initialized.
        if (checkForEnableIM) {
            enableInputMethodsIfNecessary();
        }

        // TextComponent handles key events without touching the eventMask or
        // having a key listener, so just check whether the flag is set
        return (eventMask & AWTEvent.INPUT_METHODS_ENABLED_MASK) != 0;
    }

    public InputMethodRequests getInputMethodRequests() {
        TextComponentPeer peer = (TextComponentPeer)this.peer;
        if (peer != null) return peer.getInputMethodRequests();
        else return null;
    }



    /**
     * Makes this Component displayable by connecting it to a
     * native screen resource.
     * This method is called internally by the toolkit and should
     * not be called directly by programs.
     * @see       java.awt.TextComponent#removeNotify
     */
    public void addNotify() {
        super.addNotify();
        enableInputMethodsIfNecessary();
    }

    /**
     * Removes the {@code TextComponent}'s peer.
     * The peer allows us to modify the appearance of the
     * {@code TextComponent} without changing its
     * functionality.
     */
    public void removeNotify() {
        synchronized (getTreeLock()) {
            TextComponentPeer peer = (TextComponentPeer)this.peer;
            if (peer != null) {
                text = peer.getText();
                selectionStart = peer.getSelectionStart();
                selectionEnd = peer.getSelectionEnd();
            }
            super.removeNotify();
        }
    }

    /**
     * Sets the text that is presented by this
     * text component to be the specified text.
     * @param       t   the new text;
     *                  if this parameter is {@code null} then
     *                  the text is set to the empty string ""
     * @see         java.awt.TextComponent#getText
     */
    public synchronized void setText(String t) {
        text = (t != null) ? t : "";
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        TextComponentPeer peer = (TextComponentPeer) this.peer;
        if (peer != null && !text.equals(peer.getText())) {
            // Please note that we do not want to post an event
            // if TextArea.setText() or TextField.setText() replaces text
            // by same text, that is, if component's text remains unchanged.
            peer.setText(text);
        }
        if (selectionStart != selectionEnd) {
            select(selectionStart, selectionEnd);
        }
    }

    /**
     * Returns the text that is presented by this text component.
     * By default, this is an empty string.
     *
     * @return the value of this {@code TextComponent}
     * @see     java.awt.TextComponent#setText
     */
    public synchronized String getText() {
        TextComponentPeer peer = (TextComponentPeer)this.peer;
        if (peer != null) {
            text = peer.getText();
        }
        return text;
    }

    /**
     * Returns the selected text from the text that is
     * presented by this text component.
     * @return      the selected text of this text component
     * @see         java.awt.TextComponent#select
     */
    public synchronized String getSelectedText() {
        return getText().substring(getSelectionStart(), getSelectionEnd());
    }

    /**
     * Indicates whether or not this text component is editable.
     * @return     {@code true} if this text component is
     *                  editable; {@code false} otherwise.
     * @see        java.awt.TextComponent#setEditable
     * @since      1.0
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Sets the flag that determines whether or not this
     * text component is editable.
     * <p>
     * If the flag is set to {@code true}, this text component
     * becomes user editable. If the flag is set to {@code false},
     * the user cannot change the text of this text component.
     * By default, non-editable text components have a background color
     * of SystemColor.control.  This default can be overridden by
     * calling setBackground.
     *
     * @param     b   a flag indicating whether this text component
     *                      is user editable.
     * @see       java.awt.TextComponent#isEditable
     * @since     1.0
     */
    public synchronized void setEditable(boolean b) {
        if (editable == b) {
            return;
        }

        editable = b;
        TextComponentPeer peer = (TextComponentPeer)this.peer;
        if (peer != null) {
            peer.setEditable(b);
        }
    }

    /**
     * Gets the background color of this text component.
     *
     * By default, non-editable text components have a background color
     * of SystemColor.control.  This default can be overridden by
     * calling setBackground.
     *
     * @return This text component's background color.
     *         If this text component does not have a background color,
     *         the background color of its parent is returned.
     * @see #setBackground(Color)
     * @since 1.0
     */
    public Color getBackground() {
        if (!editable && !backgroundSetByClientCode) {
            return SystemColor.control;
        }

        return super.getBackground();
    }

    /**
     * Sets the background color of this text component.
     *
     * @param c The color to become this text component's color.
     *        If this parameter is null then this text component
     *        will inherit the background color of its parent.
     * @see #getBackground()
     * @since 1.0
     */
    public void setBackground(Color c) {
        backgroundSetByClientCode = true;
        super.setBackground(c);
    }

    /**
     * Gets the start position of the selected text in
     * this text component.
     * @return      the start position of the selected text
     * @see         java.awt.TextComponent#setSelectionStart
     * @see         java.awt.TextComponent#getSelectionEnd
     */
    public synchronized int getSelectionStart() {
        TextComponentPeer peer = (TextComponentPeer)this.peer;
        if (peer != null) {
            selectionStart = peer.getSelectionStart();
        }
        return selectionStart;
    }

    /**
     * Sets the selection start for this text component to
     * the specified position. The new start point is constrained
     * to be at or before the current selection end. It also
     * cannot be set to less than zero, the beginning of the
     * component's text.
     * If the caller supplies a value for {@code selectionStart}
     * that is out of bounds, the method enforces these constraints
     * silently, and without failure.
     * @param       selectionStart   the start position of the
     *                        selected text
     * @see         java.awt.TextComponent#getSelectionStart
     * @see         java.awt.TextComponent#setSelectionEnd
     * @since       1.1
     */
    public synchronized void setSelectionStart(int selectionStart) {
        /* Route through select method to enforce consistent policy
         * between selectionStart and selectionEnd.
         */
        select(selectionStart, getSelectionEnd());
    }

    /**
     * Gets the end position of the selected text in
     * this text component.
     * @return      the end position of the selected text
     * @see         java.awt.TextComponent#setSelectionEnd
     * @see         java.awt.TextComponent#getSelectionStart
     */
    public synchronized int getSelectionEnd() {
        TextComponentPeer peer = (TextComponentPeer)this.peer;
        if (peer != null) {
            selectionEnd = peer.getSelectionEnd();
        }
        return selectionEnd;
    }

    /**
     * Sets the selection end for this text component to
     * the specified position. The new end point is constrained
     * to be at or after the current selection start. It also
     * cannot be set beyond the end of the component's text.
     * If the caller supplies a value for {@code selectionEnd}
     * that is out of bounds, the method enforces these constraints
     * silently, and without failure.
     * @param       selectionEnd   the end position of the
     *                        selected text
     * @see         java.awt.TextComponent#getSelectionEnd
     * @see         java.awt.TextComponent#setSelectionStart
     * @since       1.1
     */
    public synchronized void setSelectionEnd(int selectionEnd) {
        /* Route through select method to enforce consistent policy
         * between selectionStart and selectionEnd.
         */
        select(getSelectionStart(), selectionEnd);
    }

    /**
     * Selects the text between the specified start and end positions.
     * <p>
     * This method sets the start and end positions of the
     * selected text, enforcing the restriction that the start position
     * must be greater than or equal to zero.  The end position must be
     * greater than or equal to the start position, and less than or
     * equal to the length of the text component's text.  The
     * character positions are indexed starting with zero.
     * The length of the selection is
     * {@code endPosition} - {@code startPosition}, so the
     * character at {@code endPosition} is not selected.
     * If the start and end positions of the selected text are equal,
     * all text is deselected.
     * <p>
     * If the caller supplies values that are inconsistent or out of
     * bounds, the method enforces these constraints silently, and
     * without failure. Specifically, if the start position or end
     * position is greater than the length of the text, it is reset to
     * equal the text length. If the start position is less than zero,
     * it is reset to zero, and if the end position is less than the
     * start position, it is reset to the start position.
     *
     * @param        selectionStart the zero-based index of the first
     *               character ({@code char} value) to be selected
     * @param        selectionEnd the zero-based end position of the
     *               text to be selected; the character ({@code char} value) at
     *               {@code selectionEnd} is not selected
     * @see          java.awt.TextComponent#setSelectionStart
     * @see          java.awt.TextComponent#setSelectionEnd
     * @see          java.awt.TextComponent#selectAll
     */
    public synchronized void select(int selectionStart, int selectionEnd) {
        String text = getText();
        if (selectionStart < 0) {
            selectionStart = 0;
        }
        if (selectionStart > text.length()) {
            selectionStart = text.length();
        }
        if (selectionEnd > text.length()) {
            selectionEnd = text.length();
        }
        if (selectionEnd < selectionStart) {
            selectionEnd = selectionStart;
        }

        this.selectionStart = selectionStart;
        this.selectionEnd = selectionEnd;

        TextComponentPeer peer = (TextComponentPeer)this.peer;
        if (peer != null) {
            peer.select(selectionStart, selectionEnd);
        }
    }

    /**
     * Selects all the text in this text component.
     * @see        java.awt.TextComponent#select
     */
    public synchronized void selectAll() {
        this.selectionStart = 0;
        this.selectionEnd = getText().length();

        TextComponentPeer peer = (TextComponentPeer)this.peer;
        if (peer != null) {
            peer.select(selectionStart, selectionEnd);
        }
    }

    /**
     * Sets the position of the text insertion caret.
     * The caret position is constrained to be between 0
     * and the last character of the text, inclusive.
     * If the passed-in value is greater than this range,
     * the value is set to the last character (or 0 if
     * the {@code TextComponent} contains no text)
     * and no error is returned.  If the passed-in value is
     * less than 0, an {@code IllegalArgumentException}
     * is thrown.
     *
     * @param        position the position of the text insertion caret
     * @exception    IllegalArgumentException if {@code position}
     *               is less than zero
     * @since        1.1
     */
    public synchronized void setCaretPosition(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("position less than zero.");
        }

        int maxposition = getText().length();
        if (position > maxposition) {
            position = maxposition;
        }

        TextComponentPeer peer = (TextComponentPeer)this.peer;
        if (peer != null) {
            peer.setCaretPosition(position);
        } else {
            select(position, position);
        }
    }

    /**
     * Returns the position of the text insertion caret.
     * The caret position is constrained to be between 0
     * and the last character of the text, inclusive.
     * If the text or caret have not been set, the default
     * caret position is 0.
     *
     * @return       the position of the text insertion caret
     * @see #setCaretPosition(int)
     * @since        1.1
     */
    public synchronized int getCaretPosition() {
        TextComponentPeer peer = (TextComponentPeer)this.peer;
        int position = 0;

        if (peer != null) {
            position = peer.getCaretPosition();
        } else {
            position = selectionStart;
        }
        int maxposition = getText().length();
        if (position > maxposition) {
            position = maxposition;
        }
        return position;
    }

    /**
     * Adds the specified text event listener to receive text events
     * from this text component.
     * If {@code l} is {@code null}, no exception is
     * thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param l the text event listener
     * @see             #removeTextListener
     * @see             #getTextListeners
     * @see             java.awt.event.TextListener
     */
    public synchronized void addTextListener(TextListener l) {
        if (l == null) {
            return;
        }
        textListener = AWTEventMulticaster.add(textListener, l);
        newEventsOnly = true;
    }

    /**
     * Removes the specified text event listener so that it no longer
     * receives text events from this text component
     * If {@code l} is {@code null}, no exception is
     * thrown and no action is performed.
     * <p>Refer to <a href="doc-files/AWTThreadIssues.html#ListenersThreads"
     * >AWT Threading Issues</a> for details on AWT's threading model.
     *
     * @param           l     the text listener
     * @see             #addTextListener
     * @see             #getTextListeners
     * @see             java.awt.event.TextListener
     * @since           1.1
     */
    public synchronized void removeTextListener(TextListener l) {
        if (l == null) {
            return;
        }
        textListener = AWTEventMulticaster.remove(textListener, l);
    }

    /**
     * Returns an array of all the text listeners
     * registered on this text component.
     *
     * @return all of this text component's {@code TextListener}s
     *         or an empty array if no text
     *         listeners are currently registered
     *
     *
     * @see #addTextListener
     * @see #removeTextListener
     * @since 1.4
     */
    public synchronized TextListener[] getTextListeners() {
        return getListeners(TextListener.class);
    }

    /**
     * Returns an array of all the objects currently registered
     * as <code><em>Foo</em>Listener</code>s
     * upon this {@code TextComponent}.
     * <code><em>Foo</em>Listener</code>s are registered using the
     * <code>add<em>Foo</em>Listener</code> method.
     *
     * <p>
     * You can specify the {@code listenerType} argument
     * with a class literal, such as
     * <code><em>Foo</em>Listener.class</code>.
     * For example, you can query a
     * {@code TextComponent t}
     * for its text listeners with the following code:
     *
     * <pre>TextListener[] tls = (TextListener[])(t.getListeners(TextListener.class));</pre>
     *
     * If no such listeners exist, this method returns an empty array.
     *
     * @param listenerType the type of listeners requested; this parameter
     *          should specify an interface that descends from
     *          {@code java.util.EventListener}
     * @return an array of all objects registered as
     *          <code><em>Foo</em>Listener</code>s on this text component,
     *          or an empty array if no such
     *          listeners have been added
     * @exception ClassCastException if {@code listenerType}
     *          doesn't specify a class or interface that implements
     *          {@code java.util.EventListener}
     *
     * @see #getTextListeners
     * @since 1.3
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        EventListener l = null;
        if  (listenerType == TextListener.class) {
            l = textListener;
        } else {
            return super.getListeners(listenerType);
        }
        return AWTEventMulticaster.getListeners(l, listenerType);
    }

    // REMIND: remove when filtering is done at lower level
    boolean eventEnabled(AWTEvent e) {
        if (e.id == TextEvent.TEXT_VALUE_CHANGED) {
            if ((eventMask & AWTEvent.TEXT_EVENT_MASK) != 0 ||
                textListener != null) {
                return true;
            }
            return false;
        }
        return super.eventEnabled(e);
    }

    /**
     * Processes events on this text component. If the event is a
     * {@code TextEvent}, it invokes the {@code processTextEvent}
     * method else it invokes its superclass's {@code processEvent}.
     * <p>Note that if the event parameter is {@code null}
     * the behavior is unspecified and may result in an
     * exception.
     *
     * @param e the event
     */
    protected void processEvent(AWTEvent e) {
        if (e instanceof TextEvent) {
            processTextEvent((TextEvent)e);
            return;
        }
        super.processEvent(e);
    }

    /**
     * Processes text events occurring on this text component by
     * dispatching them to any registered {@code TextListener} objects.
     * <p>
     * NOTE: This method will not be called unless text events
     * are enabled for this component. This happens when one of the
     * following occurs:
     * <ul>
     * <li>A {@code TextListener} object is registered
     * via {@code addTextListener}
     * <li>Text events are enabled via {@code enableEvents}
     * </ul>
     * <p>Note that if the event parameter is {@code null}
     * the behavior is unspecified and may result in an
     * exception.
     *
     * @param e the text event
     * @see Component#enableEvents
     */
    protected void processTextEvent(TextEvent e) {
        TextListener listener = textListener;
        if (listener != null) {
            int id = e.getID();
            switch (id) {
            case TextEvent.TEXT_VALUE_CHANGED:
                listener.textValueChanged(e);
                break;
            }
        }
    }

    /**
     * Returns a string representing the state of this
     * {@code TextComponent}. This
     * method is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not be
     * {@code null}.
     *
     * @return      the parameter string of this text component
     */
    protected String paramString() {
        String str = super.paramString() + ",text=" + getText();
        if (editable) {
            str += ",editable";
        }
        return str + ",selection=" + getSelectionStart() + "-" + getSelectionEnd();
    }

    /**
     * Assigns a valid value to the canAccessClipboard instance variable.
     */
    private boolean canAccessClipboard() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) return true;
        try {
            sm.checkPermission(AWTPermissions.ACCESS_CLIPBOARD_PERMISSION);
            return true;
        } catch (SecurityException e) {}
        return false;
    }

    /*
     * Serialization support.
     */
    /**
     * The textComponent SerializedDataVersion.
     *
     * @serial
     */
    private int textComponentSerializedDataVersion = 1;

    /**
     * Writes default serializable fields to stream.  Writes
     * a list of serializable TextListener(s) as optional data.
     * The non-serializable TextListener(s) are detected and
     * no attempt is made to serialize them.
     *
     * @serialData Null terminated sequence of zero or more pairs.
     *             A pair consists of a String and Object.
     *             The String indicates the type of object and
     *             is one of the following :
     *             textListenerK indicating and TextListener object.
     *
     * @param  s the {@code ObjectOutputStream} to write
     * @throws IOException if an I/O error occurs
     * @see AWTEventMulticaster#save(ObjectOutputStream, String, EventListener)
     * @see java.awt.Component#textListenerK
     */
    @Serial
    private void writeObject(java.io.ObjectOutputStream s)
      throws IOException
    {
        // Serialization support.  Since the value of the fields
        // selectionStart, selectionEnd, and text aren't necessarily
        // up to date, we sync them up with the peer before serializing.
        TextComponentPeer peer = (TextComponentPeer)this.peer;
        if (peer != null) {
            text = peer.getText();
            selectionStart = peer.getSelectionStart();
            selectionEnd = peer.getSelectionEnd();
        }

        s.defaultWriteObject();

        AWTEventMulticaster.save(s, textListenerK, textListener);
        s.writeObject(null);
    }

    /**
     * Read the ObjectInputStream, and if it isn't null,
     * add a listener to receive text events fired by the
     * TextComponent.  Unrecognized keys or values will be
     * ignored.
     *
     * @param  s the {@code ObjectInputStream} to read
     * @throws ClassNotFoundException if the class of a serialized object could
     *         not be found
     * @throws IOException if an I/O error occurs
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *         returns {@code true}
     * @see #removeTextListener
     * @see #addTextListener
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    @Serial
    private void readObject(ObjectInputStream s)
        throws ClassNotFoundException, IOException, HeadlessException
    {
        GraphicsEnvironment.checkHeadless();
        s.defaultReadObject();

        // Make sure the state we just read in for text,
        // selectionStart and selectionEnd has legal values
        this.text = (text != null) ? text : "";
        select(selectionStart, selectionEnd);

        Object keyOrNull;
        while(null != (keyOrNull = s.readObject())) {
            String key = ((String)keyOrNull).intern();

            if (textListenerK == key) {
                addTextListener((TextListener)(s.readObject()));
            } else {
                // skip value for unrecognized key
                s.readObject();
            }
        }
        enableInputMethodsIfNecessary();
    }


/////////////////
// Accessibility support
////////////////

    /**
     * Gets the AccessibleContext associated with this TextComponent.
     * For text components, the AccessibleContext takes the form of an
     * AccessibleAWTTextComponent.
     * A new AccessibleAWTTextComponent instance is created if necessary.
     *
     * @return an AccessibleAWTTextComponent that serves as the
     *         AccessibleContext of this TextComponent
     * @since 1.3
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleAWTTextComponent();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * {@code TextComponent} class.  It provides an implementation of the
     * Java Accessibility API appropriate to text component user-interface
     * elements.
     * @since 1.3
     */
    protected class AccessibleAWTTextComponent extends AccessibleAWTComponent
        implements AccessibleText, TextListener
    {
        /**
         * Use serialVersionUID from JDK 1.3 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 3631432373506317811L;

        /**
         * Constructs an AccessibleAWTTextComponent.  Adds a listener to track
         * caret change.
         */
        public AccessibleAWTTextComponent() {
            TextComponent.this.addTextListener(this);
        }

        /**
         * TextListener notification of a text value change.
         */
        public void textValueChanged(TextEvent textEvent)  {
            Integer cpos = Integer.valueOf(TextComponent.this.getCaretPosition());
            firePropertyChange(ACCESSIBLE_TEXT_PROPERTY, null, cpos);
        }

        /**
         * Gets the state set of the TextComponent.
         * The AccessibleStateSet of an object is composed of a set of
         * unique AccessibleStates.  A change in the AccessibleStateSet
         * of an object will cause a PropertyChangeEvent to be fired
         * for the AccessibleContext.ACCESSIBLE_STATE_PROPERTY property.
         *
         * @return an instance of AccessibleStateSet containing the
         * current state set of the object
         * @see AccessibleStateSet
         * @see AccessibleState
         * @see #addPropertyChangeListener
         */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states = super.getAccessibleStateSet();
            if (TextComponent.this.isEditable()) {
                states.add(AccessibleState.EDITABLE);
            }
            return states;
        }


        /**
         * Gets the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object (AccessibleRole.TEXT)
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.TEXT;
        }

        /**
         * Get the AccessibleText associated with this object.  In the
         * implementation of the Java Accessibility API for this class,
         * return this object, which is responsible for implementing the
         * AccessibleText interface on behalf of itself.
         *
         * @return this object
         */
        public AccessibleText getAccessibleText() {
            return this;
        }


        // --- interface AccessibleText methods ------------------------

        /**
         * Many of these methods are just convenience methods; they
         * just call the equivalent on the parent
         */

        /**
         * Given a point in local coordinates, return the zero-based index
         * of the character under that Point.  If the point is invalid,
         * this method returns -1.
         *
         * @param p the Point in local coordinates
         * @return the zero-based index of the character under Point p.
         */
        public int getIndexAtPoint(Point p) {
            return -1;
        }

        /**
         * Determines the bounding box of the character at the given
         * index into the string.  The bounds are returned in local
         * coordinates.  If the index is invalid a null rectangle
         * is returned.
         *
         * @param i the index into the String &gt;= 0
         * @return the screen coordinates of the character's bounding box
         */
        public Rectangle getCharacterBounds(int i) {
            return null;
        }

        /**
         * Returns the number of characters (valid indices)
         *
         * @return the number of characters &gt;= 0
         */
        public int getCharCount() {
            return TextComponent.this.getText().length();
        }

        /**
         * Returns the zero-based offset of the caret.
         *
         * Note: The character to the right of the caret will have the
         * same index value as the offset (the caret is between
         * two characters).
         *
         * @return the zero-based offset of the caret.
         */
        public int getCaretPosition() {
            return TextComponent.this.getCaretPosition();
        }

        /**
         * Returns the AttributeSet for a given character (at a given index).
         *
         * @param i the zero-based index into the text
         * @return the AttributeSet of the character
         */
        public AttributeSet getCharacterAttribute(int i) {
            return null; // No attributes in TextComponent
        }

        /**
         * Returns the start offset within the selected text.
         * If there is no selection, but there is
         * a caret, the start and end offsets will be the same.
         * Return 0 if the text is empty, or the caret position
         * if no selection.
         *
         * @return the index into the text of the start of the selection &gt;= 0
         */
        public int getSelectionStart() {
            return TextComponent.this.getSelectionStart();
        }

        /**
         * Returns the end offset within the selected text.
         * If there is no selection, but there is
         * a caret, the start and end offsets will be the same.
         * Return 0 if the text is empty, or the caret position
         * if no selection.
         *
         * @return the index into the text of the end of the selection &gt;= 0
         */
        public int getSelectionEnd() {
            return TextComponent.this.getSelectionEnd();
        }

        /**
         * Returns the portion of the text that is selected.
         *
         * @return the text, null if no selection
         */
        public String getSelectedText() {
            String selText = TextComponent.this.getSelectedText();
            // Fix for 4256662
            if (selText == null || selText.isEmpty()) {
                return null;
            }
            return selText;
        }

        /**
         * Returns the String at a given index.
         *
         * @param part the AccessibleText.CHARACTER, AccessibleText.WORD,
         * or AccessibleText.SENTENCE to retrieve
         * @param index an index within the text &gt;= 0
         * @return the letter, word, or sentence,
         *   null for an invalid index or part
         */
        public String getAtIndex(int part, int index) {
            if (index < 0 || index >= TextComponent.this.getText().length()) {
                return null;
            }
            switch (part) {
            case AccessibleText.CHARACTER:
                return TextComponent.this.getText().substring(index, index+1);
            case AccessibleText.WORD:  {
                    String s = TextComponent.this.getText();
                    BreakIterator words = BreakIterator.getWordInstance();
                    words.setText(s);
                    int end = words.following(index);
                    return s.substring(words.previous(), end);
                }
            case AccessibleText.SENTENCE:  {
                    String s = TextComponent.this.getText();
                    BreakIterator sentence = BreakIterator.getSentenceInstance();
                    sentence.setText(s);
                    int end = sentence.following(index);
                    return s.substring(sentence.previous(), end);
                }
            default:
                return null;
            }
        }

        private static final boolean NEXT = true;
        private static final boolean PREVIOUS = false;

        /**
         * Needed to unify forward and backward searching.
         * The method assumes that s is the text assigned to words.
         */
        private int findWordLimit(int index, BreakIterator words, boolean direction,
                                         String s) {
            // Fix for 4256660 and 4256661.
            // Words iterator is different from character and sentence iterators
            // in that end of one word is not necessarily start of another word.
            // Please see java.text.BreakIterator JavaDoc. The code below is
            // based on nextWordStartAfter example from BreakIterator.java.
            int last = (direction == NEXT) ? words.following(index)
                                           : words.preceding(index);
            int current = (direction == NEXT) ? words.next()
                                              : words.previous();
            while (current != BreakIterator.DONE) {
                for (int p = Math.min(last, current); p < Math.max(last, current); p++) {
                    if (Character.isLetter(s.charAt(p))) {
                        return last;
                    }
                }
                last = current;
                current = (direction == NEXT) ? words.next()
                                              : words.previous();
            }
            return BreakIterator.DONE;
        }

        /**
         * Returns the String after a given index.
         *
         * @param part the AccessibleText.CHARACTER, AccessibleText.WORD,
         * or AccessibleText.SENTENCE to retrieve
         * @param index an index within the text &gt;= 0
         * @return the letter, word, or sentence, null for an invalid
         *  index or part
         */
        public String getAfterIndex(int part, int index) {
            if (index < 0 || index >= TextComponent.this.getText().length()) {
                return null;
            }
            switch (part) {
            case AccessibleText.CHARACTER:
                if (index+1 >= TextComponent.this.getText().length()) {
                   return null;
                }
                return TextComponent.this.getText().substring(index+1, index+2);
            case AccessibleText.WORD:  {
                    String s = TextComponent.this.getText();
                    BreakIterator words = BreakIterator.getWordInstance();
                    words.setText(s);
                    int start = findWordLimit(index, words, NEXT, s);
                    if (start == BreakIterator.DONE || start >= s.length()) {
                        return null;
                    }
                    int end = words.following(start);
                    if (end == BreakIterator.DONE || end >= s.length()) {
                        return null;
                    }
                    return s.substring(start, end);
                }
            case AccessibleText.SENTENCE:  {
                    String s = TextComponent.this.getText();
                    BreakIterator sentence = BreakIterator.getSentenceInstance();
                    sentence.setText(s);
                    int start = sentence.following(index);
                    if (start == BreakIterator.DONE || start >= s.length()) {
                        return null;
                    }
                    int end = sentence.following(start);
                    if (end == BreakIterator.DONE || end >= s.length()) {
                        return null;
                    }
                    return s.substring(start, end);
                }
            default:
                return null;
            }
        }


        /**
         * Returns the String before a given index.
         *
         * @param part the AccessibleText.CHARACTER, AccessibleText.WORD,
         *   or AccessibleText.SENTENCE to retrieve
         * @param index an index within the text &gt;= 0
         * @return the letter, word, or sentence, null for an invalid index
         *  or part
         */
        public String getBeforeIndex(int part, int index) {
            if (index < 0 || index > TextComponent.this.getText().length()-1) {
                return null;
            }
            switch (part) {
            case AccessibleText.CHARACTER:
                if (index == 0) {
                    return null;
                }
                return TextComponent.this.getText().substring(index-1, index);
            case AccessibleText.WORD:  {
                    String s = TextComponent.this.getText();
                    BreakIterator words = BreakIterator.getWordInstance();
                    words.setText(s);
                    int end = findWordLimit(index, words, PREVIOUS, s);
                    if (end == BreakIterator.DONE) {
                        return null;
                    }
                    int start = words.preceding(end);
                    if (start == BreakIterator.DONE) {
                        return null;
                    }
                    return s.substring(start, end);
                }
            case AccessibleText.SENTENCE:  {
                    String s = TextComponent.this.getText();
                    BreakIterator sentence = BreakIterator.getSentenceInstance();
                    sentence.setText(s);
                    int end = sentence.following(index);
                    end = sentence.previous();
                    int start = sentence.previous();
                    if (start == BreakIterator.DONE) {
                        return null;
                    }
                    return s.substring(start, end);
                }
            default:
                return null;
            }
        }
    }  // end of AccessibleAWTTextComponent

    /**
     * Whether support of input methods should be checked or not.
     */
    private boolean checkForEnableIM = true;
}
