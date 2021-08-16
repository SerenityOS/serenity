/*
 * @test /nodynamiccopyright/
 * @bug 8157519
 * @summary Error messages when compiling a malformed module-info.java confusing
 * @compile/fail/ref=moduleinfo.out -XDrawDiagnostics module-info.java
 */

module java.transaction {
  requires java.base;
  resuires javax.interceptor.javax.interceptor.api;
  requires transitive javax.enterprise.cdi.api;
  requires transitive java.sql;
  requires transitive java.rmi;
  export javax.transaction;
}
