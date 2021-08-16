/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package propertiesparser.parser;

import propertiesparser.parser.MessageType.CompoundType;
import propertiesparser.parser.MessageType.OrType;
import propertiesparser.parser.MessageType.SimpleType;
import propertiesparser.parser.MessageType.UnionType;

import java.util.ArrayList;
import java.util.List;

/**
 * An object to represent the comment that may precede the property
 * specification in a Message.
 * The comment is modelled as a list of fields, where the fields correspond
 * to the placeholder values (e.g. {0}, {1}, etc) within the message value.
 */
public final class MessageInfo {

    /** The fields of the Info object. */
    List<MessageType> types = new ArrayList<>();

    MessageInfo(String text) throws IllegalArgumentException {
        if (text != null) {
            if (!text.startsWith("# "))
                throw new IllegalArgumentException();
            String[] segs = text.substring(2).split(", ");
            types = new ArrayList<>();
            for (String seg : segs) {
                types.add(parseType(seg));
            }
        }
    }

    public List<MessageType> getTypes() {
        return types;
    }

    boolean isEmpty() {
        return types.isEmpty();
    }

    @Override
    public String toString() {
        return types.toString();
    }

    /**
     * Split the type comment into multiple alternatives (separated by 'or') - then parse each of them
     * individually and form an 'or' type.
     */
    MessageType parseType(String text) {
        int commentStart = text.indexOf("(");
        if (commentStart != -1) {
            //remove optional comment
            text = text.substring(0, commentStart);
        }
        text = text.substring(text.indexOf(": ") + 2);
        String[] alternatives = text.split(" " + OrType.OR_NAME + " ");
        MessageType[] types = new MessageType[alternatives.length];
        for (int i = 0 ; i < alternatives.length ; i++) {
            types[i] = parseAlternative(alternatives[i].trim());
        }
        return types.length > 1 ?
                new OrType(types) : types[0];
    }

    /**
     * Parse a subset of the type comment; valid matches are simple types, compound types,
     * union types and custom types.
     */
    MessageType parseAlternative(String text) {
        //try with custom types
        if (text.charAt(0) == '\'') {
            int end = text.indexOf('\'', 1);
            return new MessageType.CustomType(text.substring(1, end));
        }
        //try with simple types
        for (SimpleType st : SimpleType.values()) {
            if (text.equals(st.kindName())) {
                return st;
            }
        }
        //try with compound types
        for (CompoundType.Kind ck : CompoundType.Kind.values()) {
            if (text.startsWith(ck.kindName)) {
                MessageType elemtype = parseAlternative(text.substring(ck.kindName.length() + 1).trim());
                return new CompoundType(ck, elemtype);
            }
        }
        //try with union types
        for (UnionType.Kind uk : UnionType.Kind.values()) {
            if (text.startsWith(uk.kindName)) {
                return new UnionType(uk);
            }
        }
        //no match - report a warning
        System.err.println("WARNING - unrecognized type: " + text);
        return SimpleType.UNKNOWN;
    }

    /** Dummy message info to be used when no resource key comment is available. */
    static final MessageInfo dummyInfo = new MessageInfo(null);
}
