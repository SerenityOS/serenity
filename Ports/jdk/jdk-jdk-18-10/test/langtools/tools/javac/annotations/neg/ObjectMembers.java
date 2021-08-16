/*
 * @test /nodynamiccopyright/
 * @bug 4901264
 * @summary JSR175 (2): don't allow annotating members from Object
 * @author gafter
 *
 * @compile/fail/ref=ObjectMembers.out -XDrawDiagnostics  ObjectMembers.java
 */

@ObjectMembers(hashCode = 23)
@interface ObjectMembers {}
