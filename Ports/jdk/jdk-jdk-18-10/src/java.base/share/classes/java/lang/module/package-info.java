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

/**
 * Classes to support module descriptors and creating configurations of modules
 * by means of resolution and service binding.
 *
 * <p> Unless otherwise noted, passing a {@code null} argument to a constructor
 * or method of any class or interface in this package will cause a {@link
 * java.lang.NullPointerException NullPointerException} to be thrown. Additionally,
 * invoking a method with an array or collection containing a {@code null} element
 * will cause a {@code NullPointerException}, unless otherwise specified. </p>
 *
 *
 * <h2><a id="resolution"></a>{@index "Module Resolution"}</h2>
 *
 * <p> Resolution is the process of computing how modules depend on each other.
 * The process occurs at compile time and run time. </p>
 *
 * <p> Resolution is a two-step process. The first step recursively enumerates
 * the 'requires' directives of a set of root modules. If all the enumerated
 * modules are observable, then the second step computes their readability graph.
 * The readability graph embodies how modules depend on each other, which in
 * turn controls access across module boundaries. </p>
 *
 * <h3> Step 1: Recursive enumeration </h3>
 *
 * <p> Recursive enumeration takes a set of module names, looks up each of their
 * module declarations, and for each module declaration, recursively enumerates:
 *
 * <ul>
 *   <li> <p> the module names given by the 'requires' directives with the
 *   'transitive' modifier, and </p></li>
 *   <li> <p> at the discretion of the host system, the module names given by
 *   the 'requires' directives without the 'transitive' modifier. </p></li>
 * </ul>
 *
 * <p> Module declarations are looked up in a set of observable modules. The set
 * of observable modules is determined in an implementation specific manner. The
 * set of observable modules may include modules with explicit declarations
 * (that is, with a {@code module-info.java} source file or {@code module-info.class}
 * file) and modules with implicit declarations (that is,
 * <a href="ModuleFinder.html#automatic-modules">automatic modules</a>).
 * Because an automatic module has no explicit module declaration, it has no
 * 'requires' directives of its own, although its name may be given by a
 * 'requires' directive of an explicit module declaration. </p>
 *
 * <p> The set of root modules, whose names are the initial input to this
 * algorithm, is determined in an implementation specific manner. The set of
 * root modules may include automatic modules. </p>
 *
 * <p> If at least one automatic module is enumerated by this algorithm, then
 * every observable automatic module must be enumerated, regardless of whether
 * any of their names are given by 'requires' directives of explicit module
 * declarations. </p>
 *
 * <p> If any of the following conditions occur, then resolution fails:
 * <ul>
 *   <li><p> Any root module is not observable. </p></li>
 *   <li><p> Any module whose name is given by a 'requires' directive with the
 *   'transitive' modifier is not observable. </p></li>
 *   <li><p> At the discretion of the host system, any module whose name is given
 *   by a 'requires' directive without the 'transitive' modifier is not
 *   observable. </p></li>
 *   <li><p> The algorithm in this step enumerates the same module name twice. This
 *   indicates a cycle in the 'requires' directives, disregarding any 'transitive'
 *   modifiers. </p></li>
 * </ul>
 *
 * <p> Otherwise, resolution proceeds to step 2. </p>
 *
 * <h3> Step 2: Computing the readability graph </h3>
 *
 * <p> A 'requires' directive (irrespective of 'transitive') expresses that
 * one module depends on some other module. The effect of the 'transitive'
 * modifier is to cause additional modules to also depend on the other module.
 * If module M 'requires transitive N', then not only does M depend on N, but
 * any module that depends on M also depends on N. This allows M to be
 * refactored so that some or all of its content can be moved to a new module N
 * without breaking modules that have a 'requires M' directive. </p>
 *
 * <p> Module dependencies are represented by the readability graph. The
 * readability graph is a directed graph whose vertices are the modules
 * enumerated in step 1 and whose edges represent readability between pairs of
 * modules. The edges are specified as follows:
 *
 * <p> First, readability is determined by the 'requires' directives of the
 * enumerated modules, disregarding any 'transitive' modifiers:
 *
 * <ul>
 *   <li><p> For each enumerated module A that 'requires' B: A "reads" B. </p></li>
 *   <li><p> For each enumerated module X that is automatic: X "reads" every
 *   other enumerated module (it is "as if" an automatic module has 'requires'
 *   directives for every other enumerated module). </p></li>
 * </ul>
 *
 * <p> Second, readability is augmented to account for 'transitive' modifiers:
 * <ul>
 *   <li> <p> For each enumerated module A that "reads" B: </p>
 *     <ul>
 *     <li><p> If B 'requires transitive' C, then A "reads" C as well as B. This
 *     augmentation is recursive: since A "reads" C, if C 'requires transitive'
 *     D, then A "reads" D as well as C and B. </p></li>
 *     <li><p> If B is an automatic module, then A "reads" every other enumerated
 *     automatic module. (It is "as if" an automatic module has 'requires transitive'
 *     directives for every other enumerated automatic module).</p> </li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p> Finally, every module "reads" itself. </p>
 *
 * <p> If any of the following conditions occur in the readability graph, then
 * resolution fails:
 * <ul>
 *   <li><p> A module "reads" two or more modules with the same name. This includes
 *   the case where a module "reads" another with the same name as itself. </p></li>
 *   <li><p> Two or more modules export a package with the same name to a module
 *   that "reads" both. This includes the case where a module M containing package
 *   p "reads" another module that exports p to M. </p></li>
 *   <li><p> A module M declares that it 'uses p.S' or 'provides p.S with ...' but
 *   package p is neither in module M nor exported to M by any module that M
 *   "reads". </p></li>
 * </ul>
 * <p> Otherwise, resolution succeeds, and the result of resolution is the
 * readability graph.
 *
 * <h3><a id="root-modules"></a> Root modules </h3>
 *
 * <p> The set of root modules at compile-time is usually the set of modules
 * being compiled. At run-time, the set of root modules is usually the
 * application module specified to the 'java' launcher. When compiling code in
 * the unnamed module, or at run-time when the main application class is loaded
 * from the class path, then the default set of root modules is implementation
 * specific. In the JDK the default set of root modules contains every module
 * that is observable on the upgrade module path or among the system modules,
 * and that exports at least one package without qualification. </p>
 *
 * <h3> Observable modules </h3>
 *
 * <p> The set of observable modules at both compile-time and run-time is
 * determined by searching several different paths, and also by searching
 * the compiled modules built in to the environment. The search order is as
 * follows: </p>
 *
 * <ol>
 *   <li><p> At compile time only, the compilation module path. This path
 *   contains module definitions in source form.  </p></li>
 *
 *   <li><p> The upgrade module path. This path contains compiled definitions of
 *   modules that will be observed in preference to the compiled definitions of
 *   any <i>upgradeable modules</i> that are present in (3) and (4). See the Java
 *   SE Platform for the designation of which standard modules are upgradeable.
 *   </p></li>
 *
 *   <li><p> The system modules, which are the compiled definitions built in to
 *   the environment. </p></li>
 *
 *   <li><p> The application module path. This path contains compiled definitions
 *   of library and application modules. </p></li>
 *
 * </ol>
 *
 * <h3> 'requires' directives with 'static' modifier </h3>
 *
 * <p> 'requires' directives that have the 'static' modifier express an optional
 * dependence at run time. If a module declares that it 'requires static M' then
 * resolution does not search the observable modules for M to satisfy the dependency.
 * However, if M is recursively enumerated at step 1 then all modules that are
 * enumerated and `requires static M` will read M. </p>
 *
 * <h3> Completeness </h3>
 *
 * <p> Resolution may be partial at compile-time in that the complete transitive
 * closure may not be required to compile a set of modules. Minimally, the
 * readability graph that is constructed and validated at compile-time includes
 * the modules being compiled, their direct dependences, and all implicitly
 * declared dependences (requires transitive). </p>
 *
 * <p> At run-time, resolution is an additive process. The recursive enumeration
 * at step 1 may be relative to previous resolutions so that a root module,
 * or a module named in a 'requires' directive, is not enumerated when it was
 * enumerated by a previous (or parent) resolution. The readability graph that
 * is the result of resolution may therefore have a vertex for a module enumerated
 * in step 1 but with an edge to represent that the module reads a module that
 * was enumerated by previous (or parent) resolution. </p>
 *
 * @since 9
 */

package java.lang.module;
