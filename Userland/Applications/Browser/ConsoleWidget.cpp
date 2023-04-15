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
#include <LibGfx/Font/FontDatabase.h>
#include <LibJS/MarkupGenerator.h>
#include <LibJS/SyntaxHighlighter.h>

namespace Browser {

ErrorOr<NonnullRefPtr<ConsoleWidget>> ConsoleWidget::try_create()
{
    auto main_widget = TRY(AK::adopt_nonnull_ref_or_enomem(new (nothrow) ConsoleWidget()));

    main_widget->m_output_view = TRY(main_widget->try_add<WebView::OutOfProcessWebView>());
    main_widget->m_output_view->load("data:text/html,<html style=\"font: 10pt monospace;\"></html>"sv);
    // Wait until our output WebView is loaded, and then request any messages that occurred before we existed
    main_widget->m_output_view->on_load_finish = [main_widget](auto&) {
        if (main_widget->on_request_messages)
            main_widget->on_request_messages(0);
    };

    auto bottom_container = TRY(main_widget->try_add<GUI::Widget>());
    TRY(bottom_container->try_set_layout<GUI::HorizontalBoxLayout>());
    bottom_container->set_fixed_height(22);

    main_widget->m_input = TRY(bottom_container->try_add<GUI::TextBox>());
    main_widget->m_input->set_syntax_highlighter(TRY(try_make<JS::SyntaxHighlighter>()));
    // FIXME: Syntax Highlighting breaks the cursor's position on non fixed-width fonts.
    main_widget->m_input->set_font(Gfx::FontDatabase::default_fixed_width_font());
    main_widget->m_input->set_history_enabled(true);

    main_widget->m_input->on_return_pressed = [main_widget] {
        auto js_source = main_widget->m_input->text();

        if (js_source.is_whitespace())
            return;

        main_widget->m_input->add_current_text_to_history();
        main_widget->m_input->clear();

        main_widget->print_source_line(js_source);

        if (main_widget->on_js_input)
            main_widget->on_js_input(js_source);
    };

    main_widget->set_focus_proxy(main_widget->m_input);

    auto clear_button = TRY(bottom_container->try_add<GUI::Button>());
    clear_button->set_fixed_size(22, 22);
    clear_button->set_icon(g_icon_bag.delete_icon);
    clear_button->set_tooltip("Clear the console output");
    clear_button->on_click = [main_widget](auto) {
        main_widget->clear_output();
    };

    return main_widget;
}

ConsoleWidget::ConsoleWidget()
{
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

void ConsoleWidget::handle_console_messages(i32 start_index, Vector<DeprecatedString> const& message_types, Vector<DeprecatedString> const& messages)
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
    html.append("<span class=\"repl-indicator\">"sv);
    html.append("&gt; "sv);
    html.append("</span>"sv);

    html.append(JS::MarkupGenerator::html_from_source(source).release_value_but_fixme_should_propagate_errors());

    print_html(html.string_view());
}

void ConsoleWidget::print_html(StringView line)
{
    StringBuilder builder;

    int parent_id = m_group_stack.is_empty() ? 0 : m_group_stack.last().id;
    if (parent_id == 0) {
        builder.append(R"~~~(
        var parentGroup = document.body;
)~~~"sv);
    } else {
        builder.appendff(R"~~~(
        var parentGroup = document.getElementById("group_{}");
)~~~",
            parent_id);
    }

    builder.append(R"~~~(
        var p = document.createElement("p");
        p.innerHTML = ")~~~"sv);
    builder.append_escaped_for_json(line);
    builder.append(R"~~~("
        parentGroup.appendChild(p);
)~~~"sv);
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
    )~~~"sv);
}

void ConsoleWidget::begin_group(StringView label, bool start_expanded)
{
    StringBuilder builder;
    int parent_id = m_group_stack.is_empty() ? 0 : m_group_stack.last().id;
    if (parent_id == 0) {
        builder.append(R"~~~(
        var parentGroup = document.body;
)~~~"sv);
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
        label.innerHTML = ")~~~",
        group.id);
    builder.append_escaped_for_json(label);
    builder.append(R"~~~(";
        group.appendChild(label);
        parentGroup.appendChild(group);
)~~~"sv);

    if (start_expanded)
        builder.append("group.open = true;"sv);

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
