/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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



import java.io.*;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.text.*;
import java.util.Locale;
import javax.accessibility.*;
import javax.swing.event.*;
import javax.swing.text.*;


/** A class to monitor the progress of some operation. If it looks
 * like the operation will take a while, a progress dialog will be popped up.
 * When the ProgressMonitor is created it is given a numeric range and a
 * descriptive string. As the operation progresses, call the setProgress method
 * to indicate how far along the [min,max] range the operation is.
 * Initially, there is no ProgressDialog. After the first millisToDecideToPopup
 * milliseconds (default 500) the progress monitor will predict how long
 * the operation will take.  If it is longer than millisToPopup (default 2000,
 * 2 seconds) a ProgressDialog will be popped up.
 * <p>
 * From time to time, when the Dialog box is visible, the progress bar will
 * be updated when setProgress is called.  setProgress won't always update
 * the progress bar, it will only be done if the amount of progress is
 * visibly significant.
 *
 * <p>
 *
 * For further documentation and examples see
 * <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/components/progress.html">How to Monitor Progress</a>,
 * a section in <em>The Java Tutorial.</em>
 *
 * @see ProgressMonitorInputStream
 * @author James Gosling
 * @author Lynn Monsanto (accessibility)
 * @since 1.2
 */
public class ProgressMonitor implements Accessible
{
    private ProgressMonitor root;
    private JDialog         dialog;
    private JOptionPane     pane;
    private JProgressBar    myBar;
    private JLabel          noteLabel;
    private Component       parentComponent;
    private String          note;
    private Object[]        cancelOption = null;
    private Object          message;
    private long            T0;
    private int             millisToDecideToPopup = 500;
    private int             millisToPopup = 2000;
    private int             min;
    private int             max;


    /**
     * Constructs a graphic object that shows progress, typically by filling
     * in a rectangular bar as the process nears completion.
     *
     * @param parentComponent the parent component for the dialog box
     * @param message a descriptive message that will be shown
     *        to the user to indicate what operation is being monitored.
     *        This does not change as the operation progresses.
     *        See the message parameters to methods in
     *        {@link JOptionPane#message}
     *        for the range of values.
     * @param note a short note describing the state of the
     *        operation.  As the operation progresses, you can call
     *        setNote to change the note displayed.  This is used,
     *        for example, in operations that iterate through a
     *        list of files to show the name of the file being processes.
     *        If note is initially null, there will be no note line
     *        in the dialog box and setNote will be ineffective
     * @param min the lower bound of the range
     * @param max the upper bound of the range
     * @see JDialog
     * @see JOptionPane
     */
    public ProgressMonitor(Component parentComponent,
                           Object message,
                           String note,
                           int min,
                           int max) {
        this(parentComponent, message, note, min, max, null);
    }


