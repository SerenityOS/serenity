/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @bug 8048147
 * @summary Check if proper AccessControlException is thrown
 *          in case of nested Subject.doAs() invocations
 *          when one of protection domains doesn't have permissions
 *
 * @library /test/lib
 *
 * @run main NestedActions jar NestedActionsACE.jar
 *              NestedActionsACE.class Utils.class
 * @run main NestedActions jar NestedActionsPAE.jar
 *              NestedActionsPAE.class Utils.class
 * @run main NestedActions jar NestedActionsOnePrincipal.jar
 *              NestedActionsOnePrincipal.class Utils.class
 * @run main NestedActions jar NestedActionsTwoPrincipals.jar
 *              NestedActionsTwoPrincipals.class Utils.class
 * @run main NestedActions jar WriteToFileAction.jar
 *              WriteToFileAction.class
 * @run main NestedActions jar WriteToFileNegativeAction.jar
 *              WriteToFileNegativeAction.class
 * @run main NestedActions jar WriteToFileExceptionAction.jar
 *              WriteToFileExceptionAction.class
 * @run main NestedActions jar ReadFromFileAction.jar
 *              ReadFromFileAction.class
 * @run main NestedActions jar ReadFromFileNegativeAction.jar
 *              ReadFromFileNegativeAction.class
 * @run main NestedActions jar ReadFromFileExceptionAction.jar
 *              ReadFromFileExceptionAction.class
 * @run main NestedActions jar ReadPropertyAction.jar
 *              ReadPropertyAction.class
 * @run main NestedActions jar ReadPropertyNegativeAction.jar
 *              ReadPropertyNegativeAction.class
 * @run main NestedActions jar ReadPropertyExceptionAction.jar
 *              ReadPropertyExceptionAction.class ReadPropertyException.class
 *
 * @run main NestedActions NestedActionsACE policy.expect.ace
 *              NestedActionsACE.jar WriteToFileNegativeAction.jar
 *              ReadFromFileNegativeAction.jar ReadPropertyNegativeAction.jar
 * @run main NestedActions NestedActionsPAE policy.expect.pae
 *              NestedActionsPAE.jar WriteToFileExceptionAction.jar
 *              ReadFromFileExceptionAction.jar ReadPropertyExceptionAction.jar
 * @run main NestedActions NestedActionsOnePrincipal policy.one.principal
 *              NestedActionsOnePrincipal.jar WriteToFileAction.jar
 *              ReadFromFileAction.jar ReadPropertyAction.jar
 * @run main NestedActions NestedActionsTwoPrincipals policy.two.principals
 *              NestedActionsTwoPrincipals.jar WriteToFileAction.jar
 *              ReadFromFileAction.jar ReadPropertyAction.jar
 */
public class NestedActions {

