/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

package testapp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;

public class Main {

    public static void main(String[] args) {
        Main o = new Main("testapp.some.class", null);
        o.launch(null);
    }

    private final  String uniqClassName;
    private final ConcurrentLinkedQueue<Throwable> problems;

    public Main(String uniq, ConcurrentLinkedQueue<Throwable> p) {
        uniqClassName = uniq;
        problems = p;
    }

    public void launch(HashMap<String, ImageInputStream> refs) {
        System.out.printf("%s: current context class loader: %s\n",
                          uniqClassName,
                Thread.currentThread().getContextClassLoader());
        try {
            byte[] data = new byte[1024];
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            MyImageInputStream iis = new MyImageInputStream(bais,
                                                            uniqClassName,
                                                            problems);
            if (refs != null) {
                System.out.printf("%s: added to strong store\n",
                                  uniqClassName);
                refs.put(uniqClassName, iis);
            }
            iis.read();
            //leave stream open : let's shutdown hook work!
        } catch (IOException e) {
            problems.add(e);
        }
    }

    private static class MyImageInputStream extends FileCacheImageInputStream {
        private final String uniqClassName;
        private ConcurrentLinkedQueue<Throwable> problems;
        public MyImageInputStream(InputStream is, String uniq,
                                  ConcurrentLinkedQueue<Throwable> p) throws IOException
   {
            super(is, new File("tmp"));
            uniqClassName = uniq;
            problems = p;
        }

        @Override
        public void close() throws IOException {
            Test t = new Test();
            try {
                t.doTest(uniqClassName);
            } catch (Throwable e) {
                problems.add(e);
            }

            super.close();

            problems = null;
        }
    }
}

class Test {
    public void doTest(String uniqClassName) throws ClassNotFoundException {
        System.out.printf("%s: Current thread: %s\n", uniqClassName,
                          Thread.currentThread());

        ClassLoader thisCL = this.getClass().getClassLoader();
        Class uniq = thisCL.loadClass(uniqClassName);

        System.out.printf("%s: test is done!\n",uniqClassName);
    }
}
