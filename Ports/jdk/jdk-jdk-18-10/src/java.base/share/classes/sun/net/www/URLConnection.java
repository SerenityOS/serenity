/*
 * Copyright (c) 1995, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.www;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * A class to represent an active connection to an object
 * represented by a URL.
 * @author  James Gosling
 */

public abstract class URLConnection extends java.net.URLConnection {

    /** The URL that it is connected to */

    private String contentType;
    private int contentLength = -1;

    protected MessageHeader properties;

    /**
     * Create a URLConnection object.  These should not be created directly:
     * instead they should be created by protocol handlers in response to
     * URL.openConnection.
     * @param  u       The URL that this connects to.
     */
    public URLConnection (URL u) {
        super(u);
        properties = new MessageHeader();
    }

    /**
     * Call this routine to get the property list for this object.
     * Properties (like content-type) that have explicit getXX() methods
     * associated with them should be accessed using those methods.
     */
    public MessageHeader getProperties() {
        return properties;
    }

    /** Call this routine to set the property list for this object. */
    public void setProperties(MessageHeader properties) {
        this.properties = properties;
    }

    public void setRequestProperty(String key, String value) {
        if(connected)
            throw new IllegalStateException("Already connected");
        if (key == null)
            throw new NullPointerException ("key cannot be null");
        properties.set(key, value);
    }

    /**
     * The following three methods addRequestProperty, getRequestProperty,
     * and getRequestProperties were copied from the superclass implementation
     * before it was changed by CR:6230836, to maintain backward compatibility.
     */
    public void addRequestProperty(String key, String value) {
        if (connected)
            throw new IllegalStateException("Already connected");
        if (key == null)
            throw new NullPointerException ("key is null");
    }

    public String getRequestProperty(String key) {
        if (connected)
            throw new IllegalStateException("Already connected");
        return null;
    }

    public Map<String,List<String>> getRequestProperties() {
        if (connected)
            throw new IllegalStateException("Already connected");
        return Collections.emptyMap();
    }

    public String getHeaderField(String name) {
        try {
            getInputStream();
        } catch (Exception e) {
            return null;
        }
        return properties == null ? null : properties.findValue(name);
    }


    Map<String, List<String>> headerFields;

    @Override
    public Map<String, List<String>> getHeaderFields() {
        if (headerFields == null) {
            try {
                getInputStream();
                if (properties == null) {
                    headerFields = super.getHeaderFields();
                } else {
                    headerFields = properties.getHeaders();
                }
            } catch (IOException e) {
                return super.getHeaderFields();
            }
        }
        return headerFields;
    }

    /**
     * Return the key for the nth header field. Returns null if
     * there are fewer than n fields.  This can be used to iterate
     * through all the headers in the message.
     */
    public String getHeaderFieldKey(int n) {
        try {
            getInputStream();
        } catch (Exception e) {
            return null;
        }
        MessageHeader props = properties;
        return props == null ? null : props.getKey(n);
    }

    /**
     * Return the value for the nth header field. Returns null if
     * there are fewer than n fields.  This can be used in conjunction
     * with getHeaderFieldKey to iterate through all the headers in the message.
     */
    public String getHeaderField(int n) {
        try {
            getInputStream();
        } catch (Exception e) {
            return null;
        }
        MessageHeader props = properties;
        return props == null ? null : props.getValue(n);
    }

    /**
     * Call this routine to get the content-type associated with this
     * object.
     */
    public String getContentType() {
        if (contentType == null)
            contentType = getHeaderField("content-type");
        if (contentType == null) {
            String ct = null;
            try {
                ct = guessContentTypeFromStream(getInputStream());
            } catch(java.io.IOException e) {
            }
            String ce = properties.findValue("content-encoding");
            if (ct == null) {
                ct = properties.findValue("content-type");

                if (ct == null)
                    if (url.getFile().endsWith("/"))
                        ct = "text/html";
                    else
                        ct = guessContentTypeFromName(url.getFile());
            }

            /*
             * If the Mime header had a Content-encoding field and its value
             * was not one of the values that essentially indicate no
             * encoding, we force the content type to be unknown. This will
             * cause a save dialog to be presented to the user.  It is not
             * ideal but is better than what we were previously doing, namely
             * bringing up an image tool for compressed tar files.
             */

            if (ct == null || ce != null &&
                    !(ce.equalsIgnoreCase("7bit")
                      || ce.equalsIgnoreCase("8bit")
                      || ce.equalsIgnoreCase("binary")))
                ct = "content/unknown";
            setContentType(ct);
        }
        return contentType;
    }

    /**
     * Set the content type of this URL to a specific value.
     * @param   type    The content type to use.  One of the
     *                  content_* static variables in this
     *                  class should be used.
     *                  e.g. setType(URL.content_html);
     */
    public void setContentType(String type) {
        contentType = type;
        properties.set("content-type", type);
    }

    /**
     * Call this routine to get the content-length associated with this
     * object.
     */
    public int getContentLength() {
        try {
            getInputStream();
        } catch (Exception e) {
            return -1;
        }
        int l = contentLength;
        if (l < 0) {
            try {
                l = Integer.parseInt(properties.findValue("content-length"));
                setContentLength(l);
            } catch(Exception e) {
            }
        }
        return l;
    }

    /**
     * Call this routine to set the content-length associated with this
     * object.
     */
    protected void setContentLength(int length) {
        contentLength = length;
        properties.set("content-length", String.valueOf(length));
    }

    /**
     * Returns true if the data associated with this URL can be cached.
     */
    public boolean canCache() {
        return url.getFile().indexOf('?') < 0   /* && url.postData == null
                REMIND */ ;
    }

    /**
     * Call this to close the connection and flush any remaining data.
     * Overriders must remember to call super.close()
     */
    public void close() {
        url = null;
    }

    private static HashMap<String,Void> proxiedHosts = new HashMap<>();

    public static synchronized void setProxiedHost(String host) {
        proxiedHosts.put(host.toLowerCase(), null);
    }

    public static synchronized boolean isProxiedHost(String host) {
        return proxiedHosts.containsKey(host.toLowerCase());
    }
}
