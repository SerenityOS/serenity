/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.impl ;

import com.sun.org.apache.xerces.internal.impl.io.ASCIIReader;
import com.sun.org.apache.xerces.internal.impl.io.UCSReader;
import com.sun.org.apache.xerces.internal.impl.io.UTF16Reader;
import com.sun.org.apache.xerces.internal.impl.io.UTF8Reader;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.impl.validation.ValidationManager;
import com.sun.org.apache.xerces.internal.util.*;
import com.sun.org.apache.xerces.internal.util.URI;
import com.sun.org.apache.xerces.internal.utils.XMLLimitAnalyzer;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityManager;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.*;
import com.sun.xml.internal.stream.Entity;
import com.sun.xml.internal.stream.StaxEntityResolverWrapper;
import com.sun.xml.internal.stream.StaxXMLInputSource;
import com.sun.xml.internal.stream.XMLEntityStorage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogException;
import javax.xml.catalog.CatalogFeatures.Feature;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import javax.xml.catalog.CatalogResolver;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.Source;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.JdkXmlUtils;
import jdk.xml.internal.SecuritySupport;
import org.xml.sax.InputSource;


/**
 * Will keep track of current entity.
 *
 * The entity manager handles the registration of general and parameter
 * entities; resolves entities; and starts entities. The entity manager
 * is a central component in a standard parser configuration and this
 * class works directly with the entity scanner to manage the underlying
 * xni.
 * <p>
 * This component requires the following features and properties from the
 * component manager that uses it:
 * <ul>
 *  <li>http://xml.org/sax/features/validation</li>
 *  <li>http://xml.org/sax/features/external-general-entities</li>
 *  <li>http://xml.org/sax/features/external-parameter-entities</li>
 *  <li>http://apache.org/xml/features/allow-java-encodings</li>
 *  <li>http://apache.org/xml/properties/internal/symbol-table</li>
 *  <li>http://apache.org/xml/properties/internal/error-reporter</li>
 *  <li>http://apache.org/xml/properties/internal/entity-resolver</li>
 * </ul>
 *
 *
 * @author Andy Clark, IBM
 * @author Arnaud  Le Hors, IBM
 * @author K.Venugopal SUN Microsystems
 * @author Neeraj Bajaj SUN Microsystems
 * @author Sunitha Reddy SUN Microsystems
 * @LastModified: May 2021
 */
public class XMLEntityManager implements XMLComponent, XMLEntityResolver {

    //
    // Constants
    //

    /** Default buffer size (2048). */
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    /** Default buffer size before we've finished with the XMLDecl:  */
    public static final int DEFAULT_XMLDECL_BUFFER_SIZE = 64;

    /** Default internal entity buffer size (1024). */
    public static final int DEFAULT_INTERNAL_BUFFER_SIZE = 1024;

    // feature identifiers

    /** Feature identifier: validation. */
    protected static final String VALIDATION =
            Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE;

    /**
     * standard uri conformant (strict uri).
     * http://apache.org/xml/features/standard-uri-conformant
     */
    protected boolean fStrictURI;


    /** Feature identifier: external general entities. */
    protected static final String EXTERNAL_GENERAL_ENTITIES =
            Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_GENERAL_ENTITIES_FEATURE;

    /** Feature identifier: external parameter entities. */
    protected static final String EXTERNAL_PARAMETER_ENTITIES =
            Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_PARAMETER_ENTITIES_FEATURE;

    /** Feature identifier: allow Java encodings. */
    protected static final String ALLOW_JAVA_ENCODINGS =
            Constants.XERCES_FEATURE_PREFIX + Constants.ALLOW_JAVA_ENCODINGS_FEATURE;

    /** Feature identifier: warn on duplicate EntityDef */
    protected static final String WARN_ON_DUPLICATE_ENTITYDEF =
            Constants.XERCES_FEATURE_PREFIX +Constants.WARN_ON_DUPLICATE_ENTITYDEF_FEATURE;

    /** Feature identifier: load external DTD. */
    protected static final String LOAD_EXTERNAL_DTD =
            Constants.XERCES_FEATURE_PREFIX + Constants.LOAD_EXTERNAL_DTD_FEATURE;

    // property identifiers

    /** Property identifier: symbol table. */
    protected static final String SYMBOL_TABLE =
            Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;

    /** Property identifier: error reporter. */
    protected static final String ERROR_REPORTER =
            Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY;

    /** Feature identifier: standard uri conformant */
    protected static final String STANDARD_URI_CONFORMANT =
            Constants.XERCES_FEATURE_PREFIX +Constants.STANDARD_URI_CONFORMANT_FEATURE;

    /** Property identifier: entity resolver. */
    protected static final String ENTITY_RESOLVER =
            Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY;

    protected static final String STAX_ENTITY_RESOLVER =
            Constants.XERCES_PROPERTY_PREFIX + Constants.STAX_ENTITY_RESOLVER_PROPERTY;

    // property identifier:  ValidationManager
    protected static final String VALIDATION_MANAGER =
            Constants.XERCES_PROPERTY_PREFIX + Constants.VALIDATION_MANAGER_PROPERTY;

    /** property identifier: buffer size. */
    protected static final String BUFFER_SIZE =
            Constants.XERCES_PROPERTY_PREFIX + Constants.BUFFER_SIZE_PROPERTY;

    /** property identifier: security manager. */
    protected static final String SECURITY_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SECURITY_MANAGER_PROPERTY;

    protected static final String PARSER_SETTINGS =
        Constants.XERCES_FEATURE_PREFIX + Constants.PARSER_SETTINGS;

    /** Property identifier: Security property manager. */
    private static final String XML_SECURITY_PROPERTY_MANAGER =
            JdkConstants.XML_SECURITY_PROPERTY_MANAGER;

    /** access external dtd: file protocol */
    static final String EXTERNAL_ACCESS_DEFAULT = JdkConstants.EXTERNAL_ACCESS_DEFAULT;

    // recognized features and properties

    /** Recognized features. */
    private static final String[] RECOGNIZED_FEATURES = {
                VALIDATION,
                EXTERNAL_GENERAL_ENTITIES,
                EXTERNAL_PARAMETER_ENTITIES,
                ALLOW_JAVA_ENCODINGS,
                WARN_ON_DUPLICATE_ENTITYDEF,
                STANDARD_URI_CONFORMANT,
                XMLConstants.USE_CATALOG
    };

    /** Feature defaults. */
    private static final Boolean[] FEATURE_DEFAULTS = {
                null,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                JdkXmlUtils.USE_CATALOG_DEFAULT
    };

    /** Recognized properties. */
    private static final String[] RECOGNIZED_PROPERTIES = {
                SYMBOL_TABLE,
                ERROR_REPORTER,
                ENTITY_RESOLVER,
                VALIDATION_MANAGER,
                BUFFER_SIZE,
                SECURITY_MANAGER,
                XML_SECURITY_PROPERTY_MANAGER,
                JdkXmlUtils.CATALOG_DEFER,
                JdkXmlUtils.CATALOG_FILES,
                JdkXmlUtils.CATALOG_PREFER,
                JdkXmlUtils.CATALOG_RESOLVE,
                JdkConstants.CDATA_CHUNK_SIZE
    };

    /** Property defaults. */
    private static final Object[] PROPERTY_DEFAULTS = {
                null,
                null,
                null,
                null,
                DEFAULT_BUFFER_SIZE,
                null,
                null,
                null,
                null,
                null,
                null,
                JdkConstants.CDATA_CHUNK_SIZE_DEFAULT
    };

    private static final String XMLEntity = "[xml]".intern();
    private static final String DTDEntity = "[dtd]".intern();

    // debugging

    /**
     * Debug printing of buffer. This debugging flag works best when you
     * resize the DEFAULT_BUFFER_SIZE down to something reasonable like
     * 64 characters.
     */
    private static final boolean DEBUG_BUFFER = false;

    /** warn on duplicate Entity declaration.
     *  http://apache.org/xml/features/warn-on-duplicate-entitydef
     */
    protected boolean fWarnDuplicateEntityDef;

    /** Debug some basic entities. */
    private static final boolean DEBUG_ENTITIES = false;

    /** Debug switching readers for encodings. */
    private static final boolean DEBUG_ENCODINGS = false;

    // should be diplayed trace resolving messages
    private static final boolean DEBUG_RESOLVER = false ;

    //
    // Data
    //

    // features

    /**
     * Validation. This feature identifier is:
     * http://xml.org/sax/features/validation
     */
    protected boolean fValidation;

    /**
     * External general entities. This feature identifier is:
     * http://xml.org/sax/features/external-general-entities
     */
    protected boolean fExternalGeneralEntities;

    /**
     * External parameter entities. This feature identifier is:
     * http://xml.org/sax/features/external-parameter-entities
     */
    protected boolean fExternalParameterEntities;

    /**
     * Allow Java encoding names. This feature identifier is:
     * http://apache.org/xml/features/allow-java-encodings
     */
    protected boolean fAllowJavaEncodings = true ;

    /** Load external DTD. */
    protected boolean fLoadExternalDTD = true;

    // properties

    /**
     * Symbol table. This property identifier is:
     * http://apache.org/xml/properties/internal/symbol-table
     */
    protected SymbolTable fSymbolTable;

    /**
     * Error reporter. This property identifier is:
     * http://apache.org/xml/properties/internal/error-reporter
     */
    protected XMLErrorReporter fErrorReporter;

    /**
     * Entity resolver. This property identifier is:
     * http://apache.org/xml/properties/internal/entity-resolver
     */
    protected XMLEntityResolver fEntityResolver;

    /** Stax Entity Resolver. This property identifier is XMLInputFactory.ENTITY_RESOLVER */

    protected StaxEntityResolverWrapper fStaxEntityResolver;

    /** Property Manager. This is used from Stax */
    protected PropertyManager fPropertyManager ;

    /** StAX properties */
    boolean fSupportDTD = true;
    boolean fReplaceEntityReferences = true;
    boolean fSupportExternalEntities = true;

    /** used to restrict external access */
    protected String fAccessExternalDTD = EXTERNAL_ACCESS_DEFAULT;

    // settings

    /**
     * Validation manager. This property identifier is:
     * http://apache.org/xml/properties/internal/validation-manager
     */
    protected ValidationManager fValidationManager;

    // settings

    /**
     * Buffer size. We get this value from a property. The default size
     * is used if the input buffer size property is not specified.
     * REVISIT: do we need a property for internal entity buffer size?
     */
    protected int fBufferSize = DEFAULT_BUFFER_SIZE;

    /** Security Manager */
    protected XMLSecurityManager fSecurityManager = null;

    protected XMLLimitAnalyzer fLimitAnalyzer = null;

    protected int entityExpansionIndex;

    /**
     * True if the document entity is standalone. This should really
     * only be set by the document source (e.g. XMLDocumentScanner).
     */
    protected boolean fStandalone;

    // are the entities being parsed in the external subset?
    // NOTE:  this *is not* the same as whether they're external entities!
    protected boolean fInExternalSubset = false;


    // handlers
    /** Entity handler. */
    protected XMLEntityHandler fEntityHandler;

    /** Current entity scanner */
    protected XMLEntityScanner fEntityScanner ;

    /** XML 1.0 entity scanner. */
    protected XMLEntityScanner fXML10EntityScanner;

    /** XML 1.1 entity scanner. */
    protected XMLEntityScanner fXML11EntityScanner;

    /** count of entities expanded: */
    protected int fEntityExpansionCount = 0;

    // entities

    /** Entities. */
    protected Map<String, Entity> fEntities = new HashMap<>();

    /** Entity stack. */
    protected Stack<Entity> fEntityStack = new Stack<>();

    /** Current entity. */
    protected Entity.ScannedEntity fCurrentEntity = null;

    /** identify if the InputSource is created by a resolver */
    boolean fISCreatedByResolver = false;

    // shared context

    protected XMLEntityStorage fEntityStorage ;

    protected final Object [] defaultEncoding = new Object[]{"UTF-8", null};


    // temp vars

    /** Resource identifer. */
    private final XMLResourceIdentifierImpl fResourceIdentifier = new XMLResourceIdentifierImpl();

    /** Augmentations for entities. */
    private final Augmentations fEntityAugs = new AugmentationsImpl();

    /** indicate whether Catalog should be used for resolving external resources */
    private boolean fUseCatalog = true;
    CatalogFeatures fCatalogFeatures;
    CatalogResolver fCatalogResolver;

    private String fCatalogFile;
    private String fDefer;
    private String fPrefer;
    private String fResolve;

    //
    // Constructors
    //

    /**
     * If this constructor is used to create the object, reset() should be invoked on this object
     */
    public XMLEntityManager() {
        //for entity managers not created by parsers
        fSecurityManager = new XMLSecurityManager(true);
        fEntityStorage = new XMLEntityStorage(this) ;
        setScannerVersion(Constants.XML_VERSION_1_0);
    } // <init>()

    /** Default constructor. */
    public XMLEntityManager(PropertyManager propertyManager) {
        fPropertyManager = propertyManager ;
        //pass a reference to current entity being scanned
        //fEntityStorage = new XMLEntityStorage(fCurrentEntity) ;
        fEntityStorage = new XMLEntityStorage(this) ;
        fEntityScanner = new XMLEntityScanner(propertyManager, this) ;
        reset(propertyManager);
    } // <init>()

