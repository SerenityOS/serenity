/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.access;

import javax.crypto.SealedObject;
import javax.crypto.spec.SecretKeySpec;
import java.io.ObjectInputFilter;
import java.lang.invoke.MethodHandles;
import java.lang.module.ModuleDescriptor;
import java.security.spec.EncodedKeySpec;
import java.util.ResourceBundle;
import java.util.jar.JarFile;
import java.io.Console;
import java.io.FileDescriptor;
import java.io.FilePermission;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.security.ProtectionDomain;
import java.security.Signature;

/** A repository of "shared secrets", which are a mechanism for
    calling implementation-private methods in another package without
    using reflection. A package-private class implements a public
    interface and provides the ability to call package-private methods
    within that package; the object implementing that interface is
    provided through a third package to which access is restricted.
    This framework avoids the primary disadvantage of using reflection
    for this purpose, namely the loss of compile-time checking. */

public class SharedSecrets {
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static JavaAWTAccess javaAWTAccess;
    private static JavaAWTFontAccess javaAWTFontAccess;
    private static JavaBeansAccess javaBeansAccess;
    private static JavaLangAccess javaLangAccess;
    private static JavaLangInvokeAccess javaLangInvokeAccess;
    private static JavaLangModuleAccess javaLangModuleAccess;
    private static JavaLangRefAccess javaLangRefAccess;
    private static JavaLangReflectAccess javaLangReflectAccess;
    private static JavaIOAccess javaIOAccess;
    private static JavaIOFileDescriptorAccess javaIOFileDescriptorAccess;
    private static JavaIOFilePermissionAccess javaIOFilePermissionAccess;
    private static JavaIORandomAccessFileAccess javaIORandomAccessFileAccess;
    private static JavaObjectInputStreamReadString javaObjectInputStreamReadString;
    private static JavaObjectInputStreamAccess javaObjectInputStreamAccess;
    private static JavaObjectInputFilterAccess javaObjectInputFilterAccess;
    private static JavaNetInetAddressAccess javaNetInetAddressAccess;
    private static JavaNetHttpCookieAccess javaNetHttpCookieAccess;
    private static JavaNetUriAccess javaNetUriAccess;
    private static JavaNetURLAccess javaNetURLAccess;
    private static JavaNioAccess javaNioAccess;
    private static JavaUtilCollectionAccess javaUtilCollectionAccess;
    private static JavaUtilJarAccess javaUtilJarAccess;
    private static JavaUtilZipFileAccess javaUtilZipFileAccess;
    private static JavaUtilResourceBundleAccess javaUtilResourceBundleAccess;
    private static JavaSecurityAccess javaSecurityAccess;
    private static JavaSecuritySignatureAccess javaSecuritySignatureAccess;
    private static JavaSecuritySpecAccess javaSecuritySpecAccess;
    private static JavaxCryptoSealedObjectAccess javaxCryptoSealedObjectAccess;
    private static JavaxCryptoSpecAccess javaxCryptoSpecAccess;

    public static void setJavaUtilCollectionAccess(JavaUtilCollectionAccess juca) {
        javaUtilCollectionAccess = juca;
    }

    public static JavaUtilCollectionAccess getJavaUtilCollectionAccess() {
        var access = javaUtilCollectionAccess;
        if (access == null) {
            try {
                Class.forName("java.util.ImmutableCollections$Access", true, null);
                access = javaUtilCollectionAccess;
            } catch (ClassNotFoundException e) {}
        }
        return access;
    }

    public static JavaUtilJarAccess javaUtilJarAccess() {
        var access = javaUtilJarAccess;
        if (access == null) {
            // Ensure JarFile is initialized; we know that this class
            // provides the shared secret
            ensureClassInitialized(JarFile.class);
            access = javaUtilJarAccess;
        }
        return access;
    }

    public static void setJavaUtilJarAccess(JavaUtilJarAccess access) {
        javaUtilJarAccess = access;
    }

    public static void setJavaLangAccess(JavaLangAccess jla) {
        javaLangAccess = jla;
    }

    public static JavaLangAccess getJavaLangAccess() {
        return javaLangAccess;
    }

    public static void setJavaLangInvokeAccess(JavaLangInvokeAccess jlia) {
        javaLangInvokeAccess = jlia;
    }

    public static JavaLangInvokeAccess getJavaLangInvokeAccess() {
        var access = javaLangInvokeAccess;
        if (access == null) {
            try {
                Class.forName("java.lang.invoke.MethodHandleImpl", true, null);
                access = javaLangInvokeAccess;
            } catch (ClassNotFoundException e) {}
        }
        return access;
    }

    public static void setJavaLangModuleAccess(JavaLangModuleAccess jlrma) {
        javaLangModuleAccess = jlrma;
    }

