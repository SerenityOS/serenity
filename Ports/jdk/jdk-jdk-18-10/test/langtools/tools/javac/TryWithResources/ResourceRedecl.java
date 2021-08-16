/*
 * @test    /nodynamiccopyright/
 * @bug     8025113
 * @author  sogoel
 * @summary Redeclaration of resource variables
 * @compile/fail/ref=ResourceRedecl.out -XDrawDiagnostics ResourceRedecl.java
 */

import java.io.*;

public class ResourceRedecl {

    public void test() {
        // compiler error if name of an exception param is redeclared within the Block of the catch clause as a local var;
        // or as an exception param of a catch clause in a try statement;
        // or as a resource in a try-with-resources statement
        try {
        } catch (Exception exParam1) {
            Object exParam1 = new Object();
            try (java.io.FileInputStream exParam1 = new java.io.FileInputStream("foo.txt")) {
                Object exParam1 = new Object();
            } catch (IOException exParam1) {
            }
        }

        // compiler error if resource is redeclared within the try Block as a local var
        // or as an exception param of a catch clause in a try statement
        try (java.io.FileInputStream exParam2 = new java.io.FileInputStream("bar.txt")) {
            Object exParam2 = new Object();
            try (BufferedReader br = new BufferedReader(new FileReader("zee.txt"))) {
            } catch (IOException exParam2) {
            }
        } catch (Exception ex) {
        }
    }
}

