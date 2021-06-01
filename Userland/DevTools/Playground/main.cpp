/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GMLAutocompleteProvider.h"
#include <AK/URL.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/GMLFormatter.h>
#include <LibGUI/GMLLexer.h>
#include <LibGUI/GMLSyntaxHighlighter.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>
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

int main(int argc, char** argv)
{
    if (pledge("stdio thread recvfd sendfd cpath rpath wpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio thread recvfd sendfd rpath cpath wpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (!Desktop::Launcher::add_allowed_handler_with_only_specific_urls(
            "/bin/Help",
            { URL::create_with_file_protocol("/usr/share/man/man1/Playground.md") })
        || !Desktop::Launcher::seal_allowlist()) {
        warnln("Failed to set up allowed launch URLs");
        return 1;
    }

    if (pledge("stdio thread recvfd sendfd rpath cpath wpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "GML file to edit", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto app_icon = GUI::Icon::default_icon("app-playground");
    auto window = GUI::Window::construct();
    window->set_title("GML Playground");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(800, 600);

    auto& splitter = window->set_main_widget<GUI::HorizontalSplitter>();

    auto& editor = splitter.add<GUI::TextEditor>();
    auto& preview = splitter.add<GUI::Frame>();

    editor.set_syntax_highlighter(make<GUI::GMLSyntaxHighlighter>());
    editor.set_autocomplete_provider(make<GMLAutocompleteProvider>());
    editor.set_should_autocomplete_automatically(true);
    editor.set_automatic_indentation_enabled(true);

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
        editor.set_text(R"~~~(@GUI::Frame {
    layout: @GUI::VerticalBoxLayout {
    }

    // Now add some widgets!
}
)~~~");
        editor.set_cursor(4, 28); // after "...widgets!"
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
        editor.set_text(file->read_all());
        update_title();
    }

    editor.on_change = [&] {
        preview.remove_all_children();
        preview.load_from_gml(editor.text(), [](const String& class_name) -> RefPtr<Core::Object> {
            return UnregisteredWidget::construct(class_name);
        });
    };

    editor.on_modified_change = [&](bool modified) {
        window->set_modified(modified);
        update_title();
    };

    auto menubar = GUI::Menubar::construct();
    auto& file_menu = menubar->add_menu("&File");

    auto save_action = GUI::CommonActions::make_save_as_action([&](auto&) {
        Optional<String> save_path = GUI::FilePicker::get_save_filepath(window, "Untitled", "gml");
        if (!save_path.has_value())
            return;

        if (!editor.write_to_file(save_path.value())) {
            GUI::MessageBox::show(window, "Unable to save file.\n", "Error", GUI::MessageBox::Type::Error);
            return;
        }
        update_title();
    });

    file_menu.add_action(GUI::CommonActions::make_open_action([&](auto&) {
        if (!file_path.is_empty() && window->is_modified()) {
            auto save_document_first_result = GUI::MessageBox::show(window, "Save changes to current document first?", "Warning", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNoCancel);
            if (save_document_first_result == GUI::Dialog::ExecResult::ExecYes)
                save_action->activate();
            if (save_document_first_result == GUI::Dialog::ExecResult::ExecCancel)
                return;
        }

        Optional<String> open_path = GUI::FilePicker::get_open_filepath(window);

        if (!open_path.has_value())
            return;

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
        editor.set_text(file->read_all());
        editor.set_focus(true);
        update_title();
    }));

    file_menu.add_action(save_action);
    file_menu.add_separator();

    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));

    auto& edit_menu = menubar->add_menu("&Edit");
    edit_menu.add_action(GUI::Action::create("&Format GML", { Mod_Ctrl | Mod_Shift, Key_I }, [&](auto&) {
        auto source = editor.text();
        GUI::GMLLexer lexer(source);
        for (auto& token : lexer.lex()) {
            if (token.m_type == GUI::GMLToken::Type::Comment) {
                auto result = GUI::MessageBox::show(
                    window,
                    "Your GML contains comments, which currently are not supported by the formatter and will be removed. Proceed?",
                    "Warning",
                    GUI::MessageBox::Type::Warning,
                    GUI::MessageBox::InputType::OKCancel);
                if (result == GUI::MessageBox::ExecCancel)
                    return;
                break;
            }
        }
        auto formatted_gml = GUI::format_gml(source);
        if (!formatted_gml.is_null()) {
            editor.set_text(formatted_gml);
        } else {
            GUI::MessageBox::show(
                window,
                "GML could not be formatted, please check the debug console for parsing errors.",
                "Error",
                GUI::MessageBox::Type::Error);
        }
    }));

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man1/Playground.md"), "/bin/Help");
    }));
    help_menu.add_action(GUI::CommonActions::make_about_action("GML Playground", app_icon, window));

    window->set_menubar(move(menubar));
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
