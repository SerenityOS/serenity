/*
 * @test  /nodynamiccopyright/
 * @bug 8171528
 * @summary Crash in Annotate with duplicate package-info declarations
 * @compile/fail/ref=DuplicatedAnnotatedPackagesTest.out -XDrawDiagnostics pkg1/package-info.java pkg2/package-info.java
 */
