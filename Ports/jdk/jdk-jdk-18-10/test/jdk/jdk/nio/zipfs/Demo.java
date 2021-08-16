/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.spi.*;
import java.nio.file.attribute.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.StandardCopyOption.*;

/*
 * ZipFileSystem usage demo
 *
 * java Demo action ZipfileName [...]
 *
 * @author Xueming Shen
 */

public class Demo {

    static enum Action {
        rename,          // <java Demo rename zipfile src dst>
                         // rename entry src to dst inside zipfile

        movein,          // <java Demo movein zipfile src dst>
                         // move an external src file into zipfile
                         // as entry dst

        moveout,         // <java Demo moveout zipfile src dst>
                         // move a zipfile entry src out to dst

        copy,            // <java Demo copy zipfile src dst>
                         // copy entry src to dst inside zipfile

        copyin,          // <java Demo copyin zipfile src dst>
                         // copy an external src file into zipfile
                         // as entry dst

        copyin_attrs,    // <java Demo copyin_attrs zipfile src dst>
                         // copy an external src file into zipfile
                         // as entry dst, with attributes (timestamp)

        copyout,         // <java Demo copyout zipfile src dst>
                         // copy zipfile entry src" out to file dst

        copyout_attrs,   // <java Demo copyout_attrs zipfile src dst>

        zzmove,          // <java Demo zzmove zfsrc zfdst path>
                         // move entry path/dir from zfsrc to zfdst

        zzcopy,          // <java Demo zzcopy zfsrc zfdst path>
                         // copy path from zipfile zfsrc to zipfile
                         // zfdst

        attrs,           // <java Demo attrs zipfile path>
                         // printout the attributes of entry path

        attrsspace,      // <java Demo attrsspace zipfile path>
                         // printout the storespace attrs of entry path

        setmtime,        // <java Demo setmtime zipfile "MM/dd/yy-HH:mm:ss" path...>
                         // set the lastModifiedTime of entry path

        setatime,        // <java Demo setatime zipfile "MM/dd/yy-HH:mm:ss" path...>
        setctime,        // <java Demo setctime zipfile "MM/dd/yy-HH:mm:ss" path...>

        lsdir,           // <java Demo lsdir zipfile dir>
                         // list dir's direct child files/dirs

        mkdir,           // <java Demo mkdir zipfile dir>

        mkdirs,          // <java Demo mkdirs zipfile dir>

        list,            // <java Demo list zipfile [dir]>
                         // recursively list all entries of dir
                         // via DirectoryStream

        tlist,           // <java Demo tlist zipfile [dir]>
                         // list with buildDirTree=true

        vlist,           // <java Demo vlist zipfile [dir]>
                         // recursively verbose list all entries of
                         // dir via DirectoryStream

        walk,            // <java Demo walk zipfile [dir]>
                         // recursively walk all entries of dir
                         // via Files.walkFileTree

        twalk,           // <java Demo twalk zipfile [dir]>
                         // walk with buildDirTree=true

        extract,         // <java Demo extract zipfile file [...]>

        update,          // <java Demo extract zipfile file [...]>

        delete,          // <java Demo delete zipfile file [...]>

        add,             // <java Demo add zipfile file [...]>

        create,          // <java Demo create zipfile file [...]>
                         // create a new zipfile if it doesn't exit
                         // and then add the file(s) into it.

        attrs2,          // <java Demo attrs2 zipfile file [...]>
                         // test different ways to print attrs

        prof,
    }

