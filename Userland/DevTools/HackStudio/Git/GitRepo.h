/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
