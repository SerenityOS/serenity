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

package javax.script;
import java.util.*;
import java.security.*;
import java.util.ServiceLoader;
import java.util.ServiceConfigurationError;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The <code>ScriptEngineManager</code> implements a discovery and instantiation
 * mechanism for <code>ScriptEngine</code> classes and also maintains a
 * collection of key/value pairs storing state shared by all engines created
 * by the Manager. This class uses the service provider mechanism described in the
 * {@link java.util.ServiceLoader} class to enumerate all the
 * implementations of <code>ScriptEngineFactory</code>. <br><br>
 * The <code>ScriptEngineManager</code> provides a method to return a list of all these factories
 * as well as utility methods which look up factories on the basis of language name, file extension
 * and mime type.
 * <p>
 * The <code>Bindings</code> of key/value pairs, referred to as the "Global Scope"  maintained
 * by the manager is available to all instances of <code>ScriptEngine</code> created
 * by the <code>ScriptEngineManager</code>.  The values in the <code>Bindings</code> are
 * generally exposed in all scripts.
 *
 * @author Mike Grogan
 * @author A. Sundararajan
 * @since 1.6
 */
public class ScriptEngineManager  {
    private static final boolean DEBUG = false;
    /**
     * The effect of calling this constructor is the same as calling
     * <code>ScriptEngineManager(Thread.currentThread().getContextClassLoader())</code>.
     *
     * @see java.lang.Thread#getContextClassLoader
     */
    public ScriptEngineManager() {
        this(Thread.currentThread().getContextClassLoader());
    }

    /**
     * This constructor loads the implementations of
     * <code>ScriptEngineFactory</code> visible to the given
     * <code>ClassLoader</code> using the service provider mechanism.<br><br>
     * If loader is <code>null</code>, the script engine factories that are
     * bundled with the platform are loaded. <br>
     *
     * @param loader ClassLoader used to discover script engine factories.
     */
    public ScriptEngineManager(ClassLoader loader) {
        initEngines(loader);
    }

    private ServiceLoader<ScriptEngineFactory> getServiceLoader(final ClassLoader loader) {
        if (loader != null) {
            return ServiceLoader.load(ScriptEngineFactory.class, loader);
        } else {
            return ServiceLoader.loadInstalled(ScriptEngineFactory.class);
        }
    }

    private void initEngines(final ClassLoader loader) {
        Iterator<ScriptEngineFactory> itr;
        try {
            @SuppressWarnings("removal")
            var sl = AccessController.doPrivileged(
                (PrivilegedAction<ServiceLoader<ScriptEngineFactory>>)() -> getServiceLoader(loader));
            itr = sl.iterator();
        } catch (ServiceConfigurationError err) {
            reportException("Can't find ScriptEngineFactory providers: ", err);
            // do not throw any exception here. user may want to
            // manage his/her own factories using this manager
            // by explicit registratation (by registerXXX) methods.
            return;
        }

        try {
            while (itr.hasNext()) {
                try {
                    ScriptEngineFactory fact = itr.next();
                    engineSpis.add(fact);
                } catch (ServiceConfigurationError err) {
                    reportException("ScriptEngineManager providers.next(): ", err);
                    // one factory failed, but check other factories...
                }
            }
        } catch (ServiceConfigurationError err) {
            reportException("ScriptEngineManager providers.hasNext(): ", err);
            // do not throw any exception here. user may want to
            // manage his/her own factories using this manager
            // by explicit registratation (by registerXXX) methods.
        }
    }

    /**
     * <code>setBindings</code> stores the specified <code>Bindings</code>
     * in the <code>globalScope</code> field. ScriptEngineManager sets this
     * <code>Bindings</code> as global bindings for <code>ScriptEngine</code>
     * objects created by it.
     *
     * @param bindings The specified <code>Bindings</code>
     * @throws IllegalArgumentException if bindings is null.
     */
    public void setBindings(Bindings bindings) {
        if (bindings == null) {
            throw new IllegalArgumentException("Global scope cannot be null.");
        }

        globalScope = bindings;
    }

    /**
     * <code>getBindings</code> returns the value of the <code>globalScope</code> field.
     * ScriptEngineManager sets this <code>Bindings</code> as global bindings for
     * <code>ScriptEngine</code> objects created by it.
     *
     * @return The globalScope field.
     */
    public Bindings getBindings() {
        return globalScope;
    }

    /**
     * Sets the specified key/value pair in the Global Scope.
     * @param key Key to set
     * @param value Value to set.
     * @throws NullPointerException if key is null.
     * @throws IllegalArgumentException if key is empty string.
     */
    public void put(String key, Object value) {
        globalScope.put(key, value);
    }

    /**
     * Gets the value for the specified key in the Global Scope
     * @param key The key whose value is to be returned.
     * @return The value for the specified key.
     */
    public Object get(String key) {
        return globalScope.get(key);
    }

    /**
     * Looks up and creates a <code>ScriptEngine</code> for a given  name.
     * The algorithm first searches for a <code>ScriptEngineFactory</code> that has been
     * registered as a handler for the specified name using the <code>registerEngineName</code>
     * method.
     * <br><br> If one is not found, it searches the set of <code>ScriptEngineFactory</code> instances
     * stored by the constructor for one with the specified name.  If a <code>ScriptEngineFactory</code>
     * is found by either method, it is used to create instance of <code>ScriptEngine</code>.
     * @param shortName The short name of the <code>ScriptEngine</code> implementation.
     * returned by the <code>getNames</code> method of its <code>ScriptEngineFactory</code>.
     * @return A <code>ScriptEngine</code> created by the factory located in the search.  Returns null
     * if no such factory was found.  The <code>ScriptEngineManager</code> sets its own <code>globalScope</code>
     * <code>Bindings</code> as the <code>GLOBAL_SCOPE</code> <code>Bindings</code> of the newly
     * created <code>ScriptEngine</code>.
     * @throws NullPointerException if shortName is null.
     */
    public ScriptEngine getEngineByName(String shortName) {
        return getEngineBy(shortName, nameAssociations, ScriptEngineFactory::getNames);
    }