    public static JavaLangModuleAccess getJavaLangModuleAccess() {
        var access = javaLangModuleAccess;
        if (access == null) {
            ensureClassInitialized(ModuleDescriptor.class);
            access = javaLangModuleAccess;
        }
        return access;
    }

    public static void setJavaLangRefAccess(JavaLangRefAccess jlra) {
        javaLangRefAccess = jlra;
    }

    public static JavaLangRefAccess getJavaLangRefAccess() {
        return javaLangRefAccess;
    }

    public static void setJavaLangReflectAccess(JavaLangReflectAccess jlra) {
        javaLangReflectAccess = jlra;
    }

    public static JavaLangReflectAccess getJavaLangReflectAccess() {
        return javaLangReflectAccess;
    }

    public static void setJavaNetUriAccess(JavaNetUriAccess jnua) {
        javaNetUriAccess = jnua;
    }

    public static JavaNetUriAccess getJavaNetUriAccess() {
        var access = javaNetUriAccess;
        if (access == null) {
            ensureClassInitialized(java.net.URI.class);
            access = javaNetUriAccess;
        }
        return access;
    }

    public static void setJavaNetURLAccess(JavaNetURLAccess jnua) {
        javaNetURLAccess = jnua;
    }

    public static JavaNetURLAccess getJavaNetURLAccess() {
        var access = javaNetURLAccess;
        if (access == null) {
            ensureClassInitialized(java.net.URL.class);
            access = javaNetURLAccess;
        }
        return access;
    }

    public static void setJavaNetInetAddressAccess(JavaNetInetAddressAccess jna) {
        javaNetInetAddressAccess = jna;
    }

    public static JavaNetInetAddressAccess getJavaNetInetAddressAccess() {
        var access = javaNetInetAddressAccess;
        if (access == null) {
            ensureClassInitialized(java.net.InetAddress.class);
            access = javaNetInetAddressAccess;
        }
        return access;
    }

    public static void setJavaNetHttpCookieAccess(JavaNetHttpCookieAccess a) {
        javaNetHttpCookieAccess = a;
    }

    public static JavaNetHttpCookieAccess getJavaNetHttpCookieAccess() {
        var access = javaNetHttpCookieAccess;
        if (access == null) {
            ensureClassInitialized(java.net.HttpCookie.class);
            access = javaNetHttpCookieAccess;
        }
        return access;
    }

    public static void setJavaNioAccess(JavaNioAccess jna) {
        javaNioAccess = jna;
    }

    public static JavaNioAccess getJavaNioAccess() {
        var access = javaNioAccess;
        if (access == null) {
            // Ensure java.nio.Buffer is initialized, which provides the
            // shared secret.
            ensureClassInitialized(java.nio.Buffer.class);
            access = javaNioAccess;
        }
        return access;
    }

    public static void setJavaIOAccess(JavaIOAccess jia) {
        javaIOAccess = jia;
    }

    public static JavaIOAccess getJavaIOAccess() {
        var access = javaIOAccess;
        if (access == null) {
            ensureClassInitialized(Console.class);
            access = javaIOAccess;
        }
        return access;
    }

    public static void setJavaIOFileDescriptorAccess(JavaIOFileDescriptorAccess jiofda) {
        javaIOFileDescriptorAccess = jiofda;
    }

    public static JavaIOFilePermissionAccess getJavaIOFilePermissionAccess() {
        var access = javaIOFilePermissionAccess;
        if (access == null) {
            ensureClassInitialized(FilePermission.class);
            access = javaIOFilePermissionAccess;
        }
        return access;
    }

    public static void setJavaIOFilePermissionAccess(JavaIOFilePermissionAccess jiofpa) {
        javaIOFilePermissionAccess = jiofpa;
    }

    public static JavaIOFileDescriptorAccess getJavaIOFileDescriptorAccess() {
        var access = javaIOFileDescriptorAccess;
        if (access == null) {
            ensureClassInitialized(FileDescriptor.class);
            access = javaIOFileDescriptorAccess;
        }
        return access;
    }

    public static void setJavaSecurityAccess(JavaSecurityAccess jsa) {
        javaSecurityAccess = jsa;
    }

    public static JavaSecurityAccess getJavaSecurityAccess() {
        var access = javaSecurityAccess;
        if (access == null) {
            ensureClassInitialized(ProtectionDomain.class);
            access = javaSecurityAccess;
        }
        return access;
    }

    public static JavaUtilZipFileAccess getJavaUtilZipFileAccess() {
        var access = javaUtilZipFileAccess;
        if (access == null) {
            ensureClassInitialized(java.util.zip.ZipFile.class);
            access = javaUtilZipFileAccess;
        }
        return access;
    }

    public static void setJavaUtilZipFileAccess(JavaUtilZipFileAccess access) {
        javaUtilZipFileAccess = access;
    }

    public static void setJavaAWTAccess(JavaAWTAccess jaa) {
        javaAWTAccess = jaa;
    }

