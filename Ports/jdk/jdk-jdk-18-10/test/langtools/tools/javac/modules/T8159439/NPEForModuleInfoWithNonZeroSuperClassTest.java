/*
 * @test /nodynamiccopyright/
 * @bug 8159439
 * @summary javac throws NPE with Module attribute and super_class != 0
 * @build module-info
 * @compile/fail/ref=NPEForModuleInfoWithNonZeroSuperClassTest.out -XDrawDiagnostics NPEForModuleInfoWithNonZeroSuperClassTest.java
 */

class NPEForModuleInfoWithNonZeroSuperClassTest {}
