/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.font;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import sun.java2d.Disposer;
import sun.java2d.DisposerRecord;
import sun.security.action.GetPropertyAction;

/**
 * TrueTypeFont is not called SFntFont because it is not expected
 * to handle all types that may be housed in a such a font file.
 * If additional types are supported later, it may make sense to
 * create an SFnt superclass. Eg to handle sfnt-housed postscript fonts.
 * OpenType fonts are handled by this class, and possibly should be
 * represented by a subclass.
 * An instance stores some information from the font file to faciliate
 * faster access. File size, the table directory and the names of the font
 * are the most important of these. It amounts to approx 400 bytes
 * for a typical font. Systems with mutiple locales sometimes have up to 400
 * font files, and an app which loads all font files would need around
 * 160Kbytes. So storing any more info than this would be expensive.
 */
public class TrueTypeFont extends FileFont {

   /* -- Tags for required TrueType tables */
    public static final int cmapTag = 0x636D6170; // 'cmap'
    public static final int glyfTag = 0x676C7966; // 'glyf'
    public static final int headTag = 0x68656164; // 'head'
    public static final int hheaTag = 0x68686561; // 'hhea'
    public static final int hmtxTag = 0x686D7478; // 'hmtx'
    public static final int locaTag = 0x6C6F6361; // 'loca'
    public static final int maxpTag = 0x6D617870; // 'maxp'
    public static final int nameTag = 0x6E616D65; // 'name'
    public static final int postTag = 0x706F7374; // 'post'
    public static final int os_2Tag = 0x4F532F32; // 'OS/2'

    /* -- Tags for opentype related tables */
    public static final int GDEFTag = 0x47444546; // 'GDEF'
    public static final int GPOSTag = 0x47504F53; // 'GPOS'
    public static final int GSUBTag = 0x47535542; // 'GSUB'
    public static final int mortTag = 0x6D6F7274; // 'mort'
    public static final int morxTag = 0x6D6F7278; // 'morx'

    /* -- Tags for non-standard tables */
    public static final int fdscTag = 0x66647363; // 'fdsc' - gxFont descriptor
    public static final int fvarTag = 0x66766172; // 'fvar' - gxFont variations
    public static final int featTag = 0x66656174; // 'feat' - layout features
    public static final int EBLCTag = 0x45424C43; // 'EBLC' - embedded bitmaps
    public static final int gaspTag = 0x67617370; // 'gasp' - hint/smooth sizes

    /* --  Other tags */
    public static final int ttcfTag = 0x74746366; // 'ttcf' - TTC file
    public static final int v1ttTag = 0x00010000; // 'v1tt' - Version 1 TT font
    public static final int trueTag = 0x74727565; // 'true' - Version 2 TT font
    public static final int ottoTag = 0x4f54544f; // 'otto' - OpenType font

    /* -- ID's used in the 'name' table */
    public static final int MAC_PLATFORM_ID = 1;
    public static final int MACROMAN_SPECIFIC_ID = 0;
    public static final int MACROMAN_ENGLISH_LANG = 0;

    public static final int MS_PLATFORM_ID = 3;
    /* MS locale id for US English is the "default" */
    public static final short ENGLISH_LOCALE_ID = 0x0409; // 1033 decimal
    public static final int FAMILY_NAME_ID = 1;
    // public static final int STYLE_WEIGHT_ID = 2; // currently unused.
    public static final int FULL_NAME_ID = 4;
    public static final int POSTSCRIPT_NAME_ID = 6;

    private static final short US_LCID = 0x0409;  // US English - default

    private static Map<String, Short> lcidMap;

    static class DirectoryEntry {
        int tag;
        int offset;
        int length;
    }

    /* There is a pool which limits the number of fd's that are in
     * use. Normally fd's are closed as they are replaced in the pool.
     * But if an instance of this class becomes unreferenced, then there
     * needs to be a way to close the fd. A finalize() method could do this,
     * but using the Disposer class will ensure its called in a more timely
     * manner. This is not something which should be relied upon to free
     * fd's - its a safeguard.
     */
    private static class TTDisposerRecord implements DisposerRecord {

        FileChannel channel = null;

        public synchronized void dispose() {
            try {
                if (channel != null) {
                    channel.close();
                }
            } catch (IOException e) {
            } finally {
                channel = null;
            }
        }
    }

    TTDisposerRecord disposerRecord = new TTDisposerRecord();

    /* > 0 only if this font is a part of a collection */
    int fontIndex = 0;

    /* Number of fonts in this collection. ==1 if not a collection */
    int directoryCount = 1;

    /* offset in file of table directory for this font */
    int directoryOffset; // 12 if its not a collection.

    /* number of table entries in the directory/offsets table */
    int numTables;

    /* The contents of the directory/offsets table */
    DirectoryEntry []tableDirectory;

//     protected byte []gposTable = null;
//     protected byte []gdefTable = null;
//     protected byte []gsubTable = null;
//     protected byte []mortTable = null;
//     protected boolean hintsTabledChecked = false;
//     protected boolean containsHintsTable = false;

    /* These fields are set from os/2 table info. */
    private boolean supportsJA;
    private boolean supportsCJK;

    /* These are for faster access to the name of the font as
     * typically exposed via API to applications.
     */
    private Locale nameLocale;
    private String localeFamilyName;
    private String localeFullName;

    /*
     * Used on Windows to validate the font selected by GDI for (sub-pixel
     * antialiased) rendering. For 'standalone' fonts it's equal to the font
     * file size, for collection (TTC, OTC) members it's the number of bytes in
     * the collection file from the start of this font's offset table till the
     * end of the file.
     */
    int fontDataSize;

    public TrueTypeFont(String platname, Object nativeNames, int fIndex,
                 boolean javaRasterizer)
        throws FontFormatException
    {
        this(platname, nativeNames, fIndex, javaRasterizer, true);
    }

    /**
     * - does basic verification of the file
     * - reads the header table for this font (within a collection)
     * - reads the names (full, family).
     * - determines the style of the font.
     * - initializes the CMAP
     * @throws FontFormatException if the font can't be opened
     * or fails verification,  or there's no usable cmap
     */
    public TrueTypeFont(String platname, Object nativeNames, int fIndex,
                 boolean javaRasterizer, boolean useFilePool)
        throws FontFormatException {
        super(platname, nativeNames);
        useJavaRasterizer = javaRasterizer;
        fontRank = Font2D.TTF_RANK;
        try {
            verify(useFilePool);
            init(fIndex);
            if (!useFilePool) {
               close();
            }
        } catch (Throwable t) {
            close();
            if (t instanceof FontFormatException) {
                throw (FontFormatException)t;
            } else {
                throw new FontFormatException("Unexpected runtime exception.");
            }
        }
        Disposer.addObjectRecord(this, disposerRecord);
    }

    private synchronized FileChannel open() throws FontFormatException {
        return open(true);
    }

    /* This is intended to be called, and the returned value used,
     * from within a block synchronized on this font object.
     * ie the channel returned may be nulled out at any time by "close()"
     * unless the caller holds a lock.
     * Deadlock warning: FontManager.addToPool(..) acquires a global lock,
     * which means nested locks may be in effect.
     */
    private synchronized FileChannel open(boolean usePool)
                                     throws FontFormatException {
        if (disposerRecord.channel == null) {
            if (FontUtilities.isLogging()) {
                FontUtilities.logInfo("open TTF: " + platName);
            }
            try {
                @SuppressWarnings("removal")
                RandomAccessFile raf = AccessController.doPrivileged(
                    new PrivilegedExceptionAction<RandomAccessFile>() {
                        public RandomAccessFile run() throws FileNotFoundException {
                            return new RandomAccessFile(platName, "r");
                    }
                });
                disposerRecord.channel = raf.getChannel();
                fileSize = (int)disposerRecord.channel.size();
                if (usePool) {
                    FontManager fm = FontManagerFactory.getInstance();
                    if (fm instanceof SunFontManager) {
                        ((SunFontManager) fm).addToPool(this);
                    }
                }
            } catch (PrivilegedActionException e) {
                close();
                Throwable reason = e.getCause();
                if (reason == null) {
                    reason = e;
                }
                throw new FontFormatException(reason.toString());
            } catch (ClosedChannelException e) {
                /* NIO I/O is interruptible, recurse to retry operation.
                 * The call to channel.size() above can throw this exception.
                 * Clear interrupts before recursing in case NIO didn't.
                 * Note that close() sets disposerRecord.channel to null.
                 */
                Thread.interrupted();
                close();
                open();
            } catch (IOException e) {
                close();
                throw new FontFormatException(e.toString());
            }
        }
        return disposerRecord.channel;
    }

