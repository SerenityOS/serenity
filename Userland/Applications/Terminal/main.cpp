/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/URL.h>
#include <LibConfig/Client.h>
#include <LibConfig/Listener.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Process.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Event.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibMain/Main.h>
#include <LibVT/TerminalWidget.h>
#include <assert.h>
#include <errno.h>
#include <pty.h>
#include <pwd.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <unistd.h>

class TerminalChangeListener : public Config::Listener {
public:
    TerminalChangeListener(VT::TerminalWidget& parent_terminal)
        : m_parent_terminal(parent_terminal)
    {
    }

    virtual void config_bool_did_change(String const& domain, String const& group, String const& key, bool value) override
    {
        VERIFY(domain == "Terminal");

        if (group == "Terminal" && key == "ShowScrollBar") {
            m_parent_terminal.set_show_scrollbar(value);
        }
    }

    virtual void config_string_did_change(String const& domain, String const& group, String const& key, String const& value) override
    {
        VERIFY(domain == "Terminal");

        if (group == "Window") {
            if (key == "Bell") {
                auto bell_mode = VT::TerminalWidget::BellMode::Visible;
                if (value == "AudibleBeep")
                    bell_mode = VT::TerminalWidget::BellMode::AudibleBeep;
                if (value == "Visible")
                    bell_mode = VT::TerminalWidget::BellMode::Visible;
                if (value == "Disabled")
                    bell_mode = VT::TerminalWidget::BellMode::Disabled;
                m_parent_terminal.set_bell_mode(bell_mode);
            } else if (key == "ColorScheme") {
                m_parent_terminal.set_color_scheme(value);
            }
        } else if (group == "Text" && key == "Font") {
            auto font = Gfx::FontDatabase::the().get_by_name(value);
            if (font.is_null())
                font = Gfx::FontDatabase::default_fixed_width_font();
            m_parent_terminal.set_font_and_resize_to_fit(*font);
            m_parent_terminal.window()->resize(m_parent_terminal.size());
        }
    }

    virtual void config_i32_did_change(String const& domain, String const& group, String const& key, i32 value) override
    {
        VERIFY(domain == "Terminal");

        if (group == "Terminal" && key == "MaxHistorySize") {
            m_parent_terminal.set_max_history_size(value);
        } else if (group == "Window" && key == "Opacity") {
            m_parent_terminal.set_opacity(value);
        }
    }

private:
    VT::TerminalWidget& m_parent_terminal;
};

