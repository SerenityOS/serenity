/**
 * @test /nodynamiccopyright/
 * @bug 8029017
 * @summary sanity testing of ElementType validation for repeating annotations
 * @compile/fail/ref=TypeUseTargetNeg.out -XDrawDiagnostics TypeUseTargetNeg.java
 */

import java.lang.annotation.*;

public class TypeUseTargetNeg {}

// Case 1:
@Target({
    ElementType.TYPE_USE,
})
@Repeatable(FooContainer.class)
@interface Foo {}

@Target({
    ElementType.ANNOTATION_TYPE,
    ElementType.TYPE,
    ElementType.TYPE_USE,
    ElementType.TYPE_PARAMETER,
    ElementType.FIELD,

})
@interface FooContainer {
  Foo[] value();
}


// Case 2:
@Target({
    ElementType.TYPE_USE,
})
@Repeatable(BarContainer.class)
@interface Bar {}

@Target({
    ElementType.ANNOTATION_TYPE,
    ElementType.TYPE,
    ElementType.TYPE_USE,
    ElementType.METHOD,
})
@interface BarContainer {
  Bar[] value();
}


// Case 3:
@Target({
    ElementType.TYPE_USE,
})
@Repeatable(BazContainer.class)
@interface Baz {}

@Target({
    ElementType.ANNOTATION_TYPE,
    ElementType.TYPE,
    ElementType.PARAMETER,
})
@interface BazContainer {
  Baz[] value();
}


// Case 4:
@Target({
    ElementType.TYPE_USE,
})
@Repeatable(QuxContainer.class)
@interface Qux {}

@interface QuxContainer {
  Qux[] value();
}


// Case 5:
@Target({})
@Repeatable(QuuxContainer.class)
@interface Quux {}

@Target({
    ElementType.TYPE_PARAMETER,
})
@interface QuuxContainer {
  Quux[] value();
}

// Case 6:
@Repeatable(QuuuxContainer.class)
@interface Quuux {}

@Target({
    ElementType.TYPE_USE,
})
@interface QuuuxContainer {
  Quuux[] value();
}
