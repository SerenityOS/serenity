/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.attach;

import com.sun.tools.attach.spi.AttachProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.io.IOException;


/**
 * A Java virtual machine.
 *
 * <p> A {@code VirtualMachine} represents a Java virtual machine to which this
 * Java virtual machine has attached. The Java virtual machine to which it is
 * attached is sometimes called the <i>target virtual machine</i>, or <i>target VM</i>.
 * An application (typically a tool such as a managemet console or profiler) uses a
 * VirtualMachine to load an agent into the target VM. For example, a profiler tool
 * written in the Java Language might attach to a running application and load its
 * profiler agent to profile the running application. </p>
 *
 * <p> A VirtualMachine is obtained by invoking the {@link #attach(String) attach} method
 * with an identifier that identifies the target virtual machine. The identifier is
 * implementation-dependent but is typically the process identifier (or pid) in
 * environments where each Java virtual machine runs in its own operating system process.
 * Alternatively, a {@code VirtualMachine} instance is obtained by invoking the
 * {@link #attach(VirtualMachineDescriptor) attach} method with a {@link
 * com.sun.tools.attach.VirtualMachineDescriptor VirtualMachineDescriptor} obtained
 * from the list of virtual machine descriptors returned by the {@link #list list} method.
 * Once a reference to a virtual machine is obtained, the {@link #loadAgent loadAgent},
 * {@link #loadAgentLibrary loadAgentLibrary}, and {@link #loadAgentPath loadAgentPath}
 * methods are used to load agents into target virtual machine. The {@link
 * #loadAgent loadAgent} method is used to load agents that are written in the Java
 * Language and deployed in a {@link java.util.jar.JarFile JAR file}. (See
 * {@link java.lang.instrument} for a detailed description on how these agents
 * are loaded and started). The {@link #loadAgentLibrary loadAgentLibrary} and
 * {@link #loadAgentPath loadAgentPath} methods are used to load agents that
 * are deployed either in a dynamic library or statically linked into the VM and
 * make use of the <a href="{@docRoot}/../specs/jvmti.html">JVM Tools Interface</a>.
 * </p>
 *
 * <p> In addition to loading agents a VirtualMachine provides read access to the
 * {@link java.lang.System#getProperties() system properties} in the target VM.
 * This can be useful in some environments where properties such as
 * {@code java.home}, {@code os.name}, or {@code os.arch} are
 * used to construct the path to agent that will be loaded into the target VM.
 *
 * <p> The following example demonstrates how VirtualMachine may be used:</p>
 *
 * <pre>
 *
 *      // attach to target VM
 *      VirtualMachine vm = VirtualMachine.attach("2177");
 *
 *      // start management agent
 *      Properties props = new Properties();
 *      props.put("com.sun.management.jmxremote.port", "5000");
 *      vm.startManagementAgent(props);
 *
 *      // detach
 *      vm.detach();
 *
 * </pre>
 *
 * <p> In this example we attach to a Java virtual machine that is identified by
 * the process identifier {@code 2177}. Then the JMX management agent is
 * started in the target process using the supplied arguments. Finally, the
 * client detaches from the target VM. </p>
 *
 * <p> A VirtualMachine is safe for use by multiple concurrent threads. </p>
 *
 * @since 1.6
 */

public abstract class VirtualMachine {
    private AttachProvider provider;
    private String id;
    private volatile int hash;        // 0 => not computed

    /**
     * Initializes a new instance of this class.
     *
     * @param   provider
     *          The attach provider creating this class.
     * @param   id
     *          The abstract identifier that identifies the Java virtual machine.
     *
     * @throws  NullPointerException
     *          If {@code provider} or {@code id} is {@code null}.
     */
    protected VirtualMachine(AttachProvider provider, String id) {
        if (provider == null) {
            throw new NullPointerException("provider cannot be null");
        }
        if (id == null) {
            throw new NullPointerException("id cannot be null");
        }
        this.provider = provider;
        this.id = id;
    }

