/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs11;

import java.util.*;
import java.util.concurrent.*;

import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;

/**
 * TemplateManager class.
 *
 * Not all PKCS#11 tokens are created equal. One token may require that one
 * value is specified when creating a certain type of object. Another token
 * may require a different value. Yet another token may only work if the
 * attribute is not specified at all.
 *
 * In order to allow an application to work unmodified with all those
 * different tokens, the SunPKCS11 provider makes the attributes that are
 * specified and their value configurable. Hence, only the SunPKCS11
 * configuration file has to be tweaked at deployment time to allow all
 * existing applications to be used.
 *
 * The template manager is responsible for reading the attribute configuration
 * information and to make it available to the various internal components
 * of the SunPKCS11 provider.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
final class TemplateManager {

    private static final boolean DEBUG = false;

    // constant for any operation (either O_IMPORT or O_GENERATE)
    static final String O_ANY      = "*";
    // constant for operation create ("importing" existing key material)
    static final String O_IMPORT   = "import";
    // constant for operation generate (generating new key material)
    static final String O_GENERATE = "generate";

    private static class KeyAndTemplate {
        final TemplateKey key;
        final Template template;

        KeyAndTemplate(TemplateKey key, Template template) {
            this.key = key;
            this.template = template;
        }
    }

    // primitive templates contains the individual template configuration
    // entries from the configuration file
    private final List<KeyAndTemplate> primitiveTemplates;

    // composite templates is a cache of the exact configuration template for
    // each specific TemplateKey (no wildcards). the entries are created
    // on demand during first use by compositing all applicable
    // primitive template entries. the result is then stored in this map
    // for performance
    private final Map<TemplateKey,Template> compositeTemplates;

    TemplateManager() {
        primitiveTemplates = new ArrayList<KeyAndTemplate>();
        compositeTemplates = new ConcurrentHashMap<TemplateKey,Template>();
    }

    // add a template. Called by Config.
    void addTemplate(String op, long objectClass, long keyAlgorithm,
            CK_ATTRIBUTE[] attrs) {
        TemplateKey key = new TemplateKey(op, objectClass, keyAlgorithm);
        Template template = new Template(attrs);
        if (DEBUG) {
            System.out.println("Adding " + key + " -> " + template);
        }
        primitiveTemplates.add(new KeyAndTemplate(key, template));
    }

    private Template getTemplate(TemplateKey key) {
        Template template = compositeTemplates.get(key);
        if (template == null) {
            template = buildCompositeTemplate(key);
            compositeTemplates.put(key, template);
        }
        return template;
    }

    // Get the attributes for the requested op and combine them with attrs.
    // This is the method called by the implementation to obtain the
    // attributes.
    CK_ATTRIBUTE[] getAttributes(String op, long type, long alg,
            CK_ATTRIBUTE[] attrs) {
        TemplateKey key = new TemplateKey(op, type, alg);
        Template template = getTemplate(key);
        CK_ATTRIBUTE[] newAttrs = template.getAttributes(attrs);
        if (DEBUG) {
            System.out.println(key + " -> " + Arrays.asList(newAttrs));
        }
        return newAttrs;
    }

    // build a composite template for the given key
    private Template buildCompositeTemplate(TemplateKey key) {
        Template comp = new Template();
        // iterate through primitive templates and add all that apply
        for (KeyAndTemplate entry : primitiveTemplates) {
            if (entry.key.appliesTo(key)) {
                comp.add(entry.template);
            }
        }
        return comp;
    }

    /**
     * Nested class representing a template identifier.
     */
    private static final class TemplateKey {
        final String operation;
        final long keyType;
        final long keyAlgorithm;
        TemplateKey(String operation, long keyType, long keyAlgorithm) {
            this.operation = operation;
            this.keyType = keyType;
            this.keyAlgorithm = keyAlgorithm;
        }
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof TemplateKey == false) {
                return false;
            }
            TemplateKey other = (TemplateKey)obj;
            boolean match = this.operation.equals(other.operation)
                        && (this.keyType == other.keyType)
                        && (this.keyAlgorithm == other.keyAlgorithm);
            return match;
        }
        public int hashCode() {
            return operation.hashCode() + (int)keyType + (int)keyAlgorithm;
        }
        boolean appliesTo(TemplateKey key) {
            if (operation.equals(O_ANY) || operation.equals(key.operation)) {
                if ((keyType == PCKO_ANY) || (keyType == key.keyType)) {
                    if ((keyAlgorithm == PCKK_ANY)
                                || (keyAlgorithm == key.keyAlgorithm)) {
                        return true;
                    }
                }
            }
            return false;
        }
        public String toString() {
            return "(" + operation + ","
                + Functions.getObjectClassName(keyType)
                + "," + Functions.getKeyName(keyAlgorithm) + ")";
        }
    }

    /**
     * Nested class representing template attributes.
     */
    private static final class Template {

        private static final CK_ATTRIBUTE[] A0 = new CK_ATTRIBUTE[0];

        private CK_ATTRIBUTE[] attributes;

        Template() {
            attributes = A0;
        }

        Template(CK_ATTRIBUTE[] attributes) {
            this.attributes = attributes;
        }

        void add(Template template) {
            attributes = getAttributes(template.attributes);
        }

        CK_ATTRIBUTE[] getAttributes(CK_ATTRIBUTE[] attrs) {
            return combine(attributes, attrs);
        }

        /**
         * Combine two sets of attributes. The second set has precedence
         * over the first and overrides its settings.
         */
        private static CK_ATTRIBUTE[] combine(CK_ATTRIBUTE[] attrs1,
                CK_ATTRIBUTE[] attrs2) {
            List<CK_ATTRIBUTE> attrs = new ArrayList<CK_ATTRIBUTE>();
            for (CK_ATTRIBUTE attr : attrs1) {
                if (attr.pValue != null) {
                    attrs.add(attr);
                }
            }
            for (CK_ATTRIBUTE attr2 : attrs2) {
                long type = attr2.type;
                for (CK_ATTRIBUTE attr1 : attrs1) {
                    if (attr1.type == type) {
                        attrs.remove(attr1);
                    }
                }
                if (attr2.pValue != null) {
                    attrs.add(attr2);
                }
            }
            return attrs.toArray(A0);
        }

        public String toString() {
            return Arrays.asList(attributes).toString();
        }

    }

}
