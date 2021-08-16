/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 *
 * Load string resources from file. Resources should be stored in
 * {@code name=value} format.
 *
 * @see org.netbeans.jemmy.BundleManager
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class Bundle extends Object {

    private Properties resources;

    /**
     * Bunble constructor.
     */
    public Bundle() {
        resources = new Properties();
    }

    /**
     * Loads resources from an input stream.
     *
     * @param stream Stream to load resources from.
     * @exception IOException
     */
    public void load(InputStream stream)
            throws IOException {
        resources.load(stream);
    }

    /**
     * Loads resources from a simple file.
     *
     * @param fileName Name of the file to load resources from.
     * @exception IOException
     * @exception FileNotFoundException
     */
    public void loadFromFile(String fileName)
            throws IOException, FileNotFoundException {
        try (FileInputStream fileInputStream = new FileInputStream(fileName)) {
            load(fileInputStream);
        }
    }

    /**
     * Loads resources from a file in a jar archive.
     *
     * @param fileName Name of the jar archive.
     * @param entryName ?enryName? Name of the file to load resources from.
     * @exception IOException
     * @exception FileNotFoundException
     */
    public void loadFromJar(String fileName, String entryName)
            throws IOException, FileNotFoundException {
        try (JarFile jFile = new JarFile(fileName);
                InputStream inputStream = jFile.getInputStream(jFile.getEntry(entryName))) {
            load(inputStream);
        }
    }

    /**
     * Loads resources from a file in a zip archive.
     *
     * @param fileName Name of the zip archive.
     * @param entryName ?enryName? Name of the file to load resources from.
     * @exception ZipException
     * @exception IOException
     * @exception FileNotFoundException
     */
    public void loadFromZip(String fileName, String entryName)
            throws IOException, FileNotFoundException, ZipException {
        try (ZipFile zFile = new ZipFile(fileName);
                InputStream inputStream = zFile.getInputStream(zFile.getEntry(entryName))) {
            load(inputStream);
        }
    }

    /**
     * Prints bundle contents.
     *
     * @param writer Writer to print data in.
     */
    public void print(PrintWriter writer) {
        Enumeration<Object> keys = resources.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            writer.println(key + "=" + getResource(key));
        }
    }

    /**
     * Prints bundle contents.
     *
     * @param stream Stream to print data in.
     */
    public void print(PrintStream stream) {
        print(new PrintWriter(stream));
    }

    /**
     * Gets resource by key.
     *
     * @param key Resource key
     * @return Resource value or null if resource was not found.
     */
    public String getResource(String key) {
        return resources.getProperty(key);
    }

}
