/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.agent;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;

import static jdk.internal.agent.AgentConfigurationError.*;
import jdk.internal.agent.spi.AgentProvider;
import jdk.internal.vm.VMSupport;
import sun.management.jdp.JdpController;
import sun.management.jdp.JdpException;
import sun.management.jmxremote.ConnectorBootstrap;

/**
 * This class provides the methods to start the management agent.
 * 1. {@link #startAgent} method is invoked by the VM if {@code -Dcom.sun.management.*} is set
 * 2. {@link #startLocalManagementAgent} or {@link #startRemoteManagementAgent}
 *    is invoked to start the management agent after the VM starts
 *    via jcmd ManagementAgent.start and start_local command.
 */
public class Agent {
    /**
     * Agent status collector strategy class
     */
    private static abstract class StatusCollector {
        protected static final Map<String, String> DEFAULT_PROPS = new HashMap<>();

        static {
            DEFAULT_PROPS.put(ConnectorBootstrap.PropertyNames.PORT,
                              ConnectorBootstrap.DefaultValues.PORT);
            DEFAULT_PROPS.put(ConnectorBootstrap.PropertyNames.USE_LOCAL_ONLY,
                              ConnectorBootstrap.DefaultValues.USE_LOCAL_ONLY);
            DEFAULT_PROPS.put(ConnectorBootstrap.PropertyNames.USE_AUTHENTICATION,
                              ConnectorBootstrap.DefaultValues.USE_AUTHENTICATION);
            DEFAULT_PROPS.put(ConnectorBootstrap.PropertyNames.USE_SSL,
                              ConnectorBootstrap.DefaultValues.USE_SSL);
            DEFAULT_PROPS.put(ConnectorBootstrap.PropertyNames.USE_REGISTRY_SSL,
                              ConnectorBootstrap.DefaultValues.USE_REGISTRY_SSL);
            DEFAULT_PROPS.put(ConnectorBootstrap.PropertyNames.SSL_NEED_CLIENT_AUTH,
                              ConnectorBootstrap.DefaultValues.SSL_NEED_CLIENT_AUTH);
            DEFAULT_PROPS.put(ConnectorBootstrap.PropertyNames.CONFIG_FILE_NAME,
                              ConnectorBootstrap.DefaultValues.CONFIG_FILE_NAME);
            DEFAULT_PROPS.put(ConnectorBootstrap.PropertyNames.PASSWORD_FILE_NAME,
                              ConnectorBootstrap.DefaultValues.PASSWORD_FILE_NAME);
            DEFAULT_PROPS.put(ConnectorBootstrap.PropertyNames.ACCESS_FILE_NAME,
                              ConnectorBootstrap.DefaultValues.ACCESS_FILE_NAME);

        }

        final protected StringBuilder sb = new StringBuilder();
        final public String collect() {
            Properties agentProps = VMSupport.getAgentProperties();
            String localConnAddr = (String)agentProps.get(LOCAL_CONNECTOR_ADDRESS_PROP);
            if (localConnAddr != null || jmxServer != null) {
                addAgentStatus(true);
                appendConnections(localConnAddr);
            } else {
                addAgentStatus(false);
            }
            return sb.toString();
        }

        private void appendConnections(String localConnAddr) {
            appendConnectionsHeader();
            if (localConnAddr != null) {
                try {
                    JMXServiceURL u = new JMXServiceURL(localConnAddr);
                    addConnection(false, u);
                } catch (MalformedURLException e) {
                    // will never happen
                }

            }
            if (jmxServer != null) {
                addConnection(true, jmxServer.getAddress());
            }
            appendConnectionsFooter();
        }

        private void addConnection(boolean remote, JMXServiceURL u) {
            appendConnectionHeader(remote);
            addConnectionDetails(u);
            addConfigProperties();
            appendConnectionFooter(remote);
        }

        private void addConfigProperties() {
            appendConfigPropsHeader();

            Properties remoteProps = configProps != null ?
                                        configProps : getManagementProperties();
            Map<Object, Object> props = new HashMap<>(DEFAULT_PROPS);

            if (remoteProps == null) {
                // local connector only
                String loc_only = System.getProperty(
                    ConnectorBootstrap.PropertyNames.USE_LOCAL_ONLY
                );

                if (loc_only != null &&
                    !ConnectorBootstrap.DefaultValues.USE_LOCAL_ONLY.equals(loc_only)) {
                    props.put(
                        ConnectorBootstrap.PropertyNames.USE_LOCAL_ONLY,
                        loc_only
                    );
                }
            } else {
                props.putAll(remoteProps);
            }

            props.entrySet().stream()
                .filter(preprocess(Map.Entry::getKey, StatusCollector::isManagementProp))
                .forEach(this::addConfigProp);

            appendConfigPropsFooter();
        }

