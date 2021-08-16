/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.xml.internal;

/**
 * This class holds constants shared across XML components. Historically, there
 * had been a component boundary within which some constants were duplicated for
 * each component, such as Xerces and Xalan.
 */
public final class JdkConstants {

    //
    // Constants
    //
    //Xerces security manager
    public static final String SECURITY_MANAGER =
            "http://apache.org/xml/properties/security-manager";

    //
    // Implementation limits: API properties
    //

    /**
     * Oracle JAXP property prefix.
     *
     * @deprecated Use {@code jdk.xml.} instead. Refer to specifications in
     * the module summary.
     */
    @Deprecated (since="17")
    public static final String ORACLE_JAXP_PROPERTY_PREFIX =
        "http://www.oracle.com/xml/jaxp/properties/";

    /**
     * JDK entity expansion limit. Note that the existing system property
     * "entityExpansionLimit" with no prefix is still observed.
     *
     * @deprecated Use {@link #SP_ENTITY_EXPANSION_LIMIT} instead.
     */
    @Deprecated (since="17")
    public static final String JDK_ENTITY_EXPANSION_LIMIT =
            ORACLE_JAXP_PROPERTY_PREFIX + "entityExpansionLimit";

    /**
     * JDK element attribute limit. Note that the existing system property
     * "elementAttributeLimit" with no prefix is still observed.
     *
     * @deprecated Use {@link #SP_ELEMENT_ATTRIBUTE_LIMIT} instead.
     */
    @Deprecated (since="17")
    public static final String JDK_ELEMENT_ATTRIBUTE_LIMIT =
            ORACLE_JAXP_PROPERTY_PREFIX + "elementAttributeLimit";

    /**
     * JDK maxOccur limit. Note that the existing system property
     * "maxOccurLimit" with no prefix is still observed
     *
     * @deprecated Use {@link #SP_ENTITY_EXPANSION_LIMIT} instead.
     */
    @Deprecated (since="17")
    public static final String JDK_MAX_OCCUR_LIMIT =
            ORACLE_JAXP_PROPERTY_PREFIX + "maxOccurLimit";

    /**
     * JDK total entity size limit.
     *
     * @deprecated Use {@link #SP_TOTAL_ENTITY_SIZE_LIMIT} instead.
     */
    @Deprecated (since="17")
    public static final String JDK_TOTAL_ENTITY_SIZE_LIMIT =
            ORACLE_JAXP_PROPERTY_PREFIX + "totalEntitySizeLimit";

    /**
     * JDK maximum general entity size limit.
     *
     * @deprecated Use {@link #SP_GENERAL_ENTITY_SIZE_LIMIT} instead.
     */
    @Deprecated (since="17")
    public static final String JDK_GENERAL_ENTITY_SIZE_LIMIT =
            ORACLE_JAXP_PROPERTY_PREFIX + "maxGeneralEntitySizeLimit";

    /**
     * JDK node count limit in entities that limits the total number of nodes
     * in all of entity references.
     *
     * @deprecated Use {@link #SP_ENTITY_REPLACEMENT_LIMIT} instead.
     */
    @Deprecated (since="17")
    public static final String JDK_ENTITY_REPLACEMENT_LIMIT =
            ORACLE_JAXP_PROPERTY_PREFIX + "entityReplacementLimit";

    /**
     * JDK maximum parameter entity size limit.
     *
     * @deprecated Use {@link #SP_PARAMETER_ENTITY_SIZE_LIMIT} instead.
     */
    @Deprecated (since="17")
    public static final String JDK_PARAMETER_ENTITY_SIZE_LIMIT =
            ORACLE_JAXP_PROPERTY_PREFIX + "maxParameterEntitySizeLimit";
    /**
     * JDK maximum XML name limit.
     *
     * @deprecated Use {@link #SP_XML_NAME_LIMIT} instead.
     */
    @Deprecated (since="17")
    public static final String JDK_XML_NAME_LIMIT =
            ORACLE_JAXP_PROPERTY_PREFIX + "maxXMLNameLimit";

    /**
     * JDK maxElementDepth limit.
     *
     * @deprecated Use {@link #SP_MAX_ELEMENT_DEPTH} instead.
     */
    @Deprecated (since="17")
    public static final String JDK_MAX_ELEMENT_DEPTH =
            ORACLE_JAXP_PROPERTY_PREFIX + "maxElementDepth";

    /**
     * JDK property indicating whether the parser shall print out entity
     * count information.
     * Value: a string "yes" means print, "no" or any other string means not.
     *
     * @deprecated Use {@link #JDK_DEBUG_LIMIT} instead.
     */
    @Deprecated (since="17")
    public static final String JDK_ENTITY_COUNT_INFO =
            ORACLE_JAXP_PROPERTY_PREFIX + "getEntityCountInfo";

