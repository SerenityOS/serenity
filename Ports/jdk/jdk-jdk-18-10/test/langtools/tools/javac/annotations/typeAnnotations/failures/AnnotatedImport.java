/*
 * @test /nodynamiccopyright/
 * @bug 8006775 8027262
 * @summary Import clauses cannot use annotations.
 * @author Werner Dietl
 * @compile/fail/ref=AnnotatedImport.out -XDrawDiagnostics AnnotatedImport.java
 */

import java.lang.annotation.*;
import java.@A util.List;
import @A java.util.Map;
import java.util.@A HashMap;

class AnnotatedImport { }

@Target(ElementType.TYPE_USE)
@interface A { }
