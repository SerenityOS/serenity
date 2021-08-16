/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Exercise initial transformation (class file loader hook)
 *  with CDS/AppCDS with Interface/Implementor pair
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 *     /test/hotspot/jtreg/runtime/cds/appcds/jvmti /test/hotspot/jtreg/runtime/cds/serviceability
 *     /test/hotspot/jtreg/runtime/cds/serviceability/transformRelatedClasses
 *     /test/hotspot/jtreg/runtime/cds /test/hotspot/jtreg/testlibrary/jvmti
 *     /test/hotspot/jtreg/runtime/cds/appcds/customLoader
 *     /test/hotspot/jtreg/runtime/cds/appcds/customLoader/test-classes
 * @requires vm.cds
 * @requires vm.jvmti
 * @requires !vm.graal.enabled
 * @build TransformUtil TransformerAgent Interface Implementor
 * @run main/othervm TransformRelatedClassesAppCDS Interface Implementor
 */
