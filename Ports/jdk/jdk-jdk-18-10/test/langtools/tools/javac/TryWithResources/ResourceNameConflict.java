/*
 * @test  /nodynamiccopyright/
 * @bug 8025113
 * @author sogoel
 * @summary Resource var cannot have same name as local variable
 * @compile/fail/ref=ResourceNameConflict.out -XDrawDiagnostics ResourceNameConflict.java
 */

/**
 * Test methods and their description
 * test1() - negative test - local variable used as test resource
 * test2() - negative test - test resource already defined in an enclosing for statement
 */

public class ResourceNameConflict implements AutoCloseable {

    static final String str = "asdf";

    void test1() {
        String tr = "A resource spec var cannot have same name as local var.";
        try (ResourceNameConflict tr = new ResourceNameConflict()) {
        }
    }

    void test2(String... strArray) {
        for (String str : strArray) {
            try (ResourceNameConflict str = new ResourceNameConflict()) {
            }
        }
    }

    @Override
    public void close() {
    }
}

