/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.tools;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;

import static javax.tools.JavaFileObject.Kind;

/**
 * File manager for tools operating on Java programming language
 * source and class files.  In this context, <em>file</em> means an
 * abstraction of regular files and other sources of data.
 *
 * <p>When constructing new JavaFileObjects, the file manager must
 * determine where to create them.  For example, if a file manager
 * manages regular files on a file system, it would most likely have a
 * current/working directory to use as default location when creating
 * or finding files.  A number of hints can be provided to a file
 * manager as to where to create files.  Any file manager might choose
 * to ignore these hints.
 *
 * <p>Some methods in this interface use class names.  Such class
 * names must be given in the Java Virtual Machine internal form of
 * fully qualified class and interface names.  For convenience '.'
 * and '/' are interchangeable.  The internal form is defined in
 * chapter four of
 * <cite>The Java Virtual Machine Specification</cite>.

 * <blockquote><p>
 *   <i>Discussion:</i> this means that the names
 *   "java/lang.package-info", "java/lang/package-info",
 *   "java.lang.package-info", are valid and equivalent.  Compare to
 *   binary name as defined in
 *   <cite>The Java Language Specification</cite>,
 *   section 13.1 "The Form of a Binary".
 * </p></blockquote>
 *
 * <p>The case of names is significant.  All names should be treated
 * as case-sensitive.  For example, some file systems have
 * case-insensitive, case-aware file names.  File objects representing
 * such files should take care to preserve case by using {@link
 * java.io.File#getCanonicalFile} or similar means.  If the system is
 * not case-aware, file objects must use other means to preserve case.
 *
 * <p><em><a id="relative_name">Relative names</a>:</em> some
 * methods in this interface use relative names.  A relative name is a
 * non-null, non-empty sequence of path segments separated by '/'.
 * '.' or '..'  are invalid path segments.  A valid relative name must
 * match the "path-rootless" rule of <a
 * href="http://www.ietf.org/rfc/rfc3986.txt">RFC&nbsp;3986</a>,
 * section&nbsp;3.3.  Informally, this should be true:
 *
 * <!-- URI.create(relativeName).normalize().getPath().equals(relativeName) -->
 * <pre>  URI.{@linkplain java.net.URI#create create}(relativeName).{@linkplain java.net.URI#normalize() normalize}().{@linkplain java.net.URI#getPath getPath}().equals(relativeName)</pre>
 *
 * <p>All methods in this interface might throw a SecurityException.
 *
 * <p>An object of this interface is not required to support
 * multi-threaded access, that is, be synchronized.  However, it must
 * support concurrent access to different file objects created by this
 * object.
 *
 * <p><em>Implementation note:</em> a consequence of this requirement
 * is that a trivial implementation of output to a {@linkplain
 * java.util.jar.JarOutputStream} is not a sufficient implementation.
 * That is, rather than creating a JavaFileObject that returns the
 * JarOutputStream directly, the contents must be cached until closed
 * and then written to the JarOutputStream.
 *
 * <p>Unless explicitly allowed, all methods in this interface might
 * throw a NullPointerException if given a {@code null} argument.
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 * @see JavaFileObject
 * @see FileObject
 * @since 1.6
 */
public interface JavaFileManager extends Closeable, Flushable, OptionChecker {

    /**
     * Interface for locations of file objects.  Used by file managers
     * to determine where to place or search for file objects.
     *
     * <p>Informally, a {@code Location} corresponds to a "search path", such as a class
     * path or module path, as used by command-line tools that use the default file system.
     *
     * <p>Some locations are typically used to identify a place in which
     * a tool can find files to be read; others are typically used to identify
     * a place where a tool can write files. If a location is used to identify
     * a place for reading files, those files may be organized in a simple
     * <em>package/class</em> hierarchy: such locations are described as
     * <strong>package-oriented</strong>.
     * Alternatively, the files may be organized in a <em>module/package/class</em>
     * hierarchy: such locations are described as <strong>module-oriented</strong>.
     * If a location is typically used to identify a place where a tool can write files,
     * it is up to the tool that writes the files to specify how those files will be
     * organized.
     *
     * <p>You can access the classes in a package-oriented location using methods like
     * {@link JavaFileManager#getJavaFileForInput} or {@link JavaFileManager#list}.
     * It is not possible to directly list the classes in a module-oriented
     * location. Instead, you can get a package-oriented location for any specific module
     * using methods like {@link JavaFileManager#getLocationForModule} or
     * {@link JavaFileManager#listLocationsForModules}.
     */
    interface Location {
        /**
         * Returns the name of this location.
         *
         * @return a name
         */
        String getName();

