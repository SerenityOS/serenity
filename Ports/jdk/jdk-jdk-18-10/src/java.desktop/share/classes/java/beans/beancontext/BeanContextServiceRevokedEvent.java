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

/**
 * <p>
 * This event type is used by the
 * {@code BeanContextServiceRevokedListener} in order to
 * identify the service being revoked.
 * </p>
 */
public class BeanContextServiceRevokedEvent extends BeanContextEvent {

    /**
     * Use serialVersionUID from JDK 1.7 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -1295543154724961754L;

    /**
     * Construct a {@code BeanContextServiceEvent}.
     * @param bcs the {@code BeanContextServices}
     * from which this service is being revoked
     * @param sc the service that is being revoked
     * @param invalidate {@code true} for immediate revocation
     */
    public BeanContextServiceRevokedEvent(BeanContextServices bcs, Class<?> sc, boolean invalidate) {
        super((BeanContext)bcs);

        serviceClass    = sc;
        invalidateRefs  = invalidate;
    }

    /**
     * Gets the source as a reference of type {@code BeanContextServices}
     * @return the {@code BeanContextServices} from which
     * this service is being revoked
     */
    public BeanContextServices getSourceAsBeanContextServices() {
        return (BeanContextServices)getBeanContext();
    }

    /**
     * Gets the service class that is the subject of this notification
     * @return A {@code Class} reference to the
     * service that is being revoked
     */
    public Class<?> getServiceClass() { return serviceClass; }

    /**
     * Checks this event to determine whether or not
     * the service being revoked is of a particular class.
     * @param service the service of interest (should be non-null)
     * @return {@code true} if the service being revoked is of the
     * same class as the specified service
     */
    public boolean isServiceClass(Class<?> service) {
        return serviceClass.equals(service);
    }

    /**
     * Reports if the current service is being forcibly revoked,
     * in which case the references are now invalidated and unusable.
     * @return {@code true} if current service is being forcibly revoked
     */
    public boolean isCurrentServiceInvalidNow() { return invalidateRefs; }

    /**
     * fields
     */

    /**
     * A {@code Class} reference to the service that is being revoked.
     */
    protected Class<?> serviceClass;

    /**
     * {@code true} if current service is being forcibly revoked.
     */
    private boolean invalidateRefs;
}
