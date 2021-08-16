/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javax.management;

import com.sun.jmx.mbeanserver.Introspector;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import sun.reflect.misc.ReflectUtil;

/**
 * Static methods from the JMX API.  There are no instances of this class.
 *
 * @since 1.6
 */
public class JMX {
    /* Code within this package can prove that by providing this instance of
     * this class.
     */
    static final JMX proof = new JMX();

    private JMX() {}

    /**
     * The name of the <a href="Descriptor.html#defaultValue">{@code
     * defaultValue}</a> field.
     */
    public static final String DEFAULT_VALUE_FIELD = "defaultValue";

    /**
     * The name of the <a href="Descriptor.html#immutableInfo">{@code
     * immutableInfo}</a> field.
     */
    public static final String IMMUTABLE_INFO_FIELD = "immutableInfo";

    /**
     * The name of the <a href="Descriptor.html#interfaceClassName">{@code
     * interfaceClassName}</a> field.
     */
    public static final String INTERFACE_CLASS_NAME_FIELD = "interfaceClassName";

    /**
     * The name of the <a href="Descriptor.html#legalValues">{@code
     * legalValues}</a> field.
     */
    public static final String LEGAL_VALUES_FIELD = "legalValues";

    /**
     * The name of the <a href="Descriptor.html#maxValue">{@code
     * maxValue}</a> field.
     */
    public static final String MAX_VALUE_FIELD = "maxValue";

    /**
     * The name of the <a href="Descriptor.html#minValue">{@code
     * minValue}</a> field.
     */
    public static final String MIN_VALUE_FIELD = "minValue";

    /**
     * The name of the <a href="Descriptor.html#mxbean">{@code
     * mxbean}</a> field.
     */
    public static final String MXBEAN_FIELD = "mxbean";

    /**
     * The name of the <a href="Descriptor.html#openType">{@code
     * openType}</a> field.
     */
    public static final String OPEN_TYPE_FIELD = "openType";

    /**
     * The name of the <a href="Descriptor.html#originalType">{@code
     * originalType}</a> field.
     */
    public static final String ORIGINAL_TYPE_FIELD = "originalType";

    /**
     * <p>Make a proxy for a Standard MBean in a local or remote
     * MBean Server.</p>
     *
     * <p>If you have an MBean Server {@code mbs} containing an MBean
     * with {@link ObjectName} {@code name}, and if the MBean's
     * management interface is described by the Java interface
     * {@code MyMBean}, you can construct a proxy for the MBean like
     * this:</p>
     *
     * <pre>
     * MyMBean proxy = JMX.newMBeanProxy(mbs, name, MyMBean.class);
     * </pre>
     *
     * <p>Suppose, for example, {@code MyMBean} looks like this:</p>
     *
     * <pre>
     * public interface MyMBean {
     *     public String getSomeAttribute();
     *     public void setSomeAttribute(String value);
     *     public void someOperation(String param1, int param2);
     * }
     * </pre>
     *
     * <p>Then you can execute:</p>
     *
     * <ul>
     *
     * <li>{@code proxy.getSomeAttribute()} which will result in a
     * call to {@code mbs.}{@link MBeanServerConnection#getAttribute
     * getAttribute}{@code (name, "SomeAttribute")}.
     *
     * <li>{@code proxy.setSomeAttribute("whatever")} which will result
     * in a call to {@code mbs.}{@link MBeanServerConnection#setAttribute
     * setAttribute}{@code (name, new Attribute("SomeAttribute", "whatever"))}.
     *
     * <li>{@code proxy.someOperation("param1", 2)} which will be
     * translated into a call to {@code mbs.}{@link
     * MBeanServerConnection#invoke invoke}{@code (name, "someOperation", <etc>)}.
     *
     * </ul>
     *
     * <p>The object returned by this method is a
     * {@link Proxy} whose {@code InvocationHandler} is an
     * {@link MBeanServerInvocationHandler}.</p>
     *
     * <p>This method is equivalent to {@link
     * #newMBeanProxy(MBeanServerConnection, ObjectName, Class,
     * boolean) newMBeanProxy(connection, objectName, interfaceClass,
     * false)}.</p>
     *
     * @param connection the MBean server to forward to.
     * @param objectName the name of the MBean within
     * {@code connection} to forward to.
     * @param interfaceClass the management interface that the MBean
     * exports, which will also be implemented by the returned proxy.
     *
     * @param <T> allows the compiler to know that if the {@code
     * interfaceClass} parameter is {@code MyMBean.class}, for
     * example, then the return type is {@code MyMBean}.
     *
     * @return the new proxy instance.
     *
     * @throws IllegalArgumentException if {@code interfaceClass} is not
     * a <a href="package-summary.html#mgIface">compliant MBean
     * interface</a>
     */
    public static <T> T newMBeanProxy(MBeanServerConnection connection,
                                      ObjectName objectName,
                                      Class<T> interfaceClass) {
        return newMBeanProxy(connection, objectName, interfaceClass, false);
    }

