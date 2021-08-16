/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.util.zip;

import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Vector;
import java.util.HashSet;
import static java.util.zip.ZipConstants64.*;
import static java.util.zip.ZipUtils.*;
import sun.nio.cs.UTF_8;
import sun.security.action.GetPropertyAction;

/**
 * This class implements an output stream filter for writing files in the
 * ZIP file format. Includes support for both compressed and uncompressed
 * entries.
 *
 * @author      David Connelly
 * @since 1.1
 */
public class ZipOutputStream extends DeflaterOutputStream implements ZipConstants {

    /**
     * Whether to use ZIP64 for zip files with more than 64k entries.
     * Until ZIP64 support in zip implementations is ubiquitous, this
     * system property allows the creation of zip files which can be
     * read by legacy zip implementations which tolerate "incorrect"
     * total entry count fields, such as the ones in jdk6, and even
     * some in jdk7.
     */
    private static final boolean inhibitZip64 =
        Boolean.parseBoolean(
            GetPropertyAction.privilegedGetProperty("jdk.util.zip.inhibitZip64"));

    private static class XEntry {
        final ZipEntry entry;
        final long offset;
        public XEntry(ZipEntry entry, long offset) {
            this.entry = entry;
            this.offset = offset;
        }
    }

    private XEntry current;
    private Vector<XEntry> xentries = new Vector<>();
    private HashSet<String> names = new HashSet<>();
    private CRC32 crc = new CRC32();
    private long written = 0;
    private long locoff = 0;
    private byte[] comment;
    private int method = DEFLATED;
    private boolean finished;

    private boolean closed = false;

    private final ZipCoder zc;

    private static int version(ZipEntry e) throws ZipException {
        return switch (e.method) {
        case DEFLATED -> 20;
        case STORED   -> 10;
        default       -> throw new ZipException("unsupported compression method");
        };
    }

