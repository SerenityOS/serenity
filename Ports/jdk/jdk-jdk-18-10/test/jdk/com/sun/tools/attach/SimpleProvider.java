/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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

/*
 *
 *
 * Simple ("no-op") AttachProvider used in unit tests for Attach API.
 */
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.spi.AttachProvider;

import java.io.IOException;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

/*
 * AttachProvider implementation
 */
public class SimpleProvider extends AttachProvider {
    public SimpleProvider() {
    }

    public String name() {
        return "simple";
    }

    public String type() {
        return "none";
    }

    public VirtualMachine attachVirtualMachine(String id)
        throws AttachNotSupportedException, IOException
    {
        if (!id.startsWith("simple:")) {
            throw new AttachNotSupportedException("id not recognized");
        }
        return new SimpleVirtualMachine(this, id);
    }

    public List<VirtualMachineDescriptor> listVirtualMachines() {
        return new ArrayList<VirtualMachineDescriptor>();
    }
}

class SimpleVirtualMachine extends VirtualMachine {
    public SimpleVirtualMachine(AttachProvider provider, String id) {
        super(provider, id);
    }

    public void detach() throws IOException {
    }

    public void loadAgentLibrary(String agentLibrary, String options)
        throws IOException, AgentLoadException, AgentInitializationException
    {
    }

    public void loadAgentPath(String agentLibrary, String options)
        throws IOException, AgentLoadException, AgentInitializationException
    {
    }

    public void loadAgent(String agentLibrary, String options)
        throws IOException, AgentLoadException, AgentInitializationException
    {
    }

    public Properties getSystemProperties() throws IOException {
        return new Properties();
    }

    public Properties getAgentProperties() throws IOException {
        return new Properties();
    }

    public void dataDumpRequest() throws IOException {
    }

    public String startLocalManagementAgent() {
        return null;
    }

    public void startManagementAgent(Properties agentProperties) {
    }

}
