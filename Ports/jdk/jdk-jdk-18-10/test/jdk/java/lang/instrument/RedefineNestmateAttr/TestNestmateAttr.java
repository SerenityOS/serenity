/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8046171
 * @summary Class redefinition must preclude changes to nest attributes
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.compiler
 *          java.instrument
 * @compile ../NamedBuffer.java
 * @run main RedefineClassHelper
 * @compile Host/Host.java
 * @run main/othervm -javaagent:redefineagent.jar -Xlog:redefine+class+nestmates=trace TestNestmateAttr Host
 * @compile HostA/Host.java
 * @run main/othervm -javaagent:redefineagent.jar -Xlog:redefine+class+nestmates=trace TestNestmateAttr HostA
 * @compile HostAB/Host.java
 * @run main/othervm -javaagent:redefineagent.jar -Xlog:redefine+class+nestmates=trace TestNestmateAttr HostAB
 * @compile HostABC/Host.java
 * @run main/othervm -javaagent:redefineagent.jar -Xlog:redefine+class+nestmates=trace TestNestmateAttr HostABC
 */

/* Test Description

The basic test class is call Host and we have variants that have zero or more
nested classes named A, B, C etc. Each variant of Host is defined in source
code in its own directory i.e.

Host/Host.java defines zero nested classes
HostA/Host.java defines one nested class A
HostAB/Host.java defines two nested classes A and B (in that order)
etc.

Each Host class has the form:

  public class Host {
    public static String getID() { return "<directory name>/Host.java"; }

    < zero or more empty nested classes>

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
  Host -> HostA' - fails - added a nest member

HostA:
  HostA -> HostA'  - succeeds m() returns 2
  HostA -> Host'   - fails - removed a nest member
  HostA -> HostAB' - fails - added a nest member
  HostA -> HostB'  - fails - replaced a nest member

HostAB:
  HostAB -> HostAB'  - succeeds m() returns 2
  HostAB -> HostBA'  - succeeds m() returns 2
  HostAB -> HostA'   - fails - removed a nest member
  HostAB -> HostABC' - fails - added a nest member
  HostAB -> HostAC'  - fails - replaced a nest member

HostABC:
  HostABC -> HostABC'  - succeeds m() returns 2
  HostABC -> HostACB'  - succeeds m() returns 2
  HostABC -> HostBAC'  - succeeds m() returns 2
  HostABC -> HostBCA'  - succeeds m() returns 2
  HostABC -> HostCAB'  - succeeds m() returns 2
  HostABC -> HostCBA'  - succeeds m() returns 2
  HostABC -> HostAB'   - fails - removed a nest member
  HostABC -> HostABCD' - fails - added a nest member
  HostABC -> HostABD'  - fails - replaced a nest member

More than three nested classes doesn't add to the code coverage so
we stop here.

Note that we always try to load the redefined version even when we expect it
to fail.

We can only directly load one class Host per classloader, so to run all the
groups we either need to use new classloaders, or we reinvoke the test
requesting a different primary directory. We chose the latter using
multiple @run tags. So we preceed as follows:

 @compile Host/Host.java
 @run TestNestmateAttr Host
 @compile HostA/Host.java  - replaces previous Host.class
 @run TestNestmateAttr HostA
 @compile HostAB/Host.java  - replaces previous Host.class
 @run TestNestmateAttr HostAB
etc.

Within the test we directly compile redefined versions of the classes,
using CompilerUtil, and then read the .class file directly as a byte[]
to use with the RedefineClassHelper.

Finally we test redefinition of the NestHost attribute - which is
conceptually simple, but in fact very tricky to do. We do that
when testing HostA so we can reuse the Host$A class.

*/

import java.io.File;
import java.io.FileInputStream;
import jdk.test.lib.ByteCodeLoader;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import static jdk.test.lib.Asserts.assertTrue;

public class TestNestmateAttr {

    static final String SRC = System.getProperty("test.src");
    static final String DEST = System.getProperty("test.classes");
    static final boolean VERBOSE = Boolean.getBoolean("verbose");

    public static void main(String[] args) throws Throwable {
        String origin = args[0];
        System.out.println("Testing original Host class from " + origin);

        // Make sure the Host class loaded directly is an original version
        // and from the expected location
        Host h = new Host();
        assertTrue(h.m() == 1);
        assertTrue(Host.getID().startsWith(origin + "/"));

        String[] badTransforms;  // directories of bad classes
        String[] goodTransforms; // directories of good classes

        boolean testNestHostChanges = false;

        switch (origin) {
        case "Host":
            badTransforms = new String[] {
                "HostA" // add member
            };
            goodTransforms = new String[] {
                origin
            };
            break;

        case "HostA":
            badTransforms = new String[] {
                "Host",   // remove member
                "HostAB", // add member
                "HostB"   // change member
            };
            goodTransforms = new String[] {
                origin
            };
            testNestHostChanges = true;
            break;

        case "HostAB":
            badTransforms = new String[] {
                "HostA",   // remove member
                "HostABC", // add member
                "HostAC"   // change member
            };
            goodTransforms = new String[] {
                origin,
                "HostBA"  // reorder members
            };
            break;

        case "HostABC":
            badTransforms = new String[] {
                "HostAB",   // remove member
                "HostABCD", // add member
                "HostABD"   // change member
            };
            goodTransforms = new String[] {
                origin,
                "HostACB",  // reorder members
                "HostBAC",  // reorder members
                "HostBCA",  // reorder members
                "HostCAB",  // reorder members
                "HostCBA"   // reorder members
            };
            break;

        default: throw new Error("Unknown test directory: " + origin);
        }

        // Compile and check bad transformations
        checkBadTransforms(Host.class, badTransforms);

        // Compile and check good transformations
        checkGoodTransforms(Host.class, goodTransforms);

        if (testNestHostChanges)
            checkNestHostChanges();
    }

