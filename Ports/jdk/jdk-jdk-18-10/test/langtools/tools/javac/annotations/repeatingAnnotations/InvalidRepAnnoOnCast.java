/**
 * @test    /nodynamiccopyright/
 * @bug     8044196
 * @summary Make sure repeatable annotations can't be erroneously applied to a cast type
 * @compile/fail/ref=InvalidRepAnnoOnCast.out -XDrawDiagnostics InvalidRepAnnoOnCast.java
 */

import java.lang.annotation.*;

class InvalidRepAnnoOnCast {

    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    @Repeatable(TC.class)
    @interface T { int value(); }

    @Target(ElementType.TYPE_PARAMETER)
    @interface TC { T[] value(); }

    String s = (@T(1) @T(2) String) new Object();
}