        private static boolean isManagementProp(Object pName) {
            return pName != null && pName.toString().startsWith("com.sun.management.");
        }

        private static <T, V> Predicate<T> preprocess(Function<T, V> f, Predicate<V> p) {
            return (T t) -> p.test(f.apply(t));
        }

        abstract protected void addAgentStatus(boolean enabled);
        abstract protected void appendConnectionsHeader();
        abstract protected void appendConnectionsFooter();
        abstract protected void addConnectionDetails(JMXServiceURL u);
        abstract protected void appendConnectionHeader(boolean remote);
        abstract protected void appendConnectionFooter(boolean remote);
        abstract protected void appendConfigPropsHeader();
        abstract protected void appendConfigPropsFooter();
        abstract protected void addConfigProp(Map.Entry<?, ?> prop);
    }

    /**
     * Free-text status collector strategy implementation
     */
    final private static class TextStatusCollector extends StatusCollector {

        @Override
        protected void addAgentStatus(boolean enabled) {
            sb.append("Agent: ").append(enabled ? "enabled" : "disabled").append('\n');
        }

        @Override
        protected void appendConnectionsHeader() {
            sb.append('\n');
        }

        @Override
        protected void addConnectionDetails(JMXServiceURL u) {
            sb.append("Protocol       : ").append(u.getProtocol()).append('\n')
              .append("Host           : ").append(u.getHost()).append('\n')
              .append("URL            : ").append(u).append('\n');
        }

        @Override
        protected void appendConnectionHeader(boolean remote) {
            sb.append("Connection Type: ").append(remote ? "remote" : "local").append('\n');
        }

        @Override
        protected void appendConfigPropsHeader() {
            sb.append("Properties     :\n");
        }

        @Override
        protected void addConfigProp(Map.Entry<?, ?> prop) {
            sb.append("  ").append(prop.getKey()).append(" = ")
              .append(prop.getValue());
            Object defVal = DEFAULT_PROPS.get(prop.getKey());
            if (defVal != null && defVal.equals(prop.getValue())) {
                sb.append(" [default]");
            }
            sb.append("\n");
        }

        @Override
        protected void appendConnectionsFooter() {}

        @Override
        protected void appendConnectionFooter(boolean remote) {
            sb.append('\n');
        }

        @Override
        protected void appendConfigPropsFooter() {}
    }

    // management properties

    private static Properties mgmtProps;
    private static ResourceBundle messageRB;
    private static final String CONFIG_FILE =
            "com.sun.management.config.file";
    private static final String JMXREMOTE =
            "com.sun.management.jmxremote";
    private static final String JMXREMOTE_PORT =
            "com.sun.management.jmxremote.port";
    private static final String RMI_PORT =
            "com.sun.management.jmxremote.rmi.port";
    private static final String ENABLE_THREAD_CONTENTION_MONITORING =
            "com.sun.management.enableThreadContentionMonitoring";
    private static final String LOCAL_CONNECTOR_ADDRESS_PROP =
            "com.sun.management.jmxremote.localConnectorAddress";

    private static final String JDP_DEFAULT_ADDRESS = "224.0.23.178";
    private static final int JDP_DEFAULT_PORT = 7095;

    // The only active agent allowed
    private static JMXConnectorServer jmxServer = null;
    // The properties used to configure the server
    private static Properties configProps = null;

    // Parse string com.sun.management.prop=xxx,com.sun.management.prop=yyyy
    // and return property set if args is null or empty
    // return empty property set
    private static Properties parseString(String args) {
        Properties argProps = new Properties();
        if (args != null && !args.trim().isEmpty()) {
            for (String option : args.split(",")) {
                String s[] = option.split("=", 2);
                String name = s[0].trim();
                String value = (s.length > 1) ? s[1].trim() : "";

                if (!name.startsWith("com.sun.management.")) {
                    error(INVALID_OPTION, name);
                }

                argProps.setProperty(name, value);
            }
        }

        return argProps;
    }

    // invoked by -javaagent or -Dcom.sun.management.agent.class
    public static void premain(String args) throws Exception {
        agentmain(args);
    }

