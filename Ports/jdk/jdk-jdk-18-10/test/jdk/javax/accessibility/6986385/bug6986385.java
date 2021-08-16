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

/* @test
   @bug 6986385
   @summary JLayer should implement accessible interface
   @author Alexander Potochkin
   @run main bug6986385
*/

import javax.swing.*;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

public class bug6986385 {

    public static void main(String... args) throws Exception {
        JLayer l = new JLayer();
        AccessibleContext acc = l.getAccessibleContext();
        if (acc == null) {
            throw new RuntimeException("JLayer's AccessibleContext is null");
        }
        if (acc.getAccessibleRole() != AccessibleRole.PANEL) {
            throw new RuntimeException("JLayer's AccessibleRole must be PANEL");
        }
    }
}
