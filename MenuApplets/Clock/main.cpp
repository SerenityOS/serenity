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

#include <LibCore/DateTime.h>
#include <LibCore/Timer.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Calendar.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <serenity.h>
#include <spawn.h>
#include <stdio.h>
#include <time.h>

class ClockWidget final : public GUI::Widget {
    C_OBJECT(ClockWidget)
public:
    ClockWidget()
    {
        m_time_width = Gfx::Font::default_bold_font().width("2222-22-22 22:22:22");

        m_timer = add<Core::Timer>(1000, [this] {
            static time_t last_update_time;
            time_t now = time(nullptr);
            if (now != last_update_time) {
                tick_clock();
                last_update_time = now;
            }
        });

        m_calendar_window = add<GUI::Window>(window());
        m_calendar_window->set_frameless(true);
        m_calendar_window->set_resizable(false);
        m_calendar_window->set_minimizable(false);
        m_calendar_window->on_active_input_change = [this](bool is_active_input) {
            if (!is_active_input)
                close();
        };

        auto& root_container = m_calendar_window->set_main_widget<GUI::Label>();
        root_container.set_fill_with_background_color(true);
        root_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
        root_container.set_layout<GUI::VerticalBoxLayout>();
        root_container.layout()->set_margins({ 0, 2, 0, 2 });
        root_container.layout()->set_spacing(0);
        root_container.set_frame_thickness(2);
        root_container.set_frame_shape(Gfx::FrameShape::Container);
        root_container.set_frame_shadow(Gfx::FrameShadow::Raised);

        auto& navigation_container = root_container.add<GUI::Widget>();
        navigation_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        navigation_container.set_preferred_size(0, 24);
        navigation_container.set_layout<GUI::HorizontalBoxLayout>();
        navigation_container.layout()->set_margins({ 2, 2, 3, 2 });

        m_prev_date = navigation_container.add<GUI::Button>();
        m_prev_date->set_button_style(Gfx::ButtonStyle::CoolBar);
        m_prev_date->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
        m_prev_date->set_preferred_size(24, 24);
        m_prev_date->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-back.png"));
        m_prev_date->on_click = [&](auto) {
            unsigned int target_month = m_calendar->selected_month();
            unsigned int target_year = m_calendar->selected_year();

            if (m_calendar->mode() == GUI::Calendar::Month) {
                target_month--;
                if (m_calendar->selected_month() <= 1) {
                    target_month = 12;
                    target_year--;
                }
            } else {
                target_year--;
            }

            m_calendar->update_tiles(target_year, target_month);
            m_selected_calendar_button->set_text(m_calendar->selected_calendar_text(GUI::Calendar::LongNames));
        };

        m_selected_calendar_button = navigation_container.add<GUI::Button>();
        m_selected_calendar_button->set_button_style(Gfx::ButtonStyle::CoolBar);
        m_selected_calendar_button->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        m_selected_calendar_button->set_preferred_size(0, 24);
        m_selected_calendar_button->on_click = [&](auto) {
            m_calendar->toggle_mode();
            m_selected_calendar_button->set_text(m_calendar->selected_calendar_text(GUI::Calendar::LongNames));
        };

        m_next_date = navigation_container.add<GUI::Button>();
        m_next_date->set_button_style(Gfx::ButtonStyle::CoolBar);
        m_next_date->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
        m_next_date->set_preferred_size(24, 24);
        m_next_date->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"));
        m_next_date->on_click = [&](auto) {
            unsigned int target_month = m_calendar->selected_month();
            unsigned int target_year = m_calendar->selected_year();

            if (m_calendar->mode() == GUI::Calendar::Month) {
                target_month++;
                if (m_calendar->selected_month() >= 12) {
                    target_month = 1;
                    target_year++;
                }
            } else {
                target_year++;
            }

            m_calendar->update_tiles(target_year, target_month);
            m_selected_calendar_button->set_text(m_calendar->selected_calendar_text(GUI::Calendar::LongNames));
        };

        auto& divider1_container = root_container.add<GUI::Widget>();
        divider1_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        divider1_container.set_preferred_size(0, 2);
        divider1_container.set_layout<GUI::HorizontalBoxLayout>();
        divider1_container.layout()->set_margins({ 2, 0, 3, 0 });

        auto& divider1 = divider1_container.add<GUI::Frame>();
        divider1.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        divider1.set_preferred_size(0, 2);
        divider1.set_frame_shape(Gfx::FrameShape::Panel);

        auto& calendar_frame_container = root_container.add<GUI::Widget>();
        calendar_frame_container.set_layout<GUI::HorizontalBoxLayout>();
        calendar_frame_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
        calendar_frame_container.layout()->set_margins({ 4, 4, 5, 4 });

        auto& calendar_frame = calendar_frame_container.add<GUI::Frame>();
        calendar_frame.set_layout<GUI::VerticalBoxLayout>();
        calendar_frame.layout()->set_margins({ 2, 2, 2, 2 });

        m_calendar = calendar_frame.add<GUI::Calendar>(Core::DateTime::now());
        m_selected_calendar_button->set_text(m_calendar->selected_calendar_text(GUI::Calendar::LongNames));

        m_calendar->on_calendar_tile_click = [&] {
            m_selected_calendar_button->set_text(m_calendar->selected_calendar_text(GUI::Calendar::LongNames));
        };

        m_calendar->on_month_tile_click = [&] {
            m_selected_calendar_button->set_text(m_calendar->selected_calendar_text(GUI::Calendar::LongNames));
        };

        auto& divider2_container = root_container.add<GUI::Widget>();
        divider2_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        divider2_container.set_preferred_size(0, 2);
        divider2_container.set_layout<GUI::HorizontalBoxLayout>();
        divider2_container.layout()->set_margins({ 2, 0, 3, 0 });

        auto& divider2 = divider2_container.add<GUI::Frame>();
        divider2.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        divider2.set_preferred_size(0, 2);
        divider2.set_frame_shape(Gfx::FrameShape::Panel);

        auto& settings_container = root_container.add<GUI::Widget>();
        settings_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        settings_container.set_preferred_size(0, 24);
        settings_container.set_layout<GUI::HorizontalBoxLayout>();
        settings_container.layout()->set_margins({ 2, 2, 3, 2 });
        settings_container.layout()->add_spacer();

        m_jump_to_button = settings_container.add<GUI::Button>();
        m_jump_to_button->set_button_style(Gfx::ButtonStyle::CoolBar);
        m_jump_to_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
        m_jump_to_button->set_preferred_size(24, 24);
        m_jump_to_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/calendar-date.png"));
        m_jump_to_button->set_tooltip("Jump to today");
        m_jump_to_button->on_click = [this](auto) {
            jump_to_current_date();
        };

        m_calendar_launcher = settings_container.add<GUI::Button>();
        m_calendar_launcher->set_button_style(Gfx::ButtonStyle::CoolBar);
        m_calendar_launcher->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
        m_calendar_launcher->set_preferred_size(24, 24);
        m_calendar_launcher->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-calendar.png"));
        m_calendar_launcher->set_tooltip("Calendar");
        m_calendar_launcher->on_click = [this](auto) {
            pid_t pid;
            const char* argv[] = { "Calendar", nullptr };
            if ((errno = posix_spawn(&pid, "/bin/Calendar", nullptr, nullptr, const_cast<char**>(argv), environ))) {
                perror("posix_spawn");
            } else {
                if (disown(pid) < 0)
                    perror("disown");
            }
        };
    }

