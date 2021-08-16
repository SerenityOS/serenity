/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package com.sun.hotspot.igv.view.actions;

import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.action.WidgetAction.State;
import org.netbeans.api.visual.action.WidgetAction.WidgetMouseEvent;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 * @author David Kaspar
 * @author Peter Hofer
 */
public class CustomizablePanAction extends WidgetAction.LockedAdapter {
    private boolean enabled = true;

    private Scene scene;
    private JScrollPane scrollPane;
    private Point lastLocation;

    private final int modifiersExMask;
    private final int modifiersEx;

    public CustomizablePanAction(int modifiersExMask, int modifiersEx) {
        this.modifiersExMask = modifiersExMask;
        this.modifiersEx = modifiersEx;
    }

    @Override
    protected boolean isLocked() {
        return scrollPane != null;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            if (isLocked())
                throw new IllegalStateException();

            this.enabled = enabled;
        }
    }

    @Override
    public State mousePressed (Widget widget, WidgetMouseEvent event) {
        if (isLocked ())
            return State.createLocked (widget, this);
        if (enabled && (event.getModifiersEx() & modifiersExMask) == modifiersEx) {
            scene = widget.getScene ();
            scrollPane = findScrollPane (scene.getView ());
            if (scrollPane != null) {
                lastLocation = scene.convertSceneToView (widget.convertLocalToScene (event.getPoint ()));
                SwingUtilities.convertPointToScreen (lastLocation, scene.getView ());
                return State.createLocked (widget, this);
            }
        }
        return State.REJECTED;
    }

    private JScrollPane findScrollPane (JComponent component) {
        for (;;) {
            if (component == null)
                return null;
            if (component instanceof JScrollPane)
                return ((JScrollPane) component);
            Container parent = component.getParent ();
            if (! (parent instanceof JComponent))
                return null;
            component = (JComponent) parent;
        }
    }

    @Override
    public State mouseReleased (Widget widget, WidgetMouseEvent event) {
        boolean state = pan (widget, event.getPoint ());
        if (state)
            scrollPane = null;
        return state ? State.createLocked (widget, this) : State.REJECTED;
    }

    @Override
    public State mouseDragged (Widget widget, WidgetMouseEvent event) {
        return pan (widget, event.getPoint ()) ? State.createLocked (widget, this) : State.REJECTED;
    }

    private boolean pan (Widget widget, Point newLocation) {
        if (scrollPane == null  ||  scene != widget.getScene ())
            return false;
        newLocation = scene.convertSceneToView (widget.convertLocalToScene (newLocation));
        SwingUtilities.convertPointToScreen (newLocation, scene.getView ());
        JComponent view = scene.getView ();
        Rectangle rectangle = view.getVisibleRect ();
        rectangle.x += lastLocation.x - newLocation.x;
        rectangle.y += lastLocation.y - newLocation.y;
        view.scrollRectToVisible (rectangle);
        lastLocation = newLocation;
        return true;
    }
}
