/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels.spi;

import java.nio.channels.*;
import java.io.IOException;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.ServiceConfigurationError;
import java.util.concurrent.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Service-provider class for asynchronous channels.
 *
 * <p> An asynchronous channel provider is a concrete subclass of this class that
 * has a zero-argument constructor and implements the abstract methods specified
 * below.  A given invocation of the Java virtual machine maintains a single
 * system-wide default provider instance, which is returned by the {@link
 * #provider() provider} method.  The first invocation of that method will locate
 * the default provider as specified below.
 *
 * <p> All of the methods in this class are safe for use by multiple concurrent
 * threads.  </p>
 *
 * @since 1.7
 */

public abstract class AsynchronousChannelProvider {
    private static Void checkPermission() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(new RuntimePermission("asynchronousChannelProvider"));
        return null;
    }
    private AsynchronousChannelProvider(Void ignore) { }

    /**
     * Initializes a new instance of this class.
     *
     * @throws  SecurityException
     *          If a security manager has been installed and it denies
     *          {@link RuntimePermission}{@code ("asynchronousChannelProvider")}
     */
    protected AsynchronousChannelProvider() {
        this(checkPermission());
    }

    // lazy initialization of default provider
    private static class ProviderHolder {
        static final AsynchronousChannelProvider provider = load();

        @SuppressWarnings("removal")
        private static AsynchronousChannelProvider load() {
            return AccessController
                .doPrivileged(new PrivilegedAction<>() {
                    public AsynchronousChannelProvider run() {
                        AsynchronousChannelProvider p;
                        p = loadProviderFromProperty();
                        if (p != null)
                            return p;
                        p = loadProviderAsService();
                        if (p != null)
                            return p;
                        return sun.nio.ch.DefaultAsynchronousChannelProvider.create();
                    }});
        }

        private static AsynchronousChannelProvider loadProviderFromProperty() {
            String cn = System.getProperty("java.nio.channels.spi.AsynchronousChannelProvider");
            if (cn == null)
                return null;
            try {
                @SuppressWarnings("deprecation")
                Object tmp = Class.forName(cn, true,
                                           ClassLoader.getSystemClassLoader()).newInstance();
                return (AsynchronousChannelProvider)tmp;
            } catch (ClassNotFoundException x) {
                throw new ServiceConfigurationError(null, x);
            } catch (IllegalAccessException x) {
                throw new ServiceConfigurationError(null, x);
            } catch (InstantiationException x) {
                throw new ServiceConfigurationError(null, x);
            } catch (SecurityException x) {
                throw new ServiceConfigurationError(null, x);
            }
        }

        private static AsynchronousChannelProvider loadProviderAsService() {
            ServiceLoader<AsynchronousChannelProvider> sl =
                ServiceLoader.load(AsynchronousChannelProvider.class,
                                   ClassLoader.getSystemClassLoader());
            Iterator<AsynchronousChannelProvider> i = sl.iterator();
            for (;;) {
                try {
                    return (i.hasNext()) ? i.next() : null;
                } catch (ServiceConfigurationError sce) {
                    if (sce.getCause() instanceof SecurityException) {
                        // Ignore the security exception, try the next provider
                        continue;
                    }
                    throw sce;
                }
            }
        }
    }

    /**
     * Returns the system-wide default asynchronous channel provider for this
     * invocation of the Java virtual machine.
     *
     * <p> The first invocation of this method locates the default provider
     * object as follows: </p>
     *
     * <ol>
     *
     *   <li><p> If the system property
     *   {@systemProperty java.nio.channels.spi.AsynchronousChannelProvider} is
     *   defined then it is taken to be the fully-qualified name of a concrete
     *   provider class. The class is loaded and instantiated; if this process
     *   fails then an unspecified error is thrown.  </p></li>
     *
     *   <li><p> If a provider class has been installed in a jar file that is
     *   visible to the system class loader, and that jar file contains a
     *   provider-configuration file named
     *   {@code java.nio.channels.spi.AsynchronousChannelProvider} in the resource
     *   directory {@code META-INF/services}, then the first class name
     *   specified in that file is taken.  The class is loaded and
     *   instantiated; if this process fails then an unspecified error is
     *   thrown.  </p></li>
     *
     *   <li><p> Finally, if no provider has been specified by any of the above
     *   means then the system-default provider class is instantiated and the
     *   result is returned.  </p></li>
     *
     * </ol>
     *
     * <p> Subsequent invocations of this method return the provider that was
     * returned by the first invocation.  </p>
     *
     * @return  The system-wide default AsynchronousChannel provider
     */
    public static AsynchronousChannelProvider provider() {
        return ProviderHolder.provider;
    }

    /**
     * Constructs a new asynchronous channel group with a fixed thread pool.
     *
     * @param   nThreads
     *          The number of threads in the pool
     * @param   threadFactory
     *          The factory to use when creating new threads
     *
     * @return  A new asynchronous channel group
     *
     * @throws  IllegalArgumentException
     *          If {@code nThreads <= 0}
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @see AsynchronousChannelGroup#withFixedThreadPool
     */
    public abstract AsynchronousChannelGroup
        openAsynchronousChannelGroup(int nThreads, ThreadFactory threadFactory) throws IOException;

    /**
     * Constructs a new asynchronous channel group with the given thread pool.
     *
     * @param   executor
     *          The thread pool
     * @param   initialSize
     *          A value {@code >=0} or a negative value for implementation
     *          specific default
     *
     * @return  A new asynchronous channel group
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @see AsynchronousChannelGroup#withCachedThreadPool
     */
    public abstract AsynchronousChannelGroup
        openAsynchronousChannelGroup(ExecutorService executor, int initialSize) throws IOException;

    /**
     * Opens an asynchronous server-socket channel.
     *
     * @param   group
     *          The group to which the channel is bound, or {@code null} to
     *          bind to the default group
     *
     * @return  The new channel
     *
     * @throws  IllegalChannelGroupException
     *          If the provider that created the group differs from this provider
     * @throws  ShutdownChannelGroupException
     *          The group is shutdown
     * @throws  IOException
     *          If an I/O error occurs
     */
    public abstract AsynchronousServerSocketChannel openAsynchronousServerSocketChannel
        (AsynchronousChannelGroup group) throws IOException;

    /**
     * Opens an asynchronous socket channel.
     *
     * @param   group
     *          The group to which the channel is bound, or {@code null} to
     *          bind to the default group
     *
     * @return  The new channel
     *
     * @throws  IllegalChannelGroupException
     *          If the provider that created the group differs from this provider
     * @throws  ShutdownChannelGroupException
     *          The group is shutdown
     * @throws  IOException
     *          If an I/O error occurs
     */
    public abstract AsynchronousSocketChannel openAsynchronousSocketChannel
        (AsynchronousChannelGroup group) throws IOException;
}