    private ProgressMonitor(Component parentComponent,
                            Object message,
                            String note,
                            int min,
                            int max,
                            ProgressMonitor group) {
        this.min = min;
        this.max = max;
        this.parentComponent = parentComponent;

        cancelOption = new Object[1];
        cancelOption[0] = UIManager.getString("OptionPane.cancelButtonText");

        this.message = message;
        this.note = note;
        if (group != null) {
            root = (group.root != null) ? group.root : group;
            T0 = root.T0;
            dialog = root.dialog;
        }
        else {
            T0 = System.currentTimeMillis();
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private class ProgressOptionPane extends JOptionPane
    {
        ProgressOptionPane(Object messageList) {
            super(messageList,
                  JOptionPane.INFORMATION_MESSAGE,
                  JOptionPane.DEFAULT_OPTION,
                  null,
                  ProgressMonitor.this.cancelOption,
                  null);
        }


        public int getMaxCharactersPerLineCount() {
            return 60;
        }


        // Equivalent to JOptionPane.createDialog,
        // but create a modeless dialog.
        // This is necessary because the Solaris implementation doesn't
        // support Dialog.setModal yet.
        public JDialog createDialog(Component parentComponent, String title) {
            final JDialog dialog;

            Window window = JOptionPane.getWindowForComponent(parentComponent);
            if (window instanceof Frame) {
                dialog = new JDialog((Frame)window, title, false);
            } else {
                dialog = new JDialog((Dialog)window, title, false);
            }
            if (window instanceof SwingUtilities.SharedOwnerFrame) {
                WindowListener ownerShutdownListener =
                        SwingUtilities.getSharedOwnerFrameShutdownListener();
                dialog.addWindowListener(ownerShutdownListener);
            }
            Container contentPane = dialog.getContentPane();

            contentPane.setLayout(new BorderLayout());
            contentPane.add(this, BorderLayout.CENTER);
            dialog.pack();
            dialog.setLocationRelativeTo(parentComponent);
            dialog.addWindowListener(new WindowAdapter() {
                boolean gotFocus = false;

                public void windowClosing(WindowEvent we) {
                    setValue(cancelOption[0]);
                }

                public void windowActivated(WindowEvent we) {
                    // Once window gets focus, set initial focus
                    if (!gotFocus) {
                        selectInitialValue();
                        gotFocus = true;
                    }
                }
            });

            addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent event) {
                    if(dialog.isVisible() &&
                       event.getSource() == ProgressOptionPane.this &&
                       (event.getPropertyName().equals(VALUE_PROPERTY) ||
                        event.getPropertyName().equals(INPUT_VALUE_PROPERTY))){
                        dialog.setVisible(false);
                        dialog.dispose();
                    }
                }
            });

            return dialog;
        }

        /////////////////
        // Accessibility support for ProgressOptionPane
        ////////////////

        /**
         * Gets the AccessibleContext for the ProgressOptionPane
         *
         * @return the AccessibleContext for the ProgressOptionPane
         * @since 1.5
         */
        public AccessibleContext getAccessibleContext() {
            return ProgressMonitor.this.getAccessibleContext();
        }

