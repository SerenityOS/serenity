/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Iterator;

import java.util.TooManyListenersException;

import java.beans.beancontext.BeanContext;

import java.beans.beancontext.BeanContextServiceProvider;

import java.beans.beancontext.BeanContextServicesListener;


/**
 * <p>
 * The BeanContextServices interface provides a mechanism for a BeanContext
 * to expose generic "services" to the BeanContextChild objects within.
 * </p>
 */
public interface BeanContextServices extends BeanContext, BeanContextServicesListener {

    /**
     * Adds a service to this BeanContext.
     * {@code BeanContextServiceProvider}s call this method
     * to register a particular service with this context.
     * If the service has not previously been added, the
     * {@code BeanContextServices} associates
     * the service with the {@code BeanContextServiceProvider} and
     * fires a {@code BeanContextServiceAvailableEvent} to all
     * currently registered {@code BeanContextServicesListeners}.
     * The method then returns {@code true}, indicating that
     * the addition of the service was successful.
     * If the given service has already been added, this method
     * simply returns {@code false}.
     * @param serviceClass     the service to add
     * @param serviceProvider  the {@code BeanContextServiceProvider}
     * associated with the service
     * @return true if the service was successful added, false otherwise
     */
    boolean addService(Class<?> serviceClass, BeanContextServiceProvider serviceProvider);

    /**
     * BeanContextServiceProviders wishing to remove
     * a currently registered service from this context
     * may do so via invocation of this method. Upon revocation of
     * the service, the {@code BeanContextServices} fires a
     * {@code BeanContextServiceRevokedEvent} to its
     * list of currently registered
     * {@code BeanContextServiceRevokedListeners} and
     * {@code BeanContextServicesListeners}.
     * @param serviceClass the service to revoke from this BeanContextServices
     * @param serviceProvider the BeanContextServiceProvider associated with
     * this particular service that is being revoked
     * @param revokeCurrentServicesNow a value of {@code true}
     * indicates an exceptional circumstance where the
     * {@code BeanContextServiceProvider} or
     * {@code BeanContextServices} wishes to immediately
     * terminate service to all currently outstanding references
     * to the specified service.
     */
    void revokeService(Class<?> serviceClass, BeanContextServiceProvider serviceProvider, boolean revokeCurrentServicesNow);

    /**
     * Reports whether or not a given service is
     * currently available from this context.
     * @param serviceClass the service in question
     * @return true if the service is available
     */
    boolean hasService(Class<?> serviceClass);

    /**
     * A {@code BeanContextChild}, or any arbitrary object
     * associated with a {@code BeanContextChild}, may obtain
     * a reference to a currently registered service from its
     * nesting {@code BeanContextServices}
     * via invocation of this method. When invoked, this method
     * gets the service by calling the getService() method on the
     * underlying {@code BeanContextServiceProvider}.
     * @param child the {@code BeanContextChild}
     * associated with this request
     * @param requestor the object requesting the service
     * @param serviceClass class of the requested service
     * @param serviceSelector the service dependent parameter
     * @param bcsrl the
     * {@code BeanContextServiceRevokedListener} to notify
     * if the service should later become revoked
     * @throws TooManyListenersException if there are too many listeners
     * @return a reference to this context's named
     * Service as requested or {@code null}
     */
    Object getService(BeanContextChild child, Object requestor, Class<?> serviceClass, Object serviceSelector, BeanContextServiceRevokedListener bcsrl) throws TooManyListenersException;

    /**
     * Releases a {@code BeanContextChild}'s
     * (or any arbitrary object associated with a BeanContextChild)
     * reference to the specified service by calling releaseService()
     * on the underlying {@code BeanContextServiceProvider}.
     * @param child the {@code BeanContextChild}
     * @param requestor the requestor
     * @param service the service
     */
    void releaseService(BeanContextChild child, Object requestor, Object service);

    /**
     * Gets the currently available services for this context.
     * @return an {@code Iterator} consisting of the
     * currently available services
     */
    Iterator<?> getCurrentServiceClasses();

    /**
     * Gets the list of service dependent service parameters
     * (Service Selectors) for the specified service, by
     * calling getCurrentServiceSelectors() on the
     * underlying BeanContextServiceProvider.
     * @param serviceClass the specified service
     * @return the currently available service selectors
     * for the named serviceClass
     */
    Iterator<?> getCurrentServiceSelectors(Class<?> serviceClass);

    /**
     * Adds a {@code BeanContextServicesListener} to this BeanContext
     * @param bcsl the {@code BeanContextServicesListener} to add
     */
    void addBeanContextServicesListener(BeanContextServicesListener bcsl);

    /**
     * Removes a {@code BeanContextServicesListener}
     * from this {@code BeanContext}
     * @param bcsl the {@code BeanContextServicesListener}
     * to remove from this context
     */
    void removeBeanContextServicesListener(BeanContextServicesListener bcsl);
}
