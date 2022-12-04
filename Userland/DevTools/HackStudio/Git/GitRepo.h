/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2021, Conor Byrne <conor@cbyrne.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/LexicalPath.h>
#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>

namespace HackStudio {

class GitRepo final : public RefCounted<GitRepo> {
public:
    struct CreateResult {
        enum class Type {
            Success,
            NoGitRepo,
            GitProgramNotFound,
        };
        Type type;
        RefPtr<GitRepo> repo;
    };

    static CreateResult try_to_create(DeprecatedString const& repository_root);
    static RefPtr<GitRepo> initialize_repository(DeprecatedString const& repository_root);

    bool stage(DeprecatedString const& file);
    bool unstage(DeprecatedString const& file);
    bool commit(DeprecatedString const& message);
    bool is_tracked(DeprecatedString const& file) const;

    Vector<DeprecatedString> unstaged_files() const;
    Vector<DeprecatedString> staged_files() const;
    Optional<DeprecatedString> original_file_content(DeprecatedString const& file) const;
    Optional<DeprecatedString> unstaged_diff(DeprecatedString const& file) const;

private:
    static bool git_is_installed();
    static bool git_repo_exists(DeprecatedString const& repo_root);

    static DeprecatedString command_wrapper(Vector<DeprecatedString> const& command_parts, DeprecatedString const& chdir);
    static Vector<DeprecatedString> parse_files_list(DeprecatedString const&);

    explicit GitRepo(DeprecatedString const& repository_root)
        : m_repository_root(repository_root)
    {
    }

    Vector<DeprecatedString> modified_files() const;
    Vector<DeprecatedString> untracked_files() const;

    DeprecatedString command(Vector<DeprecatedString> const& command_parts) const;

    DeprecatedString m_repository_root;
};

}
