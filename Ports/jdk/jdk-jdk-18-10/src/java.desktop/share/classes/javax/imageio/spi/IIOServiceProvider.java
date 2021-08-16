/*
 * Copyright (c) 2000, 2004, Oracle and/or its affiliates. All rights reserved.
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

package javax.imageio.spi;

import java.util.Locale;
import javax.imageio.spi.RegisterableService;
import javax.imageio.spi.ServiceRegistry;

/**
 * A superinterface for functionality common to all Image I/O service
 * provider interfaces (SPIs).  For more information on service
 * provider classes, see the class comment for the
 * {@code IIORegistry} class.
 *
 * @see IIORegistry
 * @see javax.imageio.spi.ImageReaderSpi
 * @see javax.imageio.spi.ImageWriterSpi
 * @see javax.imageio.spi.ImageTranscoderSpi
 * @see javax.imageio.spi.ImageInputStreamSpi
 * @see javax.imageio.spi.ImageOutputStreamSpi
 */
public abstract class IIOServiceProvider implements RegisterableService {

    /**
     * A {@code String} to be returned from
     * {@code getVendorName}, initially {@code null}.
     * Constructors should set this to a non-{@code null} value.
     */
    protected String vendorName;

    /**
     * A {@code String} to be returned from
     * {@code getVersion}, initially null.  Constructors should
     * set this to a non-{@code null} value.
     */
    protected String version;

    /**
     * Constructs an {@code IIOServiceProvider} with a given
     * vendor name and version identifier.
     *
     * @param vendorName the vendor name.
     * @param version a version identifier.
     *
     * @exception IllegalArgumentException if {@code vendorName}
     * is {@code null}.
     * @exception IllegalArgumentException if {@code version}
     * is {@code null}.
     */
    public IIOServiceProvider(String vendorName,
                              String version) {
        if (vendorName == null) {
            throw new IllegalArgumentException("vendorName == null!");
        }
        if (version == null) {
            throw new IllegalArgumentException("version == null!");
        }
        this.vendorName = vendorName;
        this.version = version;
    }

    /**
     * Constructs a blank {@code IIOServiceProvider}.  It is up
     * to the subclass to initialize instance variables and/or
     * override method implementations in order to ensure that the
     * {@code getVendorName} and {@code getVersion} methods
     * will return non-{@code null} values.
     */
    public IIOServiceProvider() {
    }

    /**
     * A callback that will be called exactly once after the Spi class
     * has been instantiated and registered in a
     * {@code ServiceRegistry}.  This may be used to verify that
     * the environment is suitable for this service, for example that
     * native libraries can be loaded.  If the service cannot function
     * in the environment where it finds itself, it should deregister
     * itself from the registry.
     *
     * <p> Only the registry should call this method.
     *
     * <p> The default implementation does nothing.
     *
     * @see ServiceRegistry#registerServiceProvider(Object provider)
     */
    public void onRegistration(ServiceRegistry registry,
                               Class<?> category) {}

    /**
     * A callback that will be whenever the Spi class has been
     * deregistered from a {@code ServiceRegistry}.
     *
     * <p> Only the registry should call this method.
     *
     * <p> The default implementation does nothing.
     *
     * @see ServiceRegistry#deregisterServiceProvider(Object provider)
     */
    public void onDeregistration(ServiceRegistry registry,
                                 Class<?> category) {}

    /**
     * Returns the name of the vendor responsible for creating this
     * service provider and its associated implementation.  Because
     * the vendor name may be used to select a service provider,
     * it is not localized.
     *
     * <p> The default implementation returns the value of the
     * {@code vendorName} instance variable.
     *
     * @return a non-{@code null String} containing
     * the name of the vendor.
     */
    public String getVendorName() {
        return vendorName;
    }

    /**
     * Returns a string describing the version
     * number of this service provider and its associated
     * implementation.  Because the version may be used by transcoders
     * to identify the service providers they understand, this method
     * is not localized.
     *
     * <p> The default implementation returns the value of the
     * {@code version} instance variable.
     *
     * @return a non-{@code null String} containing
     * the version of this service provider.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns a brief, human-readable description of this service
     * provider and its associated implementation.  The resulting
     * string should be localized for the supplied
     * {@code Locale}, if possible.
     *
     * @param locale a {@code Locale} for which the return value
     * should be localized.
     *
     * @return a {@code String} containing a description of this
     * service provider.
     */
    public abstract String getDescription(Locale locale);
}
