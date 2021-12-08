/*
 * Copyright (c) 2020, Emanuele Torre <torreemanuele6@gmail.com>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
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

Value Console::assert_()
{
    if (m_client)
        return m_client->assert_();
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
