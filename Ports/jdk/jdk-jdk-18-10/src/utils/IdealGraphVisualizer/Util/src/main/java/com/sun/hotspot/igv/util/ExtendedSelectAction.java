/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */
package com.sun.hotspot.igv.util;

import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.SelectProvider;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.action.WidgetAction.State;
import org.netbeans.api.visual.action.WidgetAction.WidgetKeyEvent;
import org.netbeans.api.visual.action.WidgetAction.WidgetMouseEvent;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Thomas Wuerthinger
 */
public class ExtendedSelectAction extends WidgetAction.Adapter {

    private WidgetAction innerAction;
    private JPanel panel;

    public ExtendedSelectAction(SelectProvider provider) {
        innerAction = ActionFactory.createSelectAction(provider);
        panel = new JPanel();
    }

    @Override
    public State mousePressed(Widget widget, WidgetMouseEvent event) {
        // TODO: Solve this differently?
        if (event.getButton() != MouseEvent.BUTTON2) {
            return innerAction.mousePressed(widget, new WidgetMouseEvent(event.getEventID(), new MouseEvent(panel, (int) event.getEventID(), event.getWhen(), event.getModifiersEx(), event.getPoint().x, event.getPoint().y, event.getClickCount(), event.isPopupTrigger(), MouseEvent.BUTTON1)));
        } else {
            return super.mousePressed(widget, event);
        }
    }

    @Override
    public State mouseReleased(Widget widget, WidgetMouseEvent event) {
        return innerAction.mouseReleased(widget, event);
    }

    @Override
    public State keyTyped(Widget widget, WidgetKeyEvent event) {
        return innerAction.keyTyped(widget, event);
    }
}
