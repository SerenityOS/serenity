/**
 * @test    /nodynamiccopyright/
 * @bug     8044196
 * @summary Ensure that containers with target FIELD can't be applied to type parameters.
 * @compile/fail/ref=InvalidClsTypeParamTarget.out -XDrawDiagnostics InvalidClsTypeParamTarget.java
 */

import java.lang.annotation.*;

class InvalidClsTypeParamTarget {

    @Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE, ElementType.FIELD})
    @Repeatable(TC.class)
    @interface T { int value(); }

    @Target(ElementType.FIELD)
    @interface TC { T[] value(); }

    class Test<@T(1) @T(2) N> {
    }
}
