/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.*;
import java.util.*;
import java.io.IOException;
import jdk.internal.misc.Unsafe;

import static sun.nio.fs.WindowsNativeDispatcher.*;
import static sun.nio.fs.WindowsConstants.*;

/**
 * A SecurityDescriptor for use when setting a file's ACL or creating a file
 * with an initial ACL.
 */

class WindowsSecurityDescriptor {
    private static final Unsafe unsafe = Unsafe.getUnsafe();

    /**
     * typedef struct _ACL {
     *     BYTE  AclRevision;
     *     BYTE  Sbz1;
     *     WORD  AclSize;
     *     WORD  AceCount;
     *     WORD  Sbz2;
     * } ACL;
     *
     * typedef struct _ACE_HEADER {
     *     BYTE AceType;
     *     BYTE AceFlags;
     *     WORD AceSize;
     * } ACE_HEADER;
     *
     * typedef struct _ACCESS_ALLOWED_ACE {
     *     ACE_HEADER Header;
     *     ACCESS_MASK Mask;
     *     DWORD SidStart;
     * } ACCESS_ALLOWED_ACE;
     *
     * typedef struct _ACCESS_DENIED_ACE {
     *     ACE_HEADER Header;
     *     ACCESS_MASK Mask;
     *     DWORD SidStart;
     * } ACCESS_DENIED_ACE;
     *
     * typedef struct _SECURITY_DESCRIPTOR {
     *     BYTE  Revision;
     *     BYTE  Sbz1;
     *     SECURITY_DESCRIPTOR_CONTROL Control;
     *     PSID Owner;
     *     PSID Group;
     *     PACL Sacl;
     *     PACL Dacl;
     * } SECURITY_DESCRIPTOR;
     */
    private static final short SIZEOF_ACL                   = 8;
    private static final short SIZEOF_ACCESS_ALLOWED_ACE    = 12;
    private static final short SIZEOF_ACCESS_DENIED_ACE     = 12;
    private static final short SIZEOF_SECURITY_DESCRIPTOR   = 20;

    private static final short OFFSETOF_TYPE                = 0;
    private static final short OFFSETOF_FLAGS               = 1;
    private static final short OFFSETOF_ACCESS_MASK         = 4;
    private static final short OFFSETOF_SID                 = 8;

    // null security descriptor
    private static final WindowsSecurityDescriptor NULL_DESCRIPTOR =
        new WindowsSecurityDescriptor();

    // native resources
    private final List<Long> sidList;
    private final NativeBuffer aclBuffer, sdBuffer;

    /**
     * Creates the "null" SecurityDescriptor
     */
    private WindowsSecurityDescriptor() {
        this.sidList = null;
        this.aclBuffer = null;
        this.sdBuffer = null;
    }

    /**
     * Creates a SecurityDescriptor from the given ACL
     */
    private WindowsSecurityDescriptor(List<AclEntry> acl) throws IOException {
        boolean initialized = false;

        // SECURITY: need to copy list in case size changes during processing
        acl = new ArrayList<AclEntry>(acl);

        // list of SIDs
        sidList = new ArrayList<Long>(acl.size());
        try {
            // initial size of ACL
            int size = SIZEOF_ACL;

            // get the SID for each entry
            for (AclEntry entry: acl) {
                UserPrincipal user = entry.principal();
                if (!(user instanceof WindowsUserPrincipals.User))
                    throw new ProviderMismatchException();
                String sidString = ((WindowsUserPrincipals.User)user).sidString();
                try {
                    long pSid = ConvertStringSidToSid(sidString);
                    sidList.add(pSid);

                    // increase size to allow for entry
                    size += GetLengthSid(pSid) +
                        Math.max(SIZEOF_ACCESS_ALLOWED_ACE, SIZEOF_ACCESS_DENIED_ACE);

                } catch (WindowsException x) {
                    throw new IOException("Failed to get SID for " + user.getName()
                        + ": " + x.errorString());
                }
            }

            // allocate memory for the ACL
            aclBuffer = NativeBuffers.getNativeBuffer(size);
            sdBuffer = NativeBuffers.getNativeBuffer(SIZEOF_SECURITY_DESCRIPTOR);

            InitializeAcl(aclBuffer.address(), size);

            // Add entry ACE to the ACL
            int i = 0;
            while (i < acl.size()) {
                AclEntry entry = acl.get(i);
                long pSid = sidList.get(i);
                try {
                    encode(entry, pSid, aclBuffer.address());
                } catch (WindowsException x) {
                    throw new IOException("Failed to encode ACE: " +
                        x.errorString());
                }
                i++;
            }

            // initialize security descriptor and set DACL
            InitializeSecurityDescriptor(sdBuffer.address());
            SetSecurityDescriptorDacl(sdBuffer.address(), aclBuffer.address());
            initialized = true;
        } catch (WindowsException x) {
            throw new IOException(x.getMessage());
        } finally {
            // release resources if not completely initialized
            if (!initialized)
                release();
        }
    }