    /**
     * <p>Make a proxy for a Standard MBean in a local or remote MBean
     * Server that may also support the methods of {@link
     * NotificationEmitter}.</p>
     *
     * <p>This method behaves the same as {@link
     * #newMBeanProxy(MBeanServerConnection, ObjectName, Class)}, but
     * additionally, if {@code notificationEmitter} is {@code
     * true}, then the MBean is assumed to be a {@link
     * NotificationBroadcaster} or {@link NotificationEmitter} and the
     * returned proxy will implement {@link NotificationEmitter} as
     * well as {@code interfaceClass}.  A call to {@link
     * NotificationBroadcaster#addNotificationListener} on the proxy
     * will result in a call to {@link
     * MBeanServerConnection#addNotificationListener(ObjectName,
     * NotificationListener, NotificationFilter, Object)}, and
     * likewise for the other methods of {@link
     * NotificationBroadcaster} and {@link NotificationEmitter}.</p>
     *
     * @param connection the MBean server to forward to.
     * @param objectName the name of the MBean within
     * {@code connection} to forward to.
     * @param interfaceClass the management interface that the MBean
     * exports, which will also be implemented by the returned proxy.
     * @param notificationEmitter make the returned proxy
     * implement {@link NotificationEmitter} by forwarding its methods
     * via {@code connection}.
     *
     * @param <T> allows the compiler to know that if the {@code
     * interfaceClass} parameter is {@code MyMBean.class}, for
     * example, then the return type is {@code MyMBean}.
     *
     * @return the new proxy instance.
     *
     * @throws IllegalArgumentException if {@code interfaceClass} is not
     * a <a href="package-summary.html#mgIface">compliant MBean
     * interface</a>
     */
    public static <T> T newMBeanProxy(MBeanServerConnection connection,
                                      ObjectName objectName,
                                      Class<T> interfaceClass,
                                      boolean notificationEmitter) {
        return createProxy(connection, objectName, interfaceClass, notificationEmitter, false);
    }

