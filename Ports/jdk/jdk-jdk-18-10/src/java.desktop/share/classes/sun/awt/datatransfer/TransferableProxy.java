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

package sun.awt.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Proxies for another Transferable so that Serializable objects are never
 * returned directly by DnD or the Clipboard. Instead, a new instance of the
 * object is returned.
 *
 * @author Lawrence P.G. Cable
 * @author David Mendenhall
 *
 * @since 1.4
 */
public class TransferableProxy implements Transferable {
    public TransferableProxy(Transferable t, boolean local) {
        transferable = t;
        isLocal = local;
    }
    public DataFlavor[] getTransferDataFlavors() {
        return transferable.getTransferDataFlavors();
    }
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return transferable.isDataFlavorSupported(flavor);
    }
    public Object getTransferData(DataFlavor df)
        throws UnsupportedFlavorException, IOException
    {
        Object data = transferable.getTransferData(df);

        // If the data is a Serializable object, then create a new instance
        // before returning it. This insulates applications sharing DnD and
        // Clipboard data from each other.
        if (data != null && isLocal && df.isFlavorSerializedObjectType()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ClassLoaderObjectOutputStream oos =
                new ClassLoaderObjectOutputStream(baos);
            oos.writeObject(data);

            ByteArrayInputStream bais =
                new ByteArrayInputStream(baos.toByteArray());

            try {
                ClassLoaderObjectInputStream ois =
                    new ClassLoaderObjectInputStream(bais,
                                                     oos.getClassLoaderMap());
                data = ois.readObject();
            } catch (ClassNotFoundException cnfe) {
                throw (IOException)new IOException().initCause(cnfe);
            }
        }

        return data;
    }

    protected final Transferable transferable;
    protected final boolean isLocal;
}

final class ClassLoaderObjectOutputStream extends ObjectOutputStream {
    private final Map<Set<String>, ClassLoader> map =
        new HashMap<Set<String>, ClassLoader>();

    ClassLoaderObjectOutputStream(OutputStream os) throws IOException {
        super(os);
    }

    protected void annotateClass(final Class<?> cl) throws IOException {
        @SuppressWarnings("removal")
        ClassLoader classLoader = AccessController.doPrivileged(
            new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return cl.getClassLoader();
                }
            });

        Set<String> s = new HashSet<String>(1);
        s.add(cl.getName());

        map.put(s, classLoader);
    }
    protected void annotateProxyClass(final Class<?> cl) throws IOException {
        @SuppressWarnings("removal")
        ClassLoader classLoader = AccessController.doPrivileged(
            new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return cl.getClassLoader();
                }
            });

        Class<?>[] interfaces = cl.getInterfaces();
        Set<String> s = new HashSet<String>(interfaces.length);
        for (int i = 0; i < interfaces.length; i++) {
            s.add(interfaces[i].getName());
        }

        map.put(s, classLoader);
    }

    Map<Set<String>, ClassLoader> getClassLoaderMap() {
        return new HashMap<>(map);
    }
}

final class ClassLoaderObjectInputStream extends ObjectInputStream {
    private final Map<Set<String>, ClassLoader> map;

    ClassLoaderObjectInputStream(InputStream is,
                                 Map<Set<String>, ClassLoader> map)
      throws IOException {
        super(is);
        if (map == null) {
            throw new NullPointerException("Null map");
        }
        this.map = map;
    }

    protected Class<?> resolveClass(ObjectStreamClass classDesc)
      throws IOException, ClassNotFoundException {
        String className = classDesc.getName();

        Set<String> s = new HashSet<String>(1);
        s.add(className);

        ClassLoader classLoader = map.get(s);
        if (classLoader != null) {
            return Class.forName(className, false, classLoader);
        } else {
            return super.resolveClass(classDesc);
        }
    }

    protected Class<?> resolveProxyClass(String[] interfaces)
      throws IOException, ClassNotFoundException {

        Set<String> s = new HashSet<String>(interfaces.length);
        for (int i = 0; i < interfaces.length; i++) {
            s.add(interfaces[i]);
        }

        ClassLoader classLoader = map.get(s);
        if (classLoader == null) {
            return super.resolveProxyClass(interfaces);
        }

        // The code below is mostly copied from the superclass.
        ClassLoader nonPublicLoader = null;
        boolean hasNonPublicInterface = false;

        // define proxy in class loader of non-public interface(s), if any
        Class<?>[] classObjs = new Class<?>[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            Class<?> cl = Class.forName(interfaces[i], false, classLoader);
            if ((cl.getModifiers() & Modifier.PUBLIC) == 0) {
                if (hasNonPublicInterface) {
                    if (nonPublicLoader != cl.getClassLoader()) {
                        throw new IllegalAccessError(
                            "conflicting non-public interface class loaders");
                    }
                } else {
                    nonPublicLoader = cl.getClassLoader();
                    hasNonPublicInterface = true;
                }
            }
            classObjs[i] = cl;
        }
        try {
            @SuppressWarnings("deprecation")
            Class<?> proxyClass = Proxy.getProxyClass(hasNonPublicInterface ?
                                                          nonPublicLoader : classLoader,
                                                      classObjs);
            return proxyClass;
        } catch (IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }
}