        /*
         * Returns the AccessibleJOptionPane
         */
        private AccessibleContext getAccessibleJOptionPane() {
            return super.getAccessibleContext();
        }
    }


    /**
     * Indicate the progress of the operation being monitored.
     * If the specified value is &gt;= the maximum, the progress
     * monitor is closed.
     * @param nv an int specifying the current value, between the
     *        maximum and minimum specified for this component
     * @see #setMinimum
     * @see #setMaximum
     * @see #close
     */
    @SuppressWarnings("deprecation")
    public void setProgress(int nv) {
        if (nv >= max) {
            close();
        }
        else {
            if (myBar != null) {
                myBar.setValue(nv);
            }
            else {
                long T = System.currentTimeMillis();
                long dT = (int)(T-T0);
                if (dT >= millisToDecideToPopup) {
                    int predictedCompletionTime;
                    if (nv > min) {
                        predictedCompletionTime = (int)(dT *
                                                        (max - min) /
                                                        (nv - min));
                    }
                    else {
                        predictedCompletionTime = millisToPopup;
                    }
                    if (predictedCompletionTime >= millisToPopup) {
                        myBar = new JProgressBar();
                        myBar.setMinimum(min);
                        myBar.setMaximum(max);
                        myBar.setValue(nv);
                        if (note != null) noteLabel = new JLabel(note);
                        pane = new ProgressOptionPane(new Object[] {message,
                                                                    noteLabel,
                                                                    myBar});
                        dialog = pane.createDialog(parentComponent,
                            UIManager.getString(
                                "ProgressMonitor.progressText"));
                        dialog.show();
                    }
                }
            }
        }
    }


    /**
     * Indicate that the operation is complete.  This happens automatically
     * when the value set by setProgress is &gt;= max, but it may be called
     * earlier if the operation ends early.
     */
    public void close() {
        if (dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
            dialog = null;
            pane = null;
            myBar = null;
        }
    }


    /**
     * Returns the minimum value -- the lower end of the progress value.
     *
     * @return an int representing the minimum value
     * @see #setMinimum
     */
    public int getMinimum() {
        return min;
    }


    /**
     * Specifies the minimum value.
     *
     * @param m  an int specifying the minimum value
     * @see #getMinimum
     */
    public void setMinimum(int m) {
        if (myBar != null) {
            myBar.setMinimum(m);
        }
        min = m;
    }


    /**
     * Returns the maximum value -- the higher end of the progress value.
     *
     * @return an int representing the maximum value
     * @see #setMaximum
     */
    public int getMaximum() {
        return max;
    }


    /**
     * Specifies the maximum value.
     *
     * @param m  an int specifying the maximum value
     * @see #getMaximum
     */
    public void setMaximum(int m) {
        if (myBar != null) {
            myBar.setMaximum(m);
        }
        max = m;
    }


    /**
     * Returns true if the user hits the Cancel button or closes
     * the progress dialog.
     *
     * @return true if the user hits the Cancel button or closes
     * the progress dialog
     */
    public boolean isCanceled() {
        if (pane == null) {
            return false;
        }

        Object v = pane.getValue();
        if (v == null) {
            return false;
        }

        return (((cancelOption.length == 1) && v.equals(cancelOption[0])) ||
                v.equals(Integer.valueOf(JOptionPane.CLOSED_OPTION)));
    }


    /**
     * Specifies the amount of time to wait before deciding whether or
     * not to popup a progress monitor.
     *
     * @param millisToDecideToPopup  an int specifying the time to wait,
     *        in milliseconds
     * @see #getMillisToDecideToPopup
     */
    public void setMillisToDecideToPopup(int millisToDecideToPopup) {
        this.millisToDecideToPopup = millisToDecideToPopup;
    }


    /**
     * Returns the amount of time this object waits before deciding whether
     * or not to popup a progress monitor.
     *
     * @return the amount of time in milliseconds this object waits before
     *         deciding whether or not to popup a progress monitor
     * @see #setMillisToDecideToPopup
     */
    public int getMillisToDecideToPopup() {
        return millisToDecideToPopup;
    }


    /**
     * Specifies the amount of time it will take for the popup to appear.
     * (If the predicted time remaining is less than this time, the popup
     * won't be displayed.)
     *
     * @param millisToPopup  an int specifying the time in milliseconds
     * @see #getMillisToPopup
     */
    public void setMillisToPopup(int millisToPopup) {
        this.millisToPopup = millisToPopup;
    }


    /**
     * Returns the amount of time it will take for the popup to appear.
     *
     * @return the amont of time in milliseconds it will take for the
     *         popup to appear
     * @see #setMillisToPopup
     */
    public int getMillisToPopup() {
        return millisToPopup;
    }


    /**
     * Specifies the additional note that is displayed along with the
     * progress message. Used, for example, to show which file the
     * is currently being copied during a multiple-file copy.
     *
     * @param note  a String specifying the note to display
     * @see #getNote
     */
    public void setNote(String note) {
        this.note = note;
        if (noteLabel != null) {
            noteLabel.setText(note);
        }
    }


    /**
     * Specifies the additional note that is displayed along with the
     * progress message.
     *
     * @return a String specifying the note to display
     * @see #setNote
     */
    public String getNote() {
        return note;
    }

    /////////////////
    // Accessibility support
    ////////////////

    /**
     * The <code>AccessibleContext</code> for the <code>ProgressMonitor</code>
     * @since 1.5
     */
    protected AccessibleContext accessibleContext = null;

    private AccessibleContext accessibleJOptionPane = null;

    /**
     * Gets the <code>AccessibleContext</code> for the
     * <code>ProgressMonitor</code>
     *
     * @return the <code>AccessibleContext</code> for the
     * <code>ProgressMonitor</code>
     * @since 1.5
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleProgressMonitor();
        }
        if (pane != null && accessibleJOptionPane == null) {
            // Notify the AccessibleProgressMonitor that the
            // ProgressOptionPane was created. It is necessary
            // to poll for ProgressOptionPane creation because
            // the ProgressMonitor does not have a Component
            // to add a listener to until the ProgressOptionPane
            // is created.
            if (accessibleContext instanceof AccessibleProgressMonitor) {
                ((AccessibleProgressMonitor)accessibleContext).optionPaneCreated();
            }
        }
        return accessibleContext;
    }

    /**
     * <code>AccessibleProgressMonitor</code> implements accessibility
     * support for the <code>ProgressMonitor</code> class.
     * @since 1.5
     */
    protected class AccessibleProgressMonitor extends AccessibleContext
        implements AccessibleText, ChangeListener, PropertyChangeListener {

        /*
         * The accessibility hierarchy for ProgressMonitor is a flattened
         * version of the ProgressOptionPane component hierarchy.
         *
         * The ProgressOptionPane component hierarchy is:
         *   JDialog
         *     ProgressOptionPane
         *       JPanel
         *         JPanel
         *           JLabel
         *           JLabel
         *           JProgressBar
         *
         * The AccessibleProgessMonitor accessibility hierarchy is:
         *   AccessibleJDialog
         *     AccessibleProgressMonitor
         *       AccessibleJLabel
         *       AccessibleJLabel
         *       AccessibleJProgressBar
         *
         * The abstraction presented to assitive technologies by
         * the AccessibleProgressMonitor is that a dialog contains a
         * progress monitor with three children: a message, a note
         * label and a progress bar.
         */

        private Object oldModelValue;

        /**
         * AccessibleProgressMonitor constructor
         */
        protected AccessibleProgressMonitor() {
        }

        /*
         * Initializes the AccessibleContext now that the ProgressOptionPane
         * has been created. Because the ProgressMonitor is not a Component
         * implementing the Accessible interface, an AccessibleContext
         * must be synthesized from the ProgressOptionPane and its children.
         *
         * For other AWT and Swing classes, the inner class that implements
         * accessibility for the class extends the inner class that implements
         * implements accessibility for the super class. AccessibleProgressMonitor
         * cannot extend AccessibleJOptionPane and must therefore delegate calls
         * to the AccessibleJOptionPane.
         */
        private void optionPaneCreated() {
            accessibleJOptionPane =
                ((ProgressOptionPane)pane).getAccessibleJOptionPane();

            // add a listener for progress bar ChangeEvents
            if (myBar != null) {
                myBar.addChangeListener(this);
            }

            // add a listener for note label PropertyChangeEvents
            if (noteLabel != null) {
                noteLabel.addPropertyChangeListener(this);
            }
        }

        /**
         * Invoked when the target of the listener has changed its state.
         *
         * @param e  a <code>ChangeEvent</code> object. Must not be null.
         * @throws NullPointerException if the parameter is null.
         */
        public void stateChanged(ChangeEvent e) {
            if (e == null) {
                return;
            }
            if (myBar != null) {
                // the progress bar value changed
                Object newModelValue = myBar.getValue();
                firePropertyChange(ACCESSIBLE_VALUE_PROPERTY,
                                   oldModelValue,
                                   newModelValue);
                oldModelValue = newModelValue;
            }
        }

        /**
         * This method gets called when a bound property is changed.
         *
         * @param e A <code>PropertyChangeEvent</code> object describing
         * the event source and the property that has changed. Must not be null.
         * @throws NullPointerException if the parameter is null.
         */
        public void propertyChange(PropertyChangeEvent e) {
            if (e.getSource() == noteLabel && e.getPropertyName() == "text") {
                // the note label text changed
                firePropertyChange(ACCESSIBLE_TEXT_PROPERTY, null, 0);
            }
        }

        /* ===== Begin AccessileContext ===== */

        /**
         * Gets the accessibleName property of this object.  The accessibleName
         * property of an object is a localized String that designates the purpose
         * of the object.  For example, the accessibleName property of a label
         * or button might be the text of the label or button itself.  In the
         * case of an object that doesn't display its name, the accessibleName
         * should still be set.  For example, in the case of a text field used
         * to enter the name of a city, the accessibleName for the en_US locale
         * could be 'city.'
         *
         * @return the localized name of the object; null if this
         * object does not have a name
         *
         * @see #setAccessibleName
         */
        public String getAccessibleName() {
            if (accessibleName != null) { // defined in AccessibleContext
                return accessibleName;
            } else if (accessibleJOptionPane != null) {
                // delegate to the AccessibleJOptionPane
                return accessibleJOptionPane.getAccessibleName();
            }
            return null;
        }

        /**
         * Gets the accessibleDescription property of this object.  The
         * accessibleDescription property of this object is a short localized
         * phrase describing the purpose of the object.  For example, in the
         * case of a 'Cancel' button, the accessibleDescription could be
         * 'Ignore changes and close dialog box.'
         *
         * @return the localized description of the object; null if
         * this object does not have a description
         *
         * @see #setAccessibleDescription
         */
        public String getAccessibleDescription() {
            if (accessibleDescription != null) { // defined in AccessibleContext
                return accessibleDescription;
            } else if (accessibleJOptionPane != null) {
                // delegate to the AccessibleJOptionPane
                return accessibleJOptionPane.getAccessibleDescription();
            }
            return null;
        }

        /**
         * Gets the role of this object.  The role of the object is the generic
         * purpose or use of the class of this object.  For example, the role
         * of a push button is AccessibleRole.PUSH_BUTTON.  The roles in
         * AccessibleRole are provided so component developers can pick from
         * a set of predefined roles.  This enables assistive technologies to
         * provide a consistent interface to various tweaked subclasses of
         * components (e.g., use AccessibleRole.PUSH_BUTTON for all components
         * that act like a push button) as well as distinguish between subclasses
         * that behave differently (e.g., AccessibleRole.CHECK_BOX for check boxes
         * and AccessibleRole.RADIO_BUTTON for radio buttons).
         * <p>Note that the AccessibleRole class is also extensible, so
         * custom component developers can define their own AccessibleRole's
         * if the set of predefined roles is inadequate.
         *
         * @return an instance of AccessibleRole describing the role of the object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PROGRESS_MONITOR;
        }

        /**
         * Gets the state set of this object.  The AccessibleStateSet of an object
         * is composed of a set of unique AccessibleStates.  A change in the
         * AccessibleStateSet of an object will cause a PropertyChangeEvent to
         * be fired for the ACCESSIBLE_STATE_PROPERTY property.
         *
         * @return an instance of AccessibleStateSet containing the
         * current state set of the object
         * @see AccessibleStateSet
         * @see AccessibleState
         * @see #addPropertyChangeListener
         */
        public AccessibleStateSet getAccessibleStateSet() {
            if (accessibleJOptionPane != null) {
                // delegate to the AccessibleJOptionPane
                return accessibleJOptionPane.getAccessibleStateSet();
            }
            return null;
        }

        /**
         * Gets the Accessible parent of this object.
         *
         * @return the Accessible parent of this object; null if this
         * object does not have an Accessible parent
         */
        public Accessible getAccessibleParent() {
            return dialog;
        }

        /*
         * Returns the parent AccessibleContext
         */
        private AccessibleContext getParentAccessibleContext() {
            if (dialog != null) {
                return dialog.getAccessibleContext();
            }
            return null;
        }

        /**
         * Gets the 0-based index of this object in its accessible parent.
         *
         * @return the 0-based index of this object in its parent; -1 if this
         * object does not have an accessible parent.
         *
         * @see #getAccessibleParent
         * @see #getAccessibleChildrenCount
         * @see #getAccessibleChild
         */
        public int getAccessibleIndexInParent() {
            if (accessibleJOptionPane != null) {
                // delegate to the AccessibleJOptionPane
                return accessibleJOptionPane.getAccessibleIndexInParent();
            }
            return -1;
        }

        /**
         * Returns the number of accessible children of the object.
         *
         * @return the number of accessible children of the object.
         */
        public int getAccessibleChildrenCount() {
            // return the number of children in the JPanel containing
            // the message, note label and progress bar
            AccessibleContext ac = getPanelAccessibleContext();
            if (ac != null) {
                return ac.getAccessibleChildrenCount();
            }
            return 0;
        }

        /**
         * Returns the specified Accessible child of the object.  The Accessible
         * children of an Accessible object are zero-based, so the first child
         * of an Accessible child is at index 0, the second child is at index 1,
         * and so on.
         *
         * @param i zero-based index of child
         * @return the Accessible child of the object
         * @see #getAccessibleChildrenCount
         */
        public Accessible getAccessibleChild(int i) {
            // return a child in the JPanel containing the message, note label
            // and progress bar
            AccessibleContext ac = getPanelAccessibleContext();
            if (ac != null) {
                return ac.getAccessibleChild(i);
            }
            return null;
        }

        /*
         * Returns the AccessibleContext for the JPanel containing the
         * message, note label and progress bar
         */
        private AccessibleContext getPanelAccessibleContext() {
            if (myBar != null) {
                Component c = myBar.getParent();
                if (c instanceof Accessible) {
                    return c.getAccessibleContext();
                }
            }
            return null;
        }

        /**
         * Gets the locale of the component. If the component does not have a
         * locale, then the locale of its parent is returned.
         *
         * @return this component's locale.  If this component does not have
         * a locale, the locale of its parent is returned.
         *
         * @exception IllegalComponentStateException
         * If the Component does not have its own locale and has not yet been
         * added to a containment hierarchy such that the locale can be
         * determined from the containing parent.
         */
        public Locale getLocale() throws IllegalComponentStateException {
            if (accessibleJOptionPane != null) {
                // delegate to the AccessibleJOptionPane
                return accessibleJOptionPane.getLocale();
            }
            return null;
        }

        /* ===== end AccessibleContext ===== */

        /**
         * Gets the AccessibleComponent associated with this object that has a
         * graphical representation.
         *
         * @return AccessibleComponent if supported by object; else return null
         * @see AccessibleComponent
         */
        public AccessibleComponent getAccessibleComponent() {
            if (accessibleJOptionPane != null) {
                // delegate to the AccessibleJOptionPane
                return accessibleJOptionPane.getAccessibleComponent();
            }
            return null;
        }

        /**
         * Gets the AccessibleValue associated with this object that supports a
         * Numerical value.
         *
         * @return AccessibleValue if supported by object; else return null
         * @see AccessibleValue
         */
        public AccessibleValue getAccessibleValue() {
            if (myBar != null) {
                // delegate to the AccessibleJProgressBar
                return myBar.getAccessibleContext().getAccessibleValue();
            }
            return null;
        }

        /**
         * Gets the AccessibleText associated with this object presenting
         * text on the display.
         *
         * @return AccessibleText if supported by object; else return null
         * @see AccessibleText
         */
        public AccessibleText getAccessibleText() {
            if (getNoteLabelAccessibleText() != null) {
                return this;
            }
            return null;
        }

        /*
         * Returns the note label AccessibleText
         */
        private AccessibleText getNoteLabelAccessibleText() {
            if (noteLabel != null) {
                // AccessibleJLabel implements AccessibleText if the
                // JLabel contains HTML text
                return noteLabel.getAccessibleContext().getAccessibleText();
            }
            return null;
        }

        /* ===== Begin AccessibleText impl ===== */

        /**
         * Given a point in local coordinates, return the zero-based index
         * of the character under that Point.  If the point is invalid,
         * this method returns -1.
         *
         * @param p the Point in local coordinates
         * @return the zero-based index of the character under Point p; if
         * Point is invalid return -1.
         */
        public int getIndexAtPoint(Point p) {
            AccessibleText at = getNoteLabelAccessibleText();
            if (at != null && sameWindowAncestor(pane, noteLabel)) {
                // convert point from the option pane bounds
                // to the note label bounds.
                Point noteLabelPoint = SwingUtilities.convertPoint(pane,
                                                                   p,
                                                                   noteLabel);
                if (noteLabelPoint != null) {
                    return at.getIndexAtPoint(noteLabelPoint);
                }
            }
            return -1;
        }

        /**
         * Determines the bounding box of the character at the given
         * index into the string.  The bounds are returned in local
         * coordinates.  If the index is invalid an empty rectangle is returned.
         *
         * @param i the index into the String
         * @return the screen coordinates of the character's bounding box,
         * if index is invalid return an empty rectangle.
         */
        public Rectangle getCharacterBounds(int i) {
            AccessibleText at = getNoteLabelAccessibleText();
            if (at != null && sameWindowAncestor(pane, noteLabel)) {
                // return rectangle in the option pane bounds
                Rectangle noteLabelRect = at.getCharacterBounds(i);
                if (noteLabelRect != null) {
                    return SwingUtilities.convertRectangle(noteLabel,
                                                           noteLabelRect,
                                                           pane);
                }
            }
            return null;
        }

        /*
         * Returns whether source and destination components have the
         * same window ancestor
         */
        private boolean sameWindowAncestor(Component src, Component dest) {
            if (src == null || dest == null) {
                return false;
            }
            return SwingUtilities.getWindowAncestor(src) ==
                SwingUtilities.getWindowAncestor(dest);
        }

        /**
         * Returns the number of characters (valid indicies)
         *
         * @return the number of characters
         */
        public int getCharCount() {
            AccessibleText at = getNoteLabelAccessibleText();
            if (at != null) {   // JLabel contains HTML text
                return at.getCharCount();
            }
            return -1;
        }

        /**
         * Returns the zero-based offset of the caret.
         *
         * Note: That to the right of the caret will have the same index
         * value as the offset (the caret is between two characters).
         * @return the zero-based offset of the caret.
         */
        public int getCaretPosition() {
            AccessibleText at = getNoteLabelAccessibleText();
            if (at != null) {   // JLabel contains HTML text
                return at.getCaretPosition();
            }
            return -1;
        }

        /**
         * Returns the String at a given index.
         *
         * @param part the CHARACTER, WORD, or SENTENCE to retrieve
         * @param index an index within the text
         * @return the letter, word, or sentence
         */
        public String getAtIndex(int part, int index) {
            AccessibleText at = getNoteLabelAccessibleText();
            if (at != null) {   // JLabel contains HTML text
                return at.getAtIndex(part, index);
            }
            return null;
        }

        /**
         * Returns the String after a given index.
         *
         * @param part the CHARACTER, WORD, or SENTENCE to retrieve
         * @param index an index within the text
         * @return the letter, word, or sentence
         */
        public String getAfterIndex(int part, int index) {
            AccessibleText at = getNoteLabelAccessibleText();
            if (at != null) {   // JLabel contains HTML text
                return at.getAfterIndex(part, index);
            }
            return null;
        }

        /**
         * Returns the String before a given index.
         *
         * @param part the CHARACTER, WORD, or SENTENCE to retrieve
         * @param index an index within the text
         * @return the letter, word, or sentence
         */
        public String getBeforeIndex(int part, int index) {
            AccessibleText at = getNoteLabelAccessibleText();
            if (at != null) {   // JLabel contains HTML text
                return at.getBeforeIndex(part, index);
            }
            return null;
        }

        /**
         * Returns the AttributeSet for a given character at a given index
         *
         * @param i the zero-based index into the text
         * @return the AttributeSet of the character
         */
        public AttributeSet getCharacterAttribute(int i) {
            AccessibleText at = getNoteLabelAccessibleText();
            if (at != null) {   // JLabel contains HTML text
                return at.getCharacterAttribute(i);
            }
            return null;
        }

        /**
         * Returns the start offset within the selected text.
         * If there is no selection, but there is
         * a caret, the start and end offsets will be the same.
         *
         * @return the index into the text of the start of the selection
         */
        public int getSelectionStart() {
            AccessibleText at = getNoteLabelAccessibleText();
            if (at != null) {   // JLabel contains HTML text
                return at.getSelectionStart();
            }
            return -1;
        }

        /**
         * Returns the end offset within the selected text.
         * If there is no selection, but there is
         * a caret, the start and end offsets will be the same.
         *
         * @return the index into the text of the end of the selection
         */
        public int getSelectionEnd() {
            AccessibleText at = getNoteLabelAccessibleText();
            if (at != null) {   // JLabel contains HTML text
                return at.getSelectionEnd();
            }
            return -1;
        }

        /**
         * Returns the portion of the text that is selected.
         *
         * @return the String portion of the text that is selected
         */
        public String getSelectedText() {
            AccessibleText at = getNoteLabelAccessibleText();
            if (at != null) {   // JLabel contains HTML text
                return at.getSelectedText();
            }
            return null;
        }
        /* ===== End AccessibleText impl ===== */
    }
    // inner class AccessibleProgressMonitor

}
