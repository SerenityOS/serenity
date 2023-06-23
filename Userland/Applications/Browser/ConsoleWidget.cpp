/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConsoleWidget.h"
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <Applications/Browser/Browser.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Painter.h>
#include <LibJS/MarkupGenerator.h>
#include <LibJS/SyntaxHighlighter.h>
#include <LibWeb/HTML/SyntaxHighlighter/SyntaxHighlighter.h>

static constexpr auto arrow_right_emoji = "\xE2\x9E\xA1"sv;
static constexpr auto red_circle_emoji = "\xF0\x9F\x94\xB4"sv;

namespace Browser {

ConsoleWidget::ConsoleWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    set_fill_with_background_color(true);

    auto& splitter = add<GUI::HorizontalSplitter>();
    m_debugger_view = splitter.add<GUI::Widget>();
    m_console_view = splitter.add<GUI::Widget>();

    m_console_view->set_layout<GUI::VerticalBoxLayout>();

    m_output_view = m_console_view->add<WebView::OutOfProcessWebView>();
    m_output_view->load("data:text/html,<html style=\"font: 10pt monospace;\"></html>"sv);
    // Wait until our output WebView is loaded, and then request any messages that occurred before we existed
    m_output_view->on_load_finish = [this](auto&) {
        if (on_request_messages)
            on_request_messages(0);
    };

    auto& bottom_container = m_console_view->add<GUI::Widget>();
    bottom_container.set_layout<GUI::HorizontalBoxLayout>();
    bottom_container.set_fixed_height(22);

    m_input = bottom_container.add<GUI::TextBox>();
    m_input->set_syntax_highlighter(make<JS::SyntaxHighlighter>());
    // FIXME: Syntax Highlighting breaks the cursor's position on non fixed-width fonts.
    m_input->set_font(Gfx::FontDatabase::default_fixed_width_font());
    m_input->set_history_enabled(true);

    m_input->on_return_pressed = [this] {
        auto js_source = m_input->text();

        if (js_source.is_whitespace())
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

    m_debugger_view->set_layout<GUI::VerticalBoxLayout>();

    m_toolbar_container = m_debugger_view->add<GUI::ToolbarContainer>();
    m_source_viewer = m_debugger_view->add<GUI::TextEditor>();
    m_source_viewer->set_mode(GUI::TextEditor::ReadOnly);
    m_source_viewer->set_gutter_visible(true);
    m_source_viewer->set_ruler_visible(true);

    m_breakpoint_indicator_id = MUST(m_source_viewer->register_gutter_indicator(
        [this](auto& painter, Gfx::IntRect rect, size_t line) {
            if (m_active_breakpoints.contains(line))
                painter.draw_text(rect, red_circle_emoji, font(), Gfx::TextAlignment::Center, palette().color(Gfx::ColorRole::Selection));
        }));

    m_active_line_indicator_id = MUST(m_source_viewer->register_gutter_indicator(
        [this](auto& painter, Gfx::IntRect rect, size_t line) {
            if (line == m_current_source_line)
                painter.draw_text(rect, arrow_right_emoji, font(), Gfx::TextAlignment::Center, palette().color(Gfx::ColorRole::Selection));
        }));

    m_source_viewer->on_gutter_click = [this](size_t line, unsigned) {
        m_active_breakpoints.set(line);
        m_source_viewer->add_gutter_indicator(m_breakpoint_indicator_id, line);
        if (on_breakpoint_change)
            on_breakpoint_change(line, true);
    };

    auto& toolbar = m_toolbar_container->add<GUI::Toolbar>();
    toolbar.add_action(GUI::Action::create("Continue",
        MUST(Gfx::Bitmap::load_from_file("/res/icons/16x16/debug-continue.png"sv)),
        [&](auto&) {
            if (on_debug_continue)
                on_debug_continue();
        }));

    toolbar.add_action(GUI::Action::create("Step Over",
        MUST(Gfx::Bitmap::load_from_file("/res/icons/16x16/debug-step-over.png"sv)),
        [&](auto&) {
            if (on_debug_next)
                on_debug_next();
        }));

    m_debugger_view->set_visible(false);
}

void ConsoleWidget::clear_gutter_markers()
{
    m_source_viewer->clear_gutter_indicators(m_breakpoint_indicator_id);
    m_source_viewer->clear_gutter_indicators(m_active_line_indicator_id);
}

void ConsoleWidget::set_active_source_line(size_t line)
{
    m_current_source_line = line;
    m_source_viewer->clear_gutter_indicators(m_active_line_indicator_id);
    m_source_viewer->add_gutter_indicator(m_active_line_indicator_id, line);
    m_source_viewer->set_cursor(line, 1);
}

void ConsoleWidget::hide_debugger_view()
{
    m_debugger_view->set_visible(false);
}

void ConsoleWidget::show_debugger_view(StringView source_url, StringView source)
{
    auto url = URL(source_url);
    if (url.is_valid()) {
        auto path = LexicalPath(url.path_segment_at_index(url.path_segment_count() - 1));
        auto extension = path.extension();
        if (extension == "js"sv)
            m_source_viewer->set_syntax_highlighter(make<JS::SyntaxHighlighter>());
        else if (extension == "html"sv)
            m_source_viewer->set_syntax_highlighter(make<Web::HTML::SyntaxHighlighter>());
        else
            m_source_viewer->set_syntax_highlighter(nullptr);
    }

    m_source_viewer->set_text(source);
    m_debugger_view->set_visible(true);
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
