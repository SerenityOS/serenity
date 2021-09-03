/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrowserConsoleClient.h"
#include "ConsoleWidget.h"
#include <YAK/StringBuilder.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/TextBox.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/DOMTreeModel.h>

namespace Browser {

JS::Value BrowserConsoleClient::log()
{
    m_console_widget.print_html(escape_html_entities(vm().join_arguments()));
    return JS::js_undefined();
}

JS::Value BrowserConsoleClient::info()
{
    StringBuilder html;
    html.append("<span class=\"info\">");
    html.append("(i) ");
    html.append(escape_html_entities(vm().join_arguments()));
    html.append("</span>");
    m_console_widget.print_html(html.string_view());
    return JS::js_undefined();
}

JS::Value BrowserConsoleClient::debug()
{
    StringBuilder html;
    html.append("<span class=\"debug\">");
    html.append("(d) ");
    html.append(escape_html_entities(vm().join_arguments()));
    html.append("</span>");
    m_console_widget.print_html(html.string_view());
    return JS::js_undefined();
}

JS::Value BrowserConsoleClient::warn()
{
    StringBuilder html;
    html.append("<span class=\"warn\">");
    html.append("(w) ");
    html.append(escape_html_entities(vm().join_arguments()));
    html.append("</span>");
    m_console_widget.print_html(html.string_view());
    return JS::js_undefined();
}

JS::Value BrowserConsoleClient::error()
{
    StringBuilder html;
    html.append("<span class=\"error\">");
    html.append("(e) ");
    html.append(escape_html_entities(vm().join_arguments()));
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
    html.append(escape_html_entities(vm().join_arguments()));
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

JS::Value BrowserConsoleClient::assert_()
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
        m_console_widget.print_html(html.string_view());
    }
    return JS::js_undefined();
}

}
