/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 8246774
 * @summary Basic tests for prohibited magic serialPersistentFields
 * @library /test/lib
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @run testng SerialPersistentFieldsTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Type;
import jdk.test.lib.ByteCodeLoader;
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static jdk.internal.org.objectweb.asm.ClassWriter.*;
import static jdk.internal.org.objectweb.asm.Opcodes.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Checks that the serialPersistentFields declaration is effectively ignored.
 */
public class SerialPersistentFieldsTest {

    ClassLoader serializableRecordLoader;

    /**
     * Generates the serializable record classes used by the test. First creates
     * the initial bytecode for the record class using javac, then adds the
     * prohibited serialization magic field. Effectively, for example:
     *
     *   record R () implements Serializable {
     *       private static final ObjectStreamField[] serialPersistentFields = {
     *           new ObjectStreamField("s", String.class),
     *           new ObjectStreamField("i", int.class),
     *           new ObjectStreamField("l", long.class),
     *       };
     *   }
     */
    @BeforeTest
    public void setup() {
        {  // R1
            byte[] byteCode = InMemoryJavaCompiler.compile("R1",
                    "public record R1 () implements java.io.Serializable { }");
            ObjectStreamField[] serialPersistentFields = {
                    new ObjectStreamField("s", String.class),
                    new ObjectStreamField("i", int.class),
                    new ObjectStreamField("l", long.class),
                    new ObjectStreamField("d", double.class)
            };
            byteCode = addSerialPersistentFields(byteCode, serialPersistentFields);
            serializableRecordLoader = new ByteCodeLoader("R1", byteCode, SerialPersistentFieldsTest.class.getClassLoader());
        }
        {  // R2
            byte[] byteCode = InMemoryJavaCompiler.compile("R2",
                    "public record R2 (int x) implements java.io.Serializable { }");
            ObjectStreamField[] serialPersistentFields = {
                    new ObjectStreamField("s", String.class)
            };
            byteCode = addSerialPersistentFields(byteCode, serialPersistentFields);
            serializableRecordLoader = new ByteCodeLoader("R2", byteCode, serializableRecordLoader);
        }
        {  // R3
            byte[] byteCode = InMemoryJavaCompiler.compile("R3",
                    "public record R3 (int x, int y) implements java.io.Serializable { }");
            ObjectStreamField[] serialPersistentFields = new ObjectStreamField[0];
            byteCode = addSerialPersistentFields(byteCode, serialPersistentFields);
            serializableRecordLoader = new ByteCodeLoader("R3", byteCode, serializableRecordLoader);
        }
        {  // R4
            byte[] byteCode = InMemoryJavaCompiler.compile("R4",
                    "import java.io.Serializable;" +
                    "public record R4<U extends Serializable,V extends Serializable>(U u, V v) implements Serializable { }");
            ObjectStreamField[] serialPersistentFields = {
                    new ObjectStreamField("v", String.class)
            };
            byteCode = addSerialPersistentFields(byteCode, serialPersistentFields);
            serializableRecordLoader = new ByteCodeLoader("R4", byteCode, serializableRecordLoader);
        }
        {  // R5  -- Externalizable
            byte[] byteCode = InMemoryJavaCompiler.compile("R5",
                    "import java.io.*;" +
                    "public record R5 (int x) implements Externalizable {" +
                    "    @Override public void writeExternal(ObjectOutput out) {\n" +
                    "        throw new AssertionError(\"should not reach here\");\n" +
                    "    }\n" +
                    "    @Override public void readExternal(ObjectInput in) {\n" +
                    "        throw new AssertionError(\"should not reach here\");\n" +
                    "    }  }");
            ObjectStreamField[] serialPersistentFields = {
                    new ObjectStreamField("v", String.class)
            };
            byteCode = addSerialPersistentFields(byteCode, serialPersistentFields);
            serializableRecordLoader = new ByteCodeLoader("R5", byteCode, serializableRecordLoader);
        }
    }

    /** Constructs a new instance of given named record, with the given args. */
    Object newRecord(String name, Class<?>[] pTypes, Object[] args) {
        try {
            Class<?> c = Class.forName(name, true, serializableRecordLoader);
            assert c.isRecord();
            assert c.getRecordComponents() != null;
            return c.getConstructor(pTypes).newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    Object newR1() {
        return newRecord("R1", null, null);
    }
    Object newR2(int x) {
        return newRecord("R2", new Class[]{int.class}, new Object[]{x});
    }
    Object newR3(int x, int y) {
        return newRecord("R3", new Class[]{int.class, int.class}, new Object[]{x, y});
    }
    Object newR4(Serializable u, Serializable v) {
        return newRecord("R4", new Class[]{Serializable.class, Serializable.class}, new Object[]{u,v});
    }
    Object newR5(int x) {
        return newRecord("R5", new Class[]{int.class}, new Object[]{x});
    }

    @DataProvider(name = "recordInstances")
    public Object[][] recordInstances() {
        return new Object[][] {
            new Object[] { newR1()                                },
            new Object[] { newR2(5)                               },
            new Object[] { newR3(7, 8)                            },
            new Object[] { newR4("str", BigDecimal.valueOf(4567)) },
            new Object[] { newR5(9)                               },
        };
    }

    @Test(dataProvider = "recordInstances")
    public void roundTrip(Object objToSerialize) throws Exception {
        out.println("\n---");
        out.println("serializing : " + objToSerialize);
        var objDeserialized = serializeDeserialize(objToSerialize);
        out.println("deserialized: " + objDeserialized);
        assertEquals(objToSerialize, objDeserialized);
        assertEquals(objDeserialized, objToSerialize);
    }

    <T> byte[] serialize(T obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    <T> T deserialize(byte[] streamBytes)
        throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(streamBytes);
        ObjectInputStream ois  = new ObjectInputStream(bais) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc)
                    throws ClassNotFoundException {
                return Class.forName(desc.getName(), false, serializableRecordLoader);
            }
        };
        return (T) ois.readObject();
    }

