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

package com.sun.tools.attach.spi;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.AttachPermission;
import com.sun.tools.attach.AttachNotSupportedException;
import java.util.ServiceLoader;

/**
 * Attach provider class for attaching to a Java virtual machine.
 *
 * <p> An attach provider is a concrete subclass of this class that has a
 * zero-argument constructor and implements the abstract methods specified
 * below.
 *
 * <p> An attach provider implementation is typically tied to a Java virtual
 * machine implementation, version, or even mode of operation. That is, a specific
 * provider implementation will typically only be capable of attaching to
 * a specific Java virtual machine implementation or version. For example, Oracle's
 * JDK implementation ships with provider implementations that can only attach to
 * Oracle's <i>HotSpot</i> virtual machine. In general, if an environment
 * consists of Java virtual machines of different versions and from different
 * vendors then there will be an attach provider implementation for each
 * <i>family</i> of implementations or versions.
 *
 * <p> An attach provider is identified by its {@link #name <i>name</i>} and
 * {@link #type <i>type</i>}. The <i>name</i> is typically, but not required to
 * be, a name that corresponds to the VM vendor. The Oracle JDK implementation,
 * for example, ships with attach providers that use the package name <i>"sun"</i>
 * (for historical reasons). The
 * <i>type</i> typically corresponds to the attach mechanism. For example, an
 * implementation that uses the Doors inter-process communication mechanism
 * might use the type <i>"doors"</i>. The purpose of the name and type is to
 * identify providers in environments where there are multiple providers
 * installed.
 *
 * <p> AttachProvider implementations are loaded and instantiated at the first
 * invocation of the {@link #providers() providers} method. This method
 * attempts to load all provider implementations that are installed on the
 * platform.
 *
 * <p> All of the methods in this class are safe for use by multiple
 * concurrent threads.
 *
 * @since 1.6
 */

public abstract class AttachProvider {

    private static final Object lock = new Object();
    private static List<AttachProvider> providers = null;

