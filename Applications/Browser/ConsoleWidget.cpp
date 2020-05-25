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

#include "ConsoleWidget.h"
#include <AK/StringBuilder.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/JSSyntaxHighlighter.h>
#include <LibGUI/TextBox.h>
#include <LibJS/Interpreter.h>
#include <LibJS/MarkupGenerator.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Error.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/HTMLBodyElement.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/DOMTreeModel.h>

namespace Browser {

ConsoleWidget::ConsoleWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    set_fill_with_background_color(true);

    auto base_document = adopt(*new Web::Document);
    base_document->append_child(adopt(*new Web::DocumentType(base_document)));
    auto html_element = create_element(base_document, "html");
    base_document->append_child(html_element);
    auto head_element = create_element(base_document, "head");
    html_element->append_child(head_element);
    auto body_element = create_element(base_document, "body");
    html_element->append_child(body_element);
    m_console_output_container = body_element;

    m_console_output_view = add<Web::HtmlView>();
    m_console_output_view->set_document(base_document);

    m_console_input = add<GUI::TextBox>();
    m_console_input->set_syntax_highlighter(make<GUI::JSSyntaxHighlighter>());
    m_console_input->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_console_input->set_preferred_size(0, 22);
    // FIXME: Syntax Highlighting breaks the cursor's position on non fixed-width fonts.
    m_console_input->set_font(Gfx::Font::default_fixed_width_font());

    m_console_input->on_return_pressed = [this] {
        auto js_source = m_console_input->text();

        // FIXME: An is_blank check to check if there is only whitespace would probably be preferable.
        if (js_source.is_empty())
            return;

        m_console_input->clear();
        print_source_line(js_source);

        auto parser = JS::Parser(JS::Lexer(js_source));
        auto program = parser.parse_program();

        if (parser.has_errors()) {
            auto error = parser.errors()[0];
            m_interpreter->throw_exception<JS::SyntaxError>(error.to_string());
        } else {
            m_interpreter->run(*program);
        }

        if (m_interpreter->exception()) {
            StringBuilder output_html;
            output_html.append("Uncaught exception: ");
            output_html.append(JS::MarkupGenerator::html_from_value(m_interpreter->exception()->value()));
            print_html(output_html.string_view());

            m_interpreter->clear_exception();
            return;
        }

        print_html(JS::MarkupGenerator::html_from_value(m_interpreter->last_value()));
    };
}

ConsoleWidget::~ConsoleWidget()
{
}

void ConsoleWidget::set_interpreter(WeakPtr<JS::Interpreter> interpreter)
{
    if (m_interpreter.ptr() == interpreter.ptr())
        return;

    m_interpreter = interpreter;
    m_console_client = adopt_own(*new BrowserConsoleClient(interpreter->console(), *this));
    interpreter->console().set_client(*m_console_client.ptr());

    clear_output();
}

void ConsoleWidget::print_source_line(const StringView& source)
{
    StringBuilder html;
    html.append("<span class=\"repl-indicator\">");
    html.append("&gt; ");
    html.append("</span>");

    html.append(JS::MarkupGenerator::html_from_source(source));

    print_html(html.string_view());
}

void ConsoleWidget::print_html(const StringView& line)
{
    auto paragraph = create_element(m_console_output_container->document(), "p");
    paragraph->set_inner_html(line);

    m_console_output_container->append_child(paragraph);
    m_console_output_container->document().invalidate_layout();
    m_console_output_container->document().update_layout();

    m_console_output_view->scroll_to_bottom();
}

void ConsoleWidget::clear_output()
{
    const_cast<Web::HTMLBodyElement*>(m_console_output_view->document()->body())->remove_all_children();
}

}
