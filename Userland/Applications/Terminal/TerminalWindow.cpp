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
#include <LibCore/Timer.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Action.h>
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
#include <termios.h>

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

static void kill_shell_processes(pid_t shell_pid)
{
    // Kill the shell's process group (PGID == shell_pid after forkpty) and the shell itself.
    (void)Core::System::kill(-shell_pid, SIGHUP);
    (void)Core::System::kill(shell_pid, SIGHUP);
    // Kill background children that may have their own process groups.
    (void)Core::Directory::for_each_entry(
        String::formatted("/proc/{}/children", shell_pid).release_value_but_fixme_should_propagate_errors(),
        Core::DirIterator::Flags::SkipParentAndBaseDir,
        [&](auto& entry, auto&) {
            if (auto child_pid = entry.name.template to_number<pid_t>(); child_pid.has_value()) {
                (void)Core::System::kill(-child_pid.value(), SIGHUP);
                (void)Core::System::kill(child_pid.value(), SIGHUP);
            }
            return IterationDecision::Continue;
        });
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

    auto main_widget = set_main_widget<GUI::Widget>();
    main_widget->set_layout<GUI::VerticalBoxLayout>(0);
    main_widget->set_fill_with_background_color(true);

    m_top_line = main_widget->add<GUI::HorizontalSeparator>();
    m_top_line->set_fixed_height(2);
    m_top_line->set_visible(false);

    m_tab_widget = main_widget->add<GUI::TabWidget>();
    m_tab_widget->set_container_margins(GUI::Margins { 0 });
    m_tab_widget->set_reorder_allowed(true);
    m_tab_widget->set_close_button_enabled(true);
    m_tab_widget->on_tab_count_change = [this](size_t count) {
        m_top_line->set_visible(count > 1);
        m_tab_widget->set_bar_visible(!is_fullscreen() && count > 1);
        if (count == 0)
            GUI::Application::the()->quit(0);
    };
    m_tab_widget->on_change = [this](auto& widget) {
        auto& terminal = verify_cast<VT::TerminalWidget>(widget);
        terminal.apply_size_increments_to_window(*this);
        for (auto& data : m_tab_data_list) {
            if (data.terminal.ptr() == &terminal) {
                set_title(data.title.is_empty() ? "Terminal" : data.title);
                break;
            }
        }
    };
    m_tab_widget->on_tab_close_click = [this](auto& widget) {
        auto& terminal = verify_cast<VT::TerminalWidget>(widget);
        if (check_tab_quit(terminal) == GUI::MessageBox::ExecResult::OK) {
            close_terminal_tab(terminal);
        }
    };
    m_tab_widget->set_add_tab_button_enabled(true);
    m_tab_widget->on_add_tab_button_click = [this] { (void)open_new_terminal_tab(); };

    on_close_request = [this]() -> GUI::Window::CloseRequestDecision {
        if (check_terminal_quit() == GUI::MessageBox::ExecResult::OK)
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    on_input_preemption_change = [this](bool is_preempted) {
        active_terminal().set_logical_focus(!is_preempted);
    };

    m_modified_state_check_timer = Core::Timer::create_repeating(500, [this] {
        for (auto& data : m_tab_data_list) {
            bool modified = tty_has_foreground_process(data.ptm_fd, data.shell_pid)
                || shell_child_process_count(data.shell_pid) > 0;
            m_tab_widget->set_tab_modified(*data.terminal, modified);
        }
        set_modified(m_tab_widget->is_any_tab_modified());
    });
}

ErrorOr<void> TerminalWindow::initialize(StringView initial_command, bool keep_open)
{
    auto app_icon = GUI::Icon::default_icon("app-terminal"sv);
    m_open_settings_action = GUI::Action::create("Terminal &Settings",
        TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/settings.png"sv)),
        [this](auto&) { GUI::Process::spawn_or_show_error(this, "/bin/TerminalSettings"sv); });
    TRY(build_menus(app_icon));

    TRY(open_new_terminal_tab(initial_command, keep_open));

    Optional<Gfx::IntSize> fallback_size;
    auto increment = size_increment();
    if (increment.width() > 0 && increment.height() > 0) {
        auto base = base_size();
        fallback_size = Gfx::IntSize { base.width() + 80 * increment.width(), base.height() + 25 * increment.height() };
    }
    restore_size_and_position("Terminal"sv, "Geometry"sv, fallback_size);
    save_size_and_position_on_close("Terminal"sv, "Geometry"sv);

    if (m_should_confirm_close)
        m_modified_state_check_timer->start();

    return {};
}

ErrorOr<void> TerminalWindow::build_menus(GUI::Icon const& app_icon)
{
    auto file_menu = add_menu("&File"_string);
    file_menu->add_action(GUI::Action::create("New &Tab", { Mod_Ctrl | Mod_Shift, Key_T },
        TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/new-tab.png"sv)),
        [this](auto&) { (void)open_new_terminal_tab(); }));
    file_menu->add_action(GUI::Action::create("Open New &Window", { Mod_Ctrl | Mod_Shift, Key_N },
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
    edit_menu->add_action(GUI::Action::create("&Copy", { Mod_Ctrl | Mod_Shift, Key_C },
        TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-copy.png"sv)),
        [this](auto&) { active_terminal().copy(); }));
    edit_menu->add_action(GUI::Action::create("&Paste", { Mod_Ctrl | Mod_Shift, Key_V },
        TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/paste.png"sv)),
        [this](auto&) { active_terminal().paste(); }));
    edit_menu->add_separator();
    edit_menu->add_action(GUI::Action::create("&Find...", { Mod_Ctrl | Mod_Shift, Key_F },
        TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/find.png"sv)),
        [this](auto&) {
            auto& terminal = active_terminal();
            for (auto& data : m_tab_data_list) {
                if (data.terminal.ptr() == &terminal) {
                    if (!data.find_window)
                        data.find_window = create_find_window(terminal).release_value_but_fixme_should_propagate_errors();
                    data.find_window->show();
                    data.find_window->move_to_front();
                    return;
                }
            }
        }));

    auto view_menu = add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([this](auto&) {
        set_fullscreen(!is_fullscreen());
    }));
    view_menu->add_action(GUI::Action::create("Clear Including &History", { Mod_Ctrl | Mod_Shift, Key_K },
        [this](auto&) { active_terminal().clear_including_history(); }));
    view_menu->add_action(GUI::Action::create("Clear &Previous Command", { Mod_Ctrl | Mod_Shift, Key_U },
        [this](auto&) { active_terminal().clear_to_previous_mark(); }));
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

VT::TerminalWidget& TerminalWindow::active_terminal()
{
    return verify_cast<VT::TerminalWidget>(*m_tab_widget->active_widget());
}

ErrorOr<void> TerminalWindow::create_terminal_tab(int ptm_fd, pid_t shell_pid, ByteString ptsname)
{
    auto& terminal = m_tab_widget->add_tab<VT::TerminalWidget>("Terminal"_string, ptm_fd, false);
    terminal.set_startup_process_id(shell_pid);

    terminal.set_max_history_size(Config::read_i32("Terminal"sv, "Terminal"sv, "MaxHistorySize"sv, terminal.max_history_size()));
    terminal.set_show_scrollbar(m_show_scroll_bar);
    terminal.set_opacity(m_opacity);
    terminal.set_cursor_shape(m_cursor_shape);
    terminal.set_cursor_blinking(m_cursor_blinking);
    terminal.set_bell_mode(m_bell);
    terminal.set_auto_mark_mode(m_automark);
    terminal.apply_size_increments_to_window(*this);
    terminal.on_terminal_size_change = [this](auto size) {
        resize(size);
    };
    terminal.context_menu().add_separator();
    terminal.context_menu().add_action(*m_open_settings_action);

    m_tab_data_list.append({ ptm_fd, shell_pid, move(ptsname), {}, terminal, nullptr });

    auto* terminal_ptr = &terminal;

    terminal.on_command_exit = [this, terminal_ptr] {
        m_tab_widget->deferred_invoke([this, terminal_ptr] {
            close_terminal_tab(*terminal_ptr);
        });
    };

    terminal.on_title_change = [this, terminal_ptr](auto title) {
        m_tab_widget->set_tab_title(*terminal_ptr, String::from_utf8(title).release_value_but_fixme_should_propagate_errors());
        for (auto& data : m_tab_data_list) {
            if (data.terminal.ptr() == terminal_ptr) {
                data.title = title;
                break;
            }
        }
        if (m_tab_widget->active_widget() == terminal_ptr)
            set_title(title);
    };

    m_tab_widget->set_active_widget(&terminal);
    return {};
}

void TerminalWindow::close_terminal_tab(VT::TerminalWidget& terminal)
{
    for (size_t i = 0; i < m_tab_data_list.size(); ++i) {
        if (m_tab_data_list[i].terminal == &terminal) {
            kill_shell_processes(m_tab_data_list[i].shell_pid);
            (void)utmp_update(m_tab_data_list[i].ptsname, 0, false);
            m_tab_data_list.remove(i);
            break;
        }
    }
    m_tab_widget->remove_tab(terminal);
}

ErrorOr<void> TerminalWindow::open_new_terminal_tab(StringView cmd, bool keep_open)
{
    int ptm_fd;
    pid_t shell_pid = forkpty(&ptm_fd, nullptr, nullptr, nullptr);
    if (shell_pid < 0)
        return Error::from_errno(errno);
    if (shell_pid == 0) {
        auto startup_command = Config::read_string("Terminal"sv, "Startup"sv, "Command"sv, ""sv);
        if (run_command(cmd.is_empty() ? startup_command.view() : cmd, keep_open).is_error())
            _exit(1);
        VERIFY_NOT_REACHED();
    }
    auto ptsname = TRY(Core::System::ptsname(ptm_fd));
    TRY(utmp_update(ptsname, shell_pid, true));
    return create_terminal_tab(ptm_fd, shell_pid, ptsname);
}

ErrorOr<void> TerminalWindow::cleanup()
{
    dbgln("Terminal: Exiting, updating utmp");
    for (auto& data : m_tab_data_list) {
        kill_shell_processes(data.shell_pid);
        TRY(utmp_update(data.ptsname, 0, false));
    }
    return {};
}

GUI::Dialog::ExecResult TerminalWindow::check_terminal_quit()
{
    if (!m_should_confirm_close)
        return GUI::MessageBox::ExecResult::OK;
    bool has_foreground = false;
    int total_children = 0;
    for (auto& data : m_tab_data_list) {
        if (tty_has_foreground_process(data.ptm_fd, data.shell_pid))
            has_foreground = true;
        total_children += shell_child_process_count(data.shell_pid);
    }
    Optional<String> close_message;
    auto title = "Running Process"sv;
    if (has_foreground) {
        close_message = "Close Terminal and kill its foreground process?"_string;
    } else {
        if (total_children > 1) {
            title = "Running Processes"sv;
            close_message = String::formatted("Close Terminal and kill its {} background processes?", total_children).release_value_but_fixme_should_propagate_errors();
        } else if (total_children == 1) {
            close_message = "Close Terminal and kill its background process?"_string;
        }
    }
    if (close_message.has_value())
        return GUI::MessageBox::show(this, *close_message, title, GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::OKCancel);
    return GUI::MessageBox::ExecResult::OK;
}

GUI::Dialog::ExecResult TerminalWindow::check_tab_quit(VT::TerminalWidget& terminal)
{
    if (!m_should_confirm_close)
        return GUI::MessageBox::ExecResult::OK;
    for (auto& data : m_tab_data_list) {
        if (data.terminal.ptr() != &terminal)
            continue;
        bool has_foreground = tty_has_foreground_process(data.ptm_fd, data.shell_pid);
        int children = shell_child_process_count(data.shell_pid);
        Optional<String> close_message;
        auto title = "Running Process"sv;
        if (has_foreground) {
            close_message = "Close tab and kill its foreground process?"_string;
        } else if (children > 1) {
            title = "Running Processes"sv;
            close_message = String::formatted("Close tab and kill its {} background processes?", children).release_value_but_fixme_should_propagate_errors();
        } else if (children == 1) {
            close_message = "Close tab and kill its background process?"_string;
        }
        if (close_message.has_value())
            return GUI::MessageBox::show(this, *close_message, title, GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::OKCancel);
        return GUI::MessageBox::ExecResult::OK;
    }
    return GUI::MessageBox::ExecResult::OK;
}

void TerminalWindow::adjust_font_size(float adjustment, Gfx::Font::AllowInexactSizeMatch preference)
{
    auto& terminal = active_terminal();
    auto& font = terminal.font();
    auto new_size = max(5, font.presentation_size() + adjustment);
    if (auto new_font = Gfx::FontDatabase::the().get(font.family(), new_size, font.weight(), font.width(), font.slope(), preference)) {
        terminal.set_font_and_resize_to_fit(*new_font);
        terminal.apply_size_increments_to_window(*this);
        resize(terminal.size());
    }
}

void TerminalWindow::config_bool_did_change(StringView domain, StringView group, StringView key, bool value)
{
    VERIFY(domain == "Terminal");

    if (group == "Terminal") {
        if (key == "ShowScrollBar") {
            m_show_scroll_bar = value;
            for (auto& data : m_tab_data_list)
                data.terminal->set_show_scrollbar(value);
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
        for (auto& data : m_tab_data_list)
            data.terminal->set_cursor_blinking(value);
    }
}

void TerminalWindow::config_string_did_change(StringView domain, StringView group, StringView key, StringView value)
{
    VERIFY(domain == "Terminal");

    if (group == "Window" && key == "Bell") {
        m_bell = VT::TerminalWidget::parse_bell(value).value_or(VT::TerminalWidget::BellMode::Visible);
        for (auto& data : m_tab_data_list)
            data.terminal->set_bell_mode(m_bell);
    } else if (group == "Text" && key == "Font") {
        auto font = Gfx::FontDatabase::the().get_by_name(value);
        if (font.is_null())
            font = Gfx::FontDatabase::default_fixed_width_font();
        for (auto& data : m_tab_data_list) {
            data.terminal->set_font_and_resize_to_fit(*font);
            data.terminal->apply_size_increments_to_window(*data.terminal->window());
            data.terminal->window()->resize(data.terminal->size());
        }
    } else if (group == "Cursor" && key == "Shape") {
        m_cursor_shape = VT::TerminalWidget::parse_cursor_shape(value).value_or(VT::CursorShape::Block);
        for (auto& data : m_tab_data_list)
            data.terminal->set_cursor_shape(m_cursor_shape);
    } else if (group == "Terminal" && key == "AutoMark") {
        m_automark = VT::TerminalWidget::parse_automark_mode(value).value_or(VT::TerminalWidget::AutoMarkMode::MarkInteractiveShellPrompt);
        for (auto& data : m_tab_data_list)
            data.terminal->set_auto_mark_mode(m_automark);
    }
}

void TerminalWindow::config_i32_did_change(StringView domain, StringView group, StringView key, i32 value)
{
    VERIFY(domain == "Terminal");

    if (group == "Terminal" && key == "MaxHistorySize") {
        for (auto& data : m_tab_data_list)
            data.terminal->set_max_history_size(value);
    } else if (group == "Window" && key == "Opacity") {
        m_opacity = value;
        set_has_alpha_channel(value < 255);
        for (auto& data : m_tab_data_list)
            data.terminal->set_opacity(value);
    }
}
