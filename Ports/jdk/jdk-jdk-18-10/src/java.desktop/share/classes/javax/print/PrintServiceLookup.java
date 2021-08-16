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

package javax.print;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import javax.print.attribute.AttributeSet;

import sun.awt.AppContext;

/**
 * Implementations of this class provide lookup services for print services
 * (typically equivalent to printers) of a particular type.
 * <p>
 * Multiple implementations may be installed concurrently. All implementations
 * must be able to describe the located printers as instances of a
 * {@code PrintService}. Typically implementations of this service class are
 * located automatically in JAR files (see the SPI JAR file specification).
 * These classes must be instantiable using a default constructor. Alternatively
 * applications may explicitly register instances at runtime.
 * <p>
 * Applications use only the static methods of this abstract class. The instance
 * methods are implemented by a service provider in a subclass and the
 * unification of the results from all installed lookup classes are reported by
 * the static methods of this class when called by the application.
 * <p>
 * A {@code PrintServiceLookup} implementor is recommended to check for the
 * {@code SecurityManager.checkPrintJobAccess()} to deny access to untrusted
 * code. Following this recommended policy means that untrusted code may not be
 * able to locate any print services. Downloaded applets are the most common
 * example of untrusted code.
 * <p>
 * This check is made on a per lookup service basis to allow flexibility in the
 * policy to reflect the needs of different lookup services.
 * <p>
 * Services which are registered by {@link #registerService(PrintService)} will
 * not be included in lookup results if a security manager is installed and its
 * {@code checkPrintJobAccess()} method denies access.
 */
public abstract class PrintServiceLookup {

    /**
     * Constructor for subclasses to call.
     */
    protected PrintServiceLookup() {}

    /**
     * Contains a lists of services.
     */
    static class Services {

        /**
         * The list of lookup services.
         */
        private ArrayList<PrintServiceLookup> listOfLookupServices = null;

        /**
         * The list of registered services.
         */
        private ArrayList<PrintService> registeredServices = null;
    }

    /**
     * Returns the services from the current appcontext.
     *
     * @return the services
     */
    private static Services getServicesForContext() {
        Services services =
            (Services)AppContext.getAppContext().get(Services.class);
        if (services == null) {
            services = new Services();
            AppContext.getAppContext().put(Services.class, services);
        }
        return services;
    }

    /**
     * Returns the list of lookup services.
     *
     * @return the list of lookup services
     */
    private static ArrayList<PrintServiceLookup> getListOfLookupServices() {
        return getServicesForContext().listOfLookupServices;
    }

    /**
     * Initialize the list of lookup services.
     *
     * @return the list of lookup services
     */
    private static ArrayList<PrintServiceLookup> initListOfLookupServices() {
        ArrayList<PrintServiceLookup> listOfLookupServices = new ArrayList<>();
        getServicesForContext().listOfLookupServices = listOfLookupServices;
        return listOfLookupServices;
    }

    /**
     * Returns the list of registered services.
     *
     * @return the list of registered services
     */
    private static ArrayList<PrintService> getRegisteredServices() {
        return getServicesForContext().registeredServices;
    }

    /**
     * Initialize the list of registered services.
     *
     * @return the list of registered services
     */
    private static ArrayList<PrintService> initRegisteredServices() {
        ArrayList<PrintService> registeredServices = new ArrayList<>();
        getServicesForContext().registeredServices = registeredServices;
        return registeredServices;
    }

    /**
     * Locates print services capable of printing the specified
     * {@link DocFlavor}.
     *
     * @param  flavor the flavor to print. If {@code null}, this constraint is
     *         not used.
     * @param  attributes attributes that the print service must support. If
     *         {@code null} this constraint is not used.
     * @return array of matching {@code PrintService} objects representing print
     *         services that support the specified flavor attributes. If no
     *         services match, the array is zero-length.
     */
    public static final PrintService[]
        lookupPrintServices(DocFlavor flavor,
                            AttributeSet attributes) {
        ArrayList<PrintService> list = getServices(flavor, attributes);
        return list.toArray(new PrintService[list.size()]);
    }

