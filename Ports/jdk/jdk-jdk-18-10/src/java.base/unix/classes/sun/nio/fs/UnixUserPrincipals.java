/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.attribute.*;
import java.io.IOException;
import static sun.nio.fs.UnixNativeDispatcher.*;

/**
 * Unix implementation of java.nio.file.attribute.UserPrincipal
 */

public class UnixUserPrincipals {
    private static User createSpecial(String name) { return new User(-1, name); }

    static final User SPECIAL_OWNER = createSpecial("OWNER@");
    static final User SPECIAL_GROUP = createSpecial("GROUP@");
    static final User SPECIAL_EVERYONE = createSpecial("EVERYONE@");

    static class User implements UserPrincipal {
        private final int id;             // uid or gid
        private final boolean isGroup;
        private final String name;

        private User(int id, boolean isGroup, String name) {
            this.id = id;
            this.isGroup = isGroup;
            this.name = name;
        }

        User(int id, String name) {
            this(id, false, name);
        }

        int uid() {
            if (isGroup)
                throw new AssertionError();
            return id;
        }

        int gid() {
            if (isGroup)
                return id;
            throw new AssertionError();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (!(obj instanceof User))
                return false;
            User other = (User)obj;
            if ((this.id != other.id) ||
                (this.isGroup != other.isGroup)) {
                return false;
            }
            // specials
            if (this.id == -1 && other.id == -1)
                return this.name.equals(other.name);

            return true;
        }

        @Override
        public int hashCode() {
            return (id != -1) ? id : name.hashCode();
        }
    }

    static class Group extends User implements GroupPrincipal {
        Group(int id, String name) {
            super(id, true, name);
        }
    }

    // return UserPrincipal representing given uid
    public static User fromUid(int uid) {
        String name;
        try {
            name = Util.toString(getpwuid(uid));
        } catch (UnixException x) {
            name = Integer.toString(uid);
        }
        return new User(uid, name);
    }

    // return GroupPrincipal representing given gid
    public static Group fromGid(int gid) {
        String name;
        try {
            name = Util.toString(getgrgid(gid));
        } catch (UnixException x) {
            name = Integer.toString(gid);
        }
        return new Group(gid, name);
    }

    // lookup user or group name
    private static int lookupName(String name, boolean isGroup)
        throws IOException
    {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("lookupUserInformation"));
        }
        int id;
        try {
            id = (isGroup) ? getgrnam(name) : getpwnam(name);
        } catch (UnixException x) {
            throw new IOException(name + ": " + x.errorString());
        }
        if (id == -1) {
            // lookup failed, allow input to be uid or gid
            try {
                id = Integer.parseInt(name);
            } catch (NumberFormatException ignore) {
                throw new UserPrincipalNotFoundException(name);
            }
        }
        return id;

    }

    // lookup user name
    static UserPrincipal lookupUser(String name) throws IOException {
        if (name.equals(SPECIAL_OWNER.getName()))
            return SPECIAL_OWNER;
        if (name.equals(SPECIAL_GROUP.getName()))
            return SPECIAL_GROUP;
        if (name.equals(SPECIAL_EVERYONE.getName()))
            return SPECIAL_EVERYONE;
        int uid = lookupName(name, false);
        return new User(uid, name);
    }

    // lookup group name
    static GroupPrincipal lookupGroup(String group)
        throws IOException
    {
        int gid = lookupName(group, true);
        return new Group(gid, group);
    }
}
