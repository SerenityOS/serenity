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

import java.util.*;
import java.io.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
// These imports needed only as a workaround for a JavaDoc bug
import java.lang.Integer;
import java.lang.Long;
import java.lang.Float;
import java.lang.Double;

/**
 * This class provides a skeletal implementation of the {@link Preferences}
 * class, greatly easing the task of implementing it.
 *
 * <p><strong>This class is for {@code Preferences} implementers only.
 * Normal users of the {@code Preferences} facility should have no need to
 * consult this documentation.  The {@link Preferences} documentation
 * should suffice.</strong>
 *
 * <p>Implementors must override the nine abstract service-provider interface
 * (SPI) methods: {@link #getSpi(String)}, {@link #putSpi(String,String)},
 * {@link #removeSpi(String)}, {@link #childSpi(String)}, {@link
 * #removeNodeSpi()}, {@link #keysSpi()}, {@link #childrenNamesSpi()}, {@link
 * #syncSpi()} and {@link #flushSpi()}.  All of the concrete methods specify
 * precisely how they are implemented atop these SPI methods.  The implementor
 * may, at his discretion, override one or more of the concrete methods if the
 * default implementation is unsatisfactory for any reason, such as
 * performance.
 *
 * <p>The SPI methods fall into three groups concerning exception
 * behavior. The {@code getSpi} method should never throw exceptions, but it
 * doesn't really matter, as any exception thrown by this method will be
 * intercepted by {@link #get(String,String)}, which will return the specified
 * default value to the caller.  The {@code removeNodeSpi, keysSpi,
 * childrenNamesSpi, syncSpi} and {@code flushSpi} methods are specified
 * to throw {@link BackingStoreException}, and the implementation is required
 * to throw this checked exception if it is unable to perform the operation.
 * The exception propagates outward, causing the corresponding API method
 * to fail.
 *
 * <p>The remaining SPI methods {@link #putSpi(String,String)}, {@link
 * #removeSpi(String)} and {@link #childSpi(String)} have more complicated
 * exception behavior.  They are not specified to throw
 * {@code BackingStoreException}, as they can generally obey their contracts
 * even if the backing store is unavailable.  This is true because they return
 * no information and their effects are not required to become permanent until
 * a subsequent call to {@link Preferences#flush()} or
 * {@link Preferences#sync()}. Generally speaking, these SPI methods should not
 * throw exceptions.  In some implementations, there may be circumstances
 * under which these calls cannot even enqueue the requested operation for
 * later processing.  Even under these circumstances it is generally better to
 * simply ignore the invocation and return, rather than throwing an
 * exception.  Under these circumstances, however, subsequently invoking
 * {@code flush()} or {@code sync} would not imply that all previous
 * operations had successfully been made permanent.
 *
 * <p>There is one circumstance under which {@code putSpi, removeSpi and
 * childSpi} <i>should</i> throw an exception: if the caller lacks
 * sufficient privileges on the underlying operating system to perform the
 * requested operation.  This will, for instance, occur on most systems
 * if a non-privileged user attempts to modify system preferences.
 * (The required privileges will vary from implementation to
 * implementation.  On some implementations, they are the right to modify the
 * contents of some directory in the file system; on others they are the right
 * to modify contents of some key in a registry.)  Under any of these
 * circumstances, it would generally be undesirable to let the program
 * continue executing as if these operations would become permanent at a later
 * time.  While implementations are not required to throw an exception under
 * these circumstances, they are encouraged to do so.  A {@link
 * SecurityException} would be appropriate.
 *
 * <p>Most of the SPI methods require the implementation to read or write
 * information at a preferences node.  The implementor should beware of the
 * fact that another VM may have concurrently deleted this node from the
 * backing store.  It is the implementation's responsibility to recreate the
 * node if it has been deleted.
 *
 * <p>Implementation note: In Sun's default {@code Preferences}
 * implementations, the user's identity is inherited from the underlying
 * operating system and does not change for the lifetime of the virtual
 * machine.  It is recognized that server-side {@code Preferences}
 * implementations may have the user identity change from request to request,
 * implicitly passed to {@code Preferences} methods via the use of a
 * static {@link ThreadLocal} instance.  Authors of such implementations are
 * <i>strongly</i> encouraged to determine the user at the time preferences
 * are accessed (for example by the {@link #get(String,String)} or {@link
 * #put(String,String)} method) rather than permanently associating a user
 * with each {@code Preferences} instance.  The latter behavior conflicts
 * with normal {@code Preferences} usage and would lead to great confusion.
 *
 * @author  Josh Bloch
 * @see     Preferences
 * @since   1.4
 */
public abstract class AbstractPreferences extends Preferences {
    /**
     * The code point U+0000, assigned to the null control character, is the
     * only character encoded in Unicode and ISO/IEC 10646 that is always
     * invalid in any XML 1.0 and 1.1 document.
     */
    static final int CODE_POINT_U0000 = '\u0000';

    /**
     * Our name relative to parent.
     */
    private final String name;

    /**
     * Our absolute path name.
     */
    private final String absolutePath;

    /**
     * Our parent node.
     */
    final AbstractPreferences parent;

    /**
     * Our root node.
     */
    private final AbstractPreferences root; // Relative to this node

    /**
     * This field should be {@code true} if this node did not exist in the
     * backing store prior to the creation of this object.  The field
     * is initialized to false, but may be set to true by a subclass
     * constructor (and should not be modified thereafter).  This field
     * indicates whether a node change event should be fired when
     * creation is complete.
     */
    protected boolean newNode = false;

    /**
     * All known unremoved children of this node.  (This "cache" is consulted
     * prior to calling childSpi() or getChild().
     */
    private Map<String, AbstractPreferences> kidCache = new HashMap<>();

    /**
     * This field is used to keep track of whether or not this node has
     * been removed.  Once it's set to true, it will never be reset to false.
     */
    private boolean removed = false;

    /**
     * Registered preference change listeners.
     */
    private PreferenceChangeListener[] prefListeners =
        new PreferenceChangeListener[0];

    /**
     * Registered node change listeners.
     */
    private NodeChangeListener[] nodeListeners = new NodeChangeListener[0];

    /**
     * An object whose monitor is used to lock this node.  This object
     * is used in preference to the node itself to reduce the likelihood of
     * intentional or unintentional denial of service due to a locked node.
     * To avoid deadlock, a node is <i>never</i> locked by a thread that
     * holds a lock on a descendant of that node.
     */
    protected final Object lock = new Object();

