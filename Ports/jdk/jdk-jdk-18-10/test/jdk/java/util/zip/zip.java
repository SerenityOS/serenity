/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * A stripped-down version of Jar tool with a "-encoding" option to
 * support non-UTF8 encoidng for entry name and comment.
 */
public class zip {
    String program;
    PrintStream out, err;
    String fname;
    String zname = "";
    String[] files;
    Charset cs = Charset.forName("UTF-8");

    Map<String, File> entryMap = new HashMap<>();
    Set<File> entries = new LinkedHashSet<>();
    List<String> paths = new ArrayList<>();

    CRC32 crc32 = new CRC32();
    /*
     * cflag: create
     * uflag: update
     * xflag: xtract
     * tflag: table
     * vflag: verbose
     * flag0: no zip compression (store only)
     */
    boolean cflag, uflag, xflag, tflag, vflag, flag0;

    private static ResourceBundle rsrc;
    static {
        try {
            // just use the jar message
            rsrc = ResourceBundle.getBundle("sun.tools.jar.resources.jar");
        } catch (MissingResourceException e) {
            throw new Error("Fatal: Resource for jar is missing");
        }
    }

    public zip(PrintStream out, PrintStream err, String program) {
        this.out = out;
        this.err = err;
        this.program = program;
    }

    private boolean ok;

    public synchronized boolean run(String args[]) {
        ok = true;
        if (!parseArgs(args)) {
            return false;
        }
        try {
            if (cflag || uflag) {
                if (fname != null) {
                    zname = fname.replace(File.separatorChar, '/');
                    if (zname.startsWith("./")) {
                        zname = zname.substring(2);
                    }
                }
            }
            if (cflag) {
                OutputStream out;
                if (fname != null) {
                    out = new FileOutputStream(fname);
                } else {
                    out = new FileOutputStream(FileDescriptor.out);
                    if (vflag) {
                         vflag = false;
                    }
                }
                expand(null, files, false);
                create(new BufferedOutputStream(out, 4096));
                out.close();
            } else if (uflag) {
                File inputFile = null, tmpFile = null;
                FileInputStream in;
                FileOutputStream out;
                if (fname != null) {
                    inputFile = new File(fname);
                    String path = inputFile.getParent();
                    tmpFile = File.createTempFile("tmp", null,
                              new File((path == null) ? "." : path));
                    in = new FileInputStream(inputFile);
                    out = new FileOutputStream(tmpFile);
                } else {
                    in = new FileInputStream(FileDescriptor.in);
                    out = new FileOutputStream(FileDescriptor.out);
                    vflag = false;
                }
                expand(null, files, true);
                boolean updateOk = update(in, new BufferedOutputStream(out));
                if (ok) {
                    ok = updateOk;
                }
                in.close();
                out.close();
                if (fname != null) {
                    inputFile.delete();
                    if (!tmpFile.renameTo(inputFile)) {
                        tmpFile.delete();
                        throw new IOException(getMsg("error.write.file"));
                    }
                    tmpFile.delete();
                }
            } else if (tflag) {
                replaceFSC(files);
                if (fname != null) {
                    list(fname, files);
                } else {
                    InputStream in = new FileInputStream(FileDescriptor.in);
                    try{
                        list(new BufferedInputStream(in), files);
                    } finally {
                        in.close();
                    }
                }
            } else if (xflag) {
                replaceFSC(files);
                if (fname != null && files != null) {
                    extract(fname, files);
                } else {
                    InputStream in = (fname == null)
                        ? new FileInputStream(FileDescriptor.in)
                        : new FileInputStream(fname);
                    try {
                        extract(new BufferedInputStream(in), files);
                    } finally {
                        in.close();
                    }
                }
            }
        } catch (IOException e) {
            fatalError(e);
            ok = false;
        } catch (Error ee) {
            ee.printStackTrace();
            ok = false;
        } catch (Throwable t) {
            t.printStackTrace();
            ok = false;
        }
        out.flush();
        err.flush();
        return ok;
    }