    // invoked by attach mechanism
    public static void agentmain(String args) throws Exception {
        if (args == null || args.length() == 0) {
            args = JMXREMOTE;           // default to local management
        }

        Properties arg_props = parseString(args);

        // Read properties from the config file
        Properties config_props = new Properties();
        String fname = arg_props.getProperty(CONFIG_FILE);
        readConfiguration(fname, config_props);

        // Arguments override config file
        config_props.putAll(arg_props);
        startAgent(config_props);
    }

    /*
     * Starts the local management agent.
     * This method is invoked by either startAgent method or
     * by the VM directly via jcmd ManagementAgent.start_local command.
     */
    private static synchronized void startLocalManagementAgent() {
        Properties agentProps = VMSupport.getAgentProperties();

        // start local connector if not started
        if (agentProps.get(LOCAL_CONNECTOR_ADDRESS_PROP) == null) {
            JMXConnectorServer cs = ConnectorBootstrap.startLocalConnectorServer();
            String address = cs.getAddress().toString();
            // Add the local connector address to the agent properties
            agentProps.put(LOCAL_CONNECTOR_ADDRESS_PROP, address);

            try {
                // export the address to the instrumentation buffer
                ConnectorAddressLink.export(address);
            } catch (Exception x) {
                // Connector server started but unable to export address
                // to instrumentation buffer - non-fatal error.
                warning(EXPORT_ADDRESS_FAILED, x.getMessage());
            }
        }
    }

    /*
     * This method is invoked by the VM to start the remote management agent
     * via jcmd ManagementAgent.start command.
     */
    private static synchronized void startRemoteManagementAgent(String args) throws Exception {
        if (jmxServer != null) {
            throw new RuntimeException(getText(INVALID_STATE, "Agent already started"));
        }

        try {
            Properties argProps = parseString(args);
            configProps = new Properties();

            // Load the management properties from the config file
            // if config file is not specified readConfiguration implicitly
            // reads <java.home>/conf/management/management.properties

            String fname = System.getProperty(CONFIG_FILE);
            readConfiguration(fname, configProps);

            // management properties can be overridden by system properties
            // which take precedence
            Properties sysProps = System.getProperties();
            synchronized (sysProps) {
                configProps.putAll(sysProps);
            }

            // if user specifies config file into command line for either
            // jcmd utilities or attach command it overrides properties set in
            // command line at the time of VM start
            String fnameUser = argProps.getProperty(CONFIG_FILE);
            if (fnameUser != null) {
                readConfiguration(fnameUser, configProps);
            }

            // arguments specified in command line of jcmd utilities
            // override both system properties and one set by config file
            // specified in jcmd command line
            configProps.putAll(argProps);

            // jcmd doesn't allow to change ThreadContentionMonitoring, but user
            // can specify this property inside config file, so enable optional
            // monitoring functionality if this property is set
            final String enableThreadContentionMonitoring =
                    configProps.getProperty(ENABLE_THREAD_CONTENTION_MONITORING);

            if (enableThreadContentionMonitoring != null) {
                ManagementFactory.getThreadMXBean().
                        setThreadContentionMonitoringEnabled(true);
            }

            String jmxremotePort = configProps.getProperty(JMXREMOTE_PORT);
            if (jmxremotePort != null) {
                jmxServer = ConnectorBootstrap.
                        startRemoteConnectorServer(jmxremotePort, configProps);

                startDiscoveryService(configProps);
            } else {
                throw new AgentConfigurationError(INVALID_JMXREMOTE_PORT, "No port specified");
            }
        } catch (JdpException e) {
            error(e);
        } catch (AgentConfigurationError err) {
            error(err);
        }
    }

    private static synchronized void stopRemoteManagementAgent() throws Exception {

        JdpController.stopDiscoveryService();

        if (jmxServer != null) {
            ConnectorBootstrap.unexportRegistry();
            ConnectorAddressLink.unexportRemote();

            // Attempt to stop already stopped agent
            // Don't cause any errors.
            jmxServer.stop();
            jmxServer = null;
            configProps = null;
        }
    }

    private static synchronized String getManagementAgentStatus() throws Exception {
        return new TextStatusCollector().collect();
    }