    /**
     * Creates a preference node with the specified parent and the specified
     * name relative to its parent.
     *
     * @param parent the parent of this preference node, or null if this
     *               is the root.
     * @param name the name of this preference node, relative to its parent,
     *             or {@code ""} if this is the root.
     * @throws IllegalArgumentException if {@code name} contains a slash
     *          ({@code '/'}),  or {@code parent} is {@code null} and
     *          name isn't {@code ""}.
     */
    protected AbstractPreferences(AbstractPreferences parent, String name) {
        if (parent==null) {
            if (!name.isEmpty())
                throw new IllegalArgumentException("Root name '"+name+
                                                   "' must be \"\"");
            this.absolutePath = "/";
            root = this;
        } else {
            if (name.indexOf('/') != -1)
                throw new IllegalArgumentException("Name '" + name +
                                                 "' contains '/'");
            if (name.isEmpty())
              throw new IllegalArgumentException("Illegal name: empty string");

            root = parent.root;
            absolutePath = (parent==root ? "/" + name
                                         : parent.absolutePath() + "/" + name);
        }
        this.name = name;
        this.parent = parent;
    }

    /**
     * Implements the {@code put} method as per the specification in
     * {@link Preferences#put(String,String)}.
     *
     * <p>This implementation checks that the key and value are legal,
     * obtains this preference node's lock, checks that the node
     * has not been removed, invokes {@link #putSpi(String,String)}, and if
     * there are any preference change listeners, enqueues a notification
     * event for processing by the event dispatch thread.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @throws NullPointerException if key or value is {@code null}.
     * @throws IllegalArgumentException if {@code key.length()} exceeds
     *       {@code MAX_KEY_LENGTH} or if {@code value.length} exceeds
     *       {@code MAX_VALUE_LENGTH}.
     * @throws IllegalArgumentException if either key or value contain
     *         the null control character, code point U+0000.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public void put(String key, String value) {
        if (key==null || value==null)
            throw new NullPointerException();
        if (key.length() > MAX_KEY_LENGTH)
            throw new IllegalArgumentException("Key too long: "+key);
        if (value.length() > MAX_VALUE_LENGTH)
            throw new IllegalArgumentException("Value too long: "+value);
        if (key.indexOf(CODE_POINT_U0000) != -1)
            throw new IllegalArgumentException("Key contains code point U+0000");
        if (value.indexOf(CODE_POINT_U0000) != -1)
            throw new IllegalArgumentException("Value contains code point U+0000");

        synchronized(lock) {
            if (removed)
                throw new IllegalStateException("Node has been removed.");

            putSpi(key, value);
            enqueuePreferenceChangeEvent(key, value);
        }
    }

    /**
     * Implements the {@code get} method as per the specification in
     * {@link Preferences#get(String,String)}.
     *
     * <p>This implementation first checks to see if {@code key} is
     * {@code null} throwing a {@code NullPointerException} if this is
     * the case.  Then it obtains this preference node's lock,
     * checks that the node has not been removed, invokes {@link
     * #getSpi(String)}, and returns the result, unless the {@code getSpi}
     * invocation returns {@code null} or throws an exception, in which case
     * this invocation returns {@code def}.
     *
     * @param key key whose associated value is to be returned.
     * @param def the value to be returned in the event that this
     *        preference node has no value associated with {@code key}.
     * @return the value associated with {@code key}, or {@code def}
     *         if no value is associated with {@code key}.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws NullPointerException if key is {@code null}.  (A
     *         {@code null} default <i>is</i> permitted.)
     * @throws IllegalArgumentException if key contains the null control
     *         character, code point U+0000.
     */
    public String get(String key, String def) {
        if (key==null)
            throw new NullPointerException("Null key");
        if (key.indexOf(CODE_POINT_U0000) != -1)
            throw new IllegalArgumentException("Key contains code point U+0000");
        synchronized(lock) {
            if (removed)
                throw new IllegalStateException("Node has been removed.");

            String result = null;
            try {
                result = getSpi(key);
            } catch (Exception e) {
                // Ignoring exception causes default to be returned
            }
            return (result==null ? def : result);
        }
    }

    /**
     * Implements the {@code remove(String)} method as per the specification
     * in {@link Preferences#remove(String)}.
     *
     * <p>This implementation obtains this preference node's lock,
     * checks that the node has not been removed, invokes
     * {@link #removeSpi(String)} and if there are any preference
     * change listeners, enqueues a notification event for processing by the
     * event dispatch thread.
     *
     * @param key key whose mapping is to be removed from the preference node.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws IllegalArgumentException if key contains the null control
     *         character, code point U+0000.
     * @throws NullPointerException {@inheritDoc}.
     */
    public void remove(String key) {
        Objects.requireNonNull(key, "Specified key cannot be null");
        if (key.indexOf(CODE_POINT_U0000) != -1)
            throw new IllegalArgumentException("Key contains code point U+0000");
        synchronized(lock) {
            if (removed)
                throw new IllegalStateException("Node has been removed.");

            removeSpi(key);
            enqueuePreferenceChangeEvent(key, null);
        }
    }

    /**
     * Implements the {@code clear} method as per the specification in
     * {@link Preferences#clear()}.
     *
     * <p>This implementation obtains this preference node's lock,
     * invokes {@link #keys()} to obtain an array of keys, and
     * iterates over the array invoking {@link #remove(String)} on each key.
     *
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public void clear() throws BackingStoreException {
        synchronized(lock) {
            for (String key : keys())
                remove(key);
        }
    }

    /**
     * Implements the {@code putInt} method as per the specification in
     * {@link Preferences#putInt(String,int)}.
     *
     * <p>This implementation translates {@code value} to a string with
     * {@link Integer#toString(int)} and invokes {@link #put(String,String)}
     * on the result.
     *
     * @param key key with which the string form of value is to be associated.
     * @param value value whose string form is to be associated with key.
     * @throws NullPointerException if key is {@code null}.
     * @throws IllegalArgumentException if {@code key.length()} exceeds
     *         {@code MAX_KEY_LENGTH}.
     * @throws IllegalArgumentException if key contains
     *         the null control character, code point U+0000.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public void putInt(String key, int value) {
        put(key, Integer.toString(value));
    }

    /**
     * Implements the {@code getInt} method as per the specification in
     * {@link Preferences#getInt(String,int)}.
     *
     * <p>This implementation invokes {@link #get(String,String) get(key,
     * null)}.  If the return value is non-null, the implementation
     * attempts to translate it to an {@code int} with
     * {@link Integer#parseInt(String)}.  If the attempt succeeds, the return
     * value is returned by this method.  Otherwise, {@code def} is returned.
     *
     * @param key key whose associated value is to be returned as an int.
     * @param def the value to be returned in the event that this
     *        preference node has no value associated with {@code key}
     *        or the associated value cannot be interpreted as an int.
     * @return the int value represented by the string associated with
     *         {@code key} in this preference node, or {@code def} if the
     *         associated value does not exist or cannot be interpreted as
     *         an int.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws IllegalArgumentException if key contains the null control
     *         character, code point U+0000.
     */
    public int getInt(String key, int def) {
        int result = def;
        try {
            String value = get(key, null);
            if (value != null)
                result = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Ignoring exception causes specified default to be returned
        }

        return result;
    }

