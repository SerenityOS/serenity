/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jmx.remote.util;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import java.security.AccessController;

import javax.management.ObjectName;
import javax.management.MBeanServer;
import javax.management.InstanceNotFoundException;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServerFactory;
import com.sun.jmx.mbeanserver.GetPropertyAction;
import com.sun.jmx.remote.security.NotificationAccessController;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorServer;

public class EnvHelp {

    /**
     * Name of the attribute that specifies a default class loader
     * object.
     * The value associated with this attribute is a ClassLoader object.
     */
    private static final String DEFAULT_CLASS_LOADER =
        JMXConnectorFactory.DEFAULT_CLASS_LOADER;

    /**
     * Name of the attribute that specifies a default class loader
     * ObjectName.
     * The value associated with this attribute is an ObjectName object.
     */
    private static final String DEFAULT_CLASS_LOADER_NAME =
        JMXConnectorServerFactory.DEFAULT_CLASS_LOADER_NAME;

    /**
     * Get the Connector Server default class loader.
     * <p>
     * Returns:
     * <ul>
     * <li>
     *     The ClassLoader object found in <var>env</var> for
     *     <code>jmx.remote.default.class.loader</code>, if any.
     * </li>
     * <li>
     *     The ClassLoader pointed to by the ObjectName found in
     *     <var>env</var> for <code>jmx.remote.default.class.loader.name</code>,
     *     and registered in <var>mbs</var> if any.
     * </li>
     * <li>
     *     The current thread's context classloader otherwise.
     * </li>
     * </ul>
     *
     * @param env Environment attributes.
     * @param mbs The MBeanServer for which the connector server provides
     * remote access.
     *
     * @return the connector server's default class loader.
     *
     * @exception IllegalArgumentException if one of the following is true:
     * <ul>
     * <li>both
     *     <code>jmx.remote.default.class.loader</code> and
     *     <code>jmx.remote.default.class.loader.name</code> are specified,
     * </li>
     * <li>or
     *     <code>jmx.remote.default.class.loader</code> is not
     *     an instance of {@link ClassLoader},
     * </li>
     * <li>or
     *     <code>jmx.remote.default.class.loader.name</code> is not
     *     an instance of {@link ObjectName},
     * </li>
     * <li>or
     *     <code>jmx.remote.default.class.loader.name</code> is specified
     *     but <var>mbs</var> is null.
     * </li>
     * </ul>
     * @exception InstanceNotFoundException if
     * <code>jmx.remote.default.class.loader.name</code> is specified
     * and the ClassLoader MBean is not found in <var>mbs</var>.
     */
    public static ClassLoader resolveServerClassLoader(Map<String, ?> env,
                                                       MBeanServer mbs)
        throws InstanceNotFoundException {

        if (env == null)
            return Thread.currentThread().getContextClassLoader();

        Object loader = env.get(DEFAULT_CLASS_LOADER);
        Object name   = env.get(DEFAULT_CLASS_LOADER_NAME);

        if (loader != null && name != null) {
            final String msg = "Only one of " +
                DEFAULT_CLASS_LOADER + " or " +
                DEFAULT_CLASS_LOADER_NAME +
                " should be specified.";
            throw new IllegalArgumentException(msg);
        }

        if (loader == null && name == null)
            return Thread.currentThread().getContextClassLoader();

        if (loader != null) {
            if (loader instanceof ClassLoader) {
                return (ClassLoader) loader;
            } else {
                final String msg =
                    "ClassLoader object is not an instance of " +
                    ClassLoader.class.getName() + " : " +
                    loader.getClass().getName();
                throw new IllegalArgumentException(msg);
            }
        }

        ObjectName on;
        if (name instanceof ObjectName) {
            on = (ObjectName) name;
        } else {
            final String msg =
                "ClassLoader name is not an instance of " +
                ObjectName.class.getName() + " : " +
                name.getClass().getName();
            throw new IllegalArgumentException(msg);
        }

        if (mbs == null)
            throw new IllegalArgumentException("Null MBeanServer object");

        return mbs.getClassLoader(on);
    }