    protected synchronized void close() {
        disposerRecord.dispose();
    }


    int readBlock(ByteBuffer buffer, int offset, int length) {
        int bread = 0;
        try {
            synchronized (this) {
                if (disposerRecord.channel == null) {
                    open();
                }
                if (offset + length > fileSize) {
                    if (offset >= fileSize) {
                        /* Since the caller ensures that offset is < fileSize
                         * this condition suggests that fileSize is now
                         * different than the value we originally provided
                         * to native when the scaler was created.
                         * Also fileSize is updated every time we
                         * open() the file here, but in native the value
                         * isn't updated. If the file has changed whilst we
                         * are executing we want to bail, not spin.
                         */
                        if (FontUtilities.isLogging()) {
                            String msg = "Read offset is " + offset +
                                    " file size is " + fileSize+
                                    " file is " + platName;
                            FontUtilities.logSevere(msg);
                        }
                        return -1;
                    } else {
                        length = fileSize - offset;
                    }
                }
                buffer.clear();
                disposerRecord.channel.position(offset);
                while (bread < length) {
                    int cnt = disposerRecord.channel.read(buffer);
                    if (cnt == -1) {
                        String msg = "Unexpected EOF " + this;
                        int currSize = (int)disposerRecord.channel.size();
                        if (currSize != fileSize) {
                            msg += " File size was " + fileSize +
                                " and now is " + currSize;
                        }
                        if (FontUtilities.isLogging()) {
                            FontUtilities.logSevere(msg);
                        }
                        // We could still flip() the buffer here because
                        // it's possible that we did read some data in
                        // an earlier loop, and we probably should
                        // return that to the caller. Although if
                        // the caller expected 8K of data and we return
                        // only a few bytes then maybe it's better instead to
                        // set bread = -1 to indicate failure.
                        // The following is therefore using arbitrary values
                        // but is meant to allow cases where enough
                        // data was read to probably continue.
                        if (bread > length/2 || bread > 16384) {
                            buffer.flip();
                            if (FontUtilities.isLogging()) {
                                msg = "Returning " + bread + " bytes instead of " + length;
                                FontUtilities.logSevere(msg);
                            }
                        } else {
                            bread = -1;
                        }
                        throw new IOException(msg);
                    }
                    bread += cnt;
                }
                buffer.flip();
                if (bread > length) { // possible if buffer.size() > length
                    bread = length;
                }
            }
        } catch (FontFormatException e) {
            if (FontUtilities.isLogging()) {
                FontUtilities.getLogger().severe("While reading " + platName, e);
            }
            bread = -1; // signal EOF
            deregisterFontAndClearStrikeCache();
        } catch (ClosedChannelException e) {
            /* NIO I/O is interruptible, recurse to retry operation.
             * Clear interrupts before recursing in case NIO didn't.
             */
            Thread.interrupted();
            close();
            return readBlock(buffer, offset, length);
        } catch (IOException e) {
            /* If we did not read any bytes at all and the exception is
             * not a recoverable one (ie is not ClosedChannelException) then
             * we should indicate that there is no point in re-trying.
             * Other than an attempt to read past the end of the file it
             * seems unlikely this would occur as problems opening the
             * file are handled as a FontFormatException.
             */
            if (FontUtilities.isLogging()) {
                FontUtilities.getLogger().severe("While reading " + platName, e);
            }
            if (bread == 0) {
                bread = -1; // signal EOF
                deregisterFontAndClearStrikeCache();
            }
        }
        return bread;
    }

    ByteBuffer readBlock(int offset, int length) {

        ByteBuffer buffer = ByteBuffer.allocate(length);
        try {
            synchronized (this) {
                if (disposerRecord.channel == null) {
                    open();
                }
                if (offset + length > fileSize) {
                    if (offset > fileSize) {
                        return null; // assert?
                    } else {
                        buffer = ByteBuffer.allocate(fileSize-offset);
                    }
                }
                disposerRecord.channel.position(offset);
                disposerRecord.channel.read(buffer);
                buffer.flip();
            }
        } catch (FontFormatException e) {
            return null;
        } catch (ClosedChannelException e) {
            /* NIO I/O is interruptible, recurse to retry operation.
             * Clear interrupts before recursing in case NIO didn't.
             */
            Thread.interrupted();
            close();
            readBlock(buffer, offset, length);
        } catch (IOException e) {
            return null;
        }
        return buffer;
    }

    /* This is used by native code which can't allocate a direct byte
     * buffer because of bug 4845371. It, and references to it in native
     * code in scalerMethods.c can be removed once that bug is fixed.
     * 4845371 is now fixed but we'll keep this around as it doesn't cost
     * us anything if its never used/called.
     */
    byte[] readBytes(int offset, int length) {
        ByteBuffer buffer = readBlock(offset, length);
        if (buffer.hasArray()) {
            return buffer.array();
        } else {
            byte[] bufferBytes = new byte[buffer.limit()];
            buffer.get(bufferBytes);
            return bufferBytes;
        }
    }

    private void verify(boolean usePool) throws FontFormatException {
        open(usePool);
    }

    /* sizes, in bytes, of TT/TTC header records */
    private static final int TTCHEADERSIZE = 12;
    private static final int DIRECTORYHEADERSIZE = 12;
    private static final int DIRECTORYENTRYSIZE = 16;

    protected void init(int fIndex) throws FontFormatException  {
        int headerOffset = 0;
        ByteBuffer buffer = readBlock(0, TTCHEADERSIZE);
        try {
            switch (buffer.getInt()) {

            case ttcfTag:
                buffer.getInt(); // skip TTC version ID
                directoryCount = buffer.getInt();
                if (fIndex >= directoryCount) {
                    throw new FontFormatException("Bad collection index");
                }
                fontIndex = fIndex;
                buffer = readBlock(TTCHEADERSIZE+4*fIndex, 4);
                headerOffset = buffer.getInt();
                fontDataSize = Math.max(0, fileSize - headerOffset);
                break;

            case v1ttTag:
            case trueTag:
            case ottoTag:
                fontDataSize = fileSize;
                break;

            default:
                throw new FontFormatException("Unsupported sfnt " +
                                              getPublicFileName());
            }

            /* Now have the offset of this TT font (possibly within a TTC)
             * After the TT version/scaler type field, is the short
             * representing the number of tables in the table directory.
             * The table directory begins at 12 bytes after the header.
             * Each table entry is 16 bytes long (4 32-bit ints)
             */
            buffer = readBlock(headerOffset+4, 2);
            numTables = buffer.getShort();
            directoryOffset = headerOffset+DIRECTORYHEADERSIZE;
            ByteBuffer bbuffer = readBlock(directoryOffset,
                                           numTables*DIRECTORYENTRYSIZE);
            IntBuffer ibuffer = bbuffer.asIntBuffer();
            DirectoryEntry table;
            tableDirectory = new DirectoryEntry[numTables];
            for (int i=0; i<numTables;i++) {
                tableDirectory[i] = table = new DirectoryEntry();
                table.tag   =  ibuffer.get();
                /* checksum */ ibuffer.get();
                table.offset = ibuffer.get() & 0x7FFFFFFF;
                table.length = ibuffer.get() & 0x7FFFFFFF;
                if (table.offset + table.length > fileSize) {
                    throw new FontFormatException("bad table, tag="+table.tag);
                }
            }

            if (getDirectoryEntry(headTag) == null) {
                throw new FontFormatException("missing head table");
            }
            if (getDirectoryEntry(maxpTag) == null) {
                throw new FontFormatException("missing maxp table");
            }
            if (getDirectoryEntry(hmtxTag) != null
                    && getDirectoryEntry(hheaTag) == null) {
                throw new FontFormatException("missing hhea table");
            }
            ByteBuffer maxpTable = getTableBuffer(maxpTag);
            if (maxpTable.getChar(4) == 0) {
                throw new FontFormatException("zero glyphs");
            }
            initNames();
        } catch (Exception e) {
            if (FontUtilities.isLogging()) {
                FontUtilities.logSevere(e.toString());
            }
            if (e instanceof FontFormatException) {
                throw (FontFormatException)e;
            } else {
                throw new FontFormatException(e.toString());
            }
        }
        if (familyName == null || fullName == null) {
            throw new FontFormatException("Font name not found");
        }
        /* The os2_Table is needed to gather some info, but we don't
         * want to keep it around (as a field) so obtain it once and
         * pass it to the code that needs it.
         */
        ByteBuffer os2_Table = getTableBuffer(os_2Tag);
        setStyle(os2_Table);
        setCJKSupport(os2_Table);
    }

