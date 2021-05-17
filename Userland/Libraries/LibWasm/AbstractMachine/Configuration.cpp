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
    for (size_t index = m_stack.size(); index > 0; --index) {
        auto& entry = m_stack.entries()[index - 1];
        if (auto ptr = entry.get_pointer<NonnullOwnPtr<Label>>()) {
            if (i == 0)
                return **ptr;
            --i;
        }
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
    return host_function.function()(*this, arguments);
}

Result Configuration::execute()
{
    Interpreter interpreter;
    interpreter.interpret(*this);
    if (interpreter.did_trap())
        return Trap {};

    Vector<NonnullOwnPtr<Value>> results;
    for (size_t i = 0; i < m_current_frame->arity(); ++i)
        results.append(move(stack().pop().get<NonnullOwnPtr<Value>>()));
    auto label = stack().pop();
    // ASSERT: label == current frame
    if (!label.has<NonnullOwnPtr<Label>>())
        return Trap {};
    Vector<Value> results_moved;
    results_moved.ensure_capacity(results.size());
    for (auto& entry : results)
        results_moved.unchecked_append(move(*entry));
    return Result { move(results_moved) };
}

void Configuration::dump_stack()
{
    for (const auto& entry : stack().entries()) {
        entry.visit(
            [](const NonnullOwnPtr<Value>& v) {
                v->value().visit([]<typename T>(const T& v) {
                    if constexpr (IsIntegral<T> || IsFloatingPoint<T>)
                        dbgln("    {}", v);
                    else
                        dbgln("    *{}", v.value());
                });
            },
            [](const NonnullOwnPtr<Frame>& f) {
                dbgln("    frame({})", f->arity());
                for (auto& local : f->locals()) {
                    local.value().visit([]<typename T>(const T& v) {
                        if constexpr (IsIntegral<T> || IsFloatingPoint<T>)
                            dbgln("        {}", v);
                        else
                            dbgln("        *{}", v.value());
                    });
                }
            },
            [](const NonnullOwnPtr<Label>& l) {
                dbgln("    label({}) -> {}", l->arity(), l->continuation());
            });
    }
}

}
