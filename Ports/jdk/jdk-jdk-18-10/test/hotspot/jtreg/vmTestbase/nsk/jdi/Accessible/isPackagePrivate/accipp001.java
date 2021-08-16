/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.Accessible.isPackagePrivate;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * This test checks if the method <code>isPackagePrivate()</code>
 * of the JDI interface <code>Accessible</code> works fine with
 * the <code>ArrayType</code> sub-interface.
 */
public class accipp001 extends Log {
    final static boolean MODE_VERBOSE = false;

    /** The main class names of the debugger & debugee applications. */
    private final static String
        thisClassName = "nsk.jdi.Accessible.isPackagePrivate.accipp001",
        debugeeName   = thisClassName + "a";


    static ArgumentHandler      argsHandler;
    private static Log  logHandler;


    /** Debugee's classes which status (private or public) is known. */
    private final static String knownClasses[][] = {
        {"boolean", "public"},
        {"byte"   , "public"},
        {"char"   , "public"},
        {"double" , "public"},
        {"float"  , "public"},
        {"int"    , "public"},
        {"long"   , "public"},
        {"short"  , "public"},

        {"java.lang.Boolean"  , "public"},
        {"java.lang.Byte"     , "public"},
        {"java.lang.Character", "public"},
        {"java.lang.Double"   , "public"},
        {"java.lang.Float"    , "public"},
        {"java.lang.Integer"  , "public"},
        {"java.lang.Long"     , "public"},
        {"java.lang.Short"    , "public"},
        {"java.lang.String"   , "public"},
        {"java.lang.Object"   , "public"},

        {thisClassName+"a", "public" },
        {thisClassName+"e", "package private"},

        {debugeeName+"$U", "private"        },
        {debugeeName+"$V", "protected"      },
        {debugeeName+"$W", "public"         },
        {debugeeName+"$P", "package private"}
    };

    /**
     * Re-call to <code>run(args,out)</code>, and exit with
     * either status 95 or 97 (JCK-like exit status).
     */
    public static void main (String args[]) {
        int exitCode = run(args,System.out);
        System.exit(exitCode + 95);
    }

    /**
     * JCK-like entry point to the test: perform testing, and
     * return exit code 0 (PASSED) or either 2 (FAILED).
     */
    public static int run (String args[], PrintStream out) {
        return new accipp001().runThis(args,out); // incarnate Log
    }

    /**
     * Non-static variant of the method <code>run(args,out)</code>
     * can use Log features.
     */
    private int runThis (String argv[], PrintStream out) {

        Debugee debugee;

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);


        if (argsHandler.verbose()) {
            debugee = binder.bindToDebugee(debugeeName + " -vbs");
        } else {
            debugee = binder.bindToDebugee(debugeeName);
        }

        IOPipe pipe     = new IOPipe(debugee);


        debugee.redirectStderr(out);
        debugee.resume();

        String line = pipe.readln();
        if (!line.equals("ready")) {
            logHandler.complain("# Cannot recognize debugee's signal: " + line);
            return 2;
        };

//        ReferenceType classes[] = debugee.classes();
//        for (int i=0; i<classes.length; i++) {
//            ReferenceType t = classes[i];
//            if (t.signature().startsWith("["))
//                logHandler.display(t.name() + ": " + t.isPackagePrivate());
//        };

        int errors = 0;
        for (int i=0; i<knownClasses.length; i++) {
            String basicName = knownClasses[i][0];
            for (int indirectionLevel=1; indirectionLevel<5; indirectionLevel++) {
                String brackets[] = {"", "[]", "[][]", "[][][]", "[][][][]"};
                String className = basicName + brackets[indirectionLevel];
                ReferenceType refType = debugee.classByName(className);
                if (refType == null) {
                    logHandler.complain("Could not find class: " + className);
                    errors++;
                    continue;
                };
                boolean isPackagePrivate =
                    !knownClasses[i][1].equals("private") &&
                    !knownClasses[i][1].equals("public") &&
                    !knownClasses[i][1].equals("protected");
                if (refType.isPackagePrivate() != isPackagePrivate) {
                    logHandler.complain("Class is not treated package private: "
                        + className);
                    errors++;
                    continue;
                };
                logHandler.display(className + ": " + knownClasses[i][1]);
            };
        };
        if (errors > 0) {
            logHandler.complain("Errors revealed: " + errors);
            return 2;
        };

        pipe.println("quit");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != 95) {
            logHandler.complain("Debugee's exit status=" + status);
            return 2;
        };

        logHandler.display("Passed");
        return 0;
    }
}
