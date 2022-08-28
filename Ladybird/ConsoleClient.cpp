/*
 * Copyright (c) 2021, Brandon Scott <xeon.productions@gmail.com>
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConsoleClient.h"
#include "ConsoleGlobalObject.h"
#include "WebView.h"
#include <LibJS/Interpreter.h>
#include <LibJS/MarkupGenerator.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Scripting/Environments.h>

namespace Ladybird {

ConsoleClient::ConsoleClient(JS::Console& console, WeakPtr<JS::Interpreter> interpreter, WebView& view)
    : JS::ConsoleClient(console)
    , m_view(view)
    , m_interpreter(interpreter)
{
    JS::DeferGC defer_gc(m_interpreter->heap());

    auto& vm = m_interpreter->vm();
    auto& realm = m_interpreter->realm();
    auto& window = static_cast<Web::Bindings::WindowObject&>(realm.global_object());

    auto console_global_object = m_interpreter->heap().allocate_without_realm<ConsoleGlobalObject>(realm, window);

    // NOTE: We need to push an execution context here for NativeFunction::create() to succeed during global object initialization.
    // It gets removed immediately after creating the interpreter in Document::interpreter().
    auto& eso = verify_cast<Web::HTML::EnvironmentSettingsObject>(*m_interpreter->realm().host_defined());
    vm.push_execution_context(eso.realm_execution_context());
    console_global_object->initialize(realm);
    vm.pop_execution_context();

    m_console_global_object = JS::make_handle(console_global_object);
}

void ConsoleClient::handle_input(String const& js_source)
{
    auto& settings = verify_cast<Web::HTML::EnvironmentSettingsObject>(*m_interpreter->realm().host_defined());
    auto script = Web::HTML::ClassicScript::create("(console)", js_source, settings, settings.api_base_url());

    // FIXME: Add parse error printouts back once ClassicScript can report parse errors.

    auto result = script->run();

    StringBuilder output_html;

    if (result.is_abrupt()) {
        output_html.append("Uncaught exception: "sv);
        auto error = *result.release_error().value();
        if (error.is_object())
            output_html.append(JS::MarkupGenerator::html_from_error(error.as_object()));
        else
            output_html.append(JS::MarkupGenerator::html_from_value(error));
        print_html(output_html.string_view());
        return;
    }

    if (result.value().has_value())
        print_html(JS::MarkupGenerator::html_from_value(*result.value()));
}

void ConsoleClient::print_html(String const& line)
{
    m_message_log.append({ .type = ConsoleOutput::Type::HTML, .data = line });
    m_view.did_output_js_console_message(m_message_log.size() - 1);
}

void ConsoleClient::clear_output()
{
    m_message_log.append({ .type = ConsoleOutput::Type::Clear, .data = "" });
    m_view.did_output_js_console_message(m_message_log.size() - 1);
}

void ConsoleClient::begin_group(String const& label, bool start_expanded)
{
    m_message_log.append({ .type = start_expanded ? ConsoleOutput::Type::BeginGroup : ConsoleOutput::Type::BeginGroupCollapsed, .data = label });
    m_view.did_output_js_console_message(m_message_log.size() - 1);
}

void ConsoleClient::end_group()
{
    m_message_log.append({ .type = ConsoleOutput::Type::EndGroup, .data = "" });
    m_view.did_output_js_console_message(m_message_log.size() - 1);
}

void ConsoleClient::send_messages(i32 start_index)
{
    // FIXME: Cap the number of messages we send at once?
    auto messages_to_send = m_message_log.size() - start_index;
    if (messages_to_send < 1) {
        // When the console is first created, it requests any messages that happened before
        // then, by requesting with start_index=0. If we don't have any messages at all, that
        // is still a valid request, and we can just ignore it.
        return;
    }

    // FIXME: Replace with a single Vector of message structs
    Vector<String> message_types;
    Vector<String> messages;
    message_types.ensure_capacity(messages_to_send);
    messages.ensure_capacity(messages_to_send);

    for (size_t i = start_index; i < m_message_log.size(); i++) {
        auto& message = m_message_log[i];
        switch (message.type) {
        case ConsoleOutput::Type::HTML:
            message_types.append("html"sv);
            break;
        case ConsoleOutput::Type::Clear:
            message_types.append("clear"sv);
            break;
        case ConsoleOutput::Type::BeginGroup:
            message_types.append("group"sv);
            break;
        case ConsoleOutput::Type::BeginGroupCollapsed:
            message_types.append("groupCollapsed"sv);
            break;
        case ConsoleOutput::Type::EndGroup:
            message_types.append("groupEnd"sv);
            break;
        }

        messages.append(message.data);
    }

    m_view.did_get_js_console_messages(start_index, message_types, messages);
}

void ConsoleClient::clear()
{
    clear_output();
}

// 2.3. Printer(logLevel, args[, options]), https://console.spec.whatwg.org/#printer
JS::ThrowCompletionOr<JS::Value> ConsoleClient::printer(JS::Console::LogLevel log_level, PrinterArguments arguments)
{
    if (log_level == JS::Console::LogLevel::Trace) {
        auto trace = arguments.get<JS::Console::Trace>();
        StringBuilder html;
        if (!trace.label.is_empty())
            html.appendff("<span class='title'>{}</span><br>", escape_html_entities(trace.label));

        html.append("<span class='trace'>"sv);
        for (auto& function_name : trace.stack)
            html.appendff("-> {}<br>", escape_html_entities(function_name));
        html.append("</span>"sv);

        print_html(html.string_view());
        return JS::js_undefined();
    }

    if (log_level == JS::Console::LogLevel::Group || log_level == JS::Console::LogLevel::GroupCollapsed) {
        auto group = arguments.get<JS::Console::Group>();
        begin_group(group.label, log_level == JS::Console::LogLevel::Group);
        return JS::js_undefined();
    }

    auto output = String::join(' ', arguments.get<JS::MarkedVector<JS::Value>>());
    m_console.output_debug_message(log_level, output);

    StringBuilder html;
    switch (log_level) {
    case JS::Console::LogLevel::Debug:
        html.append("<span class=\"debug\">(d) "sv);
        break;
    case JS::Console::LogLevel::Error:
        html.append("<span class=\"error\">(e) "sv);
        break;
    case JS::Console::LogLevel::Info:
        html.append("<span class=\"info\">(i) "sv);
        break;
    case JS::Console::LogLevel::Log:
        html.append("<span class=\"log\"> "sv);
        break;
    case JS::Console::LogLevel::Warn:
    case JS::Console::LogLevel::CountReset:
        html.append("<span class=\"warn\">(w) "sv);
        break;
    default:
        html.append("<span>"sv);
        break;
    }

    html.append(escape_html_entities(output));
    html.append("</span>"sv);
    print_html(html.string_view());
    return JS::js_undefined();
}
}