    virtual ~ClockWidget() override { }

    int get_width()
    {
        return m_time_width + menubar_menu_margin();
    }

private:
    static int menubar_menu_margin() { return 2; }

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        auto time_text = Core::DateTime::now().to_string();
        GUI::Painter painter(*this);
        painter.fill_rect(event.rect(), palette().window());
        painter.draw_text(event.rect(), time_text, Gfx::Font::default_font(), Gfx::TextAlignment::Center, palette().window_text());
    }

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.button() != GUI::MouseButton::Left) {
            return;
        } else {
            if (!m_calendar_window->is_visible())
                open();
            else
                close();
        }
    }

    void tick_clock()
    {
        update();
    }

    void open()
    {
        position_calendar_window();
        jump_to_current_date();
        m_calendar_window->show();
    }

    void close()
    {
        m_calendar_window->hide();
    }

    void position_calendar_window()
    {
        m_calendar_window->set_rect(
            window()->rect_in_menubar().x() - ((m_calendar_window->rect().width() - window()->rect().width()) / 2),
            19,
            153,
            180);
    }

    void jump_to_current_date()
    {
        if (m_calendar->mode() == GUI::Calendar::Year)
            m_calendar->toggle_mode();
        m_calendar->set_selected_date(Core::DateTime::now());
        m_calendar->update_tiles(Core::DateTime::now().year(), Core::DateTime::now().month());
        m_selected_calendar_button->set_text(m_calendar->selected_calendar_text(GUI::Calendar::LongNames));
    }

    RefPtr<GUI::Window> m_calendar_window;
    RefPtr<GUI::Calendar> m_calendar;
    RefPtr<GUI::Button> m_next_date;
    RefPtr<GUI::Button> m_prev_date;
    RefPtr<GUI::Button> m_selected_calendar_button;
    RefPtr<GUI::Button> m_jump_to_button;
    RefPtr<GUI::Button> m_calendar_launcher;
    RefPtr<Core::Timer> m_timer;
    int m_time_width;
};

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer accept rpath unix cpath fattr exec proc", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio shared_buffer accept rpath exec proc", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GUI::Window::construct();
    window->set_title("Clock");
    window->set_window_type(GUI::WindowType::MenuApplet);

    auto& widget = window->set_main_widget<ClockWidget>();
    window->resize(widget.get_width(), 16);
    window->show();

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin/Calendar", "x") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    return app->exec();
}