    /**
     * Locates {@code MultiDoc} print {@code Services} capable of printing
     * {@code MultiDocs} containing all the specified doc flavors.
     * <p>
     * This method is useful to help locate a service that can print a
     * {@code MultiDoc} in which the elements may be different flavors. An
     * application could perform this itself by multiple lookups on each
     * {@code DocFlavor} in turn and collating the results, but the lookup
     * service may be able to do this more efficiently.
     *
     * @param  flavors the flavors to print. If {@code null} or empty this
     *         constraint is not used. Otherwise return only multidoc print
     *         services that can print all specified doc flavors.
     * @param  attributes attributes that the print service must support. If
     *         {@code null} this constraint is not used.
     * @return array of matching {@link MultiDocPrintService} objects. If no
     *         services match, the array is zero-length.
     */
    public static final MultiDocPrintService[]
        lookupMultiDocPrintServices(DocFlavor[] flavors,
                                    AttributeSet attributes) {
        ArrayList<MultiDocPrintService> list = getMultiDocServices(flavors, attributes);
        return list.toArray(new MultiDocPrintService[list.size()]);
    }

    /**
     * Locates the default print service for this environment. This may return
     * {@code null}. If multiple lookup services each specify a default, the
     * chosen service is not precisely defined, but a platform native service,
     * rather than an installed service, is usually returned as the default. If
     * there is no clearly identifiable platform native default print service,
     * the default is the first to be located in an implementation-dependent
     * manner.
     * <p>
     * This may include making use of any preferences API that is available as
     * part of the Java or native platform. This algorithm may be overridden by
     * a user setting the property {@code javax.print.defaultPrinter}. A service
     * specified must be discovered to be valid and currently available to be
     * returned as the default.
     *
     * @return the default {@code PrintService}
     */
    public static final PrintService lookupDefaultPrintService() {

        Iterator<PrintServiceLookup> psIterator = getAllLookupServices().iterator();
        while (psIterator.hasNext()) {
            try {
                PrintServiceLookup lus = psIterator.next();
                PrintService service = lus.getDefaultPrintService();
                if (service != null) {
                    return service;
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    /**
     * Allows an application to explicitly register a class that implements
     * lookup services. The registration will not persist across VM invocations.
     * This is useful if an application needs to make a new service available
     * that is not part of the installation. If the lookup service is already
     * registered, or cannot be registered, the method returns {@code false}.
     *
     * @param  sp an implementation of a lookup service
     * @return {@code true} if the new lookup service is newly registered;
     *         {@code false} otherwise
     */
    public static boolean registerServiceProvider(PrintServiceLookup sp) {
        synchronized (PrintServiceLookup.class) {
            Iterator<PrintServiceLookup> psIterator =
                getAllLookupServices().iterator();
            while (psIterator.hasNext()) {
                try {
                    Object lus = psIterator.next();
                    if (lus.getClass() == sp.getClass()) {
                        return false;
                    }
                } catch (Exception e) {
                }
            }
            getListOfLookupServices().add(sp);
            return true;
        }
    }

    /**
     * Allows an application to directly register an instance of a class which
     * implements a print service. The lookup operations for this service will
     * be performed by the {@code PrintServiceLookup} class using the attribute
     * values and classes reported by the service. This may be less efficient
     * than a lookup service tuned for that service. Therefore registering a
     * {@code PrintServiceLookup} instance instead is recommended. The method
     * returns {@code true} if this service is not previously registered and is
     * now successfully registered. This method should not be called with
     * {@code StreamPrintService} instances. They will always fail to register
     * and the method will return {@code false}.
     *
     * @param  service an implementation of a print service
     * @return {@code true} if the service is newly registered; {@code false}
     *         otherwise
     */
    public static boolean registerService(PrintService service) {
        synchronized (PrintServiceLookup.class) {
            if (service == null || service instanceof StreamPrintService) {
                return false;
            }
            ArrayList<PrintService> registeredServices = getRegisteredServices();
            if (registeredServices == null) {
                registeredServices = initRegisteredServices();
            }
            else {
              if (registeredServices.contains(service)) {
                return false;
              }
            }
            registeredServices.add(service);
            return true;
        }
    }

    /**
     * Locates services that can be positively confirmed to support the
     * combination of attributes and {@code DocFlavors} specified. This method
     * is not called directly by applications.
     * <p>
     * Implemented by a service provider, used by the static methods of this
     * class.
     * <p>
     * The results should be the same as obtaining all the {@code PrintServices}
     * and querying each one individually on its support for the specified
     * attributes and flavors, but the process can be more efficient by taking
     * advantage of the capabilities of lookup services for the print services.
     *
     * @param  flavor of document required. If {@code null} it is ignored.
     * @param  attributes required to be supported. If {@code null} this
     *         constraint is not used.
     * @return array of matching {@code PrintServices}. If no services match,
     *         the array is zero-length.
     */
    public abstract PrintService[] getPrintServices(DocFlavor flavor,
                                                    AttributeSet attributes);

    /**
     * Not called directly by applications. Implemented by a service provider,
     * used by the static methods of this class.
     *
     * @return array of all {@code PrintServices} known to this lookup service
     *         class. If none are found, the array is zero-length.
     */
    public abstract PrintService[] getPrintServices() ;

    /**
     * Not called directly by applications.
     * <p>
     * Implemented by a service provider, used by the static methods of this
     * class.
     * <p>
     * Locates {@code MultiDoc} print services which can be positively confirmed
     * to support the combination of attributes and {@code DocFlavors}
     * specified.
     *
     * @param  flavors of documents required. If {@code null} or empty it is
     *         ignored.
     * @param  attributes required to be supported. If {@code null} this
     *         constraint is not used.
     * @return array of matching {@code PrintServices}. If no services match,
     *         the array is zero-length.
     */
    public abstract MultiDocPrintService[]
        getMultiDocPrintServices(DocFlavor[] flavors,
                                 AttributeSet attributes);

    /**
     * Not called directly by applications. Implemented by a service provider,
     * and called by the print lookup service.
     *
     * @return the default {@code PrintService} for this lookup service. If
     *         there is no default, returns {@code null}.
     */
    public abstract PrintService getDefaultPrintService();

    /**
     * Returns all lookup services for this environment.
     *
     * @return all lookup services for this environment
     */
    @SuppressWarnings("removal")
    private static ArrayList<PrintServiceLookup> getAllLookupServices() {
        synchronized (PrintServiceLookup.class) {
            ArrayList<PrintServiceLookup> listOfLookupServices = getListOfLookupServices();
            if (listOfLookupServices != null) {
                return listOfLookupServices;
            } else {
                listOfLookupServices = initListOfLookupServices();
            }
            try {
                java.security.AccessController.doPrivileged(
                     new java.security.PrivilegedExceptionAction<Object>() {
                        public Object run() {
                            Iterator<PrintServiceLookup> iterator =
                                ServiceLoader.load(PrintServiceLookup.class).
                                iterator();
                            ArrayList<PrintServiceLookup> los = getListOfLookupServices();
                            while (iterator.hasNext()) {
                                try {
                                    los.add(iterator.next());
                                }  catch (ServiceConfigurationError err) {
                                    /* In the applet case, we continue */
                                    if (System.getSecurityManager() != null) {
                                        err.printStackTrace();
                                    } else {
                                        throw err;
                                    }
                                }
                            }
                            return null;
                        }
                });
            } catch (java.security.PrivilegedActionException e) {
            }

            return listOfLookupServices;
        }
    }

    /**
     * Locates print services capable of printing the specified
     * {@link DocFlavor}.
     *
     * @param  flavor the flavor to print. If {@code null}, this constraint is
     *         not used.
     * @param  attributes attributes that the print service must support. If
     *         {@code null} this constraint is not used.
     * @return list of matching {@code PrintService} objects representing print
     *         services that support the specified flavor attributes. If no
     *         services match, the empty list is returned.
     */
    private static ArrayList<PrintService> getServices(DocFlavor flavor,
                                                       AttributeSet attributes) {

        ArrayList<PrintService> listOfServices = new ArrayList<>();
        Iterator<PrintServiceLookup> psIterator = getAllLookupServices().iterator();
        while (psIterator.hasNext()) {
            try {
                PrintServiceLookup lus = psIterator.next();
                PrintService[] services=null;
                if (flavor == null && attributes == null) {
                    try {
                    services = lus.getPrintServices();
                    } catch (Throwable tr) {
                    }
                } else {
                    services = lus.getPrintServices(flavor, attributes);
                }
                if (services == null) {
                    continue;
                }
                for (int i=0; i<services.length; i++) {
                    listOfServices.add(services[i]);
                }
            } catch (Exception e) {
            }
        }
        /*
         * add any directly registered services
         */
        ArrayList<PrintService> registeredServices = null;
        try {
          @SuppressWarnings("removal")
          SecurityManager security = System.getSecurityManager();
          if (security != null) {
            security.checkPrintJobAccess();
          }
          registeredServices = getRegisteredServices();
        } catch (SecurityException se) {
        }
        if (registeredServices != null) {
            PrintService[] services = registeredServices.toArray(
                           new PrintService[registeredServices.size()]);
            for (int i=0; i<services.length; i++) {
                if (!listOfServices.contains(services[i])) {
                    if (flavor == null && attributes == null) {
                        listOfServices.add(services[i]);
                    } else if (((flavor != null &&
                                 services[i].isDocFlavorSupported(flavor)) ||
                                flavor == null) &&
                               null == services[i].getUnsupportedAttributes(
                                                      flavor, attributes)) {
                        listOfServices.add(services[i]);
                    }
                }
            }
        }
        return listOfServices;
    }

    /**
     * Locates {@code MultiDoc} print {@code Services} capable of printing
     * {@code MultiDocs} containing all the specified doc flavors.
     *
     * @param  flavors the flavors to print. If {@code null} or empty this
     *         constraint is not used. Otherwise return only multidoc print
     *         services that can print all specified doc flavors.
     * @param  attributes attributes that the print service must support. If
     *         {@code null} this constraint is not used.
     * @return list of matching {@link MultiDocPrintService} objects. If no
     *         services match, the empty list is returned.
     */
    private static ArrayList<MultiDocPrintService> getMultiDocServices(DocFlavor[] flavors,
                                                                       AttributeSet attributes) {


        ArrayList<MultiDocPrintService> listOfServices = new ArrayList<>();
        Iterator<PrintServiceLookup> psIterator = getAllLookupServices().iterator();
        while (psIterator.hasNext()) {
            try {
                PrintServiceLookup lus = psIterator.next();
                MultiDocPrintService[] services  =
                    lus.getMultiDocPrintServices(flavors, attributes);
                if (services == null) {
                    continue;
                }
                for (int i=0; i<services.length; i++) {
                    listOfServices.add(services[i]);
                }
            } catch (Exception e) {
            }
        }
        /*
         * add any directly registered services
         */
        ArrayList<PrintService> registeredServices = null;
        try {
          @SuppressWarnings("removal")
          SecurityManager security = System.getSecurityManager();
          if (security != null) {
            security.checkPrintJobAccess();
          }
          registeredServices = getRegisteredServices();
        } catch (Exception e) {
        }
        if (registeredServices != null) {
            PrintService[] services =
                registeredServices.toArray(new PrintService[registeredServices.size()]);
            for (int i=0; i<services.length; i++) {
                if (services[i] instanceof MultiDocPrintService &&
                    !listOfServices.contains(services[i])) {
                    if (flavors == null || flavors.length == 0) {
                        listOfServices.add((MultiDocPrintService)services[i]);
                    } else {
                        boolean supported = true;
                        for (int f=0; f<flavors.length; f++) {
                            if (services[i].isDocFlavorSupported(flavors[f])) {

                                if (services[i].getUnsupportedAttributes(
                                     flavors[f], attributes) != null) {
                                        supported = false;
                                        break;
                                }
                            } else {
                                supported = false;
                                break;
                            }
                        }
                        if (supported) {
                            listOfServices.add((MultiDocPrintService)services[i]);
                        }
                    }
                }
            }
        }
        return listOfServices;
    }
}
