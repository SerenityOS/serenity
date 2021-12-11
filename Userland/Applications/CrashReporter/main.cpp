/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <AK/URL.h>
#include <Applications/CrashReporter/CrashReporterWindowGML.h>
#include <LibC/serenity.h>
#include <LibC/spawn.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibCoredump/Backtrace.h>
#include <LibCoredump/Reader.h>
#include <LibDesktop/AppFile.h>
#include <LibDesktop/Launcher.h>
#include <LibELF/Core.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Icon.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/LinkLabel.h>
#include <LibGUI/Progressbar.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <string.h>
#include <unistd.h>

struct TitleAndText {
    String title;
    String text;
};

static NonnullRefPtr<GUI::Window> create_progress_window()
{
    auto window = GUI::Window::construct();

    window->set_title("CrashReporter");
    window->set_resizable(false);
    window->resize(240, 64);
    window->center_on_screen();

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();

    auto& label = main_widget.add<GUI::Label>("Generating crash report...");
    label.set_fixed_height(30);

    auto& progressbar = main_widget.add<GUI::Progressbar>();
    progressbar.set_name("progressbar");
    progressbar.set_fixed_width(150);
    progressbar.set_fixed_height(22);

    window->on_close = [&]() {
        if (progressbar.value() != progressbar.max())
            exit(0);
    };

    return window;
}

