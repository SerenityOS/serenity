/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

// Non-public classes so we can keep them all in this one source file

class HostOfMemberNoHost {
    // Missing NestHost attribute
    static class MemberNoHost {}
}

class HostOfMemberMissingHost {
    // Missing NestHost class
    static class MemberMissingHost {}
}

class HostOfMemberNotInstanceHost {
    // Invalid NestHost class (not instance class)
    static class MemberNotInstanceHost {
        Object[] oa; // create CP entry to use in jcod change
    }
}

class HostOfMemberNotOurHost {
    // Valid but different NestHost class
    static class MemberNotOurHost {}
}

class HostOfMemberMalformedHost {
    // Malformed NestHost class
    static class MemberMalformedHost {}
}

// Host lists itself as a member along
// with real member.
class HostWithSelfMember {
    static class Member {}
}

// Host lists duplicate members.
class HostWithDuplicateMembers {
    static class Member1 {}
    static interface Member2 {}
}
