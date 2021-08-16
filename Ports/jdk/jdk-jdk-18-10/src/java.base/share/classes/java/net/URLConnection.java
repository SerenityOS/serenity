/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.StringTokenizer;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.security.Permission;
import java.security.AccessController;
import sun.security.util.SecurityConstants;
import sun.net.www.MessageHeader;
import sun.security.action.GetPropertyAction;

/**
 * The abstract class {@code URLConnection} is the superclass
 * of all classes that represent a communications link between the
 * application and a URL. Instances of this class can be used both to
 * read from and to write to the resource referenced by the URL.
 *
 * <p>
 * In general, creating a connection to a URL is a multistep process:
 * <ol>
 * <li>The connection object is created by invoking the
 *     {@link URL#openConnection() openConnection} method on a URL.
 * <li>The setup parameters and general request properties are manipulated.
 * <li>The actual connection to the remote object is made, using the
 *    {@link #connect() connect} method.
 * <li>The remote object becomes available. The header fields and the contents
 *     of the remote object can be accessed.
 * </ol>
 * <p>
 * The setup parameters are modified using the following methods:
 * <ul>
 *   <li>{@code setAllowUserInteraction}
 *   <li>{@code setDoInput}
 *   <li>{@code setDoOutput}
 *   <li>{@code setIfModifiedSince}
 *   <li>{@code setUseCaches}
 * </ul>
 * <p>
 * and the general request properties are modified using the method:
 * <ul>
 *   <li>{@code setRequestProperty}
 * </ul>
 * <p>
 * Default values for the {@code AllowUserInteraction} and
 * {@code UseCaches} parameters can be set using the methods
 * {@code setDefaultAllowUserInteraction} and
 * {@code setDefaultUseCaches}.
 * <p>
 * Each of the above {@code set} methods has a corresponding
 * {@code get} method to retrieve the value of the parameter or
 * general request property. The specific parameters and general
 * request properties that are applicable are protocol specific.
 * <p>
 * The following methods are used to access the header fields and
 * the contents after the connection is made to the remote object:
 * <ul>
 *   <li>{@code getContent}
 *   <li>{@code getHeaderField}
 *   <li>{@code getInputStream}
 *   <li>{@code getOutputStream}
 * </ul>
 * <p>
 * Certain header fields are accessed frequently. The methods:
 * <ul>
 *   <li>{@code getContentEncoding}
 *   <li>{@code getContentLength}
 *   <li>{@code getContentType}
 *   <li>{@code getDate}
 *   <li>{@code getExpiration}
 *   <li>{@code getLastModified}
 * </ul>
 * <p>
 * provide convenient access to these fields. The
 * {@code getContentType} method is used by the
 * {@code getContent} method to determine the type of the remote
 * object; subclasses may find it convenient to override the
 * {@code getContentType} method.
 * <p>
 * In the common case, all of the pre-connection parameters and
 * general request properties can be ignored: the pre-connection
 * parameters and request properties default to sensible values. For
 * most clients of this interface, there are only two interesting
 * methods: {@code getInputStream} and {@code getContent},
 * which are mirrored in the {@code URL} class by convenience methods.
 * <p>
 * More information on the request properties and header fields of
 * an {@code http} connection can be found at:
 * <blockquote><pre>
 * <a href="http://www.ietf.org/rfc/rfc2616.txt">http://www.ietf.org/rfc/rfc2616.txt</a>
 * </pre></blockquote>
 *
 * Invoking the {@code close()} methods on the {@code InputStream} or {@code OutputStream} of an
 * {@code URLConnection} after a request may free network resources associated with this
 * instance, unless particular protocol specifications specify different behaviours
 * for it.
 *
 * @author  James Gosling
 * @see     java.net.URL#openConnection()
 * @see     java.net.URLConnection#connect()
 * @see     java.net.URLConnection#getContent()
 * @see     java.net.URLConnection#getContentEncoding()
 * @see     java.net.URLConnection#getContentLength()
 * @see     java.net.URLConnection#getContentType()
 * @see     java.net.URLConnection#getDate()
 * @see     java.net.URLConnection#getExpiration()
 * @see     java.net.URLConnection#getHeaderField(int)
 * @see     java.net.URLConnection#getHeaderField(java.lang.String)
 * @see     java.net.URLConnection#getInputStream()
 * @see     java.net.URLConnection#getLastModified()
 * @see     java.net.URLConnection#getOutputStream()
 * @see     java.net.URLConnection#setAllowUserInteraction(boolean)
 * @see     java.net.URLConnection#setDefaultUseCaches(boolean)
 * @see     java.net.URLConnection#setDoInput(boolean)
 * @see     java.net.URLConnection#setDoOutput(boolean)
 * @see     java.net.URLConnection#setIfModifiedSince(long)
 * @see     java.net.URLConnection#setRequestProperty(java.lang.String, java.lang.String)
 * @see     java.net.URLConnection#setUseCaches(boolean)
 * @since   1.0
 */
public abstract class URLConnection {

    /**
     * The URL represents the remote object on the World Wide Web to
     * which this connection is opened.
     * <p>
     * The value of this field can be accessed by the
     * {@code getURL} method.
     * <p>
     * The default value of this variable is the value of the URL
     * argument in the {@code URLConnection} constructor.
     *
     * @see     java.net.URLConnection#getURL()
     * @see     java.net.URLConnection#url
     */
    protected URL url;

    /**
     * This variable is set by the {@code setDoInput} method. Its
     * value is returned by the {@code getDoInput} method.
     * <p>
     * A URL connection can be used for input and/or output. Setting the
     * {@code doInput} flag to {@code true} indicates that
     * the application intends to read data from the URL connection.
     * <p>
     * The default value of this field is {@code true}.
     *
     * @see     java.net.URLConnection#getDoInput()
     * @see     java.net.URLConnection#setDoInput(boolean)
     */
    protected boolean doInput = true;

    /**
     * This variable is set by the {@code setDoOutput} method. Its
     * value is returned by the {@code getDoOutput} method.
     * <p>
     * A URL connection can be used for input and/or output. Setting the
     * {@code doOutput} flag to {@code true} indicates
     * that the application intends to write data to the URL connection.
     * <p>
     * The default value of this field is {@code false}.
     *
     * @see     java.net.URLConnection#getDoOutput()
     * @see     java.net.URLConnection#setDoOutput(boolean)
     */
    protected boolean doOutput = false;

    private static boolean defaultAllowUserInteraction = false;

    /**
     * If {@code true}, this {@code URL} is being examined in
     * a context in which it makes sense to allow user interactions such
     * as popping up an authentication dialog. If {@code false},
     * then no user interaction is allowed.
     * <p>
     * The value of this field can be set by the
     * {@code setAllowUserInteraction} method.
     * Its value is returned by the
     * {@code getAllowUserInteraction} method.
     * Its default value is the value of the argument in the last invocation
     * of the {@code setDefaultAllowUserInteraction} method.
     *
     * @see     java.net.URLConnection#getAllowUserInteraction()
     * @see     java.net.URLConnection#setAllowUserInteraction(boolean)
     * @see     java.net.URLConnection#setDefaultAllowUserInteraction(boolean)
     */
    protected boolean allowUserInteraction = defaultAllowUserInteraction;

    private static volatile boolean defaultUseCaches = true;

