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

package java.lang.management;
import java.io.FilePermission;
import java.io.IOException;
import javax.management.DynamicMBean;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerPermission;
import javax.management.NotificationEmitter;
import javax.management.ObjectName;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.StandardEmitterMBean;
import javax.management.StandardMBean;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.management.JMX;
import sun.management.Util;
import sun.management.spi.PlatformMBeanProvider;
import sun.management.spi.PlatformMBeanProvider.PlatformComponent;

/**
 * The {@code ManagementFactory} class is a factory class for getting
 * managed beans for the Java platform.
 * This class consists of static methods each of which returns
 * one or more <i>platform MXBeans</i> representing
 * the management interface of a component of the Java virtual
 * machine.
 *
 * <h2><a id="MXBean">Platform MXBeans</a></h2>
 * <p>
 * A platform MXBean is a <i>managed bean</i> that
 * conforms to the <a href="../../../javax/management/package-summary.html">JMX</a>
 * Instrumentation Specification and only uses a set of basic data types.
 * A JMX management application and the {@linkplain
 * #getPlatformMBeanServer platform MBeanServer}
 * can interoperate without requiring classes for MXBean specific
 * data types.
 * The data types being transmitted between the JMX connector
 * server and the connector client are
 * {@linkplain javax.management.openmbean.OpenType open types}
 * and this allows interoperation across versions.
 * See <a href="../../../javax/management/MXBean.html#MXBean-spec">
 * the specification of MXBeans</a> for details.
 *
 * <a id="MXBeanNames"></a>
 * <p>Each platform MXBean is a {@link PlatformManagedObject}
 * and it has a unique
 * {@link javax.management.ObjectName ObjectName} for
 * registration in the platform {@code MBeanServer} as returned by
 * by the {@link PlatformManagedObject#getObjectName getObjectName}
 * method.
 *
 * <p>
 * An application can access a platform MXBean in the following ways:
 * <h3>1. Direct access to an MXBean interface</h3>
 * <blockquote>
 * <ul>
 *     <li>Get an MXBean instance by calling the
 *         {@link #getPlatformMXBean(Class) getPlatformMXBean} or
 *         {@link #getPlatformMXBeans(Class) getPlatformMXBeans} method
 *         and access the MXBean locally in the running
 *         virtual machine.
 *         </li>
 *     <li>Construct an MXBean proxy instance that forwards the
 *         method calls to a given {@link MBeanServer MBeanServer} by calling
 *         the {@link #getPlatformMXBean(MBeanServerConnection, Class)} or
 *         {@link #getPlatformMXBeans(MBeanServerConnection, Class)} method.
 *         The {@link #newPlatformMXBeanProxy newPlatformMXBeanProxy} method
 *         can also be used to construct an MXBean proxy instance of
 *         a given {@code ObjectName}.
 *         A proxy is typically constructed to remotely access
 *         an MXBean of another running virtual machine.
 *         </li>
 * </ul>
 * <h3>2. Indirect access to an MXBean interface via MBeanServer</h3>
 * <ul>
 *     <li>Go through the platform {@code MBeanServer} to access MXBeans
 *         locally or a specific {@code MBeanServerConnection} to access
 *         MXBeans remotely.
 *         The attributes and operations of an MXBean use only
 *         <em>JMX open types</em> which include basic data types,
 *         {@link javax.management.openmbean.CompositeData CompositeData},
 *         and {@link javax.management.openmbean.TabularData TabularData}
 *         defined in
 *         {@link javax.management.openmbean.OpenType OpenType}.
 *         The mapping is specified in
 *         the {@linkplain javax.management.MXBean MXBean} specification
 *         for details.
 *        </li>
 * </ul>
 * </blockquote>
 *
 * <p>
 * The {@link #getPlatformManagementInterfaces getPlatformManagementInterfaces}
 * method returns all management interfaces supported in the Java virtual machine
 * including the standard management interfaces listed in the tables
 * below as well as the management interfaces extended by the JDK implementation.
 * <p>
 * A Java virtual machine has a single instance of the following management
 * interfaces:
 *
 * <table class="striped" style="margin-left:2em">
 * <caption style="display:none">The list of Management Interfaces and their single instances</caption>
 * <thead>
 * <tr>
 * <th scope="col">Management Interface</th>
 * <th scope="col">ObjectName</th>
 * </tr>
 * </thead>
 * <tbody style="text-align:left;">
 * <tr>
 * <th scope="row"> {@link ClassLoadingMXBean} </th>
 * <td> {@link #CLASS_LOADING_MXBEAN_NAME
 *             java.lang:type=ClassLoading}</td>
 * </tr>
 * <tr>
 * <th scope="row"> {@link MemoryMXBean} </th>
 * <td> {@link #MEMORY_MXBEAN_NAME
 *             java.lang:type=Memory}</td>
 * </tr>
 * <tr>
 * <th scope="row"> {@link ThreadMXBean} </th>
 * <td> {@link #THREAD_MXBEAN_NAME
 *             java.lang:type=Threading}</td>
 * </tr>
 * <tr>
 * <th scope="row"> {@link RuntimeMXBean} </th>
 * <td> {@link #RUNTIME_MXBEAN_NAME
 *             java.lang:type=Runtime}</td>
 * </tr>
 * <tr>
 * <th scope="row"> {@link OperatingSystemMXBean} </th>
 * <td> {@link #OPERATING_SYSTEM_MXBEAN_NAME
 *             java.lang:type=OperatingSystem}</td>
 * </tr>
 * <tr>
 * <th scope="row"> {@link PlatformLoggingMXBean} </th>
 * <td> {@link java.util.logging.LogManager#LOGGING_MXBEAN_NAME
 *             java.util.logging:type=Logging}</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * <p>
 * A Java virtual machine has zero or a single instance of
 * the following management interfaces.
 *
 * <table class="striped" style="margin-left:2em">
 * <caption style="display:none">The list of Management Interfaces and their single instances</caption>
 * <thead>
 * <tr>
 * <th scope="col">Management Interface</th>
 * <th scope="col">ObjectName</th>
 * </tr>
 * </thead>
 * <tbody style="text-align:left;">
 * <tr>
 * <th scope="row"> {@link CompilationMXBean} </th>
 * <td> {@link #COMPILATION_MXBEAN_NAME
 *             java.lang:type=Compilation}</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * <p>
 * A Java virtual machine may have one or more instances of the following
 * management interfaces.
 * <table class="striped" style="margin-left:2em">
 * <caption style="display:none">The list of Management Interfaces and their single instances</caption>
 * <thead>
 * <tr>
 * <th scope="col">Management Interface</th>
 * <th scope="col">ObjectName</th>
 * </tr>
 * </thead>
 * <tbody style="text-align:left;">
 * <tr>
 * <th scope="row"> {@link GarbageCollectorMXBean} </th>
 * <td> {@link #GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE
 *             java.lang:type=GarbageCollector}{@code ,name=}<i>collector's name</i></td>
 * </tr>
 * <tr>
 * <th scope="row"> {@link MemoryManagerMXBean} </th>
 * <td> {@link #MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE
 *             java.lang:type=MemoryManager}{@code ,name=}<i>manager's name</i></td>
 * </tr>
 * <tr>
 * <th scope="row"> {@link MemoryPoolMXBean} </th>
 * <td> {@link #MEMORY_POOL_MXBEAN_DOMAIN_TYPE
 *             java.lang:type=MemoryPool}{@code ,name=}<i>pool's name</i></td>
 * </tr>
 * <tr>
 * <th scope="row"> {@link BufferPoolMXBean} </th>
 * <td> {@code java.nio:type=BufferPool,name=}<i>pool name</i></td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @see <a href="../../../javax/management/package-summary.html">
 *      JMX Specification</a>
 * @see <a href="package-summary.html#examples">
 *      Ways to Access Management Metrics</a>
 * @see javax.management.MXBean
 *
 * @author  Mandy Chung
 * @since   1.5
 */
