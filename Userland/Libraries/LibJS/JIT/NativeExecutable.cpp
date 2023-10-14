/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/JIT/NativeExecutable.h>
#include <LibJS/Runtime/VM.h>
#include <sys/mman.h>

namespace JS::JIT {

NativeExecutable::NativeExecutable(void* code, size_t size)
    : m_code(code)
    , m_size(size)
{
}

NativeExecutable::~NativeExecutable()
{
    munmap(m_code, m_size);
}

void NativeExecutable::run(VM& vm)
{
    typedef void (*JITCode)(VM&, Value* registers, Value* locals);
    ((JITCode)m_code)(vm,
        vm.bytecode_interpreter().registers().data(),
        vm.running_execution_context().local_variables.data());
}

}