    /**
     * Implements the {@code putLong} method as per the specification in
     * {@link Preferences#putLong(String,long)}.
     *
     * <p>This implementation translates {@code value} to a string with
     * {@link Long#toString(long)} and invokes {@link #put(String,String)}
     * on the result.
     *
     * @param key key with which the string form of value is to be associated.
     * @param value value whose string form is to be associated with key.
     * @throws NullPointerException if key is {@code null}.
     * @throws IllegalArgumentException if {@code key.length()} exceeds
     *         {@code MAX_KEY_LENGTH}.
     * @throws IllegalArgumentException if key contains
     *         the null control character, code point U+0000.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public void putLong(String key, long value) {
        put(key, Long.toString(value));
    }

    /**
     * Implements the {@code getLong} method as per the specification in
     * {@link Preferences#getLong(String,long)}.
     *
     * <p>This implementation invokes {@link #get(String,String) get(key,
     * null)}.  If the return value is non-null, the implementation
     * attempts to translate it to a {@code long} with
     * {@link Long#parseLong(String)}.  If the attempt succeeds, the return
     * value is returned by this method.  Otherwise, {@code def} is returned.
     *
     * @param key key whose associated value is to be returned as a long.
     * @param def the value to be returned in the event that this
     *        preference node has no value associated with {@code key}
     *        or the associated value cannot be interpreted as a long.
     * @return the long value represented by the string associated with
     *         {@code key} in this preference node, or {@code def} if the
     *         associated value does not exist or cannot be interpreted as
     *         a long.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws IllegalArgumentException if key contains the null control
     *         character, code point U+0000.
     */
    public long getLong(String key, long def) {
        long result = def;
        try {
            String value = get(key, null);
            if (value != null)
                result = Long.parseLong(value);
        } catch (NumberFormatException e) {
            // Ignoring exception causes specified default to be returned
        }

        return result;
    }

    /**
     * Implements the {@code putBoolean} method as per the specification in
     * {@link Preferences#putBoolean(String,boolean)}.
     *
     * <p>This implementation translates {@code value} to a string with
     * {@link String#valueOf(boolean)} and invokes {@link #put(String,String)}
     * on the result.
     *
     * @param key key with which the string form of value is to be associated.
     * @param value value whose string form is to be associated with key.
     * @throws NullPointerException if key is {@code null}.
     * @throws IllegalArgumentException if {@code key.length()} exceeds
     *         {@code MAX_KEY_LENGTH}.
     * @throws IllegalArgumentException if key contains
     *         the null control character, code point U+0000.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public void putBoolean(String key, boolean value) {
        put(key, String.valueOf(value));
    }

    /**
     * Implements the {@code getBoolean} method as per the specification in
     * {@link Preferences#getBoolean(String,boolean)}.
     *
     * <p>This implementation invokes {@link #get(String,String) get(key,
     * null)}.  If the return value is non-null, it is compared with
     * {@code "true"} using {@link String#equalsIgnoreCase(String)}.  If the
     * comparison returns {@code true}, this invocation returns
     * {@code true}.  Otherwise, the original return value is compared with
     * {@code "false"}, again using {@link String#equalsIgnoreCase(String)}.
     * If the comparison returns {@code true}, this invocation returns
     * {@code false}.  Otherwise, this invocation returns {@code def}.
     *
     * @param key key whose associated value is to be returned as a boolean.
     * @param def the value to be returned in the event that this
     *        preference node has no value associated with {@code key}
     *        or the associated value cannot be interpreted as a boolean.
     * @return the boolean value represented by the string associated with
     *         {@code key} in this preference node, or {@code def} if the
     *         associated value does not exist or cannot be interpreted as
     *         a boolean.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws IllegalArgumentException if key contains the null control
     *         character, code point U+0000.
     */
    public boolean getBoolean(String key, boolean def) {
        boolean result = def;
        String value = get(key, null);
        if (value != null) {
            if (value.equalsIgnoreCase("true"))
                result = true;
            else if (value.equalsIgnoreCase("false"))
                result = false;
        }

        return result;
    }

    /**
     * Implements the {@code putFloat} method as per the specification in
     * {@link Preferences#putFloat(String,float)}.
     *
     * <p>This implementation translates {@code value} to a string with
     * {@link Float#toString(float)} and invokes {@link #put(String,String)}
     * on the result.
     *
     * @param key key with which the string form of value is to be associated.
     * @param value value whose string form is to be associated with key.
     * @throws NullPointerException if key is {@code null}.
     * @throws IllegalArgumentException if {@code key.length()} exceeds
     *         {@code MAX_KEY_LENGTH}.
     * @throws IllegalArgumentException if key contains
     *         the null control character, code point U+0000.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public void putFloat(String key, float value) {
        put(key, Float.toString(value));
    }

    /**
     * Implements the {@code getFloat} method as per the specification in
     * {@link Preferences#getFloat(String,float)}.
     *
     * <p>This implementation invokes {@link #get(String,String) get(key,
     * null)}.  If the return value is non-null, the implementation
     * attempts to translate it to an {@code float} with
     * {@link Float#parseFloat(String)}.  If the attempt succeeds, the return
     * value is returned by this method.  Otherwise, {@code def} is returned.
     *
     * @param key key whose associated value is to be returned as a float.
     * @param def the value to be returned in the event that this
     *        preference node has no value associated with {@code key}
     *        or the associated value cannot be interpreted as a float.
     * @return the float value represented by the string associated with
     *         {@code key} in this preference node, or {@code def} if the
     *         associated value does not exist or cannot be interpreted as
     *         a float.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws IllegalArgumentException if key contains the null control
     *         character, code point U+0000.
     */
    public float getFloat(String key, float def) {
        float result = def;
        try {
            String value = get(key, null);
            if (value != null)
                result = Float.parseFloat(value);
        } catch (NumberFormatException e) {
            // Ignoring exception causes specified default to be returned
        }

        return result;
    }

