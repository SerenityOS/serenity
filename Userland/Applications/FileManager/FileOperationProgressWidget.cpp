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
#include <LibCore/Notifier.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/ProgressBar.h>
#include <LibGUI/Window.h>

namespace FileManager {

FileOperationProgressWidget::FileOperationProgressWidget(FILE* helper_pipe)
    : m_helper_pipe(helper_pipe)
{
    load_from_gml(file_operation_progress_gml);

    auto& button = *find_descendant_of_type_named<GUI::Button>("button");

    button.on_click = [this] {
        close_pipe();
        window()->close();
    };

    m_notifier = Core::Notifier::construct(fileno(m_helper_pipe), Core::Notifier::Read);
    m_notifier->on_ready_to_read = [this] {
        char buffer[8192];
        if (!fgets(buffer, sizeof(buffer), m_helper_pipe)) {
            did_error();
            return;
        }
        auto parts = StringView(buffer).split_view(' ');
        VERIFY(!parts.is_empty());

        if (parts[0] == "FINISH\n"sv) {
            did_finish();
            return;
        }

        if (parts[0] == "PROGRESS"sv) {
            VERIFY(parts.size() >= 6);
            did_progress(
                parts[3].to_uint().value_or(0),
                parts[4].to_uint().value_or(0),
                parts[1].to_uint().value_or(0),
                parts[2].to_uint().value_or(0),
                parts[5]);
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

void FileOperationProgressWidget::did_progress(off_t bytes_done, off_t total_byte_count, size_t files_done, size_t total_file_count, const StringView& current_file_name)
{
    auto& current_file_label = *find_descendant_of_type_named<GUI::Label>("current_file_label");
    auto& current_file_progress_bar = *find_descendant_of_type_named<GUI::ProgressBar>("current_file_progress_bar");
    auto& overall_progress_label = *find_descendant_of_type_named<GUI::Label>("overall_progress_label");
    auto& overall_progress_bar = *find_descendant_of_type_named<GUI::ProgressBar>("overall_progress_bar");

    current_file_label.set_text(current_file_name);
    current_file_progress_bar.set_max(total_byte_count);
    current_file_progress_bar.set_value(bytes_done);

    overall_progress_label.set_text(String::formatted("{} of {}", files_done, total_file_count));
    overall_progress_bar.set_max(total_file_count);
    overall_progress_bar.set_value(files_done);
}

void FileOperationProgressWidget::close_pipe()
{
    if (!m_helper_pipe)
        return;
    pclose(m_helper_pipe);
    m_helper_pipe = nullptr;
    if (m_notifier)
        m_notifier->on_ready_to_read = nullptr;
    m_notifier = nullptr;
}

}
