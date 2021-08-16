/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.loader;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.Permission;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.zip.ZipFile;

import jdk.internal.access.JavaNetURLAccess;
import jdk.internal.access.JavaUtilZipFileAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.util.jar.InvalidJarIndexError;
import jdk.internal.util.jar.JarIndex;
import sun.net.util.URLUtil;
import sun.net.www.ParseUtil;
import sun.security.action.GetPropertyAction;

/**
 * This class is used to maintain a search path of URLs for loading classes
 * and resources from both JAR files and directories.
 *
 * @author  David Connelly
 */
public class URLClassPath {
    private static final String USER_AGENT_JAVA_VERSION = "UA-Java-Version";
    private static final String JAVA_VERSION;
    private static final boolean DEBUG;
    private static final boolean DISABLE_JAR_CHECKING;
    private static final boolean DISABLE_ACC_CHECKING;
    private static final boolean DISABLE_CP_URL_CHECK;
    private static final boolean DEBUG_CP_URL_CHECK;

    static {
        Properties props = GetPropertyAction.privilegedGetProperties();
        JAVA_VERSION = props.getProperty("java.version");
        DEBUG = (props.getProperty("sun.misc.URLClassPath.debug") != null);
        String p = props.getProperty("sun.misc.URLClassPath.disableJarChecking");
        DISABLE_JAR_CHECKING = p != null ? p.equals("true") || p.isEmpty() : false;

        p = props.getProperty("jdk.net.URLClassPath.disableRestrictedPermissions");
        DISABLE_ACC_CHECKING = p != null ? p.equals("true") || p.isEmpty() : false;

        // This property will be removed in a later release
        p = props.getProperty("jdk.net.URLClassPath.disableClassPathURLCheck");
        DISABLE_CP_URL_CHECK = p != null ? p.equals("true") || p.isEmpty() : false;

        // Print a message for each Class-Path entry that is ignored (assuming
        // the check is not disabled).
        p = props.getProperty("jdk.net.URLClassPath.showIgnoredClassPathEntries");
        DEBUG_CP_URL_CHECK = p != null ? p.equals("true") || p.isEmpty() : false;
    }

    /* The original search path of URLs. */
    private final ArrayList<URL> path;

    /* The deque of unopened URLs */
    private final ArrayDeque<URL> unopenedUrls;

    /* The resulting search path of Loaders */
    private final ArrayList<Loader> loaders = new ArrayList<>();

    /* Map of each URL opened to its corresponding Loader */
    private final HashMap<String, Loader> lmap = new HashMap<>();

    /* The jar protocol handler to use when creating new URLs */
    private final URLStreamHandler jarHandler;

    /* Whether this URLClassLoader has been closed yet */
    private boolean closed = false;

    /* The context to be used when loading classes and resources.  If non-null
     * this is the context that was captured during the creation of the
     * URLClassLoader. null implies no additional security restrictions. */
    @SuppressWarnings("removal")
    private final AccessControlContext acc;

    /**
     * Creates a new URLClassPath for the given URLs. The URLs will be
     * searched in the order specified for classes and resources. A URL
     * ending with a '/' is assumed to refer to a directory. Otherwise,
     * the URL is assumed to refer to a JAR file.
     *
     * @param urls the directory and JAR file URLs to search for classes
     *        and resources
     * @param factory the URLStreamHandlerFactory to use when creating new URLs
     * @param acc the context to be used when loading classes and resources, may
     *            be null
     */
    public URLClassPath(URL[] urls,
                        URLStreamHandlerFactory factory,
                        @SuppressWarnings("removal") AccessControlContext acc) {
        ArrayList<URL> path = new ArrayList<>(urls.length);
        ArrayDeque<URL> unopenedUrls = new ArrayDeque<>(urls.length);
        for (URL url : urls) {
            path.add(url);
            unopenedUrls.add(url);
        }
        this.path = path;
        this.unopenedUrls = unopenedUrls;

        if (factory != null) {
            jarHandler = factory.createURLStreamHandler("jar");
        } else {
            jarHandler = null;
        }
        if (DISABLE_ACC_CHECKING)
            this.acc = null;
        else
            this.acc = acc;
    }

