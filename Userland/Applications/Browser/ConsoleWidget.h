/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "History.h"
#include <LibGUI/Widget.h>
#include <LibWeb/OutOfProcessWebView.h>

namespace Browser {

class ConsoleWidget final : public GUI::Widget {
    C_OBJECT(ConsoleWidget)
public:
    virtual ~ConsoleWidget();

    void notify_about_new_console_message(i32 message_index);
    void handle_console_messages(i32 start_index, Vector<String> const& message_types, Vector<String> const& messages);
    void print_source_line(StringView);
    void print_html(StringView);
    void reset();

    Function<void(const String&)> on_js_input;
    Function<void(i32)> on_request_messages;

private:
    ConsoleWidget();

    void request_console_messages();
    void clear_output();
    void begin_group(StringView label, bool start_expanded);
    void end_group();

    RefPtr<GUI::TextBox> m_input;
    RefPtr<Web::OutOfProcessWebView> m_output_view;

    i32 m_highest_notified_message_index { -1 };
    i32 m_highest_received_message_index { -1 };
    bool m_waiting_for_messages { false };

    struct Group {
        int id { 0 };
        String label;
    };
    Vector<Group> m_group_stack;
    int m_next_group_id { 1 };
};

}
