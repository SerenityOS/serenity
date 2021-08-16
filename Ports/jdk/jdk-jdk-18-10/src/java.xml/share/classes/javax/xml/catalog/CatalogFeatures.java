/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package javax.xml.catalog;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import jdk.xml.internal.SecuritySupport;

/**
 * The CatalogFeatures holds a collection of features and properties.
 *
 *
 * <table class="plain" id="CatalogFeatures">
 * <caption>Catalog Features</caption>
 * <thead>
 * <tr>
 * <th scope="col" rowspan="2">Feature</th>
 * <th scope="col" rowspan="2">Description</th>
 * <th scope="col" rowspan="2">Property Name</th>
 * <th scope="col" rowspan="2">System Property [1]</th>
 * <th scope="col" rowspan="2">jaxp.properties [1]</th>
 * <th scope="col" colspan="2" style="text-align:center">Value [2]</th>
 * <th scope="col" rowspan="2">Action</th>
 * </tr>
 * <tr>
 * <th scope="col">Type</th>
 * <th scope="col">Value</th>
 * </tr>
 * </thead>
 *
 * <tbody>
 *
 * <tr>
 * <th scope="row" style="font-weight:normal" id="FILES">FILES</th>
 * <td>A semicolon-delimited list of URIs to locate the catalog files.
 * The URIs must be absolute and have a URL protocol handler for the URI scheme.
 * </td>
 * <td>javax.xml.catalog.files</td>
 * <td>javax.xml.catalog.files</td>
 * <td>javax.xml.catalog.files</td>
 * <td>String</td>
 * <th id="URIs" scope="row" style="font-weight:normal">URIs</th>
 * <td>
 * Reads the first catalog as the current catalog; Loads others if no match
 * is found in the current catalog including delegate catalogs if any.
 * </td>
 * </tr>
 *
 * <tr>
 * <th rowspan="2" scope="row" style="font-weight:normal" id="PREFER">PREFER</th>
 * <td rowspan="2">Indicates the preference between the public and system
 * identifiers. The default value is public [3].</td>
 * <td rowspan="2">javax.xml.catalog.prefer</td>
 * <td rowspan="2">N/A</td>
 * <td rowspan="2">N/A</td>
 * <td rowspan="2">String</td>
 * <th scope="row" id="system" style="font-weight:normal">{@code system}</th>
 * <td>
 * Searches system entries for a match; Searches public entries when
 * external identifier specifies only a public identifier</td>
 * </tr>
 * <tr>
 * <th scope="row" id="public" style="font-weight:normal">{@code public}</th>
 * <td>
 * Searches system entries for a match; Searches public entries when
 * there is no matching system entry.</td>
 * </tr>
 *
 * <tr>
 * <th rowspan="2" scope="row" style="font-weight:normal" id="DEFER">DEFER</th>
 * <td rowspan="2">Indicates that the alternative catalogs including those
 * specified in delegate entries or nextCatalog are not read until they are
 * needed. The default value is true.</td>
 * <td rowspan="2">javax.xml.catalog.defer [4]</td>
 * <td rowspan="2">javax.xml.catalog.defer</td>
 * <td rowspan="2">javax.xml.catalog.defer</td>
 * <td rowspan="2">String</td>
 * <th scope="row" id="true" style="font-weight:normal">{@code true}</th>
 * <td>
 * Loads alternative catalogs as needed.
 * </td>
 * </tr>
 * <tr>
 * <th scope="row" id="false" style="font-weight:normal">{@code false}</th>
 * <td>
 * Loads all catalogs[5]. </td>
 * </tr>
 *
 * <tr>
 * <th rowspan="3" scope="row" style="font-weight:normal" id="RESOLVE">RESOLVE</th>
 * <td rowspan="3">Determines the action if there is no matching entry found after
 * all of the specified catalogs are exhausted. The default is strict.</td>
 * <td rowspan="3">javax.xml.catalog.resolve [4]</td>
 * <td rowspan="3">javax.xml.catalog.resolve</td>
 * <td rowspan="3">javax.xml.catalog.resolve</td>
 * <td rowspan="3">String</td>
 * <th scope="row" id="strict" style="font-weight:normal">{@code strict}</th>
 * <td>
 * Throws CatalogException if there is no match.
 * </td>
 * </tr>
 * <tr>
 * <th scope="row" id="continue" style="font-weight:normal">{@code continue}</th>
 * <td>
 * Allows the XML parser to continue as if there is no match.
 * </td>
 * </tr>
 * <tr>
 * <th scope="row" id="ignore" style="font-weight:normal">{@code ignore}</th>
 * <td>
 * Tells the XML parser to skip the external references if there no match.
 * </td>
 * </tr>
 *
 * </tbody>
 * </table>
 * <p>
 * <b>[1]</b> There is no System property for the features that marked as "N/A".
 *
 * <p>
 * <b>[2]</b> The value shall be exactly as listed in this table, case-sensitive.
 * Any unspecified value will result in {@link IllegalArgumentException}.
 * <p>
 * <b>[3]</b> The Catalog specification defined complex rules on
 * <a href="https://www.oasis-open.org/committees/download.php/14809/xml-catalogs.html#attrib.prefer">
 * the prefer attribute</a>. Although the prefer can be public or system, the
 * specification actually made system the preferred option, that is, no matter
 * the option, a system entry is always used if found. Public entries are only
 * considered if the prefer is public and system entries are not found. It is
 * therefore recommended that the prefer attribute be set as public
 * (which is the default).
 * <p>
 * <b>[4]</b> Although non-standard attributes in the OASIS Catalog specification,
 * {@code defer} and {@code resolve} are recognized by the Java Catalog API the
 * same as the {@code prefer} as being an attribute in the catalog entry of the
 * main catalog. Note that only the attributes specified for the catalog entry
 * of the main Catalog file will be used.
  * <p>
 * <b>[5]</b> If the intention is to share an entire catalog store, it may be desirable to
 * set the property {@code javax.xml.catalog.defer} to false to allow the entire
 * catalog to be pre-loaded.
 *
 * <h2>Scope and Order</h2>
 * Features and properties can be set through the catalog file, the Catalog API,
 * system properties, and {@code jaxp.properties}, with a preference in the same order.
 * <p>
 * Properties that are specified as attributes in the catalog file for the
 * catalog and group entries shall take preference over any of the other settings.
 * For example, if a {@code prefer} attribute is set in the catalog file as in
 * {@code <catalog prefer="public">}, any other input for the "prefer" property
 * is not necessary or will be ignored.
 * <p>
 * Properties set through the Catalog API override those that may have been set
 * by system properties and/or in {@code jaxp.properties}. In case of multiple
 * interfaces, the latest in a procedure shall take preference. For
 * {@link Feature#FILES}, this means that the URI(s) specified through the methods
 * of the {@link CatalogManager} will override any that may have been entered
 * through the {@link Builder}.
 *
 * <p>
 * System properties when set shall override those in {@code jaxp.properties}.
 * <p>
 * The {@code jaxp.properties} file is typically in the conf directory of the Java
 * installation. The file is read only once by the JAXP implementation and
 * its values are then cached for future use. If the file does not exist
 * when the first attempt is made to read from it, no further attempts are
 * made to check for its existence. It is not possible to change the value
 * of any properties in {@code jaxp.properties} after it has been read.
 * <p>
 * A CatalogFeatures instance can be created through its builder as illustrated
 * in the following sample code:
 * <pre>{@code
                CatalogFeatures f = CatalogFeatures.builder()
                        .with(Feature.FILES, "file:///etc/xml/catalog")
                        .with(Feature.PREFER, "public")
                        .with(Feature.DEFER, "true")
                        .with(Feature.RESOLVE, "ignore")
                        .build();
 * }</pre>
 *
 * <h2>JAXP XML Processor Support</h2>
 * The Catalog Features are supported throughout the JAXP processors, including
 * SAX and DOM ({@link javax.xml.parsers}), and StAX parsers ({@link javax.xml.stream}),
 * Schema Validation ({@link javax.xml.validation}), and XML Transformation
 * ({@link javax.xml.transform}). The features described above can be set through JAXP
 * factories or processors that define a setProperty or setAttribute interface.
 * For example, the following code snippet sets a URI to a catalog file on a SAX
 * parser through the {@code javax.xml.catalog.files} property:
 *
 * <pre>{@code
 *      SAXParserFactory spf = SAXParserFactory.newInstance();
 *      spf.setFeature(XMLConstants.USE_CATALOG, true); [1]
 *      SAXParser parser = spf.newSAXParser();
 *      parser.setProperty(CatalogFeatures.Feature.FILES.getPropertyName(), "file:///etc/xml/catalog");
 * }</pre>
 * <p>
 * [1] Note that this statement is not required since the default value of
 * {@link javax.xml.XMLConstants#USE_CATALOG USE_CATALOG} is true.
 *
 * <p>
 * The JAXP Processors' support for Catalog depends on both the
 * {@link javax.xml.XMLConstants#USE_CATALOG USE_CATALOG} feature and the
 * existence of valid Catalog file(s). A JAXP processor will use the Catalog
 * only when the feature is true and valid Catalog file(s) are specified through
 * the {@code javax.xml.catalog.files} property. It will make no attempt to use
 * the Catalog if either {@link javax.xml.XMLConstants#USE_CATALOG USE_CATALOG}
 * is set to false, or there is no Catalog file specified.
 *
 * <p>
 * The JAXP processors will observe the default settings of the
 * {@link javax.xml.catalog.CatalogFeatures}. The processors, for example, will
 * report an Exception by default when no matching entry is found since the
 * default value of the {@code javax.xml.catalog.resolve} property is strict.
 *
 * <p>
 * The JAXP processors give preference to user-specified custom resolvers. If such
 * a resolver is registered, it will be used over the CatalogResolver. If it returns
 * null however, the processors will continue resolving with the CatalogResolver.
 * If it returns an empty source, no attempt will be made by the CatalogResolver.
 *
 * <p>
 * The Catalog support is available for any process in the JAXP library that
 * supports a resolver. The following table lists all such processes.
 *
 * <h2><a id="ProcessesWithCatalogSupport">Processes with Catalog Support</a></h2>
 *
 * <table class="striped">
 * <caption>Processes with Catalog Support</caption>
 * <thead>
 * <tr>
 * <th scope="col">Process</th>
 * <th scope="col">Catalog Entry Type</th>
 * <th scope="col">Example</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <th scope="row">DTDs and external entities</th>
 * <td>public, system</td>
 * <td>
 * <pre>{@literal
   The following DTD reference:
   <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

   Can be resolved using the following Catalog entry:
   <public publicId="-//W3C//DTD XHTML 1.0 Strict//EN" uri="catalog/xhtml1-strict.dtd"/>
   or
   <systemSuffix systemIdSuffix="html1-strict.dtd" uri="catalog/xhtml1-strict.dtd"/>
 * }</pre>
 * </td>
 * </tr>
 * <tr>
 * <th scope="row">XInclude</th>
 * <td>uri</td>
 * <td>
 * <pre>{@literal
   The following XInclude element:
   <xi:include href="http://openjdk.java.net/xml/disclaimer.xml"/>

   can be resolved using a URI entry:
   <uri name="http://openjdk.java.net/xml/disclaimer.xml" uri="file:///pathto/local/disclaimer.xml"/>
   or
   <uriSuffix uriSuffix="disclaimer.xml" uri="file:///pathto/local/disclaimer.xml"/>
 * }</pre>
 * </td>
 * </tr>
 * <tr>
 * <th scope="row">XSD import</th>
 * <td>uri</td>
 * <td>
 * <pre>{@literal
   The following import element:
    <xsd:import namespace="http://openjdk.java.net/xsd/XSDImport_person"
                schemaLocation="http://openjdk.java.net/xsd/XSDImport_person.xsd"/>

   can be resolved using a URI entry:
   <uri name="http://openjdk.java.net/xsd/XSDImport_person.xsd" uri="file:///pathto/local/XSDImport_person.xsd"/>
   or
   <uriSuffix uriSuffix="XSDImport_person.xsd" uri="file:///pathto/local/XSDImport_person.xsd"/>
   or
   <uriSuffix uriSuffix="http://openjdk.java.net/xsd/XSDImport_person" uri="file:///pathto/local/XSDImport_person.xsd"/>
 * }</pre>
 * </td>
 * </tr>
 * <tr>
 * <th scope="row">XSD include</th>
 * <td>uri</td>
 * <td>
 * <pre>{@literal
   The following include element:
   <xsd:include schemaLocation="http://openjdk.java.net/xsd/XSDInclude_person.xsd"/>

   can be resolved using a URI entry:
   <uri name="http://openjdk.java.net/xsd/XSDInclude_person.xsd" uri="file:///pathto/local/XSDInclude_person.xsd"/>
   or
   <uriSuffix uriSuffix="XSDInclude_person.xsd" uri="file:///pathto/local/XSDInclude_person.xsd"/>
 * }</pre>
 * </td>
 * </tr>
 * <tr>
 * <th scope="row">XSL import and include</th>
 * <td>uri</td>
 * <td>
 * <pre>{@literal
   The following include element:
   <xsl:include href="http://openjdk.java.net/xsl/include.xsl"/>

   can be resolved using a URI entry:
   <uri name="http://openjdk.java.net/xsl/include.xsl" uri="file:///pathto/local/include.xsl"/>
   or
   <uriSuffix uriSuffix="include.xsl" uri="file:///pathto/local/include.xsl"/>
 * }</pre>
 * </td>
 * </tr>
 * <tr>
 * <th scope="row">XSL document function</th>
 * <td>uri</td>
 * <td>
 * <pre>{@literal
   The document in the following element:
   <xsl:variable name="dummy" select="document('http://openjdk.java.net/xsl/list.xml')"/>

   can be resolved using a URI entry:
   <uri name="http://openjdk.java.net/xsl/list.xml" uri="file:///pathto/local/list.xml"/>
   or
   <uriSuffix uriSuffix="list.xml" uri="file:///pathto/local/list.xml"/>
 * }</pre>
 * </td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @since 9
 */
