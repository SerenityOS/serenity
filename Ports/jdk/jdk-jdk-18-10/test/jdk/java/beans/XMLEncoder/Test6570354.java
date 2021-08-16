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

/*
 * @test
 * @bug 6570354
 * @summary Tests listeners removing
 * @run main/othervm -Djava.security.manager=allow Test6570354
 * @author Sergey Malenkov
 */

import java.beans.PropertyChangeListener;
import javax.swing.JLabel;

public final class Test6570354 extends AbstractTest<JLabel> {
    public static void main(String[] args) {
        new Test6570354().test(true);
    }

    protected JLabel getObject() {
        JLabel label = new JLabel("");
        label.removePropertyChangeListener((PropertyChangeListener) label.getUI());
        return label;
    }

    protected JLabel getAnotherObject() {
        return new JLabel("");
    }
}
