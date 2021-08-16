/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import static jdk.xml.internal.JdkConstants.FQ_IS_STANDALONE;
import static jdk.xml.internal.JdkConstants.JDK_DEBUG_LIMIT;
import static jdk.xml.internal.JdkConstants.JDK_ENTITY_COUNT_INFO;
import static jdk.xml.internal.JdkConstants.JDK_EXTENSION_CLASSLOADER;
import static jdk.xml.internal.JdkConstants.JDK_EXT_CLASSLOADER;
import static jdk.xml.internal.JdkConstants.JDK_IS_STANDALONE;
import static jdk.xml.internal.JdkConstants.ORACLE_IS_STANDALONE;
import static jdk.xml.internal.JdkConstants.SP_IS_STANDALONE;
import static jdk.xml.internal.JdkConstants.SP_XSLTC_IS_STANDALONE;
import static jdk.xml.internal.JdkConstants.ORACLE_ENABLE_EXTENSION_FUNCTION;
import static jdk.xml.internal.JdkConstants.ORACLE_FEATURE_SERVICE_MECHANISM;
import static jdk.xml.internal.JdkConstants.SP_ENABLE_EXTENSION_FUNCTION;
import static jdk.xml.internal.JdkConstants.SP_ENABLE_EXTENSION_FUNCTION_SPEC;
import static jdk.xml.internal.JdkConstants.CDATA_CHUNK_SIZE;
import static jdk.xml.internal.JdkConstants.OVERRIDE_PARSER;
import static jdk.xml.internal.JdkConstants.RESET_SYMBOL_TABLE;

/**
 * Represents a JDK Implementation Specific Property. This class holds the name
 * and value of a property along with a state indicating the means through which
 * the property has been set. The value may change only if the setter has a state
 * that represents an equal or higher overriding order.
 *
 * @param <T> the type of the property value.
 */
public final class JdkProperty<T> {

    private ImplPropMap pName;
    private T pValue;
    private State pState = State.DEFAULT;

    /**
     * Constructs a JDkProperty.
     * @param name the name of the property
     * @param value the initial value
     * @param state the state of the property
     */
    public JdkProperty(ImplPropMap name, T value, State state) {
        this.pName = name;
        this.pValue = value;
        this.pState = state;
    }

    /**
     * Returns the property value.
     * @return the property value
     */
    public T getValue() {
        return pValue;
    }

    /**
     * Sets the property value. The value is set only if the setter has a higher
     * overriding order.
     * @param name the property name
     * @param value the value
     * @param state the state of the specified property
     * @return true if the value is set successfully (because the setter has a
     * higher order); false otherwise.
     */
    public boolean setValue(String name, T value, State state) {
        State pState1;
        if ((pState1 = pName.getState(name)) != null) {
            if (pState1.compareTo(this.pState) >= 0) {
                this.pState = pState1;
                pValue = value;
                return true;
            }
        }
        return false;
    }

    /**
     * Properties Name Map that includes Implementation-Specific Features and
     * Properties except the limits that are defined in XMLSecurityManager.
     * The purpose of the map is to provide a map between the new property names
     * with a prefix "jdk.xml" as defined in the module summary and legacy names
     * with URL style prefixes. The new names are the same as those of their
     * System Properties.
     */
    @SuppressWarnings("deprecation")
    public static enum ImplPropMap {

        ISSTANDALONE("isStandalone", FQ_IS_STANDALONE, SP_IS_STANDALONE, true, null, null),
        XSLTCISSTANDALONE("xsltcIsStandalone", JDK_IS_STANDALONE, SP_XSLTC_IS_STANDALONE,
            true, ORACLE_IS_STANDALONE, null),
        CDATACHUNKSIZE("cdataChunkSize", CDATA_CHUNK_SIZE, CDATA_CHUNK_SIZE, false, null, null),
        EXTCLSLOADER("extensionClassLoader", JDK_EXT_CLASSLOADER, null,
            true, JDK_EXTENSION_CLASSLOADER, null),
        ENABLEEXTFUNC("enableExtensionFunctions", ORACLE_ENABLE_EXTENSION_FUNCTION,
            SP_ENABLE_EXTENSION_FUNCTION_SPEC, true, null, SP_ENABLE_EXTENSION_FUNCTION),
        OVERRIDEPARSER("overrideDefaultParser", OVERRIDE_PARSER, OVERRIDE_PARSER,
            false, ORACLE_FEATURE_SERVICE_MECHANISM, ORACLE_FEATURE_SERVICE_MECHANISM),
        RESETSYMBOLTABLE("resetSymbolTable", RESET_SYMBOL_TABLE, RESET_SYMBOL_TABLE,
            false, null, null),
        ENTITYCOUNT("getEntityCountInfo", JDK_DEBUG_LIMIT, null, true, JDK_ENTITY_COUNT_INFO, null)
        ;