public class CatalogFeatures {

    /**
     * The constant name of the javax.xml.catalog.files property as described
     * in the property table above.
     */
    static final String CATALOG_FILES = "javax.xml.catalog.files";

    /**
     * The javax.xml.catalog.prefer property as described
     * in the property table above.
     */
    static final String CATALOG_PREFER = "javax.xml.catalog.prefer";

    /**
     * The javax.xml.catalog.defer property as described
     * in the property table above.
     */
    static final String CATALOG_DEFER = "javax.xml.catalog.defer";

    /**
     * The javax.xml.catalog.resolve property as described
     * in the property table above.
     */
    static final String CATALOG_RESOLVE = "javax.xml.catalog.resolve";

    //values for the prefer property
    static final String PREFER_SYSTEM = "system";
    static final String PREFER_PUBLIC = "public";

    //values for the defer property
    static final String DEFER_TRUE = "true";
    static final String DEFER_FALSE = "false";

    //values for the Resolve property
    static final String RESOLVE_STRICT = "strict";
    static final String RESOLVE_CONTINUE = "continue";
    static final String RESOLVE_IGNORE = "ignore";

    /**
     * A Feature type as defined in the
     * <a href="CatalogFeatures.html#CatalogFeatures">Catalog Features table</a>.
     */
    public static enum Feature {
        /**
         * The {@code javax.xml.catalog.files} property as described in
         * item <a href="CatalogFeatures.html#FILES">FILES</a> of the
         * Catalog Features table.
         */
        FILES(CATALOG_FILES, null, true),
        /**
         * The {@code javax.xml.catalog.prefer} property as described in
         * item <a href="CatalogFeatures.html#PREFER">PREFER</a> of the
         * Catalog Features table.
         */
        PREFER(CATALOG_PREFER, PREFER_PUBLIC, false),
        /**
         * The {@code javax.xml.catalog.defer} property as described in
         * item <a href="CatalogFeatures.html#DEFER">DEFER</a> of the
         * Catalog Features table.
         */
        DEFER(CATALOG_DEFER, DEFER_TRUE, true),
        /**
         * The {@code javax.xml.catalog.resolve} property as described in
         * item <a href="CatalogFeatures.html#RESOLVE">RESOLVE</a> of the
         * Catalog Features table.
         */
        RESOLVE(CATALOG_RESOLVE, RESOLVE_STRICT, true);