    /**
     * Adds an internal entity declaration.
     * <p>
     * <strong>Note:</strong> This method ignores subsequent entity
     * declarations.
     * <p>
     * <strong>Note:</strong> The name should be a unique symbol. The
     * SymbolTable can be used for this purpose.
     *
     * @param name The name of the entity.
     * @param text The text of the entity.
     *
     * @see SymbolTable
     */
    public void addInternalEntity(String name, String text) {
        if (!fEntities.containsKey(name)) {
            Entity entity = new Entity.InternalEntity(name, text, fInExternalSubset);
            fEntities.put(name, entity);
        } else{
            if(fWarnDuplicateEntityDef){
                fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                        "MSG_DUPLICATE_ENTITY_DEFINITION",
                        new Object[]{ name },
                        XMLErrorReporter.SEVERITY_WARNING );
            }
        }

    } // addInternalEntity(String,String)

    /**
     * Adds an external entity declaration.
     * <p>
     * <strong>Note:</strong> This method ignores subsequent entity
     * declarations.
     * <p>
     * <strong>Note:</strong> The name should be a unique symbol. The
     * SymbolTable can be used for this purpose.
     *
     * @param name         The name of the entity.
     * @param publicId     The public identifier of the entity.
     * @param literalSystemId     The system identifier of the entity.
     * @param baseSystemId The base system identifier of the entity.
     *                     This is the system identifier of the entity
     *                     where <em>the entity being added</em> and
     *                     is used to expand the system identifier when
     *                     the system identifier is a relative URI.
     *                     When null the system identifier of the first
     *                     external entity on the stack is used instead.
     *
     * @see SymbolTable
     */
    public void addExternalEntity(String name,
            String publicId, String literalSystemId,
            String baseSystemId) throws IOException {
        if (!fEntities.containsKey(name)) {
            if (baseSystemId == null) {
                // search for the first external entity on the stack
                int size = fEntityStack.size();
                if (size == 0 && fCurrentEntity != null && fCurrentEntity.entityLocation != null) {
                    baseSystemId = fCurrentEntity.entityLocation.getExpandedSystemId();
                }
                for (int i = size - 1; i >= 0 ; i--) {
                    Entity.ScannedEntity externalEntity =
                            (Entity.ScannedEntity)fEntityStack.get(i);
                    if (externalEntity.entityLocation != null && externalEntity.entityLocation.getExpandedSystemId() != null) {
                        baseSystemId = externalEntity.entityLocation.getExpandedSystemId();
                        break;
                    }
                }
            }
            Entity entity = new Entity.ExternalEntity(name,
                    new XMLEntityDescriptionImpl(name, publicId, literalSystemId, baseSystemId,
                    expandSystemId(literalSystemId, baseSystemId, false)), null, fInExternalSubset);
            fEntities.put(name, entity);
        } else{
            if(fWarnDuplicateEntityDef){
                fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                        "MSG_DUPLICATE_ENTITY_DEFINITION",
                        new Object[]{ name },
                        XMLErrorReporter.SEVERITY_WARNING );
            }
        }

    } // addExternalEntity(String,String,String,String)


    /**
     * Adds an unparsed entity declaration.
     * <p>
     * <strong>Note:</strong> This method ignores subsequent entity
     * declarations.
     * <p>
     * <strong>Note:</strong> The name should be a unique symbol. The
     * SymbolTable can be used for this purpose.
     *
     * @param name     The name of the entity.
     * @param publicId The public identifier of the entity.
     * @param systemId The system identifier of the entity.
     * @param notation The name of the notation.
     *
     * @see SymbolTable
     */
    public void addUnparsedEntity(String name,
            String publicId, String systemId,
            String baseSystemId, String notation) {
        if (!fEntities.containsKey(name)) {
            Entity.ExternalEntity entity = new Entity.ExternalEntity(name,
                    new XMLEntityDescriptionImpl(name, publicId, systemId, baseSystemId, null),
                    notation, fInExternalSubset);
            fEntities.put(name, entity);
        } else{
            if(fWarnDuplicateEntityDef){
                fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,
                        "MSG_DUPLICATE_ENTITY_DEFINITION",
                        new Object[]{ name },
                        XMLErrorReporter.SEVERITY_WARNING );
            }
        }
    } // addUnparsedEntity(String,String,String,String)


    /** get the entity storage object from entity manager */
    public XMLEntityStorage getEntityStore(){
        return fEntityStorage ;
    }

    /** return the entity responsible for reading the entity */
    public XMLEntityScanner getEntityScanner(){
        if(fEntityScanner == null) {
            // default to 1.0
            if(fXML10EntityScanner == null) {
                fXML10EntityScanner = new XMLEntityScanner();
            }
            fXML10EntityScanner.reset(fSymbolTable, this, fErrorReporter);
            fEntityScanner = fXML10EntityScanner;
        }
        return fEntityScanner;

    }

    public void setScannerVersion(short version) {

        if(version == Constants.XML_VERSION_1_0) {
            if(fXML10EntityScanner == null) {
                fXML10EntityScanner = new XMLEntityScanner();
            }
            fXML10EntityScanner.reset(fSymbolTable, this, fErrorReporter);
            fEntityScanner = fXML10EntityScanner;
            fEntityScanner.setCurrentEntity(fCurrentEntity);
        } else {
            if(fXML11EntityScanner == null) {
                fXML11EntityScanner = new XML11EntityScanner();
            }
            fXML11EntityScanner.reset(fSymbolTable, this, fErrorReporter);
            fEntityScanner = fXML11EntityScanner;
            fEntityScanner.setCurrentEntity(fCurrentEntity);
        }

    }

    /**
     * This method uses the passed-in XMLInputSource to make
     * fCurrentEntity usable for reading.
     *
     * @param reference flag to indicate whether the entity is an Entity Reference.
     * @param name  name of the entity (XML is it's the document entity)
     * @param xmlInputSource    the input source, with sufficient information
     *      to begin scanning characters.
     * @param literal        True if this entity is started within a
     *                       literal value.
     * @param isExternal    whether this entity should be treated as an internal or external entity.
     * @throws IOException  if anything can't be read
     *  XNIException    If any parser-specific goes wrong.
     * @return the encoding of the new entity or null if a character stream was employed
     */
    public String setupCurrentEntity(boolean reference, String name, XMLInputSource xmlInputSource,
            boolean literal, boolean isExternal)
            throws IOException, XNIException {
        // get information

        final String publicId = xmlInputSource.getPublicId();
        String literalSystemId = xmlInputSource.getSystemId();
        String baseSystemId = xmlInputSource.getBaseSystemId();
        String encoding = xmlInputSource.getEncoding();
        final boolean encodingExternallySpecified = (encoding != null);
        Boolean isBigEndian = null;

        // create reader
        InputStream stream = null;
        Reader reader = xmlInputSource.getCharacterStream();

        // First chance checking strict URI
        String expandedSystemId = expandSystemId(literalSystemId, baseSystemId, fStrictURI);
        if (baseSystemId == null) {
            baseSystemId = expandedSystemId;
        }
        if (reader == null) {
            stream = xmlInputSource.getByteStream();
            if (stream == null) {
                URL location = new URL(expandedSystemId);
                URLConnection connect = location.openConnection();
                if (!(connect instanceof HttpURLConnection)) {
                    stream = connect.getInputStream();
                }
                else {
                    boolean followRedirects = true;

                    // setup URLConnection if we have an HTTPInputSource
                    if (xmlInputSource instanceof HTTPInputSource) {
                        final HttpURLConnection urlConnection = (HttpURLConnection) connect;
                        final HTTPInputSource httpInputSource = (HTTPInputSource) xmlInputSource;

                        // set request properties
                        Iterator<Map.Entry<String, String>> propIter = httpInputSource.getHTTPRequestProperties();
                        while (propIter.hasNext()) {
                            Map.Entry<String, String> entry = propIter.next();
                            urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
                        }

                        // set preference for redirection
                        followRedirects = httpInputSource.getFollowHTTPRedirects();
                        if (!followRedirects) {
                            urlConnection.setInstanceFollowRedirects(followRedirects);
                        }
                    }

                    stream = connect.getInputStream();

                    // REVISIT: If the URLConnection has external encoding
                    // information, we should be reading it here. It's located
                    // in the charset parameter of Content-Type. -- mrglavas

                    if (followRedirects) {
                        String redirect = connect.getURL().toString();
                        // E43: Check if the URL was redirected, and then
                        // update literal and expanded system IDs if needed.
                        if (!redirect.equals(expandedSystemId)) {
                            literalSystemId = redirect;
                            expandedSystemId = redirect;
                        }
                    }
                }
            }

            // wrap this stream in RewindableInputStream
            RewindableInputStream rewindableStream = new RewindableInputStream(stream);
            stream = rewindableStream;

            // perform auto-detect of encoding if necessary
            if (encoding == null) {
                // read first four bytes and determine encoding
                final byte[] b4 = new byte[4];
                int count = 0;
                for (; count<4; count++ ) {
                    b4[count] = (byte)rewindableStream.readAndBuffer();
                }
                if (count == 4) {
                    final EncodingInfo info = getEncodingInfo(b4, count);
                    encoding = info.autoDetectedEncoding;
                    final String readerEncoding = info.readerEncoding;
                    isBigEndian = info.isBigEndian;
                    stream.reset();
                    if (info.hasBOM) {
                        // Special case UTF-8 files with BOM created by Microsoft
                        // tools. It's more efficient to consume the BOM than make
                        // the reader perform extra checks. -Ac
                        if (EncodingInfo.STR_UTF8.equals(readerEncoding)) {
                            // UTF-8 BOM: 0xEF 0xBB 0xBF
                            stream.skip(3);
                        }
                        // It's also more efficient to consume the UTF-16 BOM.
                        else if (EncodingInfo.STR_UTF16.equals(readerEncoding)) {
                            // UTF-16 BE BOM: 0xFE 0xFF
                            // UTF-16 LE BOM: 0xFF 0xFE
                            stream.skip(2);
                        }
                    }
                    reader = createReader(stream, readerEncoding, isBigEndian);
                } else {
                    reader = createReader(stream, encoding, isBigEndian);
                }
            }

            // use specified encoding
            else {
                encoding = encoding.toUpperCase(Locale.ENGLISH);

                // If encoding is UTF-8, consume BOM if one is present.
                if (EncodingInfo.STR_UTF8.equals(encoding)) {
                    final int[] b3 = new int[3];
                    int count = 0;
                    for (; count < 3; ++count) {
                        b3[count] = rewindableStream.readAndBuffer();
                        if (b3[count] == -1)
                            break;
                    }
                    if (count == 3) {
                        if (b3[0] != 0xEF || b3[1] != 0xBB || b3[2] != 0xBF) {
                            // First three bytes are not BOM, so reset.
                            stream.reset();
                        }
                    } else {
                        stream.reset();
                    }
                }
                // If encoding is UTF-16, we still need to read the first
                // four bytes, in order to discover the byte order.
                else if (EncodingInfo.STR_UTF16.equals(encoding)) {
                    final int[] b4 = new int[4];
                    int count = 0;
                    for (; count < 4; ++count) {
                        b4[count] = rewindableStream.readAndBuffer();
                        if (b4[count] == -1)
                            break;
                    }
                    stream.reset();
                    if (count >= 2) {
                        final int b0 = b4[0];
                        final int b1 = b4[1];
                        if (b0 == 0xFE && b1 == 0xFF) {
                            // UTF-16, big-endian
                            isBigEndian = Boolean.TRUE;
                            stream.skip(2);
                        }
                        else if (b0 == 0xFF && b1 == 0xFE) {
                            // UTF-16, little-endian
                            isBigEndian = Boolean.FALSE;
                            stream.skip(2);
                        }
                        else if (count == 4) {
                            final int b2 = b4[2];
                            final int b3 = b4[3];
                            if (b0 == 0x00 && b1 == 0x3C && b2 == 0x00 && b3 == 0x3F) {
                                // UTF-16, big-endian, no BOM
                                isBigEndian = Boolean.TRUE;
                            }
                            if (b0 == 0x3C && b1 == 0x00 && b2 == 0x3F && b3 == 0x00) {
                                // UTF-16, little-endian, no BOM
                                isBigEndian = Boolean.FALSE;
                            }
                        }
                    }
                }
                // If encoding is UCS-4, we still need to read the first four bytes
                // in order to discover the byte order.
                else if (EncodingInfo.STR_UCS4.equals(encoding)) {
                    final int[] b4 = new int[4];
                    int count = 0;
                    for (; count < 4; ++count) {
                        b4[count] = rewindableStream.readAndBuffer();
                        if (b4[count] == -1)
                            break;
                    }
                    stream.reset();

                    // Ignore unusual octet order for now.
                    if (count == 4) {
                        // UCS-4, big endian (1234)
                        if (b4[0] == 0x00 && b4[1] == 0x00 && b4[2] == 0x00 && b4[3] == 0x3C) {
                            isBigEndian = Boolean.TRUE;
                        }
                        // UCS-4, little endian (1234)
                        else if (b4[0] == 0x3C && b4[1] == 0x00 && b4[2] == 0x00 && b4[3] == 0x00) {
                            isBigEndian = Boolean.FALSE;
                        }
                    }
                }
                // If encoding is UCS-2, we still need to read the first four bytes
                // in order to discover the byte order.
                else if (EncodingInfo.STR_UCS2.equals(encoding)) {
                    final int[] b4 = new int[4];
                    int count = 0;
                    for (; count < 4; ++count) {
                        b4[count] = rewindableStream.readAndBuffer();
                        if (b4[count] == -1)
                            break;
                    }
                    stream.reset();

                    if (count == 4) {
                        // UCS-2, big endian
                        if (b4[0] == 0x00 && b4[1] == 0x3C && b4[2] == 0x00 && b4[3] == 0x3F) {
                            isBigEndian = Boolean.TRUE;
                        }
                        // UCS-2, little endian
                        else if (b4[0] == 0x3C && b4[1] == 0x00 && b4[2] == 0x3F && b4[3] == 0x00) {
                            isBigEndian = Boolean.FALSE;
                        }
                    }
                }

                reader = createReader(stream, encoding, isBigEndian);
            }

            // read one character at a time so we don't jump too far
            // ahead, converting characters from the byte stream in
            // the wrong encoding
            if (DEBUG_ENCODINGS) {
                System.out.println("$$$ no longer wrapping reader in OneCharReader");
            }
            //reader = new OneCharReader(reader);
        }

        // We've seen a new Reader.
        // Push it on the stack so we can close it later.
        fReaderStack.push(reader);

        // push entity on stack
        if (fCurrentEntity != null) {
            fEntityStack.push(fCurrentEntity);
        }

        // create entity
        /* if encoding is specified externally, 'encoding' information present
         * in the prolog of the XML document is not considered. Hence, prolog can
         * be read in Chunks of data instead of byte by byte.
         */
        fCurrentEntity = new Entity.ScannedEntity(reference, name,
                new XMLResourceIdentifierImpl(publicId, literalSystemId, baseSystemId, expandedSystemId),
                stream, reader, encoding, literal, encodingExternallySpecified, isExternal);
        fCurrentEntity.setEncodingExternallySpecified(encodingExternallySpecified);
        fEntityScanner.setCurrentEntity(fCurrentEntity);
        fResourceIdentifier.setValues(publicId, literalSystemId, baseSystemId, expandedSystemId);
        if (fLimitAnalyzer != null) {
            fLimitAnalyzer.startEntity(name);
        }
        return encoding;
    } //setupCurrentEntity(String, XMLInputSource, boolean, boolean):  String


    /**
     * Checks whether an entity given by name is external.
     *
     * @param entityName The name of the entity to check.
     * @return True if the entity is external, false otherwise
     * (including when the entity is not declared).
     */
    public boolean isExternalEntity(String entityName) {

        Entity entity = fEntities.get(entityName);
        if (entity == null) {
            return false;
        }
        return entity.isExternal();
    }

    /**
     * Checks whether the declaration of an entity given by name is
     * // in the external subset.
     *
     * @param entityName The name of the entity to check.
     * @return True if the entity was declared in the external subset, false otherwise
     *           (including when the entity is not declared).
     */
    public boolean isEntityDeclInExternalSubset(String entityName) {

        Entity entity = fEntities.get(entityName);
        if (entity == null) {
            return false;
        }
        return entity.isEntityDeclInExternalSubset();
    }



    //
    // Public methods
    //

    /**
     * Sets whether the document entity is standalone.
     *
     * @param standalone True if document entity is standalone.
     */
    public void setStandalone(boolean standalone) {
        fStandalone = standalone;
    }
    // setStandalone(boolean)

    /** Returns true if the document entity is standalone. */
    public boolean isStandalone() {
        return fStandalone;
    }  //isStandalone():boolean

    public boolean isDeclaredEntity(String entityName) {

        Entity entity = fEntities.get(entityName);
        return entity != null;
    }

    public boolean isUnparsedEntity(String entityName) {

        Entity entity = fEntities.get(entityName);
        if (entity == null) {
            return false;
        }
        return entity.isUnparsed();
    }



    // this simply returns the fResourceIdentifier object;
    // this should only be used with caution by callers that
    // carefully manage the entity manager's behaviour, so that
    // this doesn't returning meaningless or misleading data.
    // @return  a reference to the current fResourceIdentifier object
    public XMLResourceIdentifier getCurrentResourceIdentifier() {
        return fResourceIdentifier;
    }

    /**
     * Sets the entity handler. When an entity starts and ends, the
     * entity handler is notified of the change.
     *
     * @param entityHandler The new entity handler.
     */

    public void setEntityHandler(com.sun.org.apache.xerces.internal.impl.XMLEntityHandler entityHandler) {
        fEntityHandler = entityHandler;
    } // setEntityHandler(XMLEntityHandler)

    //this function returns StaxXMLInputSource
    public StaxXMLInputSource resolveEntityAsPerStax(XMLResourceIdentifier resourceIdentifier) throws java.io.IOException{

        if(resourceIdentifier == null ) return null;

        String publicId = resourceIdentifier.getPublicId();
        String literalSystemId = resourceIdentifier.getLiteralSystemId();
        String baseSystemId = resourceIdentifier.getBaseSystemId();
        String expandedSystemId = resourceIdentifier.getExpandedSystemId();
        // if no base systemId given, assume that it's relative
        // to the systemId of the current scanned entity
        // Sometimes the system id is not (properly) expanded.
        // We need to expand the system id if:
        // a. the expanded one was null; or
        // b. the base system id was null, but becomes non-null from the current entity.
        boolean needExpand = (expandedSystemId == null);
        // REVISIT:  why would the baseSystemId ever be null?  if we
        // didn't have to make this check we wouldn't have to reuse the
        // fXMLResourceIdentifier object...
        if (baseSystemId == null && fCurrentEntity != null && fCurrentEntity.entityLocation != null) {
            baseSystemId = fCurrentEntity.entityLocation.getExpandedSystemId();
            if (baseSystemId != null)
                needExpand = true;
        }
        if (needExpand)
            expandedSystemId = expandSystemId(literalSystemId, baseSystemId,false);

        // give the entity resolver a chance
        StaxXMLInputSource staxInputSource = null;
        XMLInputSource xmlInputSource = null;

        XMLResourceIdentifierImpl ri = null;

        if (resourceIdentifier instanceof XMLResourceIdentifierImpl) {
            ri = (XMLResourceIdentifierImpl)resourceIdentifier;
        } else {
            fResourceIdentifier.clear();
            ri = fResourceIdentifier;
        }
        ri.setValues(publicId, literalSystemId, baseSystemId, expandedSystemId);
        if(DEBUG_RESOLVER){
            System.out.println("BEFORE Calling resolveEntity") ;
        }

        fISCreatedByResolver = false;
        //either of Stax or Xerces would be null
        if(fStaxEntityResolver != null){
            staxInputSource = fStaxEntityResolver.resolveEntity(ri);
            if(staxInputSource != null) {
                fISCreatedByResolver = true;
            }
        }

        if(fEntityResolver != null){
            xmlInputSource = fEntityResolver.resolveEntity(ri);
            if(xmlInputSource != null) {
                fISCreatedByResolver = true;
            }
        }

        if(xmlInputSource != null){
            //wrap this XMLInputSource to StaxInputSource
            staxInputSource = new StaxXMLInputSource(xmlInputSource, fISCreatedByResolver);
        }

        if (staxInputSource == null && fUseCatalog) {
            if (fCatalogFeatures == null) {
                fCatalogFeatures = JdkXmlUtils.getCatalogFeatures(fDefer, fCatalogFile, fPrefer, fResolve);
            }
            fCatalogFile = fCatalogFeatures.get(Feature.FILES);
            if (fCatalogFile != null) {
                try {
                    if (fCatalogResolver == null) {
                        fCatalogResolver = CatalogManager.catalogResolver(fCatalogFeatures);
                    }
                    InputSource is = fCatalogResolver.resolveEntity(publicId, literalSystemId);
                    if (is != null && !is.isEmpty()) {
                        staxInputSource = new StaxXMLInputSource(new XMLInputSource(is, true), true);
                    }
                } catch (CatalogException e) {
                    fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,"CatalogException",
                    new Object[]{SecuritySupport.sanitizePath(fCatalogFile)},
                    XMLErrorReporter.SEVERITY_FATAL_ERROR, e );
                }
            }
        }

        // do default resolution
        //this works for both stax & Xerces, if staxInputSource is null,
        //it means parser need to revert to default resolution
        if (staxInputSource == null) {
            // REVISIT: when systemId is null, I think we should return null.
            //          is this the right solution? -SG
            //if (systemId != null)
            staxInputSource = new StaxXMLInputSource(
                    new XMLInputSource(publicId, literalSystemId, baseSystemId, true), false);
        }else if(staxInputSource.hasXMLStreamOrXMLEventReader()){
            //Waiting for the clarification from EG. - nb
        }

        if (DEBUG_RESOLVER) {
            System.err.println("XMLEntityManager.resolveEntity(" + publicId + ")");
            System.err.println(" = " + xmlInputSource);
        }

        return staxInputSource;

    }

    /**
     * Resolves the specified public and system identifiers. This
     * method first attempts to resolve the entity based on the
     * EntityResolver registered by the application. If no entity
     * resolver is registered or if the registered entity handler
     * is unable to resolve the entity, then default entity
     * resolution will occur.
     *
     * @param publicId     The public identifier of the entity.
     * @param systemId     The system identifier of the entity.
     * @param baseSystemId The base system identifier of the entity.
     *                     This is the system identifier of the current
     *                     entity and is used to expand the system
     *                     identifier when the system identifier is a
     *                     relative URI.
     *
     * @return Returns an input source that wraps the resolved entity.
     *         This method will never return null.
     *
     * @throws IOException  Thrown on i/o error.
     * @throws XNIException Thrown by entity resolver to signal an error.
     */
    public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier) throws IOException, XNIException {
        if(resourceIdentifier == null ) return null;
        String publicId = resourceIdentifier.getPublicId();
        String literalSystemId = resourceIdentifier.getLiteralSystemId();
        String baseSystemId = resourceIdentifier.getBaseSystemId();
        String expandedSystemId = resourceIdentifier.getExpandedSystemId();

        // if no base systemId given, assume that it's relative
        // to the systemId of the current scanned entity
        // Sometimes the system id is not (properly) expanded.
        // We need to expand the system id if:
        // a. the expanded one was null; or
        // b. the base system id was null, but becomes non-null from the current entity.
        boolean needExpand = (expandedSystemId == null);
        // REVISIT:  why would the baseSystemId ever be null?  if we
        // didn't have to make this check we wouldn't have to reuse the
        // fXMLResourceIdentifier object...
        if (baseSystemId == null && fCurrentEntity != null && fCurrentEntity.entityLocation != null) {
            baseSystemId = fCurrentEntity.entityLocation.getExpandedSystemId();
            if (baseSystemId != null)
                needExpand = true;
        }
        if (needExpand)
            expandedSystemId = expandSystemId(literalSystemId, baseSystemId,false);

        // give the entity resolver a chance
        XMLInputSource xmlInputSource = null;

        if (fEntityResolver != null) {
            resourceIdentifier.setBaseSystemId(baseSystemId);
            resourceIdentifier.setExpandedSystemId(expandedSystemId);
            xmlInputSource = fEntityResolver.resolveEntity(resourceIdentifier);
        }

        if (xmlInputSource == null && fUseCatalog) {
            if (fCatalogFeatures == null) {
                fCatalogFeatures = JdkXmlUtils.getCatalogFeatures(fDefer, fCatalogFile, fPrefer, fResolve);
            }
            fCatalogFile = fCatalogFeatures.get(Feature.FILES);
            if (fCatalogFile != null) {
                /*
                 since the method can be called from various processors, both
                 EntityResolver and URIResolver are used to attempt to find
                 a match
                */
                InputSource is = null;
                try {
                    if (fCatalogResolver == null) {
                        fCatalogResolver = CatalogManager.catalogResolver(fCatalogFeatures);
                    }
                    String pid = (publicId != null? publicId : resourceIdentifier.getNamespace());
                    if (pid != null || literalSystemId != null) {
                        is = fCatalogResolver.resolveEntity(pid, literalSystemId);
                    }
                } catch (CatalogException e) {}

                if (is != null && !is.isEmpty()) {
                    xmlInputSource = new XMLInputSource(is, true);
                } else if (literalSystemId != null) {
                    if (fCatalogResolver == null) {
                        fCatalogResolver = CatalogManager.catalogResolver(fCatalogFeatures);
                    }

                    Source source = null;
                    try {
                        source = fCatalogResolver.resolve(literalSystemId, baseSystemId);
                    } catch (CatalogException e) {
                        throw new XNIException(e);
                    }
                    if (source != null && !source.isEmpty()) {
                        xmlInputSource = new XMLInputSource(publicId, source.getSystemId(), baseSystemId, true);
                    }
                }
            }
        }

        // do default resolution
        // REVISIT: what's the correct behavior if the user provided an entity
        // resolver (fEntityResolver != null), but resolveEntity doesn't return
        // an input source (xmlInputSource == null)?
        // do we do default resolution, or do we just return null? -SG
        if (xmlInputSource == null) {
            // REVISIT: when systemId is null, I think we should return null.
            //          is this the right solution? -SG
            //if (systemId != null)
            xmlInputSource = new XMLInputSource(publicId, literalSystemId, baseSystemId, false);
        }

        if (DEBUG_RESOLVER) {
            System.err.println("XMLEntityManager.resolveEntity(" + publicId + ")");
            System.err.println(" = " + xmlInputSource);
        }

        return xmlInputSource;

    } // resolveEntity(XMLResourceIdentifier):XMLInputSource

    /**
     * Starts a named entity.
     *
     * @param isGE flag to indicate whether the entity is a General Entity
     * @param entityName The name of the entity to start.
     * @param literal    True if this entity is started within a literal
     *                   value.
     *
     * @throws IOException  Thrown on i/o error.
     * @throws XNIException Thrown by entity handler to signal an error.
     */
    public void startEntity(boolean isGE, String entityName, boolean literal)
    throws IOException, XNIException {

        // was entity declared?
        Entity entity = fEntityStorage.getEntity(entityName);
        if (entity == null) {
            if (fEntityHandler != null) {
                String encoding = null;
                fResourceIdentifier.clear();
                fEntityAugs.removeAllItems();
                fEntityAugs.putItem(Constants.ENTITY_SKIPPED, Boolean.TRUE);
                fEntityHandler.startEntity(entityName, fResourceIdentifier, encoding, fEntityAugs);
                fEntityAugs.removeAllItems();
                fEntityAugs.putItem(Constants.ENTITY_SKIPPED, Boolean.TRUE);
                fEntityHandler.endEntity(entityName, fEntityAugs);
            }
            return;
        }

        // should we skip external entities?
        boolean external = entity.isExternal();
        Entity.ExternalEntity externalEntity = null;
        String extLitSysId = null, extBaseSysId = null, expandedSystemId = null;
        if (external) {
            externalEntity = (Entity.ExternalEntity)entity;
            extLitSysId = (externalEntity.entityLocation != null ? externalEntity.entityLocation.getLiteralSystemId() : null);
            extBaseSysId = (externalEntity.entityLocation != null ? externalEntity.entityLocation.getBaseSystemId() : null);
            expandedSystemId = expandSystemId(extLitSysId, extBaseSysId);
            boolean unparsed = entity.isUnparsed();
            boolean parameter = entityName.startsWith("%");
            boolean general = !parameter;
            if (unparsed || (general && !fExternalGeneralEntities) ||
                    (parameter && !fExternalParameterEntities) ||
                    !fSupportDTD || !fSupportExternalEntities) {

                if (fEntityHandler != null) {
                    fResourceIdentifier.clear();
                    final String encoding = null;
                    fResourceIdentifier.setValues(
                            (externalEntity.entityLocation != null ? externalEntity.entityLocation.getPublicId() : null),
                            extLitSysId, extBaseSysId, expandedSystemId);
                    fEntityAugs.removeAllItems();
                    fEntityAugs.putItem(Constants.ENTITY_SKIPPED, Boolean.TRUE);
                    fEntityHandler.startEntity(entityName, fResourceIdentifier, encoding, fEntityAugs);
                    fEntityAugs.removeAllItems();
                    fEntityAugs.putItem(Constants.ENTITY_SKIPPED, Boolean.TRUE);
                    fEntityHandler.endEntity(entityName, fEntityAugs);
                }
                return;
            }
        }

        // is entity recursive?
        int size = fEntityStack.size();
        for (int i = size; i >= 0; i--) {
            Entity activeEntity = i == size
                    ? fCurrentEntity
                    : fEntityStack.get(i);
            if (activeEntity.name == entityName) {
                String path = entityName;
                for (int j = i + 1; j < size; j++) {
                    activeEntity = fEntityStack.get(j);
                    path = path + " -> " + activeEntity.name;
                }
                path = path + " -> " + fCurrentEntity.name;
                path = path + " -> " + entityName;
                fErrorReporter.reportError(this.getEntityScanner(),XMLMessageFormatter.XML_DOMAIN,
                        "RecursiveReference",
                        new Object[] { entityName, path },
                        XMLErrorReporter.SEVERITY_FATAL_ERROR);

                        if (fEntityHandler != null) {
                            fResourceIdentifier.clear();
                            final String encoding = null;
                            if (external) {
                                fResourceIdentifier.setValues(
                                        (externalEntity.entityLocation != null ? externalEntity.entityLocation.getPublicId() : null),
                                        extLitSysId, extBaseSysId, expandedSystemId);
                            }
                            fEntityAugs.removeAllItems();
                            fEntityAugs.putItem(Constants.ENTITY_SKIPPED, Boolean.TRUE);
                            fEntityHandler.startEntity(entityName, fResourceIdentifier, encoding, fEntityAugs);
                            fEntityAugs.removeAllItems();
                            fEntityAugs.putItem(Constants.ENTITY_SKIPPED, Boolean.TRUE);
                            fEntityHandler.endEntity(entityName, fEntityAugs);
                        }

                        return;
            }
        }

        // resolve external entity
        StaxXMLInputSource staxInputSource = null;
        XMLInputSource xmlInputSource = null ;

        if (external) {
            staxInputSource = resolveEntityAsPerStax(externalEntity.entityLocation);
            /** xxx:  Waiting from the EG
             * //simply return if there was entity resolver registered and application
             * //returns either XMLStreamReader or XMLEventReader.
             * if(staxInputSource.hasXMLStreamOrXMLEventReader()) return ;
             */
            xmlInputSource = staxInputSource.getXMLInputSource() ;
            if (!fISCreatedByResolver) {
                //let the not-LoadExternalDTD or not-SupportDTD process to handle the situation
                if (fLoadExternalDTD) {
                    String accessError = SecuritySupport.checkAccess(expandedSystemId, fAccessExternalDTD, JdkConstants.ACCESS_EXTERNAL_ALL);
                    if (accessError != null) {
                        fErrorReporter.reportError(this.getEntityScanner(),XMLMessageFormatter.XML_DOMAIN,
                                "AccessExternalEntity",
                                new Object[] { SecuritySupport.sanitizePath(expandedSystemId), accessError },
                                XMLErrorReporter.SEVERITY_FATAL_ERROR);
                    }
                }
            }
        }
        // wrap internal entity
        else {
            Entity.InternalEntity internalEntity = (Entity.InternalEntity)entity;
            Reader reader = new StringReader(internalEntity.text);
            xmlInputSource = new XMLInputSource(null, null, null, reader, null);
        }

        // start the entity
        startEntity(isGE, entityName, xmlInputSource, literal, external);

    } // startEntity(String,boolean)

    /**
     * Starts the document entity. The document entity has the "[xml]"
     * pseudo-name.
     *
     * @param xmlInputSource The input source of the document entity.
     *
     * @throws IOException  Thrown on i/o error.
     * @throws XNIException Thrown by entity handler to signal an error.
     */
    public void startDocumentEntity(XMLInputSource xmlInputSource)
    throws IOException, XNIException {
        startEntity(false, XMLEntity, xmlInputSource, false, true);
    } // startDocumentEntity(XMLInputSource)

    //xxx these methods are not required.
    /**
     * Starts the DTD entity. The DTD entity has the "[dtd]"
     * pseudo-name.
     *
     * @param xmlInputSource The input source of the DTD entity.
     *
     * @throws IOException  Thrown on i/o error.
     * @throws XNIException Thrown by entity handler to signal an error.
     */
    public void startDTDEntity(XMLInputSource xmlInputSource)
    throws IOException, XNIException {
        startEntity(false, DTDEntity, xmlInputSource, false, true);
    } // startDTDEntity(XMLInputSource)

    // indicate start of external subset so that
    // location of entity decls can be tracked
    public void startExternalSubset() {
        fInExternalSubset = true;
    }

    public void endExternalSubset() {
        fInExternalSubset = false;
    }

    /**
     * Starts an entity.
     * <p>
     * This method can be used to insert an application defined XML
     * entity stream into the parsing stream.
     *
     * @param isGE flag to indicate whether the entity is a General Entity
     * @param name           The name of the entity.
     * @param xmlInputSource The input source of the entity.
     * @param literal        True if this entity is started within a
     *                       literal value.
     * @param isExternal    whether this entity should be treated as an internal or external entity.
     *
     * @throws IOException  Thrown on i/o error.
     * @throws XNIException Thrown by entity handler to signal an error.
     */
    public void startEntity(boolean isGE, String name,
            XMLInputSource xmlInputSource,
            boolean literal, boolean isExternal)
            throws IOException, XNIException {

        String encoding = setupCurrentEntity(isGE, name, xmlInputSource, literal, isExternal);

        //when entity expansion limit is set by the Application, we need to
        //check for the entity expansion limit set by the parser, if number of entity
        //expansions exceeds the entity expansion limit, parser will throw fatal error.
        // Note that this represents the nesting level of open entities.
        fEntityExpansionCount++;
        if(fLimitAnalyzer != null) {
           fLimitAnalyzer.addValue(entityExpansionIndex, name, 1);
        }
        if( fSecurityManager != null && fSecurityManager.isOverLimit(entityExpansionIndex, fLimitAnalyzer)){
            fSecurityManager.debugPrint(fLimitAnalyzer);
            fErrorReporter.reportError(XMLMessageFormatter.XML_DOMAIN,"EntityExpansionLimit",
                    new Object[]{fSecurityManager.getLimitValueByIndex(entityExpansionIndex)},
                                             XMLErrorReporter.SEVERITY_FATAL_ERROR );
            // is there anything better to do than reset the counter?
            // at least one can envision debugging applications where this might
            // be useful...
            fEntityExpansionCount = 0;
        }

        // call handler
        if (fEntityHandler != null) {
            fEntityHandler.startEntity(name, fResourceIdentifier, encoding, null);
        }

    } // startEntity(String,XMLInputSource)

    /**
     * Return the current entity being scanned. Current entity is SET using startEntity function.
     * @return Entity.ScannedEntity
     */

    public Entity.ScannedEntity getCurrentEntity(){
        return fCurrentEntity ;
    }

    /**
     * Return the top level entity handled by this manager, or null
     * if no entity was added.
     */
    public Entity.ScannedEntity getTopLevelEntity() {
        return (Entity.ScannedEntity)
            (fEntityStack.empty() ? null : fEntityStack.get(0));
    }

    // A stack containing all the open readers
    protected Stack<Reader> fReaderStack = new Stack<>();

    /**
     * Close all opened InputStreams and Readers opened by this parser.
     */
    public void closeReaders() {
        // close all readers
        while (!fReaderStack.isEmpty()) {
            try {
                (fReaderStack.pop()).close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public void endEntity() throws IOException, XNIException {

        // call handler
        if (DEBUG_BUFFER) {
            System.out.print("(endEntity: ");
            print();
            System.out.println();
        }
        //pop the entity from the stack
        Entity.ScannedEntity entity = fEntityStack.size() > 0 ? (Entity.ScannedEntity)fEntityStack.pop() : null ;

        /** need to close the reader first since the program can end
         *  prematurely (e.g. fEntityHandler.endEntity may throw exception)
         *  leaving the reader open
         */
        //close the reader
        if(fCurrentEntity != null){
            //close the reader
            try{
                if (fLimitAnalyzer != null) {
                    fLimitAnalyzer.endEntity(XMLSecurityManager.Limit.GENERAL_ENTITY_SIZE_LIMIT, fCurrentEntity.name);
                    if (fCurrentEntity.name.equals("[xml]")) {
                        fSecurityManager.debugPrint(fLimitAnalyzer);
                    }
                }
                fCurrentEntity.close();
            }catch(IOException ex){
                throw new XNIException(ex);
            }
        }

        // REVISIT: We should never encounter underflow if the calls
        // to startEntity and endEntity are balanced, but guard
        // against the EmptyStackException for now. -- mrglavas
        if (!fReaderStack.isEmpty()) {
            fReaderStack.pop();
        }

        if (fEntityHandler != null) {
            //so this is the last opened entity, signal it to current fEntityHandler using Augmentation
            if(entity == null){
                fEntityAugs.removeAllItems();
                fEntityAugs.putItem(Constants.LAST_ENTITY, Boolean.TRUE);
                fEntityHandler.endEntity(fCurrentEntity.name, fEntityAugs);
                fEntityAugs.removeAllItems();
            }else{
                fEntityHandler.endEntity(fCurrentEntity.name, null);
            }
        }
        //check if it is a document entity
        boolean documentEntity = fCurrentEntity.name == XMLEntity;

        //set popped entity as current entity
        fCurrentEntity = entity;
        fEntityScanner.setCurrentEntity(fCurrentEntity);

        //check if there are any entity left in the stack -- if there are
        //no entries EOF has been reached.
        // throw exception when it is the last entity but it is not a document entity

        if(fCurrentEntity == null & !documentEntity){
            throw new EOFException() ;
        }

        if (DEBUG_BUFFER) {
            System.out.print(")endEntity: ");
            print();
            System.out.println();
        }

    } // endEntity()


    //
    // XMLComponent methods
    //
    public void reset(PropertyManager propertyManager){
        // xerces properties
        fSymbolTable = (SymbolTable)propertyManager.getProperty(Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY);
        fErrorReporter = (XMLErrorReporter)propertyManager.getProperty(Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY);
        try {
            fStaxEntityResolver = (StaxEntityResolverWrapper)propertyManager.getProperty(STAX_ENTITY_RESOLVER);
        } catch (XMLConfigurationException e) {
            fStaxEntityResolver = null;
        }

        fSupportDTD = ((Boolean)propertyManager.getProperty(XMLInputFactory.SUPPORT_DTD));
        fReplaceEntityReferences = ((Boolean)propertyManager.getProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES));
        fSupportExternalEntities = ((Boolean)propertyManager.getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES));

        // Zephyr feature ignore-external-dtd is the opposite of Xerces' load-external-dtd
        fLoadExternalDTD = !((Boolean)propertyManager.getProperty(Constants.ZEPHYR_PROPERTY_PREFIX + Constants.IGNORE_EXTERNAL_DTD));

        //Use Catalog
        fUseCatalog = (Boolean)propertyManager.getProperty(XMLConstants.USE_CATALOG);
        fCatalogFile = (String)propertyManager.getProperty(JdkXmlUtils.CATALOG_FILES);
        fDefer = (String)propertyManager.getProperty(JdkXmlUtils.CATALOG_DEFER);
        fPrefer = (String)propertyManager.getProperty(JdkXmlUtils.CATALOG_PREFER);
        fResolve = (String)propertyManager.getProperty(JdkXmlUtils.CATALOG_RESOLVE);

        // JAXP 1.5 feature
        XMLSecurityPropertyManager spm = (XMLSecurityPropertyManager) propertyManager.getProperty(XML_SECURITY_PROPERTY_MANAGER);
        fAccessExternalDTD = spm.getValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_DTD);

        fSecurityManager = (XMLSecurityManager)propertyManager.getProperty(SECURITY_MANAGER);

        fLimitAnalyzer = new XMLLimitAnalyzer();
        //reset fEntityStorage
        fEntityStorage.reset(propertyManager);
        //reset XMLEntityReaderImpl
        fEntityScanner.reset(propertyManager);

        // initialize state
        //fStandalone = false;
        fEntities.clear();
        fEntityStack.removeAllElements();
        fCurrentEntity = null;
        fValidation = false;
        fExternalGeneralEntities = true;
        fExternalParameterEntities = true;
        fAllowJavaEncodings = true ;
    }

    /**
     * Resets the component. The component can query the component manager
     * about any features and properties that affect the operation of the
     * component.
     *
     * @param componentManager The component manager.
     *
     * @throws SAXException Thrown by component on initialization error.
     *                      For example, if a feature or property is
     *                      required for the operation of the component, the
     *                      component manager may throw a
     *                      SAXNotRecognizedException or a
     *                      SAXNotSupportedException.
     */
    public void reset(XMLComponentManager componentManager)
    throws XMLConfigurationException {

        boolean parser_settings = componentManager.getFeature(PARSER_SETTINGS, true);

        if (!parser_settings) {
            // parser settings have not been changed
            reset();
            if(fEntityScanner != null){
                fEntityScanner.reset(componentManager);
            }
            if(fEntityStorage != null){
                fEntityStorage.reset(componentManager);
            }
            return;
        }

        // sax features
        fValidation = componentManager.getFeature(VALIDATION, false);
        fExternalGeneralEntities = componentManager.getFeature(EXTERNAL_GENERAL_ENTITIES, true);
        fExternalParameterEntities = componentManager.getFeature(EXTERNAL_PARAMETER_ENTITIES, true);

        // xerces features
        fAllowJavaEncodings = componentManager.getFeature(ALLOW_JAVA_ENCODINGS, false);
        fWarnDuplicateEntityDef = componentManager.getFeature(WARN_ON_DUPLICATE_ENTITYDEF, false);
        fStrictURI = componentManager.getFeature(STANDARD_URI_CONFORMANT, false);
        fLoadExternalDTD = componentManager.getFeature(LOAD_EXTERNAL_DTD, true);

        // xerces properties
        fSymbolTable = (SymbolTable)componentManager.getProperty(SYMBOL_TABLE);
        fErrorReporter = (XMLErrorReporter)componentManager.getProperty(ERROR_REPORTER);
        fEntityResolver = (XMLEntityResolver)componentManager.getProperty(ENTITY_RESOLVER, null);
        fStaxEntityResolver = (StaxEntityResolverWrapper)componentManager.getProperty(STAX_ENTITY_RESOLVER, null);
        fValidationManager = (ValidationManager)componentManager.getProperty(VALIDATION_MANAGER, null);
        fSecurityManager = (XMLSecurityManager)componentManager.getProperty(SECURITY_MANAGER, null);
        entityExpansionIndex = fSecurityManager.getIndex(JdkConstants.SP_ENTITY_EXPANSION_LIMIT);

        //StAX Property
        fSupportDTD = true;
        fReplaceEntityReferences = true;
        fSupportExternalEntities = true;

        // JAXP 1.5 feature
        XMLSecurityPropertyManager spm = (XMLSecurityPropertyManager) componentManager.getProperty(XML_SECURITY_PROPERTY_MANAGER, null);
        if (spm == null) {
            spm = new XMLSecurityPropertyManager();
        }
        fAccessExternalDTD = spm.getValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_DTD);

        //Use Catalog
        fUseCatalog = componentManager.getFeature(XMLConstants.USE_CATALOG, true);
        fCatalogFile = (String)componentManager.getProperty(JdkXmlUtils.CATALOG_FILES);
        fDefer = (String)componentManager.getProperty(JdkXmlUtils.CATALOG_DEFER);
        fPrefer = (String)componentManager.getProperty(JdkXmlUtils.CATALOG_PREFER);
        fResolve = (String)componentManager.getProperty(JdkXmlUtils.CATALOG_RESOLVE);

        //reset general state
        reset();

        fEntityScanner.reset(componentManager);
        fEntityStorage.reset(componentManager);

    } // reset(XMLComponentManager)

    // reset general state.  Should not be called other than by
    // a class acting as a component manager but not
    // implementing that interface for whatever reason.
    public void reset() {
        fLimitAnalyzer = new XMLLimitAnalyzer();
        // initialize state
        fStandalone = false;
        fEntities.clear();
        fEntityStack.removeAllElements();
        fEntityExpansionCount = 0;

        fCurrentEntity = null;
        // reset scanner
        if(fXML10EntityScanner != null){
            fXML10EntityScanner.reset(fSymbolTable, this, fErrorReporter);
        }
        if(fXML11EntityScanner != null) {
            fXML11EntityScanner.reset(fSymbolTable, this, fErrorReporter);
        }

        // DEBUG
        if (DEBUG_ENTITIES) {
            addInternalEntity("text", "Hello, World.");
            addInternalEntity("empty-element", "<foo/>");
            addInternalEntity("balanced-element", "<foo></foo>");
            addInternalEntity("balanced-element-with-text", "<foo>Hello, World</foo>");
            addInternalEntity("balanced-element-with-entity", "<foo>&text;</foo>");
            addInternalEntity("unbalanced-entity", "<foo>");
            addInternalEntity("recursive-entity", "<foo>&recursive-entity2;</foo>");
            addInternalEntity("recursive-entity2", "<bar>&recursive-entity3;</bar>");
            addInternalEntity("recursive-entity3", "<baz>&recursive-entity;</baz>");
            try {
                addExternalEntity("external-text", null, "external-text.ent", "test/external-text.xml");
                addExternalEntity("external-balanced-element", null, "external-balanced-element.ent", "test/external-balanced-element.xml");
                addExternalEntity("one", null, "ent/one.ent", "test/external-entity.xml");
                addExternalEntity("two", null, "ent/two.ent", "test/ent/one.xml");
            }
            catch (IOException ex) {
                // should never happen
            }
        }

        fEntityHandler = null;

        // reset scanner
        //if(fEntityScanner!=null)
          //  fEntityScanner.reset(fSymbolTable, this,fErrorReporter);

    }
    /**
     * Returns a list of feature identifiers that are recognized by
     * this component. This method may return null if no features
     * are recognized by this component.
     */
    public String[] getRecognizedFeatures() {
        return RECOGNIZED_FEATURES.clone();
    } // getRecognizedFeatures():String[]

    /**
     * Sets the state of a feature. This method is called by the component
     * manager any time after reset when a feature changes state.
     * <p>
     * <strong>Note:</strong> Components should silently ignore features
     * that do not affect the operation of the component.
     *
     * @param featureId The feature identifier.
     * @param state     The state of the feature.
     *
     * @throws SAXNotRecognizedException The component should not throw
     *                                   this exception.
     * @throws SAXNotSupportedException The component should not throw
     *                                  this exception.
     */
    public void setFeature(String featureId, boolean state)
    throws XMLConfigurationException {

        // xerces features
        if (featureId.startsWith(Constants.XERCES_FEATURE_PREFIX)) {
            final int suffixLength = featureId.length() - Constants.XERCES_FEATURE_PREFIX.length();
            if (suffixLength == Constants.ALLOW_JAVA_ENCODINGS_FEATURE.length() &&
                featureId.endsWith(Constants.ALLOW_JAVA_ENCODINGS_FEATURE)) {
                fAllowJavaEncodings = state;
            }
            if (suffixLength == Constants.LOAD_EXTERNAL_DTD_FEATURE.length() &&
                featureId.endsWith(Constants.LOAD_EXTERNAL_DTD_FEATURE)) {
                fLoadExternalDTD = state;
                return;
            }
        } else if (featureId.equals(XMLConstants.USE_CATALOG)) {
            fUseCatalog = state;
        }

    } // setFeature(String,boolean)

    /**
     * Sets the value of a property. This method is called by the component
     * manager any time after reset when a property changes value.
     * <p>
     * <strong>Note:</strong> Components should silently ignore properties
     * that do not affect the operation of the component.
     *
     * @param propertyId The property identifier.
     * @param value      The value of the property.
     *
     * @throws SAXNotRecognizedException The component should not throw
     *                                   this exception.
     * @throws SAXNotSupportedException The component should not throw
     *                                  this exception.
     */
    public void setProperty(String propertyId, Object value){
        // Xerces properties
        if (propertyId.startsWith(Constants.XERCES_PROPERTY_PREFIX)) {
            final int suffixLength = propertyId.length() - Constants.XERCES_PROPERTY_PREFIX.length();

            if (suffixLength == Constants.SYMBOL_TABLE_PROPERTY.length() &&
                propertyId.endsWith(Constants.SYMBOL_TABLE_PROPERTY)) {
                fSymbolTable = (SymbolTable)value;
                return;
            }
            if (suffixLength == Constants.ERROR_REPORTER_PROPERTY.length() &&
                propertyId.endsWith(Constants.ERROR_REPORTER_PROPERTY)) {
                fErrorReporter = (XMLErrorReporter)value;
                return;
            }
            if (suffixLength == Constants.ENTITY_RESOLVER_PROPERTY.length() &&
                propertyId.endsWith(Constants.ENTITY_RESOLVER_PROPERTY)) {
                fEntityResolver = (XMLEntityResolver)value;
                return;
            }
            if (suffixLength == Constants.BUFFER_SIZE_PROPERTY.length() &&
                propertyId.endsWith(Constants.BUFFER_SIZE_PROPERTY)) {
                Integer bufferSize = (Integer)value;
                if (bufferSize != null &&
                    bufferSize.intValue() > DEFAULT_XMLDECL_BUFFER_SIZE) {
                    fBufferSize = bufferSize.intValue();
                    fEntityScanner.setBufferSize(fBufferSize);
                }
            }
            if (suffixLength == Constants.SECURITY_MANAGER_PROPERTY.length() &&
                propertyId.endsWith(Constants.SECURITY_MANAGER_PROPERTY)) {
                fSecurityManager = (XMLSecurityManager)value;
            }
        }

        //JAXP 1.5 properties
        if (propertyId.equals(XML_SECURITY_PROPERTY_MANAGER))
        {
            XMLSecurityPropertyManager spm = (XMLSecurityPropertyManager)value;
            fAccessExternalDTD = spm.getValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_DTD);
            return;
        }

        //Catalog properties
        if (propertyId.equals(JdkXmlUtils.CATALOG_FILES)) {
            fCatalogFile = (String)value;
        } else if (propertyId.equals(JdkXmlUtils.CATALOG_DEFER)) {
            fDefer = (String)value;
        } else if (propertyId.equals(JdkXmlUtils.CATALOG_PREFER)) {
            fPrefer = (String)value;
        } else if (propertyId.equals(JdkXmlUtils.CATALOG_RESOLVE)) {
            fResolve = (String)value;
        }
    }

    public void setLimitAnalyzer(XMLLimitAnalyzer fLimitAnalyzer) {
        this.fLimitAnalyzer = fLimitAnalyzer;
    }

    /**
     * Returns a list of property identifiers that are recognized by
     * this component. This method may return null if no properties
     * are recognized by this component.
     */
    public String[] getRecognizedProperties() {
        return RECOGNIZED_PROPERTIES.clone();
    } // getRecognizedProperties():String[]
    /**
     * Returns the default state for a feature, or null if this
     * component does not want to report a default value for this
     * feature.
     *
     * @param featureId The feature identifier.
     *
     * @since Xerces 2.2.0
     */
    public Boolean getFeatureDefault(String featureId) {
        for (int i = 0; i < RECOGNIZED_FEATURES.length; i++) {
            if (RECOGNIZED_FEATURES[i].equals(featureId)) {
                return FEATURE_DEFAULTS[i];
            }
        }
        return null;
    } // getFeatureDefault(String):Boolean

    /**
     * Returns the default state for a property, or null if this
     * component does not want to report a default value for this
     * property.
     *
     * @param propertyId The property identifier.
     *
     * @since Xerces 2.2.0
     */
    public Object getPropertyDefault(String propertyId) {
        for (int i = 0; i < RECOGNIZED_PROPERTIES.length; i++) {
            if (RECOGNIZED_PROPERTIES[i].equals(propertyId)) {
                return PROPERTY_DEFAULTS[i];
            }
        }
        return null;
    } // getPropertyDefault(String):Object

    //
    // Public static methods
    //

    /**
     * Expands a system id and returns the system id as a URI, if
     * it can be expanded. A return value of null means that the
     * identifier is already expanded. An exception thrown
     * indicates a failure to expand the id.
     *
     * @param systemId The systemId to be expanded.
     *
     * @return Returns the URI string representing the expanded system
     *         identifier. A null value indicates that the given
     *         system identifier is already expanded.
     *
     */
    public static String expandSystemId(String systemId) {
        return expandSystemId(systemId, null);
    } // expandSystemId(String):String

    //
    // Public static methods
    //

    // current value of the "user.dir" property
    private static String gUserDir;
    // cached URI object for the current value of the escaped "user.dir" property stored as a URI
    private static URI gUserDirURI;
    // which ASCII characters need to be escaped
    private static boolean gNeedEscaping[] = new boolean[128];
    // the first hex character if a character needs to be escaped
    private static char gAfterEscaping1[] = new char[128];
    // the second hex character if a character needs to be escaped
    private static char gAfterEscaping2[] = new char[128];
    private static char[] gHexChs = {'0', '1', '2', '3', '4', '5', '6', '7',
                                     '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    // initialize the above 3 arrays
    static {
        for (int i = 0; i <= 0x1f; i++) {
            gNeedEscaping[i] = true;
            gAfterEscaping1[i] = gHexChs[i >> 4];
            gAfterEscaping2[i] = gHexChs[i & 0xf];
        }
        gNeedEscaping[0x7f] = true;
        gAfterEscaping1[0x7f] = '7';
        gAfterEscaping2[0x7f] = 'F';
        char[] escChs = {' ', '<', '>', '#', '%', '"', '{', '}',
                         '|', '\\', '^', '~', '[', ']', '`'};
        int len = escChs.length;
        char ch;
        for (int i = 0; i < len; i++) {
            ch = escChs[i];
            gNeedEscaping[ch] = true;
            gAfterEscaping1[ch] = gHexChs[ch >> 4];
            gAfterEscaping2[ch] = gHexChs[ch & 0xf];
        }
    }

    // To escape the "user.dir" system property, by using %HH to represent
    // special ASCII characters: 0x00~0x1F, 0x7F, ' ', '<', '>', '#', '%'
    // and '"'. It's a static method, so needs to be synchronized.
    // this method looks heavy, but since the system property isn't expected
    // to change often, so in most cases, we only need to return the URI
    // that was escaped before.
    // According to the URI spec, non-ASCII characters (whose value >= 128)
    // need to be escaped too.
    // REVISIT: don't know how to escape non-ASCII characters, especially
    // which encoding to use. Leave them for now.
    private static synchronized URI getUserDir() throws URI.MalformedURIException {
        // get the user.dir property
        String userDir = "";
        try {
            userDir = SecuritySupport.getSystemProperty("user.dir");
        }
        catch (SecurityException se) {
        }

        // return empty string if property value is empty string.
        if (userDir.length() == 0)
            return new URI("file", "", "", null, null);
        // compute the new escaped value if the new property value doesn't
        // match the previous one
        if (gUserDirURI != null && userDir.equals(gUserDir)) {
            return gUserDirURI;
        }

        // record the new value as the global property value
        gUserDir = userDir;

        char separator = java.io.File.separatorChar;
        userDir = userDir.replace(separator, '/');

        int len = userDir.length(), ch;
        StringBuilder buffer = new StringBuilder(len*3);
        // change C:/blah to /C:/blah
        if (len >= 2 && userDir.charAt(1) == ':') {
            ch = Character.toUpperCase(userDir.charAt(0));
            if (ch >= 'A' && ch <= 'Z') {
                buffer.append('/');
            }
        }

        // for each character in the path
        int i = 0;
        for (; i < len; i++) {
            ch = userDir.charAt(i);
            // if it's not an ASCII character, break here, and use UTF-8 encoding
            if (ch >= 128)
                break;
            if (gNeedEscaping[ch]) {
                buffer.append('%');
                buffer.append(gAfterEscaping1[ch]);
                buffer.append(gAfterEscaping2[ch]);
                // record the fact that it's escaped
            }
            else {
                buffer.append((char)ch);
            }
        }

        // we saw some non-ascii character
        if (i < len) {
            // get UTF-8 bytes for the remaining sub-string
            byte[] bytes = null;
            byte b;
            try {
                bytes = userDir.substring(i).getBytes("UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                // should never happen
                return new URI("file", "", userDir, null, null);
            }
            len = bytes.length;

            // for each byte
            for (i = 0; i < len; i++) {
                b = bytes[i];
                // for non-ascii character: make it positive, then escape
                if (b < 0) {
                    ch = b + 256;
                    buffer.append('%');
                    buffer.append(gHexChs[ch >> 4]);
                    buffer.append(gHexChs[ch & 0xf]);
                }
                else if (gNeedEscaping[b]) {
                    buffer.append('%');
                    buffer.append(gAfterEscaping1[b]);
                    buffer.append(gAfterEscaping2[b]);
                }
                else {
                    buffer.append((char)b);
                }
            }
        }

        // change blah/blah to blah/blah/
        if (!userDir.endsWith("/"))
            buffer.append('/');

        gUserDirURI = new URI("file", "", buffer.toString(), null, null);

        return gUserDirURI;
    }

    public static OutputStream createOutputStream(String uri) throws IOException {
        // URI was specified. Handle relative URIs.
        final String expanded = XMLEntityManager.expandSystemId(uri, null, true);
        final URL url = new URL(expanded != null ? expanded : uri);
        OutputStream out = null;
        String protocol = url.getProtocol();
        String host = url.getHost();
        // Use FileOutputStream if this URI is for a local file.
        if (protocol.equals("file")
                && (host == null || host.length() == 0 || host.equals("localhost"))) {
            File file = new File(getPathWithoutEscapes(url.getPath()));
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
            }
            out = new FileOutputStream(file);
        }
        // Try to write to some other kind of URI. Some protocols
        // won't support this, though HTTP should work.
        else {
            URLConnection urlCon = url.openConnection();
            urlCon.setDoInput(false);
            urlCon.setDoOutput(true);
            urlCon.setUseCaches(false); // Enable tunneling.
            if (urlCon instanceof HttpURLConnection) {
                // The DOM L3 REC says if we are writing to an HTTP URI
                // it is to be done with an HTTP PUT.
                HttpURLConnection httpCon = (HttpURLConnection) urlCon;
                httpCon.setRequestMethod("PUT");
            }
            out = urlCon.getOutputStream();
        }
        return out;
    }

    private static String getPathWithoutEscapes(String origPath) {
        if (origPath != null && origPath.length() != 0 && origPath.indexOf('%') != -1) {
            // Locate the escape characters
            StringTokenizer tokenizer = new StringTokenizer(origPath, "%");
            StringBuilder result = new StringBuilder(origPath.length());
            int size = tokenizer.countTokens();
            result.append(tokenizer.nextToken());
            for(int i = 1; i < size; ++i) {
                String token = tokenizer.nextToken();
                // Decode the 2 digit hexadecimal number following % in '%nn'
                result.append((char)Integer.valueOf(token.substring(0, 2), 16).intValue());
                result.append(token.substring(2));
            }
            return result.toString();
        }
        return origPath;
    }

    /**
     * Absolutizes a URI using the current value
     * of the "user.dir" property as the base URI. If
     * the URI is already absolute, this is a no-op.
     *
     * @param uri the URI to absolutize
     */
    public static void absolutizeAgainstUserDir(URI uri)
        throws URI.MalformedURIException {
        uri.absolutize(getUserDir());
    }

    /**
     * Expands a system id and returns the system id as a URI, if
     * it can be expanded. A return value of null means that the
     * identifier is already expanded. An exception thrown
     * indicates a failure to expand the id.
     *
     * @param systemId The systemId to be expanded.
     *
     * @return Returns the URI string representing the expanded system
     *         identifier. A null value indicates that the given
     *         system identifier is already expanded.
     *
     */
    public static String expandSystemId(String systemId, String baseSystemId) {

        // check for bad parameters id
        if (systemId == null || systemId.length() == 0) {
            return systemId;
        }
        // if id already expanded, return
        try {
            URI uri = new URI(systemId);
            if (uri != null) {
                return systemId;
            }
        } catch (URI.MalformedURIException e) {
            // continue on...
        }
        // normalize id
        String id = fixURI(systemId);

        // normalize base
        URI base = null;
        URI uri = null;
        try {
            if (baseSystemId == null || baseSystemId.length() == 0 ||
                    baseSystemId.equals(systemId)) {
                String dir = getUserDir().toString();
                base = new URI("file", "", dir, null, null);
            } else {
                try {
                    base = new URI(fixURI(baseSystemId));
                } catch (URI.MalformedURIException e) {
                    if (baseSystemId.indexOf(':') != -1) {
                        // for xml schemas we might have baseURI with
                        // a specified drive
                        base = new URI("file", "", fixURI(baseSystemId), null, null);
                    } else {
                        String dir = getUserDir().toString();
                        dir = dir + fixURI(baseSystemId);
                        base = new URI("file", "", dir, null, null);
                    }
                }
            }
            // expand id
            uri = new URI(base, id);
        } catch (Exception e) {
            // let it go through

        }

        if (uri == null) {
            return systemId;
        }
        return uri.toString();

    } // expandSystemId(String,String):String

    /**
     * Expands a system id and returns the system id as a URI, if
     * it can be expanded. A return value of null means that the
     * identifier is already expanded. An exception thrown
     * indicates a failure to expand the id.
     *
     * @param systemId The systemId to be expanded.
     *
     * @return Returns the URI string representing the expanded system
     *         identifier. A null value indicates that the given
     *         system identifier is already expanded.
     *
     */
    public static String expandSystemId(String systemId, String baseSystemId,
                                        boolean strict)
            throws URI.MalformedURIException {

        // check if there is a system id before
        // trying to expand it.
        if (systemId == null) {
            return null;
        }

        // system id has to be a valid URI
        if (strict) {
            try {
                // if it's already an absolute one, return it
                new URI(systemId);
                return systemId;
            }
            catch (URI.MalformedURIException ex) {
            }
            URI base = null;
            // if there isn't a base uri, use the working directory
            if (baseSystemId == null || baseSystemId.length() == 0) {
                base = new URI("file", "", getUserDir().toString(), null, null);
            }
            // otherwise, use the base uri
            else {
                try {
                    base = new URI(baseSystemId);
                }
                catch (URI.MalformedURIException e) {
                    // assume "base" is also a relative uri
                    String dir = getUserDir().toString();
                    dir = dir + baseSystemId;
                    base = new URI("file", "", dir, null, null);
                }
            }
            // absolutize the system id using the base
            URI uri = new URI(base, systemId);
            // return the string rep of the new uri (an absolute one)
            return uri.toString();

            // if any exception is thrown, it'll get thrown to the caller.
        }

        // Assume the URIs are well-formed. If it turns out they're not, try fixing them up.
        try {
             return expandSystemIdStrictOff(systemId, baseSystemId);
        }
        catch (URI.MalformedURIException e) {
            /** Xerces URI rejects unicode, try java.net.URI
             * this is not ideal solution, but it covers known cases which either
             * Xerces URI or java.net.URI can handle alone
             * will file bug against java.net.URI
             */
            try {
                return expandSystemIdStrictOff1(systemId, baseSystemId);
            } catch (URISyntaxException ex) {
                // continue on...
            }
        }
        // check for bad parameters id
        if (systemId.length() == 0) {
            return systemId;
        }

        // normalize id
        String id = fixURI(systemId);

        // normalize base
        URI base = null;
        URI uri = null;
        try {
            if (baseSystemId == null || baseSystemId.length() == 0 ||
                baseSystemId.equals(systemId)) {
                base = getUserDir();
            }
            else {
                try {
                    base = new URI(fixURI(baseSystemId).trim());
                }
                catch (URI.MalformedURIException e) {
                    if (baseSystemId.indexOf(':') != -1) {
                        // for xml schemas we might have baseURI with
                        // a specified drive
                        base = new URI("file", "", fixURI(baseSystemId).trim(), null, null);
                    }
                    else {
                        base = new URI(getUserDir(), fixURI(baseSystemId));
                    }
                }
             }
             // expand id
             uri = new URI(base, id.trim());
        }
        catch (Exception e) {
            // let it go through

        }

        if (uri == null) {
            return systemId;
        }
        return uri.toString();

    } // expandSystemId(String,String,boolean):String

    /**
     * Helper method for expandSystemId(String,String,boolean):String
     */
    private static String expandSystemIdStrictOn(String systemId, String baseSystemId)
        throws URI.MalformedURIException {

        URI systemURI = new URI(systemId, true);
        // If it's already an absolute one, return it
        if (systemURI.isAbsoluteURI()) {
            return systemId;
        }

        // If there isn't a base URI, use the working directory
        URI baseURI = null;
        if (baseSystemId == null || baseSystemId.length() == 0) {
            baseURI = getUserDir();
        }
        else {
            baseURI = new URI(baseSystemId, true);
            if (!baseURI.isAbsoluteURI()) {
                // assume "base" is also a relative uri
                baseURI.absolutize(getUserDir());
            }
        }

        // absolutize the system identifier using the base URI
        systemURI.absolutize(baseURI);

        // return the string rep of the new uri (an absolute one)
        return systemURI.toString();

        // if any exception is thrown, it'll get thrown to the caller.

    } // expandSystemIdStrictOn(String,String):String

    /**
     * Helper method for expandSystemId(String,String,boolean):String
     */
    private static String expandSystemIdStrictOff(String systemId, String baseSystemId)
        throws URI.MalformedURIException {

        URI systemURI = new URI(systemId, true);
        // If it's already an absolute one, return it
        if (systemURI.isAbsoluteURI()) {
            if (systemURI.getScheme().length() > 1) {
                return systemId;
            }
            /**
             * If the scheme's length is only one character,
             * it's likely that this was intended as a file
             * path. Fixing this up in expandSystemId to
             * maintain backwards compatibility.
             */
            throw new URI.MalformedURIException();
        }

        // If there isn't a base URI, use the working directory
        URI baseURI = null;
        if (baseSystemId == null || baseSystemId.length() == 0) {
            baseURI = getUserDir();
        }
        else {
            baseURI = new URI(baseSystemId, true);
            if (!baseURI.isAbsoluteURI()) {
                // assume "base" is also a relative uri
                baseURI.absolutize(getUserDir());
            }
        }

        // absolutize the system identifier using the base URI
        systemURI.absolutize(baseURI);

        // return the string rep of the new uri (an absolute one)
        return systemURI.toString();

        // if any exception is thrown, it'll get thrown to the caller.

    } // expandSystemIdStrictOff(String,String):String

    private static String expandSystemIdStrictOff1(String systemId, String baseSystemId)
        throws URISyntaxException, URI.MalformedURIException {

            java.net.URI systemURI = new java.net.URI(systemId);
        // If it's already an absolute one, return it
        if (systemURI.isAbsolute()) {
            if (systemURI.getScheme().length() > 1) {
                return systemId;
            }
            /**
             * If the scheme's length is only one character,
             * it's likely that this was intended as a file
             * path. Fixing this up in expandSystemId to
             * maintain backwards compatibility.
             */
            throw new URISyntaxException(systemId, "the scheme's length is only one character");
        }

        // If there isn't a base URI, use the working directory
        URI baseURI = null;
        if (baseSystemId == null || baseSystemId.length() == 0) {
            baseURI = getUserDir();
        }
        else {
            baseURI = new URI(baseSystemId, true);
            if (!baseURI.isAbsoluteURI()) {
                // assume "base" is also a relative uri
                baseURI.absolutize(getUserDir());
            }
        }

        // absolutize the system identifier using the base URI
//        systemURI.absolutize(baseURI);
        systemURI = (new java.net.URI(baseURI.toString())).resolve(systemURI);

        // return the string rep of the new uri (an absolute one)
        return systemURI.toString();

        // if any exception is thrown, it'll get thrown to the caller.

    } // expandSystemIdStrictOff(String,String):String

    //
    // Protected methods
    //


    /**
     * Returns the IANA encoding name that is auto-detected from
     * the bytes specified, with the endian-ness of that encoding where appropriate.
     *
     * @param b4    The first four bytes of the input.
     * @param count The number of bytes actually read.
     * @return an instance of EncodingInfo which represents the auto-detected encoding.
     */
    protected EncodingInfo getEncodingInfo(byte[] b4, int count) {

        if (count < 2) {
            return EncodingInfo.UTF_8;
        }

        // UTF-16, with BOM
        int b0 = b4[0] & 0xFF;
        int b1 = b4[1] & 0xFF;
        if (b0 == 0xFE && b1 == 0xFF) {
            // UTF-16, big-endian
            return EncodingInfo.UTF_16_BIG_ENDIAN_WITH_BOM;
        }
        if (b0 == 0xFF && b1 == 0xFE) {
            // UTF-16, little-endian
            return EncodingInfo.UTF_16_LITTLE_ENDIAN_WITH_BOM;
        }

        // default to UTF-8 if we don't have enough bytes to make a
        // good determination of the encoding
        if (count < 3) {
            return EncodingInfo.UTF_8;
        }

        // UTF-8 with a BOM
        int b2 = b4[2] & 0xFF;
        if (b0 == 0xEF && b1 == 0xBB && b2 == 0xBF) {
            return EncodingInfo.UTF_8_WITH_BOM;
        }

        // default to UTF-8 if we don't have enough bytes to make a
        // good determination of the encoding
        if (count < 4) {
            return EncodingInfo.UTF_8;
        }

        // other encodings
        int b3 = b4[3] & 0xFF;
        if (b0 == 0x00 && b1 == 0x00 && b2 == 0x00 && b3 == 0x3C) {
            // UCS-4, big endian (1234)
            return EncodingInfo.UCS_4_BIG_ENDIAN;
        }
        if (b0 == 0x3C && b1 == 0x00 && b2 == 0x00 && b3 == 0x00) {
            // UCS-4, little endian (4321)
            return EncodingInfo.UCS_4_LITTLE_ENDIAN;
        }
        if (b0 == 0x00 && b1 == 0x00 && b2 == 0x3C && b3 == 0x00) {
            // UCS-4, unusual octet order (2143)
            // REVISIT: What should this be?
            return EncodingInfo.UCS_4_UNUSUAL_BYTE_ORDER;
        }
        if (b0 == 0x00 && b1 == 0x3C && b2 == 0x00 && b3 == 0x00) {
            // UCS-4, unusual octect order (3412)
            // REVISIT: What should this be?
            return EncodingInfo.UCS_4_UNUSUAL_BYTE_ORDER;
        }
        if (b0 == 0x00 && b1 == 0x3C && b2 == 0x00 && b3 == 0x3F) {
            // UTF-16, big-endian, no BOM
            // (or could turn out to be UCS-2...
            // REVISIT: What should this be?
            return EncodingInfo.UTF_16_BIG_ENDIAN;
        }
        if (b0 == 0x3C && b1 == 0x00 && b2 == 0x3F && b3 == 0x00) {
            // UTF-16, little-endian, no BOM
            // (or could turn out to be UCS-2...
            return EncodingInfo.UTF_16_LITTLE_ENDIAN;
        }
        if (b0 == 0x4C && b1 == 0x6F && b2 == 0xA7 && b3 == 0x94) {
            // EBCDIC
            // a la xerces1, return CP037 instead of EBCDIC here
            return EncodingInfo.EBCDIC;
        }

        // default encoding
        return EncodingInfo.UTF_8;

    } // getEncodingName(byte[],int):Object[]

    /**
     * Creates a reader capable of reading the given input stream in
     * the specified encoding.
     *
     * @param inputStream  The input stream.
     * @param encoding     The encoding name that the input stream is
     *                     encoded using. If the user has specified that
     *                     Java encoding names are allowed, then the
     *                     encoding name may be a Java encoding name;
     *                     otherwise, it is an ianaEncoding name.
     * @param isBigEndian   For encodings (like uCS-4), whose names cannot
     *                      specify a byte order, this tells whether the order
     *                      is bigEndian.  null if unknown or irrelevant.
     *
     * @return Returns a reader.
     */
    protected Reader createReader(InputStream inputStream, String encoding, Boolean isBigEndian)
        throws IOException {

        String enc = (encoding != null) ? encoding : EncodingInfo.STR_UTF8;
        enc = enc.toUpperCase(Locale.ENGLISH);
        MessageFormatter f = fErrorReporter.getMessageFormatter(XMLMessageFormatter.XML_DOMAIN);
        Locale l = fErrorReporter.getLocale();
        switch (enc) {
            case EncodingInfo.STR_UTF8:
                return new UTF8Reader(inputStream, fBufferSize, f, l);
            case EncodingInfo.STR_UTF16:
                if (isBigEndian != null) {
                    return new UTF16Reader(inputStream, fBufferSize, isBigEndian, f, l);
                }
                break;
            case EncodingInfo.STR_UTF16BE:
                return new UTF16Reader(inputStream, fBufferSize, true, f, l);
            case EncodingInfo.STR_UTF16LE:
                return new UTF16Reader(inputStream, fBufferSize, false, f, l);
            case EncodingInfo.STR_UCS4:
                if(isBigEndian != null) {
                    if(isBigEndian) {
                        return new UCSReader(inputStream, UCSReader.UCS4BE);
                    } else {
                        return new UCSReader(inputStream, UCSReader.UCS4LE);
                    }
                } else {
                    fErrorReporter.reportError(this.getEntityScanner(),
                            XMLMessageFormatter.XML_DOMAIN,
                            "EncodingByteOrderUnsupported",
                            new Object[] { encoding },
                            XMLErrorReporter.SEVERITY_FATAL_ERROR);
                }
                break;
            case EncodingInfo.STR_UCS2:
                if(isBigEndian != null) {
                    if(isBigEndian) {
                        return new UCSReader(inputStream, UCSReader.UCS2BE);
                    } else {
                        return new UCSReader(inputStream, UCSReader.UCS2LE);
                    }
                } else {
                    fErrorReporter.reportError(this.getEntityScanner(),
                            XMLMessageFormatter.XML_DOMAIN,
                            "EncodingByteOrderUnsupported",
                            new Object[] { encoding },
                            XMLErrorReporter.SEVERITY_FATAL_ERROR);
                }
                break;
        }

        // check for valid name
        boolean validIANA = XMLChar.isValidIANAEncoding(encoding);
        boolean validJava = XMLChar.isValidJavaEncoding(encoding);
        if (!validIANA || (fAllowJavaEncodings && !validJava)) {
            fErrorReporter.reportError(this.getEntityScanner(),
                    XMLMessageFormatter.XML_DOMAIN,
                    "EncodingDeclInvalid",
                    new Object[] { encoding },
                    XMLErrorReporter.SEVERITY_FATAL_ERROR);
            // NOTE: AndyH suggested that, on failure, we use ISO Latin 1
            //       because every byte is a valid ISO Latin 1 character.
            //       It may not translate correctly but if we failed on
            //       the encoding anyway, then we're expecting the content
            //       of the document to be bad. This will just prevent an
            //       invalid UTF-8 sequence to be detected. This is only
            //       important when continue-after-fatal-error is turned
            //       on. -Ac
                    encoding = "ISO-8859-1";
        }

        // try to use a Java reader
        String javaEncoding = EncodingMap.getIANA2JavaMapping(enc);
        if (javaEncoding == null) {
            if (fAllowJavaEncodings) {
                javaEncoding = encoding;
            } else {
                fErrorReporter.reportError(this.getEntityScanner(),
                        XMLMessageFormatter.XML_DOMAIN,
                        "EncodingDeclInvalid",
                        new Object[] { encoding },
                        XMLErrorReporter.SEVERITY_FATAL_ERROR);
                // see comment above.
                javaEncoding = "ISO8859_1";
            }
        }
        if (DEBUG_ENCODINGS) {
            System.out.print("$$$ creating Java InputStreamReader: encoding="+javaEncoding);
            if (javaEncoding == encoding) {
                System.out.print(" (IANA encoding)");
            }
            System.out.println();
        }
        return new BufferedReader( new InputStreamReader(inputStream, javaEncoding));

    } // createReader(InputStream,String, Boolean): Reader


    /**
     * Return the public identifier for the current document event.
     * <p>
     * The return value is the public identifier of the document
     * entity or of the external parsed entity in which the markup
     * triggering the event appears.
     *
     * @return A string containing the public identifier, or
     *         null if none is available.
     */
    public String getPublicId() {
        return (fCurrentEntity != null && fCurrentEntity.entityLocation != null) ? fCurrentEntity.entityLocation.getPublicId() : null;
    } // getPublicId():String

    /**
     * Return the expanded system identifier for the current document event.
     * <p>
     * The return value is the expanded system identifier of the document
     * entity or of the external parsed entity in which the markup
     * triggering the event appears.
     * <p>
     * If the system identifier is a URL, the parser must resolve it
     * fully before passing it to the application.
     *
     * @return A string containing the expanded system identifier, or null
     *         if none is available.
     */
    public String getExpandedSystemId() {
        if (fCurrentEntity != null) {
            if (fCurrentEntity.entityLocation != null &&
                    fCurrentEntity.entityLocation.getExpandedSystemId() != null ) {
                return fCurrentEntity.entityLocation.getExpandedSystemId();
            } else {
                // search for the first external entity on the stack
                int size = fEntityStack.size();
                for (int i = size - 1; i >= 0 ; i--) {
                    Entity.ScannedEntity externalEntity =
                            (Entity.ScannedEntity)fEntityStack.get(i);

                    if (externalEntity.entityLocation != null &&
                            externalEntity.entityLocation.getExpandedSystemId() != null) {
                        return externalEntity.entityLocation.getExpandedSystemId();
                    }
                }
            }
        }
        return null;
    } // getExpandedSystemId():String

    /**
     * Return the literal system identifier for the current document event.
     * <p>
     * The return value is the literal system identifier of the document
     * entity or of the external parsed entity in which the markup
     * triggering the event appears.
     * <p>
     * @return A string containing the literal system identifier, or null
     *         if none is available.
     */
    public String getLiteralSystemId() {
        if (fCurrentEntity != null) {
            if (fCurrentEntity.entityLocation != null &&
                    fCurrentEntity.entityLocation.getLiteralSystemId() != null ) {
                return fCurrentEntity.entityLocation.getLiteralSystemId();
            } else {
                // search for the first external entity on the stack
                int size = fEntityStack.size();
                for (int i = size - 1; i >= 0 ; i--) {
                    Entity.ScannedEntity externalEntity =
                            (Entity.ScannedEntity)fEntityStack.get(i);

                    if (externalEntity.entityLocation != null &&
                            externalEntity.entityLocation.getLiteralSystemId() != null) {
                        return externalEntity.entityLocation.getLiteralSystemId();
                    }
                }
            }
        }
        return null;
    } // getLiteralSystemId():String

    /**
     * Return the line number where the current document event ends.
     * <p>
     * <strong>Warning:</strong> The return value from the method
     * is intended only as an approximation for the sake of error
     * reporting; it is not intended to provide sufficient information
     * to edit the character content of the original XML document.
     * <p>
     * The return value is an approximation of the line number
     * in the document entity or external parsed entity where the
     * markup triggering the event appears.
     * <p>
     * If possible, the SAX driver should provide the line position
     * of the first character after the text associated with the document
     * event.  The first line in the document is line 1.
     *
     * @return The line number, or -1 if none is available.
     */
    public int getLineNumber() {
        if (fCurrentEntity != null) {
            if (fCurrentEntity.isExternal()) {
                return fCurrentEntity.lineNumber;
            } else {
                // search for the first external entity on the stack
                int size = fEntityStack.size();
                for (int i=size-1; i>0 ; i--) {
                    Entity.ScannedEntity firstExternalEntity = (Entity.ScannedEntity)fEntityStack.get(i);
                    if (firstExternalEntity.isExternal()) {
                        return firstExternalEntity.lineNumber;
                    }
                }
            }
        }

        return -1;

    } // getLineNumber():int

    /**
     * Return the column number where the current document event ends.
     * <p>
     * <strong>Warning:</strong> The return value from the method
     * is intended only as an approximation for the sake of error
     * reporting; it is not intended to provide sufficient information
     * to edit the character content of the original XML document.
     * <p>
     * The return value is an approximation of the column number
     * in the document entity or external parsed entity where the
     * markup triggering the event appears.
     * <p>
     * If possible, the SAX driver should provide the line position
     * of the first character after the text associated with the document
     * event.
     * <p>
     * If possible, the SAX driver should provide the line position
     * of the first character after the text associated with the document
     * event.  The first column in each line is column 1.
     *
     * @return The column number, or -1 if none is available.
     */
    public int getColumnNumber() {
        if (fCurrentEntity != null) {
            if (fCurrentEntity.isExternal()) {
                return fCurrentEntity.columnNumber;
            } else {
                // search for the first external entity on the stack
                int size = fEntityStack.size();
                for (int i=size-1; i>0 ; i--) {
                    Entity.ScannedEntity firstExternalEntity = (Entity.ScannedEntity)fEntityStack.get(i);
                    if (firstExternalEntity.isExternal()) {
                        return firstExternalEntity.columnNumber;
                    }
                }
            }
        }

        return -1;
    } // getColumnNumber():int


    //
    // Protected static methods
    //

    /**
     * Fixes a platform dependent filename to standard URI form.
     *
     * @param str The string to fix.
     *
     * @return Returns the fixed URI string.
     */
    protected static String fixURI(String str) {

        // handle platform dependent strings
        str = str.replace(java.io.File.separatorChar, '/');

        // Windows fix
        if (str.length() >= 2) {
            char ch1 = str.charAt(1);
            // change "C:blah" to "/C:blah"
            if (ch1 == ':') {
                char ch0 = Character.toUpperCase(str.charAt(0));
                if (ch0 >= 'A' && ch0 <= 'Z') {
                    str = "/" + str;
                }
            }
            // change "//blah" to "file://blah"
            else if (ch1 == '/' && str.charAt(0) == '/') {
                str = "file:" + str;
            }
        }

        // replace spaces in file names with %20.
        // Original comment from JDK5: the following algorithm might not be
        // very performant, but people who want to use invalid URI's have to
        // pay the price.
        int pos = str.indexOf(' ');
        if (pos >= 0) {
            StringBuilder sb = new StringBuilder(str.length());
            // put characters before ' ' into the string builder
            for (int i = 0; i < pos; i++)
                sb.append(str.charAt(i));
            // and %20 for the space
            sb.append("%20");
            // for the remamining part, also convert ' ' to "%20".
            for (int i = pos+1; i < str.length(); i++) {
                if (str.charAt(i) == ' ')
                    sb.append("%20");
                else
                    sb.append(str.charAt(i));
            }
            str = sb.toString();
        }

        // done
        return str;

    } // fixURI(String):String


    //
    // Package visible methods
    //
    /** Prints the contents of the buffer. */
    final void print() {
        if (DEBUG_BUFFER) {
            if (fCurrentEntity != null) {
                System.out.print('[');
                System.out.print(fCurrentEntity.count);
                System.out.print(' ');
                System.out.print(fCurrentEntity.position);
                if (fCurrentEntity.count > 0) {
                    System.out.print(" \"");
                    for (int i = 0; i < fCurrentEntity.count; i++) {
                        if (i == fCurrentEntity.position) {
                            System.out.print('^');
                        }
                        char c = fCurrentEntity.ch[i];
                        switch (c) {
                            case '\n': {
                                System.out.print("\\n");
                                break;
                            }
                            case '\r': {
                                System.out.print("\\r");
                                break;
                            }
                            case '\t': {
                                System.out.print("\\t");
                                break;
                            }
                            case '\\': {
                                System.out.print("\\\\");
                                break;
                            }
                            default: {
                                System.out.print(c);
                            }
                        }
                    }
                    if (fCurrentEntity.position == fCurrentEntity.count) {
                        System.out.print('^');
                    }
                    System.out.print('"');
                }
                System.out.print(']');
                System.out.print(" @ ");
                System.out.print(fCurrentEntity.lineNumber);
                System.out.print(',');
                System.out.print(fCurrentEntity.columnNumber);
            } else {
                System.out.print("*NO CURRENT ENTITY*");
            }
        }
    } // print()

    /**
     * Information about auto-detectable encodings.
     *
     * @xerces.internal
     *
     * @author Michael Glavassevich, IBM
     */
    private static class EncodingInfo {
        public static final String STR_UTF8 = "UTF-8";
        public static final String STR_UTF16 = "UTF-16";
        public static final String STR_UTF16BE = "UTF-16BE";
        public static final String STR_UTF16LE = "UTF-16LE";
        public static final String STR_UCS4 = "ISO-10646-UCS-4";
        public static final String STR_UCS2 = "ISO-10646-UCS-2";
        public static final String STR_CP037 = "CP037";

        /** UTF-8 **/
        public static final EncodingInfo UTF_8 =
                new EncodingInfo(STR_UTF8, null, false);

        /** UTF-8, with BOM **/
        public static final EncodingInfo UTF_8_WITH_BOM =
                new EncodingInfo(STR_UTF8, null, true);

        /** UTF-16, big-endian **/
        public static final EncodingInfo UTF_16_BIG_ENDIAN =
                new EncodingInfo(STR_UTF16BE, STR_UTF16, Boolean.TRUE, false);

        /** UTF-16, big-endian with BOM **/
        public static final EncodingInfo UTF_16_BIG_ENDIAN_WITH_BOM =
                new EncodingInfo(STR_UTF16BE, STR_UTF16, Boolean.TRUE, true);

        /** UTF-16, little-endian **/
        public static final EncodingInfo UTF_16_LITTLE_ENDIAN =
                new EncodingInfo(STR_UTF16LE, STR_UTF16, Boolean.FALSE, false);

        /** UTF-16, little-endian with BOM **/
        public static final EncodingInfo UTF_16_LITTLE_ENDIAN_WITH_BOM =
                new EncodingInfo(STR_UTF16LE, STR_UTF16, Boolean.FALSE, true);

        /** UCS-4, big-endian **/
        public static final EncodingInfo UCS_4_BIG_ENDIAN =
                new EncodingInfo(STR_UCS4, Boolean.TRUE, false);

        /** UCS-4, little-endian **/
        public static final EncodingInfo UCS_4_LITTLE_ENDIAN =
                new EncodingInfo(STR_UCS4, Boolean.FALSE, false);

        /** UCS-4, unusual byte-order (2143) or (3412) **/
        public static final EncodingInfo UCS_4_UNUSUAL_BYTE_ORDER =
                new EncodingInfo(STR_UCS4, null, false);

        /** EBCDIC **/
        public static final EncodingInfo EBCDIC = new EncodingInfo(STR_CP037, null, false);

        public final String autoDetectedEncoding;
        public final String readerEncoding;
        public final Boolean isBigEndian;
        public final boolean hasBOM;

        private EncodingInfo(String autoDetectedEncoding, Boolean isBigEndian, boolean hasBOM) {
            this(autoDetectedEncoding, autoDetectedEncoding, isBigEndian, hasBOM);
        } // <init>(String,Boolean,boolean)

        private EncodingInfo(String autoDetectedEncoding, String readerEncoding,
                Boolean isBigEndian, boolean hasBOM) {
            this.autoDetectedEncoding = autoDetectedEncoding;
            this.readerEncoding = readerEncoding;
            this.isBigEndian = isBigEndian;
            this.hasBOM = hasBOM;
        } // <init>(String,String,Boolean,boolean)

    } // class EncodingInfo

    /**
    * This class wraps the byte inputstreams we're presented with.
    * We need it because java.io.InputStreams don't provide
    * functionality to reread processed bytes, and they have a habit
    * of reading more than one character when you call their read()
    * methods.  This means that, once we discover the true (declared)
    * encoding of a document, we can neither backtrack to read the
    * whole doc again nor start reading where we are with a new
    * reader.
    *
    * This class allows rewinding an inputStream by allowing a mark
    * to be set, and the stream reset to that position.  <strong>The
    * class assumes that it needs to read one character per
    * invocation when it's read() method is inovked, but uses the
    * underlying InputStream's read(char[], offset length) method--it
    * won't buffer data read this way!</strong>
    *
    * @xerces.internal
    *
    * @author Neil Graham, IBM
    * @author Glenn Marcy, IBM
    */

    protected final class RewindableInputStream extends InputStream {

        private InputStream fInputStream;
        private byte[] fData;
        private int fStartOffset;
        private int fEndOffset;
        private int fOffset;
        private int fLength;
        private int fMark;

        public RewindableInputStream(InputStream is) {
            fData = new byte[DEFAULT_XMLDECL_BUFFER_SIZE];
            fInputStream = is;
            fStartOffset = 0;
            fEndOffset = -1;
            fOffset = 0;
            fLength = 0;
            fMark = 0;
        }

        public void setStartOffset(int offset) {
            fStartOffset = offset;
        }

        public void rewind() {
            fOffset = fStartOffset;
        }

        public int readAndBuffer() throws IOException {
            if (fOffset == fData.length) {
                byte[] newData = new byte[fOffset << 1];
                System.arraycopy(fData, 0, newData, 0, fOffset);
                fData = newData;
            }
            final int b = fInputStream.read();
            if (b == -1) {
                fEndOffset = fOffset;
                return -1;
            }
            fData[fLength++] = (byte)b;
            fOffset++;
            return b & 0xff;
        }

        public int read() throws IOException {
            if (fOffset < fLength) {
                return fData[fOffset++] & 0xff;
            }
            if (fOffset == fEndOffset) {
                return -1;
            }
            if (fCurrentEntity.mayReadChunks) {
                return fInputStream.read();
            }
            return readAndBuffer();
        }

        public int read(byte[] b, int off, int len) throws IOException {
            final int bytesLeft = fLength - fOffset;
            if (bytesLeft == 0) {
                if (fOffset == fEndOffset) {
                    return -1;
                }

                // read a block of data as requested
                if(fCurrentEntity.mayReadChunks || !fCurrentEntity.xmlDeclChunkRead) {

                    if (!fCurrentEntity.xmlDeclChunkRead)
                    {
                        fCurrentEntity.xmlDeclChunkRead = true;
                        len = Entity.ScannedEntity.DEFAULT_XMLDECL_BUFFER_SIZE;
                    }
                    return fInputStream.read(b, off, len);
                }
                int returnedVal = readAndBuffer();
                if (returnedVal == -1) {
                    fEndOffset = fOffset;
                    return -1;
                }
                b[off] = (byte)returnedVal;
                return 1;
            }
            if (len < bytesLeft) {
                if (len <= 0) {
                    return 0;
                }
            } else {
                len = bytesLeft;
            }
            if (b != null) {
                System.arraycopy(fData, fOffset, b, off, len);
            }
            fOffset += len;
            return len;
        }

        public long skip(long n) throws IOException {
            int bytesLeft;
            if (n <= 0) {
                return 0;
            }
            bytesLeft = fLength - fOffset;
            if (bytesLeft == 0) {
                if (fOffset == fEndOffset) {
                    return 0;
                }
                return fInputStream.skip(n);
            }
            if (n <= bytesLeft) {
                fOffset += n;
                return n;
            }
            fOffset += bytesLeft;
            if (fOffset == fEndOffset) {
                return bytesLeft;
            }
            n -= bytesLeft;
           /*
            * In a manner of speaking, when this class isn't permitting more
            * than one byte at a time to be read, it is "blocking".  The
            * available() method should indicate how much can be read without
            * blocking, so while we're in this mode, it should only indicate
            * that bytes in its buffer are available; otherwise, the result of
            * available() on the underlying InputStream is appropriate.
            */
            return fInputStream.skip(n) + bytesLeft;
        }

        public int available() throws IOException {
            final int bytesLeft = fLength - fOffset;
            if (bytesLeft == 0) {
                if (fOffset == fEndOffset) {
                    return -1;
                }
                return fCurrentEntity.mayReadChunks ? fInputStream.available()
                                                    : 0;
            }
            return bytesLeft;
        }

        public void mark(int howMuch) {
            fMark = fOffset;
        }

        public void reset() {
            fOffset = fMark;
        }

        public boolean markSupported() {
            return true;
        }

        public void close() throws IOException {
            if (fInputStream != null) {
                fInputStream.close();
                fInputStream = null;
            }
        }
    } // end of RewindableInputStream class

    public void test(){
        //System.out.println("TESTING: Added familytree to entityManager");
        //Usecase1
        fEntityStorage.addExternalEntity("entityUsecase1",null,
                "/space/home/stax/sun/6thJan2004/zephyr/data/test.txt",
                "/space/home/stax/sun/6thJan2004/zephyr/data/entity.xml");

        //Usecase2
        fEntityStorage.addInternalEntity("entityUsecase2","<Test>value</Test>");
        fEntityStorage.addInternalEntity("entityUsecase3","value3");
        fEntityStorage.addInternalEntity("text", "Hello World.");
        fEntityStorage.addInternalEntity("empty-element", "<foo/>");
        fEntityStorage.addInternalEntity("balanced-element", "<foo></foo>");
        fEntityStorage.addInternalEntity("balanced-element-with-text", "<foo>Hello, World</foo>");
        fEntityStorage.addInternalEntity("balanced-element-with-entity", "<foo>&text;</foo>");
        fEntityStorage.addInternalEntity("unbalanced-entity", "<foo>");
        fEntityStorage.addInternalEntity("recursive-entity", "<foo>&recursive-entity2;</foo>");
        fEntityStorage.addInternalEntity("recursive-entity2", "<bar>&recursive-entity3;</bar>");
        fEntityStorage.addInternalEntity("recursive-entity3", "<baz>&recursive-entity;</baz>");
        fEntityStorage.addInternalEntity("ch","&#x00A9;");
        fEntityStorage.addInternalEntity("ch1","&#84;");
        fEntityStorage.addInternalEntity("% ch2","param");
    }

} // class XMLEntityManager