    /**
     * Make a proxy for an MXBean in a local or remote MBean Server.
     *
     * <p>If you have an MBean Server {@code mbs} containing an
     * MXBean with {@link ObjectName} {@code name}, and if the
     * MXBean's management interface is described by the Java
     * interface {@code MyMXBean}, you can construct a proxy for
     * the MXBean like this:</p>
     *
     * <pre>
     * MyMXBean proxy = JMX.newMXBeanProxy(mbs, name, MyMXBean.class);
     * </pre>
     *
     * <p>Suppose, for example, {@code MyMXBean} looks like this:</p>
     *
     * <pre>
     * public interface MyMXBean {
     *     public String getSimpleAttribute();
     *     public void setSimpleAttribute(String value);
     *     public {@link java.lang.management.MemoryUsage} getMappedAttribute();
     *     public void setMappedAttribute(MemoryUsage memoryUsage);
     *     public MemoryUsage someOperation(String param1, MemoryUsage param2);
     * }
     * </pre>
     *
     * <p>Then:</p>
     *
     * <ul>
     *
     * <li><p>{@code proxy.getSimpleAttribute()} will result in a
     * call to {@code mbs.}{@link MBeanServerConnection#getAttribute
     * getAttribute}{@code (name, "SimpleAttribute")}.</p>
     *
     * <li><p>{@code proxy.setSimpleAttribute("whatever")} will result
     * in a call to {@code mbs.}{@link
     * MBeanServerConnection#setAttribute setAttribute}<code>(name,
     * new Attribute("SimpleAttribute", "whatever"))</code>.</p>
     *
     *     <p>Because {@code String} is a <em>simple type</em>, in the
     *     sense of {@link javax.management.openmbean.SimpleType}, it
     *     is not changed in the context of an MXBean.  The MXBean
     *     proxy behaves the same as a Standard MBean proxy (see
     *     {@link #newMBeanProxy(MBeanServerConnection, ObjectName,
     *     Class) newMBeanProxy}) for the attribute {@code
     *     SimpleAttribute}.</p>
     *
     * <li><p>{@code proxy.getMappedAttribute()} will result in a call
     * to {@code mbs.getAttribute("MappedAttribute")}.  The MXBean
     * mapping rules mean that the actual type of the attribute {@code
     * MappedAttribute} will be {@link
     * javax.management.openmbean.CompositeData CompositeData} and
     * that is what the {@code mbs.getAttribute} call will return.
     * The proxy will then convert the {@code CompositeData} back into
     * the expected type {@code MemoryUsage} using the MXBean mapping
     * rules.</p>
     *
     * <li><p>Similarly, {@code proxy.setMappedAttribute(memoryUsage)}
     * will convert the {@code MemoryUsage} argument into a {@code
     * CompositeData} before calling {@code mbs.setAttribute}.</p>
     *
     * <li><p>{@code proxy.someOperation("whatever", memoryUsage)}
     * will convert the {@code MemoryUsage} argument into a {@code
     * CompositeData} and call {@code mbs.invoke}.  The value returned
     * by {@code mbs.invoke} will be also be a {@code CompositeData},
     * and the proxy will convert this into the expected type {@code
     * MemoryUsage} using the MXBean mapping rules.</p>
     *
     * </ul>
     *
     * <p>The object returned by this method is a
     * {@link Proxy} whose {@code InvocationHandler} is an
     * {@link MBeanServerInvocationHandler}.</p>
     *
     * <p>This method is equivalent to {@link
     * #newMXBeanProxy(MBeanServerConnection, ObjectName, Class,
     * boolean) newMXBeanProxy(connection, objectName, interfaceClass,
     * false)}.</p>
     *
     * @param connection the MBean server to forward to.
     * @param objectName the name of the MBean within
     * {@code connection} to forward to.
     * @param interfaceClass the MXBean interface,
     * which will also be implemented by the returned proxy.
     *
     * @param <T> allows the compiler to know that if the {@code
     * interfaceClass} parameter is {@code MyMXBean.class}, for
     * example, then the return type is {@code MyMXBean}.
     *
     * @return the new proxy instance.
     *
     * @throws IllegalArgumentException if {@code interfaceClass} is not
     * a {@link javax.management.MXBean compliant MXBean interface}
     */
    public static <T> T newMXBeanProxy(MBeanServerConnection connection,
                                       ObjectName objectName,
                                       Class<T> interfaceClass) {
        return newMXBeanProxy(connection, objectName, interfaceClass, false);
    }

