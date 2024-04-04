/*
 * Copyright (c) 2022, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ProjectBuilder.h"
#include <AK/LexicalPath.h>
#include <LibCore/Command.h>
#include <LibFileSystem/FileSystem.h>
#include <LibRegex/Regex.h>
#include <fcntl.h>
#include <sys/stat.h>

namespace HackStudio {

ProjectBuilder::ProjectBuilder(NonnullRefPtr<TerminalWrapper> terminal, Project const& project)
    : m_project_root(project.root_path())
    , m_project(project)
    , m_terminal(move(terminal))
    , m_is_serenity(project.project_is_serenity() ? IsSerenityRepo::Yes : IsSerenityRepo::No)
{
}

ErrorOr<void> ProjectBuilder::build(StringView active_file)
{
    m_terminal->clear_including_history();

    if (auto command = m_project.config()->build_command(); command.has_value()) {
        TRY(m_terminal->run_command(command.value()));
        return {};
    }

    if (active_file.is_null())
        return Error::from_string_literal("no active file");

    if (active_file.ends_with(".js"sv)) {
        TRY(m_terminal->run_command(ByteString::formatted("js -A {}", active_file)));
        return {};
    }

    if (m_is_serenity == IsSerenityRepo::No) {
        TRY(verify_make_is_installed());
        TRY(m_terminal->run_command("make"));
        return {};
    }

    TRY(update_active_file(active_file));

    return build_serenity_component();
}

ErrorOr<void> ProjectBuilder::run(StringView active_file)
{
    if (auto command = m_project.config()->run_command(); command.has_value()) {
        TRY(m_terminal->run_command(command.value()));
        return {};
    }

    if (active_file.is_null())
        return Error::from_string_literal("no active file");

    if (active_file.ends_with(".js"sv)) {
        TRY(m_terminal->run_command(ByteString::formatted("js {}", active_file)));
        return {};
    }

    if (m_is_serenity == IsSerenityRepo::No) {
        TRY(verify_make_is_installed());
        TRY(m_terminal->run_command("make run"));
        return {};
    }

    TRY(update_active_file(active_file));

    return run_serenity_component();
}

ErrorOr<void> ProjectBuilder::run_serenity_component()
{
    auto relative_path_to_dir = LexicalPath::relative_path(LexicalPath::dirname(m_serenity_component_cmake_file), m_project_root);
    TRY(m_terminal->run_command(LexicalPath::join(relative_path_to_dir, m_serenity_component_name).string(), build_directory()));
    return {};
}

ErrorOr<void> ProjectBuilder::update_active_file(StringView active_file)
{
    TRY(verify_cmake_is_installed());
    auto cmake_file = find_cmake_file_for(active_file);
    if (!cmake_file.has_value()) {
        warnln("did not find cmake file for: {}", active_file);
        return Error::from_string_literal("did not find cmake file");
    }

    if (m_serenity_component_cmake_file == cmake_file.value())
        return {};

    m_serenity_component_cmake_file = cmake_file.value();
    m_serenity_component_name = TRY(component_name(m_serenity_component_cmake_file));

    TRY(initialize_build_directory());
    return {};
}

ErrorOr<void> ProjectBuilder::build_serenity_component()
{
    TRY(verify_make_is_installed());
    TRY(m_terminal->run_command(ByteString::formatted("make {}", m_serenity_component_name), build_directory(), TerminalWrapper::WaitForExit::Yes, "Make failed"sv));
    return {};
}

ErrorOr<ByteString> ProjectBuilder::component_name(StringView cmake_file_path)
{
    auto file = TRY(Core::File::open(cmake_file_path, Core::File::OpenMode::Read));
    auto content = TRY(file->read_until_eof());

    static Regex<ECMA262> const component_name(R"~~~(serenity_component\([\s]*(\w+)[\s\S]*\))~~~");
    RegexResult result;
    if (!component_name.search(StringView { content }, result))
        return Error::from_string_literal("component not found");

    return ByteString { result.capture_group_matches.at(0).at(0).view.string_view() };
}

ErrorOr<void> ProjectBuilder::initialize_build_directory()
{
    if (!FileSystem::exists(build_directory())) {
        if (mkdir(LexicalPath::join(build_directory()).string().characters(), 0700)) {
            return Error::from_errno(errno);
        }
    }

    auto cmake_file_path = LexicalPath::join(build_directory(), "CMakeLists.txt"sv).string();
    if (FileSystem::exists(cmake_file_path))
        MUST(FileSystem::remove(cmake_file_path, FileSystem::RecursionMode::Disallowed));

    auto cmake_file = TRY(Core::File::open(cmake_file_path, Core::File::OpenMode::Write));
    TRY(cmake_file->write_until_depleted(generate_cmake_file_content()));

    TRY(m_terminal->run_command(ByteString::formatted("cmake -S {} -DHACKSTUDIO_BUILD=ON -DHACKSTUDIO_BUILD_CMAKE_FILE={}"
                                                      " -DENABLE_UNICODE_DATABASE_DOWNLOAD=OFF",
                                    m_project_root, cmake_file_path),
        build_directory(), TerminalWrapper::WaitForExit::Yes, "CMake error"sv));

    return {};
}

Optional<ByteString> ProjectBuilder::find_cmake_file_for(StringView file_path) const
{
    auto directory = LexicalPath::dirname(file_path);
    while (!directory.is_empty()) {
        auto cmake_path = LexicalPath::join(m_project_root, directory, "CMakeLists.txt"sv);
        if (FileSystem::exists(cmake_path.string()))
            return cmake_path.string();
        directory = LexicalPath::dirname(directory);
    }
    return {};
}

ByteString ProjectBuilder::generate_cmake_file_content() const
{
    StringBuilder builder;
    builder.appendff("add_subdirectory({})\n", LexicalPath::dirname(m_serenity_component_cmake_file));

    auto defined_libraries = get_defined_libraries();
    for (auto& library : defined_libraries) {
        builder.appendff("add_library({} SHARED IMPORTED GLOBAL)\n", library.key);
        builder.appendff("set_target_properties({} PROPERTIES IMPORTED_LOCATION {})\n", library.key, library.value->path);

        if (library.key == "LibCStaticWithoutDeps"sv)
            continue;

        // We need to specify the dependencies for each defined library in CMake because some applications do not specify
        // all of their direct dependencies in the CMakeLists file.
        // For example, a target may directly use LibGFX but only specify LibGUI as a dependency (which in turn depends on LibGFX).
        // In this example, if we don't specify the dependencies of LibGUI in the CMake file, linking will fail because of undefined LibGFX symbols.
        builder.appendff("target_link_libraries({} INTERFACE {})\n", library.key, ByteString::join(' ', library.value->dependencies));
    }

    return builder.to_byte_string();
}

HashMap<ByteString, NonnullOwnPtr<ProjectBuilder::LibraryInfo>> ProjectBuilder::get_defined_libraries()
{
    HashMap<ByteString, NonnullOwnPtr<ProjectBuilder::LibraryInfo>> libraries;

    for_each_library_definition([&libraries](ByteString name, ByteString path) {
        libraries.set(name, make<ProjectBuilder::LibraryInfo>(move(path)));
    });
    for_each_library_dependencies([&libraries](ByteString name, Vector<StringView> const& dependencies) {
        auto library = libraries.get(name);
        if (!library.has_value())
            return;
        for (auto const& dependency : dependencies) {
            if (libraries.contains(dependency))
                library.value()->dependencies.append(dependency);
        }
    });
    return libraries;
}

void ProjectBuilder::for_each_library_definition(Function<void(ByteString, ByteString)> func)
{
    Vector<ByteString> arguments = { "-c", "find Userland -name CMakeLists.txt | xargs grep serenity_lib" };
    auto res = Core::command("/bin/sh", arguments, {});
    if (res.is_error()) {
        warnln("{}", res.error());
        return;
    }

    static Regex<ECMA262> const parse_library_definition(R"~~~(.+:serenity_lib[c]?\((\w+) (\w+)\).*)~~~");
    for (auto& line : StringView(res.value().output).split_view('\n')) {
        RegexResult result;
        if (!parse_library_definition.search(line, result))
            continue;
        if (result.capture_group_matches.size() != 1 || result.capture_group_matches[0].size() != 2)
            continue;

        auto library_name = result.capture_group_matches.at(0).at(0).view.string_view();
        auto library_obj_name = result.capture_group_matches.at(0).at(1).view.string_view();
        auto so_path = ByteString::formatted("{}.so", LexicalPath::join("/usr/lib"sv, ByteString::formatted("lib{}", library_obj_name)).string());
        func(library_name, so_path);
    }

    // ssp is defined with "add_library" so it doesn't get picked up with the current logic for finding library definitions.
    func("ssp", "/usr/lib/libssp.a");
}

void ProjectBuilder::for_each_library_dependencies(Function<void(ByteString, Vector<StringView>)> func)
{
    Vector<ByteString> arguments = { "-c", "find Userland/Libraries -name CMakeLists.txt | xargs grep target_link_libraries" };
    auto res = Core::command("/bin/sh", arguments, {});
    if (res.is_error()) {
        warnln("{}", res.error());
        return;
    }
    auto libraries = StringView(res.value().output).split_view('\n');

    static Regex<ECMA262> const parse_library_definition(R"~~~(.+:target_link_libraries\((\w+) ([\w\s]+)\).*)~~~");
    for (auto& line : libraries) {
        RegexResult result;
        if (!parse_library_definition.search(line, result))
            continue;
        if (result.capture_group_matches.size() != 1 || result.capture_group_matches[0].size() != 2)
            continue;

        auto library_name = result.capture_group_matches.at(0).at(0).view.string_view();
        auto dependencies_string = result.capture_group_matches.at(0).at(1).view.string_view();

        func(library_name, dependencies_string.split_view(' '));
    }
}

ErrorOr<void> ProjectBuilder::verify_cmake_is_installed()
{
    auto res = Core::command("cmake --version", {});
    if (!res.is_error() && res.value().exit_code == 0)
        return {};
    return Error::from_string_literal("CMake port is not installed");
}

ErrorOr<void> ProjectBuilder::verify_make_is_installed()
{
    auto res = Core::command("make --version", {});
    if (!res.is_error() && res.value().exit_code == 0)
        return {};
    return Error::from_string_literal("Make port is not installed");
}

ByteString ProjectBuilder::build_directory() const
{
    return LexicalPath::join(m_project_root, "Build"sv).string();
}

}
