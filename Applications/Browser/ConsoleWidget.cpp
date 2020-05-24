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
#include <LibJS/Parser.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Date.h>
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
    auto style_element = create_element(base_document, "style");
    style_element->append_child(adopt(*new Web::Text(base_document, "div { font-family: Csilla; font-weight: lighter; }")));
    head_element->append_child(style_element);
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
            print_value(m_interpreter->exception()->value(), output_html);
            print_html(output_html.string_view());

            m_interpreter->clear_exception();
            return;
        }

        StringBuilder output_html;
        print_value(m_interpreter->last_value(), output_html);
        print_html(output_html.string_view());
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
    html.append("&gt; ");
    // FIXME: Support output highlighting
    html.append(source);

    print_html(html.string_view());
}

void ConsoleWidget::print_value(JS::Value value, StringBuilder& output_html, HashTable<JS::Object*> seen_objects)
{
    // FIXME: Support output highlighting

    if (value.is_empty()) {
        output_html.append("&lt;empty&gt;");
        return;
    }

    if (value.is_object()) {
        if (seen_objects.contains(&value.as_object())) {
            // FIXME: Maybe we should only do this for circular references,
            //        not for all reoccurring objects.
            output_html.appendf("&lt;already printed Object %p&gt;", &value.as_object());
            return;
        }
        seen_objects.set(&value.as_object());
    }

    if (value.is_array())
        return print_array(static_cast<const JS::Array&>(value.as_object()), output_html, seen_objects);

    if (value.is_object()) {
        auto& object = value.as_object();
        if (object.is_function())
            return print_function(object, output_html, seen_objects);
        if (object.is_date())
            return print_date(object, output_html, seen_objects);
        if (object.is_error())
            return print_error(object, output_html, seen_objects);
        return print_object(object, output_html, seen_objects);
    }

    if (value.is_string())
        output_html.append('"');
    output_html.append(value.to_string_without_side_effects());
    if (value.is_string())
        output_html.append('"');
}

void ConsoleWidget::print_array(const JS::Array& array, StringBuilder& html_output, HashTable<JS::Object*>& seen_objects)
{
    html_output.append("[ ");
    for (size_t i = 0; i < array.elements().size(); ++i) {
        print_value(array.elements()[i], html_output, seen_objects);
        if (i != array.elements().size() - 1)
            html_output.append(", ");
    }
    html_output.append(" ]");
}

void ConsoleWidget::print_object(const JS::Object& object, StringBuilder& html_output, HashTable<JS::Object*>& seen_objects)
{
    html_output.append("{ ");

    for (size_t i = 0; i < object.elements().size(); ++i) {
        if (object.elements()[i].is_empty())
            continue;
        html_output.appendf("\"m%zu\": ", i);
        print_value(object.elements()[i], html_output, seen_objects);
        if (i != object.elements().size() - 1)
            html_output.append(", ");
    }

    if (!object.elements().is_empty() && object.shape().property_count())
        html_output.append(", ");

    size_t index = 0;
    for (auto& it : object.shape().property_table_ordered()) {
        html_output.appendf("\"%s\": ", it.key.characters());
        print_value(object.get_direct(it.value.offset), html_output, seen_objects);
        if (index != object.shape().property_count() - 1)
            html_output.append(", ");
        ++index;
    }

    html_output.append(" }");
}

void ConsoleWidget::print_function(const JS::Object& function, StringBuilder& html_output, HashTable<JS::Object*>&)
{
    html_output.appendf("[%s]", function.class_name());
}

void ConsoleWidget::print_date(const JS::Object& date, StringBuilder& html_output, HashTable<JS::Object*>&)
{
    html_output.appendf("Date %s", static_cast<const JS::Date&>(date).string().characters());
}

void ConsoleWidget::print_error(const JS::Object& object, StringBuilder& html_output, HashTable<JS::Object*>&)
{
    auto& error = static_cast<const JS::Error&>(object);
    html_output.appendf("[%s]", error.name().characters());
    if (!error.message().is_empty())
        html_output.appendf(": %s", error.message().characters());
}

void ConsoleWidget::print_html(const StringView& line)
{
    auto paragraph = create_element(m_console_output_container->document(), "p");
    paragraph->set_inner_html(line);

    m_console_output_container->append_child(paragraph);
    m_console_output_container->set_needs_style_update(true);
    m_console_output_container->document().schedule_style_update();
    m_console_output_container->document().invalidate_layout();
}

void ConsoleWidget::clear_output()
{
    const_cast<Web::HTMLBodyElement*>(m_console_output_view->document()->body())->remove_all_children();
}

}