    /**
     * Implements the {@code putDouble} method as per the specification in
     * {@link Preferences#putDouble(String,double)}.
     *
     * <p>This implementation translates {@code value} to a string with
     * {@link Double#toString(double)} and invokes {@link #put(String,String)}
     * on the result.
     *
     * @param key key with which the string form of value is to be associated.
     * @param value value whose string form is to be associated with key.
     * @throws NullPointerException if key is {@code null}.
     * @throws IllegalArgumentException if {@code key.length()} exceeds
     *         {@code MAX_KEY_LENGTH}.
     * @throws IllegalArgumentException if key contains
     *         the null control character, code point U+0000.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public void putDouble(String key, double value) {
        put(key, Double.toString(value));
    }

    /**
     * Implements the {@code getDouble} method as per the specification in
     * {@link Preferences#getDouble(String,double)}.
     *
     * <p>This implementation invokes {@link #get(String,String) get(key,
     * null)}.  If the return value is non-null, the implementation
     * attempts to translate it to an {@code double} with
     * {@link Double#parseDouble(String)}.  If the attempt succeeds, the return
     * value is returned by this method.  Otherwise, {@code def} is returned.
     *
     * @param key key whose associated value is to be returned as a double.
     * @param def the value to be returned in the event that this
     *        preference node has no value associated with {@code key}
     *        or the associated value cannot be interpreted as a double.
     * @return the double value represented by the string associated with
     *         {@code key} in this preference node, or {@code def} if the
     *         associated value does not exist or cannot be interpreted as
     *         a double.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws IllegalArgumentException if key contains the null control
     *         character, code point U+0000.
     */
    public double getDouble(String key, double def) {
        double result = def;
        try {
            String value = get(key, null);
            if (value != null)
                result = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // Ignoring exception causes specified default to be returned
        }

        return result;
    }

    /**
     * Implements the {@code putByteArray} method as per the specification in
     * {@link Preferences#putByteArray(String,byte[])}.
     *
     * @param key key with which the string form of value is to be associated.
     * @param value value whose string form is to be associated with key.
     * @throws NullPointerException if key or value is {@code null}.
     * @throws IllegalArgumentException if key.length() exceeds MAX_KEY_LENGTH
     *         or if value.length exceeds MAX_VALUE_LENGTH*3/4.
     * @throws IllegalArgumentException if key contains
     *         the null control character, code point U+0000.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public void putByteArray(String key, byte[] value) {
        put(key, Base64.byteArrayToBase64(value));
    }

    /**
     * Implements the {@code getByteArray} method as per the specification in
     * {@link Preferences#getByteArray(String,byte[])}.
     *
     * @param key key whose associated value is to be returned as a byte array.
     * @param def the value to be returned in the event that this
     *        preference node has no value associated with {@code key}
     *        or the associated value cannot be interpreted as a byte array.
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
     */
    public byte[] getByteArray(String key, byte[] def) {
        byte[] result = def;
        String value = get(key, null);
        try {
            if (value != null)
                result = Base64.base64ToByteArray(value);
        }
        catch (RuntimeException e) {
            // Ignoring exception causes specified default to be returned
        }

        return result;
    }

    /**
     * Implements the {@code keys} method as per the specification in
     * {@link Preferences#keys()}.
     *
     * <p>This implementation obtains this preference node's lock, checks that
     * the node has not been removed and invokes {@link #keysSpi()}.
     *
     * @return an array of the keys that have an associated value in this
     *         preference node.
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public String[] keys() throws BackingStoreException {
        synchronized(lock) {
            if (removed)
                throw new IllegalStateException("Node has been removed.");

            return keysSpi();
        }
    }

    /**
     * Implements the {@code children} method as per the specification in
     * {@link Preferences#childrenNames()}.
     *
     * <p>This implementation obtains this preference node's lock, checks that
     * the node has not been removed, constructs a {@code TreeSet} initialized
     * to the names of children already cached (the children in this node's
     * "child-cache"), invokes {@link #childrenNamesSpi()}, and adds all of the
     * returned child-names into the set.  The elements of the tree set are
     * dumped into a {@code String} array using the {@code toArray} method,
     * and this array is returned.
     *
     * @return the names of the children of this preference node.
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @see #cachedChildren()
     */
    public String[] childrenNames() throws BackingStoreException {
        synchronized(lock) {
            if (removed)
                throw new IllegalStateException("Node has been removed.");

            Set<String> s = new TreeSet<>(kidCache.keySet());
            for (String kid : childrenNamesSpi())
                s.add(kid);
            return s.toArray(EMPTY_STRING_ARRAY);
        }
    }

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Returns all known unremoved children of this node.
     *
     * @return all known unremoved children of this node.
     */
    protected final AbstractPreferences[] cachedChildren() {
        return kidCache.values().toArray(EMPTY_ABSTRACT_PREFS_ARRAY);
    }

    private static final AbstractPreferences[] EMPTY_ABSTRACT_PREFS_ARRAY
        = new AbstractPreferences[0];

    /**
     * Implements the {@code parent} method as per the specification in
     * {@link Preferences#parent()}.
     *
     * <p>This implementation obtains this preference node's lock, checks that
     * the node has not been removed and returns the parent value that was
     * passed to this node's constructor.
     *
     * @return the parent of this preference node.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public Preferences parent() {
        synchronized(lock) {
            if (removed)
                throw new IllegalStateException("Node has been removed.");

            return parent;
        }
    }

    /**
     * Implements the {@code node} method as per the specification in
     * {@link Preferences#node(String)}.
     *
     * <p>This implementation obtains this preference node's lock and checks
     * that the node has not been removed.  If {@code path} is {@code ""},
     * this node is returned; if {@code path} is {@code "/"}, this node's
     * root is returned.  If the first character in {@code path} is
     * not {@code '/'}, the implementation breaks {@code path} into
     * tokens and recursively traverses the path from this node to the
     * named node, "consuming" a name and a slash from {@code path} at
     * each step of the traversal.  At each step, the current node is locked
     * and the node's child-cache is checked for the named node.  If it is
     * not found, the name is checked to make sure its length does not
     * exceed {@code MAX_NAME_LENGTH}.  Then the {@link #childSpi(String)}
     * method is invoked, and the result stored in this node's child-cache.
     * If the newly created {@code Preferences} object's {@link #newNode}
     * field is {@code true} and there are any node change listeners,
     * a notification event is enqueued for processing by the event dispatch
     * thread.
     *
     * <p>When there are no more tokens, the last value found in the
     * child-cache or returned by {@code childSpi} is returned by this
     * method.  If during the traversal, two {@code "/"} tokens occur
     * consecutively, or the final token is {@code "/"} (rather than a name),
     * an appropriate {@code IllegalArgumentException} is thrown.
     *
     * <p> If the first character of {@code path} is {@code '/'}
     * (indicating an absolute path name) this preference node's
     * lock is dropped prior to breaking {@code path} into tokens, and
     * this method recursively traverses the path starting from the root
     * (rather than starting from this node).  The traversal is otherwise
     * identical to the one described for relative path names.  Dropping
     * the lock on this node prior to commencing the traversal at the root
     * node is essential to avoid the possibility of deadlock, as per the
     * {@link #lock locking invariant}.
     *
     * @param path the path name of the preference node to return.
     * @return the specified preference node.
     * @throws IllegalArgumentException if the path name is invalid (i.e.,
     *         it contains multiple consecutive slash characters, or ends
     *         with a slash character and is more than one character long).
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public Preferences node(String path) {
        synchronized(lock) {
            if (removed)
                throw new IllegalStateException("Node has been removed.");
            if (path.isEmpty())
                return this;
            if (path.equals("/"))
                return root;
            if (path.charAt(0) != '/')
                return node(new StringTokenizer(path, "/", true));
        }

        // Absolute path.  Note that we've dropped our lock to avoid deadlock
        return root.node(new StringTokenizer(path.substring(1), "/", true));
    }

    /**
     * tokenizer contains <name> {'/' <name>}*
     */
    private Preferences node(StringTokenizer path) {
        String token = path.nextToken();
        if (token.equals("/"))  // Check for consecutive slashes
            throw new IllegalArgumentException("Consecutive slashes in path");
        synchronized(lock) {
            AbstractPreferences child = kidCache.get(token);
            if (child == null) {
                if (token.length() > MAX_NAME_LENGTH)
                    throw new IllegalArgumentException(
                        "Node name " + token + " too long");
                child = childSpi(token);
                if (child.newNode)
                    enqueueNodeAddedEvent(child);
                kidCache.put(token, child);
            }
            if (!path.hasMoreTokens())
                return child;
            path.nextToken();  // Consume slash
            if (!path.hasMoreTokens())
                throw new IllegalArgumentException("Path ends with slash");
            return child.node(path);
        }
    }