    /* The array index corresponds to a bit offset in the TrueType
     * font's OS/2 compatibility table's code page ranges fields.
     * These are two 32 bit unsigned int fields at offsets 78 and 82.
     * We are only interested in determining if the font supports
     * the windows encodings we expect as the default encoding in
     * supported locales, so we only map the first of these fields.
     */
    static final String[] encoding_mapping = {
        "cp1252",    /*  0:Latin 1  */
        "cp1250",    /*  1:Latin 2  */
        "cp1251",    /*  2:Cyrillic */
        "cp1253",    /*  3:Greek    */
        "cp1254",    /*  4:Turkish/Latin 5  */
        "cp1255",    /*  5:Hebrew   */
        "cp1256",    /*  6:Arabic   */
        "cp1257",    /*  7:Windows Baltic   */
        "",          /*  8:reserved for alternate ANSI */
        "",          /*  9:reserved for alternate ANSI */
        "",          /* 10:reserved for alternate ANSI */
        "",          /* 11:reserved for alternate ANSI */
        "",          /* 12:reserved for alternate ANSI */
        "",          /* 13:reserved for alternate ANSI */
        "",          /* 14:reserved for alternate ANSI */
        "",          /* 15:reserved for alternate ANSI */
        "ms874",     /* 16:Thai     */
        "ms932",     /* 17:JIS/Japanese */
        "gbk",       /* 18:PRC GBK Cp950  */
        "ms949",     /* 19:Korean Extended Wansung */
        "ms950",     /* 20:Chinese (Taiwan, Hongkong, Macau) */
        "ms1361",    /* 21:Korean Johab */
        "",          /* 22 */
        "",          /* 23 */
        "",          /* 24 */
        "",          /* 25 */
        "",          /* 26 */
        "",          /* 27 */
        "",          /* 28 */
        "",          /* 29 */
        "",          /* 30 */
        "",          /* 31 */
    };

    /* This maps two letter language codes to a Windows code page.
     * Note that eg Cp1252 (the first subarray) is not exactly the same as
     * Latin-1 since Windows code pages are do not necessarily correspond.
     * There are two codepages for zh and ko so if a font supports
     * only one of these ranges then we need to distinguish based on
     * country. So far this only seems to matter for zh.
     * REMIND: Unicode locales such as Hindi do not have a code page so
     * this whole mechanism needs to be revised to map languages to
     * the Unicode ranges either when this fails, or as an additional
     * validating test. Basing it on Unicode ranges should get us away
     * from needing to map to this small and incomplete set of Windows
     * code pages which looks odd on non-Windows platforms.
     */
    private static final String[][] languages = {

        /* cp1252/Latin 1 */
        { "en", "ca", "da", "de", "es", "fi", "fr", "is", "it",
          "nl", "no", "pt", "sq", "sv", },

         /* cp1250/Latin2 */
        { "cs", "cz", "et", "hr", "hu", "nr", "pl", "ro", "sk",
          "sl", "sq", "sr", },

        /* cp1251/Cyrillic */
        { "bg", "mk", "ru", "sh", "uk" },

        /* cp1253/Greek*/
        { "el" },

         /* cp1254/Turkish,Latin 5 */
        { "tr" },

         /* cp1255/Hebrew */
        { "he" },

        /* cp1256/Arabic */
        { "ar" },

         /* cp1257/Windows Baltic */
        { "et", "lt", "lv" },

        /* ms874/Thai */
        { "th" },

         /* ms932/Japanese */
        { "ja" },

        /* gbk/Chinese (PRC GBK Cp950) */
        { "zh", "zh_CN", },

        /* ms949/Korean Extended Wansung */
        { "ko" },

        /* ms950/Chinese (Taiwan, Hongkong, Macau) */
        { "zh_HK", "zh_TW", },

        /* ms1361/Korean Johab */
        { "ko" },
    };

    private static final String[] codePages = {
        "cp1252",
        "cp1250",
        "cp1251",
        "cp1253",
        "cp1254",
        "cp1255",
        "cp1256",
        "cp1257",
        "ms874",
        "ms932",
        "gbk",
        "ms949",
        "ms950",
        "ms1361",
    };

    private static String defaultCodePage = null;
    @SuppressWarnings("removal")
    static String getCodePage() {

        if (defaultCodePage != null) {
            return defaultCodePage;
        }

        if (FontUtilities.isWindows) {
            defaultCodePage =
                AccessController.doPrivileged(new GetPropertyAction("file.encoding"));
        } else {
            if (languages.length != codePages.length) {
                throw new InternalError("wrong code pages array length");
            }
            Locale locale = sun.awt.SunToolkit.getStartupLocale();

            String language = locale.getLanguage();
            if (language != null) {
                if (language.equals("zh")) {
                    String country = locale.getCountry();
                    if (country != null) {
                        language = language + "_" + country;
                    }
                }
                for (int i=0; i<languages.length;i++) {
                    for (int l=0;l<languages[i].length; l++) {
                        if (language.equals(languages[i][l])) {
                            defaultCodePage = codePages[i];
                            return defaultCodePage;
                        }
                    }
                }
            }
        }
        if (defaultCodePage == null) {
            defaultCodePage = "";
        }
        return defaultCodePage;
    }

    /* Theoretically, reserved bits must not be set, include symbol bits */
    public static final int reserved_bits1 = 0x80000000;
    public static final int reserved_bits2 = 0x0000ffff;
    @Override
    boolean supportsEncoding(String encoding) {
        if (encoding == null) {
            encoding = getCodePage();
        }
        if ("".equals(encoding)) {
            return false;
        }

        encoding = encoding.toLowerCase();

        /* java_props_md.c has a couple of special cases
         * if language packs are installed. In these encodings the
         * fontconfig files pick up different fonts :
         * SimSun-18030 and MingLiU_HKSCS. Since these fonts will
         * indicate they support the base encoding, we need to rewrite
         * these encodings here before checking the map/array.
         */
        if (encoding.equals("gb18030")) {
            encoding = "gbk";
        } else if (encoding.equals("ms950_hkscs")) {
            encoding = "ms950";
        }

        ByteBuffer buffer = getTableBuffer(os_2Tag);
        /* required info is at offsets 78 and 82 */
        if (buffer == null || buffer.capacity() < 86) {
            return false;
        }

        int range1 = buffer.getInt(78); /* ulCodePageRange1 */
        // int range2 = buffer.getInt(82); /* ulCodePageRange2 */

        /* This test is too stringent for Arial on Solaris (and perhaps
         * other fonts). Arial has at least one reserved bit set for an
         * unknown reason.
         */
        // if (((range1 & reserved_bits1) | (range2 & reserved_bits2)) != 0) {
        //     return false;
        // }

        for (int em=0; em<encoding_mapping.length; em++) {
            if (encoding_mapping[em].equals(encoding)) {
                if (((1 << em) & range1) != 0) {
                    return true;
                }
            }
        }
        return false;
    }


    /* Use info in the os_2Table to test CJK support */
    private void setCJKSupport(ByteBuffer os2Table) {
        /* required info is in ulong at offset 46 */
        if (os2Table == null || os2Table.capacity() < 50) {
            return;
        }
        int range2 = os2Table.getInt(46); /* ulUnicodeRange2 */

        /* Any of these bits set in the 32-63 range indicate a font with
         * support for a CJK range. We aren't looking at some other bits
         * in the 64-69 range such as half width forms as its unlikely a font
         * would include those and none of these.
         */
        supportsCJK = ((range2 & 0x29bf0000) != 0);

        /* This should be generalised, but for now just need to know if
         * Hiragana or Katakana ranges are supported by the font.
         * In the 4 longs representing unicode ranges supported
         * bits 49 & 50 indicate hiragana and katakana
         * This is bits 17 & 18 in the 2nd ulong. If either is supported
         * we presume this is a JA font.
         */
        supportsJA = ((range2 & 0x60000) != 0);
    }

