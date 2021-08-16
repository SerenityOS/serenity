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

package javax.annotation.processing;

import java.util.Map;
import java.util.Locale;
import javax.lang.model.SourceVersion;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * An annotation processing tool framework will {@linkplain
 * Processor#init provide an annotation processor with an object
 * implementing this interface} so the processor can use facilities
 * provided by the framework to write new files, report error
 * messages, and find other utilities.
 *
 * <p>Third parties may wish to provide value-add wrappers around the
 * facility objects from this interface, for example a {@code Filer}
 * extension that allows multiple processors to coordinate writing out
 * a single source file.  To enable this, for processors running in a
 * context where their side effects via the API could be visible to
 * each other, the tool infrastructure must provide corresponding
 * facility objects that are {@code .equals}, {@code Filer}s that are
 * {@code .equals}, and so on.  In addition, the tool invocation must
 * be able to be configured such that from the perspective of the
 * running annotation processors, at least the chosen subset of helper
 * classes are viewed as being loaded by the same class loader.
 * (Since the facility objects manage shared state, the implementation
 * of a wrapper class must know whether or not the same base facility
 * object has been wrapped before.)
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @since 1.6
 */
public interface ProcessingEnvironment {
    /**
     * {@return the processor-specific options passed to the annotation
     * processing tool}  Options are returned in the form of a map from
     * option name to option value.  For an option with no value, the
     * corresponding value in the map is {@code null}.
     *
     * <p>See documentation of the particular tool infrastructure
     * being used for details on how to pass in processor-specific
     * options.  For example, a command-line implementation may
     * distinguish processor-specific options by prefixing them with a
     * known string like {@code "-A"}; other tool implementations may
     * follow different conventions or provide alternative mechanisms.
     * A given implementation may also provide implementation-specific
     * ways of finding options passed to the tool in addition to the
     * processor-specific options.
     */
    Map<String,String> getOptions();

    /**
     * {@return the messager used to report errors, warnings, and other
     * notices}
     */
    Messager getMessager();

    /**
     * {@return the filer used to create new source, class, or auxiliary
     * files}
     */
    Filer getFiler();

    /**
     * {@return an implementation of some utility methods for
     * operating on elements}
     */
    Elements getElementUtils();

    /**
     * {@return an implementation of some utility methods for
     * operating on types}
     */
    Types getTypeUtils();

    /**
     * {@return the source version that any generated {@linkplain
     * Filer#createSourceFile source} and {@linkplain
     * Filer#createClassFile class} files should conform to}
     *
     * @see Processor#getSupportedSourceVersion
     */
    SourceVersion getSourceVersion();

    /**
     * {@return the current locale or {@code null} if no locale is in
     * effect}  The locale can be be used to provide localized
     * {@linkplain Messager messages}.
     */
    Locale getLocale();

    /**
     * Returns {@code true} if <em>preview features</em> are enabled
     * and {@code false} otherwise.
     * @return whether or not preview features are enabled
     *
     * @implSpec The default implementation of this method returns
     * {@code false}.
     *
     * @since 13
     */
    default boolean isPreviewEnabled() {
        return false;
    }
}
