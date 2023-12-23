/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Alexander Narsudinov <a.narsudinov@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileOperationProgressWidget.h"
#include "FileUtils.h"
#include <Applications/FileManager/FileOperationProgressGML.h>
#include <LibCore/Notifier.h>
#include <LibGUI/Button.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Progressbar.h>
#include <LibGUI/Window.h>

namespace FileManager {

FileOperationProgressWidget::FileOperationProgressWidget(FileOperation operation, NonnullOwnPtr<Core::InputBufferedFile> helper_pipe, int helper_pipe_fd)
    : m_operation(operation)
    , m_helper_pipe(move(helper_pipe))
{
    load_from_gml(file_operation_progress_gml).release_value_but_fixme_should_propagate_errors();

    auto& button = *find_descendant_of_type_named<GUI::Button>("button");

    auto& file_copy_animation = *find_descendant_of_type_named<GUI::ImageWidget>("file_copy_animation");
    file_copy_animation.load_from_file("/res/graphics/file-flying-animation.gif"sv);
    file_copy_animation.animate();

    auto& source_folder_icon = *find_descendant_of_type_named<GUI::ImageWidget>("source_folder_icon");
    source_folder_icon.load_from_file("/res/icons/32x32/filetype-folder-open.png"sv);

    auto& destination_folder_icon = *find_descendant_of_type_named<GUI::ImageWidget>("destination_folder_icon");

    switch (m_operation) {
    case FileOperation::Delete:
        destination_folder_icon.load_from_file("/res/icons/32x32/recycle-bin.png"sv);
        break;
    default:
        destination_folder_icon.load_from_file("/res/icons/32x32/filetype-folder-open.png"sv);
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
        files_copied_label.set_text("Copying files..."_string);
        current_file_action_label.set_text("Copying: "_string);
        break;
    case FileOperation::Move:
        files_copied_label.set_text("Moving files..."_string);
        current_file_action_label.set_text("Moving: "_string);
        break;
    case FileOperation::Delete:
        files_copied_label.set_text("Deleting files..."_string);
        current_file_action_label.set_text("Deleting: "_string);
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    m_notifier = Core::Notifier::construct(helper_pipe_fd, Core::Notifier::Type::Read);
    m_notifier->on_activation = [this] {
        auto line_buffer_or_error = ByteBuffer::create_zeroed(1 * KiB);
        if (line_buffer_or_error.is_error()) {
            did_error("Failed to allocate ByteBuffer for reading data."sv);
            return;
        }
        auto line_buffer = line_buffer_or_error.release_value();
        auto line_or_error = m_helper_pipe->read_line(line_buffer.bytes());
        if (line_or_error.is_error() || line_or_error.value().is_empty()) {
            did_error("Read from pipe returned null."sv);
            return;
        }

        auto line = line_or_error.release_value();

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
                parts[3].to_number<unsigned>().value_or(0),
                parts[4].to_number<unsigned>().value_or(0),
                parts[1].to_number<unsigned>().value_or(0),
                parts[2].to_number<unsigned>().value_or(0),
                parts[5].to_number<unsigned>().value_or(0),
                parts[6].to_number<unsigned>().value_or(0),
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
    GUI::MessageBox::show(window(), ByteString::formatted("An error occurred: {}", message), "Error"sv, GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK);
    window()->close();
}

ByteString FileOperationProgressWidget::estimate_time(off_t bytes_done, off_t total_byte_count)
{
    i64 const elapsed_seconds = m_elapsed_timer.elapsed_time().to_seconds();

    if (bytes_done == 0 || elapsed_seconds < 3)
        return "Estimating...";

    off_t bytes_left = total_byte_count - bytes_done;
    int seconds_remaining = (bytes_left * elapsed_seconds) / bytes_done;

    if (seconds_remaining < 30)
        return ByteString::formatted("{} seconds", 5 + seconds_remaining - seconds_remaining % 5);
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
            return ByteString::formatted("About {} minutes", minutes_remaining);
        return ByteString::formatted("Over {} minutes", minutes_remaining);
    }

    time_t hours_remaining = minutes_remaining / 60;
    minutes_remaining %= 60;

    return ByteString::formatted("{} hours and {} minutes", hours_remaining, minutes_remaining);
}

void FileOperationProgressWidget::did_progress(off_t bytes_done, off_t total_byte_count, size_t files_done, size_t total_file_count, [[maybe_unused]] off_t current_file_done, [[maybe_unused]] off_t current_file_size, StringView current_filename)
{
    auto& files_copied_label = *find_descendant_of_type_named<GUI::Label>("files_copied_label");
    auto& current_file_label = *find_descendant_of_type_named<GUI::Label>("current_file_label");
    auto& overall_progressbar = *find_descendant_of_type_named<GUI::Progressbar>("overall_progressbar");
    auto& estimated_time_label = *find_descendant_of_type_named<GUI::Label>("estimated_time_label");

    current_file_label.set_text(String::from_utf8(current_filename).release_value_but_fixme_should_propagate_errors());

    switch (m_operation) {
    case FileOperation::Copy:
        files_copied_label.set_text(String::formatted("Copying file {} of {}", files_done, total_file_count).release_value_but_fixme_should_propagate_errors());
        break;
    case FileOperation::Move:
        files_copied_label.set_text(String::formatted("Moving file {} of {}", files_done, total_file_count).release_value_but_fixme_should_propagate_errors());
        break;
    case FileOperation::Delete:
        files_copied_label.set_text(String::formatted("Deleting file {} of {}", files_done, total_file_count).release_value_but_fixme_should_propagate_errors());
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    estimated_time_label.set_text(String::from_byte_string(estimate_time(bytes_done, total_byte_count)).release_value_but_fixme_should_propagate_errors());

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
        m_notifier->on_activation = nullptr;
    }
    m_notifier = nullptr;
}

}
