/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.beans.beancontext;

import java.io.Serial;
import java.util.Iterator;

/**
 * <p>
 * This event type is used by the BeanContextServicesListener in order to
 * identify the service being registered.
 * </p>
 */

public class BeanContextServiceAvailableEvent extends BeanContextEvent {

    /**
     * Use serialVersionUID from JDK 1.7 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -5333985775656400778L;

    /**
     * Construct a {@code BeanContextAvailableServiceEvent}.
     * @param bcs The context in which the service has become available
     * @param sc A {@code Class} reference to the newly available service
     */
    public BeanContextServiceAvailableEvent(BeanContextServices bcs, Class<?> sc) {
        super((BeanContext)bcs);

        serviceClass = sc;
    }

    /**
     * Gets the source as a reference of type {@code BeanContextServices}.
     * @return The context in which the service has become available
     */
    public BeanContextServices getSourceAsBeanContextServices() {
        return (BeanContextServices)getBeanContext();
    }

    /**
     * Gets the service class that is the subject of this notification.
     * @return A {@code Class} reference to the newly available service
     */
    public Class<?> getServiceClass() { return serviceClass; }

    /**
     * Gets the list of service dependent selectors.
     * @return the current selectors available from the service
     */
    public Iterator<?> getCurrentServiceSelectors() {
        return ((BeanContextServices)getSource()).getCurrentServiceSelectors(serviceClass);
    }

    /*
     * fields
     */

    /**
     * A {@code Class} reference to the newly available service
     */
    protected Class<?>                   serviceClass;
}
