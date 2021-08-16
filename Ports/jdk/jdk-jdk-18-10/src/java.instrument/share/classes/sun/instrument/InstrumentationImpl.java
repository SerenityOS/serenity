/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.instrument;

import java.lang.instrument.UnmodifiableModuleException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.AccessibleObject;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import jdk.internal.module.Modules;
import jdk.internal.vm.annotation.IntrinsicCandidate;

/*
 * Copyright 2003 Wily Technology, Inc.
 */

/**
 * The Java side of the JPLIS implementation. Works in concert with a native JVMTI agent
 * to implement the JPLIS API set. Provides both the Java API implementation of
 * the Instrumentation interface and utility Java routines to support the native code.
 * Keeps a pointer to the native data structure in a scalar field to allow native
 * processing behind native methods.
 */
public class InstrumentationImpl implements Instrumentation {
    private final     TransformerManager      mTransformerManager;
    private           TransformerManager      mRetransfomableTransformerManager;
    // needs to store a native pointer, so use 64 bits
    private final     long                    mNativeAgent;
    private final     boolean                 mEnvironmentSupportsRedefineClasses;
    private volatile  boolean                 mEnvironmentSupportsRetransformClassesKnown;
    private volatile  boolean                 mEnvironmentSupportsRetransformClasses;
    private final     boolean                 mEnvironmentSupportsNativeMethodPrefix;

    private
    InstrumentationImpl(long    nativeAgent,
                        boolean environmentSupportsRedefineClasses,
                        boolean environmentSupportsNativeMethodPrefix) {
        mTransformerManager                    = new TransformerManager(false);
        mRetransfomableTransformerManager      = null;
        mNativeAgent                           = nativeAgent;
        mEnvironmentSupportsRedefineClasses    = environmentSupportsRedefineClasses;
        mEnvironmentSupportsRetransformClassesKnown = false; // false = need to ask
        mEnvironmentSupportsRetransformClasses = false;      // don't know yet
        mEnvironmentSupportsNativeMethodPrefix = environmentSupportsNativeMethodPrefix;
    }

    public void
    addTransformer(ClassFileTransformer transformer) {
        addTransformer(transformer, false);
    }

