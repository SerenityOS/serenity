/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.util.prefs;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.ServiceConfigurationError;

// These imports needed only as a workaround for a JavaDoc bug
import java.lang.RuntimePermission;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Float;
import java.lang.Double;

/**
 * A node in a hierarchical collection of preference data.  This class
 * allows applications to store and retrieve user and system
 * preference and configuration data.  This data is stored
 * persistently in an implementation-dependent backing store.  Typical
 * implementations include flat files, OS-specific registries,
 * directory servers and SQL databases.  The user of this class needn't
 * be concerned with details of the backing store.
 *
 * <p>There are two separate trees of preference nodes, one for user
 * preferences and one for system preferences.  Each user has a separate user
 * preference tree, and all users in a given system share the same system
 * preference tree.  The precise description of "user" and "system" will vary
 * from implementation to implementation.  Typical information stored in the
 * user preference tree might include font choice, color choice, or preferred
 * window location and size for a particular application.  Typical information
 * stored in the system preference tree might include installation
 * configuration data for an application.
 *
 * <p>Nodes in a preference tree are named in a similar fashion to
 * directories in a hierarchical file system.   Every node in a preference
 * tree has a <i>node name</i> (which is not necessarily unique),
 * a unique <i>absolute path name</i>, and a path name <i>relative</i> to each
 * ancestor including itself.
 *
 * <p>The root node has a node name of the empty string ("").  Every other
 * node has an arbitrary node name, specified at the time it is created.  The
 * only restrictions on this name are that it cannot be the empty string, and
 * it cannot contain the slash character ('/').
 *
 * <p>The root node has an absolute path name of {@code "/"}.  Children of
 * the root node have absolute path names of {@code "/" + }<i>&lt;node
 * name&gt;</i>.  All other nodes have absolute path names of <i>&lt;parent's
 * absolute path name&gt;</i>{@code  + "/" + }<i>&lt;node name&gt;</i>.
 * Note that all absolute path names begin with the slash character.
 *
 * <p>A node <i>n</i>'s path name relative to its ancestor <i>a</i>
 * is simply the string that must be appended to <i>a</i>'s absolute path name
 * in order to form <i>n</i>'s absolute path name, with the initial slash
 * character (if present) removed.  Note that:
 * <ul>
 * <li>No relative path names begin with the slash character.
 * <li>Every node's path name relative to itself is the empty string.
 * <li>Every node's path name relative to its parent is its node name (except
 * for the root node, which does not have a parent).
 * <li>Every node's path name relative to the root is its absolute path name
 * with the initial slash character removed.
 * </ul>
 *
 * <p>Note finally that:
 * <ul>
 * <li>No path name contains multiple consecutive slash characters.
 * <li>No path name with the exception of the root's absolute path name
 * ends in the slash character.
 * <li>Any string that conforms to these two rules is a valid path name.
 * </ul>
 *
 * <p>All of the methods that modify preferences data are permitted to operate
 * asynchronously; they may return immediately, and changes will eventually
 * propagate to the persistent backing store with an implementation-dependent
 * delay.  The {@code flush} method may be used to synchronously force
 * updates to the backing store.  Normal termination of the Java Virtual
 * Machine will <i>not</i> result in the loss of pending updates -- an explicit
 * {@code flush} invocation is <i>not</i> required upon termination to ensure
 * that pending updates are made persistent.
 *
 * <p>All of the methods that read preferences from a {@code Preferences}
 * object require the invoker to provide a default value.  The default value is
 * returned if no value has been previously set <i>or if the backing store is
 * unavailable</i>.  The intent is to allow applications to operate, albeit
 * with slightly degraded functionality, even if the backing store becomes
 * unavailable.  Several methods, like {@code flush}, have semantics that
 * prevent them from operating if the backing store is unavailable.  Ordinary
 * applications should have no need to invoke any of these methods, which can
 * be identified by the fact that they are declared to throw {@link
 * BackingStoreException}.
 *
 * <p>The methods in this class may be invoked concurrently by multiple threads
 * in a single JVM without the need for external synchronization, and the
 * results will be equivalent to some serial execution.  If this class is used
 * concurrently <i>by multiple JVMs</i> that store their preference data in
 * the same backing store, the data store will not be corrupted, but no
 * other guarantees are made concerning the consistency of the preference
 * data.
 *
 * <p>This class contains an export/import facility, allowing preferences
 * to be "exported" to an XML document, and XML documents representing
 * preferences to be "imported" back into the system.  This facility
 * may be used to back up all or part of a preference tree, and
 * subsequently restore from the backup.
 *
 * <p>The XML document has the following DOCTYPE declaration:
 * <pre>{@code
 * <!DOCTYPE preferences SYSTEM "http://java.sun.com/dtd/preferences.dtd">
 * }</pre>
 * Note that the system URI (http://java.sun.com/dtd/preferences.dtd) is
 * <i>not</i> accessed when exporting or importing preferences; it merely
 * serves as a string to uniquely identify the DTD, which is:
 * <pre>{@code
 *    <?xml version="1.0" encoding="UTF-8"?>
 *
 *    <!-- DTD for a Preferences tree. -->
 *
 *    <!-- The preferences element is at the root of an XML document
 *         representing a Preferences tree. -->
 *    <!ELEMENT preferences (root)>
 *
 *    <!-- The preferences element contains an optional version attribute,
 *          which specifies version of DTD. -->
 *    <!ATTLIST preferences EXTERNAL_XML_VERSION CDATA "0.0" >
 *
 *    <!-- The root element has a map representing the root's preferences
 *         (if any), and one node for each child of the root (if any). -->
 *    <!ELEMENT root (map, node*) >
 *
 *    <!-- Additionally, the root contains a type attribute, which
 *         specifies whether it's the system or user root. -->
 *    <!ATTLIST root
 *              type (system|user) #REQUIRED >
 *
 *    <!-- Each node has a map representing its preferences (if any),
 *         and one node for each child (if any). -->
 *    <!ELEMENT node (map, node*) >
 *
 *    <!-- Additionally, each node has a name attribute -->
 *    <!ATTLIST node
 *              name CDATA #REQUIRED >
 *
 *    <!-- A map represents the preferences stored at a node (if any). -->
 *    <!ELEMENT map (entry*) >
 *
 *    <!-- An entry represents a single preference, which is simply
 *          a key-value pair. -->
 *    <!ELEMENT entry EMPTY >
 *    <!ATTLIST entry
 *              key   CDATA #REQUIRED
 *              value CDATA #REQUIRED >
 * }</pre>
 *
 * Every {@code Preferences} implementation must have an associated {@link
 * PreferencesFactory} implementation.  Every Java(TM) SE implementation must provide
 * some means of specifying which {@code PreferencesFactory} implementation
 * is used to generate the root preferences nodes.  This allows the
 * administrator to replace the default preferences implementation with an
 * alternative implementation.
 *
 * @implNote
 * The {@code PreferencesFactory} implementation is located as follows:
 *
 * <ol>
 *
 * <li><p>If the system property
 * {@systemProperty java.util.prefs.PreferencesFactory} is defined, then it is
 * taken to be the fully-qualified name of a class implementing the
 * {@code PreferencesFactory} interface.  The class is loaded and
 * instantiated; if this process fails then an unspecified error is
 * thrown.</p></li>
 *
 * <li><p> If a {@code PreferencesFactory} implementation class file
 * has been installed in a jar file that is visible to the
 * {@link java.lang.ClassLoader#getSystemClassLoader system class loader},
 * and that jar file contains a provider-configuration file named
 * {@code java.util.prefs.PreferencesFactory} in the resource
 * directory {@code META-INF/services}, then the first class name
 * specified in that file is taken.  If more than one such jar file is
 * provided, the first one found will be used.  The class is loaded
 * and instantiated; if this process fails then an unspecified error
 * is thrown.  </p></li>
 *
 * <li><p>Finally, if neither the above-mentioned system property nor
 * an extension jar file is provided, then the system-wide default
 * {@code PreferencesFactory} implementation for the underlying
 * platform is loaded and instantiated.</p></li>
 *
 * </ol>
 *
 * @author  Josh Bloch
 * @since   1.4
 */
