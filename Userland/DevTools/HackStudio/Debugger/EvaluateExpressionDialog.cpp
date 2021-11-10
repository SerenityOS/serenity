/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EvaluateExpressionDialog.h"
#include "DebuggerGlobalJSObject.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGfx/FontDatabase.h>
#include <LibJS/Interpreter.h>
#include <LibJS/MarkupGenerator.h>
#include <LibJS/Parser.h>
#include <LibJS/SyntaxHighlighter.h>
#include <LibWeb/DOM/DocumentType.h>

namespace HackStudio {

static JS::VM& global_vm()
{
    static RefPtr<JS::VM> vm;
    if (!vm)
        vm = JS::VM::create();
    return *vm;
}

EvaluateExpressionDialog::EvaluateExpressionDialog(Window* parent_window)
    : Dialog(parent_window)
    , m_interpreter(JS::Interpreter::create<DebuggerGlobalJSObject>(global_vm()))
{
    set_title("Evaluate Expression");
    set_icon(parent_window->icon());
    build(parent_window);
}

void EvaluateExpressionDialog::build(Window* parent_window)
{
    auto& widget = set_main_widget<GUI::Widget>();

    int width = max(parent_window->width() / 2, 150);
    int height = max(parent_window->height() * (2 / 3), 350);

    set_rect(x(), y(), width, height);

    widget.set_layout<GUI::VerticalBoxLayout>();
    widget.set_fill_with_background_color(true);

    widget.layout()->set_margins(6);
    widget.layout()->set_spacing(6);

    m_text_editor = widget.add<GUI::TextBox>();
    m_text_editor->set_fixed_height(19);
    m_text_editor->set_syntax_highlighter(make<JS::SyntaxHighlighter>());
    m_text_editor->set_font(Gfx::FontDatabase::default_fixed_width_font());
    m_text_editor->set_history_enabled(true);

    auto base_document = Web::DOM::Document::create();
    base_document->append_child(adopt_ref(*new Web::DOM::DocumentType(base_document)));
    auto html_element = base_document->create_element("html");
    base_document->append_child(html_element);
    auto head_element = base_document->create_element("head");
    html_element->append_child(head_element);
    auto body_element = base_document->create_element("body");
    html_element->append_child(body_element);
    m_output_container = body_element;

    m_output_view = widget.add<Web::InProcessWebView>();
    m_output_view->set_document(base_document);

    auto& button_container_outer = widget.add<GUI::Widget>();
    button_container_outer.set_fixed_height(20);
    button_container_outer.set_layout<GUI::VerticalBoxLayout>();

    auto& button_container_inner = button_container_outer.add<GUI::Widget>();
    button_container_inner.set_layout<GUI::HorizontalBoxLayout>();
    button_container_inner.layout()->set_spacing(6);
    button_container_inner.layout()->set_margins({ 4, 0, 4 });
    button_container_inner.layout()->add_spacer();

    m_evaluate_button = button_container_inner.add<GUI::Button>();
    m_evaluate_button->set_fixed_height(20);
    m_evaluate_button->set_text("Evaluate");
    m_evaluate_button->on_click = [this](auto) {
        handle_evaluation(m_text_editor->text());
    };

    m_close_button = button_container_inner.add<GUI::Button>();
    m_close_button->set_fixed_height(20);
    m_close_button->set_text("Close");
    m_close_button->on_click = [this](auto) {
        done(ExecOK);
    };

    m_text_editor->on_return_pressed = [this] {
        m_evaluate_button->click();
    };
    m_text_editor->on_escape_pressed = [this] {
        m_close_button->click();
    };
    m_text_editor->set_focus(true);
}

void EvaluateExpressionDialog::handle_evaluation(const String& expression)
{
    m_output_container->remove_all_children();
    m_output_view->update();

    auto parser = JS::Parser(JS::Lexer(expression));
    auto program = parser.parse_program();

    StringBuilder output_html;
    if (parser.has_errors()) {
        auto error = parser.errors()[0];
        auto hint = error.source_location_hint(expression);
        if (!hint.is_empty())
            output_html.append(String::formatted("<pre>{}</pre>", escape_html_entities(hint)));
        m_interpreter->vm().throw_exception<JS::SyntaxError>(m_interpreter->global_object(), error.to_string());
    } else {
        m_interpreter->run(m_interpreter->global_object(), *program);
    }

    if (m_interpreter->exception()) {
        auto* exception = m_interpreter->exception();
        m_interpreter->vm().clear_exception();
        output_html.append("Uncaught exception: ");
        auto error = exception->value();
        if (error.is_object())
            output_html.append(JS::MarkupGenerator::html_from_error(error.as_object()));
        else
            output_html.append(JS::MarkupGenerator::html_from_value(error));
        set_output(output_html.string_view());
        return;
    }

    set_output(JS::MarkupGenerator::html_from_value(m_interpreter->vm().last_value()));
}

void EvaluateExpressionDialog::set_output(StringView html)
{
    auto paragraph = m_output_container->document().create_element("p");
    paragraph->set_inner_html(html);

    m_output_container->append_child(paragraph);
}

}
