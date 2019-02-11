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
#include <LibGUI/GNotifier.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>

static void make_shell(int ptm_fd)
{
    pid_t pid = fork();
    if (pid == 0) {
        const char* tty_name = ptsname(ptm_fd);
        if (!tty_name) {
            perror("ptsname");
            exit(1);
        }
        int rc = 0;
        close(ptm_fd);
        int pts_fd = open(tty_name, O_RDWR);
        rc = ioctl(0, TIOCNOTTY);
        if (rc < 0) {
            perror("ioctl(TIOCNOTTY)");
            exit(1);
        }
        close(0);
        close(1);
        close(2);
        dup2(pts_fd, 0);
        dup2(pts_fd, 1);
        dup2(pts_fd, 2);
        close(pts_fd);
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

    GNotifier ptm_notifier(ptm_fd, GNotifier::Read);
    ptm_notifier.on_ready_to_read = [&terminal] (GNotifier& notifier) {
        byte buffer[BUFSIZ];
        ssize_t nread = read(notifier.fd(), buffer, sizeof(buffer));
        if (nread < 0) {
            dbgprintf("Terminal read error: %s\n", strerror(errno));
            perror("read(ptm)");
            GApplication::the().exit(1);
            return;
        }
        if (nread == 0) {
            dbgprintf("Terminal: EOF on master pty, closing.\n");
            GApplication::the().exit(0);
            return;
        }
        for (ssize_t i = 0; i < nread; ++i)
            terminal.on_char(buffer[i]);
        terminal.flush_dirty_lines();
    };

    window->show();

    return app.exec();
}