    /**
     * Implements the {@code nodeExists} method as per the specification in
     * {@link Preferences#nodeExists(String)}.
     *
     * <p>This implementation is very similar to {@link #node(String)},
     * except that {@link #getChild(String)} is used instead of {@link
     * #childSpi(String)}.
     *
     * @param path the path name of the node whose existence is to be checked.
     * @return true if the specified node exists.
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     * @throws IllegalArgumentException if the path name is invalid (i.e.,
     *         it contains multiple consecutive slash characters, or ends
     *         with a slash character and is more than one character long).
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method and
     *         {@code pathname} is not the empty string ({@code ""}).
     */
    public boolean nodeExists(String path)
        throws BackingStoreException
    {
        synchronized(lock) {
            if (path.isEmpty())
                return !removed;
            if (removed)
                throw new IllegalStateException("Node has been removed.");
            if (path.equals("/"))
                return true;
            if (path.charAt(0) != '/')
                return nodeExists(new StringTokenizer(path, "/", true));
        }

        // Absolute path.  Note that we've dropped our lock to avoid deadlock
        return root.nodeExists(new StringTokenizer(path.substring(1), "/",
                                                   true));
    }

    /**
     * tokenizer contains <name> {'/' <name>}*
     */
    private boolean nodeExists(StringTokenizer path)
        throws BackingStoreException
    {
        String token = path.nextToken();
        if (token.equals("/"))  // Check for consecutive slashes
            throw new IllegalArgumentException("Consecutive slashes in path");
        synchronized(lock) {
            AbstractPreferences child = kidCache.get(token);
            if (child == null)
                child = getChild(token);
            if (child==null)
                return false;
            if (!path.hasMoreTokens())
                return true;
            path.nextToken();  // Consume slash
            if (!path.hasMoreTokens())
                throw new IllegalArgumentException("Path ends with slash");
            return child.nodeExists(path);
        }
    }

    /**

     * Implements the {@code removeNode()} method as per the specification in
     * {@link Preferences#removeNode()}.
     *
     * <p>This implementation checks to see that this node is the root; if so,
     * it throws an appropriate exception.  Then, it locks this node's parent,
     * and calls a recursive helper method that traverses the subtree rooted at
     * this node.  The recursive method locks the node on which it was called,
     * checks that it has not already been removed, and then ensures that all
     * of its children are cached: The {@link #childrenNamesSpi()} method is
     * invoked and each returned child name is checked for containment in the
     * child-cache.  If a child is not already cached, the {@link
     * #childSpi(String)} method is invoked to create a {@code Preferences}
     * instance for it, and this instance is put into the child-cache.  Then
     * the helper method calls itself recursively on each node contained in its
     * child-cache.  Next, it invokes {@link #removeNodeSpi()}, marks itself
     * as removed, and removes itself from its parent's child-cache.  Finally,
     * if there are any node change listeners, it enqueues a notification
     * event for processing by the event dispatch thread.
     *
     * <p>Note that the helper method is always invoked with all ancestors up
     * to the "closest non-removed ancestor" locked.
     *
     * @throws IllegalStateException if this node (or an ancestor) has already
     *         been removed with the {@link #removeNode()} method.
     * @throws UnsupportedOperationException if this method is invoked on
     *         the root node.
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     */
    public void removeNode() throws BackingStoreException {
        if (this==root)
            throw new UnsupportedOperationException("Can't remove the root!");
        synchronized(parent.lock) {
            removeNode2();
            parent.kidCache.remove(name);
        }
    }

    /*
     * Called with locks on all nodes on path from parent of "removal root"
     * to this (including the former but excluding the latter).
     */
    private void removeNode2() throws BackingStoreException {
        synchronized(lock) {
            if (removed)
                throw new IllegalStateException("Node already removed.");

            // Ensure that all children are cached
            String[] kidNames = childrenNamesSpi();
            for (String kidName : kidNames)
                if (!kidCache.containsKey(kidName))
                    kidCache.put(kidName, childSpi(kidName));

            // Recursively remove all cached children
            for (Iterator<AbstractPreferences> i = kidCache.values().iterator();
                 i.hasNext();) {
                try {
                    i.next().removeNode2();
                    i.remove();
                } catch (BackingStoreException x) { }
            }

            // Now we have no descendants - it's time to die!
            removeNodeSpi();
            removed = true;
            parent.enqueueNodeRemovedEvent(this);
        }
    }

    /**
     * Implements the {@code name} method as per the specification in
     * {@link Preferences#name()}.
     *
     * <p>This implementation merely returns the name that was
     * passed to this node's constructor.
     *
     * @return this preference node's name, relative to its parent.
     */
    public String name() {
        return name;
    }

    /**
     * Implements the {@code absolutePath} method as per the specification in
     * {@link Preferences#absolutePath()}.
     *
     * <p>This implementation merely returns the absolute path name that
     * was computed at the time that this node was constructed (based on
     * the name that was passed to this node's constructor, and the names
     * that were passed to this node's ancestors' constructors).
     *
     * @return this preference node's absolute path name.
     */
    public String absolutePath() {
        return absolutePath;
    }

