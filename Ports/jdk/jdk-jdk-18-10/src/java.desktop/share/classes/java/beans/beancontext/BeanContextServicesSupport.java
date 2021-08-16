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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TooManyListenersException;

/**
 * <p>
 * This helper class provides a utility implementation of the
 * java.beans.beancontext.BeanContextServices interface.
 * </p>
 * <p>
 * Since this class directly implements the BeanContextServices interface,
 * the class can, and is intended to be used either by subclassing this
 * implementation, or via delegation of an instance of this class
 * from another through the BeanContextProxy interface.
 * </p>
 *
 * @author Laurence P. G. Cable
 * @since 1.2
 */

public class      BeanContextServicesSupport extends BeanContextSupport
       implements BeanContextServices {

    /**
     * Use serialVersionUID from JDK 1.7 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -8494482757288719206L;

    /**
     * <p>
     * Construct a BeanContextServicesSupport instance
     * </p>
     *
     * @param peer      The peer BeanContext we are supplying an implementation for, if null the this object is its own peer
     * @param lcle      The current Locale for this BeanContext.
     * @param dTime     The initial state, true if in design mode, false if runtime.
     * @param visible   The initial visibility.
     *
     */

    public BeanContextServicesSupport(BeanContextServices peer, Locale lcle, boolean dTime, boolean visible) {
        super(peer, lcle, dTime, visible);
    }

    /**
     * Create an instance using the specified Locale and design mode.
     *
     * @param peer      The peer BeanContext we are supplying an implementation for, if null the this object is its own peer
     * @param lcle      The current Locale for this BeanContext.
     * @param dtime     The initial state, true if in design mode, false if runtime.
     */

    public BeanContextServicesSupport(BeanContextServices peer, Locale lcle, boolean dtime) {
        this (peer, lcle, dtime, true);
    }

    /**
     * Create an instance using the specified locale
     *
     * @param peer      The peer BeanContext we are supplying an implementation for, if null the this object is its own peer
     * @param lcle      The current Locale for this BeanContext.
     */

    public BeanContextServicesSupport(BeanContextServices peer, Locale lcle) {
        this (peer, lcle, false, true);
    }

    /**
     * Create an instance with a peer
     *
     * @param peer      The peer BeanContext we are supplying an implementation for, if null the this object is its own peer
     */

    public BeanContextServicesSupport(BeanContextServices peer) {
        this (peer, null, false, true);
    }

    /**
     * Create an instance that is not a delegate of another object
     */

    public BeanContextServicesSupport() {
        this (null, null, false, true);
    }

    /**
     * called by BeanContextSupport superclass during construction and
     * deserialization to initialize subclass transient state.
     *
     * subclasses may envelope this method, but should not override it or
     * call it directly.
     */

    public void initialize() {
        super.initialize();
        services     = new HashMap<>(serializable + 1);
        bcsListeners = new ArrayList<>(1);
    }

    /**
     * Gets the {@code BeanContextServices} associated with this
     * {@code BeanContextServicesSupport}.
     *
     * @return the instance of {@code BeanContext}
     * this object is providing the implementation for.
     */
    public BeanContextServices getBeanContextServicesPeer() {
        return (BeanContextServices)getBeanContextChildPeer();
    }

    /************************************************************************/

    /*
     * protected nested class containing per child information, an instance
     * of which is associated with each child in the "children" hashtable.
     * subclasses can extend this class to include their own per-child state.
     *
     * Note that this 'value' is serialized with the corresponding child 'key'
     * when the BeanContextSupport is serialized.
     */

    protected class BCSSChild extends BeanContextSupport.BCSChild  {

        /**
         * Use serialVersionUID from JDK 1.7 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = -3263851306889194873L;

        /*
         * private nested class to map serviceClass to Provider and requestors
         * listeners.
         */

        class BCSSCServiceClassRef {

            // create an instance of a service ref

            BCSSCServiceClassRef(Class<?> sc, BeanContextServiceProvider bcsp, boolean delegated) {
                super();

                serviceClass     = sc;

                if (delegated)
                    delegateProvider = bcsp;
                else
                    serviceProvider  = bcsp;
            }

            // add a requestor and assoc listener

            void addRequestor(Object requestor, BeanContextServiceRevokedListener bcsrl) throws TooManyListenersException {
                BeanContextServiceRevokedListener cbcsrl = requestors.get(requestor);

                if (cbcsrl != null && !cbcsrl.equals(bcsrl))
                    throw new TooManyListenersException();

                requestors.put(requestor, bcsrl);
            }

            // remove a requestor

            void removeRequestor(Object requestor) {
                requestors.remove(requestor);
            }

            // check a requestors listener

            void verifyRequestor(Object requestor, BeanContextServiceRevokedListener bcsrl) throws TooManyListenersException {
                BeanContextServiceRevokedListener cbcsrl = requestors.get(requestor);

                if (cbcsrl != null && !cbcsrl.equals(bcsrl))
                    throw new TooManyListenersException();
            }

            void verifyAndMaybeSetProvider(BeanContextServiceProvider bcsp, boolean isDelegated) {
                BeanContextServiceProvider current;

                if (isDelegated) { // the provider is delegated
                    current = delegateProvider;

                    if (current == null || bcsp == null) {
                        delegateProvider = bcsp;
                        return;
                    }
                } else { // the provider is registered with this BCS
                    current = serviceProvider;

                    if (current == null || bcsp == null) {
                        serviceProvider = bcsp;
                        return;
                    }
                }

                if (!current.equals(bcsp))
                    throw new UnsupportedOperationException("existing service reference obtained from different BeanContextServiceProvider not supported");

            }

            @SuppressWarnings("unchecked") // Cast from clone
            Iterator<Map.Entry<Object, BeanContextServiceRevokedListener>> cloneOfEntries() {
                return ((HashMap<Object, BeanContextServiceRevokedListener>)requestors.clone()).entrySet().iterator();
            }

            Iterator<Map.Entry<Object, BeanContextServiceRevokedListener>> entries() {
                return requestors.entrySet().iterator();
            }

            boolean isEmpty() { return requestors.isEmpty(); }

            Class<?> getServiceClass() { return serviceClass; }

            BeanContextServiceProvider getServiceProvider() {
                return serviceProvider;
            }

            BeanContextServiceProvider getDelegateProvider() {
                return delegateProvider;
            }

            boolean isDelegated() { return delegateProvider != null; }

            void addRef(boolean delegated) {
                if (delegated) {
                    delegateRefs++;
                } else {
                    serviceRefs++;
                }
            }


            void releaseRef(boolean delegated) {
                if (delegated) {
                    if (--delegateRefs == 0) {
                        delegateProvider = null;
                    }
                } else {
                    if (--serviceRefs  <= 0) {
                        serviceProvider = null;
                    }
                }
            }

            int getRefs() { return serviceRefs + delegateRefs; }

            int getDelegateRefs() { return delegateRefs; }

            int getServiceRefs() { return serviceRefs; }

            /*
             * fields
             */

            Class<?>                            serviceClass;

            BeanContextServiceProvider          serviceProvider;
            int                                 serviceRefs;

            BeanContextServiceProvider          delegateProvider; // proxy
            int                                 delegateRefs;

            HashMap<Object, BeanContextServiceRevokedListener> requestors = new HashMap<>(1);
        }

        /*
         * per service reference info ...
         */

        class BCSSCServiceRef {
            BCSSCServiceRef(BCSSCServiceClassRef scref, boolean isDelegated) {
                serviceClassRef = scref;
                delegated       = isDelegated;
            }

            void addRef()  { refCnt++;        }
            int  release() { return --refCnt; }

            BCSSCServiceClassRef getServiceClassRef() { return serviceClassRef; }

            boolean isDelegated() { return delegated; }

            /*
             * fields
             */

            BCSSCServiceClassRef serviceClassRef;
            int                  refCnt    = 1;
            boolean              delegated = false;
        }

        BCSSChild(Object bcc, Object peer) { super(bcc, peer); }

        // note usage of service per requestor, per service

        synchronized void usingService(Object requestor, Object service, Class<?> serviceClass, BeanContextServiceProvider bcsp, boolean isDelegated, BeanContextServiceRevokedListener bcsrl)  throws TooManyListenersException, UnsupportedOperationException {

            // first, process mapping from serviceClass to requestor(s)

            BCSSCServiceClassRef serviceClassRef = null;

            if (serviceClasses == null)
                serviceClasses = new HashMap<>(1);
            else
                serviceClassRef = serviceClasses.get(serviceClass);

            if (serviceClassRef == null) { // new service being used ...
                serviceClassRef = new BCSSCServiceClassRef(serviceClass, bcsp, isDelegated);
                serviceClasses.put(serviceClass, serviceClassRef);

            } else { // existing service ...
                serviceClassRef.verifyAndMaybeSetProvider(bcsp, isDelegated); // throws
                serviceClassRef.verifyRequestor(requestor, bcsrl); // throws
            }

            serviceClassRef.addRequestor(requestor, bcsrl);
            serviceClassRef.addRef(isDelegated);

            // now handle mapping from requestor to service(s)

            BCSSCServiceRef serviceRef = null;
            Map<Object , BCSSCServiceRef> services   = null;

            if (serviceRequestors == null) {
                serviceRequestors = new HashMap<>(1);
            } else {
                services = serviceRequestors.get(requestor);
            }

            if (services == null) {
                services = new HashMap<>(1);

                serviceRequestors.put(requestor, services);
            } else
                serviceRef = services.get(service);

            if (serviceRef == null) {
                serviceRef = new BCSSCServiceRef(serviceClassRef, isDelegated);

                services.put(service, serviceRef);
            } else {
                serviceRef.addRef();
            }
        }

        // release a service reference

        synchronized void releaseService(Object requestor, Object service) {
            if (serviceRequestors == null) return;

            Map<Object, BCSSCServiceRef> services = serviceRequestors.get(requestor);

            if (services == null) return; // oops its not there anymore!

            BCSSCServiceRef serviceRef = services.get(service);

            if (serviceRef == null) return; // oops its not there anymore!

            BCSSCServiceClassRef serviceClassRef = serviceRef.getServiceClassRef();
            boolean                    isDelegated = serviceRef.isDelegated();
            BeanContextServiceProvider bcsp        = isDelegated ? serviceClassRef.getDelegateProvider() : serviceClassRef.getServiceProvider();

            bcsp.releaseService(BeanContextServicesSupport.this.getBeanContextServicesPeer(), requestor, service);

            serviceClassRef.releaseRef(isDelegated);
            serviceClassRef.removeRequestor(requestor);

            if (serviceRef.release() == 0) {

                services.remove(service);

                if (services.isEmpty()) {
                    serviceRequestors.remove(requestor);
                    serviceClassRef.removeRequestor(requestor);
                }

                if (serviceRequestors.isEmpty()) {
                    serviceRequestors = null;
                }

                if (serviceClassRef.isEmpty()) {
                    serviceClasses.remove(serviceClassRef.getServiceClass());
                }

                if (serviceClasses.isEmpty())
                    serviceClasses = null;
            }
        }

        // revoke a service

        synchronized void revokeService(Class<?> serviceClass, boolean isDelegated, boolean revokeNow) {
            if (serviceClasses == null) return;

            BCSSCServiceClassRef serviceClassRef = serviceClasses.get(serviceClass);

            if (serviceClassRef == null) return;

            Iterator<Map.Entry<Object, BeanContextServiceRevokedListener>> i = serviceClassRef.cloneOfEntries();

            BeanContextServiceRevokedEvent bcsre       = new BeanContextServiceRevokedEvent(BeanContextServicesSupport.this.getBeanContextServicesPeer(), serviceClass, revokeNow);
            boolean                        noMoreRefs  = false;

            while (i.hasNext() && serviceRequestors != null) {
                Map.Entry<Object,BeanContextServiceRevokedListener> entry    = i.next();
                BeanContextServiceRevokedListener listener = entry.getValue();

                if (revokeNow) {
                    Object  requestor = entry.getKey();
                    Map<Object, BCSSCServiceRef> services  = serviceRequestors.get(requestor);

                    if (services != null) {
                        Iterator<Map.Entry<Object, BCSSCServiceRef>> i1 = services.entrySet().iterator();

                        while (i1.hasNext()) {
                            Map.Entry<Object, BCSSCServiceRef> tmp        = i1.next();

                            BCSSCServiceRef serviceRef = tmp.getValue();
                            if (serviceRef.getServiceClassRef().equals(serviceClassRef) && isDelegated == serviceRef.isDelegated()) {
                                i1.remove();
                            }
                        }

                        if (noMoreRefs = services.isEmpty()) {
                            serviceRequestors.remove(requestor);
                        }
                    }

                    if (noMoreRefs) serviceClassRef.removeRequestor(requestor);
                }

                listener.serviceRevoked(bcsre);
            }

            if (revokeNow && serviceClasses != null) {
                if (serviceClassRef.isEmpty())
                    serviceClasses.remove(serviceClass);

                if (serviceClasses.isEmpty())
                    serviceClasses = null;
            }

            if (serviceRequestors != null && serviceRequestors.isEmpty())
                serviceRequestors = null;
        }

        // release all references for this child since it has been unnested.

        void cleanupReferences() {

            if (serviceRequestors == null) return;

            Iterator<Map.Entry<Object, Map<Object, BCSSCServiceRef>>> requestors = serviceRequestors.entrySet().iterator();

            while(requestors.hasNext()) {
                Map.Entry<Object, Map<Object, BCSSCServiceRef>> tmp = requestors.next();
                Object               requestor = tmp.getKey();
                Iterator<Map.Entry<Object, BCSSCServiceRef>> services  = tmp.getValue().entrySet().iterator();

                requestors.remove();

                while (services.hasNext()) {
                    Map.Entry<Object, BCSSCServiceRef> entry   = services.next();
                    Object          service = entry.getKey();
                    BCSSCServiceRef sref    = entry.getValue();

                    BCSSCServiceClassRef       scref = sref.getServiceClassRef();

                    BeanContextServiceProvider bcsp  = sref.isDelegated() ? scref.getDelegateProvider() : scref.getServiceProvider();

                    scref.removeRequestor(requestor);
                    services.remove();

                    while (sref.release() >= 0) {
                        bcsp.releaseService(BeanContextServicesSupport.this.getBeanContextServicesPeer(), requestor, service);
                    }
                }
            }

            serviceRequestors = null;
            serviceClasses    = null;
        }

        void revokeAllDelegatedServicesNow() {
            if (serviceClasses == null) return;

            for (BCSSCServiceClassRef serviceClassRef : new HashSet<>(serviceClasses.values())) {
                if (!serviceClassRef.isDelegated()) continue;

                Iterator<Map.Entry<Object, BeanContextServiceRevokedListener>> i = serviceClassRef.cloneOfEntries();
                BeanContextServiceRevokedEvent bcsre       = new BeanContextServiceRevokedEvent(BeanContextServicesSupport.this.getBeanContextServicesPeer(), serviceClassRef.getServiceClass(), true);
                boolean                        noMoreRefs  = false;

                while (i.hasNext()) {
                    Map.Entry<Object, BeanContextServiceRevokedListener> entry     = i.next();
                    BeanContextServiceRevokedListener listener  = entry.getValue();

                    Object                            requestor = entry.getKey();
                    Map<Object, BCSSCServiceRef>      services  = serviceRequestors.get(requestor);

                    if (services != null) {
                        Iterator<Map.Entry<Object, BCSSCServiceRef>> i1 = services.entrySet().iterator();

                        while (i1.hasNext()) {
                            Map.Entry<Object, BCSSCServiceRef>   tmp        = i1.next();

                            BCSSCServiceRef serviceRef = tmp.getValue();
                            if (serviceRef.getServiceClassRef().equals(serviceClassRef) && serviceRef.isDelegated()) {
                                i1.remove();
                            }
                        }

                        if (noMoreRefs = services.isEmpty()) {
                            serviceRequestors.remove(requestor);
                        }
                    }

                    if (noMoreRefs) serviceClassRef.removeRequestor(requestor);

                    listener.serviceRevoked(bcsre);

                    if (serviceClassRef.isEmpty())
                        serviceClasses.remove(serviceClassRef.getServiceClass());
                }
            }

            if (serviceClasses.isEmpty()) serviceClasses = null;

            if (serviceRequestors != null && serviceRequestors.isEmpty())
                serviceRequestors = null;
        }

        /*
         * fields
         */

        private transient HashMap<Class<?>, BCSSCServiceClassRef> serviceClasses;
        private transient HashMap<Object, Map<Object, BeanContextServicesSupport.BCSSChild.BCSSCServiceRef>> serviceRequestors;
    }

    /**
     * <p>
     * Subclasses can override this method to insert their own subclass
     * of Child without having to override add() or the other Collection
     * methods that add children to the set.
     * </p>
     *
     * @param targetChild the child to create the Child on behalf of
     * @param peer        the peer if the targetChild and peer are related by BeanContextProxy
     */

    protected BCSChild createBCSChild(Object targetChild, Object peer) {
        return new BCSSChild(targetChild, peer);
    }

    /************************************************************************/

        /**
         * subclasses may subclass this nested class to add behaviors for
         * each BeanContextServicesProvider.
         */

        protected static class BCSSServiceProvider implements Serializable {

            /**
             * Use serialVersionUID from JDK 1.7 for interoperability.
             */
            @Serial
            private static final long serialVersionUID = 861278251667444782L;

            BCSSServiceProvider(Class<?> sc, BeanContextServiceProvider bcsp) {
                super();

                serviceProvider = bcsp;
            }

            /**
             * Returns the service provider.
             * @return the service provider
             */
            protected BeanContextServiceProvider getServiceProvider() {
                return serviceProvider;
            }

            /**
             * The service provider.
             */
            @SuppressWarnings("serial") // Not statically typed as Serializable
            protected BeanContextServiceProvider serviceProvider;
        }

        /**
         * subclasses can override this method to create new subclasses of
         * BCSSServiceProvider without having to override addService() in
         * order to instantiate.
         * @param sc the class
         * @param bcsp the service provider
         * @return a service provider without overriding addService()
         */

        protected BCSSServiceProvider createBCSSServiceProvider(Class<?> sc, BeanContextServiceProvider bcsp) {
            return new BCSSServiceProvider(sc, bcsp);
        }

    /************************************************************************/

    /**
     * add a BeanContextServicesListener
     *
     * @throws NullPointerException if the argument is null
     */

    public void addBeanContextServicesListener(BeanContextServicesListener bcsl) {
        if (bcsl == null) throw new NullPointerException("bcsl");

        synchronized(bcsListeners) {
            if (bcsListeners.contains(bcsl))
                return;
            else
                bcsListeners.add(bcsl);
        }
    }

    /**
     * remove a BeanContextServicesListener
     */

    public void removeBeanContextServicesListener(BeanContextServicesListener bcsl) {
        if (bcsl == null) throw new NullPointerException("bcsl");

        synchronized(bcsListeners) {
            if (!bcsListeners.contains(bcsl))
                return;
            else
                bcsListeners.remove(bcsl);
        }
    }

    /**
     * add a service
     * @param serviceClass the service class
     * @param bcsp the service provider
     */

    public boolean addService(Class<?> serviceClass, BeanContextServiceProvider bcsp) {
        return addService(serviceClass, bcsp, true);
    }

    /**
     * add a service
     * @param serviceClass the service class
     * @param bcsp the service provider
     * @param fireEvent whether or not an event should be fired
     * @return true if the service was successfully added
     */

    protected boolean addService(Class<?> serviceClass, BeanContextServiceProvider bcsp, boolean fireEvent) {

        if (serviceClass == null) throw new NullPointerException("serviceClass");
        if (bcsp         == null) throw new NullPointerException("bcsp");

        synchronized(BeanContext.globalHierarchyLock) {
            if (services.containsKey(serviceClass))
                return false;
            else {
                services.put(serviceClass,  createBCSSServiceProvider(serviceClass, bcsp));

                if (bcsp instanceof Serializable) serializable++;

                if (!fireEvent) return true;


                BeanContextServiceAvailableEvent bcssae = new BeanContextServiceAvailableEvent(getBeanContextServicesPeer(), serviceClass);

                fireServiceAdded(bcssae);

                synchronized(children) {
                    for (Object c : children.keySet()) {
                        if (c instanceof BeanContextServices) {
                            ((BeanContextServicesListener)c).serviceAvailable(bcssae);
                        }
                    }
                }

                return true;
            }
        }
    }

    /**
     * remove a service
     * @param serviceClass the service class
     * @param bcsp the service provider
     * @param revokeCurrentServicesNow whether or not to revoke the service
     */

    public void revokeService(Class<?> serviceClass, BeanContextServiceProvider bcsp, boolean revokeCurrentServicesNow) {

        if (serviceClass == null) throw new NullPointerException("serviceClass");
        if (bcsp         == null) throw new NullPointerException("bcsp");

        synchronized(BeanContext.globalHierarchyLock) {
            if (!services.containsKey(serviceClass)) return;

            BCSSServiceProvider bcsssp = services.get(serviceClass);

            if (!bcsssp.getServiceProvider().equals(bcsp))
                throw new IllegalArgumentException("service provider mismatch");

            services.remove(serviceClass);

            if (bcsp instanceof Serializable) serializable--;

            Iterator<BeanContextSupport.BCSChild> i = bcsChildren(); // get the BCSChild values.

            while (i.hasNext()) {
                ((BCSSChild)i.next()).revokeService(serviceClass, false, revokeCurrentServicesNow);
            }

            fireServiceRevoked(serviceClass, revokeCurrentServicesNow);
        }
    }

    /**
     * has a service, which may be delegated
     */

    public synchronized boolean hasService(Class<?> serviceClass) {
        if (serviceClass == null) throw new NullPointerException("serviceClass");

        synchronized(BeanContext.globalHierarchyLock) {
            if (services.containsKey(serviceClass)) return true;

            BeanContextServices bcs = null;

            try {
                bcs = (BeanContextServices)getBeanContext();
            } catch (ClassCastException cce) {
                return false;
            }

            return bcs == null ? false : bcs.hasService(serviceClass);
        }
    }

    /************************************************************************/

    /*
     * a nested subclass used to represent a proxy for serviceClasses delegated
     * to an enclosing BeanContext.
     */

    protected class BCSSProxyServiceProvider implements BeanContextServiceProvider, BeanContextServiceRevokedListener {

        BCSSProxyServiceProvider(BeanContextServices bcs) {
            super();

            nestingCtxt = bcs;
        }

        public Object getService(BeanContextServices bcs, Object requestor, Class<?> serviceClass, Object serviceSelector) {
            Object service = null;

            try {
                service = nestingCtxt.getService(bcs, requestor, serviceClass, serviceSelector, this);
            } catch (TooManyListenersException tmle) {
                return null;
            }

            return service;
        }

        public void releaseService(BeanContextServices bcs, Object requestor, Object service) {
            nestingCtxt.releaseService(bcs, requestor, service);
        }

        public Iterator<?> getCurrentServiceSelectors(BeanContextServices bcs, Class<?> serviceClass) {
            return nestingCtxt.getCurrentServiceSelectors(serviceClass);
        }

        public void serviceRevoked(BeanContextServiceRevokedEvent bcsre) {
            Iterator<BeanContextSupport.BCSChild> i = bcsChildren(); // get the BCSChild values.

            while (i.hasNext()) {
                ((BCSSChild)i.next()).revokeService(bcsre.getServiceClass(), true, bcsre.isCurrentServiceInvalidNow());
            }
        }

        /*
         * fields
         */

        private BeanContextServices nestingCtxt;
    }

    /************************************************************************/

    /**
     * obtain a service which may be delegated
     */

     public Object getService(BeanContextChild child, Object requestor, Class<?> serviceClass, Object serviceSelector, BeanContextServiceRevokedListener bcsrl) throws TooManyListenersException {
        if (child        == null) throw new NullPointerException("child");
        if (serviceClass == null) throw new NullPointerException("serviceClass");
        if (requestor    == null) throw new NullPointerException("requestor");
        if (bcsrl        == null) throw new NullPointerException("bcsrl");

        Object              service = null;
        BCSSChild           bcsc;
        BeanContextServices bcssp   = getBeanContextServicesPeer();

        synchronized(BeanContext.globalHierarchyLock) {
            synchronized(children) { bcsc = (BCSSChild)children.get(child); }

            if (bcsc == null) throw new IllegalArgumentException("not a child of this context"); // not a child ...

            BCSSServiceProvider bcsssp = services.get(serviceClass);

            if (bcsssp != null) {
                BeanContextServiceProvider bcsp = bcsssp.getServiceProvider();
                service = bcsp.getService(bcssp, requestor, serviceClass, serviceSelector);
                if (service != null) { // do bookkeeping ...
                    try {
                        bcsc.usingService(requestor, service, serviceClass, bcsp, false, bcsrl);
                    } catch (TooManyListenersException tmle) {
                        bcsp.releaseService(bcssp, requestor, service);
                        throw tmle;
                    } catch (UnsupportedOperationException uope) {
                        bcsp.releaseService(bcssp, requestor, service);
                        throw uope; // unchecked rt exception
                    }

                    return service;
                }
            }


            if (proxy != null) {

                // try to delegate ...

                service = proxy.getService(bcssp, requestor, serviceClass, serviceSelector);

                if (service != null) { // do bookkeeping ...
                    try {
                        bcsc.usingService(requestor, service, serviceClass, proxy, true, bcsrl);
                    } catch (TooManyListenersException tmle) {
                        proxy.releaseService(bcssp, requestor, service);
                        throw tmle;
                    } catch (UnsupportedOperationException uope) {
                        proxy.releaseService(bcssp, requestor, service);
                        throw uope; // unchecked rt exception
                    }

                    return service;
                }
            }
        }

        return null;
    }

    /**
     * release a service
     */

    public void releaseService(BeanContextChild child, Object requestor, Object service) {
        if (child     == null) throw new NullPointerException("child");
        if (requestor == null) throw new NullPointerException("requestor");
        if (service   == null) throw new NullPointerException("service");

        BCSSChild bcsc;

        synchronized(BeanContext.globalHierarchyLock) {
                synchronized(children) { bcsc = (BCSSChild)children.get(child); }

                if (bcsc != null)
                    bcsc.releaseService(requestor, service);
                else
                   throw new IllegalArgumentException("child actual is not a child of this BeanContext");
        }
    }

    /**
     * @return an iterator for all the currently registered service classes.
     */

    public Iterator<Object> getCurrentServiceClasses() {
        return new BCSIterator(services.keySet().iterator());
    }

    /**
     * @return an iterator for all the currently available service selectors
     * (if any) available for the specified service.
     */

    public Iterator<?> getCurrentServiceSelectors(Class<?> serviceClass) {

        BCSSServiceProvider bcsssp = services.get(serviceClass);

        return bcsssp != null ? new BCSIterator(bcsssp.getServiceProvider().getCurrentServiceSelectors(getBeanContextServicesPeer(), serviceClass)) : null;
    }

    /**
     * BeanContextServicesListener callback, propagates event to all
     * currently registered listeners and BeanContextServices children,
     * if this BeanContextService does not already implement this service
     * itself.
     *
     * subclasses may override or envelope this method to implement their
     * own propagation semantics.
     */

     public void serviceAvailable(BeanContextServiceAvailableEvent bcssae) {
        synchronized(BeanContext.globalHierarchyLock) {
            if (services.containsKey(bcssae.getServiceClass())) return;

            fireServiceAdded(bcssae);

            Iterator<Object> i;

            synchronized(children) {
                i = children.keySet().iterator();
            }

            while (i.hasNext()) {
                Object c = i.next();

                if (c instanceof BeanContextServices) {
                    ((BeanContextServicesListener)c).serviceAvailable(bcssae);
                }
            }
        }
     }

    /**
     * BeanContextServicesListener callback, propagates event to all
     * currently registered listeners and BeanContextServices children,
     * if this BeanContextService does not already implement this service
     * itself.
     *
     * subclasses may override or envelope this method to implement their
     * own propagation semantics.
     */

    public void serviceRevoked(BeanContextServiceRevokedEvent bcssre) {
        synchronized(BeanContext.globalHierarchyLock) {
            if (services.containsKey(bcssre.getServiceClass())) return;

            fireServiceRevoked(bcssre);

            Iterator<Object> i;

            synchronized(children) {
                i = children.keySet().iterator();
            }

            while (i.hasNext()) {
                Object c = i.next();

                if (c instanceof BeanContextServices) {
                    ((BeanContextServicesListener)c).serviceRevoked(bcssre);
                }
            }
        }
    }

    /**
     * Gets the {@code BeanContextServicesListener} (if any) of the specified
     * child.
     *
     * @param child the specified child
     * @return the BeanContextServicesListener (if any) of the specified child
     */
    protected static final BeanContextServicesListener getChildBeanContextServicesListener(Object child) {
        try {
            return (BeanContextServicesListener)child;
        } catch (ClassCastException cce) {
            return null;
        }
    }

    /**
     * called from superclass child removal operations after a child
     * has been successfully removed. called with child synchronized.
     *
     * This subclass uses this hook to immediately revoke any services
     * being used by this child if it is a BeanContextChild.
     *
     * subclasses may envelope this method in order to implement their
     * own child removal side-effects.
     */

    protected void childJustRemovedHook(Object child, BCSChild bcsc) {
        BCSSChild bcssc = (BCSSChild)bcsc;

        bcssc.cleanupReferences();
    }

    /**
     * called from setBeanContext to notify a BeanContextChild
     * to release resources obtained from the nesting BeanContext.
     *
     * This method revokes any services obtained from its parent.
     *
     * subclasses may envelope this method to implement their own semantics.
     */

    protected synchronized void releaseBeanContextResources() {
        Object[] bcssc;

        super.releaseBeanContextResources();

        synchronized(children) {
            if (children.isEmpty()) return;

            bcssc = children.values().toArray();
        }


        for (int i = 0; i < bcssc.length; i++) {
            ((BCSSChild)bcssc[i]).revokeAllDelegatedServicesNow();
        }

        proxy = null;
    }

    /**
     * called from setBeanContext to notify a BeanContextChild
     * to allocate resources obtained from the nesting BeanContext.
     *
     * subclasses may envelope this method to implement their own semantics.
     */

    protected synchronized void initializeBeanContextResources() {
        super.initializeBeanContextResources();

        BeanContext nbc = getBeanContext();

        if (nbc == null) return;

        try {
            BeanContextServices bcs = (BeanContextServices)nbc;

            proxy = new BCSSProxyServiceProvider(bcs);
        } catch (ClassCastException cce) {
            // do nothing ...
        }
    }

    /**
     * Fires a {@code BeanContextServiceEvent} notifying of a new service.
     * @param serviceClass the service class
     */
    protected final void fireServiceAdded(Class<?> serviceClass) {
        BeanContextServiceAvailableEvent bcssae = new BeanContextServiceAvailableEvent(getBeanContextServicesPeer(), serviceClass);

        fireServiceAdded(bcssae);
    }

    /**
     * Fires a {@code BeanContextServiceAvailableEvent} indicating that a new
     * service has become available.
     *
     * @param bcssae the {@code BeanContextServiceAvailableEvent}
     */
    protected final void fireServiceAdded(BeanContextServiceAvailableEvent bcssae) {
        Object[]                         copy;

        synchronized (bcsListeners) { copy = bcsListeners.toArray(); }

        for (int i = 0; i < copy.length; i++) {
            ((BeanContextServicesListener)copy[i]).serviceAvailable(bcssae);
        }
    }

    /**
     * Fires a {@code BeanContextServiceEvent} notifying of a service being revoked.
     *
     * @param bcsre the {@code BeanContextServiceRevokedEvent}
     */
    protected final void fireServiceRevoked(BeanContextServiceRevokedEvent bcsre) {
        Object[]                         copy;

        synchronized (bcsListeners) { copy = bcsListeners.toArray(); }

        for (int i = 0; i < copy.length; i++) {
            ((BeanContextServiceRevokedListener)copy[i]).serviceRevoked(bcsre);
        }
    }

    /**
     * Fires a {@code BeanContextServiceRevokedEvent}
     * indicating that a particular service is
     * no longer available.
     * @param serviceClass the service class
     * @param revokeNow whether or not the event should be revoked now
     */
    protected final void fireServiceRevoked(Class<?> serviceClass, boolean revokeNow) {
        Object[]                       copy;
        BeanContextServiceRevokedEvent bcsre = new BeanContextServiceRevokedEvent(getBeanContextServicesPeer(), serviceClass, revokeNow);

        synchronized (bcsListeners) { copy = bcsListeners.toArray(); }

        for (int i = 0; i < copy.length; i++) {
            ((BeanContextServicesListener)copy[i]).serviceRevoked(bcsre);
        }
   }

    /**
     * called from BeanContextSupport writeObject before it serializes the
     * children ...
     *
     * This class will serialize any Serializable BeanContextServiceProviders
     * herein.
     *
     * subclasses may envelope this method to insert their own serialization
     * processing that has to occur prior to serialization of the children
     */

    protected synchronized void bcsPreSerializationHook(ObjectOutputStream oos) throws IOException {

        oos.writeInt(serializable);

        if (serializable <= 0) return;

        int count = 0;

        Iterator<Map.Entry<Object, BCSSServiceProvider>> i = services.entrySet().iterator();

        while (i.hasNext() && count < serializable) {
            Map.Entry<Object, BCSSServiceProvider> entry = i.next();
            BCSSServiceProvider bcsp  = null;

             try {
                bcsp = entry.getValue();
             } catch (ClassCastException cce) {
                continue;
             }

             if (bcsp.getServiceProvider() instanceof Serializable) {
                oos.writeObject(entry.getKey());
                oos.writeObject(bcsp);
                count++;
             }
        }

        if (count != serializable)
            throw new IOException("wrote different number of service providers than expected");
    }

    /**
     * called from BeanContextSupport readObject before it deserializes the
     * children ...
     *
     * This class will deserialize any Serializable BeanContextServiceProviders
     * serialized earlier thus making them available to the children when they
     * deserialized.
     *
     * subclasses may envelope this method to insert their own serialization
     * processing that has to occur prior to serialization of the children
     */

    protected synchronized void bcsPreDeserializationHook(ObjectInputStream ois) throws IOException, ClassNotFoundException {

        serializable = ois.readInt();

        int count = serializable;

        while (count > 0) {
            services.put(ois.readObject(), (BCSSServiceProvider)ois.readObject());
            count--;
        }
    }

    /**
     * Serialize the instance.
     *
     * @param  oos the {@code ObjectOutputStream} to write
     * @throws IOException if an I/O error occurs
     */
    @Serial
    private synchronized void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();

        serialize(oos, (Collection)bcsListeners);
    }

    /**
     * Deserialize the instance.
     *
     * @param  ois the {@code ObjectInputStream} to read
     * @throws ClassNotFoundException if the class of a serialized object could
     *         not be found
     * @throws IOException if an I/O error occurs
     */
    @Serial
    private synchronized void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {

        ois.defaultReadObject();

        deserialize(ois, (Collection)bcsListeners);
    }


    /*
     * fields
     */

    /**
     * all accesses to the {@code protected transient HashMap services}
     * field should be synchronized on that object
     */
    protected transient HashMap<Object, BCSSServiceProvider>  services;

    /**
     * The number of instances of a serializable {@code BeanContextServceProvider}.
     */
    protected transient int                      serializable = 0;


    /**
     * Delegate for the {@code BeanContextServiceProvider}.
     */
    protected transient BCSSProxyServiceProvider proxy;


    /**
     * List of {@code BeanContextServicesListener} objects.
     */
    protected transient ArrayList<BeanContextServicesListener> bcsListeners;
}
