#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <assert.h>
#include <sys/ioctl.h>
#include <sys/select.h>
#include <LibC/gui.h>
#include "Terminal.h"
#include <Kernel/KeyCode.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GAction.h>

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
        rc = execvp("/bin/sh", nullptr);
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
    window->set_should_exit_app_on_close(true);

    Terminal terminal(ptm_fd);
    window->set_main_widget(&terminal);
    window->move_to(300, 300);
    window->show();

    auto menubar = make<GMenuBar>();

    auto app_menu = make<GMenu>("Terminal");
    app_menu->add_action(make<GAction>("Quit", String(), [] (const GAction&) {
        dbgprintf("Terminal: Quit menu activated!\n");
        GApplication::the().exit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto font_menu = make<GMenu>("Font");
    auto handle_font_selection = [&terminal] (const GAction& action) {
        terminal.set_font(Font::load_from_file(action.custom_data()));
        terminal.force_repaint();
    };
    font_menu->add_action(make<GAction>("Liza Thin", "/res/fonts/LizaThin8x10.font", move(handle_font_selection)));
    font_menu->add_action(make<GAction>("Liza Regular", "/res/fonts/LizaRegular8x10.font", move(handle_font_selection)));
    font_menu->add_action(make<GAction>("Liza Bold", "/res/fonts/LizaBold8x10.font", move(handle_font_selection)));

    menubar->add_menu(move(font_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(make<GAction>("About", [] (const GAction&) {
        dbgprintf("FIXME: Implement Help/About\n");
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    return app.exec();
}
