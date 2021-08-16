/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.bar;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Provides;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.HashSet;
import java.util.Set;

import jdk.internal.module.ModuleHashes;
import jdk.internal.module.ModuleReferenceImpl;
import jdk.test.bar.internal.Message;

public class Bar {
    public static void main(String[] args) throws Exception {
        System.out.println("message:" + Message.get());

        ModuleDescriptor md = Bar.class.getModule().getDescriptor();
        System.out.println("nameAndVersion:" + md.toNameAndVersion());
        md.mainClass().ifPresent(mc -> System.out.println("mainClass:" + mc));

        StringJoiner sj = new StringJoiner(",");
        md.requires().stream().map(ModuleDescriptor.Requires::name).sorted().forEach(sj::add);
        System.out.println("requires:" + sj.toString());

        sj = new StringJoiner(",");
        md.exports().stream().map(ModuleDescriptor.Exports::source).sorted().forEach(sj::add);
        if (!sj.toString().equals(""))
            System.out.println("exports:" + sj.toString());

        sj = new StringJoiner(",");
        md.uses().stream().sorted().forEach(sj::add);
        if (!sj.toString().equals(""))
            System.out.println("uses:" + sj.toString());

        sj = new StringJoiner(",");
        md.provides().stream().map(Provides::service).sorted().forEach(sj::add);
        if (!sj.toString().equals(""))
            System.out.println("provides:" + sj.toString());

        sj = new StringJoiner(",");
        Set<String> concealed = new HashSet<>(md.packages());
        md.exports().stream().map(Exports::source).forEach(concealed::remove);
        concealed.forEach(sj::add);
        if (!sj.toString().equals(""))
            System.out.println("contains:" + sj.toString());


        Module foo = jdk.test.foo.Foo.class.getModule();
        Optional<ResolvedModule> om = foo.getLayer().configuration().findModule(foo.getName());
        assert om.isPresent();
        ModuleReference mref = om.get().reference();
        assert mref instanceof ModuleReferenceImpl;
        ModuleHashes hashes = ((ModuleReferenceImpl) mref).recordedHashes();
        assert hashes != null;
        System.out.println("hashes:" + hashes.hashFor("bar"));
    }
}
