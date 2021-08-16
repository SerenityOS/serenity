/**
 * @test    /nodynamiccopyright/
 * @bug     8044196
 * @summary Ensure that containers with target METHOD can't be applied to type parameters.
 * @compile/fail/ref=InvalidMethodTypeParamTarget.out -XDrawDiagnostics InvalidMethodTypeParamTarget.java
 */

import java.lang.annotation.*;

class InvalidMethodTypeParamTarget {

    @Target({ElementType.TYPE_PARAMETER, ElementType.METHOD})
    @Repeatable(TC.class)
    @interface T { int value(); }

    @Target(ElementType.METHOD)
    @interface TC { T[] value(); }

    public <@T(1) @T(2) N> void method() { }
}
