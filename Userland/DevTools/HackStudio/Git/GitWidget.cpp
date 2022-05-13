/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GitWidget.h"
#include "../Dialogs/Git/GitCommitDialog.h"
#include "GitFilesModel.h"
#include <LibCore/File.h>
#include <LibDiff/Format.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <stdio.h>

namespace HackStudio {

GitWidget::GitWidget(String const& repo_root)
    : m_repo_root(repo_root)
{
    set_layout<GUI::HorizontalBoxLayout>();

    auto& unstaged = add<GUI::Widget>();
    unstaged.set_layout<GUI::VerticalBoxLayout>();
    auto& unstaged_header = unstaged.add<GUI::Widget>();
    unstaged_header.set_layout<GUI::HorizontalBoxLayout>();

    auto& refresh_button = unstaged_header.add<GUI::Button>();
    refresh_button.set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/reload.png").release_value_but_fixme_should_propagate_errors());
    refresh_button.set_fixed_size(16, 16);
    refresh_button.set_tooltip("refresh");
    refresh_button.on_click = [this](int) { refresh(); };

    auto& unstaged_label = unstaged_header.add<GUI::Label>();
    unstaged_label.set_text("Unstaged");

    unstaged_header.set_fixed_height(20);
    m_unstaged_files = unstaged.add<GitFilesView>(
        [this](auto const& file) { stage_file(file); },
        Gfx::Bitmap::try_load_from_file("/res/icons/16x16/plus.png").release_value_but_fixme_should_propagate_errors());
    m_unstaged_files->on_selection_change = [this] {
        const auto& index = m_unstaged_files->selection().first();
        if (!index.is_valid())
            return;

        const auto& selected = index.data().as_string();
        show_diff(selected);
    };

    auto& staged = add<GUI::Widget>();
    staged.set_layout<GUI::VerticalBoxLayout>();

    auto& staged_header = staged.add<GUI::Widget>();
    staged_header.set_layout<GUI::HorizontalBoxLayout>();

    auto& commit_button = staged_header.add<GUI::Button>();
    commit_button.set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/commit.png").release_value_but_fixme_should_propagate_errors());
    commit_button.set_fixed_size(16, 16);
    commit_button.set_tooltip("commit");
    commit_button.on_click = [this](int) { commit(); };

    auto& staged_label = staged_header.add<GUI::Label>();
    staged_label.set_text("Staged");

    staged_header.set_fixed_height(20);
    m_staged_files = staged.add<GitFilesView>(
        [this](auto const& file) { unstage_file(file); },
        Gfx::Bitmap::try_load_from_file("/res/icons/16x16/minus.png").release_value_but_fixme_should_propagate_errors());
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
        if (decision != GUI::Dialog::ExecResult::Yes)
            return false;
        m_git_repo = GitRepo::initialize_repository(m_repo_root);
        return true;
    }
    default:
        VERIFY_NOT_REACHED();
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

    VERIFY(!m_git_repo.is_null());

    m_unstaged_files->set_model(GitFilesModel::create(m_git_repo->unstaged_files()));
    m_staged_files->set_model(GitFilesModel::create(m_git_repo->staged_files()));
}

void GitWidget::stage_file(String const& file)
{
    dbgln("staging: {}", file);
    bool rc = m_git_repo->stage(file);
    VERIFY(rc);
    refresh();
}

void GitWidget::unstage_file(String const& file)
{
    dbgln("unstaging: {}", file);
    bool rc = m_git_repo->unstage(file);
    VERIFY(rc);
    refresh();
}

void GitWidget::commit()
{
    if (m_git_repo.is_null()) {
        GUI::MessageBox::show(window(), "There is no git repository to commit to!", "Error", GUI::MessageBox::Type::Error);
        return;
    }

    auto dialog = GitCommitDialog::construct(window());
    dialog->on_commit = [this](auto& message) {
        m_git_repo->commit(message);
        refresh();
    };
    dialog->exec();
}

void GitWidget::set_view_diff_callback(ViewDiffCallback callback)
{
    m_view_diff_callback = move(callback);
}

void GitWidget::show_diff(String const& file_path)
{
    if (!m_git_repo->is_tracked(file_path)) {
        auto file = Core::File::construct(file_path);
        if (!file->open(Core::OpenMode::ReadOnly)) {
            perror("open");
            VERIFY_NOT_REACHED();
        }

        auto content = file->read_all();
        String content_string((char*)content.data(), content.size());
        m_view_diff_callback("", Diff::generate_only_additions(content_string));
        return;
    }
    auto const& original_content = m_git_repo->original_file_content(file_path);
    auto const& diff = m_git_repo->unstaged_diff(file_path);
    VERIFY(original_content.has_value() && diff.has_value());
    m_view_diff_callback(original_content.value(), diff.value());
}

void GitWidget::change_repo(String const& repo_root)
{
    m_repo_root = repo_root;
    m_git_repo = nullptr;
    m_unstaged_files->set_model(nullptr);
    m_staged_files->set_model(nullptr);
}
}
