/*
 * Copyright (c) 2014, Red Hat, Inc.
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
 * @bug 8035105
 * @summary DNS resource record parsing
 * @modules jdk.naming.dns/com.sun.jndi.dns:+open
 */

import com.sun.jndi.dns.ResourceRecord;
import javax.naming.CommunicationException;
import javax.naming.InvalidNameException;;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

public class Parser {
    static Constructor<ResourceRecord> rrConstructor;
    static {
        try {
            rrConstructor = ResourceRecord.class.getDeclaredConstructor(
                byte[].class, int.class, int.class, boolean.class,
                boolean.class);
            rrConstructor.setAccessible(true);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    static ResourceRecord parse(String data, int offset, boolean qSection)
        throws Throwable {
        byte[] bytes = data.getBytes(ISO_8859_1);
        try {
            return rrConstructor.newInstance(
                bytes, bytes.length, offset, qSection, !qSection);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public static void main(String[] args) throws Throwable {
        ResourceRecord rr;

        rr = parse("\003www\007example\003com\000\000\002\000\001",
            0, true);
        if (!rr.getName().toString().equals("www.example.com."))
            throw new AssertionError(rr.getName().toString());
        if (rr.getRrclass() != 1)
            throw new AssertionError("RCLASS: " + rr.getRrclass());
        if (rr.getType() != 2)
            throw new AssertionError("RTYPE: " + rr.getType());

        String longLabel = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" +
            "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";

        rr = parse("\077" + longLabel + "\077" + longLabel +
            "\077" + longLabel + "\061" + longLabel.substring(0, 49) +
            "\007example\003com\000\000\002\000\001",
             0, true);
        if (!rr.getName().toString().equals(longLabel +
            '.' + longLabel + '.' + longLabel +
            '.' + longLabel.substring(0, 49) + ".example.com."))
            throw new AssertionError(rr.getName().toString());
        if (rr.getRrclass() != 1)
            throw new AssertionError("RCLASS: " + rr.getRrclass());
        if (rr.getType() != 2)
            throw new AssertionError("RTYPE: " + rr.getType());

        rr = parse("1-2-3-4-5-6-" +
            "\003www\007example\003com\000\000\002\000\001" +
            "\300\014\000\002\000\001\000\001\121\200" +
            "\000\005\002ns\300\020",
            33, false);
        if (!rr.getName().toString().equals("www.example.com."))
            throw new AssertionError(rr.getName().toString());
        if (rr.getRrclass() != 1)
            throw new AssertionError("RCLASS: " + rr.getRrclass());
        if (rr.getType() != 2)
            throw new AssertionError("RTYPE: " + rr.getType());
        if (!rr.getRdata().toString().equals("ns.example.com."))
            throw new AssertionError("RDATA: " + rr.getRdata());

        try {
            parse("1-2-3-4-5-6-" +
                "\003www\007example\003com\000\000\002\000\001" +
                "\300\014\000\002\000\001\300\051\300\047" +
                "\000\005\002ns\300\051",
                33, false);
            throw new AssertionError();
        } catch (CommunicationException e) {
            if (!e.getMessage().equals("DNS error: malformed packet")
                || e.getCause().getClass() != IOException.class
                || !e.getCause().getMessage().equals(
                    "Too many compression references"))
                throw e;
        }

        try {
            String longLabel62 = "\076" + longLabel.substring(1);
            parse(longLabel62 + longLabel62 + longLabel62 + longLabel62 +
                "\002XX\000\000\002\000\001", 0, true);
            throw new AssertionError();
        } catch (CommunicationException e) {
            if (!e.getMessage().equals("DNS error: malformed packet")
                || e.getCause().getClass() != InvalidNameException.class
                || !e.getCause().getMessage().equals("Name too long"))
                throw e;
        }
        try {
            parse("\100Y" + longLabel + "\000\000\002\000\001", 0, true);
            throw new AssertionError();
        } catch (CommunicationException e) {
            if (!e.getMessage().equals("DNS error: malformed packet")
                || e.getCause().getClass() != IOException.class
                || !e.getCause().getMessage().equals("Invalid label type: 64"))
                throw e;
        }
    }
}
