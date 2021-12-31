/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2021, Conor Byrne <conor@cbyrne.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/LexicalPath.h>
#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
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

    static CreateResult try_to_create(String const& repository_root);
    static RefPtr<GitRepo> initialize_repository(String const& repository_root);

    bool stage(String const& file);
    bool unstage(String const& file);
    bool commit(String const& message);
    bool is_tracked(String const& file) const;

    Vector<String> unstaged_files() const;
    Vector<String> staged_files() const;
    Optional<String> original_file_content(String const& file) const;
    Optional<String> unstaged_diff(String const& file) const;

private:
    static bool git_is_installed();
    static bool git_repo_exists(String const& repo_root);

    static String command_wrapper(Vector<String> const& command_parts, String const& chdir);
    static Vector<String> parse_files_list(String const&);

    explicit GitRepo(String const& repository_root)
        : m_repository_root(repository_root)
    {
    }

    Vector<String> modified_files() const;
    Vector<String> untracked_files() const;

    String command(Vector<String> const& command_parts) const;

    String m_repository_root;
};

}