    /**
     * Return a list of Java virtual machines.
     *
     * <p> This method returns a list of Java {@link
     * com.sun.tools.attach.VirtualMachineDescriptor} elements.
     * The list is an aggregation of the virtual machine
     * descriptor lists obtained by invoking the {@link
     * com.sun.tools.attach.spi.AttachProvider#listVirtualMachines
     * listVirtualMachines} method of all installed
     * {@link com.sun.tools.attach.spi.AttachProvider attach providers}.
     * If there are no Java virtual machines known to any provider
     * then an empty list is returned.
     *
     * @return  The list of virtual machine descriptors.
     */
    public static List<VirtualMachineDescriptor> list() {
        ArrayList<VirtualMachineDescriptor> l =
            new ArrayList<VirtualMachineDescriptor>();
        List<AttachProvider> providers = AttachProvider.providers();
        for (AttachProvider provider: providers) {
            l.addAll(provider.listVirtualMachines());
        }
        return l;
    }

    /**
     * Attaches to a Java virtual machine.
     *
     * <p> This method obtains the list of attach providers by invoking the
     * {@link com.sun.tools.attach.spi.AttachProvider#providers()
     * AttachProvider.providers()} method. It then iterates overs the list
     * and invokes each provider's {@link
     * com.sun.tools.attach.spi.AttachProvider#attachVirtualMachine(java.lang.String)
     * attachVirtualMachine} method in turn. If a provider successfully
     * attaches then the iteration terminates, and the VirtualMachine created
     * by the provider that successfully attached is returned by this method.
     * If the {@code attachVirtualMachine} method of all providers throws
     * {@link com.sun.tools.attach.AttachNotSupportedException AttachNotSupportedException}
     * then this method also throws {@code AttachNotSupportedException}.
     * This means that {@code AttachNotSupportedException} is thrown when
     * the identifier provided to this method is invalid, or the identifier
     * corresponds to a Java virtual machine that does not exist, or none
     * of the providers can attach to it. This exception is also thrown if
     * {@link com.sun.tools.attach.spi.AttachProvider#providers()
     * AttachProvider.providers()} returns an empty list. </p>
     *
     * @param   id
     *          The abstract identifier that identifies the Java virtual machine.
     *
     * @return  A VirtualMachine representing the target VM.
     *
     * @throws  SecurityException
     *          If a security manager has been installed and it denies
     *          {@link com.sun.tools.attach.AttachPermission AttachPermission}
     *          {@code ("attachVirtualMachine")}, or another permission
     *          required by the implementation.
     *
     * @throws  AttachNotSupportedException
     *          If the {@code attachVirtualmachine} method of all installed
     *          providers throws {@code AttachNotSupportedException}, or
     *          there aren't any providers installed.
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code id} is {@code null}.
     */
    public static VirtualMachine attach(String id)
        throws AttachNotSupportedException, IOException
    {
        if (id == null) {
            throw new NullPointerException("id cannot be null");
        }
        List<AttachProvider> providers = AttachProvider.providers();
        if (providers.size() == 0) {
            throw new AttachNotSupportedException("no providers installed");
        }
        AttachNotSupportedException lastExc = null;
        for (AttachProvider provider: providers) {
            try {
                return provider.attachVirtualMachine(id);
            } catch (AttachNotSupportedException x) {
                lastExc = x;
            }
        }
        throw lastExc;
    }

    /**
     * Attaches to a Java virtual machine.
     *
     * <p> This method first invokes the {@link
     * com.sun.tools.attach.VirtualMachineDescriptor#provider() provider()} method
     * of the given virtual machine descriptor to obtain the attach provider. It
     * then invokes the attach provider's {@link
     * com.sun.tools.attach.spi.AttachProvider#attachVirtualMachine(VirtualMachineDescriptor)
     * attachVirtualMachine} to attach to the target VM.
     *
     * @param   vmd
     *          The virtual machine descriptor.
     *
     * @return  A VirtualMachine representing the target VM.
     *
     * @throws  SecurityException
     *          If a security manager has been installed and it denies
     *          {@link com.sun.tools.attach.AttachPermission AttachPermission}
     *          {@code ("attachVirtualMachine")}, or another permission
     *          required by the implementation.
     *
     * @throws  AttachNotSupportedException
     *          If the attach provider's {@code attachVirtualmachine}
     *          throws {@code AttachNotSupportedException}.
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code vmd} is {@code null}.
     */
    public static VirtualMachine attach(VirtualMachineDescriptor vmd)
        throws AttachNotSupportedException, IOException
    {
        return vmd.provider().attachVirtualMachine(vmd);
    }

