/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.util.spi;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * {@code ResourceBundleProvider} is a service provider interface for
 * resource bundles. It is used by
 * {@link ResourceBundle#getBundle(String) ResourceBundle.getBundle}
 * factory methods to locate and load the service providers that are deployed as
 * modules via {@link java.util.ServiceLoader ServiceLoader}.
 *
 * <h2>Developing resource bundle services</h2>
 *
 * A service for a resource bundle of a given <em>{@code baseName}</em> must have
 * a fully-qualified class name of the form:
 * <blockquote>
 * {@code <package of baseName> + ".spi." + <simple name of baseName> + "Provider"}
 * </blockquote>
 *
 * The service type is in a {@code spi} subpackage as it may be packaged in
 * a module separate from the resource bundle providers.
 * For example, the service for a resource bundle named
 * {@code com.example.app.MyResources} must be
 * {@code com.example.app.spi.MyResourcesProvider}:
 *
 * <blockquote><pre>
 * {@code package com.example.app.spi;
 * public interface MyResourcesProvider extends ResourceBundleProvider {
 * }
 * }</pre></blockquote>
 *
 * <h2>Deploying resource bundle service providers</h2>
 *
 * Resource bundles can be deployed in one or more service providers
 * in modules.  For example, a provider for a service
 * named "{@code com.example.app.spi.MyResourcesProvider}"
 * has the following implementation class:
 *
 * <blockquote><pre>
 * {@code import com.example.app.spi.MyResourcesProvider;
 * class MyResourcesProviderImpl extends AbstractResourceBundleProvider
 *     implements MyResourcesProvider
 * {
 *     public MyResourcesProviderImpl() {
 *         super("java.properties");
 *     }
 *     // this provider maps the resource bundle to per-language package
 *     protected String toBundleName(String baseName, Locale locale) {
 *         return "p." + locale.getLanguage() + "." + baseName;
 *     }
 *
 *     public ResourceBundle getBundle(String baseName, Locale locale) {
 *         // this module only provides bundles in French
 *         if (locale.equals(Locale.FRENCH)) {
 *              return super.getBundle(baseName, locale);
 *         }
 *         // otherwise return null
 *         return null;
 *     }
 * }}</pre></blockquote>
 *
 * This example provides "{@code com.example.app.MyResources}"
 * resource bundle of the French locale.  Traditionally resource bundles of
 * all locales are packaged in the same package as the resource bundle base name.
 * When deploying resource bundles in more than one modules and two modules
 * containing a package of the same name, <em>split package</em>,
 * is not supported, resource bundles in each module can be packaged in
 * a different package as shown in this example where this provider packages
 * the resource bundles in per-language package, i.e. {@code com.example.app.fr}
 * for French locale.
 *
 * <p> A provider can provide more than one services, each of which is a service
 * for a resource bundle of a different base name.
 *
 * <p>{@link AbstractResourceBundleProvider}
 * provides the basic implementation for {@code ResourceBundleProvider}
 * and a subclass can override the {@link
 * AbstractResourceBundleProvider#toBundleName(String, Locale) toBundleName}
 * method to return a provider-specific location of the resource to be loaded,
 * for example, per-language package.
 * A provider can override {@link #getBundle ResourceBundleProvider.getBundle}
 * method for example to only search the known supported locales or
 * return resource bundles in other formats such as XML.
 *
 * <p>The module declaration of this provider module specifies the following
 * directive:
 * <pre>
 *     provides com.example.app.spi.MyResourcesProvider with com.example.impl.MyResourcesProviderImpl;
 * </pre>
 *
 * <h2><a id="obtain-resource-bundle">Obtaining resource bundles from providers</a></h2>
 *
 * The module declaration of the <em>consumer module</em> that calls one of the
 * {@code ResourceBundle.getBundle} factory methods to obtain a resource
 * bundle from service providers must specify the following directive:
 * <pre>
 *     uses com.example.app.spi.MyResourcesProvider;
 * </pre>
 *
 * {@link ResourceBundle#getBundle(String, Locale)
 * ResourceBundle.getBundle("com.example.app.MyResource", locale)}
 * locates and loads the providers for {@code com.example.app.spi.MyResourcesProvider}
 * service and then invokes {@link #getBundle(String, Locale)
 * ResourceBundleProvider.getBundle("com.example.app.MyResource", locale)} to
 * find the resource bundle of the given base name and locale.
 * If the consumer module is a resource bundle service provider for
 * {@code com.example.app.spi.MyResourcesProvider}, {@code ResourceBundle.getBundle}
 * will locate resource bundles only from service providers.
 * Otherwise, {@code ResourceBundle.getBundle} may continue the search of
 * the resource bundle in other modules and class path per the specification
 * of the {@code ResourceBundle.getBundle} method being called.
 *
 * @see AbstractResourceBundleProvider
 * @see <a href="../ResourceBundle.html#resource-bundle-modules">
 *      Resource Bundles and Named Modules</a>
 * @see java.util.ServiceLoader
 * @since 9
 */
public interface ResourceBundleProvider {
    /**
     * Returns a {@code ResourceBundle} for the given bundle name and locale.
     * This method returns {@code null} if there is no {@code ResourceBundle}
     * found for the given parameters.
     *
     *
     * @param baseName
     *        the base bundle name of the resource bundle, a fully
     *        qualified class name
     * @param locale
     *        the locale for which the resource bundle should be loaded
     * @return the ResourceBundle created for the given parameters, or null if no
     *         {@code ResourceBundle} for the given parameters is found
     */
    public ResourceBundle getBundle(String baseName, Locale locale);
}