    boolean supportsJA() {
        return supportsJA;
    }

     ByteBuffer getTableBuffer(int tag) {
        DirectoryEntry entry = null;

        for (int i=0;i<numTables;i++) {
            if (tableDirectory[i].tag == tag) {
                entry = tableDirectory[i];
                break;
            }
        }
        if (entry == null || entry.length == 0 ||
            entry.offset+entry.length > fileSize) {
            return null;
        }

        int bread = 0;
        ByteBuffer buffer = ByteBuffer.allocate(entry.length);
        synchronized (this) {
            try {
                if (disposerRecord.channel == null) {
                    open();
                }
                disposerRecord.channel.position(entry.offset);
                bread = disposerRecord.channel.read(buffer);
                buffer.flip();
            } catch (ClosedChannelException e) {
                /* NIO I/O is interruptible, recurse to retry operation.
                 * Clear interrupts before recursing in case NIO didn't.
                 */
                Thread.interrupted();
                close();
                return getTableBuffer(tag);
            } catch (IOException e) {
                return null;
            } catch (FontFormatException e) {
                return null;
            }

            if (bread < entry.length) {
                return null;
            } else {
                return buffer;
            }
        }
    }

    @Override
    protected byte[] getTableBytes(int tag) {
        ByteBuffer buffer = getTableBuffer(tag);
        if (buffer == null) {
            return null;
        } else if (buffer.hasArray()) {
            try {
                return buffer.array();
            } catch (Exception re) {
            }
        }
        byte []data = new byte[getTableSize(tag)];
        buffer.get(data);
        return data;
    }

    int getTableSize(int tag) {
        for (int i=0;i<numTables;i++) {
            if (tableDirectory[i].tag == tag) {
                return tableDirectory[i].length;
            }
        }
        return 0;
    }

    int getTableOffset(int tag) {
        for (int i=0;i<numTables;i++) {
            if (tableDirectory[i].tag == tag) {
                return tableDirectory[i].offset;
            }
        }
        return 0;
    }

    DirectoryEntry getDirectoryEntry(int tag) {
        for (int i=0;i<numTables;i++) {
            if (tableDirectory[i].tag == tag) {
                return tableDirectory[i];
            }
        }
        return null;
    }

    /* Used to determine if this size has embedded bitmaps, which
     * for CJK fonts should be used in preference to LCD glyphs.
     */
    boolean useEmbeddedBitmapsForSize(int ptSize) {
        if (!supportsCJK) {
            return false;
        }
        if (getDirectoryEntry(EBLCTag) == null) {
            return false;
        }
        ByteBuffer eblcTable = getTableBuffer(EBLCTag);
        int numSizes = eblcTable.getInt(4);
        /* The bitmapSizeTable's start at offset of 8.
         * Each bitmapSizeTable entry is 48 bytes.
         * The offset of ppemY in the entry is 45.
         */
        for (int i=0;i<numSizes;i++) {
            int ppemY = eblcTable.get(8+(i*48)+45) &0xff;
            if (ppemY == ptSize) {
                return true;
            }
        }
        return false;
    }

    public String getFullName() {
        return fullName;
    }

    /* This probably won't get called but is there to support the
     * contract() of setStyle() defined in the superclass.
     */
    @Override
    protected void setStyle() {
        setStyle(getTableBuffer(os_2Tag));
    }

    private int fontWidth = 0;
    @Override
    public int getWidth() {
       return (fontWidth > 0) ? fontWidth : super.getWidth();
    }

    private int fontWeight = 0;
    @Override
    public int getWeight() {
       return (fontWeight > 0) ? fontWeight : super.getWeight();
    }

    /* TrueTypeFont can use the fsSelection fields of OS/2 table
     * to determine the style. In the unlikely case that doesn't exist,
     * can use macStyle in the 'head' table but simpler to
     * fall back to super class algorithm of looking for well known string.
     * A very few fonts don't specify this information, but I only
     * came across one: Lucida Sans Thai Typewriter Oblique in
     * /usr/openwin/lib/locale/th_TH/X11/fonts/TrueType/lucidai.ttf
     * that explicitly specified the wrong value. It says its regular.
     * I didn't find any fonts that were inconsistent (ie regular plus some
     * other value).
     */
    private static final int fsSelectionItalicBit  = 0x00001;
    private static final int fsSelectionBoldBit    = 0x00020;
    private static final int fsSelectionRegularBit = 0x00040;
    private void setStyle(ByteBuffer os_2Table) {
        if (os_2Table == null) {
            return;
        }
        if (os_2Table.capacity() >= 8) {
            fontWeight = os_2Table.getChar(4) & 0xffff;
            fontWidth  = os_2Table.getChar(6) & 0xffff;
        }
        /* fsSelection is unsigned short at buffer offset 62 */
        if (os_2Table.capacity() < 64) {
            super.setStyle();
            return;
        }
        int fsSelection = os_2Table.getChar(62) & 0xffff;
        int italic  = fsSelection & fsSelectionItalicBit;
        int bold    = fsSelection & fsSelectionBoldBit;
        int regular = fsSelection & fsSelectionRegularBit;
//      System.out.println("platname="+platName+" font="+fullName+
//                         " family="+familyName+
//                         " R="+regular+" I="+italic+" B="+bold);
        if (regular!=0 && ((italic|bold)!=0)) {
            /* This is inconsistent. Try using the font name algorithm */
            super.setStyle();
            return;
        } else if ((regular|italic|bold) == 0) {
            /* No style specified. Try using the font name algorithm */
            super.setStyle();
            return;
        }
        switch (bold|italic) {
        case fsSelectionItalicBit:
            style = Font.ITALIC;
            break;
        case fsSelectionBoldBit:
            style = Font.BOLD;
            break;
        case fsSelectionBoldBit|fsSelectionItalicBit:
            style = Font.BOLD|Font.ITALIC;
        }
    }

    private float stSize, stPos, ulSize, ulPos;

    private void setStrikethroughMetrics(ByteBuffer os_2Table, int upem) {
        if (os_2Table == null || os_2Table.capacity() < 30 || upem < 0) {
            stSize = 0.05f;
            stPos = -0.4f;
            return;
        }
        ShortBuffer sb = os_2Table.asShortBuffer();
        stSize = sb.get(13) / (float)upem;
        stPos = -sb.get(14) / (float)upem;
        if (stSize < 0f) {
            stSize = 0.05f;
        }
        if (Math.abs(stPos) > 2.0f) {
            stPos = -0.4f;
        }
    }

    private void setUnderlineMetrics(ByteBuffer postTable, int upem) {
        if (postTable == null || postTable.capacity() < 12 || upem < 0) {
            ulSize = 0.05f;
            ulPos = 0.1f;
            return;
        }
        ShortBuffer sb = postTable.asShortBuffer();
        ulSize = sb.get(5) / (float)upem;
        ulPos = -sb.get(4) / (float)upem;
        if (ulSize < 0f) {
            ulSize = 0.05f;
        }
        if (Math.abs(ulPos) > 2.0f) {
            ulPos = 0.1f;
        }
    }

    @Override
    public void getStyleMetrics(float pointSize, float[] metrics, int offset) {

        if (ulSize == 0f && ulPos == 0f) {

            ByteBuffer head_Table = getTableBuffer(headTag);
            int upem = -1;
            if (head_Table != null && head_Table.capacity() >= 18) {
                ShortBuffer sb = head_Table.asShortBuffer();
                upem = sb.get(9) & 0xffff;
                if (upem < 16 || upem > 16384) {
                    upem = 2048;
                }
            }

            ByteBuffer os2_Table = getTableBuffer(os_2Tag);
            setStrikethroughMetrics(os2_Table, upem);

            ByteBuffer post_Table = getTableBuffer(postTag);
            setUnderlineMetrics(post_Table, upem);
        }

        metrics[offset] = stPos * pointSize;
        metrics[offset+1] = stSize * pointSize;

        metrics[offset+2] = ulPos * pointSize;
        metrics[offset+3] = ulSize * pointSize;
    }

