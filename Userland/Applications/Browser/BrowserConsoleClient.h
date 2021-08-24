/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>
#include <LibJS/Console.h>
#include <LibJS/Forward.h>

namespace Browser {

class ConsoleWidget;

class BrowserConsoleClient final : public JS::ConsoleClient {
public:
    BrowserConsoleClient(JS::Console& console, ConsoleWidget& console_widget)
        : ConsoleClient(console)
        , m_console_widget(console_widget)
    {
    }

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

    ConsoleWidget& m_console_widget;
};

}
