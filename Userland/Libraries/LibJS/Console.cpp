/*
 * Copyright (c) 2020, Emanuele Torre <torreemanuele6@gmail.com>
 * Copyright (c) 2020-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <AK/StringBuilder.h>
#include <LibJS/Console.h>
#include <LibJS/Print.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/StringConstructor.h>
#include <LibJS/Runtime/Temporal/Duration.h>
#include <LibJS/Runtime/ValueInlines.h>

namespace JS {

JS_DEFINE_ALLOCATOR(Console);
JS_DEFINE_ALLOCATOR(ConsoleClient);

Console::Console(Realm& realm)
    : m_realm(realm)
{
}

Console::~Console() = default;

void Console::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_realm);
    visitor.visit(m_client);
}

// 1.1.1. assert(condition, ...data), https://console.spec.whatwg.org/#assert
ThrowCompletionOr<Value> Console::assert_()
{
    auto& vm = realm().vm();

    // 1. If condition is true, return.
    auto condition = vm.argument(0).to_boolean();
    if (condition)
        return js_undefined();

    // 2. Let message be a string without any formatting specifiers indicating generically an assertion failure (such as "Assertion failed").
    auto message = PrimitiveString::create(vm, "Assertion failed"_string);

    // NOTE: Assemble `data` from the function arguments.
    MarkedVector<Value> data { vm.heap() };
    if (vm.argument_count() > 1) {
        data.ensure_capacity(vm.argument_count() - 1);
        for (size_t i = 1; i < vm.argument_count(); ++i) {
            data.append(vm.argument(i));
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
            auto concat = TRY_OR_THROW_OOM(vm, String::formatted("{}: {}", message->utf8_string(), MUST(first.to_string(vm))));
            // 2. Set data[0] to concat.
            data[0] = PrimitiveString::create(vm, move(concat));
        }
    }

    // 5. Perform Logger("assert", data).
    if (m_client)
        TRY(m_client->logger(LogLevel::Assert, data));
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

// 1.1.8. trace(...data), https://console.spec.whatwg.org/#trace
ThrowCompletionOr<Value> Console::trace()
{
    if (!m_client)
        return js_undefined();

    auto& vm = realm().vm();

    // 1. Let trace be some implementation-specific, potentially-interactive representation of the callstack from where this function was called.
    Console::Trace trace;
    auto& execution_context_stack = vm.execution_context_stack();
    // NOTE: -2 to skip the console.trace() execution context
    for (ssize_t i = execution_context_stack.size() - 2; i >= 0; --i) {
        auto const& function_name = execution_context_stack[i]->function_name;
        trace.stack.append((!function_name || function_name->is_empty())
                ? "<anonymous>"_string
                : function_name->utf8_string());
    }

    // 2. Optionally, let formattedData be the result of Formatter(data), and incorporate formattedData as a label for trace.
    if (vm.argument_count() > 0) {
        auto data = vm_arguments();
        auto formatted_data = TRY(m_client->formatter(data));
        trace.label = TRY(value_vector_to_string(formatted_data));
    }

    // 3. Perform Printer("trace", « trace »).
    return m_client->printer(Console::LogLevel::Trace, trace);
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

// 1.1.10. dir(item, options), https://console.spec.whatwg.org/#dir
ThrowCompletionOr<Value> Console::dir()
{
    auto& vm = realm().vm();

    // 1. Let object be item with generic JavaScript object formatting applied.
    // NOTE: Generic formatting is performed by ConsoleClient::printer().
    auto object = vm.argument(0);

    // 2. Perform Printer("dir", « object », options).
    if (m_client) {
        MarkedVector<Value> printer_arguments { vm.heap() };
        TRY_OR_THROW_OOM(vm, printer_arguments.try_append(object));

        return m_client->printer(LogLevel::Dir, move(printer_arguments));
    }

    return js_undefined();
}

static ThrowCompletionOr<String> label_or_fallback(VM& vm, StringView fallback)
{
    return vm.argument_count() > 0
        ? vm.argument(0).to_string(vm)
        : TRY_OR_THROW_OOM(vm, String::from_utf8(fallback));
}

// 1.2.1. count(label), https://console.spec.whatwg.org/#count
ThrowCompletionOr<Value> Console::count()
{
    auto& vm = realm().vm();

    // NOTE: "default" is the default value in the IDL. https://console.spec.whatwg.org/#ref-for-count
    auto label = TRY(label_or_fallback(vm, "default"sv));

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
    auto concat = TRY_OR_THROW_OOM(vm, String::formatted("{}: {}", label, map.get(label).value()));

    // 5. Perform Logger("count", « concat »).
    MarkedVector<Value> concat_as_vector { vm.heap() };
    concat_as_vector.append(PrimitiveString::create(vm, move(concat)));
    if (m_client)
        TRY(m_client->logger(LogLevel::Count, concat_as_vector));
    return js_undefined();
}

// 1.2.2. countReset(label), https://console.spec.whatwg.org/#countreset
ThrowCompletionOr<Value> Console::count_reset()
{
    auto& vm = realm().vm();

    // NOTE: "default" is the default value in the IDL. https://console.spec.whatwg.org/#ref-for-countreset
    auto label = TRY(label_or_fallback(vm, "default"sv));

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
        auto message = TRY_OR_THROW_OOM(vm, String::formatted("\"{}\" doesn't have a count", label));
        // 2. Perform Logger("countReset", « message »);
        MarkedVector<Value> message_as_vector { vm.heap() };
        message_as_vector.append(PrimitiveString::create(vm, move(message)));
        if (m_client)
            TRY(m_client->logger(LogLevel::CountReset, message_as_vector));
    }

    return js_undefined();
}

// 1.3.1. group(...data), https://console.spec.whatwg.org/#group
ThrowCompletionOr<Value> Console::group()
{
    // 1. Let group be a new group.
    Group group;

    // 2. If data is not empty, let groupLabel be the result of Formatter(data).
    String group_label {};
    auto data = vm_arguments();
    if (!data.is_empty()) {
        auto formatted_data = TRY(m_client->formatter(data));
        group_label = TRY(value_vector_to_string(formatted_data));
    }
    // ... Otherwise, let groupLabel be an implementation-chosen label representing a group.
    else {
        group_label = "Group"_string;
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
    String group_label {};
    auto data = vm_arguments();
    if (!data.is_empty()) {
        auto formatted_data = TRY(m_client->formatter(data));
        group_label = TRY(value_vector_to_string(formatted_data));
    }
    // ... Otherwise, let groupLabel be an implementation-chosen label representing a group.
    else {
        group_label = "Group"_string;
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
    auto& vm = realm().vm();

    // NOTE: "default" is the default value in the IDL. https://console.spec.whatwg.org/#ref-for-time
    auto label = TRY(label_or_fallback(vm, "default"sv));

    // 1. If the associated timer table contains an entry with key label, return, optionally reporting
    //    a warning to the console indicating that a timer with label `label` has already been started.
    if (m_timer_table.contains(label)) {
        if (m_client) {
            MarkedVector<Value> timer_already_exists_warning_message_as_vector { vm.heap() };

            auto message = TRY_OR_THROW_OOM(vm, String::formatted("Timer '{}' already exists.", label));
            timer_already_exists_warning_message_as_vector.append(PrimitiveString::create(vm, move(message)));

            TRY(m_client->printer(LogLevel::Warn, move(timer_already_exists_warning_message_as_vector)));
        }
        return js_undefined();
    }

    // 2. Otherwise, set the value of the entry with key label in the associated timer table to the current time.
    m_timer_table.set(label, Core::ElapsedTimer::start_new());
    return js_undefined();
}

// 1.4.2. timeLog(label, ...data), https://console.spec.whatwg.org/#timelog
ThrowCompletionOr<Value> Console::time_log()
{
    auto& vm = realm().vm();

    // NOTE: "default" is the default value in the IDL. https://console.spec.whatwg.org/#ref-for-timelog
    auto label = TRY(label_or_fallback(vm, "default"sv));

    // 1. Let timerTable be the associated timer table.

    // 2. Let startTime be timerTable[label].
    auto maybe_start_time = m_timer_table.find(label);

    // NOTE: Warn if the timer doesn't exist. Not part of the spec yet, but discussed here: https://github.com/whatwg/console/issues/134
    if (maybe_start_time == m_timer_table.end()) {
        if (m_client) {
            MarkedVector<Value> timer_does_not_exist_warning_message_as_vector { vm.heap() };

            auto message = TRY_OR_THROW_OOM(vm, String::formatted("Timer '{}' does not exist.", label));
            timer_does_not_exist_warning_message_as_vector.append(PrimitiveString::create(vm, move(message)));

            TRY(m_client->printer(LogLevel::Warn, move(timer_does_not_exist_warning_message_as_vector)));
        }
        return js_undefined();
    }
    auto start_time = maybe_start_time->value;

    // 3. Let duration be a string representing the difference between the current time and startTime, in an implementation-defined format.
    auto duration = TRY(format_time_since(start_time));

    // 4. Let concat be the concatenation of label, U+003A (:), U+0020 SPACE, and duration.
    auto concat = TRY_OR_THROW_OOM(vm, String::formatted("{}: {}", label, duration));

    // 5. Prepend concat to data.
    MarkedVector<Value> data { vm.heap() };
    data.ensure_capacity(vm.argument_count());
    data.append(PrimitiveString::create(vm, move(concat)));
    for (size_t i = 1; i < vm.argument_count(); ++i)
        data.append(vm.argument(i));

    // 6. Perform Printer("timeLog", data).
    if (m_client)
        TRY(m_client->printer(LogLevel::TimeLog, move(data)));
    return js_undefined();
}

// 1.4.3. timeEnd(label), https://console.spec.whatwg.org/#timeend
ThrowCompletionOr<Value> Console::time_end()
{
    auto& vm = realm().vm();

    // NOTE: "default" is the default value in the IDL. https://console.spec.whatwg.org/#ref-for-timeend
    auto label = TRY(label_or_fallback(vm, "default"sv));

    // 1. Let timerTable be the associated timer table.

    // 2. Let startTime be timerTable[label].
    auto maybe_start_time = m_timer_table.find(label);

    // NOTE: Warn if the timer doesn't exist. Not part of the spec yet, but discussed here: https://github.com/whatwg/console/issues/134
    if (maybe_start_time == m_timer_table.end()) {
        if (m_client) {
            MarkedVector<Value> timer_does_not_exist_warning_message_as_vector { vm.heap() };

            auto message = TRY_OR_THROW_OOM(vm, String::formatted("Timer '{}' does not exist.", label));
            timer_does_not_exist_warning_message_as_vector.append(PrimitiveString::create(vm, move(message)));

            TRY(m_client->printer(LogLevel::Warn, move(timer_does_not_exist_warning_message_as_vector)));
        }
        return js_undefined();
    }
    auto start_time = maybe_start_time->value;

    // 3. Remove timerTable[label].
    m_timer_table.remove(label);

    // 4. Let duration be a string representing the difference between the current time and startTime, in an implementation-defined format.
    auto duration = TRY(format_time_since(start_time));

    // 5. Let concat be the concatenation of label, U+003A (:), U+0020 SPACE, and duration.
    auto concat = TRY_OR_THROW_OOM(vm, String::formatted("{}: {}", label, duration));

    // 6. Perform Printer("timeEnd", « concat »).
    if (m_client) {
        MarkedVector<Value> concat_as_vector { vm.heap() };
        concat_as_vector.append(PrimitiveString::create(vm, move(concat)));
        TRY(m_client->printer(LogLevel::TimeEnd, move(concat_as_vector)));
    }
    return js_undefined();
}

MarkedVector<Value> Console::vm_arguments()
{
    auto& vm = realm().vm();

    MarkedVector<Value> arguments { vm.heap() };
    arguments.ensure_capacity(vm.argument_count());
    for (size_t i = 0; i < vm.argument_count(); ++i) {
        arguments.append(vm.argument(i));
    }
    return arguments;
}

void Console::output_debug_message(LogLevel log_level, String const& output) const
{
    switch (log_level) {
    case Console::LogLevel::Debug:
        dbgln("\033[32;1m(js debug)\033[0m {}", output);
        break;
    case Console::LogLevel::Error:
        dbgln("\033[32;1m(js error)\033[0m {}", output);
        break;
    case Console::LogLevel::Info:
        dbgln("\033[32;1m(js info)\033[0m {}", output);
        break;
    case Console::LogLevel::Log:
        dbgln("\033[32;1m(js log)\033[0m {}", output);
        break;
    case Console::LogLevel::Warn:
        dbgln("\033[32;1m(js warn)\033[0m {}", output);
        break;
    default:
        dbgln("\033[32;1m(js)\033[0m {}", output);
        break;
    }
}

void Console::report_exception(JS::Error const& exception, bool in_promise) const
{
    if (m_client)
        m_client->report_exception(exception, in_promise);
}

ThrowCompletionOr<String> Console::value_vector_to_string(MarkedVector<Value> const& values)
{
    auto& vm = realm().vm();
    StringBuilder builder;

    for (auto const& item : values) {
        if (!builder.is_empty())
            builder.append(' ');

        builder.append(TRY(item.to_string(vm)));
    }

    return MUST(builder.to_string());
}

ThrowCompletionOr<String> Console::format_time_since(Core::ElapsedTimer timer)
{
    auto& vm = realm().vm();

    auto elapsed_ms = timer.elapsed_time().to_milliseconds();
    auto duration = TRY(Temporal::balance_duration(vm, 0, 0, 0, 0, elapsed_ms, 0, "0"_sbigint, "year"sv));

    auto append = [&](auto& builder, auto format, auto number) {
        if (!builder.is_empty())
            builder.append(' ');
        builder.appendff(format, number);
    };

    StringBuilder builder;

    if (duration.days > 0)
        append(builder, "{:.0} day(s)"sv, duration.days);
    if (duration.hours > 0)
        append(builder, "{:.0} hour(s)"sv, duration.hours);
    if (duration.minutes > 0)
        append(builder, "{:.0} minute(s)"sv, duration.minutes);
    if (duration.seconds > 0 || duration.milliseconds > 0) {
        double combined_seconds = duration.seconds + (0.001 * duration.milliseconds);
        append(builder, "{:.3} seconds"sv, combined_seconds);
    }

    return MUST(builder.to_string());
}

ConsoleClient::ConsoleClient(Console& console)
    : m_console(console)
{
}

ConsoleClient::~ConsoleClient() = default;

void ConsoleClient::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_console);
}

// 2.1. Logger(logLevel, args), https://console.spec.whatwg.org/#logger
ThrowCompletionOr<Value> ConsoleClient::logger(Console::LogLevel log_level, MarkedVector<Value> const& args)
{
    auto& vm = m_console->realm().vm();

    // 1. If args is empty, return.
    if (args.is_empty())
        return js_undefined();

    // 2. Let first be args[0].
    auto first = args[0];

    // 3. Let rest be all elements following first in args.
    size_t rest_size = args.size() - 1;

    // 4. If rest is empty, perform Printer(logLevel, « first ») and return.
    if (rest_size == 0) {
        MarkedVector<Value> first_as_vector { vm.heap() };
        first_as_vector.append(first);
        return printer(log_level, move(first_as_vector));
    }

    // 5. Otherwise, perform Printer(logLevel, Formatter(args)).
    else {
        auto formatted = TRY(formatter(args));
        TRY(printer(log_level, formatted));
    }

    // 6. Return undefined.
    return js_undefined();
}

// 2.2. Formatter(args), https://console.spec.whatwg.org/#formatter
ThrowCompletionOr<MarkedVector<Value>> ConsoleClient::formatter(MarkedVector<Value> const& args)
{
    auto& realm = m_console->realm();
    auto& vm = realm.vm();

    // 1. If args’s size is 1, return args.
    if (args.size() == 1)
        return args;

    // 2. Let target be the first element of args.
    auto target = (!args.is_empty()) ? TRY(args.first().to_string(vm)) : String {};

    // 3. Let current be the second element of args.
    auto current = (args.size() > 1) ? args[1] : js_undefined();

    // 4. Find the first possible format specifier specifier, from the left to the right in target.
    auto find_specifier = [](StringView target) -> Optional<StringView> {
        size_t start_index = 0;
        while (start_index < target.length()) {
            auto maybe_index = target.find('%');
            if (!maybe_index.has_value())
                return {};

            auto index = maybe_index.value();
            if (index + 1 >= target.length())
                return {};

            switch (target[index + 1]) {
            case 'c':
            case 'd':
            case 'f':
            case 'i':
            case 'o':
            case 'O':
            case 's':
                return target.substring_view(index, 2);
            }

            start_index = index + 1;
        }
        return {};
    };
    auto maybe_specifier = find_specifier(target);

    // 5. If no format specifier was found, return args.
    if (!maybe_specifier.has_value()) {
        return args;
    }
    // 6. Otherwise:
    else {
        auto specifier = maybe_specifier.release_value();
        Optional<Value> converted;

        // 1. If specifier is %s, let converted be the result of Call(%String%, undefined, « current »).
        if (specifier == "%s"sv) {
            converted = TRY(call(vm, *realm.intrinsics().string_constructor(), js_undefined(), current));
        }
        // 2. If specifier is %d or %i:
        else if (specifier.is_one_of("%d"sv, "%i"sv)) {
            // 1. If Type(current) is Symbol, let converted be NaN
            if (current.is_symbol()) {
                converted = js_nan();
            }
            // 2. Otherwise, let converted be the result of Call(%parseInt%, undefined, « current, 10 »).
            else {
                converted = TRY(call(vm, *realm.intrinsics().parse_int_function(), js_undefined(), current, Value { 10 }));
            }
        }
        // 3. If specifier is %f:
        else if (specifier == "%f"sv) {
            // 1. If Type(current) is Symbol, let converted be NaN
            if (current.is_symbol()) {
                converted = js_nan();
            }
            // 2. Otherwise, let converted be the result of Call(% parseFloat %, undefined, « current »).
            else {
                converted = TRY(call(vm, *realm.intrinsics().parse_float_function(), js_undefined(), current));
            }
        }
        // 4. If specifier is %o, optionally let converted be current with optimally useful formatting applied.
        else if (specifier == "%o"sv) {
            // TODO: "Optimally-useful formatting"
            converted = current;
        }
        // 5. If specifier is %O, optionally let converted be current with generic JavaScript object formatting applied.
        else if (specifier == "%O"sv) {
            // TODO: "generic JavaScript object formatting"
            converted = current;
        }
        // 6. TODO: process %c
        else if (specifier == "%c"sv) {
            // NOTE: This has no spec yet. `%c` specifiers treat the argument as CSS styling for the log message.
            add_css_style_to_current_message(TRY(current.to_string(vm)));
            converted = PrimitiveString::create(vm, String {});
        }

        // 7. If any of the previous steps set converted, replace specifier in target with converted.
        if (converted.has_value())
            target = TRY_OR_THROW_OOM(vm, target.replace(specifier, TRY(converted->to_string(vm)), ReplaceMode::FirstOnly));
    }

    // 7. Let result be a list containing target together with the elements of args starting from the third onward.
    MarkedVector<Value> result { vm.heap() };
    result.ensure_capacity(args.size() - 1);
    result.empend(PrimitiveString::create(vm, move(target)));
    for (size_t i = 2; i < args.size(); ++i)
        result.unchecked_append(args[i]);

    // 8. Return Formatter(result).
    return formatter(result);
}

ThrowCompletionOr<String> ConsoleClient::generically_format_values(MarkedVector<Value> const& values)
{
    AllocatingMemoryStream stream;
    auto& vm = m_console->realm().vm();
    PrintContext ctx { vm, stream, true };
    bool first = true;
    for (auto const& value : values) {
        if (!first)
            TRY_OR_THROW_OOM(vm, stream.write_until_depleted(" "sv.bytes()));
        TRY_OR_THROW_OOM(vm, JS::print(value, ctx));
        first = false;
    }
    // FIXME: Is it possible we could end up serializing objects to invalid UTF-8?
    return TRY_OR_THROW_OOM(vm, String::from_stream(stream, stream.used_buffer_size()));
}

}
