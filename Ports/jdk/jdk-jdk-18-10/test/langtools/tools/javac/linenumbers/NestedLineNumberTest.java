/*
 * @test /nodynamiccopyright/
 * @bug 8061778
 * @summary  Wrong LineNumberTable for default constructors
 * @modules jdk.jdeps/com.sun.tools.classfile
 */

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.LineNumberTable_attribute;
import com.sun.tools.classfile.LineNumberTable_attribute.Entry;

import java.io.IOException;

public class NestedLineNumberTest {

    public static void main(String[] args) throws Exception {
        Entry[] lines = findEntries();
        if (lines == null || lines.length != 1) {
            int found = lines == null ? 0 : lines.length;
            error(String.format("LineNumberTable contains wrong number of entries - expected %d, found %d", 1, found));
        }

        int line = lines[0].line_number;
        if (line != 54) {
            error(String.format("LineNumberTable contains wrong line number - expected %d, found %d", 54, line));
        }
    }

    static Entry[] findEntries() throws IOException, ConstantPoolException {
        ClassFile self = ClassFile.read(NestedLineNumberTest.Test.class.getResourceAsStream("NestedLineNumberTest$Test.class"));
        for (Method m : self.methods) {
            if ("<init>".equals(m.getName(self.constant_pool))) {
                Code_attribute code_attribute = (Code_attribute)m.attributes.get(Attribute.Code);
                for (Attribute at : code_attribute.attributes) {
                    if (Attribute.LineNumberTable.equals(at.getName(self.constant_pool))) {
                        return ((LineNumberTable_attribute)at).line_number_table;
                    }
                }
            }
        }
        return null;
    }

    static void error(String msg) {
        throw new AssertionError(msg);
    }

    // The default constructor in this class should get only one LineNumberTable entry,
    // pointing to the first line of the declaration of class Test.
    static class Test {
        static class Empty { }
    }
}
