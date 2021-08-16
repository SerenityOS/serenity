/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.access.JavaUtilResourceBundleAccess;
import jdk.internal.access.SharedSecrets;
import sun.util.resources.Bundles;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import static sun.security.util.SecurityConstants.GET_CLASSLOADER_PERMISSION;

/**
 * {@code AbstractResourceBundleProvider} is an abstract class that provides
 * the basic support for a provider implementation class for
 * {@link ResourceBundleProvider}.
 *
 * <p>
 * Resource bundles can be packaged in one or more
 * named modules, <em>service provider modules</em>.  The <em>consumer</em> of the
 * resource bundle is the one calling {@link ResourceBundle#getBundle(String)}.
 * In order for the consumer module to load a resource bundle
 * "{@code com.example.app.MyResources}" provided by another module,
 * it will use the {@linkplain java.util.ServiceLoader service loader}
 * mechanism.  A service interface named "{@code com.example.app.spi.MyResourcesProvider}"
 * must be defined and a <em>service provider module</em> will provide an
 * implementation class of "{@code com.example.app.spi.MyResourcesProvider}"
 * as follows:
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
 * Refer to {@link ResourceBundleProvider} for details.
 *
 * @see <a href="../ResourceBundle.html#resource-bundle-modules">
 *      Resource Bundles and Named Modules</a>
 * @since 9
 */
public abstract class AbstractResourceBundleProvider implements ResourceBundleProvider {
    private static final JavaUtilResourceBundleAccess RB_ACCESS =
        SharedSecrets.getJavaUtilResourceBundleAccess();

    private static final String FORMAT_CLASS = "java.class";
    private static final String FORMAT_PROPERTIES = "java.properties";

    private final String[] formats;

    /**
     * Constructs an {@code AbstractResourceBundleProvider} with the
     * "java.properties" format. This constructor is equivalent to
     * {@code AbstractResourceBundleProvider("java.properties")}.
     */
    protected AbstractResourceBundleProvider() {
        this(FORMAT_PROPERTIES);
    }

    /**
     * Constructs an {@code AbstractResourceBundleProvider} with the specified
     * {@code formats}. The {@link #getBundle(String, Locale)} method looks up
     * resource bundles for the given {@code formats}. {@code formats} must
     * be "java.class" or "java.properties".
     *
     * @param formats the formats to be used for loading resource bundles
     * @throws NullPointerException if the given {@code formats} is null
     * @throws IllegalArgumentException if the given {@code formats} is not
     *         "java.class" or "java.properties".
     */
    protected AbstractResourceBundleProvider(String... formats) {
        this.formats = formats.clone();  // defensive copy
        if (this.formats.length == 0) {
            throw new IllegalArgumentException("empty formats");
        }
        for (String f : this.formats) {
            if (!FORMAT_CLASS.equals(f) && !FORMAT_PROPERTIES.equals(f)) {
                throw new IllegalArgumentException(f);
            }
        }
    }

