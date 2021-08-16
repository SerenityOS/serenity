/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * $Id: TemplatesImpl.java,v 1.8 2007/03/26 20:12:27 spericas Exp $
 */

package com.sun.org.apache.xalan.internal.xsltc.trax;

import com.sun.org.apache.xalan.internal.utils.ObjectFactory;
import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.Translet;
import com.sun.org.apache.xalan.internal.xsltc.compiler.Constants;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.RuntimePermission;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.URIResolver;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.SecuritySupport;


/**
 * @author Morten Jorgensen
 * @author G. Todd Millerj
 * @author Jochen Cordes <Jochen.Cordes@t-online.de>
 * @author Santiago Pericas-Geertsen
 * @LastModified: May 2021
 */
public final class TemplatesImpl implements Templates, Serializable {
    static final long serialVersionUID = 673094361519270707L;
    public final static String DESERIALIZE_TRANSLET = "jdk.xml.enableTemplatesImplDeserialization";

    /**
     * Name of the superclass of all translets. This is needed to
     * determine which, among all classes comprising a translet,
     * is the main one.
     */
    private static String ABSTRACT_TRANSLET
        = "com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet";

    /**
     * Name of the main class or default name if unknown.
     */
    private String _name = null;

    /**
     * Contains the actual class definition for the translet class and
     * any auxiliary classes.
     */
    private byte[][] _bytecodes = null;

    /**
     * Contains the translet class definition(s). These are created when
     * this Templates is created or when it is read back from disk.
     */
    private Class<?>[] _class = null;

    /**
     * The index of the main translet class in the arrays _class[] and
     * _bytecodes.
     */
    private int _transletIndex = -1;

    /**
     * Contains the list of auxiliary class definitions.
     */
    private transient Map<String, Class<?>> _auxClasses = null;

    /**
     * Output properties of this translet.
     */
    private Properties _outputProperties;

    /**
     * Number of spaces to add for output indentation.
     */
    private int _indentNumber;

    /**
     * This URIResolver is passed to all Transformers.
     * Declaring it transient to fix bug 22438
     */
    private transient URIResolver _uriResolver = null;

    /**
     * Cache the DTM for the stylesheet in a thread local variable,
     * which is used by the document('') function.
     * Use ThreadLocal because a DTM cannot be shared between
     * multiple threads.
     * Declaring it transient to fix bug 22438
     */
    private transient ThreadLocal<DOM> _sdom = new ThreadLocal<>();

    /**
     * A reference to the transformer factory that this templates
     * object belongs to.
     */
    private transient TransformerFactoryImpl _tfactory = null;

    /**
     * A flag to determine whether the system-default parser may be overridden
     */
    private transient boolean _overrideDefaultParser;

    /**
     * protocols allowed for external references set by the stylesheet processing instruction, Import and Include element.
     */
    private transient String _accessExternalStylesheet = JdkConstants.EXTERNAL_ACCESS_DEFAULT;

    /**
     * @serialField _name String The Name of the main class
     * @serialField _bytecodes byte[][] Class definition
     * @serialField _class Class[] The translet class definition(s).
     * @serialField _transletIndex int The index of the main translet class
     * @serialField _outputProperties Properties Output properties of this translet.
     * @serialField _indentNumber int Number of spaces to add for output indentation.
     */
    private static final ObjectStreamField[] serialPersistentFields =
        new ObjectStreamField[] {
            new ObjectStreamField("_name", String.class),
            new ObjectStreamField("_bytecodes", byte[][].class),
            new ObjectStreamField("_class", Class[].class),
            new ObjectStreamField("_transletIndex", int.class),
            new ObjectStreamField("_outputProperties", Properties.class),
            new ObjectStreamField("_indentNumber", int.class),
        };

    static final class TransletClassLoader extends ClassLoader {
        private final Map<String, Class<?>> _loadedExternalExtensionFunctions;

         TransletClassLoader(ClassLoader parent) {
             super(parent);
            _loadedExternalExtensionFunctions = null;
        }

        TransletClassLoader(ClassLoader parent, Map<String, Class<?>> mapEF) {
            super(parent);
            _loadedExternalExtensionFunctions = mapEF;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            Class<?> ret = null;
            // The _loadedExternalExtensionFunctions will be empty when the
            // SecurityManager is not set and the FSP is turned off
            if (_loadedExternalExtensionFunctions != null) {
                ret = _loadedExternalExtensionFunctions.get(name);
            }
            if (ret == null) {
                ret = super.loadClass(name);
            }
            return ret;
         }

