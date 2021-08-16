/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdb.hidden_class.hc001;

import java.io.File;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.nio.file.Files;
import java.nio.file.Paths;

import nsk.share.jdb.*;

/* Interface for tested hidden class to implement. */
interface HCInterf {
    void hcMethod();
}

/* Hidden class definition used to define tested hidden class
 * with lookup.defineHiddenClass. */
class HiddenClass implements HCInterf {
    static String hcField = null;

    private String getClassName() {
        return this.getClass().getName();
    }

    public void hcMethod() {
        hcField = getClassName();
        if (hcField.indexOf("HiddenClass") == -1) {
            throw new RuntimeException("Debuggee: Unexpected HiddenClass name: " + hcField);
        }
    }
}

/* This is debuggee aplication */
public class hc001a {
    static PrintStream log = null;
    static void logMsg(String msg) { log.println(msg); log.flush(); }

    static final String JAVA_CP = System.getProperty("java.class.path");
    static final String HC_NAME = HiddenClass.class.getName().replace(".", File.separator) + ".class";
    static final String HC_PATH = getClassPath(HiddenClass.class);
    static String hcName = null; // the debugger gets value of this field

    static String getClassPath(Class<?> klass) {
        String classPath = klass.getTypeName().replace(".", File.separator) + ".class";
        for (String path: JAVA_CP.split(File.pathSeparator)) {
            String fullClassPath = path + File.separator + classPath;
            if (new File(fullClassPath).exists()) {
                return fullClassPath;
            }
        }
        throw new RuntimeException("class path for " + klass.getName() + " not found");
    }

    public static void main(String args[]) throws Exception {
        // The Jdb framework uses stdout for commands reply,
        // so it is not usable for normal logging.
        // Use a separate log file for debuggee located in JTwork/scratch.
        log = new PrintStream("Debuggee.log");

        hc001a testApp = new hc001a();
        int status = testApp.runIt(args);
        System.exit(hc001.JCK_STATUS_BASE + status);
    }

    // This method is to hit a breakpoint at expected execution point.
    void emptyMethod() {}

    public int runIt(String args[]) throws Exception {
        JdbArgumentHandler argumentHandler = new JdbArgumentHandler(args);

        logMsg("Debuggee: runIt: started");
        logMsg("Debuggee: JAVA_CP: " + JAVA_CP);
        logMsg("Debuggee: HC_NAME: " + HC_NAME);
        logMsg("Debuggee: HC_PATH: " + HC_PATH);

        // Define tested hidden class.
        Class<?> hc = defineHiddenClass(HC_PATH);

        // A hidden class name has an unpredictable suffix at the end,
        // and so, can not be hard coded and known to debugger in advance.
        // Store the hidden class name in field, so the debugger can read it from there.
        hcName = hc.getName();
        logMsg("Debuggee: Defined HiddenClass: " + hcName);

        // It is impossible to use a hidden class name to define a variable,
        // so we use the interface which the tested hidden class implements.
        HCInterf hcObj = (HCInterf)hc.newInstance();
        logMsg("Debuggee: created an instance of a hidden class: " + hcName);

        // It is for debuuger to set a breakpoint at a well known execution point.
        logMsg("Debuggee: invoking emptyMethod to hit expected breakpoint");
        emptyMethod();

        // Invoke a hidden class method.
        logMsg("Debuggee: invoking a method of a hidden class: " + hcName);
        hcObj.hcMethod();

        logMsg("Debuggee: runIt finished");
        return hc001.PASSED;
    }

    static Class<?> defineHiddenClass(String classFileName) throws Exception {
        try {
            Lookup lookup = MethodHandles.lookup();
            byte[] bytes = Files.readAllBytes(Paths.get(classFileName));
            // The class name from class file is a normal binary name but the
            // defineHiddenClass appends a suffix "/<unqualified-name>" to it,
            // so it is not a valid binary name anymore.
            Class<?> hc = lookup.defineHiddenClass(bytes, false).lookupClass();
            return hc;
        } catch (Exception ex) {
            logMsg("Debuggee: defineHiddenClass: caught Exception " + ex.getMessage());
            ex.printStackTrace(log);
            log.flush();
            throw ex;
        }
    }
}
