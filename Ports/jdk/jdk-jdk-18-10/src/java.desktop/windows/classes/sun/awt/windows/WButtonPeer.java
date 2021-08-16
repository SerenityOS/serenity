/*
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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
package sun.awt.windows;

import java.awt.*;
import java.awt.peer.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

final class WButtonPeer extends WComponentPeer implements ButtonPeer {

    static {
        initIDs();
    }

    // ComponentPeer overrides

    @Override
    public Dimension getMinimumSize() {
        FontMetrics fm = getFontMetrics(((Button)target).getFont());
        String label = ((Button)target).getLabel();
        if ( label == null ) {
            label = "";
        }
        return new Dimension(fm.stringWidth(label) + 14,
                             fm.getHeight() + 8);
    }
    @Override
    public boolean isFocusable() {
        return true;
    }

    // ButtonPeer implementation

    @Override
    public native void setLabel(String label);

    // Toolkit & peer internals

    WButtonPeer(Button target) {
        super(target);
    }

    @Override
    native void create(WComponentPeer peer);

    // native callbacks

    // NOTE: This is called on the privileged toolkit thread. Do not
    //       call directly into user code using this thread!
    public void handleAction(final long when, final int modifiers) {
        // Fixed 5064013: the InvocationEvent time should be equals
        // the time of the ActionEvent
        WToolkit.executeOnEventHandlerThread(target, new Runnable() {
            @Override
            public void run() {
                postEvent(new ActionEvent(target, ActionEvent.ACTION_PERFORMED,
                                          ((Button)target).getActionCommand(),
                                          when, modifiers));
            }
        }, when);
    }


    @Override
    public boolean shouldClearRectBeforePaint() {
        return false;
    }

    /**
     * Initialize JNI field and method IDs
     */
    private static native void initIDs();

    @Override
    @SuppressWarnings("deprecation")
    public boolean handleJavaKeyEvent(KeyEvent e) {
         switch (e.getID()) {
            case KeyEvent.KEY_RELEASED:
                if (e.getKeyCode() == KeyEvent.VK_SPACE){
                    handleAction(e.getWhen(), e.getModifiers());
                }
            break;
         }
         return false;
    }
}