        /**
         * Determines if this is an output location.
         * An output location is a location that is conventionally used for
         * output.
         *
         * @apiNote An output location may be used to write files in either
         * a package-oriented organization or in a module-oriented organization.
         *
         * @return true if this is an output location, false otherwise
         */
        boolean isOutputLocation();

        /**
         * Indicates if this location is module-oriented location, and therefore
         * expected to contain classes in a <em>module/package/class</em>
         * hierarchy, as compared to a package-oriented location, which
         * is expected to contain classes in a <em>package/class</em> hierarchy.
         * The result of this method is undefined if this is an output
         * location.
         *
         * @implNote This implementation returns true if the name includes
         * the word "MODULE".
         *
         * @return true if this location is expected to contain modules
         * @since 9
         */
        default boolean isModuleOrientedLocation() {
            return getName().matches("\\bMODULE\\b");
        }
    }

    /**
     * Returns a class loader for loading plug-ins from the given
     * package-oriented location.
     * For example, to load annotation processors,
     * a compiler will request a class loader for the {@link
     * StandardLocation#ANNOTATION_PROCESSOR_PATH
     * ANNOTATION_PROCESSOR_PATH} location.
     *
     * @param location a location
     * @return a class loader for the given location; or {@code null}
     * if loading plug-ins from the given location is disabled or if
     * the location is not known
     * @throws SecurityException if a class loader can not be created
     * in the current security context
     * @throws IllegalStateException if {@link #close} has been called
     * and this file manager cannot be reopened
     * @throws IllegalArgumentException if the location is a module-oriented location
     */
    ClassLoader getClassLoader(Location location);

    /**
     * Lists all file objects matching the given criteria in the given
     * package-oriented location.
     * List file objects in "subpackages" if recurse is true.
     *
     * <p>Note: even if the given location is unknown to this file
     * manager, it may not return {@code null}.  Also, an unknown
     * location may not cause an exception.
     *
     * @param location     a location
     * @param packageName  a package name
     * @param kinds        return objects only of these kinds
     * @param recurse      if true include "subpackages"
     * @return an Iterable of file objects matching the given criteria
     * @throws IOException if an I/O error occurred, or if {@link
     * #close} has been called and this file manager cannot be
     * reopened
     * @throws IllegalArgumentException if the location is a module-oriented location
     * @throws IllegalStateException if {@link #close} has been called
     * and this file manager cannot be reopened
     */
    Iterable<JavaFileObject> list(Location location,
                                  String packageName,
                                  Set<Kind> kinds,
                                  boolean recurse)
        throws IOException;

    /**
     * Infers a binary name of a file object based on a package-oriented location.
     * The binary name returned might not be a valid binary name according to
     * <cite>The Java Language Specification</cite>.
     *
     * @param location a location
     * @param file a file object
     * @return a binary name or {@code null} the file object is not
     * found in the given location
     * @throws IllegalArgumentException if the location is a module-oriented location
     * @throws IllegalStateException if {@link #close} has been called
     * and this file manager cannot be reopened
     */
    String inferBinaryName(Location location, JavaFileObject file);

    /**
     * Compares two file objects and return true if they represent the
     * same underlying object.
     *
     * @param a a file object
     * @param b a file object
     * @return true if the given file objects represent the same
     * underlying object
     *
     * @throws IllegalArgumentException if either of the arguments
     * were created with another file manager and this file manager
     * does not support foreign file objects
     */
    boolean isSameFile(FileObject a, FileObject b);

