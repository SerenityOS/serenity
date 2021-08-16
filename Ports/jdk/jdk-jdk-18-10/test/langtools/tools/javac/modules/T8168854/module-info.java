/*
 * @test
 * @bug 8168854
 * @summary javac erroneously reject a a service interface inner class in a provides clause
 * @compile module-info.java
 */
module mod {
    exports pack1;
    provides pack1.Outer.Inter with pack1.Outer1.Implem;
}