    boolean parseArgs(String args[]) {
        try {
            args = parse(args);
        } catch (FileNotFoundException e) {
            fatalError(formatMsg("error.cant.open", e.getMessage()));
            return false;
        } catch (IOException e) {
            fatalError(e);
            return false;
        }
        int count = 1;
        try {
            String flags = args[0];
            if (flags.startsWith("-")) {
                flags = flags.substring(1);
            }
            for (int i = 0; i < flags.length(); i++) {
                switch (flags.charAt(i)) {
                case 'c':
                    if (xflag || tflag || uflag) {
                        usageError();
                        return false;
                    }
                    cflag = true;
                    break;
                case 'u':
                    if (cflag || xflag || tflag) {
                        usageError();
                        return false;
                    }
                    uflag = true;
                    break;
                case 'x':
                    if (cflag || uflag || tflag) {
                        usageError();
                        return false;
                    }
                    xflag = true;
                    break;
                case 't':
                    if (cflag || uflag || xflag) {
                        usageError();
                        return false;
                    }
                    tflag = true;
                    break;
                case 'v':
                    vflag = true;
                    break;
                case 'f':
                    fname = args[count++];
                    break;
                case '0':
                    flag0 = true;
                    break;
                default:
                    error(formatMsg("error.illegal.option",
                                String.valueOf(flags.charAt(i))));
                    usageError();
                    return false;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            usageError();
            return false;
        }
        if (!cflag && !tflag && !xflag && !uflag) {
            error(getMsg("error.bad.option"));
            usageError();
            return false;
        }
        /* parse file arguments */
        int n = args.length - count;
        if (n > 0) {
            int k = 0;
            String[] nameBuf = new String[n];
            try {
                for (int i = count; i < args.length; i++) {
                    if (args[i].equals("-encoding")) {
                        cs = Charset.forName(args[++i]);
                    } else if (args[i].equals("-C")) {
                        /* change the directory */
                        String dir = args[++i];
                        dir = (dir.endsWith(File.separator) ?
                               dir : (dir + File.separator));
                        dir = dir.replace(File.separatorChar, '/');
                        while (dir.indexOf("//") > -1) {
                            dir = dir.replace("//", "/");
                        }
                        paths.add(dir.replace(File.separatorChar, '/'));
                        nameBuf[k++] = dir + args[++i];
                    } else {
                        nameBuf[k++] = args[i];
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                usageError();
                return false;
            }
            if (k != 0) {
                files = new String[k];
                System.arraycopy(nameBuf, 0, files, 0, k);
            }
        } else if (cflag || uflag) {
            error(getMsg("error.bad.uflag"));
            usageError();
            return false;
        }
        return true;
    }

    void expand(File dir, String[] files, boolean isUpdate) {
        if (files == null) {
            return;
        }
        for (int i = 0; i < files.length; i++) {
            File f;
            if (dir == null) {
                f = new File(files[i]);
            } else {
                f = new File(dir, files[i]);
            }
            if (f.isFile()) {
                if (entries.add(f)) {
                    if (isUpdate)
                        entryMap.put(entryName(f.getPath()), f);
                }
            } else if (f.isDirectory()) {
                if (entries.add(f)) {
                    if (isUpdate) {
                        String dirPath = f.getPath();
                        dirPath = (dirPath.endsWith(File.separator)) ? dirPath :
                            (dirPath + File.separator);
                        entryMap.put(entryName(dirPath), f);
                    }
                    expand(f, f.list(), isUpdate);
                }
            } else {
                error(formatMsg("error.nosuch.fileordir", String.valueOf(f)));
                ok = false;
            }
        }
    }

    void create(OutputStream out) throws IOException
    {
        try (ZipOutputStream zos = new ZipOutputStream(out, cs)) {
            if (flag0) {
                zos.setMethod(ZipOutputStream.STORED);
            }
            for (File file: entries) {
                addFile(zos, file);
            }
        }
    }

    boolean update(InputStream in, OutputStream out) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(in, cs);
             ZipOutputStream zos = new ZipOutputStream(out, cs))
        {
            ZipEntry e = null;
            byte[] buf = new byte[1024];
            int n = 0;

            // put the old entries first, replace if necessary
            while ((e = zis.getNextEntry()) != null) {
                String name = e.getName();
                if (!entryMap.containsKey(name)) { // copy the old stuff
                    // do our own compression
                    ZipEntry e2 = new ZipEntry(name);
                    e2.setMethod(e.getMethod());
                    e2.setTime(e.getTime());
                    e2.setComment(e.getComment());
                    e2.setExtra(e.getExtra());
                    if (e.getMethod() == ZipEntry.STORED) {
                        e2.setSize(e.getSize());
                        e2.setCrc(e.getCrc());
                    }
                    zos.putNextEntry(e2);
                    while ((n = zis.read(buf, 0, buf.length)) != -1) {
                        zos.write(buf, 0, n);
                    }
                } else { // replace with the new files
                    File f = entryMap.get(name);
                    addFile(zos, f);
                    entryMap.remove(name);
                    entries.remove(f);
                }
            }

            // add the remaining new files
            for (File f : entries) {
                addFile(zos, f);
            }
        }
        return true;
    }

    private String entryName(String name) {
        name = name.replace(File.separatorChar, '/');
        String matchPath = "";
        for (String path : paths) {
            if (name.startsWith(path) && (path.length() > matchPath.length())) {
                matchPath = path;
            }
        }
        name = name.substring(matchPath.length());

        if (name.startsWith("/")) {
            name = name.substring(1);
        } else if (name.startsWith("./")) {
            name = name.substring(2);
        }
        return name;
    }

    void addFile(ZipOutputStream zos, File file) throws IOException {
        String name = file.getPath();
        boolean isDir = file.isDirectory();
        if (isDir) {
            name = name.endsWith(File.separator) ? name :
                (name + File.separator);
        }
        name = entryName(name);

        if (name.equals("") || name.equals(".") || name.equals(zname)) {
            return;
        }

        long size = isDir ? 0 : file.length();

        if (vflag) {
            out.print(formatMsg("out.adding", name));
        }
        ZipEntry e = new ZipEntry(name);
        e.setTime(file.lastModified());
        if (size == 0) {
            e.setMethod(ZipEntry.STORED);
            e.setSize(0);
            e.setCrc(0);
        } else if (flag0) {
            e.setSize(size);
            e.setMethod(ZipEntry.STORED);
            crc32File(e, file);
        }
        zos.putNextEntry(e);
        if (!isDir) {
            byte[] buf = new byte[8192];
            int len;
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            while ((len = is.read(buf, 0, buf.length)) != -1) {
                zos.write(buf, 0, len);
            }
            is.close();
        }
        zos.closeEntry();
        /* report how much compression occurred. */
        if (vflag) {
            size = e.getSize();
            long csize = e.getCompressedSize();
            out.print(formatMsg2("out.size", String.valueOf(size),
                        String.valueOf(csize)));
            if (e.getMethod() == ZipEntry.DEFLATED) {
                long ratio = 0;
                if (size != 0) {
                    ratio = ((size - csize) * 100) / size;
                }
                output(formatMsg("out.deflated", String.valueOf(ratio)));
            } else {
                output(getMsg("out.stored"));
            }
        }
    }

    private void crc32File(ZipEntry e, File f) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(f));
        byte[] buf = new byte[8192];
        crc32.reset();
        int r = 0;
        int nread = 0;
        long len = f.length();
        while ((r = is.read(buf)) != -1) {
            nread += r;
            crc32.update(buf, 0, r);
        }
        is.close();
        if (nread != (int) len) {
            throw new ZipException(formatMsg(
                        "error.incorrect.length", f.getPath()));
        }
        e.setCrc(crc32.getValue());
    }