    public URLClassPath(URL[] urls, @SuppressWarnings("removal") AccessControlContext acc) {
        this(urls, null, acc);
    }

    /**
     * Constructs a URLClassPath from a class path string.
     *
     * @param cp the class path string
     * @param skipEmptyElements indicates if empty elements are ignored or
     *        treated as the current working directory
     *
     * @apiNote Used to create the application class path.
     */
    URLClassPath(String cp, boolean skipEmptyElements) {
        ArrayList<URL> path = new ArrayList<>();
        if (cp != null) {
            // map each element of class path to a file URL
            int off = 0, next;
            do {
                next = cp.indexOf(File.pathSeparator, off);
                String element = (next == -1)
                    ? cp.substring(off)
                    : cp.substring(off, next);
                if (!element.isEmpty() || !skipEmptyElements) {
                    URL url = toFileURL(element);
                    if (url != null) path.add(url);
                }
                off = next + 1;
            } while (next != -1);
        }

        // can't use ArrayDeque#addAll or new ArrayDeque(Collection);
        // it's too early in the bootstrap to trigger use of lambdas
        int size = path.size();
        ArrayDeque<URL> unopenedUrls = new ArrayDeque<>(size);
        for (int i = 0; i < size; i++)
            unopenedUrls.add(path.get(i));

        this.unopenedUrls = unopenedUrls;
        this.path = path;
        this.jarHandler = null;
        this.acc = null;
    }

    public synchronized List<IOException> closeLoaders() {
        if (closed) {
            return Collections.emptyList();
        }
        List<IOException> result = new ArrayList<>();
        for (Loader loader : loaders) {
            try {
                loader.close();
            } catch (IOException e) {
                result.add(e);
            }
        }
        closed = true;
        return result;
    }

    /**
     * Appends the specified URL to the search path of directory and JAR
     * file URLs from which to load classes and resources.
     * <p>
     * If the URL specified is null or is already in the list of
     * URLs, then invoking this method has no effect.
     */
    public synchronized void addURL(URL url) {
        if (closed || url == null)
            return;
        synchronized (unopenedUrls) {
            if (! path.contains(url)) {
                unopenedUrls.addLast(url);
                path.add(url);
            }
        }
    }

    /**
     * Appends the specified file path as a file URL to the search path.
     */
    public void addFile(String s) {
        URL url = toFileURL(s);
        if (url != null) {
            addURL(url);
        }
    }

