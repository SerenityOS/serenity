/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWasm/AbstractMachine/Configuration.h>
#include <LibWasm/AbstractMachine/Interpreter.h>

namespace Wasm {

Optional<Label> Configuration::nth_label(size_t i)
{
    if (i > 0)
        TODO();
    for (auto& entry : m_stack.entries()) {
        if (auto ptr = entry.get_pointer<NonnullOwnPtr<Label>>())
            return **ptr;
    }
    return {};
}

Result Configuration::call(FunctionAddress address, Vector<Value> arguments)
{
    auto* function = m_store.get(address);
    if (!function)
        return Trap {};
    if (auto* wasm_function = function->get_pointer<WasmFunction>()) {
        Vector<Value> locals;
        locals.ensure_capacity(arguments.size() + wasm_function->code().locals().size());
        for (auto& value : arguments)
            locals.append(Value { value });
        for (auto& type : wasm_function->code().locals())
            locals.empend(type, 0ull);

        auto frame = make<Frame>(
            wasm_function->module(),
            move(locals),
            wasm_function->code().body(),
            wasm_function->type().results().size());

        set_frame(move(frame));
        return execute();
    }

    // It better be a host function, else something is really wrong.
    auto& host_function = function->get<HostFunction>();
    auto result = bit_cast<HostFunctionType>(host_function.ptr())(m_store, arguments);
    auto count = host_function.type().results().size();
    if (count == 0)
        return Result { Vector<Value> {} };
    if (count == 1)
        return Result { Vector<Value> { Value { host_function.type().results().first(), result } } };
    TODO();
}

Result Configuration::execute()
{
    Interpreter interpreter;
    interpreter.interpret(*this);
    Vector<NonnullOwnPtr<Value>> results;
    for (size_t i = 0; i < m_current_frame->arity(); ++i)
        results.append(move(stack().pop().get<NonnullOwnPtr<Value>>()));
    auto label = stack().pop();
    // ASSERT: label == current frame
    VERIFY(label.has<NonnullOwnPtr<Label>>());
    Vector<Value> results_moved;
    results_moved.ensure_capacity(results.size());
    for (auto& entry : results)
        results_moved.unchecked_append(move(*entry));
    return Result { move(results_moved) };
}

}