    void replaceFSC(String files[]) {
        if (files != null) {
            for (String file : files) {
                file = file.replace(File.separatorChar, '/');
            }
        }
    }

    Set<ZipEntry> newDirSet() {
        return new HashSet<ZipEntry>() {
            private static final long serialVersionUID = 4547977575248028254L;

            public boolean add(ZipEntry e) {
                return (e == null || super.add(e));
            }};
    }

    void updateLastModifiedTime(Set<ZipEntry> zes) throws IOException {
        for (ZipEntry ze : zes) {
            long lastModified = ze.getTime();
            if (lastModified != -1) {
                File f = new File(ze.getName().replace('/', File.separatorChar));
                f.setLastModified(lastModified);
            }
        }
    }

    void extract(InputStream in, String files[]) throws IOException {
        ZipInputStream zis = new ZipInputStream(in, cs);
        ZipEntry e;
        Set<ZipEntry> dirs = newDirSet();
        while ((e = zis.getNextEntry()) != null) {
            if (files == null) {
                dirs.add(extractFile(zis, e));
            } else {
                String name = e.getName();
                for (String file : files) {
                    if (name.startsWith(file)) {
                        dirs.add(extractFile(zis, e));
                        break;
                    }
                }
            }
        }
        updateLastModifiedTime(dirs);
    }

