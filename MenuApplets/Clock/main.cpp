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

#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/Timer.h>
#include <LibCore/ConfigFile.h>
#include <LibGUI/Application.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <stdio.h>
#include <time.h>

class ClockWidget final : public GUI::Widget {
    C_OBJECT(ClockWidget)
public:
    ClockWidget()
    {
        auto config = Core::ConfigFile::open("/etc/WindowServer/Applet.ini");
        m_format = config->read_entry("Clock", "Format");

        m_time_width = Gfx::Font::default_bold_font().width(convert((tm){
				66, 59, 23, 31, 11, 2222, 6, 365, 0
			}));

	int interval = config->read_num_entry("Clock", "Interval");
	interval = interval <= 0 ? 1 : interval;

        m_timer = add<Core::Timer>(1000 * interval, [this] {
            static time_t last_update_time;
            time_t now = time(nullptr);
            if (now != last_update_time) {
                tick_clock();
                last_update_time = now;
            }
        });
    }

    virtual ~ClockWidget() override {}

    int get_width()
    {
        return m_time_width + menubar_menu_margin() * 0;
    }

private:
    static int menubar_menu_margin() { return 2; }

    virtual void paint_event(GUI::PaintEvent& event) override
    {
        time_t now = time(nullptr);
        auto* tm = localtime(&now);

        auto time_text = convert(*tm);

        GUI::Painter painter(*this);
        painter.fill_rect(event.rect(), palette().window());
        painter.draw_text(event.rect(), time_text, Gfx::Font::default_font(), Gfx::TextAlignment::Center, palette().window_text());
    }

    void tick_clock()
    {
        update();
    }

    // FIXME: Find a better name for this function
    String convert(tm tm)
    {
	(void) tm;
    	StringBuilder builder;
	auto format_parts = m_format.split('%');

	for(size_t i = 0; i < format_parts.size(); i++)
	{
		// If the first part doesn't start with a %
		if(i == 0 && !m_format.starts_with('%')){
			builder.append(format_parts[i]);
			continue;
		}

		switch(format_parts[i].characters()[0]){
			case 'S':
				builder.append(String::format("%02i", tm.tm_sec));
				break;
			case 'M':
				builder.append(String::format("%02i", tm.tm_min));
				break;
			case 'H':
				builder.append(String::format("%02i", tm.tm_hour));
				break;

			case 'h':
				if(tm.tm_hour != 0)
					builder.append(String::format("%02i", tm.tm_hour < 12 ? tm.tm_hour : tm.tm_hour - 12));
				else
					builder.append(String("12"));
				break;
			case 'a':
				builder.append(String(tm.tm_hour < 12 && tm.tm_hour > 0 ? "AM" : "PM"));
				break;

			case 'd':
				builder.append(String::format("%02i", tm.tm_mday));
				break;
			case 'm':
				builder.append(String::format("%02i", tm.tm_mon + 1));
				break;
			case 'y':
				builder.append(String::format("%04i", tm.tm_year + 1900));
				break;

			default:
				builder.append("%");
				builder.append(format_parts[i]);
				continue;
		}

		// This will add the text after the first character
		builder.append(format_parts[i].substring(1, format_parts[i].length() - 1));
	}	

	return builder.to_string();
    }

    RefPtr<Core::Timer> m_timer;
    int m_time_width;
    String m_format;
};

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

    auto window = GUI::Window::construct();
    window->set_title("Clock");
    window->set_window_type(GUI::WindowType::MenuApplet);

    auto widget = ClockWidget::construct();

    window->resize(widget->get_width(), 16);
    window->set_main_widget(widget);
    window->show();

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    return app.exec();
}
