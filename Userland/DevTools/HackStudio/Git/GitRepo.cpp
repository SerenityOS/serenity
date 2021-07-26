/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2021, Conor Byrne <cbyrneee@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GitRepo.h"
#include <LibCore/Command.h>

namespace HackStudio {

static GitRepo* s_the;

GitRepo& GitRepo::the()
{
    VERIFY(s_the);
    return *s_the;
}

void GitRepo::initialize(LexicalPath root)
{
    s_the = new GitRepo(root);
}

bool GitRepo::repo_exists(bool cached)
{
    if (!cached)
        m_repo_exists = !execute_git_command({ "status" }, m_root).is_null();

    return m_repo_exists;
}

bool GitRepo::is_git_installed()
{
    return !execute_git_command({ "--help" }, LexicalPath("/")).is_null();
}

bool GitRepo::initialize_repository()
{
    return !execute_git_command({ "init" }, m_root).is_null();
}

bool GitRepo::stage(LexicalPath const& file)
{
    return !execute_git_command({ "add", file.string() }, m_root).is_null();
}

bool GitRepo::unstage(LexicalPath const& file)
{
    return !execute_git_command({ "reset", "HEAD", "--", file.string() }, m_root).is_null();
}

bool GitRepo::commit(String const& message)
{
    return !execute_git_command({ "commit", "-m", message }, m_root).is_null();
}

bool GitRepo::is_tracked(LexicalPath const& file)
{
    auto res = execute_git_command({ "ls-files", file.string() }, m_root);
    if (res.is_null())
        return false;

    return !res.is_empty();
}

Vector<LexicalPath> GitRepo::staged_files()
{
    auto raw_result = execute_git_command({ "diff", "--cached", "--name-only" }, m_root);
    if (raw_result.is_null())
        return {};

    return parse_files_list(raw_result);
}

Vector<LexicalPath> GitRepo::unstaged_files()
{
    auto modified = modified_files();
    auto untracked = untracked_files();
    modified.extend(move(untracked));
    return modified;
}

Vector<LexicalPath> GitRepo::modified_files()
{
    auto raw_result = execute_git_command({ "ls-files", "--modified", "--exclude-standard" }, m_root);
    if (raw_result.is_null())
        return {};

    return parse_files_list(raw_result);
}

Vector<LexicalPath> GitRepo::untracked_files()
{
    auto raw_result = execute_git_command({ "ls-files", "--others", "--exclude-standard" }, m_root);
    if (raw_result.is_null())
        return {};

    return parse_files_list(raw_result);
}

Vector<LexicalPath> GitRepo::parse_files_list(String const& raw_result)
{
    auto lines = raw_result.split('\n');

    Vector<LexicalPath> files;
    for (auto const& line : lines) {
        files.empend(line);
    }

    return files;
}

Optional<String> GitRepo::original_file_content(LexicalPath const& file)
{
    return execute_git_command({ "show", String::formatted("HEAD:{}", file) }, m_root);
}

Optional<String> GitRepo::unstaged_diff(LexicalPath const& file)
{
    return execute_git_command({ "diff", file.string().characters() }, m_root);
}

String GitRepo::execute_git_command(Vector<String> const& command_parts, LexicalPath const& chdir)
{
    return Core::command("git", command_parts, chdir);
}

bool GitRepo::stage_files(Vector<LexicalPath> const& files)
{
    Vector<String> arguments = { String("add") };

    for (auto& file : files) {
        arguments.append(file.string());
    }

    return !execute_git_command(arguments, m_root).is_null();
}

bool GitRepo::unstage_files(Vector<LexicalPath> const& files)
{
    Vector<String> arguments = { String("reset"), String("HEAD"), String("--") };

    for (auto& file : files) {
        arguments.append(file.string());
    }

    return !execute_git_command(arguments, m_root).is_null();
}

}