    void extract(String fname, String files[]) throws IOException {
        try (ZipFile zf = new ZipFile(fname, cs)) {
            Set<ZipEntry> dirs = newDirSet();
            Enumeration<? extends ZipEntry> zes = zf.entries();
            while (zes.hasMoreElements()) {
                ZipEntry e = zes.nextElement();
                if (files == null) {
                    dirs.add(extractFile(zf.getInputStream(e), e));
                } else {
                    String name = e.getName();
                    for (String file : files) {
                        if (name.startsWith(file)) {
                            dirs.add(extractFile(zf.getInputStream(e), e));
                            break;
                        }
                    }
                }
            }
            updateLastModifiedTime(dirs);
        }
    }

    ZipEntry extractFile(InputStream is, ZipEntry e) throws IOException {
        ZipEntry rc = null;
        String name = e.getName();
        File f = new File(e.getName().replace('/', File.separatorChar));
        if (e.isDirectory()) {
            if (f.exists()) {
                if (!f.isDirectory()) {
                    throw new IOException(formatMsg("error.create.dir",
                        f.getPath()));
                }
            } else {
                if (!f.mkdirs()) {
                    throw new IOException(formatMsg("error.create.dir",
                        f.getPath()));
                } else {
                    rc = e;
                }
            }
            if (vflag) {
                output(formatMsg("out.create", name));
            }
        } else {
            if (f.getParent() != null) {
                File d = new File(f.getParent());
                if (!d.exists() && !d.mkdirs() || !d.isDirectory()) {
                    throw new IOException(formatMsg(
                        "error.create.dir", d.getPath()));
                }
            }
            OutputStream os = new FileOutputStream(f);
            byte[] b = new byte[8192];
            int len;
            try {
                while ((len = is.read(b, 0, b.length)) != -1) {
                    os.write(b, 0, len);
                }
            } finally {
                if (is instanceof ZipInputStream)
                    ((ZipInputStream)is).closeEntry();
                else
                    is.close();
                os.close();
            }
            if (vflag) {
                if (e.getMethod() == ZipEntry.DEFLATED) {
                    output(formatMsg("out.inflated", name));
                } else {
                    output(formatMsg("out.extracted", name));
                }
            }
        }
        long lastModified = e.getTime();
        if (lastModified != -1) {
            f.setLastModified(lastModified);
        }
        return rc;
    }

