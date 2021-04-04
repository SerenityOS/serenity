/*
 * Copyright (c) 2020-2021, Linus Groh <mail@linusgroh.de>
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
#include <LibCore/File.h>
#include <LibCoreDump/Backtrace.h>
#include <LibCoreDump/Reader.h>
#include <LibDesktop/AppFile.h>
#include <LibDesktop/Launcher.h>
#include <LibELF/CoreDump.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/LinkLabel.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <string.h>
#include <unistd.h>

struct TitleAndText {
    String title;
    String text;
};

static TitleAndText build_backtrace(const CoreDump::Reader& coredump, const ELF::Core::ThreadInfo& thread_info, size_t thread_index)
{
    CoreDump::Backtrace backtrace(coredump, thread_info);
    auto metadata = coredump.metadata();

    StringBuilder builder;

    auto prepend_metadata = [&](auto& key, StringView fmt) {
        auto maybe_value = metadata.get(key);
        if (!maybe_value.has_value() || maybe_value.value().is_empty())
            return;
        builder.appendff(fmt, maybe_value.value());
        builder.append('\n');
        builder.append('\n');
    };

    auto& backtrace_entries = backtrace.entries();

    if (metadata.contains("assertion"))
        prepend_metadata("assertion", "ASSERTION FAILED: {}");
    else if (metadata.contains("pledge_violation"))
        prepend_metadata("pledge_violation", "Has not pledged {}");

    auto first_entry = true;
    for (auto& entry : backtrace.entries()) {
        if (first_entry)
            first_entry = false;
        else
            builder.append('\n');
        builder.append(entry.to_string());
    }

    return {
        String::formatted("Thread #{} (TID {})", thread_index, thread_info.tid),
        builder.build()
    };
}

static TitleAndText build_cpu_registers(const ELF::Core::ThreadInfo& thread_info, size_t thread_index)
{
    auto& regs = thread_info.regs;

    StringBuilder builder;

    builder.appendff("eax={:08x} ebx={:08x} ecx={:08x} edx={:08x}", regs.eax, regs.ebx, regs.ecx, regs.edx);
    builder.append('\n');
    builder.appendff("ebp={:08x} esp={:08x} esi={:08x} edi={:08x}", regs.ebp, regs.esp, regs.esi, regs.edi);
    builder.append('\n');
    builder.appendff("eip={:08x} eflags={:08x}", regs.eip, regs.eflags);

    return {
        String::formatted("Thread #{} (TID {})", thread_index, thread_info.tid),
        builder.build()
    };
}

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd accept cpath rpath unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* coredump_path = nullptr;
    bool unlink_after_use = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Show information from an application crash coredump.");
    args_parser.add_positional_argument(coredump_path, "Coredump path", "coredump-path");
    args_parser.add_option(unlink_after_use, "Delete the coredump after its parsed", "unlink", 0);
    args_parser.parse(argc, argv);

    Vector<TitleAndText> thread_backtraces;
    Vector<TitleAndText> thread_cpu_registers;

    String executable_path;
    Vector<String> arguments;
    Vector<String> environment;
    int pid { 0 };
    u8 termination_signal { 0 };

    {
        auto coredump = CoreDump::Reader::create(coredump_path);
        if (!coredump) {
            warnln("Could not open coredump '{}'", coredump_path);
            return 1;
        }

        size_t thread_index = 0;
        coredump->for_each_thread_info([&](auto& thread_info) {
            thread_backtraces.append(build_backtrace(*coredump, thread_info, thread_index));
            thread_cpu_registers.append(build_cpu_registers(thread_info, thread_index));
            ++thread_index;
            return IterationDecision::Continue;
        });

        executable_path = coredump->process_executable_path();
        arguments = coredump->process_arguments();
        environment = coredump->process_environment();
        pid = coredump->process_pid();
        termination_signal = coredump->process_termination_signal();
    }

    if (unlink_after_use) {
        if (Core::File::remove(coredump_path, Core::File::RecursionMode::Disallowed, false).is_error())
            dbgln("Failed deleting coredump file");
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd accept rpath unix", nullptr) < 0) {
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
    description_label.set_text(String::formatted("\"{}\" (PID {}) has crashed - {} (signal {})", app_name, pid, strsignal(termination_signal), termination_signal));

    auto& executable_link_label = *widget.find_descendant_of_type_named<GUI::LinkLabel>("executable_link");
    executable_link_label.set_text(LexicalPath::canonicalized_path(executable_path));
    executable_link_label.on_click = [&] {
        LexicalPath path { executable_path };
        Desktop::Launcher::open(URL::create_with_file_protocol(path.dirname(), path.basename()));
    };

    auto& coredump_link_label = *widget.find_descendant_of_type_named<GUI::LinkLabel>("coredump_link");
    coredump_link_label.set_text(LexicalPath::canonicalized_path(coredump_path));
    coredump_link_label.on_click = [&] {
        LexicalPath path { coredump_path };
        Desktop::Launcher::open(URL::create_with_file_protocol(path.dirname(), path.basename()));
    };

    auto& arguments_label = *widget.find_descendant_of_type_named<GUI::Label>("arguments_label");
    arguments_label.set_text(String::join(" ", arguments));

    auto& tab_widget = *widget.find_descendant_of_type_named<GUI::TabWidget>("tab_widget");

    auto& backtrace_tab = tab_widget.add_tab<GUI::Widget>("Backtrace");
    backtrace_tab.set_layout<GUI::VerticalBoxLayout>();
    backtrace_tab.layout()->set_margins({ 4, 4, 4, 4 });

    auto& backtrace_label = backtrace_tab.add<GUI::Label>("A backtrace for each thread alive during the crash is listed below:");
    backtrace_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    backtrace_label.set_fixed_height(16);

    auto& backtrace_tab_widget = backtrace_tab.add<GUI::TabWidget>();
    backtrace_tab_widget.set_tab_position(GUI::TabWidget::TabPosition::Bottom);
    for (auto& backtrace : thread_backtraces) {
        auto& backtrace_text_editor = backtrace_tab_widget.add_tab<GUI::TextEditor>(backtrace.title);
        backtrace_text_editor.set_layout<GUI::VerticalBoxLayout>();
        backtrace_text_editor.layout()->set_margins({ 4, 4, 4, 4 });
        backtrace_text_editor.set_text(backtrace.text);
        backtrace_text_editor.set_mode(GUI::TextEditor::Mode::ReadOnly);
        backtrace_text_editor.set_should_hide_unnecessary_scrollbars(true);
    }

    auto& cpu_registers_tab = tab_widget.add_tab<GUI::Widget>("CPU Registers");
    cpu_registers_tab.set_layout<GUI::VerticalBoxLayout>();
    cpu_registers_tab.layout()->set_margins({ 4, 4, 4, 4 });

    auto& cpu_registers_label = cpu_registers_tab.add<GUI::Label>("The CPU register state for each thread alive during the crash is listed below:");
    cpu_registers_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    cpu_registers_label.set_fixed_height(16);

    auto& cpu_registers_tab_widget = cpu_registers_tab.add<GUI::TabWidget>();
    cpu_registers_tab_widget.set_tab_position(GUI::TabWidget::TabPosition::Bottom);
    for (auto& cpu_registers : thread_cpu_registers) {
        auto& cpu_registers_text_editor = cpu_registers_tab_widget.add_tab<GUI::TextEditor>(cpu_registers.title);
        cpu_registers_text_editor.set_layout<GUI::VerticalBoxLayout>();
        cpu_registers_text_editor.layout()->set_margins({ 4, 4, 4, 4 });
        cpu_registers_text_editor.set_text(cpu_registers.text);
        cpu_registers_text_editor.set_mode(GUI::TextEditor::Mode::ReadOnly);
        cpu_registers_text_editor.set_should_hide_unnecessary_scrollbars(true);
    }

    auto& environment_tab = tab_widget.add_tab<GUI::Widget>("Environment");
    environment_tab.set_layout<GUI::VerticalBoxLayout>();
    environment_tab.layout()->set_margins({ 4, 4, 4, 4 });

    auto& environment_text_editor = environment_tab.add<GUI::TextEditor>();
    environment_text_editor.set_text(String::join("\n", environment));
    environment_text_editor.set_mode(GUI::TextEditor::Mode::ReadOnly);
    environment_text_editor.set_should_hide_unnecessary_scrollbars(true);

    auto& close_button = *widget.find_descendant_of_type_named<GUI::Button>("close_button");
    close_button.on_click = [&](auto) {
        app->quit();
    };

    window->show();

    return app->exec();
}
