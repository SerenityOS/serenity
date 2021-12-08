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
#include <LibJS/Parser.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <WebContent/ConsoleGlobalObject.h>

namespace WebContent {

WebContentConsoleClient::WebContentConsoleClient(JS::Console& console, WeakPtr<JS::Interpreter> interpreter, ClientConnection& client)
    : ConsoleClient(console)
    , m_client(client)
    , m_interpreter(interpreter)
{
    JS::DeferGC defer_gc(m_interpreter->heap());
    auto console_global_object = m_interpreter->heap().allocate_without_global_object<ConsoleGlobalObject>(static_cast<Web::Bindings::WindowObject&>(m_interpreter->global_object()));
    console_global_object->initialize_global_object();
    m_console_global_object = JS::make_handle(console_global_object);
}

void WebContentConsoleClient::handle_input(String const& js_source)
{
    auto parser = JS::Parser(JS::Lexer(js_source));
    auto program = parser.parse_program();

    StringBuilder output_html;
    if (parser.has_errors()) {
        auto error = parser.errors()[0];
        auto hint = error.source_location_hint(js_source);
        if (!hint.is_empty())
            output_html.append(String::formatted("<pre>{}</pre>", escape_html_entities(hint)));
        m_interpreter->vm().throw_exception<JS::SyntaxError>(*m_console_global_object.cell(), error.to_string());
    } else {
        // FIXME: This is not the correct way to do this, we probably want to have
        //        multiple execution contexts we switch between.
        auto& global_object_before = m_interpreter->realm().global_object();
        VERIFY(is<Web::Bindings::WindowObject>(global_object_before));
        auto& this_value_before = m_interpreter->realm().global_environment().global_this_value();
        m_interpreter->realm().set_global_object(*m_console_global_object.cell(), &global_object_before);

        m_interpreter->run(*m_console_global_object.cell(), *program);

        m_interpreter->realm().set_global_object(global_object_before, &this_value_before);
    }

    if (m_interpreter->exception()) {
        auto* exception = m_interpreter->exception();
        m_interpreter->vm().clear_exception();
        output_html.append("Uncaught exception: ");
        auto error = exception->value();
        if (error.is_object())
            output_html.append(JS::MarkupGenerator::html_from_error(error.as_object()));
        else
            output_html.append(JS::MarkupGenerator::html_from_value(error));
        print_html(output_html.string_view());
        return;
    }

    print_html(JS::MarkupGenerator::html_from_value(m_interpreter->vm().last_value()));
}

void WebContentConsoleClient::print_html(String const& line)
{
    m_message_log.append({ .type = ConsoleOutput::Type::HTML, .html = line });
    m_client.async_did_output_js_console_message(m_message_log.size() - 1);
}

void WebContentConsoleClient::clear_output()
{
    m_message_log.append({ .type = ConsoleOutput::Type::Clear, .html = "" });
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
        }

        messages.append(message.html);
    }

    m_client.async_did_get_js_console_messages(start_index, message_types, messages);
}

void WebContentConsoleClient::clear()
{
    clear_output();
}

JS::Value WebContentConsoleClient::trace()
{
    StringBuilder html;
    html.append(escape_html_entities(vm().join_arguments()));
    auto trace = get_trace();
    for (auto& function_name : trace) {
        if (function_name.is_empty())
            function_name = "&lt;anonymous&gt;";
        html.appendff(" -> {}<br>", function_name);
    }
    print_html(html.string_view());
    return JS::js_undefined();
}

JS::Value WebContentConsoleClient::assert_()
{
    auto& vm = this->vm();
    if (!vm.argument(0).to_boolean()) {
        StringBuilder html;
        if (vm.argument_count() > 1) {
            html.append("<span class=\"error\">");
            html.append("Assertion failed:");
            html.append("</span>");
            html.append(" ");
            html.append(escape_html_entities(vm.join_arguments(1)));
        } else {
            html.append("<span class=\"error\">");
            html.append("Assertion failed");
            html.append("</span>");
        }
        print_html(html.string_view());
    }
    return JS::js_undefined();
}

// 2.3. Printer(logLevel, args[, options]), https://console.spec.whatwg.org/#printer
JS::ThrowCompletionOr<JS::Value> WebContentConsoleClient::printer(JS::Console::LogLevel log_level, Vector<JS::Value>& arguments)
{
    auto output = String::join(" ", arguments);
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
