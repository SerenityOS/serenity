/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.cert.CertificateException;
import java.util.*;

import sun.security.util.HexDumpEncoder;

import sun.security.util.*;

/**
 * This class defines the Extensions attribute for the Certificate.
 *
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 * @see CertAttrSet
 */
public class CertificateExtensions implements CertAttrSet<Extension> {
    /**
     * Identifier for this attribute, to be used with the
     * get, set, delete methods of Certificate, x509 type.
     */
    public static final String IDENT = "x509.info.extensions";
    /**
     * name
     */
    public static final String NAME = "extensions";

    private static final Debug debug = Debug.getInstance("x509");

    private Map<String,Extension> map = Collections.synchronizedMap(
            new TreeMap<String,Extension>());
    private boolean unsupportedCritExt = false;

    private Map<String,Extension> unparseableExtensions;

    /**
     * Default constructor.
     */
    public CertificateExtensions() { }

    /**
     * Create the object, decoding the values from the passed DER stream.
     *
     * @param in the DerInputStream to read the Extension from.
     * @exception IOException on decoding errors.
     */
    public CertificateExtensions(DerInputStream in) throws IOException {
        init(in);
    }

    // helper routine
    private void init(DerInputStream in) throws IOException {

        DerValue[] exts = in.getSequence(5);

        for (int i = 0; i < exts.length; i++) {
            Extension ext = new Extension(exts[i]);
            parseExtension(ext);
        }
    }

    private static Class<?>[] PARAMS = {Boolean.class, Object.class};

    // Parse the encoded extension
    private void parseExtension(Extension ext) throws IOException {
        try {
            Class<?> extClass = OIDMap.getClass(ext.getExtensionId());
            if (extClass == null) {   // Unsupported extension
                if (ext.isCritical()) {
                    unsupportedCritExt = true;
                }
                if (map.put(ext.getExtensionId().toString(), ext) == null) {
                    return;
                } else {
                    throw new IOException("Duplicate extensions not allowed");
                }
            }
            Constructor<?> cons = extClass.getConstructor(PARAMS);

            Object[] passed = new Object[] {Boolean.valueOf(ext.isCritical()),
                    ext.getExtensionValue()};
                    CertAttrSet<?> certExt = (CertAttrSet<?>)
                            cons.newInstance(passed);
                    if (map.put(certExt.getName(), (Extension)certExt) != null) {
                        throw new IOException("Duplicate extensions not allowed");
                    }
        } catch (InvocationTargetException invk) {
            Throwable e = invk.getCause();
            if (ext.isCritical() == false) {
                // ignore errors parsing non-critical extensions
                if (unparseableExtensions == null) {
                    unparseableExtensions = new TreeMap<String,Extension>();
                }
                unparseableExtensions.put(ext.getExtensionId().toString(),
                        new UnparseableExtension(ext, e));
                if (debug != null) {
                    debug.println("Debug info only." +
                       " Error parsing extension: " + ext);
                    e.printStackTrace();
                    HexDumpEncoder h = new HexDumpEncoder();
                    System.err.println(h.encodeBuffer(ext.getExtensionValue()));
                }
                return;
            }
            if (e instanceof IOException) {
                throw (IOException)e;
            } else {
                throw new IOException(e);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Encode the extensions in DER form to the stream, setting
     * the context specific tag as needed in the X.509 v3 certificate.
     *
     * @param out the DerOutputStream to marshal the contents to.
     * @exception CertificateException on encoding errors.
     * @exception IOException on errors.
     */
    public void encode(OutputStream out)
    throws CertificateException, IOException {
        encode(out, false);
    }

    /**
     * Encode the extensions in DER form to the stream.
     *
     * @param out the DerOutputStream to marshal the contents to.
     * @param isCertReq if true then no context specific tag is added.
     * @exception CertificateException on encoding errors.
     * @exception IOException on errors.
     */
    public void encode(OutputStream out, boolean isCertReq)
    throws CertificateException, IOException {
        DerOutputStream extOut = new DerOutputStream();
        Collection<Extension> allExts = map.values();
        Object[] objs = allExts.toArray();

        for (int i = 0; i < objs.length; i++) {
            if (objs[i] instanceof CertAttrSet)
                ((CertAttrSet)objs[i]).encode(extOut);
            else if (objs[i] instanceof Extension)
                ((Extension)objs[i]).encode(extOut);
            else
                throw new CertificateException("Illegal extension object");
        }

        DerOutputStream seq = new DerOutputStream();
        seq.write(DerValue.tag_Sequence, extOut);

        DerOutputStream tmp;
        if (!isCertReq) { // certificate
            tmp = new DerOutputStream();
            tmp.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)3),
                    seq);
        } else
            tmp = seq; // pkcs#10 certificateRequest

        out.write(tmp.toByteArray());
    }

