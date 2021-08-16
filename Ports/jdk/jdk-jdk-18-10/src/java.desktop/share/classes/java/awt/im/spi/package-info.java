/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Provides interfaces that enable the development of input methods that can be
 * used with any Java runtime environment. Input methods are software components
 * that let the user enter text in ways other than simple typing on a keyboard.
 * They are commonly used to enter Japanese, Chinese, or Korean - languages
 * using thousands of different characters - on keyboards with far fewer keys.
 * However, this package also allows the development of input methods for other
 * languages and the use of entirely different input mechanisms, such as
 * handwriting recognition.
 *
 * <h2><a id="Packaging"></a>Packaging Input Methods</h2>
 * Input methods can be made available by adding them to the application's class
 * path. The main JAR file of an input method must contain the file:
 * <pre>
 *     META-INF/services/java.awt.im.spi.InputMethodDescriptor
 * </pre>
 * The file should contain a list of fully-qualified class names, one per line,
 * of classes implementing the {@code java.awt.im.spi.InputMethodDescriptor}
 * interface. Space and tab characters surrounding each name, as well as blank
 * lines, are ignored. The comment character is {@code '#'}
 * ({@code \u005Cu0023}); on each line all characters following the first
 * comment character are ignored. The file must be encoded in UTF-8.
 * <p>
 * For example, if the fully-qualified name of the class that implements
 * {@code java.awt.im.spi.InputMethodDesciptor} for the <em>Foo</em> input
 * method is {@code com.sun.ime.FooInputMethodDescriptor}, the file
 * {@code META-INF/services/java.awt.im.spi.InputMethodDescriptor}
 * contains a line:
 * <pre>
 *     com.sun.ime.FooInputMethodDescriptor
 * </pre>
 * The input method must also provide at least two classes: one class
 * implementing the {@code java.awt.im.spi.InputMethodDescriptor} interface, one
 * class implementing the {@code java.awt.im.spi.InputMethod} interface. The
 * input method should separate the implementations for these interfaces, so
 * that loading of the class implementing {@code InputMethod} can be deferred
 * until actually needed.
 *
 * <h2><a id="Loading"></a>Loading Input Methods</h2>
 * The input method framework will usually defer loading of input  method
 * classes until they are absolutely needed. It loads only the
 * {@code InputMethodDescriptor} implementations during AWT initialization. It
 * loads an {@code InputMethod} implementation when the input method has been
 * selected.
 *
 * <h2><a id="PeeredComponents"></a>Java Input Methods and Peered Text
 * Components</h2>
 * The Java input method framework intends to support all combinations of input
 * methods (host input methods and Java input methods) and components (peered
 * and lightweight). However, because of limitations in the underlying platform,
 * it may not always be possible to enable the communication between Java input
 * methods and peered AWT components. Support for this specific combination is
 * therefore platform dependent. In Sun's Java SE Runtime Environments, this
 * combination is supported on Windows, but not on Solaris.
 *
 * <h2>Related Documentation</h2>
 * For overviews, tutorials, examples, guides, and tool documentation, please
 * see {@extLink imf_overview Input Method Framework Overview}.
 *
 * @since 1.3
 */
package java.awt.im.spi;
