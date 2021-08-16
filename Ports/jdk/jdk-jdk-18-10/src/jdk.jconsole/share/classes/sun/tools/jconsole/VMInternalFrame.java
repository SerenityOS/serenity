/*
 * Copyright (c) 2004, 2012, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jconsole;

import java.awt.*;
import javax.swing.*;


import static sun.tools.jconsole.Utilities.*;

@SuppressWarnings("serial")
public class VMInternalFrame extends MaximizableInternalFrame {
    private VMPanel vmPanel;

    public VMInternalFrame(VMPanel vmPanel) {
        super("", true, true, true, true);

        this.vmPanel = vmPanel;
        setAccessibleDescription(this,
                                 Messages.VMINTERNAL_FRAME_ACCESSIBLE_DESCRIPTION);
        getContentPane().add(vmPanel, BorderLayout.CENTER);
        pack();
        vmPanel.updateFrameTitle();
    }

    public VMPanel getVMPanel() {
        return vmPanel;
    }

    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        JDesktopPane desktop = getDesktopPane();
        if (desktop != null) {
            Dimension desktopSize = desktop.getSize();
            if (desktopSize.width > 0 && desktopSize.height > 0) {
                d.width  = Math.min(desktopSize.width  - 40, d.width);
                d.height = Math.min(desktopSize.height - 40, d.height);
            }
        }
        return d;
    }
}
