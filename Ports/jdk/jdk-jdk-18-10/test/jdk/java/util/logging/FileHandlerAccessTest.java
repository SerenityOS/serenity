/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @test
 * @bug 8252883
 * @summary tests the handling of AccessDeniedException due to delay in Windows deletion.
 * @modules java.logging/java.util.logging:open
 * @run main/othervm FileHandlerAccessTest 20
 * @author evwhelan
 */

public class FileHandlerAccessTest {
    public static void main(String[] args) {
        var count = Integer.parseInt(args[0]);
        System.out.println("Testing with " + count + " threads");
        for (var i = 0; i < count; i++) {
            new Thread(FileHandlerAccessTest::access).start();
        }
    }

    private static void access() {
        try {
            var handler = new FileHandler("sample%g.log", 1048576, 2, true);
            handler.publish(new LogRecord(Level.SEVERE, "TEST"));
            handler.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
