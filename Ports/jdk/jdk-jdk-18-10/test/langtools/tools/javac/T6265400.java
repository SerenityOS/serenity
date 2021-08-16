/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6265400
 * @summary  Javac should be shielded from client code errors in JSR 199
 * @modules java.compiler
 *          jdk.compiler
 * @run main T6265400
 */

import java.util.Arrays;

import java.io.*;
import javax.tools.*;

public class T6265400 {
    public static final String SILLY_BILLY = "oh what a silly billy I am";

    public static void main (String... args) {
        try {
            JavaCompiler javac = javax.tools.ToolProvider.getSystemJavaCompiler();
            DiagnosticListener<JavaFileObject> dl =  new DiagnosticListener<JavaFileObject>() {
                    public void report (Diagnostic<? extends JavaFileObject> message) {
                        throw new NullPointerException(SILLY_BILLY);
                    }
                };
            try (StandardJavaFileManager fm = javac.getStandardFileManager(dl, null, null)) {
                Iterable<? extends JavaFileObject> files =
                    fm.getJavaFileObjectsFromStrings(Arrays.asList("badfile.java"));
                javac.getTask(null, fm, dl, null, null, files).call();
            }
        }
        catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof NullPointerException
                && cause.getMessage().equals(SILLY_BILLY))
                return;
            throw new Error("unexpected exception caught: " + e);
        }
        catch (Throwable t) {
            throw new Error("unexpected exception caught: " + t);
        }
        throw new Error("no exception caught");
    }
}
