/*
 * @test /nodynamiccopyright/
 * @bug 8026564
 * @summary A missing annotation type in a type variable bound
 *     should result in the same errors with and without an
 *     annotation processor.
 * @author Werner Dietl
 *
 * @modules java.compiler
 *          jdk.compiler
 * @compile DummyProcessor.java
 * @compile/fail/ref=TypeVariableMissingTA.out -XDrawDiagnostics TypeVariableMissingTA.java
 * @compile/fail/ref=TypeVariableMissingTA.out -XDrawDiagnostics -cp . -processor DummyProcessor TypeVariableMissingTA.java
 */

import java.lang.annotation.*;

class TypeVariableMissingTA<T extends @MISSING Object> {}
