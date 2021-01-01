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
#include <LibELF/CoreDump.h>
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

static String build_backtrace(const CoreDump::Reader& coredump)
{
    StringBuilder builder;

    auto assertion = coredump.metadata().get("assertion");
    if (assertion.has_value() && !assertion.value().is_empty()) {
        builder.append("ASSERTION FAILED: ");
        builder.append(assertion.value().characters());
        builder.append('\n');
        builder.append('\n');
    }

    auto first = true;
    for (auto& entry : coredump.backtrace().entries()) {
        if (first)
            first = false;
        else
            builder.append('\n');
        builder.append(entry.to_string());
    }

    return builder.build();
}

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

    String backtrace;
    String executable_path;
    int pid { 0 };

    {
        auto coredump = CoreDump::Reader::create(coredump_path);
        if (!coredump) {
            warnln("Could not open coredump '{}'", coredump_path);
            return 1;
        }
        auto& process_info = coredump->process_info();
        backtrace = build_backtrace(*coredump);
        executable_path = String(process_info.executable_path);
        pid = process_info.pid;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio shared_buffer accept rpath unix", nullptr) < 0) {
        perror("pledge");
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

    auto& icon_image_widget = *widget.find_descendant_of_type_named<GUI::ImageWidget>("icon");
    icon_image_widget.set_bitmap(GUI::FileIconProvider::icon_for_executable(executable_path).bitmap_for_size(32));

    auto app_name = LexicalPath(executable_path).basename();
    auto af = Desktop::AppFile::get_for_app(app_name);
    if (af->is_valid())
        app_name = af->name();

    auto& description_label = *widget.find_descendant_of_type_named<GUI::Label>("description");
    description_label.set_text(String::formatted("\"{}\" (PID {}) has crashed!", app_name, pid));

    auto& executable_link_label = *widget.find_descendant_of_type_named<GUI::LinkLabel>("executable_link");
    executable_link_label.set_text(LexicalPath::canonicalized_path(executable_path));
    executable_link_label.on_click = [&] {
        Desktop::Launcher::open(URL::create_with_file_protocol(LexicalPath(executable_path).dirname()));
    };

    auto& coredump_link_label = *widget.find_descendant_of_type_named<GUI::LinkLabel>("coredump_link");
    coredump_link_label.set_text(LexicalPath::canonicalized_path(coredump_path));
    coredump_link_label.on_click = [&] {
        Desktop::Launcher::open(URL::create_with_file_protocol(LexicalPath(coredump_path).dirname()));
    };

    auto& backtrace_text_editor = *widget.find_descendant_of_type_named<GUI::TextEditor>("backtrace_text_editor");
    backtrace_text_editor.set_text(backtrace);

    auto& close_button = *widget.find_descendant_of_type_named<GUI::Button>("close_button");
    close_button.on_click = [&](auto) {
        app->quit();
    };

    window->show();

    return app->exec();
}
