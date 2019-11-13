#include "TerminalWrapper.h"
#include "ProcessStateWidget.h"
#include <AK/String.h>
#include <LibCore/CConfigFile.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GMessageBox.h>
#include <LibVT/TerminalWidget.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <unistd.h>

void TerminalWrapper::run_command(const String& command)
{
    if (m_pid != -1) {
        GMessageBox::show(
            "A command is already running in this TerminalWrapper",
            "Can't run command",
            GMessageBox::Type::Error,
            GMessageBox::InputType::OK,
            window());
        return;
    }

    int ptm_fd = open("/dev/ptmx", O_RDWR | O_CLOEXEC);
    if (ptm_fd < 0) {
        perror("open(ptmx)");
        ASSERT_NOT_REACHED();
    }

    m_terminal_widget->set_pty_master_fd(ptm_fd);
    m_terminal_widget->on_command_exit = [this] {
        int wstatus;
        int rc = waitpid(m_pid, &wstatus, 0);
        if (rc < 0) {
            perror("waitpid");
            ASSERT_NOT_REACHED();
        }
        if (WIFEXITED(wstatus)) {
            m_terminal_widget->inject_string(String::format("\033[%d;1m(Command exited with code %d)\033[0m\n", wstatus == 0 ? 32 : 31, WEXITSTATUS(wstatus)));
        } else if (WIFSTOPPED(wstatus)) {
            m_terminal_widget->inject_string(String::format("\033[34;1m(Command stopped!)\033[0m\n"));
        } else if (WIFSIGNALED(wstatus)) {
            m_terminal_widget->inject_string(String::format("\033[34;1m(Command signaled with %s!)\033[0m\n", strsignal(WTERMSIG(wstatus))));
        }
        m_process_state_widget->set_tty_fd(-1);
        m_pid = -1;
    };

    m_pid = fork();
    if (m_pid == 0) {
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

    // Parent process, cont'd.
    m_process_state_widget->set_tty_fd(ptm_fd);
}

TerminalWrapper::TerminalWrapper(GWidget* parent)
    : GWidget(parent)
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));

    RefPtr<CConfigFile> config = CConfigFile::get_for_app("Terminal");
    m_terminal_widget = TerminalWidget::construct(-1, false, config);
    add_child(*m_terminal_widget);

    m_process_state_widget = ProcessStateWidget::construct(this);
}

TerminalWrapper::~TerminalWrapper()
{
}
