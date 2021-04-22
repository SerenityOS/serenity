/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BrowserConsoleClient.h"
#include "History.h"
#include <LibGUI/Widget.h>
#include <LibJS/Forward.h>
#include <LibWeb/InProcessWebView.h>

namespace Browser {

class ConsoleWidget final : public GUI::Widget {
    C_OBJECT(ConsoleWidget)
public:
    virtual ~ConsoleWidget();

    void set_interpreter(WeakPtr<JS::Interpreter>);
    void handle_js_console_output(const String& method, const String& line);
    void print_source_line(const StringView&);
    void print_html(const StringView&);
    void clear_output();

    Function<void(const String&)> on_js_input;

private:
    ConsoleWidget();

    RefPtr<GUI::TextBox> m_input;
    RefPtr<Web::InProcessWebView> m_output_view;
    RefPtr<Web::DOM::Element> m_output_container;
    WeakPtr<JS::Interpreter> m_interpreter;
    OwnPtr<BrowserConsoleClient> m_console_client;
};

}
