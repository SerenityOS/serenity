/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6857057
 * @summary test that the JCK GlyphView2021 test doesn't fail
 * @author Sergey Groznyh
 * @run main bug6857057
 */

import javax.swing.*;
import javax.swing.text.Element;
import javax.swing.text.GlyphView;
import javax.swing.text.View;

public class bug6857057 {

    bug6857057() {
        Element elem = new StubBranchElement(" G L Y P H V");
        GlyphView view = new GlyphView(elem);
        float pos = elem.getStartOffset();
        float len = elem.getEndOffset() - pos;
        int res = view.getBreakWeight(View.X_AXIS, pos, len);
        if (res != View.ExcellentBreakWeight) {
            throw new RuntimeException("breakWeight != ExcellentBreakWeight");
        }
    }

    public static void main(String[] args) throws Throwable {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new bug6857057();
            }
        });

        System.out.println("OK");
    }
}
