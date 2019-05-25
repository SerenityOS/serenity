#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <assert.h>
#include <sys/ioctl.h>
#include <sys/select.h>
#include "Terminal.h"
#include <Kernel/KeyCode.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GFontDatabase.h>
#include <LibGUI/GSlider.h>

static void make_shell(int ptm_fd)
{
    pid_t pid = fork();
    if (pid == 0) {
        const char* tty_name = ptsname(ptm_fd);
        if (!tty_name) {
            perror("ptsname");
            exit(1);
        }
        close(ptm_fd);
        int pts_fd = open(tty_name, O_RDWR);
        if (pts_fd < 0) {
            perror("open");
            exit(1);
        }

        // NOTE: It's okay if this fails.
        (void) ioctl(0, TIOCNOTTY);

        close(0);
        close(1);
        close(2);

        int rc = dup2(pts_fd, 0);
        if (rc < 0) {
            perror("dup2");
            exit(1);
        }
        rc = dup2(pts_fd, 1);
        if (rc < 0) {
            perror("dup2");
            exit(1);
        }
        rc = dup2(pts_fd, 2);
        if (rc < 0) {
            perror("dup2");
            exit(1);
        }
        rc = close(pts_fd);
        if (rc < 0) {
            perror("close");
            exit(1);
        }
        rc = ioctl(0, TIOCSCTTY);
        if (rc < 0) {
            perror("ioctl(TIOCSCTTY)");
            exit(1);
        }
        char* args[] = { "/bin/Shell", nullptr };
        char* envs[] = { "TERM=xterm", "PATH=/bin:/usr/bin", nullptr };
        rc = execve("/bin/Shell", args, envs);
        if (rc < 0) {
            perror("execve");
            exit(1);
        }
        ASSERT_NOT_REACHED();
    }
}

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    int ptm_fd = open("/dev/ptmx", O_RDWR);
    if (ptm_fd < 0) {
        perror("open(ptmx)");
        return 1;
    }

    make_shell(ptm_fd);

    auto* window = new GWindow;
    window->set_title("Terminal");
    window->set_background_color(Color::Black);
    window->set_double_buffering_enabled(false);
    window->set_should_exit_event_loop_on_close(true);

    RetainPtr<CConfigFile> config = CConfigFile::get_for_app("Terminal");
    Terminal terminal(ptm_fd, config);
    window->set_has_alpha_channel(true);
    window->set_main_widget(&terminal);
    window->move_to(300, 300);
    terminal.apply_size_increments_to_window(*window);
    window->show();
    window->set_icon_path("/res/icons/16x16/app-terminal.png");

    auto* opacity_adjustment_window = new GWindow;
    opacity_adjustment_window->set_title("Adjust opacity");
    opacity_adjustment_window->set_rect(50, 50, 200, 100);

    auto* slider = new GSlider(nullptr);
    opacity_adjustment_window->set_main_widget(slider);
    slider->set_fill_with_background_color(true);
    slider->set_background_color(Color::LightGray);

    slider->on_value_changed = [&terminal] (int value) {
        float opacity = value / 100.0;
        terminal.set_opacity(opacity);
    };

    slider->set_range(0, 100);
    slider->set_value(100);

    auto new_opacity = config->read_num_entry("Window", "Opacity", 255);
    terminal.set_opacity((float)new_opacity / 255.0);

    auto menubar = make<GMenuBar>();

    auto app_menu = make<GMenu>("Terminal");
    app_menu->add_action(GAction::create("Adjust opacity...", [opacity_adjustment_window] (const GAction&) {
        opacity_adjustment_window->show();
    }));
    app_menu->add_action(GAction::create("Quit", { Mod_Alt, Key_F4 }, [] (const GAction&) {
        dbgprintf("Terminal: Quit menu activated!\n");
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto font_menu = make<GMenu>("Font");
    GFontDatabase::the().for_each_fixed_width_font([&] (const String& font_name) {
        font_menu->add_action(GAction::create(font_name, [&terminal] (const GAction& action) {
            terminal.set_font(GFontDatabase::the().get_by_name(action.text()));
            auto metadata = GFontDatabase::the().get_metadata_by_name(action.text());
            terminal.config()->write_entry("Text", "Font", metadata.path);
            terminal.config()->sync();
            terminal.force_repaint();
        }));
    });
    menubar->add_menu(move(font_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [] (const GAction&) {
        dbgprintf("FIXME: Implement Help/About\n");
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    config->sync();
    return app.exec();
}
