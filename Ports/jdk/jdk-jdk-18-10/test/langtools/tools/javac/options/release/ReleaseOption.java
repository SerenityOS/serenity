/**
 * @test /nodynamiccopyright/
 * @bug 8072480
 * @summary Verify that javac rejects Java 8 program with --release 7
 * @compile ReleaseOption.java
 * @compile/fail/ref=ReleaseOption-release7.out -XDrawDiagnostics --release 7 -Xlint:-options ReleaseOption.java
 */

interface ReleaseOption extends java.util.stream.Stream {
}
