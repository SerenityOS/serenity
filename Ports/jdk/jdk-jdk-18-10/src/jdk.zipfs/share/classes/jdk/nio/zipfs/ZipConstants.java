/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.nio.zipfs;

/**
 * @author Xueming Shen
 */
class ZipConstants {
    /*
     * Compression methods
     */
    static final int METHOD_STORED     = 0;
    static final int METHOD_DEFLATED   = 8;
    static final int METHOD_DEFLATED64 = 9;
    static final int METHOD_BZIP2      = 12;
    static final int METHOD_LZMA       = 14;
    static final int METHOD_LZ77       = 19;
    static final int METHOD_AES        = 99;

    /*
     * General purpose bit flag
     */
    static final int FLAG_ENCRYPTED  = 0x01;
    static final int FLAG_DATADESCR  = 0x08;    // crc, size and csize in dd
    static final int FLAG_USE_UTF8   = 0x800;   // If this bit is set the filename and
                                                // comment fields for this file must be
                                                // encoded using UTF-8.
    /*
     * Header signatures
     */
    static long LOCSIG = 0x04034b50L;   // "PK\003\004"
    static long EXTSIG = 0x08074b50L;   // "PK\007\008"
    static long CENSIG = 0x02014b50L;   // "PK\001\002"
    static long ENDSIG = 0x06054b50L;   // "PK\005\006"

    /*
     * Header sizes in bytes (including signatures)
     */
    static final int LOCHDR = 30;       // LOC header size
    static final int EXTHDR = 16;       // EXT header size
    static final int CENHDR = 46;       // CEN header size
    static final int ENDHDR = 22;       // END header size

    /*
     * File attribute compatibility types of CEN field "version made by"
     */
    static final int FILE_ATTRIBUTES_UNIX = 3; // Unix

    /*
     * Base values for CEN field "version made by"
     */
    static final int VERSION_MADE_BY_BASE_UNIX = FILE_ATTRIBUTES_UNIX << 8; // Unix

    /*
     * Local file (LOC) header field offsets
     */
    static final int LOCVER = 4;        // version needed to extract
    static final int LOCFLG = 6;        // general purpose bit flag
    static final int LOCHOW = 8;        // compression method
    static final int LOCTIM = 10;       // modification time
    static final int LOCCRC = 14;       // uncompressed file crc-32 value
    static final int LOCSIZ = 18;       // compressed size
    static final int LOCLEN = 22;       // uncompressed size
    static final int LOCNAM = 26;       // filename length
    static final int LOCEXT = 28;       // extra field length

    /*
     * Extra local (EXT) header field offsets
     */
    static final int EXTCRC = 4;        // uncompressed file crc-32 value
    static final int EXTSIZ = 8;        // compressed size
    static final int EXTLEN = 12;       // uncompressed size

    /*
     * Central directory (CEN) header field offsets
     */
    static final int CENVEM = 4;        // version made by
    static final int CENVER = 6;        // version needed to extract
    static final int CENFLG = 8;        // encrypt, decrypt flags
    static final int CENHOW = 10;       // compression method
    static final int CENTIM = 12;       // modification time
    static final int CENCRC = 16;       // uncompressed file crc-32 value
    static final int CENSIZ = 20;       // compressed size
    static final int CENLEN = 24;       // uncompressed size
    static final int CENNAM = 28;       // filename length
    static final int CENEXT = 30;       // extra field length
    static final int CENCOM = 32;       // comment length
    static final int CENDSK = 34;       // disk number start
    static final int CENATT = 36;       // internal file attributes
    static final int CENATX = 38;       // external file attributes
    static final int CENOFF = 42;       // LOC header offset

    /*
     * End of central directory (END) header field offsets
     */
    static final int ENDSUB = 8;        // number of entries on this disk
    static final int ENDTOT = 10;       // total number of entries
    static final int ENDSIZ = 12;       // central directory size in bytes
    static final int ENDOFF = 16;       // offset of first CEN header
    static final int ENDCOM = 20;       // zip file comment length

