/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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


package sun.security.krb5.internal.rcache;

import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

import sun.security.action.GetPropertyAction;
import sun.security.krb5.internal.KerberosTime;
import sun.security.krb5.internal.Krb5;
import sun.security.krb5.internal.KrbApErrException;
import sun.security.krb5.internal.ReplayCache;


/**
 * A dfl file is used to sustores AuthTime entries when the system property
 * sun.security.krb5.rcache is set to
 *
 *    dfl(|:path/|:path/name|:name)
 *
 * The file will be path/name. If path is not given, it will be
 *
 *    System.getProperty("java.io.tmpdir")
 *
 * If name is not given, it will be
 *
 *    service_euid
 *
 * in which euid is available as jdk.internal.misc.VM.geteuid().
 *
 * The file has a header:
 *
 *    i16 0x0501 (KRB5_RC_VNO) in network order
 *    i32 number of seconds for lifespan (in native order, same below)
 *
 * followed by cache entries concatenated, which can be encoded in
 * 2 styles:
 *
 * The traditional style is:
 *
 *    LC of client principal
 *    LC of server principal
 *    i32 cusec of Authenticator
 *    i32 ctime of Authenticator
 *
 * The new style has a hash:
 *
 *    LC of ""
 *    LC of "HASH:%s %lu:%s %lu:%s" of (hash, clientlen, client, serverlen,
 *          server) where msghash is 32 char (lower case) text mode md5sum
 *          of the ciphertext of authenticator.
 *    i32 cusec of Authenticator
 *    i32 ctime of Authenticator
 *
 * where LC of a string means
 *
 *    i32 strlen(string) + 1
 *    octets of string, with the \0x00 ending
 *
 * The old style block is always created by MIT krb5 used even if a new style
 * is available, which means there can be 2 entries for a single Authenticator.
 * Java also does this way.
 *
 * See src/lib/krb5/rcache/rc_io.c and src/lib/krb5/rcache/rc_dfl.c.
 *
 * Update: New version can use other hash algorithms.
 */
public class DflCache extends ReplayCache {

    private static final int KRB5_RV_VNO = 0x501;
    private static final int EXCESSREPS = 30;   // if missed-hit>this, recreate

    private final String source;

    private static long uid;
    static {
        // Available on Linux and Mac. Otherwise, -1 and no _euid suffix
        uid = jdk.internal.misc.VM.geteuid();
    }

    public DflCache (String source) {
        this.source = source;
    }

    private static String defaultPath() {
        return GetPropertyAction.privilegedGetProperty("java.io.tmpdir");
    }

    private static String defaultFile(String server) {
        // service/host@REALM -> service
        int slash = server.indexOf('/');
        if (slash == -1) {
            // A normal principal? say, dummy@REALM
            slash = server.indexOf('@');
        }
        if (slash != -1) {
            // Should not happen, but be careful
            server= server.substring(0, slash);
        }
        if (uid != -1) {
            server += "_" + uid;
        }
        return server;
    }

    private static Path getFileName(String source, String server) {
        String path, file;
        if (source.equals("dfl")) {
            path = defaultPath();
            file = defaultFile(server);
        } else if (source.startsWith("dfl:")) {
            source = source.substring(4);
            int pos = source.lastIndexOf('/');
            int pos1 = source.lastIndexOf('\\');
            if (pos1 > pos) pos = pos1;
            if (pos == -1) {
                // Only file name
                path = defaultPath();
                file = source;
            } else if (new File(source).isDirectory()) {
                // Only path
                path = source;
                file = defaultFile(server);
            } else {
                // Full pathname
                path = null;
                file = source;
            }
        } else {
            throw new IllegalArgumentException();
        }
        return new File(path, file).toPath();
    }

    @Override
    public void checkAndStore(KerberosTime currTime, AuthTimeWithHash time)
            throws KrbApErrException {
        try {
            checkAndStore0(currTime, time);
        } catch (IOException ioe) {
            KrbApErrException ke = new KrbApErrException(Krb5.KRB_ERR_GENERIC);
            ke.initCause(ioe);
            throw ke;
        }
    }

    private synchronized void checkAndStore0(KerberosTime currTime, AuthTimeWithHash time)
            throws IOException, KrbApErrException {
        Path p = getFileName(source, time.server);
        int missed = 0;
        try (Storage s = new Storage()) {
            try {
                missed = s.loadAndCheck(p, time, currTime);
            } catch (IOException ioe) {
                // Non-existing or invalid file
                Storage.create(p);
                missed = s.loadAndCheck(p, time, currTime);
            }
            s.append(time);
        }
        if (missed > EXCESSREPS) {
            Storage.expunge(p, currTime);
        }
    }


    private static class Storage implements Closeable {
        // Static methods
        @SuppressWarnings("try")
        private static void create(Path p) throws IOException {
            try (SeekableByteChannel newChan = createNoClose(p)) {
                // Do nothing, wait for close
            }
            makeMine(p);
        }

