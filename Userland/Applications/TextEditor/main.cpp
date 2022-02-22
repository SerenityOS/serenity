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

    auto app = TRY(GUI::Application::try_create(arguments));

    Config::pledge_domain("TextEditor");

    char const* preview_mode = "auto";
    char const* file_to_edit = nullptr;
    Core::ArgsParser parser;
    parser.add_option(preview_mode, "Preview mode, one of 'none', 'html', 'markdown', 'auto'", "preview-mode", '\0', "mode");
    parser.add_positional_argument(file_to_edit, "File to edit, with optional starting line and column number", "file[:line[:column]]", Core::ArgsParser::Required::No);
    parser.parse(arguments);

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/portal/launch", "rw"));
    TRY(Core::System::unveil("/tmp/portal/webcontent", "rw"));
    TRY(Core::System::unveil("/tmp/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    StringView preview_mode_view = preview_mode;

    auto app_icon = GUI::Icon::default_icon("app-text-editor");

    auto window = TRY(GUI::Window::try_create());
    window->resize(640, 400);

    auto text_widget = TRY(window->try_set_main_widget<MainWidget>());

    text_widget->editor().set_focus(true);

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (text_widget->request_close())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    if (preview_mode_view == "auto") {
        text_widget->set_auto_detect_preview_mode(true);
    } else if (preview_mode_view == "markdown") {
        text_widget->set_preview_mode(MainWidget::PreviewMode::Markdown);
    } else if (preview_mode_view == "html") {
        text_widget->set_preview_mode(MainWidget::PreviewMode::HTML);
    } else if (preview_mode_view == "none") {
        text_widget->set_preview_mode(MainWidget::PreviewMode::None);
    } else {
        warnln("Invalid mode '{}'", preview_mode);
        return 1;
    }

    text_widget->initialize_menubar(*window);

    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));

    if (file_to_edit) {
        FileArgument parsed_argument(file_to_edit);
        auto response = FileSystemAccessClient::Client::the().try_request_file_read_only_approved(window, parsed_argument.filename());

        if (response.is_error()) {
            if (response.error().code() == ENOENT)
                text_widget->open_nonexistent_file(parsed_argument.filename());
            else
                return 1;
        } else {
            if (!text_widget->read_file(*response.value()))
                return 1;
            text_widget->editor().set_cursor_and_focus_line(parsed_argument.line().value_or(1) - 1, parsed_argument.column().value_or(0));
        }
    }
    text_widget->update_title();
    text_widget->update_statusbar();

    return app->exec();
}
