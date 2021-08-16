/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.perf;

import java.nio.ByteBuffer;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.io.IOException;

import sun.nio.cs.UTF_8;

import jdk.internal.ref.CleanerFactory;

/**
 * The Perf class provides the ability to attach to an instrumentation
 * buffer maintained by a Java virtual machine. The instrumentation
 * buffer may be for the Java virtual machine running the methods of
 * this class or it may be for another Java virtual machine on the
 * same system.
 * <p>
 * In addition, this class provides methods to create instrumentation
 * objects in the instrumentation buffer for the Java virtual machine
 * that is running these methods. It also contains methods for acquiring
 * the value of a platform specific high resolution clock for time
 * stamp and interval measurement purposes.
 *
 * @author   Brian Doherty
 * @since    1.4.2
 * @see      #getPerf
 * @see      jdk.internal.perf.Perf.GetPerfAction
 * @see      java.nio.ByteBuffer
 */
public final class Perf {

    private static Perf instance;

    private static final int PERF_MODE_RO = 0;
    private static final int PERF_MODE_RW = 1;

    private Perf() { }    // prevent instantiation

    /**
     * The GetPerfAction class is a convenience class for acquiring access
     * to the singleton Perf instance using the
     * <code>AccessController.doPrivileged()</code> method.
     * <p>
     * An instance of this class can be used as the argument to
     * <code>AccessController.doPrivileged(PrivilegedAction)</code>.
     * <p> Here is a suggested idiom for use of this class:
     *
     * <blockquote><pre>{@code
     * class MyTrustedClass {
     *   private static final Perf perf =
     *       AccessController.doPrivileged(new Perf.GetPerfAction<Perf>());
     *   ...
     * }
     * }</pre></blockquote>
     * <p>
     * In the presence of a security manager, the <code>MyTrustedClass</code>
     * class in the above example will need to be granted the
     * <em>"sun.misc.Perf.getPerf"</em> <code>RuntimePermission</code>
     * permission in order to successfully acquire the singleton Perf instance.
     * <p>
     * Please note that the <em>"sun.misc.Perf.getPerf"</em> permission
     * is not a JDK specified permission.
     *
     * @see  java.security.AccessController#doPrivileged(PrivilegedAction)
     * @see  java.lang.RuntimePermission
     */
    public static class GetPerfAction implements PrivilegedAction<Perf>
    {
        /**
         * Run the <code>Perf.getPerf()</code> method in a privileged context.
         *
         * @see #getPerf
         */
        public Perf run() {
            return getPerf();
        }
    }

    /**
     * Return a reference to the singleton Perf instance.
     * <p>
     * The getPerf() method returns the singleton instance of the Perf
     * class. The returned object provides the caller with the capability
     * for accessing the instrumentation buffer for this or another local
     * Java virtual machine.
     * <p>
     * If a security manager is installed, its <code>checkPermission</code>
     * method is called with a <code>RuntimePermission</code> with a target
     * of <em>"sun.misc.Perf.getPerf"</em>. A security exception will result
     * if the caller has not been granted this permission.
     * <p>
     * Access to the returned <code>Perf</code> object should be protected
     * by its caller and not passed on to untrusted code. This object can
     * be used to attach to the instrumentation buffer provided by this Java
     * virtual machine or for those of other Java virtual machines running
     * on the same system. The instrumentation buffer may contain senstitive
     * information. API's built on top of this interface may want to provide
     * finer grained access control to the contents of individual
     * instrumentation objects contained within the buffer.
     * <p>
     * Please note that the <em>"sun.misc.Perf.getPerf"</em> permission
     * is not a JDK specified permission.
     *
     * @return  A reference to the singleton Perf instance.
     * @throws SecurityException  if a security manager exists and its
     *         <code>checkPermission</code> method doesn't allow access
     *         to the <em>"jdk.internal.perf.Perf.getPerf""</em> target.
     * @see  java.lang.RuntimePermission
     * @see  #attach
     */
    public static Perf getPerf()
    {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            Permission perm = new RuntimePermission("jdk.internal.perf.Perf.getPerf");
            security.checkPermission(perm);
        }

