/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.sjavac.comp;

import java.io.*;
import java.net.URI;
import java.nio.file.NoSuchFileException;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;

/**
 * The SmartFileObject will return an outputstream that cache the written data
 * and compare the new content with the old content on disk. Only if they differ,
 * will the file be updated.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class SmartFileObject implements JavaFileObject {

    JavaFileObject file;

    public SmartFileObject(JavaFileObject r) {
        file = r;
    }

    @Override
    public boolean equals(Object other) {
        return file.equals(other);
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    @DefinedBy(Api.COMPILER)
    public Kind getKind() {
        return file.getKind();
    }

    @DefinedBy(Api.COMPILER)
    public boolean isNameCompatible(String simpleName, Kind kind) {
        return file.isNameCompatible(simpleName, kind);
    }

    @DefinedBy(Api.COMPILER)
    public URI toUri() {
        return file.toUri();
    }

    @DefinedBy(Api.COMPILER)
    public String getName() {
        return file.getName();
    }

    @DefinedBy(Api.COMPILER)
    public InputStream openInputStream() throws IOException {
        return file.openInputStream();
    }

    @DefinedBy(Api.COMPILER)
    public OutputStream openOutputStream() throws IOException {
        return file.openOutputStream();
    }

    @DefinedBy(Api.COMPILER)
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return file.getCharContent(ignoreEncodingErrors);
    }

    static String lineseparator = System.getProperty("line.separator");

    @DefinedBy(Api.COMPILER)
    public Writer openWriter() throws IOException {
        StringBuilder s = new StringBuilder();
        try (BufferedReader r = new BufferedReader(file.openReader(true))) {
            while (r.ready()) {
                s.append(r.readLine()+lineseparator);
            }
        } catch (FileNotFoundException | NoSuchFileException e) {
            // Perfectly ok.
        }
        return new SmartWriter(file, s.toString(), file.getName());
    }

    @DefinedBy(Api.COMPILER)
    public long getLastModified() {
        return file.getLastModified();
    }

    @DefinedBy(Api.COMPILER)
    public boolean delete() {
        return file.delete();
    }

    @DefinedBy(Api.COMPILER)
    public Modifier getAccessLevel() {
        return file.getAccessLevel();
    }

    @DefinedBy(Api.COMPILER)
    public NestingKind getNestingKind() {
        return file.getNestingKind();
    }

    @DefinedBy(Api.COMPILER)
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        return file.openReader(ignoreEncodingErrors);
    }

}
