/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"

#include <AK/LexicalPath.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <LibCore/File.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibURL/URL.h>
#include <errno.h>

#include "SampleFileBlock.h"

MainWidget::MainWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    set_fill_with_background_color(true);
}

ErrorOr<void> MainWidget::open(StringView path)
{
    auto source_file = TRY(try_make_ref_counted<SampleSourceFile>(path));
    size_t length = source_file->length();
    auto file_block = TRY(try_make_ref_counted<SampleFileBlock>(
        source_file, (size_t)0, (size_t)length - 1));
    m_sample_widget->set(file_block);
    m_sample_path = path;
    m_sample_name = LexicalPath { path }.title();
    window()->set_title(
        ByteString::formatted("Sample Editor - {}", m_sample_name));
    return {};
}

ErrorOr<void> MainWidget::save(StringView path)
{
    TRY(m_sample_widget->save(path));
    window()->set_title(
        ByteString::formatted("Sample Editor - {}", LexicalPath { path }.title()));
    return {};
}

ErrorOr<void> MainWidget::initialize_menu_and_toolbar(
    NonnullRefPtr<GUI::Window> window)
{
    m_toolbar_container = add<GUI::ToolbarContainer>();
    m_toolbar = m_toolbar_container->add<GUI::Toolbar>();

    m_new_action = GUI::Action::create(
        "&New", { Mod_Ctrl, Key_N },
        TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/new.png"sv)),
        [this, window](const GUI::Action&) {
            m_sample_widget->clear();
            m_sample_path = {};
            m_sample_name = {};
            window->set_title("Sample Editor");
        });

    m_open_action = GUI::CommonActions::make_open_action([this, window](auto&) {
        FileSystemAccessClient::OpenFileOptions options {
            .window_title = "Open sample file..."sv,
            .allowed_file_types = { { GUI::FileTypeFilter { "Audio Files", { { "wav", "flac" } } },
                GUI::FileTypeFilter::all_files() } }
        };
        auto response = FileSystemAccessClient::Client::the().open_file(window, options);
        if (response.is_error())
            return;
        auto filename = response.value().filename();
        if (auto result = open(filename); result.is_error()) {
            auto message = MUST(String::formatted("Failed to open file: {}", result.error()));
            GUI::MessageBox::show_error(window.ptr(), message);
        }
    });

    m_save_action = GUI::CommonActions::make_save_action([this, window](auto&) {
        if (m_sample_path.is_empty())
            return;
        if (auto result = save(m_sample_path); result.is_error()) {
            auto message = MUST(String::formatted("Failed to save file: {}", result.error()));
            GUI::MessageBox::show_error(window.ptr(), message);
        }
    });

    m_save_as_action = GUI::CommonActions::make_save_as_action([this, window](auto&) {
        // Default extension based on current file, or wav if new
        ByteString default_extension = "wav";
        if (!m_sample_path.is_empty()) {
            auto current_extension = LexicalPath { m_sample_path }.extension();
            if (current_extension.equals_ignoring_ascii_case("flac"sv))
                default_extension = "flac";
        }

        auto response = FileSystemAccessClient::Client::the().save_file(
            window, m_sample_name, default_extension, Core::File::OpenMode::ReadWrite);
        if (response.is_error()) {
            auto const& error = response.error();
            if (!error.is_errno() || error.code() != ECANCELED) {
                auto message = MUST(String::formatted("Failed to prepare save target: {}", error));
                GUI::MessageBox::show_error(window.ptr(), message);
            }
            return;
        }
        auto filename = response.value().filename();
        if (auto result = save(filename); result.is_error()) {
            auto message = MUST(String::formatted("Failed to save file: {}", result.error()));
            GUI::MessageBox::show_error(window.ptr(), message);
            return;
        }
    });

    m_copy_action = GUI::CommonActions::make_copy_action([this, window](auto&) {
        ErrorOr<String> maybe_selection = m_sample_widget->selection();
        if (maybe_selection.is_error()) {
            auto message = MUST(String::formatted("Copy failed: {}", maybe_selection.error()));
            GUI::MessageBox::show_error(window.ptr(), message);
        } else {
            StringView selection = maybe_selection.value();
            GUI::Clipboard::the().set_plain_text(selection);
        }
    });

    m_cut_action = GUI::CommonActions::make_cut_action([this, window](auto&) {
        ErrorOr<String> maybe_cut = m_sample_widget->cut();
        if (maybe_cut.is_error()) {
            auto message = MUST(String::formatted("Cut failed: {}", maybe_cut.error()));
            GUI::MessageBox::show_error(window.ptr(), message);
        } else {
            StringView cut_content = maybe_cut.value();
            GUI::Clipboard::the().set_plain_text(cut_content);
        }
    });

    m_paste_action = GUI::CommonActions::make_paste_action([this, window](auto&) {
        auto [data, mime_type, _] = GUI::Clipboard::the().fetch_data_and_type();
        if (!mime_type.starts_with("text/"sv) || data.is_empty()) {
            GUI::MessageBox::show_error(window.ptr(), "Clipboard is empty or does not contain text data"_string);
            return;
        }

        auto clipboard_bytes = ByteString::from_utf8(data.bytes());
        if (clipboard_bytes.is_error()) {
            auto message = MUST(String::formatted("Clipboard data is invalid UTF-8: {}", clipboard_bytes.error()));
            GUI::MessageBox::show_error(window.ptr(), message);
            return;
        }

        auto clipboard_text = String::from_byte_string(clipboard_bytes.release_value());
        if (clipboard_text.is_error()) {
            auto message = MUST(String::formatted("Clipboard text conversion failed: {}", clipboard_text.error()));
            GUI::MessageBox::show_error(window.ptr(), message);
            return;
        }

        auto result = m_sample_widget->paste_from_text(clipboard_text.release_value());
        if (result.is_error()) {
            auto message = MUST(String::formatted("Paste failed: {}", result.error()));
            GUI::MessageBox::show_error(window.ptr(), message);
        }
    });

    m_select_all_action = GUI::CommonActions::make_select_all_action([this](auto&) {
        m_sample_widget->select_all();
    });

    m_clear_selection_action = GUI::Action::create(
        "Clear Selection",
        TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/clear-selection.png"sv)),
        [this](auto&) {
            m_sample_widget->clear_selection();
        });

    m_zoom_in_action = GUI::CommonActions::make_zoom_in_action(
        [this](auto&) { m_sample_widget->zoom_in(); });

    m_zoom_out_action = GUI::CommonActions::make_zoom_out_action(
        [this](auto&) { m_sample_widget->zoom_out(); });

    m_play_action = GUI::Action::create(
        "Play", { Mod_None, Key_Space },
        TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/play.png"sv)),
        [this](auto&) {
            m_sample_widget->play();
            m_play_action->set_enabled(false);
            m_stop_action->set_enabled(true);
        });

    m_stop_action = GUI::Action::create(
        "Stop",
        TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/stop.png"sv)),
        [this](auto&) {
            m_sample_widget->stop();
            m_play_action->set_enabled(true);
            m_stop_action->set_enabled(false);
        });
    m_stop_action->set_enabled(false);

    m_toolbar->add_action(*m_new_action);
    m_toolbar->add_action(*m_open_action);
    m_toolbar->add_action(*m_save_action);
    m_toolbar->add_action(*m_save_as_action);
    m_toolbar->add_action(*m_copy_action);
    m_toolbar->add_action(*m_cut_action);
    m_toolbar->add_action(*m_paste_action);
    m_toolbar->add_action(*m_select_all_action);
    m_toolbar->add_action(*m_clear_selection_action);
    m_toolbar->add_separator();
    m_toolbar->add_action(*m_play_action);
    m_toolbar->add_action(*m_stop_action);
    m_toolbar->add_separator();
    m_toolbar->add_action(*m_zoom_in_action);
    m_toolbar->add_action(*m_zoom_out_action);
    m_sample_widget = add<SampleWidget>();

    m_sample_widget->on_playback_finished = [this]() {
        m_play_action->set_enabled(true);
        m_stop_action->set_enabled(false);
    };

    auto file_menu = window->add_menu("&File"_string);
    file_menu->add_action(*m_new_action);
    file_menu->add_action(*m_open_action);
    file_menu->add_action(*m_save_action);
    file_menu->add_action(*m_save_as_action);
    file_menu->add_separator();
    file_menu->add_action(GUI::CommonActions::make_quit_action(
        [](auto&) { GUI::Application::the()->quit(); }));

    auto edit_menu = window->add_menu("&Edit"_string);
    edit_menu->add_action(*m_copy_action);
    edit_menu->add_action(*m_cut_action);
    edit_menu->add_action(*m_paste_action);
    edit_menu->add_separator();
    edit_menu->add_action(*m_select_all_action);
    edit_menu->add_action(*m_clear_selection_action);

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(*m_zoom_in_action);
    view_menu->add_action(*m_zoom_out_action);

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man1/Applications/SampleEditor.md"), "/bin/Help");
    }));

    help_menu->add_action(GUI::CommonActions::make_about_action(
        "Sample Editor"_string, GUI::Icon::default_icon("app-sample-editor"sv),
        window));

    m_sample_widget->on_selection_changed = [this]() {
        update_action_states();
    };

    update_action_states();

    return {};
}

void MainWidget::update_action_states()
{
    bool has_selection = m_sample_widget->has_selection();
    m_copy_action->set_enabled(has_selection);
    m_cut_action->set_enabled(has_selection);

    bool has_cursor = m_sample_widget->has_cursor_placed();
    bool is_initial_state = m_sample_widget->is_initial_null_block();
    auto clipboard_mime = GUI::Clipboard::the().fetch_mime_type();
    bool has_text_in_clipboard = clipboard_mime.starts_with("text/"sv);
    m_paste_action->set_enabled((has_cursor || is_initial_state) && has_text_in_clipboard);
}

void MainWidget::clipboard_content_did_change(ByteString const& mime_type)
{
    bool has_cursor = m_sample_widget->has_cursor_placed();
    bool is_initial_state = m_sample_widget->is_initial_null_block();
    bool has_text_in_clipboard = mime_type.starts_with("text/"sv);
    m_paste_action->set_enabled((has_cursor || is_initial_state) && has_text_in_clipboard);
}
