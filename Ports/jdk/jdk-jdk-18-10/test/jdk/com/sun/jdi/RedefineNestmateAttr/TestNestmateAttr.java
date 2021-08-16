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
 * @comment This is a copy of test/jdk/java/lang/instrument/RedefineNestmateAttr/
 * @comment modified for JDI
 * @library /test/lib ..
 * @modules java.compiler
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @compile NamedBuffer.java
 * @compile Host/Host.java
 * @run main/othervm TestNestmateAttr Host
 * @compile HostA/Host.java
 * @run main/othervm TestNestmateAttr HostA
 * @compile HostAB/Host.java
 * @run main/othervm TestNestmateAttr HostAB
 * @compile HostABC/Host.java
 * @run main/othervm TestNestmateAttr HostABC
 */

/* Test Description

The basic test class is called Host and we have variants that have zero or more
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
multiple @run tags. So we proceed as follows:

 @compile Host/Host.java
 @run TestNestmateAttr Host
 @compile HostA/Host.java  - replaces previous Host.class
 @run TestNestmateAttr HostA
 @compile HostAB/Host.java  - replaces previous Host.class
 @run TestNestmateAttr HostAB
etc.

Within the test we directly compile redefined versions of the classes,
using CompilerUtil, and then read the .class file directly as a byte[].

Finally we test redefinition of the NestHost attribute - which is
conceptually simple, but in fact very tricky to do. We do that
when testing HostA so we can reuse the Host$A class.

*/

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.test.lib.ByteCodeLoader;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import static jdk.test.lib.Asserts.assertTrue;

/* For JDI the test is split across two VMs and so split into
   two main classes. This is the class we will run under the debugger.
   Package access so we can define in the same source file for ease of
   reference.
*/
class Target {

    static Class<?> topLevelHostA; // Prevent unloading of the class

    // We have to load all of the variants of the classes that we will
    // attempt to redefine. This requires some in-memory compilation
    // and use of additional classloaders.

    public static void main(String[] args) throws Throwable {
        String origin = args[0];
        System.out.println("Target: Testing original Host class from " + origin);

        // Make sure the Host class loaded directly is an original version
        // and from the expected location
        Host h = new Host();
        assertTrue(h.m() == 1);
        assertTrue(Host.getID().startsWith(origin + "/"));

        // The rest of this setup is only needed for the case
        // when we perform the checkNestHostChanges() test.
        if (origin.equals("HostA")) {
            String name = "Host$A";

            // Have to do this reflectively as there is no Host$A
            // when compiling the "Host/" case.
            Class<?> nestedA = Class.forName(name); // triggers initialization

            // This is compiled as a top-level class: the $ in the name is not
            // significant to the compiler.
            String hostA = "public class " + name + " {}";
            byte[] bytes = InMemoryJavaCompiler.compile(name, hostA);
            // And we have to load this into a new classloader
            topLevelHostA = ByteCodeLoader.load(name, bytes);
            // The loaded class has not been linked (as per ClassLoader.resolveClass)
            // and so will be filtered out by VirtualMachine.allClasses(). There are
            // a number of ways to force linking - this is the simplest.
            Object o = topLevelHostA.newInstance();

            // sanity check
            assertTrue(nestedA.getClassLoader() != topLevelHostA.getClassLoader());

        }

        breakpoint();    // debugger runs to here before enabling events
        allowRedefine(); // debugger stops us here to attempt redefinitions

        System.out.println("Target executed okay");
    }

    static void allowRedefine() { }
    static void breakpoint() { }
}

public class TestNestmateAttr extends TestScaffold {

    static final String SRC = System.getProperty("test.src");
    static final String DEST = System.getProperty("test.classes");
    static final boolean VERBOSE = Boolean.getBoolean("verbose");

    static String origin;

    // override this to correct a bug so arguments can be passed to
    // the Target class
    protected void startUp(String targetName) {
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        argList.add(0, targetName); // pre-pend so it becomes the first "app" arg
        // We need the class path that contains the path to jdk.test.lib.Asserts.
        argList.add(0, " -cp " + System.getProperty("test.class.path"));
        println("run args: " + argList);
        connect((String[]) argList.toArray(args));
        waitForVMStart();
    }

