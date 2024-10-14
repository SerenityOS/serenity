/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FixedArray.h>
#include <AK/QuickSort.h>
#include <AK/TypedTransfer.h>
#include <LibConfig/Client.h>
#include <LibConfig/Listener.h>
#include <LibCore/Account.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Directory.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystem/FileSystem.h>
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
#include <LibGUI/MessageBox.h>
#include <LibGUI/Process.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>
#include <LibVT/TerminalWidget.h>
#include <pty.h>

class TerminalChangeListener : public Config::Listener {
public:
    TerminalChangeListener(VT::TerminalWidget& parent_terminal)
        : m_parent_terminal(parent_terminal)
    {
    }

    virtual void config_bool_did_change(StringView domain, StringView group, StringView key, bool value) override
    {
        VERIFY(domain == "Terminal");

        if (group == "Terminal") {
            if (key == "ShowScrollBar")
                m_parent_terminal.set_show_scrollbar(value);
            else if (key == "ConfirmClose" && on_confirm_close_changed)
                on_confirm_close_changed(value);
        } else if (group == "Cursor" && key == "Blinking") {
            m_parent_terminal.set_cursor_blinking(value);
        }
    }

    virtual void config_string_did_change(StringView domain, StringView group, StringView key, StringView value) override
    {
        VERIFY(domain == "Terminal");

        if (group == "Window" && key == "Bell") {
            auto bell_mode = VT::TerminalWidget::parse_bell(value).value_or(VT::TerminalWidget::BellMode::Visible);
            m_parent_terminal.set_bell_mode(bell_mode);
        } else if (group == "Text" && key == "Font") {
            auto font = Gfx::FontDatabase::the().get_by_name(value);
            if (font.is_null())
                font = Gfx::FontDatabase::default_fixed_width_font();
            m_parent_terminal.set_font_and_resize_to_fit(*font);
            m_parent_terminal.apply_size_increments_to_window(*m_parent_terminal.window());
            m_parent_terminal.window()->resize(m_parent_terminal.size());
        } else if (group == "Cursor" && key == "Shape") {
            auto cursor_shape = VT::TerminalWidget::parse_cursor_shape(value).value_or(VT::CursorShape::Block);
            m_parent_terminal.set_cursor_shape(cursor_shape);
        } else if (group == "Terminal" && key == "AutoMark") {
            auto automark_mode = VT::TerminalWidget::parse_automark_mode(value).value_or(VT::TerminalWidget::AutoMarkMode::MarkInteractiveShellPrompt);
            m_parent_terminal.set_auto_mark_mode(automark_mode);
        }
    }

    virtual void config_i32_did_change(StringView domain, StringView group, StringView key, i32 value) override
    {
        VERIFY(domain == "Terminal");

        if (group == "Terminal" && key == "MaxHistorySize") {
            m_parent_terminal.set_max_history_size(value);
        } else if (group == "Window" && key == "Opacity") {
            m_parent_terminal.set_opacity(value);
        }
    }

    Function<void(bool)> on_confirm_close_changed;

private:
    VT::TerminalWidget& m_parent_terminal;
};

static ErrorOr<void> utmp_update(StringView tty, pid_t pid, bool create)
{
    auto pid_string = String::number(pid);
    Array utmp_update_command {
        "-f"sv,
        "Terminal"sv,
        "-p"sv,
        pid_string.bytes_as_string_view(),
        (create ? "-c"sv : "-d"sv),
        tty,
    };

    auto utmpupdate_pid = TRY(Core::Process::spawn("/bin/utmpupdate"sv, utmp_update_command, {}, Core::Process::KeepAsChild::Yes));

    Core::System::WaitPidResult status;
    auto wait_successful = false;
    while (!wait_successful) {
        auto result = Core::System::waitpid(utmpupdate_pid, 0);
        if (result.is_error() && result.error().code() != EINTR) {
            return result.release_error();
        } else if (!result.is_error()) {
            status = result.release_value();
            wait_successful = true;
        }
    }

    if (WIFEXITED(status.status) && WEXITSTATUS(status.status) != 0)
        dbgln("Terminal: utmpupdate exited with status {}", WEXITSTATUS(status.status));
    else if (WIFSIGNALED(status.status))
        dbgln("Terminal: utmpupdate exited due to unhandled signal {}", WTERMSIG(status.status));

    return {};
}

