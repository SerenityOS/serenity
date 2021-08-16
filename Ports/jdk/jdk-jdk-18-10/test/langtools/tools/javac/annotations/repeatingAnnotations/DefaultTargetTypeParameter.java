import java.lang.annotation.*;

/**
 * @test /nodynamiccopyright/
 * @bug 8006547
 * @compile/fail/ref=DefaultTargetTypeParameter.out -XDrawDiagnostics DefaultTargetTypeParameter.java
 */

@Target({
    ElementType.TYPE_PARAMETER,
})
@interface Container {
  DefaultTargetTypeParameter[] value();
}

@Repeatable(Container.class)
public @interface DefaultTargetTypeParameter {}
