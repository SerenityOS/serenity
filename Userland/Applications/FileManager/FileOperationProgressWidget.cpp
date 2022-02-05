/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileOperationProgressWidget.h"
#include "FileUtils.h"
#include <Applications/FileManager/FileOperationProgressGML.h>
#include <LibCore/File.h>
#include <LibCore/Notifier.h>
#include <LibGUI/Button.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Progressbar.h>
#include <LibGUI/Window.h>

namespace FileManager {

FileOperationProgressWidget::FileOperationProgressWidget(FileOperation operation, NonnullRefPtr<Core::File> helper_pipe)
    : m_operation(operation)
    , m_helper_pipe(move(helper_pipe))
{
    load_from_gml(file_operation_progress_gml);

    auto& button = *find_descendant_of_type_named<GUI::Button>("button");

    auto& file_copy_animation = *find_descendant_of_type_named<GUI::ImageWidget>("file_copy_animation");
    file_copy_animation.load_from_file("/res/graphics/file-flying-animation.gif");
    file_copy_animation.animate();

    auto& source_folder_icon = *find_descendant_of_type_named<GUI::ImageWidget>("source_folder_icon");
    source_folder_icon.load_from_file("/res/icons/32x32/filetype-folder-open.png");

    auto& destination_folder_icon = *find_descendant_of_type_named<GUI::ImageWidget>("destination_folder_icon");

    switch (m_operation) {
    case FileOperation::Delete:
        destination_folder_icon.load_from_file("/res/icons/32x32/recycle-bin.png");
        break;
    default:
        destination_folder_icon.load_from_file("/res/icons/32x32/filetype-folder-open.png");
        break;
    }

    button.on_click = [this](auto) {
        close_pipe();
        window()->close();
    };

    auto& files_copied_label = *find_descendant_of_type_named<GUI::Label>("files_copied_label");
    auto& current_file_action_label = *find_descendant_of_type_named<GUI::Label>("current_file_action_label");

    switch (m_operation) {
    case FileOperation::Copy:
        files_copied_label.set_text("Copying files...");
        current_file_action_label.set_text("Copying: ");
        break;
    case FileOperation::Move:
        files_copied_label.set_text("Moving files...");
        current_file_action_label.set_text("Moving: ");
        break;
    case FileOperation::Delete:
        files_copied_label.set_text("Deleting files...");
        current_file_action_label.set_text("Deleting: ");
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    m_notifier = Core::Notifier::construct(m_helper_pipe->fd(), Core::Notifier::Read);
    m_notifier->on_ready_to_read = [this] {
        auto line = m_helper_pipe->read_line();
        if (line.is_null()) {
            did_error("Read from pipe returned null."sv);
            return;
        }

        auto parts = line.split_view(' ');
        VERIFY(!parts.is_empty());

        if (parts[0] == "ERROR"sv) {
            did_error(line.substring_view(6));
            return;
        }

        if (parts[0] == "WARN"sv) {
            did_error(line.substring_view(5));
            return;
        }

        if (parts[0] == "FINISH"sv) {
            did_finish();
            return;
        }

        if (parts[0] == "PROGRESS"sv) {
            VERIFY(parts.size() >= 8);
            did_progress(
                parts[3].to_uint().value_or(0),
                parts[4].to_uint().value_or(0),
                parts[1].to_uint().value_or(0),
                parts[2].to_uint().value_or(0),
                parts[5].to_uint().value_or(0),
                parts[6].to_uint().value_or(0),
                parts[7]);
        }
    };

    m_elapsed_timer.start();
}

FileOperationProgressWidget::~FileOperationProgressWidget()
{
    close_pipe();
}

void FileOperationProgressWidget::did_finish()
{
    close_pipe();
    window()->close();
}

void FileOperationProgressWidget::did_error(StringView message)
{
    // FIXME: Communicate more with the user about errors.
    close_pipe();
    GUI::MessageBox::show(window(), String::formatted("An error occurred: {}", message), "Error", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK);
    window()->close();
}

String FileOperationProgressWidget::estimate_time(off_t bytes_done, off_t total_byte_count)
{
    int elapsed = m_elapsed_timer.elapsed() / 1000;

    if (bytes_done == 0 || elapsed < 3)
        return "Estimating...";

    off_t bytes_left = total_byte_count - bytes_done;
    int seconds_remaining = (bytes_left * elapsed) / bytes_done;

    if (seconds_remaining < 30)
        return String::formatted("{} seconds", 5 + seconds_remaining - seconds_remaining % 5);
    if (seconds_remaining < 60)
        return "About a minute";
    if (seconds_remaining < 90)
        return "Over a minute";
    if (seconds_remaining < 120)
        return "Less than two minutes";

    time_t minutes_remaining = seconds_remaining / 60;
    seconds_remaining %= 60;

    if (minutes_remaining < 60) {
        if (seconds_remaining < 30)
            return String::formatted("About {} minutes", minutes_remaining);
        return String::formatted("Over {} minutes", minutes_remaining);
    }

    time_t hours_remaining = minutes_remaining / 60;
    minutes_remaining %= 60;

    return String::formatted("{} hours and {} minutes", hours_remaining, minutes_remaining);
}

void FileOperationProgressWidget::did_progress(off_t bytes_done, off_t total_byte_count, size_t files_done, size_t total_file_count, [[maybe_unused]] off_t current_file_done, [[maybe_unused]] off_t current_file_size, StringView current_filename)
{
    auto& files_copied_label = *find_descendant_of_type_named<GUI::Label>("files_copied_label");
    auto& current_file_label = *find_descendant_of_type_named<GUI::Label>("current_file_label");
    auto& overall_progressbar = *find_descendant_of_type_named<GUI::Progressbar>("overall_progressbar");
    auto& estimated_time_label = *find_descendant_of_type_named<GUI::Label>("estimated_time_label");

    current_file_label.set_text(current_filename);

    switch (m_operation) {
    case FileOperation::Copy:
        files_copied_label.set_text(String::formatted("Copying file {} of {}", files_done, total_file_count));
        break;
    case FileOperation::Move:
        files_copied_label.set_text(String::formatted("Moving file {} of {}", files_done, total_file_count));
        break;
    case FileOperation::Delete:
        files_copied_label.set_text(String::formatted("Deleting file {} of {}", files_done, total_file_count));
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    estimated_time_label.set_text(estimate_time(bytes_done, total_byte_count));

    if (total_byte_count) {
        window()->set_progress(100.0f * bytes_done / total_byte_count);
        overall_progressbar.set_max(total_byte_count);
        overall_progressbar.set_value(bytes_done);
    }
}

void FileOperationProgressWidget::close_pipe()
{
    if (!m_helper_pipe)
        return;
    m_helper_pipe = nullptr;
    if (m_notifier) {
        m_notifier->set_enabled(false);
        m_notifier->on_ready_to_read = nullptr;
    }
    m_notifier = nullptr;
}

}
