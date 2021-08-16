/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.MissingResourceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.DocumentationTool;
import javax.tools.FileObject;
import javax.tools.JavaFileManager.Location;
import javax.tools.StandardLocation;

import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;
import jdk.javadoc.internal.doclets.toolkit.Resources;

/**
 * Abstraction for handling files, which may be specified directly
 * (e.g. via a path on the command line) or relative to a Location.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 */
public abstract class DocFile {

    /** Create a DocFile for a directory. */
    public static DocFile createFileForDirectory(BaseConfiguration configuration, String file) {
        return DocFileFactory.getFactory(configuration).createFileForDirectory(file);
    }

    /** Create a DocFile for a file that will be opened for reading. */
    public static DocFile createFileForInput(BaseConfiguration configuration, String file) {
        return DocFileFactory.getFactory(configuration).createFileForInput(file);
    }

    /** Create a DocFile for a file that will be opened for reading. */
    public static DocFile createFileForInput(BaseConfiguration configuration, Path file) {
        return DocFileFactory.getFactory(configuration).createFileForInput(file);
    }

    /** Create a DocFile for a file that will be opened for writing. */
    public static DocFile createFileForOutput(BaseConfiguration configuration, DocPath path) {
        return DocFileFactory.getFactory(configuration).createFileForOutput(path);
    }

    /**
     * The location for this file. Maybe null if the file was created without
     * a location or path.
     */
    protected final Location location;

    /**
     * The path relative to the (output) location. Maybe null if the file was
     * created without a location or path.
     */
    protected final DocPath path;

    /**
     * List the directories and files found in subdirectories along the
     * elements of the given location.
     * @param configuration the doclet configuration
     * @param location currently, only {@link StandardLocation#SOURCE_PATH} is supported.
     * @param path the subdirectory of the directories of the location for which to
     *  list files
     */
    public static Iterable<DocFile> list(BaseConfiguration configuration, Location location, DocPath path) {
        return DocFileFactory.getFactory(configuration).list(location, path);
    }

    /** Create a DocFile without a location or path */
    protected DocFile() {
        this.location = null;
        this.path = null;
    }

    /** Create a DocFile for a given location and relative path. */
    protected DocFile(Location location, DocPath path) {
        this.location = location;
        this.path = path;
    }

    /**
     * Returns a file object for the file.
     * @return a file object
     */
    public abstract FileObject getFileObject();

    /**
     * Open an input stream for the file.
     *
     * @return an open input stream for the file
     * @throws DocFileIOException if there is a problem opening the stream
     */
    public abstract InputStream openInputStream() throws DocFileIOException;

    /**
     * Open an output stream for the file.
     * The file must have been created with a location of
     * {@link DocumentationTool.Location#DOCUMENTATION_OUTPUT}
     * and a corresponding relative path.
     *
     * @return an open output stream for the file
     * @throws DocFileIOException if there is a problem opening the stream
     * @throws UnsupportedEncodingException if the configured encoding is not supported
     */
    public abstract OutputStream openOutputStream() throws DocFileIOException, UnsupportedEncodingException;

    /**
     * Open an writer for the file, using the encoding (if any) given in the
     * doclet configuration.
     * The file must have been created with a location of
     * {@link DocumentationTool.Location#DOCUMENTATION_OUTPUT} and a corresponding relative path.
     *
     * @return an open output stream for the file
     * @throws DocFileIOException if there is a problem opening the stream
     * @throws UnsupportedEncodingException if the configured encoding is not supported
     */
    public abstract Writer openWriter() throws DocFileIOException, UnsupportedEncodingException;

    /**
     * Copy the contents of another file directly to this file.
     *
     * @param fromFile the file to be copied
     * @throws DocFileIOException if there is a problem file copying the file
     */
    public void copyFile(DocFile fromFile) throws DocFileIOException {
        try (OutputStream output = openOutputStream()) {
            try (InputStream input = fromFile.openInputStream()) {
                byte[] bytearr = new byte[1024];
                int len;
                while ((len = read(fromFile, input, bytearr)) != -1) {
                    write(this, output, bytearr, len);
                }
            } catch (IOException e) {
                throw new DocFileIOException(fromFile, DocFileIOException.Mode.READ, e);
            }
        } catch (IOException e) {
            throw new DocFileIOException(this, DocFileIOException.Mode.WRITE, e);
        }
    }