    /**
     * Detach from the virtual machine.
     *
     * <p> After detaching from the virtual machine, any further attempt to invoke
     * operations on that virtual machine will cause an {@link java.io.IOException
     * IOException} to be thrown. If an operation (such as {@link #loadAgent
     * loadAgent} for example) is in progress when this method is invoked then
     * the behaviour is implementation dependent. In other words, it is
     * implementation specific if the operation completes or throws
     * {@code IOException}.
     *
     * <p> If already detached from the virtual machine then invoking this
     * method has no effect. </p>
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public abstract void detach() throws IOException;

    /**
     * Returns the provider that created this virtual machine.
     *
     * @return  The provider that created this virtual machine.
     */
    public final AttachProvider provider() {
        return provider;
    }

    /**
     * Returns the identifier for this Java virtual machine.
     *
     * @return  The identifier for this Java virtual machine.
     */
    public final String id() {
        return id;
    }

    /**
     * Loads an agent library.
     *
     * <p> A <a href="{@docRoot}/../specs/jvmti.html">JVM TI</a>
     * client is called an <i>agent</i>. It is developed in a native language.
     * A JVM TI agent is deployed in a platform specific manner but it is typically the
     * platform equivalent of a dynamic library. Alternatively, it may be statically linked into the VM.
     * This method causes the given agent library to be loaded into the target
     * VM (if not already loaded or if not statically linked into the VM).
     * It then causes the target VM to invoke the {@code Agent_OnAttach} function
     * or, for a statically linked agent named 'L', the {@code Agent_OnAttach_L} function
     * as specified in the
     * <a href="{@docRoot}/../specs/jvmti.html">JVM Tools Interface</a> specification.
     * Note that the {@code Agent_OnAttach[_L]}
     * function is invoked even if the agent library was loaded prior to invoking
     * this method.
     *
     * <p> The agent library provided is the name of the agent library. It is interpreted
     * in the target virtual machine in an implementation-dependent manner. Typically an
     * implementation will expand the library name into an operating system specific file
     * name. For example, on UNIX systems, the name {@code L} might be expanded to
     * {@code libL.so}, and located using the search path specified by the
     * {@code LD_LIBRARY_PATH} environment variable. If the agent named 'L' is
     * statically linked into the VM then the VM must export a function named
     * {@code Agent_OnAttach_L}.</p>
     *
     * <p> If the {@code Agent_OnAttach[_L]} function in the agent library returns
     * an error then an {@link com.sun.tools.attach.AgentInitializationException} is
     * thrown. The return value from the {@code Agent_OnAttach[_L]} can then be
     * obtained by invoking the {@link
     * com.sun.tools.attach.AgentInitializationException#returnValue() returnValue}
     * method on the exception. </p>
     *
     * @param   agentLibrary
     *          The name of the agent library.
     *
     * @param   options
     *          The options to provide to the {@code Agent_OnAttach[_L]}
     *          function (can be {@code null}).
     *
     * @throws  AgentLoadException
     *          If the agent library does not exist, the agent library is not
     *          statically linked with the VM, or the agent library cannot be
     *          loaded for another reason.
     *
     * @throws  AgentInitializationException
     *          If the {@code Agent_OnAttach[_L]} function returns an error.
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code agentLibrary} is {@code null}.
     *
     * @see     com.sun.tools.attach.AgentInitializationException#returnValue()
     */
    public abstract void loadAgentLibrary(String agentLibrary, String options)
        throws AgentLoadException, AgentInitializationException, IOException;

