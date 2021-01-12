/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibGUI/Widget.h>
#include <LibJS/Console.h>
#include <LibJS/Forward.h>
#include <LibWeb/InProcessWebView.h>

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

    ConsoleWidget& m_console_widget;
};

}
