/*
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/Format.h>
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <AK/Result.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibCore/ConfigFile.h>
#include <LibFileSystem/FileSystem.h>
#include <spawn.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <unistd.h>

enum class ComponentCategory {
    Optional,
    Recommended,
    Required
};

struct ComponentData {
    ByteString name;
    ByteString description;
    ComponentCategory category { ComponentCategory::Optional };
    bool was_selected { false };
    Vector<ByteString> dependencies;
    bool is_selected { false };
};

struct WhiptailOption {
    ByteString tag;
    ByteString name;
    ByteString description;
    bool checked { false };
};

enum class WhiptailMode {
    Menu,
    Checklist
};

static Vector<ComponentData> read_component_data(Core::ConfigFile const& config_file)
{
    VERIFY(!config_file.read_entry("Global", "build_everything", {}).is_empty());
    Vector<ComponentData> components;

    auto groups = config_file.groups();
    quick_sort(groups, [](auto& a, auto& b) {
        return a.to_lowercase() < b.to_lowercase();
    });

    for (auto& component_name : groups) {
        if (component_name == "Global")
            continue;
        auto description = config_file.read_entry(component_name, "description", "");
        auto recommended = config_file.read_bool_entry(component_name, "recommended", false);
        auto required = config_file.read_bool_entry(component_name, "required", false);
        auto user_selected = config_file.read_bool_entry(component_name, "user_selected", false);
        auto depends = config_file.read_entry(component_name, "depends", "").split(';');
        // NOTE: Recommended and required shouldn't be set at the same time.
        VERIFY(!recommended || !required);
        ComponentCategory category { ComponentCategory::Optional };
        if (recommended)
            category = ComponentCategory::Recommended;
        else if (required)
            category = ComponentCategory ::Required;

        components.append(ComponentData { component_name, move(description), category, user_selected, move(depends), false });
    }

    return components;
}

static Result<Vector<ByteString>, int> run_whiptail(WhiptailMode mode, Vector<WhiptailOption> const& options, StringView title, StringView description)
{
    struct winsize w;
    if (ioctl(0, TIOCGWINSZ, &w) < 0) {
        perror("ioctl");
        return -errno;
    }

    auto height = w.ws_row - 6;
    auto width = min(w.ws_col - 6, 80);

    int pipefd[2];
    if (pipe(pipefd) < 0) {
        perror("pipefd");
        return -errno;
    }

    int read_fd = pipefd[0];
    int write_fd = pipefd[1];

    Vector<ByteString> arguments = { "whiptail", "--notags", "--separate-output", "--output-fd", ByteString::number(write_fd) };

    if (!title.is_empty()) {
        arguments.append("--title");
        arguments.append(title);
    }

    switch (mode) {
    case WhiptailMode::Menu:
        arguments.append("--menu");
        break;
    case WhiptailMode::Checklist:
        arguments.append("--checklist");
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    if (description.is_empty())
        arguments.append(ByteString::empty());
    else
        arguments.append(ByteString::formatted("\n {}", description));

    arguments.append(ByteString::number(height));
    arguments.append(ByteString::number(width));
    arguments.append(ByteString::number(height - 9));

    // Check how wide the name field needs to be.
    size_t max_name_width = 0;
    for (auto& option : options) {
        if (option.name.length() > max_name_width)
            max_name_width = option.name.length();
    }

    for (auto& option : options) {
        arguments.append(option.tag);
        arguments.append(ByteString::formatted("{:{2}}    {}", option.name, option.description, max_name_width));
        if (mode == WhiptailMode::Checklist)
            arguments.append(option.checked ? "1" : "0");
    }

    char* argv[arguments.size() + 1];
    for (size_t i = 0; i < arguments.size(); ++i)
        argv[i] = const_cast<char*>(arguments[i].characters());
    argv[arguments.size()] = nullptr;

    auto* term_variable = getenv("TERM");
    if (!term_variable) {
        warnln("getenv: TERM variable not set.");
        close(write_fd);
        close(read_fd);
        return -1;
    }

    auto full_term_variable = ByteString::formatted("TERM={}", term_variable);
    auto colors = "NEWT_COLORS=root=,black\ncheckbox=black,lightgray";

    char* env[3];
    env[0] = const_cast<char*>(full_term_variable.characters());
    env[1] = const_cast<char*>(colors);
    env[2] = nullptr;

    pid_t pid;
    if (posix_spawnp(&pid, arguments[0].characters(), nullptr, nullptr, argv, env)) {
        perror("posix_spawnp");
        warnln("\e[31mError:\e[0m Could not execute 'whiptail', maybe it isn't installed.");
        close(write_fd);
        close(read_fd);
        return -errno;
    }

    int status = -1;
    if (waitpid(pid, &status, 0) < 0) {
        perror("waitpid");
        close(write_fd);
        close(read_fd);
        return -errno;
    }

    close(write_fd);

    if (!WIFEXITED(status)) {
        close(read_fd);
        return -1;
    }

    int return_code = WEXITSTATUS(status);
    if (return_code > 0) {
        close(read_fd);
        // posix_spawn returns 127 if it cannot exec the child, so maybe 'whiptail' is missing.
        if (return_code == 127)
            warnln("\e[31mError:\e[0m Could not execute 'whiptail', maybe it isn't installed.");
        return return_code;
    }

    auto file_or_error = Core::File::adopt_fd(read_fd, Core::File::OpenMode::Read);
    if (file_or_error.is_error()) {
        warnln("\e[31mError:\e[0m Could not adopt file descriptor for reading: {}", file_or_error.error());
        return -1;
    }
    auto data_or_error = file_or_error.value()->read_until_eof();
    if (data_or_error.is_error()) {
        warnln("\e[31mError:\e[0m Could not read data from file descriptor: {}", data_or_error.error());
        return -1;
    }
    return ByteString::copy(data_or_error.value()).split('\n');
}

static bool run_system_command(ByteString const& command, StringView command_name)
{
    if (command.starts_with("cmake"sv))
        warnln("\e[34mRunning CMake...\e[0m");
    else
        warnln("\e[34mRunning '{}'...\e[0m", command);
    auto rc = system(command.characters());
    if (rc < 0) {
        perror("system");
        warnln("\e[31mError:\e[0m Could not run {}.", command_name);
        return false;
    } else if (rc > 0) {
        warnln("\e[31mError:\e[0m {} returned status code {}.", command_name, rc);
        return false;
    }
    return true;
}

int main()
{
    // Step 1: Check if everything is in order.
    if (!isatty(STDIN_FILENO)) {
        warnln("Not a terminal!");
        return 1;
    }

    auto current_working_directory = FileSystem::current_working_directory();
    if (current_working_directory.is_error())
        return 1;
    auto lexical_cwd = LexicalPath(current_working_directory.release_value());
    auto& parts = lexical_cwd.parts_view();
    if (parts.size() < 2 || parts[parts.size() - 2] != "Build") {
        warnln("\e[31mError:\e[0m This program needs to be executed from inside 'Build/*'.");
        return 1;
    }

    if (!FileSystem::exists("components.ini"sv)) {
        warnln("\e[31mError:\e[0m There is no 'components.ini' in the current working directory.");
        warnln("       It can be generated by running CMake with 'cmake ../.. -G Ninja'");
        return 1;
    }

    // Step 2: Open and parse the 'components.ini' file.
    auto components_file = Core::ConfigFile::open("components.ini").release_value_but_fixme_should_propagate_errors();
    if (components_file->groups().is_empty()) {
        warnln("\e[31mError:\e[0m The 'components.ini' file is either not a valid ini file or contains no entries.");
        return 1;
    }

    bool build_everything = components_file->read_bool_entry("Global", "build_everything", false);
    auto components = read_component_data(components_file);
    warnln("{} components were read from 'components.ini'.", components.size());

    // Step 3: Ask the user which starting configuration to use.
    Vector<WhiptailOption> configs;
    configs.append({ "REQUIRED", "Required", "Only the essentials.", false });
    configs.append({ "RECOMMENDED", "Recommended", "A sensible collection of programs.", false });
    configs.append({ "FULL", "Full", "All available programs.", false });
    configs.append({ "CUSTOM_REQUIRED", "Required", "Customizable.", false });
    configs.append({ "CUSTOM_RECOMMENDED", "Recommended", "Customizable.", false });
    configs.append({ "CUSTOM_FULL", "Full", "Customizable.", false });
    configs.append({ "CUSTOM_CURRENT", "Current", "Customize current configuration.", false });

    auto configs_result = run_whiptail(WhiptailMode::Menu, configs, "SerenityOS - System Configurations"sv, "Which system configuration do you want to use or customize?"sv);
    if (configs_result.is_error()) {
        warnln("ConfigureComponents cancelled.");
        return 0;
    }

    VERIFY(configs_result.value().size() == 1);
    auto type = configs_result.value().first();

    bool customize = type.starts_with("CUSTOM_"sv);
    StringView build_type = customize ? type.substring_view(7) : type.view();

    // Step 4: Customize the configuration if the user requested to. In any case, set the components component.is_selected value correctly.
    Vector<ByteString> activated_components;

    if (customize) {
        Vector<WhiptailOption> options;
        for (auto& component : components) {
            auto is_required = component.category == ComponentCategory::Required;

            StringBuilder description_builder;
            description_builder.append(component.description);
            if (is_required) {
                if (!description_builder.is_empty())
                    description_builder.append(' ');
                description_builder.append("[required]"sv);
            }

            // NOTE: Required components will always be preselected.
            WhiptailOption option { component.name, component.name, description_builder.to_byte_string(), is_required };
            if (build_type == "REQUIRED") {
                // noop
            } else if (build_type == "RECOMMENDED") {
                if (component.category == ComponentCategory::Recommended)
                    option.checked = true;
            } else if (build_type == "FULL") {
                option.checked = true;
            } else if (build_type == "CURRENT") {
                if (build_everything || component.was_selected)
                    option.checked = true;
            } else {
                VERIFY_NOT_REACHED();
            }
            options.append(move(option));
        }

        auto result = run_whiptail(WhiptailMode::Checklist, options, "SerenityOS - System Components"sv, "Which optional system components do you want to include?"sv);
        if (result.is_error()) {
            warnln("ConfigureComponents cancelled.");
            return 0;
        }

        auto selected_components = result.value();
        for (auto& component : components) {
            if (selected_components.contains_slow(component.name)) {
                component.is_selected = true;
            } else if (component.category == ComponentCategory::Required) {
                warnln("\e[33mWarning:\e[0m {} was not selected even though it is required. It will be enabled anyway.", component.name);
                component.is_selected = true;
            }
        }
    } else {
        for (auto& component : components) {
            if (build_type == "REQUIRED")
                component.is_selected = component.category == ComponentCategory::Required;
            else if (build_type == "RECOMMENDED")
                component.is_selected = component.category == ComponentCategory::Required || component.category == ComponentCategory::Recommended;
            else if (build_type == "FULL")
                component.is_selected = true;
            else
                VERIFY_NOT_REACHED();
        }
    }

    // Step 5: Generate the cmake command.
    Vector<ByteString> cmake_arguments = { "cmake", "../..", "-G", "Ninja", "-DBUILD_EVERYTHING=OFF" };
    for (auto& component : components)
        cmake_arguments.append(ByteString::formatted("-DBUILD_{}={}", component.name.to_uppercase(), component.is_selected ? "ON" : "OFF"));

    warnln("\e[34mThe following command will be run:\e[0m");
    outln("ninja clean \\\n  && rm -rf Root \\");
    outln("  && {}", ByteString::join(' ', cmake_arguments));
    warn("\e[34mDo you want to run the command?\e[0m [Y/n] ");
    auto character = getchar();
    if (character == 'n' || character == 'N') {
        warnln("ConfigureComponents cancelled.");
        return 0;
    }

    // Step 6: Run 'ninja clean', 'rm -rf Root' and CMake
    auto command = ByteString::join(' ', cmake_arguments);
    if (!run_system_command("ninja clean"sv, "Ninja"sv))
        return 1;
    if (!run_system_command("rm -rf Root"sv, "rm"sv))
        return 1;
    if (!run_system_command(command, "CMake"sv))
        return 1;
    return 0;
}
