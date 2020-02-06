/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GDesktop.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GWindow.h>
#include <sys/utsname.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer accept rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GUI::Application app(argc, argv);

    if (pledge("stdio shared_buffer accept rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    auto window = GUI::Window::construct();
    window->set_title("About SerenityOS");
    Gfx::Rect window_rect { 0, 0, 240, 180 };
    window_rect.center_within(GUI::Desktop::the().rect());
    window->set_resizable(false);
    window->set_rect(window_rect);

    auto widget = GUI::Widget::construct();
    window->set_main_widget(widget);
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GUI::VBoxLayout>());
    widget->layout()->set_margins({ 0, 8, 0, 8 });
    widget->layout()->set_spacing(8);

    auto icon_label = GUI::Label::construct(widget);
    icon_label->set_icon(Gfx::Bitmap::load_from_file("/res/icons/serenity.png"));
    icon_label->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    icon_label->set_preferred_size(icon_label->icon()->size());

    auto label = GUI::Label::construct(widget);
    label->set_font(Gfx::Font::default_bold_font());
    label->set_text("SerenityOS");
    label->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    label->set_preferred_size(0, 11);

    utsname uts;
    int rc = uname(&uts);
    ASSERT(rc == 0);

    auto version_label = GUI::Label::construct(widget);
    version_label->set_text(String::format("Version %s", uts.release));
    version_label->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    version_label->set_preferred_size(0, 11);

    auto git_info_label = GUI::Label::construct(widget);
    git_info_label->set_text(String::format("Built on %s@%s", GIT_BRANCH, GIT_COMMIT));
    git_info_label->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    git_info_label->set_preferred_size(0, 11);

    auto git_changes_label = GUI::Label::construct(widget);
    git_changes_label->set_text(String::format("Changes: %s", GIT_CHANGES));
    git_changes_label->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    git_changes_label->set_preferred_size(0, 11);

    auto quit_button = GUI::Button::construct(widget);
    quit_button->set_text("Okay");
    quit_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    quit_button->set_preferred_size(100, 20);
    quit_button->on_click = [](GUI::Button&) {
        GUI::Application::the().quit(0);
    };

    quit_button->set_focus(true);
    window->show();
    return app.exec();
}
