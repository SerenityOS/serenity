/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.jdi;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.*;
import com.sun.jdi.connect.spi.*;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/*
 * An AttachingConnector that uses the SharedMemoryTransportService
 */
public class SharedMemoryAttachingConnector extends GenericAttachingConnector {

    static final String ARG_NAME = "name";

    public SharedMemoryAttachingConnector() {
        super(new SharedMemoryTransportService());

        addStringArgument(
            ARG_NAME,
            getString("memory_attaching.name.label"),
            getString("memory_attaching.name"),
            "",
            true);

        transport = new Transport() {
            public String name() {
                return "dt_shmem";              // for compatibility reasons
            }
        };
    }

    public VirtualMachine
        attach(Map<String, ? extends Connector.Argument> arguments)
        throws IOException, IllegalConnectorArgumentsException
    {
        String name = argument(ARG_NAME, arguments).value();
        return super.attach(name, arguments);
    }

    public String name() {
        return "com.sun.jdi.SharedMemoryAttach";
    }

    public String description() {
       return getString("memory_attaching.description");
    }
}
