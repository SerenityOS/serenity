package compiler.c2.aarch64;

import java.lang.reflect.Field;
import jdk.internal.misc.Unsafe;

class TestUnsafeVolatileLoad
{
    public volatile int f_int = 0;
    public volatile Integer f_obj = Integer.valueOf(0);

    public static Unsafe unsafe = Unsafe.getUnsafe();
    public static Field f_int_field;
    public static Field f_obj_field;
    public static long f_int_off;
    public static long f_obj_off;

    static {
        try {
            f_int_field = TestUnsafeVolatileLoad.class.getField("f_int");
            f_obj_field = TestUnsafeVolatileLoad.class.getField("f_obj");
            f_int_off = unsafe.objectFieldOffset(f_int_field);
            f_obj_off = unsafe.objectFieldOffset(f_obj_field);
        } catch (Exception e) {
            System.out.println("reflection failed " + e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        final TestUnsafeVolatileLoad t = new TestUnsafeVolatileLoad();
        for (int i = 0; i < 100_000; i++) {
            t.f_int = i;
            int r = t.testInt();
            if (r != i) {
                throw new RuntimeException("bad result!");
            }
        }
        for (int i = 0; i < 100_000; i++) {
            t.f_obj = Integer.valueOf(i);
            int r = t.testObj();
            if (r != i) {
                throw new RuntimeException("bad result!");
            }
        }
    }
    public int testInt()
    {
        return unsafe.getIntVolatile(this, f_int_off);
    }

    public int testObj()
    {
        return ((Integer)unsafe.getReferenceVolatile(this, f_obj_off));
    }
}