static void utmp_update(String const& tty, pid_t pid, bool create)
{
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
        execl("/bin/utmpupdate", "/bin/utmpupdate", "-f", "Terminal", "-p", pid_str, (create ? "-c" : "-d"), tty.characters(), nullptr);
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

static void run_command(String command, bool keep_open)
{
    String shell = "/bin/Shell";
    auto* pw = getpwuid(getuid());
    if (pw && pw->pw_shell) {
        shell = pw->pw_shell;
    }
    endpwent();

    const char* args[5] = { shell.characters(), nullptr, nullptr, nullptr, nullptr };
    if (!command.is_empty()) {
        int arg_index = 1;
        if (keep_open)
            args[arg_index++] = "--keep-open";
        args[arg_index++] = "-c";
        args[arg_index++] = command.characters();
    }
    const char* envs[] = { "TERM=xterm", "PAGER=more", "PATH=/usr/local/bin:/usr/bin:/bin", nullptr };
    int rc = execve(shell.characters(), const_cast<char**>(args), const_cast<char**>(envs));
    if (rc < 0) {
        perror("execve");
        exit(1);
    }
    VERIFY_NOT_REACHED();
}

static ErrorOr<NonnullRefPtr<GUI::Window>> create_find_window(VT::TerminalWidget& terminal)
{
    auto window = TRY(GUI::Window::try_create());
    window->set_window_type(GUI::WindowType::ToolWindow);
    window->set_title("Find in Terminal");
    window->set_resizable(false);
    window->resize(300, 90);

    auto main_widget = TRY(window->try_set_main_widget<GUI::Widget>());
    main_widget->set_fill_with_background_color(true);
    main_widget->set_background_role(ColorRole::Button);
    (void)TRY(main_widget->try_set_layout<GUI::VerticalBoxLayout>());
    main_widget->layout()->set_margins(4);

    auto find = TRY(main_widget->try_add<GUI::Widget>());
    (void)TRY(find->try_set_layout<GUI::HorizontalBoxLayout>());
    find->layout()->set_margins(4);
    find->set_fixed_height(30);

    auto find_textbox = TRY(find->try_add<GUI::TextBox>());
    find_textbox->set_fixed_width(230);
    find_textbox->set_focus(true);
    if (terminal.has_selection())
        find_textbox->set_text(terminal.selected_text().replace("\n", " ", true));
    auto find_backwards = TRY(find->try_add<GUI::Button>());
    find_backwards->set_fixed_width(25);
    find_backwards->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/upward-triangle.png").release_value_but_fixme_should_propagate_errors());
    auto find_forwards = TRY(find->try_add<GUI::Button>());
    find_forwards->set_fixed_width(25);
    find_forwards->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/downward-triangle.png").release_value_but_fixme_should_propagate_errors());

    find_textbox->on_return_pressed = [find_backwards]() mutable {
        find_backwards->click();
    };

    find_textbox->on_shift_return_pressed = [find_forwards]() mutable {
        find_forwards->click();
    };

    auto match_case = TRY(main_widget->try_add<GUI::CheckBox>("Case sensitive"));
    auto wrap_around = TRY(main_widget->try_add<GUI::CheckBox>("Wrap around"));

    find_backwards->on_click = [&terminal, find_textbox, match_case, wrap_around](auto) mutable {
        auto needle = find_textbox->text();
        if (needle.is_empty()) {
            return;
        }

        auto found_range = terminal.find_previous(needle, terminal.normalized_selection().start(), match_case->is_checked(), wrap_around->is_checked());

        if (found_range.is_valid()) {
            terminal.scroll_to_row(found_range.start().row());
            terminal.set_selection(found_range);
        }
    };
    find_forwards->on_click = [&terminal, find_textbox, match_case, wrap_around](auto) {
        auto needle = find_textbox->text();
        if (needle.is_empty()) {
            return;
        }

        auto found_range = terminal.find_next(needle, terminal.normalized_selection().end(), match_case->is_checked(), wrap_around->is_checked());

        if (found_range.is_valid()) {
            terminal.scroll_to_row(found_range.start().row());
            terminal.set_selection(found_range);
        }
    };

    return window;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio tty rpath cpath wpath recvfd sendfd proc exec unix sigaction"));

    struct sigaction act;
    memset(&act, 0, sizeof(act));
    act.sa_flags = SA_NOCLDWAIT;
    act.sa_handler = SIG_IGN;

    TRY(Core::System::sigaction(SIGCHLD, &act, nullptr));

    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::pledge("stdio tty rpath cpath wpath recvfd sendfd proc exec unix"));

    Config::pledge_domain("Terminal");

    const char* command_to_execute = nullptr;
    bool keep_open = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(command_to_execute, "Execute this command inside the terminal", nullptr, 'e', "command");
    args_parser.add_option(keep_open, "Keep the terminal open after the command has finished executing", nullptr, 'k');

    args_parser.parse(arguments);

    if (keep_open && !command_to_execute) {
        warnln("Option -k can only be used in combination with -e.");
        return 1;
    }

    int ptm_fd;
    pid_t shell_pid = forkpty(&ptm_fd, nullptr, nullptr, nullptr);
    if (shell_pid < 0) {
        perror("forkpty");
        return 1;
    }
    if (shell_pid == 0) {
        close(ptm_fd);
        if (command_to_execute)
            run_command(command_to_execute, keep_open);
        else
            run_command(Config::read_string("Terminal", "Startup", "Command", ""), false);
        VERIFY_NOT_REACHED();
    }

    auto ptsname = TRY(Core::System::ptsname(ptm_fd));
    utmp_update(ptsname, shell_pid, true);

    auto app_icon = GUI::Icon::default_icon("app-terminal");

    auto window = TRY(GUI::Window::try_create());
    window->set_title("Terminal");
    window->set_double_buffering_enabled(false);

    auto terminal = TRY(window->try_set_main_widget<VT::TerminalWidget>(ptm_fd, true));
    terminal->on_command_exit = [&] {
        app->quit(0);
    };
    terminal->on_title_change = [&](auto title) {
        window->set_title(title);
    };
    terminal->on_terminal_size_change = [&](auto& size) {
        window->resize(size);
    };
    terminal->apply_size_increments_to_window(*window);
    window->set_icon(app_icon.bitmap_for_size(16));

    Config::monitor_domain("Terminal");
    TerminalChangeListener listener { terminal };

    auto bell = Config::read_string("Terminal", "Window", "Bell", "Visible");
    if (bell == "AudibleBeep") {
        terminal->set_bell_mode(VT::TerminalWidget::BellMode::AudibleBeep);
    } else if (bell == "Disabled") {
        terminal->set_bell_mode(VT::TerminalWidget::BellMode::Disabled);
    } else {
        terminal->set_bell_mode(VT::TerminalWidget::BellMode::Visible);
    }

    auto find_window = TRY(create_find_window(terminal));

    auto new_opacity = Config::read_i32("Terminal", "Window", "Opacity", 255);
    terminal->set_opacity(new_opacity);
    window->set_has_alpha_channel(new_opacity < 255);

    auto new_scrollback_size = Config::read_i32("Terminal", "Terminal", "MaxHistorySize", terminal->max_history_size());
    terminal->set_max_history_size(new_scrollback_size);

    auto show_scroll_bar = Config::read_bool("Terminal", "Terminal", "ShowScrollBar", true);
    terminal->set_show_scrollbar(show_scroll_bar);

    auto open_settings_action = GUI::Action::create("&Settings", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/settings.png").release_value_but_fixme_should_propagate_errors(),
        [&](auto&) {
            Core::Process::spawn("/bin/TerminalSettings");
        });

    TRY(terminal->context_menu().try_add_separator());
    TRY(terminal->context_menu().try_add_action(open_settings_action));

    auto file_menu = TRY(window->try_add_menu("&File"));
    TRY(file_menu->try_add_action(GUI::Action::create("Open New &Terminal", { Mod_Ctrl | Mod_Shift, Key_N }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-terminal.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        Core::Process::spawn("/bin/Terminal");
    })));

    TRY(file_menu->try_add_action(open_settings_action));
    TRY(file_menu->try_add_separator());
    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([](auto&) {
        dbgln("Terminal: Quit menu activated!");
        GUI::Application::the()->quit();
    })));

    auto edit_menu = TRY(window->try_add_menu("&Edit"));
    TRY(edit_menu->try_add_action(terminal->copy_action()));
    TRY(edit_menu->try_add_action(terminal->paste_action()));
    TRY(edit_menu->try_add_separator());
    TRY(edit_menu->try_add_action(GUI::Action::create("&Find...", { Mod_Ctrl | Mod_Shift, Key_F }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/find.png").release_value_but_fixme_should_propagate_errors(),
        [&](auto&) {
            find_window->show();
            find_window->move_to_front();
        })));

    auto view_menu = TRY(window->try_add_menu("&View"));
    TRY(view_menu->try_add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    })));
    TRY(view_menu->try_add_action(terminal->clear_including_history_action()));

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man1/Terminal.md"), "/bin/Help");
    })));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Terminal", app_icon, window)));

    window->on_close = [&]() {
        find_window->close();
    };

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/bin", "r"));
    TRY(Core::System::unveil("/bin/Terminal", "x"));
    TRY(Core::System::unveil("/bin/TerminalSettings", "x"));
    TRY(Core::System::unveil("/bin/utmpupdate", "x"));
    TRY(Core::System::unveil("/etc/FileIconProvider.ini", "r"));
    TRY(Core::System::unveil("/tmp/portal/launch", "rw"));
    TRY(Core::System::unveil("/tmp/portal/config", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    window->show();
    int result = app->exec();
    dbgln("Exiting terminal, updating utmp");
    utmp_update(ptsname, 0, false);
    return result;
}