    public static JavaAWTAccess getJavaAWTAccess() {
        // this may return null in which case calling code needs to
        // provision for.
        return javaAWTAccess;
    }

    public static void setJavaAWTFontAccess(JavaAWTFontAccess jafa) {
        javaAWTFontAccess = jafa;
    }

    public static JavaAWTFontAccess getJavaAWTFontAccess() {
        // this may return null in which case calling code needs to
        // provision for.
        return javaAWTFontAccess;
    }

    public static JavaBeansAccess getJavaBeansAccess() {
        return javaBeansAccess;
    }

    public static void setJavaBeansAccess(JavaBeansAccess access) {
        javaBeansAccess = access;
    }

    public static JavaUtilResourceBundleAccess getJavaUtilResourceBundleAccess() {
        var access = javaUtilResourceBundleAccess;
        if (access == null) {
            ensureClassInitialized(ResourceBundle.class);
            access = javaUtilResourceBundleAccess;
        }
        return access;
    }

    public static void setJavaUtilResourceBundleAccess(JavaUtilResourceBundleAccess access) {
        javaUtilResourceBundleAccess = access;
    }

    public static JavaObjectInputStreamReadString getJavaObjectInputStreamReadString() {
        var access = javaObjectInputStreamReadString;
        if (access == null) {
            ensureClassInitialized(ObjectInputStream.class);
            access = javaObjectInputStreamReadString;
        }
        return access;
    }

    public static void setJavaObjectInputStreamReadString(JavaObjectInputStreamReadString access) {
        javaObjectInputStreamReadString = access;
    }

    public static JavaObjectInputStreamAccess getJavaObjectInputStreamAccess() {
        var access = javaObjectInputStreamAccess;
        if (access == null) {
            ensureClassInitialized(ObjectInputStream.class);
            access = javaObjectInputStreamAccess;
        }
        return access;
    }

    public static void setJavaObjectInputStreamAccess(JavaObjectInputStreamAccess access) {
        javaObjectInputStreamAccess = access;
    }

    public static JavaObjectInputFilterAccess getJavaObjectInputFilterAccess() {
        var access = javaObjectInputFilterAccess;
        if (access == null) {
            ensureClassInitialized(ObjectInputFilter.Config.class);
            access = javaObjectInputFilterAccess;
        }
        return access;
    }

    public static void setJavaObjectInputFilterAccess(JavaObjectInputFilterAccess access) {
        javaObjectInputFilterAccess = access;
    }

    public static void setJavaIORandomAccessFileAccess(JavaIORandomAccessFileAccess jirafa) {
        javaIORandomAccessFileAccess = jirafa;
    }

    public static JavaIORandomAccessFileAccess getJavaIORandomAccessFileAccess() {
        var access = javaIORandomAccessFileAccess;
        if (access == null) {
            ensureClassInitialized(RandomAccessFile.class);
            access = javaIORandomAccessFileAccess;
        }
        return access;
    }

    public static void setJavaSecuritySignatureAccess(JavaSecuritySignatureAccess jssa) {
        javaSecuritySignatureAccess = jssa;
    }

    public static JavaSecuritySignatureAccess getJavaSecuritySignatureAccess() {
        var access = javaSecuritySignatureAccess;
        if (access == null) {
            ensureClassInitialized(Signature.class);
            access = javaSecuritySignatureAccess;
        }
        return access;
    }

    public static void setJavaSecuritySpecAccess(JavaSecuritySpecAccess jssa) {
        javaSecuritySpecAccess = jssa;
    }

    public static JavaSecuritySpecAccess getJavaSecuritySpecAccess() {
        if (javaSecuritySpecAccess == null) {
            ensureClassInitialized(EncodedKeySpec.class);
        }
        return javaSecuritySpecAccess;
    }

    public static void setJavaxCryptoSpecAccess(JavaxCryptoSpecAccess jcsa) {
        javaxCryptoSpecAccess = jcsa;
    }

    public static JavaxCryptoSpecAccess getJavaxCryptoSpecAccess() {
        if (javaxCryptoSpecAccess == null) {
            ensureClassInitialized(SecretKeySpec.class);
        }
        return javaxCryptoSpecAccess;
    }

    public static void setJavaxCryptoSealedObjectAccess(JavaxCryptoSealedObjectAccess jcsoa) {
        javaxCryptoSealedObjectAccess = jcsoa;
    }

    public static JavaxCryptoSealedObjectAccess getJavaxCryptoSealedObjectAccess() {
        var access = javaxCryptoSealedObjectAccess;
        if (access == null) {
            ensureClassInitialized(SealedObject.class);
            access = javaxCryptoSealedObjectAccess;
        }
        return access;
    }

    private static void ensureClassInitialized(Class<?> c) {
        try {
            MethodHandles.lookup().ensureInitialized(c);
        } catch (IllegalAccessException e) {}
    }
}
