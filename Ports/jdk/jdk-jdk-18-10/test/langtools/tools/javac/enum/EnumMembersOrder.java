/**
 * @test    /nodynamiccopyright/
 * @bug     8025537 5028491
 * @author  sogoel
 * @summary enum constants should precede other enum members
 * @compile/fail/ref=EnumMembersOrder.out -XDrawDiagnostics EnumMembersOrder.java
 */

enum Days {

    Days(String d) { day = d; } // constructor

    // enum constants
    WEEKEND("SAT"),
    WEEKDAY("MON");

    private String day;

}