    /**
     * Checks to make sure that this stream has not been closed.
     */
    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }

    /**
     * Compression method for uncompressed (STORED) entries.
     */
    public static final int STORED = ZipEntry.STORED;

    /**
     * Compression method for compressed (DEFLATED) entries.
     */
    public static final int DEFLATED = ZipEntry.DEFLATED;

    /**
     * Creates a new ZIP output stream.
     *
     * <p>The UTF-8 {@link java.nio.charset.Charset charset} is used
     * to encode the entry names and comments.
     *
     * @param out the actual output stream
     */
    public ZipOutputStream(OutputStream out) {
        this(out, UTF_8.INSTANCE);
    }

    /**
     * Creates a new ZIP output stream.
     *
     * @param out the actual output stream
     *
     * @param charset the {@linkplain java.nio.charset.Charset charset}
     *                to be used to encode the entry names and comments
     *
     * @since 1.7
     */
    public ZipOutputStream(OutputStream out, Charset charset) {
        super(out, out != null ? new Deflater(Deflater.DEFAULT_COMPRESSION, true) : null);
        if (charset == null)
            throw new NullPointerException("charset is null");
        this.zc = ZipCoder.get(charset);
        usesDefaultDeflater = true;
    }

    /**
     * Sets the ZIP file comment.
     * @param     comment the comment string
     * @throws    IllegalArgumentException if the length of the specified
     *            ZIP file comment is greater than 0xFFFF bytes
     */
    public void setComment(String comment) {
        if (comment != null) {
            this.comment = zc.getBytes(comment);
            if (this.comment.length > 0xffff)
                throw new IllegalArgumentException("ZIP file comment too long.");
        }
    }

    /**
     * Sets the default compression method for subsequent entries. This
     * default will be used whenever the compression method is not specified
     * for an individual ZIP file entry, and is initially set to DEFLATED.
     * @param     method the default compression method
     * @throws    IllegalArgumentException if the specified compression method
     *            is invalid
     */
    public void setMethod(int method) {
        if (method != DEFLATED && method != STORED) {
            throw new IllegalArgumentException("invalid compression method");
        }
        this.method = method;
    }

    /**
     * Sets the compression level for subsequent entries which are DEFLATED.
     * The default setting is DEFAULT_COMPRESSION.
     * @param     level the compression level (0-9)
     * @throws    IllegalArgumentException if the compression level is invalid
     */
    public void setLevel(int level) {
        def.setLevel(level);
    }

    /**
     * Begins writing a new ZIP file entry and positions the stream to the
     * start of the entry data. Closes the current entry if still active.
     * <p>
     * The default compression method will be used if no compression method
     * was specified for the entry. When writing a compressed (DEFLATED)
     * entry, and the compressed size has not been explicitly set with the
     * {@link ZipEntry#setCompressedSize(long)} method, then the compressed
     * size will be set to the actual compressed size after deflation.
     * <p>
     * The current time will be used if the entry has no set modification time.
     *
     * @param     e the ZIP entry to be written
     * @throws    ZipException if a ZIP format error has occurred
     * @throws    IOException if an I/O error has occurred
     */
    public void putNextEntry(ZipEntry e) throws IOException {
        ensureOpen();
        if (current != null) {
            closeEntry();       // close previous entry
        }
        if (e.xdostime == -1) {
            // by default, do NOT use extended timestamps in extra
            // data, for now.
            e.setTime(System.currentTimeMillis());
        }
        if (e.method == -1) {
            e.method = method;  // use default method
        }
        // store size, compressed size, and crc-32 in LOC header
        e.flag = 0;
        switch (e.method) {
        case DEFLATED:
            // If not set, store size, compressed size, and crc-32 in data
            // descriptor immediately following the compressed entry data.
            // Ignore the compressed size of a ZipEntry if it was implcitely set
            // while reading that ZipEntry from a  ZipFile or ZipInputStream because
            // we can't know the compression level of the source zip file/stream.
            if (e.size  == -1 || e.csize == -1 || e.crc   == -1 || !e.csizeSet) {
                e.flag = 8;
            }
            break;
        case STORED:
            // compressed size, uncompressed size, and crc-32 must all be
            // set for entries using STORED compression method
            if (e.size == -1) {
                e.size = e.csize;
            } else if (e.csize == -1) {
                e.csize = e.size;
            } else if (e.size != e.csize) {
                throw new ZipException(
                    "STORED entry where compressed != uncompressed size");
            }
            if (e.size == -1 || e.crc == -1) {
                throw new ZipException(
                    "STORED entry missing size, compressed size, or crc-32");
            }
            break;
        default:
            throw new ZipException("unsupported compression method");
        }
        if (! names.add(e.name)) {
            throw new ZipException("duplicate entry: " + e.name);
        }
        if (zc.isUTF8())
            e.flag |= USE_UTF8;
        current = new XEntry(e, written);
        xentries.add(current);
        writeLOC(current);
    }

    /**
     * Closes the current ZIP entry and positions the stream for writing
     * the next entry.
     * @throws    ZipException if a ZIP format error has occurred
     * @throws    IOException if an I/O error has occurred
     */
    public void closeEntry() throws IOException {
        ensureOpen();
        if (current != null) {
            ZipEntry e = current.entry;
            switch (e.method) {
            case DEFLATED -> {
                def.finish();
                while (!def.finished()) {
                    deflate();
                }
                if ((e.flag & 8) == 0) {
                    // verify size, compressed size, and crc-32 settings
                    if (e.size != def.getBytesRead()) {
                        throw new ZipException(
                            "invalid entry size (expected " + e.size +
                            " but got " + def.getBytesRead() + " bytes)");
                    }
                    if (e.csize != def.getBytesWritten()) {
                        throw new ZipException(
                            "invalid entry compressed size (expected " +
                            e.csize + " but got " + def.getBytesWritten() + " bytes)");
                    }
                    if (e.crc != crc.getValue()) {
                        throw new ZipException(
                            "invalid entry CRC-32 (expected 0x" +
                            Long.toHexString(e.crc) + " but got 0x" +
                            Long.toHexString(crc.getValue()) + ")");
                    }
                } else {
                    e.size = def.getBytesRead();
                    e.csize = def.getBytesWritten();
                    e.crc = crc.getValue();
                    writeEXT(e);
                }
                def.reset();
                written += e.csize;
            }
            case STORED -> {
                // we already know that both e.size and e.csize are the same
                if (e.size != written - locoff) {
                    throw new ZipException(
                        "invalid entry size (expected " + e.size +
                        " but got " + (written - locoff) + " bytes)");
                }
                if (e.crc != crc.getValue()) {
                    throw new ZipException(
                        "invalid entry crc-32 (expected 0x" +
                        Long.toHexString(e.crc) + " but got 0x" +
                        Long.toHexString(crc.getValue()) + ")");
                }
            }
            default -> throw new ZipException("invalid compression method");
            }
            crc.reset();
            current = null;
        }
    }

    /**
     * Writes an array of bytes to the current ZIP entry data. This method
     * will block until all the bytes are written.
     * @param     b the data to be written
     * @param     off the start offset in the data
     * @param     len the number of bytes that are written
     * @throws    ZipException if a ZIP file error has occurred
     * @throws    IOException if an I/O error has occurred
     */
    public synchronized void write(byte[] b, int off, int len)
        throws IOException
    {
        ensureOpen();
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        if (current == null) {
            throw new ZipException("no current ZIP entry");
        }
        ZipEntry entry = current.entry;
        switch (entry.method) {
            case DEFLATED -> super.write(b, off, len);
            case STORED -> {
                written += len;
                if (written - locoff > entry.size) {
                    throw new ZipException(
                        "attempt to write past end of STORED entry");
                }
                out.write(b, off, len);
            }
            default -> throw new ZipException("invalid compression method");
        }
        crc.update(b, off, len);
    }

    /**
     * Finishes writing the contents of the ZIP output stream without closing
     * the underlying stream. Use this method when applying multiple filters
     * in succession to the same output stream.
     * @throws    ZipException if a ZIP file error has occurred
     * @throws    IOException if an I/O exception has occurred
     */
    public void finish() throws IOException {
        ensureOpen();
        if (finished) {
            return;
        }
        if (current != null) {
            closeEntry();
        }
        // write central directory
        long off = written;
        for (XEntry xentry : xentries)
            writeCEN(xentry);
        writeEND(off, written - off);
        finished = true;
    }

    /**
     * Closes the ZIP output stream as well as the stream being filtered.
     * @throws    ZipException if a ZIP file error has occurred
     * @throws    IOException if an I/O error has occurred
     */
    public void close() throws IOException {
        if (!closed) {
            super.close();
            closed = true;
        }
    }

    /*
     * Writes local file (LOC) header for specified entry.
     */
    private void writeLOC(XEntry xentry) throws IOException {
        ZipEntry e = xentry.entry;
        int flag = e.flag;
        boolean hasZip64 = false;
        int elen = getExtraLen(e.extra);

        writeInt(LOCSIG);               // LOC header signature
        if ((flag & 8) == 8) {
            writeShort(version(e));     // version needed to extract
            writeShort(flag);           // general purpose bit flag
            writeShort(e.method);       // compression method
            writeInt(e.xdostime);       // last modification time
            // store size, uncompressed size, and crc-32 in data descriptor
            // immediately following compressed entry data
            writeInt(0);
            writeInt(0);
            writeInt(0);
        } else {
            if (e.csize >= ZIP64_MAGICVAL || e.size >= ZIP64_MAGICVAL) {
                hasZip64 = true;
                writeShort(45);         // ver 4.5 for zip64
            } else {
                writeShort(version(e)); // version needed to extract
            }
            writeShort(flag);           // general purpose bit flag
            writeShort(e.method);       // compression method
            writeInt(e.xdostime);       // last modification time
            writeInt(e.crc);            // crc-32
            if (hasZip64) {
                writeInt(ZIP64_MAGICVAL);
                writeInt(ZIP64_MAGICVAL);
                elen += 20;        //headid(2) + size(2) + size(8) + csize(8)
            } else {
                writeInt(e.csize);  // compressed size
                writeInt(e.size);   // uncompressed size
            }
        }
        byte[] nameBytes = zc.getBytes(e.name);
        writeShort(nameBytes.length);

        int elenEXTT = 0;         // info-zip extended timestamp
        int flagEXTT = 0;
        long umtime = -1;
        long uatime = -1;
        long uctime = -1;
        if (e.mtime != null) {
            elenEXTT += 4;
            flagEXTT |= EXTT_FLAG_LMT;
            umtime = fileTimeToUnixTime(e.mtime);
        }
        if (e.atime != null) {
            elenEXTT += 4;
            flagEXTT |= EXTT_FLAG_LAT;
            uatime = fileTimeToUnixTime(e.atime);
        }
        if (e.ctime != null) {
            elenEXTT += 4;
            flagEXTT |= EXTT_FLAT_CT;
            uctime = fileTimeToUnixTime(e.ctime);
        }
        if (flagEXTT != 0) {
            // to use ntfs time if any m/a/ctime is beyond unixtime upper bound
            if (umtime > UPPER_UNIXTIME_BOUND ||
                uatime > UPPER_UNIXTIME_BOUND ||
                uctime > UPPER_UNIXTIME_BOUND) {
                elen += 36;                // NTFS time, total 36 bytes
            } else {
                elen += (elenEXTT + 5);    // headid(2) + size(2) + flag(1) + data
            }
        }
        writeShort(elen);
        writeBytes(nameBytes, 0, nameBytes.length);
        if (hasZip64) {
            writeShort(ZIP64_EXTID);
            writeShort(16);
            writeLong(e.size);
            writeLong(e.csize);
        }
        if (flagEXTT != 0) {
            if (umtime > UPPER_UNIXTIME_BOUND ||
                uatime > UPPER_UNIXTIME_BOUND ||
                uctime > UPPER_UNIXTIME_BOUND) {
                writeShort(EXTID_NTFS);    // id
                writeShort(32);            // data size
                writeInt(0);               // reserved
                writeShort(0x0001);        // NTFS attr tag
                writeShort(24);
                writeLong(e.mtime == null ? WINDOWS_TIME_NOT_AVAILABLE
                                          : fileTimeToWinTime(e.mtime));
                writeLong(e.atime == null ? WINDOWS_TIME_NOT_AVAILABLE
                                          : fileTimeToWinTime(e.atime));
                writeLong(e.ctime == null ? WINDOWS_TIME_NOT_AVAILABLE
                                          : fileTimeToWinTime(e.ctime));
            } else {
                writeShort(EXTID_EXTT);
                writeShort(elenEXTT + 1);  // flag + data
                writeByte(flagEXTT);
                if (e.mtime != null)
                    writeInt(umtime);
                if (e.atime != null)
                    writeInt(uatime);
                if (e.ctime != null)
                    writeInt(uctime);
            }
        }
        writeExtra(e.extra);
        locoff = written;
    }

    /*
     * Writes extra data descriptor (EXT) for specified entry.
     */
    private void writeEXT(ZipEntry e) throws IOException {
        writeInt(EXTSIG);           // EXT header signature
        writeInt(e.crc);            // crc-32
        if (e.csize >= ZIP64_MAGICVAL || e.size >= ZIP64_MAGICVAL) {
            writeLong(e.csize);
            writeLong(e.size);
        } else {
            writeInt(e.csize);          // compressed size
            writeInt(e.size);           // uncompressed size
        }
    }

    /**
     * Adds information about compatibility of file attribute information
     * to a version value.
     */
    private int versionMadeBy(ZipEntry e, int version) {
        return (e.extraAttributes < 0) ? version :
                VERSION_MADE_BY_BASE_UNIX | (version & 0xff);
    }

    /*
     * Write central directory (CEN) header for specified entry.
     * REMIND: add support for file attributes
     */
    private void writeCEN(XEntry xentry) throws IOException {
        ZipEntry e  = xentry.entry;
        int flag = e.flag;
        int version = version(e);
        long csize = e.csize;
        long size = e.size;
        long offset = xentry.offset;
        int elenZIP64 = 0;
        boolean hasZip64 = false;

        if (e.csize >= ZIP64_MAGICVAL) {
            csize = ZIP64_MAGICVAL;
            elenZIP64 += 8;              // csize(8)
            hasZip64 = true;
        }
        if (e.size >= ZIP64_MAGICVAL) {
            size = ZIP64_MAGICVAL;    // size(8)
            elenZIP64 += 8;
            hasZip64 = true;
        }
        if (xentry.offset >= ZIP64_MAGICVAL) {
            offset = ZIP64_MAGICVAL;
            elenZIP64 += 8;              // offset(8)
            hasZip64 = true;
        }
        writeInt(CENSIG);           // CEN header signature
        if (hasZip64) {
            writeShort(versionMadeBy(e,45));         // ver 4.5 for zip64
            writeShort(45);
        } else {
            writeShort(versionMadeBy(e, version));    // version made by
            writeShort(version);    // version needed to extract
        }
        writeShort(flag);           // general purpose bit flag
        writeShort(e.method);       // compression method
        writeInt(e.xdostime);       // last modification time
        writeInt(e.crc);            // crc-32
        writeInt(csize);            // compressed size
        writeInt(size);             // uncompressed size
        byte[] nameBytes = zc.getBytes(e.name);
        writeShort(nameBytes.length);

        int elen = getExtraLen(e.extra);
        if (hasZip64) {
            elen += (elenZIP64 + 4);// + headid(2) + datasize(2)
        }
        // cen info-zip extended timestamp only outputs mtime
        // but set the flag for a/ctime, if present in loc
        int flagEXTT = 0;
        long umtime = -1;
        long uatime = -1;
        long uctime = -1;
        if (e.mtime != null) {
            flagEXTT |= EXTT_FLAG_LMT;
            umtime = fileTimeToUnixTime(e.mtime);
        }
        if (e.atime != null) {
            flagEXTT |= EXTT_FLAG_LAT;
            uatime = fileTimeToUnixTime(e.atime);
        }
        if (e.ctime != null) {
            flagEXTT |= EXTT_FLAT_CT;
            uctime = fileTimeToUnixTime(e.ctime);
        }
        if (flagEXTT != 0) {
            // to use ntfs time if any m/a/ctime is beyond unixtime upper bound
            if (umtime > UPPER_UNIXTIME_BOUND ||
                uatime > UPPER_UNIXTIME_BOUND ||
                uctime > UPPER_UNIXTIME_BOUND) {
                elen += 36;         // NTFS time total 36 bytes
            } else {
                elen += 5;          // headid(2) + sz(2) + flag(1)
                if (e.mtime != null)
                    elen += 4;      // + mtime (4)
            }
        }
        writeShort(elen);
        byte[] commentBytes;
        if (e.comment != null) {
            commentBytes = zc.getBytes(e.comment);
            writeShort(Math.min(commentBytes.length, 0xffff));
        } else {
            commentBytes = null;
            writeShort(0);
        }
        writeShort(0);              // starting disk number
        writeShort(0);              // internal file attributes (unused)
        // extra file attributes, used for storing posix permissions etc.
        writeInt(e.extraAttributes > 0 ? e.extraAttributes << 16 : 0);
        writeInt(offset);           // relative offset of local header
        writeBytes(nameBytes, 0, nameBytes.length);

        // take care of EXTID_ZIP64 and EXTID_EXTT
        if (hasZip64) {
            writeShort(ZIP64_EXTID);// Zip64 extra
            writeShort(elenZIP64);
            if (size == ZIP64_MAGICVAL)
                writeLong(e.size);
            if (csize == ZIP64_MAGICVAL)
                writeLong(e.csize);
            if (offset == ZIP64_MAGICVAL)
                writeLong(xentry.offset);
        }
        if (flagEXTT != 0) {
            if (umtime > UPPER_UNIXTIME_BOUND ||
                uatime > UPPER_UNIXTIME_BOUND ||
                uctime > UPPER_UNIXTIME_BOUND) {
                writeShort(EXTID_NTFS);    // id
                writeShort(32);            // data size
                writeInt(0);               // reserved
                writeShort(0x0001);        // NTFS attr tag
                writeShort(24);
                writeLong(e.mtime == null ? WINDOWS_TIME_NOT_AVAILABLE
                                          : fileTimeToWinTime(e.mtime));
                writeLong(e.atime == null ? WINDOWS_TIME_NOT_AVAILABLE
                                          : fileTimeToWinTime(e.atime));
                writeLong(e.ctime == null ? WINDOWS_TIME_NOT_AVAILABLE
                                          : fileTimeToWinTime(e.ctime));
            } else {
                writeShort(EXTID_EXTT);
                if (e.mtime != null) {
                    writeShort(5);      // flag + mtime
                    writeByte(flagEXTT);
                    writeInt(umtime);
                } else {
                    writeShort(1);      // flag only
                    writeByte(flagEXTT);
                }
            }
        }
        writeExtra(e.extra);
        if (commentBytes != null) {
            writeBytes(commentBytes, 0, Math.min(commentBytes.length, 0xffff));
        }
    }

    /*
     * Writes end of central directory (END) header.
     */
    private void writeEND(long off, long len) throws IOException {
        boolean hasZip64 = false;
        long xlen = len;
        long xoff = off;
        if (xlen >= ZIP64_MAGICVAL) {
            xlen = ZIP64_MAGICVAL;
            hasZip64 = true;
        }
        if (xoff >= ZIP64_MAGICVAL) {
            xoff = ZIP64_MAGICVAL;
            hasZip64 = true;
        }
        int count = xentries.size();
        if (count >= ZIP64_MAGICCOUNT) {
            hasZip64 |= !inhibitZip64;
            if (hasZip64) {
                count = ZIP64_MAGICCOUNT;
            }
        }
        if (hasZip64) {
            long off64 = written;
            //zip64 end of central directory record
            writeInt(ZIP64_ENDSIG);        // zip64 END record signature
            writeLong(ZIP64_ENDHDR - 12);  // size of zip64 end
            writeShort(45);                // version made by
            writeShort(45);                // version needed to extract
            writeInt(0);                   // number of this disk
            writeInt(0);                   // central directory start disk
            writeLong(xentries.size());    // number of directory entires on disk
            writeLong(xentries.size());    // number of directory entires
            writeLong(len);                // length of central directory
            writeLong(off);                // offset of central directory

            //zip64 end of central directory locator
            writeInt(ZIP64_LOCSIG);        // zip64 END locator signature
            writeInt(0);                   // zip64 END start disk
            writeLong(off64);              // offset of zip64 END
            writeInt(1);                   // total number of disks (?)
        }
        writeInt(ENDSIG);                 // END record signature
        writeShort(0);                    // number of this disk
        writeShort(0);                    // central directory start disk
        writeShort(count);                // number of directory entries on disk
        writeShort(count);                // total number of directory entries
        writeInt(xlen);                   // length of central directory
        writeInt(xoff);                   // offset of central directory
        if (comment != null) {            // zip file comment
            writeShort(comment.length);
            writeBytes(comment, 0, comment.length);
        } else {
            writeShort(0);
        }
    }

    /*
     * Returns the length of extra data without EXTT and ZIP64.
     */
    private int getExtraLen(byte[] extra) {
        if (extra == null)
            return 0;
        int skipped = 0;
        int len = extra.length;
        int off = 0;
        while (off + 4 <= len) {
            int tag = get16(extra, off);
            int sz = get16(extra, off + 2);
            if (sz < 0 || (off + 4 + sz) > len) {
                break;
            }
            if (tag == EXTID_EXTT || tag == EXTID_ZIP64) {
                skipped += (sz + 4);
            }
            off += (sz + 4);
        }
        return len - skipped;
    }

    /*
     * Writes extra data without EXTT and ZIP64.
     *
     * Extra timestamp and ZIP64 data is handled/output separately
     * in writeLOC and writeCEN.
     */
    private void writeExtra(byte[] extra) throws IOException {
        if (extra != null) {
            int len = extra.length;
            int off = 0;
            while (off + 4 <= len) {
                int tag = get16(extra, off);
                int sz = get16(extra, off + 2);
                if (sz < 0 || (off + 4 + sz) > len) {
                    writeBytes(extra, off, len - off);
                    return;
                }
                if (tag != EXTID_EXTT && tag != EXTID_ZIP64) {
                    writeBytes(extra, off, sz + 4);
                }
                off += (sz + 4);
            }
            if (off < len) {
                writeBytes(extra, off, len - off);
            }
        }
    }

    /*
     * Writes a 8-bit byte to the output stream.
     */
    private void writeByte(int v) throws IOException {
        OutputStream out = this.out;
        out.write(v & 0xff);
        written += 1;
    }

    /*
     * Writes a 16-bit short to the output stream in little-endian byte order.
     */
    private void writeShort(int v) throws IOException {
        OutputStream out = this.out;
        out.write((v >>> 0) & 0xff);
        out.write((v >>> 8) & 0xff);
        written += 2;
    }

    /*
     * Writes a 32-bit int to the output stream in little-endian byte order.
     */
    private void writeInt(long v) throws IOException {
        OutputStream out = this.out;
        out.write((int)((v >>>  0) & 0xff));
        out.write((int)((v >>>  8) & 0xff));
        out.write((int)((v >>> 16) & 0xff));
        out.write((int)((v >>> 24) & 0xff));
        written += 4;
    }

    /*
     * Writes a 64-bit int to the output stream in little-endian byte order.
     */
    private void writeLong(long v) throws IOException {
        OutputStream out = this.out;
        out.write((int)((v >>>  0) & 0xff));
        out.write((int)((v >>>  8) & 0xff));
        out.write((int)((v >>> 16) & 0xff));
        out.write((int)((v >>> 24) & 0xff));
        out.write((int)((v >>> 32) & 0xff));
        out.write((int)((v >>> 40) & 0xff));
        out.write((int)((v >>> 48) & 0xff));
        out.write((int)((v >>> 56) & 0xff));
        written += 8;
    }

    /*
     * Writes an array of bytes to the output stream.
     */
    private void writeBytes(byte[] b, int off, int len) throws IOException {
        super.out.write(b, off, len);
        written += len;
    }
}
