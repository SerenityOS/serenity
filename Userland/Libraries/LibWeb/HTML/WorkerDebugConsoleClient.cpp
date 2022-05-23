/*
 * Copyright (c) 2022, Ben Abraham <ben.d.abraham@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/MarkedVector.h>
#include <LibJS/Runtime/Completion.h>
#include <LibWeb/HTML/WorkerDebugConsoleClient.h>

namespace Web::HTML {

WorkerDebugConsoleClient::WorkerDebugConsoleClient(JS::Console& console)
    : ConsoleClient(console)
{
}

void WorkerDebugConsoleClient::clear()
{
    dbgln("\033[3J\033[H\033[2J");
    m_group_stack_depth = 0;
    fflush(stdout);
}

void WorkerDebugConsoleClient::end_group()
{
    if (m_group_stack_depth > 0)
        m_group_stack_depth--;
}

// 2.3. Printer(logLevel, args[, options]), https://console.spec.whatwg.org/#printer
JS::ThrowCompletionOr<JS::Value> WorkerDebugConsoleClient::printer(JS::Console::LogLevel log_level, PrinterArguments arguments)
{
    String indent = String::repeated("  ", m_group_stack_depth);

    if (log_level == JS::Console::LogLevel::Trace) {
        auto trace = arguments.get<JS::Console::Trace>();
        StringBuilder builder;
        if (!trace.label.is_empty())
            builder.appendff("{}\033[36;1m{}\033[0m\n", indent, trace.label);

        for (auto& function_name : trace.stack)
            builder.appendff("{}-> {}\n", indent, function_name);

        dbgln("{}", builder.string_view());
        return JS::js_undefined();
    }

    if (log_level == JS::Console::LogLevel::Group || log_level == JS::Console::LogLevel::GroupCollapsed) {
        auto group = arguments.get<JS::Console::Group>();
        dbgln("{}\033[36;1m{}\033[0m", indent, group.label);
        m_group_stack_depth++;
        return JS::js_undefined();
    }

    auto output = String::join(" ", arguments.get<JS::MarkedVector<JS::Value>>());
    m_console.output_debug_message(log_level, output);

    switch (log_level) {
    case JS::Console::LogLevel::Debug:
        dbgln("{}\033[36;1m{}\033[0m", indent, output);
        break;
    case JS::Console::LogLevel::Error:
    case JS::Console::LogLevel::Assert:
        dbgln("{}\033[31;1m{}\033[0m", indent, output);
        break;
    case JS::Console::LogLevel::Info:
        dbgln("{}(i) {}", indent, output);
        break;
    case JS::Console::LogLevel::Log:
        dbgln("{}{}", indent, output);
        break;
    case JS::Console::LogLevel::Warn:
    case JS::Console::LogLevel::CountReset:
        dbgln("{}\033[33;1m{}\033[0m", indent, output);
        break;
    default:
        dbgln("{}{}", indent, output);
        break;
    }
    return JS::js_undefined();
}

} // namespace Web::HTML
