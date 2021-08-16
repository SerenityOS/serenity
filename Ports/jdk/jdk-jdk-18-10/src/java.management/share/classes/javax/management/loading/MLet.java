/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.loading;

// Java import
import com.sun.jmx.defaults.JmxProperties;

import com.sun.jmx.defaults.ServiceName;

import com.sun.jmx.remote.util.EnvHelp;

import java.io.Externalizable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistration;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import static com.sun.jmx.defaults.JmxProperties.MLET_LIB_DIR;
import static com.sun.jmx.defaults.JmxProperties.MLET_LOGGER;
import com.sun.jmx.defaults.ServiceName;
import javax.management.ServiceNotFoundException;

/**
 * Allows you to instantiate and register one or several MBeans in the MBean server
 * coming from a remote URL. M-let is a shortcut for management applet. The m-let service does this
 * by loading an m-let text file, which specifies information on the MBeans to be obtained.
 * The information on each MBean is specified in a single instance of a tag, called the MLET tag.
 * The location of the m-let text file is specified by a URL.
 * <p>
 * The <CODE>MLET</CODE> tag has the following syntax:
 * <p>
 * &lt;<CODE>MLET</CODE><BR>
 *      <CODE>CODE = </CODE><VAR>class</VAR><CODE> | OBJECT = </CODE><VAR>serfile</VAR><BR>
 *      <CODE>ARCHIVE = &quot;</CODE><VAR>archiveList</VAR><CODE>&quot;</CODE><BR>
 *      <CODE>[CODEBASE = </CODE><VAR>codebaseURL</VAR><CODE>]</CODE><BR>
 *      <CODE>[NAME = </CODE><VAR>mbeanname</VAR><CODE>]</CODE><BR>
 *      <CODE>[VERSION = </CODE><VAR>version</VAR><CODE>]</CODE><BR>
 * &gt;<BR>
 *      <CODE>[</CODE><VAR>arglist</VAR><CODE>]</CODE><BR>
 * &lt;<CODE>/MLET</CODE>&gt;
 * <p>
 * where:
 * <DL>
 * <DT><CODE>CODE = </CODE><VAR>class</VAR></DT>
 * <DD>
 * This attribute specifies the full Java class name, including package name, of the MBean to be obtained.
 * The compiled <CODE>.class</CODE> file of the MBean must be contained in one of the <CODE>.jar</CODE> files specified by the <CODE>ARCHIVE</CODE>
 * attribute. Either <CODE>CODE</CODE> or <CODE>OBJECT</CODE> must be present.
 * </DD>
 * <DT><CODE>OBJECT = </CODE><VAR>serfile</VAR></DT>
 * <DD>
 * This attribute specifies the <CODE>.ser</CODE> file that contains a serialized representation of the MBean to be obtained.
 * This file must be contained in one of the <CODE>.jar</CODE> files specified by the <CODE>ARCHIVE</CODE> attribute. If the <CODE>.jar</CODE> file contains a directory hierarchy, specify the path of the file within this hierarchy. Otherwise  a match will not be found. Either <CODE>CODE</CODE> or <CODE>OBJECT</CODE> must be present.
 * </DD>
 * <DT><CODE>ARCHIVE = &quot;</CODE><VAR>archiveList</VAR><CODE>&quot;</CODE></DT>
 * <DD>
 * This mandatory attribute specifies one or more <CODE>.jar</CODE> files
 * containing MBeans or other resources used by
 * the MBean to be obtained. One of the <CODE>.jar</CODE> files must contain the file specified by the <CODE>CODE</CODE> or <CODE>OBJECT</CODE> attribute.
 * If archivelist contains more than one file:
 * <UL>
 * <LI>Each file must be separated from the one that follows it by a comma (,).
 * <LI><VAR>archivelist</VAR> must be enclosed in double quote marks.
 * </UL>
 * All <CODE>.jar</CODE> files in <VAR>archivelist</VAR> must be stored in the directory specified by the code base URL.
 * </DD>
 * <DT><CODE>CODEBASE = </CODE><VAR>codebaseURL</VAR></DT>
 * <DD>
 * This optional attribute specifies the code base URL of the MBean to be obtained. It identifies the directory that contains
 * the <CODE>.jar</CODE> files specified by the <CODE>ARCHIVE</CODE> attribute. Specify this attribute only if the <CODE>.jar</CODE> files are not in the same
 * directory as the m-let text file. If this attribute is not specified, the base URL of the m-let text file is used.
 * </DD>
 * <DT><CODE>NAME = </CODE><VAR>mbeanname</VAR></DT>
 * <DD>
 * This optional attribute specifies the object name to be assigned to the
 * MBean instance when the m-let service registers it. If
 * <VAR>mbeanname</VAR> starts with the colon character (:), the domain
 * part of the object name is the default domain of the MBean server,
 * as returned by {@link javax.management.MBeanServer#getDefaultDomain()}.
 * </DD>
 * <DT><CODE>VERSION = </CODE><VAR>version</VAR></DT>
 * <DD>
 * This optional attribute specifies the version number of the MBean and
 * associated <CODE>.jar</CODE> files to be obtained. This version number can
 * be used to specify that the <CODE>.jar</CODE> files are loaded from the
 * server to update those stored locally in the cache the next time the m-let
 * text file is loaded. <VAR>version</VAR> must be a series of non-negative
 * decimal integers each separated by a period from the one that precedes it.
 * </DD>
 * <DT><VAR>arglist</VAR></DT>
 * <DD>
 * This optional attribute specifies a list of one or more parameters for the
 * MBean to be instantiated. This list describes the parameters to be passed the MBean's constructor.
 * Use the following syntax to specify each item in
 * <VAR>arglist</VAR>:
 * <DL>
 * <DT>&lt;<CODE>ARG TYPE=</CODE><VAR>argumentType</VAR> <CODE>VALUE=</CODE><VAR>value</VAR>&gt;</DT>
 * <DD>where:
 * <UL>
 * <LI><VAR>argumentType</VAR> is the type of the argument that will be passed as parameter to the MBean's constructor.</UL>
 * </DD>
 * </DL>
 * <P>The arguments' type in the argument list should be a Java primitive type or a Java basic type
 * (<CODE>java.lang.Boolean, java.lang.Byte, java.lang.Short, java.lang.Long, java.lang.Integer, java.lang.Float, java.lang.Double, java.lang.String</CODE>).
 * </DD>
 * </DL>
 *
 * When an m-let text file is loaded, an
 * instance of each MBean specified in the file is created and registered.
 * <P>
 * The m-let service extends the <CODE>java.net.URLClassLoader</CODE> and can be used to load remote classes
 * and jar files in the VM of the agent.
 * <p><STRONG>Note - </STRONG> The <CODE>MLet</CODE> class loader uses the {@link javax.management.MBeanServerFactory#getClassLoaderRepository(javax.management.MBeanServer)}
 * to load classes that could not be found in the loaded jar files.
 *
 * @since 1.5
 */
