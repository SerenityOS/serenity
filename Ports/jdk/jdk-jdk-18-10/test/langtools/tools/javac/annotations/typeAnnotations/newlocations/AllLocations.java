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

/*
 * @test
 * @bug 8027262
 * @summary Stress test for type annotatons
 * @compile AllLocations.java
 */

import java.util.function.Function;
import java.lang.annotation.*;
import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;
import java.io.*;
import java.lang.ref.WeakReference;

public class AllLocations {

    public class ParamStream<T> extends FileOutputStream {
        public ParamStream(File f) throws FileNotFoundException { super(f); }
    }

    public class Inner<S> {
        public Inner() {}
        public <@A T> Inner(@B Object o) {}
        public <@C T> Object g(Inner<@D S> this, Object @E [] o) {
            return new @F int @G [5];
        }
    }

    public <@H T extends @I Inner<@J ? extends @K String>> AllLocations(Object o) {}

    public @L Object @M [] @N [] arr = new @O Object @P [5] @Q [5];

    public Inner<@R ? extends @S Inner<@T ? extends @U Integer>> inner;

    public Function func(@V AllLocations this) {
        try (final ParamStream<@W Integer @X []> fs = new ParamStream<@Y Integer @Z []>(new File("testfile"))) {
            return @AA AllLocations.Inner<@AB String>::<@AC Integer>new;
        } catch(@AD Exception ex) {
            return null;
        }
    }

    public <@AE T extends @AF Inner<@AG Integer @AH []>> Function func2() {
        arr[0][0] = new @AI Inner((@AJ Object) arr[0]);
        return Ext.f((@AK Object) arr[0]) instanceof @AL Inner @AM [] @AN [] ?
            @AO @AP Ext::<@AQ @AR Integer> f :
            @AS @AT Ext::<@AU @AV Integer> f;
    }

    public Object func3(Object @AW [] arr) {
        Inner<@AX ? extends @AY Inner<@AZ ? extends @BA Integer>> loc;
        if (arr[0] instanceof @BB Inner @BC [] @BD [])
            return this.<Inner<@BE Integer @BF []>> func4();
        else
            return new <@BG Inner<@BH Integer>> @BI Inner<@BJ Inner<@BK Integer>>(null);
    }

    public <@BL T extends @BO Inner<@BP Integer @BQ []>>
    @BR Inner<@BS Inner<@BT String>> func4() {
        return (@BU Inner<@BV Inner<@BW String>>)
            new <@BX Inner<@BY Integer>> @BZ Inner<@CA Inner<@CB String>>(null) {};
    }

  { Inner<@CC ? extends @CD Inner<@CE ? extends @CF Integer>> loc =
      new @CG Inner<@CH Inner<@CI Integer>>() {};
      Ext.func(Ext.func(@CJ WeakReference::new));
      Ext.func(Ext.func(@CK Ext::<@CL Integer>f));
      Ext.func((@CM Object a) -> { @CN Object b = a; return b; });
  }

}

class Ext {
    public static <@CO T> Object f(Object o) {
        return null;
    }
    public static Function func(Function f) { return f; }
}


@Retention(RUNTIME) @Target({TYPE_USE}) @interface A { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface B { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface C { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface D { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface E { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface F { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface G { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface H { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface I { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface J { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface K { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface L { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface M { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface N { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface O { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface P { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface Q { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface R { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface S { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface T { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface U { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface V { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface W { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface X { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface Y { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface Z { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AA { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AB { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AC { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AD { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AE { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AF { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AG { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AH { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AI { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AJ { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AK { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AL { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AM { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AN { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AO { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AP { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AQ { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AR { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AS { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AT { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AU { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AV { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AW { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AX { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AY { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface AZ { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BA { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BB { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BC { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BD { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BE { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BF { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BG { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BH { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BI { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BJ { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BK { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BL { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BM { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BN { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BO { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BP { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BQ { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BR { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BS { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BT { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BU { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BV { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BW { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BX { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BY { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface BZ { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface CA { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface CB { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface CC { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface CD { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface CE { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface CF { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface CG { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface CH { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface CI { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface CJ { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface CK { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface CL { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface CM { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface CN { }
@Retention(RUNTIME) @Target({TYPE_USE}) @interface CO { }
