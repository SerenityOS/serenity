/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8191073
 * @summary Test verifies that when user tries to read image data from a
 *          tables-only image input stream it should through IIOException
 *          instead of throwing any other exception as per specification.
 * @run     main JpegTablesOnlyReadTest
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;

public class JpegTablesOnlyReadTest {
    // JPEG input stream containing tables-only image
    private static String inputImageBase64 = "/9j/4IAQSkZJRgABAQEASABIAAD"
            + "/2wBDAFA3PEY8MlBGQUZaVVBfeMiCeG5uePWvuZHI//////////////////////"
            + "//////////////////////////////2wBDAVVaWnhpeOuCguv//////////////"
            + "///////////////////////////////////////////////////////////wAAR"
            + "CAAgACADASIAAhEBAxEB/8QAFwAAAwEAAAAAAAAAAAAAAAAAAAECA//EACUQAQA"
            + "CAAUDBAMAAAAAAAAAAAEAAhESITGxQXKSA2Fi0SIyUf/EABYBAQEBAAAAAAAAAA"
            + "AAAAAAAAABA//EABcRAQEBAQAAAAAAAAAAAAAAAAEAESH/2gAMAwEAAhEDEQA/A"
            + "Nf2VW2OKaWTqnRhl97eb9wrs91uWPEBUX+EtmrssvvbzfuJWjVG2tg1svLLtgJ0"
            + "Uxwmd96d5zE7tVdnutyxm5JVoo0u6rpXHdWLP8PU8WIjtRuvVZN96d5zDP8AD1P"
            + "Fhre1Apc/Ida4RAdv/9k=";

    public static void main(String[] args) throws IOException {
        byte[] inputBytes = Base64.getDecoder().decode(inputImageBase64);
        InputStream in = new ByteArrayInputStream(inputBytes);

        // Read tables-only JPEG image
        try {
            ImageIO.read(in);
        } catch (IIOException e) {
            // do nothing we expect it to throw IIOException if it throws
            // any other exception test will fail.
        }
    }
}