    /**
     * Copy the contents of a resource file to this file.
     *
     * @param resource the path of the resource, relative to the package of this class
     * @param overwrite whether or not to overwrite the file if it already exists
     * @param replaceNewLine if false, the file is copied as a binary file;
     *     if true, the file is written line by line, using the platform line
     *     separator
     *
     * @throws DocFileIOException if there is a problem while writing the copy
     * @throws ResourceIOException if there is a problem while reading the resource
     */
    public void copyResource(DocPath resource, boolean overwrite, boolean replaceNewLine)
            throws DocFileIOException, ResourceIOException {
        if (exists() && !overwrite)
            return;

        copyResource(resource, replaceNewLine, null);
    }

    /**
     * Copy the contents of a resource file to this file.
     *
     * @param resource the path of the resource, relative to the package of this class
     * @param resources if not {@code null}, substitute occurrences of {@code ##REPLACE:key##}
     *
     * @throws DocFileIOException if there is a problem while writing the copy
     * @throws ResourceIOException if there is a problem while reading the resource
     */
    public void copyResource(DocPath resource, Resources resources) throws DocFileIOException, ResourceIOException {
        copyResource(resource, true, resources);
    }

    private void copyResource(DocPath resource, boolean replaceNewLine, Resources resources)
                throws DocFileIOException, ResourceIOException {
        try {
            InputStream in = BaseConfiguration.class.getResourceAsStream(resource.getPath());
            if (in == null)
                return;

            try {
                if (replaceNewLine) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                        try (Writer writer = openWriter()) {
                            String line;
                            while ((line = readResourceLine(resource, reader)) != null) {
                                write(this, writer, resources == null ? line : localize(line, resources));
                                write(this, writer, DocletConstants.NL);
                            }
                        } catch (IOException e) {
                            throw new DocFileIOException(this, DocFileIOException.Mode.WRITE, e);
                        }
                    }
                } else {
                    try (OutputStream out = openOutputStream()) {
                        byte[] buf = new byte[2048];
                        int n;
                        while ((n = readResource(resource, in, buf)) > 0) {
                            write(this, out, buf, n);
                        }
                    } catch (IOException e) {
                        throw new DocFileIOException(this, DocFileIOException.Mode.WRITE, e);
                    }
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new ResourceIOException(resource, e);
        }
    }

    private static final Pattern replacePtn = Pattern.compile("##REPLACE:(?<key>[A-Za-z0-9._]+)##");

    private String localize(String line, Resources resources) {
        Matcher m = replacePtn.matcher(line);
        StringBuilder sb = null;
        int start = 0;
        while (m.find()) {
            if (sb == null) {
                sb = new StringBuilder();
            }
            sb.append(line, start, m.start());
            try {
                sb.append(resources.getText(m.group("key")));
            } catch (MissingResourceException e) {
                sb.append(m.group());
            }
            start = m.end();
        }
        if (sb == null) {
            return line;
        } else {
            sb.append(line.substring(start));
            return sb.toString();
        }
    }

    /** Return true if the file can be read. */
    public abstract boolean canRead();

    /** Return true if the file can be written. */
    public abstract boolean canWrite();

    /** Return true if the file exists. */
    public abstract boolean exists();

    /** Return the base name (last component) of the file name. */
    public abstract String getName();

    /** Return the file system path for this file. */
    public abstract String getPath();

    /** Return true if file has an absolute path name. */
    public abstract boolean isAbsolute();

    /** Return true if file identifies a directory. */
    public abstract boolean isDirectory();

    /** Return true if file identifies a file. */
    public abstract boolean isFile();

    /** Return true if this file is the same as another. */
    public abstract boolean isSameFile(DocFile other);

