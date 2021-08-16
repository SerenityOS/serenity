/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.spi.DirectoryManager;
import javax.naming.spi.DirStateFactory;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.InputStream;

import java.util.Base64;
import java.util.Hashtable;
import java.util.Vector;
import java.util.StringTokenizer;

import java.lang.reflect.Proxy;
import java.lang.reflect.Modifier;

/**
  * Class containing static methods and constants for dealing with
  * encoding/decoding JNDI References and Serialized Objects
  * in LDAP.
  * @author Vincent Ryan
  * @author Rosanna Lee
  */
final class Obj {

    private Obj () {}; // Make sure no one can create one

    // package private; used by Connection
    static VersionHelper helper = VersionHelper.getVersionHelper();

    // LDAP attributes used to support Java objects.
    static final String[] JAVA_ATTRIBUTES = {
        "objectClass",
        "javaSerializedData",
        "javaClassName",
        "javaFactory",
        "javaCodeBase",
        "javaReferenceAddress",
        "javaClassNames",
        "javaRemoteLocation"     // Deprecated
    };

    static final int OBJECT_CLASS = 0;
    static final int SERIALIZED_DATA = 1;
    static final int CLASSNAME = 2;
    static final int FACTORY = 3;
    static final int CODEBASE = 4;
    static final int REF_ADDR = 5;
    static final int TYPENAME = 6;
    /**
     * @deprecated
     */
    @Deprecated
    private static final int REMOTE_LOC = 7;

    // LDAP object classes to support Java objects
    static final String[] JAVA_OBJECT_CLASSES = {
        "javaContainer",
        "javaObject",
        "javaNamingReference",
        "javaSerializedObject",
        "javaMarshalledObject",
    };

    static final String[] JAVA_OBJECT_CLASSES_LOWER = {
        "javacontainer",
        "javaobject",
        "javanamingreference",
        "javaserializedobject",
        "javamarshalledobject",
    };

    static final int STRUCTURAL = 0;    // structural object class
    static final int BASE_OBJECT = 1;   // auxiliary java object class
    static final int REF_OBJECT = 2;    // auxiliary reference object class
    static final int SER_OBJECT = 3;    // auxiliary serialized object class
    static final int MAR_OBJECT = 4;    // auxiliary marshalled object class

    /**
     * Encode an object in LDAP attributes.
     * Supports binding Referenceable or Reference, Serializable,
     * and DirContext.
     *
     * If the object supports the Referenceable interface then encode
     * the reference to the object. See encodeReference() for details.
     *<p>
     * If the object is serializable, it is stored as follows:
     * javaClassName
     *   value: Object.getClass();
     * javaSerializedData
     *   value: serialized form of Object (in binary form).
     * javaTypeName
     *   value: getTypeNames(Object.getClass());
     */
    private static Attributes encodeObject(char separator,
        Object obj, Attributes attrs,
        Attribute objectClass, boolean cloned)
        throws NamingException {
            boolean structural =
                (objectClass.size() == 0 ||
                    (objectClass.size() == 1 && objectClass.contains("top")));

            if (structural) {
                objectClass.add(JAVA_OBJECT_CLASSES[STRUCTURAL]);
            }

    // References
            if (obj instanceof Referenceable) {
                objectClass.add(JAVA_OBJECT_CLASSES[BASE_OBJECT]);
                objectClass.add(JAVA_OBJECT_CLASSES[REF_OBJECT]);
                if (!cloned) {
                    attrs = (Attributes)attrs.clone();
                }
                attrs.put(objectClass);
                return (encodeReference(separator,
                    ((Referenceable)obj).getReference(),
                    attrs, obj));

            } else if (obj instanceof Reference) {
                objectClass.add(JAVA_OBJECT_CLASSES[BASE_OBJECT]);
                objectClass.add(JAVA_OBJECT_CLASSES[REF_OBJECT]);
                if (!cloned) {
                    attrs = (Attributes)attrs.clone();
                }
                attrs.put(objectClass);
                return (encodeReference(separator, (Reference)obj, attrs, null));

    // Serializable Object
            } else if (obj instanceof java.io.Serializable) {
                objectClass.add(JAVA_OBJECT_CLASSES[BASE_OBJECT]);
                if (!(objectClass.contains(JAVA_OBJECT_CLASSES[MAR_OBJECT]) ||
                    objectClass.contains(JAVA_OBJECT_CLASSES_LOWER[MAR_OBJECT]))) {
                    objectClass.add(JAVA_OBJECT_CLASSES[SER_OBJECT]);
                }
                if (!cloned) {
                    attrs = (Attributes)attrs.clone();
                }
                attrs.put(objectClass);
                attrs.put(new BasicAttribute(JAVA_ATTRIBUTES[SERIALIZED_DATA],
                    serializeObject(obj)));
                if (attrs.get(JAVA_ATTRIBUTES[CLASSNAME]) == null) {
                    attrs.put(JAVA_ATTRIBUTES[CLASSNAME],
                        obj.getClass().getName());
                }
                if (attrs.get(JAVA_ATTRIBUTES[TYPENAME]) == null) {
                    Attribute tAttr =
                        LdapCtxFactory.createTypeNameAttr(obj.getClass());
                    if (tAttr != null) {
                        attrs.put(tAttr);
                    }
                }
    // DirContext Object
            } else if (obj instanceof DirContext) {
                // do nothing
            } else {
                throw new IllegalArgumentException(
            "can only bind Referenceable, Serializable, DirContext");
            }
            //      System.err.println(attrs);
            return attrs;
    }

