/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.classload;

import java.io.*;
import java.util.*;
import nsk.share.*;

/**
 * Classloader that generates classes on the fly.
 *
 * This classloader can load classes with name starting with 'Class'.
 * It will use nsk.share.classload.TemplateClass as template and will
 * replace class name in the bytecode of template class. It can be used
 * for example to detect memory leaks in class loading or to quickly
 * fill PermGen.
 */
public class GeneratingClassLoader extends ClassLoader {
        public static final String DEFAULT_CLASSNAME = TemplateClass.class.getName();
        public static final String PREFIX = "Class";

        private final String [] classPath;
        private byte[] bytecode;
        private int[] offsets;
        private final String encoding = "UTF8";
        private final String templateClassName;
        private final byte[] templateClassNameBytes;

        /**
         * Create generating class loader that will use class file
         * for given class from classpath as template.
         */
        public GeneratingClassLoader(String templateClassName) {
                this.templateClassName = templateClassName;
                classPath = System.getProperty("java.class.path").split(File.pathSeparator);
                try {
                        templateClassNameBytes = templateClassName.getBytes(encoding);
                } catch (UnsupportedEncodingException e) {
                        throw new TestBug(e);
                }
        }

        /**
         * Create generating class loader that will use class file
         * for nsk.share.classload.TemplateClass as template.
         */
        public GeneratingClassLoader() {
                this("nsk.share.classload.TemplateClass");
        }

        public int getNameLength() {
                return templateClassName.length();
        }

        public String getPrefix() {
                return PREFIX;
        }

        public String getClassName(int number) {
                StringBuffer sb = new StringBuffer();
                sb.append(PREFIX);
                sb.append(number);
                int n = templateClassName.length() - sb.length();
                for (int i = 0; i < n; ++i)
                        sb.append("_");
                return sb.toString();
        }

        public synchronized Class loadClass(String name) throws ClassNotFoundException {
                return loadClass(name, false);
        }

        public synchronized Class loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
                Class c = findLoadedClass(name);
                if (c != null)
                        return c;
                if (!name.startsWith(PREFIX))
                        return super.loadClass(name, resolve);
                if (name.length() != templateClassName.length())
                        throw new ClassNotFoundException("Only can load classes with name.length() = " + getNameLength() + " got: '" + name + "' length: " + name.length());
                byte[] bytecode = getPatchedByteCode(name);
                c = defineClass(name, bytecode, 0, bytecode.length);
                if (resolve)
                        resolveClass(c);
                return c;
        }

        private byte[] getPatchedByteCode(String name) throws ClassNotFoundException {
                try {
                        byte[] bytecode = getByteCode();
                        String fname = name.replace(".", File.separator);
                        byte[] replaceBytes = fname.getBytes(encoding);
                        for (int offset : offsets) {
                                for (int i = 0; i < replaceBytes.length; ++i)
                                        bytecode[offset + i] = replaceBytes[i];
                        }
                        return bytecode;
                } catch (UnsupportedEncodingException e) {
                        throw new TestBug(e);
                }
        }

        private byte[] getByteCode() throws ClassNotFoundException {
                if (bytecode == null)
                        readByteCode();
                if (offsets == null) {
                        getOffsets(bytecode);
                        if (offsets == null)
                                throw new TestBug("Class name not found in template class file");
                }
                return (byte[]) bytecode.clone();
        }

        private void readByteCode() throws ClassNotFoundException {
                String fname = templateClassName.replace(".", File.separator) + ".class";
                File target = null;
                for (int i = 0; i < classPath.length; ++i) {
                        target = new File(classPath[i] + File.separator + fname);
                        if (target.exists())
                                break;
                }

                if (target == null || !target.exists())
                        throw new ClassNotFoundException("File not found: " + target);
                try {
                        bytecode = FileUtils.readFile(target);
                } catch (IOException e) {
                        throw new ClassNotFoundException(templateClassName, e);
                }
        }

        private void getOffsets(byte[] bytecode) {
                List<Integer> offsets = new ArrayList<Integer>();
                if (this.offsets == null) {
                        String pname = templateClassName.replace(".", "/");
                        try {
                                byte[] pnameb = pname.getBytes(encoding);
                                int i = 0;
                                while (true) {
                                        while (i < bytecode.length) {
                                                int j = 0;
                                                while (j < pnameb.length && bytecode[i + j] == pnameb[j])
                                                        ++j;
                                                if (j == pnameb.length)
                                                        break;
                                                i++;
                                        }
                                        if (i == bytecode.length)
                                                break;
                                        offsets.add(Integer.valueOf(i));
                                        i++;
                                }
                        } catch (UnsupportedEncodingException e) {
                                throw new TestBug(e);
                        }
                        this.offsets = new int[offsets.size()];
                        for (int i = 0; i < offsets.size(); ++i)
                                this.offsets[i] = offsets.get(i).intValue();
                }
        }
}
