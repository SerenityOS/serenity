/*
 * @test /nodynamiccopyright/
 * @bug 8172880
 * @summary  Wrong LineNumberTable for synthetic null checks
 * @modules jdk.jdeps/com.sun.tools.classfile
 */

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.LineNumberTable_attribute;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NullCheckLineNumberTest {

    //test data:
    static class Test {

        public Test() {
            String a = "", b = null;

            Stream.of("x")
                  .filter(a::equals)
                  .filter(b::equals)
                  .count();
        }

    }

    public static void main(String[] args) throws Exception {
        List<Entry> actualEntries = findEntries();
        List<Entry> expectedEntries = List.of(
                new SimpleEntry<>(29, 0),
                new SimpleEntry<>(30, 4),
                new SimpleEntry<>(32, 9),
                new SimpleEntry<>(33, 16),
                new SimpleEntry<>(34, 32),
                new SimpleEntry<>(35, 46),
                new SimpleEntry<>(36, 52)
        );
        if (!Objects.equals(actualEntries, expectedEntries)) {
            error(String.format("Unexpected LineNumberTable: %s", actualEntries.toString()));
        }

        try {
            new Test();
        } catch (NullPointerException npe) {
            if (Arrays.stream(npe.getStackTrace())
                      .noneMatch(se -> se.getFileName().contains("NullCheckLineNumberTest") &&
                                       se.getLineNumber() == 34)) {
                throw new AssertionError("Should go through line 34!");
            }
        }
    }

    static List<Entry> findEntries() throws IOException, ConstantPoolException {
        ClassFile self = ClassFile.read(NullCheckLineNumberTest.Test.class.getResourceAsStream("NullCheckLineNumberTest$Test.class"));
        for (Method m : self.methods) {
            if ("<init>".equals(m.getName(self.constant_pool))) {
                Code_attribute code_attribute = (Code_attribute)m.attributes.get(Attribute.Code);
                for (Attribute at : code_attribute.attributes) {
                    if (Attribute.LineNumberTable.equals(at.getName(self.constant_pool))) {
                        return Arrays.stream(((LineNumberTable_attribute)at).line_number_table)
                                     .map(e -> new SimpleEntry<> (e.line_number, e.start_pc))
                                     .collect(Collectors.toList());
                    }
                }
            }
        }
        return null;
    }

    static void error(String msg) {
        throw new AssertionError(msg);
    }

}
