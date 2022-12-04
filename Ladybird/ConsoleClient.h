/*
 * Copyright (c) 2022, Brandon Scott <xeon.productions@gmail.com>
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define AK_DONT_REPLACE_STD

#include "ConsoleGlobalObject.h"
#include <AK/WeakPtr.h>
#include <LibJS/Console.h>
#include <LibJS/Forward.h>
#include <LibWeb/Forward.h>

class SimpleWebView;

namespace Ladybird {

class ConsoleClient final : public JS::ConsoleClient {
public:
    ConsoleClient(JS::Console&, JS::Realm&, SimpleWebView&);

    void handle_input(DeprecatedString const& js_source);
    void send_messages(i32 start_index);

private:
    virtual void clear() override;
    virtual JS::ThrowCompletionOr<JS::Value> printer(JS::Console::LogLevel log_level, PrinterArguments) override;

    virtual void add_css_style_to_current_message(StringView style) override
    {
        m_current_message_style.append(style);
        m_current_message_style.append(';');
    }

    SimpleWebView& m_view;

    WeakPtr<JS::Interpreter> m_interpreter;
    JS::Handle<ConsoleGlobalObject> m_console_global_object;

    void clear_output();
    void print_html(DeprecatedString const& line);
    void begin_group(DeprecatedString const& label, bool start_expanded);
    virtual void end_group() override;

    struct ConsoleOutput {
        enum class Type {
            HTML,
            Clear,
            BeginGroup,
            BeginGroupCollapsed,
            EndGroup,
        };
        Type type;
        DeprecatedString data;
    };
    Vector<ConsoleOutput> m_message_log;

    StringBuilder m_current_message_style;

    WeakPtr<JS::Realm> m_realm;
};

}
