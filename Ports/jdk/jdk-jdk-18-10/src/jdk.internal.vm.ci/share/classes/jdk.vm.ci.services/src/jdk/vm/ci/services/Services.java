/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package jdk.vm.ci.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import jdk.internal.misc.VM;

/**
 * Provides utilities needed by JVMCI clients.
 */
public final class Services {

    /**
     * Guards code that should be run when building an JVMCI shared library but should be excluded
     * from (being compiled into) the library. Such code must be directly guarded by an {@code if}
     * statement on this field - the guard cannot be behind a method call.
     */
    public static final boolean IS_BUILDING_NATIVE_IMAGE = Boolean.parseBoolean(VM.getSavedProperty("jdk.vm.ci.services.aot"));

    /**
     * Guards code that should only be run in a JVMCI shared library. Such code must be directly
     * guarded by an {@code if} statement on this field - the guard cannot be behind a method call.
     *
     * The value of this field in a JVMCI shared library runtime must be {@code true}.
     */
    public static final boolean IS_IN_NATIVE_IMAGE;
    static {
        /*
         * Prevents javac from constant folding use of this field. It is set to true by the process
         * that builds the shared library.
         */
        IS_IN_NATIVE_IMAGE = false;
    }

    private Services() {
    }

    /**
     * In a native image, this field is initialized by {@link #initializeSavedProperties(byte[])}.
     */
    private static volatile Map<String, String> savedProperties;

    static final boolean JVMCI_ENABLED = Boolean.parseBoolean(VM.getSavedProperties().get("jdk.internal.vm.ci.enabled"));

    /**
     * Checks that JVMCI is enabled in the VM and throws an error if it isn't.
     */
    static void checkJVMCIEnabled() {
        if (!JVMCI_ENABLED) {
            throw new Error("The EnableJVMCI VM option must be true (i.e., -XX:+EnableJVMCI) to use JVMCI");
        }
    }

    /**
     * Gets an unmodifiable copy of the system properties saved when {@link System} is initialized.
     */
    public static Map<String, String> getSavedProperties() {
        checkJVMCIEnabled();
        if (IS_IN_NATIVE_IMAGE) {
            if (savedProperties == null) {
                throw new InternalError("Saved properties not initialized");
            }
        } else {
            if (savedProperties == null) {
                synchronized (Services.class) {
                    if (savedProperties == null) {
                        @SuppressWarnings("removal")
                        SecurityManager sm = System.getSecurityManager();
                        if (sm != null) {
                            sm.checkPermission(new JVMCIPermission());
                        }
                        savedProperties = VM.getSavedProperties();
                    }
                }
            }
        }
        return savedProperties;
    }

    /**
     * Helper method equivalent to {@link #getSavedProperties()}{@code .getOrDefault(name, def)}.
     */
    public static String getSavedProperty(String name, String def) {
        return Services.getSavedProperties().getOrDefault(name, def);
    }

    /**
     * Helper method equivalent to {@link #getSavedProperties()}{@code .get(name)}.
     */
    public static String getSavedProperty(String name) {
        return Services.getSavedProperties().get(name);
    }

    /**
     * Causes the JVMCI subsystem to be initialized if it isn't already initialized.
     */
    public static void initializeJVMCI() {
        checkJVMCIEnabled();
        try {
            Class.forName("jdk.vm.ci.runtime.JVMCI");
        } catch (ClassNotFoundException e) {
            throw new InternalError(e);
        }
    }

    private static final Map<Class<?>, List<?>> servicesCache = IS_BUILDING_NATIVE_IMAGE ? new HashMap<>() : null;

    @SuppressWarnings("unchecked")
    private static <S> Iterable<S> load0(Class<S> service) {
        if (IS_IN_NATIVE_IMAGE || IS_BUILDING_NATIVE_IMAGE) {
            List<?> list = servicesCache.get(service);
            if (list != null) {
                return (Iterable<S>) list;
            }
            if (IS_IN_NATIVE_IMAGE) {
                throw new InternalError(String.format("No %s providers found when building native image", service.getName()));
            }
        }

        Iterable<S> providers = ServiceLoader.load(service, ClassLoader.getSystemClassLoader());
        if (IS_BUILDING_NATIVE_IMAGE) {
            synchronized (servicesCache) {
                ArrayList<S> providersList = new ArrayList<>();
                for (S provider : providers) {
                    providersList.add(provider);
                }
                servicesCache.put(service, providersList);
                providers = providersList;
            }
        }
        return providers;
    }

    /**
     * Opens all JVMCI packages to {@code otherModule}.
     */
    static void openJVMCITo(Module otherModule) {
        Module jvmci = Services.class.getModule();
        if (jvmci != otherModule) {
            Set<String> packages = jvmci.getPackages();
            for (String pkg : packages) {
                boolean opened = jvmci.isOpen(pkg, otherModule);
                if (!opened) {
                    jvmci.addOpens(pkg, otherModule);
                }
            }
        }
    }