        private final String name;
        private final String defaultValue;
        private String value;
        private final boolean hasSystem;

        /**
         * Constructs a CatalogFeature instance.
         * @param name the name of the feature
         * @param value the value of the feature
         * @param hasSystem a flag to indicate whether the feature is supported
         * with a System property
         */
        Feature(String name, String value, boolean hasSystem) {
            this.name = name;
            this.defaultValue = value;
            this.hasSystem = hasSystem;
        }

        /**
         * Checks whether the specified property is equal to the current property.
         * @param propertyName the name of a property
         * @return true if the specified property is the current property, false
         * otherwise
         */
        boolean equalsPropertyName(String propertyName) {
            return name.equals(propertyName);
        }

        /**
         * Returns the name of the corresponding System Property.
         *
         * @return the name of the System Property
         */
        public String getPropertyName() {
            return name;
        }

        /**
         * Returns the default value of the property.
         * @return the default value of the property
         */
        public String defaultValue() {
            return defaultValue;
        }

        /**
         * Returns the value of the property.
         * @return the value of the property
         */
        String getValue() {
            return value;
        }

        /**
         * Checks whether System property is supported for the feature.
         * @return true it is supported, false otherwise
         */
        boolean hasSystemProperty() {
            return hasSystem;
        }
    }

    /**
     * States of the settings of a property, in the order: default value,
     * jaxp.properties file, jaxp system properties, and jaxp api properties
     */
    static enum State {
        /** represents the default state of a feature. */
        DEFAULT("default"),
        /** indicates the value of the feature is read from jaxp.properties. */
        JAXPDOTPROPERTIES("jaxp.properties"),
        /** indicates the value of the feature is read from its System property. */
        SYSTEMPROPERTY("system property"),
        /** indicates the value of the feature is specified through the API. */
        APIPROPERTY("property"),
        /** indicates the value of the feature is specified as a catalog attribute. */
        CATALOGATTRIBUTE("catalog attribute");

