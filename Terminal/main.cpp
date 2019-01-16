#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <assert.h>
#include <Widgets/Font.h>
#include <Widgets/GraphicsBitmap.h>
#include <Widgets/Painter.h>
#include <sys/ioctl.h>
#include <sys/select.h>
#include <gui.h>
#include "Terminal.h"

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
        dbgprintf("*** In child (%d), opening slave pty %s, pts_fd=%d\n", getpid(), tty_name, pts_fd);
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
        rc = execve("/bin/sh", nullptr, nullptr);
        if (rc < 0) {
            perror("execve");
            exit(1);
        }
        ASSERT_NOT_REACHED();
    } else {
        dbgprintf("*** In parent, child is %d\n", pid);
    }
}

static int max(int a, int b)
{
    return a > b ? a : b;
}

static int open_ptm()
{
    char buf[32];
    for (unsigned i = 0; i < 4; ++i) {
        sprintf(buf, "/dev/ptm%u", i);
        int fd = open(buf, O_RDWR);
        if (fd)
            return fd;
    }
    dbgprintf("No master PTY available :(\n");
    exit(1);
    return -1;
}

int main(int, char**)
{
    int ptm_fd = open_ptm();

    make_shell(ptm_fd);

    int event_fd = open("/dev/gui_events", O_RDONLY);
    if (event_fd < 0) {
        perror("open");
        return 1;
    }

    Terminal terminal;
    terminal.create_window();
    terminal.paint();

    for (;;) {
        fd_set rfds;
        FD_ZERO(&rfds);
        FD_SET(ptm_fd, &rfds);
        FD_SET(event_fd, &rfds);
        int nfds = select(max(ptm_fd, event_fd) + 1, &rfds, nullptr, nullptr, nullptr);
        if (nfds < 0) {
            dbgprintf("Terminal(%u) select() failed :( errno=%d\n", getpid(), errno);
        }

        if (FD_ISSET(ptm_fd, &rfds)) {
            byte buffer[1024];
            ssize_t nread = read(ptm_fd, buffer, sizeof(buffer));
            if (nread < 0) {
                perror("read(ptm)");
                continue;
            }
            assert(nread != 0);
            for (ssize_t i = 0; i < nread; ++i)
                terminal.on_char(buffer[i]);
            terminal.paint();
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
            dbgprintf("(Terminal:%d) ", getpid());
            switch (event.type) {
            case GUI_Event::Type::Paint: dbgprintf("WID=%x Paint [%d,%d %dx%d]\n", event.window_id, event.paint.rect.location.x, event.paint.rect.location.y, event.paint.rect.size.width, event.paint.rect.size.height); break;
            case GUI_Event::Type::MouseDown: dbgprintf("WID=%x MouseDown %d,%d\n", event.window_id, event.mouse.position.x, event.mouse.position.y); break;
            case GUI_Event::Type::MouseUp: dbgprintf("WID=%x MouseUp %d,%d\n", event.window_id, event.mouse.position.x, event.mouse.position.y); break;
            case GUI_Event::Type::MouseMove: dbgprintf("WID=%x MouseMove %d,%d\n", event.window_id, event.mouse.position.x, event.mouse.position.y); break;
            case GUI_Event::Type::KeyDown: dbgprintf("WID=%x KeyDown 0x%b (%c)\n", event.window_id, event.key.character, event.key.character); break;
            default:
                ASSERT_NOT_REACHED();
            }

            if (event.type == GUI_Event::Type::Paint) {
                terminal.paint();
            } else if (event.type == GUI_Event::Type::KeyDown) {
                write(ptm_fd, &event.key.character, 1);
            }
        }
    }
    return 0;
}