    /**
     * Handles one option.  If {@code current} is an option to this
     * file manager it will consume any arguments to that option from
     * {@code remaining} and return true, otherwise return false.
     *
     * @param current current option
     * @param remaining remaining options
     * @return true if this option was handled by this file manager,
     * false otherwise
     * @throws IllegalArgumentException if this option to this file
     * manager is used incorrectly
     * @throws IllegalStateException if {@link #close} has been called
     * and this file manager cannot be reopened
     */
    boolean handleOption(String current, Iterator<String> remaining);

    /**
     * Determines if a location is known to this file manager.
     *
     * @param location a location
     * @return true if the location is known
     */
    boolean hasLocation(Location location);

    /**
     * Returns a {@linkplain JavaFileObject file object} for input
     * representing the specified class of the specified kind in the
     * given package-oriented location.
     *
     * @param location a location
     * @param className the name of a class
     * @param kind the kind of file, must be one of {@link
     * JavaFileObject.Kind#SOURCE SOURCE} or {@link
     * JavaFileObject.Kind#CLASS CLASS}
     * @return a file object, might return {@code null} if the
     * file does not exist
     * @throws IllegalArgumentException if the location is not known
     * to this file manager and the file manager does not support
     * unknown locations, or if the kind is not valid, or if the
     * location is a module-oriented location
     * @throws IOException if an I/O error occurred, or if {@link
     * #close} has been called and this file manager cannot be
     * reopened
     * @throws IllegalStateException if {@link #close} has been called
     * and this file manager cannot be reopened
     */
    JavaFileObject getJavaFileForInput(Location location,
                                       String className,
                                       Kind kind)
        throws IOException;

    /**
     * Returns a {@linkplain JavaFileObject file object} for output
     * representing the specified class of the specified kind in the
     * given package-oriented location.
     *
     * <p>Optionally, this file manager might consider the sibling as
     * a hint for where to place the output.  The exact semantics of
     * this hint is unspecified.  The JDK compiler, javac, for
     * example, will place class files in the same directories as
     * originating source files unless a class file output directory
     * is provided.  To facilitate this behavior, javac might provide
     * the originating source file as sibling when calling this
     * method.
     *
     * @param location a package-oriented location
     * @param className the name of a class
     * @param kind the kind of file, must be one of {@link
     * JavaFileObject.Kind#SOURCE SOURCE} or {@link
     * JavaFileObject.Kind#CLASS CLASS}
     * @param sibling a file object to be used as hint for placement;
     * might be {@code null}
     * @return a file object for output
     * @throws IllegalArgumentException if sibling is not known to
     * this file manager, or if the location is not known to this file
     * manager and the file manager does not support unknown
     * locations, or if the kind is not valid, or if the location is
     * not an output location
     * @throws IOException if an I/O error occurred, or if {@link
     * #close} has been called and this file manager cannot be
     * reopened
     * @throws IllegalStateException {@link #close} has been called
     * and this file manager cannot be reopened
     */
    JavaFileObject getJavaFileForOutput(Location location,
                                        String className,
                                        Kind kind,
                                        FileObject sibling)
        throws IOException;

