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

#include "BrowserConsoleClient.h"
#include <LibGUI/Widget.h>
#include <LibJS/Forward.h>
#include <LibWeb/HtmlView.h>

namespace Browser {

class ConsoleWidget final : public GUI::Widget {
    C_OBJECT(ConsoleWidget)
public:
    virtual ~ConsoleWidget();

    void set_interpreter(WeakPtr<JS::Interpreter>);
    void print_source_line(const StringView&);
    void print_html(const StringView&);
    void clear_output();

private:
    ConsoleWidget();

    void print_value(JS::Value, StringBuilder& output_html, HashTable<JS::Object*> seen_objects = {});
    void print_array(const JS::Array&, StringBuilder& output_html, HashTable<JS::Object*>&);
    void print_object(const JS::Object&, StringBuilder& output_html, HashTable<JS::Object*>&);
    void print_function(const JS::Object&, StringBuilder& output_html, HashTable<JS::Object*>&);
    void print_date(const JS::Object&, StringBuilder& output_html, HashTable<JS::Object*>&);
    void print_error(const JS::Object&, StringBuilder& output_html, HashTable<JS::Object*>&);

    RefPtr<GUI::TextBox> m_console_input;
    RefPtr<Web::HtmlView> m_console_output_view;
    RefPtr<Web::Element> m_console_output_container;
    WeakPtr<JS::Interpreter> m_interpreter;
    OwnPtr<BrowserConsoleClient> m_console_client;
};

}
