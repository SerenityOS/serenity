/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.Set;

/**
 * Provides the implementation of the Zip file system provider.
 * The Zip file system provider treats the contents of a Zip or JAR file as a file system.
 *
 * <h2>Accessing a Zip File System</h2>
 *
 * The {@linkplain java.nio.file.FileSystems FileSystems} {@code newFileSystem}
 * static factory methods can be used to:
 * <ul>
 *   <li>Create a Zip file system</li>
 *   <li>Open an existing file as a Zip file system</li>
 * </ul>
 *
 * The Zip file system provider does not support opening an existing Zip file
 * that contains entries with "." or ".." in its name elements.
 *
 * <h2>URI Scheme Used to Identify the Zip File System</h2>
 *
 * The URI {@link java.net.URI#getScheme scheme} that identifies the ZIP file system is {@code jar}.
 *
 * <h2>POSIX file attributes</h2>
 *
 * A Zip file system supports a file attribute {@link FileAttributeView view}
 * named "{@code zip}" that defines the following file attribute:
 *
 * <blockquote>
 * <table class="striped">
 * <caption style="display:none">Supported attributes</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Name</th>
 *     <th scope="col">Type</th>
 *   </tr>
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row">permissions</th>
 *     <td>{@link Set}&lt;{@link PosixFilePermission}&gt;</td>
 *   </tr>
 * </tbody>
 * </table>
 * </blockquote>
 *
 * The "permissions" attribute is the set of access permissions that are optionally
 * stored for entries in a Zip file. The value of the attribute is {@code null}
 * for entries that do not have access permissions. Zip file systems do not
 * enforce access permissions.
 *
 * <p> The "permissions" attribute may be read and set using the
 * {@linkplain Files#getAttribute(Path, String, LinkOption...) Files.getAttribute} and
 * {@linkplain Files#setAttribute(Path, String, Object, LinkOption...) Files.setAttribute}
 * methods. The following example uses these methods to read and set the attribute:
 * <pre> {@code
 *     Set<PosixFilePermission> perms = Files.getAttribute(entry, "zip:permissions");
 *     if (perms == null) {
 *         perms = PosixFilePermissions.fromString("rw-rw-rw-");
 *         Files.setAttribute(entry, "zip:permissions", perms);
 *     }
 * } </pre>
 *
 * <p> In addition to the "{@code zip}" view, a Zip file system optionally supports
 * the {@link PosixFileAttributeView} ("{@code posix}").
 * This view extends the "{@code basic}" view with type safe access to the
 * {@link PosixFileAttributes#owner() owner}, {@link PosixFileAttributes#group() group-owner},
 * and {@link PosixFileAttributes#permissions() permissions} attributes. The
 * "{@code posix}" view is only supported when the Zip file system is created with
 * the provider property "{@code enablePosixFileAttributes}" set to "{@code true}".
 * The following creates a file system with this property and reads the access
 * permissions of a file:
 * <pre> {@code
 *     var env = Map.of("enablePosixFileAttributes", "true");
 *     try (FileSystem fs = FileSystems.newFileSystem(file, env) {
 *         Path entry = fs.getPath("entry");
 *         Set<PosixFilePermission> perms = Files.getPosixFilePermissions(entry);
 *     }
 * } </pre>
 *
 * <p> The file owner and group owner attributes are not persisted, meaning they are
 * not stored in the zip file. The "{@code defaultOwner}" and "{@code defaultGroup}"
 * provider properties (listed below) can be used to configure the default values
 * for these attributes. If these properties are not set then the file owner
 * defaults to the owner of the zip file, and the group owner defaults to the
 * zip file's group owner (or the file owner on platforms that don't support a
 * group owner).
 *
 * <p> The "{@code permissions}" attribute is not optional in the "{@code posix}"
 * view so a default set of permissions are used for entries that do not have
 * access permissions stored in the Zip file. The default set of permissions
 * are
 * <ul>
 *   <li>{@link PosixFilePermission#OWNER_READ OWNER_READ}</li>
 *   <li>{@link PosixFilePermission#OWNER_WRITE OWNER_WRITE}</li>
 *   <li>{@link PosixFilePermission#GROUP_READ GROUP_READ}</li>
 * </ul>
 * The default permissions can be configured with the "{@code defaultPermissions}"
 * property described below.
 *
 * <h2>Zip File System Properties</h2>
 *
 * The following properties may be specified when creating a Zip
 * file system:
 * <table class="striped">
 * <caption style="display:none">
 *     Configurable properties that may be specified when creating
 *     a new Zip file system
 * </caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Property Name</th>
 *     <th scope="col">Data Type</th>
 *     <th scope="col">Default Value</th>
 *     <th scope="col">Description</th>
 *   </tr>
 * </thead>
 *
 * <tbody>
 * <tr>
 *   <th scope="row">create</th>
 *   <td>{@link java.lang.String} or {@link java.lang.Boolean}</td>
 *   <td>false</td>
 *   <td>
 *       If the value is {@code true}, the Zip file system provider
 *       creates a new Zip or JAR file if it does not exist.
 *   </td>
 * </tr>
 * <tr>
 *   <th scope="row">encoding</th>
 *   <td>{@link java.lang.String}</td>
 *   <td>UTF-8</td>
 *   <td>
 *       The value indicates the encoding scheme for the
 *       names of the entries in the Zip or JAR file.
 *   </td>
 * </tr>
 * <tr>
 *   <th scope="row">enablePosixFileAttributes</th>
 *   <td>{@link java.lang.String} or {@link java.lang.Boolean}</td>
 *   <td>false</td>
 *   <td>
 *       If the value is {@code true}, the Zip file system will support
 *       the {@link java.nio.file.attribute.PosixFileAttributeView PosixFileAttributeView}.
 *   </td>
 * </tr>
 * <tr>
 *   <th scope="row">defaultOwner</th>
 *   <td>{@link java.nio.file.attribute.UserPrincipal UserPrincipal}<br> or
 *   {@link java.lang.String}</td>
 *   <td>null/unset</td>
 *   <td>
 *       Override the default owner for entries in the Zip file system.<br>
 *       The value can be a UserPrincipal or a String value that is used as the UserPrincipal's name.
 *   </td>
 * </tr>
 * <tr>
 *   <th scope="row">defaultGroup</th>
 *   <td>{@link java.nio.file.attribute.GroupPrincipal GroupPrincipal}<br> or
 *   {@link java.lang.String}</td>
 *   <td>null/unset</td>
 *   <td>
 *       Override the the default group for entries in the Zip file system.<br>
 *       The value can be a GroupPrincipal or a String value that is used as the GroupPrincipal's name.
 *   </td>
 * </tr>
 * <tr>
 *   <th scope="row">defaultPermissions</th>
 *   <td>{@link java.util.Set Set}&lt;{@link java.nio.file.attribute.PosixFilePermission PosixFilePermission}&gt;<br>
 *       or {@link java.lang.String}</td>
 *   <td>null/unset</td>
 *   <td>
 *       Override the default Set of permissions for entries in the Zip file system.<br>
 *       The value can be a {@link java.util.Set Set}&lt;{@link java.nio.file.attribute.PosixFilePermission PosixFilePermission}&gt; or<br>
 *       a String that is parsed by {@link java.nio.file.attribute.PosixFilePermissions#fromString PosixFilePermissions::fromString}
 *   </td>
 * </tr>
 * <tr>
 *   <th scope="row">compressionMethod</th>
 *   <td>{@link java.lang.String}</td>
 *   <td>"DEFLATED"</td>
 *   <td>
 *       The value representing the compression method to use when writing entries
 *       to the Zip file system.
 *       <ul>
 *           <li>
 *               If the value is {@code "STORED"}, the Zip file system provider will
 *               not compress entries when writing to the Zip file system.
 *           </li>
 *           <li>
 *               If the value is {@code "DEFLATED"} or the property is not set,
 *               the Zip file system provider will use data compression when
 *               writing entries to the Zip file system.
 *           </li>
 *           <li>
 *               If the value is not {@code "STORED"} or {@code "DEFLATED"}, an
 *               {@code IllegalArgumentException} will be thrown when the Zip
 *               filesystem is created.
 *           </li>
 *       </ul>
 *   </td>
 * </tr>
 * <tr>
 *   <th scope="row">releaseVersion</th>
 *   <td>{@link java.lang.String} or {@link java.lang.Integer}</td>
 *   <td>null/unset</td>
 *   <td>
 *       A value representing the version entry to use when accessing a
 *       <a href="{@docRoot}/../specs/jar/jar.html#multi-release-jar-files">
 *       multi-release JAR</a>. If the JAR is not a
 *       <a href="{@docRoot}/../specs/jar/jar.html#multi-release-jar-files">
 *       multi-release JAR</a>, the value will be ignored and the JAR will be
 *       considered un-versioned.
 *       <p>
 *       The value must be either the string "runtime" or represent a valid
 *       {@linkplain Runtime.Version Java SE Platform version number},
 *       such as {@code 9} or {@code 14}, in order to determine the version entry.
 *
 *       <ul>
 *           <li>
 *               If the value is {@code null} or the property is not set,
 *               then the JAR will be treated as an un-versioned JAR.
 *           </li>
 *           <li>
 *               If the value is {@code "runtime"}, the
 *               version entry will be determined by invoking
 *               {@linkplain Runtime.Version#feature() Runtime.Version.feature()}.
 *           </li>
 *           <li>
 *               If the value does not represent a valid
 *               {@linkplain Runtime.Version Java SE Platform version number},
 *               an {@code IllegalArgumentException} will be thrown.
 *           </li>
 *       </ul>
 *   </td>
 * </tr>
 *  </tbody>
 * </table>
 *
 * <h2>Examples:</h2>
 *
 * Construct a new Zip file system that is identified by a URI.  If the Zip file does not exist,
 * it will be created:
 * <pre>
 * {@code
 *
 *     URI uri = URI.create("jar:file:/home/luckydog/tennisTeam.zip");
 *     Map<String, String> env = Map.of("create", "true");
 *     FileSystem zipfs = FileSystems.newFileSystem(uri, env);
 * }
 * </pre>
 *
 * Construct a new Zip file system that is identified by specifying a path
 * and using automatic file type detection. Iterate from the root of the JAR displaying each
 * found entry:
 * <pre>
 * {@code
 *
 *     FileSystem zipfs = FileSystems.newFileSystem(Path.of("helloworld.jar"));
 *     Path rootDir = zipfs.getPath("/");
 *     Files.walk(rootDir)
 *            .forEach(System.out::println);
 * }
 * </pre>
 * @provides java.nio.file.spi.FileSystemProvider
 * @moduleGraph
 * @since 9
 */
module jdk.zipfs {
    provides java.nio.file.spi.FileSystemProvider with
        jdk.nio.zipfs.ZipFileSystemProvider;
}
