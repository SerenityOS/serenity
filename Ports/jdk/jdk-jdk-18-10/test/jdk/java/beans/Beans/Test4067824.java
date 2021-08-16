/*
 * Copyright (c) 1997, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4067824
 * @summary Tests exception details in Beans.instantiate()
 * @author Graham Hamilton
 */

import java.beans.Beans;
import java.io.FileOutputStream;
import java.io.StreamCorruptedException;

public class Test4067824 {
    public static void main(String[] args) throws Exception {
        ClassLoader cl = Test4067824.class.getClassLoader();
        try {
            Beans.instantiate(cl, "Test4067824");
        }
        catch (ClassNotFoundException exception) {
            // This is expected.  Make sure there is the right detail message:
            if (exception.toString().indexOf("IllegalAccessException") < 0)
                throw new Error("unexpected exception", exception);
        }
        FileOutputStream fout = new FileOutputStream("foo.ser");
        fout.write(new byte [] {1, 2, 3, 4, 5});
        fout.close();
        try {
            // trying to instantiate corrupt foo.ser
            Beans.instantiate(cl, "foo");
            throw new Error("Instantiated corrupt .ser file OK!!??");
        }
        catch (ClassNotFoundException exception) {
            // expected exception
        }
        catch (StreamCorruptedException exception) {
            // expected exception
        }
    }

    // private constructor means Beans.instantiate() will fail
    private Test4067824() {
    }
}
