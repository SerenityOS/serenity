/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @key headful
 * @bug 6802868
 * @summary JInternalFrame is not maximized when maximized parent frame
 * @author Alexander Potochkin
 * @library ..
 */

import java.awt.Dimension;
import java.awt.Point;
import java.beans.PropertyVetoException;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;

public class Test6802868 {

    public static void main(String[] args) throws Throwable {
        SwingTest.start(Test6802868.class);
    }

    private final JFrame frame;
    private final JInternalFrame internal;
    private Dimension size;
    private Point location;

    public Test6802868(JFrame frame) {
        JDesktopPane desktop = new JDesktopPane();

        this.frame = frame;
        this.frame.add(desktop);

        this.internal = new JInternalFrame(getClass().getName(), true, true, true, true);
        this.internal.setVisible(true);

        desktop.add(this.internal);
    }

    public void firstAction() throws PropertyVetoException {
        this.internal.setMaximum(true);
    }

    public void firstTest() {
        this.size = this.internal.getSize();
        resizeFrame();
    }

    public void firstValidation() {
        if (this.internal.getSize().equals(this.size)) {
            throw new Error("InternalFrame hasn't changed its size");
        }
    }

    public void secondAction() throws PropertyVetoException {
        this.internal.setIcon(true);
    }

    public void secondTest() {
        this.location = this.internal.getDesktopIcon().getLocation();
        resizeFrame();
    }

    public void secondValidation() {
        if (this.internal.getDesktopIcon().getLocation().equals(this.location)) {
            throw new Error("JDesktopIcon hasn't moved");
        }
    }

    private void resizeFrame() {
        Dimension size = this.frame.getSize();
        size.width += 10;
        size.height += 10;
        this.frame.setSize(size);
    }
}