    /**
     * Loads an agent library.
     *
     * <p> This convenience method works as if by invoking:
     *
     * <blockquote><code>
     * {@link #loadAgentLibrary(String, String) loadAgentLibrary}(agentLibrary,&nbsp;null);
     * </code></blockquote>
     *
     * @param   agentLibrary
     *          The name of the agent library.
     *
     * @throws  AgentLoadException
     *          If the agent library does not exist, the agent library is not
     *          statically linked with the VM, or the agent library cannot be
     *          loaded for another reason.
     *
     * @throws  AgentInitializationException
     *          If the {@code Agent_OnAttach[_L]} function returns an error.
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code agentLibrary} is {@code null}.
     */
    public void loadAgentLibrary(String agentLibrary)
        throws AgentLoadException, AgentInitializationException, IOException
    {
        loadAgentLibrary(agentLibrary, null);
    }

    /**
     * Load a native agent library by full pathname.
     *
     * <p> A <a href="{@docRoot}/../specs/jvmti.html">JVM TI</a>
     * client is called an <i>agent</i>. It is developed in a native language.
     * A JVM TI agent is deployed in a platform specific manner but it is typically the
     * platform equivalent of a dynamic library. Alternatively, the native
     * library specified by the agentPath parameter may be statically
     * linked with the VM. The parsing of the agentPath parameter into
     * a statically linked library name is done in a platform
     * specific manner in the VM. For example, in UNIX, an agentPath parameter
     * of {@code /a/b/libL.so} would name a library 'L'.
     *
     * See the JVM TI Specification for more details.
     *
     * This method causes the given agent library to be loaded into the target
     * VM (if not already loaded or if not statically linked into the VM).
     * It then causes the target VM to invoke the {@code Agent_OnAttach}
     * function or, for a statically linked agent named 'L', the
     * {@code Agent_OnAttach_L} function as specified in the
     * <a href="{@docRoot}/../specs/jvmti.html">JVM Tools Interface</a> specification.
     * Note that the {@code Agent_OnAttach[_L]}
     * function is invoked even if the agent library was loaded prior to invoking
     * this method.
     *
     * <p> The agent library provided is the absolute path from which to load the
     * agent library. Unlike {@link #loadAgentLibrary loadAgentLibrary}, the library name
     * is not expanded in the target virtual machine. </p>
     *
     * <p> If the {@code Agent_OnAttach[_L]} function in the agent library returns
     * an error then an {@link com.sun.tools.attach.AgentInitializationException} is
     * thrown. The return value from the {@code Agent_OnAttach[_L]} can then be
     * obtained by invoking the {@link
     * com.sun.tools.attach.AgentInitializationException#returnValue() returnValue}
     * method on the exception. </p>
     *
     * @param   agentPath
     *          The full path of the agent library.
     *
     * @param   options
     *          The options to provide to the {@code Agent_OnAttach[_L]}
     *          function (can be {@code null}).
     *
     * @throws  AgentLoadException
     *          If the agent library does not exist, the agent library is not
     *          statically linked with the VM, or the agent library cannot be
     *          loaded for another reason.
     *
     * @throws  AgentInitializationException
     *          If the {@code Agent_OnAttach[_L]} function returns an error.
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code agentPath} is {@code null}.
     *
     * @see     com.sun.tools.attach.AgentInitializationException#returnValue()
     */
    public abstract void loadAgentPath(String agentPath, String options)
        throws AgentLoadException, AgentInitializationException, IOException;

    /**
     * Load a native agent library by full pathname.
     *
     * <p> This convenience method works as if by invoking:
     *
     * <blockquote><code>
     * {@link #loadAgentPath(String, String) loadAgentPath}(agentLibrary,&nbsp;null);
     * </code></blockquote>
     *
     * @param   agentPath
     *          The full path to the agent library.
     *
     * @throws  AgentLoadException
     *          If the agent library does not exist, the agent library is not
     *          statically linked with the VM, or the agent library cannot be
     *          loaded for another reason.
     *
     * @throws  AgentInitializationException
     *          If the {@code Agent_OnAttach[_L]} function returns an error.
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code agentPath} is {@code null}.
     */
    public void loadAgentPath(String agentPath)
       throws AgentLoadException, AgentInitializationException, IOException
    {
        loadAgentPath(agentPath, null);
    }


