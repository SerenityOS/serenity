/*
 * Copyright (c) 2020, Emanuele Torre <torreemanuele6@gmail.com>
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

#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/Noncopyable.h>
#include <LibJS/Forward.h>

namespace JS {

class ConsoleClient;

class Console {
    AK_MAKE_NONCOPYABLE(Console);
    AK_MAKE_NONMOVABLE(Console);

public:
    explicit Console(GlobalObject&);

    void set_client(ConsoleClient& client) { m_client = &client; }

    GlobalObject& global_object() { return m_global_object; }
    const GlobalObject& global_object() const { return m_global_object; }

    HashMap<String, unsigned>& counters() { return m_counters; }
    const HashMap<String, unsigned>& counters() const { return m_counters; }

    Value debug();
    Value error();
    Value info();
    Value log();
    Value warn();

    Value clear();

    Value trace();

    Value count();
    Value count_reset();

    unsigned counter_increment(String label);
    bool counter_reset(String label);

private:
    GlobalObject& m_global_object;
    ConsoleClient* m_client { nullptr };

    HashMap<String, unsigned> m_counters;
};

class ConsoleClient {
public:
    ConsoleClient(Console& console)
        : m_console(console)
    {
    }

    virtual Value debug() = 0;
    virtual Value error() = 0;
    virtual Value info() = 0;
    virtual Value log() = 0;
    virtual Value warn() = 0;
    virtual Value clear() = 0;
    virtual Value trace() = 0;
    virtual Value count() = 0;
    virtual Value count_reset() = 0;

protected:
    VM& vm();

    GlobalObject& global_object() { return m_console.global_object(); }
    const GlobalObject& global_object() const { return m_console.global_object(); }

    Vector<String> get_trace() const;

    Console& m_console;
};

}
