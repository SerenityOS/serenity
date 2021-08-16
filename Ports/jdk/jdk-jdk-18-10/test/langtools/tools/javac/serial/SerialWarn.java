/*
 * @test /nodynamiccopyright/
 * @bug 4854628
 * @summary include Throwable subclasses in missing serialVersionUID warning
 * @author gafter
 *
 * @compile                    -Werror SerialWarn.java
 * @compile/fail/ref=SerialWarn.out -XDrawDiagnostics -Xlint:serial -Werror SerialWarn.java
 */

class SerialWarn extends Throwable {}
