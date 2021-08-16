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
 * @bug 4062657
 * @summary Verify that property.save uses local lineseparator
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

/* Note: this regression test only detects incorrect line
 * separator on platform running the test
 */

public class SaveSeparator {

    public static void main(String[] argv) throws IOException {
        // Save a property set
        Properties myProps = new Properties();
        ByteArrayOutputStream myOut = new ByteArrayOutputStream(40);
        myProps.store(myOut, "A test");

        // Examine the result to verify that line.separator was used
        String theSeparator = System.getProperty("line.separator");
        String content = myOut.toString();
        if (!content.endsWith(theSeparator))
            throw new RuntimeException("Incorrect Properties line separator.");
    }
}