    /*
     * ZIP64 constants
     */
    static final long ZIP64_ENDSIG = 0x06064b50L;  // "PK\006\006"
    static final long ZIP64_LOCSIG = 0x07064b50L;  // "PK\006\007"
    static final int  ZIP64_ENDHDR = 56;           // ZIP64 end header size
    static final int  ZIP64_LOCHDR = 20;           // ZIP64 end loc header size
    static final int  ZIP64_EXTHDR = 24;           // EXT header size
    static final int  ZIP64_EXTID  = 0x0001;       // Extra field Zip64 header ID

    static final int  ZIP64_MINVAL32 = 0xFFFF;
    static final long ZIP64_MINVAL = 0xFFFFFFFFL;

    /*
     * Zip64 End of central directory (END) header field offsets
     */
    static final int  ZIP64_ENDLEN = 4;       // size of zip64 end of central dir
    static final int  ZIP64_ENDVEM = 12;      // version made by
    static final int  ZIP64_ENDVER = 14;      // version needed to extract
    static final int  ZIP64_ENDNMD = 16;      // number of this disk
    static final int  ZIP64_ENDDSK = 20;      // disk number of start
    static final int  ZIP64_ENDTOD = 24;      // total number of entries on this disk
    static final int  ZIP64_ENDTOT = 32;      // total number of entries
    static final int  ZIP64_ENDSIZ = 40;      // central directory size in bytes
    static final int  ZIP64_ENDOFF = 48;      // offset of first CEN header
    static final int  ZIP64_ENDEXT = 56;      // zip64 extensible data sector

    /*
     * Zip64 End of central directory locator field offsets
     */
    static final int  ZIP64_LOCDSK = 4;       // disk number start
    static final int  ZIP64_LOCOFF = 8;       // offset of zip64 end
    static final int  ZIP64_LOCTOT = 16;      // total number of disks

    /*
     * Zip64 Extra local (EXT) header field offsets
     */
    static final int  ZIP64_EXTCRC = 4;       // uncompressed file crc-32 value
    static final int  ZIP64_EXTSIZ = 8;       // compressed size, 8-byte
    static final int  ZIP64_EXTLEN = 16;      // uncompressed size, 8-byte

    /*
     * Extra field header ID
     */
    static final int  EXTID_ZIP64 = 0x0001;      // ZIP64
    static final int  EXTID_NTFS  = 0x000a;      // NTFS
    static final int  EXTID_UNIX  = 0x000d;      // UNIX
    static final int  EXTID_EFS   = 0x0017;      // Strong Encryption
    static final int  EXTID_EXTT  = 0x5455;      // Info-ZIP Extended Timestamp

    /*
     * fields access methods
     */
    ///////////////////////////////////////////////////////
    static final int CH(byte[] b, int n) {
        return Byte.toUnsignedInt(b[n]);
    }

    static final int SH(byte[] b, int n) {
        return Byte.toUnsignedInt(b[n]) | (Byte.toUnsignedInt(b[n + 1]) << 8);
    }

    static final long LG(byte[] b, int n) {
        return ((SH(b, n)) | (SH(b, n + 2) << 16)) & 0xffffffffL;
    }

    static final long LL(byte[] b, int n) {
        return (LG(b, n)) | (LG(b, n + 4) << 32);
    }

    static long getSig(byte[] b, int n) { return LG(b, n); }

    private static boolean pkSigAt(byte[] b, int n, int b1, int b2) {
        return b[n] == 'P' & b[n + 1] == 'K' & b[n + 2] == b1 & b[n + 3] == b2;
    }

    static boolean cenSigAt(byte[] b, int n) { return pkSigAt(b, n, 1, 2); }
    static boolean locSigAt(byte[] b, int n) { return pkSigAt(b, n, 3, 4); }
    static boolean endSigAt(byte[] b, int n) { return pkSigAt(b, n, 5, 6); }
    static boolean extSigAt(byte[] b, int n) { return pkSigAt(b, n, 7, 8); }
    static boolean end64SigAt(byte[] b, int n) { return pkSigAt(b, n, 6, 6); }
    static boolean locator64SigAt(byte[] b, int n) { return pkSigAt(b, n, 6, 7); }

