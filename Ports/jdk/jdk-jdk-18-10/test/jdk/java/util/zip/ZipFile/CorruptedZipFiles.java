/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4770745 6218846 6218848 6237956
 * @summary test for correct detection and reporting of corrupted zip files
 * @author Martin Buchholz
 */

import java.util.*;
import java.util.zip.*;
import java.io.*;
import static java.lang.System.*;
import static java.util.zip.ZipFile.*;

public class CorruptedZipFiles {
    static int passed = 0, failed = 0;

    static void fail(String msg) {
        failed++;
        err.println(msg);
    }

    static void unexpected(Throwable t) {
        failed++;
        t.printStackTrace();
    }

    public static void main(String[] args) throws Exception {
        try (FileOutputStream fos = new FileOutputStream("x.zip");
             ZipOutputStream zos = new ZipOutputStream(fos))
        {
            ZipEntry e = new ZipEntry("x");
            zos.putNextEntry(e);
            zos.write((int)'x');
        }

        int len = (int)(new File("x.zip").length());
        byte[] good = new byte[len];
        try (FileInputStream fis = new FileInputStream("x.zip")) {
            fis.read(good);
        }
        new File("x.zip").delete();

        int endpos = len - ENDHDR;
        int cenpos = u16(good, endpos+ENDOFF);
        int locpos = u16(good, cenpos+CENOFF);
        if (u32(good, endpos) != ENDSIG) fail("Where's ENDSIG?");
        if (u32(good, cenpos) != CENSIG) fail("Where's CENSIG?");
        if (u32(good, locpos) != LOCSIG) fail("Where's LOCSIG?");
        if (u16(good, locpos+LOCNAM) != u16(good,cenpos+CENNAM))
            fail("Name field length mismatch");
        if (u16(good, locpos+LOCEXT) != u16(good,cenpos+CENEXT))
            fail("Extra field length mismatch");

        byte[] bad;

        err.println("corrupted ENDSIZ");
        bad = good.clone();
        bad[endpos+ENDSIZ]=(byte)0xff;
        checkZipException(bad, ".*bad central directory size.*");

        err.println("corrupted ENDOFF");
        bad = good.clone();
        bad[endpos+ENDOFF]=(byte)0xff;
        checkZipException(bad, ".*bad central directory offset.*");

        err.println("corrupted CENSIG");
        bad = good.clone();
        bad[cenpos]++;
        checkZipException(bad, ".*bad signature.*");

        err.println("corrupted CENFLG");
        bad = good.clone();
        bad[cenpos+CENFLG] |= 1;
        checkZipException(bad, ".*encrypted entry.*");

        err.println("corrupted CENNAM 1");
        bad = good.clone();
        bad[cenpos+CENNAM]++;
        checkZipException(bad, ".*bad header size.*");

        err.println("corrupted CENNAM 2");
        bad = good.clone();
        bad[cenpos+CENNAM]--;
        checkZipException(bad, ".*bad header size.*");

        err.println("corrupted CENNAM 3");
        bad = good.clone();
        bad[cenpos+CENNAM]   = (byte)0xfd;
        bad[cenpos+CENNAM+1] = (byte)0xfd;
        checkZipException(bad, ".*bad header size.*");

        err.println("corrupted CENEXT 1");
        bad = good.clone();
        bad[cenpos+CENEXT]++;
        checkZipException(bad, ".*bad header size.*");

        err.println("corrupted CENEXT 2");
        bad = good.clone();
        bad[cenpos+CENEXT]   = (byte)0xfd;
        bad[cenpos+CENEXT+1] = (byte)0xfd;
        checkZipException(bad, ".*bad header size.*");

        err.println("corrupted CENCOM");
        bad = good.clone();
        bad[cenpos+CENCOM]++;
        checkZipException(bad, ".*bad header size.*");

        err.println("corrupted CENHOW");
        bad = good.clone();
        bad[cenpos+CENHOW] = 2;
        checkZipException(bad, ".*bad compression method.*");

        err.println("corrupted LOCSIG");
        bad = good.clone();
        bad[locpos]++;
        checkZipExceptionInGetInputStream(bad, ".*bad signature.*");

        out.printf("passed = %d, failed = %d%n", passed, failed);
        if (failed > 0) throw new Exception("Some tests failed");
    }

