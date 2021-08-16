/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.sun.swingset3.demos;

import java.awt.*;
import java.net.URI;
import java.io.IOException;
import javax.swing.*;

/**
 * @author Pavel Porvatov
 */
public class DemoUtilities {

    private DemoUtilities() {
        // Hide constructor
    }

    public static void setToplevelLocation(Window toplevel, Component component,
            int relativePosition) {

        Rectangle compBounds = component.getBounds();

        // Convert component location to screen coordinates
        Point p = new Point();
        SwingUtilities.convertPointToScreen(p, component);

        int x;
        int y;

        // Set frame location to be centered on panel
        switch (relativePosition) {
            case SwingConstants.NORTH: {
                x = (p.x + (compBounds.width / 2)) - (toplevel.getWidth() / 2);
                y = p.y - toplevel.getHeight();
                break;
            }
            case SwingConstants.EAST: {
                x = p.x + compBounds.width;
                y = (p.y + (compBounds.height / 2)) - (toplevel.getHeight() / 2);
                break;
            }
            case SwingConstants.SOUTH: {
                x = (p.x + (compBounds.width / 2)) - (toplevel.getWidth() / 2);
                y = p.y + compBounds.height;
                break;
            }
            case SwingConstants.WEST: {
                x = p.x - toplevel.getWidth();
                y = (p.y + (compBounds.height / 2)) - (toplevel.getHeight() / 2);
                break;
            }
            case SwingConstants.NORTH_EAST: {
                x = p.x + compBounds.width;
                y = p.y - toplevel.getHeight();
                break;
            }
            case SwingConstants.NORTH_WEST: {
                x = p.x - toplevel.getWidth();
                y = p.y - toplevel.getHeight();
                break;
            }
            case SwingConstants.SOUTH_EAST: {
                x = p.x + compBounds.width;
                y = p.y + compBounds.height;
                break;
            }
            case SwingConstants.SOUTH_WEST: {
                x = p.x - toplevel.getWidth();
                y = p.y + compBounds.height;
                break;
            }
            default:
            case SwingConstants.CENTER: {
                x = (p.x + (compBounds.width / 2)) - (toplevel.getWidth() / 2);
                y = (p.y + (compBounds.height / 2)) - (toplevel.getHeight() / 2);
            }
        }
        toplevel.setLocation(x, y);
    }

    public static boolean browse(URI uri) throws IOException {
        // Try using the Desktop api first
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.browse(uri);

            return true;
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return false;
    }
}
