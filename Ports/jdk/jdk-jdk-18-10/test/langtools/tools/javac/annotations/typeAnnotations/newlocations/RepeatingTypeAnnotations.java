import java.lang.annotation.*;

/*
 * @test /nodynamiccopyright/
 * @bug 8006775
 * @summary repeating type annotations are possible
 * @author Werner Dietl
 * @ignore 8057683 improve ordering of errors with type annotations
 * @compile/fail/ref=RepeatingTypeAnnotations.out -XDrawDiagnostics RepeatingTypeAnnotations.java
 */

class RepeatingTypeAnnotations {
    // Fields
    @RTA @RTA Object fr1 = null;
    Object fr2 = new @RTA @RTA Object();
    // error
    Object fs = new @TA @TA Object();
    // error
    Object ft = new @TA @TA Object();
    Object fe = new @TA @TA Object();

    // Local variables
    Object foo() {
        Object o = new @RTA @RTA Object();
        o = new @TA @RTA @RTA Object();
        o = new @RTA @TA @RTA Object();
        // error
        o = new @RTA @TA @RTA @TA Object();
        // error
        return new @TA @TA Object();
    }

    // Instance creation
    Object bar() {
        Object o = new @RTA @RTA MyList<@RTA @RTA Object>();
        o = new @TA @RTA MyList<@TA @RTA Object>();
        o = new @TA @RTA @RTA MyList<@RTA @TA @RTA Object>();
        // error
        o = new @TA @TA MyList<@RTA @RTA Object>();
        // error
        o = new @RTA @RTA MyList<@TA @TA Object>();
        // error
        return new @TA @TA MyList<@RTA @RTA Object>();
    }

    // More tests
    void oneArg() {
        Object o = new @RTA @RTA Object();
        // error
        o = new @TA @TA Object();
        o = new @RTA @TA @RTA Object();

        o = new MyList<@RTA @RTA Object>();
        // error
        o = new MyList<@TA @TA Object>();
        // error
        o = new @TA @TA MyList<@TA @TA Object>();
        // error
        this.<@TA @TA String>newList();

        this.<@RTA @RTA MyList<@RTA @RTA String>>newList();
        // error
        this.<@TA @TA MyList<@TA @TA String>>newList();

        o = (@RTA @RTA MyList<@RTA @RTA Object>) o;
        // error
        o = (@TA @TA MyList<@TA @TA Object>) o;

        this.<@RTA @RTA String, @RTA @RTA Object>newMap();
        // error
        this.<@TA @TA String, @TA @TA Object>newMap();

        this.<@RTA @RTA String @RTA @RTA []>newList();
        // error
        this.<@TA @TA String @TA @TA []>newList();

        this.<@RTA @RTA String @RTA @RTA [] @RTA @RTA [], MyList<@RTA @RTA String> @RTA @RTA []>newMap();
        // error
        this.<String @TA @TA [] @TA @TA [], MyList<@TA @TA String> @TA @TA []>newMap();
    }

    static <E> void newList() { }
    static <K, V> void newMap() { }
}

class MyList<E> { }


@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface TA { }

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface TAs {
    TA[] value();
}

@Repeatable(RTAs.class)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface RTA { }

@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@interface RTAs {
    RTA[] value();
}