    /**
     * Implements the {@code isUserNode} method as per the specification in
     * {@link Preferences#isUserNode()}.
     *
     * <p>This implementation compares this node's root node (which is stored
     * in a private field) with the value returned by
     * {@link Preferences#userRoot()}.  If the two object references are
     * identical, this method returns true.
     *
     * @return {@code true} if this preference node is in the user
     *         preference tree, {@code false} if it's in the system
     *         preference tree.
     */
    @SuppressWarnings("removal")
    public boolean isUserNode() {
        return AccessController.doPrivileged(
            new PrivilegedAction<Boolean>() {
                public Boolean run() {
                    return root == Preferences.userRoot();
            }
            }).booleanValue();
    }

    public void addPreferenceChangeListener(PreferenceChangeListener pcl) {
        if (pcl==null)
            throw new NullPointerException("Change listener is null.");
        synchronized(lock) {
            if (removed)
                throw new IllegalStateException("Node has been removed.");

            // Copy-on-write
            PreferenceChangeListener[] old = prefListeners;
            prefListeners = new PreferenceChangeListener[old.length + 1];
            System.arraycopy(old, 0, prefListeners, 0, old.length);
            prefListeners[old.length] = pcl;
        }
        startEventDispatchThreadIfNecessary();
    }

    public void removePreferenceChangeListener(PreferenceChangeListener pcl) {
        synchronized(lock) {
            if (removed)
                throw new IllegalStateException("Node has been removed.");
            if ((prefListeners == null) || (prefListeners.length == 0))
                throw new IllegalArgumentException("Listener not registered.");

            // Copy-on-write
            PreferenceChangeListener[] newPl =
                new PreferenceChangeListener[prefListeners.length - 1];
            int i = 0;
            while (i < newPl.length && prefListeners[i] != pcl)
                newPl[i] = prefListeners[i++];

            if (i == newPl.length &&  prefListeners[i] != pcl)
                throw new IllegalArgumentException("Listener not registered.");
            while (i < newPl.length)
                newPl[i] = prefListeners[++i];
            prefListeners = newPl;
        }
    }

    public void addNodeChangeListener(NodeChangeListener ncl) {
        if (ncl==null)
            throw new NullPointerException("Change listener is null.");
        synchronized(lock) {
            if (removed)
                throw new IllegalStateException("Node has been removed.");

            // Copy-on-write
            if (nodeListeners == null) {
                nodeListeners = new NodeChangeListener[1];
                nodeListeners[0] = ncl;
            } else {
                NodeChangeListener[] old = nodeListeners;
                nodeListeners = new NodeChangeListener[old.length + 1];
                System.arraycopy(old, 0, nodeListeners, 0, old.length);
                nodeListeners[old.length] = ncl;
            }
        }
        startEventDispatchThreadIfNecessary();
    }

    public void removeNodeChangeListener(NodeChangeListener ncl) {
        synchronized(lock) {
            if (removed)
                throw new IllegalStateException("Node has been removed.");
            if ((nodeListeners == null) || (nodeListeners.length == 0))
                throw new IllegalArgumentException("Listener not registered.");

            // Copy-on-write
            int i = 0;
            while (i < nodeListeners.length && nodeListeners[i] != ncl)
                i++;
            if (i == nodeListeners.length)
                throw new IllegalArgumentException("Listener not registered.");
            NodeChangeListener[] newNl =
                new NodeChangeListener[nodeListeners.length - 1];
            if (i != 0)
                System.arraycopy(nodeListeners, 0, newNl, 0, i);
            if (i != newNl.length)
                System.arraycopy(nodeListeners, i + 1,
                                 newNl, i, newNl.length - i);
            nodeListeners = newNl;
        }
    }

    // "SPI" METHODS

    /**
     * Put the given key-value association into this preference node.  It is
     * guaranteed that {@code key} and {@code value} are non-null and of
     * legal length.  Also, it is guaranteed that this node has not been
     * removed.  (The implementor needn't check for any of these things.)
     *
     * <p>This method is invoked with the lock on this node held.
     * @param key the key
     * @param value the value
     */
    protected abstract void putSpi(String key, String value);

    /**
     * Return the value associated with the specified key at this preference
     * node, or {@code null} if there is no association for this key, or the
     * association cannot be determined at this time.  It is guaranteed that
     * {@code key} is non-null.  Also, it is guaranteed that this node has
     * not been removed.  (The implementor needn't check for either of these
     * things.)
     *
     * <p> Generally speaking, this method should not throw an exception
     * under any circumstances.  If, however, if it does throw an exception,
     * the exception will be intercepted and treated as a {@code null}
     * return value.
     *
     * <p>This method is invoked with the lock on this node held.
     *
     * @param key the key
     * @return the value associated with the specified key at this preference
     *          node, or {@code null} if there is no association for this
     *          key, or the association cannot be determined at this time.
     */
    protected abstract String getSpi(String key);

    /**
     * Remove the association (if any) for the specified key at this
     * preference node.  It is guaranteed that {@code key} is non-null.
     * Also, it is guaranteed that this node has not been removed.
     * (The implementor needn't check for either of these things.)
     *
     * <p>This method is invoked with the lock on this node held.
     * @param key the key
     */
    protected abstract void removeSpi(String key);

    /**
     * Removes this preference node, invalidating it and any preferences that
     * it contains.  The named child will have no descendants at the time this
     * invocation is made (i.e., the {@link Preferences#removeNode()} method
     * invokes this method repeatedly in a bottom-up fashion, removing each of
     * a node's descendants before removing the node itself).
     *
     * <p>This method is invoked with the lock held on this node and its
     * parent (and all ancestors that are being removed as a
     * result of a single invocation to {@link Preferences#removeNode()}).
     *
     * <p>The removal of a node needn't become persistent until the
     * {@code flush} method is invoked on this node (or an ancestor).
     *
     * <p>If this node throws a {@code BackingStoreException}, the exception
     * will propagate out beyond the enclosing {@link #removeNode()}
     * invocation.
     *
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     */
    protected abstract void removeNodeSpi() throws BackingStoreException;

    /**
     * Returns all of the keys that have an associated value in this
     * preference node.  (The returned array will be of size zero if
     * this node has no preferences.)  It is guaranteed that this node has not
     * been removed.
     *
     * <p>This method is invoked with the lock on this node held.
     *
     * <p>If this node throws a {@code BackingStoreException}, the exception
     * will propagate out beyond the enclosing {@link #keys()} invocation.
     *
     * @return an array of the keys that have an associated value in this
     *         preference node.
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     */
    protected abstract String[] keysSpi() throws BackingStoreException;

