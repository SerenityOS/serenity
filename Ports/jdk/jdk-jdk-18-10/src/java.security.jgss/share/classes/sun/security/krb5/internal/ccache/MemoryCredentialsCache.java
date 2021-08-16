/*
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

/*
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5.internal.ccache;

import sun.security.krb5.*;
import sun.security.krb5.internal.*;
import java.io.IOException;
import java.io.File;

//Windows supports the "API: cache" type, which is a shared memory cache.  This is
//implemented by krbcc32.dll as part of the MIT Kerberos for Win32 distribution.
//MemoryCredentialsCache will provide future functions to access shared memeory cache on
//Windows platform. Native code implementation may be necessary.
/**
 * This class extends CredentialsCache. It is used for accessing data in shared memory
 * cache on Windows platforms.
 *
 * @author Yanni Zhang
 */
public abstract class MemoryCredentialsCache extends CredentialsCache {

    private static CredentialsCache getCCacheInstance(PrincipalName p) {
        return null;
    }

    private static CredentialsCache getCCacheInstance(PrincipalName p, File cacheFile) {
        return null;
    }


    public abstract boolean exists(String cache);

    public abstract void update(Credentials c);

    public abstract void save() throws IOException, KrbException;

    public abstract Credentials[] getCredsList();

    public abstract Credentials getCreds(PrincipalName sname) ;

    public abstract PrincipalName getPrimaryPrincipal();

}
