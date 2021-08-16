/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8058112
 * @summary Invalid BootstrapMethod for constructor/method reference
 */

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class MethodReferenceIntersection1 {

    public static void main(String[] args) {
        MethodReferenceIntersection1 main = new MethodReferenceIntersection1();
        List<Info_MRI1> list = main.toInfoListError(Arrays.asList(new Base_MRI1()));
        System.out.printf("result %d\n", list.size());
    }

    public <H extends B_MRI1 & A_MRI1> List<Info_MRI1> toInfoListError(List<H> list) {
        Comparator<B_MRI1> byNameComparator =
                    (B_MRI1 b1, B_MRI1 b2) -> b1.getB().compareToIgnoreCase(b2.getB());
        return list.stream().sorted(byNameComparator).map(Info_MRI1::new).collect(toList());
    }

    public <H extends B_MRI1 & A_MRI1> List<Info_MRI1> toInfoListWorks(List<H> list) {
        Comparator<B_MRI1> byNameComparator =
                    (B_MRI1 b1, B_MRI1 b2) -> b1.getB().compareToIgnoreCase(b2.getB());
        return list.stream().sorted(byNameComparator).map(s -> new Info_MRI1(s)).collect(toList());
    }
}

interface B_MRI1 {
    public String getB();
}

interface A_MRI1 {
    public long getA();
}

class Info_MRI1 {
    private final long a;
    private final String b;

    <H extends A_MRI1 & B_MRI1> Info_MRI1(H h) {
        a = h.getA();
        b = h.getB();
    }
}

class Base_MRI1 implements A_MRI1, B_MRI1 {

    @Override
    public long getA() {
        return 7L;
    }

    @Override
    public String getB() {
        return "hello";
    }
}