        private static void makeMine(Path p) throws IOException {
            // chmod to owner-rw only, otherwise MIT krb5 rejects
            try {
                Set<PosixFilePermission> attrs = new HashSet<>();
                attrs.add(PosixFilePermission.OWNER_READ);
                attrs.add(PosixFilePermission.OWNER_WRITE);
                Files.setPosixFilePermissions(p, attrs);
            } catch (UnsupportedOperationException uoe) {
                // No POSIX permission. That's OK.
            }
        }

        private static SeekableByteChannel createNoClose(Path p)
                throws IOException {
            SeekableByteChannel newChan = Files.newByteChannel(
                    p, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE);
            ByteBuffer buffer = ByteBuffer.allocate(6);
            buffer.putShort((short)KRB5_RV_VNO);
            buffer.order(ByteOrder.nativeOrder());
            buffer.putInt(KerberosTime.getDefaultSkew());
            buffer.flip();
            newChan.write(buffer);
            return newChan;
        }

        private static void expunge(Path p, KerberosTime currTime)
                throws IOException {
            Path p2 = Files.createTempFile(p.getParent(), "rcache", null);
            try (SeekableByteChannel oldChan = Files.newByteChannel(p);
                    SeekableByteChannel newChan = createNoClose(p2)) {
                long timeLimit = currTime.getSeconds() - readHeader(oldChan);
                while (true) {
                    try {
                        AuthTime at = AuthTime.readFrom(oldChan);
                        if (at.ctime > timeLimit) {
                            ByteBuffer bb = ByteBuffer.wrap(at.encode(true));
                            newChan.write(bb);
                        }
                    } catch (BufferUnderflowException e) {
                        break;
                    }
                }
            }
            makeMine(p2);
            Files.move(p2, p,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        }

        // Instance methods
        SeekableByteChannel chan;
        private int loadAndCheck(Path p, AuthTimeWithHash time,
                KerberosTime currTime)
                throws IOException, KrbApErrException {
            int missed = 0;
            if (Files.isSymbolicLink(p)) {
                throw new IOException("Symlink not accepted");
            }
            try {
                Set<PosixFilePermission> perms =
                        Files.getPosixFilePermissions(p);
                if (uid != -1 &&
                        (Integer)Files.getAttribute(p, "unix:uid") != uid) {
                    throw new IOException("Not mine");
                }
                if (perms.contains(PosixFilePermission.GROUP_READ) ||
                        perms.contains(PosixFilePermission.GROUP_WRITE) ||
                        perms.contains(PosixFilePermission.GROUP_EXECUTE) ||
                        perms.contains(PosixFilePermission.OTHERS_READ) ||
                        perms.contains(PosixFilePermission.OTHERS_WRITE) ||
                        perms.contains(PosixFilePermission.OTHERS_EXECUTE)) {
                    throw new IOException("Accessible by someone else");
                }
            } catch (UnsupportedOperationException uoe) {
                // No POSIX permissions? Ignore it.
            }
            chan = Files.newByteChannel(p, StandardOpenOption.WRITE,
                    StandardOpenOption.READ);

            long timeLimit = currTime.getSeconds() - readHeader(chan);

            long pos = 0;
            boolean seeNewButNotSame = false;
            while (true) {
                try {
                    pos = chan.position();
                    AuthTime a = AuthTime.readFrom(chan);
                    if (a instanceof AuthTimeWithHash) {
                        if (time.equals(a)) {
                            // Exact match, must be a replay
                            throw new KrbApErrException(Krb5.KRB_AP_ERR_REPEAT);
                        } else if (time.sameTimeDiffHash((AuthTimeWithHash)a)) {
                            // Two different authenticators in the same second.
                            // Remember it
                            seeNewButNotSame = true;
                        }
                    } else {
                        if (time.isSameIgnoresHash(a)) {
                            // Two authenticators in the same second. Considered
                            // same if we haven't seen a new style version of it
                            if (!seeNewButNotSame) {
                                throw new KrbApErrException(Krb5.KRB_AP_ERR_REPEAT);
                            }
                        }
                    }
                    if (a.ctime < timeLimit) {
                        missed++;
                    } else {
                        missed--;
                    }
                } catch (BufferUnderflowException e) {
                    // Half-written file?
                    chan.position(pos);
                    break;
                }
            }
            return missed;
        }

        private static int readHeader(SeekableByteChannel chan)
                throws IOException {
            ByteBuffer bb = ByteBuffer.allocate(6);
            chan.read(bb);
            if (bb.getShort(0) != KRB5_RV_VNO) {
                throw new IOException("Not correct rcache version");
            }
            bb.order(ByteOrder.nativeOrder());
            return bb.getInt(2);
        }

        private void append(AuthTimeWithHash at) throws IOException {
            // Write an entry with hash, to be followed by one without it,
            // for the benefit of old implementations.
            ByteBuffer bb;
            bb = ByteBuffer.wrap(at.encode(true));
            chan.write(bb);
            bb = ByteBuffer.wrap(at.encode(false));
            chan.write(bb);
        }

        @Override
        public void close() throws IOException {
            if (chan != null) chan.close();
            chan = null;
        }
    }
}