    /**
     * Initializes a new instance of this class.
     *
     * @throws  SecurityException
     *          If a security manager has been installed and it denies
     *          {@link com.sun.tools.attach.AttachPermission AttachPermission}
     *          ("{@code createAttachProvider}")
     */
    protected AttachProvider() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(new AttachPermission("createAttachProvider"));
    }

    /**
     * Return this provider's name.
     *
     * @return  The name of this provider
     */
    public abstract String name();

    /**
     * Return this provider's type.
     *
     * @return  The type of this provider
     */
    public abstract String type();

    /**
     * Attaches to a Java virtual machine.
     *
     * <p> A Java virtual machine is identified by an abstract identifier. The
     * nature of this identifier is platform dependent but in many cases it will be the
     * string representation of the process identifier (or pid).
     *
     * <p> This method parses the identifier and maps the identifier to a Java
     * virtual machine (in an implementation dependent manner). If the identifier
     * cannot be parsed by the provider then an
     * {@link com.sun.tools.attach.AttachNotSupportedException AttachNotSupportedException}
     * is thrown. Once parsed this method attempts to attach to the Java virtual machine.
     * If the provider detects that the identifier corresponds to a Java virtual machine
     * that does not exist, or it corresponds to a Java virtual machine that does not support
     * the attach mechanism implemented by this provider, or it detects that the
     * Java virtual machine is a version to which this provider cannot attach, then
     * an {@code AttachNotSupportedException} is thrown.
     *
     * @param  id
     *         The abstract identifier that identifies the Java virtual machine.
     *
     * @return  VirtualMachine representing the target virtual machine.
     *
     * @throws  SecurityException
     *          If a security manager has been installed and it denies
     *          {@link com.sun.tools.attach.AttachPermission AttachPermission}
     *          ("{@code attachVirtualMachine}"), or other permission
     *          required by the implementation.
     *
     * @throws  AttachNotSupportedException
     *          If the identifier cannot be parsed, or it corresponds to
     *          to a Java virtual machine that does not exist, or it
     *          corresponds to a Java virtual machine which this
     *          provider cannot attach.
     *
     * @throws  IOException
     *          If some other I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code id} is {@code null}
     */
    public abstract VirtualMachine attachVirtualMachine(String id)
        throws AttachNotSupportedException, IOException;

    /**
     * Attaches to a Java virtual machine.
     *
     * <p> A Java virtual machine can be described using a
     * {@link com.sun.tools.attach.VirtualMachineDescriptor VirtualMachineDescriptor}.
     * This method invokes the descriptor's
     * {@link com.sun.tools.attach.VirtualMachineDescriptor#provider() provider()} method
     * to check that it is equal to this provider. It then attempts to attach to the
     * Java virtual machine.
     *
     * @param  vmd
     *         The virtual machine descriptor
     *
     * @return  VirtualMachine representing the target virtual machine.
     *
     * @throws  SecurityException
     *          If a security manager has been installed and it denies
     *          {@link com.sun.tools.attach.AttachPermission AttachPermission}
     *          ("{@code attachVirtualMachine}"), or other permission
     *          required by the implementation.
     *
     * @throws  AttachNotSupportedException
     *          If the descriptor's
     *          {@link com.sun.tools.attach.VirtualMachineDescriptor#provider() provider()}
     *          method returns a provider that is not this provider, or it does not
     *          correspond to a Java virtual machine to which this provider can attach.
     *
     * @throws  IOException
     *          If some other I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code vmd} is {@code null}
     */
    public VirtualMachine attachVirtualMachine(VirtualMachineDescriptor vmd)
        throws AttachNotSupportedException, IOException
    {
        if (vmd.provider() != this) {
            throw new AttachNotSupportedException("provider mismatch");
        }
        return attachVirtualMachine(vmd.id());
    }

    /**
     * Lists the Java virtual machines known to this provider.
     *
     * <p> This method returns a list of
     * {@link com.sun.tools.attach.VirtualMachineDescriptor} elements. Each
     * {@code VirtualMachineDescriptor} describes a Java virtual machine
     * to which this provider can <i>potentially</i> attach.  There isn't any
     * guarantee that invoking
     * {@link #attachVirtualMachine(VirtualMachineDescriptor) attachVirtualMachine}
     * on each descriptor in the list will succeed.
     *
     * @return  The list of virtual machine descriptors which describe the
     *          Java virtual machines known to this provider (may be empty).
     */
    public abstract List<VirtualMachineDescriptor> listVirtualMachines();


    /**
     * Returns a list of the installed attach providers.
     *
     * <p> An AttachProvider is installed on the platform if:
     *
     * <ul>
     *   <li>It is installed in a JAR file that is visible to the defining
     *   class loader of the AttachProvider type (usually, but not required
     *   to be, the {@link java.lang.ClassLoader#getSystemClassLoader system
     *   class loader}).</li>
     *
     *   <li>The JAR file contains a provider configuration named
     *   {@code com.sun.tools.attach.spi.AttachProvider} in the resource directory
     *   {@code META-INF/services}.</li>
     *
     *   <li>The provider configuration file lists the full-qualified class
     *   name of the AttachProvider implementation.</li>
     * </ul>
     *
     * <p> The format of the provider configuration file is one fully-qualified
     * class name per line. Space and tab characters surrounding each class name,
     * as well as blank lines are ignored. The comment character is
     *  {@code '#'} ({@code 0x23}), and on each line all characters following
     * the first comment character are ignored. The file must be encoded in
     * UTF-8.
     *
     * <p> AttachProvider implementations are loaded and instantiated
     * (using the zero-arg constructor) at the first invocation of this method.
     * The list returned by the first invocation of this method is the list
     * of providers. Subsequent invocations of this method return a list of the same
     * providers. The list is unmodifiable.
     *
     * @return  A list of the installed attach providers.
     */
    public static List<AttachProvider> providers() {
        synchronized (lock) {
            if (providers == null) {
                providers = new ArrayList<AttachProvider>();

                ServiceLoader<AttachProvider> providerLoader =
                    ServiceLoader.load(AttachProvider.class,
                                       AttachProvider.class.getClassLoader());

                Iterator<AttachProvider> i = providerLoader.iterator();

                while (i.hasNext()) {
                    try {
                        providers.add(i.next());
                    } catch (Throwable t) {
                        if (t instanceof ThreadDeath) {
                            ThreadDeath td = (ThreadDeath)t;
                            throw td;
                        }
                        // Log errors and exceptions since we cannot return them
                        t.printStackTrace();
                    }
                }
            }
            return Collections.unmodifiableList(providers);
        }
    }
}
