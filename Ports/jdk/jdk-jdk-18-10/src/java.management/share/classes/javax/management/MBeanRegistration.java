/*
 * Copyright (c) 1999, 2008, Oracle and/or its affiliates. All rights reserved.
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


/**
 * <p>Can be implemented by an MBean in order to
 * carry out operations before and after being registered or unregistered from
 * the MBean Server.  An MBean can also implement this interface in order
 * to get a reference to the MBean Server and/or its name within that
 * MBean Server.</p>
 *
 * @since 1.5
 */
public interface MBeanRegistration   {


    /**
     * Allows the MBean to perform any operations it needs before
     * being registered in the MBean Server.  If the name of the MBean
     * is not specified, the MBean can provide a name for its
     * registration.  If any exception is raised, the MBean will not be
     * registered in the MBean Server.
     *
     * @param server The MBean Server in which the MBean will be registered.
     *
     * @param name The object name of the MBean.  This name is null if
     * the name parameter to one of the <code>createMBean</code> or
     * <code>registerMBean</code> methods in the {@link MBeanServer}
     * interface is null.  In that case, this method must return a
     * non-null ObjectName for the new MBean.
     *
     * @return The name under which the MBean is to be registered.
     * This value must not be null.  If the <code>name</code>
     * parameter is not null, it will usually but not necessarily be
     * the returned value.
     *
     * @exception java.lang.Exception This exception will be caught by
     * the MBean Server and re-thrown as an {@link
     * MBeanRegistrationException}.
     */
    public ObjectName preRegister(MBeanServer server,
                                  ObjectName name) throws java.lang.Exception;

    /**
     * Allows the MBean to perform any operations needed after having been
     * registered in the MBean server or after the registration has failed.
     * <p>If the implementation of this method throws a {@link RuntimeException}
     * or an {@link Error}, the MBean Server will rethrow those inside
     * a {@link RuntimeMBeanException} or {@link RuntimeErrorException},
     * respectively. However, throwing an exception in {@code postRegister}
     * will not change the state of the MBean:
     * if the MBean was already registered ({@code registrationDone} is
     * {@code true}), the MBean will remain registered. </p>
     * <p>This might be confusing for the code calling {@code createMBean()}
     * or {@code registerMBean()}, as such code might assume that MBean
     * registration has failed when such an exception is raised.
     * Therefore it is recommended that implementations of
     * {@code postRegister} do not throw Runtime Exceptions or Errors if it
     * can be avoided.</p>
     * @param registrationDone Indicates whether or not the MBean has
     * been successfully registered in the MBean server. The value
     * false means that the registration phase has failed.
     */
    public void postRegister(Boolean registrationDone);

    /**
     * Allows the MBean to perform any operations it needs before
     * being unregistered by the MBean server.
     *
     * @exception java.lang.Exception This exception will be caught by
     * the MBean server and re-thrown as an {@link
     * MBeanRegistrationException}.
     */
    public void preDeregister() throws java.lang.Exception ;

    /**
     * Allows the MBean to perform any operations needed after having been
     * unregistered in the MBean server.
     * <p>If the implementation of this method throws a {@link RuntimeException}
     * or an {@link Error}, the MBean Server will rethrow those inside
     * a {@link RuntimeMBeanException} or {@link RuntimeErrorException},
     * respectively. However, throwing an exception in {@code postDeregister}
     * will not change the state of the MBean:
     * the MBean was already successfully deregistered and will remain so. </p>
     * <p>This might be confusing for the code calling
     * {@code unregisterMBean()}, as it might assume that MBean deregistration
     * has failed. Therefore it is recommended that implementations of
     * {@code postDeregister} do not throw Runtime Exceptions or Errors if it
     * can be avoided.</p>
     */
    public void postDeregister();

 }
