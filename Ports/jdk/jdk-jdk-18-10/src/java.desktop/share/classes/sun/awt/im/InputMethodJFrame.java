/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.im;

import java.io.Serial;

import javax.swing.JFrame;
import javax.swing.JRootPane;

/**
 * Implements a Swing based input method window that provides the minimal
 * functionality as specified in
 * {@link java.awt.im.spi.InputMethodContext#createInputMethodJFrame}.
 *
 */
public class InputMethodJFrame
        extends JFrame
        implements InputMethodWindow {

    InputContext inputContext = null;

    /**
     * Constructs a Swing based input method window.
     */
    public InputMethodJFrame(String title, InputContext context) {
        super(title);
        //InputMethodJFrame never has LookAndFeel decoration
        if(JFrame.isDefaultLookAndFeelDecorated())
        {
           this.setUndecorated(true);
           this.getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        }
        if (context != null) {
            this.inputContext = context;
        }
        setFocusableWindowState(false);
    }

    public void setInputContext(InputContext inputContext) {
        this.inputContext = inputContext;
    }

    public java.awt.im.InputContext getInputContext() {
        if (inputContext != null) {
            return inputContext;
        } else {
            return super.getInputContext();
        }
    }

    /**
     * Use serialVersionUID from JDK 1.7 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -4705856747771842549L;
}
