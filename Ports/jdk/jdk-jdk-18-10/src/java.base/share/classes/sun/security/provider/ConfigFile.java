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

package sun.security.provider;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.Security;
import java.security.URIParameter;
import java.text.MessageFormat;
import java.util.*;
import javax.security.auth.AuthPermission;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.ConfigurationSpi;
import sun.security.util.Debug;
import sun.security.util.PropertyExpander;
import sun.security.util.ResourcesMgr;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class represents a default implementation for
 * {@code javax.security.auth.login.Configuration}.
 *
 * <p> This object stores the runtime login configuration representation,
 * and is the amalgamation of multiple static login configurations that
 * resides in files. The algorithm for locating the login configuration
 * file(s) and reading their information into this {@code Configuration}
 * object is:
 *
 * <ol>
 * <li>
 *   Loop through the security properties,
 *   <i>login.config.url.1</i>, <i>login.config.url.2</i>, ...,
 *   <i>login.config.url.X</i>.
 *   Each property value specifies a {@code URL} pointing to a
 *   login configuration file to be loaded.  Read in and load
 *   each configuration.
 *
 * <li>
 *   The {@code java.lang.System} property
 *   <i>java.security.auth.login.config</i>
 *   may also be set to a {@code URL} pointing to another
 *   login configuration file
 *   (which is the case when a user uses the -D switch at runtime).
 *   If this property is defined, and its use is allowed by the
 *   security property file (the Security property,
 *   <i>policy.allowSystemProperty</i> is set to <i>true</i>),
 *   also load that login configuration.
 *
 * <li>
 *   If the <i>java.security.auth.login.config</i> property is defined using
 *   "==" (rather than "="), then ignore all other specified
 *   login configurations and only load this configuration.
 *
 * <li>
 *   If no system or security properties were set, try to read from the file,
 *   ${user.home}/.java.login.config, where ${user.home} is the value
 *   represented by the "user.home" System property.
 * </ol>
 *
 * <p> The configuration syntax supported by this implementation
 * is exactly that syntax specified in the
 * {@code javax.security.auth.login.Configuration} class.
 *
 * @see javax.security.auth.login.LoginContext
 * @see java.security.Security security properties
 */
public final class ConfigFile extends Configuration {

    private final Spi spi;