    private static void startAgent(Properties props) throws Exception {
        String jmxremote = props.getProperty(JMXREMOTE);
        String jmxremotePort = props.getProperty(JMXREMOTE_PORT);

        // Enable optional monitoring functionality if requested
        final String enableThreadContentionMonitoring =
                props.getProperty(ENABLE_THREAD_CONTENTION_MONITORING);
        if (enableThreadContentionMonitoring != null) {
            ManagementFactory.getThreadMXBean().
                    setThreadContentionMonitoringEnabled(true);
        }

        try {

            /*
             * If the jmxremote.port property is set then we start the
             * RMIConnectorServer for remote M&M.
             *
             * If the jmxremote or jmxremote.port properties are set then
             * we start a RMIConnectorServer for local M&M. The address
             * of this "local" server is exported as a counter to the jstat
             * instrumentation buffer.
             */
            if (jmxremote != null || jmxremotePort != null) {
                if (jmxremotePort != null) {
                    jmxServer = ConnectorBootstrap.
                            startRemoteConnectorServer(jmxremotePort, props);
                    startDiscoveryService(props);
                }
                startLocalManagementAgent();
            }

        } catch (AgentConfigurationError e) {
            error(e);
        } catch (Exception e) {
            error(e);
        }
    }

    private static void startDiscoveryService(Properties props)
            throws IOException, JdpException {
        // Start discovery service if requested
        String discoveryPort = props.getProperty("com.sun.management.jdp.port");
        String discoveryAddress = props.getProperty("com.sun.management.jdp.address");
        String discoveryShouldStart = props.getProperty("com.sun.management.jmxremote.autodiscovery");

        // Decide whether we should start autodicovery service.
        // To start autodiscovery following conditions should be met:
        // autodiscovery==true OR (autodicovery==null AND jdp.port != NULL)

        boolean shouldStart = false;
        if (discoveryShouldStart == null){
            shouldStart = (discoveryPort != null);
        }
        else{
            try{
               shouldStart = Boolean.parseBoolean(discoveryShouldStart);
            } catch (NumberFormatException e) {
                throw new AgentConfigurationError(AGENT_EXCEPTION, "Couldn't parse autodiscovery argument");
            }
        }

        if (shouldStart) {
            // port and address are required arguments and have no default values
            InetAddress address;
            try {
                address = (discoveryAddress == null) ?
                        InetAddress.getByName(JDP_DEFAULT_ADDRESS) : InetAddress.getByName(discoveryAddress);
            } catch (UnknownHostException e) {
                throw new AgentConfigurationError(AGENT_EXCEPTION, e, "Unable to broadcast to requested address");
            }

            int port = JDP_DEFAULT_PORT;
            if (discoveryPort != null) {
               try {
                  port = Integer.parseInt(discoveryPort);
               } catch (NumberFormatException e) {
                 throw new AgentConfigurationError(AGENT_EXCEPTION, "Couldn't parse JDP port argument");
               }
            }

            // Get service URL to broadcast it
            Map<String,String> remoteProps = ConnectorAddressLink.importRemoteFrom(0);
            String jmxUrlStr = remoteProps.get("sun.management.JMXConnectorServer.0.remoteAddress");

            String instanceName = props.getProperty("com.sun.management.jdp.name");

            JdpController.startDiscoveryService(address, port, instanceName, jmxUrlStr);
        }
    }

    public static Properties loadManagementProperties() {
        Properties props = new Properties();

        // Load the management properties from the config file

        String fname = System.getProperty(CONFIG_FILE);
        readConfiguration(fname, props);

        // management properties can be overridden by system properties
        // which take precedence
        Properties sysProps = System.getProperties();
        synchronized (sysProps) {
            props.putAll(sysProps);
        }

        return props;
    }

    public static synchronized Properties getManagementProperties() {
        if (mgmtProps == null) {
            String configFile = System.getProperty(CONFIG_FILE);
            String jmxremote = System.getProperty(JMXREMOTE);
            String jmxremotePort = System.getProperty(JMXREMOTE_PORT);

            if (configFile == null && jmxremote == null && jmxremotePort == null) {
                // return if out-of-the-management option is not specified
                return null;
            }
            mgmtProps = loadManagementProperties();
        }
        return mgmtProps;
    }

