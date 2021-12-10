/*
 * Copyright (c) 2020, Emanuele Torre <torreemanuele6@gmail.com>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/Noncopyable.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class ConsoleClient;

// https://console.spec.whatwg.org
class Console {
    AK_MAKE_NONCOPYABLE(Console);
    AK_MAKE_NONMOVABLE(Console);

public:
    // These are not really levels, but that's the term used in the spec.
    enum class LogLevel {
        Assert,
        Count,
        CountReset,
        Debug,
        Dir,
        DirXML,
        Error,
        Group,
        GroupCollapsed,
        Info,
        Log,
        TimeEnd,
        TimeLog,
        Trace,
        Warn,
    };

    explicit Console(GlobalObject&);

    void set_client(ConsoleClient& client) { m_client = &client; }

    GlobalObject& global_object() { return m_global_object; }
    const GlobalObject& global_object() const { return m_global_object; }

    VM& vm();
    Vector<Value> vm_arguments();

    HashMap<String, unsigned>& counters() { return m_counters; }
    const HashMap<String, unsigned>& counters() const { return m_counters; }

    ThrowCompletionOr<Value> debug();
    ThrowCompletionOr<Value> error();
    ThrowCompletionOr<Value> info();
    ThrowCompletionOr<Value> log();
    ThrowCompletionOr<Value> warn();
    Value clear();
    Value trace();
    Value count();
    Value count_reset();
    Value assert_();

    unsigned counter_increment(String label);
    bool counter_reset(String label);

    void output_debug_message(LogLevel log_level, String output) const;

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

    ThrowCompletionOr<Value> logger(Console::LogLevel log_level, Vector<Value>& args);
    ThrowCompletionOr<Vector<Value>> formatter(Vector<Value>& args);
    virtual ThrowCompletionOr<Value> printer(Console::LogLevel log_level, Vector<Value>&) = 0;

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
