/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibGUI/Widget.h>
#include <LibWebView/Forward.h>

namespace Browser {

class ConsoleWidget final : public GUI::Widget {
    C_OBJECT(ConsoleWidget)
public:
    virtual ~ConsoleWidget();

    void reset();

private:
    explicit ConsoleWidget(WebView::OutOfProcessWebView& content_view);

    void request_console_messages();
    void clear_output();

    OwnPtr<WebView::ConsoleClient> m_console_client;

    RefPtr<GUI::TextBox> m_input;
    RefPtr<WebView::OutOfProcessWebView> m_output_view;
};

}
