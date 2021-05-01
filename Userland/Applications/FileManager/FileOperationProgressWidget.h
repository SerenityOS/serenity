/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/ElapsedTimer.h>
#include <LibGUI/Widget.h>

namespace FileManager {

class FileOperationProgressWidget : public GUI::Widget {
    C_OBJECT(FileOperationProgressWidget);

public:
    virtual ~FileOperationProgressWidget() override;

private:
    explicit FileOperationProgressWidget(NonnullRefPtr<Core::File> helper_pipe);

    void did_finish();
    void did_error(String message);
    void did_progress(off_t bytes_done, off_t total_byte_count, size_t files_done, size_t total_file_count, off_t current_file_done, off_t current_file_size, const StringView& current_filename);

    void close_pipe();

    String estimate_time(off_t bytes_done, off_t total_byte_count);
    Core::ElapsedTimer m_elapsed_timer;

    RefPtr<Core::Notifier> m_notifier;
    RefPtr<Core::File> m_helper_pipe;
};
}
