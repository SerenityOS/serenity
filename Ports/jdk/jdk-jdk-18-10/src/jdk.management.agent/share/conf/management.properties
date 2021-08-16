#####################################################################
#	Default Configuration File for Java Platform Management
#####################################################################
#
# The Management Configuration file (in java.util.Properties format)
# will be read if one of the following system properties is set:
#    -Dcom.sun.management.jmxremote.port=<port-number>
# or -Dcom.sun.management.config.file=<this-file>
#
# The default Management Configuration file is:
#
#       $JRE/conf/management/management.properties
#
# Another location for the Management Configuration File can be specified
# by the following property on the Java command line:
#
#    -Dcom.sun.management.config.file=<this-file>
#
# If -Dcom.sun.management.config.file=<this-file> is set, the port
# number for the management agent can be specified in the config file
# using the following lines:
#
# ################ Management Agent Port #########################
#
# For setting the JMX RMI agent port use the following line
# com.sun.management.jmxremote.port=<port-number>
#
# For setting the JMX local server port use the following line
# com.sun.management.jmxremote.local.port=<port-number>

#####################################################################
#                   Optional Instrumentation
#####################################################################
#
# By default only the basic instrumentation with low overhead is on.
# The following properties allow to selectively turn on optional
# instrumentation which are off by default and may have some
# additional overhead.
#
# com.sun.management.enableThreadContentionMonitoring
#
#      This option enables thread contention monitoring if the
#      Java virtual machine supports such instrumentation.
#      Refer to the specification for the java.lang.management.ThreadMXBean
#      interface - see isThreadContentionMonitoringSupported() method.
#

# To enable thread contention monitoring, uncomment the following line
# com.sun.management.enableThreadContentionMonitoring

#####################################################################
#			RMI Management Properties
#####################################################################
#
# If system property -Dcom.sun.management.jmxremote.port=<port-number>
# is set then
#     - A MBean server is started
#     - JRE Platform MBeans are registered in the MBean server
#     - RMI connector is published  in a private readonly registry at
#       specified port using a well known name, "jmxrmi"
#     - the following properties are read for JMX remote management.
#
# The configuration can be specified only at startup time.
# Later changes to above system property (e.g. via setProperty method),
# this config file, the password file, or the access file have no effect to the
# running MBean server, the connector, or the registry.
#

#
# ########## RMI connector settings for local management ##########
#
# com.sun.management.jmxremote.local.only=true|false
#      Default for this property is true. (Case for true/false ignored)
#      If this property is specified as true then the local JMX RMI connector
#      server will only accept connection requests from clients running on
#      the host where the out-of-the-box JMX management agent is running.
#      In order to ensure backwards compatibility this property could be
#      set to false. However, deploying the local management agent in this
#      way is discouraged because the local JMX RMI connector server will
#      accept connection requests from any client either local or remote.
#      For remote management the remote JMX RMI connector server should
#      be used instead with authentication and SSL/TLS encryption enabled.
#

# For allowing the local management agent accept local
# and remote connection requests use the following line
# com.sun.management.jmxremote.local.only=false

#
# ###################### RMI SSL #############################
#
# com.sun.management.jmxremote.ssl=true|false
#      Default for this property is true. (Case for true/false ignored)
#      If this property is specified as false then SSL is not used.
#

# For RMI monitoring without SSL use the following line
# com.sun.management.jmxremote.ssl=false

# com.sun.management.jmxremote.ssl.config.file=filepath
#      Specifies the location of the SSL configuration file. A properties
#      file can be used to supply the keystore and truststore location and
#      password settings thus avoiding to pass them as cleartext in the
#      command-line.
#
#      The current implementation of the out-of-the-box management agent will
#      look up and use the properties specified below to configure the SSL
#      keystore and truststore, if present:
#          javax.net.ssl.keyStore=<keystore-location>
#          javax.net.ssl.keyStorePassword=<keystore-password>
#          javax.net.ssl.trustStore=<truststore-location>
#          javax.net.ssl.trustStorePassword=<truststore-password>
#      Any other properties in the file will be ignored. This will allow us
#      to extend the property set in the future if required by the default
#      SSL implementation.
#
#      If the property "com.sun.management.jmxremote.ssl" is set to false,
#      then this property is ignored.
#