    public static final String JDK_DEBUG_LIMIT = "jdk.xml.getEntityCountInfo";

    //
    // Implementation limits: corresponding System Properties of the above
    // API properties.
    //
    // Note: as of JDK 17, properties and System properties now share the same
    // name with a prefix "jdk.xml.".
    //
    /**
     * JDK entity expansion limit; Note that the existing system property
     * "entityExpansionLimit" with no prefix is still observed
     */
    public static final String SP_ENTITY_EXPANSION_LIMIT = "jdk.xml.entityExpansionLimit";

    /**
     * JDK element attribute limit; Note that the existing system property
     * "elementAttributeLimit" with no prefix is still observed
     */
    public static final String SP_ELEMENT_ATTRIBUTE_LIMIT =  "jdk.xml.elementAttributeLimit";

    /**
     * JDK maxOccur limit; Note that the existing system property
     * "maxOccurLimit" with no prefix is still observed
     */
    public static final String SP_MAX_OCCUR_LIMIT = "jdk.xml.maxOccurLimit";

    /**
     * JDK total entity size limit
     */
    public static final String SP_TOTAL_ENTITY_SIZE_LIMIT = "jdk.xml.totalEntitySizeLimit";

    /**
     * JDK maximum general entity size limit
     */
    public static final String SP_GENERAL_ENTITY_SIZE_LIMIT = "jdk.xml.maxGeneralEntitySizeLimit";

    /**
     * JDK node count limit in entities that limits the total number of nodes
     * in all of entity references.
     */
    public static final String SP_ENTITY_REPLACEMENT_LIMIT = "jdk.xml.entityReplacementLimit";

    /**
     * JDK maximum parameter entity size limit
     */
    public static final String SP_PARAMETER_ENTITY_SIZE_LIMIT = "jdk.xml.maxParameterEntitySizeLimit";
    /**
     * JDK maximum XML name limit
     */
    public static final String SP_XML_NAME_LIMIT = "jdk.xml.maxXMLNameLimit";

    /**
     * JDK maxElementDepth limit
     */
    public static final String SP_MAX_ELEMENT_DEPTH = "jdk.xml.maxElementDepth";

    /**
     * JDK TransformerFactory and Transformer attribute that specifies a class
     * loader that will be used for extension functions class loading
     * Value: a "null", the default value, means that the default EF class loading
     * path will be used.
     * Instance of ClassLoader: the specified instance of ClassLoader will be used
     * for extension functions loading during translation process
     */
    public static final String JDK_EXTENSION_CLASSLOADER = "jdk.xml.transform.extensionClassLoader";
    // spec-compatible name with a prefix "jdk.xml"
    public static final String JDK_EXT_CLASSLOADER = "jdk.xml.extensionClassLoader";

    //legacy System Properties
    public final static String ENTITY_EXPANSION_LIMIT = "entityExpansionLimit";
    public static final String ELEMENT_ATTRIBUTE_LIMIT = "elementAttributeLimit" ;
    public final static String MAX_OCCUR_LIMIT = "maxOccurLimit";

    /**
     * A string "yes" that can be used for properties such as getEntityCountInfo
     */
    public static final String JDK_YES = "yes";

    // Oracle Feature:
    /**
     * <p>Use Service Mechanism</p>
     *
     * <ul>
     *   <li>
     * {@code true} instruct an object to use service mechanism to
     * find a service implementation. This is the default behavior.
     *   </li>
     *   <li>
     * {@code false} instruct an object to skip service mechanism and
     * use the default implementation for that service.
     *   </li>
     * </ul>
     * @deprecated Use {@link jdk.xml.internal.JdkXmlUtils#OVERRIDE_PARSER} instead.
     */
    @Deprecated (since="17")
    public static final String ORACLE_FEATURE_SERVICE_MECHANISM =
            "http://www.oracle.com/feature/use-service-mechanism";

    //System Properties corresponding to ACCESS_EXTERNAL_* properties
    public static final String SP_ACCESS_EXTERNAL_STYLESHEET = "javax.xml.accessExternalStylesheet";
    public static final String SP_ACCESS_EXTERNAL_DTD = "javax.xml.accessExternalDTD";
    public static final String SP_ACCESS_EXTERNAL_SCHEMA = "javax.xml.accessExternalSchema";

    //all access keyword
    public static final String ACCESS_EXTERNAL_ALL = "all";

    /**
     * Default value when FEATURE_SECURE_PROCESSING (FSP) is set to true
     */
    public static final String EXTERNAL_ACCESS_DEFAULT_FSP = "";