public abstract class Preferences {

    private static final PreferencesFactory factory = factory();

    @SuppressWarnings("removal")
    private static PreferencesFactory factory() {
        // 1. Try user-specified system property
        String factoryName = AccessController.doPrivileged(
            new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty(
                        "java.util.prefs.PreferencesFactory");}});
        if (factoryName != null) {
            // FIXME: This code should be run in a doPrivileged and
            // not use the context classloader, to avoid being
            // dependent on the invoking thread.
            // Checking AllPermission also seems wrong.
            try {
                @SuppressWarnings("deprecation")
                Object result =Class.forName(factoryName, false,
                                             ClassLoader.getSystemClassLoader())
                    .newInstance();
                return (PreferencesFactory)result;
            } catch (Exception ex) {
                try {
                    // workaround for javaws, plugin,
                    // load factory class using non-system classloader
                    SecurityManager sm = System.getSecurityManager();
                    if (sm != null) {
                        sm.checkPermission(new java.security.AllPermission());
                    }
                    @SuppressWarnings("deprecation")
                    Object result = Class.forName(factoryName, false,
                                                  Thread.currentThread()
                                                  .getContextClassLoader())
                        .newInstance();
                    return (PreferencesFactory) result;
                } catch (Exception e) {
                    throw new InternalError(
                        "Can't instantiate Preferences factory "
                        + factoryName, e);
                }
            }
        }

        return AccessController.doPrivileged(
            new PrivilegedAction<PreferencesFactory>() {
                public PreferencesFactory run() {
                    return factory1();}});
    }

    private static PreferencesFactory factory1() {
        // 2. Try service provider interface
        Iterator<PreferencesFactory> itr = ServiceLoader
            .load(PreferencesFactory.class, ClassLoader.getSystemClassLoader())
            .iterator();

        // choose first provider instance
        while (itr.hasNext()) {
            try {
                return itr.next();
            } catch (ServiceConfigurationError sce) {
                if (sce.getCause() instanceof SecurityException) {
                    // Ignore the security exception, try the next provider
                    continue;
                }
                throw sce;
            }
        }

        // 3. Use platform-specific system-wide default
        String osName = System.getProperty("os.name");
        String platformFactory;
        if (osName.startsWith("Windows")) {
            platformFactory = "java.util.prefs.WindowsPreferencesFactory";
        } else if (osName.contains("OS X")) {
            platformFactory = "java.util.prefs.MacOSXPreferencesFactory";
        } else {
            platformFactory = "java.util.prefs.FileSystemPreferencesFactory";
        }
        try {
            @SuppressWarnings("deprecation")
            Object result = Class.forName(platformFactory, false,
                                          Preferences.class.getClassLoader()).newInstance();
            return (PreferencesFactory) result;
        } catch (Exception e) {
            throw new InternalError(
                "Can't instantiate platform default Preferences factory "
                + platformFactory, e);
        }
    }

    /**
     * Maximum length of string allowed as a key (80 characters).
     */
    public static final int MAX_KEY_LENGTH = 80;

    /**
     * Maximum length of string allowed as a value (8192 characters).
     */
    public static final int MAX_VALUE_LENGTH = 8*1024;

    /**
     * Maximum length of a node name (80 characters).
     */
    public static final int MAX_NAME_LENGTH = 80;

    /**
     * Returns the preference node from the calling user's preference tree
     * that is associated (by convention) with the specified class's package.
     * The convention is as follows: the absolute path name of the node is the
     * fully qualified package name, preceded by a slash ({@code '/'}), and
     * with each period ({@code '.'}) replaced by a slash.  For example the
     * absolute path name of the node associated with the class
     * {@code com.acme.widget.Foo} is {@code /com/acme/widget}.
     *
     * <p>This convention does not apply to the unnamed package, whose
     * associated preference node is {@code <unnamed>}.  This node
     * is not intended for long term use, but for convenience in the early
     * development of programs that do not yet belong to a package, and
     * for "throwaway" programs.  <i>Valuable data should not be stored
     * at this node as it is shared by all programs that use it.</i>
     *
     * <p>A class {@code Foo} wishing to access preferences pertaining to its
     * package can obtain a preference node as follows: <pre>
     *    static Preferences prefs = Preferences.userNodeForPackage(Foo.class);
     * </pre>
     * This idiom obviates the need for using a string to describe the
     * preferences node and decreases the likelihood of a run-time failure.
     * (If the class name is misspelled, it will typically result in a
     * compile-time error.)
     *
     * <p>Invoking this method will result in the creation of the returned
     * node and its ancestors if they do not already exist.  If the returned
     * node did not exist prior to this call, this node and any ancestors that
     * were created by this call are not guaranteed to become permanent until
     * the {@code flush} method is called on the returned node (or one of its
     * ancestors or descendants).
     *
     * @param c the class for whose package a user preference node is desired.
     * @return the user preference node associated with the package of which
     *         {@code c} is a member.
     * @throws NullPointerException if {@code c} is {@code null}.
     * @throws SecurityException if a security manager is present and
     *         it denies {@code RuntimePermission("preferences")}.
     * @see    RuntimePermission
     */
    public static Preferences userNodeForPackage(Class<?> c) {
        return userRoot().node(nodeName(c));
    }

    /**
     * Returns the preference node from the system preference tree that is
     * associated (by convention) with the specified class's package.  The
     * convention is as follows: the absolute path name of the node is the
     * fully qualified package name, preceded by a slash ({@code '/'}), and
     * with each period ({@code '.'}) replaced by a slash.  For example the
     * absolute path name of the node associated with the class
     * {@code com.acme.widget.Foo} is {@code /com/acme/widget}.
     *
     * <p>This convention does not apply to the unnamed package, whose
     * associated preference node is {@code <unnamed>}.  This node
     * is not intended for long term use, but for convenience in the early
     * development of programs that do not yet belong to a package, and
     * for "throwaway" programs.  <i>Valuable data should not be stored
     * at this node as it is shared by all programs that use it.</i>
     *
     * <p>A class {@code Foo} wishing to access preferences pertaining to its
     * package can obtain a preference node as follows: <pre>
     *  static Preferences prefs = Preferences.systemNodeForPackage(Foo.class);
     * </pre>
     * This idiom obviates the need for using a string to describe the
     * preferences node and decreases the likelihood of a run-time failure.
     * (If the class name is misspelled, it will typically result in a
     * compile-time error.)
     *
     * <p>Invoking this method will result in the creation of the returned
     * node and its ancestors if they do not already exist.  If the returned
     * node did not exist prior to this call, this node and any ancestors that
     * were created by this call are not guaranteed to become permanent until
     * the {@code flush} method is called on the returned node (or one of its
     * ancestors or descendants).
     *
     * @param c the class for whose package a system preference node is desired.
     * @return the system preference node associated with the package of which
     *         {@code c} is a member.
     * @throws NullPointerException if {@code c} is {@code null}.
     * @throws SecurityException if a security manager is present and
     *         it denies {@code RuntimePermission("preferences")}.
     * @see    RuntimePermission
     */
    public static Preferences systemNodeForPackage(Class<?> c) {
        return systemRoot().node(nodeName(c));
    }

    /**
     * Returns the absolute path name of the node corresponding to the package
     * of the specified object.
     *
     * @throws IllegalArgumentException if the package has node preferences
     *         node associated with it.
     */
    private static String nodeName(Class<?> c) {
        if (c.isArray())
            throw new IllegalArgumentException(
                "Arrays have no associated preferences node.");
        String className = c.getName();
        int pkgEndIndex = className.lastIndexOf('.');
        if (pkgEndIndex < 0)
            return "/<unnamed>";
        String packageName = className.substring(0, pkgEndIndex);
        return "/" + packageName.replace('.', '/');
    }

    /**
     * This permission object represents the permission required to get
     * access to the user or system root (which in turn allows for all
     * other operations).
     */
    private static Permission prefsPerm = new RuntimePermission("preferences");

    /**
     * Returns the root preference node for the calling user.
     *
     * @return the root preference node for the calling user.
     * @throws SecurityException If a security manager is present and
     *         it denies {@code RuntimePermission("preferences")}.
     * @see    RuntimePermission
     */
    public static Preferences userRoot() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            security.checkPermission(prefsPerm);

        return factory.userRoot();
    }

    /**
     * Returns the root preference node for the system.
     *
     * @return the root preference node for the system.
     * @throws SecurityException If a security manager is present and
     *         it denies {@code RuntimePermission("preferences")}.
     * @see    RuntimePermission
     */
    public static Preferences systemRoot() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            security.checkPermission(prefsPerm);

        return factory.systemRoot();
    }

    /**
     * Sole constructor. (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected Preferences() {
    }

    /**
     * Associates the specified value with the specified key in this
     * preference node.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @throws NullPointerException if key or value is {@code null}.
     * @throws IllegalArgumentException if {@code key.length()} exceeds
     *       {@code MAX_KEY_LENGTH} or if {@code value.length} exceeds
     *       {@code MAX_VALUE_LENGTH}.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws IllegalArgumentException if either key or value contain
     *         the null control character, code point U+0000.
     */
    public abstract void put(String key, String value);

    /**
     * Returns the value associated with the specified key in this preference
     * node.  Returns the specified default if there is no value associated
     * with the key, or the backing store is inaccessible.
     *
     * <p>Some implementations may store default values in their backing
     * stores.  If there is no value associated with the specified key
     * but there is such a <i>stored default</i>, it is returned in
     * preference to the specified default.
     *
     * @param key key whose associated value is to be returned.
     * @param def the value to be returned in the event that this
     *        preference node has no value associated with {@code key}.
     * @return the value associated with {@code key}, or {@code def}
     *         if no value is associated with {@code key}, or the backing
     *         store is inaccessible.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws NullPointerException if {@code key} is {@code null}.  (A
     *         {@code null} value for {@code def} <i>is</i> permitted.)
     * @throws IllegalArgumentException if key contains the null control
     *         character, code point U+0000.
     */
    public abstract String get(String key, String def);

    /**
     * Removes the value associated with the specified key in this preference
     * node, if any.
     *
     * <p>If this implementation supports <i>stored defaults</i>, and there is
     * such a default for the specified preference, the stored default will be
     * "exposed" by this call, in the sense that it will be returned
     * by a succeeding call to {@code get}.
     *
     * @param key key whose mapping is to be removed from the preference node.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws IllegalArgumentException if key contains the null control
     *         character, code point U+0000.
     */
    public abstract void remove(String key);

    /**
     * Removes all of the preferences (key-value associations) in this
     * preference node.  This call has no effect on any descendants
     * of this node.
     *
     * <p>If this implementation supports <i>stored defaults</i>, and this
     * node in the preferences hierarchy contains any such defaults,
     * the stored defaults will be "exposed" by this call, in the sense that
     * they will be returned by succeeding calls to {@code get}.
     *
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @see #removeNode()
     */
    public abstract void clear() throws BackingStoreException;

    /**
     * Associates a string representing the specified int value with the
     * specified key in this preference node.  The associated string is the
     * one that would be returned if the int value were passed to
     * {@link Integer#toString(int)}.  This method is intended for use in
     * conjunction with {@link #getInt}.
     *
     * @param key key with which the string form of value is to be associated.
     * @param value value whose string form is to be associated with key.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws IllegalArgumentException if {@code key.length()} exceeds
     *         {@code MAX_KEY_LENGTH}.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws IllegalArgumentException if key contains
     *         the null control character, code point U+0000.
     * @see #getInt(String,int)
     */
    public abstract void putInt(String key, int value);

    /**
     * Returns the int value represented by the string associated with the
     * specified key in this preference node.  The string is converted to
     * an integer as by {@link Integer#parseInt(String)}.  Returns the
     * specified default if there is no value associated with the key,
     * the backing store is inaccessible, or if
     * {@code Integer.parseInt(String)} would throw a {@link
     * NumberFormatException} if the associated value were passed.  This
     * method is intended for use in conjunction with {@link #putInt}.
     *
     * <p>If the implementation supports <i>stored defaults</i> and such a
     * default exists, is accessible, and could be converted to an int
     * with {@code Integer.parseInt}, this int is returned in preference to
     * the specified default.
     *
     * @param key key whose associated value is to be returned as an int.
     * @param def the value to be returned in the event that this
     *        preference node has no value associated with {@code key}
     *        or the associated value cannot be interpreted as an int,
     *        or the backing store is inaccessible.
     * @return the int value represented by the string associated with
     *         {@code key} in this preference node, or {@code def} if the
     *         associated value does not exist or cannot be interpreted as
     *         an int.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws IllegalArgumentException if key contains the null control
     *         character, code point U+0000.
     * @see #putInt(String,int)
     * @see #get(String,String)
     */
    public abstract int getInt(String key, int def);

    /**
     * Associates a string representing the specified long value with the
     * specified key in this preference node.  The associated string is the
     * one that would be returned if the long value were passed to
     * {@link Long#toString(long)}.  This method is intended for use in
     * conjunction with {@link #getLong}.
     *
     * @param key key with which the string form of value is to be associated.
     * @param value value whose string form is to be associated with key.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws IllegalArgumentException if {@code key.length()} exceeds
     *         {@code MAX_KEY_LENGTH}.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws IllegalArgumentException if key contains
     *         the null control character, code point U+0000.
     * @see #getLong(String,long)
     */
    public abstract void putLong(String key, long value);

    /**
     * Returns the long value represented by the string associated with the
     * specified key in this preference node.  The string is converted to
     * a long as by {@link Long#parseLong(String)}.  Returns the
     * specified default if there is no value associated with the key,
     * the backing store is inaccessible, or if
     * {@code Long.parseLong(String)} would throw a {@link
     * NumberFormatException} if the associated value were passed.  This
     * method is intended for use in conjunction with {@link #putLong}.
     *
     * <p>If the implementation supports <i>stored defaults</i> and such a
     * default exists, is accessible, and could be converted to a long
     * with {@code Long.parseLong}, this long is returned in preference to
     * the specified default.
     *
     * @param key key whose associated value is to be returned as a long.
     * @param def the value to be returned in the event that this
     *        preference node has no value associated with {@code key}
     *        or the associated value cannot be interpreted as a long,
     *        or the backing store is inaccessible.
     * @return the long value represented by the string associated with
     *         {@code key} in this preference node, or {@code def} if the
     *         associated value does not exist or cannot be interpreted as
     *         a long.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws IllegalArgumentException if key contains the null control
     *         character, code point U+0000.
     * @see #putLong(String,long)
     * @see #get(String,String)
     */
    public abstract long getLong(String key, long def);

    /**
     * Associates a string representing the specified boolean value with the
     * specified key in this preference node.  The associated string is
     * {@code "true"} if the value is true, and {@code "false"} if it is
     * false.  This method is intended for use in conjunction with
     * {@link #getBoolean}.
     *
     * @param key key with which the string form of value is to be associated.
     * @param value value whose string form is to be associated with key.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws IllegalArgumentException if {@code key.length()} exceeds
     *         {@code MAX_KEY_LENGTH}.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws IllegalArgumentException if key contains
     *         the null control character, code point U+0000.
     * @see #getBoolean(String,boolean)
     * @see #get(String,String)
     */
    public abstract void putBoolean(String key, boolean value);

    /**
     * Returns the boolean value represented by the string associated with the
     * specified key in this preference node.  Valid strings
     * are {@code "true"}, which represents true, and {@code "false"}, which
     * represents false.  Case is ignored, so, for example, {@code "TRUE"}
     * and {@code "False"} are also valid.  This method is intended for use in
     * conjunction with {@link #putBoolean}.
     *
     * <p>Returns the specified default if there is no value
     * associated with the key, the backing store is inaccessible, or if the
     * associated value is something other than {@code "true"} or
     * {@code "false"}, ignoring case.
     *
     * <p>If the implementation supports <i>stored defaults</i> and such a
     * default exists and is accessible, it is used in preference to the
     * specified default, unless the stored default is something other than
     * {@code "true"} or {@code "false"}, ignoring case, in which case the
     * specified default is used.
     *
     * @param key key whose associated value is to be returned as a boolean.
     * @param def the value to be returned in the event that this
     *        preference node has no value associated with {@code key}
     *        or the associated value cannot be interpreted as a boolean,
     *        or the backing store is inaccessible.
     * @return the boolean value represented by the string associated with
     *         {@code key} in this preference node, or {@code def} if the
     *         associated value does not exist or cannot be interpreted as
     *         a boolean.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws IllegalArgumentException if key contains the null control
     *         character, code point U+0000.
     * @see #get(String,String)
     * @see #putBoolean(String,boolean)
     */
    public abstract boolean getBoolean(String key, boolean def);

    /**
     * Associates a string representing the specified float value with the
     * specified key in this preference node.  The associated string is the
     * one that would be returned if the float value were passed to
     * {@link Float#toString(float)}.  This method is intended for use in
     * conjunction with {@link #getFloat}.
     *
     * @param key key with which the string form of value is to be associated.
     * @param value value whose string form is to be associated with key.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws IllegalArgumentException if {@code key.length()} exceeds
     *         {@code MAX_KEY_LENGTH}.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws IllegalArgumentException if key contains
     *         the null control character, code point U+0000.
     * @see #getFloat(String,float)
     */
    public abstract void putFloat(String key, float value);

    /**
     * Returns the float value represented by the string associated with the
     * specified key in this preference node.  The string is converted to an
     * integer as by {@link Float#parseFloat(String)}.  Returns the specified
     * default if there is no value associated with the key, the backing store
     * is inaccessible, or if {@code Float.parseFloat(String)} would throw a
     * {@link NumberFormatException} if the associated value were passed.
     * This method is intended for use in conjunction with {@link #putFloat}.
     *
     * <p>If the implementation supports <i>stored defaults</i> and such a
     * default exists, is accessible, and could be converted to a float
     * with {@code Float.parseFloat}, this float is returned in preference to
     * the specified default.
     *
     * @param key key whose associated value is to be returned as a float.
     * @param def the value to be returned in the event that this
     *        preference node has no value associated with {@code key}
     *        or the associated value cannot be interpreted as a float,
     *        or the backing store is inaccessible.
     * @return the float value represented by the string associated with
     *         {@code key} in this preference node, or {@code def} if the
     *         associated value does not exist or cannot be interpreted as
     *         a float.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws IllegalArgumentException if key contains the null control
     *         character, code point U+0000.
     * @see #putFloat(String,float)
     * @see #get(String,String)
     */
    public abstract float getFloat(String key, float def);

    /**
     * Associates a string representing the specified double value with the
     * specified key in this preference node.  The associated string is the
     * one that would be returned if the double value were passed to
     * {@link Double#toString(double)}.  This method is intended for use in
     * conjunction with {@link #getDouble}.
     *
     * @param key key with which the string form of value is to be associated.
     * @param value value whose string form is to be associated with key.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws IllegalArgumentException if {@code key.length()} exceeds
     *         {@code MAX_KEY_LENGTH}.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws IllegalArgumentException if key contains
     *         the null control character, code point U+0000.
     * @see #getDouble(String,double)
     */
    public abstract void putDouble(String key, double value);

    /**
     * Returns the double value represented by the string associated with the
     * specified key in this preference node.  The string is converted to an
     * integer as by {@link Double#parseDouble(String)}.  Returns the specified
     * default if there is no value associated with the key, the backing store
     * is inaccessible, or if {@code Double.parseDouble(String)} would throw a
     * {@link NumberFormatException} if the associated value were passed.
     * This method is intended for use in conjunction with {@link #putDouble}.
     *
     * <p>If the implementation supports <i>stored defaults</i> and such a
     * default exists, is accessible, and could be converted to a double
     * with {@code Double.parseDouble}, this double is returned in preference
     * to the specified default.
     *
     * @param key key whose associated value is to be returned as a double.
     * @param def the value to be returned in the event that this
     *        preference node has no value associated with {@code key}
     *        or the associated value cannot be interpreted as a double,
     *        or the backing store is inaccessible.
     * @return the double value represented by the string associated with
     *         {@code key} in this preference node, or {@code def} if the
     *         associated value does not exist or cannot be interpreted as
     *         a double.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws IllegalArgumentException if key contains the null control
     *         character, code point U+0000.
     * @see #putDouble(String,double)
     * @see #get(String,String)
     */
    public abstract double getDouble(String key, double def);

    /**
     * Associates a string representing the specified byte array with the
     * specified key in this preference node.  The associated string is
     * the <i>Base64</i> encoding of the byte array, as defined in <a
     * href=http://www.ietf.org/rfc/rfc2045.txt>RFC 2045</a>, Section 6.8,
     * with one minor change: the string will consist solely of characters
     * from the <i>Base64 Alphabet</i>; it will not contain any newline
     * characters.  Note that the maximum length of the byte array is limited
     * to three quarters of {@code MAX_VALUE_LENGTH} so that the length
     * of the Base64 encoded String does not exceed {@code MAX_VALUE_LENGTH}.
     * This method is intended for use in conjunction with
     * {@link #getByteArray}.
     *
     * @param key key with which the string form of value is to be associated.
     * @param value value whose string form is to be associated with key.
     * @throws NullPointerException if key or value is {@code null}.
     * @throws IllegalArgumentException if key.length() exceeds MAX_KEY_LENGTH
     *         or if value.length exceeds MAX_VALUE_LENGTH*3/4.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws IllegalArgumentException if key contains
     *         the null control character, code point U+0000.
     * @see #getByteArray(String,byte[])
     * @see #get(String,String)
     */
    public abstract void putByteArray(String key, byte[] value);

    /**
     * Returns the byte array value represented by the string associated with
     * the specified key in this preference node.  Valid strings are
     * <i>Base64</i> encoded binary data, as defined in <a
     * href=http://www.ietf.org/rfc/rfc2045.txt>RFC 2045</a>, Section 6.8,
     * with one minor change: the string must consist solely of characters
     * from the <i>Base64 Alphabet</i>; no newline characters or
     * extraneous characters are permitted.  This method is intended for use
     * in conjunction with {@link #putByteArray}.
     *
     * <p>Returns the specified default if there is no value
     * associated with the key, the backing store is inaccessible, or if the
     * associated value is not a valid Base64 encoded byte array
     * (as defined above).
     *
     * <p>If the implementation supports <i>stored defaults</i> and such a
     * default exists and is accessible, it is used in preference to the
     * specified default, unless the stored default is not a valid Base64
     * encoded byte array (as defined above), in which case the
     * specified default is used.
     *
     * @param key key whose associated value is to be returned as a byte array.
     * @param def the value to be returned in the event that this
     *        preference node has no value associated with {@code key}
     *        or the associated value cannot be interpreted as a byte array,
     *        or the backing store is inaccessible.
     * @return the byte array value represented by the string associated with
     *         {@code key} in this preference node, or {@code def} if the
     *         associated value does not exist or cannot be interpreted as
     *         a byte array.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws NullPointerException if {@code key} is {@code null}.  (A
     *         {@code null} value for {@code def} <i>is</i> permitted.)
     * @throws IllegalArgumentException if key contains the null control
     *         character, code point U+0000.
     * @see #get(String,String)
     * @see #putByteArray(String,byte[])
     */
    public abstract byte[] getByteArray(String key, byte[] def);

    /**
     * Returns all of the keys that have an associated value in this
     * preference node.  (The returned array will be of size zero if
     * this node has no preferences.)
     *
     * <p>If the implementation supports <i>stored defaults</i> and there
     * are any such defaults at this node that have not been overridden,
     * by explicit preferences, the defaults are returned in the array in
     * addition to any explicit preferences.
     *
     * @return an array of the keys that have an associated value in this
     *         preference node.
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public abstract String[] keys() throws BackingStoreException;

    /**
     * Returns the names of the children of this preference node, relative to
     * this node.  (The returned array will be of size zero if this node has
     * no children.)
     *
     * @return the names of the children of this preference node.
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public abstract String[] childrenNames() throws BackingStoreException;

    /**
     * Returns the parent of this preference node, or {@code null} if this is
     * the root.
     *
     * @return the parent of this preference node.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public abstract Preferences parent();

    /**
     * Returns the named preference node in the same tree as this node,
     * creating it and any of its ancestors if they do not already exist.
     * Accepts a relative or absolute path name.  Relative path names
     * (which do not begin with the slash character {@code ('/')}) are
     * interpreted relative to this preference node.
     *
     * <p>If the returned node did not exist prior to this call, this node and
     * any ancestors that were created by this call are not guaranteed
     * to become permanent until the {@code flush} method is called on
     * the returned node (or one of its ancestors or descendants).
     *
     * @param pathName the path name of the preference node to return.
     * @return the specified preference node.
     * @throws IllegalArgumentException if the path name is invalid (i.e.,
     *         it contains multiple consecutive slash characters, or ends
     *         with a slash character and is more than one character long).
     * @throws NullPointerException if path name is {@code null}.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @see #flush()
     */
    public abstract Preferences node(String pathName);

    /**
     * Returns true if the named preference node exists in the same tree
     * as this node.  Relative path names (which do not begin with the slash
     * character {@code ('/')}) are interpreted relative to this preference
     * node.
     *
     * <p>If this node (or an ancestor) has already been removed with the
     * {@link #removeNode()} method, it <i>is</i> legal to invoke this method,
     * but only with the path name {@code ""}; the invocation will return
     * {@code false}.  Thus, the idiom {@code p.nodeExists("")} may be
     * used to test whether {@code p} has been removed.
     *
     * @param pathName the path name of the node whose existence
     *        is to be checked.
     * @return true if the specified node exists.
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     * @throws IllegalArgumentException if the path name is invalid (i.e.,
     *         it contains multiple consecutive slash characters, or ends
     *         with a slash character and is more than one character long).
     * @throws NullPointerException if path name is {@code null}.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method and
     *         {@code pathName} is not the empty string ({@code ""}).
     */
    public abstract boolean nodeExists(String pathName)
        throws BackingStoreException;

    /**
     * Removes this preference node and all of its descendants, invalidating
     * any preferences contained in the removed nodes.  Once a node has been
     * removed, attempting any method other than {@link #name()},
     * {@link #absolutePath()}, {@link #isUserNode()}, {@link #flush()} or
     * {@link #node(String) nodeExists("")} on the corresponding
     * {@code Preferences} instance will fail with an
     * {@code IllegalStateException}.  (The methods defined on {@link Object}
     * can still be invoked on a node after it has been removed; they will not
     * throw {@code IllegalStateException}.)
     *
     * <p>The removal is not guaranteed to be persistent until the
     * {@code flush} method is called on this node (or an ancestor).
     *
     * <p>If this implementation supports <i>stored defaults</i>, removing a
     * node exposes any stored defaults at or below this node.  Thus, a
     * subsequent call to {@code nodeExists} on this node's path name may
     * return {@code true}, and a subsequent call to {@code node} on this
     * path name may return a (different) {@code Preferences} instance
     * representing a non-empty collection of preferences and/or children.
     *
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     * @throws IllegalStateException if this node (or an ancestor) has already
     *         been removed with the {@link #removeNode()} method.
     * @throws UnsupportedOperationException if this method is invoked on
     *         the root node.
     * @see #flush()
     */
    public abstract void removeNode() throws BackingStoreException;

    /**
     * Returns this preference node's name, relative to its parent.
     *
     * @return this preference node's name, relative to its parent.
     */
    public abstract String name();

    /**
     * Returns this preference node's absolute path name.
     *
     * @return this preference node's absolute path name.
     */
    public abstract String absolutePath();

    /**
     * Returns {@code true} if this preference node is in the user
     * preference tree, {@code false} if it's in the system preference tree.
     *
     * @return {@code true} if this preference node is in the user
     *         preference tree, {@code false} if it's in the system
     *         preference tree.
     */
    public abstract boolean isUserNode();

    /**
     * Returns a string representation of this preferences node,
     * as if computed by the expression:{@code (this.isUserNode() ? "User" :
     * "System") + " Preference Node: " + this.absolutePath()}.
     */
    public abstract String toString();

    /**
     * Forces any changes in the contents of this preference node and its
     * descendants to the persistent store.  Once this method returns
     * successfully, it is safe to assume that all changes made in the
     * subtree rooted at this node prior to the method invocation have become
     * permanent.
     *
     * <p>Implementations are free to flush changes into the persistent store
     * at any time.  They do not need to wait for this method to be called.
     *
     * <p>When a flush occurs on a newly created node, it is made persistent,
     * as are any ancestors (and descendants) that have yet to be made
     * persistent.  Note however that any preference value changes in
     * ancestors are <i>not</i> guaranteed to be made persistent.
     *
     * <p> If this method is invoked on a node that has been removed with
     * the {@link #removeNode()} method, flushSpi() is invoked on this node,
     * but not on others.
     *
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     * @see    #sync()
     */
    public abstract void flush() throws BackingStoreException;

    /**
     * Ensures that future reads from this preference node and its
     * descendants reflect any changes that were committed to the persistent
     * store (from any VM) prior to the {@code sync} invocation.  As a
     * side-effect, forces any changes in the contents of this preference node
     * and its descendants to the persistent store, as if the {@code flush}
     * method had been invoked on this node.
     *
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @see    #flush()
     */
    public abstract void sync() throws BackingStoreException;

    /**
     * Registers the specified listener to receive <i>preference change
     * events</i> for this preference node.  A preference change event is
     * generated when a preference is added to this node, removed from this
     * node, or when the value associated with a preference is changed.
     * (Preference change events are <i>not</i> generated by the {@link
     * #removeNode()} method, which generates a <i>node change event</i>.
     * Preference change events <i>are</i> generated by the {@code clear}
     * method.)
     *
     * <p>Events are only guaranteed for changes made within the same JVM
     * as the registered listener, though some implementations may generate
     * events for changes made outside this JVM.  Events may be generated
     * before the changes have been made persistent.  Events are not generated
     * when preferences are modified in descendants of this node; a caller
     * desiring such events must register with each descendant.
     *
     * @param pcl The preference change listener to add.
     * @throws NullPointerException if {@code pcl} is null.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @see #removePreferenceChangeListener(PreferenceChangeListener)
     * @see #addNodeChangeListener(NodeChangeListener)
     */
    public abstract void addPreferenceChangeListener(
        PreferenceChangeListener pcl);

    /**
     * Removes the specified preference change listener, so it no longer
     * receives preference change events.
     *
     * @param pcl The preference change listener to remove.
     * @throws IllegalArgumentException if {@code pcl} was not a registered
     *         preference change listener on this node.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @see #addPreferenceChangeListener(PreferenceChangeListener)
     */
    public abstract void removePreferenceChangeListener(
        PreferenceChangeListener pcl);

    /**
     * Registers the specified listener to receive <i>node change events</i>
     * for this node.  A node change event is generated when a child node is
     * added to or removed from this node.  (A single {@link #removeNode()}
     * invocation results in multiple <i>node change events</i>, one for every
     * node in the subtree rooted at the removed node.)
     *
     * <p>Events are only guaranteed for changes made within the same JVM
     * as the registered listener, though some implementations may generate
     * events for changes made outside this JVM.  Events may be generated
     * before the changes have become permanent.  Events are not generated
     * when indirect descendants of this node are added or removed; a
     * caller desiring such events must register with each descendant.
     *
     * <p>Few guarantees can be made regarding node creation.  Because nodes
     * are created implicitly upon access, it may not be feasible for an
     * implementation to determine whether a child node existed in the backing
     * store prior to access (for example, because the backing store is
     * unreachable or cached information is out of date).  Under these
     * circumstances, implementations are neither required to generate node
     * change events nor prohibited from doing so.
     *
     * @param ncl The {@code NodeChangeListener} to add.
     * @throws NullPointerException if {@code ncl} is null.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @see #removeNodeChangeListener(NodeChangeListener)
     * @see #addPreferenceChangeListener(PreferenceChangeListener)
     */
    public abstract void addNodeChangeListener(NodeChangeListener ncl);

    /**
     * Removes the specified {@code NodeChangeListener}, so it no longer
     * receives change events.
     *
     * @param ncl The {@code NodeChangeListener} to remove.
     * @throws IllegalArgumentException if {@code ncl} was not a registered
     *         {@code NodeChangeListener} on this node.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @see #addNodeChangeListener(NodeChangeListener)
     */
    public abstract void removeNodeChangeListener(NodeChangeListener ncl);

    /**
     * Emits on the specified output stream an XML document representing all
     * of the preferences contained in this node (but not its descendants).
     * This XML document is, in effect, an offline backup of the node.
     *
     * <p>The XML document will have the following DOCTYPE declaration:
     * <pre>{@code
     * <!DOCTYPE preferences SYSTEM "http://java.sun.com/dtd/preferences.dtd">
     * }</pre>
     * The UTF-8 character encoding will be used.
     *
     * <p>This method is an exception to the general rule that the results of
     * concurrently executing multiple methods in this class yields
     * results equivalent to some serial execution.  If the preferences
     * at this node are modified concurrently with an invocation of this
     * method, the exported preferences comprise a "fuzzy snapshot" of the
     * preferences contained in the node; some of the concurrent modifications
     * may be reflected in the exported data while others may not.
     *
     * @param os the output stream on which to emit the XML document.
     * @throws IOException if writing to the specified output stream
     *         results in an {@code IOException}.
     * @throws BackingStoreException if preference data cannot be read from
     *         backing store.
     * @see    #importPreferences(InputStream)
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public abstract void exportNode(OutputStream os)
        throws IOException, BackingStoreException;

    /**
     * Emits an XML document representing all of the preferences contained
     * in this node and all of its descendants.  This XML document is, in
     * effect, an offline backup of the subtree rooted at the node.
     *
     * <p>The XML document will have the following DOCTYPE declaration:
     * <pre>{@code
     * <!DOCTYPE preferences SYSTEM "http://java.sun.com/dtd/preferences.dtd">
     * }</pre>
     * The UTF-8 character encoding will be used.
     *
     * <p>This method is an exception to the general rule that the results of
     * concurrently executing multiple methods in this class yields
     * results equivalent to some serial execution.  If the preferences
     * or nodes in the subtree rooted at this node are modified concurrently
     * with an invocation of this method, the exported preferences comprise a
     * "fuzzy snapshot" of the subtree; some of the concurrent modifications
     * may be reflected in the exported data while others may not.
     *
     * @param os the output stream on which to emit the XML document.
     * @throws IOException if writing to the specified output stream
     *         results in an {@code IOException}.
     * @throws BackingStoreException if preference data cannot be read from
     *         backing store.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @see    #importPreferences(InputStream)
     * @see    #exportNode(OutputStream)
     */
    public abstract void exportSubtree(OutputStream os)
        throws IOException, BackingStoreException;

    /**
     * Imports all of the preferences represented by the XML document on the
     * specified input stream.  The document may represent user preferences or
     * system preferences.  If it represents user preferences, the preferences
     * will be imported into the calling user's preference tree (even if they
     * originally came from a different user's preference tree).  If any of
     * the preferences described by the document inhabit preference nodes that
     * do not exist, the nodes will be created.
     *
     * <p>The XML document must have the following DOCTYPE declaration:
     * <pre>{@code
     * <!DOCTYPE preferences SYSTEM "http://java.sun.com/dtd/preferences.dtd">
     * }</pre>
     * (This method is designed for use in conjunction with
     * {@link #exportNode(OutputStream)} and
     * {@link #exportSubtree(OutputStream)}.
     *
     * <p>This method is an exception to the general rule that the results of
     * concurrently executing multiple methods in this class yields
     * results equivalent to some serial execution.  The method behaves
     * as if implemented on top of the other public methods in this class,
     * notably {@link #node(String)} and {@link #put(String, String)}.
     *
     * @param is the input stream from which to read the XML document.
     * @throws IOException if reading from the specified input stream
     *         results in an {@code IOException}.
     * @throws InvalidPreferencesFormatException Data on input stream does not
     *         constitute a valid XML document with the mandated document type.
     * @throws SecurityException If a security manager is present and
     *         it denies {@code RuntimePermission("preferences")}.
     * @see    RuntimePermission
     */
    public static void importPreferences(InputStream is)
        throws IOException, InvalidPreferencesFormatException
    {
        XmlSupport.importPreferences(is);
    }
}
