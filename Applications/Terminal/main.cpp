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

#include <Kernel/KeyCode.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/UserInfo.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/FontDatabase.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/Slider.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <LibVT/TerminalWidget.h>
#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <pwd.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/select.h>
#include <unistd.h>

static void run_command(int ptm_fd, String command)
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
        const char* envs[] = { "PROMPT=\\X\\u@\\h:\\w\\a\\e[33;1m\\h\\e[0m \\e[32;1m\\w\\e[0m \\p ", "TERM=xterm", "PAGER=more", "PATH=/bin:/usr/bin:/usr/local/bin", nullptr };
        rc = execve(shell.characters(), const_cast<char**>(args), const_cast<char**>(envs));
        if (rc < 0) {
            perror("execve");
            exit(1);
        }
        ASSERT_NOT_REACHED();
    }
}

RefPtr<GUI::Window> create_settings_window(TerminalWidget& terminal)
{
    auto window = GUI::Window::construct();
    window->set_title("Terminal Settings");
    window->set_rect(50, 50, 200, 140);
    window->set_modal(true);

    auto& settings = window->set_main_widget<GUI::Widget>();
    settings.set_fill_with_background_color(true);
    settings.set_background_role(ColorRole::Button);
    settings.set_layout<GUI::VerticalBoxLayout>();
    settings.layout()->set_margins({ 4, 4, 4, 4 });

    auto& radio_container = settings.add<GUI::GroupBox>("Bell Mode");
    radio_container.set_layout<GUI::VerticalBoxLayout>();
    radio_container.layout()->set_margins({ 6, 16, 6, 6 });
    radio_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    radio_container.set_preferred_size(100, 70);

    auto& sysbell_radio = radio_container.add<GUI::RadioButton>("Use (Audible) System Bell");
    auto& visbell_radio = radio_container.add<GUI::RadioButton>("Use (Visual) Terminal Bell");
    sysbell_radio.set_checked(terminal.should_beep());
    visbell_radio.set_checked(!terminal.should_beep());
    sysbell_radio.on_checked = [&terminal](const bool checked) {
        terminal.set_should_beep(checked);
    };

    auto& slider_container = settings.add<GUI::GroupBox>("Background Opacity");
    slider_container.set_layout<GUI::VerticalBoxLayout>();
    slider_container.layout()->set_margins({ 6, 16, 6, 6 });
    slider_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    slider_container.set_preferred_size(100, 50);
    auto& slider = slider_container.add<GUI::HorizontalSlider>();

    slider.on_value_changed = [&terminal](int value) {
        terminal.set_opacity(value);
    };

    slider.set_range(0, 255);
    slider.set_value(terminal.opacity());

    return window;
}

int main(int argc, char** argv)
{
    if (pledge("stdio tty rpath accept cpath wpath shared_buffer proc exec unix fattr", nullptr) < 0) {
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

    GUI::Application app(argc, argv);

    if (pledge("stdio tty rpath accept cpath wpath shared_buffer proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* command_to_execute = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(command_to_execute, "Execute this command inside the terminal", nullptr, 'e', "command");
    args_parser.parse(argc, argv);

    if (chdir(get_current_user_home_path().characters()) < 0)
        perror("chdir");

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

    run_command(ptm_fd, command_to_execute);

    auto window = GUI::Window::construct();
    window->set_title("Terminal");
    window->set_background_color(Color::Black);
    window->set_double_buffering_enabled(false);

    RefPtr<Core::ConfigFile> config = Core::ConfigFile::get_for_app("Terminal");
    auto& terminal = window->set_main_widget<TerminalWidget>(ptm_fd, true, config);
    terminal.on_command_exit = [&] {
        app.quit(0);
    };
    terminal.on_title_change = [&](auto& title) {
        window->set_title(title);
    };
    window->move_to(300, 300);
    terminal.apply_size_increments_to_window(*window);
    window->show();
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-terminal.png"));
    terminal.set_should_beep(config->read_bool_entry("Window", "AudibleBeep", false));

    RefPtr<GUI::Window> settings_window;

    auto new_opacity = config->read_num_entry("Window", "Opacity", 255);
    terminal.set_opacity(new_opacity);
    window->set_has_alpha_channel(new_opacity < 255);

    auto menubar = make<GUI::MenuBar>();

    auto app_menu = GUI::Menu::construct("Terminal");
    app_menu->add_action(GUI::Action::create("Open new terminal", { Mod_Ctrl | Mod_Shift, Key_N }, Gfx::Bitmap::load_from_file("/res/icons/16x16/app-terminal.png"), [&](auto&) {
        if (!fork()) {
            execl("/bin/Terminal", "Terminal", nullptr);
            exit(1);
        }
    }));
    app_menu->add_action(GUI::Action::create("Settings...", Gfx::Bitmap::load_from_file("/res/icons/gear16.png"),
        [&](const GUI::Action&) {
            if (!settings_window) {
                settings_window = create_settings_window(terminal);
                settings_window->on_close_request = [&] {
                    settings_window = nullptr;
                    return GUI::Window::CloseRequestDecision::Close;
                };
            }
            settings_window->show();
            settings_window->move_to_front();
        }));
    app_menu->add_separator();
    app_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        dbgprintf("Terminal: Quit menu activated!\n");
        GUI::Application::the().quit(0);
    }));
    menubar->add_menu(move(app_menu));

    auto edit_menu = GUI::Menu::construct("Edit");
    edit_menu->add_action(terminal.copy_action());
    edit_menu->add_action(terminal.paste_action());
    menubar->add_menu(move(edit_menu));

    GUI::ActionGroup font_action_group;
    font_action_group.set_exclusive(true);
    auto font_menu = GUI::Menu::construct("Font");
    GUI::FontDatabase::the().for_each_fixed_width_font([&](const StringView& font_name) {
        auto action = GUI::Action::create(font_name, [&](GUI::Action& action) {
            action.set_checked(true);
            terminal.set_font(GUI::FontDatabase::the().get_by_name(action.text()));
            auto metadata = GUI::FontDatabase::the().get_metadata_by_name(action.text());
            ASSERT(metadata.has_value());
            config->write_entry("Text", "Font", metadata.value().path);
            config->sync();
            terminal.force_repaint();
        });
        font_action_group.add_action(*action);
        action->set_checkable(true);
        if (terminal.font().name() == font_name)
            action->set_checked(true);
        font_menu->add_action(*action);
    });
    menubar->add_menu(move(font_menu));

    auto help_menu = GUI::Menu::construct("Help");
    help_menu->add_action(GUI::Action::create("About", [&](const GUI::Action&) {
        GUI::AboutDialog::show("Terminal", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-terminal.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin/Terminal", "x") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(config->file_name().characters(), "rwc")) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    config->sync();
    return app.exec();
}
