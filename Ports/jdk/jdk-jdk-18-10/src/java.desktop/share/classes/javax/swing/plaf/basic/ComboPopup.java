/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.basic;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.KeyListener;
import javax.swing.JList;


/**
 * The interface which defines the methods required for the implementation of the popup
 * portion of a combo box.
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
 * @author Tom Santos
 */
@SuppressWarnings("serial") // Same-version serialization only
public interface ComboPopup {
    /**
     * Shows the popup
     */
    public void show();

    /**
     * Hides the popup
     */
    public void hide();

    /**
     * Returns true if the popup is visible (currently being displayed).
     *
     * @return <code>true</code> if the component is visible; <code>false</code> otherwise.
     */
    public boolean isVisible();

    /**
     * Returns the list that is being used to draw the items in the combo box.
     * This method is highly implementation specific and should not be used
     * for general list manipulation.
     *
     * @return the list that is being used to draw the items in the combo box
     */
    public JList<Object> getList();

    /**
     * Returns a mouse listener that will be added to the combo box or null.
     * If this method returns null then it will not be added to the combo box.
     *
     * @return a <code>MouseListener</code> or null
     */
    public MouseListener getMouseListener();

    /**
     * Returns a mouse motion listener that will be added to the combo box or null.
     * If this method returns null then it will not be added to the combo box.
     *
     * @return a <code>MouseMotionListener</code> or null
     */
    public MouseMotionListener getMouseMotionListener();

    /**
     * Returns a key listener that will be added to the combo box or null.
     * If this method returns null then it will not be added to the combo box.
     *
     * @return a key listener that will be added to the combo box or null
     */
    public KeyListener getKeyListener();

    /**
     * Called to inform the ComboPopup that the UI is uninstalling.
     * If the ComboPopup added any listeners in the component, it should remove them here.
     */
    public void uninstallingUI();
}