@SuppressWarnings("removal")
public class ManagementFactory {
    // A class with only static fields and methods.
    private ManagementFactory() {};

    /**
     * String representation of the
     * {@code ObjectName} for the {@link ClassLoadingMXBean}.
     */
    public static final String CLASS_LOADING_MXBEAN_NAME =
        "java.lang:type=ClassLoading";

    /**
     * String representation of the
     * {@code ObjectName} for the {@link CompilationMXBean}.
     */
    public static final String COMPILATION_MXBEAN_NAME =
        "java.lang:type=Compilation";

    /**
     * String representation of the
     * {@code ObjectName} for the {@link MemoryMXBean}.
     */
    public static final String MEMORY_MXBEAN_NAME =
        "java.lang:type=Memory";

    /**
     * String representation of the
     * {@code ObjectName} for the {@link OperatingSystemMXBean}.
     */
    public static final String OPERATING_SYSTEM_MXBEAN_NAME =
        "java.lang:type=OperatingSystem";

    /**
     * String representation of the
     * {@code ObjectName} for the {@link RuntimeMXBean}.
     */
    public static final String RUNTIME_MXBEAN_NAME =
        "java.lang:type=Runtime";

    /**
     * String representation of the
     * {@code ObjectName} for the {@link ThreadMXBean}.
     */
    public static final String THREAD_MXBEAN_NAME =
        "java.lang:type=Threading";

