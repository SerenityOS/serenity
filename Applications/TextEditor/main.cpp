/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "TextEditorWidget.h"
#include <LibCore/ArgsParser.h>
#include <LibGfx/Bitmap.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio thread rpath accept cpath wpath shared_buffer unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio thread rpath accept cpath wpath shared_buffer unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* preview_mode = "auto";
    const char* file_to_edit = nullptr;
    Core::ArgsParser parser;
    parser.add_option(preview_mode, "Preview mode, one of 'none', 'html', 'markdown', 'auto'", "preview-mode", '\0', "mode");
    parser.add_positional_argument(file_to_edit, "File to edit", "file", Core::ArgsParser::Required::No);

    parser.parse(argc, argv);

    StringView preview_mode_view = preview_mode;

    auto window = GUI::Window::construct();
    window->set_title("Text Editor");
    window->resize(640, 400);

    auto& text_widget = window->set_main_widget<TextEditorWidget>();

    text_widget.editor().set_focus(true);

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (text_widget.request_close())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    if (preview_mode_view == "auto") {
        text_widget.set_auto_detect_preview_mode(true);
    } else if (preview_mode_view == "markdown") {
        text_widget.set_preview_mode(TextEditorWidget::PreviewMode::Markdown);
    } else if (preview_mode_view == "html") {
        text_widget.set_preview_mode(TextEditorWidget::PreviewMode::HTML);
    } else if (preview_mode_view == "none") {
        text_widget.set_preview_mode(TextEditorWidget::PreviewMode::None);
    } else {
        warnln("Invalid mode '{}'", preview_mode);
        return 1;
    }

    if (file_to_edit)
        text_widget.open_sesame(file_to_edit);

    window->show();
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-text-editor.png"));

    return app->exec();
}