        return instance;
    }

    /**
     * Attach to the instrumentation buffer for the specified Java virtual
     * machine.
     * <p>
     * This method will attach to the instrumentation buffer for the
     * specified virtual machine. It returns a <code>ByteBuffer</code> object
     * that is initialized to access the instrumentation buffer for the
     * indicated Java virtual machine. The <code>lvmid</code> parameter is
     * a integer value that uniquely identifies the target local Java virtual
     * machine. It is typically, but not necessarily, the process id of
     * the target Java virtual machine.
     * <p>
     * If the <code>lvmid</code> identifies a Java virtual machine different
     * from the one running this method, then the coherency characteristics
     * of the buffer are implementation dependent. Implementations that do
     * not support named, coherent, shared memory may return a
     * <code>ByteBuffer</code> object that contains only a snap shot of the
     * data in the instrumentation buffer. Implementations that support named,
     * coherent, shared memory, may return a <code>ByteBuffer</code> object
     * that will be changing dynamically over time as the target Java virtual
     * machine updates its mapping of this buffer.
     * <p>
     * If the <code>lvmid</code> is 0 or equal to the actual <code>lvmid</code>
     * for the Java virtual machine running this method, then the returned
     * <code>ByteBuffer</code> object will always be coherent and dynamically
     * changing.
     * <p>
     * The attach mode specifies the access permissions requested for the
     * instrumentation buffer of the target virtual machine. The permitted
     * access permissions are:
     * <ul>
     * <li>"r"  - Read only access. This Java virtual machine has only
     * read access to the instrumentation buffer for the target Java
     * virtual machine.
     * <li>"rw"  - Read/Write access. This Java virtual machine has read and
     * write access to the instrumentation buffer for the target Java virtual
     * machine. This mode is currently not supported and is reserved for
     * future enhancements.
     * </ul>
     *
     * @param   lvmid            an integer that uniquely identifies the
     *                           target local Java virtual machine.
     * @param   mode             a string indicating the attach mode.
     * @return  ByteBuffer       a direct allocated byte buffer
     * @throws  IllegalArgumentException  The lvmid or mode was invalid.
     * @throws  IOException      An I/O error occurred while trying to acquire
     *                           the instrumentation buffer.
     * @throws  OutOfMemoryError The instrumentation buffer could not be mapped
     *                           into the virtual machine's address space.
     * @see     java.nio.ByteBuffer
     */
    public ByteBuffer attach(int lvmid, String mode)
           throws IllegalArgumentException, IOException
    {
        if (mode.compareTo("r") == 0) {
            return attachImpl(null, lvmid, PERF_MODE_RO);
        }
        else if (mode.compareTo("rw") == 0) {
            return attachImpl(null, lvmid, PERF_MODE_RW);
        }
        else {
            throw new IllegalArgumentException("unknown mode");
        }
    }

    /**
     * Attach to the instrumentation buffer for the specified Java virtual
     * machine owned by the given user.
     * <p>
     * This method behaves just as the <code>attach(int lvmid, String mode)
     * </code> method, except that it only searches for Java virtual machines
     * owned by the specified user.
     *
     * @param   user             A <code>String</code> object containing the
     *                           name of the user that owns the target Java
     *                           virtual machine.
     * @param   lvmid            an integer that uniquely identifies the
     *                           target local Java virtual machine.
     * @param   mode             a string indicating the attach mode.
     * @return  ByteBuffer       a direct allocated byte buffer
     * @throws  IllegalArgumentException  The lvmid or mode was invalid.
     * @throws  IOException      An I/O error occurred while trying to acquire
     *                           the instrumentation buffer.
     * @throws  OutOfMemoryError The instrumentation buffer could not be mapped
     *                           into the virtual machine's address space.
     * @see     java.nio.ByteBuffer
     */
    public ByteBuffer attach(String user, int lvmid, String mode)
           throws IllegalArgumentException, IOException
    {
        if (mode.compareTo("r") == 0) {
            return attachImpl(user, lvmid, PERF_MODE_RO);
        }
        else if (mode.compareTo("rw") == 0) {
            return attachImpl(user, lvmid, PERF_MODE_RW);
        }
        else {
            throw new IllegalArgumentException("unknown mode");
        }
    }

    /**
     * Call the implementation specific attach method.
     * <p>
     * This method calls into the Java virtual machine to perform the platform
     * specific attach method. Buffers returned from this method are
     * internally managed as <code>PhantomRefereces</code> to provide for
     * guaranteed, secure release of the native resources.
     *
     * @param   user             A <code>String</code> object containing the
     *                           name of the user that owns the target Java
     *                           virtual machine.
     * @param   lvmid            an integer that uniquely identifies the
     *                           target local Java virtual machine.
     * @param   mode             a string indicating the attach mode.
     * @return  ByteBuffer       a direct allocated byte buffer
     * @throws  IllegalArgumentException  The lvmid or mode was invalid.
     * @throws  IOException      An I/O error occurred while trying to acquire
     *                           the instrumentation buffer.
     * @throws  OutOfMemoryError The instrumentation buffer could not be mapped
     *                           into the virtual machine's address space.
     */
    private ByteBuffer attachImpl(String user, int lvmid, int mode)
            throws IllegalArgumentException, IOException
    {
        final ByteBuffer b = attach(user, lvmid, mode);

        if (lvmid == 0) {
            // The native instrumentation buffer for this Java virtual
            // machine is never unmapped.
            return b;
        }
        else {
            // This is an instrumentation buffer for another Java virtual
            // machine with native resources that need to be managed. We
            // create a duplicate of the native ByteBuffer and manage it
            // with a Cleaner. When the duplicate becomes phantom reachable,
            // the native resources will be released.

            final ByteBuffer dup = b.duplicate();

            CleanerFactory.cleaner()
                          .register(dup, new CleanerAction(instance, b));
            return dup;
        }
    }

    private static class CleanerAction implements Runnable {
        private final ByteBuffer bb;
        private final Perf perf;
        CleanerAction(Perf perf, ByteBuffer bb) {
            this.perf = perf;
            this.bb = bb;
        }
        public void run() {
            try {
                perf.detach(bb);
            } catch (Throwable th) {
                // avoid crashing the reference handler thread,
                // but provide for some diagnosability
                assert false : th.toString();
            }
        }
    }

    /**
     * Native method to perform the implementation specific attach mechanism.
     * <p>
     * The implementation of this method may return distinct or identical
     * <code>ByteBuffer</code> objects for two distinct calls requesting
     * attachment to the same Java virtual machine.
     * <p>
     * For the Sun HotSpot JVM, two distinct calls to attach to the same
     * target Java virtual machine will result in two distinct ByteBuffer
     * objects returned by this method. This may change in a future release.
     *
     * @param   user             A <code>String</code> object containing the
     *                           name of the user that owns the target Java
     *                           virtual machine.
     * @param   lvmid            an integer that uniquely identifies the
     *                           target local Java virtual machine.
     * @param   mode             a string indicating the attach mode.
     * @return  ByteBuffer       a direct allocated byte buffer
     * @throws  IllegalArgumentException  The lvmid or mode was invalid.
     * @throws  IOException      An I/O error occurred while trying to acquire
     *                           the instrumentation buffer.
     * @throws  OutOfMemoryError The instrumentation buffer could not be mapped
     *                           into the virtual machine's address space.
     */
    private native ByteBuffer attach(String user, int lvmid, int mode)
                   throws IllegalArgumentException, IOException;

    /**
     * Native method to perform the implementation specific detach mechanism.
     * <p>
     * If this method is passed a <code>ByteBuffer</code> object that is
     * not created by the <code>attach</code> method, then the results of
     * this method are undefined, with unpredictable and potentially damaging
     * effects to the Java virtual machine. To prevent accidental or malicious
     * use of this method, all native ByteBuffer created by the <code>
     * attach</code> method are managed internally as PhantomReferences
     * and resources are freed by the system.
     * <p>
     * If this method is passed a <code>ByteBuffer</code> object created
     * by the <code>attach</code> method with a lvmid for the Java virtual
     * machine running this method (lvmid=0, for example), then the detach
     * request is silently ignored.
     *
     * @param bb  A direct allocated byte buffer created by the
     *                    <code>attach</code> method.
     * @see   java.nio.ByteBuffer
     * @see   #attach
     */
    private native void detach(ByteBuffer bb);

    /**
     * Create a <code>long</code> scalar entry in the instrumentation buffer
     * with the given variability characteristic, units, and initial value.
     * <p>
     * Access to the instrument is provided through the returned <code>
     * ByteBuffer</code> object. Typically, this object should be wrapped
     * with <code>LongBuffer</code> view object.
     *
     * @param   variability the variability characteristic for this entry.
     * @param   units       the units for this entry.
     * @param   name        the name of this entry.
     * @param   value       the initial value for this entry.
     * @return  ByteBuffer  a direct allocated ByteBuffer object that
     *                      allows write access to a native memory location
     *                      containing a <code>long</code> value.
     *
     * see sun.misc.perf.Variability
     * see sun.misc.perf.Units
     * @see java.nio.ByteBuffer
     */
    public native ByteBuffer createLong(String name, int variability,
                                        int units, long value);

    /**
     * Create a <code>String</code> entry in the instrumentation buffer with
     * the given variability characteristic, units, and initial value.
     * <p>
     * The maximum length of the <code>String</code> stored in this string
     * instrument is given in by <code>maxLength</code> parameter. Updates
     * to this instrument with <code>String</code> values with lengths greater
     * than <code>maxLength</code> will be truncated to <code>maxLength</code>.
     * The truncated value will be terminated by a null character.
     * <p>
     * The underlying implementation may further limit the length of the
     * value, but will continue to preserve the null terminator.
     * <p>
     * Access to the instrument is provided through the returned <code>
     * ByteBuffer</code> object.
     *
     * @param   variability the variability characteristic for this entry.
     * @param   units       the units for this entry.
     * @param   name        the name of this entry.
     * @param   value       the initial value for this entry.
     * @param   maxLength   the maximum string length for this string
     *                      instrument.
     * @return  ByteBuffer  a direct allocated ByteBuffer that allows
     *                      write access to a native memory location
     *                      containing a <code>long</code> value.
     *
     * see sun.misc.perf.Variability
     * see sun.misc.perf.Units
     * @see java.nio.ByteBuffer
     */
    public ByteBuffer createString(String name, int variability,
                                   int units, String value, int maxLength)
    {
        byte[] v = value.getBytes(UTF_8.INSTANCE);
        byte[] v1 = new byte[v.length+1];
        System.arraycopy(v, 0, v1, 0, v.length);
        v1[v.length] = '\0';
        return createByteArray(name, variability, units, v1, Math.max(v1.length, maxLength));
    }

    /**
     * Create a <code>String</code> entry in the instrumentation buffer with
     * the given variability characteristic, units, and initial value.
     * <p>
     * The maximum length of the <code>String</code> stored in this string
     * instrument is implied by the length of the <code>value</code> parameter.
     * Subsequent updates to the value of this instrument will be truncated
     * to this implied maximum length. The truncated value will be terminated
     * by a null character.
     * <p>
     * The underlying implementation may further limit the length of the
     * initial or subsequent value, but will continue to preserve the null
     * terminator.
     * <p>
     * Access to the instrument is provided through the returned <code>
     * ByteBuffer</code> object.
     *
     * @param   variability the variability characteristic for this entry.
     * @param   units       the units for this entry.
     * @param   name        the name of this entry.
     * @param   value       the initial value for this entry.
     * @return  ByteBuffer  a direct allocated ByteBuffer that allows
     *                      write access to a native memory location
     *                      containing a <code>long</code> value.
     *
     * see sun.misc.perf.Variability
     * see sun.misc.perf.Units
     * @see java.nio.ByteBuffer
     */
    public ByteBuffer createString(String name, int variability,
                                   int units, String value)
    {
        byte[] v = value.getBytes(UTF_8.INSTANCE);
        byte[] v1 = new byte[v.length+1];
        System.arraycopy(v, 0, v1, 0, v.length);
        v1[v.length] = '\0';
        return createByteArray(name, variability, units, v1, v1.length);
    }

    /**
     * Create a <code>byte</code> vector entry in the instrumentation buffer
     * with the given variability characteristic, units, and initial value.
     * <p>
     * The <code>maxLength</code> parameter limits the size of the byte
     * array instrument such that the initial or subsequent updates beyond
     * this length are silently ignored. No special handling of truncated
     * updates is provided.
     * <p>
     * The underlying implementation may further limit the length of the
     * length of the initial or subsequent value.
     * <p>
     * Access to the instrument is provided through the returned <code>
     * ByteBuffer</code> object.
     *
     * @param   variability the variability characteristic for this entry.
     * @param   units       the units for this entry.
     * @param   name        the name of this entry.
     * @param   value       the initial value for this entry.
     * @param   maxLength   the maximum length of this byte array.
     * @return  ByteBuffer  a direct allocated byte buffer that allows
     *                      write access to a native memory location
     *                      containing a <code>long</code> value.
     *
     * see sun.misc.perf.Variability
     * see sun.misc.perf.Units
     * @see java.nio.ByteBuffer
     */
    public native ByteBuffer createByteArray(String name, int variability,
                                             int units, byte[] value,
                                             int maxLength);

    /**
     * Return the value of the High Resolution Counter.
     *
     * The High Resolution Counter returns the number of ticks since
     * since the start of the Java virtual machine. The resolution of
     * the counter is machine dependent and can be determined from the
     * value return by the {@link #highResFrequency} method.
     *
     * @return  the number of ticks of machine dependent resolution since
     *          the start of the Java virtual machine.
     *
     * @see #highResFrequency
     * @see java.lang.System#currentTimeMillis()
     */
    public native long highResCounter();

    /**
     * Returns the frequency of the High Resolution Counter, in ticks per
     * second.
     *
     * This value can be used to convert the value of the High Resolution
     * Counter, as returned from a call to the {@link #highResCounter} method,
     * into the number of seconds since the start of the Java virtual machine.
     *
     * @return  the frequency of the High Resolution Counter.
     * @see #highResCounter
     */
    public native long highResFrequency();

    private static native void registerNatives();

    static {
        registerNatives();
        instance = new Perf();
    }
}