    /**
     * FEATURE_SECURE_PROCESSING (FSP) is false by default
     */
    public static final String EXTERNAL_ACCESS_DEFAULT = ACCESS_EXTERNAL_ALL;

    public static final String XML_SECURITY_PROPERTY_MANAGER =
            "jdk.xml.xmlSecurityPropertyManager";

    /**
     * Values for a feature
     */
    public static final String FEATURE_TRUE = "true";
    public static final String FEATURE_FALSE = "false";

    /**
     * For DOM Serializer.
     *
     * Indicates that the serializer should treat the output as a standalone document.
     * The JDK specific standalone property controls whether a newline should be
     * added after the XML header.
     *
     * @see similar property xsltcIsStandalone for XSLTC.
     */
    public static final String S_IS_STANDALONE = "isStandalone";

    /**
     * Fully-qualified property name with the JDK Impl prefix.
     *
     * @deprecated Use {@link #SP_IS_STANDALONE} instead.
     */
    @Deprecated (since="17")
    public static final String FQ_IS_STANDALONE = ORACLE_JAXP_PROPERTY_PREFIX + S_IS_STANDALONE;

    // Corresponding System property
    public static final String SP_IS_STANDALONE = "jdk.xml.isStandalone";

    /**
     * For XSLTC.
     *
     * Instructs the processor to act as if OutputKeys.STANDALONE is specified
     * but without writing it out in the declaration.
     * This property may be used to mitigate the effect of Xalan patch 1495 that
     * has caused incompatible behaviors.
     */
    /**
     * <p>Is Standalone</p>
     *
     * <ul>
     *   <li>
     *     <code>yes</code> to indicate the output is intended to be used as standalone
     *   </li>
     *   <li>
     *     <code>no</code> has no effect.
     *   </li>
     * </ul>
     *
     * @deprecated Use {@link #SP_XSLTC_IS_STANDALONE} instead.
     */
    @Deprecated (since="17")
    public static final String ORACLE_IS_STANDALONE = "http://www.oracle.com/xml/is-standalone";

    /**
     * This property was added to align with those that used ORACLE_JAXP_PROPERTY_PREFIX
     * as prefix.
     * @deprecated Use {@link #SP_XSLTC_IS_STANDALONE} instead.
     */
    @Deprecated (since="17")
    public static final String JDK_IS_STANDALONE = ORACLE_JAXP_PROPERTY_PREFIX +
            "xsltcIsStandalone";

    // Corresponding System property
    public static final String SP_XSLTC_IS_STANDALONE = "jdk.xml.xsltcIsStandalone";

    /**
     * Feature enableExtensionFunctions
     */
    public static final String ORACLE_ENABLE_EXTENSION_FUNCTION =
            ORACLE_JAXP_PROPERTY_PREFIX + "enableExtensionFunctions";
    public static final String SP_ENABLE_EXTENSION_FUNCTION =
            "javax.xml.enableExtensionFunctions";
    // This is the correct name by the spec
    public static final String SP_ENABLE_EXTENSION_FUNCTION_SPEC =
            "jdk.xml.enableExtensionFunctions";

    /**
     * Reset SymbolTable feature System property name is identical to feature
     * name
     */
    public final static String RESET_SYMBOL_TABLE = "jdk.xml.resetSymbolTable";
    /**
     * Default value of RESET_SYMBOL_TABLE. This will read the System property
     */
    public static final boolean RESET_SYMBOL_TABLE_DEFAULT
            = SecuritySupport.getJAXPSystemProperty(Boolean.class, RESET_SYMBOL_TABLE, "false");

    /**
     * jdk.xml.overrideDefaultParser: enables the use of a 3rd party's parser
     * implementation to override the system-default parser.
     */
    public static final String OVERRIDE_PARSER = "jdk.xml.overrideDefaultParser";
    public static final boolean OVERRIDE_PARSER_DEFAULT = SecuritySupport.getJAXPSystemProperty(
                    Boolean.class, OVERRIDE_PARSER, "false");

    /**
     * instructs the parser to return the data in a CData section in a single chunk
     * when the property is zero or unspecified, or in multiple chunks when it is
     * greater than zero. The parser shall split the data by linebreaks, and any
     * chunks that are larger than the specified size to ones that are equal to
     * or smaller than the size.
     */
    public final static String CDATA_CHUNK_SIZE = "jdk.xml.cdataChunkSize";
    public static final int CDATA_CHUNK_SIZE_DEFAULT
            = SecuritySupport.getJAXPSystemProperty(Integer.class, CDATA_CHUNK_SIZE, "0");

}
