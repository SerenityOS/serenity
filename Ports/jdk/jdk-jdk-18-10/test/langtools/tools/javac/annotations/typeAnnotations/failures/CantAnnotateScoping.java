/*
 * @test /nodynamiccopyright/
 * @bug 8006733 8006775
 * @summary Ensure behavior for nested types is correct.
 * @author Werner Dietl
 * @ignore 8057679 clarify error messages trying to annotate scoping
 * @compile/fail/ref=CantAnnotateScoping.out -XDrawDiagnostics CantAnnotateScoping.java
 */

import java.util.List;
import java.util.ArrayList;

import java.lang.annotation.*;

@Target({ElementType.TYPE_USE})
@interface TA {}
@Target({ElementType.TYPE_USE})
@interface TA2 {}

@Target({ElementType.FIELD})
@interface DA {}
@Target({ElementType.FIELD})
@interface DA2 {}

@Target({ElementType.TYPE_USE, ElementType.FIELD})
@interface DTA {}
@Target({ElementType.TYPE_USE, ElementType.FIELD})
@interface DTA2 {}

class Test {
    static class Outer {
        static class SInner {}
    }

    // Legal
    List<Outer. @TA SInner> li;

    // Illegal
    @TA Outer.SInner osi;
    // Illegal
    List<@TA Outer.SInner> aloi;
    // Illegal
    Object o1 = new @TA @DA @TA2 Outer.SInner();
    // Illegal
    Object o = new ArrayList<@TA @DA Outer.SInner>();

    // Illegal: @TA is only a type-use annotation
    @TA java.lang.Object f1;

    // Legal: @DA is only a declaration annotation
    @DA java.lang.Object f2;

    // Legal: @DTA is both a type-use and declaration annotation
    @DTA java.lang.Object f3;

    // Illegal: @TA and @TA2 are only type-use annotations
    @DTA @DA @TA @DA2 @TA2 java.lang.Object f4;

    // Illegal: Do we want one or two messages?
    // 1: @DA in invalid location
    // 2: Not finding class "lang"
    java. @DA lang.Object f5;

    // Illegal: Do we want one or two messages?
    // 1: @DA in invalid location
    // 2: Not finding class "XXX"
    java. @DA XXX.Object f6;

    // Illegal: Can't find class "lang".
    // Would a different error message be desirable?
    java. @TA lang.Object f7;
}
