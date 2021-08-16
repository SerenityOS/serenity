/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Basic test of jrt file system provider
 * @run testng Basic
 */

import java.io.InputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.DataInputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

/**
 * Basic tests for jrt:/ file system provider.
 */

public class Basic {

    private FileSystem theFileSystem;
    private FileSystem fs;
    private boolean isExplodedBuild = false;

    @BeforeClass
    public void setup() {
        theFileSystem = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path modulesPath = Paths.get(System.getProperty("java.home"),
                "lib", "modules");
        isExplodedBuild = Files.notExists(modulesPath);
        if (isExplodedBuild) {
            System.out.printf("%s doesn't exist.", modulesPath.toString());
            System.out.println();
            System.out.println("It is most probably an exploded build."
                    + " Skip non-default FileSystem testing.");
            return;
        }

        Map<String, String> env = new HashMap<>();
        // set java.home property to be underlying java.home
        // so that jrt-fs.jar loading is exercised.
        env.put("java.home", System.getProperty("java.home"));
        try {
            fs = FileSystems.newFileSystem(URI.create("jrt:/"), env);
        } catch (IOException ioExp) {
            throw new RuntimeException(ioExp);
        }
    }

    @AfterClass
    public void tearDown() {
        try {
            fs.close();
        } catch (Exception ignored) {}
    }

    private FileSystem selectFileSystem(boolean theDefault) {
        return theDefault? theFileSystem : fs;
    }

    // Checks that the given FileSystem is a jrt file system.
    private void checkFileSystem(FileSystem fs) {
        assertTrue(fs.provider().getScheme().equalsIgnoreCase("jrt"));
        assertTrue(fs.isOpen());
        assertTrue(fs.isReadOnly());
        assertEquals(fs.getSeparator(), "/");

        // one root
        Iterator<Path> roots = fs.getRootDirectories().iterator();
        assertTrue(roots.next().toString().equals("/"));
        assertFalse(roots.hasNext());
    }

    @Test
    public void testGetFileSystem() {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        checkFileSystem(fs);

        // getFileSystem should return the same object each time
        assertTrue(fs == FileSystems.getFileSystem(URI.create("jrt:/")));
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testCloseFileSystem() throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        fs.close(); // should throw UOE
    }

