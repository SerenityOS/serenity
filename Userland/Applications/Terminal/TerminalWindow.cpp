/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TerminalWindow.h"
#include <AK/FixedArray.h>
#include <LibConfig/Client.h>
#include <LibCore/Account.h>
#include <LibCore/Directory.h>
#include <LibCore/Process.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Process.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/SystemTheme.h>
#include <LibURL/URL.h>
#include <pty.h>
#include <errno.h>
#include <termios.h>
#include <unistd.h>

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

static bool tty_has_foreground_process(int ptm_fd, pid_t shell_pid)
{
    pid_t fg_pid = tcgetpgrp(ptm_fd);
    return fg_pid != -1 && fg_pid != shell_pid;
}

static int shell_child_process_count(pid_t shell_pid)
{
    int count = 0;
    Core::Directory::for_each_entry(String::formatted("/proc/{}/children", shell_pid).release_value_but_fixme_should_propagate_errors(),
        Core::DirIterator::Flags::SkipParentAndBaseDir, [&](auto&, auto&) {
            ++count;
            return IterationDecision::Continue;
        })
        .release_value_but_fixme_should_propagate_errors();
    return count;
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
    main_widget->set_background_role(Gfx::ColorRole::Button);
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

    find_textbox.on_return_pressed = [&find_backwards] { find_backwards.click(); };
    find_textbox.on_shift_return_pressed = [&find_forwards] { find_forwards.click(); };

    auto& match_case = main_widget->add<GUI::CheckBox>("Case sensitive"_string);
    auto& wrap_around = main_widget->add<GUI::CheckBox>("Wrap around"_string);

    find_backwards.on_click = [&terminal, &find_textbox, &match_case, &wrap_around](auto) {
        auto needle = find_textbox.text();
        if (needle.is_empty())
            return;
        auto found_range = terminal.find_previous(needle, terminal.normalized_selection().start(), match_case.is_checked(), wrap_around.is_checked());
        if (found_range.is_valid()) {
            terminal.scroll_to_row(found_range.start().row());
            terminal.set_selection(found_range);
        }
    };
    find_forwards.on_click = [&terminal, &find_textbox, &match_case, &wrap_around](auto) {
        auto needle = find_textbox.text();
        if (needle.is_empty())
            return;
        auto found_range = terminal.find_next(needle, terminal.normalized_selection().end(), match_case.is_checked(), wrap_around.is_checked());
        if (found_range.is_valid()) {
            terminal.scroll_to_row(found_range.start().row());
            terminal.set_selection(found_range);
        }
    };

    return window;
}

