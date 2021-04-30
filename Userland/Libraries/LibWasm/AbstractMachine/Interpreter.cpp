/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWasm/AbstractMachine/AbstractMachine.h>
#include <LibWasm/AbstractMachine/Configuration.h>
#include <LibWasm/AbstractMachine/Interpreter.h>
#include <LibWasm/Opcode.h>

namespace Wasm {

void Interpreter::interpret(Configuration& configuration)
{
    // FIXME: Interpret stuff
    dbgln("FIXME: Interpret stuff!");
    // Push some dummy values
    for (size_t i = 0; i < configuration.frame()->arity(); ++i)
        configuration.stack().push(make<Value>(0));
}

}