static ErrorOr<void> run_command(StringView command, bool keep_open)
{
    auto shell = TRY(String::from_byte_string(TRY(Core::Account::self(Core::Account::Read::PasswdOnly)).shell()));
    if (shell.is_empty())
        shell = "/bin/Shell"_string;

    Vector<StringView> arguments;
    arguments.append(shell);
    if (!command.is_empty()) {
        if (keep_open)
            arguments.append("--keep-open"sv);
        arguments.append("-c"sv);
        arguments.append(command);
    }
    auto env = TRY(FixedArray<StringView>::create({ "TERM=xterm"sv, "PAGER=more"sv, "PATH="sv DEFAULT_PATH_SV }));
    TRY(Core::System::exec(shell, arguments, Core::System::SearchInPath::No, env.span()));
    VERIFY_NOT_REACHED();
}

static ErrorOr<NonnullRefPtr<GUI::Window>> create_find_window(VT::TerminalWidget& terminal)
{
    auto window = GUI::Window::construct(&terminal);
    window->set_window_mode(GUI::WindowMode::RenderAbove);
    window->set_title("Find in Terminal");
    window->set_resizable(false);
    window->resize(300, 90);

    auto main_widget = window->set_main_widget<GUI::Widget>();
    main_widget->set_fill_with_background_color(true);
    main_widget->set_background_role(ColorRole::Button);
    main_widget->set_layout<GUI::VerticalBoxLayout>(4);

    auto& find = main_widget->add<GUI::Widget>();
    find.set_layout<GUI::HorizontalBoxLayout>(4);
    find.set_fixed_height(30);

    auto& find_textbox = find.add<GUI::TextBox>();
    find_textbox.set_fixed_width(230);
    find_textbox.set_focus(true);
    if (terminal.has_selection())
        find_textbox.set_text(terminal.selected_text().replace("\n"sv, " "sv, ReplaceMode::All));
    auto& find_backwards = find.add<GUI::Button>();
    find_backwards.set_fixed_width(25);
    find_backwards.set_icon(TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/upward-triangle.png"sv)));
    auto& find_forwards = find.add<GUI::Button>();
    find_forwards.set_fixed_width(25);
    find_forwards.set_icon(TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/downward-triangle.png"sv)));

    find_textbox.on_return_pressed = [&find_backwards] {
        find_backwards.click();
    };

    find_textbox.on_shift_return_pressed = [&find_forwards] {
        find_forwards.click();
    };

    auto& match_case = main_widget->add<GUI::CheckBox>("Case sensitive"_string);
    auto& wrap_around = main_widget->add<GUI::CheckBox>("Wrap around"_string);

    find_backwards.on_click = [&terminal, &find_textbox, &match_case, &wrap_around](auto) {
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
    find_forwards.on_click = [&terminal, &find_textbox, &match_case, &wrap_around](auto) {
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

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio tty rpath cpath wpath recvfd sendfd proc exec unix sigaction"));

    struct sigaction act;
    act.sa_mask = 0;
    // Do not trust that both function pointers overlap.
    act.sa_sigaction = nullptr;

    act.sa_flags = SA_NOCLDWAIT;
    act.sa_handler = SIG_IGN;

    TRY(Core::System::sigaction(SIGCHLD, &act, nullptr));

    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::pledge("stdio tty rpath cpath wpath recvfd sendfd proc exec unix"));

    Config::pledge_domain("Terminal");

    StringView command_to_execute;
    bool keep_open = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(command_to_execute, "Execute this command inside the terminal", nullptr, 'e', "command");
    args_parser.add_option(keep_open, "Keep the terminal open after the command has finished executing", nullptr, 'k');

    args_parser.parse(arguments);

    if (keep_open && command_to_execute.is_empty()) {
        warnln("Option -k can only be used in combination with -e.");
        return 1;
    }

    int ptm_fd;
    pid_t shell_pid = forkpty(&ptm_fd, nullptr, nullptr, nullptr);
    if (shell_pid < 0)
        return Error::from_errno(errno);

    // We're the child process; run the startup command.
    if (shell_pid == 0) {
        if (!command_to_execute.is_empty())
            TRY(run_command(command_to_execute, keep_open));
        else
            TRY(run_command(Config::read_string("Terminal"sv, "Startup"sv, "Command"sv, ""sv), false));
        VERIFY_NOT_REACHED();
    }

    auto ptsname = TRY(Core::System::ptsname(ptm_fd));
    TRY(utmp_update(ptsname, shell_pid, true));

    auto app_icon = GUI::Icon::default_icon("app-terminal"sv);

    auto window = GUI::Window::construct();
    window->set_title("Terminal");
    window->set_obey_widget_min_size(false);

    auto terminal = window->set_main_widget<VT::TerminalWidget>(ptm_fd, true);
    terminal->set_startup_process_id(shell_pid);

    terminal->on_command_exit = [&] {
        app->quit(0);
    };
    terminal->on_title_change = [&](auto title) {
        window->set_title(title);
    };
    terminal->on_terminal_size_change = [&](auto size) {
        window->resize(size);
    };
    terminal->apply_size_increments_to_window(*window);
    window->set_icon(app_icon.bitmap_for_size(16));

    Config::monitor_domain("Terminal");
    auto should_confirm_close = Config::read_bool("Terminal"sv, "Terminal"sv, "ConfirmClose"sv, true);
    TerminalChangeListener listener { terminal };

    auto bell = Config::read_string("Terminal"sv, "Window"sv, "Bell"sv, "Visible"sv);
    if (bell == "AudibleBeep") {
        terminal->set_bell_mode(VT::TerminalWidget::BellMode::AudibleBeep);
    } else if (bell == "Disabled") {
        terminal->set_bell_mode(VT::TerminalWidget::BellMode::Disabled);
    } else {
        terminal->set_bell_mode(VT::TerminalWidget::BellMode::Visible);
    }

    auto automark = Config::read_string("Terminal"sv, "Terminal"sv, "AutoMark"sv, "MarkInteractiveShellPrompt"sv);
    if (automark == "MarkNothing") {
        terminal->set_auto_mark_mode(VT::TerminalWidget::AutoMarkMode::MarkNothing);
    } else {
        terminal->set_auto_mark_mode(VT::TerminalWidget::AutoMarkMode::MarkInteractiveShellPrompt);
    }

    auto cursor_shape = VT::TerminalWidget::parse_cursor_shape(Config::read_string("Terminal"sv, "Cursor"sv, "Shape"sv, "Block"sv)).value_or(VT::CursorShape::Block);
    terminal->set_cursor_shape(cursor_shape);

    auto cursor_blinking = Config::read_bool("Terminal"sv, "Cursor"sv, "Blinking"sv, true);
    terminal->set_cursor_blinking(cursor_blinking);

    auto find_window = TRY(create_find_window(terminal));

    auto new_opacity = Config::read_i32("Terminal"sv, "Window"sv, "Opacity"sv, 255);
    terminal->set_opacity(new_opacity);
    window->set_has_alpha_channel(new_opacity < 255);

    auto new_scrollback_size = Config::read_i32("Terminal"sv, "Terminal"sv, "MaxHistorySize"sv, terminal->max_history_size());
    terminal->set_max_history_size(new_scrollback_size);

    auto show_scroll_bar = Config::read_bool("Terminal"sv, "Terminal"sv, "ShowScrollBar"sv, true);
    terminal->set_show_scrollbar(show_scroll_bar);

    auto open_settings_action = GUI::Action::create("Terminal &Settings", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/settings.png"sv)),
        [&](auto&) {
            GUI::Process::spawn_or_show_error(window, "/bin/TerminalSettings"sv);
        });

    terminal->context_menu().add_separator();
    terminal->context_menu().add_action(open_settings_action);

    auto file_menu = window->add_menu("&File"_string);
    file_menu->add_action(GUI::Action::create("Open New &Terminal", { Mod_Ctrl | Mod_Shift, Key_N }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-terminal.png"sv)), [&](auto&) {
        GUI::Process::spawn_or_show_error(window, "/bin/Terminal"sv);
    }));

    file_menu->add_action(open_settings_action);
    file_menu->add_separator();

    auto tty_has_foreground_process = [&] {
        pid_t fg_pid = tcgetpgrp(ptm_fd);
        return fg_pid != -1 && fg_pid != shell_pid;
    };

    auto shell_child_process_count = [&] {
        int background_process_count = 0;
        Core::Directory::for_each_entry(String::formatted("/proc/{}/children", shell_pid).release_value_but_fixme_should_propagate_errors(), Core::DirIterator::Flags::SkipParentAndBaseDir, [&](auto&, auto&) {
            ++background_process_count;
            return IterationDecision::Continue;
        }).release_value_but_fixme_should_propagate_errors();
        return background_process_count;
    };

    auto check_terminal_quit = [&]() -> GUI::Dialog::ExecResult {
        if (!should_confirm_close)
            return GUI::MessageBox::ExecResult::OK;
        Optional<String> close_message;
        auto title = "Running Process"sv;
        if (tty_has_foreground_process()) {
            close_message = "Close Terminal and kill its foreground process?"_string;
        } else {
            auto child_process_count = shell_child_process_count();
            if (child_process_count > 1) {
                title = "Running Processes"sv;
                close_message = String::formatted("Close Terminal and kill its {} background processes?", child_process_count).release_value_but_fixme_should_propagate_errors();
            } else if (child_process_count == 1) {
                close_message = "Close Terminal and kill its background process?"_string;
            }
        }
        if (close_message.has_value())
            return GUI::MessageBox::show(window, *close_message, title, GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::OKCancel);
        return GUI::MessageBox::ExecResult::OK;
    };

    file_menu->add_action(GUI::CommonActions::make_quit_action(
        [&](auto&) {
            dbgln("Terminal: Quit menu activated!");
            if (check_terminal_quit() == GUI::MessageBox::ExecResult::OK)
                GUI::Application::the()->quit();
        },
        GUI::CommonActions::QuitAltShortcut::None));

    auto edit_menu = window->add_menu("&Edit"_string);
    edit_menu->add_action(terminal->copy_action());
    edit_menu->add_action(terminal->paste_action());
    edit_menu->add_separator();
    edit_menu->add_action(GUI::Action::create("&Find...", { Mod_Ctrl | Mod_Shift, Key_F }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/find.png"sv)),
        [&](auto&) {
            find_window->show();
            find_window->move_to_front();
        }));

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));
    view_menu->add_action(terminal->clear_including_history_action());
    view_menu->add_action(terminal->clear_to_previous_mark_action());

    auto adjust_font_size = [&](float adjustment, Gfx::Font::AllowInexactSizeMatch preference) {
        auto& font = terminal->font();
        auto new_size = max(5, font.presentation_size() + adjustment);
        if (auto new_font = Gfx::FontDatabase::the().get(font.family(), new_size, font.weight(), font.width(), font.slope(), preference)) {
            terminal->set_font_and_resize_to_fit(*new_font);
            terminal->apply_size_increments_to_window(*window);
            window->resize(terminal->size());
        }
    };

    view_menu->add_separator();
    view_menu->add_action(GUI::CommonActions::make_zoom_in_action([&](auto&) {
        adjust_font_size(1, Gfx::Font::AllowInexactSizeMatch::Larger);
    }));
    view_menu->add_action(GUI::CommonActions::make_zoom_out_action([&](auto&) {
        adjust_font_size(-1, Gfx::Font::AllowInexactSizeMatch::Smaller);
    }));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man1/Applications/Terminal.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Terminal"_string, app_icon, window));

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (check_terminal_quit() == GUI::MessageBox::ExecResult::OK)
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    window->on_input_preemption_change = [&](bool is_preempted) {
        terminal->set_logical_focus(!is_preempted);
    };

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/bin", "r"));
    TRY(Core::System::unveil("/proc", "r"));
    TRY(Core::System::unveil("/bin/Terminal", "x"));
    TRY(Core::System::unveil("/bin/TerminalSettings", "x"));
    TRY(Core::System::unveil("/bin/utmpupdate", "x"));
    TRY(Core::System::unveil("/etc/FileIconProvider.ini", "r"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/dev/beep", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto modified_state_check_timer = Core::Timer::create_repeating(500, [&] {
        window->set_modified(tty_has_foreground_process() || shell_child_process_count() > 0);
    });

    listener.on_confirm_close_changed = [&](bool confirm_close) {
        if (confirm_close) {
            modified_state_check_timer->start();
        } else {
            modified_state_check_timer->stop();
            window->set_modified(false);
        }
        should_confirm_close = confirm_close;
    };

    window->show();
    if (should_confirm_close)
        modified_state_check_timer->start();
    int result = app->exec();
    dbgln("Exiting terminal, updating utmp");
    TRY(utmp_update(ptsname, 0, false));
    return result;
}
