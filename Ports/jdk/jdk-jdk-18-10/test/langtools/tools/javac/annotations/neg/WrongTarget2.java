/*
 * @test /nodynamiccopyright/
 * @bug 4973504
 * @summary Compiler allows Inherited meta-attribute on local variable declaration.
 * @author gafter
 *
 * @compile/fail/ref=WrongTarget2.out -XDrawDiagnostics  WrongTarget2.java
 */

import java.lang.annotation.Inherited;
class Field {{
     @Inherited int vec;
}}