    private String makeString(byte[] bytes, int len,
                             short platformID, short encoding) {

        if (platformID == MAC_PLATFORM_ID) {
            encoding = -1; // hack so we can re-use the code below.
        }

        /* Check for fonts using encodings 2->6 is just for
         * some old DBCS fonts, apparently mostly on Solaris.
         * Some of these fonts encode ascii names as double-byte characters.
         * ie with a leading zero byte for what properly should be a
         * single byte-char.
         */
        if (encoding >=2 && encoding <= 6) {
             byte[] oldbytes = bytes;
             int oldlen = len;
             bytes = new byte[oldlen];
             len = 0;
             for (int i=0; i<oldlen; i++) {
                 if (oldbytes[i] != 0) {
                     bytes[len++] = oldbytes[i];
                 }
             }
         }

        String charset;
        switch (encoding) {
            case -1: charset = "US-ASCII";break;
            case 1:  charset = "UTF-16";  break; // most common case first.
            case 0:  charset = "UTF-16";  break; // symbol uses this
            case 2:  charset = "SJIS";    break;
            case 3:  charset = "GBK";     break;
            case 4:  charset = "MS950";   break;
            case 5:  charset = "EUC_KR";  break;
            case 6:  charset = "Johab";   break;
            default: charset = "UTF-16";  break;
        }

        try {
            return new String(bytes, 0, len, charset);
        } catch (UnsupportedEncodingException e) {
            if (FontUtilities.isLogging()) {
                FontUtilities.logWarning(e + " EncodingID=" + encoding);
            }
            return new String(bytes, 0, len);
        } catch (Throwable t) {
            return null;
        }
    }

    protected void initNames() {

        byte[] name = new byte[256];
        ByteBuffer buffer = getTableBuffer(nameTag);

        if (buffer != null) {
            ShortBuffer sbuffer = buffer.asShortBuffer();
            sbuffer.get(); // format - not needed.
            short numRecords = sbuffer.get();
            /* The name table uses unsigned shorts. Many of these
             * are known small values that fit in a short.
             * The values that are sizes or offsets into the table could be
             * greater than 32767, so read and store those as ints
             */
            int stringPtr = sbuffer.get() & 0xffff;

            nameLocale = sun.awt.SunToolkit.getStartupLocale();
            short nameLocaleID = getLCIDFromLocale(nameLocale);
            languageCompatibleLCIDs =
                getLanguageCompatibleLCIDsFromLocale(nameLocale);

            for (int i=0; i<numRecords; i++) {
                short platformID = sbuffer.get();
                if (platformID != MS_PLATFORM_ID &&
                    platformID != MAC_PLATFORM_ID) {
                    sbuffer.position(sbuffer.position()+5);
                    continue; // skip over this record.
                }
                short encodingID = sbuffer.get();
                short langID     = sbuffer.get();
                short nameID     = sbuffer.get();
                int nameLen    = ((int) sbuffer.get()) & 0xffff;
                int namePtr    = (((int) sbuffer.get()) & 0xffff) + stringPtr;
                String tmpName = null;

                // only want MacRoman encoding and English name on Mac.
                if ((platformID == MAC_PLATFORM_ID) &&
                    (encodingID != MACROMAN_SPECIFIC_ID ||
                     langID != MACROMAN_ENGLISH_LANG)) {
                    continue;
                }

                switch (nameID) {

                case FAMILY_NAME_ID:
                    boolean compatible = false;
                    if (familyName == null || langID == ENGLISH_LOCALE_ID ||
                        langID == nameLocaleID ||
                        (localeFamilyName == null &&
                         (compatible = isLanguageCompatible(langID))))
                    {
                        buffer.position(namePtr);
                        buffer.get(name, 0, nameLen);
                        tmpName = makeString(name, nameLen, platformID, encodingID);
                        if (familyName == null || langID == ENGLISH_LOCALE_ID){
                            familyName = tmpName;
                        }
                        if (langID == nameLocaleID ||
                            (localeFamilyName == null && compatible))
                        {
                            localeFamilyName = tmpName;
                        }
                    }
/*
                    for (int ii=0;ii<nameLen;ii++) {
                        int val = (int)name[ii]&0xff;
                        System.err.print(Integer.toHexString(val)+ " ");
                    }
                    System.err.println();
                    System.err.println("familyName="+familyName +
                                       " nameLen="+nameLen+
                                       " langID="+langID+ " eid="+encodingID +
                                       " str len="+familyName.length());

*/
                    break;

                case FULL_NAME_ID:
                    compatible = false;
                    if (fullName == null || langID == ENGLISH_LOCALE_ID ||
                        langID == nameLocaleID ||
                        (localeFullName == null &&
                         (compatible = isLanguageCompatible(langID))))
                    {
                        buffer.position(namePtr);
                        buffer.get(name, 0, nameLen);
                        tmpName = makeString(name, nameLen, platformID, encodingID);

                        if (fullName == null || langID == ENGLISH_LOCALE_ID) {
                            fullName = tmpName;
                        }
                        if (langID == nameLocaleID ||
                            (localeFullName == null && compatible))
                        {
                            localeFullName = tmpName;
                        }
                    }
                    break;
                }
            }
            if (localeFamilyName == null) {
                localeFamilyName = familyName;
            }
            if (localeFullName == null) {
                localeFullName = fullName;
            }
        }
    }

    /* Return the requested name in the requested locale, for the
     * MS platform ID. If the requested locale isn't found, return US
     * English, if that isn't found, return null and let the caller
     * figure out how to handle that.
     */
    protected String lookupName(short findLocaleID, int findNameID) {
        String foundName = null;
        byte[] name = new byte[1024];

        ByteBuffer buffer = getTableBuffer(nameTag);
        if (buffer != null) {
            ShortBuffer sbuffer = buffer.asShortBuffer();
            sbuffer.get(); // format - not needed.
            short numRecords = sbuffer.get();

            /* The name table uses unsigned shorts. Many of these
             * are known small values that fit in a short.
             * The values that are sizes or offsets into the table could be
             * greater than 32767, so read and store those as ints
             */
            int stringPtr = ((int) sbuffer.get()) & 0xffff;

            for (int i=0; i<numRecords; i++) {
                short platformID = sbuffer.get();
                if (platformID != MS_PLATFORM_ID) {
                    sbuffer.position(sbuffer.position()+5);
                    continue; // skip over this record.
                }
                short encodingID = sbuffer.get();
                short langID     = sbuffer.get();
                short nameID     = sbuffer.get();
                int   nameLen    = ((int) sbuffer.get()) & 0xffff;
                int   namePtr    = (((int) sbuffer.get()) & 0xffff) + stringPtr;
                if (nameID == findNameID &&
                    ((foundName == null && langID == ENGLISH_LOCALE_ID)
                     || langID == findLocaleID)) {
                    buffer.position(namePtr);
                    buffer.get(name, 0, nameLen);
                    foundName = makeString(name, nameLen, platformID, encodingID);
                    if (langID == findLocaleID) {
                        return foundName;
                    }
                }
            }
        }
        return foundName;
    }

    /**
     * @return number of logical fonts. Is "1" for all but TTC files
     */
    public int getFontCount() {
        return directoryCount;
    }

    protected synchronized FontScaler getScaler() {
        if (scaler == null) {
            scaler = FontScaler.getScaler(this, fontIndex,
                supportsCJK, fileSize);
        }
        return scaler;
    }


    /* Postscript name is rarely requested. Don't waste cycles locating it
     * as part of font creation, nor storage to hold it. Get it only on demand.
     */
    @Override
    public String getPostscriptName() {
        String name = lookupName(ENGLISH_LOCALE_ID, POSTSCRIPT_NAME_ID);
        if (name == null) {
            return fullName;
        } else {
            return name;
        }
    }

    @Override
    public String getFontName(Locale locale) {
        if (locale == null) {
            return fullName;
        } else if (locale.equals(nameLocale) && localeFullName != null) {
            return localeFullName;
        } else {
            short localeID = getLCIDFromLocale(locale);
            String name = lookupName(localeID, FULL_NAME_ID);
            if (name == null) {
                return fullName;
            } else {
                return name;
            }
        }
    }