        final String literal;

        State(String literal) {
            this.literal = literal;
        }

        String literal() {
            return literal;
        }
    }

    /**
     * Values of the properties
     */
    private String[] values;

    /**
     * States of the settings for each property
     */
    private State[] states;

    /**
     * Private class constructor
     */
    private CatalogFeatures() {
    }

    /**
     * Returns a CatalogFeatures instance with default settings.
     * @return a default CatalogFeatures instance
     */
    public static CatalogFeatures defaults() {
        return CatalogFeatures.builder().build();
    }

    /**
     * Constructs a new CatalogFeatures instance with the builder.
     *
     * @param builder the builder to build the CatalogFeatures
     */
    CatalogFeatures(Builder builder) {
        init();
        setProperties(builder);
    }

    /**
     * Returns the value of the specified feature.
     *
     * @param cf the type of the Catalog feature
     * @return the value of the feature
     */
    public String get(Feature cf) {
        return values[cf.ordinal()];
    }

    /**
     * Initializes the supported properties
     */
    private void init() {
        values = new String[Feature.values().length];
        states = new State[Feature.values().length];
        for (Feature cf : Feature.values()) {
            setProperty(cf, State.DEFAULT, cf.defaultValue());
        }
        //read system properties or jaxp.properties
        readSystemProperties();
    }

