/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Julius Heijmen <julius.heijmen@gmail.com>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
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
    UnregisteredWidget(const String& class_name);

    virtual void paint_event(GUI::PaintEvent& event) override;

    String m_text;
};

UnregisteredWidget::UnregisteredWidget(const String& class_name)
{
    StringBuilder builder;
    builder.append(class_name);
    builder.append("\nnot registered");
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

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_protocol("/usr/share/man/man1/Playground.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio thread recvfd sendfd rpath cpath wpath"));

    const char* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "GML file to edit", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-playground"));
    auto window = TRY(GUI::Window::try_create());
    window->set_title("GML Playground");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(800, 600);

    auto splitter = TRY(window->try_set_main_widget<GUI::HorizontalSplitter>());

    auto editor = TRY(splitter->try_add<GUI::TextEditor>());
    auto preview = TRY(splitter->try_add<GUI::Frame>());

    editor->set_syntax_highlighter(make<GUI::GML::SyntaxHighlighter>());
    editor->set_autocomplete_provider(make<GUI::GML::AutocompleteProvider>());
    editor->set_should_autocomplete_automatically(true);
    editor->set_automatic_indentation_enabled(true);
    editor->set_ruler_visible(true);

    String file_path;
    auto update_title = [&] {
        StringBuilder builder;
        if (file_path.is_empty())
            builder.append("Untitled");
        else
            builder.append(file_path);

        if (window->is_modified())
            builder.append("[*]");

        builder.append(" - GML Playground");
        window->set_title(builder.to_string());
    };

    if (String(path).is_empty()) {
        editor->set_text(R"~~~(@GUI::Frame {
    layout: @GUI::VerticalBoxLayout {
    }

    // Now add some widgets!
}
)~~~");
        editor->set_cursor(4, 28); // after "...widgets!"
        update_title();
    } else {
        auto file = Core::File::construct(path);
        if (!file->open(Core::OpenMode::ReadOnly)) {
            GUI::MessageBox::show(window, String::formatted("Opening \"{}\" failed: {}", path, strerror(errno)), "Error", GUI::MessageBox::Type::Error);
            return 1;
        }
        if (file->is_device()) {
            GUI::MessageBox::show(window, String::formatted("Opening \"{}\" failed: Can't open device files", path), "Error", GUI::MessageBox::Type::Error);
            return 1;
        }
        file_path = path;
        editor->set_text(file->read_all());
        update_title();
    }

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
        Optional<String> new_save_path = GUI::FilePicker::get_save_filepath(window, "Untitled", "gml");
        if (!new_save_path.has_value())
            return;

        if (!editor->write_to_file(new_save_path.value())) {
            GUI::MessageBox::show(window, "Unable to save file.\n", "Error", GUI::MessageBox::Type::Error);
            return;
        }
        file_path = new_save_path.value();
        update_title();
    });

    auto save_action = GUI::CommonActions::make_save_action([&](auto&) {
        if (!file_path.is_empty()) {
            if (!editor->write_to_file(file_path)) {
                GUI::MessageBox::show(window, "Unable to save file.\n", "Error", GUI::MessageBox::Type::Error);
                return;
            }
            update_title();
            return;
        }

        save_as_action->activate();
    });

    TRY(file_menu->try_add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath(window);

        if (!open_path.has_value())
            return;

        if (window->is_modified()) {
            auto save_document_first_result = GUI::MessageBox::show(window, "Save changes to current document first?", "Warning", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNoCancel);
            if (save_document_first_result == GUI::Dialog::ExecResult::ExecYes)
                save_action->activate();
            if (save_document_first_result != GUI::Dialog::ExecResult::ExecNo && window->is_modified())
                return;
        }

        auto file = Core::File::construct(open_path.value());
        if (!file->open(Core::OpenMode::ReadOnly) && file->error() != ENOENT) {
            GUI::MessageBox::show(window, String::formatted("Opening \"{}\" failed: {}", open_path.value(), strerror(errno)), "Error", GUI::MessageBox::Type::Error);
            return;
        }

        if (file->is_device()) {
            GUI::MessageBox::show(window, String::formatted("Opening \"{}\" failed: Can't open device files", open_path.value()), "Error", GUI::MessageBox::Type::Error);
            return;
        }
        file_path = open_path.value();
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
            editor->set_text(formatted_gml_or_error.release_value());
        } else {
            GUI::MessageBox::show(
                window,
                String::formatted("GML could not be formatted: {}", formatted_gml_or_error.error()),
                "Error",
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

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man1/Playground.md"), "/bin/Help");
    })));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("GML Playground", app_icon, window)));

    window->on_close_request = [&] {
        if (!window->is_modified())
            return GUI::Window::CloseRequestDecision::Close;

        auto result = GUI::MessageBox::show(window, "The document has been modified. Would you like to save?", "Unsaved changes", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNoCancel);
        if (result == GUI::MessageBox::ExecYes) {
            save_action->activate();
            if (window->is_modified())
                return GUI::Window::CloseRequestDecision::StayOpen;
            return GUI::Window::CloseRequestDecision::Close;
        }

        if (result == GUI::MessageBox::ExecNo)
            return GUI::Window::CloseRequestDecision::Close;

        return GUI::Window::CloseRequestDecision::StayOpen;
    };
    window->show();
    return app->exec();
}
