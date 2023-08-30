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
#include <LibWebView/ConsoleClient.h>
#include <QFontDatabase>
#include <QKeyEvent>
#include <QLineEdit>
#include <QPushButton>
#include <QVBoxLayout>

namespace Ladybird {

bool is_using_dark_system_theme(QWidget&);

ConsoleWidget::ConsoleWidget(WebContentView& content_view)
{
    setLayout(new QVBoxLayout);

    m_output_view = new WebContentView({}, WebView::EnableCallgrindProfiling::No, UseLagomNetworking::No);
    if (is_using_dark_system_theme(*this))
        m_output_view->update_palette(WebContentView::PaletteMode::Dark);

    m_console_client = make<WebView::ConsoleClient>(content_view, *m_output_view);

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
        client().clear();
    });

    m_input->setFocus();
}

ConsoleWidget::~ConsoleWidget() = default;

Optional<String> ConsoleWidget::previous_history_item()
{
    return m_console_client->previous_history_item();
}

Optional<String> ConsoleWidget::next_history_item()
{
    return m_console_client->next_history_item();
}

void ConsoleWidget::reset()
{
    m_console_client->reset();
}

void ConsoleInputEdit::keyPressEvent(QKeyEvent* event)
{
    switch (event->key()) {
    case Qt::Key_Up:
        if (auto script = m_console_widget.previous_history_item(); script.has_value())
            setText(qstring_from_ak_string(*script));
        break;

    case Qt::Key_Down:
        if (auto script = m_console_widget.next_history_item(); script.has_value())
            setText(qstring_from_ak_string(*script));
        break;

    case Qt::Key_Return: {
        auto js_source = MUST(ak_string_from_qstring(text()));
        if (js_source.bytes_as_string_view().is_whitespace())
            return;

        m_console_widget.client().execute(std::move(js_source));
        clear();

        break;
    }

    default:
        QLineEdit::keyPressEvent(event);
    }
}

}
