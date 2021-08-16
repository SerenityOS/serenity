/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8160421 8161902
 * @summary Test to check OffScreenImageSource handles case where
 *          imageComplete(ImageConsumer.SINGLEFRAMEDONE) unregisters.
 * @run main/othervm ImageConsumerUnregisterTest
 */

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ImageConsumerUnregisterTest extends javax.swing.JFrame {

    public static void main(String[] args) throws Exception {

        final java.awt.Component component = new ImageConsumerUnregisterTest();

        // Redirect the System.err stream and verify there is no
        // stacktrace printed
        ByteArrayOutputStream rs = new ByteArrayOutputStream();
        PrintStream obj = System.err;
        System.setErr(new PrintStream(rs));

        String str = "";

        try {
            // Test call
            component.getToolkit().createCustomCursor(
                    component.getGraphicsConfiguration().createCompatibleImage(
                            16, 16, java.awt.Transparency.BITMASK),
                            new java.awt.Point(0, 0), "Hidden");

            // Convert the redirected System.err contents to a string
            str = rs.toString();
        } finally {
            // Reset System.err
            System.setErr(obj);

            if (!str.isEmpty()) {
                throw new RuntimeException("Invalid"
                        + " imageComplete(STATICIMAGEDONE) call");
            }
        }
    }
}