    /**
     * Each value in javaCodebase contains a list of space-separated
     * URLs. Each value is independent; we can pick any of the values
     * so we just use the first one.
     * @return an array of URL strings for the codebase
     */
    private static String[] getCodebases(Attribute codebaseAttr) throws
        NamingException {
        if (codebaseAttr == null) {
            return null;
        } else {
            StringTokenizer parser =
                new StringTokenizer((String)codebaseAttr.get());
            Vector<String> vec = new Vector<>(10);
            while (parser.hasMoreTokens()) {
                vec.addElement(parser.nextToken());
            }
            String[] answer = new String[vec.size()];
            for (int i = 0; i < answer.length; i++) {
                answer[i] = vec.elementAt(i);
            }
            return answer;
        }
    }

    /*
     * Decode an object from LDAP attribute(s).
     * The object may be a Reference, or a Serialized object.
     *
     * See encodeObject() and encodeReference() for details on formats
     * expected.
     */
    static Object decodeObject(Attributes attrs)
        throws NamingException {

        Attribute attr;

        // Get codebase, which is used in all 3 cases.
        String[] codebases = getCodebases(attrs.get(JAVA_ATTRIBUTES[CODEBASE]));
        try {
            if ((attr = attrs.get(JAVA_ATTRIBUTES[SERIALIZED_DATA])) != null) {
                if (!VersionHelper.isSerialDataAllowed()) {
                    throw new NamingException("Object deserialization is not allowed");
                }
                ClassLoader cl = helper.getURLClassLoader(codebases);
                return deserializeObject((byte[])attr.get(), cl);
            } else if ((attr = attrs.get(JAVA_ATTRIBUTES[REMOTE_LOC])) != null) {
                // For backward compatibility only
                return decodeRmiObject(
                    (String)attrs.get(JAVA_ATTRIBUTES[CLASSNAME]).get(),
                    (String)attr.get(), codebases);
            }

            attr = attrs.get(JAVA_ATTRIBUTES[OBJECT_CLASS]);
            if (attr != null &&
                (attr.contains(JAVA_OBJECT_CLASSES[REF_OBJECT]) ||
                    attr.contains(JAVA_OBJECT_CLASSES_LOWER[REF_OBJECT]))) {
                return decodeReference(attrs, codebases);
            }
            return null;
        } catch (IOException e) {
            NamingException ne = new NamingException();
            ne.setRootCause(e);
            throw ne;
        }
    }