    /**
     * Returns a {@linkplain FileObject file object} for input
     * representing the specified <a href="JavaFileManager.html#relative_name">relative
     * name</a> in the specified package in the given package-oriented location.
     *
     * <p>If the returned object represents a {@linkplain
     * JavaFileObject.Kind#SOURCE source} or {@linkplain
     * JavaFileObject.Kind#CLASS class} file, it must be an instance
     * of {@link JavaFileObject}.
     *
     * <p>Informally, the file object returned by this method is
     * located in the concatenation of the location, package name, and
     * relative name.  For example, to locate the properties file
     * "resources/compiler.properties" in the package
     * "com.sun.tools.javac" in the {@linkplain
     * StandardLocation#SOURCE_PATH SOURCE_PATH} location, this method
     * might be called like so:
     *
     * <pre>getFileForInput(SOURCE_PATH, "com.sun.tools.javac", "resources/compiler.properties");</pre>
     *
     * <p>If the call was executed on Windows, with SOURCE_PATH set to
     * <code>"C:\Documents&nbsp;and&nbsp;Settings\UncleBob\src\share\classes"</code>,
     * a valid result would be a file object representing the file
     * <code>"C:\Documents&nbsp;and&nbsp;Settings\UncleBob\src\share\classes\com\sun\tools\javac\resources\compiler.properties"</code>.
     *
     * @param location a package-oriented location
     * @param packageName a package name
     * @param relativeName a relative name
     * @return a file object, might return {@code null} if the file
     * does not exist
     * @throws IllegalArgumentException if the location is not known
     * to this file manager and the file manager does not support
     * unknown locations, or if {@code relativeName} is not valid,
     * or if the location is a module-oriented location
     * @throws IOException if an I/O error occurred, or if {@link
     * #close} has been called and this file manager cannot be
     * reopened
     * @throws IllegalStateException if {@link #close} has been called
     * and this file manager cannot be reopened
     */
    FileObject getFileForInput(Location location,
                               String packageName,
                               String relativeName)
        throws IOException;

    /**
     * Returns a {@linkplain FileObject file object} for output
     * representing the specified <a href="JavaFileManager.html#relative_name">relative
     * name</a> in the specified package in the given location.
     *
     * <p>Optionally, this file manager might consider the sibling as
     * a hint for where to place the output.  The exact semantics of
     * this hint is unspecified.  The JDK compiler, javac, for
     * example, will place class files in the same directories as
     * originating source files unless a class file output directory
     * is provided.  To facilitate this behavior, javac might provide
     * the originating source file as sibling when calling this
     * method.
     *
     * <p>If the returned object represents a {@linkplain
     * JavaFileObject.Kind#SOURCE source} or {@linkplain
     * JavaFileObject.Kind#CLASS class} file, it must be an instance
     * of {@link JavaFileObject}.
     *
     * <p>Informally, the file object returned by this method is
     * located in the concatenation of the location, package name, and
     * relative name or next to the sibling argument.  See {@link
     * #getFileForInput getFileForInput} for an example.
     *
     * @param location an output location
     * @param packageName a package name
     * @param relativeName a relative name
     * @param sibling a file object to be used as hint for placement;
     * might be {@code null}
     * @return a file object
     * @throws IllegalArgumentException if sibling is not known to
     * this file manager, or if the location is not known to this file
     * manager and the file manager does not support unknown
     * locations, or if {@code relativeName} is not valid,
     * or if the location is not an output location
     * @throws IOException if an I/O error occurred, or if {@link
     * #close} has been called and this file manager cannot be
     * reopened
     * @throws IllegalStateException if {@link #close} has been called
     * and this file manager cannot be reopened
     */
    FileObject getFileForOutput(Location location,
                                String packageName,
                                String relativeName,
                                FileObject sibling)
        throws IOException;

    /**
     * Flushes any resources opened for output by this file manager
     * directly or indirectly.  Flushing a closed file manager has no
     * effect.
     *
     * @throws IOException if an I/O error occurred
     * @see #close
     */
    @Override
    void flush() throws IOException;

    /**
     * Releases any resources opened by this file manager directly or
     * indirectly.  This might render this file manager useless and
     * the effect of subsequent calls to methods on this object or any
     * objects obtained through this object is undefined unless
     * explicitly allowed.  However, closing a file manager which has
     * already been closed has no effect.
     *
     * @throws IOException if an I/O error occurred
     * @see #flush
     */
    @Override
    void close() throws IOException;