    /**
     * <p>Make a proxy for an MXBean in a local or remote MBean
     * Server that may also support the methods of {@link
     * NotificationEmitter}.</p>
     *
     * <p>This method behaves the same as {@link
     * #newMXBeanProxy(MBeanServerConnection, ObjectName, Class)}, but
     * additionally, if {@code notificationEmitter} is {@code
     * true}, then the MXBean is assumed to be a {@link
     * NotificationBroadcaster} or {@link NotificationEmitter} and the
     * returned proxy will implement {@link NotificationEmitter} as
     * well as {@code interfaceClass}.  A call to {@link
     * NotificationBroadcaster#addNotificationListener} on the proxy
     * will result in a call to {@link
     * MBeanServerConnection#addNotificationListener(ObjectName,
     * NotificationListener, NotificationFilter, Object)}, and
     * likewise for the other methods of {@link
     * NotificationBroadcaster} and {@link NotificationEmitter}.</p>
     *
     * @param connection the MBean server to forward to.
     * @param objectName the name of the MBean within
     * {@code connection} to forward to.
     * @param interfaceClass the MXBean interface,
     * which will also be implemented by the returned proxy.
     * @param notificationEmitter make the returned proxy
     * implement {@link NotificationEmitter} by forwarding its methods
     * via {@code connection}.
     *
     * @param <T> allows the compiler to know that if the {@code
     * interfaceClass} parameter is {@code MyMXBean.class}, for
     * example, then the return type is {@code MyMXBean}.
     *
     * @return the new proxy instance.
     *
     * @throws IllegalArgumentException if {@code interfaceClass} is not
     * a {@link javax.management.MXBean compliant MXBean interface}
     */
    public static <T> T newMXBeanProxy(MBeanServerConnection connection,
                                       ObjectName objectName,
                                       Class<T> interfaceClass,
                                       boolean notificationEmitter) {
        return createProxy(connection, objectName, interfaceClass, notificationEmitter, true);
    }

    /**
     * <p>Test whether an interface is an MXBean interface.
     * An interface is an MXBean interface if it is public,
     * annotated {@link MXBean &#64;MXBean} or {@code @MXBean(true)}
     * or if it does not have an {@code @MXBean} annotation
     * and its name ends with "{@code MXBean}".</p>
     *
     * @param interfaceClass The candidate interface.
     *
     * @return true if {@code interfaceClass} is a
     * {@link javax.management.MXBean compliant MXBean interface}
     *
     * @throws NullPointerException if {@code interfaceClass} is null.
     */
    public static boolean isMXBeanInterface(Class<?> interfaceClass) {
        if (!interfaceClass.isInterface())
            return false;
        if (!Modifier.isPublic(interfaceClass.getModifiers()) &&
            !Introspector.ALLOW_NONPUBLIC_MBEAN) {
            return false;
        }
        MXBean a = interfaceClass.getAnnotation(MXBean.class);
        if (a != null)
            return a.value();
        return interfaceClass.getName().endsWith("MXBean");
        // We don't bother excluding the case where the name is
        // exactly the string "MXBean" since that would mean there
        // was no package name, which is pretty unlikely in practice.
    }

    /**
     * Centralised M(X)Bean proxy creation code
     * @param connection {@linkplain MBeanServerConnection} to use
     * @param objectName M(X)Bean object name
     * @param interfaceClass M(X)Bean interface class
     * @param notificationEmitter Is a notification emitter?
     * @param isMXBean Is an MXBean?
     * @return Returns an M(X)Bean proxy generated for the provided interface class
     * @throws SecurityException
     * @throws IllegalArgumentException
     */
    private static <T> T createProxy(MBeanServerConnection connection,
                                     ObjectName objectName,
                                     Class<T> interfaceClass,
                                     boolean notificationEmitter,
                                     boolean isMXBean) {

        try {
            if (isMXBean) {
                // Check interface for MXBean compliance
                Introspector.testComplianceMXBeanInterface(interfaceClass);
            } else {
                // Check interface for MBean compliance
                Introspector.testComplianceMBeanInterface(interfaceClass);
            }
        } catch (NotCompliantMBeanException e) {
            throw new IllegalArgumentException(e);
        }

        InvocationHandler handler = new MBeanServerInvocationHandler(
                connection, objectName, isMXBean);
        final Class<?>[] interfaces;
        if (notificationEmitter) {
            interfaces =
                new Class<?>[] {interfaceClass, NotificationEmitter.class};
        } else
            interfaces = new Class<?>[] {interfaceClass};

        Object proxy = Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                interfaces,
                handler);
        return interfaceClass.cast(proxy);
    }
}
