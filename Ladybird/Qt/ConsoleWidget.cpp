/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConsoleWidget.h"
#include "StringUtils.h"
#include "WebContentView.h"
#include <AK/StringBuilder.h>
#include <LibJS/MarkupGenerator.h>
#include <QFontDatabase>
#include <QKeyEvent>
#include <QLineEdit>
#include <QPalette>
#include <QPushButton>
#include <QTextEdit>
#include <QVBoxLayout>

namespace Ladybird {

bool is_using_dark_system_theme(QWidget&);

ConsoleWidget::ConsoleWidget()
{
    setLayout(new QVBoxLayout);

    m_output_view = new WebContentView({}, WebView::EnableCallgrindProfiling::No, UseLagomNetworking::No);
    m_output_view->use_native_user_style_sheet();
    if (is_using_dark_system_theme(*this))
        m_output_view->update_palette(WebContentView::PaletteMode::Dark);

    m_output_view->load("data:text/html,<html style=\"font: 10pt monospace;\"></html>"sv);
    // Wait until our output WebView is loaded, and then request any messages that occurred before we existed
    m_output_view->on_load_finish = [this](auto&) {
        if (on_request_messages)
            on_request_messages(0);
    };

    layout()->addWidget(m_output_view);

    auto* bottom_container = new QWidget(this);
    bottom_container->setLayout(new QHBoxLayout);

    layout()->addWidget(bottom_container);

    m_input = new ConsoleInputEdit(bottom_container, *this);
    m_input->setFont(QFontDatabase::systemFont(QFontDatabase::FixedFont));
    bottom_container->layout()->addWidget(m_input);

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

    html.append(JS::MarkupGenerator::html_from_source(source).release_value_but_fixme_should_propagate_errors());

    print_html(html.string_view());
}

void ConsoleWidget::print_html(StringView line)
{
    StringBuilder builder;

    builder.append(R"~~~(
        var p = document.createElement("p");
        p.innerHTML = ")~~~"sv);
    builder.append_escaped_for_json(line);
    builder.append(R"~~~("
        document.body.appendChild(p);
)~~~"sv);

    // FIXME: It should be sufficient to scrollTo a y value of document.documentElement.offsetHeight,
    // but due to an unknown bug offsetHeight seems to not be properly updated after spamming
    // a lot of document changes.
    //
    // The setTimeout makes the scrollTo async and allows the DOM to be updated.
    builder.append("setTimeout(function() { window.scrollTo(0, 1_000_000_000); }, 0);"sv);

    m_output_view->run_javascript(builder.string_view());
}

void ConsoleWidget::clear_output()
{
    m_output_view->run_javascript(R"~~~(
        document.body.innerHTML = "";
    )~~~"sv);
}

void ConsoleWidget::reset()
{
    clear_output();
    m_highest_notified_message_index = -1;
    m_highest_received_message_index = -1;
    m_waiting_for_messages = false;
}

void ConsoleInputEdit::keyPressEvent(QKeyEvent* event)
{
    switch (event->key()) {
    case Qt::Key_Down: {
        if (m_history.is_empty())
            break;
        auto last_index = m_history.size() - 1;
        if (m_history_index < last_index) {
            m_history_index++;
            setText(qstring_from_ak_deprecated_string(m_history.at(m_history_index)));
        } else if (m_history_index == last_index) {
            m_history_index++;
            clear();
        }
        break;
    }
    case Qt::Key_Up:
        if (m_history_index > 0) {
            m_history_index--;
            setText(qstring_from_ak_deprecated_string(m_history.at(m_history_index)));
        }
        break;
    case Qt::Key_Return: {
        auto js_source = ak_deprecated_string_from_qstring(text());
        if (js_source.is_whitespace())
            return;

        if (m_history.is_empty() || m_history.last() != js_source) {
            m_history.append(js_source);
            m_history_index = m_history.size();
        }

        clear();

        m_console_widget.print_source_line(js_source);

        if (m_console_widget.on_js_input)
            m_console_widget.on_js_input(js_source);

        break;
    }
    default:
        QLineEdit::keyPressEvent(event);
    }
}

}
