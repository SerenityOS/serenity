/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/URL.h>
#include <Applications/Terminal/TerminalSettingsWindowGML.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Event.h>
#include <LibGUI/FontPicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/OpacitySlider.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>
#include <LibVT/TerminalWidget.h>
#include <assert.h>
#include <errno.h>
#include <pty.h>
#include <pwd.h>
#include <serenity.h>
#include <signal.h>
#include <spawn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
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

static void run_command(String command)
{
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
    int rc = execve(shell.characters(), const_cast<char**>(args), const_cast<char**>(envs));
    if (rc < 0) {
        perror("execve");
        exit(1);
    }
    VERIFY_NOT_REACHED();
}

static RefPtr<GUI::Window> create_settings_window(VT::TerminalWidget& terminal)
{
    auto window = GUI::Window::construct();
    window->set_window_type(GUI::WindowType::ToolWindow);
    window->set_title("Terminal settings");
    window->set_resizable(false);
    window->resize(200, 240);
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

    // The settings window takes a reference to this vector, so it needs to outlive this scope.
    // As long as we ensure that only one settings window may be open at a time (which we do),
    // this should cause no problems.
    static Vector<String> color_scheme_names;
    color_scheme_names.clear();
    Core::DirIterator iterator("/res/terminal-colors", Core::DirIterator::SkipParentAndBaseDir);
    while (iterator.has_next()) {
        auto path = iterator.next_path();
        path.replace(".ini", "");
        color_scheme_names.append(path);
    }
    quick_sort(color_scheme_names);
    auto& color_scheme_combo = *settings.find_descendant_of_type_named<GUI::ComboBox>("color_scheme_combo");
    color_scheme_combo.set_only_allow_values_from_model(true);
    color_scheme_combo.set_model(*GUI::ItemListModel<String>::create(color_scheme_names));
    color_scheme_combo.set_selected_index(color_scheme_names.find_first_index(terminal.color_scheme_name()).value());
    color_scheme_combo.on_change = [&](auto&, const GUI::ModelIndex& index) {
        terminal.set_color_scheme(index.data().as_string());
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

    find_textbox.on_shift_return_pressed = [&]() {
        find_forwards.click();
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
    if (pledge("stdio tty rpath cpath wpath recvfd sendfd proc exec unix sigaction", nullptr) < 0) {
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

    if (pledge("stdio tty rpath cpath wpath recvfd sendfd proc exec unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* command_to_execute = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(command_to_execute, "Execute this command inside the terminal", nullptr, 'e', "command");

    args_parser.parse(argc, argv);

    RefPtr<Core::ConfigFile> config = Core::ConfigFile::get_for_app("Terminal");
    Core::File::ensure_parent_directories(config->filename());

    int ptm_fd;
    pid_t shell_pid = forkpty(&ptm_fd, nullptr, nullptr, nullptr);
    if (shell_pid < 0) {
        perror("forkpty");
        return 1;
    }
    if (shell_pid == 0) {
        close(ptm_fd);
        if (command_to_execute)
            run_command(command_to_execute);
        else
            run_command(config->read_entry("Startup", "Command", ""));
        VERIFY_NOT_REACHED();
    }

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

    auto open_settings_action = GUI::Action::create("&Settings", Gfx::Bitmap::load_from_file("/res/icons/16x16/settings.png"),
        [&](const GUI::Action&) {
            if (!settings_window)
                settings_window = create_settings_window(terminal);
            settings_window->show();
            settings_window->move_to_front();
        });

    terminal.context_menu().add_separator();
    auto pick_font_action = GUI::Action::create("&Terminal Font...", Gfx::Bitmap::load_from_file("/res/icons/16x16/app-font-editor.png"),
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

    auto menubar = GUI::Menubar::construct();

    auto& file_menu = menubar->add_menu("&File");
    file_menu.add_action(GUI::Action::create("Open New &Terminal", { Mod_Ctrl | Mod_Shift, Key_N }, Gfx::Bitmap::load_from_file("/res/icons/16x16/app-terminal.png"), [&](auto&) {
        pid_t child;
        const char* argv[] = { "Terminal", nullptr };
        if ((errno = posix_spawn(&child, "/bin/Terminal", nullptr, nullptr, const_cast<char**>(argv), environ))) {
            perror("posix_spawn");
        } else {
            if (disown(child) < 0)
                perror("disown");
        }
    }));

    file_menu.add_action(open_settings_action);
    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        dbgln("Terminal: Quit menu activated!");
        GUI::Application::the()->quit();
    }));

    auto& edit_menu = menubar->add_menu("&Edit");
    edit_menu.add_action(terminal.copy_action());
    edit_menu.add_action(terminal.paste_action());
    edit_menu.add_separator();
    edit_menu.add_action(GUI::Action::create("&Find...", { Mod_Ctrl | Mod_Shift, Key_F }, Gfx::Bitmap::load_from_file("/res/icons/16x16/find.png"),
        [&](auto&) {
            if (!find_window)
                find_window = create_find_window(terminal);
            find_window->show();
            find_window->move_to_front();
        }));

    auto& view_menu = menubar->add_menu("&View");
    view_menu.add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));
    view_menu.add_action(terminal.clear_including_history_action());
    view_menu.add_separator();
    view_menu.add_action(pick_font_action);

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man1/Terminal.md"), "/bin/Help");
    }));
    help_menu.add_action(GUI::CommonActions::make_about_action("Terminal", app_icon, window));

    window->set_menubar(menubar);

    window->on_close = [&]() {
        if (find_window)
            find_window->close();
        if (settings_window)
            settings_window->close();
    };

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

    if (unveil(config->filename().characters(), "rwc") < 0) {
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
