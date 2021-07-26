/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2021, Conor Byrne <cbyrneee@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>

#pragma once

namespace HackStudio {

class GitRepo {
public:
    static GitRepo& the();
    static void initialize(LexicalPath root);

    bool repo_exists(bool cached);
    bool is_git_installed();
    bool initialize_repository();

    bool stage(LexicalPath const& file);
    bool stage_files(Vector<LexicalPath> const& files);
    bool unstage(LexicalPath const& file);
    bool unstage_files(Vector<LexicalPath> const& file);

    bool commit(String const& message);
    bool is_tracked(LexicalPath const& file);

    Optional<String> original_file_content(LexicalPath const& file);
    Optional<String> unstaged_diff(LexicalPath const& file);

    Vector<LexicalPath> staged_files();
    Vector<LexicalPath> modified_files();
    Vector<LexicalPath> untracked_files();
    Vector<LexicalPath> unstaged_files();

private:
    explicit GitRepo(LexicalPath& root)
        : m_root(root)
    {
    }

    Vector<LexicalPath> parse_files_list(const String& raw_result);
    String execute_git_command(const Vector<String>& command_parts, const LexicalPath& chdir);

    LexicalPath m_root;
    bool m_repo_exists { false };
};

}
