/*
 * @test /nodynamiccopyright/
 * @bug 8138822
 * @summary test that only Java 8+ allows repeating annotations
 * @compile WrongVersion.java
 * @compile -Xlint:-options -source 8 WrongVersion.java
 * @compile/fail/ref=WrongVersion7.out -XDrawDiagnostics -Xlint:-options -source 7 WrongVersion.java
 */
import java.lang.annotation.Repeatable;

@Ann(1) @Ann(2)
class C {
}

@Repeatable(AnnContainer.class)
@interface Ann {
    int value();
}

@interface AnnContainer {
    Ann[] value();
}
