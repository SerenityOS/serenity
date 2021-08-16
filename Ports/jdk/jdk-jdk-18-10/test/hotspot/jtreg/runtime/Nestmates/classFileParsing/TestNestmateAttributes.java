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

/*
 * @test
 * @bug 8046171
 * @summary Test incorrect use of Nestmate related attributes
 * @compile TwoNestHost.jcod
 *          TwoNestMembers.jcod
 *          ConflictingAttributesInNestHost.jcod
 *          ConflictingAttributesInNestMember.jcod
 *          BadNestMembersLength.jcod
 *          BadNestMembersEntry.jcod
 *          BadNestHost.jcod
 *          BadNestHostLength.jcod
 * @run main TestNestmateAttributes
 */

public class TestNestmateAttributes {
    public static void main(String args[]) throws Throwable {
        String[] badClasses = new String[] {
            "NestmateAttributeHolder$TwoNestHost",
            "NestmateAttributeHolder",
            "ConflictingAttributesInNestHost",
            "NestmateAttributeHolder$ConflictingAttributesInNestMember",
            "BadNestMembersLength",
            "BadNestMembersEntry",
            "NestmateAttributeHolder$BadNestHost",
            "NestmateAttributeHolder$BadNestHostLength",
        };

        String[] messages = new String[] {
            "Multiple NestHost attributes in class file",
            "Multiple NestMembers attributes in class file",
            "Conflicting NestMembers and NestHost attributes",
            "Conflicting NestHost and NestMembers attributes",
            "Wrong NestMembers attribute length",
            "Nest member class_info_index 9 has bad constant type",
            "Nest-host class_info_index 10 has bad constant type",
            "Wrong NestHost attribute length",
        };

        for (int i = 0; i < badClasses.length; i++ ) {
            try {
                Class c = Class.forName(badClasses[i]);
                throw new Error("Missing ClassFormatError: " + messages[i]);
            }
            catch (ClassFormatError expected) {
                if (!expected.getMessage().contains(messages[i]))
                   throw new Error("Wrong ClassFormatError message: \"" +
                                   expected.getMessage() + "\" does not contain \"" +
                                   messages[i] + "\"");
                System.out.println("OK - got expected exception: " + expected);
            }
        }
    }
}