    // local file (LOC) header fields
    static final long LOCSIG(byte[] b) { return LG(b, 0); } // signature
    static final int  LOCVER(byte[] b) { return SH(b, 4); } // version needed to extract
    static final int  LOCFLG(byte[] b) { return SH(b, 6); } // general purpose bit flags
    static final int  LOCHOW(byte[] b) { return SH(b, 8); } // compression method
    static final long LOCTIM(byte[] b) { return LG(b, 10);} // modification time
    static final long LOCCRC(byte[] b) { return LG(b, 14);} // crc of uncompressed data
    static final long LOCSIZ(byte[] b) { return LG(b, 18);} // compressed data size
    static final long LOCLEN(byte[] b) { return LG(b, 22);} // uncompressed data size
    static final int  LOCNAM(byte[] b) { return SH(b, 26);} // filename length
    static final int  LOCEXT(byte[] b) { return SH(b, 28);} // extra field length

    // extra local (EXT) header fields
    static final long EXTCRC(byte[] b) { return LG(b, 4);}  // crc of uncompressed data
    static final long EXTSIZ(byte[] b) { return LG(b, 8);}  // compressed size
    static final long EXTLEN(byte[] b) { return LG(b, 12);} // uncompressed size

    // end of central directory header (END) fields
    static final int  ENDSUB(byte[] b) { return SH(b, 8); }  // number of entries on this disk
    static final int  ENDTOT(byte[] b) { return SH(b, 10);}  // total number of entries
    static final long ENDSIZ(byte[] b) { return LG(b, 12);}  // central directory size
    static final long ENDOFF(byte[] b) { return LG(b, 16);}  // central directory offset
    static final int  ENDCOM(byte[] b) { return SH(b, 20);}  // size of zip file comment
    static final int  ENDCOM(byte[] b, int off) { return SH(b, off + 20);}

    // zip64 end of central directory recoder fields
    static final long ZIP64_ENDTOD(byte[] b) { return LL(b, 24);}  // total number of entries on disk
    static final long ZIP64_ENDTOT(byte[] b) { return LL(b, 32);}  // total number of entries
    static final long ZIP64_ENDSIZ(byte[] b) { return LL(b, 40);}  // central directory size
    static final long ZIP64_ENDOFF(byte[] b) { return LL(b, 48);}  // central directory offset
    static final long ZIP64_LOCOFF(byte[] b) { return LL(b, 8);}   // zip64 end offset

    // central directory header (CEN) fields
    static final long CENSIG(byte[] b, int pos) { return LG(b, pos + 0); } // signature
    static final int  CENVEM(byte[] b, int pos) { return SH(b, pos + 4); } // version made by
    static final int  CENVEM_FA(byte[] b, int pos) { return CH(b, pos + 5); } // file attribute compatibility
    static final int  CENVER(byte[] b, int pos) { return SH(b, pos + 6); } // version needed to extract
    static final int  CENFLG(byte[] b, int pos) { return SH(b, pos + 8); } // encrypt, decrypt flags
    static final int  CENHOW(byte[] b, int pos) { return SH(b, pos + 10);} // compression method
    static final long CENTIM(byte[] b, int pos) { return LG(b, pos + 12);} // modification time
    static final long CENCRC(byte[] b, int pos) { return LG(b, pos + 16);} // uncompressed file crc-32 value
    static final long CENSIZ(byte[] b, int pos) { return LG(b, pos + 20);} // compressed size
    static final long CENLEN(byte[] b, int pos) { return LG(b, pos + 24);} // uncompressed size
    static final int  CENNAM(byte[] b, int pos) { return SH(b, pos + 28);} // filename length
    static final int  CENEXT(byte[] b, int pos) { return SH(b, pos + 30);} // extra field length
    static final int  CENCOM(byte[] b, int pos) { return SH(b, pos + 32);} // comment length
    static final int  CENDSK(byte[] b, int pos) { return SH(b, pos + 34);} // disk number start
    static final int  CENATT(byte[] b, int pos) { return SH(b, pos + 36);} // internal file attributes
    static final long CENATX(byte[] b, int pos) { return LG(b, pos + 38);} // external file attributes
    static final int  CENATX_PERMS(byte[] b, int pos) { return SH(b, pos + 40);} // posix permission data
    static final long CENOFF(byte[] b, int pos) { return LG(b, pos + 42);} // LOC header offset

    /* The END header is followed by a variable length comment of size < 64k. */
    static final long END_MAXLEN = 0xFFFF + ENDHDR;
    static final int READBLOCKSZ = 128;
}
