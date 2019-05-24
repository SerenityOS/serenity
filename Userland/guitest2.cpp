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
#include <LibGUI/GPainter.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GCheckBox.h>
#include <LibGUI/GProgressBar.h>
#include <LibGUI/GApplication.h>
#include <signal.h>

static GWindow* make_launcher_window();
static GWindow* make_progress_window();
static GWindow* make_frames_window();

void handle_sigchld(int)
{
    dbgprintf("Got SIGCHLD\n");
    int pid = waitpid(-1, nullptr, 0);
    dbgprintf("waitpid() returned %d\n", pid);
    ASSERT(pid > 0);
}

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    signal(SIGCHLD, handle_sigchld);

    auto* launcher_window = make_launcher_window();
    launcher_window->set_should_exit_event_loop_on_close(true);
    launcher_window->show();

    auto* progress_window = make_progress_window();
    progress_window->show();

    auto* frames_window = make_frames_window();
    frames_window->show();

    return app.exec();
}

GWindow* make_launcher_window()
{
    auto* window = new GWindow;
    window->set_title("GUI Test II");
    window->set_rect({ 100, 400, 100, 230 });

    auto* widget = new GWidget;
    widget->set_fill_with_background_color(true);
    window->set_main_widget(widget);

    auto* label = new GLabel(widget);
    label->set_relative_rect({ 0, 0, 100, 20 });
    label->set_text("Apps");

    auto* terminal_button = new GButton(widget);
    terminal_button->set_relative_rect({ 5, 20, 90, 20 });
    terminal_button->set_text("Terminal");

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
    guitest_button->set_text("guitest");

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
    dummy_button->set_text("Dummy");

    auto* textbox = new GTextBox(widget);
    textbox->set_relative_rect({ 5, 110, 90, 20 });
    textbox->on_return_pressed = [window, textbox] {
        window->set_title(textbox->text());
    };

    auto* other_textbox = new GTextBox(widget);
    other_textbox->set_relative_rect({ 5, 140, 90, 20 });
    other_textbox->set_text("Hello there I am text.");

    auto* checkbox = new GCheckBox(widget);
    checkbox->set_relative_rect({ 5, 170, 90, 20 });
    checkbox->set_caption("CheckBox");

    window->set_focused_widget(textbox);

    auto* close_button = new GButton(widget);
    close_button->set_relative_rect({ 5, 200, 90, 20 });
    close_button->set_text("Close");
    close_button->on_click = [window] (GButton&) {
        window->close();
    };

    return window;
}

static GWindow* make_progress_window()
{
    auto* window = new GWindow;
    window->set_title("Progress bar test");
    window->set_rect({ 100, 400, 240, 80 });

    auto* widget = new GWidget;
    widget->set_fill_with_background_color(true);
    window->set_main_widget(widget);

    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    widget->layout()->set_margins({ 8, 8, 8, 8 });

    auto* label = new GLabel("Hi /dpt/", widget);
    label->set_size_policy(SizePolicy::Fill, SizePolicy::Fill);

    auto* progress_bar = new GProgressBar(widget);
    progress_bar->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    progress_bar->set_preferred_size({ 200, 20 });

    progress_bar->set_range(0, 100);
    progress_bar->set_value(25);

    return window;
}

static GWindow* make_frames_window()
{
    auto* window = new GWindow;
    window->set_title("GFrame styles test");
    window->set_rect({ 100, 400, 240, 80 });

    auto* widget = new GWidget;
    widget->set_fill_with_background_color(true);
    window->set_main_widget(widget);

    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    widget->layout()->set_margins({ 8, 8, 8, 8 });
    widget->layout()->set_spacing(8);

    auto add_label = [widget] (const String& text, FrameShape shape, FrameShadow shadow) {
        auto* label = new GLabel(text, widget);
        label->set_size_policy(SizePolicy::Fill, SizePolicy::Fill);
        label->set_frame_thickness(1);
        label->set_frame_shape(shape);
        label->set_frame_shadow(shadow);
        if (shape == FrameShape::Container) {
            label->set_frame_thickness(2);
            label->set_fill_with_background_color(true);
            label->set_background_color(Color::White);
        }
    };

    add_label("Panel + Raised", FrameShape::Panel, FrameShadow::Raised);
    add_label("Panel + Sunken", FrameShape::Panel, FrameShadow::Sunken);
    add_label("Panel + Plain", FrameShape::Panel, FrameShadow::Plain);
    add_label("Container + Raised", FrameShape::Container, FrameShadow::Raised);
    add_label("Container + Sunken", FrameShape::Container, FrameShadow::Sunken);
    add_label("Container + Plain", FrameShape::Container, FrameShadow::Plain);

    return window;
}
