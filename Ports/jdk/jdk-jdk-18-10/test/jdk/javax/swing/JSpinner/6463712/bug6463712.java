/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/*
 * @test
 * @bug 6463712
 * @summary Events forwarded from previous model
 */
public class bug6463712 implements ChangeListener {

    public bug6463712() {
        SpinnerNumberModel m1 = new SpinnerNumberModel();
        JSpinner s = new JSpinner(m1);
        s.addChangeListener(this);
        SpinnerDateModel m2 = new SpinnerDateModel();
        s.setModel(m2);

        // m1 is no longer linked to the JSpinner (it has been replaced by m2), so
        // the following should not trigger a call to our stateChanged() method...
        m1.setValue(new Integer(1));
    }

    public void stateChanged(ChangeEvent e) {
        throw new RuntimeException("Should not receive this event.");
    }

    public static void main(String[] args) {
        bug6463712 bug = new bug6463712();
    }
}
