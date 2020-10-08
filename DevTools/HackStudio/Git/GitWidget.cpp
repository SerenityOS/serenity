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
#include <LibCore/File.h>
#include <LibDiff/Format.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <stdio.h>

namespace HackStudio {

GitWidget::GitWidget(const LexicalPath& repo_root)
    : m_repo_root(repo_root)
{
    set_layout<GUI::HorizontalBoxLayout>();

    auto& unstaged = add<GUI::Widget>();
    unstaged.set_layout<GUI::VerticalBoxLayout>();
    auto& unstaged_header = unstaged.add<GUI::Widget>();
    unstaged_header.set_layout<GUI::HorizontalBoxLayout>();

    auto& refresh_button = unstaged_header.add<GUI::Button>();
    refresh_button.set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/reload.png"));
    refresh_button.set_preferred_size({ 16, 16 });
    refresh_button.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    refresh_button.set_tooltip("refresh");
    refresh_button.on_click = [this](int) { refresh(); };

    auto& unstaged_label = unstaged_header.add<GUI::Label>();
    unstaged_label.set_text("Unstaged");

    unstaged_header.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    unstaged_header.set_preferred_size(0, 20);
    m_unstaged_files = unstaged.add<GitFilesView>(
        [this](const auto& file) { stage_file(file); },
        Gfx::Bitmap::load_from_file("/res/icons/16x16/plus.png").release_nonnull());
    m_unstaged_files->on_selection = [this](const GUI::ModelIndex& index) {
        const auto& selected = index.data().as_string();
        show_diff(LexicalPath(selected));
    };

    auto& staged = add<GUI::Widget>();
    staged.set_layout<GUI::VerticalBoxLayout>();

    auto& staged_header = staged.add<GUI::Widget>();
    staged_header.set_layout<GUI::HorizontalBoxLayout>();

    auto& commit_button = staged_header.add<GUI::Button>();
    commit_button.set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/commit.png"));
    commit_button.set_preferred_size({ 16, 16 });
    commit_button.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    commit_button.set_tooltip("commit");
    commit_button.on_click = [this](int) { commit(); };

    auto& staged_label = staged_header.add<GUI::Label>();
    staged_label.set_text("Staged");

    staged_header.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    staged_header.set_preferred_size(0, 20);
    m_staged_files = staged.add<GitFilesView>(
        [this](const auto& file) { unstage_file(file); },
        Gfx::Bitmap::load_from_file("/res/icons/16x16/minus.png").release_nonnull());
}

bool GitWidget::initialize()
{
    auto result = GitRepo::try_to_create(m_repo_root);
    switch (result.type) {
    case GitRepo::CreateResult::Type::Success:
        m_git_repo = result.repo;
        return true;
    case GitRepo::CreateResult::Type::GitProgramNotFound:
        GUI::MessageBox::show(window(), "Please install the Git port", "Error", GUI::MessageBox::Type::Error);
        return false;
    case GitRepo::CreateResult::Type::NoGitRepo: {
        auto decision = GUI::MessageBox::show(window(), "Create git repository?", "Git", GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
        if (decision != GUI::Dialog::ExecResult::ExecYes)
            return false;
        m_git_repo = GitRepo::initialize_repository(m_repo_root);
        return true;
    }
    default:
        ASSERT_NOT_REACHED();
    }
}

bool GitWidget::initialize_if_needed()
{
    if (initialized())
        return true;

    return initialize();
}

void GitWidget::refresh()
{
    if (!initialize_if_needed()) {
        dbgln("GitWidget initialization failed");
        return;
    }

    ASSERT(!m_git_repo.is_null());

    m_unstaged_files->set_model(GitFilesModel::create(m_git_repo->unstaged_files()));
    m_staged_files->set_model(GitFilesModel::create(m_git_repo->staged_files()));
}

void GitWidget::stage_file(const LexicalPath& file)
{
    dbgln("staging: {}", file);
    bool rc = m_git_repo->stage(file);
    ASSERT(rc);
    refresh();
}

void GitWidget::unstage_file(const LexicalPath& file)
{
    dbgln("unstaging: {}", file);
    bool rc = m_git_repo->unstage(file);
    ASSERT(rc);
    refresh();
}

void GitWidget::commit()
{
    String message;
    auto res = GUI::InputBox::show(message, window(), "Commit message:", "Commit");
    if (res != GUI::InputBox::ExecOK || message.is_empty())
        return;
    dbgln("commit message: {}", message);
    m_git_repo->commit(message);
    refresh();
}

void GitWidget::set_view_diff_callback(ViewDiffCallback callback)
{
    m_view_diff_callback = move(callback);
}

void GitWidget::show_diff(const LexicalPath& file_path)
{
    if (!m_git_repo->is_tracked(file_path)) {
        auto file = Core::File::construct(file_path.string());
        if (!file->open(Core::IODevice::ReadOnly)) {
            perror("open");
            ASSERT_NOT_REACHED();
        }

        auto content = file->read_all();
        String content_string((char*)content.data(), content.size());
        m_view_diff_callback("", Diff::generate_only_additions(content_string));
        return;
    }
    const auto& original_content = m_git_repo->original_file_content(file_path);
    const auto& diff = m_git_repo->unstaged_diff(file_path);
    ASSERT(original_content.has_value() && diff.has_value());
    m_view_diff_callback(original_content.value(), diff.value());
}
}