   /**
     * Loads an agent.
     *
     * <p> The agent provided to this method is a path name to a JAR file on the file
     * system of the target virtual machine. This path is passed to the target virtual
     * machine where it is interpreted. The target virtual machine attempts to start
     * the agent as specified by the {@link java.lang.instrument} specification.
     * That is, the specified JAR file is added to the system class path (of the target
     * virtual machine), and the {@code agentmain} method of the agent class, specified
     * by the {@code Agent-Class} attribute in the JAR manifest, is invoked. This
     * method completes when the {@code agentmain} method completes.
     *
     * @param   agent
     *          Path to the JAR file containing the agent.
     *
     * @param   options
     *          The options to provide to the agent's {@code agentmain}
     *          method (can be {@code null}).
     *
     * @throws  AgentLoadException
     *          If the agent does not exist, or cannot be started in the manner
     *          specified in the {@link java.lang.instrument} specification.
     *
     * @throws  AgentInitializationException
     *          If the {@code agentmain} throws an exception
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code agent} is {@code null}.
     */
    public abstract void loadAgent(String agent, String options)
        throws AgentLoadException, AgentInitializationException, IOException;

    /**
     * Loads an agent.
     *
     * <p> This convenience method works as if by invoking:
     *
     * <blockquote><code>
     * {@link #loadAgent(String, String) loadAgent}(agent,&nbsp;null);
     * </code></blockquote>
     *
     * @param   agent
     *          Path to the JAR file containing the agent.
     *
     * @throws  AgentLoadException
     *          If the agent does not exist, or cannot be started in the manner
     *          specified in the {@link java.lang.instrument} specification.
     *
     * @throws  AgentInitializationException
     *          If the {@code agentmain} throws an exception
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code agent} is {@code null}.
     */
    public void loadAgent(String agent)
        throws AgentLoadException, AgentInitializationException, IOException
    {
        loadAgent(agent, null);
    }

    /**
     * Returns the current system properties in the target virtual machine.
     *
     * <p> This method returns the system properties in the target virtual
     * machine. Properties whose key or value is not a {@code String} are
     * omitted. The method is approximately equivalent to the invocation of the
     * method {@link java.lang.System#getProperties System.getProperties}
     * in the target virtual machine except that properties with a key or
     * value that is not a {@code String} are not included.
     *
     * <p> This method is typically used to decide which agent to load into
     * the target virtual machine with {@link #loadAgent loadAgent}, or
     * {@link #loadAgentLibrary loadAgentLibrary}. For example, the
     * {@code java.home} or {@code user.dir} properties might be
     * use to create the path to the agent library or JAR file.
     *
     * @return  The system properties
     *
     * @throws  AttachOperationFailedException
     *          If the target virtual machine is unable to complete the
     *          attach operation. A more specific error message will be
     *          given by {@link AttachOperationFailedException#getMessage()}.
     *
     * @throws  IOException
     *          If an I/O error occurs, a communication error for example,
     *          that cannot be identified as an error to indicate that the
     *          operation failed in the target VM.
     *
     * @see     java.lang.System#getProperties
     * @see     #loadAgentLibrary
     * @see     #loadAgent
     */
    public abstract Properties getSystemProperties() throws IOException;

    /**
     * Returns the current <i>agent properties</i> in the target virtual
     * machine.
     *
     * <p> The target virtual machine can maintain a list of properties on
     * behalf of agents. The manner in which this is done, the names of the
     * properties, and the types of values that are allowed, is implementation
     * specific. Agent properties are typically used to store communication
     * end-points and other agent configuration details. For example, a debugger
     * agent might create an agent property for its transport address.
     *
     * <p> This method returns the agent properties whose key and value is a
     * {@code String}. Properties whose key or value is not a {@code String}
     * are omitted. If there are no agent properties maintained in the target
     * virtual machine then an empty property list is returned.
     *
     * @return       The agent properties
     *
     * @throws       AttachOperationFailedException
     *               If the target virtual machine is unable to complete the
     *               attach operation. A more specific error message will be
     *               given by {@link AttachOperationFailedException#getMessage()}.
     *
     * @throws       IOException
     *               If an I/O error occurs, a communication error for example,
     *               that cannot be identified as an error to indicate that the
     *               operation failed in the target VM.
     */
    public abstract Properties getAgentProperties() throws IOException;

