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
/**
 * @test
 * @bug 4195978
 * @summary Verifies that resource bundle names have case distinction.
 * @author joconner
 */
import java.util.*;

public class Bug4195978Test extends ListResourceBundle {

    public static void main(final String args[]) throws Exception {
        new Bug4195978Test().test();
    }

    public void test() throws Exception {
        try {
            // load a property resourcebundle
            final ResourceBundle bundle = ResourceBundle.getBundle("bug4195978Test");
            // load this file as a ListResourceBundle
            final ResourceBundle bundle2 = ResourceBundle.getBundle("Bug4195978Test");

            // get the "test" keyid from both bundles
            String b1 = bundle.getString("test");
            String b2 = bundle2.getString("test");

            // one should be lowercase, the other is uppercase
            // each bundle can be used seperately because their names
            // are distinguished by case.
            if (b1.equals("test") && b2.equals("TEST")) {
                System.out.println("Passed");
            }
        } catch (Exception e) {
            System.err.println("Failed");
            System.err.println(e);
            throw e;
        }


    }

    public Object[][] getContents() {
        return contents;
    }

    Object[][] contents = {
        {"test", "TEST"},
    };


}
