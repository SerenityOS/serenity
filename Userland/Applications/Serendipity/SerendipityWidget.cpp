/*
 * Copyright (c) 2021, the SerenityOS Developers
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

#include "SerendipityWidget.h"
#include <Applications/Serendipity/SerendipityWindowGML.h>
#include <LibCore/File.h>
#include <LibGUI/Application.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibMarkdown/Document.h>
#include <LibWeb/OutOfProcessWebView.h>
#include <serenity.h>
#include <spawn.h>
#include <time.h>

SerendipityWidget::SerendipityWidget()
{
    load_from_gml(serendipity_window_gml);

    auto& banner_label = *find_descendant_of_type_named<GUI::Label>("banner_label");
    banner_label.set_icon(Gfx::Bitmap::load_from_file("/res/graphics/welcome-serendipity.png"));

    auto& tip_frame = *find_descendant_of_type_named<GUI::Frame>("tip_frame");
    auto palette = tip_frame.palette();
    palette.set_color(Gfx::ColorRole::Base, Color::from_rgb(0xffffe1));
    tip_frame.set_palette(palette);
    tip_frame.set_background_role(Gfx::ColorRole::Base);
    tip_frame.set_fill_with_background_color(true);

    auto& light_bulb_label = *find_descendant_of_type_named<GUI::Label>("light_bulb_label");
    light_bulb_label.set_icon(Gfx::Bitmap::load_from_file("/res/icons/32x32/app-serendipity.png"));

    auto& did_you_know_label = *find_descendant_of_type_named<GUI::Label>("did_you_know_label");
    did_you_know_label.set_font(Gfx::BitmapFont::load_from_file("/res/fonts/KaticaBold12.font"));

    m_web_view = *find_descendant_of_type_named<Web::OutOfProcessWebView>("web_view");

    m_tip_label = *find_descendant_of_type_named<GUI::Label>("tip_label");
    m_tip_label->set_font(Gfx::BitmapFont::load_from_file("/res/fonts/KaticaRegular12.font"));

    m_next_button = *find_descendant_of_type_named<GUI::Button>("next_button");
    m_next_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"));
    m_next_button->on_click = [&]() {
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
    m_help_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/book-open.png"));
    m_help_button->on_click = []() {
        pid_t pid;
        const char* argv[] = { "Help", nullptr };
        if ((errno = posix_spawn(&pid, "/bin/Help", nullptr, nullptr, const_cast<char**>(argv), environ))) {
            perror("posix_spawn");
        } else {
            if (disown(pid) < 0)
                perror("disown");
        }
    };

    m_new_button = *find_descendant_of_type_named<GUI::Button>("new_button");
    m_new_button->on_click = [&]() {
        m_web_view->set_visible(!m_web_view->is_visible());
        tip_frame.set_visible(!tip_frame.is_visible());
    };

    m_close_button = *find_descendant_of_type_named<GUI::Button>("close_button");
    m_close_button->on_click = []() {
        GUI::Application::the()->quit();
    };

    open_and_parse_readme_file();
    open_and_parse_tips_file();
    srand(time(nullptr));
    set_random_tip();
}

SerendipityWidget::~SerendipityWidget()
{
}

void SerendipityWidget::open_and_parse_tips_file()
{
    auto file = Core::File::construct("/home/anon/Documents/tips.txt");
    if (!file->open(Core::IODevice::ReadOnly)) {
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

void SerendipityWidget::open_and_parse_readme_file()
{
    auto file = Core::File::construct("/home/anon/ReadMe.md");
    if (!file->open(Core::IODevice::ReadOnly))
        return;

    auto document = Markdown::Document::parse(file->read_all());
    if (document) {
        auto html = document->render_to_html();
        m_web_view->load_html(html, URL::create_with_file_protocol("/home/anon/ReadMe.md"));
    }
}

void SerendipityWidget::set_random_tip()
{
    if (m_tips.is_empty())
        return;

    size_t n;
    do
        n = rand();
    while (n >= m_tips.size());
    m_initial_tip_index = n;
    m_tip_label->set_text(m_tips[n]);
}
