/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define AK_DONT_REPLACE_STD

#include "ConsoleWidget.h"
#include "Utilities.h"
#include <AK/StringBuilder.h>
#include <LibJS/MarkupGenerator.h>
#include <QLineEdit>
#include <QPushButton>
#include <QTextEdit>
#include <QVBoxLayout>

namespace Ladybird {

ConsoleWidget::ConsoleWidget()
{
    setLayout(new QVBoxLayout);

    m_output_view = new QTextEdit(this);
    m_output_view->setReadOnly(true);
    layout()->addWidget(m_output_view);

    if (on_request_messages)
        on_request_messages(0);

    auto* bottom_container = new QWidget(this);
    bottom_container->setLayout(new QHBoxLayout);

    layout()->addWidget(bottom_container);

    m_input = new QLineEdit(bottom_container);
    bottom_container->layout()->addWidget(m_input);

    QObject::connect(m_input, &QLineEdit::returnPressed, [this] {
        auto js_source = ak_deprecated_string_from_qstring(m_input->text());

        if (js_source.is_whitespace())
            return;

        m_input->clear();

        print_source_line(js_source);

        if (on_js_input)
            on_js_input(js_source);
    });

    setFocusProxy(m_input);

    auto* clear_button = new QPushButton(bottom_container);
    bottom_container->layout()->addWidget(clear_button);
    clear_button->setFixedSize(22, 22);
    clear_button->setText("X");
    clear_button->setToolTip("Clear the console output");
    QObject::connect(clear_button, &QPushButton::pressed, [this] {
        clear_output();
    });

    m_input->setFocus();
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
            // FIXME: Implement.
        } else if (type == "groupCollapsed") {
            // FIXME: Implement.
        } else if (type == "groupEnd") {
            // FIXME: Implement.
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

    html.append(JS::MarkupGenerator::html_from_source(source));

    print_html(html.string_view());
}

void ConsoleWidget::print_html(StringView line)
{
    m_output_view->append(QString::fromUtf8(line.characters_without_null_termination(), line.length()));
}

void ConsoleWidget::clear_output()
{
    m_output_view->clear();
}

void ConsoleWidget::reset()
{
    clear_output();
    m_highest_notified_message_index = -1;
    m_highest_received_message_index = -1;
    m_waiting_for_messages = false;
}

}
