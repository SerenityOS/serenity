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

JS::Value WebContentConsoleClient::log()
{
    print_html(escape_html_entities(vm().join_arguments()));
    return JS::js_undefined();
}

JS::Value WebContentConsoleClient::info()
{
    StringBuilder html;
    html.append("<span class=\"info\">");
    html.append("(i) ");
    html.append(escape_html_entities(vm().join_arguments()));
    html.append("</span>");
    print_html(html.string_view());
    return JS::js_undefined();
}

JS::Value WebContentConsoleClient::debug()
{
    StringBuilder html;
    html.append("<span class=\"debug\">");
    html.append("(d) ");
    html.append(escape_html_entities(vm().join_arguments()));
    html.append("</span>");
    print_html(html.string_view());
    return JS::js_undefined();
}

JS::Value WebContentConsoleClient::warn()
{
    StringBuilder html;
    html.append("<span class=\"warn\">");
    html.append("(w) ");
    html.append(escape_html_entities(vm().join_arguments()));
    html.append("</span>");
    print_html(html.string_view());
    return JS::js_undefined();
}

JS::Value WebContentConsoleClient::error()
{
    StringBuilder html;
    html.append("<span class=\"error\">");
    html.append("(e) ");
    html.append(escape_html_entities(vm().join_arguments()));
    html.append("</span>");
    print_html(html.string_view());
    return JS::js_undefined();
}

JS::Value WebContentConsoleClient::clear()
{
    clear_output();
    return JS::js_undefined();
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

JS::Value WebContentConsoleClient::count()
{
    auto label = vm().argument_count() ? vm().argument(0).to_string_without_side_effects() : "default";
    auto counter_value = m_console.counter_increment(label);
    print_html(String::formatted("{}: {}", label, counter_value));
    return JS::js_undefined();
}

JS::Value WebContentConsoleClient::count_reset()
{
    auto label = vm().argument_count() ? vm().argument(0).to_string_without_side_effects() : "default";
    if (m_console.counter_reset(label)) {
        print_html(String::formatted("{}: 0", label));
    } else {
        print_html(String::formatted("\"{}\" doesn't have a count", label));
    }
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

}
