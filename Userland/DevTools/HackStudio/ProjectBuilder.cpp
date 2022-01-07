/*
 * Copyright (c) 2022, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProjectBuilder.h"
#include <AK/LexicalPath.h>
#include <LibCore/Command.h>
#include <LibCore/File.h>
#include <LibRegex/Regex.h>
#include <fcntl.h>
#include <sys/stat.h>

namespace HackStudio {

ProjectBuilder::ProjectBuilder(NonnullRefPtr<TerminalWrapper> terminal, Project const& project)
    : m_project_root(project.root_path())
    , m_terminal(move(terminal))
    , m_is_serenity(project.project_is_serenity() ? IsSerenityRepo::Yes : IsSerenityRepo::No)
{
}

ErrorOr<void> ProjectBuilder::build(StringView active_file)
{
    m_terminal->clear_including_history();
    if (active_file.is_null())
        return Error::from_string_literal("no active file"sv);

    if (active_file.ends_with(".js")) {
        m_terminal->run_command(String::formatted("js -A {}", active_file));
        return {};
    }
    if (m_is_serenity == IsSerenityRepo::No) {
        m_terminal->run_command("make");
        return {};
    }

    TRY(update_active_file(active_file));

    return build_serenity_component();
}

ErrorOr<void> ProjectBuilder::run(StringView active_file)
{
    if (active_file.is_null())
        return Error::from_string_literal("no active file"sv);

    if (active_file.ends_with(".js")) {
        m_terminal->run_command(String::formatted("js {}", active_file));
        return {};
    }

    if (m_is_serenity == IsSerenityRepo::No) {
        m_terminal->run_command("make run");
        return {};
    }

    TRY(update_active_file(active_file));

    return run_serenity_component();
}

ErrorOr<void> ProjectBuilder::run_serenity_component()
{
    auto relative_path_to_dir = LexicalPath::relative_path(LexicalPath::dirname(m_serenity_component_cmake_file), m_project_root);
    m_terminal->run_command(LexicalPath::join(relative_path_to_dir, m_serenity_component_name).string(), LexicalPath::join(m_build_directory->path(), "Build").string());
    return {};
}

ErrorOr<void> ProjectBuilder::update_active_file(StringView active_file)
{
    TRY(verify_cmake_is_installed());
    auto cmake_file = find_cmake_file_for(active_file);
    if (!cmake_file.has_value()) {
        warnln("did not find cmake file for: {}", active_file);
        return Error::from_string_literal("did not find cmake file"sv);
    }

    if (m_serenity_component_cmake_file == cmake_file.value())
        return {};

    if (!m_serenity_component_cmake_file.is_null())
        m_build_directory.clear();

    m_serenity_component_cmake_file = cmake_file.value();
    m_serenity_component_name = TRY(component_name(m_serenity_component_cmake_file));

    TRY(initialize_build_directory());
    return {};
}

ErrorOr<void> ProjectBuilder::build_serenity_component()
{
    m_terminal->run_command(String::formatted("make {}", m_serenity_component_name), LexicalPath::join(m_build_directory->path(), "Build"sv).string(), TerminalWrapper::WaitForExit::Yes);
    if (m_terminal->child_exit_status() == 0)
        return {};
    return Error::from_string_literal("Make failed"sv);
}

ErrorOr<String> ProjectBuilder::component_name(StringView cmake_file_path)
{
    auto content = TRY(Core::File::open(cmake_file_path, Core::OpenMode::ReadOnly))->read_all();

    static const Regex<ECMA262> component_name(R"~~~(serenity_component\([\s]*(\w+)[\s\S]*\))~~~");
    RegexResult result;
    if (!component_name.search(StringView { content }, result))
        return Error::from_string_literal("component not found"sv);

    return String { result.capture_group_matches.at(0).at(0).view.string_view() };
}

ErrorOr<void> ProjectBuilder::initialize_build_directory()
{
    m_build_directory = Core::TempFile::create(Core::TempFile::Type::Directory);
    if (mkdir(LexicalPath::join(m_build_directory->path(), "Build").string().characters(), 0700)) {
        return Error::from_errno(errno);
    }

    auto cmake_file_path = LexicalPath::join(m_build_directory->path(), "CMakeLists.txt").string();
    auto cmake_file = TRY(Core::File::open(cmake_file_path, Core::OpenMode::WriteOnly));
    cmake_file->write(generate_cmake_file_content());

    m_terminal->run_command(String::formatted("cmake -S {} -DHACKSTUDIO_BUILD=ON -DHACKSTUDIO_BUILD_CMAKE_FILE={}"
                                              " -DENABLE_UNICODE_DATABASE_DOWNLOAD=OFF",
                                m_project_root, cmake_file_path),
        LexicalPath::join(m_build_directory->path(), "Build"sv).string(), TerminalWrapper::WaitForExit::Yes);

    if (m_terminal->child_exit_status() == 0)
        return {};
    return Error::from_string_literal("CMake error"sv);
}

Optional<String> ProjectBuilder::find_cmake_file_for(StringView file_path) const
{
    auto directory = LexicalPath::dirname(file_path);
    while (!directory.is_empty()) {
        auto cmake_path = LexicalPath::join(m_project_root, directory, "CMakeLists.txt");
        if (Core::File::exists(cmake_path.string()))
            return cmake_path.string();
        directory = LexicalPath::dirname(directory);
    }
    return {};
}

String ProjectBuilder::generate_cmake_file_content() const
{
    StringBuilder builder;
    builder.appendff("add_subdirectory({})\n", LexicalPath::dirname(m_serenity_component_cmake_file));
    generate_cmake_library_definitions(builder);
    builder.append('\n');
    generate_cmake_library_dependencies(builder);

    return builder.to_string();
}

void ProjectBuilder::generate_cmake_library_definitions(StringBuilder& builder)
{
    Vector<String> arguments = { "sh", "-c", "find Userland/Libraries -name CMakeLists.txt | xargs grep serenity_lib" };
    auto res = Core::command("/bin/sh", arguments, {});
    if (res.is_error()) {
        warnln("{}", res.error());
        return;
    }

    static const Regex<ECMA262> parse_library_definition(R"~~~(.+:serenity_lib[c]?\((\w+) (\w+)\).*)~~~");
    for (auto& line : res.value().stdout.split('\n')) {

        RegexResult result;
        if (!parse_library_definition.search(line, result))
            continue;
        if (result.capture_group_matches.size() != 1 || result.capture_group_matches[0].size() != 2)
            continue;

        auto library_name = result.capture_group_matches.at(0).at(0).view.string_view();
        auto library_obj_name = result.capture_group_matches.at(0).at(1).view.string_view();
        builder.appendff("add_library({} SHARED IMPORTED GLOBAL)\n", library_name);
        auto so_path = String::formatted("{}.so", LexicalPath::join("/usr/lib"sv, String::formatted("lib{}", library_obj_name)).string());
        builder.appendff("set_target_properties({} PROPERTIES IMPORTED_LOCATION {})\n", library_name, so_path);
    }
}

void ProjectBuilder::generate_cmake_library_dependencies(StringBuilder& builder)
{
    Vector<String> arguments = { "sh", "-c", "find Userland/Libraries -name CMakeLists.txt | xargs grep target_link_libraries" };
    auto res = Core::command("/bin/sh", arguments, {});
    if (res.is_error()) {
        warnln("{}", res.error());
        return;
    }

    static const Regex<ECMA262> parse_library_definition(R"~~~(.+:target_link_libraries\((\w+) ([\w\s]+)\).*)~~~");
    for (auto& line : res.value().stdout.split('\n')) {

        RegexResult result;
        if (!parse_library_definition.search(line, result))
            continue;
        if (result.capture_group_matches.size() != 1 || result.capture_group_matches[0].size() != 2)
            continue;

        auto library_name = result.capture_group_matches.at(0).at(0).view.string_view();
        auto dependencies = result.capture_group_matches.at(0).at(1).view.string_view();
        if (library_name == "LibCStaticWithoutDeps"sv || library_name == "DumpLayoutTree"sv)
            continue;
        builder.appendff("target_link_libraries({} INTERFACE {})\n", library_name, dependencies);
    }
}

ErrorOr<void> ProjectBuilder::verify_cmake_is_installed()
{
    auto res = Core::command("cmake --version", {});
    if (!res.is_error() && res.value().exit_code == 0)
        return {};
    return Error::from_string_literal("CMake port is not installed"sv);
}

}