        private final String name;
        private final String qName;
        private final String spName;
        private final boolean differ;
        private final String oldQName;
        private final String oldSPName;

        /**
         * Constructs an instance.
         * @param name the property name
         * @param qName the qualified property name
         * @param spName the corresponding System Property
         * @param differ a flag indicating whether qName and spName are the same
         * @param oldName the legacy property name, null if N/A
         * @param oldSPName the legacy System Property name, null if N/A
         */
        ImplPropMap(String name, String qName, String spName, boolean differ,
                String oldQName, String oldSPName) {
            this.name = name;
            this.qName = qName;
            this.spName = spName;
            this.differ = differ;
            this.oldQName = oldQName;
            this.oldSPName = oldSPName;
        }

        /**
         * Checks whether the specified name is the property. Checks both the
         * property and System Property if they differ. Checks also the legacy
         * name if applicable.
         *
         * @param name the specified name
         * @return true if there is a match, false otherwise
         */
        public boolean is(String name) {
            // current spec calls for using a name same as spName
            return (spName != null && spName.equals(name)) ||
                   // check qName only if it differs from spName
                   (differ && qName.equals(name)) ||
                   // check the legacy name if applicable
                   (oldQName != null && oldQName.equals(name));
        }

        /**
         * Returns the value indicating whether the qName and spName are different.
         * @return the value indicating whether the qName and spName are different
         */
        public boolean isNameDiffer() {
            return differ;
        }

        /**
         * Returns the state of a property name. By the specification as of JDK 17,
         * the "jdk.xml." prefixed System property name is also the current API
         * name. Both the URI-based qName and old name if any are legacy.
         *
         * @param name the property name
         * @return the state of the property name, null if no match
         */
        public State getState(String name) {
            if ((spName != null && spName.equals(name)) ||
                    (spName == null && qName.equals(name))) {
                return State.APIPROPERTY;
            } else if ((differ && qName.equals(name)) ||
                   (oldQName != null && oldQName.equals(name))) {
                //both the URI-style qName and an old name if any are legacy
                return State.LEGACY_APIPROPERTY;
            }
            return null;
        }

        /**
         * Returns the qualified name of the property.
         *
         * @return the qualified name of the property
         */
        public String qName() {
            return qName;
        }

        /**
         * Returns the legacy name of the property.
         *
         * @return the legacy name of the property
         */
        public String qNameOld() {
            return oldQName;
        }

        /**
         * Returns the name of the corresponding System Property.
         *
         * @return the name of the System Property
         */
        public String systemProperty() {
            return spName;
        }

        /**
         * Returns the name of the legacy System Property.
         *
         * @return the name of the legacy System Property
         */
        public String systemPropertyOld() {
            return oldSPName;
        }
    }

    /**
     * Represents the state of the settings of a property. The states are in
     * descending order: the default value, value set by FEATURE_SECURE_PROCESSING (FSP),
     * in jaxp.properties, by legacy or new system property, and on factories
     * using legacy or new property names.
     */
    public static enum State {
        //this order reflects the overriding order
        DEFAULT("default"), FSP("FEATURE_SECURE_PROCESSING"), JAXPDOTPROPERTIES("jaxp.properties"),
        LEGACY_SYSTEMPROPERTY("legacy system property"), SYSTEMPROPERTY("system property"),
        LEGACY_APIPROPERTY("legacy property"), APIPROPERTY("property");

        final String literal;
        State(String literal) {
            this.literal = literal;
        }

        public String literal() {
            return literal;
        }
    }
}
