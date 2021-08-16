/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @author    Sun Microsystems, Inc.
 * @build        @BUILD_TAG_PLACEHOLDER@
 *
 * @COPYRIGHT_MINI_LEGAL_NOTICE_PLACEHOLDER@
 */

package sun.management.jmxremote;

import java.io.ObjectInputFilter;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

import sun.rmi.registry.RegistryImpl;

/** A Registry that consists of a single entry that never changes. */
public class SingleEntryRegistry extends RegistryImpl {
    SingleEntryRegistry(int port, String name, Remote object)
            throws RemoteException {
        super(port, null, null, SingleEntryRegistry::singleRegistryFilter);
        this.name = name;
        this.object = object;
    }

    SingleEntryRegistry(int port,
                        RMIClientSocketFactory csf,
                        RMIServerSocketFactory ssf,
                        String name,
                        Remote object)
            throws RemoteException {
        super(port, csf, ssf, SingleEntryRegistry::singleRegistryFilter);
        this.name = name;
        this.object = object;
    }

    public String[] list() {
        return new String[] {name};
    }

    public Remote lookup(String name) throws NotBoundException {
        if (name.equals(this.name))
            return object;
        throw new NotBoundException("Not bound: \"" + name + "\" (only " +
                                    "bound name is \"" + this.name + "\")");
    }

    public void bind(String name, Remote obj) throws AccessException {
        throw new AccessException("Cannot modify this registry");
    }

    public void rebind(String name, Remote obj) throws AccessException {
        throw new AccessException("Cannot modify this registry");
    }

    public void unbind(String name) throws AccessException {
        throw new AccessException("Cannot modify this registry");
    }

    /**
     * ObjectInputFilter to check parameters to SingleEntryRegistry.
     * Since it is a read-only Registry, no classes are accepted.
     * String arguments are accepted without passing them to the serialFilter.
     *
     * @param info a reference to the serialization filter information
     * @return Status.REJECTED if parameters are out of range
     */
    private static ObjectInputFilter.Status singleRegistryFilter(ObjectInputFilter.FilterInfo info) {
        return (info.serialClass() != null ||
                info.depth() > 2 ||
                info.references() > 4 ||
                info.arrayLength() >= 0)
        ? ObjectInputFilter.Status.REJECTED
        : ObjectInputFilter.Status.ALLOWED;
    }

    private final String name;
    private final Remote object;

    private static final long serialVersionUID = -4897238949499730950L;
}