    /**
     * Convert a Reference object into several LDAP attributes.
     *
     * A Reference is stored as into the following attributes:
     * javaClassName
     *   value: Reference.getClassName();
     * javaFactory
     *   value: Reference.getFactoryClassName();
     * javaCodeBase
     *   value: Reference.getFactoryClassLocation();
     * javaReferenceAddress
     *   value: #0#typeA#valA
     *   value: #1#typeB#valB
     *   value: #2#typeC##[serialized RefAddr C]
     *   value: #3#typeD#valD
     *
     * where
     * -  the first character denotes the separator
     * -  the number following the first separator denotes the position
     *    of the RefAddr within the Reference
     * -  "typeA" is RefAddr.getType()
     * -  ## denotes that the Base64-encoded form of the non-StringRefAddr
     *    is to follow; otherwise the value that follows is
     *    StringRefAddr.getContents()
     *
     * The default separator is the hash character (#).
     * May provide property for this in future.
     */

    private static Attributes encodeReference(char separator,
        Reference ref, Attributes attrs, Object orig)
        throws NamingException {

        if (ref == null)
            return attrs;

        String s;

        if ((s = ref.getClassName()) != null) {
            attrs.put(new BasicAttribute(JAVA_ATTRIBUTES[CLASSNAME], s));
        }

        if ((s = ref.getFactoryClassName()) != null) {
            attrs.put(new BasicAttribute(JAVA_ATTRIBUTES[FACTORY], s));
        }

        if ((s = ref.getFactoryClassLocation()) != null) {
            attrs.put(new BasicAttribute(JAVA_ATTRIBUTES[CODEBASE], s));
        }

        // Get original object's types if caller has not explicitly
        // specified other type names
        if (orig != null && attrs.get(JAVA_ATTRIBUTES[TYPENAME]) != null) {
            Attribute tAttr =
                LdapCtxFactory.createTypeNameAttr(orig.getClass());
            if (tAttr != null) {
                attrs.put(tAttr);
            }
        }

        int count = ref.size();

        if (count > 0) {

            Attribute refAttr = new BasicAttribute(JAVA_ATTRIBUTES[REF_ADDR]);
            RefAddr refAddr;
            Base64.Encoder encoder = null;

            for (int i = 0; i < count; i++) {
                refAddr = ref.get(i);

                if (refAddr instanceof StringRefAddr) {
                    refAttr.add(""+ separator + i +
                        separator +     refAddr.getType() +
                        separator + refAddr.getContent());
                } else {
                    if (encoder == null)
                        encoder = Base64.getMimeEncoder();

                    refAttr.add(""+ separator + i +
                        separator + refAddr.getType() +
                        separator + separator +
                        encoder.encodeToString(serializeObject(refAddr)));
                }
            }
            attrs.put(refAttr);
        }
        return attrs;
    }

    /*
     * A RMI object is stored in the directory as
     * javaClassName
     *   value: Object.getClass();
     * javaRemoteLocation
     *   value: URL of RMI object (accessed through the RMI Registry)
     * javaCodebase:
     *   value: URL of codebase of where to find classes for object
     *
     * Return the RMI Location URL itself. This will be turned into
     * an RMI object when getObjectInstance() is called on it.
     * %%% Ignore codebase for now. Depend on RMI registry to send code.-RL
     * @deprecated For backward compatibility only
     */
    private static Object decodeRmiObject(String className,
        String rmiName, String[] codebases) throws NamingException {
            return new Reference(className, new StringRefAddr("URL", rmiName));
    }