    /** If the file is a directory, list its contents.
     *
     * @return the contents of the directory
     * @throws DocFileIOException if there is a problem while listing the directory
     */
    public abstract Iterable<DocFile> list() throws DocFileIOException;

    /** Create the file as a directory, including any parent directories. */
    public abstract boolean mkdirs();

    /**
     * Derive a new file by resolving a relative path against this file.
     * The new file will inherit the configuration and location of this file
     * If this file has a path set, the new file will have a corresponding
     * new path.
     */
    public abstract DocFile resolve(DocPath p);

    /**
     * Derive a new file by resolving a relative path against this file.
     * The new file will inherit the configuration and location of this file
     * If this file has a path set, the new file will have a corresponding
     * new path.
     */
    public abstract DocFile resolve(String p);

    /**
     * Resolve a relative file against the given output location.
     * @param locn Currently, only
     * {@link DocumentationTool.Location#DOCUMENTATION_OUTPUT} is supported.
     */
    public abstract DocFile resolveAgainst(Location locn);


    /**
     * Reads from an input stream opened from a given file into a given buffer.
     * If an {@code IOException} occurs, it is wrapped in a {@code DocFileIOException}.
     *
     * @param inFile the file for the stream
     * @param input  the stream
     * @param buf    the buffer
     *
     * @return the number of bytes read, or -1 if at end of file
     * @throws DocFileIOException if an exception occurred while reading the stream
     */
    private static int read(DocFile inFile, InputStream input, byte[] buf) throws DocFileIOException {
        try {
            return input.read(buf);
        } catch (IOException e) {
            throw new DocFileIOException(inFile, DocFileIOException.Mode.READ, e);
        }
    }

    /**
     * Writes to an output stream for a given file from a given buffer.
     * If an {@code IOException} occurs, it is wrapped in a {@code DocFileIOException}.
     *
     * @param outFile the file for the stream
     * @param out     the stream
     * @param buf     the buffer
     *
     * @throws DocFileIOException if an exception occurred while writing the stream
     */
    private static void write(DocFile outFile, OutputStream out, byte[] buf, int len) throws DocFileIOException {
        try {
            out.write(buf, 0, len);
        } catch (IOException e) {
            throw new DocFileIOException(outFile, DocFileIOException.Mode.WRITE, e);
        }
    }

    /**
     * Writes text to an output stream for a given file from a given buffer.
     * If an {@code IOException} occurs, it is wrapped in a {@code DocFileIOException}.
     *
     * @param outFile the file for the stream
     * @param out     the stream
     * @param text    the text to be written
     *
     * @throws DocFileIOException if an exception occurred while writing the stream
     */
    private static void write(DocFile outFile, Writer out, String text) throws DocFileIOException {
        try {
            out.write(text);
        } catch (IOException e) {
            throw new DocFileIOException(outFile, DocFileIOException.Mode.WRITE, e);
        }
    }

    /**
     * Reads from an input stream opened from a given resource into a given buffer.
     * If an {@code IOException} occurs, it is wrapped in a {@code ResourceIOException}.
     *
     * @param docPath the resource for the stream
     * @param in      the stream
     * @param buf     the buffer
     *
     * @return the number of bytes read, or -1 if at end of file
     * @throws ResourceIOException if an exception occurred while reading the stream
     */
    private static int readResource(DocPath docPath, InputStream in, byte[] buf) throws ResourceIOException {
        try {
            return in.read(buf);
        } catch (IOException e) {
            throw new ResourceIOException(docPath, e);
        }
    }

    /**
     * Reads a line of characters from an input stream opened from a given resource.
     * If an {@code IOException} occurs, it is wrapped in a {@code ResourceIOException}.
     *
     * @param docPath the resource for the stream
     * @param in      the stream
     *
     * @return the line of text, or {@code null} if at end of stream
     * @throws ResourceIOException if an exception occurred while reading the stream
     */
    private static String readResourceLine(DocPath docPath, BufferedReader in) throws ResourceIOException {
        try {
            return in.readLine();
        } catch (IOException e) {
            throw new ResourceIOException(docPath, e);
        }
    }
}
