/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Julius Heijmen <julius.heijmen@gmail.com>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, Karol Kosek <krkk@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include <AK/LexicalPath.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/FileTypeFilter.h>
#include <LibGUI/GML/AutocompleteProvider.h>
#include <LibGUI/GML/Formatter.h>
#include <LibGUI/GML/SyntaxHighlighter.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/RegularEditingEngine.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/VimEditingEngine.h>
#include <Userland/DevTools/GMLPlayground/GMLPlaygroundWindowGML.h>

namespace {

class UnregisteredWidget final : public GUI::Widget {
    C_OBJECT(UnregisteredWidget);

private:
    UnregisteredWidget(ByteString const& class_name);

    virtual void paint_event(GUI::PaintEvent& event) override;

    ByteString m_text;
};

UnregisteredWidget::UnregisteredWidget(ByteString const& class_name)
{
    StringBuilder builder;
    builder.append(class_name);
    builder.append("\nnot registered"sv);
    m_text = builder.to_byte_string();
}

void UnregisteredWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Gfx::Color::DarkRed);
    painter.draw_text(rect(), m_text, Gfx::TextAlignment::Center, Color::White);
}

}

ErrorOr<NonnullRefPtr<MainWidget>> MainWidget::try_create(GUI::Icon const& icon)
{
    auto main_widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) MainWidget()));
    TRY(main_widget->load_from_gml(gml_playground_window_gml));
    main_widget->m_icon = icon;

    main_widget->m_toolbar = main_widget->find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    main_widget->m_splitter = main_widget->find_descendant_of_type_named<GUI::HorizontalSplitter>("splitter");
    main_widget->m_editor = main_widget->find_descendant_of_type_named<GUI::TextEditor>("text_editor");
    main_widget->m_preview_frame_widget = main_widget->find_descendant_of_type_named<GUI::Frame>("preview_frame");
    main_widget->m_statusbar = main_widget->find_descendant_of_type_named<GUI::Statusbar>("statusbar");

    main_widget->m_preview_window = GUI::Window::construct(main_widget);
    main_widget->m_preview_window->set_title("Preview - GML Playground");
    main_widget->m_preview_window->set_icon(icon.bitmap_for_size(16));
    main_widget->m_preview_window_widget = main_widget->m_preview_window->set_main_widget<GUI::Widget>();
    main_widget->m_preview_window_widget->set_fill_with_background_color(true);

    main_widget->m_preview = main_widget->m_preview_frame_widget;

    main_widget->m_editor->set_syntax_highlighter(TRY(try_make<GUI::GML::SyntaxHighlighter>()));
    main_widget->m_editor->set_autocomplete_provider(TRY(try_make<GUI::GML::AutocompleteProvider>()));
    main_widget->m_editor->set_should_autocomplete_automatically(true);
    main_widget->m_editor->set_automatic_indentation_enabled(true);
    main_widget->m_editor->set_ruler_visible(true);

    main_widget->m_editor->on_change = [main_widget = main_widget.ptr()] {
        main_widget->m_preview->remove_all_children();
        // FIXME: Parsing errors happen while the user is typing. What should we do about them?
        (void)main_widget->m_preview->load_from_gml(main_widget->m_editor->text(), [](StringView class_name) -> ErrorOr<NonnullRefPtr<Core::EventReceiver>> {
            return UnregisteredWidget::try_create(class_name);
        });
    };

    main_widget->m_editor->on_modified_change = [main_widget = main_widget.ptr()](bool modified) {
        main_widget->window()->set_modified(modified);
    };

    return main_widget;
}

MainWidget::MainWidget()
{
    GUI::Application::the()->on_action_enter = [this](GUI::Action& action) {
        m_statusbar->set_override_text(action.status_tip());
    };
    GUI::Application::the()->on_action_leave = [this](GUI::Action&) {
        m_statusbar->set_override_text({});
    };
}

void MainWidget::update_title()
{
    window()->set_title(ByteString::formatted("{}[*] - GML Playground", m_file_path.is_empty() ? "Untitled"sv : m_file_path.view()));
}

