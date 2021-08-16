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

import java.util.regex.Pattern;

/**
 * A line of text within the message file.
 * The lines form a doubly linked list for simple navigation.
 */
public class MessageLine {

    static final Pattern emptyOrCommentPattern = Pattern.compile("( *#.*)?");
    static final Pattern typePattern = Pattern.compile("[-\\\\'A-Z\\.a-z ]+( \\([-A-Za-z 0-9]+\\))?");
    static final Pattern infoPattern = Pattern.compile(String.format("# ([0-9]+: %s, )*[0-9]+: %s",
            typePattern.pattern(), typePattern.pattern()));

    public String text;
    MessageLine prev;
    MessageLine next;

    MessageLine(String text) {
        this.text = text;
    }

    public boolean isEmptyOrComment() {
        return emptyOrCommentPattern.matcher(text).matches();
    }

    public boolean isInfo() {
        return infoPattern.matcher(text).matches();
    }

    boolean hasContinuation() {
        return (next != null) && text.endsWith("\\");
    }

    MessageLine append(String text) {
        MessageLine l = new MessageLine(text);
        append(l);
        return l;
    }

    void append(MessageLine l) {
        assert l.prev == null && l.next == null;
        l.prev = this;
        l.next = next;
        if (next != null) {
            next.prev = l;
        }
        next = l;
    }
}
