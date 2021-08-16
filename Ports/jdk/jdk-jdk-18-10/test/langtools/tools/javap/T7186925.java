/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7186925
 * @summary JavapTask passes null to java.io.Writer
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;
import java.util.*;
import javax.tools.*;
import com.sun.tools.javap.*;

public class T7186925
{
    public static void main(String... args) {
        new T7186925().run();
    }

    void run() {
        verify("java.lang.Object");
        if (errors > 0)
            throw new Error(errors + " found.");
    }

    void verify(String className) {
        try {
            JavaFileManager fileManager = JavapFileManager.create(null, null);
            JavaFileObject fo = fileManager.getJavaFileForInput(StandardLocation.PLATFORM_CLASS_PATH, className, JavaFileObject.Kind.CLASS);
            if (fo == null) {
                error("Can't find " + className);
            } else {
                JavapTask t = new JavapTask(null, fileManager, null);
                t.handleOptions(new String[] { "-sysinfo", className });
                JavapTask.ClassFileInfo cfInfo = t.read(fo);
                expectEqual(cfInfo.cf.byteLength(), cfInfo.size);
            }
        } catch (NullPointerException ee) {
            ee.printStackTrace();
            error("Exception: " + ee);
        } catch (Exception ee) {
            System.err.println("Caught exception: " + ee);
        }
    }

    void expectEqual(int found, int expected) {
        if (found != expected)
            error("bad value found: " + found + " expected: " + expected);
    }

    void error(String msg) {
        System.err.println(msg);
        errors++;
    }

    int errors;
}
