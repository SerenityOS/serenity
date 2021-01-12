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

#include "GitRepo.h"
#include <LibCore/Command.h>
#include <stdio.h>
#include <stdlib.h>

namespace HackStudio {

GitRepo::CreateResult GitRepo::try_to_create(const LexicalPath& repository_root)
{
    if (!git_is_installed()) {
        return { CreateResult::Type::GitProgramNotFound, nullptr };
    }
    if (!git_repo_exists(repository_root)) {
        return { CreateResult::Type::NoGitRepo, nullptr };
    }

    return { CreateResult::Type::Success, adopt(*new GitRepo(repository_root)) };
}

RefPtr<GitRepo> GitRepo::initialize_repository(const LexicalPath& repository_root)
{
    auto res = command_wrapper({ "init" }, repository_root);
    if (res.is_null())
        return {};

    ASSERT(git_repo_exists(repository_root));
    return adopt(*new GitRepo(repository_root));
}

Vector<LexicalPath> GitRepo::unstaged_files() const
{
    auto modified = modified_files();
    auto untracked = untracked_files();
    modified.append(move(untracked));
    return modified;
}
//
Vector<LexicalPath> GitRepo::staged_files() const
{
    auto raw_result = command({ "diff", "--cached", "--name-only" });
    if (raw_result.is_null())
        return {};
    return parse_files_list(raw_result);
}

Vector<LexicalPath> GitRepo::modified_files() const
{
    auto raw_result = command({ "ls-files", "--modified", "--exclude-standard" });
    if (raw_result.is_null())
        return {};
    return parse_files_list(raw_result);
}

Vector<LexicalPath> GitRepo::untracked_files() const
{
    auto raw_result = command({ "ls-files", "--others", "--exclude-standard" });
    if (raw_result.is_null())
        return {};
    return parse_files_list(raw_result);
}

Vector<LexicalPath> GitRepo::parse_files_list(const String& raw_result)
{
    auto lines = raw_result.split('\n');
    Vector<LexicalPath> files;
    for (const auto& line : lines) {
        files.empend(line);
    }
    return files;
}

String GitRepo::command(const Vector<String>& command_parts) const
{
    return command_wrapper(command_parts, m_repository_root);
}

String GitRepo::command_wrapper(const Vector<String>& command_parts, const LexicalPath& chdir)
{
    return Core::command("git", command_parts, chdir);
}

bool GitRepo::git_is_installed()
{
    return !command_wrapper({ "--help" }, LexicalPath("/")).is_null();
}

bool GitRepo::git_repo_exists(const LexicalPath& repo_root)
{
    return !command_wrapper({ "status" }, repo_root).is_null();
}

bool GitRepo::stage(const LexicalPath& file)
{
    return !command({ "add", file.string() }).is_null();
}

bool GitRepo::unstage(const LexicalPath& file)
{
    return !command({ "reset", "HEAD", "--", file.string() }).is_null();
}

bool GitRepo::commit(const String& message)
{
    return !command({ "commit", "-m", message }).is_null();
}

Optional<String> GitRepo::original_file_content(const LexicalPath& file) const
{
    return command({ "show", String::formatted("HEAD:{}", file) });
}

Optional<String> GitRepo::unstaged_diff(const LexicalPath& file) const
{
    return command({ "diff", file.string().characters() });
}

bool GitRepo::is_tracked(const LexicalPath& file) const
{
    auto res = command({ "ls-files", file.string() });
    if (res.is_null())
        return false;
    return !res.is_empty();
}

}