    /**
     * Returns the names of the children of this preference node.  (The
     * returned array will be of size zero if this node has no children.)
     * This method need not return the names of any nodes already cached,
     * but may do so without harm.
     *
     * <p>This method is invoked with the lock on this node held.
     *
     * <p>If this node throws a {@code BackingStoreException}, the exception
     * will propagate out beyond the enclosing {@link #childrenNames()}
     * invocation.
     *
     * @return an array containing the names of the children of this
     *         preference node.
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     */
    protected abstract String[] childrenNamesSpi()
        throws BackingStoreException;

    /**
     * Returns the named child if it exists, or {@code null} if it does not.
     * It is guaranteed that {@code nodeName} is non-null, non-empty,
     * does not contain the slash character ('/'), and is no longer than
     * {@link #MAX_NAME_LENGTH} characters.  Also, it is guaranteed
     * that this node has not been removed.  (The implementor needn't check
     * for any of these things if he chooses to override this method.)
     *
     * <p>Finally, it is guaranteed that the named node has not been returned
     * by a previous invocation of this method or {@link #childSpi} after the
     * last time that it was removed.  In other words, a cached value will
     * always be used in preference to invoking this method.  (The implementor
     * needn't maintain his own cache of previously returned children if he
     * chooses to override this method.)
     *
     * <p>This implementation obtains this preference node's lock, invokes
     * {@link #childrenNames()} to get an array of the names of this node's
     * children, and iterates over the array comparing the name of each child
     * with the specified node name.  If a child node has the correct name,
     * the {@link #childSpi(String)} method is invoked and the resulting
     * node is returned.  If the iteration completes without finding the
     * specified name, {@code null} is returned.
     *
     * @param nodeName name of the child to be searched for.
     * @return the named child if it exists, or null if it does not.
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     */
    protected AbstractPreferences getChild(String nodeName)
            throws BackingStoreException {
        synchronized(lock) {
            // assert kidCache.get(nodeName)==null;
            String[] kidNames = childrenNames();
            for (String kidName : kidNames)
                if (kidName.equals(nodeName))
                    return childSpi(kidName);
        }
        return null;
    }

    /**
     * Returns the named child of this preference node, creating it if it does
     * not already exist.  It is guaranteed that {@code name} is non-null,
     * non-empty, does not contain the slash character ('/'), and is no longer
     * than {@link #MAX_NAME_LENGTH} characters.  Also, it is guaranteed that
     * this node has not been removed.  (The implementor needn't check for any
     * of these things.)
     *
     * <p>Finally, it is guaranteed that the named node has not been returned
     * by a previous invocation of this method or {@link #getChild(String)}
     * after the last time that it was removed.  In other words, a cached
     * value will always be used in preference to invoking this method.
     * Subclasses need not maintain their own cache of previously returned
     * children.
     *
     * <p>The implementer must ensure that the returned node has not been
     * removed.  If a like-named child of this node was previously removed, the
     * implementer must return a newly constructed {@code AbstractPreferences}
     * node; once removed, an {@code AbstractPreferences} node
     * cannot be "resuscitated."
     *
     * <p>If this method causes a node to be created, this node is not
     * guaranteed to be persistent until the {@code flush} method is
     * invoked on this node or one of its ancestors (or descendants).
     *
     * <p>This method is invoked with the lock on this node held.
     *
     * @param name The name of the child node to return, relative to
     *        this preference node.
     * @return The named child node.
     */
    protected abstract AbstractPreferences childSpi(String name);

    /**
     * Returns the absolute path name of this preferences node.
     */
    public String toString() {
        return (this.isUserNode() ? "User" : "System") +
               " Preference Node: " + this.absolutePath();
    }

    /**
     * Implements the {@code sync} method as per the specification in
     * {@link Preferences#sync()}.
     *
     * <p>This implementation calls a recursive helper method that locks this
     * node, invokes syncSpi() on it, unlocks this node, and recursively
     * invokes this method on each "cached child."  A cached child is a child
     * of this node that has been created in this VM and not subsequently
     * removed.  In effect, this method does a depth first traversal of the
     * "cached subtree" rooted at this node, calling syncSpi() on each node in
     * the subTree while only that node is locked. Note that syncSpi() is
     * invoked top-down.
     *
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @see #flush()
     */
    public void sync() throws BackingStoreException {
        sync2();
    }

    private void sync2() throws BackingStoreException {
        AbstractPreferences[] cachedKids;

        synchronized(lock) {
            if (removed)
                throw new IllegalStateException("Node has been removed");
            syncSpi();
            cachedKids = cachedChildren();
        }

        for (AbstractPreferences cachedKid : cachedKids)
            cachedKid.sync2();
    }

    /**
     * This method is invoked with this node locked.  The contract of this
     * method is to synchronize any cached preferences stored at this node
     * with any stored in the backing store.  (It is perfectly possible that
     * this node does not exist on the backing store, either because it has
     * been deleted by another VM, or because it has not yet been created.)
     * Note that this method should <i>not</i> synchronize the preferences in
     * any subnodes of this node.  If the backing store naturally syncs an
     * entire subtree at once, the implementer is encouraged to override
     * sync(), rather than merely overriding this method.
     *
     * <p>If this node throws a {@code BackingStoreException}, the exception
     * will propagate out beyond the enclosing {@link #sync()} invocation.
     *
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     */
    protected abstract void syncSpi() throws BackingStoreException;

    /**
     * Implements the {@code flush} method as per the specification in
     * {@link Preferences#flush()}.
     *
     * <p>This implementation calls a recursive helper method that locks this
     * node, invokes flushSpi() on it, unlocks this node, and recursively
     * invokes this method on each "cached child."  A cached child is a child
     * of this node that has been created in this VM and not subsequently
     * removed.  In effect, this method does a depth first traversal of the
     * "cached subtree" rooted at this node, calling flushSpi() on each node in
     * the subTree while only that node is locked. Note that flushSpi() is
     * invoked top-down.
     *
     * <p> If this method is invoked on a node that has been removed with
     * the {@link #removeNode()} method, flushSpi() is invoked on this node,
     * but not on others.
     *
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     * @see #flush()
     */
    public void flush() throws BackingStoreException {
        flush2();
    }

    private void flush2() throws BackingStoreException {
        AbstractPreferences[] cachedKids;

        synchronized(lock) {
            flushSpi();
            if(removed)
                return;
            cachedKids = cachedChildren();
        }

        for (AbstractPreferences cachedKid : cachedKids)
            cachedKid.flush2();
    }