    /**
     * The domain name and the type key property in
     * the {@code ObjectName} for a {@link GarbageCollectorMXBean}.
     * The unique {@code ObjectName} for a {@code GarbageCollectorMXBean}
     * can be formed by appending this string with
     * "{@code ,name=}<i>collector's name</i>".
     */
    public static final String GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE =
        "java.lang:type=GarbageCollector";

    /**
     * The domain name and the type key property in
     * the {@code ObjectName} for a {@link MemoryManagerMXBean}.
     * The unique {@code ObjectName} for a {@code MemoryManagerMXBean}
     * can be formed by appending this string with
     * "{@code ,name=}<i>manager's name</i>".
     */
    public static final String MEMORY_MANAGER_MXBEAN_DOMAIN_TYPE=
        "java.lang:type=MemoryManager";

    /**
     * The domain name and the type key property in
     * the {@code ObjectName} for a {@link MemoryPoolMXBean}.
     * The unique {@code ObjectName} for a {@code MemoryPoolMXBean}
     * can be formed by appending this string with
     * {@code ,name=}<i>pool's name</i>.
     */
    public static final String MEMORY_POOL_MXBEAN_DOMAIN_TYPE=
        "java.lang:type=MemoryPool";

    /**
     * Returns the managed bean for the class loading system of
     * the Java virtual machine.
     *
     * @return a {@link ClassLoadingMXBean} object for
     * the Java virtual machine.
     */
    public static ClassLoadingMXBean getClassLoadingMXBean() {
        return getPlatformMXBean(ClassLoadingMXBean.class);
    }

    /**
     * Returns the managed bean for the memory system of
     * the Java virtual machine.
     *
     * @return a {@link MemoryMXBean} object for the Java virtual machine.
     */
    public static MemoryMXBean getMemoryMXBean() {
        return getPlatformMXBean(MemoryMXBean.class);
    }

    /**
     * Returns the managed bean for the thread system of
     * the Java virtual machine.
     *
     * @return a {@link ThreadMXBean} object for the Java virtual machine.
     */
    public static ThreadMXBean getThreadMXBean() {
        return getPlatformMXBean(ThreadMXBean.class);
    }

    /**
     * Returns the managed bean for the runtime system of
     * the Java virtual machine.
     *
     * @return a {@link RuntimeMXBean} object for the Java virtual machine.

     */
    public static RuntimeMXBean getRuntimeMXBean() {
        return getPlatformMXBean(RuntimeMXBean.class);
    }

    /**
     * Returns the managed bean for the compilation system of
     * the Java virtual machine.  This method returns {@code null}
     * if the Java virtual machine has no compilation system.
     *
     * @return a {@link CompilationMXBean} object for the Java virtual
     *   machine or {@code null} if the Java virtual machine has
     *   no compilation system.
     */
    public static CompilationMXBean getCompilationMXBean() {
        return getPlatformMXBean(CompilationMXBean.class);
    }

    /**
     * Returns the managed bean for the operating system on which
     * the Java virtual machine is running.
     *
     * @return an {@link OperatingSystemMXBean} object for
     * the Java virtual machine.
     */
    public static OperatingSystemMXBean getOperatingSystemMXBean() {
        return getPlatformMXBean(OperatingSystemMXBean.class);
    }

    /**
     * Returns a list of {@link MemoryPoolMXBean} objects in the
     * Java virtual machine.
     * The Java virtual machine can have one or more memory pools.
     * It may add or remove memory pools during execution.
     *
     * @return a list of {@code MemoryPoolMXBean} objects.
     *
     */
    public static List<MemoryPoolMXBean> getMemoryPoolMXBeans() {
        return getPlatformMXBeans(MemoryPoolMXBean.class);
    }

    /**
     * Returns a list of {@link MemoryManagerMXBean} objects
     * in the Java virtual machine.
     * The Java virtual machine can have one or more memory managers.
     * It may add or remove memory managers during execution.
     *
     * @return a list of {@code MemoryManagerMXBean} objects.
     *
     */
    public static List<MemoryManagerMXBean> getMemoryManagerMXBeans() {
        return getPlatformMXBeans(MemoryManagerMXBean.class);
    }


    /**
     * Returns a list of {@link GarbageCollectorMXBean} objects
     * in the Java virtual machine.
     * The Java virtual machine may have one or more
     * {@code GarbageCollectorMXBean} objects.
     * It may add or remove {@code GarbageCollectorMXBean}
     * during execution.
     *
     * @return a list of {@code GarbageCollectorMXBean} objects.
     *
     */
    public static List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() {
        return getPlatformMXBeans(GarbageCollectorMXBean.class);
    }