    /**
     * Releases memory associated with SecurityDescriptor
     */
    void release() {
        if (sdBuffer != null)
            sdBuffer.release();
        if (aclBuffer != null)
            aclBuffer.release();
        if (sidList != null) {
            // release memory for SIDs
            for (Long sid: sidList) {
                LocalFree(sid);
            }
        }
    }

    /**
     * Returns address of SecurityDescriptor
     */
    long address() {
        return (sdBuffer == null) ? 0L : sdBuffer.address();
    }

    // decode Windows ACE to NFSv4 AclEntry
    private static AclEntry decode(long aceAddress)
        throws IOException
    {
        // map type
        byte aceType = unsafe.getByte(aceAddress + OFFSETOF_TYPE);
        if (aceType != ACCESS_ALLOWED_ACE_TYPE && aceType != ACCESS_DENIED_ACE_TYPE)
            return null;
        AclEntryType type;
        if (aceType == ACCESS_ALLOWED_ACE_TYPE) {
            type = AclEntryType.ALLOW;
        } else {
            type = AclEntryType.DENY;
        }

        // map flags
        byte aceFlags = unsafe.getByte(aceAddress + OFFSETOF_FLAGS);
        Set<AclEntryFlag> flags = EnumSet.noneOf(AclEntryFlag.class);
        if ((aceFlags & OBJECT_INHERIT_ACE) != 0)
            flags.add(AclEntryFlag.FILE_INHERIT);
        if ((aceFlags & CONTAINER_INHERIT_ACE) != 0)
            flags.add(AclEntryFlag.DIRECTORY_INHERIT);
        if ((aceFlags & NO_PROPAGATE_INHERIT_ACE) != 0)
            flags.add(AclEntryFlag.NO_PROPAGATE_INHERIT);
        if ((aceFlags & INHERIT_ONLY_ACE) != 0)
            flags.add(AclEntryFlag.INHERIT_ONLY);

        // map access mask
        int mask = unsafe.getInt(aceAddress + OFFSETOF_ACCESS_MASK);
        Set<AclEntryPermission> perms = EnumSet.noneOf(AclEntryPermission.class);
        if ((mask & FILE_READ_DATA) > 0)
            perms.add(AclEntryPermission.READ_DATA);
        if ((mask & FILE_WRITE_DATA) > 0)
            perms.add(AclEntryPermission.WRITE_DATA);
        if ((mask & FILE_APPEND_DATA ) > 0)
            perms.add(AclEntryPermission.APPEND_DATA);
        if ((mask & FILE_READ_EA) > 0)
            perms.add(AclEntryPermission.READ_NAMED_ATTRS);
        if ((mask & FILE_WRITE_EA) > 0)
            perms.add(AclEntryPermission.WRITE_NAMED_ATTRS);
        if ((mask & FILE_EXECUTE) > 0)
            perms.add(AclEntryPermission.EXECUTE);
        if ((mask & FILE_DELETE_CHILD ) > 0)
            perms.add(AclEntryPermission.DELETE_CHILD);
        if ((mask & FILE_READ_ATTRIBUTES) > 0)
            perms.add(AclEntryPermission.READ_ATTRIBUTES);
        if ((mask & FILE_WRITE_ATTRIBUTES) > 0)
            perms.add(AclEntryPermission.WRITE_ATTRIBUTES);
        if ((mask & DELETE) > 0)
            perms.add(AclEntryPermission.DELETE);
        if ((mask & READ_CONTROL) > 0)
            perms.add(AclEntryPermission.READ_ACL);
        if ((mask & WRITE_DAC) > 0)
            perms.add(AclEntryPermission.WRITE_ACL);
        if ((mask & WRITE_OWNER) > 0)
            perms.add(AclEntryPermission.WRITE_OWNER);
        if ((mask & SYNCHRONIZE) > 0)
            perms.add(AclEntryPermission.SYNCHRONIZE);

        // lookup SID to create UserPrincipal
        long sidAddress = aceAddress + OFFSETOF_SID;
        UserPrincipal user = WindowsUserPrincipals.fromSid(sidAddress);

        return AclEntry.newBuilder()
            .setType(type)
            .setPrincipal(user)
            .setFlags(flags).setPermissions(perms).build();
    }