    <T> T serializeDeserialize(T obj)
        throws IOException, ClassNotFoundException
    {
        return deserialize(serialize(obj));
    }

    // -- machinery for augmenting a record class with prohibited serial field --

    static byte[] addSerialPersistentFields(byte[] classBytes,
                                            ObjectStreamField[] spf) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(reader, COMPUTE_MAXS | COMPUTE_FRAMES);
        reader.accept(new SerialPersistentFieldsVisitor(writer, spf), 0);
        return writer.toByteArray();
    }

    /** A visitor that adds a serialPersistentFields field, and assigns it in clinit. */
    static final class SerialPersistentFieldsVisitor extends ClassVisitor {
        static final String FIELD_NAME = "serialPersistentFields";
        static final String FIELD_DESC = "[Ljava/io/ObjectStreamField;";
        final ObjectStreamField[] spf;
        String className;
        SerialPersistentFieldsVisitor(ClassVisitor cv, ObjectStreamField[] spf) {
            super(ASM8, cv);
            this.spf = spf;
        }
        @Override
        public void visit(final int version,
                          final int access,
                          final String name,
                          final String signature,
                          final String superName,
                          final String[] interfaces) {
            this.className = name;
            cv.visit(version, access, name, signature, superName, interfaces);
        }
        @Override
        public FieldVisitor visitField(final int access,
                                       final String name,
                                       final String descriptor,
                                       final String signature,
                                       final Object value) {
            // the field-to-be-added should not already exist
            assert !name.equals("serialPersistentFields") : "Unexpected " + name + " field";
            return cv.visitField(access, name, descriptor, signature, value);
        }
        @Override
        public void visitEnd() {
            {
                FieldVisitor fv = cv.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                                                FIELD_NAME,
                                                FIELD_DESC,
                                                null,
                                                null);
                fv.visitEnd();
            }
            {
                MethodVisitor mv = cv.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
                mv.visitCode();
                mv.visitIntInsn(BIPUSH, spf.length);
                mv.visitTypeInsn(ANEWARRAY, "java/io/ObjectStreamField");

                for (int i = 0; i < spf.length; i++) {
                    ObjectStreamField osf = spf[i];
                    mv.visitInsn(DUP);
                    mv.visitIntInsn(BIPUSH, i);
                    mv.visitTypeInsn(NEW, "java/io/ObjectStreamField");
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(osf.getName());
                    if (osf.getType().isPrimitive()) {
                        mv.visitFieldInsn(GETSTATIC,  getPrimitiveBoxClass(osf.getType()), "TYPE", "Ljava/lang/Class;");
                    } else {
                        mv.visitLdcInsn(Type.getType(osf.getType()));
                    }
                    mv.visitMethodInsn(INVOKESPECIAL, "java/io/ObjectStreamField", "<init>", "(Ljava/lang/String;Ljava/lang/Class;)V", false);
                    mv.visitInsn(AASTORE);
                }

                mv.visitFieldInsn(PUTSTATIC, className, "serialPersistentFields", "[Ljava/io/ObjectStreamField;");
                mv.visitInsn(RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            cv.visitEnd();
        }

        static String getPrimitiveBoxClass(final Class<?> clazz) {
            if (!clazz.isPrimitive())
                throw new AssertionError("unexpected non-primitive:" + clazz);

            if (clazz == Integer.TYPE) {
                return "java/lang/Integer";
            } else if (clazz == Boolean.TYPE) {
                return "java/lang/Boolean";
            } else if (clazz == Byte.TYPE) {
                return "java/lang/Byte";
            } else if (clazz == Character.TYPE) {
                return "java/lang/Character";
            } else if (clazz == Short.TYPE) {
                return "java/lang/Short";
            } else if (clazz == Double.TYPE) {
                return "java/lang/Double";
            } else if (clazz == Float.TYPE) {
                return "java/lang/Float";
            } else if (clazz == Long.TYPE) {
                return "java/lang/Long";
            } else {
                throw new AssertionError("unknown:" + clazz);
            }
        }
    }

    // -- infra sanity --

    /** Checks to ensure correct operation of the test's generation logic. */
    @Test(dataProvider = "recordInstances")
    public void wellFormedGeneratedClasses(Object obj) throws Exception {
        out.println("\n---");
        out.println(obj);
        Field f = obj.getClass().getDeclaredField("serialPersistentFields");
        assertTrue((f.getModifiers() & Modifier.PRIVATE) != 0);
        assertTrue((f.getModifiers() & Modifier.STATIC) != 0);
        assertTrue((f.getModifiers() & Modifier.FINAL) != 0);
        f.setAccessible(true);
        ObjectStreamField[] fv = (ObjectStreamField[])f.get(obj);
        assertTrue(fv != null, "Unexpected null value");
        assertTrue(fv.length >= 0, "Unexpected negative length:" + fv.length);
    }
}