void MainWidget::load_file(FileSystemAccessClient::File file)
{
    auto buffer_or_error = file.stream().read_until_eof();
    if (buffer_or_error.is_error())
        return;

    m_editor->set_text(buffer_or_error.release_value());
    m_editor->set_focus(true);

    m_file_path = file.filename();
    update_title();

    GUI::Application::the()->set_most_recently_open_file(file.filename());
}

ErrorOr<void> MainWidget::initialize_menubar(GUI::Window& window)
{
    auto file_menu = window.add_menu("&File"_string);

    m_save_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        LexicalPath initial_path(m_file_path.is_empty() ? "Untitled.gml" : m_file_path);
        auto response = FileSystemAccessClient::Client::the().save_file(&window, initial_path.title(), initial_path.extension());
        if (response.is_error())
            return;

        auto file = response.value().release_stream();
        if (auto result = m_editor->write_to_file(*file); result.is_error()) {
            GUI::MessageBox::show(&window, ByteString::formatted("Unable to save file: {}\n"sv, result.release_error()), "Error"sv, GUI::MessageBox::Type::Error);
            return;
        }
        m_file_path = response.value().filename();
        update_title();

        GUI::Application::the()->set_most_recently_open_file(response.value().filename());
    });

    m_save_action = GUI::CommonActions::make_save_action([&](auto&) {
        if (m_file_path.is_empty()) {
            m_save_as_action->activate();
            return;
        }
        auto response = FileSystemAccessClient::Client::the().request_file(&window, m_file_path, Core::File::OpenMode::Truncate | Core::File::OpenMode::Write);
        if (response.is_error())
            return;

        auto file = response.value().release_stream();
        if (auto result = m_editor->write_to_file(*file); result.is_error()) {
            GUI::MessageBox::show(&window, ByteString::formatted("Unable to save file: {}\n"sv, result.release_error()), "Error"sv, GUI::MessageBox::Type::Error);
            return;
        }
        update_title();
    });

    auto open_action = GUI::CommonActions::make_open_action([&](auto&) {
        if (request_close() == GUI::Window::CloseRequestDecision::StayOpen)
            return;
        FileSystemAccessClient::OpenFileOptions options {
            .path = "/usr/src/serenity/Userland/Applications"sv,
            .allowed_file_types = Vector {
                GUI::FileTypeFilter { "GML Files", { { "gml" } } },
                GUI::FileTypeFilter::all_files(),
            }
        };
        auto response = FileSystemAccessClient::Client::the().open_file(&window, options);
        if (response.is_error())
            return;

        load_file(response.release_value());
    });

    file_menu->add_action(open_action);
    file_menu->add_action(*m_save_action);
    file_menu->add_action(*m_save_as_action);
    file_menu->add_separator();

    file_menu->add_recent_files_list([&](auto& action) {
        if (request_close() == GUI::Window::CloseRequestDecision::StayOpen)
            return;
        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(&window, action.text());
        if (response.is_error())
            return;
        load_file(response.release_value());
    });

    file_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        if (window.on_close_request() == GUI::Window::CloseRequestDecision::Close)
            GUI::Application::the()->quit();
    }));

    auto edit_menu = window.add_menu("&Edit"_string);
    edit_menu->add_action(m_editor->undo_action());
    edit_menu->add_action(m_editor->redo_action());
    edit_menu->add_separator();
    edit_menu->add_action(m_editor->cut_action());
    edit_menu->add_action(m_editor->copy_action());
    edit_menu->add_action(m_editor->paste_action());
    edit_menu->add_separator();
    edit_menu->add_action(m_editor->select_all_action());
    edit_menu->add_action(m_editor->go_to_line_or_column_action());
    edit_menu->add_separator();

    auto format_gml_action = GUI::Action::create("&Format GML", { Mod_Ctrl | Mod_Shift, Key_I }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/reformat.png"sv)), [&](auto&) {
        auto formatted_gml_or_error = GUI::GML::format_gml(m_editor->text());
        if (!formatted_gml_or_error.is_error()) {
            m_editor->replace_all_text_without_resetting_undo_stack(formatted_gml_or_error.release_value(), "Format GML"sv);
        } else {
            GUI::MessageBox::show(
                &window,
                ByteString::formatted("GML could not be formatted: {}", formatted_gml_or_error.error()),
                "Error"sv,
                GUI::MessageBox::Type::Error);
        }
    });
    edit_menu->add_action(format_gml_action);

    auto vim_emulation_setting_action = GUI::Action::create_checkable("&Vim Emulation", { Mod_Ctrl | Mod_Shift | Mod_Alt, Key_V }, [&](auto& action) {
        if (action.is_checked())
            m_editor->set_editing_engine(make<GUI::VimEditingEngine>());
        else
            m_editor->set_editing_engine(make<GUI::RegularEditingEngine>());
    });
    vim_emulation_setting_action->set_checked(false);
    edit_menu->add_action(vim_emulation_setting_action);

    auto view_menu = window.add_menu("&View"_string);
    m_views_group.set_exclusive(true);
    m_views_group.set_unchecking_allowed(false);

    m_view_frame_action = GUI::Action::create_checkable("&Frame", [&](auto&) {
        dbgln("View switched to frame");
        m_preview = m_preview_frame_widget;
        m_editor->on_change();
        m_preview_window->hide();
        m_preview_frame_widget->set_preferred_width(m_splitter->width() / 2);
        m_preview_frame_widget->set_visible(true);
    });
    view_menu->add_action(*m_view_frame_action);
    m_views_group.add_action(*m_view_frame_action);
    m_view_frame_action->set_checked(true);

    m_view_window_action = GUI::Action::create_checkable("&Window", [&](auto&) {
        dbgln("View switched to window");
        m_preview = m_preview_window_widget;
        m_editor->on_change();
        m_preview_window->resize(400, 300);
        m_preview_window->show();
        m_preview_frame_widget->set_visible(false);
    });
    view_menu->add_action(*m_view_window_action);
    m_views_group.add_action(*m_view_window_action);
    view_menu->add_separator();
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window.set_fullscreen(!window.is_fullscreen());
    }));

    m_preview_window->on_close = [&] {
        m_view_frame_action->activate();
    };

    auto help_menu = window.add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(&window));
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man1/Applications/GMLPlayground.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("GML Playground"_string, m_icon, &window));

    m_toolbar->add_action(open_action);
    m_toolbar->add_action(*m_save_action);
    m_toolbar->add_action(*m_save_as_action);
    m_toolbar->add_separator();
    m_toolbar->add_action(m_editor->cut_action());
    m_toolbar->add_action(m_editor->copy_action());
    m_toolbar->add_action(m_editor->paste_action());
    m_toolbar->add_separator();
    m_toolbar->add_action(m_editor->undo_action());
    m_toolbar->add_action(m_editor->redo_action());
    m_toolbar->add_separator();
    m_toolbar->add_action(format_gml_action);

    return {};
}

