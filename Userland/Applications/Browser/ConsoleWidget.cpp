/*
 * Copyright (c) 2020, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConsoleWidget.h"
#include <AK/StringBuilder.h>
#include <Applications/Browser/Browser.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibJS/MarkupGenerator.h>
#include <LibJS/SyntaxHighlighter.h>
#include <LibWebView/ConsoleClient.h>
#include <LibWebView/OutOfProcessWebView.h>

namespace Browser {

ConsoleWidget::ConsoleWidget(WebView::OutOfProcessWebView& content_view)
{
    set_layout<GUI::VerticalBoxLayout>();
    set_fill_with_background_color(true);

    m_output_view = add<WebView::OutOfProcessWebView>();
    m_console_client = make<WebView::ConsoleClient>(content_view, *m_output_view);

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

        if (js_source.is_whitespace())
            return;

        m_input->add_current_text_to_history();
        m_input->clear();

        m_console_client->execute(MUST(String::from_deprecated_string(js_source)));
    };

    set_focus_proxy(m_input);

    auto& clear_button = bottom_container.add<GUI::Button>();
    clear_button.set_fixed_size(22, 22);
    clear_button.set_icon(g_icon_bag.delete_icon);
    clear_button.set_tooltip_deprecated("Clear the console output");
    clear_button.on_click = [this](auto) {
        m_console_client->clear();
    };
}

ConsoleWidget::~ConsoleWidget() = default;

void ConsoleWidget::reset()
{
    m_console_client->reset();
}

}