TerminalWindow::TerminalWindow()
{
    auto app_icon = GUI::Icon::default_icon("app-terminal"sv);
    set_title("Terminal");
    set_obey_widget_min_size(false);
    set_icon(app_icon.bitmap_for_size(16));

    Config::monitor_domain("Terminal");
    m_should_confirm_close = Config::read_bool("Terminal"sv, "Terminal"sv, "ConfirmClose"sv, true);
    m_bell = VT::TerminalWidget::parse_bell(Config::read_string("Terminal"sv, "Window"sv, "Bell"sv, "Visible"sv)).value_or(VT::TerminalWidget::BellMode::Visible);
    m_automark = VT::TerminalWidget::parse_automark_mode(Config::read_string("Terminal"sv, "Terminal"sv, "AutoMark"sv, "MarkInteractiveShellPrompt"sv)).value_or(VT::TerminalWidget::AutoMarkMode::MarkInteractiveShellPrompt);
    m_cursor_shape = VT::TerminalWidget::parse_cursor_shape(Config::read_string("Terminal"sv, "Cursor"sv, "Shape"sv, "Block"sv)).value_or(VT::CursorShape::Block);
    m_cursor_blinking = Config::read_bool("Terminal"sv, "Cursor"sv, "Blinking"sv, true);
    m_opacity = Config::read_i32("Terminal"sv, "Window"sv, "Opacity"sv, 255);
    m_show_scroll_bar = Config::read_bool("Terminal"sv, "Terminal"sv, "ShowScrollBar"sv, true);
    set_has_alpha_channel(m_opacity < 255);

    on_close_request = [this]() -> GUI::Window::CloseRequestDecision {
        if (check_terminal_quit() == GUI::MessageBox::ExecResult::OK)
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    on_input_preemption_change = [this](bool is_preempted) {
        if (m_terminal)
            m_terminal->set_logical_focus(!is_preempted);
    };

    m_modified_state_check_timer = Core::Timer::create_repeating(500, [this] {
        set_modified(tty_has_foreground_process(m_ptm_fd, m_shell_pid) || shell_child_process_count(m_shell_pid) > 0);
    });
}

ErrorOr<void> TerminalWindow::initialize(StringView initial_command, bool keep_open)
{
    int ptm_fd;
    pid_t shell_pid = forkpty(&ptm_fd, nullptr, nullptr, nullptr);
    if (shell_pid < 0)
        return Error::from_errno(errno);
    if (shell_pid == 0) {
        auto startup_command = Config::read_string("Terminal"sv, "Startup"sv, "Command"sv, ""sv);
        if (run_command(initial_command.is_empty() ? startup_command.view() : initial_command, keep_open).is_error())
            _exit(1);
        VERIFY_NOT_REACHED();
    }

    m_ptm_fd = ptm_fd;
    m_shell_pid = shell_pid;
    m_ptsname = TRY(Core::System::ptsname(ptm_fd));
    TRY(utmp_update(m_ptsname, shell_pid, true));

    auto app_icon = GUI::Icon::default_icon("app-terminal"sv);
    auto terminal = set_main_widget<VT::TerminalWidget>(ptm_fd, true);
    m_terminal = terminal;
    terminal->set_startup_process_id(shell_pid);
    terminal->on_command_exit = [] {
        GUI::Application::the()->quit(0);
    };
    terminal->on_title_change = [this](auto title) {
        set_title(title);
    };
    terminal->on_terminal_size_change = [this](auto size) {
        resize(size);
    };
    terminal->apply_size_increments_to_window(*this);
    set_icon(app_icon.bitmap_for_size(16));

    terminal->set_bell_mode(m_bell);
    terminal->set_auto_mark_mode(m_automark);
    terminal->set_cursor_shape(m_cursor_shape);
    terminal->set_cursor_blinking(m_cursor_blinking);
    terminal->set_opacity(m_opacity);
    terminal->set_max_history_size(Config::read_i32("Terminal"sv, "Terminal"sv, "MaxHistorySize"sv, terminal->max_history_size()));
    terminal->set_show_scrollbar(m_show_scroll_bar);

    m_find_window = TRY(create_find_window(*terminal));

    m_open_settings_action = GUI::Action::create("Terminal &Settings",
        TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/settings.png"sv)),
        [this](auto&) { GUI::Process::spawn_or_show_error(this, "/bin/TerminalSettings"sv); });
    terminal->context_menu().add_separator();
    terminal->context_menu().add_action(*m_open_settings_action);

    TRY(build_menus(app_icon));

    if (m_should_confirm_close)
        m_modified_state_check_timer->start();

    return {};
}

ErrorOr<void> TerminalWindow::build_menus(GUI::Icon const& app_icon)
{
    VERIFY(m_terminal);

    auto file_menu = add_menu("&File"_string);
    file_menu->add_action(GUI::Action::create("Open New &Terminal", { Mod_Ctrl | Mod_Shift, Key_N },
        TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-terminal.png"sv)),
        [this](auto&) { GUI::Process::spawn_or_show_error(this, "/bin/Terminal"sv); }));
    file_menu->add_action(*m_open_settings_action);
    file_menu->add_separator();
    file_menu->add_action(GUI::CommonActions::make_quit_action(
        [this](auto&) {
            dbgln("Terminal: Quit menu activated!");
            if (check_terminal_quit() == GUI::MessageBox::ExecResult::OK)
                GUI::Application::the()->quit();
        },
        GUI::CommonActions::QuitAltShortcut::None));

    auto edit_menu = add_menu("&Edit"_string);
    edit_menu->add_action(m_terminal->copy_action());
    edit_menu->add_action(m_terminal->paste_action());
    edit_menu->add_separator();
    edit_menu->add_action(GUI::Action::create("&Find...", { Mod_Ctrl | Mod_Shift, Key_F },
        TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/find.png"sv)),
        [this](auto&) {
            VERIFY(m_find_window);
            m_find_window->show();
            m_find_window->move_to_front();
        }));

    auto view_menu = add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([this](auto&) {
        set_fullscreen(!is_fullscreen());
    }));
    view_menu->add_action(m_terminal->clear_including_history_action());
    view_menu->add_action(m_terminal->clear_to_previous_mark_action());
    view_menu->add_separator();
    view_menu->add_action(GUI::CommonActions::make_zoom_in_action([this](auto&) {
        adjust_font_size(1, Gfx::Font::AllowInexactSizeMatch::Larger);
    }));
    view_menu->add_action(GUI::CommonActions::make_zoom_out_action([this](auto&) {
        adjust_font_size(-1, Gfx::Font::AllowInexactSizeMatch::Smaller);
    }));

    auto help_menu = add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(this));
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man1/Applications/Terminal.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Terminal"_string, app_icon, this));
    return {};
}