    // Return a Microsoft LCID from the given Locale.
    // Used when getting localized font data.

    private static void addLCIDMapEntry(Map<String, Short> map,
                                        String key, short value) {
        map.put(key, Short.valueOf(value));
    }

    private static synchronized void createLCIDMap() {
        if (lcidMap != null) {
            return;
        }

        Map<String, Short> map = new HashMap<>(200);

        // the following statements are derived from the langIDMap
        // in src/windows/native/java/lang/java_props_md.c using the following
        // awk script:
        //    $1~/\/\*/   { next}
        //    $3~/\?\?/   { next }
        //    $3!~/_/     { next }
        //    $1~/0x0409/ { next }
        //    $1~/0x0c0a/ { next }
        //    $1~/0x042c/ { next }
        //    $1~/0x0443/ { next }
        //    $1~/0x0812/ { next }
        //    $1~/0x04/   { print "        addLCIDMapEntry(map, " substr($3, 0, 3) "\", (short) " substr($1, 0, 6) ");" ; next }
        //    $3~/,/      { print "        addLCIDMapEntry(map, " $3  " (short) " substr($1, 0, 6) ");" ; next }
        //                { print "        addLCIDMapEntry(map, " $3 ", (short) " substr($1, 0, 6) ");" ; next }
        // The lines of this script:
        // - eliminate comments
        // - eliminate questionable locales
        // - eliminate language-only locales
        // - eliminate the default LCID value
        // - eliminate a few other unneeded LCID values
        // - print language-only locale entries for x04* LCID values
        //   (apparently Microsoft doesn't use language-only LCID values -
        //   see http://www.microsoft.com/OpenType/otspec/name.htm
        // - print complete entries for all other LCID values
        // Run
        //     awk -f awk-script langIDMap > statements
        addLCIDMapEntry(map, "ar", (short) 0x0401);
        addLCIDMapEntry(map, "bg", (short) 0x0402);
        addLCIDMapEntry(map, "ca", (short) 0x0403);
        addLCIDMapEntry(map, "zh", (short) 0x0404);
        addLCIDMapEntry(map, "cs", (short) 0x0405);
        addLCIDMapEntry(map, "da", (short) 0x0406);
        addLCIDMapEntry(map, "de", (short) 0x0407);
        addLCIDMapEntry(map, "el", (short) 0x0408);
        addLCIDMapEntry(map, "es", (short) 0x040a);
        addLCIDMapEntry(map, "fi", (short) 0x040b);
        addLCIDMapEntry(map, "fr", (short) 0x040c);
        addLCIDMapEntry(map, "iw", (short) 0x040d);
        addLCIDMapEntry(map, "hu", (short) 0x040e);
        addLCIDMapEntry(map, "is", (short) 0x040f);
        addLCIDMapEntry(map, "it", (short) 0x0410);
        addLCIDMapEntry(map, "ja", (short) 0x0411);
        addLCIDMapEntry(map, "ko", (short) 0x0412);
        addLCIDMapEntry(map, "nl", (short) 0x0413);
        addLCIDMapEntry(map, "no", (short) 0x0414);
        addLCIDMapEntry(map, "pl", (short) 0x0415);
        addLCIDMapEntry(map, "pt", (short) 0x0416);
        addLCIDMapEntry(map, "rm", (short) 0x0417);
        addLCIDMapEntry(map, "ro", (short) 0x0418);
        addLCIDMapEntry(map, "ru", (short) 0x0419);
        addLCIDMapEntry(map, "hr", (short) 0x041a);
        addLCIDMapEntry(map, "sk", (short) 0x041b);
        addLCIDMapEntry(map, "sq", (short) 0x041c);
        addLCIDMapEntry(map, "sv", (short) 0x041d);
        addLCIDMapEntry(map, "th", (short) 0x041e);
        addLCIDMapEntry(map, "tr", (short) 0x041f);
        addLCIDMapEntry(map, "ur", (short) 0x0420);
        addLCIDMapEntry(map, "in", (short) 0x0421);
        addLCIDMapEntry(map, "uk", (short) 0x0422);
        addLCIDMapEntry(map, "be", (short) 0x0423);
        addLCIDMapEntry(map, "sl", (short) 0x0424);
        addLCIDMapEntry(map, "et", (short) 0x0425);
        addLCIDMapEntry(map, "lv", (short) 0x0426);
        addLCIDMapEntry(map, "lt", (short) 0x0427);
        addLCIDMapEntry(map, "fa", (short) 0x0429);
        addLCIDMapEntry(map, "vi", (short) 0x042a);
        addLCIDMapEntry(map, "hy", (short) 0x042b);
        addLCIDMapEntry(map, "eu", (short) 0x042d);
        addLCIDMapEntry(map, "mk", (short) 0x042f);
        addLCIDMapEntry(map, "tn", (short) 0x0432);
        addLCIDMapEntry(map, "xh", (short) 0x0434);
        addLCIDMapEntry(map, "zu", (short) 0x0435);
        addLCIDMapEntry(map, "af", (short) 0x0436);
        addLCIDMapEntry(map, "ka", (short) 0x0437);
        addLCIDMapEntry(map, "fo", (short) 0x0438);
        addLCIDMapEntry(map, "hi", (short) 0x0439);
        addLCIDMapEntry(map, "mt", (short) 0x043a);
        addLCIDMapEntry(map, "se", (short) 0x043b);
        addLCIDMapEntry(map, "gd", (short) 0x043c);
        addLCIDMapEntry(map, "ms", (short) 0x043e);
        addLCIDMapEntry(map, "kk", (short) 0x043f);
        addLCIDMapEntry(map, "ky", (short) 0x0440);
        addLCIDMapEntry(map, "sw", (short) 0x0441);
        addLCIDMapEntry(map, "tt", (short) 0x0444);
        addLCIDMapEntry(map, "bn", (short) 0x0445);
        addLCIDMapEntry(map, "pa", (short) 0x0446);
        addLCIDMapEntry(map, "gu", (short) 0x0447);
        addLCIDMapEntry(map, "ta", (short) 0x0449);
        addLCIDMapEntry(map, "te", (short) 0x044a);
        addLCIDMapEntry(map, "kn", (short) 0x044b);
        addLCIDMapEntry(map, "ml", (short) 0x044c);
        addLCIDMapEntry(map, "mr", (short) 0x044e);
        addLCIDMapEntry(map, "sa", (short) 0x044f);
        addLCIDMapEntry(map, "mn", (short) 0x0450);
        addLCIDMapEntry(map, "cy", (short) 0x0452);
        addLCIDMapEntry(map, "gl", (short) 0x0456);
        addLCIDMapEntry(map, "dv", (short) 0x0465);
        addLCIDMapEntry(map, "qu", (short) 0x046b);
        addLCIDMapEntry(map, "mi", (short) 0x0481);
        addLCIDMapEntry(map, "ar_IQ", (short) 0x0801);
        addLCIDMapEntry(map, "zh_CN", (short) 0x0804);
        addLCIDMapEntry(map, "de_CH", (short) 0x0807);
        addLCIDMapEntry(map, "en_GB", (short) 0x0809);
        addLCIDMapEntry(map, "es_MX", (short) 0x080a);
        addLCIDMapEntry(map, "fr_BE", (short) 0x080c);
        addLCIDMapEntry(map, "it_CH", (short) 0x0810);
        addLCIDMapEntry(map, "nl_BE", (short) 0x0813);
        addLCIDMapEntry(map, "no_NO_NY", (short) 0x0814);
        addLCIDMapEntry(map, "pt_PT", (short) 0x0816);
        addLCIDMapEntry(map, "ro_MD", (short) 0x0818);
        addLCIDMapEntry(map, "ru_MD", (short) 0x0819);
        addLCIDMapEntry(map, "sr_CS", (short) 0x081a);
        addLCIDMapEntry(map, "sv_FI", (short) 0x081d);
        addLCIDMapEntry(map, "az_AZ", (short) 0x082c);
        addLCIDMapEntry(map, "se_SE", (short) 0x083b);
        addLCIDMapEntry(map, "ga_IE", (short) 0x083c);
        addLCIDMapEntry(map, "ms_BN", (short) 0x083e);
        addLCIDMapEntry(map, "uz_UZ", (short) 0x0843);
        addLCIDMapEntry(map, "qu_EC", (short) 0x086b);
        addLCIDMapEntry(map, "ar_EG", (short) 0x0c01);
        addLCIDMapEntry(map, "zh_HK", (short) 0x0c04);
        addLCIDMapEntry(map, "de_AT", (short) 0x0c07);
        addLCIDMapEntry(map, "en_AU", (short) 0x0c09);
        addLCIDMapEntry(map, "fr_CA", (short) 0x0c0c);
        addLCIDMapEntry(map, "sr_CS", (short) 0x0c1a);
        addLCIDMapEntry(map, "se_FI", (short) 0x0c3b);
        addLCIDMapEntry(map, "qu_PE", (short) 0x0c6b);
        addLCIDMapEntry(map, "ar_LY", (short) 0x1001);
        addLCIDMapEntry(map, "zh_SG", (short) 0x1004);
        addLCIDMapEntry(map, "de_LU", (short) 0x1007);
        addLCIDMapEntry(map, "en_CA", (short) 0x1009);
        addLCIDMapEntry(map, "es_GT", (short) 0x100a);
        addLCIDMapEntry(map, "fr_CH", (short) 0x100c);
        addLCIDMapEntry(map, "hr_BA", (short) 0x101a);
        addLCIDMapEntry(map, "ar_DZ", (short) 0x1401);
        addLCIDMapEntry(map, "zh_MO", (short) 0x1404);
        addLCIDMapEntry(map, "de_LI", (short) 0x1407);
        addLCIDMapEntry(map, "en_NZ", (short) 0x1409);
        addLCIDMapEntry(map, "es_CR", (short) 0x140a);
        addLCIDMapEntry(map, "fr_LU", (short) 0x140c);
        addLCIDMapEntry(map, "bs_BA", (short) 0x141a);
        addLCIDMapEntry(map, "ar_MA", (short) 0x1801);
        addLCIDMapEntry(map, "en_IE", (short) 0x1809);
        addLCIDMapEntry(map, "es_PA", (short) 0x180a);
        addLCIDMapEntry(map, "fr_MC", (short) 0x180c);
        addLCIDMapEntry(map, "sr_BA", (short) 0x181a);
        addLCIDMapEntry(map, "ar_TN", (short) 0x1c01);
        addLCIDMapEntry(map, "en_ZA", (short) 0x1c09);
        addLCIDMapEntry(map, "es_DO", (short) 0x1c0a);
        addLCIDMapEntry(map, "sr_BA", (short) 0x1c1a);
        addLCIDMapEntry(map, "ar_OM", (short) 0x2001);
        addLCIDMapEntry(map, "en_JM", (short) 0x2009);
        addLCIDMapEntry(map, "es_VE", (short) 0x200a);
        addLCIDMapEntry(map, "ar_YE", (short) 0x2401);
        addLCIDMapEntry(map, "es_CO", (short) 0x240a);
        addLCIDMapEntry(map, "ar_SY", (short) 0x2801);
        addLCIDMapEntry(map, "en_BZ", (short) 0x2809);
        addLCIDMapEntry(map, "es_PE", (short) 0x280a);
        addLCIDMapEntry(map, "ar_JO", (short) 0x2c01);
        addLCIDMapEntry(map, "en_TT", (short) 0x2c09);
        addLCIDMapEntry(map, "es_AR", (short) 0x2c0a);
        addLCIDMapEntry(map, "ar_LB", (short) 0x3001);
        addLCIDMapEntry(map, "en_ZW", (short) 0x3009);
        addLCIDMapEntry(map, "es_EC", (short) 0x300a);
        addLCIDMapEntry(map, "ar_KW", (short) 0x3401);
        addLCIDMapEntry(map, "en_PH", (short) 0x3409);
        addLCIDMapEntry(map, "es_CL", (short) 0x340a);
        addLCIDMapEntry(map, "ar_AE", (short) 0x3801);
        addLCIDMapEntry(map, "es_UY", (short) 0x380a);
        addLCIDMapEntry(map, "ar_BH", (short) 0x3c01);
        addLCIDMapEntry(map, "es_PY", (short) 0x3c0a);
        addLCIDMapEntry(map, "ar_QA", (short) 0x4001);
        addLCIDMapEntry(map, "es_BO", (short) 0x400a);
        addLCIDMapEntry(map, "es_SV", (short) 0x440a);
        addLCIDMapEntry(map, "es_HN", (short) 0x480a);
        addLCIDMapEntry(map, "es_NI", (short) 0x4c0a);
        addLCIDMapEntry(map, "es_PR", (short) 0x500a);

        lcidMap = map;
    }

