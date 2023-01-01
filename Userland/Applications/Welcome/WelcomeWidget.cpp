/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WelcomeWidget.h"
#include <AK/Random.h>
#include <Applications/Welcome/WelcomeWindowGML.h>
#include <LibConfig/Client.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/Stream.h>
#include <LibGUI/Application.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Process.h>
#include <LibGfx/Font/BitmapFont.h>
#include <LibGfx/Palette.h>

WelcomeWidget::WelcomeWidget()
{
    load_from_gml(welcome_window_gml);

    m_web_view = find_descendant_of_type_named<WebView::OutOfProcessWebView>("web_view");
    m_web_view->load(URL::create_with_file_scheme(DeprecatedString::formatted("{}/README.md", Core::StandardPaths::home_directory())));

    m_tip_label = find_descendant_of_type_named<GUI::Label>("tip_label");
    m_tip_frame = find_descendant_of_type_named<GUI::Frame>("tip_frame");

    m_next_button = find_descendant_of_type_named<GUI::Button>("next_button");
    m_next_button->on_click = [&](auto) {
        m_web_view->set_visible(false);
        m_tip_frame->set_visible(true);
        if (m_tips.is_empty())
            return;
        m_initial_tip_index++;
        if (m_initial_tip_index >= m_tips.size())
            m_initial_tip_index = 0;
        m_tip_label->set_text(m_tips[m_initial_tip_index]);
    };

    m_help_button = find_descendant_of_type_named<GUI::Button>("help_button");
    m_help_button->on_click = [&](auto) {
        GUI::Process::spawn_or_show_error(window(), "/bin/Help"sv);
    };

    m_new_button = find_descendant_of_type_named<GUI::Button>("new_button");
    m_new_button->on_click = [&](auto) {
        m_web_view->set_visible(!m_web_view->is_visible());
        m_tip_frame->set_visible(!m_tip_frame->is_visible());
    };

    m_close_button = find_descendant_of_type_named<GUI::Button>("close_button");
    m_close_button->on_click = [](auto) {
        GUI::Application::the()->quit();
    };

    auto welcome = Config::list_groups("SystemServer"sv).first_matching([](auto& group) { return group == "Welcome"sv; });
    m_startup_checkbox = find_descendant_of_type_named<GUI::CheckBox>("startup_checkbox");
    m_startup_checkbox->set_checked(welcome.has_value());
    m_startup_checkbox->on_checked = [](bool is_checked) {
        if (is_checked)
            Config::add_group("SystemServer"sv, "Welcome"sv);
        else
            Config::remove_group("SystemServer"sv, "Welcome"sv);
    };

    if (auto result = open_and_parse_tips_file(); result.is_error()) {
        auto error = DeprecatedString::formatted("Opening \"{}/tips.txt\" failed: {}", Core::StandardPaths::documents_directory(), result.error());
        m_tip_label->set_text(error);
        warnln(error);
    }

    set_random_tip();
}

ErrorOr<void> WelcomeWidget::open_and_parse_tips_file()
{
    auto path = DeprecatedString::formatted("{}/tips.txt", Core::StandardPaths::documents_directory());
    auto file = TRY(Core::Stream::File::open(path, Core::Stream::OpenMode::Read));
    auto buffered_file = TRY(Core::Stream::BufferedFile::create(move(file)));
    Array<u8, PAGE_SIZE> buffer;

    while (TRY(buffered_file->can_read_line())) {
        auto line = TRY(buffered_file->read_line(buffer));
        if (line.starts_with('#') || line.is_empty())
            continue;
        m_tips.append(line);
    }

    return {};
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

    static auto font = Gfx::BitmapFont::load_from_file("/res/fonts/MarietaRegular24.font"sv);
    painter.draw_text(Gfx::IntRect { 12, 4, 1, 30 }, "Welcome to "sv, *font, Gfx::TextAlignment::CenterLeft, palette().base_text());
    painter.draw_text(Gfx::IntRect { 12 + static_cast<int>(ceilf(font->width("Welcome to "sv))), 4, 1, 30 }, "Serenity"sv, font->bold_variant(), Gfx::TextAlignment::CenterLeft, palette().base_text());
    painter.draw_text(Gfx::IntRect { 12 + static_cast<int>(ceilf(font->width("Welcome to "sv))) + static_cast<int>(ceilf(font->bold_variant().width("Serenity"sv))), 4, 1, 30 }, "OS"sv, font->bold_variant(), Gfx::TextAlignment::CenterLeft, palette().base() == palette().window() ? palette().base_text() : palette().base());
}
