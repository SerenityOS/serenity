/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8016916
 * @summary UnstructuredName should support DirectoryString
 * @modules java.base/sun.security.pkcs10
 */

import java.util.Base64;
import sun.security.pkcs10.PKCS10;

public class UnstructuredName {

    // Certificate request with an Unstructured Name attribute
    static String csrStr =
        "MIIBtjCCAR8CAQAwEzERMA8GA1UEAxMIdGVzdE5hbWUwgZ8wDQYJKoZIhvcNAQEB\n" +
        "BQADgY0AMIGJAoGBAMTEIVCsM8IIhvsbzn6AwQFX5C8RGAWIrL6P5XEr1z+bvHx3\n" +
        "XhPD4tWLCR6CTKq0lTlo+QKKct7MUY7pdKShajpyYD+1YLgEve0nNd4r5kVUeoHe\n" +
        "CyIZoImONgAlmVD7M8IJjz2Vg84WVVjkHK67H5qt7Agi1hUnFGmRbJ8rbL7jAgMB\n" +
        "AAGgYzAXBgkqhkiG9w0BCQcxChMIcGFzc3dvcmQwHAYJKoZIhvcNAQkCMQ8TDW9w\n" +
        "dGlvbmFsIG5hbWUwKgYJKoZIhvcNAQkOMR0wGzAMBgNVHRMBAf8EAjAAMAsGA1Ud\n" +
        "DwQEAwIGQDANBgkqhkiG9w0BAQUFAAOBgQBc7ldGSmyCjMU+ssjglCimqknCVdig\n" +
        "N8FsI/aNRgLqf+eXKWZOxl1v3GB9HCXWDtqOnHd6AJKFpGtK0bqRu7bIncYIiQ1a\n" +
        "P1NW4Kup8d1fTPhw6xgYtxeHvUxRa2y4IXskPUYqp05HavfNZxmcJ5mZOLtgiDIC\n" +
        "I3J80saqEUQKqQ==";

    public static void main(String[] args) throws Exception {
        PKCS10 req = new PKCS10(Base64.getMimeDecoder().decode(csrStr));

        // If PKCS9Attribute did not accept the PrintableString ASN.1 tag,
        // this would fail with an IOException
        Object attr = req.getAttributes().getAttribute("1.2.840.113549.1.9.2");

        // Check that the attribute exists
        if (attr == null) {
            throw new Exception("Attribute should not be null.");
        }

        System.out.println("Test passed.");
    }
}
