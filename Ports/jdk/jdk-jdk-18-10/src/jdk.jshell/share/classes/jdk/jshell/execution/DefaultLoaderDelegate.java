/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jshell.execution;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jdk.jshell.spi.ExecutionControl.ClassBytecodes;
import jdk.jshell.spi.ExecutionControl.ClassInstallException;
import jdk.jshell.spi.ExecutionControl.EngineTerminationException;
import jdk.jshell.spi.ExecutionControl.InternalException;

/**
 * The standard implementation of {@link LoaderDelegate} using
 * a {@link URLClassLoader}.
 *
 * @author Robert Field
 */
class DefaultLoaderDelegate implements LoaderDelegate {

    private final RemoteClassLoader loader;
    private final Map<String, Class<?>> klasses = new HashMap<>();

    private static class RemoteClassLoader extends URLClassLoader {

        private final Map<String, ClassFile> classFiles = new HashMap<>();

        RemoteClassLoader() {
            super(new URL[0]);
        }

        private class ResourceURLStreamHandler extends URLStreamHandler {

            private final String name;

            ResourceURLStreamHandler(String name) {
                this.name = name;
            }

            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return new URLConnection(u) {
                    private InputStream in;
                    private Map<String, List<String>> fields;
                    private List<String> fieldNames;

                    @Override
                    public void connect() {
                        if (connected) {
                            return;
                        }
                        connected = true;
                        ClassFile file = classFiles.get(name);
                        in = new ByteArrayInputStream(file.data);
                        fields = new LinkedHashMap<>();
                        fields.put("content-length", List.of(Integer.toString(file.data.length)));
                        Instant instant = new Date(file.timestamp).toInstant();
                        ZonedDateTime time = ZonedDateTime.ofInstant(instant, ZoneId.of("GMT"));
                        String timeStamp = DateTimeFormatter.RFC_1123_DATE_TIME.format(time);
                        fields.put("date", List.of(timeStamp));
                        fields.put("last-modified", List.of(timeStamp));
                        fieldNames = new ArrayList<>(fields.keySet());
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        connect();
                        return in;
                    }

                    @Override
                    public String getHeaderField(String name) {
                        connect();
                        return fields.getOrDefault(name, List.of())
                                     .stream()
                                     .findFirst()
                                     .orElse(null);
                    }

                    @Override
                    public Map<String, List<String>> getHeaderFields() {
                        connect();
                        return fields;
                    }

                    @Override
                    public String getHeaderFieldKey(int n) {
                        return n < fieldNames.size() ? fieldNames.get(n) : null;
                    }

                    @Override
                    public String getHeaderField(int n) {
                        String name = getHeaderFieldKey(n);

                        return name != null ? getHeaderField(name) : null;
                    }

                };
            }
        }

        void declare(String name, byte[] bytes) {
            classFiles.put(toResourceString(name), new ClassFile(bytes, System.currentTimeMillis()));
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            ClassFile file = classFiles.get(toResourceString(name));
            if (file == null) {
                return super.findClass(name);
            }
            return super.defineClass(name, file.data, 0, file.data.length, (CodeSource) null);
        }

        @Override
        public URL findResource(String name) {
            URL u = doFindResource(name);
            return u != null ? u : super.findResource(name);
        }

        @Override
        public Enumeration<URL> findResources(String name) throws IOException {
            URL u = doFindResource(name);
            Enumeration<URL> sup = super.findResources(name);

            if (u == null) {
                return sup;
            }

            List<URL> result = new ArrayList<>();

            while (sup.hasMoreElements()) {
                result.add(sup.nextElement());
            }

            result.add(u);

            return Collections.enumeration(result);
        }

        private URL doFindResource(String name) {
            if (classFiles.containsKey(name)) {
                try {
                    return new URL(null,
                                   new URI("jshell", null, "/" + name, null).toString(),
                                   new ResourceURLStreamHandler(name));
                } catch (MalformedURLException | URISyntaxException ex) {
                    throw new InternalError(ex);
                }
            }

            return null;
        }

        private String toResourceString(String className) {
            return className.replace('.', '/') + ".class";
        }

        @Override
        public void addURL(URL url) {
            super.addURL(url);
        }

        private static class ClassFile {
            public final byte[] data;
            public final long timestamp;

            ClassFile(byte[] data, long timestamp) {
                this.data = data;
                this.timestamp = timestamp;
            }

        }
    }

    public DefaultLoaderDelegate() {
        this.loader = new RemoteClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
    }

    @Override
    public void load(ClassBytecodes[] cbcs)
            throws ClassInstallException, EngineTerminationException {
        boolean[] loaded = new boolean[cbcs.length];
        try {
            for (ClassBytecodes cbc : cbcs) {
                loader.declare(cbc.name(), cbc.bytecodes());
            }
            for (int i = 0; i < cbcs.length; ++i) {
                ClassBytecodes cbc = cbcs[i];
                Class<?> klass = loader.loadClass(cbc.name());
                klasses.put(cbc.name(), klass);
                loaded[i] = true;
                // Get class loaded to the point of, at least, preparation
                klass.getDeclaredMethods();
            }
        } catch (Throwable ex) {
            throw new ClassInstallException("load: " + ex.getMessage(), loaded);
        }
    }

    @Override
    public void classesRedefined(ClassBytecodes[] cbcs) {
        for (ClassBytecodes cbc : cbcs) {
            loader.declare(cbc.name(), cbc.bytecodes());
        }
    }

    @Override
    public void addToClasspath(String cp)
            throws EngineTerminationException, InternalException {
        try {
            for (String path : cp.split(File.pathSeparator)) {
                loader.addURL(new File(path).toURI().toURL());
            }
        } catch (Exception ex) {
            throw new InternalException(ex.toString());
        }
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> klass = klasses.get(name);
        if (klass == null) {
            throw new ClassNotFoundException(name + " not found");
        } else {
            return klass;
        }
    }

}
