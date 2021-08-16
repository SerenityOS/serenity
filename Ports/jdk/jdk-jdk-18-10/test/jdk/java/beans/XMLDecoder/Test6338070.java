/*
 * Copyright (c) 2005, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6338070
 * @summary Tests that parsing is performed even if readObject() is not called
 * @author Sergey Malenkov
 */

import java.beans.XMLDecoder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class Test6338070 {
    private static final String DATA
            = "<java>\n"
            + " <void property=\"owner\">\n"
            + "  <void method=\"init\">\n"
            + "   <string>Hello, world</string>\n"
            + "  </void>\n"
            + " </void>\n"
            + "</java> ";

    public static void main(String[] args) {
        Test6338070 test = new Test6338070();
        InputStream stream = new ByteArrayInputStream(DATA.getBytes());
        new XMLDecoder(stream, test).close();
        if (test.message == null) {
            throw new Error("owner's method is not called");
        }
    }

    private String message;

    public void init(String message) {
        this.message = message;
    }
}
