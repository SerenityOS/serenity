/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <QWidget>

class QLineEdit;
class QTextEdit;

namespace Ladybird {

class ConsoleWidget final : public QWidget {
    Q_OBJECT
public:
    ConsoleWidget();
    virtual ~ConsoleWidget() = default;

    void notify_about_new_console_message(i32 message_index);
    void handle_console_messages(i32 start_index, Vector<String> const& message_types, Vector<String> const& messages);
    void print_source_line(StringView);
    void print_html(StringView);
    void reset();

    Function<void(String const&)> on_js_input;
    Function<void(i32)> on_request_messages;

private:
    void request_console_messages();
    void clear_output();

    QTextEdit* m_output_view { nullptr };
    QLineEdit* m_input { nullptr };

    i32 m_highest_notified_message_index { -1 };
    i32 m_highest_received_message_index { -1 };
    bool m_waiting_for_messages { false };
};

}
