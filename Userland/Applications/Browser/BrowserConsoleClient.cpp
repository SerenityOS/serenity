/*
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

#include "BrowserConsoleClient.h"
#include "ConsoleWidget.h"
#include <AK/StringBuilder.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/JSSyntaxHighlighter.h>
#include <LibGUI/TextBox.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/DOMTreeModel.h>
#include <LibWeb/HTML/HTMLBodyElement.h>

namespace Browser {

JS::Value BrowserConsoleClient::log()
{
    m_console_widget.print_html(vm().join_arguments());
    return JS::js_undefined();
}

JS::Value BrowserConsoleClient::info()
{
    StringBuilder html;
    html.append("<span class=\"info\">");
    html.append("(i) ");
    html.append(vm().join_arguments());
    html.append("</span>");
    m_console_widget.print_html(html.string_view());
    return JS::js_undefined();
}

JS::Value BrowserConsoleClient::debug()
{
    StringBuilder html;
    html.append("<span class=\"debug\">");
    html.append("(d) ");
    html.append(vm().join_arguments());
    html.append("</span>");
    m_console_widget.print_html(html.string_view());
    return JS::js_undefined();
}

JS::Value BrowserConsoleClient::warn()
{
    StringBuilder html;
    html.append("<span class=\"warn\">");
    html.append("(w) ");
    html.append(vm().join_arguments());
    html.append("</span>");
    m_console_widget.print_html(html.string_view());
    return JS::js_undefined();
}

JS::Value BrowserConsoleClient::error()
{
    StringBuilder html;
    html.append("<span class=\"error\">");
    html.append("(e) ");
    html.append(vm().join_arguments());
    html.append("</span>");
    m_console_widget.print_html(html.string_view());
    return JS::js_undefined();
}

JS::Value BrowserConsoleClient::clear()
{
    m_console_widget.clear_output();
    return JS::js_undefined();
}

JS::Value BrowserConsoleClient::trace()
{
    StringBuilder html;
    html.append(vm().join_arguments());
    auto trace = get_trace();
    for (auto& function_name : trace) {
        if (function_name.is_empty())
            function_name = "&lt;anonymous&gt;";
        html.appendff(" -> {}<br>", function_name);
    }
    m_console_widget.print_html(html.string_view());
    return JS::js_undefined();
}

JS::Value BrowserConsoleClient::count()
{
    auto label = vm().argument_count() ? vm().argument(0).to_string_without_side_effects() : "default";
    auto counter_value = m_console.counter_increment(label);
    m_console_widget.print_html(String::formatted("{}: {}", label, counter_value));
    return JS::js_undefined();
}

JS::Value BrowserConsoleClient::count_reset()
{
    auto label = vm().argument_count() ? vm().argument(0).to_string_without_side_effects() : "default";
    if (m_console.counter_reset(label)) {
        m_console_widget.print_html(String::formatted("{}: 0", label));
    } else {
        m_console_widget.print_html(String::formatted("\"{}\" doesn't have a count", label));
    }
    return JS::js_undefined();
}

}
