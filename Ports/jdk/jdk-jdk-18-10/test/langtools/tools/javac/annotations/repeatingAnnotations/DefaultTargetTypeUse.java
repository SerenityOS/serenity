import java.lang.annotation.*;

/**
 * @test /nodynamiccopyright/
 * @bug 8006547
 * @compile/fail/ref=DefaultTargetTypeUse.out -XDrawDiagnostics DefaultTargetTypeUse.java
 */

@Target({
    ElementType.TYPE_USE,
})
@interface Container {
  DefaultTargetTypeUse[] value();
}

@Repeatable(Container.class)
public @interface DefaultTargetTypeUse {}
