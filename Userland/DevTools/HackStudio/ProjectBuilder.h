/*
 * Copyright (c) 2022, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AK/Error.h"
#include "Project.h"
#include "TerminalWrapper.h"
#include <AK/Noncopyable.h>
#include <LibCore/TempFile.h>

namespace HackStudio {
class ProjectBuilder {

    AK_MAKE_NONCOPYABLE(ProjectBuilder);

public:
    ProjectBuilder(NonnullRefPtr<TerminalWrapper>, Project const&);
    ~ProjectBuilder() = default;

    ErrorOr<void> build(StringView active_file);
    ErrorOr<void> run(StringView active_file);

private:
    enum class IsSerenityRepo {
        No,
        Yes
    };

    ErrorOr<void> build_serenity_component();
    ErrorOr<void> run_serenity_component();
    ErrorOr<void> initialize_build_directory();
    Optional<DeprecatedString> find_cmake_file_for(StringView file_path) const;
    DeprecatedString generate_cmake_file_content() const;
    ErrorOr<void> update_active_file(StringView active_file);
    DeprecatedString build_directory() const;

    struct LibraryInfo {
        DeprecatedString path;
        Vector<DeprecatedString> dependencies {};
    };
    static HashMap<DeprecatedString, NonnullOwnPtr<LibraryInfo>> get_defined_libraries();
    static void for_each_library_definition(Function<void(DeprecatedString, DeprecatedString)>);
    static void for_each_library_dependencies(Function<void(DeprecatedString, Vector<StringView>)>);
    static ErrorOr<DeprecatedString> component_name(StringView cmake_file_path);
    static ErrorOr<void> verify_cmake_is_installed();
    static ErrorOr<void> verify_make_is_installed();

    DeprecatedString m_project_root;
    Project const& m_project;
    NonnullRefPtr<TerminalWrapper> m_terminal;
    IsSerenityRepo m_is_serenity { IsSerenityRepo::No };
    DeprecatedString m_serenity_component_cmake_file;
    DeprecatedString m_serenity_component_name;
};
}
