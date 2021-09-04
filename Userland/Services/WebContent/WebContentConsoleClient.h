/*
 * Copyright (c) 2021, Brandon Scott <xeon.productions@gmail.com>
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
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
    WebContentConsoleClient(JS::Console& console, WeakPtr<JS::Interpreter> interpreter, ClientConnection& client)
        : ConsoleClient(console)
        , m_client(client)
        , m_interpreter(interpreter)
    {
    }

    void handle_input(String const& js_source);

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
    void clear_output();
    void print_html(String const& line);
};

}