    /**
     * Look up and create a <code>ScriptEngine</code> for a given extension.  The algorithm
     * used by <code>getEngineByName</code> is used except that the search starts
     * by looking for a <code>ScriptEngineFactory</code> registered to handle the
     * given extension using <code>registerEngineExtension</code>.
     * @param extension The given extension
     * @return The engine to handle scripts with this extension.  Returns <code>null</code>
     * if not found.
     * @throws NullPointerException if extension is null.
     */
    public ScriptEngine getEngineByExtension(String extension) {
        return getEngineBy(extension, extensionAssociations, ScriptEngineFactory::getExtensions);
    }

    /**
     * Look up and create a <code>ScriptEngine</code> for a given mime type.  The algorithm
     * used by <code>getEngineByName</code> is used except that the search starts
     * by looking for a <code>ScriptEngineFactory</code> registered to handle the
     * given mime type using <code>registerEngineMimeType</code>.
     * @param mimeType The given mime type
     * @return The engine to handle scripts with this mime type.  Returns <code>null</code>
     * if not found.
     * @throws NullPointerException if mimeType is null.
     */
    public ScriptEngine getEngineByMimeType(String mimeType) {
        return getEngineBy(mimeType, mimeTypeAssociations, ScriptEngineFactory::getMimeTypes);
    }

    private ScriptEngine getEngineBy(String selector, Map<String, ScriptEngineFactory> associations,
        Function<ScriptEngineFactory, List<String>> valuesFn)
    {
        Objects.requireNonNull(selector);
        Stream<ScriptEngineFactory> spis = Stream.concat(
            //look for registered types first
            Stream.ofNullable(associations.get(selector)),

            engineSpis.stream().filter(spi -> {
                try {
                    List<String> matches = valuesFn.apply(spi);
                    return matches != null && matches.contains(selector);
                } catch (Exception exp) {
                    debugPrint(exp);
                    return false;
                }
            })
        );
        return spis
            .map(spi -> {
                try {
                    ScriptEngine engine = spi.getScriptEngine();
                    engine.setBindings(getBindings(), ScriptContext.GLOBAL_SCOPE);
                    return engine;
                } catch (Exception exp) {
                    debugPrint(exp);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private static void reportException(String msg, Throwable exp) {
        System.err.println(msg + exp.getMessage());
        debugPrint(exp);
    }

    private static void debugPrint(Throwable exp) {
        if (DEBUG) {
            exp.printStackTrace();
        }
    }

    /**
     * Returns a list whose elements are instances of all the <code>ScriptEngineFactory</code> classes
     * found by the discovery mechanism.
     * @return List of all discovered <code>ScriptEngineFactory</code>s.
     */
    public List<ScriptEngineFactory> getEngineFactories() {
        return List.copyOf(engineSpis);
    }

    /**
     * Registers a <code>ScriptEngineFactory</code> to handle a language
     * name.  Overrides any such association found using the Discovery mechanism.
     * @param name The name to be associated with the <code>ScriptEngineFactory</code>.
     * @param factory The class to associate with the given name.
     * @throws NullPointerException if any of the parameters is null.
     */
    public void registerEngineName(String name, ScriptEngineFactory factory) {
        associateFactory(nameAssociations, name, factory);
    }

    /**
     * Registers a <code>ScriptEngineFactory</code> to handle a mime type.
     * Overrides any such association found using the Discovery mechanism.
     *
     * @param type The mime type  to be associated with the
     * <code>ScriptEngineFactory</code>.
     *
     * @param factory The class to associate with the given mime type.
     * @throws NullPointerException if any of the parameters is null.
     */
    public void registerEngineMimeType(String type, ScriptEngineFactory factory) {
        associateFactory(mimeTypeAssociations, type, factory);
    }

    /**
     * Registers a <code>ScriptEngineFactory</code> to handle an extension.
     * Overrides any such association found using the Discovery mechanism.
     *
     * @param extension The extension type  to be associated with the
     * <code>ScriptEngineFactory</code>.
     * @param factory The class to associate with the given extension.
     * @throws NullPointerException if any of the parameters is null.
     */
    public void registerEngineExtension(String extension, ScriptEngineFactory factory) {
        associateFactory(extensionAssociations, extension, factory);
    }

    private static void associateFactory(Map<String, ScriptEngineFactory> associations, String association,
        ScriptEngineFactory factory)
    {
        if (association == null || factory == null) throw new NullPointerException();
        associations.put(association, factory);
    }

    private static final Comparator<ScriptEngineFactory> COMPARATOR = Comparator.comparing(
        ScriptEngineFactory::getEngineName,
        Comparator.nullsLast(Comparator.naturalOrder())
    );

    /** Set of script engine factories discovered. */
    private final TreeSet<ScriptEngineFactory> engineSpis = new TreeSet<>(COMPARATOR);

    /** Map of engine name to script engine factory. */
    private final HashMap<String, ScriptEngineFactory> nameAssociations = new HashMap<>();

    /** Map of script file extension to script engine factory. */
    private final HashMap<String, ScriptEngineFactory> extensionAssociations = new HashMap<>();

    /** Map of script MIME type to script engine factory. */
    private final HashMap<String, ScriptEngineFactory> mimeTypeAssociations = new HashMap<>();

    /** Global bindings associated with script engines created by this manager. */
    private Bindings globalScope = new SimpleBindings();
}
