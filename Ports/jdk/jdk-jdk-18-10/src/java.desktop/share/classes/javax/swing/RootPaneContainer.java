/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Container;


/**
 * This interface is implemented by components that have a single
 * JRootPane child: JDialog, JFrame, JWindow, JApplet, JInternalFrame.
 * The methods in  this interface are just <i>covers</i> for the JRootPane
 * properties, e.g. <code>getContentPane()</code> is generally implemented
 * like this:<pre>
 *     public Container getContentPane() {
 *         return getRootPane().getContentPane();
 *     }
 * </pre>
 * This interface serves as a <i>marker</i> for Swing GUI builders
 * that need to treat components like JFrame, that contain a
 * single JRootPane, specially.  For example in a GUI builder,
 * dropping a component on a RootPaneContainer would be interpreted
 * as <code>frame.getContentPane().add(child)</code>.
 * <p>
 * As a convenience, the standard classes that implement this interface
 * (such as {@code JFrame}, {@code JDialog}, {@code JWindow}, {@code JApplet},
 * and {@code JInternalFrame}) have their {@code add}, {@code remove},
 * and {@code setLayout} methods overridden, so that they delegate calls
 * to the corresponding methods of the {@code ContentPane}.
 * For example, you can add a child component to a frame as follows:
 * <pre>
 *       frame.add(child);
 * </pre>
 * instead of:
 * <pre>
 *       frame.getContentPane().add(child);
 * </pre>
 * <p>
 * The behavior of the <code>add</code> and
 * <code>setLayout</code> methods for
 * <code>JFrame</code>, <code>JDialog</code>, <code>JWindow</code>,
 * <code>JApplet</code> and <code>JInternalFrame</code> is controlled by
 * the <code>rootPaneCheckingEnabled</code> property. If this property is
 * true (the default), then calls to these methods are
  * forwarded to the <code>contentPane</code>; if false, these
  * methods operate directly on the <code>RootPaneContainer</code>. This
  * property is only intended for subclasses, and is therefore protected.
 *
 * @see JRootPane
 * @see JFrame
 * @see JDialog
 * @see JWindow
 * @see JApplet
 * @see JInternalFrame
 *
 * @author Hans Muller
 * @since 1.2
 */
public interface RootPaneContainer
{
    /**
     * Return this component's single JRootPane child.  A conventional
     * implementation of this interface will have all of the other
     * methods indirect through this one.  The rootPane has two
     * children: the glassPane and the layeredPane.
     *
     * @return this components single JRootPane child.
     * @see JRootPane
     */
    JRootPane getRootPane();


    /**
     * The "contentPane" is the primary container for application
     * specific components.  Applications should add children to
     * the contentPane, set its layout manager, and so on.
     * <p>
     * The contentPane may not be null.
     * <p>
     * Generally implemented with
     * <code>getRootPane().setContentPane(contentPane);</code>
     *
     * @exception java.awt.IllegalComponentStateException (a runtime
     *            exception) if the content pane parameter is null
     * @param contentPane the Container to use for the contents of this
     *        JRootPane
     * @see JRootPane#getContentPane
     * @see #getContentPane
     */
    void setContentPane(Container contentPane);


    /**
     * Returns the contentPane.
     *
     * @return the value of the contentPane property.
     * @see #setContentPane
     */
    Container getContentPane();


    /**
     * A Container that manages the contentPane and in some cases a menu bar.
     * The layeredPane can be used by descendants that want to add a child
     * to the RootPaneContainer that isn't layout managed.  For example
     * an internal dialog or a drag and drop effect component.
     * <p>
     * The layeredPane may not be null.
     * <p>
     * Generally implemented with<pre>
     *    getRootPane().setLayeredPane(layeredPane);</pre>
     *
     * @param layeredPane the layered pane
     * @exception java.awt.IllegalComponentStateException (a runtime
     *            exception) if the layered pane parameter is null
     * @see #getLayeredPane
     * @see JRootPane#getLayeredPane
     */
    void setLayeredPane(JLayeredPane layeredPane);


    /**
     * Returns the layeredPane.
     *
     * @return the value of the layeredPane property.
     * @see #setLayeredPane
     */
    JLayeredPane getLayeredPane();


    /**
     * The glassPane is always the first child of the rootPane
     * and the rootPanes layout manager ensures that it's always
     * as big as the rootPane.  By default it's transparent and
     * not visible.  It can be used to temporarily grab all keyboard
     * and mouse input by adding listeners and then making it visible.
     * by default it's not visible.
     * <p>
     * The glassPane may not be null.
     * <p>
     * Generally implemented with
     * <code>getRootPane().setGlassPane(glassPane);</code>
     *
     * @param glassPane the glass pane
     * @see #getGlassPane
     * @see JRootPane#setGlassPane
     */
    void setGlassPane(Component glassPane);


    /**
     * Returns the glassPane.
     *
     * @return the value of the glassPane property.
     * @see #setGlassPane
     */
    Component getGlassPane();

}
