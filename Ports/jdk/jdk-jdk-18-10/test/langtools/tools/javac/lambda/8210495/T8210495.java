/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8210495
 * @summary compiler crashes because of illegal signature in otherwise legal code
 * @compile T8210495.java
 */

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

class T8210495 {
    interface IFilter {
        Component getComponent();
    }

    static class Filter implements IFilter {
        @Override
        public Component getComponent() {
            return null;
        }

    }

    public Component buildFilter(List<? extends Filter> l, Dialog dialog) {
        Panel c = new Panel();
        l.stream()
                .map(f -> {
                    Button btn = (Button)f.getComponent();
                    btn.addActionListener((java.io.Serializable & ActionListener)evt -> {
                        applyFilter(f);
                        dialog.setVisible(false);
                    });
                    return btn;
                })
                .forEach(c::add);
        return c;
    }

    private void applyFilter(IFilter f) { }
}
