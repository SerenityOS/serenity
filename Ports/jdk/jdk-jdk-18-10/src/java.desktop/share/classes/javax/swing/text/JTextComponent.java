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
package javax.swing.text;

import com.sun.beans.util.Cache;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.beans.JavaBean;
import java.beans.BeanProperty;
import java.beans.Transient;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

import java.util.concurrent.*;

import java.io.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.awt.datatransfer.*;
import java.awt.im.InputContext;
import java.awt.im.InputMethodRequests;
import java.awt.font.TextHitInfo;
import java.awt.font.TextAttribute;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.awt.print.Printable;
import java.awt.print.PrinterException;

import javax.print.PrintService;
import javax.print.attribute.PrintRequestAttributeSet;

import java.text.*;
import java.text.AttributedCharacterIterator.Attribute;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;

import javax.accessibility.*;

import javax.print.attribute.*;

import sun.awt.AppContext;


import sun.swing.PrintingStatus;
import sun.swing.SwingUtilities2;
import sun.swing.text.TextComponentPrintable;
import sun.swing.SwingAccessor;

/**
 * <code>JTextComponent</code> is the base class for swing text
 * components.  It tries to be compatible with the
 * <code>java.awt.TextComponent</code> class
 * where it can reasonably do so.  Also provided are other services
 * for additional flexibility (beyond the pluggable UI and bean
 * support).
 * You can find information on how to use the functionality
 * this class provides in
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/generaltext.html">General Rules for Using Text Components</a>,
 * a section in <em>The Java Tutorial.</em>
 *
 * <dl>
 * <dt><b>Caret Changes</b>
 * <dd>
 * The caret is a pluggable object in swing text components.
 * Notification of changes to the caret position and the selection
 * are sent to implementations of the <code>CaretListener</code>
 * interface that have been registered with the text component.
 * The UI will install a default caret unless a customized caret
 * has been set. <br>
 * By default the caret tracks all the document changes
 * performed on the Event Dispatching Thread and updates it's position
 * accordingly if an insertion occurs before or at the caret position
 * or a removal occurs before the caret position. <code>DefaultCaret</code>
 * tries to make itself visible which may lead to scrolling
 * of a text component within <code>JScrollPane</code>. The default caret
 * behavior can be changed by the {@link DefaultCaret#setUpdatePolicy} method.
 * <br>
 * <b>Note</b>: Non-editable text components also have a caret though
 * it may not be painted.
 *
 * <dt><b>Commands</b>
 * <dd>
 * Text components provide a number of commands that can be used
 * to manipulate the component.  This is essentially the way that
 * the component expresses its capabilities.  These are expressed
 * in terms of the swing <code>Action</code> interface,
 * using the <code>TextAction</code> implementation.
 * The set of commands supported by the text component can be
 * found with the {@link #getActions} method.  These actions
 * can be bound to key events, fired from buttons, etc.
 *
 * <dt><b>Text Input</b>
 * <dd>
 * The text components support flexible and internationalized text input, using
 * keymaps and the input method framework, while maintaining compatibility with
 * the AWT listener model.
 * <p>
 * A {@link javax.swing.text.Keymap} lets an application bind key
 * strokes to actions.
 * In order to allow keymaps to be shared across multiple text components, they
 * can use actions that extend <code>TextAction</code>.
 * <code>TextAction</code> can determine which <code>JTextComponent</code>
 * most recently has or had focus and therefore is the subject of
 * the action (In the case that the <code>ActionEvent</code>
 * sent to the action doesn't contain the target text component as its source).
 * <p>
 * The {@extLink imf_overview Input Method Framework}
 * lets text components interact with input methods, separate software
 * components that preprocess events to let users enter thousands of
 * different characters using keyboards with far fewer keys.
 * <code>JTextComponent</code> is an <em>active client</em> of
 * the framework, so it implements the preferred user interface for interacting
 * with input methods. As a consequence, some key events do not reach the text
 * component because they are handled by an input method, and some text input
 * reaches the text component as committed text within an {@link
 * java.awt.event.InputMethodEvent} instead of as a key event.
 * The complete text input is the combination of the characters in
 * <code>keyTyped</code> key events and committed text in input method events.
 * <p>
 * The AWT listener model lets applications attach event listeners to
 * components in order to bind events to actions. Swing encourages the
 * use of keymaps instead of listeners, but maintains compatibility
 * with listeners by giving the listeners a chance to steal an event
 * by consuming it.
 * <p>
 * Keyboard event and input method events are handled in the following stages,
 * with each stage capable of consuming the event:
 *
 * <table class="striped">
 * <caption>Stages of keyboard and input method event handling</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Stage
 *     <th scope="col">KeyEvent
 *     <th scope="col">InputMethodEvent
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row">1.
 *     <td>input methods
 *     <td>(generated here)
 *   <tr>
 *     <th scope="row">2.
 *     <td>focus manager
 *     <td>
 *   </tr>
 *   <tr>
 *     <th scope="row">3.
 *     <td>registered key listeners
 *     <td>registered input method listeners
 *   <tr>
 *     <th scope="row">4.
 *     <td>
 *     <td>input method handling in JTextComponent
 *   <tr>
 *     <th scope="row">5.
 *     <td colspan=2>keymap handling using the current keymap
 *   <tr>
 *     <th scope="row">6.
 *     <td>keyboard handling in JComponent (e.g. accelerators, component
 *     navigation, etc.)
 *     <td>
 * </tbody>
 * </table>
 * <p>
 * To maintain compatibility with applications that listen to key
 * events but are not aware of input method events, the input
 * method handling in stage 4 provides a compatibility mode for
 * components that do not process input method events. For these
 * components, the committed text is converted to keyTyped key events
 * and processed in the key event pipeline starting at stage 3
 * instead of in the input method event pipeline.
 * <p>
 * By default the component will create a keymap (named <b>DEFAULT_KEYMAP</b>)
 * that is shared by all JTextComponent instances as the default keymap.
 * Typically a look-and-feel implementation will install a different keymap
 * that resolves to the default keymap for those bindings not found in the
 * different keymap. The minimal bindings include:
 * <ul>
 * <li>inserting content into the editor for the
 *  printable keys.
 * <li>removing content with the backspace and del
 *  keys.
 * <li>caret movement forward and backward
 * </ul>
 *
 * <dt><b>Model/View Split</b>
 * <dd>
 * The text components have a model-view split.  A text component pulls
 * together the objects used to represent the model, view, and controller.
 * The text document model may be shared by other views which act as observers
 * of the model (e.g. a document may be shared by multiple components).
 *
 * <p style="text-align:center"><img src="doc-files/editor.gif" alt="Diagram showing interaction between Controller, Document, events, and ViewFactory"
 *                  HEIGHT=358 WIDTH=587></p>
 *
 * <p>
 * The model is defined by the {@link Document} interface.
 * This is intended to provide a flexible text storage mechanism
 * that tracks change during edits and can be extended to more sophisticated
 * models.  The model interfaces are meant to capture the capabilities of
 * expression given by SGML, a system used to express a wide variety of
 * content.
 * Each modification to the document causes notification of the
 * details of the change to be sent to all observers in the form of a
 * {@link DocumentEvent} which allows the views to stay up to date with the model.
 * This event is sent to observers that have implemented the
 * {@link DocumentListener}
 * interface and registered interest with the model being observed.
 *
 * <dt><b>Location Information</b>
 * <dd>
 * The capability of determining the location of text in
 * the view is provided.  There are two methods, {@link #modelToView}
 * and {@link #viewToModel} for determining this information.
 *
 * <dt><b>Undo/Redo support</b>
 * <dd>
 * Support for an edit history mechanism is provided to allow
 * undo/redo operations.  The text component does not itself
 * provide the history buffer by default, but does provide
 * the <code>UndoableEdit</code> records that can be used in conjunction
 * with a history buffer to provide the undo/redo support.
 * The support is provided by the Document model, which allows
 * one to attach UndoableEditListener implementations.
 *
 * <dt><b>Thread Safety</b>
 * <dd>
 * The swing text components provide some support of thread
 * safe operations.  Because of the high level of configurability
 * of the text components, it is possible to circumvent the
 * protection provided.  The protection primarily comes from
 * the model, so the documentation of <code>AbstractDocument</code>
 * describes the assumptions of the protection provided.
 * The methods that are safe to call asynchronously are marked
 * with comments.
 *
 * <dt><b>Newlines</b>
 * <dd>
 * For a discussion on how newlines are handled, see
 * <a href="DefaultEditorKit.html">DefaultEditorKit</a>.
 *
 *
 * <dt><b>Printing support</b>
 * <dd>
 * Several {@link #print print} methods are provided for basic
 * document printing.  If more advanced printing is needed, use the
 * {@link #getPrintable} method.
 * </dl>
 *
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
 * @author Igor Kushnirskiy (printing support)
 * @see Document
 * @see DocumentEvent
 * @see DocumentListener
 * @see Caret
 * @see CaretEvent
 * @see CaretListener
 * @see TextUI
 * @see View
 * @see ViewFactory
 */
@JavaBean(defaultProperty = "UI")
@SwingContainer(false)
@SuppressWarnings("serial") // Same-version serialization only
public abstract class JTextComponent extends JComponent implements Scrollable, Accessible
{
    /**
     * Creates a new <code>JTextComponent</code>.
     * Listeners for caret events are established, and the pluggable
     * UI installed.  The component is marked as editable.  No layout manager
     * is used, because layout is managed by the view subsystem of text.
     * The document model is set to <code>null</code>.
     */
    public JTextComponent() {
        super();
        // enable InputMethodEvent for on-the-spot pre-editing
        enableEvents(AWTEvent.KEY_EVENT_MASK | AWTEvent.INPUT_METHOD_EVENT_MASK);
        caretEvent = new MutableCaretEvent(this);
        addMouseListener(caretEvent);
        addFocusListener(caretEvent);
        setEditable(true);
        setDragEnabled(false);
        setLayout(null); // layout is managed by View hierarchy
        updateUI();
    }

    /**
     * Fetches the user-interface factory for this text-oriented editor.
     *
     * @return the factory
     */
    public TextUI getUI() { return (TextUI)ui; }

    /**
     * Sets the user-interface factory for this text-oriented editor.
     *
     * @param ui the factory
     */
    public void setUI(TextUI ui) {
        super.setUI(ui);
    }

    /**
     * Reloads the pluggable UI.  The key used to fetch the
     * new interface is <code>getUIClassID()</code>.  The type of
     * the UI is <code>TextUI</code>.  <code>invalidate</code>
     * is called after setting the UI.
     */
    public void updateUI() {
        setUI((TextUI)UIManager.getUI(this));
        invalidate();
    }

    /**
     * Adds a caret listener for notification of any changes
     * to the caret.
     *
     * @param listener the listener to be added
     * @see javax.swing.event.CaretEvent
     */
    public void addCaretListener(CaretListener listener) {
        listenerList.add(CaretListener.class, listener);
    }

    /**
     * Removes a caret listener.
     *
     * @param listener the listener to be removed
     * @see javax.swing.event.CaretEvent
     */
    public void removeCaretListener(CaretListener listener) {
        listenerList.remove(CaretListener.class, listener);
    }