    static int uniquifier = 432;

    static void checkZipExceptionImpl(byte[] data,
                                      String msgPattern,
                                      boolean getInputStream) {
        String zipName = "bad" + (uniquifier++) + ".zip";
        try {
            try (FileOutputStream fos = new FileOutputStream(zipName)) {
                fos.write(data);
            }
            try (ZipFile zf = new ZipFile(zipName)) {
                if (getInputStream) {
                    InputStream is = zf.getInputStream(new ZipEntry("x"));
                    is.read();
                }
            }
            fail("Failed to throw expected ZipException");
        } catch (ZipException e) {
            if (e.getMessage().matches(msgPattern))
                passed++;
            else
                unexpected(e);
        } catch (Throwable t) {
            unexpected(t);
        } finally {
            new File(zipName).delete();
        }
    }

    static void checkZipException(byte[] data, String msgPattern) {
        checkZipExceptionImpl(data, msgPattern, false);
    }

    static void checkZipExceptionInGetInputStream(byte[] data, String msgPattern) {
        checkZipExceptionImpl(data, msgPattern, true);
    }

    static int u8(byte[] data, int offset) {
        return data[offset]&0xff;
    }

    static int u16(byte[] data, int offset) {
        return u8(data,offset) + (u8(data,offset+1)<<8);
    }

    static int u32(byte[] data, int offset) {
        return u16(data,offset) + (u16(data,offset+2)<<16);
    }

    // The following can be deleted once this bug is fixed:
    // 6225935: "import static" accessibility rules for symbols different for no reason
    static final long LOCSIG = ZipFile.LOCSIG;
    static final long EXTSIG = ZipFile.EXTSIG;
    static final long CENSIG = ZipFile.CENSIG;
    static final long ENDSIG = ZipFile.ENDSIG;

    static final int LOCHDR = ZipFile.LOCHDR;
    static final int EXTHDR = ZipFile.EXTHDR;
    static final int CENHDR = ZipFile.CENHDR;
    static final int ENDHDR = ZipFile.ENDHDR;

    static final int LOCVER = ZipFile.LOCVER;
    static final int LOCFLG = ZipFile.LOCFLG;
    static final int LOCHOW = ZipFile.LOCHOW;
    static final int LOCTIM = ZipFile.LOCTIM;
    static final int LOCCRC = ZipFile.LOCCRC;
    static final int LOCSIZ = ZipFile.LOCSIZ;
    static final int LOCLEN = ZipFile.LOCLEN;
    static final int LOCNAM = ZipFile.LOCNAM;
    static final int LOCEXT = ZipFile.LOCEXT;

    static final int CENVEM = ZipFile.CENVEM;
    static final int CENVER = ZipFile.CENVER;
    static final int CENFLG = ZipFile.CENFLG;
    static final int CENHOW = ZipFile.CENHOW;
    static final int CENTIM = ZipFile.CENTIM;
    static final int CENCRC = ZipFile.CENCRC;
    static final int CENSIZ = ZipFile.CENSIZ;
    static final int CENLEN = ZipFile.CENLEN;
    static final int CENNAM = ZipFile.CENNAM;
    static final int CENEXT = ZipFile.CENEXT;
    static final int CENCOM = ZipFile.CENCOM;
    static final int CENDSK = ZipFile.CENDSK;
    static final int CENATT = ZipFile.CENATT;
    static final int CENATX = ZipFile.CENATX;
    static final int CENOFF = ZipFile.CENOFF;

    static final int ENDSUB = ZipFile.ENDSUB;
    static final int ENDTOT = ZipFile.ENDTOT;
    static final int ENDSIZ = ZipFile.ENDSIZ;
    static final int ENDOFF = ZipFile.ENDOFF;
    static final int ENDCOM = ZipFile.ENDCOM;
}
