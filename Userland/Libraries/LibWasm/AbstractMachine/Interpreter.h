/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWasm/AbstractMachine/Configuration.h>

namespace Wasm {

struct Interpreter {
    void interpret(Configuration&);

private:
    void interpret(Configuration&, InstructionPointer&, const Instruction&);
    void branch_to_label(Configuration&, LabelIndex);
    ReadonlyBytes load_from_memory(Configuration&, const Instruction&, size_t);
    void store_to_memory(Configuration&, const Instruction&, ReadonlyBytes data);
    void call_address(Configuration&, FunctionAddress);
};

}
