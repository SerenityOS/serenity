/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2021, Conor Byrne <conor@cbyrne.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
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

    static CreateResult try_to_create(ByteString const& repository_root);
    static RefPtr<GitRepo> initialize_repository(ByteString const& repository_root);

    bool stage(ByteString const& file);
    bool unstage(ByteString const& file);
    bool commit(ByteString const& message);
    bool is_tracked(ByteString const& file) const;

    Vector<ByteString> unstaged_files() const;
    Vector<ByteString> staged_files() const;
    Optional<ByteString> original_file_content(ByteString const& file) const;
    Optional<ByteString> unstaged_diff(ByteString const& file) const;

private:
    static bool git_is_installed();
    static bool git_repo_exists(ByteString const& repo_root);

    static Optional<ByteString> command_wrapper(Vector<ByteString> const& command_parts, ByteString const& chdir);
    static Vector<ByteString> parse_files_list(ByteString const&);

    explicit GitRepo(ByteString const& repository_root)
        : m_repository_root(repository_root)
    {
    }

    Vector<ByteString> modified_files() const;
    Vector<ByteString> untracked_files() const;

    Optional<ByteString> command(Vector<ByteString> const& command_parts) const;

    ByteString m_repository_root;
};

}
