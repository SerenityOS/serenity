/*
 * Copyright (c) 2020, Emanuele Torre <torreemanuele6@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Function.h>
#include <YAK/HashMap.h>
#include <YAK/Noncopyable.h>
#include <LibJS/Forward.h>

namespace JS {

class ConsoleClient;

class Console {
    YAK_MAKE_NONCOPYABLE(Console);
    YAK_MAKE_NONMOVABLE(Console);

public:
    explicit Console(GlobalObject&);

    void set_client(ConsoleClient& client) { m_client = &client; }

    GlobalObject& global_object() { return m_global_object; }
    const GlobalObject& global_object() const { return m_global_object; }

    VM& vm();

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
    Value assert_();

    unsigned counter_increment(String label);
    bool counter_reset(String label);

private:
    GlobalObject& m_global_object;
    ConsoleClient* m_client { nullptr };

    HashMap<String, unsigned> m_counters;
};

class ConsoleClient {
public:
    explicit ConsoleClient(Console& console)
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
    virtual Value assert_() = 0;

protected:
    virtual ~ConsoleClient() = default;

    VM& vm();

    GlobalObject& global_object() { return m_console.global_object(); }
    const GlobalObject& global_object() const { return m_console.global_object(); }

    Vector<String> get_trace() const;

    Console& m_console;
};

}