    public static void main(String[] args) throws Throwable {
        FileSystemProvider provider = getZipFSProvider();
        if (provider == null) {
            System.err.println("ZIP filesystem provider is not installed");
            System.exit(1);
        }

        Action action = Action.valueOf(args[0]);
        Map<String, Object> env = new HashMap<>();
        if (action == Action.create)
            env.put("create", "true");
        try (FileSystem fs = provider.newFileSystem(Paths.get(args[1]), env)) {
            Path path, src, dst;
            switch (action) {
            case rename:
                src = fs.getPath(args[2]);
                dst = fs.getPath(args[3]);
                Files.move(src, dst);
                break;
            case moveout:
                src = fs.getPath(args[2]);
                dst = Paths.get(args[3]);
                Files.move(src, dst);
                break;
            case movein:
                src = Paths.get(args[2]);
                dst = fs.getPath(args[3]);
                Files.move(src, dst);
                break;
            case copy:
                src = fs.getPath(args[2]);
                dst = fs.getPath(args[3]);
                Files.copy(src, dst);
                break;
            case copyout:
                src = fs.getPath(args[2]);
                dst = Paths.get(args[3]);
                Files.copy(src, dst);
                break;
            case copyin:
                src = Paths.get(args[2]);
                dst = fs.getPath(args[3]);
                Files.copy(src, dst);
                break;
            case copyin_attrs:
                src = Paths.get(args[2]);
                dst = fs.getPath(args[3]);
                Files.copy(src, dst, COPY_ATTRIBUTES);
                break;
            case copyout_attrs:
                src = fs.getPath(args[2]);
                dst = Paths.get(args[3]);
                Files.copy(src, dst, COPY_ATTRIBUTES);
                break;
            case zzmove:
                try (FileSystem fs2 = provider.newFileSystem(Paths.get(args[2]), env)) {
                    z2zmove(fs, fs2, args[3]);
                }
                break;
            case zzcopy:
                try (FileSystem fs2 = provider.newFileSystem(Paths.get(args[2]), env)) {
                    z2zcopy(fs, fs2, args[3]);
                }
                break;
            case attrs:
                for (int i = 2; i < args.length; i++) {
                    path = fs.getPath(args[i]);
                    System.out.println(path);
                    System.out.println(
                        Files.readAttributes(path, BasicFileAttributes.class).toString());
                }
                break;
            case setmtime:
                DateFormat df = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
                Date newDatetime = df.parse(args[2]);
                for (int i = 3; i < args.length; i++) {
                    path = fs.getPath(args[i]);
                    Files.setAttribute(path, "lastModifiedTime",
                                       FileTime.fromMillis(newDatetime.getTime()));
                    System.out.println(
                        Files.readAttributes(path, BasicFileAttributes.class).toString());
                }
                break;
            case setctime:
                df = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
                newDatetime = df.parse(args[2]);
                for (int i = 3; i < args.length; i++) {
                    path = fs.getPath(args[i]);
                    Files.setAttribute(path, "creationTime",
                                       FileTime.fromMillis(newDatetime.getTime()));
                    System.out.println(
                        Files.readAttributes(path, BasicFileAttributes.class).toString());
                }
                break;
            case setatime:
                df = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
                newDatetime = df.parse(args[2]);
                for (int i = 3; i < args.length; i++) {
                    path = fs.getPath(args[i]);
                    Files.setAttribute(path, "lastAccessTime",
                                       FileTime.fromMillis(newDatetime.getTime()));
                    System.out.println(
                        Files.readAttributes(path, BasicFileAttributes.class).toString());
                }
                break;
            case attrsspace:
                path = fs.getPath("/");
                FileStore fstore = Files.getFileStore(path);
                System.out.printf("filestore[%s]%n", fstore.name());
                System.out.printf("    totalSpace: %d%n",
                                  (Long)fstore.getAttribute("totalSpace"));
                System.out.printf("   usableSpace: %d%n",
                                  (Long)fstore.getAttribute("usableSpace"));
                System.out.printf("  unallocSpace: %d%n",
                                  (Long)fstore.getAttribute("unallocatedSpace"));
                break;
            case list:
            case tlist:
                if (args.length < 3)
                    list(fs.getPath("/"), false);
                else
                    list(fs.getPath(args[2]), false);
                break;
            case vlist:
                if (args.length < 3)
                    list(fs.getPath("/"), true);
                else
                    list(fs.getPath(args[2]), true);
                break;
            case twalk:
            case walk:
                walk(fs.getPath((args.length > 2)? args[2] : "/"));
                break;
            case extract:
                if (args.length == 2) {
                     extract(fs, "/");
                } else {
                    for (int i = 2; i < args.length; i++) {
                        extract(fs, args[i]);
                    }
                }
                break;
            case delete:
                for (int i = 2; i < args.length; i++)
                    Files.delete(fs.getPath(args[i]));
                break;
            case create:
            case add:
            case update:
                for (int i = 2; i < args.length; i++) {
                    update(fs, args[i]);
                }
                break;
            case lsdir:
                path = fs.getPath(args[2]);
                final String fStr = (args.length > 3)?args[3]:"";
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(path,
                    new DirectoryStream.Filter<Path>() {
                        @Override
                        public boolean accept(Path path) {
                            return path.toString().contains(fStr);
                        }
                    }))
                {
                    for (Path p : ds)
                        System.out.println(p);
                }
                break;
            case mkdir:
                Files.createDirectory(fs.getPath(args[2]));
                break;
            case mkdirs:
                mkdirs(fs.getPath(args[2]));
                break;
            case attrs2:
                for (int i = 2; i < args.length; i++) {
                    path = fs.getPath(args[i]);
                    System.out.printf("%n%s%n", path);
                    System.out.println("-------(1)---------");
                    System.out.println(
                        Files.readAttributes(path, BasicFileAttributes.class).toString());
                    System.out.println("-------(2)---------");
                    Map<String, Object> map = Files.readAttributes(path, "zip:*");
                    for (Map.Entry<String, Object> e : map.entrySet()) {
                        System.out.printf("    %s : %s%n", e.getKey(), e.getValue());
                    }
                    System.out.println("-------(3)---------");
                    map = Files.readAttributes(path, "size,lastModifiedTime,isDirectory");
                    for (Map.Entry<String, ?> e : map.entrySet()) {
                        System.out.printf("    %s : %s%n", e.getKey(), e.getValue());
                    }
                }
                break;
            case prof:
                list(fs.getPath("/"), false);
                while (true) {
                    Thread.sleep(10000);
                    //list(fs.getPath("/"), true);
                    System.out.println("sleeping...");
                }
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    private static FileSystemProvider getZipFSProvider() {
        for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            if ("jar".equals(provider.getScheme()))
                return provider;
        }
        return null;
    }

    @SuppressWarnings("unused")
    /**
     * Not used in demo, but included for demonstrational purposes.
     */
    private static byte[] getBytes(String name) {
        return name.getBytes();
    }

    @SuppressWarnings("unused")
    /**
     * Not used in demo, but included for demonstrational purposes.
     */
    private static String getString(byte[] name) {
        return new String(name);
    }

    private static void walk(Path path) throws IOException
    {
        Files.walkFileTree(
            path,
            new SimpleFileVisitor<Path>() {
                private int indent = 0;
                private void indent() {
                    int n = 0;
                    while (n++ < indent)
                        System.out.printf(" ");
                }

                @Override
                public FileVisitResult visitFile(Path file,
                                                 BasicFileAttributes attrs)
                {
                    indent();
                    System.out.printf("%s%n", file.getFileName().toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir,
                                                         BasicFileAttributes attrs)
                {
                    indent();
                    System.out.printf("[%s]%n", dir.toString());
                    indent += 2;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir,
                                                          IOException ioe)
                {
                    indent -= 2;
                    return FileVisitResult.CONTINUE;
                }
        });
    }

    private static void update(FileSystem fs, String path) throws Throwable{
        Path src = FileSystems.getDefault().getPath(path);
        if (Files.isDirectory(src)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(src)) {
                for (Path child : ds)
                    update(fs, child.toString());
            }
        } else {
            Path dst = fs.getPath(path);
            Path parent = dst.getParent();
            if (parent != null && Files.notExists(parent))
                mkdirs(parent);
            Files.copy(src, dst, REPLACE_EXISTING);
        }
    }

    private static void extract(FileSystem fs, String path) throws Throwable{
        Path src = fs.getPath(path);
        if (Files.isDirectory(src)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(src)) {
                for (Path child : ds)
                    extract(fs, child.toString());
            }
        } else {
            if (path.startsWith("/"))
                path = path.substring(1);
            Path dst = FileSystems.getDefault().getPath(path);
            Path parent = dst.getParent();
            if (Files.notExists(parent))
                mkdirs(parent);
            Files.copy(src, dst, REPLACE_EXISTING);
        }
    }

