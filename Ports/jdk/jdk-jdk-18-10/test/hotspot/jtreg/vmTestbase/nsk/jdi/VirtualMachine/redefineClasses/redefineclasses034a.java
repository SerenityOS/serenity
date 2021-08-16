/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.VirtualMachine.redefineClasses;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import java.io.*;
import java.net.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 *  <code>redefineclasses034a</code> is deugee's part of the redefineclasses034.
 */
public class redefineclasses034a {
    public final static String brkpMethodName = "loadClass";
    public final static int brkpLineNumber = 72;

    static Object obj;
    static Class cls;
    static ClassLoader clsLoader[];
    static Log log;

    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(System.err, argHandler);

        String oldClassDir = argv[0] + File.separator + "loadclass";

        URL urls[] = new URL[1];
        clsLoader = new ClassLoader[redefineclasses034.TESTCASE_NUMBER];
        try {
            urls[0] = new File(oldClassDir).toURL();
        } catch (MalformedURLException e) {
            log.display("->" + e);
        }
        clsLoader[0] = new redefineclasses034aClassLoaderB(urls);
        clsLoader[1] = new redefineclasses034aClassLoaderC(oldClassDir);

        for (int i = 0; i < 2; i++) {
                loadClass(clsLoader[i],redefineclasses034.testedClassName[i]);
        }
        log.display("completed succesfully.");
        System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
    }

    private static void loadClass(ClassLoader clsLoader, String className) {
        try {
            cls = clsLoader.loadClass(className); // brkpLineNumber
            obj = cls.newInstance();
            log.display(className);
            log.display("====================================");
            log.display("class loader's info on debugee side:");
            log.display("====================================");
            log.display("custom class loader\t:" + cls.getClassLoader());
            log.display("default class loader\t:"
                            + Class.forName(redefineclasses034.debugeeName).getClassLoader());

            log.display("------------------------------------");
        } catch (Exception e) {
            log.display("" + e);
            e.printStackTrace();
        }
    }
}

class redefineclasses034aClassLoaderB extends URLClassLoader{

    redefineclasses034aClassLoaderB(URL [] urllst) {
        super(urllst);
    }
}

class redefineclasses034aClassLoaderC extends ClassLoader{

  private String classPath;

  redefineclasses034aClassLoaderC(String classPath) {
        super(redefineclasses034aClassLoaderC.class.getClassLoader());
        this.classPath = classPath;
    }

    protected Class findClass(String name) throws ClassNotFoundException {
        String classFileName = classPath + File.separator
                                    + name.replace('.', File.separatorChar)
                                    + ".class";
        FileInputStream in;
        try {
            in = new FileInputStream(classFileName);
            if (in == null) {
                throw new ClassNotFoundException(classFileName);
            }
        } catch (FileNotFoundException e) {
            throw new ClassNotFoundException(classFileName, e);
        }

        try {
            int len = in.available();
            byte[] data = new byte[len];
            for (int total = 0; total < data.length; ) {
                total += in.read(data, total, data.length - total);
            }
            return defineClass(name, data, 0, data.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(classFileName, e);
        } finally {
            try {
                in.close();
            } catch (Throwable e) {
            }
        }
    }
}
