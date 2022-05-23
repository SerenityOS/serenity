#include <AK/Error.h>
#include <AK/Utf8View.h>
#include <LibJVM/Instructions.h>
#include <LibJVM/JVM.h>
#include <LibJVM/Class.h>
#include <LibJVM/ConstantPool.h>
#include <LibJVM/Thread.h>
#include <LibJVM/Value.h>

namespace JVM {

//The current design requires that method increment their own PCs (including the one that represents the opcode).

ErrorOr<void> nop(JVM jvm, Thread thread) {
    thread.inc_pc(1);
}
ErrorOr<void> aconst_null(JVM jvm, Thread thread) {
    thread.push_operand(Value(Type::Null));
    thread.inc_pc(1);
}
ErrorOr<void> iconst_m1(JVM jvm, Thread thread) {
    thread.push_operand(Value((int) -1));
    thread.inc_pc(1);
}
ErrorOr<void> iconst_0(JVM jvm, Thread thread) {
    thread.push_operand(Value((int) 0));
    thread.inc_pc(1);
}
ErrorOr<void> iconst_1(JVM jvm, Thread thread) {
    thread.push_operand(Value((int) 1));
    thread.inc_pc(1);
}
ErrorOr<void> iconst_2(JVM jvm, Thread thread) {
    thread.push_operand(Value((int) 2));
    thread.inc_pc(1);
}
ErrorOr<void> iconst_3(JVM jvm, Thread thread) {
    thread.push_operand(Value((int) 3));
    thread.inc_pc(1);
}
ErrorOr<void> iconst_4(JVM jvm, Thread thread) {
    thread.push_operand(Value((int) 4));
    thread.inc_pc(1);
}
ErrorOr<void> iconst_5(JVM jvm, Thread thread) {
    thread.push_operand(Value((int) 5));
    thread.inc_pc(1);
}
ErrorOr<void> lconst_0(JVM jvm, Thread thread) {
    thread.push_operand(Value((long) 0));
    thread.inc_pc(1);
}
ErrorOr<void> lconst_1(JVM jvm, Thread thread) {
    thread.push_operand(Value((long) 1));
    thread.inc_pc(1);
}
ErrorOr<void> fconst_0(JVM jvm, Thread thread) {
    thread.push_operand(Value((float) 0.0));
    thread.inc_pc(1);
}
ErrorOr<void> fconst_1(JVM jvm, Thread thread) {
    thread.push_operand(Value((float) 1.0));
    thread.inc_pc(1);
}
ErrorOr<void> fconst_2(JVM jvm, Thread thread) {
    thread.push_operand(Value((float) 2.0));
    thread.inc_pc(1);
}
ErrorOr<void> dconst_0(JVM jvm, Thread thread) {
    thread.push_operand(Value((double) 0.0));
    thread.inc_pc(1);
}
ErrorOr<void> dconst_1(JVM jvm, Thread thread) {
    thread.push_operand(Value((double) 1.0));
    thread.inc_pc(1);
}
ErrorOr<void> bipush(JVM jvm, Thread thread) {
    u8 byte = thread.current_frame().current_method[thread.pc() + 1]; //offset the opcode
    thread.push_operand(Value(byte));
    thread.inc_pc(2);
}
ErrorOr<void> sipush(JVM jvm, Thread thread) {
    short byte_upper = thread.current_frame().current_method[thread.pc() + 1];
    short byte_lower = thread.current_frame().current_method[thread.pc() + 2];
    short s = (byte_upper << 8) | byte_lower;
    thread.push_operand(Value(s));
    thread.inc_pc(3);
}
ErrorOr<void> ldc(JVM jvm, Thread thread) {
     u8 index = thread.current_frame().current_method[thread.pc() + 1];
     CPEntry entry = (*thread.current_frame().rt_const_pool.m_ptr).cp_entry((short) index);
     switch (entry.kind()) {
        case ConstantKind::Integer:
         thread.push_operand(Value(entry.as_int_info()));
         break;
        case ConstantKind::Float:
         thread.push_operand(Value(entry.as_float_info()));
         break;
        case ConstantKind::String:
         //The spec says that we should return a reference to an instance of class string
         break;
        case ConstantKind::Class:
         short index = entry.as_class_info();
         CPEntry entry = (*thread.current_frame().rt_const_pool.m_ptr).cp_entry(index);
         VERIFY(entry.kind() == ConstantKind::Utf8);
         Utf8Info utf8 = entry.as_utf8_info();
         VERIFY(utf8.bytes[0] == 'L');
         StringView name = StringView(utf8.bytes + 1, utf8.length); //offset the beginning 'L'.
         int c = jvm.resolve_class_reference(name);
         thread.push_operand(Value(c, Type::Class));

         break;
     }
}

}
