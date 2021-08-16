/*
 * @test /nodynamiccopyright/
 * @summary Class literals are not type uses and cannot be annotated
 * @author Werner Dietl
 * @compile/fail/ref=DotClass.out -XDrawDiagnostics DotClass.java
 */

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

@Target({TYPE_USE, TYPE_PARAMETER, TYPE})
@Retention(RetentionPolicy.RUNTIME)
@interface A {}

@interface B { int value(); }

class T0x1E {
    void m0x1E() {
        Class<Object> c = @A Object.class;
    }

    Class<?> c = @A String.class;

    Class<? extends @A String> as = @A String.class;
}

class ClassLiterals {
    public static void main(String[] args) {
        if (String.class != @A String.class) throw new Error();
        if (@A int.class != int.class) throw new Error();
        if (@A int.class != Integer.TYPE) throw new Error();
        if (@A int @B(0) [].class != int[].class) throw new Error();

        if (String[].class != @A String[].class) throw new Error();
        if (String[].class != String @A [].class) throw new Error();
        if (@A int[].class != int[].class) throw new Error();
        if (@A int @B(0) [].class != int[].class) throw new Error();
    }

    Object classLit1 = @A String @C [] @B(0) [].class;
    Object classLit2 = @A String @C []       [].class;
    Object classLit3 = @A String    [] @B(0) [].class;
    Object classLit4 =    String    [] @B(0) [].class;
    Object classLit5 =    String @C []       [].class;
    Object classLit6 =    String    []       [].class;
}