    /**
     * If {@code true}, the protocol is allowed to use caching
     * whenever it can. If {@code false}, the protocol must always
     * try to get a fresh copy of the object.
     * <p>
     * This field is set by the {@code setUseCaches} method. Its
     * value is returned by the {@code getUseCaches} method.
     * <p>
     * Its default value is the value given in the last invocation of the
     * {@code setDefaultUseCaches} method.
     * <p>
     * The default setting may be overridden per protocol with
     * {@link #setDefaultUseCaches(String,boolean)}.
     *
     * @see     java.net.URLConnection#setUseCaches(boolean)
     * @see     java.net.URLConnection#getUseCaches()
     * @see     java.net.URLConnection#setDefaultUseCaches(boolean)
     */
    protected boolean useCaches;

    private static final ConcurrentHashMap<String,Boolean> defaultCaching =
        new ConcurrentHashMap<>();

    /**
     * Some protocols support skipping the fetching of the object unless
     * the object has been modified more recently than a certain time.
     * <p>
     * A nonzero value gives a time as the number of milliseconds since
     * January 1, 1970, GMT. The object is fetched only if it has been
     * modified more recently than that time.
     * <p>
     * This variable is set by the {@code setIfModifiedSince}
     * method. Its value is returned by the
     * {@code getIfModifiedSince} method.
     * <p>
     * The default value of this field is {@code 0}, indicating
     * that the fetching must always occur.
     *
     * @see     java.net.URLConnection#getIfModifiedSince()
     * @see     java.net.URLConnection#setIfModifiedSince(long)
     */
    protected long ifModifiedSince = 0;

    /**
     * If {@code false}, this connection object has not created a
     * communications link to the specified URL. If {@code true},
     * the communications link has been established.
     */
    protected boolean connected = false;

    /**
     * @since 1.5
     */
    private int connectTimeout;
    private int readTimeout;

    /**
     * @since 1.6
     */
    private MessageHeader requests;

    /**
     * @since 1.1
     */
    private static volatile FileNameMap fileNameMap;

    /**
     * Loads filename map (a mimetable) from a data file. It will
     * first try to load the user-specific table, defined
     * by &quot;content.types.user.table&quot; property. If that fails,
     * it tries to load the default built-in table.
     *
     * @return the FileNameMap
     * @since 1.2
     * @see #setFileNameMap(java.net.FileNameMap)
     */
    public static FileNameMap getFileNameMap() {
        FileNameMap map = fileNameMap;

        if (map == null) {
            fileNameMap = map = new FileNameMap() {
                private FileNameMap internalMap =
                    sun.net.www.MimeTable.loadTable();

                public String getContentTypeFor(String fileName) {
                    return internalMap.getContentTypeFor(fileName);
                }
            };
        }

        return map;
    }

