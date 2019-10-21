#include "TerminalWrapper.h"
#include <AK/String.h>
#include <LibCore/CConfigFile.h>
#include <LibGUI/GBoxLayout.h>
#include <LibVT/TerminalWidget.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <unistd.h>

static void run_command(int ptm_fd, const String& command)
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
        (void)ioctl(0, TIOCNOTTY);

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
        const char* args[4] = { "/bin/Shell", nullptr, nullptr, nullptr };
        if (!command.is_empty()) {
            args[1] = "-c";
            args[2] = command.characters();
        }
        const char* envs[] = { "TERM=xterm", "PATH=/bin:/usr/bin:/usr/local/bin", nullptr };
        rc = execve("/bin/Shell", const_cast<char**>(args), const_cast<char**>(envs));
        if (rc < 0) {
            perror("execve");
            exit(1);
        }
        ASSERT_NOT_REACHED();
    }
}

TerminalWrapper::TerminalWrapper(GWidget* parent)
    : GWidget(parent)
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));

    int ptm_fd = open("/dev/ptmx", O_RDWR);
    if (ptm_fd < 0) {
        perror("open(ptmx)");
        ASSERT_NOT_REACHED();
    }

    run_command(ptm_fd, "/bin/Shell");

    RefPtr<CConfigFile> config = CConfigFile::get_for_app("Terminal");
    m_terminal_widget = TerminalWidget::construct(ptm_fd, config);
    add_child(*m_terminal_widget);
}

TerminalWrapper::~TerminalWrapper()
{
}
