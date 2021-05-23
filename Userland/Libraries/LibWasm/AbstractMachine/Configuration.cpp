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
        if (auto ptr = entry.get_pointer<Label>()) {
            if (i == 0)
                return *ptr;
            --i;
        }
    }
    return {};
}

void Configuration::unwind(Badge<CallFrameHandle>, const CallFrameHandle& frame_handle)
{
    VERIFY(m_stack.size() > frame_handle.stack_size);
    m_stack.entries().remove(frame_handle.stack_size, m_stack.size() - frame_handle.stack_size);
    m_current_frame_index = frame_handle.frame_index;
    m_depth--;
    m_ip = frame_handle.ip;
    VERIFY(m_stack.size() == frame_handle.stack_size);
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

        set_frame(Frame {
            wasm_function->module(),
            move(locals),
            wasm_function->code().body(),
            wasm_function->type().results().size(),
        });
        m_ip = 0;
        return execute();
    }

    // It better be a host function, else something is really wrong.
    auto& host_function = function->get<HostFunction>();
    return host_function.function()(*this, arguments);
}

Result Configuration::execute()
{
    Interpreter interpreter;
    interpreter.pre_interpret_hook = pre_interpret_hook;
    interpreter.post_interpret_hook = post_interpret_hook;

    interpreter.interpret(*this);
    if (interpreter.did_trap())
        return Trap {};

    Vector<Value> results;
    results.ensure_capacity(frame().arity());
    for (size_t i = 0; i < frame().arity(); ++i)
        results.append(move(stack().pop().get<Value>()));
    auto label = stack().pop();
    // ASSERT: label == current frame
    if (!label.has<Label>())
        return Trap {};
    return Result { move(results) };
}

void Configuration::dump_stack()
{
    for (const auto& entry : stack().entries()) {
        entry.visit(
            [](const Value& v) {
                v.value().visit([]<typename T>(const T& v) {
                    if constexpr (IsIntegral<T> || IsFloatingPoint<T>)
                        dbgln("    {}", v);
                    else
                        dbgln("    *{}", v.value());
                });
            },
            [](const Frame& f) {
                dbgln("    frame({})", f.arity());
                for (auto& local : f.locals()) {
                    local.value().visit([]<typename T>(const T& v) {
                        if constexpr (IsIntegral<T> || IsFloatingPoint<T>)
                            dbgln("        {}", v);
                        else
                            dbgln("        *{}", v.value());
                    });
                }
            },
            [](const Label& l) {
                dbgln("    label({}) -> {}", l.arity(), l.continuation());
            });
    }
}

}
