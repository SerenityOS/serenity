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

#include "GitWidget.h"
#include "GitFilesModel.h"
#include <AK/LogStream.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>

namespace HackStudio {

void GitWidget::stage_file(const LexicalPath& file)
{
    dbg() << "staging: " << file.string();
    bool rc = m_git_repo->stage(file);
    ASSERT(rc);
    refresh();
}

void GitWidget::unstage_file(const LexicalPath& file)
{
    dbg() << "unstaging: " << file.string();
    bool rc = m_git_repo->unstage(file);
    ASSERT(rc);
    refresh();
}

GitWidget::GitWidget(const LexicalPath& repo_root)
    : m_repo_root(repo_root)
{
    set_layout<GUI::HorizontalBoxLayout>();

    auto& unstaged = add<GUI::Widget>();
    unstaged.set_layout<GUI::VerticalBoxLayout>();
    auto& unstaged_label = unstaged.add<GUI::Label>();
    unstaged_label.set_text("Unstaged");
    unstaged_label.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    unstaged_label.set_preferred_size(0, 20);
    m_unstaged_files = unstaged.add<GitFilesView>(
        [this](const auto& file) { stage_file(file); },
        Gfx::Bitmap::load_from_file("/res/icons/16x16/plus.png").release_nonnull());

    auto& staged = add<GUI::Widget>();
    staged.set_layout<GUI::VerticalBoxLayout>();
    auto& staged_label = staged.add<GUI::Label>();
    staged_label.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    staged_label.set_preferred_size(0, 20);
    staged_label.set_text("Staged");
    m_staged_files = staged.add<GitFilesView>(
        [this](const auto& file) { unstage_file(file); },
        Gfx::Bitmap::load_from_file("/res/icons/16x16/minus.png").release_nonnull());
}

void GitWidget::refresh()
{
    auto result = GitRepo::try_to_create(m_repo_root);
    if (result.type == GitRepo::CreateResult::Type::Success) {
        m_git_repo = result.repo;
    } else if (result.type == GitRepo::CreateResult::Type::GitProgramNotFound) {
        GUI::MessageBox::show(window(), "Please install the Git port", "Error", GUI::MessageBox::Type::Error);
        return;
    } else if (result.type == GitRepo::CreateResult::Type::NoGitRepo) {
        auto decision = GUI::MessageBox::show(window(), "Create git repository?", "Git", GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
        if (decision != GUI::Dialog::ExecResult::ExecYes)
            return;
        m_git_repo = GitRepo::initialize_repository(m_repo_root);
    }

    ASSERT(!m_git_repo.is_null());

    m_unstaged_files->set_model(GitFilesModel::create(m_git_repo->unstaged_files()));
    m_staged_files->set_model(GitFilesModel::create(m_git_repo->staged_files()));
}

};
