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
#include <LibGUI/GEventLoop.h>
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

int main(int, char**)
{
    int ptm_fd = open("/dev/ptmx", O_RDWR);
    if (ptm_fd < 0) {
        perror("open(ptmx)");
        return 1;
    }

    make_shell(ptm_fd);

    GEventLoop loop;

    auto* window = new GWindow;
    window->set_should_exit_app_on_close(true);

    Terminal terminal(ptm_fd);
    window->set_main_widget(&terminal);

    GNotifier ptm_notifier(ptm_fd, GNotifier::Read);
    ptm_notifier.on_ready_to_read = [&terminal] (GNotifier& notifier) {
        byte buffer[BUFSIZ];
        ssize_t nread = read(notifier.fd(), buffer, sizeof(buffer));
        if (nread < 0) {
            dbgprintf("Terminal read error: %s\n", strerror(errno));
            perror("read(ptm)");
            GEventLoop::main().exit(1);
            return;
        }
        if (nread == 0) {
            dbgprintf("Terminal: EOF on master pty, closing.\n");
            GEventLoop::main().exit(0);
            return;
        }
        for (ssize_t i = 0; i < nread; ++i)
            terminal.on_char(buffer[i]);
        terminal.flush_dirty_lines();
    };

    window->show();

    return loop.exec();

#if 0
    for (;;) {
        fd_set rfds;
        FD_ZERO(&rfds);
        FD_SET(ptm_fd, &rfds);
        FD_SET(event_fd, &rfds);
        int nfds = select(max(ptm_fd, event_fd) + 1, &rfds, nullptr, nullptr, nullptr);
        if (nfds < 0) {
            dbgprintf("Terminal(%u) select() failed :( errno=%d\n", getpid(), errno);
            return 1;
        }

        if (FD_ISSET(ptm_fd, &rfds)) {
            byte buffer[4096];
            ssize_t nread = read(ptm_fd, buffer, sizeof(buffer));
            if (nread < 0) {
                dbgprintf("Terminal read error: %s\n", strerror(errno));
                perror("read(ptm)");
                continue;
            }
            if (nread == 0) {
                dbgprintf("Terminal: EOF on master pty, closing.\n");
                break;
            }
            for (ssize_t i = 0; i < nread; ++i)
                terminal.on_char(buffer[i]);
            terminal.update();
        }

        if (FD_ISSET(event_fd, &rfds)) {
            GUI_Event event;
            ssize_t nread = read(event_fd, &event, sizeof(event));
            if (nread < 0) {
                perror("read(event)");
                return 1;
            }

            assert(nread != 0);
            assert(nread == sizeof(event));

            if (event.type == GUI_Event::Type::Paint) {
                terminal.update();
            } else if (event.type == GUI_Event::Type::KeyDown) {
                char ch = event.key.character;
                if (event.key.ctrl) {
                    if (ch >= 'a' && ch <= 'z') {
                        ch = ch - 'a' + 1;
                    } else if (ch == '\\') {
                        ch = 0x1c;
                    }
                }
                switch (event.key.key) {
                case KeyCode::Key_Up:
                    write(ptm_fd, "\033[A", 3);
                    break;
                case KeyCode::Key_Down:
                    write(ptm_fd, "\033[B", 3);
                    break;
                case KeyCode::Key_Right:
                    write(ptm_fd, "\033[C", 3);
                    break;
                case KeyCode::Key_Left:
                    write(ptm_fd, "\033[D", 3);
                    break;
                default:
                    write(ptm_fd, &ch, 1);
                }
            } else if (event.type == GUI_Event::Type::WindowActivated) {
                terminal.set_in_active_window(true);
            } else if (event.type == GUI_Event::Type::WindowDeactivated) {
                terminal.set_in_active_window(false);
            } else if (event.type == GUI_Event::Type::WindowCloseRequest) {
                return 0;
            }
        }
    }
#endif
    return 0;
}