    @Test
    public void testNewFileSystem() throws Exception {
        FileSystem theFileSystem = FileSystems.getFileSystem(URI.create("jrt:/"));
        Map<String, ?> env = Collections.emptyMap();
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jrt:/"), env)) {
            checkFileSystem(fs);
            assertTrue(fs != theFileSystem);
        }
    }

    @Test
    public void testNewFileSystemWithJavaHome() throws Exception {
        if (isExplodedBuild) {
            System.out.println("Skip testNewFileSystemWithJavaHome"
                    + " since this is an exploded build");
            return;
        }

        Map<String, String> env = new HashMap<>();
        // set java.home property to be underlying java.home
        // so that jrt-fs.jar loading is exercised.
        env.put("java.home", System.getProperty("java.home"));
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jrt:/"), env)) {
            checkFileSystem(fs);
            // jrt-fs.jar classes are loaded by another (non-boot) loader in this case
            assertNotNull(fs.provider().getClass().getClassLoader());
        }
    }

    @DataProvider(name = "knownClassFiles")
    private Object[][] knownClassFiles() {
        return new Object[][] {
            { "/modules/java.base/java/lang/Object.class", true },
            { "modules/java.base/java/lang/Object.class", true },
            { "/modules/java.base/java/lang/Object.class", false },
            { "modules/java.base/java/lang/Object.class", false },
        };
    }

    @Test(dataProvider = "knownClassFiles")
    public void testKnownClassFiles(String path, boolean theDefault) throws Exception {
        if (isExplodedBuild && !theDefault) {
            System.out.println("Skip testKnownClassFiles with non-default FileSystem");
            return;
        }

        FileSystem fs = selectFileSystem(theDefault);
        Path classFile = fs.getPath(path);

        assertTrue(Files.isRegularFile(classFile));
        assertTrue(Files.size(classFile) > 0L);

        // check magic number
        try (InputStream in = Files.newInputStream(classFile)) {
            int magic = new DataInputStream(in).readInt();
            assertEquals(magic, 0xCAFEBABE);
        }
    }

    @DataProvider(name = "knownDirectories")
    private Object[][] knownDirectories() {
        return new Object[][] {
            { "/", true                     },
            { "." , true                    },
            { "./", true                    },
            { "/.", true                    },
            { "/./", true                   },
            { "/modules/java.base/..", true         },
            { "/modules/java.base/../", true        },
            { "/modules/java.base/../.", true       },
            { "/modules/java.base", true            },
            { "/modules/java.base/java/lang", true  },
            { "modules/java.base/java/lang", true   },
            { "/modules/java.base/java/lang/", true },
            { "modules/java.base/java/lang/", true  },
            { "/", false                     },
            { "." , false                    },
            { "./", false                    },
            { "/.", false                    },
            { "/./", false                   },
            { "/modules/java.base/..", false         },
            { "/modules/java.base/../", false        },
            { "/modules/java.base/../.", false       },
            { "/modules/java.base", false            },
            { "/modules/java.base/java/lang", false  },
            { "modules/java.base/java/lang", false   },
            { "/modules/java.base/java/lang/", false },
            { "modules/java.base/java/lang/", false  },
        };
    }

    @Test(dataProvider = "knownDirectories")
    public void testKnownDirectories(String path, boolean theDefault) throws Exception {
        if (isExplodedBuild && !theDefault) {
            System.out.println("Skip testKnownDirectories with non-default FileSystem");
            return;
        }

        FileSystem fs = selectFileSystem(theDefault);
        Path dir = fs.getPath(path);

        assertTrue(Files.isDirectory(dir));

        // directory should not be empty
        try (Stream<Path> stream = Files.list(dir)) {
            assertTrue(stream.count() > 0L);
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            assertTrue(stream.count() > 0L);
        }
    }

    @DataProvider(name = "topLevelNonExistingDirs")
    private Object[][] topLevelNonExistingDirs() {
        return new Object[][] {
            { "/java/lang" },
            { "java/lang"  },
            { "/java/util" },
            { "java/util"  },
            { "/modules/modules"  },
            { "/modules/modules/"  },
            { "/modules/modules/java.base"  },
            { "/modules/modules/java.base/"  },
            { "/modules/modules/java.base/java/lang/Object.class"  },
            { "/modules/modules/javax.scripting"  },
            { "/modules/modules/javax.scripting/"  },
            { "/modules/modules/javax.scripting/javax/script/ScriptEngine.class"  },
        };
    }

    @Test(dataProvider = "topLevelNonExistingDirs")
    public void testNotExists(String path) throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path dir = fs.getPath(path);

        // These directories should not be there at top level
        assertTrue(Files.notExists(dir));
    }

    /**
     * Test the URI of every file in the jrt file system
     */
    @Test
    public void testToAndFromUri() throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path top = fs.getPath("/");
        try (Stream<Path> stream = Files.walk(top)) {
            stream.forEach(path -> {
                String pathStr = path.toAbsolutePath().toString();
                URI u = null;
                try {
                    u = path.toUri();
                } catch (IOError e) {
                    assertFalse(pathStr.startsWith("/modules"));
                    return;
                }

                assertTrue(u.getScheme().equalsIgnoreCase("jrt"));
                assertFalse(u.isOpaque());
                assertTrue(u.getAuthority() == null);

                pathStr = pathStr.substring("/modules".length());
                if (pathStr.isEmpty()) {
                    pathStr = "/";
                }
                assertEquals(u.getPath(), pathStr);
                Path p = Paths.get(u);
                assertEquals(p, path);
            });
        }
    }

    // @bug 8216553: JrtFIleSystemProvider getPath(URI) omits /modules element from file path
    @Test
    public void testPathToURIConversion() throws Exception {
        var uri = URI.create("jrt:/java.base/module-info.class");
        var path = Path.of(uri);
        assertTrue(Files.exists(path));

        uri = URI.create("jrt:/java.base/../java.base/module-info.class");
        boolean seenIAE = false;
        try {
            Path.of(uri);
        } catch (IllegalArgumentException iaExp) {
            seenIAE = true;
        }
        assertTrue(seenIAE);

        // check round-trip
        var jrtfs = FileSystems.getFileSystem(URI.create("jrt:/"));
        assertTrue(Files.exists(jrtfs.getPath(path.toString())));

        path = jrtfs.getPath("/modules/../modules/java.base/");
        boolean seenIOError = false;
        try {
            path.toUri();
        } catch (IOError ioError) {
            seenIOError = true;
        }
        assertTrue(seenIOError);
    }

    @Test
    public void testDirectoryNames() throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path top = fs.getPath("/");
        // check that directory names do not have trailing '/' char
        try (Stream<Path> stream = Files.walk(top)) {
            stream.skip(1).filter(Files::isDirectory).forEach(path -> {
                assertFalse(path.toString().endsWith("/"));
            });
        }
    }

    @DataProvider(name = "pathPrefixs")
    private Object[][] pathPrefixes() {
        return new Object[][] {
            { "/"                       },
            { "modules/java.base/java/lang"     },
            { "./modules/java.base/java/lang"   },
            { "/modules/java.base/java/lang"    },
            { "/./modules/java.base/java/lang"  },
            { "modules/java.base/java/lang/"    },
            { "./modules/java.base/java/lang/"  },
            { "/./modules/java.base/java/lang/" },
        };
    }

    // @Test(dataProvider = "pathPrefixes")
    public void testParentInDirList(String dir) throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path base = fs.getPath(dir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(base)) {
            for (Path entry: stream) {
                assertTrue( entry.getParent().equals(base),
                    base.toString() + "-> " + entry.toString() );
            }
        }
    }

    @DataProvider(name = "dirStreamStringFilterData")
    private Object[][] dirStreamStringFilterData() {
        return new Object[][] {
            { "/modules/java.base/java/lang", "/reflect"      },
            { "/modules/java.base/java/lang", "/Object.class" },
            { "/modules/java.base/java/util", "/stream"       },
            { "/modules/java.base/java/util", "/List.class"   },
        };
    }

    @Test(dataProvider = "dirStreamStringFilterData")
    public void testDirectoryStreamStringFilter(String dir, String filter) throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path base = fs.getPath(dir);
        try (DirectoryStream<Path> stream =
                Files.newDirectoryStream(base, p->!p.toString().endsWith(filter))) {
            for (Path entry: stream) {
                assertFalse(entry.toString().contains(filter),
                    "filtered path seen: " + filter);
            }
        }

        // make sure without filter, we do see that matching entry!
        boolean seen = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(base)) {
            for (Path entry: stream) {
                if (entry.toString().endsWith(filter)) {
                    seen = true;
                    break;
                }
            }
        }

        assertTrue(seen, "even without filter " + filter + " is missing");
    }

    @DataProvider(name = "dirStreamFilterData")
    private Object[][] dirStreamFilterData() {
        return new Object[][] {
            {
              "/",
              (DirectoryStream.Filter<Path>)(Files::isDirectory),
              "isDirectory"
            },
            {
              "/modules/java.base/java/lang",
              (DirectoryStream.Filter<Path>)(Files::isRegularFile),
              "isFile"
            }
        };
    }

    @Test(dataProvider = "dirStreamFilterData")
    private void testDirectoryStreamFilter(String dir, DirectoryStream.Filter filter,
            String name) throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path base = fs.getPath(dir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(base, filter)) {
            for (Path entry: stream) {
                assertTrue(filter.accept(entry), "filtered path seen: " + name);
            }
        }

        // make sure without filter, we do see that matching entry!
        boolean seen = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(base)) {
            for (Path entry: stream) {
                if (filter.accept(entry)) {
                    seen = true;
                    break;
                }
            }
        }

        assertTrue(seen, "even without filter " + name + " is missing");
    }

    @Test
    private void testDirectoryStreamIterator() throws Exception {
        // run the tests with null filter (no filter)
        dirStreamIteratorTest(null);
        // run the same tests with trivial "accept all" filter
        dirStreamIteratorTest(p->true);
        // two other non-trivial ones
        dirStreamIteratorTest(Files::isDirectory);
        dirStreamIteratorTest(Files::isRegularFile);
    }

    private void dirStreamIteratorTest(DirectoryStream.Filter<Path> filter)
            throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        // This test assumes at least there are two elements in "java/lang"
        // package with any filter passed. don't change to different path here!
        Path dir = fs.getPath("/modules/java.base/java/lang");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {
            Iterator<Path> itr = stream.iterator();
            itr.hasNext();
            Path path1 = itr.next();
            // missing second hasNext call
            Path path2 = itr.next();
            assertNotEquals(path1, path2);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {
            Iterator<Path> itr = stream.iterator();
            // no hasNext calls at all
            Path path1 = itr.next();
            Path path2 = itr.next();
            assertNotEquals(path1, path2);
        }

        int numEntries = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {
            Iterator<Path> itr = stream.iterator();
            while (itr.hasNext()) {
                numEntries++;
                itr.next();
            }

            // reached EOF, next call should result in exception
            try {
                itr.next();
                throw new AssertionError("should have thrown exception");
            } catch (NoSuchElementException nsee) {
                System.out.println("got NoSuchElementException as expected");
            }
        }

        // redundant hasNext calls
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {
            Iterator<Path> itr = stream.iterator();
            // any number of hasNext should definitely stay at first element
            for (int i = 0; i < 2*numEntries; i++) {
                itr.hasNext();
            }

            for (int j = 0; j < numEntries; j++) {
                itr.next();
            }
            // exactly count number of entries!
            assertFalse(itr.hasNext());
        }
    }

    @DataProvider(name = "hiddenPaths")
    private Object[][] hiddenPaths() {
        return new Object[][] {
            { "/META-INF" },
            { "/META-INF/services" },
            { "/META-INF/services/java.nio.file.spi.FileSystemProvider" },
            { "/modules/java.base/packages.offsets" },
            { "/modules/java.instrument/packages.offsets" },
            { "/modules/jdk.zipfs/packages.offsets" },
            { "/modules/java.base/_the.java.base.vardeps" },
            { "/modules/java.base/_the.java.base_batch" },
            { "/java/lang" },
            { "/java/util" },
        };
    }

    @Test(dataProvider = "hiddenPaths")
    public void testHiddenPathsNotExposed(String path) throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        assertTrue(Files.notExists(fs.getPath(path)), path + " should not exist");
    }

    @DataProvider(name = "pathGlobPatterns")
    private Object[][] pathGlobPatterns() {
        return new Object[][] {
            { "/modules/*", "/modules/java.base", true },
            { "/modules/*", "/modules/java.base/java", false },
            { "/modules/j*", "/modules/java.base", true },
            { "/modules/J*", "/modules/java.base", false },
            { "**.class", "/modules/java.base/java/lang/Object.class", true },
            { "**.java", "/modules/java.base/java/lang/Object.class", false },
            { "**java/*", "/modules/java.base/java/lang", true },
            { "**java/lang/ref*", "/modules/java.base/java/lang/reflect", true },
            { "**java/lang/ref*", "/modules/java.base/java/lang/ref", true },
            { "**java/lang/ref?", "/modules/java.base/java/lang/ref", false },
            { "**java/lang/{ref,refl*}", "/modules/java.base/java/lang/ref", true },
            { "**java/lang/{ref,refl*}", "/modules/java.base/java/lang/reflect", true },
            { "**java/[a-u]?*/*.class", "/modules/java.base/java/util/Map.class", true },
            { "**java/util/[a-z]*.class", "/modules/java.base/java/util/TreeMap.class", false },
        };
    }

    @Test(dataProvider = "pathGlobPatterns")
    public void testGlobPathMatcher(String pattern, String path,
            boolean expectMatch) throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        PathMatcher pm = fs.getPathMatcher("glob:" + pattern);
        Path p = fs.getPath(path);
        assertTrue(Files.exists(p), path);
        assertTrue(!(pm.matches(p) ^ expectMatch),
            p + (expectMatch? " should match " : " should not match ") +
            pattern);
    }

    @DataProvider(name = "pathRegexPatterns")
    private Object[][] pathRegexPatterns() {
        return new Object[][] {
            { "/modules/.*", "/modules/java.base", true },
            { "/modules/[^/]*", "/modules/java.base/java", false },
            { "/modules/j.*", "/modules/java.base", true },
            { "/modules/J.*", "/modules/java.base", false },
            { ".*\\.class", "/modules/java.base/java/lang/Object.class", true },
            { ".*\\.java", "/modules/java.base/java/lang/Object.class", false },
            { ".*java/.*", "/modules/java.base/java/lang", true },
            { ".*java/lang/ref.*", "/modules/java.base/java/lang/reflect", true },
            { ".*java/lang/ref.*", "/modules/java.base/java/lang/ref", true },
            { ".*/java/lang/ref.+", "/modules/java.base/java/lang/ref", false },
            { ".*/java/lang/(ref|refl.*)", "/modules/java.base/java/lang/ref", true },
            { ".*/java/lang/(ref|refl.*)", "/modules/java.base/java/lang/reflect", true },
            { ".*/java/[a-u]?.*/.*\\.class", "/modules/java.base/java/util/Map.class", true },
            { ".*/java/util/[a-z]*\\.class", "/modules/java.base/java/util/TreeMap.class", false },
        };
    }

    @Test(dataProvider = "pathRegexPatterns")
    public void testRegexPathMatcher(String pattern, String path,
            boolean expectMatch) throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        PathMatcher pm = fs.getPathMatcher("regex:" + pattern);
        Path p = fs.getPath(path);
        assertTrue(Files.exists(p), path);
        assertTrue(!(pm.matches(p) ^ expectMatch),
            p + (expectMatch? " should match " : " should not match ") +
            pattern);
    }

    @Test
    public void testPackagesAndModules() throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        assertTrue(Files.isDirectory(fs.getPath("/packages")));
        assertTrue(Files.isDirectory(fs.getPath("/modules")));
    }

    @DataProvider(name = "packagesSubDirs")
    private Object[][] packagesSubDirs() {
        return new Object[][] {
            { "java.lang" },
            { "java.util" },
            { "java.nio"  },
        };
    }

    @Test(dataProvider = "packagesSubDirs")
    public void testPackagesSubDirs(String pkg) throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        assertTrue(Files.isDirectory(fs.getPath("/packages/" + pkg)),
            pkg + " missing");
    }

    @DataProvider(name = "packagesLinks")
    private Object[][] packagesLinks() {
        return new Object[][] {
            { "/packages/java.lang/java.base" },
            { "/packages/java.lang/java.instrument" },
            { "/packages/java/java.base" },
            { "/packages/java/java.instrument" },
            { "/packages/java/java.rmi"  },
            { "/packages/java/java.sql"  },
            { "/packages/javax/java.base"  },
            { "/packages/javax/java.sql"  },
            { "/packages/javax/java.xml"  },
            { "/packages/javax/java.management"  },
            { "/packages/java.util/java.base" },
        };
    }

    @Test(dataProvider = "packagesLinks")
    public void testPackagesLinks(String link) throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path path = fs.getPath(link);
        assertTrue(Files.exists(path), link + " missing");
        assertTrue(Files.isSymbolicLink(path), path + " is not a link");
        path = Files.readSymbolicLink(path);
        assertEquals(path.toString(), "/modules" + link.substring(link.lastIndexOf("/")));
    }

    @DataProvider(name = "modulesSubDirs")
    private Object[][] modulesSubDirs() {
        return new Object[][] {
            { "java.base" },
            { "java.sql" },
        };
    }

    @Test(dataProvider = "modulesSubDirs")
    public void testModulesSubDirs(String module) throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path path = fs.getPath("/modules/" + module);
        assertTrue(Files.isDirectory(path), module + " missing");
        assertTrue(!Files.isSymbolicLink(path), path + " is a link");
    }

    @DataProvider(name="linkChases")
    private Object[][] linkChases() {
        return new Object[][] {
            { "/modules/java.base/java/lang" },
            { "/modules/java.base/java/util/Vector.class" },
            { "/packages/java.lang/java.base/java/lang" },
            { "/packages/java.util/java.base/java/util/Vector.class" },
        };
    }

    @Test(dataProvider = "linkChases")
    public void testLinkChases(String link) throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path path = fs.getPath(link);
        assertTrue(Files.exists(path), link);
    }

    @Test
    public void testSymlinkDirList() throws Exception {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path path = fs.getPath("/packages/java.lang/java.base");
        assertTrue(Files.isSymbolicLink(path));
        assertTrue(Files.isDirectory(path));

        boolean javaSeen = false, javaxSeen = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path p : stream) {
                String str = p.toString();
                if (str.endsWith("/java")) {
                    javaSeen = true;
                } else if (str.endsWith("javax")) {
                    javaxSeen = true;
                }
            }
        }
        assertTrue(javaSeen);
        assertTrue(javaxSeen);
    }

    @Test
    public void invalidPathTest() {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        InvalidPathException ipe = null;
        try {
            boolean res = Files.exists(fs.getPath("/packages/\ud834\udd7b"));
            assertFalse(res);
            return;
        } catch (InvalidPathException e) {
            ipe = e;
        }
        assertTrue(ipe != null);
    }

    @DataProvider(name="packagesLinkedDirs")
    private Object[][] packagesLinkedDirs() {
        return new Object[][] {
            { "/packages/java.lang/java.base/java/lang/ref"             },
            { "/./packages/java.lang/java.base/java/lang/ref"           },
            { "packages/java.lang/java.base/java/lang/ref"              },
            { "/packages/../packages/java.lang/java.base/java/lang/ref" },
            { "/packages/java.lang/java.base/java/util/zip"             },
            { "/./packages/java.lang/java.base/java/util/zip"           },
            { "packages/java.lang/java.base/java/util/zip"              },
            { "/packages/../packages/java.lang/java.base/java/util/zip" },
        };
    }

    // @bug 8141521: jrt file system's DirectoryStream reports child paths
    // with wrong paths for directories under /packages
    @Test(dataProvider = "packagesLinkedDirs")
    public void dirStreamPackagesDirTest(String dirName) throws IOException {
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path path = fs.getPath(dirName);

        int childCount = 0, dirPrefixOkayCount = 0;
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(path)) {
            for (Path child : dirStream) {
                childCount++;
                if (child.toString().startsWith(dirName)) {
                    dirPrefixOkayCount++;
                }
            }
        }

        assertTrue(childCount != 0);
        assertEquals(dirPrefixOkayCount, childCount);
    }

    @Test
    public void objectClassSizeTest() throws Exception {
        String path = "/modules/java.base/java/lang/Object.class";
        FileSystem fs = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path classFile = fs.getPath(path);

        assertTrue(Files.size(classFile) > 0L);
    }

    // @bug 8266291: (jrtfs) Calling Files.exists may break the JRT filesystem
    @Test
    public void fileExistsCallBreaksFileSystem() throws Exception {
        Path p = FileSystems.getFileSystem(URI.create("jrt:/")).getPath("modules");
        boolean wasDirectory = Files.isDirectory(p);
        Path m = p.resolve("modules");
        Files.exists(m);
        assertTrue(wasDirectory == Files.isDirectory(p));
    }
}

