/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6348193
 * @summary AS8.1 UR2 BAT test failure with "javac"
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 * @compile -proc:none T6348193.java
 * @run main/othervm -Djava.security.manager=allow T6348193
 */

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.tools.*;
import com.sun.tools.javac.api.JavacTool;

@SupportedAnnotationTypes({"*"})
public class T6348193 extends AbstractProcessor
{
    private static final boolean verbose = true;

    enum NoYes { NO, YES };
    enum NoGoodBad { NO, GOOD, BAD};

    public static final String myName = T6348193.class.getName();

    public static void main(String... args) throws IOException {
        if (System.getSecurityManager() != null)
            throw new AssertionError("unexpected security manager");

        for (NoYes secMgr: EnumSet.allOf(NoYes.class))
            for (NoGoodBad config: EnumSet.allOf(NoGoodBad.class))
                for (NoYes proc: EnumSet.allOf(NoYes.class))
                    test(secMgr, config, proc);
    }

    private static File processed = new File("processed");

    public static void test(NoYes secMgr, NoGoodBad config, NoYes proc) throws IOException {
        if (verbose)
            System.err.println("secMgr:" + secMgr + " config:" + config + " proc:" + proc);

        if (secMgr == NoYes.YES && System.getSecurityManager() == null)
            System.setSecurityManager(new NoLoaderSecurityManager());

        installConfigFile(config);

        processed.delete();

        List<String> args = new ArrayList<String>();
        //args.add("-XprintRounds");
        if (proc == NoYes.YES) {
            args.add("-processor");
            args.add(myName);
        }
        args.add("-processorpath");
        args.add(System.getProperty("java.class.path"));
        args.add("-d");
        args.add(".");

        JavacTool t = JavacTool.create(); // avoid using class loader

        MyDiagListener dl = new MyDiagListener();
        PrintWriter out = new PrintWriter(System.err, true);
        try (StandardJavaFileManager fm = t.getStandardFileManager(dl, null, null)) {
            File file = new File(System.getProperty("test.src"), myName+".java");
            Iterable<? extends JavaFileObject> files =
                fm.getJavaFileObjectsFromFiles(Arrays.asList(file));
            boolean ok = t.getTask(out, null, dl, args, null, files).call();

            if (config == NoGoodBad.GOOD || proc == NoYes.YES) {
                if (secMgr == NoYes.YES) {
                    if (dl.last == null)
                        throw new AssertionError("Security manager installed, and processors present, "
                                                 + " but no diagnostic received");
                }
                else {
                    if (!processed.exists())
                        throw new AssertionError("No security manager installed, and processors present, "
                                                 + " but no processing occurred");
                }
            }
            else if (config == NoGoodBad.BAD) {
                // TODO: should verify that no compiler crash occurred
                // needs revised JSR199 spec
            }
            else {
                if (processed.exists())
                    throw new AssertionError("No processors present, but processing occurred!");
            }

            if (verbose)
                System.err.println("OK");
        }
    }

    // set up or remove a service configuration file
    static void installConfigFile(NoGoodBad type) throws IOException {
        File f = new File(System.getProperty("test.classes", "."));
        for (String s: new String[] { "META-INF", "services", Processor.class.getName() })
            f = new File(f, s);
        BufferedWriter out;
        switch (type) {
        case GOOD:
            f.getParentFile().mkdirs();
            out = new BufferedWriter(new FileWriter(f));
            out.write(myName);
            out.newLine();
            out.close();
            break;
        case BAD:
            f.getParentFile().mkdirs();
            out = new BufferedWriter(new FileWriter(f));
            out.write("This is not a valid line");
            out.newLine();
            out.close();
            break;
        case NO:
            f.delete();
        }


    }

    // annotation processor method
    public boolean process(Set<? extends TypeElement> tes, RoundEnvironment renv )
    {
        try {
            // touch a file to indicate we have run
            new FileWriter(processed).close();
        } catch (IOException e) {
        }
        return true;
    }

    static class MyDiagListener implements DiagnosticListener<JavaFileObject>
    {
        public void report(Diagnostic<? extends JavaFileObject> message) {
            if (verbose)
                System.err.println(message);
            last = message;
        }

        Diagnostic<? extends JavaFileObject> last;
    }

    static class NoLoaderSecurityManager extends SecurityManager
    {
        public void checkCreateClassLoader() {
            throw new SecurityException("Not today, thanks you!");
        }

        public void checkPropertyAccess(String key) { /*OK*/ }

        public void checkDelete(String file) { /*OK*/ }
        public void checkPermission(Permission perm) { /*OK*/ }
        public void checkRead(FileDescriptor fd) { /*OK*/ }
        public void checkRead(String file) { /*OK*/ }
        public void checkRead(String file, Object context) { /*OK*/ }
        public void checkWrite(String file) { /*OK*/ }

    }
}
