/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/URL.h>
#include <Applications/Terminal/TerminalSettingsWindowGML.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Event.h>
#include <LibGUI/FontPicker.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/OpacitySlider.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <LibVT/TerminalWidget.h>
#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <pwd.h>
#include <serenity.h>
#include <signal.h>
#include <spawn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/select.h>
#include <sys/wait.h>
#include <unistd.h>

static void utmp_update(const char* tty, pid_t pid, bool create)
{
    if (!tty)
        return;
    int utmpupdate_pid = fork();
    if (utmpupdate_pid < 0) {
        perror("fork");
        return;
    }
    if (utmpupdate_pid == 0) {
        // Be careful here! Because fork() only clones one thread it's
        // possible that we deadlock on anything involving a mutex,
        // including the heap! So resort to low-level APIs
        char pid_str[32];
        snprintf(pid_str, sizeof(pid_str), "%d", pid);
        execl("/bin/utmpupdate", "/bin/utmpupdate", "-f", "Terminal", "-p", pid_str, (create ? "-c" : "-d"), tty, nullptr);
    } else {
    wait_again:
        int status = 0;
        if (waitpid(utmpupdate_pid, &status, 0) < 0) {
            int err = errno;
            if (err == EINTR)
                goto wait_again;
            perror("waitpid");
            return;
        }
        if (WIFEXITED(status) && WEXITSTATUS(status) != 0)
            dbgln("Terminal: utmpupdate exited with status {}", WEXITSTATUS(status));
        else if (WIFSIGNALED(status))
            dbgln("Terminal: utmpupdate exited due to unhandled signal {}", WTERMSIG(status));
    }
}

static pid_t run_command(int ptm_fd, String command)
{
    pid_t pid = fork();
    if (pid < 0) {
        perror("fork");
        dbgln("run_command: could not fork to run '{}'", command);
        return pid;
    }

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

        if (setsid() < 0) {
            perror("setsid");
        }

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

        String shell = "/bin/Shell";
        auto* pw = getpwuid(getuid());
        if (pw && pw->pw_shell) {
            shell = pw->pw_shell;
        }
        endpwent();

        const char* args[4] = { shell.characters(), nullptr, nullptr, nullptr };
        if (!command.is_empty()) {
            args[1] = "-c";
            args[2] = command.characters();
        }
        const char* envs[] = { "TERM=xterm", "PAGER=more", "PATH=/bin:/usr/bin:/usr/local/bin", nullptr };
        rc = execve(shell.characters(), const_cast<char**>(args), const_cast<char**>(envs));
        if (rc < 0) {
            perror("execve");
            exit(1);
        }
        VERIFY_NOT_REACHED();
    }

    return pid;
}

static RefPtr<GUI::Window> create_settings_window(VT::TerminalWidget& terminal)
{
    auto window = GUI::Window::construct();
    window->set_window_type(GUI::WindowType::ToolWindow);
    window->set_title("Terminal settings");
    window->set_resizable(false);
    window->resize(200, 210);
    window->center_within(*terminal.window());

    auto& settings = window->set_main_widget<GUI::Widget>();
    settings.load_from_gml(terminal_settings_window_gml);

    auto& beep_bell_radio = *settings.find_descendant_of_type_named<GUI::RadioButton>("beep_bell_radio");
    auto& visual_bell_radio = *settings.find_descendant_of_type_named<GUI::RadioButton>("visual_bell_radio");
    auto& no_bell_radio = *settings.find_descendant_of_type_named<GUI::RadioButton>("no_bell_radio");

    switch (terminal.bell_mode()) {
    case VT::TerminalWidget::BellMode::Visible:
        visual_bell_radio.set_checked(true);
        break;
    case VT::TerminalWidget::BellMode::AudibleBeep:
        beep_bell_radio.set_checked(true);
        break;
    case VT::TerminalWidget::BellMode::Disabled:
        no_bell_radio.set_checked(true);
        break;
    }

    beep_bell_radio.on_checked = [&terminal](bool) {
        terminal.set_bell_mode(VT::TerminalWidget::BellMode::AudibleBeep);
    };
    visual_bell_radio.on_checked = [&terminal](bool) {
        terminal.set_bell_mode(VT::TerminalWidget::BellMode::Visible);
    };
    no_bell_radio.on_checked = [&terminal](bool) {
        terminal.set_bell_mode(VT::TerminalWidget::BellMode::Disabled);
    };

    auto& slider = *settings.find_descendant_of_type_named<GUI::OpacitySlider>("background_opacity_slider");
    slider.on_change = [&terminal](int value) {
        terminal.set_opacity(value);
    };
    slider.set_value(terminal.opacity());

    auto& history_size_spinbox = *settings.find_descendant_of_type_named<GUI::SpinBox>("history_size_spinbox");
    history_size_spinbox.set_value(terminal.max_history_size());
    history_size_spinbox.on_change = [&terminal](int value) {
        terminal.set_max_history_size(value);
    };

    return window;
}