    static void checkNestHostChanges() throws Throwable {
        // case 1: remove NestHost attribute
        //   - try to redefine Host$A with a top-level
        //     class called Host$A
        System.out.println("Trying bad retransform that removes the NestHost attribute");

        String name = "Host$A";
        // This is compiled as a top-level class: the $ in the name is not
        // significant to the compiler.
        String hostA = "public class " + name + " {}";

        // Have to do this reflectively as there is no Host$A
        // when compiling the "Host/" case.
        Class<?> nestedA = Class.forName(name);

        try {
            RedefineClassHelper.redefineClass(nestedA, hostA);
            throw new Error("Retransformation to top-level class " + name +
                            " succeeded unexpectedly");
        }
        catch (UnsupportedOperationException uoe) {
            if (uoe.getMessage().contains("attempted to change the class Nest")) {
                System.out.println("Got expected exception " + uoe);
            }
            else throw new Error("Wrong UnsupportedOperationException", uoe);
        }

        // case 2: add NestHost attribute
        //  - This is tricky because the class with no NestHost attribute
        //    has to have the name of a nested class! Plus we need the
        //    redefining class in bytecode form.
        //  - Use the InMemoryJavaCompiler plus ByteCodeLoader to load
        //    the top-level Host$A class
        //  - Try to redefine that class with a real nested Host$A

        System.out.println("Trying bad retransform that adds the NestHost attribute");
        byte[] bytes = InMemoryJavaCompiler.compile(name, hostA);
        Class<?> topLevelHostA = ByteCodeLoader.load(name, bytes);

        byte[] nestedBytes;
        File clsfile = new File(DEST + "/" + name + ".class");
        if (VERBOSE) System.out.println("Reading bytes from " + clsfile);
        try (FileInputStream str = new FileInputStream(clsfile)) {
            nestedBytes = NamedBuffer.loadBufferFromStream(str);
        }
        try {
            RedefineClassHelper.redefineClass(topLevelHostA, nestedBytes);
            throw new Error("Retransformation to nested class " + name +
                            " succeeded unexpectedly");
        }
        catch (UnsupportedOperationException uoe) {
            if (uoe.getMessage().contains("attempted to change the class Nest")) {
                System.out.println("Got expected exception " + uoe);
            }
            else throw new Error("Wrong UnsupportedOperationException", uoe);
        }

        // case 3: replace the NestHost attribute
        //  - the easiest way (perhaps only reasonable way) to do this
        //    is to search for the Utf8 entry used by the Constant_ClassRef,
        //    set in the NestHost attribute, and edit it to refer to a different
        //    name.
        System.out.println("Trying bad retransform that changes the NestHost attribute");
        int utf8Entry_length = 7;
        boolean found = false;
        for (int i = 0; i < nestedBytes.length - utf8Entry_length; i++) {
            if (nestedBytes[i] == 1 &&   // utf8 tag
                nestedBytes[i+1] == 0 && // msb of length
                nestedBytes[i+2] == 4 && // lsb of length
                nestedBytes[i+3] == (byte) 'H' &&
                nestedBytes[i+4] == (byte) 'o' &&
                nestedBytes[i+5] == (byte) 's' &&
                nestedBytes[i+6] == (byte) 't') {

                if (VERBOSE) System.out.println("Appear to have found Host utf8 entry starting at " + i);

                nestedBytes[i+3] = (byte) 'G';
                found = true;
                break;
            }
        }

        if (!found)
            throw new Error("Could not locate 'Host' name in byte array");

        try {
            RedefineClassHelper.redefineClass(nestedA, nestedBytes);
            throw new Error("Retransformation to modified nested class" +
                            " succeeded unexpectedly");
        }
        catch (UnsupportedOperationException uoe) {
            if (uoe.getMessage().contains("attempted to change the class Nest")) {
                System.out.println("Got expected exception " + uoe);
            }
            else throw new Error("Wrong UnsupportedOperationException", uoe);
        }

    }

    static void checkGoodTransforms(Class<?> c, String[] dirs) throws Throwable {
        for (String dir : dirs) {
            dir += "/redef";
            System.out.println("Trying good retransform from " + dir);
            byte[] buf = bytesForHostClass(dir);
            RedefineClassHelper.redefineClass(c, buf);

            // Test redefintion worked
            Host h = new Host();
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
                if (uoe.getMessage().contains("attempted to change the class Nest")) {
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
                              false /* don't recurse */,
                              new String[0]);
    }
}