ErrorOr<void> TerminalWindow::cleanup()
{
    dbgln("Terminal: Exiting, updating utmp");
    return utmp_update(m_ptsname, 0, false);
}

GUI::Dialog::ExecResult TerminalWindow::check_terminal_quit()
{
    if (!m_should_confirm_close)
        return GUI::MessageBox::ExecResult::OK;

    Optional<String> close_message;
    auto title = "Running Process"sv;
    if (tty_has_foreground_process(m_ptm_fd, m_shell_pid)) {
        close_message = "Close Terminal and kill its foreground process?"_string;
    } else {
        auto child_process_count = shell_child_process_count(m_shell_pid);
        if (child_process_count > 1) {
            title = "Running Processes"sv;
            close_message = String::formatted("Close Terminal and kill its {} background processes?", child_process_count).release_value_but_fixme_should_propagate_errors();
        } else if (child_process_count == 1) {
            close_message = "Close Terminal and kill its background process?"_string;
        }
    }

    if (close_message.has_value())
        return GUI::MessageBox::show(this, *close_message, title, GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::OKCancel);
    return GUI::MessageBox::ExecResult::OK;
}

void TerminalWindow::adjust_font_size(float adjustment, Gfx::Font::AllowInexactSizeMatch preference)
{
    VERIFY(m_terminal);

    auto& font = m_terminal->font();
    auto new_size = max(5, font.presentation_size() + adjustment);
    if (auto new_font = Gfx::FontDatabase::the().get(font.family(), new_size, font.weight(), font.width(), font.slope(), preference)) {
        m_terminal->set_font_and_resize_to_fit(*new_font);
        m_terminal->apply_size_increments_to_window(*this);
        resize(m_terminal->size());
    }
}

void TerminalWindow::config_bool_did_change(StringView domain, StringView group, StringView key, bool value)
{
    VERIFY(domain == "Terminal");
    VERIFY(m_terminal);

    if (group == "Terminal") {
        if (key == "ShowScrollBar") {
            m_show_scroll_bar = value;
            m_terminal->set_show_scrollbar(value);
        } else if (key == "ConfirmClose") {
            m_should_confirm_close = value;
            if (value) {
                m_modified_state_check_timer->start();
            } else {
                m_modified_state_check_timer->stop();
                set_modified(false);
            }
        }
    } else if (group == "Cursor" && key == "Blinking") {
        m_cursor_blinking = value;
        m_terminal->set_cursor_blinking(value);
    }
}

void TerminalWindow::config_string_did_change(StringView domain, StringView group, StringView key, StringView value)
{
    VERIFY(domain == "Terminal");
    VERIFY(m_terminal);

    if (group == "Window" && key == "Bell") {
        m_bell = VT::TerminalWidget::parse_bell(value).value_or(VT::TerminalWidget::BellMode::Visible);
        m_terminal->set_bell_mode(m_bell);
    } else if (group == "Text" && key == "Font") {
        auto font = Gfx::FontDatabase::the().get_by_name(value);
        if (font.is_null())
            font = Gfx::FontDatabase::default_fixed_width_font();
        m_terminal->set_font_and_resize_to_fit(*font);
        m_terminal->apply_size_increments_to_window(*this);
        resize(m_terminal->size());
    } else if (group == "Cursor" && key == "Shape") {
        m_cursor_shape = VT::TerminalWidget::parse_cursor_shape(value).value_or(VT::CursorShape::Block);
        m_terminal->set_cursor_shape(m_cursor_shape);
    } else if (group == "Terminal" && key == "AutoMark") {
        m_automark = VT::TerminalWidget::parse_automark_mode(value).value_or(VT::TerminalWidget::AutoMarkMode::MarkInteractiveShellPrompt);
        m_terminal->set_auto_mark_mode(m_automark);
    }
}

void TerminalWindow::config_i32_did_change(StringView domain, StringView group, StringView key, i32 value)
{
    VERIFY(domain == "Terminal");
    VERIFY(m_terminal);

    if (group == "Terminal" && key == "MaxHistorySize") {
        m_terminal->set_max_history_size(value);
    } else if (group == "Window" && key == "Opacity") {
        m_opacity = value;
        set_has_alpha_channel(value < 255);
        m_terminal->set_opacity(value);
    }
}
