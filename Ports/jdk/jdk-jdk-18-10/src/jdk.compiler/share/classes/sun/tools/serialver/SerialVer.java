/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.tools.serialver;

import java.io.*;
import java.io.ObjectStreamClass;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.net.URLClassLoader;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Supporting class for the serialver tool.
 */
public class SerialVer {

    /*
     * A class loader that will load from the CLASSPATH environment
     * variable set by the user.
     */
    static URLClassLoader loader = null;

    /*
     * Create a URL class loader that will load classes from the
     * specified classpath.
     */
    static void initializeLoader(String cp) throws IOException {
        String[] paths = cp.split(File.pathSeparator);
        int count = paths.length;
        URL[] urls = new URL[count];
        for (int i = 0; i < count; i++) {
            urls[i] = Paths.get(paths[i]).toUri().toURL();
        }
        loader = new URLClassLoader(urls);
    }

    /*
     * From the classname find the serialVersionUID string formatted
     * for to be copied to a java class.
     */
    static String serialSyntax(String classname) throws ClassNotFoundException {
        String ret = null;
        boolean classFound = false;

        // If using old style of qualifying inner classes with '$'s.
        if (classname.indexOf('$') != -1) {
            ret = resolveClass(classname);
        } else {
            /* Try to resolve the fully qualified name and if that fails, start
             * replacing the '.'s with '$'s starting from the last '.', until
             * the class is resolved.
             */
            try {
                ret = resolveClass(classname);
                classFound = true;
            } catch (ClassNotFoundException e) {
                /* Class not found so far */
            }
            if (!classFound) {
                StringBuilder workBuffer = new StringBuilder(classname);
                String workName = workBuffer.toString();
                int i;
                while ((i = workName.lastIndexOf('.')) != -1 && !classFound) {
                    workBuffer.setCharAt(i, '$');
                    try {
                        workName = workBuffer.toString();
                        ret = resolveClass(workName);
                        classFound = true;
                    } catch (ClassNotFoundException e) {
                        /* Continue searching */
                    }
                }
            }
            if (!classFound) {
                throw new ClassNotFoundException();
            }
        }
        return ret;
    }

    static String resolveClass(String classname) throws ClassNotFoundException {
        Class<?> cl = Class.forName(classname, false, loader);
        ObjectStreamClass desc = ObjectStreamClass.lookup(cl);
        if (desc != null) {
            return "    private static final long serialVersionUID = " +
                desc.getSerialVersionUID() + "L;";
        } else {
            return null;
        }
    }

    /**
     * Entry point for serialver tool.
     * @param args the arguments
     */
    public static void main(String[] args) {
        String envcp = null;
        int i = 0;

        if (args.length == 0) {
            usage();
            System.exit(1);
        }

        for (i = 0; i < args.length; i++) {
            if (args[i].equals("-classpath")) {
                if ((i+1 == args.length) || args[i+1].startsWith("-")) {
                    System.err.println(Res.getText("error.missing.classpath"));
                    usage();
                    System.exit(1);
                }
                envcp = new String(args[i+1]);
                i++;
            }  else if (args[i].startsWith("-")) {
                System.err.println(Res.getText("invalid.flag", args[i]));
                usage();
                System.exit(1);
            } else {
                break;          // drop into processing class names
            }
        }


        /*
         * Get user's CLASSPATH environment variable, if the -classpath option
         * is not defined, and make a loader that can read from that path.
         */
        if (envcp == null) {
            envcp = System.getProperty("env.class.path");
            /*
             * If environment variable not set, add current directory to path.
             */
            if (envcp == null) {
                envcp = ".";
            }
        }

        try {
            initializeLoader(envcp);
        } catch (MalformedURLException mue) {
            System.err.println(Res.getText("error.parsing.classpath", envcp));
            System.exit(2);
        } catch (IOException ioe) {
            System.err.println(Res.getText("error.parsing.classpath", envcp));
            System.exit(3);
        }

        /*
         * Check if there are any class names specified
         */
        if (i == args.length) {
            usage();
            System.exit(1);
        }

        /*
         * The rest of the parameters are classnames.
         */
        boolean exitFlag = false;
        for (i = i; i < args.length; i++ ) {
            try {
                String syntax = serialSyntax(args[i]);
                if (syntax != null)
                    System.out.println(args[i] + ":" + syntax);
                else {
                    System.err.println(Res.getText("NotSerializable",
                        args[i]));
                    exitFlag = true;
                }
            } catch (ClassNotFoundException cnf) {
                System.err.println(Res.getText("ClassNotFound", args[i]));
                exitFlag = true;
            }
        }
        if (exitFlag) {
            System.exit(1);
        }
    }


    /**
     * Usage
     */
    public static void usage() {
        System.err.println(Res.getText("usage"));
    }

}

/**
 * Utility for integrating with serialver and for localization.
 * Handle Resources. Access to error and warning counts.
 * Message formatting.
 *
 * @see java.util.ResourceBundle
 * @see java.text.MessageFormat
 */
class Res {

    private static ResourceBundle messageRB;

    /**
     * Initialize ResourceBundle
     */
    static void initResource() {
        try {
            messageRB =
                ResourceBundle.getBundle("sun.tools.serialver.resources.serialver");
        } catch (MissingResourceException e) {
            throw new Error("Fatal: Resource for serialver is missing");
        }
    }

    /**
     * get and format message string from resource
     *
     * @param key selects message from resource
     */
    static String getText(String key) {
        return getText(key, (String)null);
    }

    /**
     * get and format message string from resource
     *
     * @param key selects message from resource
     * @param a1 first argument
     */
    static String getText(String key, String a1) {
        return getText(key, a1, null);
    }

    /**
     * get and format message string from resource
     *
     * @param key selects message from resource
     * @param a1 first argument
     * @param a2 second argument
     */
    static String getText(String key, String a1, String a2) {
        return getText(key, a1, a2, null);
    }

    /**
     * get and format message string from resource
     *
     * @param key selects message from resource
     * @param a1 first argument
     * @param a2 second argument
     * @param a3 third argument
     */
    static String getText(String key, String a1, String a2, String a3) {
        if (messageRB == null) {
            initResource();
        }
        try {
            String message = messageRB.getString(key);
            return MessageFormat.format(message, a1, a2, a3);
        } catch (MissingResourceException e) {
            throw new Error("Fatal: Resource for serialver is broken. There is no " + key + " key in resource.");
        }
    }
}
