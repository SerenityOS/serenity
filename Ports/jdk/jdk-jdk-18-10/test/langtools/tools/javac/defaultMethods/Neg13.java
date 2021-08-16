/* @test /nodynamiccopyright/
 * @bug 7192246
 * @summary check that default method overriding object members are flagged as error
 * @compile/fail/ref=Neg13.out -XDrawDiagnostics Neg13.java
 */
interface Neg13 {
    default protected Object clone() { return null; } //protected not allowed here
    default boolean equals(Object obj) { return false; }
    default protected void finalize() { } //protected not allowed here
    default Class<?> getClass() { return null; }
    default int hashCode() { return 0; }
    default void notify() { }
    default void notifyAll() { }
    default String toString() { return null; }
    default void wait() { }
    default void wait(long timeout) { }
    default void wait(long timeout, int nanos) { }
}
