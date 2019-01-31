#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <assert.h>
#include <time.h>
#include <Kernel/Syscall.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <SharedGraphics/Painter.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GEventLoop.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GCheckBox.h>

class ClockWidget final : public GWidget {
public:
    explicit ClockWidget(GWidget* parent = nullptr);
    virtual ~ClockWidget() override { }

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void timer_event(GTimerEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;

    dword m_last_time { 0 };
};

ClockWidget::ClockWidget(GWidget* parent)
    : GWidget(parent)
{
    set_relative_rect({ 0, 0, 100, 40 });
    startTimer(250);
}

void ClockWidget::paint_event(GPaintEvent&)
{
    auto now = time(nullptr);
    auto& tm = *localtime(&now);

    char timeBuf[128];
    sprintf(timeBuf, "%02u:%02u:%02u", tm.tm_hour, tm.tm_min, tm.tm_sec);

    Painter painter(*this);
    painter.fill_rect(rect(), Color::White);
    painter.draw_text(rect(), timeBuf, Painter::TextAlignment::Center, Color::Black);
}

void ClockWidget::timer_event(GTimerEvent&)
{
    auto now = time(nullptr);
    if (now == m_last_time)
        return;
    m_last_time = now;
    update();
}

void ClockWidget::mousedown_event(GMouseEvent&)
{
    update();
}

static GWindow* make_font_test_window();
static GWindow* make_launcher_window();
static GWindow* make_clock_window();

int main(int argc, char** argv)
{
    GEventLoop loop;
    auto* font_test_window = make_font_test_window();
    font_test_window->show();

    auto* launcher_window = make_launcher_window();
    launcher_window->show();

    auto* clock_window = make_clock_window();
    clock_window->show();

    return loop.exec();
}

GWindow* make_font_test_window()
{
    auto* window = new GWindow;
    window->set_title("Font test");
    window->set_rect({ 440, 100, 300, 80 });

    auto* widget = new GWidget;
    window->set_main_widget(widget);
    widget->set_relative_rect({ 0, 0, 300, 80 });

    auto* l1 = new GLabel(widget);
    l1->set_relative_rect({ 0, 0, 300, 20 });
    l1->set_text("0123456789");

    auto* l2 = new GLabel(widget);
    l2->set_relative_rect({ 0, 20, 300, 20 });
    l2->set_text("ABCDEFGHIJKLMNOPQRSTUVWXYZ");

    auto* l3 = new GLabel(widget);
    l3->set_relative_rect({ 0, 40, 300, 20 });
    l3->set_text("abcdefghijklmnopqrstuvwxyz");

    auto* l4 = new GLabel(widget);
    l4->set_relative_rect({ 0, 60, 300, 20 });
    l4->set_text("!\"#$%&'()*+,-./:;<=>?@[\\]^_{|}~");

    return window;
}

GWindow* make_launcher_window()
{
    auto* window = new GWindow;
    window->set_title("Launcher");
    window->set_rect({ 100, 400, 100, 230 });

    auto* widget = new GWidget;
    window->set_main_widget(widget);
    widget->set_relative_rect({ 0, 0, 100, 230 });

    auto* label = new GLabel(widget);
    label->set_relative_rect({ 0, 0, 100, 20 });
    label->set_text("Apps");

    auto* terminal_button = new GButton(widget);
    terminal_button->set_relative_rect({ 5, 20, 90, 20 });
    terminal_button->set_caption("Terminal");

    terminal_button->on_click = [label] (GButton&) {
        pid_t child_pid = fork();
        if (!child_pid) {
            execve("/bin/Terminal", nullptr, nullptr);
            ASSERT_NOT_REACHED();
        } else {
            char buffer[32];
            sprintf(buffer, "PID: %d", child_pid);
            label->set_text(buffer);
        }
    };

    auto* guitest_button = new GButton(widget);
    guitest_button->set_relative_rect({ 5, 50, 90, 20 });
    guitest_button->set_caption("guitest");

    guitest_button->on_click = [label] (GButton&) {
        pid_t child_pid = fork();
        if (!child_pid) {
            execve("/bin/guitest", nullptr, nullptr);
            ASSERT_NOT_REACHED();
        } else {
            char buffer[32];
            sprintf(buffer, "PID: %d", child_pid);
            label->set_text(buffer);
        }
    };

    auto* dummy_button = new GButton(widget);
    dummy_button->set_relative_rect({ 5, 80, 90, 20 });
    dummy_button->set_caption("Dummy");

    auto* textbox = new GTextBox(widget);
    textbox->set_relative_rect({ 5, 110, 90, 20 });
    textbox->on_return_pressed = [window] (GTextBox& textbox) {
        window->set_title(textbox.text());
    };

    auto* other_textbox = new GTextBox(widget);
    other_textbox->set_relative_rect({ 5, 140, 90, 20 });

    auto* checkbox = new GCheckBox(widget);
    checkbox->set_relative_rect({ 5, 170, 90, 20 });
    checkbox->set_caption("CheckBox");

    window->set_focused_widget(textbox);

    auto* close_button = new GButton(widget);
    close_button->set_relative_rect({ 5, 200, 90, 20 });
    close_button->set_caption("Close");
    close_button->on_click = [window] (GButton&) {
        window->close();
    };

    return window;
}

GWindow* make_clock_window()
{
    auto* window = new GWindow;
    window->set_title("Clock");
    window->set_rect({ 200, 200, 100, 40 });

    auto* clock_widget = new ClockWidget;
    clock_widget->set_relative_rect({ 0, 0, 100, 40 });
    window->set_main_widget(clock_widget);

    return window;
}
