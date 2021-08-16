/*
 * @test /nodynamiccopyright/
 * @bug 8050993
 * @summary Verify that the condition in the conditional lexpression gets a LineNumberTable entry
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @compile -g T8050993.java
 * @run main T8050993
 */

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.tools.classfile.*;

public class T8050993 {
    public static void main(String[] args) throws IOException, ConstantPoolException {
        ClassFile someTestIn = ClassFile.read(T8050993.class.getResourceAsStream("T8050993.class"));
        Set<Integer> expectedLineNumbers = new HashSet<>(Arrays.asList(49, 50, 47, 48));
        for (Method m : someTestIn.methods) {
            if ("method".equals(m.getName(someTestIn.constant_pool))) {
                Code_attribute code_attribute = (Code_attribute) m.attributes.get(Attribute.Code);
                for (Attribute at : code_attribute.attributes) {
                    if (Attribute.LineNumberTable.equals(at.getName(someTestIn.constant_pool))) {
                        LineNumberTable_attribute att = (LineNumberTable_attribute) at;
                        Set<Integer> actualLinesNumbers = Arrays.stream(att.line_number_table)
                                                                .map(e -> e.line_number)
                                                                .collect(Collectors.toSet());
                        if (!Objects.equals(expectedLineNumbers, actualLinesNumbers)) {
                            throw new AssertionError("Expected LineNumber entries not found;" +
                                                     "actual=" + actualLinesNumbers + ";" +
                                                     "expected=" + expectedLineNumbers);
                        }
                    }
                }
            }
        }
    }

    public static int field;

    public static String method() {
        String s =
                field % 2 == 0 ?
                "true" + field :
                "false" + field;
        return s;
    }

}