    private static short getLCIDFromLocale(Locale locale) {
        // optimize for common case
        if (locale.equals(Locale.US)) {
            return US_LCID;
        }

        if (lcidMap == null) {
            createLCIDMap();
        }

        String key = locale.toString();
        while (!"".equals(key)) {
            Short lcidObject = lcidMap.get(key);
            if (lcidObject != null) {
                return lcidObject.shortValue();
            }
            int pos = key.lastIndexOf('_');
            if (pos < 1) {
                return US_LCID;
            }
            key = key.substring(0, pos);
        }

        return US_LCID;
    }

    @Override
    public String getFamilyName(Locale locale) {
        if (locale == null) {
            return familyName;
        } else if (locale.equals(nameLocale) && localeFamilyName != null) {
            return localeFamilyName;
        } else {
            short localeID = getLCIDFromLocale(locale);
            String name = lookupName(localeID, FAMILY_NAME_ID);
            if (name == null) {
                return familyName;
            } else {
                return name;
            }
        }
    }

    public CharToGlyphMapper getMapper() {
        if (mapper == null) {
            mapper = new TrueTypeGlyphMapper(this);
        }
        return mapper;
    }

    /* This duplicates initNames() but that has to run fast as its used
     * during typical start-up and the information here is likely never
     * needed.
     */
    protected void initAllNames(int requestedID, HashSet<String> names) {
        byte[] name = new byte[256];
        ByteBuffer buffer = getTableBuffer(nameTag);

        if (buffer != null) {
            ShortBuffer sbuffer = buffer.asShortBuffer();
            sbuffer.get(); // format - not needed.
            short numRecords = sbuffer.get();

            /* The name table uses unsigned shorts. Many of these
             * are known small values that fit in a short.
             * The values that are sizes or offsets into the table could be
             * greater than 32767, so read and store those as ints
             */
            int stringPtr = ((int) sbuffer.get()) & 0xffff;
            for (int i=0; i<numRecords; i++) {
                short platformID = sbuffer.get();
                if (platformID != MS_PLATFORM_ID) {
                    sbuffer.position(sbuffer.position()+5);
                    continue; // skip over this record.
                }
                short encodingID = sbuffer.get();
                /* short langID = */ sbuffer.get();
                short nameID     = sbuffer.get();
                int   nameLen    = ((int) sbuffer.get()) & 0xffff;
                int   namePtr    = (((int) sbuffer.get()) & 0xffff) + stringPtr;

                if (nameID == requestedID) {
                    buffer.position(namePtr);
                    buffer.get(name, 0, nameLen);
                    names.add(makeString(name, nameLen, platformID, encodingID));
                }
            }
        }
    }

    String[] getAllFamilyNames() {
        HashSet<String> aSet = new HashSet<>();
        try {
            initAllNames(FAMILY_NAME_ID, aSet);
        } catch (Exception e) {
            /* In case of malformed font */
        }
        return aSet.toArray(new String[0]);
    }

    String[] getAllFullNames() {
        HashSet<String> aSet = new HashSet<>();
        try {
            initAllNames(FULL_NAME_ID, aSet);
        } catch (Exception e) {
            /* In case of malformed font */
        }
        return aSet.toArray(new String[0]);
    }

    /*  Used by the OpenType engine for mark positioning.
     */
    @Override
    Point2D.Float getGlyphPoint(long pScalerContext,
                                int glyphCode, int ptNumber) {
        try {
            return getScaler().getGlyphPoint(pScalerContext,
                                             glyphCode, ptNumber);
        } catch(FontScalerException fe) {
            return null;
        }
    }

    private char[] gaspTable;