    /*
     * Restore a Reference object from several LDAP attributes
     */
    private static Reference decodeReference(Attributes attrs,
        String[] codebases) throws NamingException, IOException {

        Attribute attr;
        String className;
        String factory = null;

        if ((attr = attrs.get(JAVA_ATTRIBUTES[CLASSNAME])) != null) {
            className = (String)attr.get();
        } else {
            throw new InvalidAttributesException(JAVA_ATTRIBUTES[CLASSNAME] +
                        " attribute is required");
        }

        if ((attr = attrs.get(JAVA_ATTRIBUTES[FACTORY])) != null) {
            factory = (String)attr.get();
        }

        Reference ref = new Reference(className, factory,
            (codebases != null? codebases[0] : null));

        /*
         * string encoding of a RefAddr is either:
         *
         *      #posn#<type>#<address>
         * or
         *      #posn#<type>##<base64-encoded address>
         */
        if ((attr = attrs.get(JAVA_ATTRIBUTES[REF_ADDR])) != null) {

            String val, posnStr, type;
            char separator;
            int start, sep, posn;
            Base64.Decoder decoder = null;

            ClassLoader cl = helper.getURLClassLoader(codebases);

            /*
             * Temporary Vector for decoded RefAddr addresses - used to ensure
             * unordered addresses are correctly re-ordered.
             */
            Vector<RefAddr> refAddrList = new Vector<>();
            refAddrList.setSize(attr.size());

            for (NamingEnumeration<?> vals = attr.getAll(); vals.hasMore(); ) {

                val = (String)vals.next();

                if (val.length() == 0) {
                    throw new InvalidAttributeValueException(
                        "malformed " + JAVA_ATTRIBUTES[REF_ADDR] + " attribute - "+
                        "empty attribute value");
                }
                // first character denotes encoding separator
                separator = val.charAt(0);
                start = 1;  // skip over separator

                // extract position within Reference
                if ((sep = val.indexOf(separator, start)) < 0) {
                    throw new InvalidAttributeValueException(
                        "malformed " + JAVA_ATTRIBUTES[REF_ADDR] + " attribute - " +
                        "separator '" + separator + "'" + "not found");
                }
                if ((posnStr = val.substring(start, sep)) == null) {
                    throw new InvalidAttributeValueException(
                        "malformed " + JAVA_ATTRIBUTES[REF_ADDR] + " attribute - " +
                        "empty RefAddr position");
                }
                try {
                    posn = Integer.parseInt(posnStr);
                } catch (NumberFormatException nfe) {
                    throw new InvalidAttributeValueException(
                        "malformed " + JAVA_ATTRIBUTES[REF_ADDR] + " attribute - " +
                        "RefAddr position not an integer");
                }
                start = sep + 1; // skip over position and trailing separator

                // extract type
                if ((sep = val.indexOf(separator, start)) < 0) {
                    throw new InvalidAttributeValueException(
                        "malformed " + JAVA_ATTRIBUTES[REF_ADDR] + " attribute - " +
                        "RefAddr type not found");
                }
                if ((type = val.substring(start, sep)) == null) {
                    throw new InvalidAttributeValueException(
                        "malformed " + JAVA_ATTRIBUTES[REF_ADDR] + " attribute - " +
                        "empty RefAddr type");
                }
                start = sep + 1; // skip over type and trailing separator

                // extract content
                if (start == val.length()) {
                    // Empty content
                    refAddrList.setElementAt(new StringRefAddr(type, null), posn);
                } else if (val.charAt(start) == separator) {
                    // Double separators indicate a non-StringRefAddr
                    // Content is a Base64-encoded serialized RefAddr

                    ++start;  // skip over consecutive separator
                    // %%% RL: exception if empty after double separator

                    if (decoder == null)
                        decoder = Base64.getMimeDecoder();

                    RefAddr ra = (RefAddr)
                        deserializeObject(
                            decoder.decode(val.substring(start).getBytes()),
                            cl);

                    refAddrList.setElementAt(ra, posn);
                } else {
                    // Single separator indicates a StringRefAddr
                    refAddrList.setElementAt(new StringRefAddr(type,
                        val.substring(start)), posn);
                }
            }

            // Copy to real reference
            for (int i = 0; i < refAddrList.size(); i++) {
                ref.add(refAddrList.elementAt(i));
            }
        }

        return (ref);
    }

    /*
     * Serialize an object into a byte array
     */
    private static byte[] serializeObject(Object obj) throws NamingException {

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream serial = new ObjectOutputStream(bytes)) {
                serial.writeObject(obj);
            }

