/*
 * Copyright (c) 2021, Brandon Scott <xeon.productions@gmail.com>
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebContentConsoleClient.h"
#include <LibJS/Interpreter.h>
#include <LibJS/MarkupGenerator.h>
#include <LibJS/Parser.h>
#include <LibWeb/Bindings/DOMExceptionWrapper.h>

namespace WebContent {

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
        m_interpreter->vm().throw_exception<JS::SyntaxError>(m_interpreter->global_object(), error.to_string());
    } else {
        m_interpreter->run(m_interpreter->global_object(), *program);
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
    m_client.async_did_js_console_output("html", line);
}

void WebContentConsoleClient::clear_output()
{
    m_client.async_did_js_console_output("clear_output", {});
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