    /**
     * Starts the JMX management agent in the target virtual machine.
     *
     * <p> The configuration properties are the same as those specified on
     * the command line when starting the JMX management agent. In the same
     * way as on the command line, you need to specify at least the
     * {@code com.sun.management.jmxremote.port} property.
     *
     * <p> See the online documentation for
     * {@extLink monitoring_and_management_using_jmx_technology
     * Monitoring and Management Using JMX Technology} for further details.
     *
     * @param   agentProperties
     *          A Properties object containing the configuration properties
     *          for the agent.
     *
     * @throws  AttachOperationFailedException
     *          If the target virtual machine is unable to complete the
     *          attach operation. A more specific error message will be
     *          given by {@link AttachOperationFailedException#getMessage()}.
     *
     * @throws  IOException
     *          If an I/O error occurs, a communication error for example,
     *          that cannot be identified as an error to indicate that the
     *          operation failed in the target VM.
     *
     * @throws  IllegalArgumentException
     *          If keys or values in agentProperties are invalid.
     *
     * @throws  NullPointerException
     *          If agentProperties is null.
     *
     * @since   1.8
     */
    public abstract void startManagementAgent(Properties agentProperties) throws IOException;

    /**
     * Starts the local JMX management agent in the target virtual machine.
     *
     * <p> See the online documentation for
     * {@extLink monitoring_and_management_using_jmx_technology
     * Monitoring and Management Using JMX Technology} for further details.
     *
     * @return  The String representation of the local connector's service address.
     *          The value can be parsed by the
     *          {@link javax.management.remote.JMXServiceURL#JMXServiceURL(String)}
     *          constructor.
     *
     * @throws  AttachOperationFailedException
     *          If the target virtual machine is unable to complete the
     *          attach operation. A more specific error message will be
     *          given by {@link AttachOperationFailedException#getMessage()}.
     *
     * @throws  IOException
     *          If an I/O error occurs, a communication error for example,
     *          that cannot be identified as an error to indicate that the
     *          operation failed in the target VM.
     *
     * @since   1.8
     */
    public abstract String startLocalManagementAgent() throws IOException;

    /**
     * Returns a hash-code value for this VirtualMachine. The hash
     * code is based upon the VirtualMachine's components, and satifies
     * the general contract of the {@link java.lang.Object#hashCode()
     * Object.hashCode} method.
     *
     * @return  A hash-code value for this virtual machine
     */
    public int hashCode() {
        if (hash != 0) {
            return hash;
        }
        hash = provider.hashCode() * 127 + id.hashCode();
        return hash;
    }

    /**
     * Tests this VirtualMachine for equality with another object.
     *
     * <p> If the given object is not a VirtualMachine then this
     * method returns {@code false}. For two VirtualMachines to
     * be considered equal requires that they both reference the same
     * provider, and their {@link VirtualMachineDescriptor#id() identifiers} are equal. </p>
     *
     * <p> This method satisfies the general contract of the {@link
     * java.lang.Object#equals(Object) Object.equals} method. </p>
     *
     * @param   ob   The object to which this object is to be compared
     *
     * @return  {@code true} if, and only if, the given object is
     *                a VirtualMachine that is equal to this
     *                VirtualMachine.
     */
    public boolean equals(Object ob) {
        if (ob == this)
            return true;
        if (!(ob instanceof VirtualMachine))
            return false;
        VirtualMachine other = (VirtualMachine)ob;
        if (other.provider() != this.provider()) {
            return false;
        }
        if (!other.id().equals(this.id())) {
            return false;
        }
        return true;
    }

    /**
     * Returns the string representation of the {@code VirtualMachine}.
     */
    public String toString() {
        return provider.toString() + ": " + id;
    }
}
