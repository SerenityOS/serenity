/*
 * Copyright (c) 2020, Emanuele Torre <torreemanuele6@gmail.com>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Console.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Temporal/Duration.h>

namespace JS {

Console::Console(GlobalObject& global_object)
    : m_global_object(global_object)
{
}

VM& Console::vm()
{
    return m_global_object.vm();
}

// 1.1.3. debug(...data), https://console.spec.whatwg.org/#debug
ThrowCompletionOr<Value> Console::debug()
{
    // 1. Perform Logger("debug", data).
    if (m_client) {
        auto data = vm_arguments();
        return m_client->logger(LogLevel::Debug, data);
    }
    return js_undefined();
}

// 1.1.4. error(...data), https://console.spec.whatwg.org/#error
ThrowCompletionOr<Value> Console::error()
{
    // 1. Perform Logger("error", data).
    if (m_client) {
        auto data = vm_arguments();
        return m_client->logger(LogLevel::Error, data);
    }
    return js_undefined();
}

// 1.1.5. info(...data), https://console.spec.whatwg.org/#info
ThrowCompletionOr<Value> Console::info()
{
    // 1. Perform Logger("info", data).
    if (m_client) {
        auto data = vm_arguments();
        return m_client->logger(LogLevel::Info, data);
    }
    return js_undefined();
}

// 1.1.6. log(...data), https://console.spec.whatwg.org/#log
ThrowCompletionOr<Value> Console::log()
{
    // 1. Perform Logger("log", data).
    if (m_client) {
        auto data = vm_arguments();
        return m_client->logger(LogLevel::Log, data);
    }
    return js_undefined();
}

// 1.1.9. warn(...data), https://console.spec.whatwg.org/#warn
ThrowCompletionOr<Value> Console::warn()
{
    // 1. Perform Logger("warn", data).
    if (m_client) {
        auto data = vm_arguments();
        return m_client->logger(LogLevel::Warn, data);
    }
    return js_undefined();
}

// 1.1.2. clear(), https://console.spec.whatwg.org/#clear
Value Console::clear()
{
    // 1. Empty the appropriate group stack.
    m_group_stack.clear();

    // 2. If possible for the environment, clear the console. (Otherwise, do nothing.)
    if (m_client)
        m_client->clear();
    return js_undefined();
}

// 1.1.8. trace(...data), https://console.spec.whatwg.org/#trace
ThrowCompletionOr<Value> Console::trace()
{
    if (!m_client)
        return js_undefined();

    // 1. Let trace be some implementation-specific, potentially-interactive representation of the callstack from where this function was called.
    Console::Trace trace;
    auto& execution_context_stack = vm().execution_context_stack();
    // NOTE: -2 to skip the console.trace() execution context
    for (ssize_t i = execution_context_stack.size() - 2; i >= 0; --i) {
        auto& function_name = execution_context_stack[i]->function_name;
        trace.stack.append(function_name.is_empty() ? "<anonymous>" : function_name);
    }

    // 2. Optionally, let formattedData be the result of Formatter(data), and incorporate formattedData as a label for trace.
    if (vm().argument_count() > 0) {
        StringBuilder builder;
        auto data = vm_arguments();
        auto formatted_data = TRY(m_client->formatter(data));
        trace.label = TRY(value_vector_to_string(formatted_data));
    }

    // 3. Perform Printer("trace", « trace »).
    return m_client->printer(JS::Console::LogLevel::Trace, trace);
}

// 1.2.1. count(label), https://console.spec.whatwg.org/#count
ThrowCompletionOr<Value> Console::count()
{
    // NOTE: "default" is the default value in the IDL. https://console.spec.whatwg.org/#ref-for-count
    auto label = vm().argument_count() ? TRY(vm().argument(0).to_string(global_object())) : "default";

    // 1. Let map be the associated count map.
    auto& map = m_counters;

    // 2. If map[label] exists, set map[label] to map[label] + 1.
    if (auto found = map.find(label); found != map.end()) {
        map.set(label, found->value + 1);
    }
    // 3. Otherwise, set map[label] to 1.
    else {
        map.set(label, 1);
    }

    // 4. Let concat be the concatenation of label, U+003A (:), U+0020 SPACE, and ToString(map[label]).
    String concat = String::formatted("{}: {}", label, map.get(label).value());

    // 5. Perform Logger("count", « concat »).
    Vector<Value> concat_as_vector { js_string(vm(), concat) };
    if (m_client)
        TRY(m_client->logger(LogLevel::Count, concat_as_vector));
    return js_undefined();
}

// 1.2.2. countReset(label), https://console.spec.whatwg.org/#countreset
ThrowCompletionOr<Value> Console::count_reset()
{
    // NOTE: "default" is the default value in the IDL. https://console.spec.whatwg.org/#ref-for-countreset
    auto label = vm().argument_count() ? TRY(vm().argument(0).to_string(global_object())) : "default";

    // 1. Let map be the associated count map.
    auto& map = m_counters;

    // 2. If map[label] exists, set map[label] to 0.
    if (auto found = map.find(label); found != map.end()) {
        map.set(label, 0);
    }
    // 3. Otherwise:
    else {
        // 1. Let message be a string without any formatting specifiers indicating generically
        //    that the given label does not have an associated count.
        auto message = String::formatted("\"{}\" doesn't have a count", label);
        // 2. Perform Logger("countReset", « message »);
        Vector<Value> message_as_vector { js_string(vm(), message) };
        if (m_client)
            TRY(m_client->logger(LogLevel::CountReset, message_as_vector));
    }

    return js_undefined();
}

// 1.1.1. assert(condition, ...data), https://console.spec.whatwg.org/#assert
ThrowCompletionOr<Value> Console::assert_()
{
    // 1. If condition is true, return.
    auto condition = vm().argument(0).to_boolean();
    if (condition)
        return js_undefined();

    // 2. Let message be a string without any formatting specifiers indicating generically an assertion failure (such as "Assertion failed").
    auto message = js_string(vm(), "Assertion failed");

    // NOTE: Assemble `data` from the function arguments.
    Vector<JS::Value> data;
    if (vm().argument_count() > 1) {
        data.ensure_capacity(vm().argument_count() - 1);
        for (size_t i = 1; i < vm().argument_count(); ++i) {
            data.append(vm().argument(i));
        }
    }

    // 3. If data is empty, append message to data.
    if (data.is_empty()) {
        data.append(message);
    }
    // 4. Otherwise:
    else {
        // 1. Let first be data[0].
        auto& first = data[0];
        // 2. If Type(first) is not String, then prepend message to data.
        if (!first.is_string()) {
            data.prepend(message);
        }
        // 3. Otherwise:
        else {
            // 1. Let concat be the concatenation of message, U+003A (:), U+0020 SPACE, and first.
            auto concat = js_string(vm(), String::formatted("{}: {}", message->string(), first.to_string(global_object()).value()));
            // 2. Set data[0] to concat.
            data[0] = concat;
        }
    }

    // 5. Perform Logger("assert", data).
    if (m_client)
        TRY(m_client->logger(LogLevel::Assert, data));
    return js_undefined();
}

// 1.3.1. group(...data), https://console.spec.whatwg.org/#group
ThrowCompletionOr<Value> Console::group()
{
    // 1. Let group be a new group.
    Group group;

    // 2. If data is not empty, let groupLabel be the result of Formatter(data).
    String group_label;
    auto data = vm_arguments();
    if (!data.is_empty()) {
        auto formatted_data = TRY(m_client->formatter(data));
        group_label = TRY(value_vector_to_string(formatted_data));
    }
    // ... Otherwise, let groupLabel be an implementation-chosen label representing a group.
    else {
        group_label = "Group";
    }

    // 3. Incorporate groupLabel as a label for group.
    group.label = group_label;

    // 4. Optionally, if the environment supports interactive groups, group should be expanded by default.
    // NOTE: This is handled in Printer.

    // 5. Perform Printer("group", « group »).
    if (m_client)
        TRY(m_client->printer(LogLevel::Group, group));

    // 6. Push group onto the appropriate group stack.
    m_group_stack.append(group);

    return js_undefined();
}

// 1.3.2. groupCollapsed(...data), https://console.spec.whatwg.org/#groupcollapsed
ThrowCompletionOr<Value> Console::group_collapsed()
{
    // 1. Let group be a new group.
    Group group;

    // 2. If data is not empty, let groupLabel be the result of Formatter(data).
    String group_label;
    auto data = vm_arguments();
    if (!data.is_empty()) {
        auto formatted_data = TRY(m_client->formatter(data));
        group_label = TRY(value_vector_to_string(formatted_data));
    }
    // ... Otherwise, let groupLabel be an implementation-chosen label representing a group.
    else {
        group_label = "Group";
    }

    // 3. Incorporate groupLabel as a label for group.
    group.label = group_label;

    // 4. Optionally, if the environment supports interactive groups, group should be collapsed by default.
    // NOTE: This is handled in Printer.

    // 5. Perform Printer("groupCollapsed", « group »).
    if (m_client)
        TRY(m_client->printer(LogLevel::GroupCollapsed, group));

    // 6. Push group onto the appropriate group stack.
    m_group_stack.append(group);

    return js_undefined();
}

// 1.3.3. groupEnd(), https://console.spec.whatwg.org/#groupend
ThrowCompletionOr<Value> Console::group_end()
{
    if (m_group_stack.is_empty())
        return js_undefined();

    // 1. Pop the last group from the group stack.
    m_group_stack.take_last();
    if (m_client)
        m_client->end_group();

    return js_undefined();
}

// 1.4.1. time(label), https://console.spec.whatwg.org/#time
ThrowCompletionOr<Value> Console::time()
{
    // NOTE: "default" is the default value in the IDL. https://console.spec.whatwg.org/#ref-for-time
    auto label = vm().argument_count() ? TRY(vm().argument(0).to_string(global_object())) : "default";

    // 1. If the associated timer table contains an entry with key label, return, optionally reporting
    // a warning to the console indicating that a timer with label `label` has already been started.
    if (m_timer_table.contains(label)) {
        if (m_client)
            TRY(m_client->printer(LogLevel::Warn, { Vector<Value> { js_string(vm(), String::formatted("Timer '{}' already exists.", label)) } }));
        return js_undefined();
    }

    // 2. Otherwise, set the value of the entry with key label in the associated timer table to the current time.
    m_timer_table.set(label, Core::ElapsedTimer::start_new());
    return js_undefined();
}

// 1.4.2. timeLog(label, ...data), https://console.spec.whatwg.org/#timelog
ThrowCompletionOr<Value> Console::time_log()
{
    // NOTE: "default" is the default value in the IDL. https://console.spec.whatwg.org/#ref-for-timelog
    auto label = vm().argument_count() ? TRY(vm().argument(0).to_string(global_object())) : "default";

    // 1. Let timerTable be the associated timer table.

    // 2. Let startTime be timerTable[label].
    auto maybe_start_time = m_timer_table.find(label);

    // NOTE: Warn if the timer doesn't exist. Not part of the spec yet, but discussed here: https://github.com/whatwg/console/issues/134
    if (maybe_start_time == m_timer_table.end()) {
        if (m_client)
            TRY(m_client->printer(LogLevel::Warn, { Vector<Value> { js_string(vm(), String::formatted("Timer '{}' does not exist.", label)) } }));
        return js_undefined();
    }
    auto start_time = maybe_start_time->value;

    // 3. Let duration be a string representing the difference between the current time and startTime, in an implementation-defined format.
    auto duration = TRY(format_time_since(start_time));

    // 4. Let concat be the concatenation of label, U+003A (:), U+0020 SPACE, and duration.
    auto concat = String::formatted("{}: {}", label, duration);

    // 5. Prepend concat to data.
    Vector<Value> data;
    data.ensure_capacity(vm().argument_count());
    data.append(js_string(vm(), concat));
    for (size_t i = 1; i < vm().argument_count(); ++i)
        data.append(vm().argument(i));

    // 6. Perform Printer("timeLog", data).
    if (m_client)
        TRY(m_client->printer(LogLevel::TimeLog, data));
    return js_undefined();
}

// 1.4.3. timeEnd(label), https://console.spec.whatwg.org/#timeend
ThrowCompletionOr<Value> Console::time_end()
{
    // NOTE: "default" is the default value in the IDL. https://console.spec.whatwg.org/#ref-for-timeend
    auto label = vm().argument_count() ? TRY(vm().argument(0).to_string(global_object())) : "default";

    // 1. Let timerTable be the associated timer table.

    // 2. Let startTime be timerTable[label].
    auto maybe_start_time = m_timer_table.find(label);

    // NOTE: Warn if the timer doesn't exist. Not part of the spec yet, but discussed here: https://github.com/whatwg/console/issues/134
    if (maybe_start_time == m_timer_table.end()) {
        if (m_client)
            TRY(m_client->printer(LogLevel::Warn, { Vector<Value> { js_string(vm(), String::formatted("Timer '{}' does not exist.", label)) } }));
        return js_undefined();
    }
    auto start_time = maybe_start_time->value;

    // 3. Remove timerTable[label].
    m_timer_table.remove(label);

    // 4. Let duration be a string representing the difference between the current time and startTime, in an implementation-defined format.
    auto duration = TRY(format_time_since(start_time));

    // 5. Let concat be the concatenation of label, U+003A (:), U+0020 SPACE, and duration.
    auto concat = String::formatted("{}: {}", label, duration);

    // 6. Perform Printer("timeEnd", « concat »).
    if (m_client) {
        Vector<Value> concat_as_vector { js_string(vm(), concat) };
        TRY(m_client->printer(LogLevel::TimeEnd, concat_as_vector));
    }
    return js_undefined();
}

Vector<Value> Console::vm_arguments()
{
    Vector<Value> arguments;
    arguments.ensure_capacity(vm().argument_count());
    for (size_t i = 0; i < vm().argument_count(); ++i) {
        arguments.append(vm().argument(i));
    }
    return arguments;
}

void Console::output_debug_message([[maybe_unused]] LogLevel log_level, [[maybe_unused]] String output) const
{
#ifdef __serenity__
    switch (log_level) {
    case JS::Console::LogLevel::Debug:
        dbgln("\033[32;1m(js debug)\033[0m {}", output);
        break;
    case JS::Console::LogLevel::Error:
        dbgln("\033[32;1m(js error)\033[0m {}", output);
        break;
    case JS::Console::LogLevel::Info:
        dbgln("\033[32;1m(js info)\033[0m {}", output);
        break;
    case JS::Console::LogLevel::Log:
        dbgln("\033[32;1m(js log)\033[0m {}", output);
        break;
    case JS::Console::LogLevel::Warn:
        dbgln("\033[32;1m(js warn)\033[0m {}", output);
        break;
    default:
        dbgln("\033[32;1m(js)\033[0m {}", output);
        break;
    }
#endif
}

ThrowCompletionOr<String> Console::value_vector_to_string(Vector<Value>& values)
{
    StringBuilder builder;
    for (auto const& item : values) {
        if (!builder.is_empty())
            builder.append(' ');
        builder.append(TRY(item.to_string(global_object())));
    }
    return builder.to_string();
}

ThrowCompletionOr<String> Console::format_time_since(Core::ElapsedTimer timer)
{
    auto elapsed_ms = timer.elapsed_time().to_milliseconds();
    auto duration = TRY(Temporal::balance_duration(global_object(), 0, 0, 0, 0, elapsed_ms, 0, *js_bigint(vm(), "0"_sbigint), "year"));

    auto append = [&](StringBuilder& builder, auto format, auto... number) {
        if (!builder.is_empty())
            builder.append(' ');
        builder.appendff(format, number...);
    };
    StringBuilder builder;
    if (duration.days > 0)
        append(builder, "{:.0} day(s)", duration.days);
    if (duration.hours > 0)
        append(builder, "{:.0} hour(s)", duration.hours);
    if (duration.minutes > 0)
        append(builder, "{:.0} minute(s)", duration.minutes);
    if (duration.seconds > 0 || duration.milliseconds > 0) {
        double combined_seconds = duration.seconds + (0.001 * duration.milliseconds);
        append(builder, "{:.3} seconds", combined_seconds);
    }

    return builder.to_string();
}

VM& ConsoleClient::vm()
{
    return global_object().vm();
}

// 2.1. Logger(logLevel, args), https://console.spec.whatwg.org/#logger
ThrowCompletionOr<Value> ConsoleClient::logger(Console::LogLevel log_level, Vector<Value>& args)
{
    auto& global_object = this->global_object();

    // 1. If args is empty, return.
    if (args.is_empty())
        return js_undefined();

    // 2. Let first be args[0].
    auto first = args[0];

    // 3. Let rest be all elements following first in args.
    size_t rest_size = args.size() - 1;

    // 4. If rest is empty, perform Printer(logLevel, « first ») and return.
    if (rest_size == 0) {
        auto first_as_vector = Vector { first };
        return printer(log_level, first_as_vector);
    }

    // 5. If first does not contain any format specifiers, perform Printer(logLevel, args).
    if (!TRY(first.to_string(global_object)).contains('%')) {
        TRY(printer(log_level, args));
    } else {
        // 6. Otherwise, perform Printer(logLevel, Formatter(args)).
        auto formatted = TRY(formatter(args));
        TRY(printer(log_level, formatted));
    }

    // 7. Return undefined.
    return js_undefined();
}

// 2.2. Formatter(args), https://console.spec.whatwg.org/#formatter
ThrowCompletionOr<Vector<Value>> ConsoleClient::formatter(Vector<Value>& args)
{
    // TODO: Actually implement formatting
    return args;
}

}