        /**
         * Access to final protected superclass member from outer class.
         */
        Class<?> defineClass(final byte[] b) {
            return defineClass(null, b, 0, b.length);
        }

        Class<?> defineClass(final byte[] b, ProtectionDomain pd) {
            return defineClass(null, b, 0, b.length, pd);
        }
    }


    /**
     * Create an XSLTC template object from the bytecodes.
     * The bytecodes for the translet and auxiliary classes, plus the name of
     * the main translet class, must be supplied.
     */
    protected TemplatesImpl(byte[][] bytecodes, String transletName,
        Properties outputProperties, int indentNumber,
        TransformerFactoryImpl tfactory)
    {
        _bytecodes = bytecodes;
        init(transletName, outputProperties, indentNumber, tfactory);
    }

    /**
     * Create an XSLTC template object from the translet class definition(s).
     */
    protected TemplatesImpl(Class<?>[] transletClasses, String transletName,
        Properties outputProperties, int indentNumber,
        TransformerFactoryImpl tfactory)
    {
        _class     = transletClasses;
        _transletIndex = 0;
        init(transletName, outputProperties, indentNumber, tfactory);
    }

    private void init(String transletName,
        Properties outputProperties, int indentNumber,
        TransformerFactoryImpl tfactory) {
        _name      = transletName;
        _outputProperties = outputProperties;
        _indentNumber = indentNumber;
        _tfactory = tfactory;
        _overrideDefaultParser = tfactory.overrideDefaultParser();
        _accessExternalStylesheet = (String) tfactory.getAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET);
    }
    /**
     * Need for de-serialization, see readObject().
     */
    public TemplatesImpl() { }

    /**
     *  Overrides the default readObject implementation since we decided
     *  it would be cleaner not to serialize the entire tranformer
     *  factory.  [ ref bugzilla 12317 ]
     *  We need to check if the user defined class for URIResolver also
     *  implemented Serializable
     *  if yes then we need to deserialize the URIResolver
     *  Fix for bugzilla bug 22438
     */
    @SuppressWarnings("unchecked")
    private void  readObject(ObjectInputStream is)
      throws IOException, ClassNotFoundException
    {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null){
            String temp = SecuritySupport.getSystemProperty(DESERIALIZE_TRANSLET);
            if (temp == null || !(temp.length()==0 || temp.equalsIgnoreCase("true"))) {
                ErrorMsg err = new ErrorMsg(ErrorMsg.DESERIALIZE_TRANSLET_ERR);
                throw new UnsupportedOperationException(err.toString());
            }
        }

        // We have to read serialized fields first.
        ObjectInputStream.GetField gf = is.readFields();
        _name = (String)gf.get("_name", null);
        _bytecodes = (byte[][])gf.get("_bytecodes", null);
        _class = (Class<?>[])gf.get("_class", null);
        _transletIndex = gf.get("_transletIndex", -1);

        _outputProperties = (Properties)gf.get("_outputProperties", null);
        _indentNumber = gf.get("_indentNumber", 0);

        if (is.readBoolean()) {
            _uriResolver = (URIResolver) is.readObject();
        }

        _tfactory = new TransformerFactoryImpl();
    }


    /**
     *  This is to fix bugzilla bug 22438
     *  If the user defined class implements URIResolver and Serializable
     *  then we want it to get serialized
     */
    private void writeObject(ObjectOutputStream os)
        throws IOException {
        if (_auxClasses != null) {
            //throw with the same message as when Hashtable was used for compatibility.
            throw new NotSerializableException(
                    "com.sun.org.apache.xalan.internal.xsltc.runtime.Hashtable");
        }

        // Write serialized fields
        ObjectOutputStream.PutField pf = os.putFields();
        pf.put("_name", _name);
        pf.put("_bytecodes", _bytecodes);
        pf.put("_class", _class);
        pf.put("_transletIndex", _transletIndex);
        pf.put("_outputProperties", _outputProperties);
        pf.put("_indentNumber", _indentNumber);
        os.writeFields();

        if (_uriResolver instanceof Serializable) {
            os.writeBoolean(true);
            os.writeObject((Serializable) _uriResolver);
        }
        else {
            os.writeBoolean(false);
        }
    }

    /**
     * Return the state of the services mechanism feature.
     */
    public boolean overrideDefaultParser() {
        return _overrideDefaultParser;
    }

     /**
     * Store URIResolver needed for Transformers.
     */
    public synchronized void setURIResolver(URIResolver resolver) {
        _uriResolver = resolver;
    }

    /**
     * The TransformerFactory must pass us the translet bytecodes using this
     * method before we can create any translet instances
     *
     * Note: This method is private for security reasons. See
     * CR 6537898. When merging with Apache, we must ensure
     * that the privateness of this method is maintained (that
     * is why it wasn't removed).
     */
    private synchronized void setTransletBytecodes(byte[][] bytecodes) {
        _bytecodes = bytecodes;
    }

    /**
     * Returns the translet bytecodes stored in this template
     *
     * Note: This method is private for security reasons. See
     * CR 6537898. When merging with Apache, we must ensure
     * that the privateness of this method is maintained (that
     * is why it wasn't removed).
     */
    private synchronized byte[][] getTransletBytecodes() {
        return _bytecodes;
    }

    /**
     * Returns the translet bytecodes stored in this template
     *
     * Note: This method is private for security reasons. See
     * CR 6537898. When merging with Apache, we must ensure
     * that the privateness of this method is maintained (that
     * is why it wasn't removed).
     */
    private synchronized Class<?>[] getTransletClasses() {
        try {
            if (_class == null) defineTransletClasses();
        }
        catch (TransformerConfigurationException e) {
            // Falls through
        }
        return _class;
    }

    /**
     * Returns the index of the main class in array of bytecodes
     */
    public synchronized int getTransletIndex() {
        try {
            if (_class == null) defineTransletClasses();
        }
        catch (TransformerConfigurationException e) {
            // Falls through
        }
        return _transletIndex;
    }

    /**
     * The TransformerFactory should call this method to set the translet name
     */
    protected synchronized void setTransletName(String name) {
        _name = name;
    }

    /**
     * Returns the name of the main translet class stored in this template
     */
    protected synchronized String getTransletName() {
        return _name;
    }


    /**
     * Creates a module layer with one module that is defined to the given class
     * loader.
     */
    private Module createModule(ModuleDescriptor descriptor, ClassLoader loader) {
        String mn = descriptor.name();

        ModuleReference mref = new ModuleReference(descriptor, null) {
            @Override
            public ModuleReader open() {
                throw new UnsupportedOperationException();
            }
        };

        ModuleFinder finder = new ModuleFinder() {
            @Override
            public Optional<ModuleReference> find(String name) {
                if (name.equals(mn)) {
                    return Optional.of(mref);
                } else {
                    return Optional.empty();
                }
            }
            @Override
            public Set<ModuleReference> findAll() {
                return Set.of(mref);
            }
        };

        ModuleLayer bootLayer = ModuleLayer.boot();

        Configuration cf = bootLayer.configuration()
                .resolve(finder, ModuleFinder.of(), Set.of(mn));

        PrivilegedAction<ModuleLayer> pa = () -> bootLayer.defineModules(cf, name -> loader);
        @SuppressWarnings("removal")
        ModuleLayer layer = AccessController.doPrivileged(pa);

        Module m = layer.findModule(mn).get();
        assert m.getLayer() == layer;

        return m;
    }

    /**
     * Defines the translet class and auxiliary classes.
     * Returns a reference to the Class object that defines the main class
     */
    private void defineTransletClasses()
        throws TransformerConfigurationException {

        if (_bytecodes == null) {
            ErrorMsg err = new ErrorMsg(ErrorMsg.NO_TRANSLET_CLASS_ERR);
            throw new TransformerConfigurationException(err.toString());
        }

        @SuppressWarnings("removal")
        TransletClassLoader loader =
                AccessController.doPrivileged(new PrivilegedAction<TransletClassLoader>() {
                public TransletClassLoader run() {
                    return new TransletClassLoader(ObjectFactory.findClassLoader(),
                            _tfactory.getExternalExtensionsMap());
                }
            });

        try {
            final int classCount = _bytecodes.length;
            _class = new Class<?>[classCount];

            if (classCount > 1) {
                _auxClasses = new HashMap<>();
            }

            // create a module for the translet

            String mn = "jdk.translet";

            String pn = _tfactory.getPackageName();
            assert pn != null && pn.length() > 0;

            ModuleDescriptor descriptor =
                ModuleDescriptor.newModule(mn, Set.of(ModuleDescriptor.Modifier.SYNTHETIC))
                                .requires("java.xml")
                                .exports(pn, Set.of("java.xml"))
                                .build();

            Module m = createModule(descriptor, loader);

            // the module needs access to runtime classes
            Module thisModule = TemplatesImpl.class.getModule();
            // the module also needs permission to access each package
            // that is exported to it
            PermissionCollection perms =
                new RuntimePermission("*").newPermissionCollection();
            Arrays.asList(Constants.PKGS_USED_BY_TRANSLET_CLASSES).forEach(p -> {
                thisModule.addExports(p, m);
                perms.add(new RuntimePermission("accessClassInPackage." + p));
            });

            CodeSource codeSource = new CodeSource(null, (CodeSigner[])null);
            ProtectionDomain pd = new ProtectionDomain(codeSource, perms,
                                                       loader, null);

            // java.xml needs to instantiate the translet class
            thisModule.addReads(m);

            for (int i = 0; i < classCount; i++) {
                _class[i] = loader.defineClass(_bytecodes[i], pd);
                final Class<?> superClass = _class[i].getSuperclass();

                // Check if this is the main class
                if (superClass.getName().equals(ABSTRACT_TRANSLET)) {
                    _transletIndex = i;
                }
                else {
                    _auxClasses.put(_class[i].getName(), _class[i]);
                }
            }

            if (_transletIndex < 0) {
                ErrorMsg err= new ErrorMsg(ErrorMsg.NO_MAIN_TRANSLET_ERR, _name);
                throw new TransformerConfigurationException(err.toString());
            }
        }
        catch (ClassFormatError e) {
            ErrorMsg err = new ErrorMsg(ErrorMsg.TRANSLET_CLASS_ERR, _name);
            throw new TransformerConfigurationException(err.toString(), e);
        }
        catch (LinkageError e) {
            ErrorMsg err = new ErrorMsg(ErrorMsg.TRANSLET_OBJECT_ERR, _name);
            throw new TransformerConfigurationException(err.toString(), e);
        }
    }

    /**
     * This method generates an instance of the translet class that is
     * wrapped inside this Template. The translet instance will later
     * be wrapped inside a Transformer object.
     */
    private Translet getTransletInstance()
        throws TransformerConfigurationException {
        try {
            if (_name == null) return null;

            if (_class == null) defineTransletClasses();

            // The translet needs to keep a reference to all its auxiliary
            // class to prevent the GC from collecting them
            AbstractTranslet translet = (AbstractTranslet)
                    _class[_transletIndex].getConstructor().newInstance();
            translet.postInitialization();
            translet.setTemplates(this);
            translet.setOverrideDefaultParser(_overrideDefaultParser);
            translet.setAllowedProtocols(_accessExternalStylesheet);
            if (_auxClasses != null) {
                translet.setAuxiliaryClasses(_auxClasses);
            }

            return translet;
        }
        catch (InstantiationException | IllegalAccessException |
                NoSuchMethodException | InvocationTargetException e) {
            ErrorMsg err = new ErrorMsg(ErrorMsg.TRANSLET_OBJECT_ERR, _name);
            throw new TransformerConfigurationException(err.toString(), e);
        }
    }

    /**
     * Implements JAXP's Templates.newTransformer()
     *
     * @throws TransformerConfigurationException
     */
    public synchronized Transformer newTransformer()
        throws TransformerConfigurationException
    {
        TransformerImpl transformer;

        transformer = new TransformerImpl(getTransletInstance(), _outputProperties,
            _indentNumber, _tfactory);

        if (_uriResolver != null) {
            transformer.setURIResolver(_uriResolver);
        }

        if (_tfactory.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING)) {
            transformer.setSecureProcessing(true);
        }
        return transformer;
    }

    /**
     * Implements JAXP's Templates.getOutputProperties(). We need to
     * instanciate a translet to get the output settings, so
     * we might as well just instanciate a Transformer and use its
     * implementation of this method.
     */
    public synchronized Properties getOutputProperties() {
        try {
            return newTransformer().getOutputProperties();
        }
        catch (TransformerConfigurationException e) {
            return null;
        }
    }

    /**
     * Return the thread local copy of the stylesheet DOM.
     */
    public DOM getStylesheetDOM() {
        return _sdom.get();
    }

    /**
     * Set the thread local copy of the stylesheet DOM.
     */
    public void setStylesheetDOM(DOM sdom) {
        _sdom.set(sdom);
    }
}
