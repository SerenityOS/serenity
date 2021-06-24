/*
 * Copyright (c) 2020, Emanuele Torre <torreemanuele6@gmail.com>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Console.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

Console::Console(GlobalObject& global_object)
    : m_global_object(global_object)
{
}

VM& Console::vm()
{
    return m_global_object.vm();
}

Value Console::debug()
{
#ifdef __serenity__
    dbgln("\033[32;1m(js debug)\033[0m {}", vm().join_arguments());
#endif
    if (m_client)
        return m_client->debug();
    return js_undefined();
}

Value Console::error()
{
#ifdef __serenity__
    dbgln("\033[32;1m(js error)\033[0m {}", vm().join_arguments());
#endif
    if (m_client)
        return m_client->error();
    return js_undefined();
}

Value Console::info()
{
#ifdef __serenity__
    dbgln("\033[32;1m(js info)\033[0m {}", vm().join_arguments());
#endif
    if (m_client)
        return m_client->info();
    return js_undefined();
}

Value Console::log()
{
#ifdef __serenity__
    dbgln("\033[32;1m(js log)\033[0m {}", vm().join_arguments());
#endif
    if (m_client)
        return m_client->log();
    return js_undefined();
}

Value Console::warn()
{
#ifdef __serenity__
    dbgln("\033[32;1m(js warn)\033[0m {}", vm().join_arguments());
#endif
    if (m_client)
        return m_client->warn();
    return js_undefined();
}

Value Console::clear()
{
    if (m_client)
        return m_client->clear();
    return js_undefined();
}

Value Console::trace()
{
    if (m_client)
        return m_client->trace();
    return js_undefined();
}

Value Console::count()
{
    if (m_client)
        return m_client->count();
    return js_undefined();
}

Value Console::count_reset()
{
    if (m_client)
        return m_client->count_reset();
    return js_undefined();
}

Value Console::assert_()
{
    if (m_client)
        return m_client->assert_();
    return js_undefined();
}

unsigned Console::counter_increment(String label)
{
    auto value = m_counters.get(label);
    if (!value.has_value()) {
        m_counters.set(label, 1);
        return 1;
    }

    auto new_value = value.value() + 1;
    m_counters.set(label, new_value);
    return new_value;
}

bool Console::counter_reset(String label)
{
    if (!m_counters.contains(label))
        return false;

    m_counters.remove(label);
    return true;
}

VM& ConsoleClient::vm()
{
    return global_object().vm();
}

Vector<String> ConsoleClient::get_trace() const
{
    Vector<String> trace;
    auto& execution_context_stack = m_console.global_object().vm().execution_context_stack();
    // NOTE: -2 to skip the console.trace() execution context
    for (ssize_t i = execution_context_stack.size() - 2; i >= 0; --i)
        trace.append(execution_context_stack[i]->function_name);
    return trace;
}

}
