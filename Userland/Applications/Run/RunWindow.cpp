/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RunWindow.h"
#include <AK/LexicalPath.h>
#include <AK/URL.h>
#include <Applications/Run/RunGML.h>
#include <LibCore/Process.h>
#include <LibCore/StandardPaths.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Button.h>
#include <LibGUI/Event.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Widget.h>
#include <spawn.h>
#include <stdio.h>
#include <string.h>
#include <sys/wait.h>
#include <unistd.h>

RunWindow::RunWindow()
    : m_path_history()
    , m_path_history_model(GUI::ItemListModel<DeprecatedString>::create(m_path_history))
{
    // FIXME: Handle failure to load history somehow.
    (void)load_history();

    auto app_icon = GUI::Icon::default_icon("app-run"sv);

    set_title("Run");
    set_icon(app_icon.bitmap_for_size(16));
    resize(345, 100);
    set_resizable(false);
    set_minimizable(false);

    auto main_widget = set_main_widget<GUI::Widget>();
    main_widget->load_from_gml(run_gml).release_value_but_fixme_should_propagate_errors();

    m_icon_image_widget = *main_widget->find_descendant_of_type_named<GUI::ImageWidget>("icon");
    m_icon_image_widget->set_bitmap(app_icon.bitmap_for_size(32));

    m_path_combo_box = *main_widget->find_descendant_of_type_named<GUI::ComboBox>("path");
    m_path_combo_box->set_model(m_path_history_model);
    if (!m_path_history.is_empty())
        m_path_combo_box->set_selected_index(0);

    m_ok_button = *main_widget->find_descendant_of_type_named<GUI::DialogButton>("ok_button");
    m_ok_button->on_click = [this](auto) {
        do_run();
    };
    m_ok_button->set_default(true);

    m_cancel_button = *main_widget->find_descendant_of_type_named<GUI::DialogButton>("cancel_button");
    m_cancel_button->on_click = [this](auto) {
        close();
    };

    m_browse_button = *find_descendant_of_type_named<GUI::DialogButton>("browse_button");
    m_browse_button->on_click = [this](auto) {
        Optional<DeprecatedString> path = GUI::FilePicker::get_open_filepath(this, {}, Core::StandardPaths::home_directory(), false, GUI::Dialog::ScreenPosition::Center);
        if (path.has_value())
            m_path_combo_box->set_text(path.value().view());
    };
}

void RunWindow::event(Core::Event& event)
{
    if (event.type() == GUI::Event::KeyDown) {
        auto& key_event = static_cast<GUI::KeyEvent&>(event);
        if (key_event.key() == Key_Escape) {
            // Escape key pressed, close dialog
            close();
            return;
        } else if ((key_event.key() == Key_Up || key_event.key() == Key_Down) && m_path_history.is_empty()) {
            return;
        }
    }

    Window::event(event);
}

void RunWindow::do_run()
{
    auto run_input = m_path_combo_box->text().trim_whitespace();

    hide();

    if (run_via_launch(run_input) || run_as_command(run_input)) {
        // Remove any existing history entry, prepend the successful run string to history and save.
        m_path_history.remove_all_matching([&](DeprecatedString v) { return v == run_input; });
        m_path_history.prepend(run_input);
        // FIXME: Handle failure to save history somehow.
        (void)save_history();

        close();
        return;
    }

    GUI::MessageBox::show_error(this, "Failed to run. Please check your command, path, or address, and try again."sv);

    show();
}

bool RunWindow::run_as_command(DeprecatedString const& run_input)
{
    // TODO: Query and use the user's preferred shell.
    auto maybe_child_pid = Core::Process::spawn("/bin/Shell"sv, Array { "-c", run_input.characters() }, {}, Core::Process::KeepAsChild::Yes);
    if (maybe_child_pid.is_error())
        return false;

    pid_t child_pid = maybe_child_pid.release_value();

    // Command spawned in child shell. Hide and wait for exit code.
    int status;
    if (waitpid(child_pid, &status, 0) < 0)
        return false;

    int child_error = WEXITSTATUS(status);
    dbgln("Child shell exited with code {}", child_error);

    // 127 is typically the shell indicating command not found. 126 for all other errors.
    if (child_error == 126 || child_error == 127) {
        return false;
    }

    dbgln("Ran via command shell.");

    return true;
}

bool RunWindow::run_via_launch(DeprecatedString const& run_input)
{
    auto url = URL::create_with_url_or_path(run_input);

    if (url.scheme() == "file") {
        auto file_path = url.serialize_path();
        auto real_path_or_error = FileSystem::real_path(file_path);
        if (real_path_or_error.is_error()) {
            warnln("Failed to launch '{}': {}", file_path, real_path_or_error.error());
            return false;
        }
        url = URL::create_with_url_or_path(real_path_or_error.release_value().to_deprecated_string());
    }

    if (!Desktop::Launcher::open(url)) {
        warnln("Failed to launch '{}'", url);
        return false;
    }

    dbgln("Ran via URL launch.");

    return true;
}

DeprecatedString RunWindow::history_file_path()
{
    return LexicalPath::canonicalized_path(DeprecatedString::formatted("{}/{}", Core::StandardPaths::config_directory(), "RunHistory.txt"));
}

ErrorOr<void> RunWindow::load_history()
{
    m_path_history.clear();
    auto file = TRY(Core::File::open(history_file_path(), Core::File::OpenMode::Read));
    auto buffered_file = TRY(Core::InputBufferedFile::create(move(file)));
    Array<u8, PAGE_SIZE> line_buffer;

    while (!buffered_file->is_eof()) {
        StringView line = TRY(buffered_file->read_line(line_buffer));
        if (!line.is_empty() && !line.is_whitespace())
            m_path_history.append(line);
    }
    return {};
}

ErrorOr<void> RunWindow::save_history()
{
    auto file = TRY(Core::File::open(history_file_path(), Core::File::OpenMode::Write));

    // Write the first 25 items of history
    for (int i = 0; i < min(static_cast<int>(m_path_history.size()), 25); i++)
        TRY(file->write_until_depleted(DeprecatedString::formatted("{}\n", m_path_history[i]).bytes()));

    return {};
}