static RefPtr<GUI::Window> create_find_window(VT::TerminalWidget& terminal)
{
    auto window = GUI::Window::construct();
    window->set_window_type(GUI::WindowType::ToolWindow);
    window->set_title("Find in Terminal");
    window->set_resizable(false);
    window->resize(300, 90);

    auto& search = window->set_main_widget<GUI::Widget>();
    search.set_fill_with_background_color(true);
    search.set_background_role(ColorRole::Button);
    search.set_layout<GUI::VerticalBoxLayout>();
    search.layout()->set_margins({ 4, 4, 4, 4 });

    auto& find = search.add<GUI::Widget>();
    find.set_layout<GUI::HorizontalBoxLayout>();
    find.layout()->set_margins({ 4, 4, 4, 4 });
    find.set_fixed_height(30);

    auto& find_textbox = find.add<GUI::TextBox>();
    find_textbox.set_fixed_width(230);
    find_textbox.set_focus(true);
    if (terminal.has_selection()) {
        String selected_text = terminal.selected_text();
        selected_text.replace("\n", " ", true);
        find_textbox.set_text(selected_text);
    }
    auto& find_backwards = find.add<GUI::Button>();
    find_backwards.set_fixed_width(25);
    find_backwards.set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/upward-triangle.png"));
    auto& find_forwards = find.add<GUI::Button>();
    find_forwards.set_fixed_width(25);
    find_forwards.set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/downward-triangle.png"));

    find_textbox.on_return_pressed = [&]() {
        find_backwards.click();
    };

    auto& match_case = search.add<GUI::CheckBox>("Case sensitive");
    auto& wrap_around = search.add<GUI::CheckBox>("Wrap around");

    find_backwards.on_click = [&](auto) {
        auto needle = find_textbox.text();
        if (needle.is_empty()) {
            return;
        }

        auto found_range = terminal.find_previous(needle, terminal.normalized_selection().start(), match_case.is_checked(), wrap_around.is_checked());

        if (found_range.is_valid()) {
            terminal.scroll_to_row(found_range.start().row());
            terminal.set_selection(found_range);
        }
    };
    find_forwards.on_click = [&](auto) {
        auto needle = find_textbox.text();
        if (needle.is_empty()) {
            return;
        }

        auto found_range = terminal.find_next(needle, terminal.normalized_selection().end(), match_case.is_checked(), wrap_around.is_checked());

        if (found_range.is_valid()) {
            terminal.scroll_to_row(found_range.start().row());
            terminal.set_selection(found_range);
        }
    };

    return window;
}

