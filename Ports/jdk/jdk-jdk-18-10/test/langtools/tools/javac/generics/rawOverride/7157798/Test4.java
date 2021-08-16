/**
 * @test     /nodynamiccopyright/
 * @bug      7062745 7157798 7170058
 * @summary  Negative test of conflicting same-name methods inherited from multiple interfaces when parameter types not compatible
 * @compile/fail/ref=Test4.out -Werror -Xlint:unchecked -XDrawDiagnostics Test4.java
 */

import java.util.Set;
import java.util.HashSet;

interface A { void m(Set<Integer> s); }
interface B { void m(Set<String> s); }
interface C { void m(Set<?> s); }

interface AB extends A, B {} //error

interface AC extends A, C {} //error

interface D<T> { void m(Set<T> s); }

interface AD extends A, D<Integer> {} //OK

interface AD2 extends A, D<Number> {} //error

interface CD<T> extends C, D<T> {} //error

interface E { <T> void m(Set<T> s); }

interface DE<T> extends D<T>, E {} //error
