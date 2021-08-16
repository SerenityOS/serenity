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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import javax.print.attribute.PrintRequestAttributeSet;

import sun.awt.AppContext;

/**
 * A {@code StreamPrintServiceFactory} is the factory for
 * {@link StreamPrintService} instances, which can print to an output stream in
 * a particular document format described as a mime type. A typical output
 * document format may be Postscript(TM).
 * <p>
 * This class is implemented by a service and located by the implementation
 * using the {@link ServiceLoader} facility.
 * <p>
 * Applications locate instances of this class by calling the
 * {@link #lookupStreamPrintServiceFactories(DocFlavor, String)} method.
 * <p>
 * Applications can use a {@code StreamPrintService} obtained from a factory in
 * place of a {@code PrintService} which represents a physical printer device.
 */
public abstract class StreamPrintServiceFactory {

    /**
     * Constructor for subclasses to call.
     */
     protected StreamPrintServiceFactory() {}

    /**
     * Contains a list of factories.
     */
    static class Services {

        /**
         * The list of factories which will be stored per appcontext.
         */
        private ArrayList<StreamPrintServiceFactory> listOfFactories = null;
    }

    /**
     * Returns the services from the current appcontext.
     *
     * @return the services
     */
    private static Services getServices() {
        Services services =
            (Services)AppContext.getAppContext().get(Services.class);
        if (services == null) {
            services = new Services();
            AppContext.getAppContext().put(Services.class, services);
        }
        return services;
    }

    /**
     * Returns the list of factories.
     *
     * @return the list of factories
     */
    private static ArrayList<StreamPrintServiceFactory> getListOfFactories() {
        return getServices().listOfFactories;
    }

    /**
     * Initialize the list of factories.
     *
     * @return the list of factories
     */
    private static ArrayList<StreamPrintServiceFactory> initListOfFactories() {
        ArrayList<StreamPrintServiceFactory> listOfFactories = new ArrayList<>();
        getServices().listOfFactories = listOfFactories;
        return listOfFactories;
    }

    /**
     * Locates factories for print services that can be used with a print job to
     * output a stream of data in the format specified by
     * {@code outputMimeType}.
     * <p>
     * The {@code outputMimeType} parameter describes the document type that you
     * want to create, whereas the {@code flavor} parameter describes the format
     * in which the input data will be provided by the application to the
     * {@code StreamPrintService}.
     * <p>
     * Although {@code null} is an acceptable value to use in the lookup of
     * stream printing services, it's typical to search for a particular desired
     * format, such as Postscript(TM).
     *
     * @param  flavor of the input document type - {@code null} means match all
     *         types
     * @param  outputMimeType representing the required output format, used to
     *         identify suitable stream printer factories. A value of
     *         {@code null} means match all formats.
     * @return matching factories for stream print service instance, empty if no
     *         suitable factories could be located
     */
     public static StreamPrintServiceFactory[]
         lookupStreamPrintServiceFactories(DocFlavor flavor,
                                           String outputMimeType) {

         ArrayList<StreamPrintServiceFactory> list = getFactories(flavor, outputMimeType);
         return list.toArray(new StreamPrintServiceFactory[list.size()]);
     }

    /**
     * Queries the factory for the document format that is emitted by printers
     * obtained from this factory.
     *
     * @return the output format described as a mime type
     */
    public abstract String getOutputFormat();

    /**
     * Queries the factory for the document flavors that can be accepted by
     * printers obtained from this factory.
     *
     * @return array of supported doc flavors
     */
    public abstract DocFlavor[] getSupportedDocFlavors();

    /**
     * Returns a {@code StreamPrintService} that can print to the specified
     * output stream. The output stream is created and managed by the
     * application. It is the application's responsibility to close the stream
     * and to ensure that this {@code Printer} is not reused. The application
     * should not close this stream until any print job created from the printer
     * is complete. Doing so earlier may generate a {@code PrinterException} and
     * an event indicating that the job failed.
     * <p>
     * Whereas a {@code PrintService} connected to a physical printer can be
     * reused, a {@code StreamPrintService} connected to a stream cannot. The
     * underlying {@code StreamPrintService} may be disposed by the print system
     * with the {@link StreamPrintService#dispose() dispose} method before
     * returning from the
     * {@link DocPrintJob#print(Doc, PrintRequestAttributeSet) print} method of
     * {@code DocPrintJob} so that the print system knows this printer is no
     * longer usable. This is equivalent to a physical printer going offline -
     * permanently. Applications may supply a {@code null} print stream to
     * create a queryable service. It is not valid to create a {@code PrintJob}
     * for such a stream. Implementations which allocate resources on
     * construction should examine the stream and may wish to only allocate
     * resources if the stream is {@code non-null}.
     *
     * @param  out destination stream for generated output
     * @return a {@code PrintService} which will generate the format specified
     *         by the {@code DocFlavor} supported by this factory
     */
    public abstract StreamPrintService getPrintService(OutputStream out);

    /**
     * Returns all factories for print services.
     *
     * @return all factories
     */
    @SuppressWarnings("removal")
    private static ArrayList<StreamPrintServiceFactory> getAllFactories() {
        synchronized (StreamPrintServiceFactory.class) {

          ArrayList<StreamPrintServiceFactory> listOfFactories = getListOfFactories();
            if (listOfFactories != null) {
                return listOfFactories;
            } else {
                listOfFactories = initListOfFactories();
            }

            try {
                java.security.AccessController.doPrivileged(
                     new java.security.PrivilegedExceptionAction<Object>() {
                        public Object run() {
                            Iterator<StreamPrintServiceFactory> iterator =
                                ServiceLoader.load
                                (StreamPrintServiceFactory.class).iterator();
                            ArrayList<StreamPrintServiceFactory> lof = getListOfFactories();
                            while (iterator.hasNext()) {
                                try {
                                    lof.add(iterator.next());
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
            return listOfFactories;
        }
    }

    /**
     * Checks if the array of {@code flavors} contains the {@code flavor}
     * object.
     *
     * @param  flavor the flavor
     * @param  flavors the array of flavors
     * @return {@code true} if {@code flavors} contains the {@code flavor}
     *         object; {@code false} otherwise
     */
    private static boolean isMember(DocFlavor flavor, DocFlavor[] flavors) {
        for (int f=0; f<flavors.length; f++ ) {
            if (flavor.equals(flavors[f])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Utility method for {@link #lookupStreamPrintServiceFactories}.
     * <p>
     * Locates factories for print services that can be used with a print job to
     * output a stream of data in the format specified by
     * {@code outputMimeType}.
     *
     * @param  flavor of the input document type - {@code null} means match all
     *         types
     * @param  outType representing the required output format, used to identify
     *         suitable stream printer factories. A value of {@code null} means
     *         match all formats.
     * @return matching factories for stream print service instance, empty if no
     *         suitable factories could be located
     */
    private static ArrayList<StreamPrintServiceFactory> getFactories(DocFlavor flavor, String outType) {

        if (flavor == null && outType == null) {
            return getAllFactories();
        }

        ArrayList<StreamPrintServiceFactory> list = new ArrayList<>();
        for (StreamPrintServiceFactory factory : getAllFactories()) {
            if ((outType == null ||
                 outType.equalsIgnoreCase(factory.getOutputFormat())) &&
                (flavor == null ||
                 isMember(flavor, factory.getSupportedDocFlavors()))) {
                list.add(factory);
            }
        }

        return list;
    }
}
