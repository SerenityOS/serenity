/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GitWidget.h"
#include "GitFilesModel.h"
#include "GitRepo.h"
#include "../Dialogs/Commit/GitCommitDialog.h"
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

GitWidget::GitWidget() {
    set_layout<GUI::HorizontalBoxLayout>();

    auto& unstaged = add<GUI::Widget>();
    unstaged.set_layout<GUI::VerticalBoxLayout>();
    auto& unstaged_header = unstaged.add<GUI::Widget>();
    unstaged_header.set_layout<GUI::HorizontalBoxLayout>();

    auto& refresh_button = unstaged_header.add<GUI::Button>();
    refresh_button.set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/reload.png"));
    refresh_button.set_fixed_size(16, 16);
    refresh_button.set_tooltip("refresh");
    refresh_button.on_click = [this](int) { refresh(); };

    auto& unstaged_label = unstaged_header.add<GUI::Label>();
    unstaged_label.set_text("Unstaged");

    unstaged_header.set_fixed_height(20);
    m_unstaged_files = unstaged.add<GitFilesView>(
        [this](const auto& file) { stage_file(file); },
        Gfx::Bitmap::try_load_from_file("/res/icons/16x16/plus.png").release_nonnull());
    m_unstaged_files->on_selection_change = [this] {
        const auto& index = m_unstaged_files->selection().first();
        const auto& selected = index.data().as_string();
        show_diff(LexicalPath(selected));
    };

    auto& staged = add<GUI::Widget>();
    staged.set_layout<GUI::VerticalBoxLayout>();

    auto& staged_header = staged.add<GUI::Widget>();
    staged_header.set_layout<GUI::HorizontalBoxLayout>();

    m_commit_button = staged_header.add<GUI::Button>();
    m_commit_button->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/commit.png"));
    m_commit_button->set_fixed_size(16, 16);
    m_commit_button->set_tooltip("Commit");
    m_commit_button->on_click = [this](int) { commit(); };
    m_commit_button->set_enabled(GitRepo::the().repo_exists(true) && !GitRepo::the().staged_files().is_empty());

    auto& staged_label = staged_header.add<GUI::Label>();
    staged_label.set_text("Staged");

    staged_header.set_fixed_height(20);
    m_staged_files = staged.add<GitFilesView>(
        [this](const auto& file) { unstage_file(file); },
        Gfx::Bitmap::try_load_from_file("/res/icons/16x16/minus.png").release_nonnull());
}

bool GitWidget::initialize()
{
    if (!GitRepo::the().is_git_installed()) {
        GUI::MessageBox::show(window(), "Please install the Git port", "Error", GUI::MessageBox::Type::Error);
        return false;
    }

    if (!GitRepo::the().repo_exists(false)) {
        auto decision = GUI::MessageBox::show(window(), "Create git repository?", "Git", GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
        if (decision != GUI::Dialog::ExecResult::ExecYes)
            return false;

        auto result = GitRepo::the().initialize_repository();
        if (!result) {
            GUI::MessageBox::show(window(), "Failed to create the git repository", "Error", GUI::MessageBox::Type::Error);
            return false;
        }

        return true;
    }

    return true;
}

bool GitWidget::initialize_if_needed()
{
    if (GitRepo::the().repo_exists(false))
        return true;

    return initialize();
}

void GitWidget::refresh()
{
    if (!initialize_if_needed()) {
        dbgln("GitWidget initialization failed");
        return;
    }

    VERIFY(GitRepo::the().repo_exists(false));

    if (on_refresh) on_refresh();

    m_commit_button->set_enabled(GitRepo::the().repo_exists(true) && !GitRepo::the().staged_files().is_empty());
    m_unstaged_files->set_model(GitFilesModel::create(GitRepo::the().unstaged_files()));
    m_staged_files->set_model(GitFilesModel::create(GitRepo::the().staged_files()));
}

void GitWidget::stage_file(const LexicalPath& file)
{
    dbgln("staging: {}", file);
    bool rc = GitRepo::the().stage(file);
    VERIFY(rc);
    refresh();
}

void GitWidget::unstage_file(const LexicalPath& file)
{
    dbgln("unstaging: {}", file);
    bool rc = GitRepo::the().unstage(file);
    VERIFY(rc);
    refresh();
}

void GitWidget::commit()
{
    if (GitRepo::the().repo_exists(true)) {
        auto dialog = GitCommitDialog::construct(window());

        dialog->on_commit = [this](auto& message) {
            GitRepo::the().commit(message);
            refresh();
        };
        
        dialog->exec();
    } else {
        GUI::MessageBox::show(window(), "There is no repository to commit to!", "Error", GUI::MessageBox::Type::Error);
    }
}

void GitWidget::set_view_diff_callback(ViewDiffCallback callback)
{
    m_view_diff_callback = move(callback);
}

void GitWidget::show_diff(const LexicalPath& file_path)
{
    if (!GitRepo::the().is_tracked(file_path)) {
        auto file = Core::File::construct(file_path.string());
        if (!file->open(Core::OpenMode::ReadOnly)) {
            perror("open");
            VERIFY_NOT_REACHED();
        }

        auto content = file->read_all();
        String content_string((char*)content.data(), content.size());
        m_view_diff_callback("", Diff::generate_only_additions(content_string));
        return;
    }
    const auto& original_content = GitRepo::the().original_file_content(file_path);
    const auto& diff = GitRepo::the().unstaged_diff(file_path);
    VERIFY(original_content.has_value() && diff.has_value());
    m_view_diff_callback(original_content.value(), diff.value());
}
}
