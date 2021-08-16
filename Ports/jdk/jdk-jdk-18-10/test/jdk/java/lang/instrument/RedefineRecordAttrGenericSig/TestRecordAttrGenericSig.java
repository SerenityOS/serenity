/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8246774
 * @summary Class redefinition must preclude changes to Record attributes
 * @comment This is a copy of test/jdk/java/lang/instrument/RedefineNestmateAttr/
 * @comment modified for records and the Record attribute.
 *
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.compiler
 *          java.instrument
 * @compile ../NamedBuffer.java
 * @run main RedefineClassHelper
 * @compile Host/Host.java
 * @run main/othervm -javaagent:redefineagent.jar -Xlog:redefine+class+record=trace TestRecordAttrGenericSig Host
 * @compile HostA/Host.java
 * @run main/othervm -javaagent:redefineagent.jar -Xlog:redefine+class+record=trace TestRecordAttrGenericSig HostA
 */

/* Test Description

The basic test class is called Host and we have variants that have record components
with different or no generic_signature attributes.  Each variant of Host is defined
in source code in its own directory i.e.

Host/Host.java has record components with no generic signature attributes.
HostA/Host.java has a record component with generic signature "TT".
HostB/Host.java has a record component with generic signature "TX".

Each Host class has the form:

  public record Host <possible generic>(<java.lang.Object or generic> a, java.lang.String b) {
    public static String getID() { return "<directory name>/Host.java"; }

    public int m() {
        return 1; // original class
    }

  }

Under each directory is a directory "redef" with a modified version of the Host
class that changes the ID to e.g. Host/redef/Host.java, and the method m()
returns 2. This allows us to check we have the redefined class loaded.

Using Host' to represent the redefined version we test redefinition
combinations as follows:

Host:
  Host -> Host'  - succeeds m() returns 2
  Host -> HostA' - fails - added a generic signature

HostA:
  HostA -> HostA'  - succeeds m() returns 2
  HostA -> Host'   - fails - removed a generic signature
  HostA -> HostB'  - fails - replaced a generic signature

Note that we always try to load the redefined version even when we expect it
to fail.

We can only directly load one class Host per classloader, so to run all the
groups we either need to use new classloaders, or we reinvoke the test
requesting a different primary directory. We chose the latter using
multiple @run tags. So we preceed as follows:

 @compile Host/Host.java
 @run TestRecordAttrGenericSig Host
 @compile HostA/Host.java  - replaces previous Host.class
 @run TestRecordAttrGenericSig HostA
etc.

Within the test we directly compile redefined versions of the classes,
using CompilerUtil, and then read the .class file directly as a byte[]
to use with the RedefineClassHelper.

*/

import java.io.File;
import java.io.FileInputStream;
import jdk.test.lib.ByteCodeLoader;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import static jdk.test.lib.Asserts.assertTrue;

public class TestRecordAttrGenericSig {

    static final String SRC = System.getProperty("test.src");
    static final String DEST = System.getProperty("test.classes");
    static final boolean VERBOSE = Boolean.getBoolean("verbose");
    private static final String VERSION = Integer.toString(Runtime.version().feature());

    public static void main(String[] args) throws Throwable {
        String origin = args[0];
        System.out.println("Testing original Host class from " + origin);

        // Make sure the Host class loaded directly is an original version
        // and from the expected location. Use a ctor common to all Host
        // classes.
        Host h = new Host("abc", "def");
        assertTrue(h.m() == 1);
        assertTrue(Host.getID().startsWith(origin + "/"));

        String[] badTransforms;  // directories of bad classes
        String[] goodTransforms; // directories of good classes

        switch (origin) {
        case "Host":
            badTransforms = new String[] {
                "HostA" // add a generic signature to a record component
            };
            goodTransforms = new String[] {
                origin
            };
            break;

        case "HostA":
            badTransforms = new String[] {
                "Host",   // remove a generic signature from a record component
                "HostB"   // change a record component generic signature
            };
            goodTransforms = new String[] {
                origin
            };
            break;

        default: throw new Error("Unknown test directory: " + origin);
        }

        // Compile and check bad transformations
        checkBadTransforms(Host.class, badTransforms);

        // Compile and check good transformations
        checkGoodTransforms(Host.class, goodTransforms);
    }

    static void checkGoodTransforms(Class<?> c, String[] dirs) throws Throwable {
        for (String dir : dirs) {
            dir += "/redef";
            System.out.println("Trying good retransform from " + dir);
            byte[] buf = bytesForHostClass(dir);
            RedefineClassHelper.redefineClass(c, buf);

            // Test redefintion worked
            Host h = new Host("abc", "def");
            assertTrue(h.m() == 2);
            if (VERBOSE) System.out.println("Redefined ID: " + Host.getID());
            assertTrue(Host.getID().startsWith(dir));
        }
    }

    static void checkBadTransforms(Class<?> c, String[] dirs) throws Throwable {
        for (String dir : dirs) {
            dir += "/redef";
            System.out.println("Trying bad retransform from " + dir);
            byte[] buf = bytesForHostClass(dir);
            try {
                RedefineClassHelper.redefineClass(c, buf);
                throw new Error("Retransformation from directory " + dir +
                                " succeeded unexpectedly");
            }
            catch (UnsupportedOperationException uoe) {
                if (uoe.getMessage().contains("attempted to change the class") &&
                    uoe.getMessage().contains(" Record")) {
                    System.out.println("Got expected exception " + uoe);
                }
                else throw new Error("Wrong UnsupportedOperationException", uoe);
            }
        }
    }

    static byte[] bytesForHostClass(String dir) throws Throwable {
        compile("/" + dir);
        File clsfile = new File(DEST + "/" + dir + "/Host.class");
        if (VERBOSE) System.out.println("Reading bytes from " + clsfile);
        byte[] buf = null;
        try (FileInputStream str = new FileInputStream(clsfile)) {
            return buf = NamedBuffer.loadBufferFromStream(str);
        }
    }

    static void compile(String dir) throws Throwable {
        File src = new File(SRC + dir);
        File dst = new File(DEST + dir);
        if (VERBOSE) System.out.println("Compiling from: " + src + "\n" +
                                        "            to: " + dst);
        CompilerUtils.compile(src.toPath(),
                              dst.toPath(),
                              false /* don't recurse */);
    }
}