    /**
     * Gets a location for a named module within a location, which may be either
     * a module-oriented location or an output location.
     * The result will be an output location if the given location is
     * an output location, or it will be a package-oriented location.
     *
     * @implSpec This implementation throws {@code UnsupportedOperationException}.
     *
     * @param location the module-oriented location
     * @param moduleName the name of the module to be found
     * @return the location for the named module
     *
     * @throws IOException if an I/O error occurred
     * @throws UnsupportedOperationException if this operation if not supported by this file manager
     * @throws IllegalArgumentException if the location is neither an output location nor a
     * module-oriented location
     * @since 9
     */ // TODO: describe failure modes
    default Location getLocationForModule(Location location, String moduleName) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets a location for the module containing a specific file
     * to be found within a location, which may be either
     * a module-oriented location or an output location.
     * The result will be an output location if the given location is
     * an output location, or it will be a package-oriented location.
     *
     * @implSpec This implementation throws {@code UnsupportedOperationException}.
     *
     * @param location the module-oriented location
     * @param fo the file
     * @return the module containing the file
     *
     * @throws IOException if an I/O error occurred
     * @throws UnsupportedOperationException if this operation if not supported by this file manager
     * @throws IllegalArgumentException if the location is neither an output location nor a
     * module-oriented location
     * @since 9
     */
    default Location getLocationForModule(Location location, JavaFileObject fo) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Get a service loader for a specific service class from a given location.
     *
     * If the location is a module-oriented location, the service loader will use the
     * service declarations in the modules found in that location. Otherwise, a service loader
     * is created using the package-oriented location, in which case, the services are
     * determined using the provider-configuration files in {@code META-INF/services}.
     *
     * @implSpec This implementation throws {@code UnsupportedOperationException}.
     *
     * @param location the module-oriented location
     * @param service  the {@code Class} object of the service class
     * @param <S> the service class
     * @return a service loader for the given service class
     *
     * @throws IOException if an I/O error occurred
     * @throws UnsupportedOperationException if this operation if not supported by this file manager
     * @since 9
     */ // TODO: describe failure modes
    default <S> ServiceLoader<S> getServiceLoader(Location location, Class<S> service) throws  IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Infer the name of the module from its location, as returned by
     * {@code getLocationForModule} or {@code listModuleLocations}.
     *
     * @implSpec This implementation throws {@code UnsupportedOperationException}.
     *
     * @param location a package-oriented location representing a module
     * @return the name of the module
     *
     * @throws IOException if an I/O error occurred
     * @throws UnsupportedOperationException if this operation if not supported by this file manager
     * @throws IllegalArgumentException if the location is not one known to this file manager
     * @since 9
     */ // TODO: describe failure modes
    default String inferModuleName(Location location) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Lists the locations for all the modules in a module-oriented location or an output location.
     * The locations that are returned will be output locations if the given location is an output,
     * or it will be a package-oriented locations.
     *
     * @implSpec This implementation throws {@code UnsupportedOperationException}.
     *
     * @param location  the module-oriented location for which to list the modules
     * @return  a series of sets of locations containing modules
     *
     * @throws IOException if an I/O error occurred
     * @throws UnsupportedOperationException if this operation if not supported by this file manager
     * @throws IllegalArgumentException if the location is not a module-oriented location
     * @since 9
     */ // TODO: describe failure modes
    default Iterable<Set<Location>> listLocationsForModules(Location location) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Determines whether or not a given file object is "contained in" a specified location.
     *
     * <p>For a package-oriented location, a file object is contained in the location if there exist
     * values for <i>packageName</i> and <i>relativeName</i> such that either of the following
     * calls would return the {@link #isSameFile same} file object:
     * <pre>
     *     getFileForInput(location, <i>packageName</i>, <i>relativeName</i>)
     *     getFileForOutput(location, <i>packageName</i>, <i>relativeName</i>, null)
     * </pre>
     *
     * <p>For a module-oriented location, a file object is contained in the location if there exists
     * a module that may be obtained by the call:
     * <pre>
     *     getLocationForModule(location, <i>moduleName</i>)
     * </pre>
     * such that the file object is contained in the (package-oriented) location for that module.
     *
     * @implSpec This implementation throws {@code UnsupportedOperationException}.
     *
     * @param location the location
     * @param fo the file object
     * @return whether or not the file is contained in the location
     *
     * @throws IOException if there is a problem determining the result
     * @throws UnsupportedOperationException if the method is not supported
     *
     * @since 9
     */

    default boolean contains(Location location, FileObject fo) throws IOException {
        throw new UnsupportedOperationException();
    }

}
