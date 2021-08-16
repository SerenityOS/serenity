/*
 * @test /nodynamiccopyright/
 * @bug 5013488
 * @summary Compiler AssertionError: com.sun.tools.javac.util.Bits.incl(Bits.java:73)
 *
 * @compile/fail/ref=UseEnum.out -XDrawDiagnostics UseEnum.java
 */

import static java.lang.System.out;

class UseEnum {
    enum Animal {cat, dog, bird, fish};
    public static void main(String args[]) {
        Animal pet;

        pet = Animal.cat;
        out.println(pet);

        for (Animal beast : beast.values())
            out.println(pet);
    }
}
