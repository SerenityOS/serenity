/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GitRepo.h"
#include <LibCore/Command.h>

namespace HackStudio {

GitRepo::CreateResult GitRepo::try_to_create(const LexicalPath& repository_root)
{
    if (!git_is_installed()) {
        return { CreateResult::Type::GitProgramNotFound, nullptr };
    }
    if (!git_repo_exists(repository_root)) {
        return { CreateResult::Type::NoGitRepo, nullptr };
    }

    return { CreateResult::Type::Success, adopt_ref(*new GitRepo(repository_root)) };
}

RefPtr<GitRepo> GitRepo::initialize_repository(const LexicalPath& repository_root)
{
    auto res = command_wrapper({ "init" }, repository_root);
    if (res.is_null())
        return {};

    VERIFY(git_repo_exists(repository_root));
    return adopt_ref(*new GitRepo(repository_root));
}

Vector<LexicalPath> GitRepo::unstaged_files() const
{
    auto modified = modified_files();
    auto untracked = untracked_files();
    modified.extend(move(untracked));
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
