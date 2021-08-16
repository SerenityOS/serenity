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

/* @test
 * @bug 7021870 8023431 8026756
 * @summary Reading last gzip chain member must not close the input stream.
 *          Garbage following gzip entry must be ignored.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class GZIPInZip {

    public static void main(String[] args)
            throws Throwable {

        doTest(false, false);
        doTest(false, true);
        doTest(true, false);
        doTest(true, true);
    }

    private static void doTest(final boolean appendGarbage,
                               final boolean limitGISBuff)
            throws Throwable {

        byte[] buf;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            final byte[] xbuf = { 'x' };

            zos.putNextEntry(new ZipEntry("a.gz"));
            GZIPOutputStream gos1 = new GZIPOutputStream(zos);
            gos1.write(xbuf);
            gos1.finish();
            if (appendGarbage)
                zos.write(xbuf);
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("b.gz"));
            GZIPOutputStream gos2 = new GZIPOutputStream(zos);
            gos2.write(xbuf);
            gos2.finish();
            zos.closeEntry();

            zos.flush();
            buf = baos.toByteArray();
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(buf);
             ZipInputStream zis = new ZipInputStream(bais)) {

            zis.getNextEntry();
            GZIPInputStream gis1 = limitGISBuff ?
                    new GZIPInputStream(zis, 4) :
                    new GZIPInputStream(zis);
            // try to read more than the entry has
            gis1.skip(2);

            try {
                zis.getNextEntry();
            } catch (IOException e) {
                throw new RuntimeException("ZIP stream was prematurely closed", e);
            }
        }
    }
}
