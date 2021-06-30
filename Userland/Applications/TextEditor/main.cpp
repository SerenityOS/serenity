/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileArgument.h"
#include "MainWidget.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/Menubar.h>
#include <stdio.h>
#include <unistd.h>

using namespace TextEditor;

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd thread rpath cpath wpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    const char* preview_mode = "auto";
    const char* file_to_edit = nullptr;
    Core::ArgsParser parser;
    parser.add_option(preview_mode, "Preview mode, one of 'none', 'html', 'markdown', 'auto'", "preview-mode", '\0', "mode");
    parser.add_positional_argument(file_to_edit, "File to edit, with optional starting line and column number", "file[:line[:column]]", Core::ArgsParser::Required::No);

    parser.parse(argc, argv);

    if (file_to_edit) {
        FileArgument parsed_argument(file_to_edit);

        auto path_to_unveil = Core::File::real_path_for(parsed_argument.filename());
        if (unveil(path_to_unveil.characters(), "rwc") < 0) {
            perror("unveil");
            return 1;
        }
    }

    if (unveil(Core::StandardPaths::config_directory().characters(), "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/launch", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/webcontent", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/filesystemaccess", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    StringView preview_mode_view = preview_mode;

    auto app_icon = GUI::Icon::default_icon("app-text-editor");

    auto window = GUI::Window::construct();
    window->resize(640, 400);

    auto& text_widget = window->set_main_widget<MainWidget>();

    text_widget.editor().set_focus(true);

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (text_widget.request_close())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    if (preview_mode_view == "auto") {
        text_widget.set_auto_detect_preview_mode(true);
    } else if (preview_mode_view == "markdown") {
        text_widget.set_preview_mode(MainWidget::PreviewMode::Markdown);
    } else if (preview_mode_view == "html") {
        text_widget.set_preview_mode(MainWidget::PreviewMode::HTML);
    } else if (preview_mode_view == "none") {
        text_widget.set_preview_mode(MainWidget::PreviewMode::None);
    } else {
        warnln("Invalid mode '{}'", preview_mode);
        return 1;
    }

    auto menubar = GUI::Menubar::construct();
    text_widget.initialize_menubar(menubar);
    window->set_menubar(menubar);

    if (file_to_edit) {
        // A file name was passed, parse any possible line and column numbers included.
        FileArgument parsed_argument(file_to_edit);
        auto absolute_path = Core::File::real_path_for(parsed_argument.filename());
        auto file = Core::File::open(absolute_path, Core::OpenMode::ReadWrite);

        if (file.is_error())
            return 1;

        if (!text_widget.read_file_and_close(file.value()->leak_fd(), absolute_path))
            return 1;

        text_widget.editor().set_cursor_and_focus_line(parsed_argument.line().value_or(1) - 1, parsed_argument.column().value_or(0));
    }
    text_widget.update_title();

    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));

    window->set_menubar(menubar);

    return app->exec();
}