    /**
     * Set the attribute value.
     * @param name the extension name used in the cache.
     * @param obj the object to set.
     * @exception IOException if the object could not be cached.
     */
    public void set(String name, Object obj) throws IOException {
        if (obj instanceof Extension) {
            map.put(name, (Extension)obj);
        } else {
            throw new IOException("Unknown extension type.");
        }
    }

    /**
     * Get the attribute value.
     * @param name the extension name used in the lookup.
     * @exception IOException if named extension is not found.
     */
    public Extension get(String name) throws IOException {
        Extension obj = map.get(name);
        if (obj == null) {
            throw new IOException("No extension found with name " + name);
        }
        return (obj);
    }

    // Similar to get(String), but throw no exception, might return null.
    // Used in X509CertImpl::getExtension(OID).
    Extension getExtension(String name) {
        return map.get(name);
    }

    /**
     * Delete the attribute value.
     * @param name the extension name used in the lookup.
     * @exception IOException if named extension is not found.
     */
    public void delete(String name) throws IOException {
        Object obj = map.get(name);
        if (obj == null) {
            throw new IOException("No extension found with name " + name);
        }
        map.remove(name);
    }

    public String getNameByOid(ObjectIdentifier oid) throws IOException {
        for (String name: map.keySet()) {
            if (map.get(name).getExtensionId().equals(oid)) {
                return name;
            }
        }
        return null;
    }

    /**
     * Return an enumeration of names of attributes existing within this
     * attribute.
     */
    public Enumeration<Extension> getElements() {
        return Collections.enumeration(map.values());
    }

    /**
     * Return a collection view of the extensions.
     * @return a collection view of the extensions in this Certificate.
     */
    public Collection<Extension> getAllExtensions() {
        return map.values();
    }

    public Map<String,Extension> getUnparseableExtensions() {
        if (unparseableExtensions == null) {
            return Collections.emptyMap();
        } else {
            return unparseableExtensions;
        }
    }

    /**
     * Return the name of this attribute.
     */
    public String getName() {
        return NAME;
    }

    /**
     * Return true if a critical extension is found that is
     * not supported, otherwise return false.
     */
    public boolean hasUnsupportedCriticalExtension() {
        return unsupportedCritExt;
    }

    /**
     * Compares this CertificateExtensions for equality with the specified
     * object. If the {@code other} object is an
     * {@code instanceof} {@code CertificateExtensions}, then
     * all the entries are compared with the entries from this.
     *
     * @param other the object to test for equality with this
     * CertificateExtensions.
     * @return true iff all the entries match that of the Other,
     * false otherwise.
     */
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof CertificateExtensions))
            return false;
        Collection<Extension> otherC =
                ((CertificateExtensions)other).getAllExtensions();
        Object[] objs = otherC.toArray();

        int len = objs.length;
        if (len != map.size())
            return false;

        Extension otherExt, thisExt;
        String key = null;
        for (int i = 0; i < len; i++) {
            if (objs[i] instanceof CertAttrSet)
                key = ((CertAttrSet)objs[i]).getName();
            otherExt = (Extension)objs[i];
            if (key == null)
                key = otherExt.getExtensionId().toString();
            thisExt = map.get(key);
            if (thisExt == null)
                return false;
            if (! thisExt.equals(otherExt))
                return false;
        }
        return this.getUnparseableExtensions().equals(
                ((CertificateExtensions)other).getUnparseableExtensions());
    }

    /**
     * Returns a hashcode value for this CertificateExtensions.
     *
     * @return the hashcode value.
     */
    public int hashCode() {
        return map.hashCode() + getUnparseableExtensions().hashCode();
    }

    /**
     * Returns a string representation of this {@code CertificateExtensions}
     * object in the form of a set of entries, enclosed in braces and separated
     * by the ASCII characters "<code>,&nbsp;</code>" (comma and space).
     * <p>Overrides to {@code toString} method of {@code Object}.
     *
     * @return  a string representation of this CertificateExtensions.
     */
    public String toString() {
        return map.toString();
    }

}

class UnparseableExtension extends Extension {
    private String name;
    private String exceptionDescription;

    public UnparseableExtension(Extension ext, Throwable why) {
        super(ext);

        name = "";
        try {
            Class<?> extClass = OIDMap.getClass(ext.getExtensionId());
            if (extClass != null) {
                Field field = extClass.getDeclaredField("NAME");
                name = (String)(field.get(null)) + " ";
            }
        } catch (Exception e) {
            // If we cannot find the name, just ignore it
        }

        this.exceptionDescription = why.toString();
    }

    @Override public String toString() {
        return super.toString() +
                "Unparseable " + name + "extension due to\n" +
                exceptionDescription + "\n\n" +
                new HexDumpEncoder().encodeBuffer(getExtensionValue());
    }
}
