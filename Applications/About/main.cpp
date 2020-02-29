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

#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Label.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font.h>
#include <stdio.h>
#include <sys/utsname.h>

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
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/ladybug.png"));
    Gfx::Rect window_rect { 0, 0, 224, 178 };
    window_rect.center_within(GUI::Desktop::the().rect());
    window->set_resizable(false);
    window->set_rect(window_rect);

    auto outer_widget = GUI::Widget::construct();
    window->set_main_widget(outer_widget);
    outer_widget->set_fill_with_background_color(true);
    outer_widget->set_layout(make<GUI::VerticalBoxLayout>());
    outer_widget->layout()->set_margins({ 8, 8, 8, 8 });

    auto inner_widget = outer_widget->add<GUI::Widget>();
    inner_widget->set_layout(make<GUI::HorizontalBoxLayout>());
    inner_widget->layout()->set_spacing(8);

    auto left_outer_container = inner_widget->add<GUI::Widget>();
    left_outer_container->set_layout(make<GUI::HorizontalBoxLayout>());

    auto left_inner_container = left_outer_container->add<GUI::Widget>();
    left_inner_container->set_layout(make<GUI::VerticalBoxLayout>());
    left_inner_container->layout()->set_spacing(8);
    left_inner_container->set_preferred_size(0, 50);
    left_inner_container->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);

    auto label = left_inner_container->add<GUI::Label>();
    label->set_text_alignment(Gfx::TextAlignment::CenterRight);
    label->set_font(Gfx::Font::default_bold_font());
    label->set_text("SerenityOS");
    label->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    label->set_preferred_size(0, 11);

    utsname uts;
    int rc = uname(&uts);
    ASSERT(rc == 0);

    auto version_label = left_inner_container->add<GUI::Label>();
    version_label->set_text_alignment(Gfx::TextAlignment::CenterRight);
    version_label->set_text(String::format("Version %s", uts.release));
    version_label->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    version_label->set_preferred_size(0, 11);

    auto git_info_label = left_inner_container->add<GUI::Label>();
    git_info_label->set_text_alignment(Gfx::TextAlignment::CenterRight);
    git_info_label->set_text(String::format("%s@%s", GIT_BRANCH, GIT_COMMIT));
    git_info_label->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    git_info_label->set_preferred_size(0, 11);

    auto right_container = inner_widget->add<GUI::Widget>();
    right_container->set_layout(make<GUI::VerticalBoxLayout>());

    auto icon_label = right_container->add<GUI::Label>();
    icon_label->set_icon(Gfx::Bitmap::load_from_file("/res/icons/buggie.png"));
    icon_label->set_tooltip("Buggie");
    icon_label->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    icon_label->set_preferred_size(icon_label->icon()->size());

    auto quit_button = outer_widget->add<GUI::Button>();
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
