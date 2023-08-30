/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Function.h>
#include <AK/Span.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibWebView/Forward.h>

namespace WebView {

class ConsoleClient {
public:
    explicit ConsoleClient(ViewImplementation& content_web_view, ViewImplementation& console_web_view);
    ~ConsoleClient();

    void execute(StringView);

    void clear();
    void reset();

private:
    void handle_console_message(i32 message_index);
    void handle_console_messages(i32 start_index, ReadonlySpan<DeprecatedString> message_types, ReadonlySpan<DeprecatedString> messages);

    void print_source(StringView);
    void print_html(StringView);

    void request_console_messages();

    void begin_group(StringView label, bool start_expanded);
    void end_group();

    ViewImplementation& m_content_web_view;
    ViewImplementation& m_console_web_view;

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
