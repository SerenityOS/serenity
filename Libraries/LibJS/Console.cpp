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

void Console::debug(String text)
{
    dbg() << "debug: " << text;
}

void Console::error(String text)
{
    dbg() << "error: " << text;
}

void Console::info(String text)
{
    dbg() << "info: " << text;
}

void Console::log(String text)
{
    dbg() << "log: " << text;
}

void Console::warn(String text)
{
    dbg() << "warn: " << text;
}

void Console::clear()
{
    dbg() << "clear:";
}

void Console::trace(String title)
{
    StringBuilder message_text;
    message_text.append(title);

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
}

void Console::count(String label)
{
    auto counter_value = m_counters.get(label);
    if (!counter_value.has_value()) {
        dbg() << "log: " << label << ": 1";
        m_counters.set(label, 1);
        return;
    }

    auto new_counter_value = counter_value.value() + 1;
    dbg() << "log: " << label << ": " << new_counter_value;
    m_counters.set(label, new_counter_value);
}

void Console::count_reset(String label)
{
    if (m_counters.contains(label)) {
        dbg() << "warn: \"" << label << "\" doesn't have a count";
        return;
    }

    m_counters.remove(label);
    dbg() << "log: " << label << ": 0";
}

}