    public synchronized void
    addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
        if (transformer == null) {
            throw new NullPointerException("null passed as 'transformer' in addTransformer");
        }
        if (canRetransform) {
            if (!isRetransformClassesSupported()) {
                throw new UnsupportedOperationException(
                  "adding retransformable transformers is not supported in this environment");
            }
            if (mRetransfomableTransformerManager == null) {
                mRetransfomableTransformerManager = new TransformerManager(true);
            }
            mRetransfomableTransformerManager.addTransformer(transformer);
            if (mRetransfomableTransformerManager.getTransformerCount() == 1) {
                setHasRetransformableTransformers(mNativeAgent, true);
            }
        } else {
            mTransformerManager.addTransformer(transformer);
            if (mTransformerManager.getTransformerCount() == 1) {
                setHasTransformers(mNativeAgent, true);
            }
        }
    }

    public synchronized boolean
    removeTransformer(ClassFileTransformer transformer) {
        if (transformer == null) {
            throw new NullPointerException("null passed as 'transformer' in removeTransformer");
        }
        TransformerManager mgr = findTransformerManager(transformer);
        if (mgr != null) {
            mgr.removeTransformer(transformer);
            if (mgr.getTransformerCount() == 0) {
                if (mgr.isRetransformable()) {
                    setHasRetransformableTransformers(mNativeAgent, false);
                } else {
                    setHasTransformers(mNativeAgent, false);
                }
            }
            return true;
        }
        return false;
    }

    public boolean
    isModifiableClass(Class<?> theClass) {
        if (theClass == null) {
            throw new NullPointerException(
                         "null passed as 'theClass' in isModifiableClass");
        }
        return isModifiableClass0(mNativeAgent, theClass);
    }

    public boolean isModifiableModule(Module module) {
        if (module == null) {
            throw new NullPointerException("'module' is null");
        }
        return true;
    }

    public boolean
    isRetransformClassesSupported() {
        // ask lazily since there is some overhead
        if (!mEnvironmentSupportsRetransformClassesKnown) {
            mEnvironmentSupportsRetransformClasses = isRetransformClassesSupported0(mNativeAgent);
            mEnvironmentSupportsRetransformClassesKnown = true;
        }
        return mEnvironmentSupportsRetransformClasses;
    }

    public void
    retransformClasses(Class<?>... classes) {
        if (!isRetransformClassesSupported()) {
            throw new UnsupportedOperationException(
              "retransformClasses is not supported in this environment");
        }
        if (classes.length == 0) {
            return; // no-op
        }
        retransformClasses0(mNativeAgent, classes);
    }

    public boolean
    isRedefineClassesSupported() {
        return mEnvironmentSupportsRedefineClasses;
    }

    public void
    redefineClasses(ClassDefinition...  definitions)
            throws  ClassNotFoundException {
        if (!isRedefineClassesSupported()) {
            throw new UnsupportedOperationException("redefineClasses is not supported in this environment");
        }
        if (definitions == null) {
            throw new NullPointerException("null passed as 'definitions' in redefineClasses");
        }
        for (int i = 0; i < definitions.length; ++i) {
            if (definitions[i] == null) {
                throw new NullPointerException("element of 'definitions' is null in redefineClasses");
            }
        }
        if (definitions.length == 0) {
            return; // short-circuit if there are no changes requested
        }

        redefineClasses0(mNativeAgent, definitions);
    }

    @SuppressWarnings("rawtypes")
    public Class[]
    getAllLoadedClasses() {
        return getAllLoadedClasses0(mNativeAgent);
    }

    @SuppressWarnings("rawtypes")
    public Class[]
    getInitiatedClasses(ClassLoader loader) {
        return getInitiatedClasses0(mNativeAgent, loader);
    }

    public long
    getObjectSize(Object objectToSize) {
        if (objectToSize == null) {
            throw new NullPointerException("null passed as 'objectToSize' in getObjectSize");
        }
        return getObjectSize0(mNativeAgent, objectToSize);
    }

    public void
    appendToBootstrapClassLoaderSearch(JarFile jarfile) {
        appendToClassLoaderSearch0(mNativeAgent, jarfile.getName(), true);
    }

    public void
    appendToSystemClassLoaderSearch(JarFile jarfile) {
        appendToClassLoaderSearch0(mNativeAgent, jarfile.getName(), false);
    }

    public boolean
    isNativeMethodPrefixSupported() {
        return mEnvironmentSupportsNativeMethodPrefix;
    }

    public synchronized void
    setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {
        if (!isNativeMethodPrefixSupported()) {
            throw new UnsupportedOperationException(
                   "setNativeMethodPrefix is not supported in this environment");
        }
        if (transformer == null) {
            throw new NullPointerException(
                       "null passed as 'transformer' in setNativeMethodPrefix");
        }
        TransformerManager mgr = findTransformerManager(transformer);
        if (mgr == null) {
            throw new IllegalArgumentException(
                       "transformer not registered in setNativeMethodPrefix");
        }
        mgr.setNativeMethodPrefix(transformer, prefix);
        String[] prefixes = mgr.getNativeMethodPrefixes();
        setNativeMethodPrefixes(mNativeAgent, prefixes, mgr.isRetransformable());
    }

    @Override
    public void redefineModule(Module module,
                               Set<Module> extraReads,
                               Map<String, Set<Module>> extraExports,
                               Map<String, Set<Module>> extraOpens,
                               Set<Class<?>> extraUses,
                               Map<Class<?>, List<Class<?>>> extraProvides)
    {
        if (!module.isNamed())
            return;

        if (!isModifiableModule(module))
            throw new UnmodifiableModuleException(module.getName());

        // copy and check reads
        extraReads = new HashSet<>(extraReads);
        if (extraReads.contains(null))
            throw new NullPointerException("'extraReads' contains null");

        // copy and check exports and opens
        extraExports = cloneAndCheckMap(module, extraExports);
        extraOpens = cloneAndCheckMap(module, extraOpens);

        // copy and check uses
        extraUses = new HashSet<>(extraUses);
        if (extraUses.contains(null))
            throw new NullPointerException("'extraUses' contains null");

        // copy and check provides
        Map<Class<?>, List<Class<?>>> tmpProvides = new HashMap<>();
        for (Map.Entry<Class<?>, List<Class<?>>> e : extraProvides.entrySet()) {
            Class<?> service = e.getKey();
            if (service == null)
                throw new NullPointerException("'extraProvides' contains null");
            List<Class<?>> providers = new ArrayList<>(e.getValue());
            if (providers.isEmpty())
                throw new IllegalArgumentException("list of providers is empty");
            providers.forEach(p -> {
                if (p.getModule() != module)
                    throw new IllegalArgumentException(p + " not in " + module);
                if (!service.isAssignableFrom(p))
                    throw new IllegalArgumentException(p + " is not a " + service);
            });
            tmpProvides.put(service, providers);
        }
        extraProvides = tmpProvides;


        // update reads
        extraReads.forEach(m -> Modules.addReads(module, m));

        // update exports
        for (Map.Entry<String, Set<Module>> e : extraExports.entrySet()) {
            String pkg = e.getKey();
            Set<Module> targets = e.getValue();
            targets.forEach(m -> Modules.addExports(module, pkg, m));
        }

        // update opens
        for (Map.Entry<String, Set<Module>> e : extraOpens.entrySet()) {
            String pkg = e.getKey();
            Set<Module> targets = e.getValue();
            targets.forEach(m -> Modules.addOpens(module, pkg, m));
        }

        // update uses
        extraUses.forEach(service -> Modules.addUses(module, service));

        // update provides
        for (Map.Entry<Class<?>, List<Class<?>>> e : extraProvides.entrySet()) {
            Class<?> service = e.getKey();
            List<Class<?>> providers = e.getValue();
            providers.forEach(p -> Modules.addProvides(module, service, p));
        }
    }

    private Map<String, Set<Module>>
        cloneAndCheckMap(Module module, Map<String, Set<Module>> map)
    {
        if (map.isEmpty())
            return Collections.emptyMap();

        Map<String, Set<Module>> result = new HashMap<>();
        Set<String> packages = module.getPackages();
        for (Map.Entry<String, Set<Module>> e : map.entrySet()) {
            String pkg = e.getKey();
            if (pkg == null)
                throw new NullPointerException("package cannot be null");
            if (!packages.contains(pkg))
                throw new IllegalArgumentException(pkg + " not in module");
            Set<Module> targets = new HashSet<>(e.getValue());
            if (targets.isEmpty())
                throw new IllegalArgumentException("set of targets is empty");
            if (targets.contains(null))
                throw new NullPointerException("set of targets cannot include null");
            result.put(pkg, targets);
        }
        return result;
    }


    private TransformerManager
    findTransformerManager(ClassFileTransformer transformer) {
        if (mTransformerManager.includesTransformer(transformer)) {
            return mTransformerManager;
        }
        if (mRetransfomableTransformerManager != null &&
                mRetransfomableTransformerManager.includesTransformer(transformer)) {
            return mRetransfomableTransformerManager;
        }
        return null;
    }


    /*
     *  Natives
     */
    private native boolean
    isModifiableClass0(long nativeAgent, Class<?> theClass);

    private native boolean
    isRetransformClassesSupported0(long nativeAgent);

    private native void
    setHasTransformers(long nativeAgent, boolean has);

    private native void
    setHasRetransformableTransformers(long nativeAgent, boolean has);

    private native void
    retransformClasses0(long nativeAgent, Class<?>[] classes);

    private native void
    redefineClasses0(long nativeAgent, ClassDefinition[]  definitions)
        throws  ClassNotFoundException;

    @SuppressWarnings("rawtypes")
    private native Class[]
    getAllLoadedClasses0(long nativeAgent);

    @SuppressWarnings("rawtypes")
    private native Class[]
    getInitiatedClasses0(long nativeAgent, ClassLoader loader);

    @IntrinsicCandidate
    private native long
    getObjectSize0(long nativeAgent, Object objectToSize);

    private native void
    appendToClassLoaderSearch0(long nativeAgent, String jarfile, boolean bootLoader);

    private native void
    setNativeMethodPrefixes(long nativeAgent, String[] prefixes, boolean isRetransformable);

    static {
        System.loadLibrary("instrument");
    }

    /*
     *  Internals
     */


    // Enable or disable Java programming language access checks on a
    // reflected object (for example, a method)
    @SuppressWarnings("removal")
    private static void setAccessible(final AccessibleObject ao, final boolean accessible) {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    ao.setAccessible(accessible);
                    return null;
                }});
    }

    // Attempt to load and start an agent
    private void
    loadClassAndStartAgent( String  classname,
                            String  methodname,
                            String  optionsString)
            throws Throwable {

        ClassLoader mainAppLoader   = ClassLoader.getSystemClassLoader();
        Class<?>    javaAgentClass  = mainAppLoader.loadClass(classname);

        Method m = null;
        NoSuchMethodException firstExc = null;
        boolean twoArgAgent = false;

        // The agent class must have a premain or agentmain method that
        // has 1 or 2 arguments. We check in the following order:
        //
        // 1) declared with a signature of (String, Instrumentation)
        // 2) declared with a signature of (String)
        //
        // If no method is found then we throw the NoSuchMethodException
        // from the first attempt so that the exception text indicates
        // the lookup failed for the 2-arg method (same as JDK5.0).

        try {
            m = javaAgentClass.getDeclaredMethod( methodname,
                                 new Class<?>[] {
                                     String.class,
                                     java.lang.instrument.Instrumentation.class
                                 }
                               );
            twoArgAgent = true;
        } catch (NoSuchMethodException x) {
            // remember the NoSuchMethodException
            firstExc = x;
        }

        if (m == null) {
            // now try the declared 1-arg method
            try {
                m = javaAgentClass.getDeclaredMethod(methodname,
                                                 new Class<?>[] { String.class });
            } catch (NoSuchMethodException x) {
                // none of the methods exists so we throw the
                // first NoSuchMethodException as per 5.0
                throw firstExc;
            }
        }

        // reject non-public premain or agentmain method
        if (!Modifier.isPublic(m.getModifiers())) {
            String msg = "method " + classname + "." +  methodname + " must be declared public";
            throw new IllegalAccessException(msg);
        }

        if (!Modifier.isPublic(javaAgentClass.getModifiers()) &&
            !javaAgentClass.getModule().isNamed()) {
            // If the java agent class is in an unnamed module, the java agent class can be non-public.
            // Suppress access check upon the invocation of the premain/agentmain method.
            setAccessible(m, true);
        }

        // invoke the 1 or 2-arg method
        if (twoArgAgent) {
            m.invoke(null, new Object[] { optionsString, this });
        } else {
            m.invoke(null, new Object[] { optionsString });
        }
    }

    // WARNING: the native code knows the name & signature of this method
    private void
    loadClassAndCallPremain(    String  classname,
                                String  optionsString)
            throws Throwable {

        loadClassAndStartAgent( classname, "premain", optionsString );
    }


    // WARNING: the native code knows the name & signature of this method
    private void
    loadClassAndCallAgentmain(  String  classname,
                                String  optionsString)
            throws Throwable {

        loadClassAndStartAgent( classname, "agentmain", optionsString );
    }

    // WARNING: the native code knows the name & signature of this method
    private byte[]
    transform(  Module              module,
                ClassLoader         loader,
                String              classname,
                Class<?>            classBeingRedefined,
                ProtectionDomain    protectionDomain,
                byte[]              classfileBuffer,
                boolean             isRetransformer) {
        TransformerManager mgr = isRetransformer?
                                        mRetransfomableTransformerManager :
                                        mTransformerManager;
        // module is null when not a class load or when loading a class in an
        // unnamed module and this is the first type to be loaded in the package.
        if (module == null) {
            if (classBeingRedefined != null) {
                module = classBeingRedefined.getModule();
            } else {
                module = (loader == null) ? jdk.internal.loader.BootLoader.getUnnamedModule()
                                          : loader.getUnnamedModule();
            }
        }
        if (mgr == null) {
            return null; // no manager, no transform
        } else {
            return mgr.transform(   module,
                                    loader,
                                    classname,
                                    classBeingRedefined,
                                    protectionDomain,
                                    classfileBuffer);
        }
    }


    /**
     * Invoked by the java launcher to load a java agent that is packaged with
     * the main application in an executable JAR file.
     */
    public static void loadAgent(String path) {
        loadAgent0(path);
    }

    private static native void loadAgent0(String path);
}