    public ConfigFile() {
        spi = new Spi();
    }

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String appName) {
        return spi.engineGetAppConfigurationEntry(appName);
    }

    @Override
    public synchronized void refresh() {
        spi.engineRefresh();
    }

    public static final class Spi extends ConfigurationSpi {

        private URL url;
        private boolean expandProp = true;
        private Map<String, List<AppConfigurationEntry>> configuration;
        private int linenum;
        private StreamTokenizer st;
        private int lookahead;

        private static Debug debugConfig = Debug.getInstance("configfile");
        private static Debug debugParser = Debug.getInstance("configparser");

        /**
         * Creates a new {@code ConfigurationSpi} object.
         *
         * @throws SecurityException if the {@code ConfigurationSpi} can not be
         *                           initialized
         */
        public Spi() {
            try {
                init();
            } catch (IOException ioe) {
                throw new SecurityException(ioe);
            }
        }

        /**
         * Creates a new {@code ConfigurationSpi} object from the specified
         * {@code URI}.
         *
         * @param uri the {@code URI}
         * @throws SecurityException if the {@code ConfigurationSpi} can not be
         *                           initialized
         * @throws NullPointerException if {@code uri} is null
         */
        public Spi(URI uri) {
            // only load config from the specified URI
            try {
                url = uri.toURL();
                init();
            } catch (IOException ioe) {
                throw new SecurityException(ioe);
            }
        }

        @SuppressWarnings("removal")
        public Spi(final Configuration.Parameters params) throws IOException {

            // call in a doPrivileged
            //
            // we have already passed the Configuration.getInstance
            // security check.  also this class is not freely accessible
            // (it is in the "sun" package).

            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                    public Void run() throws IOException {
                        if (params == null) {
                            init();
                        } else {
                            if (!(params instanceof URIParameter)) {
                                throw new IllegalArgumentException
                                        ("Unrecognized parameter: " + params);
                            }
                            URIParameter uriParam = (URIParameter)params;
                            url = uriParam.getURI().toURL();
                            init();
                        }
                        return null;
                    }
                });
            } catch (PrivilegedActionException pae) {
                throw (IOException)pae.getException();
            }

            // if init() throws some other RuntimeException,
            // let it percolate up naturally.
        }

        /**
         * Read and initialize the entire login Configuration from the
         * configured URL.
         *
         * @throws IOException if the Configuration can not be initialized
         * @throws SecurityException if the caller does not have permission
         *                           to initialize the Configuration
         */
        private void init() throws IOException {

            boolean initialized = false;

            // For policy.expandProperties, check if either a security or system
            // property is set to false (old code erroneously checked the system
            // prop so we must check both to preserve compatibility).
            String expand = Security.getProperty("policy.expandProperties");
            if (expand == null) {
                expand = System.getProperty("policy.expandProperties");
            }
            if ("false".equals(expand)) {
                expandProp = false;
            }

            // new configuration
            Map<String, List<AppConfigurationEntry>> newConfig = new HashMap<>();

            if (url != null) {
                /**
                 * If the caller specified a URI via Configuration.getInstance,
                 * we only read from that URI
                 */
                if (debugConfig != null) {
                    debugConfig.println("reading " + url);
                }
                init(url, newConfig);
                configuration = newConfig;
                return;
            }

            /**
             * Caller did not specify URI via Configuration.getInstance.
             * Read from URLs listed in the java.security properties file.
             */
            String allowSys = Security.getProperty("policy.allowSystemProperty");

            if ("true".equalsIgnoreCase(allowSys)) {
                String extra_config = System.getProperty
                                      ("java.security.auth.login.config");
                if (extra_config != null) {
                    boolean overrideAll = false;
                    if (extra_config.startsWith("=")) {
                        overrideAll = true;
                        extra_config = extra_config.substring(1);
                    }
                    try {
                        extra_config = PropertyExpander.expand(extra_config);
                    } catch (PropertyExpander.ExpandException peee) {
                        throw ioException("Unable.to.properly.expand.config",
                                          extra_config);
                    }

                    URL configURL = null;
                    try {
                        configURL = new URL(extra_config);
                    } catch (MalformedURLException mue) {
                        File configFile = new File(extra_config);
                        if (configFile.exists()) {
                            configURL = configFile.toURI().toURL();
                        } else {
                            throw ioException(
                                "extra.config.No.such.file.or.directory.",
                                extra_config);
                        }
                    }

                    if (debugConfig != null) {
                        debugConfig.println("reading "+configURL);
                    }
                    init(configURL, newConfig);
                    initialized = true;
                    if (overrideAll) {
                        if (debugConfig != null) {
                            debugConfig.println("overriding other policies!");
                        }
                        configuration = newConfig;
                        return;
                    }
                }
            }

            int n = 1;
            String config_url;
            while ((config_url = Security.getProperty
                                     ("login.config.url."+n)) != null) {
                try {
                    config_url = PropertyExpander.expand
                        (config_url).replace(File.separatorChar, '/');
                    if (debugConfig != null) {
                        debugConfig.println("\tReading config: " + config_url);
                    }
                    init(new URL(config_url), newConfig);
                    initialized = true;
                } catch (PropertyExpander.ExpandException peee) {
                    throw ioException("Unable.to.properly.expand.config",
                                      config_url);
                }
                n++;
            }

            if (initialized == false && n == 1 && config_url == null) {

                // get the config from the user's home directory
                if (debugConfig != null) {
                    debugConfig.println("\tReading Policy " +
                                "from ~/.java.login.config");
                }
                config_url = System.getProperty("user.home");
                String userConfigFile = config_url + File.separatorChar +
                                        ".java.login.config";

                // No longer throws an exception when there's no config file
                // at all. Returns an empty Configuration instead.
                if (new File(userConfigFile).exists()) {
                    init(new File(userConfigFile).toURI().toURL(), newConfig);
                }
            }

            configuration = newConfig;
        }

        private void init(URL config,
                          Map<String, List<AppConfigurationEntry>> newConfig)
                          throws IOException {

            try (InputStreamReader isr
                    = new InputStreamReader(getInputStream(config), UTF_8)) {
                readConfig(isr, newConfig);
            } catch (FileNotFoundException fnfe) {
                if (debugConfig != null) {
                    debugConfig.println(fnfe.toString());
                }
                throw new IOException(ResourcesMgr.getAuthResourceString
                    ("Configuration.Error.No.such.file.or.directory"));
            }
        }

        /**
         * Retrieve an entry from the Configuration using an application name
         * as an index.
         *
         * @param applicationName the name used to index the Configuration.
         * @return an array of AppConfigurationEntries which correspond to
         *         the stacked configuration of LoginModules for this
         *         application, or null if this application has no configured
         *         LoginModules.
         */
        @Override
        public AppConfigurationEntry[] engineGetAppConfigurationEntry
            (String applicationName) {

            List<AppConfigurationEntry> list = null;
            synchronized (configuration) {
                list = configuration.get(applicationName);
            }

            if (list == null || list.size() == 0) {
                return null;
            }

            AppConfigurationEntry[] entries =
                                    new AppConfigurationEntry[list.size()];
            Iterator<AppConfigurationEntry> iterator = list.iterator();
            for (int i = 0; iterator.hasNext(); i++) {
                AppConfigurationEntry e = iterator.next();
                entries[i] = new AppConfigurationEntry(e.getLoginModuleName(),
                                                       e.getControlFlag(),
                                                       e.getOptions());
            }
            return entries;
        }

        /**
         * Refresh and reload the Configuration by re-reading all of the
         * login configurations.
         *
         * @throws SecurityException if the caller does not have permission
         *                           to refresh the Configuration.
         */
        @SuppressWarnings("removal")
        @Override
        public synchronized void engineRefresh() {

            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(
                    new AuthPermission("refreshLoginConfiguration"));
            }

            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    try {
                        init();
                    } catch (IOException ioe) {
                        throw new SecurityException(ioe.getLocalizedMessage(),
                                                    ioe);
                    }
                    return null;
                }
            });
        }

        private void readConfig(Reader reader,
            Map<String, List<AppConfigurationEntry>> newConfig)
            throws IOException {

            linenum = 1;

            if (!(reader instanceof BufferedReader)) {
                reader = new BufferedReader(reader);
            }

            st = new StreamTokenizer(reader);
            st.quoteChar('"');
            st.wordChars('$', '$');
            st.wordChars('_', '_');
            st.wordChars('-', '-');
            st.wordChars('*', '*');
            st.lowerCaseMode(false);
            st.slashSlashComments(true);
            st.slashStarComments(true);
            st.eolIsSignificant(true);

            lookahead = nextToken();
            while (lookahead != StreamTokenizer.TT_EOF) {
                parseLoginEntry(newConfig);
            }
        }

        private void parseLoginEntry(
            Map<String, List<AppConfigurationEntry>> newConfig)
            throws IOException {

            List<AppConfigurationEntry> configEntries = new LinkedList<>();

            // application name
            String appName = st.sval;
            lookahead = nextToken();

            if (debugParser != null) {
                debugParser.println("\tReading next config entry: " + appName);
            }

            match("{");

            // get the modules
            while (peek("}") == false) {
                // get the module class name
                String moduleClass = match("module class name");

                // controlFlag (required, optional, etc)
                LoginModuleControlFlag controlFlag;
                String sflag = match("controlFlag").toUpperCase(Locale.ENGLISH);
                switch (sflag) {
                    case "REQUIRED":
                        controlFlag = LoginModuleControlFlag.REQUIRED;
                        break;
                    case "REQUISITE":
                        controlFlag = LoginModuleControlFlag.REQUISITE;
                        break;
                    case "SUFFICIENT":
                        controlFlag = LoginModuleControlFlag.SUFFICIENT;
                        break;
                    case "OPTIONAL":
                        controlFlag = LoginModuleControlFlag.OPTIONAL;
                        break;
                    default:
                        throw ioException(
                            "Configuration.Error.Invalid.control.flag.flag",
                            sflag);
                }

                // get the args
                Map<String, String> options = new HashMap<>();
                while (peek(";") == false) {
                    String key = match("option key");
                    match("=");
                    try {
                        options.put(key, expand(match("option value")));
                    } catch (PropertyExpander.ExpandException peee) {
                        throw new IOException(peee.getLocalizedMessage());
                    }
                }

                lookahead = nextToken();

                // create the new element
                if (debugParser != null) {
                    debugParser.println("\t\t" + moduleClass + ", " + sflag);
                    for (String key : options.keySet()) {
                        debugParser.println("\t\t\t" + key +
                                            "=" + options.get(key));
                    }
                }
                configEntries.add(new AppConfigurationEntry(moduleClass,
                                                            controlFlag,
                                                            options));
            }

            match("}");
            match(";");

            // add this configuration entry
            if (newConfig.containsKey(appName)) {
                throw ioException(
                    "Configuration.Error.Can.not.specify.multiple.entries.for.appName",
                    appName);
            }
            newConfig.put(appName, configEntries);
        }

        private String match(String expect) throws IOException {

            String value = null;

            switch(lookahead) {
            case StreamTokenizer.TT_EOF:
                throw ioException(
                    "Configuration.Error.expected.expect.read.end.of.file.",
                    expect);

            case '"':
            case StreamTokenizer.TT_WORD:
                if (expect.equalsIgnoreCase("module class name") ||
                    expect.equalsIgnoreCase("controlFlag") ||
                    expect.equalsIgnoreCase("option key") ||
                    expect.equalsIgnoreCase("option value")) {
                    value = st.sval;
                    lookahead = nextToken();
                } else {
                    throw ioException(
                        "Configuration.Error.Line.line.expected.expect.found.value.",
                        linenum, expect, st.sval);
                }
                break;

            case '{':
                if (expect.equalsIgnoreCase("{")) {
                    lookahead = nextToken();
                } else {
                    throw ioException(
                        "Configuration.Error.Line.line.expected.expect.",
                        linenum, expect, st.sval);
                }
                break;

            case ';':
                if (expect.equalsIgnoreCase(";")) {
                    lookahead = nextToken();
                } else {
                    throw ioException(
                        "Configuration.Error.Line.line.expected.expect.",
                        linenum, expect, st.sval);
                }
                break;

            case '}':
                if (expect.equalsIgnoreCase("}")) {
                    lookahead = nextToken();
                } else {
                    throw ioException(
                        "Configuration.Error.Line.line.expected.expect.",
                        linenum, expect, st.sval);
                }
                break;

            case '=':
                if (expect.equalsIgnoreCase("=")) {
                    lookahead = nextToken();
                } else {
                    throw ioException(
                        "Configuration.Error.Line.line.expected.expect.",
                        linenum, expect, st.sval);
                }
                break;

            default:
                throw ioException(
                    "Configuration.Error.Line.line.expected.expect.found.value.",
                    linenum, expect, st.sval);
            }
            return value;
        }

        private boolean peek(String expect) {
            switch (lookahead) {
                case ',':
                    return expect.equalsIgnoreCase(",");
                case ';':
                    return expect.equalsIgnoreCase(";");
                case '{':
                    return expect.equalsIgnoreCase("{");
                case '}':
                    return expect.equalsIgnoreCase("}");
                default:
                    return false;
            }
        }

        private int nextToken() throws IOException {
            int tok;
            while ((tok = st.nextToken()) == StreamTokenizer.TT_EOL) {
                linenum++;
            }
            return tok;
        }

        private InputStream getInputStream(URL url) throws IOException {
            if ("file".equalsIgnoreCase(url.getProtocol())) {
                // Compatibility notes:
                //
                // Code changed from
                //   String path = url.getFile().replace('/', File.separatorChar);
                //   return new FileInputStream(path);
                //
                // The original implementation would search for "/tmp/a%20b"
                // when url is "file:///tmp/a%20b". This is incorrect. The
                // current codes fix this bug and searches for "/tmp/a b".
                // For compatibility reasons, when the file "/tmp/a b" does
                // not exist, the file named "/tmp/a%20b" will be tried.
                //
                // This also means that if both file exists, the behavior of
                // this method is changed, and the current codes choose the
                // correct one.
                try {
                    return url.openStream();
                } catch (Exception e) {
                    String file = url.getPath();
                    if (!url.getHost().isEmpty()) {  // For Windows UNC
                        file = "//" + url.getHost() + file;
                    }
                    if (debugConfig != null) {
                        debugConfig.println("cannot read " + url +
                                            ", try " + file);
                    }
                    return new FileInputStream(file);
                }
            } else {
                return url.openStream();
            }
        }

        private String expand(String value)
            throws PropertyExpander.ExpandException, IOException {

            if (value.isEmpty()) {
                return value;
            }

            if (!expandProp) {
                return value;
            }
            String s = PropertyExpander.expand(value);
            if (s == null || s.isEmpty()) {
                throw ioException(
                    "Configuration.Error.Line.line.system.property.value.expanded.to.empty.value",
                    linenum, value);
            }
            return s;
        }

        private IOException ioException(String resourceKey, Object... args) {
            MessageFormat form = new MessageFormat(
                ResourcesMgr.getAuthResourceString(resourceKey));
            return new IOException(form.format(args));
        }
    }
}
