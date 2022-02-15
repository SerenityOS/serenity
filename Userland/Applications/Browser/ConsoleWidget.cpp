/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConsoleWidget.h"
#include <AK/StringBuilder.h>
#include <Applications/Browser/Browser.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/FontDatabase.h>
#include <LibJS/MarkupGenerator.h>
#include <LibJS/SyntaxHighlighter.h>

namespace Browser {

ConsoleWidget::ConsoleWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    set_fill_with_background_color(true);

    m_output_view = add<Web::OutOfProcessWebView>();
    m_output_view->load("data:text/html,<html></html>");
    // Wait until our output WebView is loaded, and then request any messages that occurred before we existed
    m_output_view->on_load_finish = [this](auto&) {
        if (on_request_messages)
            on_request_messages(0);
    };

    auto& bottom_container = add<GUI::Widget>();
    bottom_container.set_layout<GUI::HorizontalBoxLayout>();
    bottom_container.set_fixed_height(22);

    m_input = bottom_container.add<GUI::TextBox>();
    m_input->set_syntax_highlighter(make<JS::SyntaxHighlighter>());
    // FIXME: Syntax Highlighting breaks the cursor's position on non fixed-width fonts.
    m_input->set_font(Gfx::FontDatabase::default_fixed_width_font());
    m_input->set_history_enabled(true);

    m_input->on_return_pressed = [this] {
        auto js_source = m_input->text();

        // FIXME: An is_blank check to check if there is only whitespace would probably be preferable.
        if (js_source.is_empty())
            return;

        m_input->add_current_text_to_history();
        m_input->clear();

        print_source_line(js_source);

        if (on_js_input)
            on_js_input(js_source);
    };

    set_focus_proxy(m_input);

    auto& clear_button = bottom_container.add<GUI::Button>();
    clear_button.set_fixed_size(22, 22);
    clear_button.set_icon(g_icon_bag.delete_icon);
    clear_button.set_tooltip("Clear the console output");
    clear_button.on_click = [this](auto) {
        clear_output();
    };
}

void ConsoleWidget::request_console_messages()
{
    VERIFY(!m_waiting_for_messages);
    VERIFY(on_request_messages);
    on_request_messages(m_highest_received_message_index + 1);
    m_waiting_for_messages = true;
}

void ConsoleWidget::notify_about_new_console_message(i32 message_index)
{
    if (message_index <= m_highest_received_message_index) {
        dbgln("Notified about console message we already have");
        return;
    }
    if (message_index <= m_highest_notified_message_index) {
        dbgln("Notified about console message we're already aware of");
        return;
    }

    m_highest_notified_message_index = message_index;
    if (!m_waiting_for_messages)
        request_console_messages();
}

void ConsoleWidget::handle_console_messages(i32 start_index, const Vector<String>& message_types, const Vector<String>& messages)
{
    i32 end_index = start_index + message_types.size() - 1;
    if (end_index <= m_highest_received_message_index) {
        dbgln("Received old console messages");
        return;
    }

    for (size_t i = 0; i < message_types.size(); i++) {
        auto& type = message_types[i];
        auto& message = messages[i];

        if (type == "html") {
            print_html(message);
        } else if (type == "clear") {
            clear_output();
        } else if (type == "group") {
            begin_group(message, true);
        } else if (type == "groupCollapsed") {
            begin_group(message, false);
        } else if (type == "groupEnd") {
            end_group();
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    m_highest_received_message_index = end_index;
    m_waiting_for_messages = false;

    if (m_highest_received_message_index < m_highest_notified_message_index)
        request_console_messages();
}

void ConsoleWidget::print_source_line(StringView source)
{
    StringBuilder html;
    html.append("<span class=\"repl-indicator\">");
    html.append("&gt; ");
    html.append("</span>");

    html.append(JS::MarkupGenerator::html_from_source(source));

    print_html(html.string_view());
}

void ConsoleWidget::print_html(StringView line)
{
    StringBuilder builder;

    int parent_id = m_group_stack.is_empty() ? 0 : m_group_stack.last().id;
    if (parent_id == 0) {
        builder.append(R"~~~(
        var parentGroup = document.body;
)~~~");
    } else {
        builder.appendff(R"~~~(
        var parentGroup = document.getElementById("group_{}");
)~~~",
            parent_id);
    }

    builder.append(R"~~~(
        var p = document.createElement("p");
        p.innerHTML = ")~~~");
    builder.append_escaped_for_json(line);
    builder.append(R"~~~("
        parentGroup.appendChild(p);
)~~~");
    m_output_view->run_javascript(builder.string_view());
    // FIXME: Make it scroll to the bottom, using `window.scrollTo()` in the JS above.
    //        We used to call `m_output_view->scroll_to_bottom();` here, but that does not work because
    //        it runs synchronously, meaning it happens before the HTML is output via IPC above.
    //        (See also: begin_group())
}

void ConsoleWidget::clear_output()
{
    m_group_stack.clear();
    m_output_view->run_javascript(R"~~~(
        document.body.innerHTML = "";
    )~~~");
}

void ConsoleWidget::begin_group(StringView label, bool start_expanded)
{
    StringBuilder builder;
    int parent_id = m_group_stack.is_empty() ? 0 : m_group_stack.last().id;
    if (parent_id == 0) {
        builder.append(R"~~~(
        var parentGroup = document.body;
)~~~");
    } else {
        builder.appendff(R"~~~(
        var parentGroup = document.getElementById("group_{}");
)~~~",
            parent_id);
    }

    Group group;
    group.id = m_next_group_id++;
    group.label = label;

    builder.appendff(R"~~~(
        var group = document.createElement("details");
        group.id = "group_{}";
        var label = document.createElement("summary");
        label.innerText = ")~~~",
        group.id);
    builder.append_escaped_for_json(label);
    builder.append(R"~~~(";
        group.appendChild(label);
        parentGroup.appendChild(group);
)~~~");

    if (start_expanded)
        builder.append("group.open = true;");

    m_output_view->run_javascript(builder.string_view());
    // FIXME: Scroll console to bottom - see note in print_html()
    m_group_stack.append(group);
}

void ConsoleWidget::end_group()
{
    m_group_stack.take_last();
}

void ConsoleWidget::reset()
{
    clear_output();
    m_highest_notified_message_index = -1;
    m_highest_received_message_index = -1;
    m_waiting_for_messages = false;
}

}
