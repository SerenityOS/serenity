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
#include <AK/OwnPtr.h>
#include <LibWebView/Forward.h>
#include <QLineEdit>
#include <QWidget>

namespace Ladybird {

class WebContentView;

class ConsoleWidget final : public QWidget {
    Q_OBJECT
public:
    explicit ConsoleWidget(WebContentView& content_view);
    virtual ~ConsoleWidget();

    Optional<String> previous_history_item();
    Optional<String> next_history_item();

    WebView::ConsoleClient& client() { return *m_console_client; }
    WebContentView& view() { return *m_output_view; }

    void reset();

private:
    OwnPtr<WebView::ConsoleClient> m_console_client;

    WebContentView* m_output_view { nullptr };
    QLineEdit* m_input { nullptr };
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
};

}
