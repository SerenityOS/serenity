/*
 * Copyright (c) 2021, Brandon Scott <xeon.productions@gmail.com>
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ClientConnection.h"
#include <LibJS/Console.h>
#include <LibJS/Forward.h>
#include <LibWeb/Forward.h>
#include <WebContent/Forward.h>

namespace WebContent {

class WebContentConsoleClient final : public JS::ConsoleClient {
public:
    WebContentConsoleClient(JS::Console&, WeakPtr<JS::Interpreter>, ClientConnection&);

    void handle_input(String const& js_source);
    void send_messages(i32 start_index);

private:
    virtual JS::Value log() override;
    virtual JS::Value info() override;
    virtual JS::Value debug() override;
    virtual JS::Value warn() override;
    virtual JS::Value error() override;
    virtual JS::Value clear() override;
    virtual JS::Value trace() override;
    virtual JS::Value count() override;
    virtual JS::Value count_reset() override;
    virtual JS::Value assert_() override;

    ClientConnection& m_client;
    WeakPtr<JS::Interpreter> m_interpreter;
    JS::Handle<ConsoleGlobalObject> m_console_global_object;

    void clear_output();
    void print_html(String const& line);

    struct ConsoleOutput {
        enum class Type {
            HTML,
            Clear
        };
        Type type;
        String html;
    };
    Vector<ConsoleOutput> m_message_log;
};

}
