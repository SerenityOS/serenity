/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/LexicalPath.h>
#include <YAK/Optional.h>
#include <YAK/RefCounted.h>
#include <YAK/RefPtr.h>
#include <YAK/String.h>
#include <YAK/Vector.h>

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

    static CreateResult try_to_create(const LexicalPath& repository_root);
    static RefPtr<GitRepo> initialize_repository(const LexicalPath& repository_root);

    Vector<LexicalPath> unstaged_files() const;
    Vector<LexicalPath> staged_files() const;
    bool stage(const LexicalPath& file);
    bool unstage(const LexicalPath& file);
    bool commit(const String& message);
    Optional<String> original_file_content(const LexicalPath& file) const;
    Optional<String> unstaged_diff(const LexicalPath& file) const;
    bool is_tracked(const LexicalPath& file) const;

private:
    static String command_wrapper(const Vector<String>& command_parts, const LexicalPath& chdir);
    static bool git_is_installed();
    static bool git_repo_exists(const LexicalPath& repo_root);
    static Vector<LexicalPath> parse_files_list(const String&);

    explicit GitRepo(const LexicalPath& repository_root)
        : m_repository_root(repository_root)
    {
    }

    Vector<LexicalPath> modified_files() const;
    Vector<LexicalPath> untracked_files() const;

    String command(const Vector<String>& command_parts) const;

    LexicalPath m_repository_root;
};

}
