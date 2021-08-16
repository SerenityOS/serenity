/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5030623
 * @summary Basic test of all append() methods in Writer and inherited classes.
 */

import java.io.*;
import java.lang.reflect.*;

public class Append {
    // append methods throw IOException
    private static Class [] io = {
        Writer.class, BufferedWriter.class, FilterWriter.class,
        OutputStreamWriter.class, FileWriter.class
    };

    // append methods don't throw IOException
    private static Class [] nio = {
        CharArrayWriter.class, StringWriter.class, PrintWriter.class,
        PrintStream.class
    };

    public static void main(String [] args) {
        for (int i = 0; i < io.length; i++)
            test(io[i], true);
        for (int i = 0; i < nio.length; i++)
            test(nio[i], false);
    }

    private static void test(Class c, boolean io) {
        try {
            Class [] cparams = { char.class };
            test(c.getMethod("append", cparams), io);
            Class [] csparams = { CharSequence.class };
            test(c.getMethod("append", csparams), io);
        } catch (NoSuchMethodException x) {
            throw new RuntimeException("No append method found");
        }
    }

    private static void test(Method m, boolean io) {
        Class [] ca = m.getExceptionTypes();
        boolean found = false;
        for (int i = 0; i < ca.length; i++) {
            if (ca[i].equals(IOException.class)) {
                found = true;
                break;
            }
        }

        if (found && !io)
            throw new RuntimeException("Unexpected IOException");
        if (!found && io)
            throw new RuntimeException("Missing IOException");
    }
}