    static final String file = "NestedActions.tmp";
    static final String PS = System.getProperty("path.separator");
    static final String FS = System.getProperty("file.separator");
    static final String TEST_CLASSES = System.getProperty("test.classes");
    static final String TEST_SOURCES = System.getProperty("test.src");
    static final String JAVA_OPTS = System.getProperty("test.java.opts");
    static final String JAVA = System.getProperty("java.home")
            + FS + "bin" + FS + "java";

    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            if ("jar".equals(args[0]) && args.length > 2) {
                createJar(args[1],
                    Arrays.copyOfRange(args, 2, args.length));
            } else {
                runJava(args);
            }
        } else {
            throw new RuntimeException("Wrong parameters");
        }
    }

    static void createJar(String dest, String... files) throws IOException {
        System.out.println("Create " + dest + " with the following content:");
        try (JarOutputStream jos = new JarOutputStream(
                new FileOutputStream(dest), new Manifest())) {
            for (String file : files) {
                System.out.println("  " + file);
                jos.putNextEntry(new JarEntry(file));
                try (FileInputStream fis = new FileInputStream(
                        TEST_CLASSES + FS + file)) {
                    fis.transferTo(jos);
                }
            }
        }
    }

    static void runJava(String[] args) {
        if (args == null || args.length < 3) {
            throw new IllegalArgumentException("wrong parameters");
        }

        List<String> cmds = new ArrayList<>();
        cmds.add(JAVA);
        StringBuilder sb = new StringBuilder();
        cmds.add("-classpath");
        for (int i=2; i<args.length; i++) {
            sb.append(args[i]).append(PS);
        }
        cmds.add(sb.toString());
        if (JAVA_OPTS != null && !JAVA_OPTS.isEmpty()) {
            Collections.addAll(cmds, JAVA_OPTS.trim().split("\\s+"));
        }
        cmds.add("-Djava.security.manager");
        cmds.add("-Djava.security.policy=" + TEST_SOURCES + FS + args[1]);
        cmds.add(args[0]);
        try {
            ProcessTools.executeCommand(cmds.toArray(new String[cmds.size()]))
                    .shouldHaveExitValue(0);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}

/**
 * Test for nested Subject.doAs() invocation:
 *
 * WriteToFileAction (CN=Duke principal) ->
 *      ReadFromFileAction (CN=Duke principal) ->
 *          ReadPropertyAction (CN=Duke principal)
 *
 * The test expects AccessControllException.
 */
class NestedActionsACE {

    public static void main(String args[]) {
        Subject subject = new Subject();
        subject.getPrincipals().add(new X500Principal("CN=Duke"));
        WriteToFileNegativeAction writeToFile
                = new WriteToFileNegativeAction(NestedActions.file);
        Subject.doAs(subject, writeToFile);
    }
}

/**
 * Test for nested Subject.doAs() invocation:
 *
 * WriteToFileAction (CN=Duke principal) ->
 *      ReadFromFileAction (CN=Duke principal) ->
 *          ReadPropertyAction (CN=Duke principal)
 *
 * The test expects PrivilegedActionException
 * that caused by AccessControlEception.
 */
class NestedActionsPAE {

    public static void main(String args[]) {
        Subject subject = new Subject();
        subject.getPrincipals().add(new X500Principal("CN=Duke"));
        try {
            WriteToFileExceptionAction writeToFile =
                    new WriteToFileExceptionAction(NestedActions.file);
            Subject.doAs(subject, writeToFile);
            throw new RuntimeException(
                    "Test failed: no PrivilegedActionException thrown");
        } catch (PrivilegedActionException pae) {
            System.out.println(
                    "PrivilegedActionException thrown as expected: "
                    + pae);

            // check if AccessControlException caused PrivilegedActionException
            Throwable exception = pae.getException();
            do {
                if (!(exception instanceof PrivilegedActionException)) {
                    break;
                }
                exception = ((PrivilegedActionException) exception).
                        getException();
            } while (true);

            if (!(exception instanceof ReadPropertyException)) {
                throw new RuntimeException(
                        "Test failed: PrivilegedActionException "
                        + "was not caused by ReadPropertyException");
            }

            exception = exception.getCause();
            if (!(exception instanceof AccessControlException)) {
                throw new RuntimeException(
                        "Test failed: PrivilegedActionException "
                        + "was not caused by ReadPropertyException");
            }

            System.out.println(
                    "Test passed: PrivilegedActionException "
                    + "was caused by AccessControlException");
        }
    }
}

/**
 * Test for nested Subject.doAs() invocation:
 *
 * WriteToFileAction (CN=Duke principal) ->
 *      ReadFromFileAction (CN=Duke principal) ->
 *          ReadPropertyAction (CN=Duke principal)
 */
class NestedActionsOnePrincipal {

    public static void main(String args[]) {
        Subject subject = new Subject();
        subject.getPrincipals().add(new X500Principal("CN=Duke"));
        WriteToFileAction writeToFile =
                new WriteToFileAction(NestedActions.file);
        Subject.doAs(subject, writeToFile);
    }
}

/**
 * Test for nested Subject.doAs() invocation:
 *
 * WriteToFileAction (CN=Duke principal) ->
 *      ReadFromFileAction (CN=Duke principal) ->
 *          ReadPropertyAction (CN=Java principal)
 */
class NestedActionsTwoPrincipals {

    public static void main(String args[]) {
        Subject subject = new Subject();
        subject.getPrincipals().add(new X500Principal("CN=Duke"));
        Subject anotherSubject = new Subject();
        anotherSubject.getPrincipals().add(new X500Principal("CN=Java"));
        ReadFromFileAction readFromFile
                = new ReadFromFileAction(NestedActions.file, anotherSubject);
        WriteToFileAction writeToFile
                = new WriteToFileAction(NestedActions.file, readFromFile);
        Subject.doAs(subject, writeToFile);
    }
}

/**
 * Helper class.
 */
class Utils {

    static void readFile(String filename) {
        System.out.println("ReadFromFileAction: try to read " + filename);
        AccessControlContext acc = AccessController.getContext();
        Subject subject = Subject.getSubject(acc);
        System.out.println("principals = " + subject.getPrincipals());
        try (FileInputStream fis = new FileInputStream(filename)) {
            // do nothing
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException", e);
        }
    }

    static void writeFile(String filename) {
        System.out.println("WriteToFileAction: try to write to " + filename);
        AccessControlContext acc = AccessController.getContext();
        Subject subject = Subject.getSubject(acc);
        System.out.println("principals = " + subject.getPrincipals());
        try (BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(filename))) {
            bos.write(0);
            bos.flush();
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException", e);
        }
    }

}

class WriteToFileAction implements PrivilegedAction {

    private final String filename;
    private final PrivilegedAction nextAction;

    WriteToFileAction(String filename, PrivilegedAction nextAction) {
        this.filename = filename;
        this.nextAction = nextAction;
    }

    WriteToFileAction(String filename) {
        this(filename, new ReadFromFileAction(filename));
    }

    @Override
    public Object run() {
        Utils.writeFile(filename);
        AccessControlContext acc = AccessController.getContext();
        Subject subject = Subject.getSubject(acc);
        return Subject.doAs(subject, nextAction);
    }

}

class ReadFromFileAction implements PrivilegedAction {

    private final String filename;
    private final Subject anotherSubject;

    ReadFromFileAction(String filename) {
        this(filename, null);
    }

