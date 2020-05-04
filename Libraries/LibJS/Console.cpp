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

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibJS/Console.h>
#include <LibJS/Interpreter.h>

namespace JS {

Console::Console(Interpreter& interpreter)
    : m_interpreter(interpreter)
{
}

Value Console::debug()
{
    dbg() << "debug: " << m_interpreter.join_arguments();
    return js_undefined();
}

Value Console::error()
{
    dbg() << "error: " << m_interpreter.join_arguments();
    return js_undefined();
}

Value Console::info()
{
    dbg() << "info: " << m_interpreter.join_arguments();
    return js_undefined();
}

Value Console::log()
{
    dbg() << "log: " << m_interpreter.join_arguments();
    return js_undefined();
}

Value Console::warn()
{
    dbg() << "warn: " << m_interpreter.join_arguments();
    return js_undefined();
}

Value Console::clear()
{
    dbg() << "clear:";
    return js_undefined();
}

Value Console::trace()
{
    StringBuilder message_text;
    message_text.append(m_interpreter.join_arguments());

    auto call_stack = m_interpreter.call_stack();
    // -2 to skip the console.trace() call frame
    for (ssize_t i = call_stack.size() - 2; i >= 0; --i) {
        auto function_name = call_stack[i].function_name;
        message_text.append("\n      -> ");
        if (String(function_name).is_empty())
            function_name = "<anonymous>";
        message_text.append(function_name);
    }

    dbg() << "log: " << message_text.build();
    return js_undefined();
}

Value Console::count()
{
    auto label = m_interpreter.argument_count() ? m_interpreter.argument(0).to_string() : "default";

    auto counter_value = m_counters.get(label);
    if (!counter_value.has_value()) {
        dbg() << "log: " << label << ": 1";
        m_counters.set(label, 1);
        return js_undefined();
    }

    auto new_counter_value = counter_value.value() + 1;
    dbg() << "log: " << label << ": " << new_counter_value;
    m_counters.set(label, new_counter_value);

    return js_undefined();
}

Value Console::count_reset()
{
    auto label = m_interpreter.argument_count() ? m_interpreter.argument(0).to_string() : "default";

    if (m_counters.contains(label)) {
        dbg() << "warn: \"" << label << "\" doesn't have a count";
        return js_undefined();
    }

    m_counters.remove(label);
    dbg() << "log: " << label << ": 0";
    return js_undefined();
}

}
