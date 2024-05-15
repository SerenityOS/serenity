/*
 * Copyright (c) 2020-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Ali Chraghi <chraghiali1@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibCoredump/Backtrace.h>
#include <LibCoredump/Reader.h>
#include <LibDesktop/AppFile.h>
#include <LibDesktop/Launcher.h>
#include <LibELF/Core.h>
#include <LibFileSystem/FileSystem.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/LinkLabel.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Process.h>
#include <LibGUI/Progressbar.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <LibThreading/BackgroundAction.h>
#include <LibURL/URL.h>
#include <mallocdefs.h>
#include <serenity.h>
#include <spawn.h>
#include <string.h>
#include <unistd.h>

struct TitleAndText {
    ByteString title;
    ByteString text;
};

struct ThreadBacktracesAndCpuRegisters {
    Vector<TitleAndText> thread_backtraces;
    Vector<TitleAndText> thread_cpu_registers;
};

static TitleAndText build_backtrace(Coredump::Reader const& coredump, ELF::Core::ThreadInfo const& thread_info, size_t thread_index, Function<void(size_t, size_t)> on_progress)
{
    auto timer = Core::ElapsedTimer::start_new();
    Coredump::Backtrace backtrace(coredump, thread_info, move(on_progress));

    auto metadata = coredump.metadata();

    dbgln("Generating backtrace took {} ms", timer.elapsed());

    StringBuilder builder;

    auto prepend_metadata = [&](auto& key, StringView fmt) {
        auto maybe_value = metadata.get(key);
        if (!maybe_value.has_value() || maybe_value.value().is_empty())
            return;
        builder.appendff(fmt, maybe_value.value());
        builder.append('\n');
        builder.append('\n');
    };

    if (metadata.contains("assertion"))
        prepend_metadata("assertion", "ASSERTION FAILED: {}"sv);
    else if (metadata.contains("pledge_violation"))
        prepend_metadata("pledge_violation", "Has not pledged {}"sv);

    auto fault_address = metadata.get("fault_address");
    auto fault_type = metadata.get("fault_type");
    auto fault_access = metadata.get("fault_access");
    if (fault_address.has_value() && fault_access.has_value()) {
        builder.appendff("{} fault on {} at address {}", fault_type.value_or("Page"), fault_access.value(), fault_address.value());
        constexpr FlatPtr malloc_scrub_pattern = explode_byte(MALLOC_SCRUB_BYTE);
        constexpr FlatPtr free_scrub_pattern = explode_byte(FREE_SCRUB_BYTE);
        auto raw_fault_address = AK::StringUtils::convert_to_uint_from_hex(fault_address.value().substring_view(2));
        if (raw_fault_address.has_value() && (raw_fault_address.value() & 0xffff0000) == (malloc_scrub_pattern & 0xffff0000)) {
            builder.append(", looks like it may be uninitialized malloc() memory\n"sv);
            dbgln("NOTE: Address {:p} looks like it may be uninitialized malloc() memory\n", raw_fault_address.value());
        } else if (raw_fault_address.has_value() && (raw_fault_address.value() & 0xffff0000) == (free_scrub_pattern & 0xffff0000)) {
            builder.append(", looks like it may be recently free()'d memory\n"sv);
            dbgln("NOTE: Address {:p} looks like it may be recently free()'d memory\n", raw_fault_address.value());
        } else {
            builder.append("\n"sv);
        }
        builder.append("\n"sv);
    }

    auto first_entry = true;
    for (auto& entry : backtrace.entries()) {
        if (first_entry)
            first_entry = false;
        else
            builder.append('\n');
        builder.append(entry.to_byte_string());
    }

    dbgln("--- Backtrace for thread #{} (TID {}) ---", thread_index, thread_info.tid);
    for (auto& entry : backtrace.entries()) {
        dbgln("{}", entry.to_byte_string(true));
    }

    return {
        ByteString::formatted("Thread #{} (TID {})", thread_index, thread_info.tid),
        builder.to_byte_string()
    };
}

static TitleAndText build_cpu_registers(const ELF::Core::ThreadInfo& thread_info, size_t thread_index)
{
    auto& regs = thread_info.regs;

    StringBuilder builder;

#if ARCH(X86_64)
    builder.appendff("rax={:p} rbx={:p} rcx={:p} rdx={:p}\n", regs.rax, regs.rbx, regs.rcx, regs.rdx);
    builder.appendff("rbp={:p} rsp={:p} rsi={:p} rdi={:p}\n", regs.rbp, regs.rsp, regs.rsi, regs.rdi);
    builder.appendff(" r8={:p}  r9={:p} r10={:p} r11={:p}\n", regs.r8, regs.r9, regs.r10, regs.r11);
    builder.appendff("r12={:p} r13={:p} r14={:p} r15={:p}\n", regs.r12, regs.r13, regs.r14, regs.r15);
    builder.appendff("rip={:p} rflags={:p}", regs.rip, regs.rflags);
#elif ARCH(AARCH64)
    builder.appendff("Stack pointer   sp={:p}\n", regs.sp);
    builder.appendff("Program counter pc={:p}\n", regs.pc);
    builder.appendff(" x0={:p}  x1={:p}  x2={:p}  x3={:p}  x4={:p}\n", regs.x[0], regs.x[1], regs.x[2], regs.x[3], regs.x[4]);
    builder.appendff(" x5={:p}  x6={:p}  x7={:p}  x8={:p}  x9={:p}\n", regs.x[5], regs.x[6], regs.x[7], regs.x[8], regs.x[9]);
    builder.appendff("x10={:p} x11={:p} x12={:p} x13={:p} x14={:p}\n", regs.x[10], regs.x[11], regs.x[12], regs.x[13], regs.x[14]);
    builder.appendff("x15={:p} x16={:p} x17={:p} x18={:p} x19={:p}\n", regs.x[15], regs.x[16], regs.x[17], regs.x[18], regs.x[19]);
    builder.appendff("x20={:p} x21={:p} x22={:p} x23={:p} x24={:p}\n", regs.x[20], regs.x[21], regs.x[22], regs.x[23], regs.x[24]);
    builder.appendff("x25={:p} x26={:p} x27={:p} x28={:p} x29={:p}\n", regs.x[25], regs.x[26], regs.x[27], regs.x[28], regs.x[29]);
    builder.appendff("x30={:p}", regs.x[30]);
#elif ARCH(RISCV64)
    builder.appendff("Program counter pc={:p}\n", regs.pc);
    builder.appendff("ra={:p} sp={:p} gp={:p} tp={:p} fp={:p}\n", regs.x[0], regs.x[1], regs.x[2], regs.x[3], regs.x[7]);
    builder.appendff("a0={:p} a1={:p} a2={:p} a3={:p} a4={:p} a5={:p} a6={:p} a7={:p}\n", regs.x[9], regs.x[10], regs.x[11], regs.x[12], regs.x[13], regs.x[14], regs.x[15], regs.x[16]);
    builder.appendff("t0={:p} t1={:p} t2={:p} t3={:p} t4={:p} t5={:p} t6={:p}\n", regs.x[4], regs.x[5], regs.x[6], regs.x[27], regs.x[28], regs.x[29], regs.x[30]);
    builder.appendff("s1={:p} s2={:p} s3={:p} s4={:p} s5={:p} s6={:p} s7={:p} s8={:p} s9={:p} s10={:p} s11={:p}\n", regs.x[8], regs.x[17], regs.x[18], regs.x[19], regs.x[20], regs.x[21], regs.x[22], regs.x[23], regs.x[24], regs.x[25], regs.x[26]);
#else
#    error Unknown architecture
#endif

    return {
        ByteString::formatted("Thread #{} (TID {})", thread_index, thread_info.tid),
        builder.to_byte_string()
    };
}

static void unlink_coredump(StringView coredump_path)
{
    if (FileSystem::remove(coredump_path, FileSystem::RecursionMode::Disallowed).is_error())
        dbgln("Failed deleting coredump file");
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd cpath rpath unix proc exec thread"));

    auto app = TRY(GUI::Application::create(arguments));

    ByteString coredump_path {};
    bool unlink_on_exit = false;
    StringBuilder full_backtrace;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Show information from an application crash coredump.");
    args_parser.add_positional_argument(coredump_path, "Coredump path", "coredump-path");
    args_parser.add_option(unlink_on_exit, "Delete the coredump after its parsed", "unlink");
    args_parser.parse(arguments);

    auto coredump = Coredump::Reader::create(coredump_path);
    if (!coredump) {
        warnln("Could not open coredump '{}'", coredump_path);
        return 1;
    }

    Vector<ByteString> memory_regions;
    coredump->for_each_memory_region_info([&](auto& memory_region_info) {
        memory_regions.append(ByteString::formatted("{:p} - {:p}: {}", memory_region_info.region_start, memory_region_info.region_end, memory_region_info.region_name));
        return IterationDecision::Continue;
    });

    auto executable_path = coredump->process_executable_path();
    auto crashed_process_arguments = coredump->process_arguments();
    auto environment = coredump->process_environment();
    auto pid = coredump->process_pid();
    auto termination_signal = coredump->process_termination_signal();

    auto app_icon = GUI::Icon::default_icon("app-crash-reporter"sv);

    auto window = GUI::Window::construct();
    window->set_title("Crash Reporter");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(460, 190);
    window->center_on_screen();
    window->on_close = [unlink_on_exit, &coredump_path]() {
        if (unlink_on_exit)
            unlink_coredump(coredump_path);
    };

    auto widget = TRY(CrashReporter::MainWidget::try_create());
    window->set_main_widget(widget);

    auto& icon_image_widget = *widget->find_descendant_of_type_named<GUI::ImageWidget>("icon");
    icon_image_widget.set_bitmap(GUI::FileIconProvider::icon_for_executable(executable_path).bitmap_for_size(32));

    auto app_name = LexicalPath::basename(executable_path);
    auto af = Desktop::AppFile::get_for_app(app_name);
    if (af->is_valid())
        app_name = af->name();

    auto& description_label = *widget->find_descendant_of_type_named<GUI::Label>("description");
    description_label.set_text(TRY(String::formatted("\"{}\" (PID {}) has crashed - {} (signal {})", app_name, pid, strsignal(termination_signal), termination_signal)));

    auto& executable_link_label = *widget->find_descendant_of_type_named<GUI::LinkLabel>("executable_link");
    executable_link_label.set_text(TRY(String::from_byte_string(LexicalPath::canonicalized_path(executable_path))));
    executable_link_label.on_click = [&] {
        LexicalPath path { executable_path };
        Desktop::Launcher::open(URL::create_with_file_scheme(path.dirname(), path.basename()));
    };

    auto& coredump_link_label = *widget->find_descendant_of_type_named<GUI::LinkLabel>("coredump_link");
    coredump_link_label.set_text(TRY(String::from_byte_string(LexicalPath::canonicalized_path(coredump_path))));
    coredump_link_label.on_click = [&] {
        LexicalPath path { coredump_path };
        Desktop::Launcher::open(URL::create_with_file_scheme(path.dirname(), path.basename()));
    };

    auto& arguments_label = *widget->find_descendant_of_type_named<GUI::Label>("arguments_label");
    arguments_label.set_text(TRY(String::join(' ', crashed_process_arguments)));

    auto& progressbar = *widget->find_descendant_of_type_named<GUI::Progressbar>("progressbar");
    auto& tab_widget = *widget->find_descendant_of_type_named<GUI::TabWidget>("tab_widget");

    auto& backtrace_tab = tab_widget.add_tab<GUI::Widget>("Backtrace"_string);
    backtrace_tab.set_layout<GUI::VerticalBoxLayout>(4);

    auto& backtrace_label = backtrace_tab.add<GUI::Label>("A backtrace for each thread alive during the crash is listed below:"_string);
    backtrace_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    backtrace_label.set_fixed_height(16);

    auto& backtrace_tab_widget = backtrace_tab.add<GUI::TabWidget>();
    backtrace_tab_widget.set_tab_position(TabPosition::Bottom);

    auto& cpu_registers_tab = tab_widget.add_tab<GUI::Widget>("CPU Registers"_string);
    cpu_registers_tab.set_layout<GUI::VerticalBoxLayout>(4);

    auto& cpu_registers_label = cpu_registers_tab.add<GUI::Label>("The CPU register state for each thread alive during the crash is listed below:"_string);
    cpu_registers_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    cpu_registers_label.set_fixed_height(16);

    auto& cpu_registers_tab_widget = cpu_registers_tab.add<GUI::TabWidget>();
    cpu_registers_tab_widget.set_tab_position(TabPosition::Bottom);

    auto& environment_tab = tab_widget.add_tab<GUI::Widget>("Environment"_string);
    environment_tab.set_layout<GUI::VerticalBoxLayout>(4);

    auto& environment_text_editor = environment_tab.add<GUI::TextEditor>();
    environment_text_editor.set_text(ByteString::join('\n', environment));
    environment_text_editor.set_mode(GUI::TextEditor::Mode::ReadOnly);
    environment_text_editor.set_wrapping_mode(GUI::TextEditor::WrappingMode::NoWrap);
    environment_text_editor.set_should_hide_unnecessary_scrollbars(true);

    auto& memory_regions_tab = tab_widget.add_tab<GUI::Widget>("Memory Regions"_string);
    memory_regions_tab.set_layout<GUI::VerticalBoxLayout>(4);

    auto& memory_regions_text_editor = memory_regions_tab.add<GUI::TextEditor>();
    memory_regions_text_editor.set_text(ByteString::join('\n', memory_regions));
    memory_regions_text_editor.set_mode(GUI::TextEditor::Mode::ReadOnly);
    memory_regions_text_editor.set_wrapping_mode(GUI::TextEditor::WrappingMode::NoWrap);
    memory_regions_text_editor.set_should_hide_unnecessary_scrollbars(true);
    memory_regions_text_editor.set_visualize_trailing_whitespace(false);

    auto& close_button = *widget->find_descendant_of_type_named<GUI::Button>("close_button");
    close_button.on_click = [&](auto) {
        window->close();
    };
    close_button.set_focus(true);

    auto& debug_button = *widget->find_descendant_of_type_named<GUI::Button>("debug_button");
    debug_button.set_icon(TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-hack-studio.png"sv)));
    debug_button.on_click = [&](int) {
        GUI::Process::spawn_or_show_error(window, "/bin/HackStudio"sv, Array { "-c", coredump_path.characters() });
    };

    auto& save_backtrace_button = *widget->find_descendant_of_type_named<GUI::Button>("save_backtrace_button");
    save_backtrace_button.set_icon(TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/save.png"sv)));
    save_backtrace_button.on_click = [&](auto) {
        LexicalPath lexical_path(ByteString::formatted("{}_{}_backtrace.txt", pid, app_name));
        auto file_or_error = FileSystemAccessClient::Client::the().save_file(window, lexical_path.title(), lexical_path.extension());
        if (file_or_error.is_error()) {
            GUI::MessageBox::show(window, ByteString::formatted("Communication failed with FileSystemAccessServer: {}.", file_or_error.release_error()), "Saving backtrace failed"sv, GUI::MessageBox::Type::Error);
            return;
        }
        auto file = file_or_error.release_value().release_stream();

        auto byte_buffer_or_error = full_backtrace.to_byte_buffer();
        if (byte_buffer_or_error.is_error()) {
            GUI::MessageBox::show(window, ByteString::formatted("Couldn't create backtrace: {}.", byte_buffer_or_error.release_error()), "Saving backtrace failed"sv, GUI::MessageBox::Type::Error);
            return;
        }
        auto byte_buffer = byte_buffer_or_error.release_value();

        if (auto result = file->write_until_depleted(byte_buffer); result.is_error())
            GUI::MessageBox::show(window, ByteString::formatted("Couldn't save file: {}.", result.release_error()), "Saving backtrace failed"sv, GUI::MessageBox::Type::Error);
    };
    save_backtrace_button.set_enabled(false);

    (void)Threading::BackgroundAction<ThreadBacktracesAndCpuRegisters>::construct(
        [&, window = window->make_weak_ptr<GUI::Window>(), coredump = move(coredump)](auto&) {
            ThreadBacktracesAndCpuRegisters results;
            size_t thread_index = 0;
            coredump->for_each_thread_info([&](auto& thread_info) {
                results.thread_backtraces.append(build_backtrace(*coredump, thread_info, thread_index, [&](size_t frame_index, size_t frame_count) {
                    app->event_loop().deferred_invoke([&, frame_index, frame_count] {
                        if (auto strong_window = window.strong_ref(); strong_window && strong_window->is_visible()) {
                            strong_window->set_progress(100.0f * (float)(frame_index + 1) / (float)frame_count);
                            progressbar.set_value(frame_index + 1);
                            progressbar.set_max(frame_count);
                        }
                    });
                }));
                results.thread_cpu_registers.append(build_cpu_registers(thread_info, thread_index));
                ++thread_index;
                return IterationDecision::Continue;
            });
            return results;
        },
        [&](auto results) -> ErrorOr<void> {
            for (auto& backtrace : results.thread_backtraces) {
                auto& container = backtrace_tab_widget.add_tab<GUI::Widget>(TRY(String::from_byte_string(backtrace.title)));
                container.template set_layout<GUI::VerticalBoxLayout>(4);
                auto& backtrace_text_editor = container.template add<GUI::TextEditor>();
                backtrace_text_editor.set_text(backtrace.text);
                backtrace_text_editor.set_mode(GUI::TextEditor::Mode::ReadOnly);
                backtrace_text_editor.set_wrapping_mode(GUI::TextEditor::WrappingMode::NoWrap);
                backtrace_text_editor.set_should_hide_unnecessary_scrollbars(true);
                TRY(full_backtrace.try_appendff("==== {} ====\n{}\n", backtrace.title, backtrace.text));
            }

            for (auto& cpu_registers : results.thread_cpu_registers) {
                auto& container = cpu_registers_tab_widget.add_tab<GUI::Widget>(TRY(String::from_byte_string(cpu_registers.title)));
                container.template set_layout<GUI::VerticalBoxLayout>(4);
                auto& cpu_registers_text_editor = container.template add<GUI::TextEditor>();
                cpu_registers_text_editor.set_text(cpu_registers.text);
                cpu_registers_text_editor.set_mode(GUI::TextEditor::Mode::ReadOnly);
                cpu_registers_text_editor.set_wrapping_mode(GUI::TextEditor::WrappingMode::NoWrap);
                cpu_registers_text_editor.set_should_hide_unnecessary_scrollbars(true);
            }

            progressbar.set_visible(false);
            tab_widget.set_visible(true);
            save_backtrace_button.set_enabled(true);
            window->resize(window->width(), max(340, window->height()));
            window->set_progress(0);
            return {};
        },
        [window](Error error) {
            dbgln("Error while parsing the coredump: {}", error);
            window->close();
        });

    window->show();

    return app->exec();
}