    TestNestmateAttr (String[] args) {
        super(args);
    }

    public static void main(String[] args) throws Throwable {
        origin = args[0];
        new TestNestmateAttr(args).startTests();
    }

    public void runTests() throws Exception {
        // Get Target into debuggable state
        BreakpointEvent bpe = startTo("Target", "breakpoint", "()V");
        EventRequestManager erm = vm().eventRequestManager();
        MethodEntryRequest mee = erm.createMethodEntryRequest();
        mee.addClassFilter("Target");
        mee.enable();

        // Allow application to complete and shut down
        listenUntilVMDisconnect();

        if (getExceptionCaught()) {
            throw new Exception("TestNestmateAttr: failed due to unexpected exception - check logs for details");
        }
        else if (!testFailed) {
            println("TestNestmateAttr: passed");
        } else {
            throw new Exception("TestNestmateAttr: failure reported - check log for details");
        }
    }

    // All the actual work is done from here once we see we've entered Target.allowRedefine()
    public void methodEntered(MethodEntryEvent event) {
        Method meth = event.location().method();

        if (!meth.name().equals("allowRedefine")) {
            return;
        }

        System.out.println("TestNestmateAttr: Testing original Host class from " + origin);

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

        // Need to locate the type we will be trying to redefine in Target
        findReferenceTypes();

        try {
            // Compile and check bad transformations
            checkBadTransforms(_Host, badTransforms);

            // Compile and check good transformations
            checkGoodTransforms(_Host, goodTransforms);

            if (testNestHostChanges)
                checkNestHostChanges();
        }
        catch (Throwable t) {
            failure(t);
        }
    }

    // override to give exception details
    protected void failure(Throwable t) {
        super.failure(t.getMessage());
        t.printStackTrace(System.out);
    }

    // These are references to the types in Target
    // that we will be trying to redefine.
    ReferenceType _Host;
    ReferenceType _Host_A_nested;
    ReferenceType _Host_A_topLevel;

    void findReferenceTypes() {
        List<ReferenceType> classes = vm().allClasses();
        ClassLoaderReference cl = null; // track the main loader
        ReferenceType a1 = null;
        ReferenceType a2 = null;
        for (ReferenceType c : classes) {
            String name = c.name();
            if (name.equals("Host")) {
                _Host = c;
                cl = c.classLoader();
            }
            else if (name.equals("Host$A")) {
                if (a1 == null) {
                    a1 = c;
                } else if (a2 == null) {
                    a2 = c;
                }
                else {
                    assertTrue(false); // Too many Host$A classes found!
                }
            }
        }
        assertTrue(_Host != null);

        // The rest of this setup is only needed for the case
        // when we perform the checkNestHostChanges() test.
        if (origin.equals("HostA")) {
            assertTrue(a1 != null);
            assertTrue(a2 != null);

            if (a1.classLoader() == cl) {
                _Host_A_nested = a1;
                assertTrue(a2.classLoader() != cl);
                _Host_A_topLevel = a2;
            }
            else if (a2.classLoader() == cl) {
                _Host_A_nested = a2;
                assertTrue(a1.classLoader() != cl);
                _Host_A_topLevel = a1;
            }
            else {
                assertTrue(false); // Wrong classLoaders found
            }
        }
    }