GUI::Window::CloseRequestDecision MainWidget::request_close()
{
    if (!window()->is_modified())
        return GUI::Window::CloseRequestDecision::Close;

    auto result = GUI::MessageBox::ask_about_unsaved_changes(window(), m_file_path, m_editor->document().undo_stack().last_unmodified_timestamp());
    if (result == GUI::MessageBox::ExecResult::Yes) {
        m_save_action->activate();
        if (window()->is_modified())
            return GUI::Window::CloseRequestDecision::StayOpen;
        return GUI::Window::CloseRequestDecision::Close;
    }

    if (result == GUI::MessageBox::ExecResult::No)
        return GUI::Window::CloseRequestDecision::Close;

    return GUI::Window::CloseRequestDecision::StayOpen;
}

void MainWidget::drag_enter_event(GUI::DragEvent& event)
{
    if (event.mime_data().has_urls())
        event.accept();
}

void MainWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();
    window()->move_to_front();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        if (urls.is_empty())
            return;
        if (urls.size() > 1) {
            GUI::MessageBox::show(window(), "GML Playground can only open one file at a time!"sv, "One at a time please!"sv, GUI::MessageBox::Type::Error);
            return;
        }
        if (request_close() == GUI::Window::CloseRequestDecision::StayOpen)
            return;

        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(window(), URL::percent_decode(urls.first().serialize_path()));
        if (response.is_error())
            return;
        load_file(response.release_value());
    }
}
