/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GitRepo.h"
#include <LibCore/Command.h>

namespace HackStudio {

GitRepo::CreateResult GitRepo::try_to_create(DeprecatedString const& repository_root)
{
    if (!git_is_installed()) {
        return { CreateResult::Type::GitProgramNotFound, nullptr };
    }
    if (!git_repo_exists(repository_root)) {
        return { CreateResult::Type::NoGitRepo, nullptr };
    }

    return { CreateResult::Type::Success, adopt_ref(*new GitRepo(repository_root)) };
}

RefPtr<GitRepo> GitRepo::initialize_repository(DeprecatedString const& repository_root)
{
    auto res = command_wrapper({ "init" }, repository_root);
    if (res.is_null())
        return {};

    VERIFY(git_repo_exists(repository_root));
    return adopt_ref(*new GitRepo(repository_root));
}

Vector<DeprecatedString> GitRepo::unstaged_files() const
{
    auto modified = modified_files();
    auto untracked = untracked_files();
    modified.extend(move(untracked));
    return modified;
}
//
Vector<DeprecatedString> GitRepo::staged_files() const
{
    auto raw_result = command({ "diff", "--cached", "--name-only" });
    if (raw_result.is_null())
        return {};
    return parse_files_list(raw_result);
}

Vector<DeprecatedString> GitRepo::modified_files() const
{
    auto raw_result = command({ "ls-files", "--modified", "--exclude-standard" });
    if (raw_result.is_null())
        return {};
    return parse_files_list(raw_result);
}

Vector<DeprecatedString> GitRepo::untracked_files() const
{
    auto raw_result = command({ "ls-files", "--others", "--exclude-standard" });
    if (raw_result.is_null())
        return {};
    return parse_files_list(raw_result);
}

Vector<DeprecatedString> GitRepo::parse_files_list(DeprecatedString const& raw_result)
{
    auto lines = raw_result.split('\n');
    Vector<DeprecatedString> files;
    for (auto const& line : lines) {
        files.empend(line);
    }
    return files;
}

DeprecatedString GitRepo::command(Vector<DeprecatedString> const& command_parts) const
{
    return command_wrapper(command_parts, m_repository_root);
}

DeprecatedString GitRepo::command_wrapper(Vector<DeprecatedString> const& command_parts, DeprecatedString const& chdir)
{
    auto const result = Core::command("git", command_parts, LexicalPath(chdir));
    if (result.is_error() || result.value().exit_code != 0)
        return {};
    return DeprecatedString(result.value().output.bytes());
}

bool GitRepo::git_is_installed()
{
    return !command_wrapper({ "--help" }, "/").is_null();
}

bool GitRepo::git_repo_exists(DeprecatedString const& repo_root)
{
    return !command_wrapper({ "status" }, repo_root).is_null();
}

bool GitRepo::stage(DeprecatedString const& file)
{
    return !command({ "add", file }).is_null();
}

bool GitRepo::unstage(DeprecatedString const& file)
{
    return !command({ "reset", "HEAD", "--", file }).is_null();
}

bool GitRepo::commit(DeprecatedString const& message)
{
    return !command({ "commit", "-m", message }).is_null();
}

Optional<DeprecatedString> GitRepo::original_file_content(DeprecatedString const& file) const
{
    return command({ "show", DeprecatedString::formatted("HEAD:{}", file) });
}

Optional<DeprecatedString> GitRepo::unstaged_diff(DeprecatedString const& file) const
{
    return command({ "diff", file.characters() });
}

bool GitRepo::is_tracked(DeprecatedString const& file) const
{
    auto res = command({ "ls-files", file });
    if (res.is_null())
        return false;

    return !res.is_empty();
}

}