    /**
     * Gets an {@link Iterable} of the JVMCI providers available for a given service.
     *
     * @throws SecurityException if a security manager is present and it denies <tt>
     *             {@link RuntimePermission}("jvmci")</tt>
     */
    public static <S> Iterable<S> load(Class<S> service) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new JVMCIPermission());
        }
        return load0(service);
    }

    /**
     * Gets the JVMCI provider for a given service for which at most one provider must be available.
     *
     * @param service the service whose provider is being requested
     * @param required specifies if an {@link InternalError} should be thrown if no provider of
     *            {@code service} is available
     * @throws SecurityException if a security manager is present and it denies <tt>
     *             {@link RuntimePermission}("jvmci")</tt>
     */
    public static <S> S loadSingle(Class<S> service, boolean required) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new JVMCIPermission());
        }
        Iterable<S> providers = load0(service);

        S singleProvider = null;
        for (S provider : providers) {
            if (singleProvider != null) {
                throw new InternalError(String.format("Multiple %s providers found: %s, %s", service.getName(), singleProvider.getClass().getName(), provider.getClass().getName()));
            }
            singleProvider = provider;
        }
        if (singleProvider == null && required) {
            String javaHome = Services.getSavedProperty("java.home");
            String vmName = Services.getSavedProperty("java.vm.name");
            Formatter errorMessage = new Formatter();
            errorMessage.format("The VM does not expose required service %s.%n", service.getName());
            errorMessage.format("Currently used Java home directory is %s.%n", javaHome);
            errorMessage.format("Currently used VM configuration is: %s", vmName);
            throw new UnsupportedOperationException(errorMessage.toString());
        }
        return singleProvider;
    }

    /**
     * A Java {@code char} has a maximal UTF8 length of 3.
     */
    private static final int MAX_UNICODE_IN_UTF8_LENGTH = 3;

    /**
     * {@link DataOutputStream#writeUTF(String)} only supports values whose UTF8 encoding length is
     * less than 65535.
     */
    private static final int MAX_UTF8_PROPERTY_STRING_LENGTH = 65535 / MAX_UNICODE_IN_UTF8_LENGTH;

    /**
     * Serializes the {@linkplain #getSavedProperties() saved system properties} to a byte array for
     * the purpose of {@linkplain #initializeSavedProperties(byte[]) initializing} the initial
     * properties in the JVMCI shared library.
     */
    @VMEntryPoint
    private static byte[] serializeSavedProperties() throws IOException {
        if (IS_IN_NATIVE_IMAGE) {
            throw new InternalError("Can only serialize saved properties in HotSpot runtime");
        }
        return serializeProperties(Services.getSavedProperties());
    }

    private static byte[] serializeProperties(Map<String, String> props) throws IOException {
        // Compute size of output on the assumption that
        // all system properties have ASCII names and values
        int estimate = 4 + 4;
        int nonUtf8Props = 0;
        for (Map.Entry<String, String> e : props.entrySet()) {
            String name = e.getKey();
            String value = e.getValue();
            estimate += (2 + (name.length())) + (2 + (value.length()));
            if (name.length() > MAX_UTF8_PROPERTY_STRING_LENGTH || value.length() > MAX_UTF8_PROPERTY_STRING_LENGTH) {
                nonUtf8Props++;
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(estimate);
        DataOutputStream out = new DataOutputStream(baos);
        out.writeInt(props.size() - nonUtf8Props);
        out.writeInt(nonUtf8Props);
        for (Map.Entry<String, String> e : props.entrySet()) {
            String name = e.getKey();
            String value = e.getValue();
            if (name.length() <= MAX_UTF8_PROPERTY_STRING_LENGTH && value.length() <= MAX_UTF8_PROPERTY_STRING_LENGTH) {
                out.writeUTF(name);
                out.writeUTF(value);
            }
        }
        if (nonUtf8Props != 0) {
            for (Map.Entry<String, String> e : props.entrySet()) {
                String name = e.getKey();
                String value = e.getValue();
                if (name.length() > MAX_UTF8_PROPERTY_STRING_LENGTH || value.length() > MAX_UTF8_PROPERTY_STRING_LENGTH) {
                    byte[] utf8Name = name.getBytes("UTF-8");
                    byte[] utf8Value = value.getBytes("UTF-8");
                    out.writeInt(utf8Name.length);
                    out.write(utf8Name);
                    out.writeInt(utf8Value.length);
                    out.write(utf8Value);
                }
            }
        }
        return baos.toByteArray();
    }

    /**
     * Initialized the {@linkplain #getSavedProperties() saved system properties} in the JVMCI
     * shared library from the {@linkplain #serializeSavedProperties() serialized saved properties}
     * in the HotSpot runtime.
     */
    @VMEntryPoint
    private static void initializeSavedProperties(byte[] serializedProperties) throws IOException {
        if (!IS_IN_NATIVE_IMAGE) {
            throw new InternalError("Can only initialize saved properties in JVMCI shared library runtime");
        }
        savedProperties = Collections.unmodifiableMap(deserializeProperties(serializedProperties));
    }

    private static Map<String, String> deserializeProperties(byte[] serializedProperties) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(serializedProperties));
        int utf8Props = in.readInt();
        int nonUtf8Props = in.readInt();
        Map<String, String> props = new HashMap<>(utf8Props + nonUtf8Props);
        int index = 0;
        while (in.available() != 0) {
            if (index < utf8Props) {
                String name = in.readUTF();
                String value = in.readUTF();
                props.put(name, value);
            } else {
                int nameLen = in.readInt();
                byte[] nameBytes = new byte[nameLen];
                in.read(nameBytes);
                int valueLen = in.readInt();
                byte[] valueBytes = new byte[valueLen];
                in.read(valueBytes);
                String name = new String(nameBytes, "UTF-8");
                String value = new String(valueBytes, "UTF-8");
                props.put(name, value);
            }
            index++;
        }
        return props;
    }
}
