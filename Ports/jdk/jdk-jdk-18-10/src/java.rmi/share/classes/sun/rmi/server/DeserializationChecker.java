/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.rmi.server;

import java.io.ObjectStreamClass;
import java.lang.reflect.Method;

/**
 * Implementing this interface to have a deserialization control when RMI
 * dispatches a remote request. If an exported object implements this interface,
 * RMI dispatching mechanism will call the method {@code check} every time
 * deserialising a remote object for invoking a method of the exported object.
 *
 * @author sjiang
 */
public interface DeserializationChecker {
    /**
     * Will be called to check a descriptor.
     * This method may be called 2 times, the first time is when a descriptor is read
     * from the stream, the second is just before creating an object described
     * by this descriptor.
     *
     * @param method the method invoked from a remote request.
     * @param descriptor The descriptor of the class of any object deserialised
     *  while deserialising the parameter. The first descriptor will be that of
     *  the top level object (the concrete class of the parameter itself);
     *  Subsequent calls with the same {@code method}, {@code paramIndex} and
     *  {@code callID} will correspond to objects contained in the parameter.
     * @param paramIndex an index indicates the position of a parameter in the
     * method. This index will be reused for deserialising all
     * objects contained in the parameter object. For example, the parameter
     * being deserialised is a {@code List}, all deserialisation calls for its
     * elements will have same index.
     * @param callID a unique ID identifying one
     * time method invocation, the same ID is used for deserialization call of
     * all parameters within the method.
     */
    public void check(Method method,
            ObjectStreamClass descriptor,
            int paramIndex,
            int callID);

    /**
     * Will be called to validate a Proxy interfaces from a remote user before loading it.
     * @param method the method invoked from a remote request.
     * @param ifaces a string table of all interfaces implemented by the proxy to be checked.
     * @param paramIndex an index indicates the position of a parameter in the
     * method. This index will be reused for deserialising all
     * objects contained in the parameter object. For example, the parameter
     * being deserialised is a {@code List}, all deserialisation calls for its
     * elements will have same index.
     * @param callID a unique ID identifying one
     * time method invocation, the same ID is used for deserialization call of
     * all parameters within the method.
     */
    public void checkProxyClass(Method method,
            String[] ifaces,
            int paramIndex,
            int callID);

    /**
     * Inform of the completion of parameter deserialisation for a method invocation.
     * This is useful if the last parameter is a complex  object, like a {@code List}
     * which elements are complex object too.
     *
     * The default implementation does nothing.
     * @param callID the ID identifying a method invocation.
     */
    public default void end(int callID) {}
}
