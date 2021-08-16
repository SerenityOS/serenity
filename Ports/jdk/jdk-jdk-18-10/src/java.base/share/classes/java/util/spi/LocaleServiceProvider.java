/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * <p>
 * This is the super class of all the locale sensitive service provider
 * interfaces (SPIs).
 * <p>
 * Locale sensitive  service provider interfaces are interfaces that
 * correspond to locale sensitive classes in the {@code java.text}
 * and {@code java.util} packages. The interfaces enable the
 * construction of locale sensitive objects and the retrieval of
 * localized names for these packages. Locale sensitive factory methods
 * and methods for name retrieval in the {@code java.text} and
 * {@code java.util} packages use implementations of the provider
 * interfaces to offer support for locales beyond the set of locales
 * supported by the Java runtime environment itself.
 *
 * <h2>Packaging of Locale Sensitive Service Provider Implementations</h2>
 * Implementations of these locale sensitive services can be made available
 * by adding them to the application's class path. A provider identifies itself with a
 * provider-configuration file in the resource directory META-INF/services,
 * using the fully qualified provider interface class name as the file name.
 * The file should contain a list of fully-qualified concrete provider class names,
 * one per line. A line is terminated by any one of a line feed ('\n'), a carriage
 * return ('\r'), or a carriage return followed immediately by a line feed. Space
 * and tab characters surrounding each name, as well as blank lines, are ignored.
 * The comment character is '#' ('\u0023'); on each line all characters following
 * the first comment character are ignored. The file must be encoded in UTF-8.
 * <p>
 * If a particular concrete provider class is named in more than one configuration
 * file, or is named in the same configuration file more than once, then the
 * duplicates will be ignored. The configuration file naming a particular provider
 * need not be in the same jar file or other distribution unit as the provider itself.
 * The provider must be accessible from the same class loader that was initially
 * queried to locate the configuration file; this is not necessarily the class loader
 * that loaded the file.
 * <p>
 * For example, an implementation of the
 * {@link java.text.spi.DateFormatProvider DateFormatProvider} class should
 * take the form of a jar file which contains the file:
 * <pre>
 * META-INF/services/java.text.spi.DateFormatProvider
 * </pre>
 * And the file {@code java.text.spi.DateFormatProvider} should have
 * a line such as:
 * <pre>
 * {@code com.foo.DateFormatProviderImpl}
 * </pre>
 * which is the fully qualified class name of the class implementing
 * {@code DateFormatProvider}.
 * <h3>Invocation of Locale Sensitive Services</h3>
 * <p>
 * Locale sensitive factory methods and methods for name retrieval in the
 * {@code java.text} and {@code java.util} packages invoke
 * service provider methods when needed to support the requested locale.
 * The methods first check whether the Java runtime environment itself
 * supports the requested locale, and use its support if available.
 * Otherwise, they call the {@link #isSupportedLocale(Locale) isSupportedLocale}
 * methods of installed providers for the appropriate interface to find one that
 * supports the requested locale. If such a provider is found, its other
 * methods are called to obtain the requested object or name.  When checking
 * whether a locale is supported, the <a href="../Locale.html#def_extensions">
 * locale's extensions</a> are ignored by default. (If locale's extensions should
 * also be checked, the {@code isSupportedLocale} method must be overridden.)
 * If neither the Java runtime environment itself nor an installed provider
 * supports the requested locale, the methods go through a list of candidate
 * locales and repeat the availability check for each until a match is found.
 * The algorithm used for creating a list of candidate locales is same as
 * the one used by {@code ResourceBundle} by default (see
 * {@link java.util.ResourceBundle.Control#getCandidateLocales getCandidateLocales}
 * for the details).  Even if a locale is resolved from the candidate list,
 * methods that return requested objects or names are invoked with the original
 * requested locale including {@code Locale} extensions. The Java runtime
 * environment must support the root locale for all locale sensitive services in
 * order to guarantee that this process terminates.
 * <p>
 * Providers of names (but not providers of other objects) are allowed to
 * return null for some name requests even for locales that they claim to
 * support by including them in their return value for
 * {@code getAvailableLocales}. Similarly, the Java runtime
 * environment itself may not have all names for all locales that it
 * supports. This is because the sets of objects for which names are
 * requested can be large and vary over time, so that it's not always
 * feasible to cover them completely. If the Java runtime environment or a
 * provider returns null instead of a name, the lookup will proceed as
 * described above as if the locale was not supported.
 * <p>
 * The search order of locale sensitive services can
 * be configured by using the {@systemProperty java.locale.providers} system property.
 * This system property declares the user's preferred order for looking up
 * the locale sensitive services separated by a comma. It is only read at
 * the Java runtime startup, so the later call to System.setProperty() won't
 * affect the order.
 * <p>
 * Java Runtime Environment provides the following four locale providers:
 * <ul>
 * <li> "CLDR": A provider based on Unicode Consortium's
 * <a href="http://cldr.unicode.org/">CLDR Project</a>.
 * <li> "COMPAT": represents the locale sensitive services that is compatible
 * with the prior JDK releases up to JDK8 (same as JDK8's "JRE").
 * <li> "SPI": represents the locale sensitive services implementing the subclasses of
 * this {@code LocaleServiceProvider} class.
 * <li> "HOST": A provider that reflects the user's custom settings in the
 * underlying operating system. This provider may not be available, depending
 * on the Java Runtime Environment implementation.
 * <li> "JRE": represents a synonym to "COMPAT". This name
 * is deprecated and will be removed in the future release of JDK.
 * </ul>
 * <p>
 * For example, if the following is specified in the property:
 * <pre>
 * java.locale.providers=SPI,CLDR,COMPAT
 * </pre>
 * the locale sensitive services in the SPI providers are looked up first. If the
 * desired locale sensitive service is not available, then the runtime looks for CLDR,
 * COMPAT in that order.
 * <p>
 * The default order for looking up the preferred locale providers is "CLDR,COMPAT",
 * so specifying "CLDR,COMPAT" is identical to the default behavior. Applications which
 * require implementations of the locale sensitive services must explicitly specify
 * "SPI" in order for the Java runtime to load them from the classpath.
 *
 * @since        1.6
 */
public abstract class LocaleServiceProvider {

    private static Void checkPermission() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("localeServiceProvider"));
        }
        return null;
    }
    private LocaleServiceProvider(Void ignore) { }

    /**
     * Initializes a new locale service provider.
     *
     * @throws  SecurityException
     *          If a security manager has been installed and it denies
     *          {@link RuntimePermission RuntimePermission("localeServiceProvider")}
     */
    protected LocaleServiceProvider() {
        this(checkPermission());
    }

    /**
     * Returns an array of all locales for which this locale service provider
     * can provide localized objects or names. This information is used to
     * compose {@code getAvailableLocales()} values of the locale-dependent
     * services, such as {@code DateFormat.getAvailableLocales()}.
     *
     * <p>The array returned by this method should not include two or more
     * {@code Locale} objects only differing in their extensions.
     *
     * @return An array of all locales for which this locale service provider
     * can provide localized objects or names.
     */
    public abstract Locale[] getAvailableLocales();

    /**
     * Returns {@code true} if the given {@code locale} is supported by
     * this locale service provider. The given {@code locale} may contain
     * <a href="../Locale.html#def_extensions">extensions</a> that should be
     * taken into account for the support determination.
     *
     * <p>The default implementation returns {@code true} if the given {@code locale}
     * is equal to any of the available {@code Locale}s returned by
     * {@link #getAvailableLocales()} with ignoring any extensions in both the
     * given {@code locale} and the available locales. Concrete locale service
     * provider implementations should override this method if those
     * implementations are {@code Locale} extensions-aware. For example,
     * {@code DecimalFormatSymbolsProvider} implementations will need to check
     * extensions in the given {@code locale} to see if any numbering system is
     * specified and can be supported. However, {@code CollatorProvider}
     * implementations may not be affected by any particular numbering systems,
     * and in that case, extensions for numbering systems should be ignored.
     *
     * @param locale a {@code Locale} to be tested
     * @return {@code true} if the given {@code locale} is supported by this
     *         provider; {@code false} otherwise.
     * @throws NullPointerException
     *         if the given {@code locale} is {@code null}
     * @see Locale#hasExtensions()
     * @see Locale#stripExtensions()
     * @since 1.8
     */
    public boolean isSupportedLocale(Locale locale) {
        locale = locale.stripExtensions(); // throws NPE if locale == null
        for (Locale available : getAvailableLocales()) {
            if (locale.equals(available.stripExtensions())) {
                return true;
}
        }
        return false;
    }
}
