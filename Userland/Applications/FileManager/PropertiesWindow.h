/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Queue.h>
#include <LibGUI/Button.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/FileSystemModel.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextBox.h>
#include <LibThreading/BackgroundAction.h>

class PropertiesWindow final : public GUI::Window {
    C_OBJECT(PropertiesWindow);

public:
    static ErrorOr<NonnullRefPtr<PropertiesWindow>> try_create(ByteString const& path, bool disable_rename, Window* parent = nullptr);
    virtual ~PropertiesWindow() override = default;

    virtual void close() final;

private:
    PropertiesWindow(ByteString const& path, Window* parent = nullptr);
    ErrorOr<void> create_widgets(bool disable_rename);
    ErrorOr<void> create_general_tab(GUI::TabWidget&, bool disable_rename);
    ErrorOr<void> create_file_type_specific_tabs(GUI::TabWidget&);
    ErrorOr<void> create_archive_tab(GUI::TabWidget&, NonnullOwnPtr<Core::MappedFile>);
    ErrorOr<void> create_audio_tab(GUI::TabWidget&, NonnullOwnPtr<Core::MappedFile>);
    ErrorOr<void> create_font_tab(GUI::TabWidget&, NonnullOwnPtr<Core::MappedFile>, StringView mime_type);
    ErrorOr<void> create_image_tab(GUI::TabWidget&, NonnullOwnPtr<Core::MappedFile>, StringView mime_type);
    ErrorOr<void> create_pdf_tab(GUI::TabWidget&, NonnullOwnPtr<Core::MappedFile>);

    struct PermissionMasks {
        mode_t read;
        mode_t write;
        mode_t execute;
    };

    class DirectoryStatisticsCalculator final : public RefCounted<DirectoryStatisticsCalculator> {
    public:
        DirectoryStatisticsCalculator(ByteString path);
        void start();
        void stop();
        Function<void(off_t total_size_in_bytes, size_t file_count, size_t directory_count)> on_update;

    private:
        off_t m_total_size_in_bytes { 0 };
        size_t m_file_count { 0 };
        size_t m_directory_count { 0 };
        RefPtr<Threading::BackgroundAction<int>> m_background_action;
        Queue<ByteString> m_work_queue;
    };

    static StringView const get_description(mode_t const mode)
    {
        if (S_ISREG(mode))
            return "File"sv;
        if (S_ISDIR(mode))
            return "Directory"sv;
        if (S_ISLNK(mode))
            return "Symbolic link"sv;
        if (S_ISCHR(mode))
            return "Character device"sv;
        if (S_ISBLK(mode))
            return "Block device"sv;
        if (S_ISFIFO(mode))
            return "FIFO (named pipe)"sv;
        if (S_ISSOCK(mode))
            return "Socket"sv;
        if (mode & S_IXUSR)
            return "Executable"sv;

        return "Unknown"sv;
    }

    static GUI::Button& make_button(String, GUI::Widget& parent);
    ErrorOr<void> setup_permission_checkboxes(GUI::CheckBox& box_read, GUI::CheckBox& box_write, GUI::CheckBox& box_execute, PermissionMasks masks, mode_t mode);
    void permission_changed(mode_t mask, bool set);
    bool apply_changes();
    void update();
    ByteString make_full_path(ByteString const& name);

    RefPtr<GUI::Button> m_apply_button;
    RefPtr<GUI::TextBox> m_name_box;
    RefPtr<GUI::ImageWidget> m_icon;
    RefPtr<GUI::Label> m_size_label;
    RefPtr<DirectoryStatisticsCalculator> m_directory_statistics_calculator;
    RefPtr<GUI::Action> m_on_escape;
    ByteString m_name;
    ByteString m_parent_path;
    ByteString m_path;
    mode_t m_mode { 0 };
    mode_t m_old_mode { 0 };
    bool m_permissions_dirty { false };
    bool m_name_dirty { false };
};