            return (bytes.toByteArray());

        } catch (IOException e) {
            NamingException ne = new NamingException();
            ne.setRootCause(e);
            throw ne;
        }
    }

    /*
     * Deserializes a byte array into an object.
     */
    private static Object deserializeObject(byte[] obj, ClassLoader cl)
        throws NamingException {

        try {
            // Create ObjectInputStream for deserialization
            ByteArrayInputStream bytes = new ByteArrayInputStream(obj);
            try (ObjectInputStream deserial = cl == null ?
                    new ObjectInputStream(bytes) :
                    new LoaderInputStream(bytes, cl)) {
                return deserial.readObject();
            } catch (ClassNotFoundException e) {
                NamingException ne = new NamingException();
                ne.setRootCause(e);
                throw ne;
            }
        } catch (IOException e) {
            NamingException ne = new NamingException();
            ne.setRootCause(e);
            throw ne;
        }
    }

    /**
      * Returns the attributes to bind given an object and its attributes.
      */
    static Attributes determineBindAttrs(
        char separator, Object obj, Attributes attrs, boolean cloned,
        Name name, Context ctx, Hashtable<?,?> env)
        throws NamingException {

        // Call state factories to convert object and attrs
        DirStateFactory.Result res =
            DirectoryManager.getStateToBind(obj, name, ctx, env, attrs);
        obj = res.getObject();
        attrs = res.getAttributes();

        // We're only storing attributes; no further processing required
        if (obj == null) {
            return attrs;
        }

        //if object to be bound is a DirContext extract its attributes
        if ((attrs == null) && (obj instanceof DirContext)) {
            cloned = true;
            attrs = ((DirContext)obj).getAttributes("");
        }

        boolean ocNeedsCloning = false;

        // Create "objectClass" attribute
        Attribute objectClass;
        if (attrs == null || attrs.size() == 0) {
            attrs = new BasicAttributes(LdapClient.caseIgnore);
            cloned = true;

            // No objectclasses supplied, use "top" to start
            objectClass = new BasicAttribute("objectClass", "top");

        } else {
            // Get existing objectclass attribute
            objectClass = attrs.get("objectClass");
            if (objectClass == null && !attrs.isCaseIgnored()) {
                // %%% workaround
                objectClass = attrs.get("objectclass");
            }

            // No objectclasses supplied, use "top" to start
            if (objectClass == null) {
                objectClass =  new BasicAttribute("objectClass", "top");
            } else if (ocNeedsCloning || !cloned) {
                objectClass = (Attribute)objectClass.clone();
            }
        }

        // convert the supplied object into LDAP attributes
        attrs = encodeObject(separator, obj, attrs, objectClass, cloned);

        // System.err.println("Determined: " + attrs);
        return attrs;
    }

    /**
     * An ObjectInputStream that uses a class loader to find classes.
     */
    private static final class LoaderInputStream extends ObjectInputStream {
        private ClassLoader classLoader;

        LoaderInputStream(InputStream in, ClassLoader cl) throws IOException {
            super(in);
            classLoader = cl;
        }

        protected Class<?> resolveClass(ObjectStreamClass desc) throws
                IOException, ClassNotFoundException {
            try {
                // %%% Should use Class.forName(desc.getName(), false, classLoader);
                // except we can't because that is only available on JDK1.2
                return classLoader.loadClass(desc.getName());
            } catch (ClassNotFoundException e) {
                return super.resolveClass(desc);
            }
        }

         protected Class<?> resolveProxyClass(String[] interfaces) throws
                IOException, ClassNotFoundException {
             ClassLoader nonPublicLoader = null;
             boolean hasNonPublicInterface = false;

             // define proxy in class loader of non-public interface(s), if any
             Class<?>[] classObjs = new Class<?>[interfaces.length];
             for (int i = 0; i < interfaces.length; i++) {
                 Class<?> cl = Class.forName(interfaces[i], false, classLoader);
                 if ((cl.getModifiers() & Modifier.PUBLIC) == 0) {
                     if (hasNonPublicInterface) {
                         if (nonPublicLoader != cl.getClassLoader()) {
                             throw new IllegalAccessError(
                                "conflicting non-public interface class loaders");
                         }
                     } else {
                         nonPublicLoader = cl.getClassLoader();
                         hasNonPublicInterface = true;
                     }
                 }
                 classObjs[i] = cl;
             }
             try {
                 @SuppressWarnings("deprecation")
                 Class<?> proxyClass = Proxy.getProxyClass(hasNonPublicInterface ?
                        nonPublicLoader : classLoader, classObjs);
                 return proxyClass;
             } catch (IllegalArgumentException e) {
                 throw new ClassNotFoundException(null, e);
             }
         }

     }
}
