/*
 * @test /nodynamiccopyright/
 * @bug 4901265
 * @summary JSR175 (3): don't allow repeated annotations
 * @author gafter
 *
 * @compile/fail/ref=Dup.out -XDrawDiagnostics  Dup.java
 */

@Dup
@Dup
@interface Dup {}
