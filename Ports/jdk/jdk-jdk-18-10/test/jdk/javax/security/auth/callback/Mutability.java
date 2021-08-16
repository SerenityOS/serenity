/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8242330
 * @library /test/lib
 * @summary Arrays should be cloned in several JAAS Callback classes
 */

import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;

import static jdk.test.lib.Asserts.assertEQ;

public class Mutability {
    public static void main(String[] args) {

        // #1. ConfirmationCallback.new(3)
        String[] i11 = {"1", "2"};
        ConfirmationCallback c1 = new ConfirmationCallback(
                ConfirmationCallback.INFORMATION,
                i11,
                0);

        // Modify argument of constructor
        i11[0] = "x";
        String[] o11 = c1.getOptions();
        assertEQ(o11[0], "1");
        // Modify output
        o11[0] = "y";
        String[] o12 = c1.getOptions();
        assertEQ(o12[0], "1");

        // #2. ConfirmationCallback.new(4)
        String[] i21 = {"1", "2"};
        ConfirmationCallback c2 = new ConfirmationCallback(
                "Hi",
                ConfirmationCallback.INFORMATION,
                i21,
                0);

        // Modify argument of constructor
        i21[0] = "x";
        assertEQ(c2.getOptions()[0], "1");

        // #3. ChoiceCallback.new
        String[] i31 = {"1", "2"};
        ChoiceCallback c3 = new ChoiceCallback(
                "Hi",
                i31,
                0,
                true);

        // Modify argument of constructor
        i31[0] = "x";
        String[] o31 = c3.getChoices();
        assertEQ(o31[0], "1");
        // Modify output of getChoices
        o31[0] = "y";
        String[] o32 = c3.getChoices();
        assertEQ(o32[0], "1");

        int[] s31 = {0, 1};
        c3.setSelectedIndexes(s31);

        // Modify argument of setSelectedIndexes
        s31[0] = 1;
        int[] s32 = c3.getSelectedIndexes();
        assertEQ(s32[0], 0);
        // Modify output of getSelectedIndexes
        s32[1] = 0;
        int[] s33 = c3.getSelectedIndexes();
        assertEQ(s33[1], 1);
    }
}