    void list(InputStream in, String files[]) throws IOException {
        ZipInputStream zis = new ZipInputStream(in, cs);
        ZipEntry e;
        while ((e = zis.getNextEntry()) != null) {
            zis.closeEntry();
            printEntry(e, files);
        }
    }

    void list(String fname, String files[]) throws IOException {
        try (ZipFile zf = new ZipFile(fname, cs)) {
            Enumeration<? extends ZipEntry> zes = zf.entries();
            while (zes.hasMoreElements()) {
                printEntry(zes.nextElement(), files);
            }
        }
    }

    void printEntry(ZipEntry e, String[] files) throws IOException {
        if (files == null) {
            printEntry(e);
        } else {
            String name = e.getName();
            for (String file : files) {
                if (name.startsWith(file)) {
                    printEntry(e);
                    return;
                }
            }
        }
    }

    void printEntry(ZipEntry e) throws IOException {
        if (vflag) {
            StringBuilder sb = new StringBuilder();
            String s = Long.toString(e.getSize());
            for (int i = 6 - s.length(); i > 0; --i) {
                sb.append(' ');
            }
            sb.append(s).append(' ').append(new Date(e.getTime()).toString());
            sb.append(' ').append(e.getName());
            output(sb.toString());
        } else {
            output(e.getName());
        }
    }

    void usageError() {
        error(
        "Usage: zip {ctxu}[vf0] [zip-file] [-encoding encname][-C dir] files ...\n" +
        "Options:\n" +
        "   -c  create new archive\n" +
        "   -t  list table of contents for archive\n" +
        "   -x  extract named (or all) files from archive\n" +
        "   -u  update existing archive\n" +
        "   -v  generate verbose output on standard output\n" +
        "   -f  specify archive file name\n" +
        "   -0  store only; use no ZIP compression\n" +
        "   -C  change to the specified directory and include the following file\n" +
        "If any file is a directory then it is processed recursively.\n");
    }

    void fatalError(Exception e) {
        e.printStackTrace();
    }


    void fatalError(String s) {
        error(program + ": " + s);
    }


    protected void output(String s) {
        out.println(s);
    }

    protected void error(String s) {
        err.println(s);
    }

    private String getMsg(String key) {
        try {
            return (rsrc.getString(key));
        } catch (MissingResourceException e) {
            throw new Error("Error in message file");
        }
    }

    private String formatMsg(String key, String arg) {
        String msg = getMsg(key);
        String[] args = new String[1];
        args[0] = arg;
        return MessageFormat.format(msg, (Object[]) args);
    }

    private String formatMsg2(String key, String arg, String arg1) {
        String msg = getMsg(key);
        String[] args = new String[2];
        args[0] = arg;
        args[1] = arg1;
        return MessageFormat.format(msg, (Object[]) args);
    }

    public static String[] parse(String[] args) throws IOException
    {
        ArrayList<String> newArgs = new ArrayList<String>(args.length);
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.length() > 1 && arg.charAt(0) == '@') {
                arg = arg.substring(1);
                if (arg.charAt(0) == '@') {
                    newArgs.add(arg);
                } else {
                    loadCmdFile(arg, newArgs);
                }
            } else {
                newArgs.add(arg);
            }
        }
        return newArgs.toArray(new String[newArgs.size()]);
    }

    private static void loadCmdFile(String name, List<String> args) throws IOException
    {
        Reader r = new BufferedReader(new FileReader(name));
        StreamTokenizer st = new StreamTokenizer(r);
        st.resetSyntax();
        st.wordChars(' ', 255);
        st.whitespaceChars(0, ' ');
        st.commentChar('#');
        st.quoteChar('"');
        st.quoteChar('\'');
        while (st.nextToken() != StreamTokenizer.TT_EOF) {
            args.add(st.sval);
        }
        r.close();
    }

    public static void main(String args[]) {
        zip z = new zip(System.out, System.err, "zip");
        System.exit(z.run(args) ? 0 : 1);
    }
}
