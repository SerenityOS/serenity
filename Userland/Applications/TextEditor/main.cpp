/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileArgument.h"
#include "MainWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibMain/Main.h>

using namespace TextEditor;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd thread rpath cpath wpath unix"));

    auto app = TRY(GUI::Application::create(arguments));

    Config::pledge_domain("TextEditor");

    app->set_config_domain("TextEditor"_string);

    auto preview_mode = "auto"sv;
    StringView file_to_edit;
    Core::ArgsParser parser;
    parser.add_option(preview_mode, "Preview mode, one of 'none', 'html', 'markdown', 'auto'", "preview-mode", 'p', "mode");
    parser.add_positional_argument(file_to_edit, "File to edit, with optional starting line and column number", "file[:line[:column]]", Core::ArgsParser::Required::No);
    parser.parse(arguments);

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/webcontent", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-text-editor"sv);

    auto window = GUI::Window::construct();
    window->restore_size_and_position("TextEditor"sv, "Window"sv, { { 640, 400 } });
    window->save_size_and_position_on_close("TextEditor"sv, "Window"sv);

    auto text_widget = TRY(TextEditor::MainWidget::try_create());
    window->set_main_widget(text_widget.ptr());

    text_widget->editor().set_focus(true);

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (text_widget->request_close())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    if (preview_mode == "auto") {
        text_widget->set_auto_detect_preview_mode(true);
    } else if (preview_mode == "markdown") {
        text_widget->set_preview_mode(MainWidget::PreviewMode::Markdown);
    } else if (preview_mode == "html") {
        text_widget->set_preview_mode(MainWidget::PreviewMode::HTML);
    } else if (preview_mode == "gemtext") {
        text_widget->set_preview_mode(MainWidget::PreviewMode::Gemtext);
    } else if (preview_mode == "none") {
        text_widget->set_preview_mode(MainWidget::PreviewMode::None);
    } else {
        warnln("Invalid mode '{}'", preview_mode);
        return 1;
    }

    TRY(text_widget->initialize_menubar(*window));
    text_widget->update_title();

    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));

    if (!file_to_edit.is_empty()) {
        auto filename = TRY(String::from_utf8(file_to_edit));
        FileArgument parsed_argument(filename);

        FileSystemAccessClient::Client::the().set_silence_errors(FileSystemAccessClient::ErrorFlag::NoEntries);
        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(window, parsed_argument.filename().to_byte_string());

        if (response.is_error()) {
            if (response.error().code() == ENOENT)
                text_widget->open_nonexistent_file(parsed_argument.filename().to_byte_string());
        } else {
            TRY(text_widget->read_file(response.value().filename(), response.value().stream()));
            text_widget->editor().set_cursor_and_focus_line(parsed_argument.line().value_or(1) - 1, parsed_argument.column().value_or(0));
        }

        text_widget->update_title();
        FileSystemAccessClient::Client::the().set_silence_errors(FileSystemAccessClient::ErrorFlag::None);
    }
    text_widget->update_statusbar();

    return app->exec();
}
