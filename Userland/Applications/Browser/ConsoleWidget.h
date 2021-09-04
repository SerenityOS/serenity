/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

    void handle_js_console_output(String const& method, String const& line);
    void print_source_line(StringView const&);
    void print_html(StringView const&);
    void clear_output();

    Function<void(String const&)> on_js_input;

private:
    ConsoleWidget();

    RefPtr<GUI::TextBox> m_input;
    RefPtr<Web::OutOfProcessWebView> m_output_view;
};

}
