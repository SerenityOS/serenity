/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Julius Heijmen <julius.heijmen@gmail.com>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/GML/AutocompleteProvider.h>
#include <LibGUI/GML/Formatter.h>
#include <LibGUI/GML/Lexer.h>
#include <LibGUI/GML/SyntaxHighlighter.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/RegularEditingEngine.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/VimEditingEngine.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <string.h>
#include <unistd.h>

namespace {

class UnregisteredWidget final : public GUI::Widget {
    C_OBJECT(UnregisteredWidget);

private:
    UnregisteredWidget(String const& class_name);

    virtual void paint_event(GUI::PaintEvent& event) override;

    String m_text;
};

UnregisteredWidget::UnregisteredWidget(String const& class_name)
{
    StringBuilder builder;
    builder.append(class_name);
    builder.append("\nnot registered"sv);
    m_text = builder.to_string();
}

void UnregisteredWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Gfx::Color::DarkRed);
    painter.draw_text(rect(), m_text, Gfx::TextAlignment::Center, Color::White);
}

}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio thread recvfd sendfd cpath rpath wpath unix"));
    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man1/GMLPlayground.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    char const* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "GML file to edit", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-gml-playground"sv));
    auto window = TRY(GUI::Window::try_create());
    window->set_title("GML Playground");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(800, 600);

    auto splitter = TRY(window->try_set_main_widget<GUI::HorizontalSplitter>());
    auto editor = TRY(splitter->try_add<GUI::TextEditor>());
    auto preview_frame_widget = TRY(splitter->try_add<GUI::Frame>());

    auto preview_window = TRY(GUI::Window::try_create());
    preview_window->set_title("Preview - GML Playground");
    preview_window->set_icon(app_icon.bitmap_for_size(16));
    auto preview_window_widget = TRY(preview_window->try_set_main_widget<GUI::Widget>());

    GUI::Widget* preview = preview_frame_widget;

    editor->set_syntax_highlighter(make<GUI::GML::SyntaxHighlighter>());
    editor->set_autocomplete_provider(make<GUI::GML::AutocompleteProvider>());
    editor->set_should_autocomplete_automatically(true);
    editor->set_automatic_indentation_enabled(true);
    editor->set_ruler_visible(true);

    String file_path;
    auto update_title = [&] {
        StringBuilder builder;
        if (file_path.is_empty())
            builder.append("Untitled"sv);
        else
            builder.append(file_path);

        if (window->is_modified())
            builder.append("[*]"sv);

        builder.append(" - GML Playground"sv);
        window->set_title(builder.to_string());
    };

    editor->on_change = [&] {
        preview->remove_all_children();
        preview->load_from_gml(editor->text(), [](const String& class_name) -> RefPtr<Core::Object> {
            return UnregisteredWidget::construct(class_name);
        });
    };

    editor->on_modified_change = [&](bool modified) {
        window->set_modified(modified);
        update_title();
    };

    auto file_menu = TRY(window->try_add_menu("&File"));

    auto save_as_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        auto response = FileSystemAccessClient::Client::the().try_save_file(window, "Untitled", "gml");
        if (response.is_error())
            return;

        auto file = response.release_value();
        if (!editor->write_to_file(file)) {
            GUI::MessageBox::show(window, "Unable to save file.\n"sv, "Error"sv, GUI::MessageBox::Type::Error);
            return;
        }
        file_path = file->filename();
        update_title();
    });

    auto save_action = GUI::CommonActions::make_save_action([&](auto&) {
        if (file_path.is_empty()) {
            save_as_action->activate();
            return;
        }
        auto response = FileSystemAccessClient::Client::the().try_request_file(window, file_path, Core::OpenMode::Truncate | Core::OpenMode::WriteOnly);
        if (response.is_error())
            return;

        auto file = response.release_value();
        if (!editor->write_to_file(file)) {
            GUI::MessageBox::show(window, "Unable to save file.\n"sv, "Error"sv, GUI::MessageBox::Type::Error);
            return;
        }
        update_title();
    });

    TRY(file_menu->try_add_action(GUI::CommonActions::make_open_action([&](auto&) {
        if (window->is_modified()) {
            auto result = GUI::MessageBox::ask_about_unsaved_changes(window, file_path, editor->document().undo_stack().last_unmodified_timestamp());
            if (result == GUI::MessageBox::ExecResult::Yes)
                save_action->activate();
            if (result != GUI::MessageBox::ExecResult::No && window->is_modified())
                return;
        }

        auto response = FileSystemAccessClient::Client::the().try_open_file(window);
        if (response.is_error())
            return;

        auto file = response.release_value();
        file_path = file->filename();
        editor->set_text(file->read_all());
        editor->set_focus(true);
        update_title();
    })));

    TRY(file_menu->try_add_action(save_action));
    TRY(file_menu->try_add_action(save_as_action));
    TRY(file_menu->try_add_separator());

    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        if (window->on_close_request() == GUI::Window::CloseRequestDecision::Close)
            app->quit();
    })));

    auto edit_menu = TRY(window->try_add_menu("&Edit"));
    TRY(edit_menu->try_add_action(editor->undo_action()));
    TRY(edit_menu->try_add_action(editor->redo_action()));
    TRY(edit_menu->try_add_separator());
    TRY(edit_menu->try_add_action(editor->cut_action()));
    TRY(edit_menu->try_add_action(editor->copy_action()));
    TRY(edit_menu->try_add_action(editor->paste_action()));
    TRY(edit_menu->try_add_separator());
    TRY(edit_menu->try_add_action(editor->select_all_action()));
    TRY(edit_menu->try_add_action(editor->go_to_line_action()));
    TRY(edit_menu->try_add_separator());

    TRY(edit_menu->try_add_action(GUI::Action::create("&Format GML", { Mod_Ctrl | Mod_Shift, Key_I }, [&](auto&) {
        auto formatted_gml_or_error = GUI::GML::format_gml(editor->text());
        if (!formatted_gml_or_error.is_error()) {
            editor->replace_all_text_without_resetting_undo_stack(formatted_gml_or_error.release_value());
        } else {
            GUI::MessageBox::show(
                window,
                String::formatted("GML could not be formatted: {}", formatted_gml_or_error.error()),
                "Error"sv,
                GUI::MessageBox::Type::Error);
        }
    })));

    auto vim_emulation_setting_action = GUI::Action::create_checkable("&Vim Emulation", { Mod_Ctrl | Mod_Shift | Mod_Alt, Key_V }, [&](auto& action) {
        if (action.is_checked())
            editor->set_editing_engine(make<GUI::VimEditingEngine>());
        else
            editor->set_editing_engine(make<GUI::RegularEditingEngine>());
    });
    vim_emulation_setting_action->set_checked(false);
    TRY(edit_menu->try_add_action(vim_emulation_setting_action));

    auto view_menu = TRY(window->try_add_menu("&View"));
    GUI::ActionGroup views_group;
    views_group.set_exclusive(true);
    views_group.set_unchecking_allowed(false);

    auto view_frame_action = GUI::Action::create_checkable("&Frame", [&](auto&) {
        dbgln("View switched to frame");
        preview = preview_frame_widget;
        editor->on_change();
        preview_window->hide();
        preview_frame_widget->set_preferred_width(splitter->width() / 2);
        preview_frame_widget->set_visible(true);
    });
    view_menu->add_action(view_frame_action);
    views_group.add_action(view_frame_action);
    view_frame_action->set_checked(true);

    auto view_window_action = GUI::Action::create_checkable("&Window", [&](auto&) {
        dbgln("View switched to window");
        preview = preview_window_widget;
        editor->on_change();
        preview_window->resize(400, 300);
        preview_window->show();
        preview_frame_widget->set_visible(false);
    });
    view_menu->add_action(view_window_action);
    views_group.add_action(view_window_action);

    preview_window->on_close = [&] {
        view_frame_action->activate();
    };

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man1/GMLPlayground.md"), "/bin/Help");
    })));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("GML Playground", app_icon, window)));

    window->on_close_request = [&] {
        if (!window->is_modified())
            return GUI::Window::CloseRequestDecision::Close;

        auto result = GUI::MessageBox::ask_about_unsaved_changes(window, file_path, editor->document().undo_stack().last_unmodified_timestamp());
        if (result == GUI::MessageBox::ExecResult::Yes) {
            save_action->activate();
            if (window->is_modified())
                return GUI::Window::CloseRequestDecision::StayOpen;
            return GUI::Window::CloseRequestDecision::Close;
        }

        if (result == GUI::MessageBox::ExecResult::No)
            return GUI::Window::CloseRequestDecision::Close;

        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    window->show();

    if (String(path).is_empty()) {
        editor->set_text(R"~~~(@GUI::Frame {
    layout: @GUI::VerticalBoxLayout {
    }

    // Now add some widgets!
}
)~~~"sv);
        editor->set_cursor(4, 28); // after "...widgets!"
        update_title();
    } else {
        auto file = TRY(FileSystemAccessClient::Client::the().try_request_file_read_only_approved(window, path));
        file_path = path;
        editor->set_text(file->read_all());
        update_title();
    }

    return app->exec();
}