    // encode NFSv4 AclEntry as Windows ACE to given ACL
    private static void encode(AclEntry ace, long sidAddress, long aclAddress)
        throws WindowsException
    {
        // ignore non-allow/deny entries for now
        if (ace.type() != AclEntryType.ALLOW && ace.type() != AclEntryType.DENY)
            return;
        boolean allow = (ace.type() == AclEntryType.ALLOW);

        // map access mask
        Set<AclEntryPermission> aceMask = ace.permissions();
        int mask = 0;
        if (aceMask.contains(AclEntryPermission.READ_DATA))
            mask |= FILE_READ_DATA;
        if (aceMask.contains(AclEntryPermission.WRITE_DATA))
            mask |= FILE_WRITE_DATA;
        if (aceMask.contains(AclEntryPermission.APPEND_DATA))
            mask |= FILE_APPEND_DATA;
        if (aceMask.contains(AclEntryPermission.READ_NAMED_ATTRS))
            mask |= FILE_READ_EA;
        if (aceMask.contains(AclEntryPermission.WRITE_NAMED_ATTRS))
            mask |= FILE_WRITE_EA;
        if (aceMask.contains(AclEntryPermission.EXECUTE))
            mask |= FILE_EXECUTE;
        if (aceMask.contains(AclEntryPermission.DELETE_CHILD))
            mask |= FILE_DELETE_CHILD;
        if (aceMask.contains(AclEntryPermission.READ_ATTRIBUTES))
            mask |= FILE_READ_ATTRIBUTES;
        if (aceMask.contains(AclEntryPermission.WRITE_ATTRIBUTES))
            mask |= FILE_WRITE_ATTRIBUTES;
        if (aceMask.contains(AclEntryPermission.DELETE))
            mask |= DELETE;
        if (aceMask.contains(AclEntryPermission.READ_ACL))
            mask |= READ_CONTROL;
        if (aceMask.contains(AclEntryPermission.WRITE_ACL))
            mask |= WRITE_DAC;
        if (aceMask.contains(AclEntryPermission.WRITE_OWNER))
            mask |= WRITE_OWNER;
        if (aceMask.contains(AclEntryPermission.SYNCHRONIZE))
            mask |= SYNCHRONIZE;

        // map flags
        Set<AclEntryFlag> aceFlags = ace.flags();
        byte flags = 0;
        if (aceFlags.contains(AclEntryFlag.FILE_INHERIT))
            flags |= OBJECT_INHERIT_ACE;
        if (aceFlags.contains(AclEntryFlag.DIRECTORY_INHERIT))
            flags |= CONTAINER_INHERIT_ACE;
        if (aceFlags.contains(AclEntryFlag.NO_PROPAGATE_INHERIT))
            flags |= NO_PROPAGATE_INHERIT_ACE;
        if (aceFlags.contains(AclEntryFlag.INHERIT_ONLY))
            flags |= INHERIT_ONLY_ACE;

        if (allow) {
            AddAccessAllowedAceEx(aclAddress, flags, mask, sidAddress);
        } else {
            AddAccessDeniedAceEx(aclAddress, flags, mask, sidAddress);
        }
    }

    /**
     * Creates a security descriptor with a DACL representing the given ACL.
     */
    static WindowsSecurityDescriptor create(List<AclEntry> acl)
        throws IOException
    {
        return new WindowsSecurityDescriptor(acl);
    }

    /**
     * Processes the array of attributes looking for the attribute "acl:acl".
     * Returns security descriptor representing the ACL or the "null" security
     * descriptor if the attribute is not in the array.
     */
    @SuppressWarnings("unchecked")
    static WindowsSecurityDescriptor fromAttribute(FileAttribute<?>... attrs)
        throws IOException
    {
        WindowsSecurityDescriptor sd = NULL_DESCRIPTOR;
        for (FileAttribute<?> attr: attrs) {
            // if more than one ACL specified then last one wins
            if (sd != NULL_DESCRIPTOR)
                sd.release();
            if (attr == null)
                throw new NullPointerException();
            if (attr.name().equals("acl:acl")) {
                List<AclEntry> acl = (List<AclEntry>)attr.value();
                sd = new WindowsSecurityDescriptor(acl);
            } else {
                throw new UnsupportedOperationException("'" + attr.name() +
                   "' not supported as initial attribute");
            }
        }
        return sd;
    }

    /**
     * Extracts DACL from security descriptor.
     */
    static List<AclEntry> getAcl(long pSecurityDescriptor) throws IOException {
        // get address of DACL
        long aclAddress = GetSecurityDescriptorDacl(pSecurityDescriptor);

        // get ACE count
        int aceCount = 0;
        if (aclAddress == 0L) {
            // no ACEs
            aceCount = 0;
        } else {
            AclInformation aclInfo = GetAclInformation(aclAddress);
            aceCount = aclInfo.aceCount();
        }
        ArrayList<AclEntry> result = new ArrayList<>(aceCount);

        // decode each of the ACEs to AclEntry objects
        for (int i=0; i<aceCount; i++) {
            long aceAddress = GetAce(aclAddress, i);
            AclEntry entry = decode(aceAddress);
            if (entry != null)
                result.add(entry);
        }
        return result;
    }
}
