/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Function.h>
#include <AK/Vector.h>
#include <QLineEdit>
#include <QWidget>

class QLineEdit;
namespace Ladybird {

class WebContentView;

class ConsoleWidget final : public QWidget {
    Q_OBJECT
public:
    ConsoleWidget();
    virtual ~ConsoleWidget() = default;

    void notify_about_new_console_message(i32 message_index);
    void handle_console_messages(i32 start_index, Vector<DeprecatedString> const& message_types, Vector<DeprecatedString> const& messages);
    void print_source_line(StringView);
    void print_html(StringView);
    void reset();

    WebContentView& view() { return *m_output_view; }

    Function<void(DeprecatedString const&)> on_js_input;
    Function<void(i32)> on_request_messages;

private:
    void request_console_messages();
    void clear_output();

    WebContentView* m_output_view { nullptr };
    QLineEdit* m_input { nullptr };

    i32 m_highest_notified_message_index { -1 };
    i32 m_highest_received_message_index { -1 };
    bool m_waiting_for_messages { false };
};

class ConsoleInputEdit final : public QLineEdit {
    Q_OBJECT
public:
    ConsoleInputEdit(QWidget* q_widget, ConsoleWidget& console_widget)
        : QLineEdit(q_widget)
        , m_console_widget(console_widget)
    {
    }

private:
    virtual void keyPressEvent(QKeyEvent* event) override;

    ConsoleWidget& m_console_widget;
    Vector<DeprecatedString> m_history;
    size_t m_history_index { 0 };
};

}