static TitleAndText build_backtrace(Coredump::Reader const& coredump, ELF::Core::ThreadInfo const& thread_info, size_t thread_index)
{
    // Show a very simple progress window ASAP to make crashing feel more responsive.
    // FIXME: This is not the most beautifully factored thing.
    auto progress_window = create_progress_window();
    progress_window->show();

    auto& progressbar = *progress_window->main_widget()->find_descendant_of_type_named<GUI::Progressbar>("progressbar");

    auto timer = Core::ElapsedTimer::start_new();
    Coredump::Backtrace backtrace(coredump, thread_info, [&](size_t frame_index, size_t frame_count) {
        progress_window->set_progress(100.0f * (float)(frame_index + 1) / (float)frame_count);
        progressbar.set_value(frame_index + 1);
        progressbar.set_max(frame_count);
        Core::EventLoop::current().pump(Core::EventLoop::WaitMode::PollForEvents);
    });
    progress_window->close();

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
        prepend_metadata("assertion", "ASSERTION FAILED: {}");
    else if (metadata.contains("pledge_violation"))
        prepend_metadata("pledge_violation", "Has not pledged {}");

    auto fault_address = metadata.get("fault_address");
    auto fault_type = metadata.get("fault_type");
    auto fault_access = metadata.get("fault_access");
    if (fault_address.has_value() && fault_type.has_value() && fault_access.has_value()) {
        builder.appendff("{} fault on {} at address {}\n\n", fault_type.value(), fault_access.value(), fault_address.value());
    }

    auto first_entry = true;
    for (auto& entry : backtrace.entries()) {
        if (first_entry)
            first_entry = false;
        else
            builder.append('\n');
        builder.append(entry.to_string());
    }

    dbgln("--- Backtrace for thread #{} (TID {}) ---", thread_index, thread_info.tid);
    for (auto& entry : backtrace.entries()) {
        dbgln("{}", entry.to_string(true));
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

#if ARCH(I386)
    builder.appendff("eax={:p} ebx={:p} ecx={:p} edx={:p}\n", regs.eax, regs.ebx, regs.ecx, regs.edx);
    builder.appendff("ebp={:p} esp={:p} esi={:p} edi={:p}\n", regs.ebp, regs.esp, regs.esi, regs.edi);
    builder.appendff("eip={:p} eflags={:p}", regs.eip, regs.eflags);
#else
    builder.appendff("rax={:p} rbx={:p} rcx={:p} rdx={:p}\n", regs.rax, regs.rbx, regs.rcx, regs.rdx);
    builder.appendff("rbp={:p} rsp={:p} rsi={:p} rdi={:p}\n", regs.rbp, regs.rsp, regs.rsi, regs.rdi);
    builder.appendff(" r8={:p}  r9={:p} r10={:p} r11={:p}\n", regs.r8, regs.r9, regs.r10, regs.r11);
    builder.appendff("r12={:p} r13={:p} r14={:p} r15={:p}\n", regs.r12, regs.r13, regs.r14, regs.r15);
    builder.appendff("rip={:p} rflags={:p}", regs.rip, regs.rflags);
#endif

    return {
        String::formatted("Thread #{} (TID {})", thread_index, thread_info.tid),
        builder.build()
    };
}

static void unlink_coredump(StringView const& coredump_path)
{
    if (Core::File::remove(coredump_path, Core::File::RecursionMode::Disallowed, false).is_error())
        dbgln("Failed deleting coredump file");
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd cpath rpath unix proc exec"));

    auto app = TRY(GUI::Application::try_create(arguments));

    const char* coredump_path = nullptr;
    bool unlink_on_exit = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Show information from an application crash coredump.");
    args_parser.add_positional_argument(coredump_path, "Coredump path", "coredump-path");
    args_parser.add_option(unlink_on_exit, "Delete the coredump after its parsed", "unlink", 0);
    args_parser.parse(arguments);

    Vector<TitleAndText> thread_backtraces;
    Vector<TitleAndText> thread_cpu_registers;

    String executable_path;
    Vector<String> crashed_process_arguments;
    Vector<String> environment;
    Vector<String> memory_regions;
    int pid { 0 };
    u8 termination_signal { 0 };

    {
        auto coredump = Coredump::Reader::create(coredump_path);
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

        coredump->for_each_memory_region_info([&](auto& memory_region_info) {
            memory_regions.append(String::formatted("{:p} - {:p}: {}", memory_region_info.region_start, memory_region_info.region_end, (const char*)memory_region_info.region_name));
            return IterationDecision::Continue;
        });

        executable_path = coredump->process_executable_path();
        crashed_process_arguments = coredump->process_arguments();
        environment = coredump->process_environment();
        pid = coredump->process_pid();
        termination_signal = coredump->process_termination_signal();
    }

    TRY(Core::System::unveil(executable_path.characters(), "r"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/portal/launch", "rw"));

    if (unlink_on_exit)
        TRY(Core::System::unveil(coredump_path, "c"));

    TRY(Core::System::unveil("/bin/HackStudio", "rx"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-crash-reporter");

    auto window = TRY(GUI::Window::try_create());
    window->set_title("Crash Reporter");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(460, 340);
    window->center_on_screen();
    window->on_close = [unlink_on_exit, &coredump_path]() {
        if (unlink_on_exit)
            unlink_coredump(coredump_path);
    };

    auto widget = TRY(window->try_set_main_widget<GUI::Widget>());
    widget->load_from_gml(crash_reporter_window_gml);

    auto& icon_image_widget = *widget->find_descendant_of_type_named<GUI::ImageWidget>("icon");
    icon_image_widget.set_bitmap(GUI::FileIconProvider::icon_for_executable(executable_path).bitmap_for_size(32));

    auto app_name = LexicalPath::basename(executable_path);
    auto af = Desktop::AppFile::get_for_app(app_name);
    if (af->is_valid())
        app_name = af->name();

    auto& description_label = *widget->find_descendant_of_type_named<GUI::Label>("description");
    description_label.set_text(String::formatted("\"{}\" (PID {}) has crashed - {} (signal {})", app_name, pid, strsignal(termination_signal), termination_signal));

    auto& executable_link_label = *widget->find_descendant_of_type_named<GUI::LinkLabel>("executable_link");
    executable_link_label.set_text(LexicalPath::canonicalized_path(executable_path));
    executable_link_label.on_click = [&] {
        LexicalPath path { executable_path };
        Desktop::Launcher::open(URL::create_with_file_protocol(path.dirname(), path.basename()));
    };

    auto& coredump_link_label = *widget->find_descendant_of_type_named<GUI::LinkLabel>("coredump_link");
    coredump_link_label.set_text(LexicalPath::canonicalized_path(coredump_path));
    coredump_link_label.on_click = [&] {
        LexicalPath path { coredump_path };
        Desktop::Launcher::open(URL::create_with_file_protocol(path.dirname(), path.basename()));
    };

    auto& arguments_label = *widget->find_descendant_of_type_named<GUI::Label>("arguments_label");
    arguments_label.set_text(String::join(" ", crashed_process_arguments));

    auto& tab_widget = *widget->find_descendant_of_type_named<GUI::TabWidget>("tab_widget");

    auto backtrace_tab = TRY(tab_widget.try_add_tab<GUI::Widget>("Backtrace"));
    (void)TRY(backtrace_tab->try_set_layout<GUI::VerticalBoxLayout>());
    backtrace_tab->layout()->set_margins(4);

    auto backtrace_label = TRY(backtrace_tab->try_add<GUI::Label>("A backtrace for each thread alive during the crash is listed below:"));
    backtrace_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    backtrace_label->set_fixed_height(16);

    auto backtrace_tab_widget = TRY(backtrace_tab->try_add<GUI::TabWidget>());
    backtrace_tab_widget->set_tab_position(GUI::TabWidget::TabPosition::Bottom);
    for (auto& backtrace : thread_backtraces) {
        auto container = TRY(backtrace_tab_widget->try_add_tab<GUI::Widget>(backtrace.title));
        (void)TRY(container->try_set_layout<GUI::VerticalBoxLayout>());
        container->layout()->set_margins(4);
        auto backtrace_text_editor = TRY(container->try_add<GUI::TextEditor>());
        backtrace_text_editor->set_text(backtrace.text);
        backtrace_text_editor->set_mode(GUI::TextEditor::Mode::ReadOnly);
        backtrace_text_editor->set_should_hide_unnecessary_scrollbars(true);
    }

    auto cpu_registers_tab = TRY(tab_widget.try_add_tab<GUI::Widget>("CPU Registers"));
    cpu_registers_tab->set_layout<GUI::VerticalBoxLayout>();
    cpu_registers_tab->layout()->set_margins(4);

    auto cpu_registers_label = TRY(cpu_registers_tab->try_add<GUI::Label>("The CPU register state for each thread alive during the crash is listed below:"));
    cpu_registers_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    cpu_registers_label->set_fixed_height(16);

    auto cpu_registers_tab_widget = TRY(cpu_registers_tab->try_add<GUI::TabWidget>());
    cpu_registers_tab_widget->set_tab_position(GUI::TabWidget::TabPosition::Bottom);
    for (auto& cpu_registers : thread_cpu_registers) {
        auto container = TRY(cpu_registers_tab_widget->try_add_tab<GUI::Widget>(cpu_registers.title));
        (void)TRY(container->try_set_layout<GUI::VerticalBoxLayout>());
        container->layout()->set_margins(4);
        auto cpu_registers_text_editor = TRY(container->try_add<GUI::TextEditor>());
        cpu_registers_text_editor->set_text(cpu_registers.text);
        cpu_registers_text_editor->set_mode(GUI::TextEditor::Mode::ReadOnly);
        cpu_registers_text_editor->set_should_hide_unnecessary_scrollbars(true);
    }

    auto environment_tab = TRY(tab_widget.try_add_tab<GUI::Widget>("Environment"));
    (void)TRY(environment_tab->try_set_layout<GUI::VerticalBoxLayout>());
    environment_tab->layout()->set_margins(4);

    auto environment_text_editor = TRY(environment_tab->try_add<GUI::TextEditor>());
    environment_text_editor->set_text(String::join("\n", environment));
    environment_text_editor->set_mode(GUI::TextEditor::Mode::ReadOnly);
    environment_text_editor->set_should_hide_unnecessary_scrollbars(true);

    auto memory_regions_tab = TRY(tab_widget.try_add_tab<GUI::Widget>("Memory Regions"));
    (void)TRY(memory_regions_tab->try_set_layout<GUI::VerticalBoxLayout>());
    memory_regions_tab->layout()->set_margins(4);

    auto memory_regions_text_editor = TRY(memory_regions_tab->try_add<GUI::TextEditor>());
    memory_regions_text_editor->set_text(String::join("\n", memory_regions));
    memory_regions_text_editor->set_mode(GUI::TextEditor::Mode::ReadOnly);
    memory_regions_text_editor->set_should_hide_unnecessary_scrollbars(true);
    memory_regions_text_editor->set_visualize_trailing_whitespace(false);

    auto& close_button = *widget->find_descendant_of_type_named<GUI::Button>("close_button");
    close_button.on_click = [&](auto) {
        if (unlink_on_exit)
            unlink_coredump(coredump_path);
        app->quit();
    };

    auto& debug_button = *widget->find_descendant_of_type_named<GUI::Button>("debug_button");
    debug_button.set_icon(TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-hack-studio.png")));
    debug_button.on_click = [&](int) {
        pid_t child;
        const char* argv[4] = { "HackStudio", "-c", coredump_path, nullptr };
        if ((errno = posix_spawn(&child, "/bin/HackStudio", nullptr, nullptr, const_cast<char**>(argv), environ))) {
            perror("posix_spawn");
        } else {
            if (disown(child) < 0)
                perror("disown");
        }
    };

    window->show();

    return app->exec();
}
