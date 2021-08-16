/**
 * @test     /nodynamiccopyright/
 * @bug      7062745 7157798
 * @summary  Negative test of conflicting same-name methods inherited from multiple interfaces when return type not compatible
 * @compile/fail/ref=Test3.out -Werror -Xlint:unchecked -XDrawDiagnostics Test3.java
 */

import java.util.List;
import java.io.Serializable;

interface A { int m(); }
interface B { Integer m(); }

interface AB extends A, B {} //error

interface C { List<Integer> m(); }
interface D { List<Number> m(); }

interface CD extends C, D {} //error

interface E<T> { T m(); }
interface F<T> { T m(); }
interface G { Serializable m(); }

interface BE extends B, E<Number> {} //ok, covariant return

interface BE2<T> extends B, E<T> {} //error

interface EF<T> extends E<T>, F<T> {} //ok

interface EF2<U, V extends U> extends E<U>, F<V> {} //ok, covariant return

interface EF3<U, V> extends E<U>, F<V> {} //error

interface EG<T extends Number> extends E<T>, G {} //ok

interface EFG<U extends Serializable, V extends Serializable> extends E<U>, F<V>, G {} //error
