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
    static ErrorOr<NonnullRefPtr<PropertiesWindow>> try_create(String const& path, bool disable_rename, Window* parent = nullptr);
    virtual ~PropertiesWindow() override = default;

    virtual void close() final;

private:
    PropertiesWindow(String const& path, Window* parent = nullptr);
    ErrorOr<void> create_widgets(bool disable_rename);

    struct PropertyValuePair {
        String property;
        String value;
        Optional<URL> link = {};
    };

    struct PermissionMasks {
        mode_t read;
        mode_t write;
        mode_t execute;
    };

    class DirectoryStatisticsCalculator final : public RefCounted<DirectoryStatisticsCalculator> {
    public:
        DirectoryStatisticsCalculator(String path);
        void start();
        void stop();
        Function<void(off_t total_size_in_bytes, size_t file_count, size_t directory_count)> on_update;

    private:
        off_t m_total_size_in_bytes { 0 };
        size_t m_file_count { 0 };
        size_t m_directory_count { 0 };
        RefPtr<Threading::BackgroundAction<int>> m_background_action;
        Queue<String> m_work_queue;
    };

    static String const get_description(mode_t const mode)
    {
        if (S_ISREG(mode))
            return String::from_utf8_short_string("File"sv);
        if (S_ISDIR(mode))
            return String::from_utf8("Directory"sv).release_value_but_fixme_should_propagate_errors();
        if (S_ISLNK(mode))
            return String::from_utf8("Symbolic link"sv).release_value_but_fixme_should_propagate_errors();
        if (S_ISCHR(mode))
            return String::from_utf8("Character device"sv).release_value_but_fixme_should_propagate_errors();
        if (S_ISBLK(mode))
            return String::from_utf8("Block device"sv).release_value_but_fixme_should_propagate_errors();
        if (S_ISFIFO(mode))
            return String::from_utf8("FIFO (named pipe)"sv).release_value_but_fixme_should_propagate_errors();
        if (S_ISSOCK(mode))
            return String::from_utf8("Socket"sv).release_value_but_fixme_should_propagate_errors();
        if (mode & S_IXUSR)
            return String::from_utf8("Executable"sv).release_value_but_fixme_should_propagate_errors();

        return String::from_utf8_short_string("Unknown"sv);
    }

    static ErrorOr<NonnullRefPtr<GUI::Button>> make_button(String, GUI::Widget& parent);
    ErrorOr<void> setup_permission_checkboxes(GUI::CheckBox& box_read, GUI::CheckBox& box_write, GUI::CheckBox& box_execute, PermissionMasks masks, mode_t mode);
    void permission_changed(mode_t mask, bool set);
    bool apply_changes();
    void update();
    String make_full_path(String const& name);

    RefPtr<GUI::Button> m_apply_button;
    RefPtr<GUI::TextBox> m_name_box;
    RefPtr<GUI::ImageWidget> m_icon;
    RefPtr<GUI::Label> m_size_label;
    RefPtr<DirectoryStatisticsCalculator> m_directory_statistics_calculator;
    String m_name;
    String m_parent_path;
    String m_path;
    mode_t m_mode { 0 };
    mode_t m_old_mode { 0 };
    bool m_permissions_dirty { false };
    bool m_name_dirty { false };
};
