/*
 * Copyright (c) 2005, 2007, Oracle and/or its affiliates. All rights reserved.
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
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

public abstract class JarTest {
    static String tmpdir = System.getProperty("java.io.tmpdir");
    static String javaCmd = System.getProperty("java.home") + File.separator +
            "bin" + File.separator + "java";

    /**
     * Reads an input stream into a byte array.
     */
    protected byte[] readFully(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[32];
        int count;

        while ((count = in.read(buffer)) >= 0) {
            out.write(buffer, 0, count);
        }

        return out.toByteArray();
    }

    /**
     * Copies the named resource into the directory specified.
     */
    protected File copyResource(File dir, String resName) throws Exception {
        BufferedOutputStream buffOut = null;
        FileOutputStream fileOut;
        InputStream in = null;
        File file = null;
        byte[] buffer;
        int count;

        file = new File(dir, resName);
        try {
            fileOut = new FileOutputStream(file);
            buffOut = new BufferedOutputStream(fileOut);
            in = getClass().getResourceAsStream(resName);
            buffer = new byte[1024];

            while ((count = in.read(buffer)) >= 0) {
                buffOut.write(buffer, 0, count);
            }
            buffOut.flush();
        } finally {
            if (buffOut != null) {
                try {
                    buffOut.close();
                } catch (IOException e) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }

        return file;
    }

    /**
     * Utility to create a temp dir.
     */
    protected File createTempDir() throws Exception {
        File result = new File(tmpdir + File.separator + getClass().getName());
        result.delete();
        result.mkdirs();

        return result;
    }

    /**
     * Utility to recursively delete a directory.
     */
    protected boolean deleteRecursively(File file) {
        File[] children;
        boolean result = true;

        children = file.listFiles();
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                result = result && deleteRecursively(children[i]);
            }
        }
        result = result && file.delete();

        return result;
    }

    static class Redirector implements Runnable {
        private BufferedReader reader;
        private PrintStream out;
        private boolean hasReadData;

        public Redirector(BufferedReader reader, PrintStream out) {
            this.reader = reader;
            this.out = out;
        }

        public void run() {
            String str;
            try {
                while ((str = reader.readLine()) != null) {
                    hasReadData = true;
                    out.println(str);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        public boolean getHasReadData() {
            return hasReadData;
        }
    }

}
