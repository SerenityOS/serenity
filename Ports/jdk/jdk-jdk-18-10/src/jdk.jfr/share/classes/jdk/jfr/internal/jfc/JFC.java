/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal.jfc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jdk.jfr.Configuration;
import jdk.jfr.internal.LogLevel;
import jdk.jfr.internal.LogTag;
import jdk.jfr.internal.Logger;
import jdk.jfr.internal.SecuritySupport;
import jdk.jfr.internal.SecuritySupport.SafePath;

/**
 * {@link Configuration} factory for JFC files. *
 */
public final class JFC {
    private static final int BUFFER_SIZE = 8192;
    private static final int MAXIMUM_FILE_SIZE = 1024 * 1024;
    private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;
    private static volatile List<KnownConfiguration> knownConfigurations;

    /**
     * Reads a known configuration file (located into a string, but doesn't
     * parse it until it's being used.
     */
    private static final class KnownConfiguration {
        private final String content;
        private final String filename;
        private final String name;
        private Configuration configuration;

        public KnownConfiguration(SafePath knownPath) throws IOException {
            this.content = readContent(knownPath);
            this.name = nameFromPath(knownPath.toPath());
            this.filename = nullSafeFileName(knownPath.toPath());
        }

        public boolean isNamed(String name) {
            return filename.equals(name) || this.name.equals(name);
        }

        public Configuration getConfigurationFile() throws IOException, ParseException {
            if (configuration == null) {
                configuration = JFCParser.createConfiguration(name, content);
            }
            return configuration;
        }

        public String getName() {
            return name;
        }

        private static String readContent(SafePath knownPath) throws IOException {
            if (SecuritySupport.getFileSize(knownPath) > MAXIMUM_FILE_SIZE) {
                throw new IOException("Configuration with more than "
                        + MAXIMUM_FILE_SIZE + " characters can't be read.");
            }
            try (InputStream r = SecuritySupport.newFileInputStream(knownPath)) {
                return JFC.readContent(r);
            }
        }
    }

    private JFC() {
        // private utility class
    }

    /**
     * Reads a configuration from a file.
     *
     * @param path the file containing the configuration, not {@code null}
     * @return {@link Configuration}, not {@code null}
     * @throws ParseException if the file can't be parsed
     * @throws IOException if the file can't be read
     *
     * @throws SecurityException if a security manager exists and its
     *         {@code checkRead} method denies read access to the file
     * @see java.io.File#getPath()
     * @see java.lang.SecurityManager#checkRead(java.lang.String)
     */
    public static Configuration create(String name, Reader reader) throws IOException, ParseException {
        return JFCParser.createConfiguration(name, reader);
    }

    /**
     * Create a path to a .jfc file.
     * <p>
     * If the name is predefined name,
     * i.e. "default" or "profile.jfc", it will return the path for
     * the predefined path in the JDK.
     *
     * @param path textual representation of the path
     *
     * @return a safe path, not null
     */
    public static SafePath createSafePath(String path) {
        for (SafePath predefined : SecuritySupport.getPredefinedJFCFiles()) {
            try {
                String name = JFC.nameFromPath(predefined.toPath());
                if (name.equals(path) || (name + ".jfc").equals(path)) {
                    return predefined;
                }
            } catch (IOException e) {
                throw new InternalError("Error in predefined .jfc file", e);
            }
        }
        return new SafePath(path);
    }


    private static String nullSafeFileName(Path file) throws IOException {
        Path filename = file.getFileName();
        if (filename == null) {
            throw new IOException("Path has no file name");
        }
        return filename.toString();
    }

    public static String nameFromPath(Path file) throws IOException {
        String f = nullSafeFileName(file);
        if (f.endsWith(JFCParser.FILE_EXTENSION)) {
            return f.substring(0, f.length() - JFCParser.FILE_EXTENSION.length());
        } else  {
            return f;
        }
    }

