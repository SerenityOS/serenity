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

import java.awt.peer.LabelPeer;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * A {@code Label} object is a component for placing text in a
 * container. A label displays a single line of read-only text.
 * The text can be changed by the application, but a user cannot edit it
 * directly.
 * <p>
 * For example, the code&nbsp;.&nbsp;.&nbsp;.
 *
 * <hr><blockquote><pre>
 * setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
 * add(new Label("Hi There!"));
 * add(new Label("Another Label"));
 * </pre></blockquote><hr>
 * <p>
 * produces the following labels:
 * <p>
 * <img src="doc-files/Label-1.gif" alt="Two labels: 'Hi There!' and
 * 'Another label'" style="margin: 7px 10px;">
 *
 * @author      Sami Shaio
 * @since       1.0
 */
public class Label extends Component implements Accessible {

    static {
        /* ensure that the necessary native libraries are loaded */
        Toolkit.loadLibraries();
        if (!GraphicsEnvironment.isHeadless()) {
            initIDs();
        }
    }

    /**
     * Indicates that the label should be left justified.
     */
    public static final int LEFT        = 0;

    /**
     * Indicates that the label should be centered.
     */
    public static final int CENTER      = 1;

    /**
     * Indicates that the label should be right justified.
     */
    public static final int RIGHT       = 2;

    /**
     * The text of this label.
     * This text can be modified by the program
     * but never by the user.
     *
     * @serial
     * @see #getText()
     * @see #setText(String)
     */
    String text;

    /**
     * The label's alignment.  The default alignment is set
     * to be left justified.
     *
     * @serial
     * @see #getAlignment()
     * @see #setAlignment(int)
     */
    int    alignment = LEFT;

    private static final String base = "label";
    private static int nameCounter = 0;

    /**
     * Use serialVersionUID from JDK 1.1 for interoperability.
     */
     @Serial
     private static final long serialVersionUID = 3094126758329070636L;

    /**
     * Constructs an empty label.
     * The text of the label is the empty string {@code ""}.
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public Label() throws HeadlessException {
        this("", LEFT);
    }

    /**
     * Constructs a new label with the specified string of text,
     * left justified.
     * @param text the string that the label presents.
     *        A {@code null} value
     *        will be accepted without causing a NullPointerException
     *        to be thrown.
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public Label(String text) throws HeadlessException {
        this(text, LEFT);
    }

    /**
     * Constructs a new label that presents the specified string of
     * text with the specified alignment.
     * Possible values for {@code alignment} are {@code Label.LEFT},
     * {@code Label.RIGHT}, and {@code Label.CENTER}.
     * @param text the string that the label presents.
     *        A {@code null} value
     *        will be accepted without causing a NullPointerException
     *        to be thrown.
     * @param     alignment   the alignment value.
     * @exception HeadlessException if GraphicsEnvironment.isHeadless()
     * returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public Label(String text, int alignment) throws HeadlessException {
        GraphicsEnvironment.checkHeadless();
        this.text = text;
        setAlignment(alignment);
    }

    /**
     * Read a label from an object input stream.
     *
     * @param  s the {@code ObjectInputStream} to read
     * @throws ClassNotFoundException if the class of a serialized object could
     *         not be found
     * @throws IOException if an I/O error occurs
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *         returns {@code true}
     * @serial
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @since 1.4
     */
    @Serial
    private void readObject(ObjectInputStream s)
        throws ClassNotFoundException, IOException, HeadlessException {
        GraphicsEnvironment.checkHeadless();
        s.defaultReadObject();
    }

    /**
     * Construct a name for this component.  Called by getName() when the
     * name is {@code null}.
     */
    String constructComponentName() {
        synchronized (Label.class) {
            return base + nameCounter++;
        }
    }

    /**
     * Creates the peer for this label.  The peer allows us to
     * modify the appearance of the label without changing its
     * functionality.
     */
    public void addNotify() {
        synchronized (getTreeLock()) {
            if (peer == null)
                peer = getComponentFactory().createLabel(this);
            super.addNotify();
        }
    }