public class MLet extends java.net.URLClassLoader
     implements MLetMBean, MBeanRegistration, Externalizable {

     private static final long serialVersionUID = 3636148327800330130L;

     /*
     * ------------------------------------------
     *   PRIVATE VARIABLES
     * ------------------------------------------
     */

     /**
      * The reference to the MBean server.
      * @serial
      */
     private MBeanServer server = null;


     /**
      * The list of instances of the <CODE>MLetContent</CODE>
      * class found at the specified URL.
      * @serial
      */
     private List<MLetContent> mletList = new ArrayList<MLetContent>();


     /**
      * The directory used for storing libraries locally before they are loaded.
      */
     private String libraryDirectory;


     /**
      * The object name of the MLet Service.
      * @serial
      */
     private ObjectName mletObjectName = null;

     /**
      * The URLs of the MLet Service.
      * @serial
      */
     private URL[] myUrls = null;

     /**
      * What ClassLoaderRepository, if any, to use if this MLet
      * doesn't find an asked-for class.
      */
     private transient ClassLoaderRepository currentClr;

     /**
      * True if we should consult the {@link ClassLoaderRepository}
      * when we do not find a class ourselves.
      */
     private transient boolean delegateToCLR;

     /**
      * objects maps from primitive classes to primitive object classes.
      */
     private Map<String,Class<?>> primitiveClasses =
         new HashMap<String,Class<?>>(8) ;
     {
         primitiveClasses.put(Boolean.TYPE.toString(), Boolean.class);
         primitiveClasses.put(Character.TYPE.toString(), Character.class);
         primitiveClasses.put(Byte.TYPE.toString(), Byte.class);
         primitiveClasses.put(Short.TYPE.toString(), Short.class);
         primitiveClasses.put(Integer.TYPE.toString(), Integer.class);
         primitiveClasses.put(Long.TYPE.toString(), Long.class);
         primitiveClasses.put(Float.TYPE.toString(), Float.class);
         primitiveClasses.put(Double.TYPE.toString(), Double.class);

     }


     /*
      * ------------------------------------------
      *  CONSTRUCTORS
      * ------------------------------------------
      */

     /*
      * The constructor stuff would be considerably simplified if our
      * parent, URLClassLoader, specified that its one- and
      * two-argument constructors were equivalent to its
      * three-argument constructor with trailing null arguments.  But
      * it doesn't, which prevents us from having all the constructors
      * but one call this(...args...).
      */

     /**
      * Constructs a new MLet using the default delegation parent ClassLoader.
      */
     public MLet() {
         this(new URL[0]);
     }

     /**
      * Constructs a new MLet for the specified URLs using the default
      * delegation parent ClassLoader.  The URLs will be searched in
      * the order specified for classes and resources after first
      * searching in the parent class loader.
      *
      * @param  urls  The URLs from which to load classes and resources.
      *
      */
     public MLet(URL[] urls) {
         this(urls, true);
     }

     /**
      * Constructs a new MLet for the given URLs. The URLs will be
      * searched in the order specified for classes and resources
      * after first searching in the specified parent class loader.
      * The parent argument will be used as the parent class loader
      * for delegation.
      *
      * @param  urls  The URLs from which to load classes and resources.
      * @param  parent The parent class loader for delegation.
      *
      */
     public MLet(URL[] urls, ClassLoader parent) {
         this(urls, parent, true);
     }

     /**
      * Constructs a new MLet for the specified URLs, parent class
      * loader, and URLStreamHandlerFactory. The parent argument will
      * be used as the parent class loader for delegation. The factory
      * argument will be used as the stream handler factory to obtain
      * protocol handlers when creating new URLs.
      *
      * @param  urls  The URLs from which to load classes and resources.
      * @param  parent The parent class loader for delegation.
      * @param  factory  The URLStreamHandlerFactory to use when creating URLs.
      *
      */
     public MLet(URL[] urls,
                 ClassLoader parent,
                 URLStreamHandlerFactory factory) {
         this(urls, parent, factory, true);
     }

     /**
      * Constructs a new MLet for the specified URLs using the default
      * delegation parent ClassLoader.  The URLs will be searched in
      * the order specified for classes and resources after first
      * searching in the parent class loader.
      *
      * @param  urls  The URLs from which to load classes and resources.
      * @param  delegateToCLR  True if, when a class is not found in
      * either the parent ClassLoader or the URLs, the MLet should delegate
      * to its containing MBeanServer's {@link ClassLoaderRepository}.
      *
      */
     public MLet(URL[] urls, boolean delegateToCLR) {
         super(urls);
         init(delegateToCLR);
     }

     /**
      * Constructs a new MLet for the given URLs. The URLs will be
      * searched in the order specified for classes and resources
      * after first searching in the specified parent class loader.
      * The parent argument will be used as the parent class loader
      * for delegation.
      *
      * @param  urls  The URLs from which to load classes and resources.
      * @param  parent The parent class loader for delegation.
      * @param  delegateToCLR  True if, when a class is not found in
      * either the parent ClassLoader or the URLs, the MLet should delegate
      * to its containing MBeanServer's {@link ClassLoaderRepository}.
      *
      */
     public MLet(URL[] urls, ClassLoader parent, boolean delegateToCLR) {
         super(urls, parent);
         init(delegateToCLR);
     }

     /**
      * Constructs a new MLet for the specified URLs, parent class
      * loader, and URLStreamHandlerFactory. The parent argument will
      * be used as the parent class loader for delegation. The factory
      * argument will be used as the stream handler factory to obtain
      * protocol handlers when creating new URLs.
      *
      * @param  urls  The URLs from which to load classes and resources.
      * @param  parent The parent class loader for delegation.
      * @param  factory  The URLStreamHandlerFactory to use when creating URLs.
      * @param  delegateToCLR  True if, when a class is not found in
      * either the parent ClassLoader or the URLs, the MLet should delegate
      * to its containing MBeanServer's {@link ClassLoaderRepository}.
      *
      */
     public MLet(URL[] urls,
                 ClassLoader parent,
                 URLStreamHandlerFactory factory,
                 boolean delegateToCLR) {
         super(urls, parent, factory);
         init(delegateToCLR);
     }

     private void init(boolean delegateToCLR) {
         this.delegateToCLR = delegateToCLR;

         try {
             libraryDirectory = System.getProperty(MLET_LIB_DIR);
             if (libraryDirectory == null)
                 libraryDirectory = getTmpDir();
         } catch (SecurityException e) {
             // OK : We don't do AccessController.doPrivileged, but we don't
             //      stop the user from creating an MLet just because they
             //      can't read the MLET_LIB_DIR or java.io.tmpdir properties
             //      either.
         }
     }


     /*
      * ------------------------------------------
      *  PUBLIC METHODS
      * ------------------------------------------
      */


     /**
      * Appends the specified URL to the list of URLs to search for classes and
      * resources.
      */
     public void addURL(URL url) {
         if (!Arrays.asList(getURLs()).contains(url))
             super.addURL(url);
     }

     /**
      * Appends the specified URL to the list of URLs to search for classes and
      * resources.
      * @exception ServiceNotFoundException The specified URL is malformed.
      */
     public void addURL(String url) throws ServiceNotFoundException {
         try {
             URL ur = new URL(url);
             if (!Arrays.asList(getURLs()).contains(ur))
                 super.addURL(ur);
         } catch (MalformedURLException e) {
             if (MLET_LOGGER.isLoggable(Level.DEBUG)) {
                 MLET_LOGGER.log(Level.DEBUG, "Malformed URL: " + url, e);
             }
             throw new
                 ServiceNotFoundException("The specified URL is malformed");
         }
     }

     /** Returns the search path of URLs for loading classes and resources.
      * This includes the original list of URLs specified to the constructor,
      * along with any URLs subsequently appended by the addURL() method.
      */
     public URL[] getURLs() {
         return super.getURLs();
     }

     /**
      * Loads a text file containing MLET tags that define the MBeans to
      * be added to the MBean server. The location of the text file is specified by
      * a URL. The MBeans specified in the MLET file will be instantiated and
      * registered in the MBean server.
      *
      * @param url The URL of the text file to be loaded as URL object.
      *
      * @return  A set containing one entry per MLET tag in the m-let text file loaded.
      * Each entry specifies either the ObjectInstance for the created MBean, or a throwable object
      * (that is, an error or an exception) if the MBean could not be created.
      *
      * @exception ServiceNotFoundException One of the following errors has occurred: The m-let text file does
      * not contain an MLET tag, the m-let text file is not found, a mandatory
      * attribute of the MLET tag is not specified, the value of url is
      * null.
      * @exception IllegalStateException MLet MBean is not registered with an MBeanServer.
      */
     public Set<Object> getMBeansFromURL(URL url)
             throws ServiceNotFoundException  {
         if (url == null) {
             throw new ServiceNotFoundException("The specified URL is null");
         }
         return getMBeansFromURL(url.toString());
     }

     /**
      * Loads a text file containing MLET tags that define the MBeans to
      * be added to the MBean server. The location of the text file is specified by
      * a URL. The MBeans specified in the MLET file will be instantiated and
      * registered in the MBean server.
      *
      * @param url The URL of the text file to be loaded as String object.
      *
      * @return A set containing one entry per MLET tag in the m-let
      * text file loaded.  Each entry specifies either the
      * ObjectInstance for the created MBean, or a throwable object
      * (that is, an error or an exception) if the MBean could not be
      * created.
      *
      * @exception ServiceNotFoundException One of the following
      * errors has occurred: The m-let text file does not contain an
      * MLET tag, the m-let text file is not found, a mandatory
      * attribute of the MLET tag is not specified, the url is
      * malformed.
      * @exception IllegalStateException MLet MBean is not registered
      * with an MBeanServer.
      *
      */
     public Set<Object> getMBeansFromURL(String url)
             throws ServiceNotFoundException  {

         if (server == null) {
             throw new IllegalStateException("This MLet MBean is not " +
                                             "registered with an MBeanServer.");
         }
         // Parse arguments
         if (url == null) {
             MLET_LOGGER.log(Level.TRACE, "URL is null");
             throw new ServiceNotFoundException("The specified URL is null");
         } else {
             url = url.replace(File.separatorChar,'/');
         }
         if (MLET_LOGGER.isLoggable(Level.TRACE)) {
             MLET_LOGGER.log(Level.TRACE, "<URL = " + url + ">");
         }

         // Parse URL
         try {
             MLetParser parser = new MLetParser();
             mletList = parser.parseURL(url);
         } catch (Exception e) {
             final String msg =
                 "Problems while parsing URL [" + url +
                 "], got exception [" + e.toString() + "]";
             MLET_LOGGER.log(Level.TRACE, msg);
             throw EnvHelp.initCause(new ServiceNotFoundException(msg), e);
         }

         // Check that the list of MLets is not empty
         if (mletList.size() == 0) {
             final String msg =
                 "File " + url + " not found or MLET tag not defined in file";
             MLET_LOGGER.log(Level.TRACE, msg);
             throw new ServiceNotFoundException(msg);
         }

         // Walk through the list of MLets
         Set<Object> mbeans = new HashSet<Object>();
         for (MLetContent elmt : mletList) {
             // Initialize local variables
             String code = elmt.getCode();
             if (code != null) {
                 if (code.endsWith(".class")) {
                     code = code.substring(0, code.length() - 6);
                 }
             }
             String name = elmt.getName();
             URL codebase = elmt.getCodeBase();
             String version = elmt.getVersion();
             String serName = elmt.getSerializedObject();
             String jarFiles = elmt.getJarFiles();
             URL documentBase = elmt.getDocumentBase();

             // Display debug information
             if (MLET_LOGGER.isLoggable(Level.TRACE)) {
                 final StringBuilder strb = new StringBuilder()
                 .append("\n\tMLET TAG     = ").append(elmt.getAttributes())
                 .append("\n\tCODEBASE     = ").append(codebase)
                 .append("\n\tARCHIVE      = ").append(jarFiles)
                 .append("\n\tCODE         = ").append(code)
                 .append("\n\tOBJECT       = ").append(serName)
                 .append("\n\tNAME         = ").append(name)
                 .append("\n\tVERSION      = ").append(version)
                 .append("\n\tDOCUMENT URL = ").append(documentBase);
                 MLET_LOGGER.log(Level.TRACE, strb::toString);
             }

             // Load classes from JAR files
             StringTokenizer st = new StringTokenizer(jarFiles, ",", false);
             while (st.hasMoreTokens()) {
                 String tok = st.nextToken().trim();
                 if (MLET_LOGGER.isLoggable(Level.TRACE)) {
                     MLET_LOGGER.log(Level.TRACE,
                             "Load archive for codebase <" + codebase +
                             ">, file <" + tok + ">");
                 }
                 // Check which is the codebase to be used for loading the jar file.
                 // If we are using the base MLet implementation then it will be
                 // always the remote server but if the service has been extended in
                 // order to support caching and versioning then this method will
                 // return the appropriate one.
                 //
                 try {
                     codebase = check(version, codebase, tok, elmt);
                 } catch (Exception ex) {
                     MLET_LOGGER.log(Level.DEBUG,
                             "Got unexpected exception", ex);
                     mbeans.add(ex);
                     continue;
                 }

                 // Appends the specified JAR file URL to the list of
                 // URLs to search for classes and resources.
                 try {
                     if (!Arrays.asList(getURLs())
                         .contains(new URL(codebase.toString() + tok))) {
                         addURL(codebase + tok);
                     }
                 } catch (MalformedURLException me) {
                     // OK : Ignore jar file if its name provokes the
                     // URL to be an invalid one.
                 }

             }
             // Instantiate the class specified in the
             // CODE or OBJECT section of the MLet tag
             //
             Object o;
             ObjectInstance objInst;

             if (code != null && serName != null) {
                 final String msg =
                     "CODE and OBJECT parameters cannot be specified at the " +
                     "same time in tag MLET";
                 MLET_LOGGER.log(Level.TRACE, msg);
                 mbeans.add(new Error(msg));
                 continue;
             }
             if (code == null && serName == null) {
                 final String msg =
                     "Either CODE or OBJECT parameter must be specified in " +
                     "tag MLET";
                 MLET_LOGGER.log(Level.TRACE, msg);
                 mbeans.add(new Error(msg));
                 continue;
             }
             try {
                 if (code != null) {

                     List<String> signat = elmt.getParameterTypes();
                     List<String> stringPars = elmt.getParameterValues();
                     List<Object> objectPars = new ArrayList<Object>();

                     for (int i = 0; i < signat.size(); i++) {
                         objectPars.add(constructParameter(stringPars.get(i),
                                                           signat.get(i)));
                     }
                     if (signat.isEmpty()) {
                         if (name == null) {
                             objInst = server.createMBean(code, null,
                                                          mletObjectName);
                         } else {
                             objInst = server.createMBean(code,
                                                          new ObjectName(name),
                                                          mletObjectName);
                         }
                     } else {
                         Object[] parms = objectPars.toArray();
                         String[] signature = new String[signat.size()];
                         signat.toArray(signature);
                         if (MLET_LOGGER.isLoggable(Level.TRACE)) {
                             final StringBuilder strb = new StringBuilder();
                             for (int i = 0; i < signature.length; i++) {
                                 strb.append("\n\tSignature     = ")
                                 .append(signature[i])
                                 .append("\t\nParams        = ")
                                 .append(parms[i]);
                             }
                             MLET_LOGGER.log(Level.TRACE, strb::toString);
                         }
                         if (name == null) {
                             objInst =
                                 server.createMBean(code, null, mletObjectName,
                                                    parms, signature);
                         } else {
                             objInst =
                                 server.createMBean(code, new ObjectName(name),
                                                    mletObjectName, parms,
                                                    signature);
                         }
                     }
                 } else {
                     o = loadSerializedObject(codebase,serName);
                     if (name == null) {
                         server.registerMBean(o, null);
                     } else {
                         server.registerMBean(o,  new ObjectName(name));
                     }
                     objInst = new ObjectInstance(name, o.getClass().getName());
                 }
             } catch (ReflectionException  ex) {
                 MLET_LOGGER.log(Level.TRACE, "ReflectionException", ex);
                 mbeans.add(ex);
                 continue;
             } catch (InstanceAlreadyExistsException  ex) {
                 MLET_LOGGER.log(Level.TRACE,
                         "InstanceAlreadyExistsException", ex);
                 mbeans.add(ex);
                 continue;
             } catch (MBeanRegistrationException ex) {
                 MLET_LOGGER.log(Level.TRACE, "MBeanRegistrationException", ex);
                 mbeans.add(ex);
                 continue;
             } catch (MBeanException  ex) {
                 MLET_LOGGER.log(Level.TRACE, "MBeanException", ex);
                 mbeans.add(ex);
                 continue;
             } catch (NotCompliantMBeanException  ex) {
                 MLET_LOGGER.log(Level.TRACE,
                         "NotCompliantMBeanException", ex);
                 mbeans.add(ex);
                 continue;
             } catch (InstanceNotFoundException   ex) {
                 MLET_LOGGER.log(Level.TRACE,
                         "InstanceNotFoundException", ex);
                 mbeans.add(ex);
                 continue;
             } catch (IOException ex) {
                 MLET_LOGGER.log(Level.TRACE, "IOException", ex);
                 mbeans.add(ex);
                 continue;
             } catch (SecurityException ex) {
                 MLET_LOGGER.log(Level.TRACE, "SecurityException", ex);
                 mbeans.add(ex);
                 continue;
             } catch (Exception ex) {
                 MLET_LOGGER.log(Level.TRACE, "Exception", ex);
                 mbeans.add(ex);
                 continue;
             } catch (Error ex) {
                 MLET_LOGGER.log(Level.TRACE, "Error", ex);
                 mbeans.add(ex);
                 continue;
             }
             mbeans.add(objInst);
         }
         return mbeans;
     }

     /**
      * Gets the current directory used by the library loader for
      * storing native libraries before they are loaded into memory.
      *
      * @return The current directory used by the library loader.
      *
      * @see #setLibraryDirectory
      *
      * @throws UnsupportedOperationException if this implementation
      * does not support storing native libraries in this way.
      */
     public synchronized String getLibraryDirectory() {
         return libraryDirectory;
     }

     /**
      * Sets the directory used by the library loader for storing
      * native libraries before they are loaded into memory.
      *
      * @param libdir The directory used by the library loader.
      *
      * @see #getLibraryDirectory
      *
      * @throws UnsupportedOperationException if this implementation
      * does not support storing native libraries in this way.
      */
     public synchronized void setLibraryDirectory(String libdir) {
         libraryDirectory = libdir;
     }

     /**
      * Allows the m-let to perform any operations it needs before
      * being registered in the MBean server. If the ObjectName is
      * null, the m-let provides a default name for its registration
      * &lt;defaultDomain&gt;:type=MLet
      *
      * @param server The MBean server in which the m-let will be registered.
      * @param name The object name of the m-let.
      *
      * @return  The name of the m-let registered.
      *
      * @exception java.lang.Exception This exception should be caught by the MBean server and re-thrown
      *as an MBeanRegistrationException.
      */
     public ObjectName preRegister(MBeanServer server, ObjectName name)
             throws Exception {

         // Initialize local pointer to the MBean server
         setMBeanServer(server);

         // If no name is specified return a default name for the MLet
         if (name == null) {
             name = new ObjectName(server.getDefaultDomain() + ":" + ServiceName.MLET);
         }

        this.mletObjectName = name;
        return this.mletObjectName;
     }

     /**
      * Allows the m-let to perform any operations needed after having been
      * registered in the MBean server or after the registration has failed.
      *
      * @param registrationDone Indicates whether or not the m-let has
      * been successfully registered in the MBean server. The value
      * false means that either the registration phase has failed.
      *
      */
     public void postRegister (Boolean registrationDone) {
     }

     /**
      * Allows the m-let to perform any operations it needs before being unregistered
      * by the MBean server.
      *
      * @exception java.lang.Exception This exception should be caught
      * by the MBean server and re-thrown as an
      * MBeanRegistrationException.
      */
     public void preDeregister() throws java.lang.Exception {
     }


     /**
      * Allows the m-let to perform any operations needed after having been
      * unregistered in the MBean server.
      */
     public void postDeregister() {
     }

     /**
      * <p>Save this MLet's contents to the given {@link ObjectOutput}.
      * Not all implementations support this method.  Those that do not
      * throw {@link UnsupportedOperationException}.  A subclass may
      * override this method to support it or to change the format of
      * the written data.</p>
      *
      * <p>The format of the written data is not specified, but if
      * an implementation supports {@link #writeExternal} it must
      * also support {@link #readExternal} in such a way that what is
      * written by the former can be read by the latter.</p>
      *
      * @param out The object output stream to write to.
      *
      * @exception IOException If a problem occurred while writing.
      * @exception UnsupportedOperationException If this
      * implementation does not support this operation.
      */
     public void writeExternal(ObjectOutput out)
             throws IOException, UnsupportedOperationException {
         throw new UnsupportedOperationException("MLet.writeExternal");
     }

     /**
      * <p>Restore this MLet's contents from the given {@link ObjectInput}.
      * Not all implementations support this method.  Those that do not
      * throw {@link UnsupportedOperationException}.  A subclass may
      * override this method to support it or to change the format of
      * the read data.</p>
      *
      * <p>The format of the read data is not specified, but if an
      * implementation supports {@link #readExternal} it must also
      * support {@link #writeExternal} in such a way that what is
      * written by the latter can be read by the former.</p>
      *
      * @param in The object input stream to read from.
      *
      * @exception IOException if a problem occurred while reading.
      * @exception ClassNotFoundException if the class for the object
      * being restored cannot be found.
      * @exception UnsupportedOperationException if this
      * implementation does not support this operation.
      */
     public void readExternal(ObjectInput in)
             throws IOException, ClassNotFoundException,
                    UnsupportedOperationException {
         throw new UnsupportedOperationException("MLet.readExternal");
     }

     /*
      * ------------------------------------------
      *  PACKAGE METHODS
      * ------------------------------------------
      */

     /**
      * <p>Load a class, using the given {@link ClassLoaderRepository} if
      * the class is not found in this MLet's URLs.  The given
      * ClassLoaderRepository can be null, in which case a {@link
      * ClassNotFoundException} occurs immediately if the class is not
      * found in this MLet's URLs.</p>
      *
      * @param name The name of the class we want to load.
      * @param clr  The ClassLoaderRepository that will be used to search
      *             for the given class, if it is not found in this
      *             ClassLoader.  May be null.
      * @return The resulting Class object.
      * @exception ClassNotFoundException The specified class could not be
      *            found in this ClassLoader nor in the given
      *            ClassLoaderRepository.
      *
      */
     public synchronized Class<?> loadClass(String name,
                                            ClassLoaderRepository clr)
              throws ClassNotFoundException {
         final ClassLoaderRepository before=currentClr;
         try {
             currentClr = clr;
             return loadClass(name);
         } finally {
             currentClr = before;
         }
     }

     /*
      * ------------------------------------------
      *  PROTECTED METHODS
      * ------------------------------------------
      */

     /**
      * This is the main method for class loaders that is being redefined.
      *
      * @param name The name of the class.
      *
      * @return The resulting Class object.
      *
      * @exception ClassNotFoundException The specified class could not be
      *            found.
      */
     protected Class<?> findClass(String name) throws ClassNotFoundException {
         /* currentClr is context sensitive - used to avoid recursion
            in the class loader repository.  (This is no longer
            necessary with the new CLR semantics but is kept for
            compatibility with code that might have called the
            two-parameter loadClass explicitly.)  */
         return findClass(name, currentClr);
     }

     /**
      * Called by {@link MLet#findClass(java.lang.String)}.
      *
      * @param name The name of the class that we want to load/find.
      * @param clr The ClassLoaderRepository that can be used to search
      *            for the given class. This parameter is
      *            <code>null</code> when called from within the
      *            {@link javax.management.MBeanServerFactory#getClassLoaderRepository(javax.management.MBeanServer) Class Loader Repository}.
      * @exception ClassNotFoundException The specified class could not be
      *            found.
      *
      **/
     Class<?> findClass(String name, ClassLoaderRepository clr)
         throws ClassNotFoundException {
         Class<?> c = null;
         MLET_LOGGER.log(Level.TRACE, name);
         // Try looking in the JAR:
         try {
             c = super.findClass(name);
             if (MLET_LOGGER.isLoggable(Level.TRACE)) {
                 MLET_LOGGER.log(Level.TRACE,
                         "Class " + name + " loaded through MLet classloader");
             }
         } catch (ClassNotFoundException e) {
             // Drop through
             if (MLET_LOGGER.isLoggable(Level.TRACE)) {
                 MLET_LOGGER.log(Level.TRACE,
                         "Class " + name + " not found locally");
             }
         }
         // if we are not called from the ClassLoaderRepository
         if (c == null && delegateToCLR && clr != null) {
             // Try the classloader repository:
             //
             try {
                 if (MLET_LOGGER.isLoggable(Level.TRACE)) {
                     MLET_LOGGER.log(Level.TRACE,
                             "Class " + name + " : looking in CLR");
                 }
                 c = clr.loadClassBefore(this, name);
                 // The loadClassBefore method never returns null.
                 // If the class is not found we get an exception.
                 if (MLET_LOGGER.isLoggable(Level.TRACE)) {
                     MLET_LOGGER.log(Level.TRACE,
                             "Class " + name + " loaded through " +
                             "the default classloader repository");
                 }
             } catch (ClassNotFoundException e) {
                 // Drop through
                 if (MLET_LOGGER.isLoggable(Level.TRACE)) {
                     MLET_LOGGER.log(Level.TRACE,
                             "Class " + name + " not found in CLR");
                 }
             }
         }
         if (c == null) {
             MLET_LOGGER.log(Level.TRACE, "Failed to load class " + name);
             throw new ClassNotFoundException(name);
         }
         return c;
     }

     /**
      * Returns the absolute path name of a native library. The VM
      * invokes this method to locate the native libraries that belong
      * to classes loaded with this class loader. Libraries are
      * searched in the JAR files using first just the native library
      * name and if not found the native library name together with
      * the architecture-specific path name
      * (<code>OSName/OSArch/OSVersion/lib/nativelibname</code>), i.e.
      * <p>
      * the library stat on Solaris SPARC 5.7 will be searched in the JAR file as:
      * <OL>
      * <LI>libstat.so
      * <LI>SunOS/sparc/5.7/lib/libstat.so
      * </OL>
      * the library stat on Windows NT 4.0 will be searched in the JAR file as:
      * <OL>
      * <LI>stat.dll
      * <LI>WindowsNT/x86/4.0/lib/stat.dll
      * </OL>
      *
      * <p>More specifically, let <em>{@code nativelibname}</em> be the result of
      * {@link System#mapLibraryName(java.lang.String)
      * System.mapLibraryName}{@code (libname)}.  Then the following names are
      * searched in the JAR files, in order:<br>
      * <em>{@code nativelibname}</em><br>
      * {@code <os.name>/<os.arch>/<os.version>/lib/}<em>{@code nativelibname}</em><br>
      * where {@code <X>} means {@code System.getProperty(X)} with any
      * spaces in the result removed, and {@code /} stands for the
      * file separator character ({@link File#separator}).
      * <p>
      * If this method returns <code>null</code>, i.e. the libraries
      * were not found in any of the JAR files loaded with this class
      * loader, the VM searches the library along the path specified
      * as the <code>java.library.path</code> property.
      *
      * @param libname The library name.
      *
      * @return The absolute path of the native library.
      */
     protected String findLibrary(String libname) {

         String abs_path;
         String mth = "findLibrary";

         // Get the platform-specific string representing a native library.
         //
         String nativelibname = System.mapLibraryName(libname);

         //
         // See if the native library is accessible as a resource through the JAR file.
         //
         if (MLET_LOGGER.isLoggable(Level.TRACE)) {
             MLET_LOGGER.log(Level.TRACE,
                     "Search " + libname + " in all JAR files");
         }

         // First try to locate the library in the JAR file using only
         // the native library name.  e.g. if user requested a load
         // for "foo" on Solaris SPARC 5.7 we try to load "libfoo.so"
         // from the JAR file.
         //
         if (MLET_LOGGER.isLoggable(Level.TRACE)) {
             MLET_LOGGER.log(Level.TRACE,
                     "loadLibraryAsResource(" + nativelibname + ")");
         }
         abs_path = loadLibraryAsResource(nativelibname);
         if (abs_path != null) {
             if (MLET_LOGGER.isLoggable(Level.TRACE)) {
                 MLET_LOGGER.log(Level.TRACE,
                         nativelibname + " loaded, absolute path = " + abs_path);
             }
             return abs_path;
         }

         // Next try to locate it using the native library name and
         // the architecture-specific path name.  e.g. if user
         // requested a load for "foo" on Solaris SPARC 5.7 we try to
         // load "SunOS/sparc/5.7/lib/libfoo.so" from the JAR file.
         //
         nativelibname = removeSpace(System.getProperty("os.name")) + File.separator +
             removeSpace(System.getProperty("os.arch")) + File.separator +
             removeSpace(System.getProperty("os.version")) + File.separator +
             "lib" + File.separator + nativelibname;
         if (MLET_LOGGER.isLoggable(Level.TRACE)) {
             MLET_LOGGER.log(Level.TRACE,
                     "loadLibraryAsResource(" + nativelibname + ")");
         }

         abs_path = loadLibraryAsResource(nativelibname);
         if (abs_path != null) {
             if (MLET_LOGGER.isLoggable(Level.TRACE)) {
                 MLET_LOGGER.log(Level.TRACE,
                         nativelibname + " loaded, absolute path = " + abs_path);
             }
             return abs_path;
         }

         //
         // All paths exhausted, library not found in JAR file.
         //

         if (MLET_LOGGER.isLoggable(Level.TRACE)) {
             MLET_LOGGER.log(Level.TRACE,
                     libname + " not found in any JAR file");
             MLET_LOGGER.log(Level.TRACE,
                     "Search " + libname + " along the path " +
                     "specified as the java.library.path property");
         }

         // Let the VM search the library along the path
         // specified as the java.library.path property.
         //
         return null;
     }


     /*
      * ------------------------------------------
      *  PRIVATE METHODS
      * ------------------------------------------
      */

     private String getTmpDir() {
         // JDK 1.4
         String tmpDir = System.getProperty("java.io.tmpdir");
         if (tmpDir != null) return tmpDir;

         // JDK < 1.4
         File tmpFile = null;
         try {
             // Try to guess the system temporary dir...
             tmpFile = File.createTempFile("tmp","jmx");
             if (tmpFile == null) return null;
             final File tmpDirFile = tmpFile.getParentFile();
             if (tmpDirFile == null) return null;
             return tmpDirFile.getAbsolutePath();
         } catch (Exception x) {
             MLET_LOGGER.log(Level.DEBUG,
                     "Failed to determine system temporary dir");
             return null;
         } finally {
             // Cleanup ...
             if (tmpFile!=null) {
                 try {
                     boolean deleted = tmpFile.delete();
                     if (!deleted) {
                         MLET_LOGGER.log(Level.DEBUG,
                                 "Failed to delete temp file");
                     }
                 } catch (Exception x) {
                     MLET_LOGGER.log(Level.DEBUG,
                             "Failed to delete temporary file", x);
                 }
             }
        }
     }

     /**
      * Search the specified native library in any of the JAR files
      * loaded by this classloader.  If the library is found copy it
      * into the library directory and return the absolute path.  If
      * the library is not found then return null.
      */
     private synchronized String loadLibraryAsResource(String libname) {
         try {
             InputStream is = getResourceAsStream(
                     libname.replace(File.separatorChar,'/'));
             if (is != null) {
                 try {
                     File directory = new File(libraryDirectory);
                     directory.mkdirs();
                     File file = Files.createTempFile(directory.toPath(),
                                                      libname + ".", null)
                                      .toFile();
                     file.deleteOnExit();
                     Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                     return file.getAbsolutePath();
                 } finally {
                     is.close();
                 }
             }
         } catch (Exception e) {
             MLET_LOGGER.log(Level.DEBUG,
                     "Failed to load library : " + libname, e);
             return null;
         }
         return null;
     }

   /**
    * Removes any white space from a string. This is used to
    * convert strings such as "Windows NT" to "WindowsNT".
    */
     private static String removeSpace(String s) {
         return s.trim().replace(" ", "");
     }

     /**
      * <p>This method is to be overridden when extending this service to
      * support caching and versioning.  It is called from {@link
      * #getMBeansFromURL getMBeansFromURL} when the version,
      * codebase, and jarfile have been extracted from the MLet file,
      * and can be used to verify that it is all right to load the
      * given MBean, or to replace the given URL with a different one.</p>
      *
      * <p>The default implementation of this method returns
      * <code>codebase</code> unchanged.</p>
      *
      * @param version The version number of the <CODE>.jar</CODE>
      * file stored locally.
      * @param codebase The base URL of the remote <CODE>.jar</CODE> file.
      * @param jarfile The name of the <CODE>.jar</CODE> file to be loaded.
      * @param mlet The <CODE>MLetContent</CODE> instance that
      * represents the <CODE>MLET</CODE> tag.
      *
      * @return the codebase to use for the loaded MBean.  The returned
      * value should not be null.
      *
      * @exception Exception if the MBean is not to be loaded for some
      * reason.  The exception will be added to the set returned by
      * {@link #getMBeansFromURL getMBeansFromURL}.
      *
      */
     protected URL check(String version, URL codebase, String jarfile,
                         MLetContent mlet)
             throws Exception {
         return codebase;
     }

    /**
     * Loads the serialized object specified by the <CODE>OBJECT</CODE>
     * attribute of the <CODE>MLET</CODE> tag.
     *
     * @param codebase The <CODE>codebase</CODE>.
     * @param filename The name of the file containing the serialized object.
     * @return The serialized object.
     * @exception ClassNotFoundException The specified serialized
     * object could not be found.
     * @exception IOException An I/O error occurred while loading
     * serialized object.
     */
     private Object loadSerializedObject(URL codebase, String filename)
             throws IOException, ClassNotFoundException {
        if (filename != null) {
            filename = filename.replace(File.separatorChar,'/');
        }
        if (MLET_LOGGER.isLoggable(Level.TRACE)) {
            MLET_LOGGER.log(Level.TRACE, codebase.toString() + filename);
        }
        InputStream is = getResourceAsStream(filename);
        if (is != null) {
            try {
                ObjectInputStream ois = new MLetObjectInputStream(is, this);
                Object serObject = ois.readObject();
                ois.close();
                return serObject;
            } catch (IOException e) {
                if (MLET_LOGGER.isLoggable(Level.DEBUG)) {
                    MLET_LOGGER.log(Level.DEBUG,
                            "Exception while deserializing " + filename, e);
                }
                throw e;
            } catch (ClassNotFoundException e) {
                if (MLET_LOGGER.isLoggable(Level.DEBUG)) {
                    MLET_LOGGER.log(Level.DEBUG,
                            "Exception while deserializing " + filename, e);
                }
                throw e;
            }
        } else {
            if (MLET_LOGGER.isLoggable(Level.DEBUG)) {
                MLET_LOGGER.log(Level.DEBUG, "Error: File " + filename +
                        " containing serialized object not found");
            }
            throw new Error("File " + filename + " containing serialized object not found");
        }
     }

     /**
      * Converts the String value of the constructor's parameter to
      * a basic Java object with the type of the parameter.
      */
     private  Object constructParameter(String param, String type) {
         // check if it is a primitive type
         Class<?> c = primitiveClasses.get(type);
         if (c != null) {
            try {
                Constructor<?> cons =
                    c.getConstructor(String.class);
                Object[] oo = new Object[1];
                oo[0]=param;
                return(cons.newInstance(oo));

            } catch (Exception  e) {
                MLET_LOGGER.log(Level.DEBUG, "Got unexpected exception", e);
            }
        }
        if (type.compareTo("java.lang.Boolean") == 0)
             return Boolean.valueOf(param);
        if (type.compareTo("java.lang.Byte") == 0)
             return Byte.valueOf(param);
        if (type.compareTo("java.lang.Short") == 0)
             return Short.valueOf(param);
        if (type.compareTo("java.lang.Long") == 0)
             return Long.valueOf(param);
        if (type.compareTo("java.lang.Integer") == 0)
             return Integer.valueOf(param);
        if (type.compareTo("java.lang.Float") == 0)
             return Float.valueOf(param);
        if (type.compareTo("java.lang.Double") == 0)
             return Double.valueOf(param);
        if (type.compareTo("java.lang.String") == 0)
             return param;

        return param;
     }

    @SuppressWarnings("removal")
    private synchronized void setMBeanServer(final MBeanServer server) {
        this.server = server;
        PrivilegedAction<ClassLoaderRepository> act =
            new PrivilegedAction<ClassLoaderRepository>() {
                public ClassLoaderRepository run() {
                    return server.getClassLoaderRepository();
                }
            };
        currentClr = AccessController.doPrivileged(act);
    }

}
