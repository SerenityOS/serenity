#pragma once
#include <AK/Error.h>
#include <LibJVM/JVM.h>
#include <LibJVM/Thread.h>

namespace JVM {

ErrorOr<void> nop(JVM jvm, Thread thread);
ErrorOr<void> aconst_null(JVM jvm, Thread thread);
ErrorOr<void> iconst_m1(JVM jvm, Thread thread);
ErrorOr<void> iconst_0(JVM jvm, Thread thread);
ErrorOr<void> iconst_1(JVM jvm, Thread thread);
ErrorOr<void> iconst_2(JVM jvm, Thread thread);
ErrorOr<void> iconst_3(JVM jvm, Thread thread);
ErrorOr<void> iconst_4(JVM jvm, Thread thread);
ErrorOr<void> iconst_5(JVM jvm, Thread thread);
ErrorOr<void> lconst_0(JVM jvm, Thread thread);
ErrorOr<void> lconst_1(JVM jvm, Thread thread);

}
