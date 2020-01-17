#include <LibCore/CTimer.h>
#include <LibDraw/Palette.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>
#include <time.h>

class ClockWidget final : public GWidget {
    C_OBJECT(ClockWidget)
public:
    ClockWidget()
        : GWidget(nullptr)
    {
        m_time_width = Font::default_bold_font().width("2222-22-22 22:22:22");

        m_timer = CTimer::construct(1000, [this] {
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
        return m_time_width + menubar_menu_margin();
    }

private:
    static int menubar_menu_margin() { return 2; }

    virtual void paint_event(GPaintEvent& event) override
    {
        time_t now = time(nullptr);
        auto* tm = localtime(&now);

        auto time_text = String::format("%4u-%02u-%02u %02u:%02u:%02u",
            tm->tm_year + 1900,
            tm->tm_mon + 1,
            tm->tm_mday,
            tm->tm_hour,
            tm->tm_min,
            tm->tm_sec);

        GPainter painter(*this);
        painter.fill_rect(event.rect(), palette().window());
        painter.draw_text(event.rect(), time_text, Font::default_font(), TextAlignment::Center, palette().window_text());
    }

    void tick_clock()
    {
        update();
    }

    RefPtr<CTimer> m_timer;
    int m_time_width;
};

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer accept rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GApplication app(argc, argv);

    if (pledge("stdio shared_buffer accept rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GWindow::construct();
    window->set_window_type(GWindowType::MenuApplet);

    auto widget = ClockWidget::construct();

    window->resize(widget->get_width(), 16);
    window->set_main_widget(widget);
    window->show();

    return app.exec();
}