# For supplying the keystore settings in a file use the following line
# com.sun.management.jmxremote.ssl.config.file=filepath

# com.sun.management.jmxremote.ssl.enabled.cipher.suites=<cipher-suites>
#      The value of this property is a string that is a comma-separated list
#      of SSL/TLS cipher suites to enable. This property can be specified in
#      conjunction with the previous property "com.sun.management.jmxremote.ssl"
#      in order to control which particular SSL/TLS cipher suites are enabled
#      for use by accepted connections. If this property is not specified then
#      the SSL/TLS RMI Server Socket Factory uses the SSL/TLS cipher suites that
#      are enabled by default.
#

# com.sun.management.jmxremote.ssl.enabled.protocols=<protocol-versions>
#      The value of this property is a string that is a comma-separated list
#      of SSL/TLS protocol versions to enable. This property can be specified in
#      conjunction with the previous property "com.sun.management.jmxremote.ssl"
#      in order to control which particular SSL/TLS protocol versions are
#      enabled for use by accepted connections. If this property is not
#      specified then the SSL/TLS RMI Server Socket Factory uses the SSL/TLS
#      protocol versions that are enabled by default.
#

# com.sun.management.jmxremote.ssl.need.client.auth=true|false
#      Default for this property is false. (Case for true/false ignored)
#      If this property is specified as true in conjunction with the previous
#      property "com.sun.management.jmxremote.ssl" then the SSL/TLS RMI Server
#      Socket Factory will require client authentication.
#

# For RMI monitoring with SSL client authentication use the following line
# com.sun.management.jmxremote.ssl.need.client.auth=true

# com.sun.management.jmxremote.registry.ssl=true|false
#      Default for this property is false. (Case for true/false ignored)
#      If this property is specified as true then the RMI registry used
#      to bind the RMIServer remote object is protected with SSL/TLS
#      RMI Socket Factories that can be configured with the properties:
#          com.sun.management.jmxremote.ssl.config.file
#          com.sun.management.jmxremote.ssl.enabled.cipher.suites
#          com.sun.management.jmxremote.ssl.enabled.protocols
#          com.sun.management.jmxremote.ssl.need.client.auth
#      If the two properties below are true at the same time, i.e.
#          com.sun.management.jmxremote.ssl=true
#          com.sun.management.jmxremote.registry.ssl=true
#      then the RMIServer remote object and the RMI registry are
#      both exported with the same SSL/TLS RMI Socket Factories.
#

# For using an SSL/TLS protected RMI registry use the following line
# com.sun.management.jmxremote.registry.ssl=true

#
# ################ RMI User authentication ################
#
# com.sun.management.jmxremote.authenticate=true|false
#      Default for this property is true. (Case for true/false ignored)
#      If this property is specified as false then no authentication is
#      performed and all users are allowed all access.
#

# For RMI monitoring without any checking use the following line
# com.sun.management.jmxremote.authenticate=false

#
# ################ RMI Login configuration ###################
#
# com.sun.management.jmxremote.login.config=<config-name>
#      Specifies the name of a JAAS login configuration entry to use when
#      authenticating users of RMI monitoring.
#
#      Setting this property is optional - the default login configuration
#      specifies a file-based authentication that uses the password file.
#
#      When using this property to override the default login configuration
#      then the named configuration entry must be in a file that gets loaded
#      by JAAS. In addition, the login module(s) specified in the configuration
#      should use the name and/or password callbacks to acquire the user's
#      credentials. See the NameCallback and PasswordCallback classes in the
#      javax.security.auth.callback package for more details.
#
#      If the property "com.sun.management.jmxremote.authenticate" is set to
#      false, then this property and the password & access files are ignored.
#

