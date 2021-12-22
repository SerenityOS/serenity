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
#include <LibCore/ElapsedTimer.h>
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

    struct Group {
        String label;
    };

    struct Trace {
        String label;
        Vector<String> stack;
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
    ThrowCompletionOr<Value> trace();
    ThrowCompletionOr<Value> count();
    ThrowCompletionOr<Value> count_reset();
    ThrowCompletionOr<Value> assert_();
    ThrowCompletionOr<Value> group();
    ThrowCompletionOr<Value> group_collapsed();
    ThrowCompletionOr<Value> group_end();
    ThrowCompletionOr<Value> time();
    ThrowCompletionOr<Value> time_log();
    ThrowCompletionOr<Value> time_end();

    void output_debug_message(LogLevel log_level, String output) const;

private:
    ThrowCompletionOr<String> value_vector_to_string(Vector<Value>&);
    ThrowCompletionOr<String> format_time_since(Core::ElapsedTimer timer);

    GlobalObject& m_global_object;
    ConsoleClient* m_client { nullptr };

    HashMap<String, unsigned> m_counters;
    HashMap<String, Core::ElapsedTimer> m_timer_table;
    Vector<Group> m_group_stack;
};

class ConsoleClient {
public:
    explicit ConsoleClient(Console& console)
        : m_console(console)
    {
    }

    using PrinterArguments = Variant<Console::Group, Console::Trace, Vector<Value>>;

    ThrowCompletionOr<Value> logger(Console::LogLevel log_level, Vector<Value>& args);
    ThrowCompletionOr<Vector<Value>> formatter(Vector<Value>& args);
    virtual ThrowCompletionOr<Value> printer(Console::LogLevel log_level, PrinterArguments) = 0;

    virtual void clear() = 0;
    virtual void end_group() = 0;

protected:
    virtual ~ConsoleClient() = default;

    VM& vm();

    GlobalObject& global_object() { return m_console.global_object(); }
    const GlobalObject& global_object() const { return m_console.global_object(); }

    Console& m_console;
};

}