    /**
     * Returns a file URL for the given file path.
     */
    private static URL toFileURL(String s) {
        try {
            File f = new File(s).getCanonicalFile();
            return ParseUtil.fileToEncodedURL(f);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Returns the original search path of URLs.
     */
    public URL[] getURLs() {
        synchronized (unopenedUrls) {
            return path.toArray(new URL[0]);
        }
    }

    /**
     * Finds the resource with the specified name on the URL search path
     * or null if not found or security check fails.
     *
     * @param name      the name of the resource
     * @param check     whether to perform a security check
     * @return a {@code URL} for the resource, or {@code null}
     * if the resource could not be found.
     */
    public URL findResource(String name, boolean check) {
        Loader loader;
        for (int i = 0; (loader = getLoader(i)) != null; i++) {
            URL url = loader.findResource(name, check);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    /**
     * Finds the first Resource on the URL search path which has the specified
     * name. Returns null if no Resource could be found.
     *
     * @param name the name of the Resource
     * @param check     whether to perform a security check
     * @return the Resource, or null if not found
     */
    public Resource getResource(String name, boolean check) {
        if (DEBUG) {
            System.err.println("URLClassPath.getResource(\"" + name + "\")");
        }

        Loader loader;
        for (int i = 0; (loader = getLoader(i)) != null; i++) {
            Resource res = loader.getResource(name, check);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    /**
     * Finds all resources on the URL search path with the given name.
     * Returns an enumeration of the URL objects.
     *
     * @param name the resource name
     * @return an Enumeration of all the urls having the specified name
     */
    public Enumeration<URL> findResources(final String name,
                                     final boolean check) {
        return new Enumeration<>() {
            private int index = 0;
            private URL url = null;

            private boolean next() {
                if (url != null) {
                    return true;
                } else {
                    Loader loader;
                    while ((loader = getLoader(index++)) != null) {
                        url = loader.findResource(name, check);
                        if (url != null) {
                            return true;
                        }
                    }
                    return false;
                }
            }

            public boolean hasMoreElements() {
                return next();
            }

            public URL nextElement() {
                if (!next()) {
                    throw new NoSuchElementException();
                }
                URL u = url;
                url = null;
                return u;
            }
        };
    }

    public Resource getResource(String name) {
        return getResource(name, true);
    }

    /**
     * Finds all resources on the URL search path with the given name.
     * Returns an enumeration of the Resource objects.
     *
     * @param name the resource name
     * @return an Enumeration of all the resources having the specified name
     */
    public Enumeration<Resource> getResources(final String name,
                                    final boolean check) {
        return new Enumeration<>() {
            private int index = 0;
            private Resource res = null;

            private boolean next() {
                if (res != null) {
                    return true;
                } else {
                    Loader loader;
                    while ((loader = getLoader(index++)) != null) {
                        res = loader.getResource(name, check);
                        if (res != null) {
                            return true;
                        }
                    }
                    return false;
                }
            }

            public boolean hasMoreElements() {
                return next();
            }

            public Resource nextElement() {
                if (!next()) {
                    throw new NoSuchElementException();
                }
                Resource r = res;
                res = null;
                return r;
            }
        };
    }

    public Enumeration<Resource> getResources(final String name) {
        return getResources(name, true);
    }

    /*
     * Returns the Loader at the specified position in the URL search
     * path. The URLs are opened and expanded as needed. Returns null
     * if the specified index is out of range.
     */
    private synchronized Loader getLoader(int index) {
        if (closed) {
            return null;
        }
        // Expand URL search path until the request can be satisfied
        // or unopenedUrls is exhausted.
        while (loaders.size() < index + 1) {
            final URL url;
            synchronized (unopenedUrls) {
                url = unopenedUrls.pollFirst();
                if (url == null)
                    return null;
            }
            // Skip this URL if it already has a Loader. (Loader
            // may be null in the case where URL has not been opened
            // but is referenced by a JAR index.)
            String urlNoFragString = URLUtil.urlNoFragString(url);
            if (lmap.containsKey(urlNoFragString)) {
                continue;
            }
            // Otherwise, create a new Loader for the URL.
            Loader loader;
            try {
                loader = getLoader(url);
                // If the loader defines a local class path then add the
                // URLs as the next URLs to be opened.
                URL[] urls = loader.getClassPath();
                if (urls != null) {
                    push(urls);
                }
            } catch (IOException e) {
                // Silently ignore for now...
                continue;
            } catch (SecurityException se) {
                // Always silently ignore. The context, if there is one, that
                // this URLClassPath was given during construction will never
                // have permission to access the URL.
                if (DEBUG) {
                    System.err.println("Failed to access " + url + ", " + se );
                }
                continue;
            }
            // Finally, add the Loader to the search path.
            loaders.add(loader);
            lmap.put(urlNoFragString, loader);
        }
        return loaders.get(index);
    }

    /*
     * Returns the Loader for the specified base URL.
     */
    @SuppressWarnings("removal")
    private Loader getLoader(final URL url) throws IOException {
        try {
            return AccessController.doPrivileged(
                    new PrivilegedExceptionAction<>() {
                        public Loader run() throws IOException {
                            String protocol = url.getProtocol();  // lower cased in URL
                            String file = url.getFile();
                            if (file != null && file.endsWith("/")) {
                                if ("file".equals(protocol)) {
                                    return new FileLoader(url);
                                } else if ("jar".equals(protocol) &&
                                        isDefaultJarHandler(url) &&
                                        file.endsWith("!/")) {
                                    // extract the nested URL
                                    URL nestedUrl = new URL(file.substring(0, file.length() - 2));
                                    return new JarLoader(nestedUrl, jarHandler, lmap, acc);
                                } else {
                                    return new Loader(url);
                                }
                            } else {
                                return new JarLoader(url, jarHandler, lmap, acc);
                            }
                        }
                    }, acc);
        } catch (PrivilegedActionException pae) {
            throw (IOException)pae.getException();
        }
    }

    private static final JavaNetURLAccess JNUA
            = SharedSecrets.getJavaNetURLAccess();

    private static boolean isDefaultJarHandler(URL u) {
        URLStreamHandler h = JNUA.getHandler(u);
        return h instanceof sun.net.www.protocol.jar.Handler;
    }

    /*
     * Pushes the specified URLs onto the head of unopened URLs.
     */
    private void push(URL[] urls) {
        synchronized (unopenedUrls) {
            for (int i = urls.length - 1; i >= 0; --i) {
                unopenedUrls.addFirst(urls[i]);
            }
        }
    }

    /*
     * Checks whether the resource URL should be returned.
     * Returns null on security check failure.
     * Called by java.net.URLClassLoader.
     */
    public static URL checkURL(URL url) {
        if (url != null) {
            try {
                check(url);
            } catch (Exception e) {
                return null;
            }
        }
        return url;
    }

    /*
     * Checks whether the resource URL should be returned.
     * Throws exception on failure.
     * Called internally within this file.
     */
    public static void check(URL url) throws IOException {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            URLConnection urlConnection = url.openConnection();
            Permission perm = urlConnection.getPermission();
            if (perm != null) {
                try {
                    security.checkPermission(perm);
                } catch (SecurityException se) {
                    // fallback to checkRead/checkConnect for pre 1.2
                    // security managers
                    if ((perm instanceof java.io.FilePermission) &&
                        perm.getActions().indexOf("read") != -1) {
                        security.checkRead(perm.getName());
                    } else if ((perm instanceof
                        java.net.SocketPermission) &&
                        perm.getActions().indexOf("connect") != -1) {
                        URL locUrl = url;
                        if (urlConnection instanceof JarURLConnection) {
                            locUrl = ((JarURLConnection)urlConnection).getJarFileURL();
                        }
                        security.checkConnect(locUrl.getHost(),
                                              locUrl.getPort());
                    } else {
                        throw se;
                    }
                }
            }
        }
    }

    /**
     * Nested class used to represent a loader of resources and classes
     * from a base URL.
     */
    private static class Loader implements Closeable {
        private final URL base;
        private JarFile jarfile; // if this points to a jar file

        /*
         * Creates a new Loader for the specified URL.
         */
        Loader(URL url) {
            base = url;
        }

        /*
         * Returns the base URL for this Loader.
         */
        URL getBaseURL() {
            return base;
        }

        URL findResource(final String name, boolean check) {
            URL url;
            try {
                url = new URL(base, ParseUtil.encodePath(name, false));
            } catch (MalformedURLException e) {
                return null;
            }

            try {
                if (check) {
                    URLClassPath.check(url);
                }

                /*
                 * For a HTTP connection we use the HEAD method to
                 * check if the resource exists.
                 */
                URLConnection uc = url.openConnection();
                if (uc instanceof HttpURLConnection) {
                    HttpURLConnection hconn = (HttpURLConnection)uc;
                    hconn.setRequestMethod("HEAD");
                    if (hconn.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                        return null;
                    }
                } else {
                    // our best guess for the other cases
                    uc.setUseCaches(false);
                    InputStream is = uc.getInputStream();
                    is.close();
                }
                return url;
            } catch (Exception e) {
                return null;
            }
        }

        Resource getResource(final String name, boolean check) {
            final URL url;
            try {
                url = new URL(base, ParseUtil.encodePath(name, false));
            } catch (MalformedURLException e) {
                return null;
            }
            final URLConnection uc;
            try {
                if (check) {
                    URLClassPath.check(url);
                }
                uc = url.openConnection();

                if (uc instanceof JarURLConnection) {
                    /* Need to remember the jar file so it can be closed
                     * in a hurry.
                     */
                    JarURLConnection juc = (JarURLConnection)uc;
                    jarfile = JarLoader.checkJar(juc.getJarFile());
                }

                InputStream in = uc.getInputStream();
            } catch (Exception e) {
                return null;
            }
            return new Resource() {
                public String getName() { return name; }
                public URL getURL() { return url; }
                public URL getCodeSourceURL() { return base; }
                public InputStream getInputStream() throws IOException {
                    return uc.getInputStream();
                }
                public int getContentLength() throws IOException {
                    return uc.getContentLength();
                }
            };
        }

        /*
         * Returns the Resource for the specified name, or null if not
         * found or the caller does not have the permission to get the
         * resource.
         */
        Resource getResource(final String name) {
            return getResource(name, true);
        }

        /*
         * Closes this loader and release all resources.
         * Method overridden in sub-classes.
         */
        @Override
        public void close() throws IOException {
            if (jarfile != null) {
                jarfile.close();
            }
        }

        /*
         * Returns the local class path for this loader, or null if none.
         */
        URL[] getClassPath() throws IOException {
            return null;
        }
    }

    /*
     * Nested class used to represent a Loader of resources from a JAR URL.
     */
    private static class JarLoader extends Loader {
        private JarFile jar;
        private final URL csu;
        private JarIndex index;
        private URLStreamHandler handler;
        private final HashMap<String, Loader> lmap;
        @SuppressWarnings("removal")
        private final AccessControlContext acc;
        private boolean closed = false;
        private static final JavaUtilZipFileAccess zipAccess =
                SharedSecrets.getJavaUtilZipFileAccess();

        /*
         * Creates a new JarLoader for the specified URL referring to
         * a JAR file.
         */
        private JarLoader(URL url, URLStreamHandler jarHandler,
                          HashMap<String, Loader> loaderMap,
                          @SuppressWarnings("removal") AccessControlContext acc)
            throws IOException
        {
            super(new URL("jar", "", -1, url + "!/", jarHandler));
            csu = url;
            handler = jarHandler;
            lmap = loaderMap;
            this.acc = acc;

            ensureOpen();
        }

        @Override
        public void close () throws IOException {
            // closing is synchronized at higher level
            if (!closed) {
                closed = true;
                // in case not already open.
                ensureOpen();
                jar.close();
            }
        }

        JarFile getJarFile () {
            return jar;
        }

        private boolean isOptimizable(URL url) {
            return "file".equals(url.getProtocol());
        }

        @SuppressWarnings("removal")
        private void ensureOpen() throws IOException {
            if (jar == null) {
                try {
                    AccessController.doPrivileged(
                        new PrivilegedExceptionAction<>() {
                            public Void run() throws IOException {
                                if (DEBUG) {
                                    System.err.println("Opening " + csu);
                                    Thread.dumpStack();
                                }

                                jar = getJarFile(csu);
                                index = JarIndex.getJarIndex(jar);
                                if (index != null) {
                                    String[] jarfiles = index.getJarFiles();
                                // Add all the dependent URLs to the lmap so that loaders
                                // will not be created for them by URLClassPath.getLoader(int)
                                // if the same URL occurs later on the main class path.  We set
                                // Loader to null here to avoid creating a Loader for each
                                // URL until we actually need to try to load something from them.
                                    for (int i = 0; i < jarfiles.length; i++) {
                                        try {
                                            URL jarURL = new URL(csu, jarfiles[i]);
                                            // If a non-null loader already exists, leave it alone.
                                            String urlNoFragString = URLUtil.urlNoFragString(jarURL);
                                            if (!lmap.containsKey(urlNoFragString)) {
                                                lmap.put(urlNoFragString, null);
                                            }
                                        } catch (MalformedURLException e) {
                                            continue;
                                        }
                                    }
                                }
                                return null;
                            }
                        }, acc);
                } catch (PrivilegedActionException pae) {
                    throw (IOException)pae.getException();
                }
            }
        }

        /* Throws if the given jar file is does not start with the correct LOC */
        @SuppressWarnings("removal")
        static JarFile checkJar(JarFile jar) throws IOException {
            if (System.getSecurityManager() != null && !DISABLE_JAR_CHECKING
                && !zipAccess.startsWithLocHeader(jar)) {
                IOException x = new IOException("Invalid Jar file");
                try {
                    jar.close();
                } catch (IOException ex) {
                    x.addSuppressed(ex);
                }
                throw x;
            }

            return jar;
        }

        private JarFile getJarFile(URL url) throws IOException {
            // Optimize case where url refers to a local jar file
            if (isOptimizable(url)) {
                FileURLMapper p = new FileURLMapper(url);
                if (!p.exists()) {
                    throw new FileNotFoundException(p.getPath());
                }
                return checkJar(new JarFile(new File(p.getPath()), true, ZipFile.OPEN_READ,
                        JarFile.runtimeVersion()));
            }
            URLConnection uc = (new URL(getBaseURL(), "#runtime")).openConnection();
            uc.setRequestProperty(USER_AGENT_JAVA_VERSION, JAVA_VERSION);
            JarFile jarFile = ((JarURLConnection)uc).getJarFile();
            return checkJar(jarFile);
        }

        /*
         * Returns the index of this JarLoader if it exists.
         */
        JarIndex getIndex() {
            try {
                ensureOpen();
            } catch (IOException e) {
                throw new InternalError(e);
            }
            return index;
        }

        /*
         * Creates the resource and if the check flag is set to true, checks if
         * is its okay to return the resource.
         */
        Resource checkResource(final String name, boolean check,
            final JarEntry entry) {

            final URL url;
            try {
                String nm;
                if (jar.isMultiRelease()) {
                    nm = entry.getRealName();
                } else {
                    nm = name;
                }
                url = new URL(getBaseURL(), ParseUtil.encodePath(nm, false));
                if (check) {
                    URLClassPath.check(url);
                }
            } catch (MalformedURLException e) {
                return null;
                // throw new IllegalArgumentException("name");
            } catch (IOException e) {
                return null;
            } catch (@SuppressWarnings("removal") AccessControlException e) {
                return null;
            }

            return new Resource() {
                public String getName() { return name; }
                public URL getURL() { return url; }
                public URL getCodeSourceURL() { return csu; }
                public InputStream getInputStream() throws IOException
                    { return jar.getInputStream(entry); }
                public int getContentLength()
                    { return (int)entry.getSize(); }
                public Manifest getManifest() throws IOException {
                    SharedSecrets.javaUtilJarAccess().ensureInitialization(jar);
                    return jar.getManifest();
                }
                public Certificate[] getCertificates()
                    { return entry.getCertificates(); };
                public CodeSigner[] getCodeSigners()
                    { return entry.getCodeSigners(); };
            };
        }


        /*
         * Returns true iff at least one resource in the jar file has the same
         * package name as that of the specified resource name.
         */
        boolean validIndex(final String name) {
            String packageName = name;
            int pos;
            if ((pos = name.lastIndexOf('/')) != -1) {
                packageName = name.substring(0, pos);
            }

            String entryName;
            ZipEntry entry;
            Enumeration<JarEntry> enum_ = jar.entries();
            while (enum_.hasMoreElements()) {
                entry = enum_.nextElement();
                entryName = entry.getName();
                if ((pos = entryName.lastIndexOf('/')) != -1)
                    entryName = entryName.substring(0, pos);
                if (entryName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }

        /*
         * Returns the URL for a resource with the specified name
         */
        @Override
        URL findResource(final String name, boolean check) {
            Resource rsc = getResource(name, check);
            if (rsc != null) {
                return rsc.getURL();
            }
            return null;
        }

        /*
         * Returns the JAR Resource for the specified name.
         */
        @Override
        Resource getResource(final String name, boolean check) {
            try {
                ensureOpen();
            } catch (IOException e) {
                throw new InternalError(e);
            }
            final JarEntry entry = jar.getJarEntry(name);
            if (entry != null)
                return checkResource(name, check, entry);

            if (index == null)
                return null;

            HashSet<String> visited = new HashSet<>();
            return getResource(name, check, visited);
        }

        /*
         * Version of getResource() that tracks the jar files that have been
         * visited by linking through the index files. This helper method uses
         * a HashSet to store the URLs of jar files that have been searched and
         * uses it to avoid going into an infinite loop, looking for a
         * non-existent resource.
         */
        @SuppressWarnings("removal")
        Resource getResource(final String name, boolean check,
                             Set<String> visited) {
            Resource res;
            String[] jarFiles;
            int count = 0;
            List<String> jarFilesList;

            /* If there no jar files in the index that can potential contain
             * this resource then return immediately.
             */
            if ((jarFilesList = index.get(name)) == null)
                return null;

            do {
                int size = jarFilesList.size();
                jarFiles = jarFilesList.toArray(new String[size]);
                /* loop through the mapped jar file list */
                while (count < size) {
                    String jarName = jarFiles[count++];
                    JarLoader newLoader;
                    final URL url;

                    try{
                        url = new URL(csu, jarName);
                        String urlNoFragString = URLUtil.urlNoFragString(url);
                        if ((newLoader = (JarLoader)lmap.get(urlNoFragString)) == null) {
                            /* no loader has been set up for this jar file
                             * before
                             */
                            newLoader = AccessController.doPrivileged(
                                new PrivilegedExceptionAction<>() {
                                    public JarLoader run() throws IOException {
                                        return new JarLoader(url, handler,
                                            lmap, acc);
                                    }
                                }, acc);

                            /* this newly opened jar file has its own index,
                             * merge it into the parent's index, taking into
                             * account the relative path.
                             */
                            JarIndex newIndex = newLoader.getIndex();
                            if (newIndex != null) {
                                int pos = jarName.lastIndexOf('/');
                                newIndex.merge(this.index, (pos == -1 ?
                                    null : jarName.substring(0, pos + 1)));
                            }

                            /* put it in the global hashtable */
                            lmap.put(urlNoFragString, newLoader);
                        }
                    } catch (PrivilegedActionException pae) {
                        continue;
                    } catch (MalformedURLException e) {
                        continue;
                    }

                    /* Note that the addition of the url to the list of visited
                     * jars incorporates a check for presence in the hashmap
                     */
                    boolean visitedURL = !visited.add(URLUtil.urlNoFragString(url));
                    if (!visitedURL) {
                        try {
                            newLoader.ensureOpen();
                        } catch (IOException e) {
                            throw new InternalError(e);
                        }
                        final JarEntry entry = newLoader.jar.getJarEntry(name);
                        if (entry != null) {
                            return newLoader.checkResource(name, check, entry);
                        }

                        /* Verify that at least one other resource with the
                         * same package name as the lookedup resource is
                         * present in the new jar
                         */
                        if (!newLoader.validIndex(name)) {
                            /* the mapping is wrong */
                            throw new InvalidJarIndexError("Invalid index");
                        }
                    }

                    /* If newLoader is the current loader or if it is a
                     * loader that has already been searched or if the new
                     * loader does not have an index then skip it
                     * and move on to the next loader.
                     */
                    if (visitedURL || newLoader == this ||
                            newLoader.getIndex() == null) {
                        continue;
                    }

                    /* Process the index of the new loader
                     */
                    if ((res = newLoader.getResource(name, check, visited))
                            != null) {
                        return res;
                    }
                }
                // Get the list of jar files again as the list could have grown
                // due to merging of index files.
                jarFilesList = index.get(name);

            // If the count is unchanged, we are done.
            } while (count < jarFilesList.size());
            return null;
        }


        /*
         * Returns the JAR file local class path, or null if none.
         */
        @Override
        URL[] getClassPath() throws IOException {
            if (index != null) {
                return null;
            }

            ensureOpen();

            // Only get manifest when necessary
            if (SharedSecrets.javaUtilJarAccess().jarFileHasClassPathAttribute(jar)) {
                Manifest man = jar.getManifest();
                if (man != null) {
                    Attributes attr = man.getMainAttributes();
                    if (attr != null) {
                        String value = attr.getValue(Name.CLASS_PATH);
                        if (value != null) {
                            return parseClassPath(csu, value);
                        }
                    }
                }
            }
            return null;
        }

        /*
         * Parses value of the Class-Path manifest attribute and returns
         * an array of URLs relative to the specified base URL.
         */
        private static URL[] parseClassPath(URL base, String value)
            throws MalformedURLException
        {
            StringTokenizer st = new StringTokenizer(value);
            URL[] urls = new URL[st.countTokens()];
            int i = 0;
            while (st.hasMoreTokens()) {
                String path = st.nextToken();
                URL url = DISABLE_CP_URL_CHECK ? new URL(base, path) : tryResolve(base, path);
                if (url != null) {
                    urls[i] = url;
                    i++;
                } else {
                    if (DEBUG_CP_URL_CHECK) {
                        System.err.println("Class-Path entry: \"" + path
                                           + "\" ignored in JAR file " + base);
                    }
                }
            }
            if (i == 0) {
                urls = null;
            } else if (i != urls.length) {
                // Truncate nulls from end of array
                urls = Arrays.copyOf(urls, i);
            }
            return urls;
        }

        static URL tryResolve(URL base, String input) throws MalformedURLException {
            if ("file".equalsIgnoreCase(base.getProtocol())) {
                return tryResolveFile(base, input);
            } else {
                return tryResolveNonFile(base, input);
            }
        }

        /**
         * Attempt to return a file URL by resolving input against a base file
         * URL.
         * @return the resolved URL or null if the input is an absolute URL with
         *         a scheme other than file (ignoring case)
         * @throws MalformedURLException
         */
        static URL tryResolveFile(URL base, String input) throws MalformedURLException {
            URL retVal = new URL(base, input);
            if (input.indexOf(':') >= 0 &&
                    !"file".equalsIgnoreCase(retVal.getProtocol())) {
                // 'input' contains a ':', which might be a scheme, or might be
                // a Windows drive letter.  If the protocol for the resolved URL
                // isn't "file:", it should be ignored.
                return null;
            }
            return retVal;
        }

        /**
         * Attempt to return a URL by resolving input against a base URL. Returns
         * null if the resolved URL is not contained by the base URL.
         *
         * @return the resolved URL or null
         * @throws MalformedURLException
         */
        static URL tryResolveNonFile(URL base, String input) throws MalformedURLException {
            String child = input.replace(File.separatorChar, '/');
            if (isRelative(child)) {
                URL url = new URL(base, child);
                String bp = base.getPath();
                String urlp = url.getPath();
                int pos = bp.lastIndexOf('/');
                if (pos == -1) {
                    pos = bp.length() - 1;
                }
                if (urlp.regionMatches(0, bp, 0, pos + 1)
                        && urlp.indexOf("..", pos) == -1) {
                    return url;
                }
            }
            return null;
        }

        /**
         * Returns true if the given input is a relative URI.
         */
        static boolean isRelative(String child) {
            try {
                return !URI.create(child).isAbsolute();
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    /*
     * Nested class used to represent a loader of classes and resources
     * from a file URL that refers to a directory.
     */
    private static class FileLoader extends Loader {
        /* Canonicalized File */
        private File dir;

        /*
         * Creates a new FileLoader for the specified URL with a file protocol.
         */
        private FileLoader(URL url) throws IOException {
            super(url);
            String path = url.getFile().replace('/', File.separatorChar);
            path = ParseUtil.decode(path);
            dir = (new File(path)).getCanonicalFile();
        }

        /*
         * Returns the URL for a resource with the specified name
         */
        @Override
        URL findResource(final String name, boolean check) {
            Resource rsc = getResource(name, check);
            if (rsc != null) {
                return rsc.getURL();
            }
            return null;
        }

        @Override
        Resource getResource(final String name, boolean check) {
            final URL url;
            try {
                URL normalizedBase = new URL(getBaseURL(), ".");
                url = new URL(getBaseURL(), ParseUtil.encodePath(name, false));

                if (url.getFile().startsWith(normalizedBase.getFile()) == false) {
                    // requested resource had ../..'s in path
                    return null;
                }

                if (check)
                    URLClassPath.check(url);

                final File file;
                if (name.indexOf("..") != -1) {
                    file = (new File(dir, name.replace('/', File.separatorChar)))
                          .getCanonicalFile();
                    if ( !((file.getPath()).startsWith(dir.getPath())) ) {
                        /* outside of base dir */
                        return null;
                    }
                } else {
                    file = new File(dir, name.replace('/', File.separatorChar));
                }

                if (file.exists()) {
                    return new Resource() {
                        public String getName() { return name; };
                        public URL getURL() { return url; };
                        public URL getCodeSourceURL() { return getBaseURL(); };
                        public InputStream getInputStream() throws IOException
                            { return new FileInputStream(file); };
                        public int getContentLength() throws IOException
                            { return (int)file.length(); };
                    };
                }
            } catch (Exception e) {
                return null;
            }
            return null;
        }
    }
}