    private char[] getGaspTable() {

        if (gaspTable != null) {
            return gaspTable;
        }

        ByteBuffer buffer = getTableBuffer(gaspTag);
        if (buffer == null) {
            return gaspTable = new char[0];
        }

        CharBuffer cbuffer = buffer.asCharBuffer();
        char format = cbuffer.get();
        /* format "1" has appeared for some Windows Vista fonts.
         * Its presently undocumented but the existing values
         * seem to be still valid so we can use it.
         */
        if (format > 1) { // unrecognised format
            return gaspTable = new char[0];
        }

        char numRanges = cbuffer.get();
        if (4+numRanges*4 > getTableSize(gaspTag)) { // sanity check
            return gaspTable = new char[0];
        }
        gaspTable = new char[2*numRanges];
        cbuffer.get(gaspTable);
        return gaspTable;
    }

    /* This is to obtain info from the TT 'gasp' (grid-fitting and
     * scan-conversion procedure) table which specifies three combinations:
     * Hint, Smooth (greyscale), Hint and Smooth.
     * In this simplified scheme we don't distinguish the latter two. We
     * hint even at small sizes, so as to preserve metrics consistency.
     * If the information isn't available default values are substituted.
     * The more precise defaults we'd do if we distinguished the cases are:
     * Bold (no other style) fonts :
     * 0-8 : Smooth ( do grey)
     * 9+  : Hint + smooth (gridfit + grey)
     * Plain, Italic and Bold-Italic fonts :
     * 0-8 : Smooth ( do grey)
     * 9-17 : Hint (gridfit)
     * 18+  : Hint + smooth (gridfit + grey)
     * The defaults should rarely come into play as most TT fonts provide
     * better defaults.
     * REMIND: consider unpacking the table into an array of booleans
     * for faster use.
     */
    @Override
    public boolean useAAForPtSize(int ptsize) {

        char[] gasp = getGaspTable();
        if (gasp.length > 0) {
            for (int i=0;i<gasp.length;i+=2) {
                if (ptsize <= gasp[i]) {
                    return ((gasp[i+1] & 0x2) != 0); // bit 2 means DO_GRAY;
                }
            }
            return true;
        }

        if (style == Font.BOLD) {
            return true;
        } else {
            return ptsize <= 8 || ptsize >= 18;
        }
    }

    @Override
    public boolean hasSupplementaryChars() {
        return ((TrueTypeGlyphMapper)getMapper()).hasSupplementaryChars();
    }

    @Override
    public String toString() {
        return "** TrueType Font: Family="+familyName+ " Name="+fullName+
            " style="+style+" fileName="+getPublicFileName();
    }


    private static Map<String, short[]> lcidLanguageCompatibilityMap;
    private static final short[] EMPTY_COMPATIBLE_LCIDS = new short[0];

    // the language compatible LCIDs for this font's nameLocale
    private short[] languageCompatibleLCIDs;

    /*
     * Returns true if the given lcid's language is compatible
     * to the language of the startup Locale. I.e. if
     * startupLocale.getLanguage().equals(lcidLocale.getLanguage()) would
     * return true.
     */
    private boolean isLanguageCompatible(short lcid){
        for (short s : languageCompatibleLCIDs) {
            if (s == lcid) {
                return true;
            }
        }
        return false;
    }

    /*
     * Returns an array of all the language compatible LCIDs for the
     * given Locale. This array is later used to find compatible
     * locales.
     */
    private static short[] getLanguageCompatibleLCIDsFromLocale(Locale locale) {
        if (lcidLanguageCompatibilityMap == null) {
            createLCIDMap();
            createLCIDLanguageCompatibilityMap();
        }
        String language = locale.getLanguage();
        short[] result = lcidLanguageCompatibilityMap.get(language);
        return result == null ? EMPTY_COMPATIBLE_LCIDS : result;
    }

//     private static void prtLine(String s) {
//        System.out.println(s);
//     }

//     /*
//      * Initializes the map from Locale keys (e.g. "en_BZ" or "de")
//      * to language compatible LCIDs.
//      * This map could be statically created based on the fixed known set
//      * added to lcidMap.
//      */
//     private static void createLCIDLanguageCompatibilityMap() {
//         if (lcidLanguageCompatibilityMap != null) {
//             return;
//         }
//         HashMap<String, List<Short>> result = new HashMap<>();
//         for (Entry<String, Short> e : lcidMap.entrySet()) {
//             String language = e.getKey();
//             int index = language.indexOf('_');
//             if (index != -1) {
//                 language = language.substring(0, index);
//             }
//             List<Short> list = result.get(language);
//             if (list == null) {
//                 list = new ArrayList<>();
//                 result.put(language, list);
//             }
//             if (index == -1) {
//                 list.add(0, e.getValue());
//             } else{
//                 list.add(e.getValue());
//             }
//         }
//         Map<String, short[]> compMap = new HashMap<>();
//         for (Entry<String, List<Short>> e : result.entrySet()) {
//             if (e.getValue().size() > 1) {
//                 List<Short> list = e.getValue();
//                 short[] shorts = new short[list.size()];
//                 for (int i = 0; i < shorts.length; i++) {
//                     shorts[i] = list.get(i);
//                 }
//                 compMap.put(e.getKey(), shorts);
//             }
//         }

//         /* Now dump code to init the map to System.out */
//         prtLine("    private static void createLCIDLanguageCompatibilityMap() {");
//         prtLine("");

//         prtLine("        Map<String, short[]> map = new HashMap<>();");
//         prtLine("");
//         prtLine("        short[] sarr;");
//         for (Entry<String, short[]> e : compMap.entrySet()) {
//             String lang = e.getKey();
//             short[] ids = e.getValue();
//             StringBuilder sb = new StringBuilder("sarr = new short[] { ");
//             for (int i = 0; i < ids.length; i++) {
//                 sb.append(ids[i]+", ");
//             }
//             sb.append("}");
//             prtLine("        " + sb + ";");
//             prtLine("        map.put(\"" + lang + "\", sarr);");
//         }
//         prtLine("");
//         prtLine("        lcidLanguageCompatibilityMap = map;");
//         prtLine("    }");
//         /* done dumping map */

//         lcidLanguageCompatibilityMap = compMap;
//     }

    private static void createLCIDLanguageCompatibilityMap() {

        Map<String, short[]> map = new HashMap<>();

        short[] sarr;
        sarr = new short[] { 1031, 3079, 5127, 2055, 4103, };
        map.put("de", sarr);
        sarr = new short[] { 1044, 2068, };
        map.put("no", sarr);
        sarr = new short[] { 1049, 2073, };
        map.put("ru", sarr);
        sarr = new short[] { 1053, 2077, };
        map.put("sv", sarr);
        sarr = new short[] { 1046, 2070, };
        map.put("pt", sarr);
        sarr = new short[] { 1131, 3179, 2155, };
        map.put("qu", sarr);
        sarr = new short[] { 1086, 2110, };
        map.put("ms", sarr);
        sarr = new short[] { 11273, 3081, 12297, 8201, 10249, 4105, 13321, 6153, 7177, 5129, 2057, };
        map.put("en", sarr);
        sarr = new short[] { 1050, 4122, };
        map.put("hr", sarr);
        sarr = new short[] { 1040, 2064, };
        map.put("it", sarr);
        sarr = new short[] { 1036, 5132, 6156, 2060, 3084, 4108, };
        map.put("fr", sarr);
        sarr = new short[] { 1034, 12298, 14346, 2058, 8202, 19466, 17418, 9226, 13322, 5130, 7178, 11274, 16394, 4106, 10250, 6154, 18442, 20490, 15370, };
        map.put("es", sarr);
        sarr = new short[] { 1028, 3076, 5124, 4100, 2052, };
        map.put("zh", sarr);
        sarr = new short[] { 1025, 8193, 16385, 9217, 2049, 14337, 15361, 11265, 13313, 10241, 7169, 12289, 4097, 5121, 6145, 3073, };
        map.put("ar", sarr);
        sarr = new short[] { 1083, 3131, 2107, };
        map.put("se", sarr);
        sarr = new short[] { 1048, 2072, };
        map.put("ro", sarr);
        sarr = new short[] { 1043, 2067, };
        map.put("nl", sarr);
        sarr = new short[] { 7194, 3098, };
        map.put("sr", sarr);

        lcidLanguageCompatibilityMap = map;
    }
}