    // Invoked by DCmdStart
    public static Configuration createKnown(String name) throws IOException, ParseException {
        // Known name, no need for permission
        for (KnownConfiguration known : getKnownConfigurations()) {
            if (known.isNamed(name)) {
                return known.getConfigurationFile();
            }
        }
        // Check JFC directory
        SafePath path = SecuritySupport.JFC_DIRECTORY;
        if (path != null && SecuritySupport.exists(path)) {
            for (String extension : Arrays.asList("", JFCParser.FILE_EXTENSION)) {
                SafePath file = new SafePath(path.toPath().resolveSibling(name + extension));
                if (SecuritySupport.exists(file) && !SecuritySupport.isDirectory(file)) {
                    try (Reader r = SecuritySupport.newFileReader(file)) {
                        String jfcName = nameFromPath(file.toPath());
                        return JFCParser.createConfiguration(jfcName, r);
                    }
                }
            }
        }

        // Assume path included in name

        Path localPath = Paths.get(name);
        String jfcName = nameFromPath(localPath);
        try (Reader r = Files.newBufferedReader(localPath)) {
            return JFCParser.createConfiguration(jfcName, r);
        }
    }

    private static String readContent(InputStream source) throws IOException {
        byte[] bytes = read(source, BUFFER_SIZE);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // copied from java.io.file.Files to avoid dependency on JDK 9 code
    private static byte[] read(InputStream source, int initialSize) throws IOException {
        int capacity = initialSize;
        byte[] buf = new byte[capacity];
        int nread = 0;
        int n;
        for (;;) {
            // read to EOF which may read more or less than initialSize (eg: file
            // is truncated while we are reading)
            while ((n = source.read(buf, nread, capacity - nread)) > 0)
                nread += n;

            // if last call to source.read() returned -1, we are done
            // otherwise, try to read one more byte; if that failed we're done too
            if (n < 0 || (n = source.read()) < 0)
                break;

            // one more byte was read; need to allocate a larger buffer
            if (capacity <= MAX_BUFFER_SIZE - capacity) {
                capacity = Math.max(capacity << 1, BUFFER_SIZE);
            } else {
                if (capacity == MAX_BUFFER_SIZE)
                    throw new OutOfMemoryError("Required array size too large");
                capacity = MAX_BUFFER_SIZE;
            }
            buf = Arrays.copyOf(buf, capacity);
            buf[nread++] = (byte)n;
        }
        return (capacity == nread) ? buf : Arrays.copyOf(buf, nread);
    }


    /**
     * Returns list of predefined configurations available.
     *
     * @return list of configurations, not null
     */
    public static List<Configuration> getConfigurations() {
        List<Configuration> configs = new ArrayList<>();
        for (KnownConfiguration knownConfig : getKnownConfigurations()) {
            try {
                configs.add(knownConfig.getConfigurationFile());
            } catch (IOException e) {
                Logger.log(LogTag.JFR, LogLevel.WARN, "Could not load configuration " + knownConfig.getName() + ". " + e.getMessage());
            } catch (ParseException e) {
                Logger.log(LogTag.JFR, LogLevel.WARN, "Could not parse configuration " + knownConfig.getName() + ". " + e.getMessage());
            }
        }
        return configs;
    }

    private static List<KnownConfiguration> getKnownConfigurations() {
        if (knownConfigurations == null) {
            List<KnownConfiguration> configProxies = new ArrayList<>();
            for (SafePath p : SecuritySupport.getPredefinedJFCFiles()) {
                try {
                    configProxies.add(new KnownConfiguration(p));
                } catch (IOException ioe) {
                    // ignore
                }
            }
            knownConfigurations = configProxies;
        }
        return knownConfigurations;
    }

    public static Configuration getPredefined(String name) throws IOException, ParseException {
        for (KnownConfiguration knownConfig : getKnownConfigurations()) {
            if (knownConfig.getName().equals(name)) {
                return knownConfig.getConfigurationFile();
            }
        }
        throw new NoSuchFileException("Could not locate configuration with name " + name);
    }
}