    /**
     * This method is invoked with this node locked.  The contract of this
     * method is to force any cached changes in the contents of this
     * preference node to the backing store, guaranteeing their persistence.
     * (It is perfectly possible that this node does not exist on the backing
     * store, either because it has been deleted by another VM, or because it
     * has not yet been created.)  Note that this method should <i>not</i>
     * flush the preferences in any subnodes of this node.  If the backing
     * store naturally flushes an entire subtree at once, the implementer is
     * encouraged to override flush(), rather than merely overriding this
     * method.
     *
     * <p>If this node throws a {@code BackingStoreException}, the exception
     * will propagate out beyond the enclosing {@link #flush()} invocation.
     *
     * @throws BackingStoreException if this operation cannot be completed
     *         due to a failure in the backing store, or inability to
     *         communicate with it.
     */
    protected abstract void flushSpi() throws BackingStoreException;

    /**
     * Returns {@code true} iff this node (or an ancestor) has been
     * removed with the {@link #removeNode()} method.  This method
     * locks this node prior to returning the contents of the private
     * field used to track this state.
     *
     * @return {@code true} iff this node (or an ancestor) has been
     *       removed with the {@link #removeNode()} method.
     */
    protected boolean isRemoved() {
        synchronized(lock) {
            return removed;
        }
    }

    /**
     * Queue of pending notification events.  When a preference or node
     * change event for which there are one or more listeners occurs,
     * it is placed on this queue and the queue is notified.  A background
     * thread waits on this queue and delivers the events.  This decouples
     * event delivery from preference activity, greatly simplifying
     * locking and reducing opportunity for deadlock.
     */
    private static final List<EventObject> eventQueue = new LinkedList<>();

    /**
     * These two classes are used to distinguish NodeChangeEvents on
     * eventQueue so the event dispatch thread knows whether to call
     * childAdded or childRemoved.
     */
    private class NodeAddedEvent extends NodeChangeEvent {
        private static final long serialVersionUID = -6743557530157328528L;
        NodeAddedEvent(Preferences parent, Preferences child) {
            super(parent, child);
        }
    }
    private class NodeRemovedEvent extends NodeChangeEvent {
        private static final long serialVersionUID = 8735497392918824837L;
        NodeRemovedEvent(Preferences parent, Preferences child) {
            super(parent, child);
        }
    }

    /**
     * A single background thread ("the event notification thread") monitors
     * the event queue and delivers events that are placed on the queue.
     */
    private static class EventDispatchThread extends Thread {
        private EventDispatchThread() {
            super(null, null, "Event Dispatch Thread", 0, false);
        }

        public void run() {
            while(true) {
                // Wait on eventQueue till an event is present
                EventObject event = null;
                synchronized(eventQueue) {
                    try {
                        while (eventQueue.isEmpty())
                            eventQueue.wait();
                        event = eventQueue.remove(0);
                    } catch (InterruptedException e) {
                        // XXX Log "Event dispatch thread interrupted. Exiting"
                        return;
                    }
                }

                // Now we have event & hold no locks; deliver evt to listeners
                AbstractPreferences src=(AbstractPreferences)event.getSource();
                if (event instanceof PreferenceChangeEvent) {
                    PreferenceChangeEvent pce = (PreferenceChangeEvent)event;
                    PreferenceChangeListener[] listeners = src.prefListeners();
                    for (PreferenceChangeListener listener : listeners)
                        listener.preferenceChange(pce);
                } else {
                    NodeChangeEvent nce = (NodeChangeEvent)event;
                    NodeChangeListener[] listeners = src.nodeListeners();
                    if (nce instanceof NodeAddedEvent) {
                        for (NodeChangeListener listener : listeners)
                            listener.childAdded(nce);
                    } else {
                        // assert nce instanceof NodeRemovedEvent;
                        for (NodeChangeListener listener : listeners)
                            listener.childRemoved(nce);
                    }
                }
            }
        }
    }

    private static Thread eventDispatchThread = null;

    /**
     * This method starts the event dispatch thread the first time it
     * is called.  The event dispatch thread will be started only
     * if someone registers a listener.
     */
    private static synchronized void startEventDispatchThreadIfNecessary() {
        if (eventDispatchThread == null) {
            // XXX Log "Starting event dispatch thread"
            eventDispatchThread = new EventDispatchThread();
            eventDispatchThread.setDaemon(true);
            eventDispatchThread.start();
        }
    }

    /**
     * Return this node's preference/node change listeners.  Even though
     * we're using a copy-on-write lists, we use synchronized accessors to
     * ensure information transmission from the writing thread to the
     * reading thread.
     */
    PreferenceChangeListener[] prefListeners() {
        synchronized(lock) {
            return prefListeners;
        }
    }
    NodeChangeListener[] nodeListeners() {
        synchronized(lock) {
            return nodeListeners;
        }
    }

    /**
     * Enqueue a preference change event for delivery to registered
     * preference change listeners unless there are no registered
     * listeners.  Invoked with this.lock held.
     */
    private void enqueuePreferenceChangeEvent(String key, String newValue) {
        if (prefListeners.length != 0) {
            synchronized(eventQueue) {
                eventQueue.add(new PreferenceChangeEvent(this, key, newValue));
                eventQueue.notify();
            }
        }
    }

    /**
     * Enqueue a "node added" event for delivery to registered node change
     * listeners unless there are no registered listeners.  Invoked with
     * this.lock held.
     */
    private void enqueueNodeAddedEvent(Preferences child) {
        if (nodeListeners.length != 0) {
            synchronized(eventQueue) {
                eventQueue.add(new NodeAddedEvent(this, child));
                eventQueue.notify();
            }
        }
    }

    /**
     * Enqueue a "node removed" event for delivery to registered node change
     * listeners unless there are no registered listeners.  Invoked with
     * this.lock held.
     */
    private void enqueueNodeRemovedEvent(Preferences child) {
        if (nodeListeners.length != 0) {
            synchronized(eventQueue) {
                eventQueue.add(new NodeRemovedEvent(this, child));
                eventQueue.notify();
            }
        }
    }

    /**
     * Implements the {@code exportNode} method as per the specification in
     * {@link Preferences#exportNode(OutputStream)}.
     *
     * @param os the output stream on which to emit the XML document.
     * @throws IOException if writing to the specified output stream
     *         results in an {@code IOException}.
     * @throws BackingStoreException if preference data cannot be read from
     *         backing store.
     */
    public void exportNode(OutputStream os)
        throws IOException, BackingStoreException
    {
        XmlSupport.export(os, this, false);
    }

    /**
     * Implements the {@code exportSubtree} method as per the specification in
     * {@link Preferences#exportSubtree(OutputStream)}.
     *
     * @param os the output stream on which to emit the XML document.
     * @throws IOException if writing to the specified output stream
     *         results in an {@code IOException}.
     * @throws BackingStoreException if preference data cannot be read from
     *         backing store.
     */
    public void exportSubtree(OutputStream os)
        throws IOException, BackingStoreException
    {
        XmlSupport.export(os, this, true);
    }
}
