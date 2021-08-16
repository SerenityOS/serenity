/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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
 *
 */

/*
 * @test
 * @bug 8234011
 * @summary Check that jdk.nio.zipfs.ZipFileSystem doesn't cache more than ZipFileSystem.MAX_FLATER Inflater/Deflater objects
 * @run main ReleaseDeflater
 * @modules jdk.zipfs/jdk.nio.zipfs:+open
 * @author Volker Simonis
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class ReleaseDeflater {
    public static void main(String[] args) throws Throwable {
        Path zipFile = Paths.get("ReleaseDeflaterTest.zip");
        try (FileSystem fs = FileSystems.newFileSystem(zipFile, Map.of("create", true))) {
            FileSystemProvider zprov = fs.provider();
            Path test = fs.getPath("test.txt");
            int STREAMS = 100;
            List<OutputStream> ostreams = new ArrayList<>(STREAMS);
            List<InputStream> istreams = new ArrayList<>(STREAMS);
            for (int i = 0; i < STREAMS; i++) {
                OutputStream zos = zprov.newOutputStream(test);
                ostreams.add(zos);
                zos.write("Hello".getBytes());
            }
            for (OutputStream os : ostreams) {
                os.close();
            }
            for (int i = 0; i < STREAMS; i++) {
                InputStream zis = zprov.newInputStream(test);
                istreams.add(zis);
            }
            for (InputStream is : istreams) {
                is.close();
            }
            try {
                Field max_flaters = fs.getClass().getDeclaredField("MAX_FLATER");
                max_flaters.setAccessible(true);
                int MAX_FLATERS = max_flaters.getInt(fs);
                Field inflaters = fs.getClass().getDeclaredField("inflaters");
                inflaters.setAccessible(true);
                int inflater_count = ((List<?>) inflaters.get(fs)).size();
                if (inflater_count > MAX_FLATERS) {
                    throw new Exception("Too many inflaters " + inflater_count);
                }
                Field deflaters = fs.getClass().getDeclaredField("deflaters");
                deflaters.setAccessible(true);
                int deflater_count = ((List<?>) deflaters.get(fs)).size();
                if (deflater_count > MAX_FLATERS) {
                    throw new Exception("Too many deflaters " + deflater_count);
                }
            } catch (NoSuchFieldException nsfe) {
                // Probably the implementation has changed, so there's not much we can do...
                throw new RuntimeException("Implementation of jdk.nio.zipfs.ZipFileSystem changed - disable or fix the test");
            }
        } finally {
            Files.deleteIfExists(zipFile);
        }

    }
}
