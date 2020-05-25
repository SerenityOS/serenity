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
    style_element->append_child(adopt(*new Web::Text(base_document, create_document_style())));
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

String ConsoleWidget::create_document_style()
{
    StringBuilder style;
    auto palette = this->palette();

    auto add_class_and_color = [&](const StringView& class_name, Color color, bool bold = false) {
        style.append(".");
        style.append(class_name);
        style.append(" { color: ");
        style.append(color.to_string_without_alpha());
        style.append(";");

        if (bold) {
            style.append("font-weight: bold;");
        }

        style.append(" } ");
    };

    add_class_and_color("js-string", palette.syntax_string());
    add_class_and_color("js-number", palette.syntax_number());
    add_class_and_color("js-boolean", palette.syntax_keyword(), true);
    add_class_and_color("js-null", palette.syntax_keyword(), true);
    add_class_and_color("js-undefined", palette.syntax_keyword(), true);
    add_class_and_color("js-array-open", palette.syntax_punctuation());
    add_class_and_color("js-array-close", palette.syntax_punctuation());
    add_class_and_color("js-array-element-separator", palette.syntax_punctuation());
    add_class_and_color("js-object-open", palette.syntax_punctuation());
    add_class_and_color("js-object-close", palette.syntax_punctuation());
    add_class_and_color("js-object-element-separator", palette.syntax_punctuation());
    add_class_and_color("js-object-element-index", palette.syntax_number());
    add_class_and_color("js-object-element-key", palette.syntax_string());

    // FIXME: Add to palette?
    add_class_and_color("js-error-name", Color::Red, true);

    return style.to_string();
}

void ConsoleWidget::print_source_line(const StringView& source)
{
    StringBuilder html;
    html.append("<span class=\"repl-indicator\">");
    html.append("&gt; ");
    html.append("</span>");

    // FIXME: Support output source highlighting
    html.append(source);

    print_html(html.string_view());
}

void ConsoleWidget::print_value(JS::Value value, StringBuilder& output_html, HashTable<JS::Object*> seen_objects)
{
    // FIXME: Support output highlighting

    if (value.is_empty()) {
        output_html.append("<span class=\"empty-object\">");
        output_html.append("&lt;empty&gt;");
        output_html.append("</span>");
        return;
    }

    if (value.is_object()) {
        if (seen_objects.contains(&value.as_object())) {
            // FIXME: Maybe we should only do this for circular references,
            //        not for all reoccurring objects.
            output_html.append("<span class=\"already-printed\">");
            output_html.appendf("&lt;already printed Object %p&gt;", &value.as_object());
            output_html.append("</span>");
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
        output_html.append("<span class=\"js-string\">");
    else if (value.is_number())
        output_html.append("<span class=\"js-number\">");
    else if (value.is_boolean())
        output_html.append("<span class=\"js-boolean\">");
    else if (value.is_null())
        output_html.append("<span class=\"js-null\">");
    else if (value.is_undefined())
        output_html.append("<span class=\"js-undefined\">");

    if (value.is_string())
        output_html.append('"');
    output_html.append(value.to_string_without_side_effects());
    if (value.is_string())
        output_html.append('"');

    output_html.append("</span>");
}

void ConsoleWidget::print_array(const JS::Array& array, StringBuilder& html_output, HashTable<JS::Object*>& seen_objects)
{
    html_output.append("<span class=\"js-array-open\">");
    html_output.append("[ ");
    html_output.append("</span>");
    for (size_t i = 0; i < array.elements().size(); ++i) {
        print_value(array.elements()[i], html_output, seen_objects);
        if (i != array.elements().size() - 1) {
            html_output.append("<span class=\"js-array-element-separator\">");
            html_output.append(", ");
            html_output.append("</span>");
        }
    }
    html_output.append("<span class=\"js-array-close\">");
    html_output.append(" ]");
    html_output.append("</span>");
}

void ConsoleWidget::print_object(const JS::Object& object, StringBuilder& html_output, HashTable<JS::Object*>& seen_objects)
{
    html_output.append("<span class=\"js-object-open\">");
    html_output.append("{ ");
    html_output.append("</span>");

    for (size_t i = 0; i < object.elements().size(); ++i) {
        if (object.elements()[i].is_empty())
            continue;
        html_output.append("<span class=\"js-object-element-index\">");
        html_output.appendf("%zu", i);
        html_output.append("</span>");
        html_output.append(": ");
        print_value(object.elements()[i], html_output, seen_objects);
        if (i != object.elements().size() - 1) {
            html_output.append("<span class=\"js-object-element-separator\">");
            html_output.append(", ");
            html_output.append("</span>");
        }
    }

    if (!object.elements().is_empty() && object.shape().property_count()) {
        html_output.append("<span class=\"js-object-element-separator\">");
        html_output.append(", ");
        html_output.append("</span>");
    }

    size_t index = 0;
    for (auto& it : object.shape().property_table_ordered()) {
        html_output.append("<span class=\"js-object-element-key\">");
        html_output.appendf("\"%s\"", it.key.characters());
        html_output.append("</span>");
        html_output.append(": ");
        print_value(object.get_direct(it.value.offset), html_output, seen_objects);
        if (index != object.shape().property_count() - 1) {
            html_output.append("<span class=\"js-object-element-separator\">");
            html_output.append(", ");
            html_output.append("</span>");
        }
        ++index;
    }

    html_output.append("<span class=\"js-object-close\">");
    html_output.append(" }");
    html_output.append("</span>");
}

void ConsoleWidget::print_function(const JS::Object& function, StringBuilder& html_output, HashTable<JS::Object*>&)
{
    html_output.append("<span class=\"js-function\">");
    html_output.appendf("[%s]", function.class_name());
    html_output.append("</span>");
}

void ConsoleWidget::print_date(const JS::Object& date, StringBuilder& html_output, HashTable<JS::Object*>&)
{
    html_output.append("<span class=\"js-date\">");
    html_output.appendf("Date %s", static_cast<const JS::Date&>(date).string().characters());
    html_output.append("</span>");
}

void ConsoleWidget::print_error(const JS::Object& object, StringBuilder& html_output, HashTable<JS::Object*>&)
{
    auto& error = static_cast<const JS::Error&>(object);
    html_output.append("<span class=\"js-error-name\">");
    html_output.appendf("[%s]", error.name().characters());
    html_output.append("</span>");
    if (!error.message().is_empty()) {
        html_output.append("<span class=\"js-error-message\">");
        html_output.appendf(": %s", error.message().characters());
        html_output.append("</span>");
    }
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
