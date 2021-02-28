/*
 * Copyright (c) 2021, Brandon Scott <xeon.productions@gmail.com>
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "WebContentConsoleClient.h"
#include <LibJS/Console.h>
#include <LibJS/Interpreter.h>
#include <LibJS/MarkupGenerator.h>
#include <LibJS/Parser.h>
#include <LibWeb/Bindings/DOMExceptionWrapper.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/DOMTreeModel.h>

namespace WebContent {

void WebContentConsoleClient::handle_input(const String& js_source)
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
        output_html.append("Uncaught exception: ");
        auto error = m_interpreter->exception()->value();
        if (error.is_object() && is<Web::Bindings::DOMExceptionWrapper>(error.as_object())) {
            auto& dom_exception_wrapper = static_cast<Web::Bindings::DOMExceptionWrapper&>(error.as_object());
            error = JS::Error::create(m_interpreter->global_object(), dom_exception_wrapper.impl().name(), dom_exception_wrapper.impl().message());
        }
        output_html.append(JS::MarkupGenerator::html_from_value(error));
        print_html(output_html.string_view());

        m_interpreter->vm().clear_exception();
        return;
    }

    print_html(JS::MarkupGenerator::html_from_value(m_interpreter->vm().last_value()));
}

void WebContentConsoleClient::print_html(const String& line)
{
    m_client.post_message(Messages::WebContentClient::DidJSConsoleOutput("html", line));
}

void WebContentConsoleClient::clear_output()
{
    m_client.post_message(Messages::WebContentClient::DidJSConsoleOutput("clear_output", {}));
}

JS::Value WebContentConsoleClient::log()
{
    print_html(vm().join_arguments());
    return JS::js_undefined();
}

JS::Value WebContentConsoleClient::info()
{
    StringBuilder html;
    html.append("<span class=\"info\">");
    html.append("(i) ");
    html.append(vm().join_arguments());
    html.append("</span>");
    print_html(html.string_view());
    return JS::js_undefined();
}

JS::Value WebContentConsoleClient::debug()
{
    StringBuilder html;
    html.append("<span class=\"debug\">");
    html.append("(d) ");
    html.append(vm().join_arguments());
    html.append("</span>");
    print_html(html.string_view());
    return JS::js_undefined();
}

JS::Value WebContentConsoleClient::warn()
{
    StringBuilder html;
    html.append("<span class=\"warn\">");
    html.append("(w) ");
    html.append(vm().join_arguments());
    html.append("</span>");
    print_html(html.string_view());
    return JS::js_undefined();
}

JS::Value WebContentConsoleClient::error()
{
    StringBuilder html;
    html.append("<span class=\"error\">");
    html.append("(e) ");
    html.append(vm().join_arguments());
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
    html.append(vm().join_arguments());
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

}
