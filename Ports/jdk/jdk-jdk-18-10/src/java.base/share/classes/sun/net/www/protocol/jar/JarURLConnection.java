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

package sun.net.www.protocol.jar;

import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.net.UnknownServiceException;
import java.util.Enumeration;
import java.util.Map;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.security.Permission;

/**
 * @author Benjamin Renaud
 * @since 1.2
 */
public class JarURLConnection extends java.net.JarURLConnection {

    private static final boolean debug = false;

    /* the Jar file factory. It handles both retrieval and caching.
     */
    private static final JarFileFactory factory = JarFileFactory.getInstance();

    /* the url for the Jar file */
    private URL jarFileURL;

    /* the permission to get this JAR file. This is the actual, ultimate,
     * permission, returned by the jar file factory.
     */
    private Permission permission;

    /* the url connection for the JAR file */
    private URLConnection jarFileURLConnection;

    /* the entry name, if any */
    private String entryName;

    /* the JarEntry */
    private JarEntry jarEntry;

    /* the jar file corresponding to this connection */
    private JarFile jarFile;

    /* the content type for this connection */
    private String contentType;

    public JarURLConnection(URL url, Handler handler)
    throws MalformedURLException, IOException {
        super(url);

        jarFileURL = getJarFileURL();
        jarFileURLConnection = jarFileURL.openConnection();
        // whether, or not, the embedded URL should use the cache will depend
        // on this instance's cache value
        jarFileURLConnection.setUseCaches(useCaches);
        entryName = getEntryName();
    }

    public JarFile getJarFile() throws IOException {
        connect();
        return jarFile;
    }

    public JarEntry getJarEntry() throws IOException {
        connect();
        return jarEntry;
    }

    public Permission getPermission() throws IOException {
        return jarFileURLConnection.getPermission();
    }

    class JarURLInputStream extends java.io.FilterInputStream {
        JarURLInputStream (InputStream src) {
            super (src);
        }
        public void close () throws IOException {
            try {
                super.close();
            } finally {
                if (!getUseCaches()) {
                    jarFile.close();
                }
            }
        }
    }

    public void connect() throws IOException {
        if (!connected) {
            boolean useCaches = getUseCaches();
            String entryName = this.entryName;

            /* the factory call will do the security checks */
            URL url = getJarFileURL();
            // if we have an entry name, and the jarfile is local,
            // don't put the jar into the cache until after we have
            // validated that the entry name exists
            jarFile = entryName == null
                    ? factory.get(url, useCaches)
                    : factory.getOrCreate(url, useCaches);

            if ((entryName != null)) {
                jarEntry = (JarEntry) jarFile.getEntry(entryName);
                if (jarEntry == null) {
                    try {
                        // only close the jar file if it isn't in the
                        // cache. If the jar file is local, it won't be
                        // in the cache yet, and so will be closed here.
                        factory.closeIfNotCached(url, jarFile);
                    } catch (Exception e) {
                    }
                    throw new FileNotFoundException("JAR entry " + entryName +
                            " not found in " +
                            jarFile.getName());
                }
            }

            // we have validated that the entry exists.
            // if useCaches was requested, update the cache now.
            if (useCaches && entryName != null) {
                // someone may have beat us and updated the cache
                // already - in which case - cacheIfAbsent will
                // return false. cacheIfAbsent returns true if
                // our jarFile is in the cache when the method
                // returns, whether because it put it there or
                // because it found it there.
                useCaches = factory.cacheIfAbsent(url, jarFile);
            }

            /* we also ask the factory the permission that was required
             * to get the jarFile, and set it as our permission.
             */
            if (useCaches) {
                boolean oldUseCaches = jarFileURLConnection.getUseCaches();
                jarFileURLConnection = factory.getConnection(jarFile);
                jarFileURLConnection.setUseCaches(oldUseCaches);
            }
            connected = true;
        }
    }

    public InputStream getInputStream() throws IOException {
        connect();

        InputStream result = null;

        if (entryName == null) {
            throw new IOException("no entry name specified");
        } else {
            if (jarEntry == null) {
                throw new FileNotFoundException("JAR entry " + entryName +
                                                " not found in " +
                                                jarFile.getName());
            }
            result = new JarURLInputStream (jarFile.getInputStream(jarEntry));
        }
        return result;
    }

    public int getContentLength() {
        long result = getContentLengthLong();
        if (result > Integer.MAX_VALUE)
            return -1;
        return (int) result;
    }

    public long getContentLengthLong() {
        long result = -1;
        try {
            connect();
            if (jarEntry == null) {
                /* if the URL referes to an archive */
                result = jarFileURLConnection.getContentLengthLong();
            } else {
                /* if the URL referes to an archive entry */
                result = getJarEntry().getSize();
            }
        } catch (IOException e) {
        }
        return result;
    }

    public Object getContent() throws IOException {
        Object result = null;

        connect();
        if (entryName == null) {
            result = jarFile;
        } else {
            result = super.getContent();
        }
        return result;
    }