    /**
     * Sets the FileNameMap.
     * <p>
     * If there is a security manager, this method first calls
     * the security manager's {@code checkSetFactory} method
     * to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param map the FileNameMap to be set
     * @throws     SecurityException  if a security manager exists and its
     *             {@code checkSetFactory} method doesn't allow the operation.
     * @see        SecurityManager#checkSetFactory
     * @see #getFileNameMap()
     * @since 1.2
     */
    public static void setFileNameMap(FileNameMap map) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkSetFactory();
        fileNameMap = map;
    }

    /**
     * Opens a communications link to the resource referenced by this
     * URL, if such a connection has not already been established.
     * <p>
     * If the {@code connect} method is called when the connection
     * has already been opened (indicated by the {@code connected}
     * field having the value {@code true}), the call is ignored.
     * <p>
     * URLConnection objects go through two phases: first they are
     * created, then they are connected.  After being created, and
     * before being connected, various options can be specified
     * (e.g., doInput and UseCaches).  After connecting, it is an
     * error to try to set them.  Operations that depend on being
     * connected, like getContentLength, will implicitly perform the
     * connection, if necessary.
     *
     * @throws SocketTimeoutException if the timeout expires before
     *               the connection can be established
     * @throws     IOException  if an I/O error occurs while opening the
     *               connection.
     * @see java.net.URLConnection#connected
     * @see #getConnectTimeout()
     * @see #setConnectTimeout(int)
     */
    public abstract void connect() throws IOException;

    /**
     * Sets a specified timeout value, in milliseconds, to be used
     * when opening a communications link to the resource referenced
     * by this URLConnection.  If the timeout expires before the
     * connection can be established, a
     * java.net.SocketTimeoutException is raised. A timeout of zero is
     * interpreted as an infinite timeout.
     *
     * <p> Some non-standard implementation of this method may ignore
     * the specified timeout. To see the connect timeout set, please
     * call getConnectTimeout().
     *
     * @param timeout an {@code int} that specifies the connect
     *               timeout value in milliseconds
     * @throws IllegalArgumentException if the timeout parameter is negative
     *
     * @see #getConnectTimeout()
     * @see #connect()
     * @since 1.5
     */
    public void setConnectTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        connectTimeout = timeout;
    }

    /**
     * Returns setting for connect timeout.
     * <p>
     * 0 return implies that the option is disabled
     * (i.e., timeout of infinity).
     *
     * @return an {@code int} that indicates the connect timeout
     *         value in milliseconds
     * @see #setConnectTimeout(int)
     * @see #connect()
     * @since 1.5
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the read timeout to a specified timeout, in
     * milliseconds. A non-zero value specifies the timeout when
     * reading from Input stream when a connection is established to a
     * resource. If the timeout expires before there is data available
     * for read, a java.net.SocketTimeoutException is raised. A
     * timeout of zero is interpreted as an infinite timeout.
     *
     * <p> Some non-standard implementation of this method ignores the
     * specified timeout. To see the read timeout set, please call
     * getReadTimeout().
     *
     * @param timeout an {@code int} that specifies the timeout
     * value to be used in milliseconds
     * @throws IllegalArgumentException if the timeout parameter is negative
     *
     * @see #getReadTimeout()
     * @see InputStream#read()
     * @since 1.5
     */
    public void setReadTimeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        readTimeout = timeout;
    }

    /**
     * Returns setting for read timeout. 0 return implies that the
     * option is disabled (i.e., timeout of infinity).
     *
     * @return an {@code int} that indicates the read timeout
     *         value in milliseconds
     *
     * @see #setReadTimeout(int)
     * @see InputStream#read()
     * @since 1.5
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Constructs a URL connection to the specified URL. A connection to
     * the object referenced by the URL is not created.
     *
     * @param   url   the specified URL.
     */
    protected URLConnection(URL url) {
        this.url = url;
        if (url == null) {
            this.useCaches = defaultUseCaches;
        } else {
            this.useCaches = getDefaultUseCaches(url.getProtocol());
        }
    }

    /**
     * Returns the value of this {@code URLConnection}'s {@code URL}
     * field.
     *
     * @return  the value of this {@code URLConnection}'s {@code URL}
     *          field.
     * @see     java.net.URLConnection#url
     */
    public URL getURL() {
        return url;
    }

    /**
     * Returns the value of the {@code content-length} header field.
     * <P>
     * <B>Note</B>: {@link #getContentLengthLong() getContentLengthLong()}
     * should be preferred over this method, since it returns a {@code long}
     * instead and is therefore more portable.</P>
     *
     * @return  the content length of the resource that this connection's URL
     *          references, {@code -1} if the content length is not known,
     *          or if the content length is greater than Integer.MAX_VALUE.
     */
    public int getContentLength() {
        long l = getContentLengthLong();
        if (l > Integer.MAX_VALUE)
            return -1;
        return (int) l;
    }

    /**
     * Returns the value of the {@code content-length} header field as a
     * long.
     *
     * @return  the content length of the resource that this connection's URL
     *          references, or {@code -1} if the content length is
     *          not known.
     * @since 1.7
     */
    public long getContentLengthLong() {
        return getHeaderFieldLong("content-length", -1);
    }

    /**
     * Returns the value of the {@code content-type} header field.
     *
     * @return  the content type of the resource that the URL references,
     *          or {@code null} if not known.
     * @see     java.net.URLConnection#getHeaderField(java.lang.String)
     */
    public String getContentType() {
        return getHeaderField("content-type");
    }

    /**
     * Returns the value of the {@code content-encoding} header field.
     *
     * @return  the content encoding of the resource that the URL references,
     *          or {@code null} if not known.
     * @see     java.net.URLConnection#getHeaderField(java.lang.String)
     */
    public String getContentEncoding() {
        return getHeaderField("content-encoding");
    }

    /**
     * Returns the value of the {@code expires} header field.
     *
     * @return  the expiration date of the resource that this URL references,
     *          or 0 if not known. The value is the number of milliseconds since
     *          January 1, 1970 GMT.
     * @see     java.net.URLConnection#getHeaderField(java.lang.String)
     */
    public long getExpiration() {
        return getHeaderFieldDate("expires", 0);
    }

    /**
     * Returns the value of the {@code date} header field.
     *
     * @return  the sending date of the resource that the URL references,
     *          or {@code 0} if not known. The value returned is the
     *          number of milliseconds since January 1, 1970 GMT.
     * @see     java.net.URLConnection#getHeaderField(java.lang.String)
     */
    public long getDate() {
        return getHeaderFieldDate("date", 0);
    }

    /**
     * Returns the value of the {@code last-modified} header field.
     * The result is the number of milliseconds since January 1, 1970 GMT.
     *
     * @return  the date the resource referenced by this
     *          {@code URLConnection} was last modified, or 0 if not known.
     * @see     java.net.URLConnection#getHeaderField(java.lang.String)
     */
    public long getLastModified() {
        return getHeaderFieldDate("last-modified", 0);
    }

    /**
     * Returns the value of the named header field.
     * <p>
     * If called on a connection that sets the same header multiple times
     * with possibly different values, only the last value is returned.
     *
     *
     * @param   name   the name of a header field.
     * @return  the value of the named header field, or {@code null}
     *          if there is no such field in the header.
     */
    public String getHeaderField(String name) {
        return null;
    }

    /**
     * Returns an unmodifiable Map of the header fields.
     * The Map keys are Strings that represent the
     * response-header field names. Each Map value is an
     * unmodifiable List of Strings that represents
     * the corresponding field values.
     *
     * @return a Map of header fields
     * @since 1.4
     */
    public Map<String,List<String>> getHeaderFields() {
        return Collections.emptyMap();
    }

    /**
     * Returns the value of the named field parsed as a number.
     * <p>
     * This form of {@code getHeaderField} exists because some
     * connection types (e.g., {@code http-ng}) have pre-parsed
     * headers. Classes for that connection type can override this method
     * and short-circuit the parsing.
     *
     * @param   name      the name of the header field.
     * @param   Default   the default value.
     * @return  the value of the named field, parsed as an integer. The
     *          {@code Default} value is returned if the field is
     *          missing or malformed.
     */
    public int getHeaderFieldInt(String name, int Default) {
        String value = getHeaderField(name);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) { }
        return Default;
    }

    /**
     * Returns the value of the named field parsed as a number.
     * <p>
     * This form of {@code getHeaderField} exists because some
     * connection types (e.g., {@code http-ng}) have pre-parsed
     * headers. Classes for that connection type can override this method
     * and short-circuit the parsing.
     *
     * @param   name      the name of the header field.
     * @param   Default   the default value.
     * @return  the value of the named field, parsed as a long. The
     *          {@code Default} value is returned if the field is
     *          missing or malformed.
     * @since 1.7
     */
    public long getHeaderFieldLong(String name, long Default) {
        String value = getHeaderField(name);
        try {
            return Long.parseLong(value);
        } catch (Exception e) { }
        return Default;
    }

    /**
     * Returns the value of the named field parsed as date.
     * The result is the number of milliseconds since January 1, 1970 GMT
     * represented by the named field.
     * <p>
     * This form of {@code getHeaderField} exists because some
     * connection types (e.g., {@code http-ng}) have pre-parsed
     * headers. Classes for that connection type can override this method
     * and short-circuit the parsing.
     *
     * @param   name     the name of the header field.
     * @param   Default   a default value.
     * @return  the value of the field, parsed as a date. The value of the
     *          {@code Default} argument is returned if the field is
     *          missing or malformed.
     */
    @SuppressWarnings("deprecation")
    public long getHeaderFieldDate(String name, long Default) {
        String value = getHeaderField(name);
        try {
            return Date.parse(value);
        } catch (Exception e) { }
        return Default;
    }

    /**
     * Returns the key for the {@code n}<sup>th</sup> header field.
     * Some implementations may treat the {@code 0}<sup>th</sup>
     * header field as special, in which case, {@link #getHeaderField(int) getHeaderField(0)}
     * may return some value, but {@code getHeaderFieldKey(0)} returns {@code null}.
     * For {@code n > 0 } it returns {@code null} if there are fewer than {@code n+1} fields.
     *
     * @param   n   an index, where {@code n>=0}
     * @return  the key for the {@code n}<sup>th</sup> header field,
     *          or {@code null} if there are fewer than {@code n+1}
     *          fields when {@code n > 0}.
     */
    public String getHeaderFieldKey(int n) {
        return null;
    }

    /**
     * Returns the value for the {@code n}<sup>th</sup> header field.
     * It returns {@code null} if there are fewer than
     * {@code n+1} fields.
     * <p>
     * This method can be used in conjunction with the
     * {@link #getHeaderFieldKey(int) getHeaderFieldKey} method to iterate through all
     * the headers in the message.
     *
     * @param   n   an index, where {@code n>=0}
     * @return  the value of the {@code n}<sup>th</sup> header field
     *          or {@code null} if there are fewer than {@code n+1} fields
     * @see     java.net.URLConnection#getHeaderFieldKey(int)
     */
    public String getHeaderField(int n) {
        return null;
    }

    /**
     * Retrieves the contents of this URL connection.
     * <p>
     * This method first determines the content type of the object by
     * calling the {@code getContentType} method. If this is
     * the first time that the application has seen that specific content
     * type, a content handler for that content type is created.
     * <p> This is done as follows:
     * <ol>
     * <li>If the application has set up a content handler factory instance
     *     using the {@code setContentHandlerFactory} method, the
     *     {@code createContentHandler} method of that instance is called
     *     with the content type as an argument; the result is a content
     *     handler for that content type.
     * <li>If no {@code ContentHandlerFactory} has yet been set up,
     *     or if the factory's {@code createContentHandler} method
     *     returns {@code null}, then the {@linkplain java.util.ServiceLoader
     *     ServiceLoader} mechanism is used to locate {@linkplain
     *     java.net.ContentHandlerFactory ContentHandlerFactory}
     *     implementations using the system class
     *     loader. The order that factories are located is implementation
     *     specific, and an implementation is free to cache the located
     *     factories. A {@linkplain java.util.ServiceConfigurationError
     *     ServiceConfigurationError}, {@code Error} or {@code RuntimeException}
     *     thrown from the {@code createContentHandler}, if encountered, will
     *     be propagated to the calling thread. The {@code
     *     createContentHandler} method of each factory, if instantiated, is
     *     invoked, with the content type, until a factory returns non-null,
     *     or all factories have been exhausted.
     * <li>Failing that, this method tries to load a content handler
     *     class as defined by {@link java.net.ContentHandler ContentHandler}.
     *     If the class does not exist, or is not a subclass of {@code
     *     ContentHandler}, then an {@code UnknownServiceException} is thrown.
     * </ol>
     *
     * @return     the object fetched. The {@code instanceof} operator
     *               should be used to determine the specific kind of object
     *               returned.
     * @throws     IOException              if an I/O error occurs while
     *               getting the content.
     * @throws     UnknownServiceException  if the protocol does not support
     *               the content type.
     * @see        java.net.ContentHandlerFactory#createContentHandler(java.lang.String)
     * @see        java.net.URLConnection#getContentType()
     * @see        java.net.URLConnection#setContentHandlerFactory(java.net.ContentHandlerFactory)
     */
    public Object getContent() throws IOException {
        // Must call getInputStream before GetHeaderField gets called
        // so that FileNotFoundException has a chance to be thrown up
        // from here without being caught.
        getInputStream();
        return getContentHandler().getContent(this);
    }

    /**
     * Retrieves the contents of this URL connection.
     *
     * @param classes the {@code Class} array
     * indicating the requested types
     * @return     the object fetched that is the first match of the type
     *               specified in the classes array. null if none of
     *               the requested types are supported.
     *               The {@code instanceof} operator should be used to
     *               determine the specific kind of object returned.
     * @throws     IOException              if an I/O error occurs while
     *               getting the content.
     * @throws     UnknownServiceException  if the protocol does not support
     *               the content type.
     * @see        java.net.URLConnection#getContent()
     * @see        java.net.ContentHandlerFactory#createContentHandler(java.lang.String)
     * @see        java.net.URLConnection#getContent(java.lang.Class[])
     * @see        java.net.URLConnection#setContentHandlerFactory(java.net.ContentHandlerFactory)
     * @since 1.3
     */
    public Object getContent(Class<?>[] classes) throws IOException {
        // Must call getInputStream before GetHeaderField gets called
        // so that FileNotFoundException has a chance to be thrown up
        // from here without being caught.
        getInputStream();
        return getContentHandler().getContent(this, classes);
    }

    /**
     * Returns a permission object representing the permission
     * necessary to make the connection represented by this
     * object. This method returns null if no permission is
     * required to make the connection. By default, this method
     * returns {@code java.security.AllPermission}. Subclasses
     * should override this method and return the permission
     * that best represents the permission required to make
     * a connection to the URL. For example, a {@code URLConnection}
     * representing a {@code file:} URL would return a
     * {@code java.io.FilePermission} object.
     *
     * <p>The permission returned may dependent upon the state of the
     * connection. For example, the permission before connecting may be
     * different from that after connecting. For example, an HTTP
     * sever, say foo.com, may redirect the connection to a different
     * host, say bar.com. Before connecting the permission returned by
     * the connection will represent the permission needed to connect
     * to foo.com, while the permission returned after connecting will
     * be to bar.com.
     *
     * <p>Permissions are generally used for two purposes: to protect
     * caches of objects obtained through URLConnections, and to check
     * the right of a recipient to learn about a particular URL. In
     * the first case, the permission should be obtained
     * <em>after</em> the object has been obtained. For example, in an
     * HTTP connection, this will represent the permission to connect
     * to the host from which the data was ultimately fetched. In the
     * second case, the permission should be obtained and tested
     * <em>before</em> connecting.
     *
     * @return the permission object representing the permission
     * necessary to make the connection represented by this
     * URLConnection.
     *
     * @throws    IOException if the computation of the permission
     * requires network or file I/O and an exception occurs while
     * computing it.
     */
    public Permission getPermission() throws IOException {
        return SecurityConstants.ALL_PERMISSION;
    }

    /**
     * Returns an input stream that reads from this open connection.
     *
     * A SocketTimeoutException can be thrown when reading from the
     * returned input stream if the read timeout expires before data
     * is available for read.
     *
     * @return     an input stream that reads from this open connection.
     * @throws     IOException              if an I/O error occurs while
     *               creating the input stream.
     * @throws     UnknownServiceException  if the protocol does not support
     *               input.
     * @see #setReadTimeout(int)
     * @see #getReadTimeout()
     */
    public InputStream getInputStream() throws IOException {
        throw new UnknownServiceException("protocol doesn't support input");
    }

    /**
     * Returns an output stream that writes to this connection.
     *
     * @return     an output stream that writes to this connection.
     * @throws     IOException              if an I/O error occurs while
     *               creating the output stream.
     * @throws     UnknownServiceException  if the protocol does not support
     *               output.
     */
    public OutputStream getOutputStream() throws IOException {
        throw new UnknownServiceException("protocol doesn't support output");
    }

    /**
     * Returns a {@code String} representation of this URL connection.
     *
     * @return  a string representation of this {@code URLConnection}.
     */
    public String toString() {
        return this.getClass().getName() + ":" + url;
    }

    /**
     * Sets the value of the {@code doInput} field for this
     * {@code URLConnection} to the specified value.
     * <p>
     * A URL connection can be used for input and/or output.  Set the doInput
     * flag to true if you intend to use the URL connection for input,
     * false if not.  The default is true.
     *
     * @param   doinput   the new value.
     * @throws IllegalStateException if already connected
     * @see     java.net.URLConnection#doInput
     * @see #getDoInput()
     */
    public void setDoInput(boolean doinput) {
        checkConnected();
        doInput = doinput;
    }

    /**
     * Returns the value of this {@code URLConnection}'s
     * {@code doInput} flag.
     *
     * @return  the value of this {@code URLConnection}'s
     *          {@code doInput} flag.
     * @see     #setDoInput(boolean)
     */
    public boolean getDoInput() {
        return doInput;
    }

    /**
     * Sets the value of the {@code doOutput} field for this
     * {@code URLConnection} to the specified value.
     * <p>
     * A URL connection can be used for input and/or output.  Set the doOutput
     * flag to true if you intend to use the URL connection for output,
     * false if not.  The default is false.
     *
     * @param   dooutput   the new value.
     * @throws IllegalStateException if already connected
     * @see #getDoOutput()
     */
    public void setDoOutput(boolean dooutput) {
        checkConnected();
        doOutput = dooutput;
    }

    /**
     * Returns the value of this {@code URLConnection}'s
     * {@code doOutput} flag.
     *
     * @return  the value of this {@code URLConnection}'s
     *          {@code doOutput} flag.
     * @see     #setDoOutput(boolean)
     */
    public boolean getDoOutput() {
        return doOutput;
    }

    /**
     * Set the value of the {@code allowUserInteraction} field of
     * this {@code URLConnection}.
     *
     * @param   allowuserinteraction   the new value.
     * @throws IllegalStateException if already connected
     * @see     #getAllowUserInteraction()
     */
    public void setAllowUserInteraction(boolean allowuserinteraction) {
        checkConnected();
        allowUserInteraction = allowuserinteraction;
    }

    /**
     * Returns the value of the {@code allowUserInteraction} field for
     * this object.
     *
     * @return  the value of the {@code allowUserInteraction} field for
     *          this object.
     * @see     #setAllowUserInteraction(boolean)
     */
    public boolean getAllowUserInteraction() {
        return allowUserInteraction;
    }

    /**
     * Sets the default value of the
     * {@code allowUserInteraction} field for all future
     * {@code URLConnection} objects to the specified value.
     *
     * @param   defaultallowuserinteraction   the new value.
     * @see     #getDefaultAllowUserInteraction()
     */
    public static void setDefaultAllowUserInteraction(boolean defaultallowuserinteraction) {
        defaultAllowUserInteraction = defaultallowuserinteraction;
    }

    /**
     * Returns the default value of the {@code allowUserInteraction}
     * field.
     * <p>
     * This default is "sticky", being a part of the static state of all
     * URLConnections.  This flag applies to the next, and all following
     * URLConnections that are created.
     *
     * @return  the default value of the {@code allowUserInteraction}
     *          field.
     * @see     #setDefaultAllowUserInteraction(boolean)
     */
    public static boolean getDefaultAllowUserInteraction() {
        return defaultAllowUserInteraction;
    }

    /**
     * Sets the value of the {@code useCaches} field of this
     * {@code URLConnection} to the specified value.
     * <p>
     * Some protocols do caching of documents.  Occasionally, it is important
     * to be able to "tunnel through" and ignore the caches (e.g., the
     * "reload" button in a browser).  If the UseCaches flag on a connection
     * is true, the connection is allowed to use whatever caches it can.
     *  If false, caches are to be ignored.
     *  The default value comes from defaultUseCaches, which defaults to
     * true. A default value can also be set per-protocol using
     * {@link #setDefaultUseCaches(String,boolean)}.
     *
     * @param usecaches a {@code boolean} indicating whether
     * or not to allow caching
     * @throws IllegalStateException if already connected
     * @see #getUseCaches()
     */
    public void setUseCaches(boolean usecaches) {
        checkConnected();
        useCaches = usecaches;
    }

    /**
     * Returns the value of this {@code URLConnection}'s
     * {@code useCaches} field.
     *
     * @return  the value of this {@code URLConnection}'s
     *          {@code useCaches} field.
     * @see #setUseCaches(boolean)
     */
    public boolean getUseCaches() {
        return useCaches;
    }

    /**
     * Sets the value of the {@code ifModifiedSince} field of
     * this {@code URLConnection} to the specified value.
     *
     * @param   ifmodifiedsince   the new value.
     * @throws IllegalStateException if already connected
     * @see     #getIfModifiedSince()
     */
    public void setIfModifiedSince(long ifmodifiedsince) {
        checkConnected();
        ifModifiedSince = ifmodifiedsince;
    }

    /**
     * Returns the value of this object's {@code ifModifiedSince} field.
     *
     * @return  the value of this object's {@code ifModifiedSince} field.
     * @see #setIfModifiedSince(long)
     */
    public long getIfModifiedSince() {
        return ifModifiedSince;
    }

    /**
     * Returns the default value of a {@code URLConnection}'s
     * {@code useCaches} flag.
     * <p>
     * This default is "sticky", being a part of the static state of all
     * URLConnections.  This flag applies to the next, and all following
     * URLConnections that are created. This default value can be over-ridden
     * per protocol using {@link #setDefaultUseCaches(String,boolean)}
     *
     * @return  the default value of a {@code URLConnection}'s
     *          {@code useCaches} flag.
     * @see     #setDefaultUseCaches(boolean)
     */
    public boolean getDefaultUseCaches() {
        return defaultUseCaches;
    }

    /**
     * Sets the default value of the {@code useCaches} field to the
     * specified value. This default value can be over-ridden
     * per protocol using {@link #setDefaultUseCaches(String,boolean)}
     *
     * @param   defaultusecaches   the new value.
     * @see     #getDefaultUseCaches()
     */
    public void setDefaultUseCaches(boolean defaultusecaches) {
        defaultUseCaches = defaultusecaches;
    }

    /**
     * Sets the default value of the {@code useCaches} field for the named
     * protocol to the given value. This value overrides any default setting
     * set by {@link #setDefaultUseCaches(boolean)} for the given protocol.
     * Successive calls to this method change the setting and affect the
     * default value for all future connections of that protocol. The protocol
     * name is case insensitive.
     *
     * @param   protocol the protocol to set the default for
     * @param   defaultVal whether caching is enabled by default for the given protocol
     * @since 9
     */
    public static void setDefaultUseCaches(String protocol, boolean defaultVal) {
        protocol = protocol.toLowerCase(Locale.US);
        defaultCaching.put(protocol, defaultVal);
    }

    /**
     * Returns the default value of the {@code useCaches} flag for the given protocol. If
     * {@link #setDefaultUseCaches(String,boolean)} was called for the given protocol,
     * then that value is returned. Otherwise, if {@link #setDefaultUseCaches(boolean)}
     * was called, then that value is returned. If neither method was called,
     * the return value is {@code true}. The protocol name is case insensitive.
     *
     * @param protocol the protocol whose defaultUseCaches setting is required
     * @return  the default value of the {@code useCaches} flag for the given protocol.
     * @since 9
     */
    public static boolean getDefaultUseCaches(String protocol) {
        Boolean protoDefault = defaultCaching.get(protocol.toLowerCase(Locale.US));
        if (protoDefault != null) {
            return protoDefault.booleanValue();
        } else {
            return defaultUseCaches;
        }
    }

    /**
     * Sets the general request property. If a property with the key already
     * exists, overwrite its value with the new value.
     *
     * <p> NOTE: HTTP requires all request properties which can
     * legally have multiple instances with the same key
     * to use a comma-separated list syntax which enables multiple
     * properties to be appended into a single property.
     *
     * @param   key     the keyword by which the request is known
     *                  (e.g., "{@code Accept}").
     * @param   value   the value associated with it.
     * @throws IllegalStateException if already connected
     * @throws NullPointerException if key is {@code null}
     * @see #getRequestProperty(java.lang.String)
     */
    public void setRequestProperty(String key, String value) {
        checkConnected();
        if (key == null)
            throw new NullPointerException ("key is null");

        if (requests == null)
            requests = new MessageHeader();

        requests.set(key, value);
    }

    /**
     * Adds a general request property specified by a
     * key-value pair.  This method will not overwrite
     * existing values associated with the same key.
     *
     * @param   key     the keyword by which the request is known
     *                  (e.g., "{@code Accept}").
     * @param   value  the value associated with it.
     * @throws IllegalStateException if already connected
     * @throws NullPointerException if key is null
     * @see #getRequestProperties()
     * @since 1.4
     */
    public void addRequestProperty(String key, String value) {
        checkConnected();
        if (key == null)
            throw new NullPointerException ("key is null");

        if (requests == null)
            requests = new MessageHeader();

        requests.add(key, value);
    }


    /**
     * Returns the value of the named general request property for this
     * connection.
     *
     * @param key the keyword by which the request is known (e.g., "Accept").
     * @return  the value of the named general request property for this
     *           connection. If key is null, then null is returned.
     * @throws IllegalStateException if already connected
     * @see #setRequestProperty(java.lang.String, java.lang.String)
     */
    public String getRequestProperty(String key) {
        checkConnected();

        if (requests == null)
            return null;

        return requests.findValue(key);
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
     * @throws IllegalStateException if already connected
     * @since 1.4
     */
    public Map<String,List<String>> getRequestProperties() {
        checkConnected();

        if (requests == null)
            return Collections.emptyMap();

        return requests.getHeaders(null);
    }

    /**
     * Sets the default value of a general request property. When a
     * {@code URLConnection} is created, it is initialized with
     * these properties.
     *
     * @param   key     the keyword by which the request is known
     *                  (e.g., "{@code Accept}").
     * @param   value   the value associated with the key.
     *
     * @see java.net.URLConnection#setRequestProperty(java.lang.String,java.lang.String)
     *
     * @deprecated The instance specific setRequestProperty method
     * should be used after an appropriate instance of URLConnection
     * is obtained. Invoking this method will have no effect.
     *
     * @see #getDefaultRequestProperty(java.lang.String)
     */
    @Deprecated
    public static void setDefaultRequestProperty(String key, String value) {
    }

    /**
     * Returns the value of the default request property. Default request
     * properties are set for every connection.
     *
     * @param key the keyword by which the request is known (e.g., "Accept").
     * @return  the value of the default request property
     * for the specified key.
     *
     * @see java.net.URLConnection#getRequestProperty(java.lang.String)
     *
     * @deprecated The instance specific getRequestProperty method
     * should be used after an appropriate instance of URLConnection
     * is obtained.
     *
     * @see #setDefaultRequestProperty(java.lang.String, java.lang.String)
     */
    @Deprecated
    public static String getDefaultRequestProperty(String key) {
        return null;
    }

    /**
     * The ContentHandler factory.
     */
    private static volatile ContentHandlerFactory factory;

    /**
     * Sets the {@code ContentHandlerFactory} of an
     * application. It can be called at most once by an application.
     * <p>
     * The {@code ContentHandlerFactory} instance is used to
     * construct a content handler from a content type.
     * <p>
     * If there is a security manager, this method first calls
     * the security manager's {@code checkSetFactory} method
     * to ensure the operation is allowed.
     * This could result in a SecurityException.
     *
     * @param      fac   the desired factory.
     * @throws     Error  if the factory has already been defined.
     * @throws     SecurityException  if a security manager exists and its
     *             {@code checkSetFactory} method doesn't allow the operation.
     * @see        java.net.ContentHandlerFactory
     * @see        java.net.URLConnection#getContent()
     * @see        SecurityManager#checkSetFactory
     */
    public static synchronized void setContentHandlerFactory(ContentHandlerFactory fac) {
        if (factory != null) {
            throw new Error("factory already defined");
        }
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkSetFactory();
        }
        factory = fac;
    }

    private static final Hashtable<String, ContentHandler> handlers = new Hashtable<>();

    /**
     * Gets the Content Handler appropriate for this connection.
     */
    private ContentHandler getContentHandler() throws UnknownServiceException {
        String contentType = stripOffParameters(getContentType());
        if (contentType == null) {
            throw new UnknownServiceException("no content-type");
        }

        ContentHandler handler = handlers.get(contentType);
        if (handler != null)
            return handler;

        if (factory != null) {
            handler = factory.createContentHandler(contentType);
            if (handler != null)
                return handler;
        }

        handler = lookupContentHandlerViaProvider(contentType);

        if (handler != null) {
            ContentHandler h = handlers.putIfAbsent(contentType, handler);
            return Objects.requireNonNullElse(h, handler);
        }

        try {
            handler = lookupContentHandlerClassFor(contentType);
        } catch (Exception e) {
            e.printStackTrace();
            handler = UnknownContentHandler.INSTANCE;
        }

        assert handler != null;

        ContentHandler h = handlers.putIfAbsent(contentType, handler);
        return Objects.requireNonNullElse(h, handler);
    }

    /*
     * Media types are in the format: type/subtype*(; parameter).
     * For looking up the content handler, we should ignore those
     * parameters.
     */
    private String stripOffParameters(String contentType)
    {
        if (contentType == null)
            return null;
        int index = contentType.indexOf(';');

        if (index > 0)
            return contentType.substring(0, index);
        else
            return contentType;
    }

    private static final String contentClassPrefix = "sun.net.www.content";
    private static final String contentPathProp = "java.content.handler.pkgs";

    /**
     * Looks for a content handler in a user-definable set of places.
     * By default it looks in {@value #contentClassPrefix}, but users can define
     * a vertical-bar delimited set of class prefixes to search through in
     * addition by defining the {@value #contentPathProp} property.
     * The class name must be of the form:
     * <pre>
     *     {package-prefix}.{major}.{minor}
     * e.g.
     *     YoyoDyne.experimental.text.plain
     * </pre>
     */
    private ContentHandler lookupContentHandlerClassFor(String contentType) {
        String contentHandlerClassName = typeToPackageName(contentType);

        String contentHandlerPkgPrefixes = getContentHandlerPkgPrefixes();

        StringTokenizer packagePrefixIter =
            new StringTokenizer(contentHandlerPkgPrefixes, "|");

        while (packagePrefixIter.hasMoreTokens()) {
            String packagePrefix = packagePrefixIter.nextToken().trim();

            try {
                String clsName = packagePrefix + "." + contentHandlerClassName;
                Class<?> cls = null;
                try {
                    cls = Class.forName(clsName);
                } catch (ClassNotFoundException e) {
                    ClassLoader cl = ClassLoader.getSystemClassLoader();
                    if (cl != null) {
                        cls = cl.loadClass(clsName);
                    }
                }
                if (cls != null) {
                    @SuppressWarnings("deprecation")
                    Object tmp = cls.newInstance();
                    return (ContentHandler) tmp;
                }
            } catch(Exception ignored) { }
        }

        return UnknownContentHandler.INSTANCE;
    }

    @SuppressWarnings("removal")
    private ContentHandler lookupContentHandlerViaProvider(String contentType) {
        return AccessController.doPrivileged(
                new PrivilegedAction<>() {
                    @Override
                    public ContentHandler run() {
                        ClassLoader cl = ClassLoader.getSystemClassLoader();
                        ServiceLoader<ContentHandlerFactory> sl =
                                ServiceLoader.load(ContentHandlerFactory.class, cl);

                        Iterator<ContentHandlerFactory> iterator = sl.iterator();

                        ContentHandler handler = null;
                        while (iterator.hasNext()) {
                            ContentHandlerFactory f;
                            try {
                                f = iterator.next();
                            } catch (ServiceConfigurationError e) {
                                if (e.getCause() instanceof SecurityException) {
                                    continue;
                                }
                                throw e;
                            }
                            handler = f.createContentHandler(contentType);
                            if (handler != null) {
                                break;
                            }
                        }
                        return handler;
                    }
                });
    }

    /**
     * Utility function to map a MIME content type into an equivalent
     * pair of class name components.  For example: "text/html" would
     * be returned as "text.html"
     */
    private String typeToPackageName(String contentType) {
        // make sure we canonicalize the class name: all lower case
        contentType = contentType.toLowerCase();
        int len = contentType.length();
        char nm[] = new char[len];
        contentType.getChars(0, len, nm, 0);
        for (int i = 0; i < len; i++) {
            char c = nm[i];
            if (c == '/') {
                nm[i] = '.';
            } else if (!('A' <= c && c <= 'Z' ||
                       'a' <= c && c <= 'z' ||
                       '0' <= c && c <= '9')) {
                nm[i] = '_';
            }
        }
        return new String(nm);
    }


    /**
     * Returns a vertical bar separated list of package prefixes for potential
     * content handlers.  Tries to get the java.content.handler.pkgs property
     * to use as a set of package prefixes to search.  Whether or not
     * that property has been defined, the {@value #contentClassPrefix}
     * is always the last one on the returned package list.
     */
    private String getContentHandlerPkgPrefixes() {
        String packagePrefixList =
                GetPropertyAction.privilegedGetProperty(contentPathProp, "");

        if (packagePrefixList != "") {
            packagePrefixList += "|";
        }

        return packagePrefixList + contentClassPrefix;
    }

    /**
     * Tries to determine the content type of an object, based
     * on the specified "file" component of a URL.
     * This is a convenience method that can be used by
     * subclasses that override the {@code getContentType} method.
     *
     * @param   fname   a filename.
     * @return  a guess as to what the content type of the object is,
     *          based upon its file name.
     * @see     java.net.URLConnection#getContentType()
     */
    public static String guessContentTypeFromName(String fname) {
        return getFileNameMap().getContentTypeFor(fname);
    }

    /**
     * Tries to determine the type of an input stream based on the
     * characters at the beginning of the input stream. This method can
     * be used by subclasses that override the
     * {@code getContentType} method.
     * <p>
     * Ideally, this routine would not be needed. But many
     * {@code http} servers return the incorrect content type; in
     * addition, there are many nonstandard extensions. Direct inspection
     * of the bytes to determine the content type is often more accurate
     * than believing the content type claimed by the {@code http} server.
     *
     * @param      is   an input stream that supports marks.
     * @return     a guess at the content type, or {@code null} if none
     *             can be determined.
     * @throws     IOException  if an I/O error occurs while reading the
     *               input stream.
     * @see        java.io.InputStream#mark(int)
     * @see        java.io.InputStream#markSupported()
     * @see        java.net.URLConnection#getContentType()
     */
    public static String guessContentTypeFromStream(InputStream is)
                        throws IOException {
        // If we can't read ahead safely, just give up on guessing
        if (!is.markSupported())
            return null;

        is.mark(16);
        int c1 = is.read();
        int c2 = is.read();
        int c3 = is.read();
        int c4 = is.read();
        int c5 = is.read();
        int c6 = is.read();
        int c7 = is.read();
        int c8 = is.read();
        int c9 = is.read();
        int c10 = is.read();
        int c11 = is.read();
        int c12 = is.read();
        int c13 = is.read();
        int c14 = is.read();
        int c15 = is.read();
        int c16 = is.read();
        is.reset();

        if (c1 == 0xCA && c2 == 0xFE && c3 == 0xBA && c4 == 0xBE) {
            return "application/java-vm";
        }

        if (c1 == 0xAC && c2 == 0xED) {
            // next two bytes are version number, currently 0x00 0x05
            return "application/x-java-serialized-object";
        }

        if (c1 == '<') {
            if (c2 == '!'
                || ((c2 == 'h' && (c3 == 't' && c4 == 'm' && c5 == 'l' ||
                                   c3 == 'e' && c4 == 'a' && c5 == 'd') ||
                (c2 == 'b' && c3 == 'o' && c4 == 'd' && c5 == 'y'))) ||
                ((c2 == 'H' && (c3 == 'T' && c4 == 'M' && c5 == 'L' ||
                                c3 == 'E' && c4 == 'A' && c5 == 'D') ||
                (c2 == 'B' && c3 == 'O' && c4 == 'D' && c5 == 'Y')))) {
                return "text/html";
            }

            if (c2 == '?' && c3 == 'x' && c4 == 'm' && c5 == 'l' && c6 == ' ') {
                return "application/xml";
            }
        }

        // big and little (identical) endian UTF-8 encodings, with BOM
        if (c1 == 0xef &&  c2 == 0xbb &&  c3 == 0xbf) {
            if (c4 == '<' &&  c5 == '?' &&  c6 == 'x') {
                return "application/xml";
            }
        }

        // big and little endian UTF-16 encodings, with byte order mark
        if (c1 == 0xfe && c2 == 0xff) {
            if (c3 == 0 && c4 == '<' && c5 == 0 && c6 == '?' &&
                c7 == 0 && c8 == 'x') {
                return "application/xml";
            }
        }

        if (c1 == 0xff && c2 == 0xfe) {
            if (c3 == '<' && c4 == 0 && c5 == '?' && c6 == 0 &&
                c7 == 'x' && c8 == 0) {
                return "application/xml";
            }
        }

        // big and little endian UTF-32 encodings, with BOM
        if (c1 == 0x00 &&  c2 == 0x00 &&  c3 == 0xfe &&  c4 == 0xff) {
            if (c5  == 0 && c6  == 0 && c7  == 0 && c8  == '<' &&
                c9  == 0 && c10 == 0 && c11 == 0 && c12 == '?' &&
                c13 == 0 && c14 == 0 && c15 == 0 && c16 == 'x') {
                return "application/xml";
            }
        }

        if (c1 == 0xff &&  c2 == 0xfe &&  c3 == 0x00 &&  c4 == 0x00) {
            if (c5  == '<' && c6  == 0 && c7  == 0 && c8  == 0 &&
                c9  == '?' && c10 == 0 && c11 == 0 && c12 == 0 &&
                c13 == 'x' && c14 == 0 && c15 == 0 && c16 == 0) {
                return "application/xml";
            }
        }

        if (c1 == 'G' && c2 == 'I' && c3 == 'F' && c4 == '8') {
            return "image/gif";
        }

        if (c1 == '#' && c2 == 'd' && c3 == 'e' && c4 == 'f') {
            return "image/x-bitmap";
        }

        if (c1 == '!' && c2 == ' ' && c3 == 'X' && c4 == 'P' &&
                        c5 == 'M' && c6 == '2') {
            return "image/x-pixmap";
        }

        if (c1 == 137 && c2 == 80 && c3 == 78 &&
                c4 == 71 && c5 == 13 && c6 == 10 &&
                c7 == 26 && c8 == 10) {
            return "image/png";
        }

        if (c1 == 0xFF && c2 == 0xD8 && c3 == 0xFF) {
            if (c4 == 0xE0 || c4 == 0xEE) {
                return "image/jpeg";
            }

            /**
             * File format used by digital cameras to store images.
             * Exif Format can be read by any application supporting
             * JPEG. Exif Spec can be found at:
             * http://www.pima.net/standards/it10/PIMA15740/Exif_2-1.PDF
             */
            if ((c4 == 0xE1) &&
                (c7 == 'E' && c8 == 'x' && c9 == 'i' && c10 =='f' &&
                 c11 == 0)) {
                return "image/jpeg";
            }
        }

        if ((c1 == 0x49 && c2 == 0x49 && c3 == 0x2a && c4 == 0x00)
            || (c1 == 0x4d && c2 == 0x4d && c3 == 0x00 && c4 == 0x2a)) {
            return "image/tiff";
        }

        if (c1 == 0xD0 && c2 == 0xCF && c3 == 0x11 && c4 == 0xE0 &&
            c5 == 0xA1 && c6 == 0xB1 && c7 == 0x1A && c8 == 0xE1) {

            /* Above is signature of Microsoft Structured Storage.
             * Below this, could have tests for various SS entities.
             * For now, just test for FlashPix.
             */
            if (checkfpx(is)) {
                return "image/vnd.fpx";
            }
        }

        if (c1 == 0x2E && c2 == 0x73 && c3 == 0x6E && c4 == 0x64) {
            return "audio/basic";  // .au format, big endian
        }

        if (c1 == 0x64 && c2 == 0x6E && c3 == 0x73 && c4 == 0x2E) {
            return "audio/basic";  // .au format, little endian
        }

        if (c1 == 'R' && c2 == 'I' && c3 == 'F' && c4 == 'F') {
            /* I don't know if this is official but evidence
             * suggests that .wav files start with "RIFF" - brown
             */
            return "audio/x-wav";
        }
        return null;
    }

    /**
     * Check for FlashPix image data in InputStream is.  Return true if
     * the stream has FlashPix data, false otherwise.  Before calling this
     * method, the stream should have already been checked to be sure it
     * contains Microsoft Structured Storage data.
     */
    private static boolean checkfpx(InputStream is) throws IOException {

        /* Test for FlashPix image data in Microsoft Structured Storage format.
         * In general, should do this with calls to an SS implementation.
         * Lacking that, need to dig via offsets to get to the FlashPix
         * ClassID.  Details:
         *
         * Offset to Fpx ClsID from beginning of stream should be:
         *
         * FpxClsidOffset = rootEntryOffset + clsidOffset
         *
         * where: clsidOffset = 0x50.
         *        rootEntryOffset = headerSize + sectorSize*sectDirStart
         *                          + 128*rootEntryDirectory
         *
         *        where:  headerSize = 0x200 (always)
         *                sectorSize = 2 raised to power of uSectorShift,
         *                             which is found in the header at
         *                             offset 0x1E.
         *                sectDirStart = found in the header at offset 0x30.
         *                rootEntryDirectory = in general, should search for
         *                                     directory labelled as root.
         *                                     We will assume value of 0 (i.e.,
         *                                     rootEntry is in first directory)
         */

        // Mark the stream so we can reset it. 0x100 is enough for the first
        // few reads, but the mark will have to be reset and set again once
        // the offset to the root directory entry is computed. That offset
        // can be very large and isn't know until the stream has been read from
        is.mark(0x100);

        // Get the byte ordering located at 0x1E. 0xFE is Intel,
        // 0xFF is other
        long toSkip = (long)0x1C;
        long posn;

        if ((posn = skipForward(is, toSkip)) < toSkip) {
          is.reset();
          return false;
        }

        int c[] = new int[16];
        if (readBytes(c, 2, is) < 0) {
            is.reset();
            return false;
        }

        int byteOrder = c[0];

        posn+=2;
        int uSectorShift;
        if (readBytes(c, 2, is) < 0) {
            is.reset();
            return false;
        }

        if(byteOrder == 0xFE) {
            uSectorShift = c[0];
            uSectorShift += c[1] << 8;
        }
        else {
            uSectorShift = c[0] << 8;
            uSectorShift += c[1];
        }

        posn += 2;
        toSkip = (long)0x30 - posn;
        long skipped = 0;
        if ((skipped = skipForward(is, toSkip)) < toSkip) {
          is.reset();
          return false;
        }
        posn += skipped;

        if (readBytes(c, 4, is) < 0) {
            is.reset();
            return false;
        }

        int sectDirStart;
        if(byteOrder == 0xFE) {
            sectDirStart = c[0];
            sectDirStart += c[1] << 8;
            sectDirStart += c[2] << 16;
            sectDirStart += c[3] << 24;
        } else {
            sectDirStart =  c[0] << 24;
            sectDirStart += c[1] << 16;
            sectDirStart += c[2] << 8;
            sectDirStart += c[3];
        }
        posn += 4;
        is.reset(); // Reset back to the beginning

        toSkip = 0x200L + (long)(1<<uSectorShift)*sectDirStart + 0x50L;

        // Sanity check!
        if (toSkip < 0) {
            return false;
        }

        /*
         * How far can we skip? Is there any performance problem here?
         * This skip can be fairly long, at least 0x4c650 in at least
         * one case. Have to assume that the skip will fit in an int.
         * Leave room to read whole root dir
         */
        is.mark((int)toSkip+0x30);

        if ((skipForward(is, toSkip)) < toSkip) {
            is.reset();
            return false;
        }

        /* should be at beginning of ClassID, which is as follows
         * (in Intel byte order):
         *    00 67 61 56 54 C1 CE 11 85 53 00 AA 00 A1 F9 5B
         *
         * This is stored from Windows as long,short,short,char[8]
         * so for byte order changes, the order only changes for
         * the first 8 bytes in the ClassID.
         *
         * Test against this, ignoring second byte (Intel) since
         * this could change depending on part of Fpx file we have.
         */

        if (readBytes(c, 16, is) < 0) {
            is.reset();
            return false;
        }

        // intel byte order
        if (byteOrder == 0xFE &&
            c[0] == 0x00 && c[2] == 0x61 && c[3] == 0x56 &&
            c[4] == 0x54 && c[5] == 0xC1 && c[6] == 0xCE &&
            c[7] == 0x11 && c[8] == 0x85 && c[9] == 0x53 &&
            c[10]== 0x00 && c[11]== 0xAA && c[12]== 0x00 &&
            c[13]== 0xA1 && c[14]== 0xF9 && c[15]== 0x5B) {
            is.reset();
            return true;
        }

        // non-intel byte order
        else if (c[3] == 0x00 && c[1] == 0x61 && c[0] == 0x56 &&
            c[5] == 0x54 && c[4] == 0xC1 && c[7] == 0xCE &&
            c[6] == 0x11 && c[8] == 0x85 && c[9] == 0x53 &&
            c[10]== 0x00 && c[11]== 0xAA && c[12]== 0x00 &&
            c[13]== 0xA1 && c[14]== 0xF9 && c[15]== 0x5B) {
            is.reset();
            return true;
        }
        is.reset();
        return false;
    }

    /**
     * Tries to read the specified number of bytes from the stream
     * Returns -1, If EOF is reached before len bytes are read, returns 0
     * otherwise
     */
    private static int readBytes(int c[], int len, InputStream is)
                throws IOException {

        byte buf[] = new byte[len];
        if (is.read(buf, 0, len) < len) {
            return -1;
        }

        // fill the passed in int array
        for (int i = 0; i < len; i++) {
             c[i] = buf[i] & 0xff;
        }
        return 0;
    }


    /**
     * Skips through the specified number of bytes from the stream
     * until either EOF is reached, or the specified
     * number of bytes have been skipped
     */
    private static long skipForward(InputStream is, long toSkip)
                throws IOException {

        long eachSkip = 0;
        long skipped = 0;

        while (skipped != toSkip) {
            eachSkip = is.skip(toSkip - skipped);

            // check if EOF is reached
            if (eachSkip <= 0) {
                if (is.read() == -1) {
                    return skipped ;
                } else {
                    skipped++;
                }
            }
            skipped += eachSkip;
        }
        return skipped;
    }

    private void checkConnected() {
        if (connected)
            throw new IllegalStateException("Already connected");
    }
}

class UnknownContentHandler extends ContentHandler {
    static final ContentHandler INSTANCE = new UnknownContentHandler();

    public Object getContent(URLConnection uc) throws IOException {
        return uc.getInputStream();
    }
}
