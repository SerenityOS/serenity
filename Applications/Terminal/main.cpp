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
    app_menu->add_item(0, "Quit");
    app_menu->on_item_activation = [] (unsigned identifier) {
        if (identifier == 0) {
            dbgprintf("Terminal: Quit menu activated!\n");
            GApplication::the().exit(0);
            return;
        }
    };
    menubar->add_menu(move(app_menu));

    auto font_menu = make<GMenu>("Font");
    font_menu->add_item(0, "Liza Thin");
    font_menu->add_item(1, "Liza Regular");
    font_menu->add_item(2, "Liza Bold");
    font_menu->on_item_activation = [&terminal] (unsigned identifier) {
        switch (identifier) {
        case 0:
            terminal.set_font(Font::load_from_file("/res/fonts/Liza8x10.font"));
            break;
        case 1:
            terminal.set_font(Font::load_from_file("/res/fonts/LizaRegular8x10.font"));
            break;
        case 2:
            terminal.set_font(Font::load_from_file("/res/fonts/LizaBold8x10.font"));
            break;
        }
        terminal.force_repaint();
    };
    menubar->add_menu(move(font_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_item(0, "About");
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    return app.exec();
}