    public String getContentType() {
        if (contentType == null) {
            if (entryName == null) {
                contentType = "x-java/jar";
            } else {
                try {
                    connect();
                    InputStream in = jarFile.getInputStream(jarEntry);
                    contentType = guessContentTypeFromStream(
                                        new BufferedInputStream(in));
                    in.close();
                } catch (IOException e) {
                    // don't do anything
                }
            }
            if (contentType == null) {
                contentType = guessContentTypeFromName(entryName);
            }
            if (contentType == null) {
                contentType = "content/unknown";
            }
        }
        return contentType;
    }

    public String getHeaderField(String name) {
        return jarFileURLConnection.getHeaderField(name);
    }

    /**
     * Sets the general request property.
     *
     * @param   key     the keyword by which the request is known
     *                  (e.g., "<code>accept</code>").
     * @param   value   the value associated with it.
     */
    public void setRequestProperty(String key, String value) {
        jarFileURLConnection.setRequestProperty(key, value);
    }

    /**
     * Returns the value of the named general request property for this
     * connection.
     *
     * @return  the value of the named general request property for this
     *           connection.
     */
    public String getRequestProperty(String key) {
        return jarFileURLConnection.getRequestProperty(key);
    }

    /**
     * Adds a general request property specified by a
     * key-value pair.  This method will not overwrite
     * existing values associated with the same key.
     *
     * @param   key     the keyword by which the request is known
     *                  (e.g., "<code>accept</code>").
     * @param   value   the value associated with it.
     */
    public void addRequestProperty(String key, String value) {
        jarFileURLConnection.addRequestProperty(key, value);
    }

    /**
     * Returns an unmodifiable Map of general request
     * properties for this connection. The Map keys
     * are Strings that represent the request-header
     * field names. Each Map value is a unmodifiable List
     * of Strings that represents the corresponding
     * field values.
     *
     * @return  a Map of the general request properties for this connection.
     */
    public Map<String,List<String>> getRequestProperties() {
        return jarFileURLConnection.getRequestProperties();
    }

    /**
     * Set the value of the <code>allowUserInteraction</code> field of
     * this <code>URLConnection</code>.
     *
     * @param   allowuserinteraction   the new value.
     * @see     java.net.URLConnection#allowUserInteraction
     */
    public void setAllowUserInteraction(boolean allowuserinteraction) {
        jarFileURLConnection.setAllowUserInteraction(allowuserinteraction);
    }

    /**
     * Returns the value of the <code>allowUserInteraction</code> field for
     * this object.
     *
     * @return  the value of the <code>allowUserInteraction</code> field for
     *          this object.
     * @see     java.net.URLConnection#allowUserInteraction
     */
    public boolean getAllowUserInteraction() {
        return jarFileURLConnection.getAllowUserInteraction();
    }

    /*
     * cache control
     */

    /**
     * Sets the value of the <code>useCaches</code> field of this
     * <code>URLConnection</code> to the specified value.
     * <p>
     * Some protocols do caching of documents.  Occasionally, it is important
     * to be able to "tunnel through" and ignore the caches (e.g., the
     * "reload" button in a browser).  If the UseCaches flag on a connection
     * is true, the connection is allowed to use whatever caches it can.
     *  If false, caches are to be ignored.
     *  The default value comes from DefaultUseCaches, which defaults to
     * true.
     *
     * @see     java.net.URLConnection#useCaches
     */
    public void setUseCaches(boolean usecaches) {
        jarFileURLConnection.setUseCaches(usecaches);
    }

    /**
     * Returns the value of this <code>URLConnection</code>'s
     * <code>useCaches</code> field.
     *
     * @return  the value of this <code>URLConnection</code>'s
     *          <code>useCaches</code> field.
     * @see     java.net.URLConnection#useCaches
     */
    public boolean getUseCaches() {
        return jarFileURLConnection.getUseCaches();
    }

    /**
     * Sets the value of the <code>ifModifiedSince</code> field of
     * this <code>URLConnection</code> to the specified value.
     *
     * @param   ifmodifiedsince   the new value.
     * @see     java.net.URLConnection#ifModifiedSince
     */
    public void setIfModifiedSince(long ifmodifiedsince) {
        jarFileURLConnection.setIfModifiedSince(ifmodifiedsince);
    }

    /**
     * Sets the default value of the <code>useCaches</code> field to the
     * specified value.
     *
     * @param   defaultusecaches   the new value.
     * @see     java.net.URLConnection#useCaches
     */
    public void setDefaultUseCaches(boolean defaultusecaches) {
        jarFileURLConnection.setDefaultUseCaches(defaultusecaches);
    }

    /**
     * Returns the default value of a <code>URLConnection</code>'s
     * <code>useCaches</code> flag.
     * <p>
     * Ths default is "sticky", being a part of the static state of all
     * URLConnections.  This flag applies to the next, and all following
     * URLConnections that are created.
     *
     * @return  the default value of a <code>URLConnection</code>'s
     *          <code>useCaches</code> flag.
     * @see     java.net.URLConnection#useCaches
     */
    public boolean getDefaultUseCaches() {
        return jarFileURLConnection.getDefaultUseCaches();
    }
}
