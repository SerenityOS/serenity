/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

//
// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 8169362
 * @summary Interop automated testing with Chrome
 * @library /test/lib
 * @modules jdk.crypto.ec
 *          java.base/sun.security.util
 * @run main/othervm ClientHelloChromeInterOp
 */

import java.util.Base64;
import jdk.test.lib.hexdump.HexPrinter;


public class ClientHelloChromeInterOp extends ClientHelloInterOp {
    // The ClientHello message.
    //
    // Captured from Chrome browser (version 54.0.2840.87 m (64-bit)) on
    // Windows 10.
    private final static String ClientHelloMsg =
        "FgMBAL4BAAC6AwOWBEueOntnurZ+WAW0D9Qn2HpdzXLu0MgDjsD9e5JU6AAAIsA\n" +
        "rwC/ALMAwzKnMqMwUzBPACcATwArAFACcAJ0ALwA1AAoBAABv/wEAAQAAAAATAB\n" +
        "EAAA53d3cub3JhY2xlLmNvbQAXAAAAIwAAAA0AEgAQBgEGAwUBBQMEAQQDAgECA\n" +
        "wAFAAUBAAAAAAASAAAAEAAOAAwCaDIIaHR0cC8xLjF1UAAAAAsAAgEAAAoACAAG\n" +
        "AB0AFwAY";

    /*
     * Main entry point for this test.
     */
    public static void main(String args[]) throws Exception {
        (new ClientHelloChromeInterOp()).run();
    }

    @Override
    protected byte[] createClientHelloMessage() {
        byte[] bytes = Base64.getMimeDecoder().decode(ClientHelloMsg);

        // Dump the hex codes of the ClientHello message so that developers
        // can easily check whether the message is captured correct or not.
        System.out.println("The ClientHello message used");
        try {
            HexPrinter.simple().format(bytes);
        } catch (Exception e) {
            // ignore
        }

        return bytes;
    }
}