    private static MBeanServer platformMBeanServer;
    /**
     * Returns the platform {@link javax.management.MBeanServer MBeanServer}.
     * On the first call to this method, it first creates the platform
     * {@code MBeanServer} by calling the
     * {@link javax.management.MBeanServerFactory#createMBeanServer
     * MBeanServerFactory.createMBeanServer}
     * method and registers each platform MXBean in this platform
     * {@code MBeanServer} with its
     * {@link PlatformManagedObject#getObjectName ObjectName}.
     * This method, in subsequent calls, will simply return the
     * initially created platform {@code MBeanServer}.
     * <p>
     * MXBeans that get created and destroyed dynamically, for example,
     * memory {@link MemoryPoolMXBean pools} and
     * {@link MemoryManagerMXBean managers},
     * will automatically be registered and deregistered into the platform
     * {@code MBeanServer}.
     * <p>
     * If the system property {@code javax.management.builder.initial}
     * is set, the platform {@code MBeanServer} creation will be done
     * by the specified {@link javax.management.MBeanServerBuilder}.
     * <p>
     * It is recommended that this platform MBeanServer also be used
     * to register other application managed beans
     * besides the platform MXBeans.
     * This will allow all MBeans to be published through the same
     * {@code MBeanServer} and hence allow for easier network publishing
     * and discovery.
     * Name conflicts with the platform MXBeans should be avoided.
     *
     * @return the platform {@code MBeanServer}; the platform
     *         MXBeans are registered into the platform {@code MBeanServer}
     *         at the first time this method is called.
     *
     * @throws SecurityException if there is a security manager
     * and the caller does not have the permission required by
     * {@link javax.management.MBeanServerFactory#createMBeanServer}.
     *
     * @see javax.management.MBeanServerFactory
     * @see javax.management.MBeanServerFactory#createMBeanServer
     */
    public static synchronized MBeanServer getPlatformMBeanServer() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            Permission perm = new MBeanServerPermission("createMBeanServer");
            sm.checkPermission(perm);
        }

        if (platformMBeanServer == null) {
            platformMBeanServer = MBeanServerFactory.createMBeanServer();
            platformComponents()
                    .stream()
                    .filter(PlatformComponent::shouldRegister)
                    .flatMap(pc -> pc.nameToMBeanMap().entrySet().stream())
                    .forEach(entry -> addMXBean(platformMBeanServer, entry.getKey(), entry.getValue()));
        }
        return platformMBeanServer;
    }

    /**
     * Returns a proxy for a platform MXBean interface of a
     * given <a href="#MXBeanNames">MXBean name</a>
     * that forwards its method calls through the given
     * {@code MBeanServerConnection}.
     *
     * <p>This method is equivalent to:
     * <blockquote>
     * {@link java.lang.reflect.Proxy#newProxyInstance
     *        Proxy.newProxyInstance}{@code (mxbeanInterface.getClassLoader(),
     *        new Class[] { mxbeanInterface }, handler)}
     * </blockquote>
     *
     * where {@code handler} is an {@link java.lang.reflect.InvocationHandler
     * InvocationHandler} to which method invocations to the MXBean interface
     * are dispatched. This {@code handler} converts an input parameter
     * from an MXBean data type to its mapped open type before forwarding
     * to the {@code MBeanServer} and converts a return value from
     * an MXBean method call through the {@code MBeanServer}
     * from an open type to the corresponding return type declared in
     * the MXBean interface.
     *
     * <p>
     * If the MXBean is a notification emitter (i.e.,
     * it implements
     * {@link javax.management.NotificationEmitter NotificationEmitter}),
     * both the {@code mxbeanInterface} and {@code NotificationEmitter}
     * will be implemented by this proxy.
     *
     * <p>
     * <b>Notes:</b>
     * <ol>
     * <li>Using an MXBean proxy is a convenience remote access to
     * a platform MXBean of a running virtual machine.  All method
     * calls to the MXBean proxy are forwarded to an
     * {@code MBeanServerConnection} where
     * {@link java.io.IOException IOException} may be thrown
     * when the communication problem occurs with the connector server.
     * If thrown, {@link java.io.IOException IOException} will be wrappped in
     * {@link java.lang.reflect.UndeclaredThrowableException UndeclaredThrowableException}.
     * An application remotely accessing the platform MXBeans using
     * proxy should prepare to catch {@code UndeclaredThrowableException} and
     * handle its {@linkplain java.lang.reflect.UndeclaredThrowableException#getCause() cause}
     * as if that cause had been thrown by the {@code MBeanServerConnection}
     * interface.</li>
     *
     * <li>When a client application is designed to remotely access MXBeans
     * for a running virtual machine whose version is different than
     * the version on which the application is running,
     * it should prepare to catch
     * {@link java.io.InvalidObjectException InvalidObjectException}
     * which is thrown when an MXBean proxy receives a name of an
     * enum constant which is missing in the enum class loaded in
     * the client application.   If thrown,
     * {@link java.io.InvalidObjectException InvalidObjectException} will be
     * wrappped in
     * {@link java.lang.reflect.UndeclaredThrowableException UndeclaredThrowableException}.
     * </li>
     *
     * <li>{@link javax.management.MBeanServerInvocationHandler
     * MBeanServerInvocationHandler} or its
     * {@link javax.management.MBeanServerInvocationHandler#newProxyInstance
     * newProxyInstance} method cannot be used to create
     * a proxy for a platform MXBean. The proxy object created
     * by {@code MBeanServerInvocationHandler} does not handle
     * the properties of the platform MXBeans described in
     * the <a href="#MXBean">class specification</a>.
     *</li>
     * </ol>
     *
     * @param connection the {@code MBeanServerConnection} to forward to.
     * @param mxbeanName the name of a platform MXBean within
     * {@code connection} to forward to. {@code mxbeanName} must be
     * in the format of {@link ObjectName ObjectName}.
     * @param mxbeanInterface the MXBean interface to be implemented
     * by the proxy.
     * @param <T> an {@code mxbeanInterface} type parameter
     *
     * @return a proxy for a platform MXBean interface of a
     * given <a href="#MXBeanNames">MXBean name</a>
     * that forwards its method calls through the given
     * {@code MBeanServerConnection}, or {@code null} if not exist.
     *
     * @throws IllegalArgumentException if
     * <ul>
     * <li>{@code mxbeanName} is not with a valid
     *     {@link ObjectName ObjectName} format, or</li>
     * <li>the named MXBean in the {@code connection} is
     *     not a MXBean provided by the platform, or</li>
     * <li>the named MXBean is not registered in the
     *     {@code MBeanServerConnection}, or</li>
     * <li>the named MXBean is not an instance of the given
     *     {@code mxbeanInterface}</li>
     * </ul>
     *
     * @throws java.io.IOException if a communication problem
     * occurred when accessing the {@code MBeanServerConnection}.
     */
    public static <T> T
        newPlatformMXBeanProxy(MBeanServerConnection connection,
                               String mxbeanName,
                               Class<T> mxbeanInterface)
            throws java.io.IOException {

        // Only allow MXBean interfaces from the platform modules loaded by the
        // bootstrap or platform class loader
        final Class<?> cls = mxbeanInterface;
        @SuppressWarnings("removal")
        ClassLoader loader =
            AccessController.doPrivileged(
                (PrivilegedAction<ClassLoader>) () -> cls.getClassLoader());
        if (!jdk.internal.misc.VM.isSystemDomainLoader(loader)) {
            throw new IllegalArgumentException(mxbeanName +
                " is not a platform MXBean");
        }

        try {
            final ObjectName objName = new ObjectName(mxbeanName);
            String intfName = mxbeanInterface.getName();
            if (!isInstanceOf(connection, objName, intfName)) {
                throw new IllegalArgumentException(mxbeanName +
                    " is not an instance of " + mxbeanInterface);
            }

            // check if the registered MBean is a notification emitter
            boolean emitter = connection.isInstanceOf(objName, NOTIF_EMITTER);

            // create an MXBean proxy
            return JMX.newMXBeanProxy(connection, objName, mxbeanInterface,
                                      emitter);
        } catch (InstanceNotFoundException|MalformedObjectNameException e) {
            throw new IllegalArgumentException(e);
        }
    }

    // This makes it possible to obtain an instance of LoggingMXBean
    // using newPlatformMXBeanProxy(mbs, on, LoggingMXBean.class)
    // even though the underlying MXBean no longer implements
    // java.util.logging.LoggingMXBean.
    // Altough java.util.logging.LoggingMXBean is deprecated, an application
    // that uses newPlatformMXBeanProxy(mbs, on, LoggingMXBean.class) will
    // continue to work.
    //
    private static boolean isInstanceOf(MBeanServerConnection connection,
            ObjectName objName, String intfName)
            throws InstanceNotFoundException, IOException
    {
        // special case for java.util.logging.LoggingMXBean.
        // java.util.logging.LoggingMXBean is deprecated and
        // replaced with java.lang.management.PlatformLoggingMXBean,
        // so we will consider that any MBean implementing
        // java.lang.management.PlatformLoggingMXBean also implements
        // java.util.logging.LoggingMXBean.
        if ("java.util.logging.LoggingMXBean".equals(intfName)) {
            if (connection.isInstanceOf(objName,
                    PlatformLoggingMXBean.class.getName())) {
                return true;
            }
        }
        return connection.isInstanceOf(objName, intfName);
    }

    /**
     * Returns the platform MXBean implementing
     * the given {@code mxbeanInterface} which is specified
     * to have one single instance in the Java virtual machine.
     * This method may return {@code null} if the management interface
     * is not implemented in the Java virtual machine (for example,
     * a Java virtual machine with no compilation system does not
     * implement {@link CompilationMXBean});
     * otherwise, this method is equivalent to calling:
     * <pre>
     *    {@link #getPlatformMXBeans(Class)
     *      getPlatformMXBeans(mxbeanInterface)}.get(0);
     * </pre>
     *
     * @param mxbeanInterface a management interface for a platform
     *     MXBean with one single instance in the Java virtual machine
     *     if implemented.
     * @param <T> an {@code mxbeanInterface} type parameter
     *
     * @return the platform MXBean that implements
     * {@code mxbeanInterface}, or {@code null} if not exist.
     *
     * @throws IllegalArgumentException if {@code mxbeanInterface}
     * is not a platform management interface or
     * not a singleton platform MXBean.
     *
     * @since 1.7
     */
    public static <T extends PlatformManagedObject>
            T getPlatformMXBean(Class<T> mxbeanInterface) {
        PlatformComponent<?> pc = PlatformMBeanFinder.findSingleton(mxbeanInterface);

        List<? extends T> mbeans = pc.getMBeans(mxbeanInterface);
        assert mbeans.isEmpty() || mbeans.size() == 1;
        return mbeans.isEmpty() ? null : mbeans.get(0);
    }

    /**
     * Returns the list of platform MXBeans implementing
     * the given {@code mxbeanInterface} in the Java
     * virtual machine.
     * The returned list may contain zero, one, or more instances.
     * The number of instances in the returned list is defined
     * in the specification of the given management interface.
     * The order is undefined and there is no guarantee that
     * the list returned is in the same order as previous invocations.
     *
     * @param mxbeanInterface a management interface for a platform
     *                        MXBean
     * @param <T> an {@code mxbeanInterface} type parameter
     *
     * @return the list of platform MXBeans that implement
     * {@code mxbeanInterface}.
     *
     * @throws IllegalArgumentException if {@code mxbeanInterface}
     * is not a platform management interface.
     *
     * @since 1.7
     */
    public static <T extends PlatformManagedObject> List<T>
            getPlatformMXBeans(Class<T> mxbeanInterface) {
        // Validates at first the specified interface by finding at least one
        // PlatformComponent whose MXBean implements this interface.
        // An interface can be implemented by different MBeans, provided by
        // different platform components.
        PlatformComponent<?> pc = PlatformMBeanFinder.findFirst(mxbeanInterface);
        if (pc == null) {
            throw new IllegalArgumentException(mxbeanInterface.getName()
                    + " is not a platform management interface");
        }

        return platformComponents().stream()
                .flatMap(p -> p.getMBeans(mxbeanInterface).stream())
                .collect(Collectors.toList());
    }

    /**
     * Returns the platform MXBean proxy for
     * {@code mxbeanInterface} which is specified to have one single
     * instance in a Java virtual machine and the proxy will
     * forward the method calls through the given {@code MBeanServerConnection}.
     * This method may return {@code null} if the management interface
     * is not implemented in the Java virtual machine being monitored
     * (for example, a Java virtual machine with no compilation system
     * does not implement {@link CompilationMXBean});
     * otherwise, this method is equivalent to calling:
     * <pre>
     *     {@link #getPlatformMXBeans(MBeanServerConnection, Class)
     *        getPlatformMXBeans(connection, mxbeanInterface)}.get(0);
     * </pre>
     *
     * @param connection the {@code MBeanServerConnection} to forward to.
     * @param mxbeanInterface a management interface for a platform
     *     MXBean with one single instance in the Java virtual machine
     *     being monitored, if implemented.
     * @param <T> an {@code mxbeanInterface} type parameter
     *
     * @return the platform MXBean proxy for
     * forwarding the method calls of the {@code mxbeanInterface}
     * through the given {@code MBeanServerConnection},
     * or {@code null} if not exist.
     *
     * @throws IllegalArgumentException if {@code mxbeanInterface}
     * is not a platform management interface or
     * not a singleton platform MXBean.
     * @throws java.io.IOException if a communication problem
     * occurred when accessing the {@code MBeanServerConnection}.
     *
     * @see #newPlatformMXBeanProxy
     * @since 1.7
     */
    public static <T extends PlatformManagedObject>
            T getPlatformMXBean(MBeanServerConnection connection,
                                Class<T> mxbeanInterface)
        throws java.io.IOException
    {
        PlatformComponent<?> pc = PlatformMBeanFinder.findSingleton(mxbeanInterface);
        return newPlatformMXBeanProxy(connection, pc.getObjectNamePattern(), mxbeanInterface);
    }

    /**
     * Returns the list of the platform MXBean proxies for
     * forwarding the method calls of the {@code mxbeanInterface}
     * through the given {@code MBeanServerConnection}.
     * The returned list may contain zero, one, or more instances.
     * The number of instances in the returned list is defined
     * in the specification of the given management interface.
     * The order is undefined and there is no guarantee that
     * the list returned is in the same order as previous invocations.
     *
     * @param connection the {@code MBeanServerConnection} to forward to.
     * @param mxbeanInterface a management interface for a platform
     *                        MXBean
     * @param <T> an {@code mxbeanInterface} type parameter
     *
     * @return the list of platform MXBean proxies for
     * forwarding the method calls of the {@code mxbeanInterface}
     * through the given {@code MBeanServerConnection}.
     *
     * @throws IllegalArgumentException if {@code mxbeanInterface}
     * is not a platform management interface.
     *
     * @throws java.io.IOException if a communication problem
     * occurred when accessing the {@code MBeanServerConnection}.
     *
     * @see #newPlatformMXBeanProxy
     * @since 1.7
     */
    public static <T extends PlatformManagedObject>
            List<T> getPlatformMXBeans(MBeanServerConnection connection,
                                       Class<T> mxbeanInterface)
        throws java.io.IOException
    {
        // Validates at first the specified interface by finding at least one
        // PlatformComponent whose MXBean implements this interface.
        // An interface can be implemented by different MBeans, provided by
        // different platform components.
        PlatformComponent<?> pc = PlatformMBeanFinder.findFirst(mxbeanInterface);
        if (pc == null) {
            throw new IllegalArgumentException(mxbeanInterface.getName()
                    + " is not a platform management interface");
        }

        // Collect all names, eliminate duplicates.
        Stream<String> names = Stream.empty();
        for (PlatformComponent<?> p : platformComponents()) {
            names = Stream.concat(names, getProxyNames(p, connection, mxbeanInterface));
        }
        Set<String> objectNames = names.collect(Collectors.toSet());
        if (objectNames.isEmpty()) return Collections.emptyList();

        // Map names on proxies.
        List<T> proxies = new ArrayList<>();
        for (String name : objectNames) {
            proxies.add(newPlatformMXBeanProxy(connection, name, mxbeanInterface));
        }
        return proxies;
    }

    // Returns a stream containing all ObjectNames of the MBeans represented by
    // the specified PlatformComponent and implementing the specified interface.
    // If the PlatformComponent is a singleton, the name returned by
    // PlatformComponent.getObjectNamePattern() will be used, otherwise
    // we will query the specified MBeanServerConnection (conn.queryNames)
    // with the pattern returned by PlatformComponent.getObjectNamePattern()
    // in order to find the names of matching MBeans.
    // In case of singleton, we do not check whether the MBean is registered
    // in the connection because the caller "getPlatformMXBeans" will do the check
    // when creating a proxy.
    private static Stream<String> getProxyNames(PlatformComponent<?> pc,
                                                MBeanServerConnection conn,
                                                Class<?> intf)
            throws IOException
    {
        if (pc.mbeanInterfaceNames().contains(intf.getName())) {
            if (pc.isSingleton()) {
                return Stream.of(pc.getObjectNamePattern());
            } else {
                return conn.queryNames(Util.newObjectName(pc.getObjectNamePattern()), null)
                        .stream().map(ObjectName::getCanonicalName);
            }
        }
        return Stream.empty();
    }

    /**
     * Returns the set of {@code Class} objects, subinterface of
     * {@link PlatformManagedObject}, representing
     * all management interfaces for
     * monitoring and managing the Java platform.
     *
     * @return the set of {@code Class} objects, subinterface of
     * {@link PlatformManagedObject} representing
     * the management interfaces for
     * monitoring and managing the Java platform.
     *
     * @since 1.7
     */
    public static Set<Class<? extends PlatformManagedObject>>
           getPlatformManagementInterfaces()
    {
        // local variable required here; see JDK-8223553
        Stream<Class<? extends PlatformManagedObject>> pmos = platformComponents()
                .stream()
                .flatMap(pc -> pc.mbeanInterfaces().stream())
                .filter(clazz -> PlatformManagedObject.class.isAssignableFrom(clazz))
                .map(clazz -> clazz.asSubclass(PlatformManagedObject.class));
        return pmos.collect(Collectors.toSet());
    }

    private static final String NOTIF_EMITTER =
        "javax.management.NotificationEmitter";

    @SuppressWarnings("removal")
    private static void addMXBean(final MBeanServer mbs, String name, final Object pmo)
    {
        try {
            ObjectName oname = ObjectName.getInstance(name);
            // Make DynamicMBean out of MXBean by wrapping it with a StandardMBean
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                final DynamicMBean dmbean;
                if (pmo instanceof DynamicMBean) {
                    dmbean = DynamicMBean.class.cast(pmo);
                } else if (pmo instanceof NotificationEmitter) {
                    dmbean = new StandardEmitterMBean(pmo, null, true, (NotificationEmitter) pmo);
                } else {
                    dmbean = new StandardMBean(pmo, null, true);
                }

                mbs.registerMBean(dmbean, oname);
                return null;
            });
        } catch (MalformedObjectNameException mone) {
            throw new IllegalArgumentException(mone);
        } catch (PrivilegedActionException e) {
            throw new RuntimeException(e.getException());
        }
    }

    private static Collection<PlatformComponent<?>> platformComponents()
    {
        return PlatformMBeanFinder.getMap().values();
    }

    private static class PlatformMBeanFinder {
        private static final Map<String, PlatformComponent<?>> componentMap;

        static {
            // get all providers
            @SuppressWarnings("removal")
            List<PlatformMBeanProvider> providers = AccessController.doPrivileged(
                new PrivilegedAction<List<PlatformMBeanProvider>>() {
                    @Override
                    public List<PlatformMBeanProvider> run() {
                        List<PlatformMBeanProvider> all = new ArrayList<>();
                        for (PlatformMBeanProvider provider : ServiceLoader.loadInstalled(PlatformMBeanProvider.class)) {
                            all.add(provider);
                        }
                        all.add(new DefaultPlatformMBeanProvider());
                        return all;
                    }
                }, null, new FilePermission("<<ALL FILES>>", "read"),
                new RuntimePermission("sun.management.spi.PlatformMBeanProvider.subclass"));

            // load all platform components into a map
            var map = new HashMap<String, PlatformComponent<?>>();
            for (PlatformMBeanProvider provider : providers) {
                // For each provider, ensure that two different components are not declared
                // with the same object name pattern.
                var names = new HashSet<String>();
                for (PlatformComponent<?> component : provider.getPlatformComponentList()) {
                    String name = component.getObjectNamePattern();
                    if (!names.add(name)) {
                        throw new InternalError(name +
                                " has been used as key by this provider" +
                                ", it cannot be reused for " + component);
                    }
                    // The first one wins if multiple PlatformComponents defined by
                    // different providers use the same ObjectName pattern
                    map.putIfAbsent(name, component);
                }
            }
            componentMap = map;
        }

        static Map<String, PlatformComponent<?>> getMap() {
            return componentMap;
        }

        // Finds the first PlatformComponent whose mbeanInterfaceNames() list
        // contains the specified class name. An MBean interface can be implemented
        // by different MBeans, provided by different platform components.
        // For instance the MemoryManagerMXBean interface is implemented both by
        // regular memory managers, and garbage collector MXBeans. This method is
        // mainly used to verify that there is at least one PlatformComponent
        // which provides an implementation of the desired interface.
        static PlatformComponent<?> findFirst(Class<?> mbeanIntf)
        {
            String name = mbeanIntf.getName();
            Optional<PlatformComponent<?>> op = getMap().values()
                .stream()
                .filter(pc -> pc.mbeanInterfaceNames().contains(name))
                .findFirst();

            if (op.isPresent()) {
                return op.get();
            } else {
                return null;
            }
        }

        // Finds a PlatformComponent whose mbeanInterface name list contains
        // the specified class name, and make sure that one and only one exists.
        static PlatformComponent<?> findSingleton(Class<?> mbeanIntf)
        {
            String name = mbeanIntf.getName();
            Optional<PlatformComponent<?>> op = getMap().values()
                .stream()
                .filter(pc -> pc.mbeanInterfaceNames().contains(name))
                .reduce((p1, p2) -> {
                    if (p2 != null) {
                        throw new IllegalArgumentException(mbeanIntf.getName() +
                            " can have more than one instance");
                    } else {
                        return p1;
                    }
                });

            PlatformComponent<?> singleton = op.isPresent() ? op.get() : null;
            if (singleton == null) {
                throw new IllegalArgumentException(mbeanIntf.getName() +
                    " is not a platform management interface");
            }
            if (!singleton.isSingleton()) {
                throw new IllegalArgumentException(mbeanIntf.getName() +
                    " can have more than one instance");
            }
            return singleton;
        }
    }

    static {
        loadNativeLib();
    }

    @SuppressWarnings("removal")
    private static void loadNativeLib() {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            System.loadLibrary("management");
            return null;
        });
    }
}