    /**
     * Gets the current alignment of this label. Possible values are
     * {@code Label.LEFT}, {@code Label.RIGHT}, and
     * {@code Label.CENTER}.
     * @return the alignment of this label
     * @see java.awt.Label#setAlignment
     */
    public int getAlignment() {
        return alignment;
    }

    /**
     * Sets the alignment for this label to the specified alignment.
     * Possible values are {@code Label.LEFT},
     * {@code Label.RIGHT}, and {@code Label.CENTER}.
     * @param      alignment    the alignment to be set.
     * @exception  IllegalArgumentException if an improper value for
     *                          {@code alignment} is given.
     * @see        java.awt.Label#getAlignment
     */
    public synchronized void setAlignment(int alignment) {
        switch (alignment) {
          case LEFT:
          case CENTER:
          case RIGHT:
            this.alignment = alignment;
            LabelPeer peer = (LabelPeer)this.peer;
            if (peer != null) {
                peer.setAlignment(alignment);
            }
            return;
        }
        throw new IllegalArgumentException("improper alignment: " + alignment);
    }

    /**
     * Gets the text of this label.
     * @return     the text of this label, or {@code null} if
     *             the text has been set to {@code null}.
     * @see        java.awt.Label#setText
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text for this label to the specified text.
     * @param      text the text that this label displays. If
     *             {@code text} is {@code null}, it is
     *             treated for display purposes like an empty
     *             string {@code ""}.
     * @see        java.awt.Label#getText
     */
    public void setText(String text) {
        boolean testvalid = false;
        synchronized (this) {
            if (text != this.text && (this.text == null ||
                                      !this.text.equals(text))) {
                this.text = text;
                LabelPeer peer = (LabelPeer)this.peer;
                if (peer != null) {
                    peer.setText(text);
                }
                testvalid = true;
            }
        }

        // This could change the preferred size of the Component.
        if (testvalid) {
            invalidateIfValid();
        }
    }

    /**
     * Returns a string representing the state of this {@code Label}.
     * This method is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not be
     * {@code null}.
     *
     * @return     the parameter string of this label
     */
    protected String paramString() {
        String align = "";
        switch (alignment) {
            case LEFT:   align = "left"; break;
            case CENTER: align = "center"; break;
            case RIGHT:  align = "right"; break;
        }
        return super.paramString() + ",align=" + align + ",text=" + text;
    }

    /**
     * Initialize JNI field and method IDs
     */
    private static native void initIDs();


/////////////////
// Accessibility support
////////////////


    /**
     * Gets the AccessibleContext associated with this Label.
     * For labels, the AccessibleContext takes the form of an
     * AccessibleAWTLabel.
     * A new AccessibleAWTLabel instance is created if necessary.
     *
     * @return an AccessibleAWTLabel that serves as the
     *         AccessibleContext of this Label
     * @since 1.3
     */
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleAWTLabel();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * {@code Label} class.  It provides an implementation of the
     * Java Accessibility API appropriate to label user-interface elements.
     * @since 1.3
     */
    protected class AccessibleAWTLabel extends AccessibleAWTComponent
    {
        /**
         * Use serialVersionUID from JDK 1.3 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = -3568967560160480438L;

        /**
         * Constructor for the accessible label.
         */
        public AccessibleAWTLabel() {
            super();
        }

        /**
         * Get the accessible name of this object.
         *
         * @return the localized name of the object -- can be null if this
         * object does not have a name
         * @see AccessibleContext#setAccessibleName
         */
        public String getAccessibleName() {
            if (accessibleName != null) {
                return accessibleName;
            } else {
                if (getText() == null) {
                    return super.getAccessibleName();
                } else {
                    return getText();
                }
            }
        }

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.LABEL;
        }

    } // inner class AccessibleAWTLabel

}
