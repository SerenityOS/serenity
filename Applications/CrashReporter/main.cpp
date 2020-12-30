/*
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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

#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <AK/URL.h>
#include <Applications/CrashReporter/CrashReporterWindowGML.h>
#include <LibCore/ArgsParser.h>
#include <LibCoreDump/Backtrace.h>
#include <LibCoreDump/Reader.h>
#include <LibDesktop/AppFile.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/Button.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/Layout.h>
#include <LibGUI/LinkLabel.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Window.h>

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer accept cpath rpath unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* coredump_path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Show information from an application crash coredump.");
    args_parser.add_positional_argument(coredump_path, "Coredump path", "coredump-path");
    args_parser.parse(argc, argv);

    Optional<CoreDump::Backtrace> backtrace;

    {
        auto coredump = CoreDump::Reader::create(coredump_path);
        if (!coredump) {
            warnln("Could not open coredump '{}'", coredump_path);
            return 1;
        }
        backtrace = coredump->backtrace();
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio shared_buffer accept rpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    String executable_path;
    // FIXME: Maybe we should just embed the process's executable path
    // in the coredump by itself so we don't have to extract it from the backtrace.
    // Such a process section could also include the PID, which currently we'd have
    // to parse from the filename.
    if (!backtrace.value().entries().is_empty()) {
        executable_path = backtrace.value().entries().last().object_name;
    } else {
        warnln("Could not determine executable path from coredump");
        return 1;
    }

    if (unveil(executable_path.characters(), "r") < 0) {
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

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-crash-reporter");

    auto window = GUI::Window::construct();
    window->set_title("Crash Reporter");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_resizable(false);
    window->resize(460, 340);
    window->center_on_screen();

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.load_from_gml(crash_reporter_window_gml);

    auto& icon_image_widget = static_cast<GUI::ImageWidget&>(*widget.find_descendant_by_name("icon"));
    icon_image_widget.set_bitmap(GUI::FileIconProvider::icon_for_executable(executable_path).bitmap_for_size(32));

    auto app_name = LexicalPath(executable_path).basename();
    auto af = Desktop::AppFile::get_for_app(app_name);
    if (af->is_valid())
        app_name = af->name();

    auto& description_label = static_cast<GUI::Label&>(*widget.find_descendant_by_name("description"));
    description_label.set_text(String::formatted("\"{}\" has crashed!", app_name));

    auto& executable_link_label = static_cast<GUI::LinkLabel&>(*widget.find_descendant_by_name("executable_link"));
    executable_link_label.set_text(LexicalPath::canonicalized_path(executable_path));
    executable_link_label.on_click = [&] {
        Desktop::Launcher::open(URL::create_with_file_protocol(LexicalPath(executable_path).dirname()));
    };

    auto& coredump_link_label = static_cast<GUI::LinkLabel&>(*widget.find_descendant_by_name("coredump_link"));
    coredump_link_label.set_text(LexicalPath::canonicalized_path(coredump_path));
    coredump_link_label.on_click = [&] {
        Desktop::Launcher::open(URL::create_with_file_protocol(LexicalPath(coredump_path).dirname()));
    };

    StringBuilder backtrace_builder;
    auto first = true;
    for (auto& entry : backtrace.value().entries()) {
        if (first)
            first = false;
        else
            backtrace_builder.append('\n');
        backtrace_builder.append(entry.to_string());
    }

    auto& backtrace_text_editor = static_cast<GUI::TextEditor&>(*widget.find_descendant_by_name("backtrace_text_editor"));
    backtrace_text_editor.set_text(backtrace_builder.build());

    auto& close_button = static_cast<GUI::Button&>(*widget.find_descendant_by_name("close_button"));
    close_button.on_click = [&](auto) {
        app->quit();
    };

    window->show();

    return app->exec();
}
