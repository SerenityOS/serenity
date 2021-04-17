/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

#include "FileOperationProgressWidget.h"
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

FileOperationProgressWidget::FileOperationProgressWidget(NonnullRefPtr<Core::File> helper_pipe)
    : m_helper_pipe(move(helper_pipe))
{
    load_from_gml(file_operation_progress_gml);

    auto& button = *find_descendant_of_type_named<GUI::Button>("button");

    auto& file_copy_animation = *find_descendant_of_type_named<GUI::ImageWidget>("file_copy_animation");
    file_copy_animation.load_from_file("/res/graphics/file-flying-animation.gif");
    file_copy_animation.animate();

    auto& source_folder_icon = *find_descendant_of_type_named<GUI::ImageWidget>("source_folder_icon");
    source_folder_icon.load_from_file("/res/icons/32x32/filetype-folder-open.png");

    auto& destination_folder_icon = *find_descendant_of_type_named<GUI::ImageWidget>("destination_folder_icon");
    destination_folder_icon.load_from_file("/res/icons/32x32/filetype-folder-open.png");

    button.on_click = [this] {
        close_pipe();
        window()->close();
    };

    m_notifier = Core::Notifier::construct(m_helper_pipe->fd(), Core::Notifier::Read);
    m_notifier->on_ready_to_read = [this] {
        auto line = m_helper_pipe->read_line();
        if (line.is_null()) {
            did_error();
            return;
        }
        auto parts = line.split_view(' ');
        VERIFY(!parts.is_empty());

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

void FileOperationProgressWidget::did_error()
{
    // FIXME: Communicate more with the user about errors.
    close_pipe();
    GUI::MessageBox::show(window(), "An error occurred", "Error", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK);
    window()->close();
}

void FileOperationProgressWidget::did_progress([[maybe_unused]] off_t bytes_done, [[maybe_unused]] off_t total_byte_count, size_t files_done, size_t total_file_count, off_t current_file_done, off_t current_file_size, const StringView& current_file_name)
{
    auto& current_file_label = *find_descendant_of_type_named<GUI::Label>("current_file_label");
    auto& current_file_progressbar = *find_descendant_of_type_named<GUI::Progressbar>("current_file_progressbar");
    auto& overall_progress_label = *find_descendant_of_type_named<GUI::Label>("overall_progress_label");
    auto& overall_progressbar = *find_descendant_of_type_named<GUI::Progressbar>("overall_progressbar");

    current_file_label.set_text(current_file_name);
    current_file_progressbar.set_max(current_file_size);
    current_file_progressbar.set_value(current_file_done);

    overall_progress_label.set_text(String::formatted("{} of {}", files_done, total_file_count));
    overall_progressbar.set_max(total_file_count);
    overall_progressbar.set_value(files_done);
}

void FileOperationProgressWidget::close_pipe()
{
    if (!m_helper_pipe)
        return;
    m_helper_pipe = nullptr;
    if (m_notifier)
        m_notifier->on_ready_to_read = nullptr;
    m_notifier = nullptr;
}

}