    // use DirectoryStream
    private static void z2zcopy(FileSystem src, FileSystem dst, String path)
        throws IOException
    {
        Path srcPath = src.getPath(path);
        Path dstPath = dst.getPath(path);

        if (Files.isDirectory(srcPath)) {
            if (!Files.exists(dstPath)) {
                try {
                    mkdirs(dstPath);
                } catch (FileAlreadyExistsException x) {}
            }
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(srcPath)) {
                for (Path child : ds) {
                    z2zcopy(src, dst,
                            path + (path.endsWith("/")?"":"/") + child.getFileName());
                }
            }
        } else {
            //System.out.println("copying..." + path);
            Files.copy(srcPath, dstPath);
        }
    }

    // use TreeWalk to move
    private static void z2zmove(FileSystem src, FileSystem dst, String path)
        throws IOException
    {
        final Path srcPath = src.getPath(path).toAbsolutePath();
        final Path dstPath = dst.getPath(path).toAbsolutePath();

        Files.walkFileTree(srcPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file,
                                            BasicFileAttributes attrs)
            {
                Path dst = srcPath.relativize(file);
                dst = dstPath.resolve(dst);
                try {
                    Path parent = dstPath.getParent();
                    if (parent != null && Files.notExists(parent))
                        mkdirs(parent);
                    Files.move(file, dst);
                } catch (IOException x) {
                    x.printStackTrace();
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                                                     BasicFileAttributes attrs)
            {
                Path dst = srcPath.relativize(dir);
                dst = dstPath.resolve(dst);
                try {

                    if (Files.notExists(dst))
                        mkdirs(dst);
                } catch (IOException x) {
                    x.printStackTrace();
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir,
                                                      IOException ioe)
                throws IOException
            {
                try {
                    Files.delete(dir);
                } catch (IOException x) {
                    //x.printStackTrace();
                }
                return FileVisitResult.CONTINUE;
            }
        });

    }

    private static void mkdirs(Path path) throws IOException {
        path = path.toAbsolutePath();
        Path parent = path.getParent();
        if (parent != null) {
            if (Files.notExists(parent))
                mkdirs(parent);
        }
        Files.createDirectory(path);
    }

    /**
     * Not used in demo, but included for demonstrational purposes.
     */
    @SuppressWarnings("unused")
    private static void rmdirs(Path path) throws IOException {
        while (path != null && path.getNameCount() != 0) {
            Files.delete(path);
            path = path.getParent();
        }
    }

    private static void list(Path path, boolean verbose ) throws IOException {
        if (!"/".equals(path.toString())) {
           System.out.printf("  %s%n", path.toString());
           if (verbose)
                System.out.println(Files.readAttributes(path, BasicFileAttributes.class).toString());
        }
        if (Files.notExists(path))
            return;
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
                for (Path child : ds)
                    list(child, verbose);
            }
        }
    }

    /**
     * Checks that the content of two paths are equal.
     * Not used in demo, but included for demonstrational purposes.
     */
    @SuppressWarnings("unused")
    private static void checkEqual(Path src, Path dst) throws IOException
    {
        //System.out.printf("checking <%s> vs <%s>...%n",
        //                  src.toString(), dst.toString());

        //streams
        byte[] bufSrc = new byte[8192];
        byte[] bufDst = new byte[8192];
        try (InputStream isSrc = Files.newInputStream(src);
             InputStream isDst = Files.newInputStream(dst))
        {
            int nSrc = 0;
            while ((nSrc = isSrc.read(bufSrc)) != -1) {
                int nDst = 0;
                while (nDst < nSrc) {
                    int n = isDst.read(bufDst, nDst, nSrc - nDst);
                    if (n == -1) {
                        System.out.printf("checking <%s> vs <%s>...%n",
                                          src.toString(), dst.toString());
                        throw new RuntimeException("CHECK FAILED!");
                    }
                    nDst += n;
                }
                while (--nSrc >= 0) {
                    if (bufSrc[nSrc] != bufDst[nSrc]) {
                        System.out.printf("checking <%s> vs <%s>...%n",
                                          src.toString(), dst.toString());
                        throw new RuntimeException("CHECK FAILED!");
                    }
                    nSrc--;
                }
            }
        }

        // channels

        try (SeekableByteChannel chSrc = Files.newByteChannel(src);
             SeekableByteChannel chDst = Files.newByteChannel(dst))
        {
            if (chSrc.size() != chDst.size()) {
                System.out.printf("src[%s].size=%d, dst[%s].size=%d%n",
                                  chSrc.toString(), chSrc.size(),
                                  chDst.toString(), chDst.size());
                throw new RuntimeException("CHECK FAILED!");
            }
            ByteBuffer bbSrc = ByteBuffer.allocate(8192);
            ByteBuffer bbDst = ByteBuffer.allocate(8192);

            int nSrc = 0;
            while ((nSrc = chSrc.read(bbSrc)) != -1) {
                int nDst = chDst.read(bbDst);
                if (nSrc != nDst) {
                    System.out.printf("checking <%s> vs <%s>...%n",
                                      src.toString(), dst.toString());
                    throw new RuntimeException("CHECK FAILED!");
                }
                while (--nSrc >= 0) {
                    if (bbSrc.get(nSrc) != bbDst.get(nSrc)) {
                        System.out.printf("checking <%s> vs <%s>...%n",
                                          src.toString(), dst.toString());
                        throw new RuntimeException("CHECK FAILED!");
                    }
                    nSrc--;
                }
                bbSrc.flip();
                bbDst.flip();
            }
        } catch (IOException x) {
            x.printStackTrace();
        }
    }

    /**
     * Not used in demo, but included for demonstrational purposes.
     */
    @SuppressWarnings("unused")
    private static void fchCopy(Path src, Path dst) throws IOException {
        Set<OpenOption> read = new HashSet<>();
        read.add(READ);
        Set<OpenOption> openwrite = new HashSet<>();
        openwrite.add(CREATE_NEW);
        openwrite.add(WRITE);

        try (FileChannel srcFc = src.getFileSystem().provider().newFileChannel(src, read);
             FileChannel dstFc = dst.getFileSystem().provider().newFileChannel(dst, openwrite))
        {
            ByteBuffer bb = ByteBuffer.allocate(8192);
            while (srcFc.read(bb) >= 0) {
                bb.flip();
                dstFc.write(bb);
                bb.clear();
            }
        }
    }

    /**
     * Not used in demo, but included for demonstrational purposes.
     */
    @SuppressWarnings("unused")
    private static void chCopy(Path src, Path dst) throws IOException {
        Set<OpenOption> read = new HashSet<>();
        read.add(READ);
        Set<OpenOption> openwrite = new HashSet<>();
        openwrite.add(CREATE_NEW);
        openwrite.add(WRITE);

        try (SeekableByteChannel srcCh = Files.newByteChannel(src, read);
             SeekableByteChannel dstCh = Files.newByteChannel(dst, openwrite))
        {
            ByteBuffer bb = ByteBuffer.allocate(8192);
            while (srcCh.read(bb) >= 0) {
                bb.flip();
                dstCh.write(bb);
                bb.clear();
            }
        }
    }

    /**
     * Not used in demo, but included for demonstrational purposes.
     */
    @SuppressWarnings("unused")
    private static void streamCopy(Path src, Path dst) throws IOException {
        byte[] buf = new byte[8192];
        try (InputStream isSrc = Files.newInputStream(src);
             OutputStream osDst = Files.newOutputStream(dst))
        {
            int n = 0;
            while ((n = isSrc.read(buf)) != -1) {
                osDst.write(buf, 0, n);
            }
        }
    }
}
