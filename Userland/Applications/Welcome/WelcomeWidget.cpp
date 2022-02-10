/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WelcomeWidget.h"
#include <AK/Random.h>
#include <Applications/Welcome/WelcomeWindowGML.h>
#include <LibConfig/Client.h>
#include <LibCore/File.h>
#include <LibCore/Process.h>
#include <LibGUI/Application.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGfx/BitmapFont.h>
#include <LibGfx/Palette.h>
#include <LibMarkdown/Document.h>
#include <LibWeb/OutOfProcessWebView.h>
#include <serenity.h>

WelcomeWidget::WelcomeWidget()
{
    load_from_gml(welcome_window_gml);

    auto& tip_frame = *find_descendant_of_type_named<GUI::Frame>("tip_frame");
    tip_frame.set_background_role(Gfx::ColorRole::Base);
    tip_frame.set_fill_with_background_color(true);

    auto& light_bulb_label = *find_descendant_of_type_named<GUI::Label>("light_bulb_label");
    light_bulb_label.set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/32x32/app-welcome.png").release_value_but_fixme_should_propagate_errors());

    m_web_view = *find_descendant_of_type_named<Web::OutOfProcessWebView>("web_view");

    m_tip_label = *find_descendant_of_type_named<GUI::Label>("tip_label");

    m_next_button = *find_descendant_of_type_named<GUI::Button>("next_button");
    m_next_button->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png").release_value_but_fixme_should_propagate_errors());
    m_next_button->on_click = [&](auto) {
        if (!tip_frame.is_visible()) {
            m_web_view->set_visible(false);
            tip_frame.set_visible(true);
        }
        if (m_tips.is_empty())
            return;
        m_initial_tip_index++;
        if (m_initial_tip_index >= m_tips.size())
            m_initial_tip_index = 0;
        m_tip_label->set_text(m_tips[m_initial_tip_index]);
    };

    m_help_button = *find_descendant_of_type_named<GUI::Button>("help_button");
    m_help_button->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/book-open.png").release_value_but_fixme_should_propagate_errors());
    m_help_button->on_click = [](auto) {
        Core::Process::spawn("/bin/Help"sv);
    };

    m_new_button = *find_descendant_of_type_named<GUI::Button>("new_button");
    m_new_button->on_click = [&](auto) {
        m_web_view->set_visible(!m_web_view->is_visible());
        tip_frame.set_visible(!tip_frame.is_visible());
    };

    m_close_button = *find_descendant_of_type_named<GUI::Button>("close_button");
    m_close_button->on_click = [](auto) {
        GUI::Application::the()->quit();
    };

    auto exec_path = Config::read_string("SystemServer", "Welcome", "Executable", {});
    m_startup_checkbox = *find_descendant_of_type_named<GUI::CheckBox>("startup_checkbox");
    m_startup_checkbox->set_checked(!exec_path.is_empty());
    m_startup_checkbox->on_checked = [](bool is_checked) {
        Config::write_string("SystemServer", "Welcome", "Executable", is_checked ? "/bin/Welcome" : "");
    };

    open_and_parse_readme_file();
    open_and_parse_tips_file();
    set_random_tip();
}

void WelcomeWidget::open_and_parse_tips_file()
{
    auto file = Core::File::construct("/home/anon/Documents/tips.txt");
    if (!file->open(Core::OpenMode::ReadOnly)) {
        m_tip_label->set_text("~/Documents/tips.txt has gone missing!");
        return;
    }

    while (file->can_read_line()) {
        auto line = file->read_line();
        auto* ch = line.characters();
        switch (*ch) {
        case '\n':
        case '\r':
        case '\0':
        case '#':
            continue;
        }
        m_tips.append(line);
    }
}

void WelcomeWidget::open_and_parse_readme_file()
{
    auto file = Core::File::construct("/home/anon/README.md");
    if (!file->open(Core::OpenMode::ReadOnly))
        return;

    auto document = Markdown::Document::parse(file->read_all());
    if (document) {
        auto html = document->render_to_html();
        m_web_view->load_html(html, URL::create_with_file_protocol("/home/anon/README.md"));
    }
}

void WelcomeWidget::set_random_tip()
{
    if (m_tips.is_empty())
        return;

    m_initial_tip_index = get_random_uniform(m_tips.size());
    m_tip_label->set_text(m_tips[m_initial_tip_index]);
}

void WelcomeWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    static auto font = Gfx::BitmapFont::load_from_file("/res/fonts/MarietaRegular24.font");
    painter.draw_text({ 12, 4, 1, 30 }, "Welcome to ", *font, Gfx::TextAlignment::CenterLeft, palette().base_text());
    painter.draw_text({ 12 + font->width("Welcome to "), 4, 1, 30 }, "Serenity", font->bold_variant(), Gfx::TextAlignment::CenterLeft, palette().base_text());
    painter.draw_text({ 12 + font->width("Welcome to ") + font->bold_variant().width("Serenity"), 4, 1, 30 }, "OS", font->bold_variant(), Gfx::TextAlignment::CenterLeft, palette().base() == palette().window() ? palette().base_text() : palette().base());
}