    // read config file and initialize the properties
    private static void readConfiguration(String fname, Properties p) {
        if (fname == null) {
            String home = System.getProperty("java.home");
            if (home == null) {
                throw new Error("Can't find java.home ??");
            }
            StringBuilder defaultFileName = new StringBuilder(home);
            defaultFileName.append(File.separator).append("conf");
            defaultFileName.append(File.separator).append("management");
            defaultFileName.append(File.separator).append("management.properties");
            // Set file name
            fname = defaultFileName.toString();
        }
        final File configFile = new File(fname);
        if (!configFile.exists()) {
            error(CONFIG_FILE_NOT_FOUND, fname);
        }

        InputStream in = null;
        try {
            in = new FileInputStream(configFile);
            BufferedInputStream bin = new BufferedInputStream(in);
            p.load(bin);
        } catch (FileNotFoundException e) {
            error(CONFIG_FILE_OPEN_FAILED, e.getMessage());
        } catch (IOException e) {
            error(CONFIG_FILE_OPEN_FAILED, e.getMessage());
        } catch (SecurityException e) {
            error(CONFIG_FILE_ACCESS_DENIED, fname);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    error(CONFIG_FILE_CLOSE_FAILED, fname);
                }
            }
        }
    }

    /**
     * This method is invoked by the VM to start the management agent
     * when -Dcom.sun.management.* is set during startup.
     */
    public static void startAgent() throws Exception {
        String prop = System.getProperty("com.sun.management.agent.class");

        // -Dcom.sun.management.agent.class not set so read management
        // properties and start agent
        if (prop == null) {
            // initialize management properties
            Properties props = getManagementProperties();
            if (props != null) {
                startAgent(props);
            }
            return;
        }

        // -Dcom.sun.management.agent.class=<agent classname>:<agent args>
        String[] values = prop.split(":");
        if (values.length < 1 || values.length > 2) {
            error(AGENT_CLASS_INVALID, "\"" + prop + "\"");
        }
        String cname = values[0];
        String args = (values.length == 2 ? values[1] : null);

        if (cname == null || cname.length() == 0) {
            error(AGENT_CLASS_INVALID, "\"" + prop + "\"");
        }

        if (cname != null) {
            try {
                // Instantiate the named class.
                // invoke the premain(String args) method
                Class<?> clz = ClassLoader.getSystemClassLoader().loadClass(cname);
                Method premain = clz.getMethod("premain",
                        new Class<?>[]{String.class});
                premain.invoke(null, /* static */
                        new Object[]{args});
            } catch (ClassNotFoundException ex) {
                error(AGENT_CLASS_NOT_FOUND, "\"" + cname + "\"");
            } catch (NoSuchMethodException ex) {
                error(AGENT_CLASS_PREMAIN_NOT_FOUND, "\"" + cname + "\"");
            } catch (SecurityException ex) {
                error(AGENT_CLASS_ACCESS_DENIED);
            } catch (Exception ex) {
                String msg = (ex.getCause() == null
                        ? ex.getMessage()
                        : ex.getCause().getMessage());
                error(AGENT_CLASS_FAILED, msg);
            }
        }
    }

    public static void error(String key) {
        String keyText = getText(key);
        System.err.print(getText("agent.err.error") + ": " + keyText);
        throw new RuntimeException(keyText);
    }

    public static void error(String key, String message) {
        String keyText = getText(key);
        System.err.print(getText("agent.err.error") + ": " + keyText);
        System.err.println(": " + message);
        throw new RuntimeException(keyText + ": " + message);
    }

    public static void error(Exception e) {
        e.printStackTrace();
        System.err.println(getText(AGENT_EXCEPTION) + ": " + e.toString());
        throw new RuntimeException(e);
    }

    public static void error(AgentConfigurationError e) {
        String keyText = getText(e.getError());
        String[] params = e.getParams();

        System.err.print(getText("agent.err.error") + ": " + keyText);

        if (params != null && params.length != 0) {
           System.err.println(": " + String.join(" ", params));
        }
        e.printStackTrace();
        throw new RuntimeException(e);
    }

    public static void warning(String key, String message) {
        System.err.print(getText("agent.err.warning") + ": " + getText(key));
        System.err.println(": " + message);
    }

    private static void initResource() {
        try {
            messageRB =
                ResourceBundle.getBundle("jdk.internal.agent.resources.agent");
        } catch (MissingResourceException e) {
            throw new Error("Fatal: Resource for management agent is missing");
        }
    }

    public static String getText(String key) {
        if (messageRB == null) {
            initResource();
        }
        try {
            return messageRB.getString(key);
        } catch (MissingResourceException e) {
            return "Missing management agent resource bundle: key = \"" + key + "\"";
        }
    }

    public static String getText(String key, String... args) {
        if (messageRB == null) {
            initResource();
        }
        String format = messageRB.getString(key);
        if (format == null) {
            format = "missing resource key: key = \"" + key + "\", "
                    + "arguments = \"{0}\", \"{1}\", \"{2}\"";
        }
        return MessageFormat.format(format, (Object[]) args);
    }
}
