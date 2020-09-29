/*
 * Copyright (c) 2020, Emanuele Torre <torreemanuele6@gmail.com>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibJS/Console.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

Console::Console(GlobalObject& global_object)
    : m_global_object(global_object)
{
}

Value Console::debug()
{
    if (m_client)
        return m_client->debug();
    return js_undefined();
}

Value Console::error()
{
    if (m_client)
        return m_client->error();
    return js_undefined();
}

Value Console::info()
{
    if (m_client)
        return m_client->info();
    return js_undefined();
}

Value Console::log()
{
    if (m_client)
        return m_client->log();
    return js_undefined();
}

Value Console::warn()
{
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
    auto& call_stack = m_console.global_object().vm().call_stack();
    // -2 to skip the console.trace() call frame
    for (ssize_t i = call_stack.size() - 2; i >= 0; --i)
        trace.append(call_stack[i].function_name);
    return trace;
}

}
