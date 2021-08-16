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
 * @bug 4353454
 * @summary Test if the second getBundle call finds a bundle in the default Locale search path.
 */

import java.util.ResourceBundle;
import java.util.Locale;

public class Bug4353454 {

    public static void main(String[] args) {
        Locale l = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            test();
            test();
        } finally {
            Locale.setDefault(l);
        }
    }

    private static void test() {
        ResourceBundle myResources = ResourceBundle.getBundle("RB4353454", new Locale(""));
        if (!"Got it!".equals(myResources.getString("text"))) {
            throw new RuntimeException("returned wrong resource for key 'text': "
                                       + myResources.getString("text"));
        }
    }
}