    /**
     * Get the Connector Client default class loader.
     * <p>
     * Returns:
     * <ul>
     * <li>
     *     The ClassLoader object found in <var>env</var> for
     *     <code>jmx.remote.default.class.loader</code>, if any.
     * </li>
     * <li>The {@code Thread.currentThread().getContextClassLoader()}
     *     otherwise.
     * </li>
     * </ul>
     * <p>
     * Usually a Connector Client will call
     * <pre>
     * ClassLoader dcl = EnvHelp.resolveClientClassLoader(env);
     * </pre>
     * in its <code>connect(Map env)</code> method.
     *
     * @return The connector client default class loader.
     *
     * @exception IllegalArgumentException if
     * <code>jmx.remote.default.class.loader</code> is specified
     * and is not an instance of {@link ClassLoader}.
     */
    public static ClassLoader resolveClientClassLoader(Map<String, ?> env) {

        if (env == null)
            return Thread.currentThread().getContextClassLoader();

        Object loader = env.get(DEFAULT_CLASS_LOADER);

        if (loader == null)
            return Thread.currentThread().getContextClassLoader();

        if (loader instanceof ClassLoader) {
            return (ClassLoader) loader;
        } else {
            final String msg =
                "ClassLoader object is not an instance of " +
                ClassLoader.class.getName() + " : " +
                loader.getClass().getName();
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Initialize the cause field of a {@code Throwable} object.
     *
     * @param throwable The {@code Throwable} on which the cause is set.
     * @param cause The cause to set on the supplied {@code Throwable}.
     * @return the {@code Throwable} with the cause field initialized.
     */
    public static <T extends Throwable> T initCause(T throwable,
                                                    Throwable cause) {
        throwable.initCause(cause);
        return throwable;
    }

    /**
     * Returns the cause field of a {@code Throwable} object.
     * The cause field can be got only if <var>t</var> has an
     * {@link Throwable#getCause()} method (JDK Version {@literal >=} 1.4)
     * @param t {@code Throwable} on which the cause must be set.
     * @return the cause if getCause() succeeded and the got value is not
     * null, otherwise return the <var>t</var>.
     */
    public static Throwable getCause(Throwable t) {
        Throwable ret = t;

        try {
            java.lang.reflect.Method getCause =
                t.getClass().getMethod("getCause", (Class<?>[]) null);
            ret = (Throwable)getCause.invoke(t, (Object[]) null);

        } catch (Exception e) {
            // OK.
            // it must be older than 1.4.
        }
        return (ret != null) ? ret: t;
    }


    /**
     * Name of the attribute that specifies the size of a notification
     * buffer for a connector server. The default value is 1000.
     */
    public static final String BUFFER_SIZE_PROPERTY =
        "jmx.remote.x.notification.buffer.size";


    /**
     * Returns the size of a notification buffer for a connector server.
     * The default value is 1000.
     */
    @SuppressWarnings("removal")
    public static int getNotifBufferSize(Map<String, ?> env) {
        int defaultQueueSize = 1000; // default value

        // keep it for the compability for the fix:
        // 6174229: Environment parameter should be notification.buffer.size
        // instead of buffer.size
        final String oldP = "jmx.remote.x.buffer.size";

        // the default value re-specified in the system
        try {
            GetPropertyAction act = new GetPropertyAction(BUFFER_SIZE_PROPERTY);
            String s = AccessController.doPrivileged(act);
            if (s != null) {
                defaultQueueSize = Integer.parseInt(s);
            } else { // try the old one
                act = new GetPropertyAction(oldP);
                s = AccessController.doPrivileged(act);
                if (s != null) {
                    defaultQueueSize = Integer.parseInt(s);
                }
            }
        } catch (RuntimeException e) {
            logger.warning("getNotifBufferSize",
                           "Can't use System property "+
                           BUFFER_SIZE_PROPERTY+ ": " + e);
              logger.debug("getNotifBufferSize", e);
        }

        int queueSize = defaultQueueSize;

        try {
            if (env.containsKey(BUFFER_SIZE_PROPERTY)) {
                queueSize = (int)EnvHelp.getIntegerAttribute(env,BUFFER_SIZE_PROPERTY,
                                            defaultQueueSize,0,
                                            Integer.MAX_VALUE);
            } else { // try the old one
                queueSize = (int)EnvHelp.getIntegerAttribute(env,oldP,
                                            defaultQueueSize,0,
                                            Integer.MAX_VALUE);
            }
        } catch (RuntimeException e) {
            logger.warning("getNotifBufferSize",
                           "Can't determine queuesize (using default): "+
                           e);
            logger.debug("getNotifBufferSize", e);
        }

        return queueSize;
    }

    /**
     * Name of the attribute that specifies the maximum number of
     * notifications that a client will fetch from its server. The
     * value associated with this attribute should be an
     * {@code Integer} object.  The default value is 1000.
     */
    public static final String MAX_FETCH_NOTIFS =
        "jmx.remote.x.notification.fetch.max";

    /**
     * Returns the maximum notification number which a client will
     * fetch every time.
     */
    public static int getMaxFetchNotifNumber(Map<String, ?> env) {
        return (int) getIntegerAttribute(env, MAX_FETCH_NOTIFS, 1000, 1,
                                         Integer.MAX_VALUE);
    }

    /**
     * Name of the attribute that specifies the timeout for a
     * client to fetch notifications from its server. The value
     * associated with this attribute should be a <code>Long</code>
     * object.  The default value is 60000 milliseconds.
     */
    public static final String FETCH_TIMEOUT =
        "jmx.remote.x.notification.fetch.timeout";

    /**
     * Returns the timeout for a client to fetch notifications.
     */
    public static long getFetchTimeout(Map<String, ?> env) {
        return getIntegerAttribute(env, FETCH_TIMEOUT, 60000L, 0,
                Long.MAX_VALUE);
    }

    /**
     * Name of the attribute that specifies an object that will check
     * accesses to add/removeNotificationListener and also attempts to
     * receive notifications.  The value associated with this attribute
     * should be a <code>NotificationAccessController</code> object.
     * The default value is null.
     * <p>
     * This field is not public because of its com.sun dependency.
     */
    public static final String NOTIF_ACCESS_CONTROLLER =
            "com.sun.jmx.remote.notification.access.controller";

    public static NotificationAccessController getNotificationAccessController(
            Map<String, ?> env) {
        return (env == null) ? null :
            (NotificationAccessController) env.get(NOTIF_ACCESS_CONTROLLER);
    }

    /**
     * Get an integer-valued attribute with name <code>name</code>
     * from <code>env</code>.  If <code>env</code> is null, or does
     * not contain an entry for <code>name</code>, return
     * <code>defaultValue</code>.  The value may be a Number, or it
     * may be a String that is parsable as a long.  It must be at
     * least <code>minValue</code> and at most<code>maxValue</code>.
     *
     * @throws IllegalArgumentException if <code>env</code> contains
     * an entry for <code>name</code> but it does not meet the
     * constraints above.
     */
    public static long getIntegerAttribute(Map<String, ?> env, String name,
                                           long defaultValue, long minValue,
                                           long maxValue) {
        final Object o;

        if (env == null || (o = env.get(name)) == null)
            return defaultValue;

        final long result;

        if (o instanceof Number)
            result = ((Number) o).longValue();
        else if (o instanceof String) {
            result = Long.parseLong((String) o);
            /* May throw a NumberFormatException, which is an
               IllegalArgumentException.  */
        } else {
            final String msg =
                "Attribute " + name + " value must be Integer or String: " + o;
            throw new IllegalArgumentException(msg);
        }

        if (result < minValue) {
            final String msg =
                "Attribute " + name + " value must be at least " + minValue +
                ": " + result;
            throw new IllegalArgumentException(msg);
        }

        if (result > maxValue) {
            final String msg =
                "Attribute " + name + " value must be at most " + maxValue +
                ": " + result;
            throw new IllegalArgumentException(msg);
        }

        return result;
    }

    public static final String DEFAULT_ORB="java.naming.corba.orb";

    /* Check that all attributes have a key that is a String.
       Could make further checks, e.g. appropriate types for attributes.  */
    public static void checkAttributes(Map<?, ?> attributes) {
        for (Object key : attributes.keySet()) {
            if (!(key instanceof String)) {
                final String msg =
                    "Attributes contain key that is not a string: " + key;
                throw new IllegalArgumentException(msg);
            }
        }
    }

    /* Return a writable map containing only those attributes that are
       serializable, and that are not hidden by
       jmx.remote.x.hidden.attributes or the default list of hidden
       attributes.  */
    public static <V> Map<String, V> filterAttributes(Map<String, V> attributes) {
        if (logger.traceOn()) {
            logger.trace("filterAttributes", "starts");
        }

        SortedMap<String, V> map = new TreeMap<String, V>(attributes);
        purgeUnserializable(map.values());
        hideAttributes(map);
        return map;
    }

    /**
     * Remove from the given Collection any element that is not a
     * serializable object.
     */
    private static void purgeUnserializable(Collection<?> objects) {
        logger.trace("purgeUnserializable", "starts");
        ObjectOutputStream oos = null;
        int i = 0;
        for (Iterator<?> it = objects.iterator(); it.hasNext(); i++) {
            Object v = it.next();

            if (v == null || v instanceof String) {
                if (logger.traceOn()) {
                    logger.trace("purgeUnserializable",
                                 "Value trivially serializable: " + v);
                }
                continue;
            }

            try {
                if (oos == null)
                    oos = new ObjectOutputStream(new SinkOutputStream());
                oos.writeObject(v);
                if (logger.traceOn()) {
                    logger.trace("purgeUnserializable",
                                 "Value serializable: " + v);
                }
            } catch (IOException e) {
                if (logger.traceOn()) {
                    logger.trace("purgeUnserializable",
                                 "Value not serializable: " + v + ": " +
                                 e);
                }
                it.remove();
                oos = null; // ObjectOutputStream invalid after exception
            }
        }
    }

    /**
     * The value of this attribute, if present, is a string specifying
     * what other attributes should not appear in
     * JMXConnectorServer.getAttributes().  It is a space-separated
     * list of attribute patterns, where each pattern is either an
     * attribute name, or an attribute prefix followed by a "*"
     * character.  The "*" has no special significance anywhere except
     * at the end of a pattern.  By default, this list is added to the
     * list defined by {@link #DEFAULT_HIDDEN_ATTRIBUTES} (which
     * uses the same format).  If the value of this attribute begins
     * with an "=", then the remainder of the string defines the
     * complete list of attribute patterns.
     */
    public static final String HIDDEN_ATTRIBUTES =
        "jmx.remote.x.hidden.attributes";

    /**
     * Default list of attributes not to show.
     * @see #HIDDEN_ATTRIBUTES
     */
    /* This list is copied directly from the spec, plus
       java.naming.security.*.  Most of the attributes here would have
       been eliminated from the map anyway because they are typically
       not serializable.  But just in case they are, we list them here
       to conform to the spec.  */
    public static final String DEFAULT_HIDDEN_ATTRIBUTES =
        "java.naming.security.* " +
        "jmx.remote.authenticator " +
        "jmx.remote.context " +
        "jmx.remote.default.class.loader " +
        "jmx.remote.message.connection.server " +
        "jmx.remote.object.wrapping " +
        "jmx.remote.rmi.client.socket.factory " +
        "jmx.remote.rmi.server.socket.factory " +
        "jmx.remote.sasl.callback.handler " +
        "jmx.remote.tls.socket.factory " +
        "jmx.remote.x.access.file " +
        "jmx.remote.x.password.file ";

    private static final SortedSet<String> defaultHiddenStrings =
            new TreeSet<String>();
    private static final SortedSet<String> defaultHiddenPrefixes =
            new TreeSet<String>();

    private static void hideAttributes(SortedMap<String, ?> map) {
        if (map.isEmpty())
            return;

        final SortedSet<String> hiddenStrings;
        final SortedSet<String> hiddenPrefixes;

        String hide = (String) map.get(HIDDEN_ATTRIBUTES);
        if (hide != null) {
            if (hide.startsWith("="))
                hide = hide.substring(1);
            else
                hide += " " + DEFAULT_HIDDEN_ATTRIBUTES;
            hiddenStrings = new TreeSet<String>();
            hiddenPrefixes = new TreeSet<String>();
            parseHiddenAttributes(hide, hiddenStrings, hiddenPrefixes);
        } else {
            hide = DEFAULT_HIDDEN_ATTRIBUTES;
            synchronized (defaultHiddenStrings) {
                if (defaultHiddenStrings.isEmpty()) {
                    parseHiddenAttributes(hide,
                                          defaultHiddenStrings,
                                          defaultHiddenPrefixes);
                }
                hiddenStrings = defaultHiddenStrings;
                hiddenPrefixes = defaultHiddenPrefixes;
            }
        }

        /* Construct a string that is greater than any key in the map.
           Setting a string-to-match or a prefix-to-match to this string
           guarantees that we will never call next() on the corresponding
           iterator.  */
        String sentinelKey = map.lastKey() + "X";
        Iterator<String> keyIterator = map.keySet().iterator();
        Iterator<String> stringIterator = hiddenStrings.iterator();
        Iterator<String> prefixIterator = hiddenPrefixes.iterator();

        String nextString;
        if (stringIterator.hasNext())
            nextString = stringIterator.next();
        else
            nextString = sentinelKey;
        String nextPrefix;
        if (prefixIterator.hasNext())
            nextPrefix = prefixIterator.next();
        else
            nextPrefix = sentinelKey;

        /* Read each key in sorted order and, if it matches a string
           or prefix, remove it. */
    keys:
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();

            /* Continue through string-match values until we find one
               that is either greater than the current key, or equal
               to it.  In the latter case, remove the key.  */
            int cmp = +1;
            while ((cmp = nextString.compareTo(key)) < 0) {
                if (stringIterator.hasNext())
                    nextString = stringIterator.next();
                else
                    nextString = sentinelKey;
            }
            if (cmp == 0) {
                keyIterator.remove();
                continue keys;
            }

            /* Continue through the prefix values until we find one
               that is either greater than the current key, or a
               prefix of it.  In the latter case, remove the key.  */
            while (nextPrefix.compareTo(key) <= 0) {
                if (key.startsWith(nextPrefix)) {
                    keyIterator.remove();
                    continue keys;
                }
                if (prefixIterator.hasNext())
                    nextPrefix = prefixIterator.next();
                else
                    nextPrefix = sentinelKey;
            }
        }
    }

    private static void parseHiddenAttributes(String hide,
                                              SortedSet<String> hiddenStrings,
                                              SortedSet<String> hiddenPrefixes) {
        final StringTokenizer tok = new StringTokenizer(hide);
        while (tok.hasMoreTokens()) {
            String s = tok.nextToken();
            if (s.endsWith("*"))
                hiddenPrefixes.add(s.substring(0, s.length() - 1));
            else
                hiddenStrings.add(s);
        }
    }

    /**
     * Name of the attribute that specifies the timeout to keep a
     * server side connection after answering last client request.
     * The default value is 120000 milliseconds.
     */
    public static final String SERVER_CONNECTION_TIMEOUT =
        "jmx.remote.x.server.connection.timeout";

    /**
     * Returns the server side connection timeout.
     */
    public static long getServerConnectionTimeout(Map<String, ?> env) {
        return getIntegerAttribute(env, SERVER_CONNECTION_TIMEOUT, 120000L,
                                   0, Long.MAX_VALUE);
    }

    /**
     * Name of the attribute that specifies the period in
     * millisecond for a client to check its connection. The default
     * value is 60000 milliseconds.
     */
    public static final String CLIENT_CONNECTION_CHECK_PERIOD =
        "jmx.remote.x.client.connection.check.period";

    /**
     * Returns the client connection check period.
     */
    public static long getConnectionCheckPeriod(Map<String, ?> env) {
        return getIntegerAttribute(env, CLIENT_CONNECTION_CHECK_PERIOD, 60000L,
                                   0, Long.MAX_VALUE);
    }

    /**
     * Computes a boolean value from a string value retrieved from a
     * property in the given map.
     *
     * @param stringBoolean the string value that must be converted
     * into a boolean value.
     *
     * @return
     *   <ul>
     *   <li>{@code false} if {@code stringBoolean} is {@code null}</li>
     *   <li>{@code false} if
     *       {@code stringBoolean.equalsIgnoreCase("false")}
     *       is {@code true}</li>
     *   <li>{@code true} if
     *       {@code stringBoolean.equalsIgnoreCase("true")}
     *       is {@code true}</li>
     *   </ul>
     *
     * @throws IllegalArgumentException if
     * {@code ((String)env.get(prop)).equalsIgnoreCase("false")} and
     * {@code ((String)env.get(prop)).equalsIgnoreCase("true")} are
     * {@code false}.
     */
    public static boolean computeBooleanFromString(String stringBoolean) {
        // returns a default value of 'false' if no property is found...
        return computeBooleanFromString(stringBoolean,false);
    }

    /**
     * Computes a boolean value from a string value retrieved from a
     * property in the given map.
     *
     * @param stringBoolean the string value that must be converted
     * into a boolean value.
     * @param defaultValue a default value to return in case no property
     *        was defined.
     *
     * @return
     *   <ul>
     *   <li>{@code defaultValue} if {@code stringBoolean}
     *   is {@code null}</li>
     *   <li>{@code false} if
     *       {@code stringBoolean.equalsIgnoreCase("false")}
     *       is {@code true}</li>
     *   <li>{@code true} if
     *       {@code stringBoolean.equalsIgnoreCase("true")}
     *       is {@code true}</li>
     *   </ul>
     *
     * @throws IllegalArgumentException if
     * {@code ((String)env.get(prop)).equalsIgnoreCase("false")} and
     * {@code ((String)env.get(prop)).equalsIgnoreCase("true")} are
     * {@code false}.
     */
    public static boolean computeBooleanFromString( String stringBoolean, boolean defaultValue) {
        if (stringBoolean == null)
            return defaultValue;
        else if (stringBoolean.equalsIgnoreCase("true"))
            return true;
        else if (stringBoolean.equalsIgnoreCase("false"))
            return false;
        else
            throw new IllegalArgumentException(
                "Property value must be \"true\" or \"false\" instead of \"" +
                stringBoolean + "\"");
    }

    /**
     * Converts a map into a valid hash table, i.e.
     * it removes all the 'null' values from the map.
     */
    public static <K, V> Hashtable<K, V> mapToHashtable(Map<K, V> map) {
        HashMap<K, V> m = new HashMap<K, V>(map);
        if (m.containsKey(null)) m.remove(null);
        for (Iterator<?> i = m.values().iterator(); i.hasNext(); )
            if (i.next() == null) i.remove();
        return new Hashtable<K, V>(m);
    }

    /**
     * Name of the attribute that specifies whether a connector server
     * should not prevent the VM from exiting
     */
    public static final String JMX_SERVER_DAEMON = "jmx.remote.x.daemon";

    /**
     * Returns true if {@value JMX_SERVER_DAEMON} is specified in the {@code env}
     * as a key and its value is a String and it is equal to true ignoring case.
     *
     * @param env
     * @return
     */
    public static boolean isServerDaemon(Map<String, ?> env) {
        return (env != null) &&
                ("true".equalsIgnoreCase((String)env.get(JMX_SERVER_DAEMON)));
    }

    private static final class SinkOutputStream extends OutputStream {
        public void write(byte[] b, int off, int len) {}
        public void write(int b) {}
    }

    private static final ClassLogger logger =
        new ClassLogger("javax.management.remote.misc", "EnvHelp");
}
