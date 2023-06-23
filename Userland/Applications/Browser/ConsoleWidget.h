/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "History.h"
#include <LibGUI/TextEditor.h>
#include <LibGUI/Widget.h>
#include <LibWebView/OutOfProcessWebView.h>

namespace Browser {

class ConsoleWidget final : public GUI::Widget {
    C_OBJECT(ConsoleWidget)
public:
    virtual ~ConsoleWidget() = default;

    void notify_about_new_console_message(i32 message_index);
    void handle_console_messages(i32 start_index, Vector<DeprecatedString> const& message_types, Vector<DeprecatedString> const& messages);
    void print_source_line(StringView);
    void print_html(StringView);
    void reset();

    void show_debugger_view(StringView source_url, StringView source);
    void hide_debugger_view();
    void set_active_source_line(size_t line);
    void clear_gutter_markers();
    bool debugger_view_is_visible() const { return m_debugger_view->is_visible(); }

    Function<void(DeprecatedString const&)> on_js_input;
    Function<void(i32)> on_request_messages;
    Function<void()> on_debug_continue;
    Function<void()> on_debug_next;
    Function<void(size_t, bool)> on_breakpoint_change;

private:
    ConsoleWidget();

    void request_console_messages();
    void clear_output();
    void begin_group(StringView label, bool start_expanded);
    void end_group();

    RefPtr<GUI::Widget> m_console_view;
    RefPtr<GUI::Widget> m_debugger_view;
    RefPtr<GUI::TextBox> m_input;
    RefPtr<WebView::OutOfProcessWebView> m_output_view;
    RefPtr<GUI::TextEditor> m_source_viewer;
    RefPtr<GUI::ToolbarContainer> m_toolbar_container;

    GUI::TextEditor::GutterIndicatorID m_breakpoint_indicator_id { 0 };
    GUI::TextEditor::GutterIndicatorID m_active_line_indicator_id { 0 };

    size_t m_current_source_line { 0 };
    HashTable<size_t> m_active_breakpoints;

    i32 m_highest_notified_message_index { -1 };
    i32 m_highest_received_message_index { -1 };
    bool m_waiting_for_messages { false };

    struct Group {
        int id { 0 };
        DeprecatedString label;
    };
    Vector<Group> m_group_stack;
    int m_next_group_id { 1 };
};

}
