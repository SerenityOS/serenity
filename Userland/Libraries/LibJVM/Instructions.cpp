#include <AK/Error.h>
#include <LibJVM/Instructions.h>
#include <LibJVM/JVM.h>
#include <LibJVM/Thread.h>
#include <LibJVM/Value.h>

namespace JVM {

ErrorOr<void> nop(JVM jvm, Thread thread) {
    thread.inc_pc(1);
}
ErrorOr<void> aconst_null(JVM jvm, Thread thread) {
    thread.current_frame().op_stack.push(Value(Type::Null));
    thread.inc_pc(1);
}
ErrorOr<void> iconst_m1(JVM jvm, Thread thread) {
    thread.current_frame().op_stack.push(Value((int) -1));
    thread.inc_pc(1);
}
ErrorOr<void> iconst_0(JVM jvm, Thread thread) {
    thread.current_frame().op_stack.push(Value((int) 0));
    thread.inc_pc(1);
}
ErrorOr<void> iconst_1(JVM jvm, Thread thread) {
    thread.current_frame().op_stack.push(Value((int) 1));
    thread.inc_pc(1);
}
ErrorOr<void> iconst_2(JVM jvm, Thread thread) {
    thread.current_frame().op_stack.push(Value((int) 2));
    thread.inc_pc(1);
}
ErrorOr<void> iconst_3(JVM jvm, Thread thread) {
    thread.current_frame().op_stack.push(Value((int) 3));
    thread.inc_pc(1);
}
ErrorOr<void> iconst_4(JVM jvm, Thread thread) {
    thread.current_frame().op_stack.push(Value((int) 4));
    thread.inc_pc(1);
}
ErrorOr<void> iconst_5(JVM jvm, Thread thread) {
    thread.current_frame().op_stack.push(Value((int) 5));
    thread.inc_pc(1);
}
ErrorOr<void> lconst_0(JVM jvm, Thread thread) {
    thread.current_frame().op_stack.push(Value((long) 0));
    thread.inc_pc(1);
}
ErrorOr<void> lconst_1(JVM jvm, Thread thread) {
    thread.current_frame().op_stack.push(Value((long) 1));
    thread.inc_pc(1);
}

}