# For a non-default login configuration use the following line
# com.sun.management.jmxremote.login.config=<config-name>

#
# ################ RMI Password file location ##################
#
# com.sun.management.jmxremote.password.file=filepath
#      Specifies location for password file
#      This is optional - default location is
#      $JRE/conf/management/jmxremote.password
#
#      If the property "com.sun.management.jmxremote.authenticate" is set to
#      false, then this property and the password & access files are ignored.
#      Otherwise the password file must exist and be in the valid format.
#      If the password file is empty or non-existent then no access is allowed.
#

# For a non-default password file location use the following line
# com.sun.management.jmxremote.password.file=filepath

#
# ################# Hash passwords in password file ##############
# com.sun.management.jmxremote.password.toHashes = true|false
#      Default for this property is true.
#      Specifies if passwords in the password file should be hashed or not.
#      If this property is true, and if the password file is writable, and if the
#      system security policy allows writing into the password file,
#      all the clear passwords in the password file will be replaced by
#      their SHA3-512 hash when the file is read by the server
#

#
# ################ RMI Access file location #####################
#
# com.sun.management.jmxremote.access.file=filepath
#      Specifies location for access  file
#      This is optional - default location is
#      $JRE/conf/management/jmxremote.access
#
#      If the property "com.sun.management.jmxremote.authenticate" is set to
#      false, then this property and the password & access files are ignored.
#      Otherwise, the access file must exist and be in the valid format.
#      If the access file is empty or non-existent then no access is allowed.
#

# For a non-default password file location use the following line
# com.sun.management.jmxremote.access.file=filepath
#

# ################ Management agent listen interface #########################
#
# com.sun.management.jmxremote.host=<host-or-interface-name>
#      Specifies the local interface on which the JMX RMI agent will bind.
#      This is useful when running on machines which have several
#      interfaces defined. It makes it possible to listen to a specific
#      subnet accessible through that interface.
#
#      The format of the value for that property is any string accepted
#      by java.net.InetAddress.getByName(String).
#

# ################ Filter for ObjectInputStream #############################
# com.sun.management.jmxremote.serial.filter.pattern=<filter-string>
#   A filter, if configured, is used by java.io.ObjectInputStream during
#   deserialization of parameters sent to the JMX default agent to validate the
#   contents of the stream.
#   A filter is configured as a sequence of patterns, each pattern is either
#   matched against the name of a class in the stream or defines a limit.
#   Patterns are separated by ";" (semicolon).
#   Whitespace is significant and is considered part of the pattern.
#
#   If a pattern includes a "=", it sets a limit.
#   If a limit appears more than once the last value is used.
#   Limits are checked before classes regardless of the order in the sequence of patterns.
#   If any of the limits are exceeded, the filter status is REJECTED.
#
#       maxdepth=value - the maximum depth of a graph
#       maxrefs=value  - the maximum number of internal references
#       maxbytes=value - the maximum number of bytes in the input stream
#       maxarray=value - the maximum array length allowed
#
#   Other patterns, from left to right, match the class or package name as
#   returned from Class.getName.
#   If the class is an array type, the class or package to be matched is the element type.
#   Arrays of any number of dimensions are treated the same as the element type.
#   For example, a pattern of "!example.Foo", rejects creation of any instance or
#   array of example.Foo.
#
#   If the pattern starts with "!", the status is REJECTED if the remaining pattern
#       is matched; otherwise the status is ALLOWED if the pattern matches.
#   If the pattern contains "/", the non-empty prefix up to the "/" is the module name;
#       if the module name matches the module name of the class then
#       the remaining pattern is matched with the class name.
#   If there is no "/", the module name is not compared.
#   If the pattern ends with ".**" it matches any class in the package and all subpackages.
#   If the pattern ends with ".*" it matches any class in the package.
#   If the pattern ends with "*", it matches any class with the pattern as a prefix.
#   If the pattern is equal to the class name, it matches.
#   Otherwise, the status is UNDECIDED.
