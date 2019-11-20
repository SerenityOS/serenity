#include <Kernel/KeyCode.h>
#include <LibCore/CArgsParser.h>
#include <LibCore/CUserInfo.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GFontDatabase.h>
#include <LibGUI/GGroupBox.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GRadioButton.h>
#include <LibGUI/GSlider.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <LibVT/TerminalWidget.h>
#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <pwd.h>
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

RefPtr<GWindow> create_settings_window(TerminalWidget& terminal, RefPtr<CConfigFile> config)
{
    auto window = GWindow::construct();
    window->set_title("Terminal Settings");
    window->set_rect(50, 50, 200, 140);

    auto settings = GWidget::construct();
    window->set_main_widget(settings);
    settings->set_fill_with_background_color(true);
    settings->set_layout(make<GBoxLayout>(Orientation::Vertical));
    settings->layout()->set_margins({ 4, 4, 4, 4 });

    auto radio_container = GGroupBox::construct("Bell Mode", settings);
    radio_container->set_layout(make<GBoxLayout>(Orientation::Vertical));
    radio_container->layout()->set_margins({ 6, 16, 6, 6 });
    radio_container->set_fill_with_background_color(true);
    radio_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    radio_container->set_preferred_size(100, 70);

    auto sysbell_radio = GRadioButton::construct("Use (Audible) System Bell", radio_container);
    auto visbell_radio = GRadioButton::construct("Use (Visual) Terminal Bell", radio_container);
    sysbell_radio->set_checked(terminal.should_beep());
    visbell_radio->set_checked(!terminal.should_beep());
    sysbell_radio->on_checked = [&terminal](const bool checked) {
        terminal.set_should_beep(checked);
    };

    auto slider_container = GGroupBox::construct("Background Opacity", settings);
    slider_container->set_layout(make<GBoxLayout>(Orientation::Vertical));
    slider_container->layout()->set_margins({ 6, 16, 6, 6 });
    slider_container->set_fill_with_background_color(true);
    slider_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    slider_container->set_preferred_size(100, 50);
    auto slider = GSlider::construct(Orientation::Horizontal, slider_container);
    slider->set_fill_with_background_color(true);
    slider->set_background_color(Color::WarmGray);

    slider->on_value_changed = [&terminal, &config](int value) {
        terminal.set_opacity(value);
    };

    slider->set_range(0, 255);
    slider->set_value(terminal.opacity());

    return window;
}

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    CArgsParser args_parser("Terminal");

    args_parser.add_arg("e", "execute", "Execute this command inside the terminal.");

    CArgsParserResult args = args_parser.parse(argc, argv);

    if (chdir(get_current_user_home_path().characters()) < 0)
        perror("chdir");

    int ptm_fd = open("/dev/ptmx", O_RDWR | O_CLOEXEC);
    if (ptm_fd < 0) {
        perror("open(ptmx)");
        return 1;
    }

    run_command(ptm_fd, args.get("e"));

    auto window = GWindow::construct();
    window->set_title("Terminal");
    window->set_background_color(Color::Black);
    window->set_double_buffering_enabled(false);

    RefPtr<CConfigFile> config = CConfigFile::get_for_app("Terminal");
    auto terminal = TerminalWidget::construct(ptm_fd, true, config);
    terminal->on_command_exit = [&] {
        app.quit(0);
    };
    terminal->on_title_change = [&](auto& title) {
        window->set_title(title);
    };
    window->set_main_widget(terminal);
    window->move_to(300, 300);
    terminal->apply_size_increments_to_window(*window);
    window->show();
    window->set_icon(load_png("/res/icons/16x16/app-terminal.png"));
    terminal->set_should_beep(config->read_bool_entry("Window", "AudibleBeep", false));

    RefPtr<GWindow> settings_window;

    auto new_opacity = config->read_num_entry("Window", "Opacity", 255);
    terminal->set_opacity(new_opacity);
    window->set_has_alpha_channel(new_opacity < 255);

    auto menubar = make<GMenuBar>();

    auto app_menu = make<GMenu>("Terminal");
    app_menu->add_action(GAction::create("Open new terminal", { Mod_Ctrl | Mod_Shift, Key_N }, GraphicsBitmap::load_from_file("/res/icons/16x16/app-terminal.png"), [&](auto&) {
        if (!fork()) {
            execl("/bin/Terminal", "Terminal", nullptr);
            exit(1);
        }
    }));
    app_menu->add_action(GAction::create("Settings...", load_png("/res/icons/gear16.png"),
        [&](const GAction&) {
            if (!settings_window) {
                settings_window = create_settings_window(*terminal, config);
                settings_window->on_close_request = [&] {
                    settings_window = nullptr;
                    return GWindow::CloseRequestDecision::Close;
                };
            }
            settings_window->show();
            settings_window->move_to_front();
        }));
    app_menu->add_separator();
    app_menu->add_action(GCommonActions::make_quit_action([](auto&) {
        dbgprintf("Terminal: Quit menu activated!\n");
        GApplication::the().quit(0);
    }));
    menubar->add_menu(move(app_menu));

    auto font_menu = make<GMenu>("Font");
    GFontDatabase::the().for_each_fixed_width_font([&](const StringView& font_name) {
        font_menu->add_action(GAction::create(font_name, [&](const GAction& action) {
            terminal->set_font(GFontDatabase::the().get_by_name(action.text()));
            auto metadata = GFontDatabase::the().get_metadata_by_name(action.text());
            ASSERT(metadata.has_value());
            config->write_entry("Text", "Font", metadata.value().path);
            config->sync();
            terminal->force_repaint();
        }));
    });
    menubar->add_menu(move(font_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [&](const GAction&) {
        GAboutDialog::show("Terminal", load_png("/res/icons/32x32/app-terminal.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    config->sync();
    return app.exec();
}