    /**
     * Returns an array of all the caret listeners
     * registered on this text component.
     *
     * @return all of this component's <code>CaretListener</code>s
     *         or an empty
     *         array if no caret listeners are currently registered
     *
     * @see #addCaretListener
     * @see #removeCaretListener
     *
     * @since 1.4
     */
    @BeanProperty(bound = false)
    public CaretListener[] getCaretListeners() {
        return listenerList.getListeners(CaretListener.class);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.  The listener list is processed in a
     * last-to-first manner.
     *
     * @param e the event
     * @see EventListenerList
     */
    protected void fireCaretUpdate(CaretEvent e) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==CaretListener.class) {
                ((CaretListener)listeners[i+1]).caretUpdate(e);
            }
        }
    }

    /**
     * Associates the editor with a text document.
     * The currently registered factory is used to build a view for
     * the document, which gets displayed by the editor after revalidation.
     * A PropertyChange event ("document") is propagated to each listener.
     *
     * @param doc  the document to display/edit
     * @see #getDocument
     */
    @BeanProperty(expert = true, description
            = "the text document model")
    public void setDocument(Document doc) {
        Document old = model;

        /*
         * acquire a read lock on the old model to prevent notification of
         * mutations while we disconnecting the old model.
         */
        try {
            if (old instanceof AbstractDocument) {
                ((AbstractDocument)old).readLock();
            }
            if (accessibleContext != null) {
                model.removeDocumentListener(
                    ((AccessibleJTextComponent)accessibleContext));
            }
            if (inputMethodRequestsHandler != null) {
                model.removeDocumentListener((DocumentListener)inputMethodRequestsHandler);
            }
            model = doc;

            // Set the document's run direction property to match the
            // component's ComponentOrientation property.
            Boolean runDir = getComponentOrientation().isLeftToRight()
                             ? TextAttribute.RUN_DIRECTION_LTR
                             : TextAttribute.RUN_DIRECTION_RTL;
            if (runDir != doc.getProperty(TextAttribute.RUN_DIRECTION)) {
                doc.putProperty(TextAttribute.RUN_DIRECTION, runDir );
            }
            firePropertyChange("document", old, doc);
        } finally {
            if (old instanceof AbstractDocument) {
                ((AbstractDocument)old).readUnlock();
            }
        }

        revalidate();
        repaint();
        if (accessibleContext != null) {
            model.addDocumentListener(
                ((AccessibleJTextComponent)accessibleContext));
        }
        if (inputMethodRequestsHandler != null) {
            model.addDocumentListener((DocumentListener)inputMethodRequestsHandler);
        }
    }

    /**
     * Fetches the model associated with the editor.  This is
     * primarily for the UI to get at the minimal amount of
     * state required to be a text editor.  Subclasses will
     * return the actual type of the model which will typically
     * be something that extends Document.
     *
     * @return the model
     */
    public Document getDocument() {
        return model;
    }

    // Override of Component.setComponentOrientation
    public void setComponentOrientation( ComponentOrientation o ) {
        // Set the document's run direction property to match the
        // ComponentOrientation property.
        Document doc = getDocument();
        if( doc !=  null ) {
            Boolean runDir = o.isLeftToRight()
                             ? TextAttribute.RUN_DIRECTION_LTR
                             : TextAttribute.RUN_DIRECTION_RTL;
            doc.putProperty( TextAttribute.RUN_DIRECTION, runDir );
        }
        super.setComponentOrientation( o );
    }

    /**
     * Fetches the command list for the editor.  This is
     * the list of commands supported by the plugged-in UI
     * augmented by the collection of commands that the
     * editor itself supports.  These are useful for binding
     * to events, such as in a keymap.
     *
     * @return the command list
     */
    @BeanProperty(bound = false)
    public Action[] getActions() {
        return getUI().getEditorKit(this).getActions();
    }

    /**
     * Sets margin space between the text component's border
     * and its text.  The text component's default <code>Border</code>
     * object will use this value to create the proper margin.
     * However, if a non-default border is set on the text component,
     * it is that <code>Border</code> object's responsibility to create the
     * appropriate margin space (else this property will effectively
     * be ignored).  This causes a redraw of the component.
     * A PropertyChange event ("margin") is sent to all listeners.
     *
     * @param m the space between the border and the text
     */
    @BeanProperty(description
            = "desired space between the border and text area")
    public void setMargin(Insets m) {
        Insets old = margin;
        margin = m;
        firePropertyChange("margin", old, m);
        invalidate();
    }

    /**
     * Returns the margin between the text component's border and
     * its text.
     *
     * @return the margin
     */
    public Insets getMargin() {
        return margin;
    }

    /**
     * Sets the <code>NavigationFilter</code>. <code>NavigationFilter</code>
     * is used by <code>DefaultCaret</code> and the default cursor movement
     * actions as a way to restrict the cursor movement.
     * @param filter the filter
     *
     * @since 1.4
     */
    public void setNavigationFilter(NavigationFilter filter) {
        navigationFilter = filter;
    }

    /**
     * Returns the <code>NavigationFilter</code>. <code>NavigationFilter</code>
     * is used by <code>DefaultCaret</code> and the default cursor movement
     * actions as a way to restrict the cursor movement. A null return value
     * implies the cursor movement and selection should not be restricted.
     *
     * @since 1.4
     * @return the NavigationFilter
     */
    public NavigationFilter getNavigationFilter() {
        return navigationFilter;
    }

    /**
     * Fetches the caret that allows text-oriented navigation over
     * the view.
     *
     * @return the caret
     */
    @Transient
    public Caret getCaret() {
        return caret;
    }

    /**
     * Sets the caret to be used.  By default this will be set
     * by the UI that gets installed.  This can be changed to
     * a custom caret if desired.  Setting the caret results in a
     * PropertyChange event ("caret") being fired.
     *
     * @param c the caret
     * @see #getCaret
     */
    @BeanProperty(expert = true, description
            = "the caret used to select/navigate")
    public void setCaret(Caret c) {
        if (caret != null) {
            caret.removeChangeListener(caretEvent);
            caret.deinstall(this);
        }
        Caret old = caret;
        caret = c;
        if (caret != null) {
            caret.install(this);
            caret.addChangeListener(caretEvent);
        }
        firePropertyChange("caret", old, caret);
    }

    /**
     * Fetches the object responsible for making highlights.
     *
     * @return the highlighter
     */
    public Highlighter getHighlighter() {
        return highlighter;
    }

    /**
     * Sets the highlighter to be used.  By default this will be set
     * by the UI that gets installed.  This can be changed to
     * a custom highlighter if desired.  The highlighter can be set to
     * <code>null</code> to disable it.
     * A PropertyChange event ("highlighter") is fired
     * when a new highlighter is installed.
     *
     * @param h the highlighter
     * @see #getHighlighter
     */
    @BeanProperty(expert = true, description
            = "object responsible for background highlights")
    public void setHighlighter(Highlighter h) {
        if (highlighter != null) {
            highlighter.deinstall(this);
        }
        Highlighter old = highlighter;
        highlighter = h;
        if (highlighter != null) {
            highlighter.install(this);
        }
        firePropertyChange("highlighter", old, h);
    }

    /**
     * Sets the keymap to use for binding events to
     * actions.  Setting to <code>null</code> effectively disables
     * keyboard input.
     * A PropertyChange event ("keymap") is fired when a new keymap
     * is installed.
     *
     * @param map the keymap
     * @see #getKeymap
     */
    @BeanProperty(description
            = "set of key event to action bindings to use")
    public void setKeymap(Keymap map) {
        Keymap old = keymap;
        keymap = map;
        firePropertyChange("keymap", old, keymap);
        updateInputMap(old, map);
    }

    /**
     * Turns on or off automatic drag handling. In order to enable automatic
     * drag handling, this property should be set to {@code true}, and the
     * component's {@code TransferHandler} needs to be {@code non-null}.
     * The default value of the {@code dragEnabled} property is {@code false}.
     * <p>
     * The job of honoring this property, and recognizing a user drag gesture,
     * lies with the look and feel implementation, and in particular, the component's
     * {@code TextUI}. When automatic drag handling is enabled, most look and
     * feels (including those that subclass {@code BasicLookAndFeel}) begin a
     * drag and drop operation whenever the user presses the mouse button over
     * a selection and then moves the mouse a few pixels. Setting this property to
     * {@code true} can therefore have a subtle effect on how selections behave.
     * <p>
     * If a look and feel is used that ignores this property, you can still
     * begin a drag and drop operation by calling {@code exportAsDrag} on the
     * component's {@code TransferHandler}.
     *
     * @param b whether or not to enable automatic drag handling
     * @exception HeadlessException if
     *            <code>b</code> is <code>true</code> and
     *            <code>GraphicsEnvironment.isHeadless()</code>
     *            returns <code>true</code>
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see #getDragEnabled
     * @see #setTransferHandler
     * @see TransferHandler
     * @since 1.4
     */
    @BeanProperty(bound = false, description
            = "determines whether automatic drag handling is enabled")
    public void setDragEnabled(boolean b) {
        checkDragEnabled(b);
        dragEnabled = b;
    }

    private static void checkDragEnabled(boolean b) {
        if (b && GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
    }

    /**
     * Returns whether or not automatic drag handling is enabled.
     *
     * @return the value of the {@code dragEnabled} property
     * @see #setDragEnabled
     * @since 1.4
     */
    public boolean getDragEnabled() {
        return dragEnabled;
    }

    /**
     * Sets the drop mode for this component. For backward compatibility,
     * the default for this property is <code>DropMode.USE_SELECTION</code>.
     * Usage of <code>DropMode.INSERT</code> is recommended, however,
     * for an improved user experience. It offers similar behavior of dropping
     * between text locations, but does so without affecting the actual text
     * selection and caret location.
     * <p>
     * <code>JTextComponents</code> support the following drop modes:
     * <ul>
     *    <li><code>DropMode.USE_SELECTION</code></li>
     *    <li><code>DropMode.INSERT</code></li>
     * </ul>
     * <p>
     * The drop mode is only meaningful if this component has a
     * <code>TransferHandler</code> that accepts drops.
     *
     * @param dropMode the drop mode to use
     * @throws IllegalArgumentException if the drop mode is unsupported
     *         or <code>null</code>
     * @see #getDropMode
     * @see #getDropLocation
     * @see #setTransferHandler
     * @see javax.swing.TransferHandler
     * @since 1.6
     */
    public final void setDropMode(DropMode dropMode) {
        checkDropMode(dropMode);
        this.dropMode = dropMode;
    }

    private static void checkDropMode(DropMode dropMode) {
        if (dropMode != null) {
            switch (dropMode) {
                case USE_SELECTION:
                case INSERT:
                    return;
            }
        }

        throw new IllegalArgumentException(dropMode + ": Unsupported drop mode for text");
    }

    /**
     * Returns the drop mode for this component.
     *
     * @return the drop mode for this component
     * @see #setDropMode
     * @since 1.6
     */
    public final DropMode getDropMode() {
        return dropMode;
    }

    static {
        SwingAccessor.setJTextComponentAccessor(
            new SwingAccessor.JTextComponentAccessor() {
                public TransferHandler.DropLocation dropLocationForPoint(JTextComponent textComp,
                                                                         Point p)
                {
                    return textComp.dropLocationForPoint(p);
                }
                public Object setDropLocation(JTextComponent textComp,
                                              TransferHandler.DropLocation location,
                                              Object state, boolean forDrop)
                {
                    return textComp.setDropLocation(location, state, forDrop);
                }
            });
    }


    /**
     * Calculates a drop location in this component, representing where a
     * drop at the given point should insert data.
     * <p>
     * Note: This method is meant to override
     * <code>JComponent.dropLocationForPoint()</code>, which is package-private
     * in javax.swing. <code>TransferHandler</code> will detect text components
     * and call this method instead via reflection. It's name should therefore
     * not be changed.
     *
     * @param p the point to calculate a drop location for
     * @return the drop location, or <code>null</code>
     */
    @SuppressWarnings("deprecation")
    DropLocation dropLocationForPoint(Point p) {
        Position.Bias[] bias = new Position.Bias[1];
        int index = getUI().viewToModel(this, p, bias);

        // viewToModel currently returns null for some HTML content
        // when the point is within the component's top inset
        if (bias[0] == null) {
            bias[0] = Position.Bias.Forward;
        }

        return new DropLocation(p, index, bias[0]);
    }

    /**
     * Called to set or clear the drop location during a DnD operation.
     * In some cases, the component may need to use it's internal selection
     * temporarily to indicate the drop location. To help facilitate this,
     * this method returns and accepts as a parameter a state object.
     * This state object can be used to store, and later restore, the selection
     * state. Whatever this method returns will be passed back to it in
     * future calls, as the state parameter. If it wants the DnD system to
     * continue storing the same state, it must pass it back every time.
     * Here's how this is used:
     * <p>
     * Let's say that on the first call to this method the component decides
     * to save some state (because it is about to use the selection to show
     * a drop index). It can return a state object to the caller encapsulating
     * any saved selection state. On a second call, let's say the drop location
     * is being changed to something else. The component doesn't need to
     * restore anything yet, so it simply passes back the same state object
     * to have the DnD system continue storing it. Finally, let's say this
     * method is messaged with <code>null</code>. This means DnD
     * is finished with this component for now, meaning it should restore
     * state. At this point, it can use the state parameter to restore
     * said state, and of course return <code>null</code> since there's
     * no longer anything to store.
     * <p>
     * Note: This method is meant to override
     * <code>JComponent.setDropLocation()</code>, which is package-private
     * in javax.swing. <code>TransferHandler</code> will detect text components
     * and call this method instead via reflection. It's name should therefore
     * not be changed.
     *
     * @param location the drop location (as calculated by
     *        <code>dropLocationForPoint</code>) or <code>null</code>
     *        if there's no longer a valid drop location
     * @param state the state object saved earlier for this component,
     *        or <code>null</code>
     * @param forDrop whether or not the method is being called because an
     *        actual drop occurred
     * @return any saved state for this component, or <code>null</code> if none
     */
    Object setDropLocation(TransferHandler.DropLocation location,
                           Object state,
                           boolean forDrop) {

        Object retVal = null;
        DropLocation textLocation = (DropLocation)location;

        if (dropMode == DropMode.USE_SELECTION) {
            if (textLocation == null) {
                if (state != null) {
                    /*
                     * This object represents the state saved earlier.
                     *     If the caret is a DefaultCaret it will be
                     *     an Object array containing, in order:
                     *         - the saved caret mark (Integer)
                     *         - the saved caret dot (Integer)
                     *         - the saved caret visibility (Boolean)
                     *         - the saved mark bias (Position.Bias)
                     *         - the saved dot bias (Position.Bias)
                     *     If the caret is not a DefaultCaret it will
                     *     be similar, but will not contain the dot
                     *     or mark bias.
                     */
                    Object[] vals = (Object[])state;

                    if (!forDrop) {
                        if (caret instanceof DefaultCaret) {
                            ((DefaultCaret)caret).setDot(((Integer)vals[0]).intValue(),
                                                         (Position.Bias)vals[3]);
                            ((DefaultCaret)caret).moveDot(((Integer)vals[1]).intValue(),
                                                         (Position.Bias)vals[4]);
                        } else {
                            caret.setDot(((Integer)vals[0]).intValue());
                            caret.moveDot(((Integer)vals[1]).intValue());
                        }
                    }

                    caret.setVisible(((Boolean)vals[2]).booleanValue());
                }
            } else {
                if (dropLocation == null) {
                    boolean visible;

                    if (caret instanceof DefaultCaret) {
                        DefaultCaret dc = (DefaultCaret)caret;
                        visible = dc.isActive();
                        retVal = new Object[] {Integer.valueOf(dc.getMark()),
                                               Integer.valueOf(dc.getDot()),
                                               Boolean.valueOf(visible),
                                               dc.getMarkBias(),
                                               dc.getDotBias()};
                    } else {
                        visible = caret.isVisible();
                        retVal = new Object[] {Integer.valueOf(caret.getMark()),
                                               Integer.valueOf(caret.getDot()),
                                               Boolean.valueOf(visible)};
                    }

                    caret.setVisible(true);
                } else {
                    retVal = state;
                }

                if (caret instanceof DefaultCaret) {
                    ((DefaultCaret)caret).setDot(textLocation.getIndex(), textLocation.getBias());
                } else {
                    caret.setDot(textLocation.getIndex());
                }
            }
        } else {
            if (textLocation == null) {
                if (state != null) {
                    caret.setVisible(((Boolean)state).booleanValue());
                }
            } else {
                if (dropLocation == null) {
                    boolean visible = caret instanceof DefaultCaret
                                      ? ((DefaultCaret)caret).isActive()
                                      : caret.isVisible();
                    retVal = Boolean.valueOf(visible);
                    caret.setVisible(false);
                } else {
                    retVal = state;
                }
            }
        }

        DropLocation old = dropLocation;
        dropLocation = textLocation;
        firePropertyChange("dropLocation", old, dropLocation);

        return retVal;
    }

    /**
     * Returns the location that this component should visually indicate
     * as the drop location during a DnD operation over the component,
     * or {@code null} if no location is to currently be shown.
     * <p>
     * This method is not meant for querying the drop location
     * from a {@code TransferHandler}, as the drop location is only
     * set after the {@code TransferHandler}'s <code>canImport</code>
     * has returned and has allowed for the location to be shown.
     * <p>
     * When this property changes, a property change event with
     * name "dropLocation" is fired by the component.
     *
     * @return the drop location
     * @see #setDropMode
     * @see TransferHandler#canImport(TransferHandler.TransferSupport)
     * @since 1.6
     */
    @BeanProperty(bound = false)
    public final DropLocation getDropLocation() {
        return dropLocation;
    }


    /**
     * Updates the <code>InputMap</code>s in response to a
     * <code>Keymap</code> change.
     * @param oldKm  the old <code>Keymap</code>
     * @param newKm  the new <code>Keymap</code>
     */
    void updateInputMap(Keymap oldKm, Keymap newKm) {
        // Locate the current KeymapWrapper.
        InputMap km = getInputMap(JComponent.WHEN_FOCUSED);
        InputMap last = km;
        while (km != null && !(km instanceof KeymapWrapper)) {
            last = km;
            km = km.getParent();
        }
        if (km != null) {
            // Found it, tweak the InputMap that points to it, as well
            // as anything it points to.
            if (newKm == null) {
                if (last != km) {
                    last.setParent(km.getParent());
                }
                else {
                    last.setParent(null);
                }
            }
            else {
                InputMap newKM = new KeymapWrapper(newKm);
                last.setParent(newKM);
                if (last != km) {
                    newKM.setParent(km.getParent());
                }
            }
        }
        else if (newKm != null) {
            km = getInputMap(JComponent.WHEN_FOCUSED);
            if (km != null) {
                // Couldn't find it.
                // Set the parent of WHEN_FOCUSED InputMap to be the new one.
                InputMap newKM = new KeymapWrapper(newKm);
                newKM.setParent(km.getParent());
                km.setParent(newKM);
            }
        }

        // Do the same thing with the ActionMap
        ActionMap am = getActionMap();
        ActionMap lastAM = am;
        while (am != null && !(am instanceof KeymapActionMap)) {
            lastAM = am;
            am = am.getParent();
        }
        if (am != null) {
            // Found it, tweak the Actionap that points to it, as well
            // as anything it points to.
            if (newKm == null) {
                if (lastAM != am) {
                    lastAM.setParent(am.getParent());
                }
                else {
                    lastAM.setParent(null);
                }
            }
            else {
                ActionMap newAM = new KeymapActionMap(newKm);
                lastAM.setParent(newAM);
                if (lastAM != am) {
                    newAM.setParent(am.getParent());
                }
            }
        }
        else if (newKm != null) {
            am = getActionMap();
            if (am != null) {
                // Couldn't find it.
                // Set the parent of ActionMap to be the new one.
                ActionMap newAM = new KeymapActionMap(newKm);
                newAM.setParent(am.getParent());
                am.setParent(newAM);
            }
        }
    }

    /**
     * Fetches the keymap currently active in this text
     * component.
     *
     * @return the keymap
     */
    public Keymap getKeymap() {
        return keymap;
    }

    /**
     * Adds a new keymap into the keymap hierarchy.  Keymap bindings
     * resolve from bottom up so an attribute specified in a child
     * will override an attribute specified in the parent.
     *
     * @param nm   the name of the keymap (must be unique within the
     *   collection of named keymaps in the document); the name may
     *   be <code>null</code> if the keymap is unnamed,
     *   but the caller is responsible for managing the reference
     *   returned as an unnamed keymap can't
     *   be fetched by name
     * @param parent the parent keymap; this may be <code>null</code> if
     *   unspecified bindings need not be resolved in some other keymap
     * @return the keymap
     */
    public static Keymap addKeymap(String nm, Keymap parent) {
        Keymap map = new DefaultKeymap(nm, parent);
        if (nm != null) {
            // add a named keymap, a class of bindings
            getKeymapTable().put(nm, map);
        }
        return map;
    }

    /**
     * Removes a named keymap previously added to the document.  Keymaps
     * with <code>null</code> names may not be removed in this way.
     *
     * @param nm  the name of the keymap to remove
     * @return the keymap that was removed
     */
    public static Keymap removeKeymap(String nm) {
        return getKeymapTable().remove(nm);
    }

    /**
     * Fetches a named keymap previously added to the document.
     * This does not work with <code>null</code>-named keymaps.
     *
     * @param nm  the name of the keymap
     * @return the keymap
     */
    public static Keymap getKeymap(String nm) {
        return getKeymapTable().get(nm);
    }

    private static HashMap<String,Keymap> getKeymapTable() {
        synchronized (KEYMAP_TABLE) {
            AppContext appContext = AppContext.getAppContext();
            @SuppressWarnings("unchecked")
            HashMap<String,Keymap> keymapTable =
                (HashMap<String,Keymap>)appContext.get(KEYMAP_TABLE);
            if (keymapTable == null) {
                keymapTable = new HashMap<String,Keymap>(17);
                appContext.put(KEYMAP_TABLE, keymapTable);
                //initialize default keymap
                Keymap binding = addKeymap(DEFAULT_KEYMAP, null);
                binding.setDefaultAction(new
                                         DefaultEditorKit.DefaultKeyTypedAction());
            }
            return keymapTable;
        }
    }

    /**
     * Binding record for creating key bindings.
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
    @SuppressWarnings("serial") // Same-version serialization only
    public static class KeyBinding {

        /**
         * The key.
         */
        public KeyStroke key;

        /**
         * The name of the action for the key.
         */
        public String actionName;

        /**
         * Creates a new key binding.
         *
         * @param key the key
         * @param actionName the name of the action for the key
         */
        public KeyBinding(KeyStroke key, String actionName) {
            this.key = key;
            this.actionName = actionName;
        }
    }

    /**
     * <p>
     * Loads a keymap with a bunch of
     * bindings.  This can be used to take a static table of
     * definitions and load them into some keymap.  The following
     * example illustrates an example of binding some keys to
     * the cut, copy, and paste actions associated with a
     * JTextComponent.  A code fragment to accomplish
     * this might look as follows:
     * <pre><code>
     *
     *   static final JTextComponent.KeyBinding[] defaultBindings = {
     *     new JTextComponent.KeyBinding(
     *       KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK),
     *       DefaultEditorKit.copyAction),
     *     new JTextComponent.KeyBinding(
     *       KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK),
     *       DefaultEditorKit.pasteAction),
     *     new JTextComponent.KeyBinding(
     *       KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK),
     *       DefaultEditorKit.cutAction),
     *   };
     *
     *   JTextComponent c = new JTextPane();
     *   Keymap k = c.getKeymap();
     *   JTextComponent.loadKeymap(k, defaultBindings, c.getActions());
     *
     * </code></pre>
     * The sets of bindings and actions may be empty but must be
     * non-<code>null</code>.
     *
     * @param map the keymap
     * @param bindings the bindings
     * @param actions the set of actions
     */
    public static void loadKeymap(Keymap map, KeyBinding[] bindings, Action[] actions) {
        Hashtable<String, Action> h = new Hashtable<String, Action>();
        for (Action a : actions) {
            String value = (String)a.getValue(Action.NAME);
            h.put((value!=null ? value:""), a);
        }
        for (KeyBinding binding : bindings) {
            Action a = h.get(binding.actionName);
            if (a != null) {
                map.addActionForKeyStroke(binding.key, a);
            }
        }
    }

    /**
     * Fetches the current color used to render the
     * caret.
     *
     * @return the color
     */
    public Color getCaretColor() {
        return caretColor;
    }

    /**
     * Sets the current color used to render the caret.
     * Setting to <code>null</code> effectively restores the default color.
     * Setting the color results in a PropertyChange event ("caretColor")
     * being fired.
     *
     * @param c the color
     * @see #getCaretColor
     */
    @BeanProperty(preferred = true, description
            = "the color used to render the caret")
    public void setCaretColor(Color c) {
        Color old = caretColor;
        caretColor = c;
        firePropertyChange("caretColor", old, caretColor);
    }

    /**
     * Fetches the current color used to render the
     * selection.
     *
     * @return the color
     */
    public Color getSelectionColor() {
        return selectionColor;
    }

    /**
     * Sets the current color used to render the selection.
     * Setting the color to <code>null</code> is the same as setting
     * <code>Color.white</code>.  Setting the color results in a
     * PropertyChange event ("selectionColor").
     *
     * @param c the color
     * @see #getSelectionColor
     */
    @BeanProperty(preferred = true, description
            = "color used to render selection background")
    public void setSelectionColor(Color c) {
        Color old = selectionColor;
        selectionColor = c;
        firePropertyChange("selectionColor", old, selectionColor);
    }

    /**
     * Fetches the current color used to render the
     * selected text.
     *
     * @return the color
     */
    public Color getSelectedTextColor() {
        return selectedTextColor;
    }

    /**
     * Sets the current color used to render the selected text.
     * Setting the color to <code>null</code> is the same as
     * <code>Color.black</code>. Setting the color results in a
     * PropertyChange event ("selectedTextColor") being fired.
     *
     * @param c the color
     * @see #getSelectedTextColor
     */
    @BeanProperty(preferred = true, description
            = "color used to render selected text")
    public void setSelectedTextColor(Color c) {
        Color old = selectedTextColor;
        selectedTextColor = c;
        firePropertyChange("selectedTextColor", old, selectedTextColor);
    }

    /**
     * Fetches the current color used to render the
     * disabled text.
     *
     * @return the color
     */
    public Color getDisabledTextColor() {
        return disabledTextColor;
    }

    /**
     * Sets the current color used to render the
     * disabled text.  Setting the color fires off a
     * PropertyChange event ("disabledTextColor").
     *
     * @param c the color
     * @see #getDisabledTextColor
     */
    @BeanProperty(preferred = true, description
            = "color used to render disabled text")
    public void setDisabledTextColor(Color c) {
        Color old = disabledTextColor;
        disabledTextColor = c;
        firePropertyChange("disabledTextColor", old, disabledTextColor);
    }

    /**
     * Replaces the currently selected content with new content
     * represented by the given string.  If there is no selection
     * this amounts to an insert of the given text.  If there
     * is no replacement text this amounts to a removal of the
     * current selection.
     * <p>
     * This is the method that is used by the default implementation
     * of the action for inserting content that gets bound to the
     * keymap actions.
     *
     * @param content  the content to replace the selection with
     */
    public void replaceSelection(String content) {
        Document doc = getDocument();
        if (doc != null) {
            try {
                boolean composedTextSaved = saveComposedText(caret.getDot());
                int p0 = Math.min(caret.getDot(), caret.getMark());
                int p1 = Math.max(caret.getDot(), caret.getMark());
                if (doc instanceof AbstractDocument) {
                    ((AbstractDocument)doc).replace(p0, p1 - p0, content,null);
                }
                else {
                    if (p0 != p1) {
                        doc.remove(p0, p1 - p0);
                    }
                    if (content != null && content.length() > 0) {
                        doc.insertString(p0, content, null);
                    }
                }
                if (composedTextSaved) {
                    restoreComposedText();
                }
            } catch (BadLocationException e) {
                UIManager.getLookAndFeel().provideErrorFeedback(JTextComponent.this);
            }
        }
    }

    /**
     * Fetches a portion of the text represented by the
     * component.  Returns an empty string if length is 0.
     *
     * @param offs the offset &ge; 0
     * @param len the length &ge; 0
     * @return the text
     * @exception BadLocationException if the offset or length are invalid
     */
    public String getText(int offs, int len) throws BadLocationException {
        return getDocument().getText(offs, len);
    }

    /**
     * Converts the given location in the model to a place in
     * the view coordinate system.
     * The component must have a positive size for
     * this translation to be computed (i.e. layout cannot
     * be computed until the component has been sized).  The
     * component does not have to be visible or painted.
     *
     * @param pos the position &ge; 0
     * @return the coordinates as a rectangle, with (r.x, r.y) as the location
     *   in the coordinate system, or null if the component does
     *   not yet have a positive size.
     * @exception BadLocationException if the given position does not
     *   represent a valid location in the associated document
     * @see TextUI#modelToView
     *
     * @deprecated replaced by
     *     {@link #modelToView2D(int)}
     */
    @Deprecated(since = "9")
    public Rectangle modelToView(int pos) throws BadLocationException {
        return getUI().modelToView(this, pos);
    }

    /**
     * Converts the given location in the model to a place in
     * the view coordinate system.
     * The component must have a positive size for
     * this translation to be computed (i.e. layout cannot
     * be computed until the component has been sized).  The
     * component does not have to be visible or painted.
     *
     * @param pos the position {@code >= 0}
     * @return the coordinates as a rectangle, with (r.x, r.y) as the location
     *   in the coordinate system, or null if the component does
     *   not yet have a positive size.
     * @exception BadLocationException if the given position does not
     *   represent a valid location in the associated document
     * @see TextUI#modelToView2D
     *
     * @since 9
     */
    public Rectangle2D modelToView2D(int pos) throws BadLocationException {
        return getUI().modelToView2D(this, pos, Position.Bias.Forward);
    }

    /**
     * Converts the given place in the view coordinate system
     * to the nearest representative location in the model.
     * The component must have a positive size for
     * this translation to be computed (i.e. layout cannot
     * be computed until the component has been sized).  The
     * component does not have to be visible or painted.
     *
     * @param pt the location in the view to translate
     * @return the offset &ge; 0 from the start of the document,
     *   or -1 if the component does not yet have a positive
     *   size.
     * @see TextUI#viewToModel
     *
     * @deprecated replaced by
     *     {@link #viewToModel2D(Point2D)}
     */
    @Deprecated(since = "9")
    public int viewToModel(Point pt) {
        return getUI().viewToModel(this, pt);
    }

    /**
     * Converts the given place in the view coordinate system
     * to the nearest representative location in the model.
     * The component must have a positive size for
     * this translation to be computed (i.e. layout cannot
     * be computed until the component has been sized).  The
     * component does not have to be visible or painted.
     *
     * @param pt the location in the view to translate
     * @return the offset {@code >= 0} from the start of the document,
     *   or {@code -1} if the component does not yet have a positive
     *   size.
     * @see TextUI#viewToModel2D
     *
     * @since 9
     */
    public int viewToModel2D(Point2D pt) {
        return getUI().viewToModel2D(this, pt, new Position.Bias[1]);
    }

    /**
     * Transfers the currently selected range in the associated
     * text model to the system clipboard, removing the contents
     * from the model.  The current selection is reset.  Does nothing
     * for <code>null</code> selections.
     *
     * @see java.awt.Toolkit#getSystemClipboard
     * @see java.awt.datatransfer.Clipboard
     */
    public void cut() {
        if (isEditable() && isEnabled()) {
            invokeAction("cut", TransferHandler.getCutAction());
        }
    }

    /**
     * Transfers the currently selected range in the associated
     * text model to the system clipboard, leaving the contents
     * in the text model.  The current selection remains intact.
     * Does nothing for <code>null</code> selections.
     *
     * @see java.awt.Toolkit#getSystemClipboard
     * @see java.awt.datatransfer.Clipboard
     */
    public void copy() {
        invokeAction("copy", TransferHandler.getCopyAction());
    }

    /**
     * Transfers the contents of the system clipboard into the
     * associated text model.  If there is a selection in the
     * associated view, it is replaced with the contents of the
     * clipboard.  If there is no selection, the clipboard contents
     * are inserted in front of the current insert position in
     * the associated view.  If the clipboard is empty, does nothing.
     *
     * @see #replaceSelection
     * @see java.awt.Toolkit#getSystemClipboard
     * @see java.awt.datatransfer.Clipboard
     */
    public void paste() {
        if (isEditable() && isEnabled()) {
            invokeAction("paste", TransferHandler.getPasteAction());
        }
    }

    /**
     * This is a convenience method that is only useful for
     * <code>cut</code>, <code>copy</code> and <code>paste</code>.  If
     * an <code>Action</code> with the name <code>name</code> does not
     * exist in the <code>ActionMap</code>, this will attempt to install a
     * <code>TransferHandler</code> and then use <code>altAction</code>.
     */
    private void invokeAction(String name, Action altAction) {
        ActionMap map = getActionMap();
        Action action = null;

        if (map != null) {
            action = map.get(name);
        }
        if (action == null) {
            installDefaultTransferHandlerIfNecessary();
            action = altAction;
        }
        action.actionPerformed(new ActionEvent(this,
                               ActionEvent.ACTION_PERFORMED, (String)action.
                               getValue(Action.NAME),
                               EventQueue.getMostRecentEventTime(),
                               getCurrentEventModifiers()));
    }

    /**
     * If the current <code>TransferHandler</code> is null, this will
     * install a new one.
     */
    private void installDefaultTransferHandlerIfNecessary() {
        if (getTransferHandler() == null) {
            if (defaultTransferHandler == null) {
                defaultTransferHandler = new DefaultTransferHandler();
            }
            setTransferHandler(defaultTransferHandler);
        }
    }

    /**
     * Moves the caret to a new position, leaving behind a mark
     * defined by the last time <code>setCaretPosition</code> was
     * called.  This forms a selection.
     * If the document is <code>null</code>, does nothing. The position
     * must be between 0 and the length of the component's text or else
     * an exception is thrown.
     *
     * @param pos the position
     * @exception    IllegalArgumentException if the value supplied
     *               for <code>position</code> is less than zero or greater
     *               than the component's text length
     * @see #setCaretPosition
     */
    public void moveCaretPosition(int pos) {
        Document doc = getDocument();
        if (doc != null) {
            if (pos > doc.getLength() || pos < 0) {
                throw new IllegalArgumentException("bad position: " + pos);
            }
            caret.moveDot(pos);
        }
    }

    /**
     * The bound property name for the focus accelerator.
     */
    public static final String FOCUS_ACCELERATOR_KEY = "focusAcceleratorKey";

    /**
     * Sets the key accelerator that will cause the receiving text
     * component to get the focus.  The accelerator will be the
     * key combination of the platform-specific modifier key and
     * the character given (converted to upper case).  For example,
     * the ALT key is used as a modifier on Windows and the CTRL+ALT
     * combination is used on Mac.  By default, there is no focus
     * accelerator key.  Any previous key accelerator setting will be
     * superseded.  A '\0' key setting will be registered, and has the
     * effect of turning off the focus accelerator.  When the new key
     * is set, a PropertyChange event (FOCUS_ACCELERATOR_KEY) will be fired.
     *
     * @param aKey the key
     * @see #getFocusAccelerator
     */
    @BeanProperty(description
            = "accelerator character used to grab focus")
    public void setFocusAccelerator(char aKey) {
        aKey = Character.toUpperCase(aKey);
        char old = focusAccelerator;
        focusAccelerator = aKey;
        // Fix for 4341002: value of FOCUS_ACCELERATOR_KEY is wrong.
        // So we fire both FOCUS_ACCELERATOR_KEY, for compatibility,
        // and the correct event here.
        firePropertyChange(FOCUS_ACCELERATOR_KEY, old, focusAccelerator);
        firePropertyChange("focusAccelerator", old, focusAccelerator);
    }

    /**
     * Returns the key accelerator that will cause the receiving
     * text component to get the focus.  Return '\0' if no focus
     * accelerator has been set.
     *
     * @return the key
     */
    public char getFocusAccelerator() {
        return focusAccelerator;
    }

    /**
     * Initializes from a stream.  This creates a
     * model of the type appropriate for the component
     * and initializes the model from the stream.
     * By default this will load the model as plain
     * text.  Previous contents of the model are discarded.
     *
     * @param in the stream to read from
     * @param desc an object describing the stream; this
     *   might be a string, a File, a URL, etc.  Some kinds
     *   of documents (such as html for example) might be
     *   able to make use of this information; if non-<code>null</code>,
     *   it is added as a property of the document
     * @exception IOException as thrown by the stream being
     *  used to initialize
     * @see EditorKit#createDefaultDocument
     * @see #setDocument
     * @see PlainDocument
     */
    public void read(Reader in, Object desc) throws IOException {
        EditorKit kit = getUI().getEditorKit(this);
        Document doc = kit.createDefaultDocument();
        if (desc != null) {
            doc.putProperty(Document.StreamDescriptionProperty, desc);
        }
        try {
            kit.read(in, doc, 0);
            setDocument(doc);
        } catch (BadLocationException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Stores the contents of the model into the given
     * stream.  By default this will store the model as plain
     * text.
     *
     * @param out the output stream
     * @exception IOException on any I/O error
     */
    public void write(Writer out) throws IOException {
        Document doc = getDocument();
        try {
            getUI().getEditorKit(this).write(out, doc, 0, doc.getLength());
        } catch (BadLocationException e) {
            throw new IOException(e.getMessage());
        }
    }

    public void removeNotify() {
        super.removeNotify();
        if (getFocusedComponent() == this) {
            AppContext.getAppContext().remove(FOCUSED_COMPONENT);
        }
    }

    // --- java.awt.TextComponent methods ------------------------

    /**
     * Sets the position of the text insertion caret for the
     * <code>TextComponent</code>.  Note that the caret tracks change,
     * so this may move if the underlying text of the component is changed.
     * If the document is <code>null</code>, does nothing. The position
     * must be between 0 and the length of the component's text or else
     * an exception is thrown.
     *
     * @param position the position
     * @exception    IllegalArgumentException if the value supplied
     *               for <code>position</code> is less than zero or greater
     *               than the component's text length
     */
    @BeanProperty(bound = false, description
            = "the caret position")
    public void setCaretPosition(int position) {
        Document doc = getDocument();
        if (doc != null) {
            if (position > doc.getLength() || position < 0) {
                throw new IllegalArgumentException("bad position: " + position);
            }
            caret.setDot(position);
        }
    }

    /**
     * Returns the position of the text insertion caret for the
     * text component.
     *
     * @return the position of the text insertion caret for the
     *  text component &ge; 0
     */
    @Transient
    public int getCaretPosition() {
        return caret.getDot();
    }

    /**
     * Sets the text of this <code>TextComponent</code>
     * to the specified text.  If the text is <code>null</code>
     * or empty, has the effect of simply deleting the old text.
     * When text has been inserted, the resulting caret location
     * is determined by the implementation of the caret class.
     *
     * <p>
     * Note that text is not a bound property, so no <code>PropertyChangeEvent
     * </code> is fired when it changes. To listen for changes to the text,
     * use <code>DocumentListener</code>.
     *
     * @param t the new text to be set
     * @see #getText
     * @see DefaultCaret
     */
    @BeanProperty(bound = false, description
            = "the text of this component")
    public void setText(String t) {
        try {
            Document doc = getDocument();
            if (doc instanceof AbstractDocument) {
                ((AbstractDocument)doc).replace(0, doc.getLength(), t,null);
            }
            else {
                doc.remove(0, doc.getLength());
                doc.insertString(0, t, null);
            }
        } catch (BadLocationException e) {
            UIManager.getLookAndFeel().provideErrorFeedback(JTextComponent.this);
        }
    }

    /**
     * Returns the text contained in this <code>TextComponent</code>.
     * If the underlying document is <code>null</code>,
     * will give a <code>NullPointerException</code>.
     *
     * Note that text is not a bound property, so no <code>PropertyChangeEvent
     * </code> is fired when it changes. To listen for changes to the text,
     * use <code>DocumentListener</code>.
     *
     * @return the text
     * @exception NullPointerException if the document is <code>null</code>
     * @see #setText
     */
    public String getText() {
        Document doc = getDocument();
        String txt;
        try {
            txt = doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            txt = null;
        }
        return txt;
    }

    /**
     * Returns the selected text contained in this
     * <code>TextComponent</code>.  If the selection is
     * <code>null</code> or the document empty, returns <code>null</code>.
     *
     * @return the text
     * @exception IllegalArgumentException if the selection doesn't
     *  have a valid mapping into the document for some reason
     * @see #setText
     */
    @BeanProperty(bound = false)
    public String getSelectedText() {
        String txt = null;
        int p0 = Math.min(caret.getDot(), caret.getMark());
        int p1 = Math.max(caret.getDot(), caret.getMark());
        if (p0 != p1) {
            try {
                Document doc = getDocument();
                txt = doc.getText(p0, p1 - p0);
            } catch (BadLocationException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        return txt;
    }

    /**
     * Returns the boolean indicating whether this
     * <code>TextComponent</code> is editable or not.
     *
     * @return the boolean value
     * @see #setEditable
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Sets the specified boolean to indicate whether or not this
     * <code>TextComponent</code> should be editable.
     * A PropertyChange event ("editable") is fired when the
     * state is changed.
     *
     * @param b the boolean to be set
     * @see #isEditable
     */
    @BeanProperty(description
            = "specifies if the text can be edited")
    public void setEditable(boolean b) {
        if (b != editable) {
            boolean oldVal = editable;
            editable = b;
            enableInputMethods(editable);
            firePropertyChange("editable", Boolean.valueOf(oldVal), Boolean.valueOf(editable));
            repaint();
        }
    }

    /**
     * Returns the selected text's start position.  Return 0 for an
     * empty document, or the value of dot if no selection.
     *
     * @return the start position &ge; 0
     */
    @Transient
    public int getSelectionStart() {
        int start = Math.min(caret.getDot(), caret.getMark());
        return start;
    }

    /**
     * Sets the selection start to the specified position.  The new
     * starting point is constrained to be before or at the current
     * selection end.
     * <p>
     * This is available for backward compatibility to code
     * that called this method on <code>java.awt.TextComponent</code>.
     * This is implemented to forward to the <code>Caret</code>
     * implementation which is where the actual selection is maintained.
     *
     * @param selectionStart the start position of the text &ge; 0
     */
    @BeanProperty(bound = false, description
            = "starting location of the selection.")
    public void setSelectionStart(int selectionStart) {
        /* Route through select method to enforce consistent policy
         * between selectionStart and selectionEnd.
         */
        select(selectionStart, getSelectionEnd());
    }

    /**
     * Returns the selected text's end position.  Return 0 if the document
     * is empty, or the value of dot if there is no selection.
     *
     * @return the end position &ge; 0
     */
    @Transient
    public int getSelectionEnd() {
        int end = Math.max(caret.getDot(), caret.getMark());
        return end;
    }

    /**
     * Sets the selection end to the specified position.  The new
     * end point is constrained to be at or after the current
     * selection start.
     * <p>
     * This is available for backward compatibility to code
     * that called this method on <code>java.awt.TextComponent</code>.
     * This is implemented to forward to the <code>Caret</code>
     * implementation which is where the actual selection is maintained.
     *
     * @param selectionEnd the end position of the text &ge; 0
     */
    @BeanProperty(bound = false, description
            = "ending location of the selection.")
    public void setSelectionEnd(int selectionEnd) {
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
     * equal to the length of the text component's text.
     * <p>
     * If the caller supplies values that are inconsistent or out of
     * bounds, the method enforces these constraints silently, and
     * without failure. Specifically, if the start position or end
     * position is greater than the length of the text, it is reset to
     * equal the text length. If the start position is less than zero,
     * it is reset to zero, and if the end position is less than the
     * start position, it is reset to the start position.
     * <p>
     * This call is provided for backward compatibility.
     * It is routed to a call to <code>setCaretPosition</code>
     * followed by a call to <code>moveCaretPosition</code>.
     * The preferred way to manage selection is by calling
     * those methods directly.
     *
     * @param selectionStart the start position of the text
     * @param selectionEnd the end position of the text
     * @see #setCaretPosition
     * @see #moveCaretPosition
     */
    public void select(int selectionStart, int selectionEnd) {
        // argument adjustment done by java.awt.TextComponent
        int docLength = getDocument().getLength();

        if (selectionStart < 0) {
            selectionStart = 0;
        }
        if (selectionStart > docLength) {
            selectionStart = docLength;
        }
        if (selectionEnd > docLength) {
            selectionEnd = docLength;
        }
        if (selectionEnd < selectionStart) {
            selectionEnd = selectionStart;
        }

        setCaretPosition(selectionStart);
        moveCaretPosition(selectionEnd);
    }

    /**
     * Selects all the text in the <code>TextComponent</code>.
     * Does nothing on a <code>null</code> or empty document.
     */
    public void selectAll() {
        Document doc = getDocument();
        if (doc != null) {
            setCaretPosition(0);
            moveCaretPosition(doc.getLength());
        }
    }

    // --- Tooltip Methods ---------------------------------------------

    /**
     * Returns the string to be used as the tooltip for <code>event</code>.
     * This will return one of:
     * <ol>
     *  <li>If <code>setToolTipText</code> has been invoked with a
     *      non-<code>null</code>
     *      value, it will be returned, otherwise
     *  <li>The value from invoking <code>getToolTipText</code> on
     *      the UI will be returned.
     * </ol>
     * By default <code>JTextComponent</code> does not register
     * itself with the <code>ToolTipManager</code>.
     * This means that tooltips will NOT be shown from the
     * <code>TextUI</code> unless <code>registerComponent</code> has
     * been invoked on the <code>ToolTipManager</code>.
     *
     * @param event the event in question
     * @return the string to be used as the tooltip for <code>event</code>
     * @see javax.swing.JComponent#setToolTipText
     * @see javax.swing.plaf.TextUI#getToolTipText
     * @see javax.swing.ToolTipManager#registerComponent
     */
    @SuppressWarnings("deprecation")
    public String getToolTipText(MouseEvent event) {
        String retValue = super.getToolTipText(event);

        if (retValue == null) {
            TextUI ui = getUI();
            if (ui != null) {
                retValue = ui.getToolTipText(this, new Point(event.getX(),
                                                             event.getY()));
            }
        }
        return retValue;
    }

    // --- Scrollable methods ---------------------------------------------

    /**
     * Returns the preferred size of the viewport for a view component.
     * This is implemented to do the default behavior of returning
     * the preferred size of the component.
     *
     * @return the <code>preferredSize</code> of a <code>JViewport</code>
     * whose view is this <code>Scrollable</code>
     */
    @BeanProperty(bound = false)
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }


    /**
     * Components that display logical rows or columns should compute
     * the scroll increment that will completely expose one new row
     * or column, depending on the value of orientation.  Ideally,
     * components should handle a partially exposed row or column by
     * returning the distance required to completely expose the item.
     * <p>
     * The default implementation of this is to simply return 10% of
     * the visible area.  Subclasses are likely to be able to provide
     * a much more reasonable value.
     *
     * @param visibleRect the view area visible within the viewport
     * @param orientation either <code>SwingConstants.VERTICAL</code> or
     *   <code>SwingConstants.HORIZONTAL</code>
     * @param direction less than zero to scroll up/left, greater than
     *   zero for down/right
     * @return the "unit" increment for scrolling in the specified direction
     * @exception IllegalArgumentException for an invalid orientation
     * @see JScrollBar#setUnitIncrement
     */
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        switch(orientation) {
        case SwingConstants.VERTICAL:
            return visibleRect.height / 10;
        case SwingConstants.HORIZONTAL:
            return visibleRect.width / 10;
        default:
            throw new IllegalArgumentException("Invalid orientation: " + orientation);
        }
    }


    /**
     * Components that display logical rows or columns should compute
     * the scroll increment that will completely expose one block
     * of rows or columns, depending on the value of orientation.
     * <p>
     * The default implementation of this is to simply return the visible
     * area.  Subclasses will likely be able to provide a much more
     * reasonable value.
     *
     * @param visibleRect the view area visible within the viewport
     * @param orientation either <code>SwingConstants.VERTICAL</code> or
     *   <code>SwingConstants.HORIZONTAL</code>
     * @param direction less than zero to scroll up/left, greater than zero
     *  for down/right
     * @return the "block" increment for scrolling in the specified direction
     * @exception IllegalArgumentException for an invalid orientation
     * @see JScrollBar#setBlockIncrement
     */
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        switch(orientation) {
        case SwingConstants.VERTICAL:
            return visibleRect.height;
        case SwingConstants.HORIZONTAL:
            return visibleRect.width;
        default:
            throw new IllegalArgumentException("Invalid orientation: " + orientation);
        }
    }


    /**
     * Returns true if a viewport should always force the width of this
     * <code>Scrollable</code> to match the width of the viewport.
     * For example a normal text view that supported line wrapping
     * would return true here, since it would be undesirable for
     * wrapped lines to disappear beyond the right
     * edge of the viewport.  Note that returning true for a
     * <code>Scrollable</code> whose ancestor is a <code>JScrollPane</code>
     * effectively disables horizontal scrolling.
     * <p>
     * Scrolling containers, like <code>JViewport</code>,
     * will use this method each time they are validated.
     *
     * @return true if a viewport should force the <code>Scrollable</code>s
     *   width to match its own
     */
    @BeanProperty(bound = false)
    public boolean getScrollableTracksViewportWidth() {
        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (parent instanceof JViewport) {
            return parent.getWidth() > getPreferredSize().width;
        }
        return false;
    }

    /**
     * Returns true if a viewport should always force the height of this
     * <code>Scrollable</code> to match the height of the viewport.
     * For example a columnar text view that flowed text in left to
     * right columns could effectively disable vertical scrolling by
     * returning true here.
     * <p>
     * Scrolling containers, like <code>JViewport</code>,
     * will use this method each time they are validated.
     *
     * @return true if a viewport should force the Scrollables height
     *   to match its own
     */
    @BeanProperty(bound = false)
    public boolean getScrollableTracksViewportHeight() {
        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (parent instanceof JViewport) {
            return parent.getHeight() > getPreferredSize().height;
        }
        return false;
    }


//////////////////
// Printing Support
//////////////////

    /**
     * A convenience print method that displays a print dialog, and then
     * prints this {@code JTextComponent} in <i>interactive</i> mode with no
     * header or footer text. Note: this method
     * blocks until printing is done.
     * <p>
     * Note: In <i>headless</i> mode, no dialogs will be shown.
     *
     * <p> This method calls the full featured
     * {@link #print(MessageFormat, MessageFormat, boolean, PrintService, PrintRequestAttributeSet, boolean)
     * print} method to perform printing.
     * @return {@code true}, unless printing is canceled by the user
     * @throws PrinterException if an error in the print system causes the job
     *         to be aborted
     * @throws SecurityException if this thread is not allowed to
     *                           initiate a print job request
     *
     * @see #print(MessageFormat, MessageFormat, boolean, PrintService, PrintRequestAttributeSet, boolean)
     *
     * @since 1.6
     */

    public boolean print() throws PrinterException {
        return print(null, null, true, null, null, true);
    }

    /**
     * A convenience print method that displays a print dialog, and then
     * prints this {@code JTextComponent} in <i>interactive</i> mode with
     * the specified header and footer text. Note: this method
     * blocks until printing is done.
     * <p>
     * Note: In <i>headless</i> mode, no dialogs will be shown.
     *
     * <p> This method calls the full featured
     * {@link #print(MessageFormat, MessageFormat, boolean, PrintService, PrintRequestAttributeSet, boolean)
     * print} method to perform printing.
     * @param headerFormat the text, in {@code MessageFormat}, to be
     *        used as the header, or {@code null} for no header
     * @param footerFormat the text, in {@code MessageFormat}, to be
     *        used as the footer, or {@code null} for no footer
     * @return {@code true}, unless printing is canceled by the user
     * @throws PrinterException if an error in the print system causes the job
     *         to be aborted
     * @throws SecurityException if this thread is not allowed to
     *                           initiate a print job request
     *
     * @see #print(MessageFormat, MessageFormat, boolean, PrintService, PrintRequestAttributeSet, boolean)
     * @see java.text.MessageFormat
     * @since 1.6
     */
    public boolean print(final MessageFormat headerFormat,
            final MessageFormat footerFormat) throws PrinterException {
        return print(headerFormat, footerFormat, true, null, null, true);
    }

    /**
     * Prints the content of this {@code JTextComponent}. Note: this method
     * blocks until printing is done.
     *
     * <p>
     * Page header and footer text can be added to the output by providing
     * {@code MessageFormat} arguments. The printing code requests
     * {@code Strings} from the formats, providing a single item which may be
     * included in the formatted string: an {@code Integer} representing the
     * current page number.
     *
     * <p>
     * {@code showPrintDialog boolean} parameter allows you to specify whether
     * a print dialog is displayed to the user. When it is, the user
     * may use the dialog to change printing attributes or even cancel the
     * print.
     *
     * <p>
     * {@code service} allows you to provide the initial
     * {@code PrintService} for the print dialog, or to specify
     * {@code PrintService} to print to when the dialog is not shown.
     *
     * <p>
     * {@code attributes} can be used to provide the
     * initial values for the print dialog, or to supply any needed
     * attributes when the dialog is not shown. {@code attributes} can
     * be used to control how the job will print, for example
     * <i>duplex</i> or <i>single-sided</i>.
     *
     * <p>
     * {@code interactive boolean} parameter allows you to specify
     * whether to perform printing in <i>interactive</i>
     * mode. If {@code true}, a progress dialog, with an abort option,
     * is displayed for the duration of printing.  This dialog is
     * <i>modal</i> when {@code print} is invoked on the <i>Event Dispatch
     * Thread</i> and <i>non-modal</i> otherwise. <b>Warning</b>:
     * calling this method on the <i>Event Dispatch Thread</i> with {@code
     * interactive false} blocks <i>all</i> events, including repaints, from
     * being processed until printing is complete. It is only
     * recommended when printing from an application with no
     * visible GUI.
     *
     * <p>
     * Note: In <i>headless</i> mode, {@code showPrintDialog} and
     * {@code interactive} parameters are ignored and no dialogs are
     * shown.
     *
     * <p>
     * This method ensures the {@code document} is not mutated during printing.
     * To indicate it visually, {@code setEnabled(false)} is set for the
     * duration of printing.
     *
     * <p>
     * This method uses {@link #getPrintable} to render document content.
     *
     * <p>
     * This method is thread-safe, although most Swing methods are not. Please
     * see <A
     * HREF="https://docs.oracle.com/javase/tutorial/uiswing/concurrency/index.html">
     * Concurrency in Swing</A> for more information.
     *
     * <p>
     * <b>Sample Usage</b>. This code snippet shows a cross-platform print
     * dialog and then prints the {@code JTextComponent} in <i>interactive</i> mode
     * unless the user cancels the dialog:
     *
     * <pre>
     * textComponent.print(new MessageFormat(&quot;My text component header&quot;),
     *     new MessageFormat(&quot;Footer. Page - {0}&quot;), true, null, null, true);
     * </pre>
     * <p>
     * Executing this code off the <i>Event Dispatch Thread</i>
     * performs printing on the <i>background</i>.
     * The following pattern might be used for <i>background</i>
     * printing:
     * <pre>
     *     FutureTask&lt;Boolean&gt; future =
     *         new FutureTask&lt;Boolean&gt;(
     *             new Callable&lt;Boolean&gt;() {
     *                 public Boolean call() {
     *                     return textComponent.print(.....);
     *                 }
     *             });
     *     executor.execute(future);
     * </pre>
     *
     * @param headerFormat the text, in {@code MessageFormat}, to be
     *        used as the header, or {@code null} for no header
     * @param footerFormat the text, in {@code MessageFormat}, to be
     *        used as the footer, or {@code null} for no footer
     * @param showPrintDialog {@code true} to display a print dialog,
     *        {@code false} otherwise
     * @param service initial {@code PrintService}, or {@code null} for the
     *        default
     * @param attributes the job attributes to be applied to the print job, or
     *        {@code null} for none
     * @param interactive whether to print in an interactive mode
     * @return {@code true}, unless printing is canceled by the user
     * @throws PrinterException if an error in the print system causes the job
     *         to be aborted
     * @throws SecurityException if this thread is not allowed to
     *                           initiate a print job request
     *
     * @see #getPrintable
     * @see java.text.MessageFormat
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.util.concurrent.FutureTask
     *
     * @since 1.6
     */
    public boolean print(final MessageFormat headerFormat,
            final MessageFormat footerFormat,
            final boolean showPrintDialog,
            final PrintService service,
            final PrintRequestAttributeSet attributes,
            final boolean interactive)
            throws PrinterException {

        final PrinterJob job = PrinterJob.getPrinterJob();
        final Printable printable;
        final PrintingStatus printingStatus;
        final boolean isHeadless = GraphicsEnvironment.isHeadless();
        final boolean isEventDispatchThread =
            SwingUtilities.isEventDispatchThread();
        final Printable textPrintable = getPrintable(headerFormat, footerFormat);
        if (interactive && ! isHeadless) {
            printingStatus =
                PrintingStatus.createPrintingStatus(this, job);
            printable =
                printingStatus.createNotificationPrintable(textPrintable);
        } else {
            printingStatus = null;
            printable = textPrintable;
        }

        if (service != null) {
            job.setPrintService(service);
        }

        job.setPrintable(printable);

        final PrintRequestAttributeSet attr = (attributes == null)
            ? new HashPrintRequestAttributeSet()
            : attributes;

        if (showPrintDialog && ! isHeadless && ! job.printDialog(attr)) {
            return false;
        }

        /*
         * there are three cases for printing:
         * 1. print non interactively (! interactive || isHeadless)
         * 2. print interactively off EDT
         * 3. print interactively on EDT
         *
         * 1 and 2 prints on the current thread (3 prints on another thread)
         * 2 and 3 deal with PrintingStatusDialog
         */
        final Callable<Object> doPrint =
            new Callable<Object>() {
                public Object call() throws Exception {
                    try {
                        job.print(attr);
                    } finally {
                        if (printingStatus != null) {
                            printingStatus.dispose();
                        }
                    }
                    return null;
                }
            };

        final FutureTask<Object> futurePrinting =
            new FutureTask<Object>(doPrint);

        final Runnable runnablePrinting =
            new Runnable() {
                public void run() {
                    //disable component
                    boolean wasEnabled = false;
                    if (isEventDispatchThread) {
                        if (isEnabled()) {
                            wasEnabled = true;
                            setEnabled(false);
                        }
                    } else {
                        try {
                            wasEnabled = SwingUtilities2.submit(
                                new Callable<Boolean>() {
                                    public Boolean call() throws Exception {
                                        boolean rv = isEnabled();
                                        if (rv) {
                                            setEnabled(false);
                                        }
                                        return rv;
                                    }
                                }).get();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (ExecutionException e) {
                            Throwable cause = e.getCause();
                            if (cause instanceof Error) {
                                throw (Error) cause;
                            }
                            if (cause instanceof RuntimeException) {
                                throw (RuntimeException) cause;
                            }
                            throw new AssertionError(cause);
                        }
                    }

                    getDocument().render(futurePrinting);

                    //enable component
                    if (wasEnabled) {
                        if (isEventDispatchThread) {
                            setEnabled(true);
                        } else {
                            try {
                                SwingUtilities2.submit(
                                    new Runnable() {
                                        public void run() {
                                            setEnabled(true);
                                        }
                                    }, null).get();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            } catch (ExecutionException e) {
                                Throwable cause = e.getCause();
                                if (cause instanceof Error) {
                                    throw (Error) cause;
                                }
                                if (cause instanceof RuntimeException) {
                                    throw (RuntimeException) cause;
                                }
                                throw new AssertionError(cause);
                            }
                        }
                    }
                }
            };

        if (! interactive || isHeadless) {
            runnablePrinting.run();
        } else {
            if (isEventDispatchThread) {
                new Thread(null, runnablePrinting,
                           "JTextComponentPrint", 0, false ).start();
                printingStatus.showModal(true);
            } else {
                printingStatus.showModal(false);
                runnablePrinting.run();
            }
        }

        //the printing is done successfully or otherwise.
        //dialog is hidden if needed.
        try {
            futurePrinting.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof PrinterAbortException) {
                if (printingStatus != null
                    && printingStatus.isAborted()) {
                    return false;
                } else {
                    throw (PrinterAbortException) cause;
                }
            } else if (cause instanceof PrinterException) {
                throw (PrinterException) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new AssertionError(cause);
            }
        }
        return true;
    }


    /**
     * Returns a {@code Printable} to use for printing the content of this
     * {@code JTextComponent}. The returned {@code Printable} prints
     * the document as it looks on the screen except being reformatted
     * to fit the paper.
     * The returned {@code Printable} can be wrapped inside another
     * {@code Printable} in order to create complex reports and
     * documents.
     *
     *
     * <p>
     * The returned {@code Printable} shares the {@code document} with this
     * {@code JTextComponent}. It is the responsibility of the developer to
     * ensure that the {@code document} is not mutated while this {@code Printable}
     * is used. Printing behavior is undefined when the {@code document} is
     * mutated during printing.
     *
     * <p>
     * Page header and footer text can be added to the output by providing
     * {@code MessageFormat} arguments. The printing code requests
     * {@code Strings} from the formats, providing a single item which may be
     * included in the formatted string: an {@code Integer} representing the
     * current page number.
     *
     * <p>
     * The returned {@code Printable} when printed, formats the
     * document content appropriately for the page size. For correct
     * line wrapping the {@code imageable width} of all pages must be the
     * same. See {@link java.awt.print.PageFormat#getImageableWidth}.
     *
     * <p>
     * This method is thread-safe, although most Swing methods are not. Please
     * see <A
     * HREF="https://docs.oracle.com/javase/tutorial/uiswing/concurrency/index.html">
     * Concurrency in Swing</A> for more information.
     *
     * <p>
     * The returned {@code Printable} can be printed on any thread.
     *
     * <p>
     * This implementation returned {@code Printable} performs all painting on
     * the <i>Event Dispatch Thread</i>, regardless of what thread it is
     * used on.
     *
     * @param headerFormat the text, in {@code MessageFormat}, to be
     *        used as the header, or {@code null} for no header
     * @param footerFormat the text, in {@code MessageFormat}, to be
     *        used as the footer, or {@code null} for no footer
     * @return a {@code Printable} for use in printing content of this
     *         {@code JTextComponent}
     *
     *
     * @see java.awt.print.Printable
     * @see java.awt.print.PageFormat
     * @see javax.swing.text.Document#render(java.lang.Runnable)
     *
     * @since 1.6
     */
    public Printable getPrintable(final MessageFormat headerFormat,
                                  final MessageFormat footerFormat) {
        return TextComponentPrintable.getPrintable(
                   this, headerFormat, footerFormat);
    }


/////////////////
// Accessibility support
////////////////


    /**
     * Gets the <code>AccessibleContext</code> associated with this
     * <code>JTextComponent</code>. For text components,
     * the <code>AccessibleContext</code> takes the form of an
     * <code>AccessibleJTextComponent</code>.
     * A new <code>AccessibleJTextComponent</code> instance
     * is created if necessary.
     *
     * @return an <code>AccessibleJTextComponent</code> that serves as the
     *         <code>AccessibleContext</code> of this
     *         <code>JTextComponent</code>
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJTextComponent();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JTextComponent</code> class.  It provides an implementation of
     * the Java Accessibility API appropriate to menu user-interface elements.
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
    @SuppressWarnings("serial") // Same-version serialization only
    public class AccessibleJTextComponent extends AccessibleJComponent
    implements AccessibleText, CaretListener, DocumentListener,
               AccessibleAction, AccessibleEditableText,
               AccessibleExtendedText {

        int caretPos;
        Point oldLocationOnScreen;

        /**
         * Constructs an AccessibleJTextComponent.  Adds a listener to track
         * caret change.
         */
        public AccessibleJTextComponent() {
            Document doc = JTextComponent.this.getDocument();
            if (doc != null) {
                doc.addDocumentListener(this);
            }
            JTextComponent.this.addCaretListener(this);
            caretPos = getCaretPosition();

            try {
                oldLocationOnScreen = getLocationOnScreen();
            } catch (IllegalComponentStateException iae) {
            }

            // Fire a ACCESSIBLE_VISIBLE_DATA_PROPERTY PropertyChangeEvent
            // when the text component moves (e.g., when scrolling).
            // Using an anonymous class since making AccessibleJTextComponent
            // implement ComponentListener would be an API change.
            JTextComponent.this.addComponentListener(new ComponentAdapter() {

                public void componentMoved(ComponentEvent e) {
                    try {
                        Point newLocationOnScreen = getLocationOnScreen();
                        firePropertyChange(ACCESSIBLE_VISIBLE_DATA_PROPERTY,
                                           oldLocationOnScreen,
                                           newLocationOnScreen);

                        oldLocationOnScreen = newLocationOnScreen;
                    } catch (IllegalComponentStateException iae) {
                    }
                }
            });
        }

        /**
         * Handles caret updates (fire appropriate property change event,
         * which are AccessibleContext.ACCESSIBLE_CARET_PROPERTY and
         * AccessibleContext.ACCESSIBLE_SELECTION_PROPERTY).
         * This keeps track of the dot position internally.  When the caret
         * moves, the internal position is updated after firing the event.
         *
         * @param e the CaretEvent
         */
        public void caretUpdate(CaretEvent e) {
            int dot = e.getDot();
            int mark = e.getMark();
            if (caretPos != dot) {
                // the caret moved
                firePropertyChange(ACCESSIBLE_CARET_PROPERTY,
                    caretPos, dot);
                caretPos = dot;

                try {
                    oldLocationOnScreen = getLocationOnScreen();
                } catch (IllegalComponentStateException iae) {
                }
            }
            if (mark != dot) {
                // there is a selection
                firePropertyChange(ACCESSIBLE_SELECTION_PROPERTY, null,
                    getSelectedText());
            }
        }

        // DocumentListener methods

        /**
         * Handles document insert (fire appropriate property change event
         * which is AccessibleContext.ACCESSIBLE_TEXT_PROPERTY).
         * This tracks the changed offset via the event.
         *
         * @param e the DocumentEvent
         */
        public void insertUpdate(DocumentEvent e) {
            final Integer pos = e.getOffset();
            if (SwingUtilities.isEventDispatchThread()) {
                firePropertyChange(ACCESSIBLE_TEXT_PROPERTY, null, pos);
            } else {
                Runnable doFire = new Runnable() {
                    public void run() {
                        firePropertyChange(ACCESSIBLE_TEXT_PROPERTY,
                                           null, pos);
                    }
                };
                SwingUtilities.invokeLater(doFire);
            }
        }

        /**
         * Handles document remove (fire appropriate property change event,
         * which is AccessibleContext.ACCESSIBLE_TEXT_PROPERTY).
         * This tracks the changed offset via the event.
         *
         * @param e the DocumentEvent
         */
        public void removeUpdate(DocumentEvent e) {
            final Integer pos = e.getOffset();
            if (SwingUtilities.isEventDispatchThread()) {
                firePropertyChange(ACCESSIBLE_TEXT_PROPERTY, null, pos);
            } else {
                Runnable doFire = new Runnable() {
                    public void run() {
                        firePropertyChange(ACCESSIBLE_TEXT_PROPERTY,
                                           null, pos);
                    }
                };
                SwingUtilities.invokeLater(doFire);
            }
        }

        /**
         * Handles document remove (fire appropriate property change event,
         * which is AccessibleContext.ACCESSIBLE_TEXT_PROPERTY).
         * This tracks the changed offset via the event.
         *
         * @param e the DocumentEvent
         */
        public void changedUpdate(DocumentEvent e) {
            final Integer pos = e.getOffset();
            if (SwingUtilities.isEventDispatchThread()) {
                firePropertyChange(ACCESSIBLE_TEXT_PROPERTY, null, pos);
            } else {
                Runnable doFire = new Runnable() {
                    public void run() {
                        firePropertyChange(ACCESSIBLE_TEXT_PROPERTY,
                                           null, pos);
                    }
                };
                SwingUtilities.invokeLater(doFire);
            }
        }

        /**
         * Gets the state set of the JTextComponent.
         * The AccessibleStateSet of an object is composed of a set of
         * unique AccessibleState's.  A change in the AccessibleStateSet
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
            if (JTextComponent.this.isEditable()) {
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
            if (p == null) {
                return -1;
            }
            return JTextComponent.this.viewToModel(p);
        }

            /**
             * Gets the editor's drawing rectangle.  Stolen
             * from the unfortunately named
             * BasicTextUI.getVisibleEditorRect()
             *
             * @return the bounding box for the root view
             */
            Rectangle getRootEditorRect() {
                Rectangle alloc = JTextComponent.this.getBounds();
                if ((alloc.width > 0) && (alloc.height > 0)) {
                        alloc.x = alloc.y = 0;
                        Insets insets = JTextComponent.this.getInsets();
                        alloc.x += insets.left;
                        alloc.y += insets.top;
                        alloc.width -= insets.left + insets.right;
                        alloc.height -= insets.top + insets.bottom;
                        return alloc;
                }
                return null;
            }

        /**
         * Determines the bounding box of the character at the given
         * index into the string.  The bounds are returned in local
         * coordinates.  If the index is invalid a null rectangle
         * is returned.
         *
         * The screen coordinates returned are "unscrolled coordinates"
         * if the JTextComponent is contained in a JScrollPane in which
         * case the resulting rectangle should be composed with the parent
         * coordinates.  A good algorithm to use is:
         * <pre>
         * Accessible a:
         * AccessibleText at = a.getAccessibleText();
         * AccessibleComponent ac = a.getAccessibleComponent();
         * Rectangle r = at.getCharacterBounds();
         * Point p = ac.getLocation();
         * r.x += p.x;
         * r.y += p.y;
         * </pre>
         *
         * Note: the JTextComponent must have a valid size (e.g. have
         * been added to a parent container whose ancestor container
         * is a valid top-level window) for this method to be able
         * to return a meaningful (non-null) value.
         *
         * @param i the index into the String &ge; 0
         * @return the screen coordinates of the character's bounding box
         */
        public Rectangle getCharacterBounds(int i) {
            if (i < 0 || i > model.getLength()-1) {
                return null;
            }
            TextUI ui = getUI();
            if (ui == null) {
                return null;
            }
            Rectangle rect = null;
            Rectangle alloc = getRootEditorRect();
            if (alloc == null) {
                return null;
            }
            if (model instanceof AbstractDocument) {
                ((AbstractDocument)model).readLock();
            }
            try {
                View rootView = ui.getRootView(JTextComponent.this);
                if (rootView != null) {
                    rootView.setSize(alloc.width, alloc.height);

                    Shape bounds = rootView.modelToView(i,
                                    Position.Bias.Forward, i+1,
                                    Position.Bias.Backward, alloc);

                    rect = (bounds instanceof Rectangle) ?
                     (Rectangle)bounds : bounds.getBounds();

                }
            } catch (BadLocationException e) {
            } finally {
                if (model instanceof AbstractDocument) {
                    ((AbstractDocument)model).readUnlock();
                }
            }
            return rect;
        }

        /**
         * Returns the number of characters (valid indices)
         *
         * @return the number of characters &ge; 0
         */
        public int getCharCount() {
            return model.getLength();
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
            return JTextComponent.this.getCaretPosition();
        }

        /**
         * Returns the AttributeSet for a given character (at a given index).
         *
         * @param i the zero-based index into the text
         * @return the AttributeSet of the character
         */
        public AttributeSet getCharacterAttribute(int i) {
            Element e = null;
            if (model instanceof AbstractDocument) {
                ((AbstractDocument)model).readLock();
            }
            try {
                for (e = model.getDefaultRootElement(); ! e.isLeaf(); ) {
                    int index = e.getElementIndex(i);
                    e = e.getElement(index);
                }
            } finally {
                if (model instanceof AbstractDocument) {
                    ((AbstractDocument)model).readUnlock();
                }
            }
            return e.getAttributes();
        }


        /**
         * Returns the start offset within the selected text.
         * If there is no selection, but there is
         * a caret, the start and end offsets will be the same.
         * Return 0 if the text is empty, or the caret position
         * if no selection.
         *
         * @return the index into the text of the start of the selection &ge; 0
         */
        public int getSelectionStart() {
            return JTextComponent.this.getSelectionStart();
        }

        /**
         * Returns the end offset within the selected text.
         * If there is no selection, but there is
         * a caret, the start and end offsets will be the same.
         * Return 0 if the text is empty, or the caret position
         * if no selection.
         *
         * @return the index into the text of the end of the selection &ge; 0
         */
        public int getSelectionEnd() {
            return JTextComponent.this.getSelectionEnd();
        }

        /**
         * Returns the portion of the text that is selected.
         *
         * @return the text, null if no selection
         */
        public String getSelectedText() {
            return JTextComponent.this.getSelectedText();
        }

       /**
         * IndexedSegment extends Segment adding the offset into the
         * the model the <code>Segment</code> was asked for.
         */
        private class IndexedSegment extends Segment {
            /**
             * Offset into the model that the position represents.
             */
            public int modelOffset;
        }


        // TIGER - 4170173
        /**
         * Returns the String at a given index. Whitespace
         * between words is treated as a word.
         *
         * @param part the CHARACTER, WORD, or SENTENCE to retrieve
         * @param index an index within the text
         * @return the letter, word, or sentence.
         *
         */
        public String getAtIndex(int part, int index) {
            return getAtIndex(part, index, 0);
        }


        /**
         * Returns the String after a given index. Whitespace
         * between words is treated as a word.
         *
         * @param part the CHARACTER, WORD, or SENTENCE to retrieve
         * @param index an index within the text
         * @return the letter, word, or sentence.
         */
        public String getAfterIndex(int part, int index) {
            return getAtIndex(part, index, 1);
        }


        /**
         * Returns the String before a given index. Whitespace
         * between words is treated a word.
         *
         * @param part the CHARACTER, WORD, or SENTENCE to retrieve
         * @param index an index within the text
         * @return the letter, word, or sentence.
         */
        public String getBeforeIndex(int part, int index) {
            return getAtIndex(part, index, -1);
        }


        /**
         * Gets the word, sentence, or character at <code>index</code>.
         * If <code>direction</code> is non-null this will find the
         * next/previous word/sentence/character.
         */
        private String getAtIndex(int part, int index, int direction) {
            if (model instanceof AbstractDocument) {
                ((AbstractDocument)model).readLock();
            }
            try {
                if (index < 0 || index >= model.getLength()) {
                    return null;
                }
                switch (part) {
                case AccessibleText.CHARACTER:
                    if (index + direction < model.getLength() &&
                        index + direction >= 0) {
                        return model.getText(index + direction, 1);
                    }
                    break;


                case AccessibleText.WORD:
                case AccessibleText.SENTENCE:
                    IndexedSegment seg = getSegmentAt(part, index);
                    if (seg != null) {
                        if (direction != 0) {
                            int next;


                            if (direction < 0) {
                                next = seg.modelOffset - 1;
                            }
                            else {
                                next = seg.modelOffset + direction * seg.count;
                            }
                            if (next >= 0 && next <= model.getLength()) {
                                seg = getSegmentAt(part, next);
                            }
                            else {
                                seg = null;
                            }
                        }
                        if (seg != null) {
                            return new String(seg.array, seg.offset,
                                                  seg.count);
                        }
                    }
                    break;


                default:
                    break;
                }
            } catch (BadLocationException e) {
            } finally {
                if (model instanceof AbstractDocument) {
                    ((AbstractDocument)model).readUnlock();
                }
            }
            return null;
        }


        /*
         * Returns the paragraph element for the specified index.
         */
        private Element getParagraphElement(int index) {
            if (model instanceof PlainDocument ) {
                PlainDocument sdoc = (PlainDocument)model;
                return sdoc.getParagraphElement(index);
            } else if (model instanceof StyledDocument) {
                StyledDocument sdoc = (StyledDocument)model;
                return sdoc.getParagraphElement(index);
            } else {
                Element para;
                for (para = model.getDefaultRootElement(); ! para.isLeaf(); ) {
                    int pos = para.getElementIndex(index);
                    para = para.getElement(pos);
                }
                if (para == null) {
                    return null;
                }
                return para.getParentElement();
            }
        }

        /*
         * Returns a <code>Segment</code> containing the paragraph text
         * at <code>index</code>, or null if <code>index</code> isn't
         * valid.
         */
        private IndexedSegment getParagraphElementText(int index)
                                  throws BadLocationException {
            Element para = getParagraphElement(index);


            if (para != null) {
                IndexedSegment segment = new IndexedSegment();
                try {
                    int length = para.getEndOffset() - para.getStartOffset();
                    model.getText(para.getStartOffset(), length, segment);
                } catch (BadLocationException e) {
                    return null;
                }
                segment.modelOffset = para.getStartOffset();
                return segment;
            }
            return null;
        }


        /**
         * Returns the Segment at <code>index</code> representing either
         * the paragraph or sentence as identified by <code>part</code>, or
         * null if a valid paragraph/sentence can't be found. The offset
         * will point to the start of the word/sentence in the array, and
         * the modelOffset will point to the location of the word/sentence
         * in the model.
         */
        private IndexedSegment getSegmentAt(int part, int index) throws
                                  BadLocationException {
            IndexedSegment seg = getParagraphElementText(index);
            if (seg == null) {
                return null;
            }
            BreakIterator iterator;
            switch (part) {
            case AccessibleText.WORD:
                iterator = BreakIterator.getWordInstance(getLocale());
                break;
            case AccessibleText.SENTENCE:
                iterator = BreakIterator.getSentenceInstance(getLocale());
                break;
            default:
                return null;
            }
            seg.first();
            iterator.setText(seg);
            int end = iterator.following(index - seg.modelOffset + seg.offset);
            if (end == BreakIterator.DONE) {
                return null;
            }
            if (end > seg.offset + seg.count) {
                return null;
            }
            int begin = iterator.previous();
            if (begin == BreakIterator.DONE ||
                         begin >= seg.offset + seg.count) {
                return null;
            }
            seg.modelOffset = seg.modelOffset + begin - seg.offset;
            seg.offset = begin;
            seg.count = end - begin;
            return seg;
        }

        // begin AccessibleEditableText methods -----

        /**
         * Returns the AccessibleEditableText interface for
         * this text component.
         *
         * @return the AccessibleEditableText interface
         * @since 1.4
         */
        public AccessibleEditableText getAccessibleEditableText() {
            return this;
        }

        /**
         * Sets the text contents to the specified string.
         *
         * @param s the string to set the text contents
         * @since 1.4
         */
        public void setTextContents(String s) {
            JTextComponent.this.setText(s);
        }

        /**
         * Inserts the specified string at the given index
         *
         * @param index the index in the text where the string will
         * be inserted
         * @param s the string to insert in the text
         * @since 1.4
         */
        public void insertTextAtIndex(int index, String s) {
            Document doc = JTextComponent.this.getDocument();
            if (doc != null) {
                try {
                    if (s != null && s.length() > 0) {
                        boolean composedTextSaved = saveComposedText(index);
                        doc.insertString(index, s, null);
                        if (composedTextSaved) {
                            restoreComposedText();
                        }
                    }
                } catch (BadLocationException e) {
                    UIManager.getLookAndFeel().provideErrorFeedback(JTextComponent.this);
                }
            }
        }

        /**
         * Returns the text string between two indices.
         *
         * @param startIndex the starting index in the text
         * @param endIndex the ending index in the text
         * @return the text string between the indices
         * @since 1.4
         */
        public String getTextRange(int startIndex, int endIndex) {
            String txt = null;
            int p0 = Math.min(startIndex, endIndex);
            int p1 = Math.max(startIndex, endIndex);
            if (p0 != p1) {
                try {
                    Document doc = JTextComponent.this.getDocument();
                    txt = doc.getText(p0, p1 - p0);
                } catch (BadLocationException e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            }
            return txt;
        }

        /**
         * Deletes the text between two indices
         *
         * @param startIndex the starting index in the text
         * @param endIndex the ending index in the text
         * @since 1.4
         */
        public void delete(int startIndex, int endIndex) {
            if (isEditable() && isEnabled()) {
                try {
                    int p0 = Math.min(startIndex, endIndex);
                    int p1 = Math.max(startIndex, endIndex);
                    if (p0 != p1) {
                        Document doc = getDocument();
                        doc.remove(p0, p1 - p0);
                    }
                } catch (BadLocationException e) {
                }
            } else {
                UIManager.getLookAndFeel().provideErrorFeedback(JTextComponent.this);
            }
        }

        /**
         * Cuts the text between two indices into the system clipboard.
         *
         * @param startIndex the starting index in the text
         * @param endIndex the ending index in the text
         * @since 1.4
         */
        public void cut(int startIndex, int endIndex) {
            selectText(startIndex, endIndex);
            JTextComponent.this.cut();
        }

        /**
         * Pastes the text from the system clipboard into the text
         * starting at the specified index.
         *
         * @param startIndex the starting index in the text
         * @since 1.4
         */
        public void paste(int startIndex) {
            setCaretPosition(startIndex);
            JTextComponent.this.paste();
        }

        /**
         * Replaces the text between two indices with the specified
         * string.
         *
         * @param startIndex the starting index in the text
         * @param endIndex the ending index in the text
         * @param s the string to replace the text between two indices
         * @since 1.4
         */
        public void replaceText(int startIndex, int endIndex, String s) {
            selectText(startIndex, endIndex);
            JTextComponent.this.replaceSelection(s);
        }

        /**
         * Selects the text between two indices.
         *
         * @param startIndex the starting index in the text
         * @param endIndex the ending index in the text
         * @since 1.4
         */
        public void selectText(int startIndex, int endIndex) {
            JTextComponent.this.select(startIndex, endIndex);
        }

        /**
         * Sets attributes for the text between two indices.
         *
         * @param startIndex the starting index in the text
         * @param endIndex the ending index in the text
         * @param as the attribute set
         * @see AttributeSet
         * @since 1.4
         */
        public void setAttributes(int startIndex, int endIndex,
            AttributeSet as) {

            // Fixes bug 4487492
            Document doc = JTextComponent.this.getDocument();
            if (doc != null && doc instanceof StyledDocument) {
                StyledDocument sDoc = (StyledDocument)doc;
                int offset = startIndex;
                int length = endIndex - startIndex;
                sDoc.setCharacterAttributes(offset, length, as, true);
            }
        }

        // ----- end AccessibleEditableText methods


        // ----- begin AccessibleExtendedText methods

// Probably should replace the helper method getAtIndex() to return
// instead an AccessibleTextSequence also for LINE & ATTRIBUTE_RUN
// and then make the AccessibleText methods get[At|After|Before]Point
// call this new method instead and return only the string portion

        /**
         * Returns the AccessibleTextSequence at a given <code>index</code>.
         * If <code>direction</code> is non-null this will find the
         * next/previous word/sentence/character.
         *
         * @param part the <code>CHARACTER</code>, <code>WORD</code>,
         * <code>SENTENCE</code>, <code>LINE</code> or
         * <code>ATTRIBUTE_RUN</code> to retrieve
         * @param index an index within the text
         * @param direction is either -1, 0, or 1
         * @return an <code>AccessibleTextSequence</code> specifying the text
         * if <code>part</code> and <code>index</code> are valid.  Otherwise,
         * <code>null</code> is returned.
         *
         * @see javax.accessibility.AccessibleText#CHARACTER
         * @see javax.accessibility.AccessibleText#WORD
         * @see javax.accessibility.AccessibleText#SENTENCE
         * @see javax.accessibility.AccessibleExtendedText#LINE
         * @see javax.accessibility.AccessibleExtendedText#ATTRIBUTE_RUN
         *
         * @since 1.6
         */
        private AccessibleTextSequence getSequenceAtIndex(int part,
            int index, int direction) {
            if (index < 0 || index >= model.getLength()) {
                return null;
            }
            if (direction < -1 || direction > 1) {
                return null;    // direction must be 1, 0, or -1
            }

            switch (part) {
            case AccessibleText.CHARACTER:
                if (model instanceof AbstractDocument) {
                    ((AbstractDocument)model).readLock();
                }
                AccessibleTextSequence charSequence = null;
                try {
                    if (index + direction < model.getLength() &&
                        index + direction >= 0) {
                        charSequence =
                            new AccessibleTextSequence(index + direction,
                            index + direction + 1,
                            model.getText(index + direction, 1));
                    }

                } catch (BadLocationException e) {
                    // we are intentionally silent; our contract says we return
                    // null if there is any failure in this method
                } finally {
                    if (model instanceof AbstractDocument) {
                        ((AbstractDocument)model).readUnlock();
                    }
                }
                return charSequence;

            case AccessibleText.WORD:
            case AccessibleText.SENTENCE:
                if (model instanceof AbstractDocument) {
                    ((AbstractDocument)model).readLock();
                }
                AccessibleTextSequence rangeSequence = null;
                try {
                    IndexedSegment seg = getSegmentAt(part, index);
                    if (seg != null) {
                        if (direction != 0) {
                            int next;

                            if (direction < 0) {
                                next = seg.modelOffset - 1;
                            }
                            else {
                                next = seg.modelOffset + seg.count;
                            }
                            if (next >= 0 && next <= model.getLength()) {
                                seg = getSegmentAt(part, next);
                            }
                            else {
                                seg = null;
                            }
                        }
                        if (seg != null &&
                            (seg.offset + seg.count) <= model.getLength()) {
                            rangeSequence =
                                new AccessibleTextSequence (seg.offset,
                                seg.offset + seg.count,
                                new String(seg.array, seg.offset, seg.count));
                        } // else we leave rangeSequence set to null
                    }
                } catch(BadLocationException e) {
                    // we are intentionally silent; our contract says we return
                    // null if there is any failure in this method
                } finally {
                    if (model instanceof AbstractDocument) {
                        ((AbstractDocument)model).readUnlock();
                    }
                }
                return rangeSequence;

            case AccessibleExtendedText.LINE:
                AccessibleTextSequence lineSequence = null;
                if (model instanceof AbstractDocument) {
                    ((AbstractDocument)model).readLock();
                }
                try {
                    int startIndex =
                        Utilities.getRowStart(JTextComponent.this, index);
                    int endIndex =
                        Utilities.getRowEnd(JTextComponent.this, index);
                    if (startIndex >= 0 && endIndex >= startIndex) {
                        if (direction == 0) {
                            lineSequence =
                                new AccessibleTextSequence(startIndex, endIndex,
                                    model.getText(startIndex,
                                        endIndex - startIndex + 1));
                        } else if (direction == -1 && startIndex > 0) {
                            endIndex =
                                Utilities.getRowEnd(JTextComponent.this,
                                    startIndex - 1);
                            startIndex =
                                Utilities.getRowStart(JTextComponent.this,
                                    startIndex - 1);
                            if (startIndex >= 0 && endIndex >= startIndex) {
                                lineSequence =
                                    new AccessibleTextSequence(startIndex,
                                        endIndex,
                                        model.getText(startIndex,
                                            endIndex - startIndex + 1));
                            }
                        } else if (direction == 1 &&
                         endIndex < model.getLength()) {
                            startIndex =
                                Utilities.getRowStart(JTextComponent.this,
                                    endIndex + 1);
                            endIndex =
                                Utilities.getRowEnd(JTextComponent.this,
                                    endIndex + 1);
                            if (startIndex >= 0 && endIndex >= startIndex) {
                                lineSequence =
                                    new AccessibleTextSequence(startIndex,
                                        endIndex, model.getText(startIndex,
                                            endIndex - startIndex + 1));
                            }
                        }
                        // already validated 'direction' above...
                    }
                } catch(BadLocationException e) {
                    // we are intentionally silent; our contract says we return
                    // null if there is any failure in this method
                } finally {
                    if (model instanceof AbstractDocument) {
                        ((AbstractDocument)model).readUnlock();
                    }
                }
                return lineSequence;

            case AccessibleExtendedText.ATTRIBUTE_RUN:
                // assumptions: (1) that all characters in a single element
                // share the same attribute set; (2) that adjacent elements
                // *may* share the same attribute set

                int attributeRunStartIndex, attributeRunEndIndex;
                String runText = null;
                if (model instanceof AbstractDocument) {
                    ((AbstractDocument)model).readLock();
                }

                try {
                    attributeRunStartIndex = attributeRunEndIndex =
                     Integer.MIN_VALUE;
                    int tempIndex = index;
                    switch (direction) {
                    case -1:
                        // going backwards, so find left edge of this run -
                        // that'll be the end of the previous run
                        // (off-by-one counting)
                        attributeRunEndIndex = getRunEdge(index, direction);
                        // now set ourselves up to find the left edge of the
                        // prev. run
                        tempIndex = attributeRunEndIndex - 1;
                        break;
                    case 1:
                        // going forward, so find right edge of this run -
                        // that'll be the start of the next run
                        // (off-by-one counting)
                        attributeRunStartIndex = getRunEdge(index, direction);
                        // now set ourselves up to find the right edge of the
                        // next run
                        tempIndex = attributeRunStartIndex;
                        break;
                    case 0:
                        // interested in the current run, so nothing special to
                        // set up in advance...
                        break;
                    default:
                        // only those three values of direction allowed...
                        throw new AssertionError(direction);
                    }

                    // set the unset edge; if neither set then we're getting
                    // both edges of the current run around our 'index'
                    attributeRunStartIndex =
                        (attributeRunStartIndex != Integer.MIN_VALUE) ?
                        attributeRunStartIndex : getRunEdge(tempIndex, -1);
                    attributeRunEndIndex =
                        (attributeRunEndIndex != Integer.MIN_VALUE) ?
                        attributeRunEndIndex : getRunEdge(tempIndex, 1);

                    runText = model.getText(attributeRunStartIndex,
                                            attributeRunEndIndex -
                                            attributeRunStartIndex);
                } catch (BadLocationException e) {
                    // we are intentionally silent; our contract says we return
                    // null if there is any failure in this method
                    return null;
                } finally {
                    if (model instanceof AbstractDocument) {
                        ((AbstractDocument)model).readUnlock();
                    }
                }
                return new AccessibleTextSequence(attributeRunStartIndex,
                                                  attributeRunEndIndex,
                                                  runText);

            default:
                break;
            }
            return null;
        }


        /**
         * Starting at text position <code>index</code>, and going in
         * <code>direction</code>, return the edge of run that shares the
         * same <code>AttributeSet</code> and parent element as those at
         * <code>index</code>.
         *
         * Note: we assume the document is already locked...
         */
        private int getRunEdge(int index, int direction) throws
         BadLocationException {
            if (index < 0 || index >= model.getLength()) {
                throw new BadLocationException("Location out of bounds", index);
            }
            // locate the Element at index
            Element indexElement;
            // locate the Element at our index/offset
            int elementIndex = -1;        // test for initialization
            for (indexElement = model.getDefaultRootElement();
                 ! indexElement.isLeaf(); ) {
                elementIndex = indexElement.getElementIndex(index);
                indexElement = indexElement.getElement(elementIndex);
            }
            if (elementIndex == -1) {
                throw new AssertionError(index);
            }
            // cache the AttributeSet and parentElement atindex
            AttributeSet indexAS = indexElement.getAttributes();
            Element parent = indexElement.getParentElement();

            // find the first Element before/after ours w/the same AttributeSet
            // if we are already at edge of the first element in our parent
            // then return that edge
            Element edgeElement;
            switch (direction) {
            case -1:
            case 1:
                int edgeElementIndex = elementIndex;
                int elementCount = parent.getElementCount();
                while ((edgeElementIndex + direction) > 0 &&
                       ((edgeElementIndex + direction) < elementCount) &&
                       parent.getElement(edgeElementIndex
                       + direction).getAttributes().isEqual(indexAS)) {
                    edgeElementIndex += direction;
                }
                edgeElement = parent.getElement(edgeElementIndex);
                break;
            default:
                throw new AssertionError(direction);
            }
            switch (direction) {
            case -1:
                return edgeElement.getStartOffset();
            case 1:
                return edgeElement.getEndOffset();
            default:
                // we already caught this case earlier; this is to satisfy
                // the compiler...
                return Integer.MIN_VALUE;
            }
        }

        // getTextRange() not needed; defined in AccessibleEditableText

        /**
         * Returns the <code>AccessibleTextSequence</code> at a given
         * <code>index</code>.
         *
         * @param part the <code>CHARACTER</code>, <code>WORD</code>,
         * <code>SENTENCE</code>, <code>LINE</code> or
         * <code>ATTRIBUTE_RUN</code> to retrieve
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
            return getSequenceAtIndex(part, index, 0);
        }

        /**
         * Returns the <code>AccessibleTextSequence</code> after a given
         * <code>index</code>.
         *
         * @param part the <code>CHARACTER</code>, <code>WORD</code>,
         * <code>SENTENCE</code>, <code>LINE</code> or
         * <code>ATTRIBUTE_RUN</code> to retrieve
         * @param index an index within the text
         * @return an <code>AccessibleTextSequence</code> specifying the text
         * if <code>part</code> and <code>index</code> are valid.  Otherwise,
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
            return getSequenceAtIndex(part, index, 1);
        }

        /**
         * Returns the <code>AccessibleTextSequence</code> before a given
         * <code>index</code>.
         *
         * @param part the <code>CHARACTER</code>, <code>WORD</code>,
         * <code>SENTENCE</code>, <code>LINE</code> or
         * <code>ATTRIBUTE_RUN</code> to retrieve
         * @param index an index within the text
         * @return an <code>AccessibleTextSequence</code> specifying the text
         * if <code>part</code> and <code>index</code> are valid.  Otherwise,
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
            return getSequenceAtIndex(part, index, -1);
        }

        /**
         * Returns the <code>Rectangle</code> enclosing the text between
         * two indicies.
         *
         * @param startIndex the start index in the text
         * @param endIndex the end index in the text
         * @return the bounding rectangle of the text if the indices are valid.
         * Otherwise, <code>null</code> is returned
         *
         * @since 1.6
         */
        public Rectangle getTextBounds(int startIndex, int endIndex) {
            if (startIndex < 0 || startIndex > model.getLength()-1 ||
                endIndex < 0 || endIndex > model.getLength()-1 ||
                startIndex > endIndex) {
                return null;
            }
            TextUI ui = getUI();
            if (ui == null) {
                return null;
            }
            Rectangle rect = null;
            Rectangle alloc = getRootEditorRect();
            if (alloc == null) {
                return null;
            }
            if (model instanceof AbstractDocument) {
                ((AbstractDocument)model).readLock();
            }
            try {
                View rootView = ui.getRootView(JTextComponent.this);
                if (rootView != null) {
                    Shape bounds = rootView.modelToView(startIndex,
                                    Position.Bias.Forward, endIndex,
                                    Position.Bias.Backward, alloc);

                    rect = (bounds instanceof Rectangle) ?
                     (Rectangle)bounds : bounds.getBounds();

                }
            } catch (BadLocationException e) {
            } finally {
                if (model instanceof AbstractDocument) {
                    ((AbstractDocument)model).readUnlock();
                }
            }
            return rect;
        }

        // ----- end AccessibleExtendedText methods


        // --- interface AccessibleAction methods ------------------------

        public AccessibleAction getAccessibleAction() {
            return this;
        }

        /**
         * Returns the number of accessible actions available in this object
         * If there are more than one, the first one is considered the
         * "default" action of the object.
         *
         * @return the zero-based number of Actions in this object
         * @since 1.4
         */
        public int getAccessibleActionCount() {
            Action [] actions = JTextComponent.this.getActions();
            return actions.length;
        }

        /**
         * Returns a description of the specified action of the object.
         *
         * @param i zero-based index of the actions
         * @return a String description of the action
         * @see #getAccessibleActionCount
         * @since 1.4
         */
        public String getAccessibleActionDescription(int i) {
            Action [] actions = JTextComponent.this.getActions();
            if (i < 0 || i >= actions.length) {
                return null;
            }
            return (String)actions[i].getValue(Action.NAME);
        }

        /**
         * Performs the specified Action on the object
         *
         * @param i zero-based index of actions
         * @return true if the action was performed; otherwise false.
         * @see #getAccessibleActionCount
         * @since 1.4
         */
        public boolean doAccessibleAction(int i) {
            Action [] actions = JTextComponent.this.getActions();
            if (i < 0 || i >= actions.length) {
                return false;
            }
            ActionEvent ae =
                new ActionEvent(JTextComponent.this,
                                ActionEvent.ACTION_PERFORMED, null,
                                EventQueue.getMostRecentEventTime(),
                                getCurrentEventModifiers());
            actions[i].actionPerformed(ae);
            return true;
        }

        // ----- end AccessibleAction methods


    }


    // --- serialization ---------------------------------------------

    @Serial
    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException
    {
        ObjectInputStream.GetField f = s.readFields();

        model = (Document) f.get("model", null);
        navigationFilter = (NavigationFilter) f.get("navigationFilter", null);
        caretColor = (Color) f.get("caretColor", null);
        selectionColor = (Color) f.get("selectionColor", null);
        selectedTextColor = (Color) f.get("selectedTextColor", null);
        disabledTextColor = (Color) f.get("disabledTextColor", null);
        editable = f.get("editable", false);
        margin = (Insets) f.get("margin", null);
        focusAccelerator = f.get("focusAccelerator", '\0');
        boolean newDragEnabled = f.get("dragEnabled", false);
        checkDragEnabled(newDragEnabled);
        dragEnabled = newDragEnabled;
        DropMode newDropMode = (DropMode) f.get("dropMode",
                DropMode.USE_SELECTION);
        checkDropMode(newDropMode);
        dropMode = newDropMode;
        composedTextAttribute = (SimpleAttributeSet) f.get("composedTextAttribute", null);
        composedTextContent = (String) f.get("composedTextContent", null);
        composedTextStart = (Position) f.get("composedTextStart", null);
        composedTextEnd = (Position) f.get("composedTextEnd", null);
        latestCommittedTextStart = (Position) f.get("latestCommittedTextStart", null);
        latestCommittedTextEnd = (Position) f.get("latestCommittedTextEnd", null);
        composedTextCaret = (ComposedTextCaret) f.get("composedTextCaret", null);
        checkedInputOverride = f.get("checkedInputOverride", false);
        needToSendKeyTypedEvent = f.get("needToSendKeyTypedEvent", false);

        caretEvent = new MutableCaretEvent(this);
        addMouseListener(caretEvent);
        addFocusListener(caretEvent);
    }

    // --- member variables ----------------------------------

    /**
     * The document model.
     */
    private Document model;

    /**
     * The caret used to display the insert position
     * and navigate throughout the document.
     *
     * PENDING(prinz)
     * This should be serializable, default installed
     * by UI.
     */
    private transient Caret caret;

    /**
     * Object responsible for restricting the cursor navigation.
     */
    private NavigationFilter navigationFilter;

    /**
     * The object responsible for managing highlights.
     *
     * PENDING(prinz)
     * This should be serializable, default installed
     * by UI.
     */
    private transient Highlighter highlighter;

    /**
     * The current key bindings in effect.
     *
     * PENDING(prinz)
     * This should be serializable, default installed
     * by UI.
     */
    private transient Keymap keymap;

    private transient MutableCaretEvent caretEvent;
    private Color caretColor;
    private Color selectionColor;
    private Color selectedTextColor;
    private Color disabledTextColor;
    private boolean editable;
    private Insets margin;
    private char focusAccelerator;
    private boolean dragEnabled;

    /**
     * The drop mode for this component.
     */
    private DropMode dropMode = DropMode.USE_SELECTION;

    /**
     * The drop location.
     */
    private transient DropLocation dropLocation;

    /**
     * Represents a drop location for <code>JTextComponent</code>s.
     *
     * @see #getDropLocation
     * @since 1.6
     */
    public static final class DropLocation extends TransferHandler.DropLocation {
        private final int index;
        private final Position.Bias bias;

        private DropLocation(Point p, int index, Position.Bias bias) {
            super(p);
            this.index = index;
            this.bias = bias;
        }

        /**
         * Returns the index where dropped data should be inserted into the
         * associated component. This index represents a position between
         * characters, as would be interpreted by a caret.
         *
         * @return the drop index
         */
        public int getIndex() {
            return index;
        }

        /**
         * Returns the bias for the drop index.
         *
         * @return the drop bias
         */
        public Position.Bias getBias() {
            return bias;
        }

        /**
         * Returns a string representation of this drop location.
         * This method is intended to be used for debugging purposes,
         * and the content and format of the returned string may vary
         * between implementations.
         *
         * @return a string representation of this drop location
         */
        public String toString() {
            return getClass().getName()
                   + "[dropPoint=" + getDropPoint() + ","
                   + "index=" + index + ","
                   + "bias=" + bias + "]";
        }
    }

    /**
     * TransferHandler used if one hasn't been supplied by the UI.
     */
    private static DefaultTransferHandler defaultTransferHandler;

    /**
     * Maps from class name to Boolean indicating if
     * <code>processInputMethodEvent</code> has been overriden.
     */
    @SuppressWarnings("removal")
    private static Cache<Class<?>,Boolean> METHOD_OVERRIDDEN
            = new Cache<Class<?>,Boolean>(Cache.Kind.WEAK, Cache.Kind.STRONG) {
        /**
         * Returns {@code true} if the specified {@code type} extends {@link JTextComponent}
         * and the {@link JTextComponent#processInputMethodEvent} method is overridden.
         */
        @Override
        public Boolean create(final Class<?> type) {
            if (JTextComponent.class == type) {
                return Boolean.FALSE;
            }
            if (get(type.getSuperclass())) {
                return Boolean.TRUE;
            }
            return AccessController.doPrivileged(
                    new PrivilegedAction<Boolean>() {
                        public Boolean run() {
                            try {
                                type.getDeclaredMethod("processInputMethodEvent", InputMethodEvent.class);
                                return Boolean.TRUE;
                            } catch (NoSuchMethodException exception) {
                                return Boolean.FALSE;
                            }
                        }
                    });
        }
    };

    /**
     * Returns a string representation of this <code>JTextComponent</code>.
     * This method is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     * <P>
     * Overriding <code>paramString</code> to provide information about the
     * specific new aspects of the JFC components.
     *
     * @return  a string representation of this <code>JTextComponent</code>
     */
    protected String paramString() {
        String editableString = (editable ?
                                 "true" : "false");
        String caretColorString = (caretColor != null ?
                                   caretColor.toString() : "");
        String selectionColorString = (selectionColor != null ?
                                       selectionColor.toString() : "");
        String selectedTextColorString = (selectedTextColor != null ?
                                          selectedTextColor.toString() : "");
        String disabledTextColorString = (disabledTextColor != null ?
                                          disabledTextColor.toString() : "");
        String marginString = (margin != null ?
                               margin.toString() : "");

        return super.paramString() +
        ",caretColor=" + caretColorString +
        ",disabledTextColor=" + disabledTextColorString +
        ",editable=" + editableString +
        ",margin=" + marginString +
        ",selectedTextColor=" + selectedTextColorString +
        ",selectionColor=" + selectionColorString;
    }


    /**
     * A Simple TransferHandler that exports the data as a String, and
     * imports the data from the String clipboard.  This is only used
     * if the UI hasn't supplied one, which would only happen if someone
     * hasn't subclassed Basic.
     */
    static class DefaultTransferHandler extends TransferHandler implements
                                        UIResource {
        public void exportToClipboard(JComponent comp, Clipboard clipboard,
                                      int action) throws IllegalStateException {
            if (comp instanceof JTextComponent) {
                JTextComponent text = (JTextComponent)comp;
                int p0 = text.getSelectionStart();
                int p1 = text.getSelectionEnd();
                if (p0 != p1) {
                    try {
                        Document doc = text.getDocument();
                        String srcData = doc.getText(p0, p1 - p0);
                        StringSelection contents =new StringSelection(srcData);

                        // this may throw an IllegalStateException,
                        // but it will be caught and handled in the
                        // action that invoked this method
                        clipboard.setContents(contents, null);

                        if (action == TransferHandler.MOVE) {
                            doc.remove(p0, p1 - p0);
                        }
                    } catch (BadLocationException ble) {}
                }
            }
        }
        public boolean importData(JComponent comp, Transferable t) {
            if (comp instanceof JTextComponent) {
                DataFlavor flavor = getFlavor(t.getTransferDataFlavors());

                if (flavor != null) {
                    InputContext ic = comp.getInputContext();
                    if (ic != null) {
                        ic.endComposition();
                    }
                    try {
                        String data = (String)t.getTransferData(flavor);

                        ((JTextComponent)comp).replaceSelection(data);
                        return true;
                    } catch (UnsupportedFlavorException ufe) {
                    } catch (IOException ioe) {
                    }
                }
            }
            return false;
        }
        public boolean canImport(JComponent comp,
                                 DataFlavor[] transferFlavors) {
            JTextComponent c = (JTextComponent)comp;
            if (!(c.isEditable() && c.isEnabled())) {
                return false;
            }
            return (getFlavor(transferFlavors) != null);
        }
        public int getSourceActions(JComponent c) {
            return NONE;
        }
        private DataFlavor getFlavor(DataFlavor[] flavors) {
            if (flavors != null) {
                for (DataFlavor flavor : flavors) {
                    if (flavor.equals(DataFlavor.stringFlavor)) {
                        return flavor;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Returns the JTextComponent that most recently had focus. The returned
     * value may currently have focus.
     */
    static final JTextComponent getFocusedComponent() {
        return (JTextComponent)AppContext.getAppContext().
            get(FOCUSED_COMPONENT);
    }

    @SuppressWarnings("deprecation")
    private int getCurrentEventModifiers() {
        int modifiers = 0;
        AWTEvent currentEvent = EventQueue.getCurrentEvent();
        if (currentEvent instanceof InputEvent) {
            modifiers = ((InputEvent)currentEvent).getModifiers();
        } else if (currentEvent instanceof ActionEvent) {
            modifiers = ((ActionEvent)currentEvent).getModifiers();
        }
        return modifiers;
    }

    private static final Object KEYMAP_TABLE =
        new StringBuilder("JTextComponent_KeymapTable");

    //
    // member variables used for on-the-spot input method
    // editing style support
    //
    private transient InputMethodRequests inputMethodRequestsHandler;
    private SimpleAttributeSet composedTextAttribute;
    private String composedTextContent;
    private Position composedTextStart;
    private Position composedTextEnd;
    private Position latestCommittedTextStart;
    private Position latestCommittedTextEnd;
    private ComposedTextCaret composedTextCaret;
    private transient Caret originalCaret;
    /**
     * Set to true after the check for the override of processInputMethodEvent
     * has been checked.
     */
    private boolean checkedInputOverride;
    private boolean needToSendKeyTypedEvent;

    static class DefaultKeymap implements Keymap {

        DefaultKeymap(String nm, Keymap parent) {
            this.nm = nm;
            this.parent = parent;
            bindings = new Hashtable<KeyStroke, Action>();
        }

        /**
         * Fetch the default action to fire if a
         * key is typed (ie a KEY_TYPED KeyEvent is received)
         * and there is no binding for it.  Typically this
         * would be some action that inserts text so that
         * the keymap doesn't require an action for each
         * possible key.
         */
        public Action getDefaultAction() {
            if (defaultAction != null) {
                return defaultAction;
            }
            return (parent != null) ? parent.getDefaultAction() : null;
        }

        /**
         * Set the default action to fire if a key is typed.
         */
        public void setDefaultAction(Action a) {
            defaultAction = a;
        }

        public String getName() {
            return nm;
        }

        public Action getAction(KeyStroke key) {
            Action a = bindings.get(key);
            if ((a == null) && (parent != null)) {
                a = parent.getAction(key);
            }
            return a;
        }

        public KeyStroke[] getBoundKeyStrokes() {
            KeyStroke[] keys = new KeyStroke[bindings.size()];
            int i = 0;
            for (Enumeration<KeyStroke> e = bindings.keys() ; e.hasMoreElements() ;) {
                keys[i++] = e.nextElement();
            }
            return keys;
        }

        public Action[] getBoundActions() {
            Action[] actions = new Action[bindings.size()];
            int i = 0;
            for (Enumeration<Action> e = bindings.elements() ; e.hasMoreElements() ;) {
                actions[i++] = e.nextElement();
            }
            return actions;
        }

        public KeyStroke[] getKeyStrokesForAction(Action a) {
            if (a == null) {
                return null;
            }
            KeyStroke[] retValue = null;
            // Determine local bindings first.
            Vector<KeyStroke> keyStrokes = null;
            for (Enumeration<KeyStroke> keys = bindings.keys(); keys.hasMoreElements();) {
                KeyStroke key = keys.nextElement();
                if (bindings.get(key) == a) {
                    if (keyStrokes == null) {
                        keyStrokes = new Vector<KeyStroke>();
                    }
                    keyStrokes.addElement(key);
                }
            }
            // See if the parent has any.
            if (parent != null) {
                KeyStroke[] pStrokes = parent.getKeyStrokesForAction(a);
                if (pStrokes != null) {
                    // Remove any bindings defined in the parent that
                    // are locally defined.
                    int rCount = 0;
                    for (int counter = pStrokes.length - 1; counter >= 0;
                         counter--) {
                        if (isLocallyDefined(pStrokes[counter])) {
                            pStrokes[counter] = null;
                            rCount++;
                        }
                    }
                    if (rCount > 0 && rCount < pStrokes.length) {
                        if (keyStrokes == null) {
                            keyStrokes = new Vector<KeyStroke>();
                        }
                        for (int counter = pStrokes.length - 1; counter >= 0;
                             counter--) {
                            if (pStrokes[counter] != null) {
                                keyStrokes.addElement(pStrokes[counter]);
                            }
                        }
                    }
                    else if (rCount == 0) {
                        if (keyStrokes == null) {
                            retValue = pStrokes;
                        }
                        else {
                            retValue = new KeyStroke[keyStrokes.size() +
                                                    pStrokes.length];
                            keyStrokes.copyInto(retValue);
                            System.arraycopy(pStrokes, 0, retValue,
                                        keyStrokes.size(), pStrokes.length);
                            keyStrokes = null;
                        }
                    }
                }
            }
            if (keyStrokes != null) {
                retValue = new KeyStroke[keyStrokes.size()];
                keyStrokes.copyInto(retValue);
            }
            return retValue;
        }

        public boolean isLocallyDefined(KeyStroke key) {
            return bindings.containsKey(key);
        }

        public void addActionForKeyStroke(KeyStroke key, Action a) {
            bindings.put(key, a);
        }

        public void removeKeyStrokeBinding(KeyStroke key) {
            bindings.remove(key);
        }

        public void removeBindings() {
            bindings.clear();
        }

        public Keymap getResolveParent() {
            return parent;
        }

        public void setResolveParent(Keymap parent) {
            this.parent = parent;
        }

        /**
         * String representation of the keymap... potentially
         * a very long string.
         */
        public String toString() {
            return "Keymap[" + nm + "]" + bindings;
        }

        String nm;
        Keymap parent;
        Hashtable<KeyStroke, Action> bindings;
        Action defaultAction;
    }


    /**
     * KeymapWrapper wraps a Keymap inside an InputMap. For KeymapWrapper
     * to be useful it must be used with a KeymapActionMap.
     * KeymapWrapper for the most part, is an InputMap with two parents.
     * The first parent visited is ALWAYS the Keymap, with the second
     * parent being the parent inherited from InputMap. If
     * <code>keymap.getAction</code> returns null, implying the Keymap
     * does not have a binding for the KeyStroke,
     * the parent is then visited. If the Keymap has a binding, the
     * Action is returned, if not and the KeyStroke represents a
     * KeyTyped event and the Keymap has a defaultAction,
     * <code>DefaultActionKey</code> is returned.
     * <p>KeymapActionMap is then able to transate the object passed in
     * to either message the Keymap, or message its default implementation.
     */
    static class KeymapWrapper extends InputMap {
        static final Object DefaultActionKey = new Object();

        private Keymap keymap;

        KeymapWrapper(Keymap keymap) {
            this.keymap = keymap;
        }

        public KeyStroke[] keys() {
            KeyStroke[] sKeys = super.keys();
            KeyStroke[] keymapKeys = keymap.getBoundKeyStrokes();
            int sCount = (sKeys == null) ? 0 : sKeys.length;
            int keymapCount = (keymapKeys == null) ? 0 : keymapKeys.length;
            if (sCount == 0) {
                return keymapKeys;
            }
            if (keymapCount == 0) {
                return sKeys;
            }
            KeyStroke[] retValue = new KeyStroke[sCount + keymapCount];
            // There may be some duplication here...
            System.arraycopy(sKeys, 0, retValue, 0, sCount);
            System.arraycopy(keymapKeys, 0, retValue, sCount, keymapCount);
            return retValue;
        }

        public int size() {
            // There may be some duplication here...
            KeyStroke[] keymapStrokes = keymap.getBoundKeyStrokes();
            int keymapCount = (keymapStrokes == null) ? 0:
                               keymapStrokes.length;
            return super.size() + keymapCount;
        }

        public Object get(KeyStroke keyStroke) {
            Object retValue = keymap.getAction(keyStroke);
            if (retValue == null) {
                retValue = super.get(keyStroke);
                if (retValue == null &&
                    keyStroke.getKeyChar() != KeyEvent.CHAR_UNDEFINED &&
                    keymap.getDefaultAction() != null) {
                    // Implies this is a KeyTyped event, use the default
                    // action.
                    retValue = DefaultActionKey;
                }
            }
            return retValue;
        }
    }


    /**
     * Wraps a Keymap inside an ActionMap. This is used with
     * a KeymapWrapper. If <code>get</code> is passed in
     * <code>KeymapWrapper.DefaultActionKey</code>, the default action is
     * returned, otherwise if the key is an Action, it is returned.
     */
    static class KeymapActionMap extends ActionMap {
        private Keymap keymap;

        KeymapActionMap(Keymap keymap) {
            this.keymap = keymap;
        }

        public Object[] keys() {
            Object[] sKeys = super.keys();
            Object[] keymapKeys = keymap.getBoundActions();
            int sCount = (sKeys == null) ? 0 : sKeys.length;
            int keymapCount = (keymapKeys == null) ? 0 : keymapKeys.length;
            boolean hasDefault = (keymap.getDefaultAction() != null);
            if (hasDefault) {
                keymapCount++;
            }
            if (sCount == 0) {
                if (hasDefault) {
                    Object[] retValue = new Object[keymapCount];
                    if (keymapCount > 1) {
                        System.arraycopy(keymapKeys, 0, retValue, 0,
                                         keymapCount - 1);
                    }
                    retValue[keymapCount - 1] = KeymapWrapper.DefaultActionKey;
                    return retValue;
                }
                return keymapKeys;
            }
            if (keymapCount == 0) {
                return sKeys;
            }
            Object[] retValue = new Object[sCount + keymapCount];
            // There may be some duplication here...
            System.arraycopy(sKeys, 0, retValue, 0, sCount);
            if (hasDefault) {
                if (keymapCount > 1) {
                    System.arraycopy(keymapKeys, 0, retValue, sCount,
                                     keymapCount - 1);
                }
                retValue[sCount + keymapCount - 1] = KeymapWrapper.
                                                 DefaultActionKey;
            }
            else {
                System.arraycopy(keymapKeys, 0, retValue, sCount, keymapCount);
            }
            return retValue;
        }

        public int size() {
            // There may be some duplication here...
            Object[] actions = keymap.getBoundActions();
            int keymapCount = (actions == null) ? 0 : actions.length;
            if (keymap.getDefaultAction() != null) {
                keymapCount++;
            }
            return super.size() + keymapCount;
        }

        public Action get(Object key) {
            Action retValue = super.get(key);
            if (retValue == null) {
                // Try the Keymap.
                if (key == KeymapWrapper.DefaultActionKey) {
                    retValue = keymap.getDefaultAction();
                }
                else if (key instanceof Action) {
                    // This is a little iffy, technically an Action is
                    // a valid Key. We're assuming the Action came from
                    // the InputMap though.
                    retValue = (Action)key;
                }
            }
            return retValue;
        }
    }

    private static final Object FOCUSED_COMPONENT =
        new StringBuilder("JTextComponent_FocusedComponent");

    /**
     * The default keymap that will be shared by all
     * <code>JTextComponent</code> instances unless they
     * have had a different keymap set.
     */
    public static final String DEFAULT_KEYMAP = "default";

    /**
     * Event to use when firing a notification of change to caret
     * position.  This is mutable so that the event can be reused
     * since caret events can be fairly high in bandwidth.
     */
    static class MutableCaretEvent extends CaretEvent implements ChangeListener, FocusListener, MouseListener {

        MutableCaretEvent(JTextComponent c) {
            super(c);
        }

        final void fire() {
            JTextComponent c = (JTextComponent) getSource();
            if (c != null) {
                Caret caret = c.getCaret();
                dot = caret.getDot();
                mark = caret.getMark();
                c.fireCaretUpdate(this);
            }
        }

        public final String toString() {
            return "dot=" + dot + "," + "mark=" + mark;
        }

        // --- CaretEvent methods -----------------------

        public final int getDot() {
            return dot;
        }

        public final int getMark() {
            return mark;
        }

        // --- ChangeListener methods -------------------

        public final void stateChanged(ChangeEvent e) {
            if (! dragActive) {
                fire();
            }
        }

        // --- FocusListener methods -----------------------------------
        public void focusGained(FocusEvent fe) {
            AppContext.getAppContext().put(FOCUSED_COMPONENT,
                                           fe.getSource());
        }

        public void focusLost(FocusEvent fe) {
        }

        // --- MouseListener methods -----------------------------------

        /**
         * Requests focus on the associated
         * text component, and try to set the cursor position.
         *
         * @param e the mouse event
         * @see MouseListener#mousePressed
         */
        public final void mousePressed(MouseEvent e) {
            dragActive = true;
        }

        /**
         * Called when the mouse is released.
         *
         * @param e the mouse event
         * @see MouseListener#mouseReleased
         */
        public final void mouseReleased(MouseEvent e) {
            dragActive = false;
            fire();
        }

        public final void mouseClicked(MouseEvent e) {
        }

        public final void mouseEntered(MouseEvent e) {
        }

        public final void mouseExited(MouseEvent e) {
        }

        private boolean dragActive;
        private int dot;
        private int mark;
    }

    //
    // Process any input method events that the component itself
    // recognizes. The default on-the-spot handling for input method
    // composed(uncommitted) text is done here after all input
    // method listeners get called for stealing the events.
    //
    @SuppressWarnings("fallthrough")
    protected void processInputMethodEvent(InputMethodEvent e) {
        // let listeners handle the events
        super.processInputMethodEvent(e);

        if (!e.isConsumed()) {
            if (! isEditable()) {
                return;
            } else {
                switch (e.getID()) {
                case InputMethodEvent.INPUT_METHOD_TEXT_CHANGED:
                    replaceInputMethodText(e);

                    // fall through

                case InputMethodEvent.CARET_POSITION_CHANGED:
                    setInputMethodCaretPosition(e);
                    break;
                }
            }

            e.consume();
        }
    }

    //
    // Overrides this method to become an active input method client.
    //
    @BeanProperty(bound = false)
    public InputMethodRequests getInputMethodRequests() {
        if (inputMethodRequestsHandler == null) {
            inputMethodRequestsHandler = new InputMethodRequestsHandler();
            Document doc = getDocument();
            if (doc != null) {
                doc.addDocumentListener((DocumentListener)inputMethodRequestsHandler);
            }
        }

        return inputMethodRequestsHandler;
    }

    //
    // Overrides this method to watch the listener installed.
    //
    public void addInputMethodListener(InputMethodListener l) {
        super.addInputMethodListener(l);
        if (l != null) {
            needToSendKeyTypedEvent = false;
            checkedInputOverride = true;
        }
    }


    //
    // Default implementation of the InputMethodRequests interface.
    //
    class InputMethodRequestsHandler implements InputMethodRequests, DocumentListener {

        // --- InputMethodRequests methods ---

        public AttributedCharacterIterator cancelLatestCommittedText(
                                                Attribute[] attributes) {
            Document doc = getDocument();
            if ((doc != null) && (latestCommittedTextStart != null)
                && (!latestCommittedTextStart.equals(latestCommittedTextEnd))) {
                try {
                    int startIndex = latestCommittedTextStart.getOffset();
                    int endIndex = latestCommittedTextEnd.getOffset();
                    String latestCommittedText =
                        doc.getText(startIndex, endIndex - startIndex);
                    doc.remove(startIndex, endIndex - startIndex);
                    return new AttributedString(latestCommittedText).getIterator();
                } catch (BadLocationException ble) {}
            }
            return null;
        }

        public AttributedCharacterIterator getCommittedText(int beginIndex,
                                        int endIndex, Attribute[] attributes) {
            int composedStartIndex = 0;
            int composedEndIndex = 0;
            if (composedTextExists()) {
                composedStartIndex = composedTextStart.getOffset();
                composedEndIndex = composedTextEnd.getOffset();
            }

            String committed;
            try {
                if (beginIndex < composedStartIndex) {
                    if (endIndex <= composedStartIndex) {
                        committed = getText(beginIndex, endIndex - beginIndex);
                    } else {
                        int firstPartLength = composedStartIndex - beginIndex;
                        committed = getText(beginIndex, firstPartLength) +
                            getText(composedEndIndex, endIndex - beginIndex - firstPartLength);
                    }
                } else {
                    committed = getText(beginIndex + (composedEndIndex - composedStartIndex),
                                        endIndex - beginIndex);
                }
            } catch (BadLocationException ble) {
                throw new IllegalArgumentException("Invalid range");
            }
            return new AttributedString(committed).getIterator();
        }

        public int getCommittedTextLength() {
            Document doc = getDocument();
            int length = 0;
            if (doc != null) {
                length = doc.getLength();
                if (composedTextContent != null) {
                    if (composedTextEnd == null
                          || composedTextStart == null) {
                        /*
                         * fix for : 6355666
                         * this is the case when this method is invoked
                         * from DocumentListener. At this point
                         * composedTextEnd and composedTextStart are
                         * not defined yet.
                         */
                        length -= composedTextContent.length();
                    } else {
                        length -= composedTextEnd.getOffset() -
                            composedTextStart.getOffset();
                    }
                }
            }
            return length;
        }

        public int getInsertPositionOffset() {
            int composedStartIndex = 0;
            int composedEndIndex = 0;
            if (composedTextExists()) {
                composedStartIndex = composedTextStart.getOffset();
                composedEndIndex = composedTextEnd.getOffset();
            }
            int caretIndex = getCaretPosition();

            if (caretIndex < composedStartIndex) {
                return caretIndex;
            } else if (caretIndex < composedEndIndex) {
                return composedStartIndex;
            } else {
                return caretIndex - (composedEndIndex - composedStartIndex);
            }
        }

        public TextHitInfo getLocationOffset(int x, int y) {
            if (composedTextAttribute == null) {
                return null;
            } else {
                Point p = getLocationOnScreen();
                p.x = x - p.x;
                p.y = y - p.y;
                int pos = viewToModel(p);
                if ((pos >= composedTextStart.getOffset()) &&
                    (pos <= composedTextEnd.getOffset())) {
                    return TextHitInfo.leading(pos - composedTextStart.getOffset());
                } else {
                    return null;
                }
            }
        }

        public Rectangle getTextLocation(TextHitInfo offset) {
            Rectangle r;

            try {
                r = modelToView(getCaretPosition());
                if (r != null) {
                    Point p = getLocationOnScreen();
                    r.translate(p.x, p.y);
                }
            } catch (BadLocationException ble) {
                r = null;
            }

            if (r == null)
                r = new Rectangle();

            return r;
        }

        public AttributedCharacterIterator getSelectedText(
                                                Attribute[] attributes) {
            String selection = JTextComponent.this.getSelectedText();
            if (selection != null) {
                return new AttributedString(selection).getIterator();
            } else {
                return null;
            }
        }

        // --- DocumentListener methods ---

        public void changedUpdate(DocumentEvent e) {
            latestCommittedTextStart = latestCommittedTextEnd = null;
        }

        public void insertUpdate(DocumentEvent e) {
            latestCommittedTextStart = latestCommittedTextEnd = null;
        }

        public void removeUpdate(DocumentEvent e) {
            latestCommittedTextStart = latestCommittedTextEnd = null;
        }
    }

    //
    // Replaces the current input method (composed) text according to
    // the passed input method event. This method also inserts the
    // committed text into the document.
    //
    private void replaceInputMethodText(InputMethodEvent e) {
        int commitCount = e.getCommittedCharacterCount();
        AttributedCharacterIterator text = e.getText();
        int composedTextIndex;

        // old composed text deletion
        Document doc = getDocument();
        if (composedTextExists()) {
            try {
                doc.remove(composedTextStart.getOffset(),
                           composedTextEnd.getOffset() -
                           composedTextStart.getOffset());
            } catch (BadLocationException ble) {}
            composedTextStart = composedTextEnd = null;
            composedTextAttribute = null;
            composedTextContent = null;
        }

        if (text != null) {
            text.first();
            int committedTextStartIndex = 0;
            int committedTextEndIndex = 0;

            // committed text insertion
            if (commitCount > 0) {
                // Remember latest committed text start index
                committedTextStartIndex = caret.getDot();

                // Need to generate KeyTyped events for the committed text for components
                // that are not aware they are active input method clients.
                if (shouldSynthensizeKeyEvents()) {
                    for (char c = text.current(); commitCount > 0;
                         c = text.next(), commitCount--) {
                        KeyEvent ke = new KeyEvent(this, KeyEvent.KEY_TYPED,
                                                   EventQueue.getMostRecentEventTime(),
                                                   0, KeyEvent.VK_UNDEFINED, c);
                        processKeyEvent(ke);
                    }
                } else {
                    StringBuilder strBuf = new StringBuilder();
                    for (char c = text.current(); commitCount > 0;
                         c = text.next(), commitCount--) {
                        strBuf.append(c);
                    }

                    // map it to an ActionEvent
                    mapCommittedTextToAction(strBuf.toString());
                }

                // Remember latest committed text end index
                committedTextEndIndex = caret.getDot();
            }

            // new composed text insertion
            composedTextIndex = text.getIndex();
            if (composedTextIndex < text.getEndIndex()) {
                createComposedTextAttribute(composedTextIndex, text);
                try {
                    replaceSelection(null);
                    doc.insertString(caret.getDot(), composedTextContent,
                                        composedTextAttribute);
                    composedTextStart = doc.createPosition(caret.getDot() -
                                                composedTextContent.length());
                    composedTextEnd = doc.createPosition(caret.getDot());
                } catch (BadLocationException ble) {
                    composedTextStart = composedTextEnd = null;
                    composedTextAttribute = null;
                    composedTextContent = null;
                }
            }

            // Save the latest committed text information
            if (committedTextStartIndex != committedTextEndIndex) {
                try {
                    latestCommittedTextStart = doc.
                        createPosition(committedTextStartIndex);
                    latestCommittedTextEnd = doc.
                        createPosition(committedTextEndIndex);
                } catch (BadLocationException ble) {
                    latestCommittedTextStart =
                        latestCommittedTextEnd = null;
                }
            } else {
                latestCommittedTextStart =
                    latestCommittedTextEnd = null;
            }
        }
    }

    private void createComposedTextAttribute(int composedIndex,
                                        AttributedCharacterIterator text) {
        Document doc = getDocument();
        StringBuilder strBuf = new StringBuilder();

        // create attributed string with no attributes
        for (char c = text.setIndex(composedIndex);
             c != CharacterIterator.DONE; c = text.next()) {
            strBuf.append(c);
        }

        composedTextContent = strBuf.toString();
        composedTextAttribute = new SimpleAttributeSet();
        composedTextAttribute.addAttribute(StyleConstants.ComposedTextAttribute,
                new AttributedString(text, composedIndex, text.getEndIndex()));
    }

    /**
     * Saves composed text around the specified position.
     *
     * The composed text (if any) around the specified position is saved
     * in a backing store and removed from the document.
     *
     * @param pos  document position to identify the composed text location
     * @return  {@code true} if the composed text exists and is saved,
     *          {@code false} otherwise
     * @see #restoreComposedText
     * @since 1.7
     */
    protected boolean saveComposedText(int pos) {
        if (composedTextExists()) {
            int start = composedTextStart.getOffset();
            int len = composedTextEnd.getOffset() -
                composedTextStart.getOffset();
            if (pos >= start && pos <= start + len) {
                try {
                    getDocument().remove(start, len);
                    return true;
                } catch (BadLocationException ble) {}
            }
        }
        return false;
    }

    /**
     * Restores composed text previously saved by {@code saveComposedText}.
     *
     * The saved composed text is inserted back into the document. This method
     * should be invoked only if {@code saveComposedText} returns {@code true}.
     *
     * @see #saveComposedText
     * @since 1.7
     */
    protected void restoreComposedText() {
        Document doc = getDocument();
        try {
            doc.insertString(caret.getDot(),
                             composedTextContent,
                             composedTextAttribute);
            composedTextStart = doc.createPosition(caret.getDot() -
                                composedTextContent.length());
            composedTextEnd = doc.createPosition(caret.getDot());
        } catch (BadLocationException ble) {}
    }

    //
    // Map committed text to an ActionEvent. If the committed text length is 1,
    // treat it as a KeyStroke, otherwise or there is no KeyStroke defined,
    // treat it just as a default action.
    //
    private void mapCommittedTextToAction(String committedText) {
        Keymap binding = getKeymap();
        if (binding != null) {
            Action a = null;
            if (committedText.length() == 1) {
                KeyStroke k = KeyStroke.getKeyStroke(committedText.charAt(0));
                a = binding.getAction(k);
            }

            if (a == null) {
                a = binding.getDefaultAction();
            }

            if (a != null) {
                ActionEvent ae =
                    new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                                    committedText,
                                    EventQueue.getMostRecentEventTime(),
                                    getCurrentEventModifiers());
                a.actionPerformed(ae);
            }
        }
    }

    //
    // Sets the caret position according to the passed input method
    // event. Also, sets/resets composed text caret appropriately.
    //
    private void setInputMethodCaretPosition(InputMethodEvent e) {
        int dot;

        if (composedTextExists()) {
            dot = composedTextStart.getOffset();
            if (!(caret instanceof ComposedTextCaret)) {
                if (composedTextCaret == null) {
                    composedTextCaret = new ComposedTextCaret();
                }
                originalCaret = caret;
                // Sets composed text caret
                exchangeCaret(originalCaret, composedTextCaret);
            }

            TextHitInfo caretPos = e.getCaret();
            if (caretPos != null) {
                int index = caretPos.getInsertionIndex();
                dot += index;
                if (index == 0) {
                    // Scroll the component if needed so that the composed text
                    // becomes visible.
                    try {
                        Rectangle d = modelToView(dot);
                        Rectangle end = modelToView(composedTextEnd.getOffset());
                        Rectangle b = getBounds();
                        d.x += Math.min(end.x - d.x, b.width);
                        scrollRectToVisible(d);
                    } catch (BadLocationException ble) {}
                }
            }
            caret.setDot(dot);
        } else if (caret instanceof ComposedTextCaret) {
            dot = caret.getDot();
            // Restores original caret
            exchangeCaret(caret, originalCaret);
            caret.setDot(dot);
        }
    }

    private void exchangeCaret(Caret oldCaret, Caret newCaret) {
        int blinkRate = oldCaret.getBlinkRate();
        setCaret(newCaret);
        caret.setBlinkRate(blinkRate);
        caret.setVisible(hasFocus());
    }

    /**
     * Returns true if KeyEvents should be synthesized from an InputEvent.
     */
    private boolean shouldSynthensizeKeyEvents() {
        if (!checkedInputOverride) {
            // Checks whether the client code overrides processInputMethodEvent.
            // If it is overridden, need not to generate KeyTyped events for committed text.
            // If it's not, behave as an passive input method client.
            needToSendKeyTypedEvent = !METHOD_OVERRIDDEN.get(getClass());
            checkedInputOverride = true;
        }
        return needToSendKeyTypedEvent;
    }

    //
    // Checks whether a composed text in this text component
    //
    boolean composedTextExists() {
        return (composedTextStart != null);
    }

    //
    // Caret implementation for editing the composed text.
    //
    class ComposedTextCaret extends DefaultCaret implements Serializable {
        Color bg;

        //
        // Get the background color of the component
        //
        public void install(JTextComponent c) {
            super.install(c);

            Document doc = c.getDocument();
            if (doc instanceof StyledDocument) {
                StyledDocument sDoc = (StyledDocument)doc;
                Element elem = sDoc.getCharacterElement(c.composedTextStart.getOffset());
                AttributeSet attr = elem.getAttributes();
                bg = sDoc.getBackground(attr);
            }

            if (bg == null) {
                bg = c.getBackground();
            }
        }

        //
        // Draw caret in XOR mode.
        //
        public void paint(Graphics g) {
            if(isVisible()) {
                try {
                    Rectangle r = component.modelToView(getDot());
                    g.setXORMode(bg);
                    g.drawLine(r.x, r.y, r.x, r.y + r.height - 1);
                    g.setPaintMode();
                } catch (BadLocationException e) {
                    // can't render I guess
                    //System.err.println("Can't render cursor");
                }
            }
        }

        //
        // If some area other than the composed text is clicked by mouse,
        // issue endComposition() to force commit the composed text.
        //
        protected void positionCaret(MouseEvent me) {
            JTextComponent host = component;
            Point pt = new Point(me.getX(), me.getY());
            int offset = host.viewToModel(pt);
            int composedStartIndex = host.composedTextStart.getOffset();
            if ((offset < composedStartIndex) ||
                (offset > composedTextEnd.getOffset())) {
                try {
                    // Issue endComposition
                    Position newPos = host.getDocument().createPosition(offset);
                    host.getInputContext().endComposition();

                    // Post a caret positioning runnable to assure that the positioning
                    // occurs *after* committing the composed text.
                    EventQueue.invokeLater(new DoSetCaretPosition(host, newPos));
                } catch (BadLocationException ble) {
                    System.err.println(ble);
                }
            } else {
                // Normal processing
                super.positionCaret(me);
            }
        }
    }

    //
    // Runnable class for invokeLater() to set caret position later.
    //
    private class DoSetCaretPosition implements Runnable {
        JTextComponent host;
        Position newPos;

        DoSetCaretPosition(JTextComponent host, Position newPos) {
            this.host = host;
            this.newPos = newPos;
        }

        public void run() {
            host.setCaretPosition(newPos.getOffset());
        }
    }
}