    ReadFromFileAction(String filename, Subject anotherSubject) {
        this.filename = filename;
        this.anotherSubject = anotherSubject;
    }

    @Override
    public Object run() {
        Utils.readFile(filename);

        AccessControlContext acc = AccessController.getContext();
        Subject subject = Subject.getSubject(acc);
        ReadPropertyAction readProperty = new ReadPropertyAction();
        if (anotherSubject != null) {
            return Subject.doAs(anotherSubject, readProperty);
        } else {
            return Subject.doAs(subject, readProperty);
        }
    }

}

class ReadPropertyAction implements PrivilegedAction {

    @Override
    public java.lang.Object run() {
        System.out.println("ReadPropertyAction: "
                + "try to read 'java.class.path' property");

        AccessControlContext acc = AccessController.getContext();
        Subject s = Subject.getSubject(acc);
        System.out.println("principals = " + s.getPrincipals());
        System.out.println("java.class.path = "
                + System.getProperty("java.class.path"));

        return null;
    }

}

class WriteToFileNegativeAction implements PrivilegedAction {

    private final String filename;

    public WriteToFileNegativeAction(String filename) {
        this.filename = filename;
    }

    @Override
    public Object run() {
        AccessControlContext acc = AccessController.getContext();
        Subject subject = Subject.getSubject(acc);
        System.out.println("principals = " + subject.getPrincipals());

        try {
            Utils.writeFile(filename);
            new File(filename).delete();
            throw new RuntimeException(
                    "Test failed: no AccessControlException thrown");
        } catch (AccessControlException ace) {
            System.out.println(
                    "AccessControlException thrown as expected: "
                    + ace.getMessage());
        }

        ReadFromFileNegativeAction readFromFile
                = new ReadFromFileNegativeAction(filename);
        return Subject.doAs(subject, readFromFile);
    }

}

class ReadFromFileNegativeAction implements PrivilegedAction {

    private final String filename;

    public ReadFromFileNegativeAction(String filename) {
        this.filename = filename;
    }

    @Override
    public Object run() {
        AccessControlContext acc = AccessController.getContext();
        Subject subject = Subject.getSubject(acc);
        System.out.println("principals = " + subject.getPrincipals());

        try {
            Utils.readFile(filename);
            throw new RuntimeException(
                    "Test failed: no AccessControlException thrown");
        } catch (AccessControlException ace) {
            System.out.println(
                    "AccessControlException thrown as expected: "
                    + ace.getMessage());
        }

        ReadPropertyNegativeAction readProperty =
                new ReadPropertyNegativeAction();
        return Subject.doAs(subject, readProperty);
    }

}

class ReadPropertyNegativeAction implements PrivilegedAction {

    @Override
    public java.lang.Object run() {
        System.out.println("Try to read 'java.class.path' property");

        AccessControlContext acc = AccessController.getContext();
        Subject s = Subject.getSubject(acc);
        System.out.println("principals = " + s.getPrincipals());

        try {
            System.out.println("java.class.path = "
                    + System.getProperty("java.class.path"));
            throw new RuntimeException(
                    "Test failed: no AccessControlException thrown");
        } catch (AccessControlException ace) {
            System.out.println(
                    "AccessControlException thrown as expected: "
                    + ace.getMessage());
        }

        return null;
    }

}

class WriteToFileExceptionAction implements PrivilegedExceptionAction {

    private final String filename;

    WriteToFileExceptionAction(String filename) {
        this.filename = filename;
    }

    @Override
    public Object run() throws Exception {
        Utils.writeFile(filename);
        AccessControlContext acc = AccessController.getContext();
        Subject subject = Subject.getSubject(acc);
        ReadFromFileExceptionAction readFromFile =
                new ReadFromFileExceptionAction(filename);
        return Subject.doAs(subject, readFromFile);
    }

}

class ReadFromFileExceptionAction implements PrivilegedExceptionAction {

    private final String filename;

    ReadFromFileExceptionAction(String filename) {
        this.filename = filename;
    }

    @Override
    public Object run() throws Exception {
        Utils.readFile(filename);
        AccessControlContext acc = AccessController.getContext();
        Subject subject = Subject.getSubject(acc);
        ReadPropertyExceptionAction readProperty =
                new ReadPropertyExceptionAction();
        return Subject.doAs(subject, readProperty);
    }

}

class ReadPropertyExceptionAction implements PrivilegedExceptionAction {

    @Override
    public java.lang.Object run() throws Exception {
        System.out.println("Try to read 'java.class.path' property");

        AccessControlContext acc = AccessController.getContext();
        Subject s = Subject.getSubject(acc);
        System.out.println("principals = " + s.getPrincipals());

        try {
            System.out.println("java.class.path = "
                    + System.getProperty("java.class.path"));
            throw new RuntimeException(
                    "Test failed: no AccessControlException thrown");
        } catch (AccessControlException ace) {
            System.out.println(
                    "AccessControlException thrown as expected: "
                    + ace.getMessage());
            throw new ReadPropertyException(ace);
        }
    }

}

class ReadPropertyException extends Exception {

    ReadPropertyException(Throwable cause) {
        super(cause);
    }
}