    /**
     * Returns the bundle name for the given {@code baseName} and {@code
     * locale} that this provider provides.
     *
     * @apiNote
     * A resource bundle provider may package its resource bundles in the
     * same package as the base name of the resource bundle if the package
     * is not split among other named modules.  If there are more than one
     * bundle providers providing the resource bundle of a given base name,
     * the resource bundles can be packaged with per-language grouping
     * or per-region grouping to eliminate the split packages.
     *
     * <p>For example, if {@code baseName} is {@code "p.resources.Bundle"} then
     * the resource bundle name of {@code "p.resources.Bundle"} of
     * <code style="white-space:nowrap">Locale("ja", "", "XX")</code>
     * and {@code Locale("en")} could be <code style="white-space:nowrap">
     * "p.resources.ja.Bundle_ja_&thinsp;_XX"</code> and
     * {@code "p.resources.Bundle_en"} respectively.
     *
     * <p> This method is called from the default implementation of the
     * {@link #getBundle(String, Locale)} method.
     *
     * @implNote The default implementation of this method is the same as the
     * implementation of
     * {@link java.util.ResourceBundle.Control#toBundleName(String, Locale)}.
     *
     * @param baseName the base name of the resource bundle, a fully qualified
     *                 class name
     * @param locale   the locale for which a resource bundle should be loaded
     * @return the bundle name for the resource bundle
     */
    protected String toBundleName(String baseName, Locale locale) {
        return ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_DEFAULT)
            .toBundleName(baseName, locale);
    }

    /**
     * Returns a {@code ResourceBundle} for the given {@code baseName} and
     * {@code locale}.
     *
     * @implNote
     * The default implementation of this method calls the
     * {@link #toBundleName(String, Locale) toBundleName} method to get the
     * bundle name for the {@code baseName} and {@code locale} and finds the
     * resource bundle of the bundle name local in the module of this provider.
     * It will only search the formats specified when this provider was
     * constructed.
     *
     * @param baseName the base bundle name of the resource bundle, a fully
     *                 qualified class name.
     * @param locale the locale for which the resource bundle should be instantiated
     * @return {@code ResourceBundle} of the given {@code baseName} and
     *         {@code locale}, or {@code null} if no resource bundle is found
     * @throws NullPointerException if {@code baseName} or {@code locale} is
     *         {@code null}
     * @throws UncheckedIOException if any IO exception occurred during resource
     *         bundle loading
     */
    @Override
    public ResourceBundle getBundle(String baseName, Locale locale) {
        Module module = this.getClass().getModule();
        String bundleName = toBundleName(baseName, locale);
        var bundle = getBundle0(module, bundleName);
        if (bundle == null) {
            var otherBundleName = Bundles.toOtherBundleName(baseName, bundleName, locale);
            if (!bundleName.equals(otherBundleName)) {
                bundle = getBundle0(module, Bundles.toOtherBundleName(baseName, bundleName, locale));
            }
        }
        return bundle;
    }

    private ResourceBundle getBundle0(Module module, String bundleName) {
        ResourceBundle bundle = null;

        for (String format : formats) {
            try {
                if (FORMAT_CLASS.equals(format)) {
                    bundle = loadResourceBundle(module, bundleName);
                } else if (FORMAT_PROPERTIES.equals(format)) {
                    bundle = loadPropertyResourceBundle(module, bundleName);
                }
                if (bundle != null) {
                    break;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return bundle;
    }

    /*
     * Returns the ResourceBundle of .class format if found in the module
     * of this provider.
     */
    private static ResourceBundle loadResourceBundle(Module module, String bundleName)
    {
        PrivilegedAction<Class<?>> pa = () -> Class.forName(module, bundleName);
        @SuppressWarnings("removal")
        Class<?> c = AccessController.doPrivileged(pa, null, GET_CLASSLOADER_PERMISSION);
        if (c != null && ResourceBundle.class.isAssignableFrom(c)) {
            @SuppressWarnings("unchecked")
            Class<ResourceBundle> bundleClass = (Class<ResourceBundle>) c;
            return RB_ACCESS.newResourceBundle(bundleClass);
        }
        return null;
    }

    /*
     * Returns the ResourceBundle of .property format if found in the module
     * of this provider.
     */
    private static ResourceBundle loadPropertyResourceBundle(Module module,
                                                             String bundleName)
        throws IOException
    {
        String resourceName = toResourceName(bundleName, "properties");
        if (resourceName == null) {
            return null;
        }

        PrivilegedAction<InputStream> pa = () -> {
            try {
                return module.getResourceAsStream(resourceName);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        try (@SuppressWarnings("removal") InputStream stream = AccessController.doPrivileged(pa)) {
            if (stream != null) {
                return new PropertyResourceBundle(stream);
            } else {
                return null;
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static String toResourceName(String bundleName, String suffix) {
        if (bundleName.contains("://")) {
            return null;
        }
        StringBuilder sb = new StringBuilder(bundleName.length() + 1 + suffix.length());
        sb.append(bundleName.replace('.', '/')).append('.').append(suffix);
        return sb.toString();
    }

}
