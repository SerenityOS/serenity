/*
 * Copyright (c) 2021, Brandon Scott <xeon.productions@gmail.com>
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebContentConsoleClient.h"
#include <LibJS/Interpreter.h>
#include <LibJS/MarkupGenerator.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/HTML/Scripting/ClassicScript.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <WebContent/ConsoleGlobalObject.h>

namespace WebContent {

WebContentConsoleClient::WebContentConsoleClient(JS::Console& console, WeakPtr<JS::Interpreter> interpreter, ConnectionFromClient& client)
    : ConsoleClient(console)
    , m_client(client)
    , m_interpreter(interpreter)
{
    JS::DeferGC defer_gc(m_interpreter->heap());

    auto& vm = m_interpreter->vm();
    auto& global_object = m_interpreter->global_object();

    auto console_global_object = m_interpreter->heap().allocate_without_global_object<ConsoleGlobalObject>(static_cast<Web::Bindings::WindowObject&>(global_object));

    // NOTE: We need to push an execution context here for NativeFunction::create() to succeed during global object initialization.
    // It gets removed immediately after creating the interpreter in Document::interpreter().
    auto& eso = verify_cast<Web::HTML::EnvironmentSettingsObject>(*m_interpreter->realm().host_defined());
    vm.push_execution_context(eso.realm_execution_context(), global_object);
    console_global_object->initialize_global_object();
    vm.pop_execution_context();

    m_console_global_object = JS::make_handle(console_global_object);
}

void WebContentConsoleClient::handle_input(String const& js_source)
{
    auto& settings = verify_cast<Web::HTML::EnvironmentSettingsObject>(*m_interpreter->realm().host_defined());
    auto script = Web::HTML::ClassicScript::create("(console)", js_source, settings, settings.api_base_url());

    // FIXME: Add parse error printouts back once ClassicScript can report parse errors.

    auto result = script->run();

    StringBuilder output_html;

    if (result.is_abrupt()) {
        output_html.append("Uncaught exception: ");
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

void WebContentConsoleClient::print_html(String const& line)
{
    m_message_log.append({ .type = ConsoleOutput::Type::HTML, .data = line });
    m_client.async_did_output_js_console_message(m_message_log.size() - 1);
}

void WebContentConsoleClient::clear_output()
{
    m_message_log.append({ .type = ConsoleOutput::Type::Clear, .data = "" });
    m_client.async_did_output_js_console_message(m_message_log.size() - 1);
}

void WebContentConsoleClient::begin_group(String const& label, bool start_expanded)
{
    m_message_log.append({ .type = start_expanded ? ConsoleOutput::Type::BeginGroup : ConsoleOutput::Type::BeginGroupCollapsed, .data = label });
    m_client.async_did_output_js_console_message(m_message_log.size() - 1);
}

void WebContentConsoleClient::end_group()
{
    m_message_log.append({ .type = ConsoleOutput::Type::EndGroup, .data = "" });
    m_client.async_did_output_js_console_message(m_message_log.size() - 1);
}

void WebContentConsoleClient::send_messages(i32 start_index)
{
    // FIXME: Cap the number of messages we send at once?
    auto messages_to_send = m_message_log.size() - start_index;
    if (messages_to_send < 1) {
        // When the console is first created, it requests any messages that happened before
        // then, by requesting with start_index=0. If we don't have any messages at all, that
        // is still a valid request, and we can just ignore it.
        if (start_index != 0)
            m_client.did_misbehave("Requested non-existent console message index.");
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
            message_types.append("html");
            break;
        case ConsoleOutput::Type::Clear:
            message_types.append("clear");
            break;
        case ConsoleOutput::Type::BeginGroup:
            message_types.append("group");
            break;
        case ConsoleOutput::Type::BeginGroupCollapsed:
            message_types.append("groupCollapsed");
            break;
        case ConsoleOutput::Type::EndGroup:
            message_types.append("groupEnd");
            break;
        }

        messages.append(message.data);
    }

    m_client.async_did_get_js_console_messages(start_index, message_types, messages);
}

void WebContentConsoleClient::clear()
{
    clear_output();
}

// 2.3. Printer(logLevel, args[, options]), https://console.spec.whatwg.org/#printer
JS::ThrowCompletionOr<JS::Value> WebContentConsoleClient::printer(JS::Console::LogLevel log_level, PrinterArguments arguments)
{
    if (log_level == JS::Console::LogLevel::Trace) {
        auto trace = arguments.get<JS::Console::Trace>();
        StringBuilder html;
        if (!trace.label.is_empty())
            html.appendff("<span class='title'>{}</span><br>", escape_html_entities(trace.label));

        html.append("<span class='trace'>");
        for (auto& function_name : trace.stack)
            html.appendff("-> {}<br>", escape_html_entities(function_name));
        html.append("</span>");

        print_html(html.string_view());
        return JS::js_undefined();
    }

    if (log_level == JS::Console::LogLevel::Group || log_level == JS::Console::LogLevel::GroupCollapsed) {
        auto group = arguments.get<JS::Console::Group>();
        begin_group(group.label, log_level == JS::Console::LogLevel::Group);
        return JS::js_undefined();
    }

    auto output = String::join(" ", arguments.get<Vector<JS::Value>>());
    m_console.output_debug_message(log_level, output);

    StringBuilder html;
    switch (log_level) {
    case JS::Console::LogLevel::Debug:
        html.append("<span class=\"debug\">(d) ");
        break;
    case JS::Console::LogLevel::Error:
        html.append("<span class=\"error\">(e) ");
        break;
    case JS::Console::LogLevel::Info:
        html.append("<span class=\"info\">(i) ");
        break;
    case JS::Console::LogLevel::Log:
        html.append("<span class=\"log\"> ");
        break;
    case JS::Console::LogLevel::Warn:
    case JS::Console::LogLevel::CountReset:
        html.append("<span class=\"warn\">(w) ");
        break;
    default:
        html.append("<span>");
        break;
    }

    html.append(escape_html_entities(output));
    html.append("</span>");
    print_html(html.string_view());
    return JS::js_undefined();
}
}