    void checkNestHostChanges() throws Throwable {
        Map<ReferenceType, byte[]> map = new HashMap<>();

        // case 1: remove NestHost attribute
        //   - try to redefine nested Host$A with a top-level
        //     class called Host$A
        System.out.println("Trying bad retransform that removes the NestHost attribute");

        String name = "Host$A";

        // This is compiled as a top-level class: the $ in the name is not
        // significant to the compiler.
        String hostA = "public class " + name + " {}";
        byte[] bytes = InMemoryJavaCompiler.compile(name, hostA);

        map.put(_Host_A_nested, bytes);

        try {
            vm().redefineClasses(map);
            throw new Error("Retransformation to top-level class " + name +
                            " succeeded unexpectedly");
        }
        catch (UnsupportedOperationException uoe) {
            if (uoe.getMessage().contains("changes to class attribute not implemented")) {
                System.out.println("Got expected exception " + uoe);
            }
            else throw new Error("Wrong UnsupportedOperationException", uoe);
        }

        map.clear();

        // case 2: add NestHost attribute
        //  - This is tricky because the class with no NestHost attribute
        //    has to have the name of a nested class! But we know how to
        //    do that as we already created a top-level Host$A. So now
        //    we try to replace with a really nested Host$A.

        System.out.println("Trying bad retransform that adds the NestHost attribute");

        byte[] nestedBytes;
        File clsfile = new File(DEST + "/" + name + ".class");
        if (VERBOSE) System.out.println("Reading bytes from " + clsfile);
        try (FileInputStream str = new FileInputStream(clsfile)) {
            nestedBytes = NamedBuffer.loadBufferFromStream(str);
        }

        map.put(_Host_A_topLevel, nestedBytes);

        try {
            vm().redefineClasses(map);
            throw new Error("Retransformation to nested class " + name +
                            " succeeded unexpectedly");
        }
        catch (UnsupportedOperationException uoe) {
            if (uoe.getMessage().contains("changes to class attribute not implemented")) {
                System.out.println("Got expected exception " + uoe);
            }
            else throw new Error("Wrong UnsupportedOperationException", uoe);
        }

        map.clear();

        // case 3: replace the NestHost attribute
        //  - the easiest way (perhaps only reasonable way) to do this
        //    is to search for the Utf8 entry used by the Constant_ClassRef,
        //    set in the NestHost attribute, and edit it to refer to a different
        //    name. We reuse nestedBytes from above.

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

        map.put(_Host_A_nested, nestedBytes);

        try {
            vm().redefineClasses(map);
            throw new Error("Retransformation to modified nested class" +
                            " succeeded unexpectedly");
        }
        catch (UnsupportedOperationException uoe) {
            if (uoe.getMessage().contains("changes to class attribute not implemented")) {
                System.out.println("Got expected exception " + uoe);
            }
            else throw new Error("Wrong UnsupportedOperationException", uoe);
        }

    }

    void checkGoodTransforms(ReferenceType c, String[] dirs) throws Throwable {
        // To verify the redefinition actually took place we will invoke the
        // Host.getID method and check the result. To do that we need to find the
        // main thread in the target VM. We don't check that "(new Host()).m()"
        // returns 2 due to the complexity of setting that up via JDI.

        ThreadReference main = null;
        List<ThreadReference> threads = vm().allThreads();
        for (ThreadReference t : threads) {
            if (t.name().equals("main")) {
                main = t;
                break;
            }
        }

        assertTrue(main != null);

        // Now find the method
        Method getID = null;
        List<Method> methods = _Host.methodsByName("getID");
        assertTrue(methods.size() == 1);
        getID = methods.get(0);

        Map<ReferenceType, byte[]> map = new HashMap<>();
        for (String dir : dirs) {
            dir += "/redef";
            System.out.println("Trying good retransform from " + dir);
            byte[] buf = bytesForHostClass(dir);
            map.put(c, buf);
            vm().redefineClasses(map);
            map.clear();
            // Test redefinition worked
            Value v = ((ClassType)_Host).invokeMethod(main, getID, Collections.emptyList(), 0);
            assertTrue(v instanceof StringReference);
            String id =  ((StringReference)v).value();
            if (VERBOSE) System.out.println("Redefined ID: " + id);
            assertTrue(id.startsWith(dir));
            assertTrue(id.contains("/redef/"));
        }
    }

    void checkBadTransforms(ReferenceType c, String[] dirs) throws Throwable {
        Map<ReferenceType, byte[]> map = new HashMap<>();
        for (String dir : dirs) {
            dir += "/redef";
            System.out.println("Trying bad retransform from " + dir);
            byte[] buf = bytesForHostClass(dir);
            map.put(c, buf);
            try {
                vm().redefineClasses(map);
                throw new Error("Retransformation from directory " + dir +
                                " succeeded unexpectedly");
            }
            catch (UnsupportedOperationException uoe) {
                if (uoe.getMessage().contains("changes to class attribute not implemented")) {
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
