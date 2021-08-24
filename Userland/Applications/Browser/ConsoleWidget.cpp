/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConsoleWidget.h"
#include <AK/StringBuilder.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/FontDatabase.h>
#include <LibJS/MarkupGenerator.h>
#include <LibJS/SyntaxHighlighter.h>

namespace Browser {

ConsoleWidget::ConsoleWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    set_fill_with_background_color(true);

    m_output_view = add<Web::OutOfProcessWebView>();
    m_output_view->load("data:text/html,<html></html>");

    auto& bottom_container = add<GUI::Widget>();
    bottom_container.set_layout<GUI::HorizontalBoxLayout>();
    bottom_container.set_fixed_height(22);

    m_input = bottom_container.add<GUI::TextBox>();
    m_input->set_syntax_highlighter(make<JS::SyntaxHighlighter>());
    // FIXME: Syntax Highlighting breaks the cursor's position on non fixed-width fonts.
    m_input->set_font(Gfx::FontDatabase::default_fixed_width_font());
    m_input->set_history_enabled(true);

    m_input->on_return_pressed = [this] {
        auto js_source = m_input->text();

        // FIXME: An is_blank check to check if there is only whitespace would probably be preferable.
        if (js_source.is_empty())
            return;

        m_input->add_current_text_to_history();
        m_input->clear();

        print_source_line(js_source);

        if (on_js_input)
            on_js_input(js_source);
    };

    set_focus_proxy(m_input);

    auto& clear_button = bottom_container.add<GUI::Button>();
    clear_button.set_fixed_size(22, 22);
    clear_button.set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/delete.png"));
    clear_button.set_tooltip("Clear the console output");
    clear_button.on_click = [this](auto) {
        clear_output();
    };
}

ConsoleWidget::~ConsoleWidget()
{
}

void ConsoleWidget::handle_js_console_output(const String& method, const String& line)
{
    if (method == "html") {
        print_html(line);
    } else if (method == "clear") {
        clear_output();
    }
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

void ConsoleWidget::print_html(StringView const& line)
{
    StringBuilder builder;
    builder.append(R"~~~(
        var p = document.createElement("p");
        p.innerHTML = ")~~~");
    builder.append_escaped_for_json(line);
    builder.append(R"~~~("
        document.body.appendChild(p);
)~~~");
    m_output_view->run_javascript(builder.string_view());
    m_output_view->scroll_to_bottom();
}

void ConsoleWidget::clear_output()
{
    m_output_view->run_javascript(R"~~~(
        document.body.innerHTML = "";
    )~~~");
}

}
