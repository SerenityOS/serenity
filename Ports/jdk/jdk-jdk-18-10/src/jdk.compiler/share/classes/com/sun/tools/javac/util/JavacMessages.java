/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.util;

import com.sun.tools.javac.api.Messages;

import java.lang.ref.SoftReference;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.sun.tools.javac.api.DiagnosticFormatter;
import com.sun.tools.javac.util.JCDiagnostic.Factory;
import com.sun.tools.javac.resources.CompilerProperties.Errors;

/**
 *  Support for formatted localized messages.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class JavacMessages implements Messages {
    /** The context key for the JavacMessages object. */
    public static final Context.Key<JavacMessages> messagesKey = new Context.Key<>();

    /** Get the JavacMessages instance for this context. */
    public static JavacMessages instance(Context context) {
        JavacMessages instance = context.get(messagesKey);
        if (instance == null)
            instance = new JavacMessages(context);
        return instance;
    }

    private Map<Locale, SoftReference<List<ResourceBundle>>> bundleCache;

    private List<ResourceBundleHelper> bundleHelpers;

    private Locale currentLocale;
    private List<ResourceBundle> currentBundles;

    private DiagnosticFormatter<JCDiagnostic> diagFormatter;
    private JCDiagnostic.Factory diagFactory;

    public Locale getCurrentLocale() {
        return currentLocale;
    }

    public void setCurrentLocale(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        this.currentBundles = getBundles(locale);
        this.currentLocale = locale;
    }

    Context context;

    /** Creates a JavacMessages object.
     */
    public JavacMessages(Context context) {
        this(defaultBundleName, context.get(Locale.class));
        this.context = context;
        context.put(messagesKey, this);
        Options options = Options.instance(context);
        boolean rawDiagnostics = options.isSet("rawDiagnostics");
        this.diagFormatter = rawDiagnostics ? new RawDiagnosticFormatter(options) :
                                                  new BasicDiagnosticFormatter(options, this);
    }

    /** Creates a JavacMessages object.
     * @param bundleName the name to identify the resource bundle of localized messages.
     */
    public JavacMessages(String bundleName) throws MissingResourceException {
        this(bundleName, null);
    }

    /** Creates a JavacMessages object.
     * @param bundleName the name to identify the resource bundle of localized messages.
     */
    public JavacMessages(String bundleName, Locale locale) throws MissingResourceException {
        bundleHelpers = List.nil();
        bundleCache = new HashMap<>();
        add(bundleName);
        setCurrentLocale(locale);
    }

    public JavacMessages() throws MissingResourceException {
        this(defaultBundleName);
    }

    @Override
    public void add(String bundleName) throws MissingResourceException {
        add(locale -> ResourceBundle.getBundle(bundleName, locale));
    }

    public void add(ResourceBundleHelper ma) {
        bundleHelpers = bundleHelpers.prepend(ma);
        if (!bundleCache.isEmpty())
            bundleCache.clear();
        currentBundles = null;
    }

    public List<ResourceBundle> getBundles(Locale locale) {
        if (locale == currentLocale && currentBundles != null)
            return currentBundles;
        SoftReference<List<ResourceBundle>> bundles = bundleCache.get(locale);
        List<ResourceBundle> bundleList = bundles == null ? null : bundles.get();
        if (bundleList == null) {
            bundleList = List.nil();
            for (ResourceBundleHelper helper : bundleHelpers) {
                try {
                    ResourceBundle rb = helper.getResourceBundle(locale);
                    bundleList = bundleList.prepend(rb);
                } catch (MissingResourceException e) {
                    throw new InternalError("Cannot find requested resource bundle for locale " +
                            locale, e);
                }
            }
            bundleCache.put(locale, new SoftReference<>(bundleList));
        }
        return bundleList;
    }

    /** Gets the localized string corresponding to a key, formatted with a set of args.
     */
    public String getLocalizedString(String key, Object... args) {
        return getLocalizedString(currentLocale, key, args);
    }

    public String getLocalizedString(JCDiagnostic.DiagnosticInfo diagInfo) {
        return getLocalizedString(currentLocale, diagInfo);
    }

    @Override
    public String getLocalizedString(Locale l, String key, Object... args) {
        if (l == null)
            l = getCurrentLocale();
        return getLocalizedString(getBundles(l), key, args);
    }

    public String getLocalizedString(Locale l, JCDiagnostic.DiagnosticInfo diagInfo) {
        if (l == null)
            l = getCurrentLocale();
        return getLocalizedString(getBundles(l), diagInfo);
    }

    /* Static access:
     * javac has a firmly entrenched notion of a default message bundle
     * which it can access from any static context. This is used to get
     * easy access to simple localized strings.
     */

    private static final String defaultBundleName = "com.sun.tools.javac.resources.compiler";
    private static ResourceBundle defaultBundle;
    private static JavacMessages defaultMessages;


    /**
     * Returns a localized string from the compiler's default bundle.
     */
    // used to support legacy Log.getLocalizedString
    static String getDefaultLocalizedString(String key, Object... args) {
        return getLocalizedString(List.of(getDefaultBundle()), key, args);
    }

    // used to support legacy static Diagnostic.fragment
    @Deprecated
    static JavacMessages getDefaultMessages() {
        if (defaultMessages == null)
            defaultMessages = new JavacMessages(defaultBundleName);
        return defaultMessages;
    }

    public static ResourceBundle getDefaultBundle() {
        try {
            if (defaultBundle == null)
                defaultBundle = ResourceBundle.getBundle(defaultBundleName);
            return defaultBundle;
        }
        catch (MissingResourceException e) {
            throw new Error("Fatal: Resource for compiler is missing", e);
        }
    }

    private static String getLocalizedString(List<ResourceBundle> bundles,
                                             String key,
                                             Object... args) {
       String msg = null;
       for (List<ResourceBundle> l = bundles; l.nonEmpty() && msg == null; l = l.tail) {
           ResourceBundle rb = l.head;
           try {
               msg = rb.getString(key);
           }
           catch (MissingResourceException e) {
               // ignore, try other bundles in list
           }
       }
       if (msg == null) {
           msg = "compiler message file broken: key=" + key +
               " arguments={0}, {1}, {2}, {3}, {4}, {5}, {6}, {7}";
       }
       return MessageFormat.format(msg, args);
    }

    private String getLocalizedString(List<ResourceBundle> bundles, JCDiagnostic.DiagnosticInfo diagInfo) {
        String msg = null;
        for (List<ResourceBundle> l = bundles; l.nonEmpty() && msg == null; l = l.tail) {
            ResourceBundle rb = l.head;
            try {
                msg = rb.getString(diagInfo.key());
            }
            catch (MissingResourceException e) {
                // ignore, try other bundles in list
            }
        }
        if (msg == null) {
            msg = "compiler message file broken: key=" + diagInfo.key() +
                " arguments={0}, {1}, {2}, {3}, {4}, {5}, {6}, {7}";
        }
        if (diagInfo == Errors.Error) {
            return MessageFormat.format(msg, new Object[0]);
        } else {
            return diagFormatter.format(getDiagFactory().create(DiagnosticSource.NO_SOURCE, null, diagInfo),
                    getCurrentLocale());
        }
    }

    JCDiagnostic.Factory getDiagFactory() {
        if (diagFactory == null) {
            this.diagFactory = JCDiagnostic.Factory.instance(context);
        }
        return diagFactory;
    }

    /**
     * This provides a way for the JavacMessager to retrieve a
     * ResourceBundle from another module such as jdk.javadoc.
     */
    public interface ResourceBundleHelper {
        /**
         * Gets the ResourceBundle.
         * @param locale the requested bundle's locale
         * @return ResourceBundle
         */
        ResourceBundle getResourceBundle(Locale locale);
    }
}
