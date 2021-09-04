/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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

    static CreateResult try_to_create(LexicalPath const& repository_root);
    static RefPtr<GitRepo> initialize_repository(LexicalPath const& repository_root);

    Vector<LexicalPath> unstaged_files() const;
    Vector<LexicalPath> staged_files() const;
    bool stage(LexicalPath const& file);
    bool unstage(LexicalPath const& file);
    bool commit(String const& message);
    Optional<String> original_file_content(LexicalPath const& file) const;
    Optional<String> unstaged_diff(LexicalPath const& file) const;
    bool is_tracked(LexicalPath const& file) const;

private:
    static String command_wrapper(const Vector<String>& command_parts, LexicalPath const& chdir);
    static bool git_is_installed();
    static bool git_repo_exists(LexicalPath const& repo_root);
    static Vector<LexicalPath> parse_files_list(String const&);

    explicit GitRepo(LexicalPath const& repository_root)
        : m_repository_root(repository_root)
    {
    }

    Vector<LexicalPath> modified_files() const;
    Vector<LexicalPath> untracked_files() const;

    String command(const Vector<String>& command_parts) const;

    LexicalPath m_repository_root;
};

}
