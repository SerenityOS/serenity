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

import java.util.ArrayList;
import java.util.List;

/**
 * A message within the message file.
 * A message is a series of lines containing a "name=value" property,
 * optionally preceded by a comment describing the use of placeholders
 * such as {0}, {1}, etc within the property value.
 */
public final class Message {
    final MessageLine firstLine;
    private MessageInfo messageInfo;

    Message(MessageLine l) {
        firstLine = l;
    }

    /**
     * Get the Info object for this message. It may be empty if there
     * if no comment preceding the property specification.
     */
    public MessageInfo getMessageInfo() {
        if (messageInfo == null) {
            MessageLine l = firstLine.prev;
            if (l != null && l.isInfo())
                messageInfo = new MessageInfo(l.text);
            else
                messageInfo = MessageInfo.dummyInfo;
        }
        return messageInfo;
    }

    /**
     * Get all the lines pertaining to this message.
     */
    public List<MessageLine> getLines(boolean includeAllPrecedingComments) {
        List<MessageLine> lines = new ArrayList<>();
        MessageLine l = firstLine;
        if (includeAllPrecedingComments) {
            // scan back to find end of prev message
            while (l.prev != null && l.prev.isEmptyOrComment())
                l = l.prev;
            // skip leading blank lines
            while (l.text.isEmpty())
                l = l.next;
        } else {
            if (l.prev != null && l.prev.isInfo())
                l = l.prev;
        }

        // include any preceding lines
        for ( ; l != firstLine; l = l.next)
            lines.add(l);

        // include message lines
        for (l = firstLine; l != null && l.hasContinuation(); l = l.next)
            lines.add(l);
        lines.add(l);

        // include trailing blank line if present
        l = l.next;
        if (l != null && l.text.isEmpty())
            lines.add(l);

        return lines;
    }
}