    /**
     * Sets properties by the Builder.
     * @param builder the CatalogFeatures builder
     */
    private void setProperties(Builder builder) {
        builder.values.entrySet().stream().forEach((entry) -> {
            setProperty(entry.getKey(), State.APIPROPERTY, entry.getValue());
        });
    }
    /**
     * Sets the value of a property, updates only if it shall override.
     *
     * @param index the index of the property
     * @param state the state of the property
     * @param value the value of the property
     * @throws IllegalArgumentException if the value is invalid
     */
    private void setProperty(Feature feature, State state, String value) {
        int index = feature.ordinal();
        if (value != null && value.length() != 0) {
            if (state != State.APIPROPERTY) {
                Util.validateFeatureInput(feature, value);
            }
            if (states[index] == null || state.compareTo(states[index]) >= 0) {
                values[index] = value;
                states[index] = state;
            }
        }
    }

    /**
     * Reads from system properties, or those in jaxp.properties
     */
    private void readSystemProperties() {
        for (Feature cf : Feature.values()) {
            getSystemProperty(cf, cf.getPropertyName());
        }
    }

    /**
     * Reads from system properties, or those in jaxp.properties
     *
     * @param cf the type of the property
     * @param sysPropertyName the name of system property
     */
    private boolean getSystemProperty(Feature cf, String sysPropertyName) {
        if (cf.hasSystemProperty()) {
            String value = SecuritySupport.getSystemProperty(sysPropertyName);
            if (value != null && !value.isEmpty()) {
                setProperty(cf, State.SYSTEMPROPERTY, value);
                return true;
            }

            value = SecuritySupport.readJAXPProperty(sysPropertyName);
            if (value != null && !value.isEmpty()) {
                setProperty(cf, State.JAXPDOTPROPERTIES, value);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an instance of the builder for creating the CatalogFeatures object.
     *
     * @return an instance of the builder
     */
    public static Builder builder() {
        return new CatalogFeatures.Builder();
    }

    /**
     * The Builder class for building the CatalogFeatures object.
     */
    public static class Builder {
        /**
         * Values of the features supported by CatalogFeatures.
         */
        Map<Feature, String> values = new HashMap<>();

        /**
         * Instantiation of Builder is not allowed.
         */
        private Builder() {}

        /**
         * Sets the value to a specified Feature.
         * @param feature the Feature to be set
         * @param value the value to be set for the Feature
         * @return this Builder instance
         * @throws IllegalArgumentException if the value is not valid for the
         * Feature or has the wrong syntax for the {@code javax.xml.catalog.files}
         * property
         */
        public Builder with(Feature feature, String value) {
            Util.validateFeatureInput(feature, value);
            values.put(feature, value);
            return this;
        }

        /**
         * Returns a CatalogFeatures object built by this builder.
         *
         * @return an instance of CatalogFeatures
         */
        public CatalogFeatures build() {
            return new CatalogFeatures(this);
        }
    }
}