int main(int argc, char** argv)
{
    if (pledge("stdio tty rpath accept cpath wpath recvfd sendfd proc exec unix fattr sigaction", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    struct sigaction act;
    memset(&act, 0, sizeof(act));
    act.sa_flags = SA_NOCLDWAIT;
    act.sa_handler = SIG_IGN;
    int rc = sigaction(SIGCHLD, &act, nullptr);
    if (rc < 0) {
        perror("sigaction");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio tty rpath accept cpath wpath recvfd sendfd proc exec unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* command_to_execute = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(command_to_execute, "Execute this command inside the terminal", nullptr, 'e', "command");

    args_parser.parse(argc, argv);

    int ptm_fd = posix_openpt(O_RDWR | O_CLOEXEC);
    if (ptm_fd < 0) {
        perror("posix_openpt");
        return 1;
    }
    if (grantpt(ptm_fd) < 0) {
        perror("grantpt");
        return 1;
    }
    if (unlockpt(ptm_fd) < 0) {
        perror("unlockpt");
        return 1;
    }

    RefPtr<Core::ConfigFile> config = Core::ConfigFile::get_for_app("Terminal");
    Core::File::ensure_parent_directories(config->file_name());

    pid_t shell_pid = 0;

    if (command_to_execute)
        shell_pid = run_command(ptm_fd, command_to_execute);
    else
        shell_pid = run_command(ptm_fd, config->read_entry("Startup", "Command", ""));

    auto* pts_name = ptsname(ptm_fd);
    utmp_update(pts_name, shell_pid, true);

    auto app_icon = GUI::Icon::default_icon("app-terminal");

    auto window = GUI::Window::construct();
    window->set_title("Terminal");
    window->set_background_color(Color::Black);
    window->set_double_buffering_enabled(false);

    auto& terminal = window->set_main_widget<VT::TerminalWidget>(ptm_fd, true, config);
    terminal.on_command_exit = [&] {
        app->quit(0);
    };
    terminal.on_title_change = [&](auto& title) {
        window->set_title(title);
    };
    terminal.on_terminal_size_change = [&](auto& size) {
        window->resize(size);
    };
    terminal.apply_size_increments_to_window(*window);
    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));

    auto bell = config->read_entry("Window", "Bell", "Visible");
    if (bell == "AudibleBeep") {
        terminal.set_bell_mode(VT::TerminalWidget::BellMode::AudibleBeep);
    } else if (bell == "Disabled") {
        terminal.set_bell_mode(VT::TerminalWidget::BellMode::Disabled);
    } else {
        terminal.set_bell_mode(VT::TerminalWidget::BellMode::Visible);
    }

    RefPtr<GUI::Window> settings_window;
    RefPtr<GUI::Window> find_window;

    auto new_opacity = config->read_num_entry("Window", "Opacity", 255);
    terminal.set_opacity(new_opacity);
    window->set_has_alpha_channel(new_opacity < 255);

    auto new_scrollback_size = config->read_num_entry("Terminal", "MaxHistorySize", terminal.max_history_size());
    terminal.set_max_history_size(new_scrollback_size);

    auto open_settings_action = GUI::Action::create("Settings", Gfx::Bitmap::load_from_file("/res/icons/16x16/gear.png"),
        [&](const GUI::Action&) {
            if (!settings_window)
                settings_window = create_settings_window(terminal);
            settings_window->show();
            settings_window->move_to_front();
        });

    terminal.context_menu().add_separator();
    auto pick_font_action = GUI::Action::create("Terminal font...", Gfx::Bitmap::load_from_file("/res/icons/16x16/app-font-editor.png"),
        [&](auto&) {
            auto picker = GUI::FontPicker::construct(window, &terminal.font(), true);
            if (picker->exec() == GUI::Dialog::ExecOK) {
                terminal.set_font_and_resize_to_fit(*picker->font());
                window->resize(terminal.size());
                config->write_entry("Text", "Font", picker->font()->qualified_name());
                config->sync();
            }
        });

    terminal.context_menu().add_action(pick_font_action);

    terminal.context_menu().add_separator();
    terminal.context_menu().add_action(open_settings_action);

    auto menubar = GUI::MenuBar::construct();

    auto& app_menu = menubar->add_menu("Terminal");
    app_menu.add_action(GUI::Action::create("Open new Terminal", { Mod_Ctrl | Mod_Shift, Key_N }, Gfx::Bitmap::load_from_file("/res/icons/16x16/app-terminal.png"), [&](auto&) {
        pid_t child;
        const char* argv[] = { "Terminal", nullptr };
        if ((errno = posix_spawn(&child, "/bin/Terminal", nullptr, nullptr, const_cast<char**>(argv), environ))) {
            perror("posix_spawn");
        } else {
            if (disown(child) < 0)
                perror("disown");
        }
    }));

    app_menu.add_action(open_settings_action);
    app_menu.add_separator();
    app_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        dbgln("Terminal: Quit menu activated!");
        GUI::Application::the()->quit();
    }));

    auto& edit_menu = menubar->add_menu("Edit");
    edit_menu.add_action(terminal.copy_action());
    edit_menu.add_action(terminal.paste_action());
    edit_menu.add_separator();
    edit_menu.add_action(GUI::Action::create("Find...", { Mod_Ctrl | Mod_Shift, Key_F }, Gfx::Bitmap::load_from_file("/res/icons/16x16/find.png"),
        [&](auto&) {
            if (!find_window)
                find_window = create_find_window(terminal);
            find_window->show();
            find_window->move_to_front();
        }));

    auto& view_menu = menubar->add_menu("View");
    view_menu.add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));
    view_menu.add_action(terminal.clear_including_history_action());
    view_menu.add_separator();
    view_menu.add_action(pick_font_action);

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man1/Terminal.md"), "/bin/Help");
    }));
    help_menu.add_action(GUI::CommonActions::make_about_action("Terminal", app_icon, window));

    app->set_menubar(move(menubar));

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin/Terminal", "x") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin/utmpupdate", "x") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/FileIconProvider.ini", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/launch", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(config->file_name().characters(), "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    config->sync();
    int result = app->exec();
    dbgln("Exiting terminal, updating utmp");
    utmp_update(pts_name, 0, false);
    return result;
}
