/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8264277
 * @library /test/lib
 * @modules jdk.httpserver
 *          java.base/jdk.internal.misc
 * @requires os.family != "windows"
 * @summary check permissions for XML signature
 */

import com.sun.net.httpserver.HttpServer;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.Proc;
import jdk.test.lib.security.XMLUtils;

import java.io.File;
import java.io.FilePermission;
import java.net.InetSocketAddress;
import java.net.SocketPermission;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

// Note: This test does not run fine on Windows because the format by
// Path.toUri.toString (file:///c:/path/to/file) is not supported by
// ResolverLocalFilesystem.translateUriToFilename.
public class FileSocketPermissions    {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            Path plain = Files.writeString(
                    Path.of(System.getProperty("user.dir"), "a.xml"), "<a>x</a>");
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/", ex -> {
                ex.sendResponseHeaders(200, 0);
                ex.getResponseBody().write("<a>x</a>".getBytes(StandardCharsets.UTF_8));
                ex.close();
            });
            server.start();
            try {
                String httpDoc = "http://localhost:" + server.getAddress().getPort() + "/b.xml";
                System.out.println(httpDoc);

                // No permission granted.
                Proc p0 = Proc.create("FileSocketPermissions")
                        .prop("java.security.manager", "")
                        .debug("S")
                        .args("sign", plain.toUri().toString(), httpDoc)
                        .start();
                Asserts.assertEQ(p0.readData(), "Error");
                Asserts.assertEQ(p0.readData(), "Error");

                // Permission to file and socket granted.
                Proc p = Proc.create("FileSocketPermissions")
                        .prop("java.security.manager", "")
                        .grant(new File(System.getProperty("test.classes")))
                        .perm(new FilePermission(plain.toString(), "read"))
                        .perm(new SocketPermission("localhost", "resolve,connect"))
                        .debug("S2")
                        .args("sign", plain.toUri().toString(), httpDoc)
                        .start();

                Proc p2 = Proc.create("FileSocketPermissions")
                        .prop("java.security.manager", "")
                        .grant(new File(System.getProperty("test.classes")))
                        .perm(new FilePermission(plain.toString(), "read"))
                        .perm(new SocketPermission("localhost", "resolve,connect"))
                        .debug("V")
                        .args("validate")
                        .start();

                while (true) {
                    String in = p.readData(); // read signed XML from signer
                    p2.println(in); // send signed XML to validator
                    if (in.equals("Over")) {
                        break;
                    }
                    if (!p2.readData().equals("true")) { // read validator result
                        throw new Exception("Validation error");
                    }
                }
            } finally {
                server.stop(0);
            }
        } else if (args[0].equals("sign")) {
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            KeyPair p = g.generateKeyPair();
            var signer = XMLUtils.signer(p.getPrivate(), p.getPublic());
            for (int i = 1; i < args.length; i++) {
                try {
                    // Multiple line XML. Send as raw bytes (in Base64)
                    Proc.binOut(XMLUtils.doc2string(signer.sign(new URI(args[i])))
                            .getBytes(StandardCharsets.UTF_8));
                } catch (Exception se) {
                    se.printStackTrace();
                    Proc.textOut("Error");
                }
            }
            Proc.textOut("Over");
        } else if (args[0].equals("validate")) {
            // Turn secureValidation off. Will read external data
            var validator = XMLUtils.validator().secureValidation(false);
            while (true) {
                String in = new String(Proc.binIn());
                if (in.equals("Over")) {
                    Proc.textOut("Over");
                    break;
                }
                Proc.textOut(Boolean.toString(validator.validate(XMLUtils.string2doc(in))));
            }
        }
    }
}
