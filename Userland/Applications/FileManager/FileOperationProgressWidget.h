/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "FileUtils.h"
#include <LibCore/ElapsedTimer.h>
#include <LibCore/File.h>
#include <LibGUI/Widget.h>

namespace FileManager {

class FileOperationProgressWidget : public GUI::Widget {
    C_OBJECT(FileOperationProgressWidget);

public:
    virtual ~FileOperationProgressWidget() override;

private:
    // FIXME: The helper_pipe_fd parameter is only needed because we can't get the fd from a Core::Stream.
    FileOperationProgressWidget(FileOperation, NonnullOwnPtr<Core::InputBufferedFile> helper_pipe, int helper_pipe_fd);

    void did_finish();
    void did_error(StringView message);
    void did_progress(off_t bytes_done, off_t total_byte_count, size_t files_done, size_t total_file_count, off_t current_file_done, off_t current_file_size, StringView current_filename);

    void close_pipe();

    ByteString estimate_time(off_t bytes_done, off_t total_byte_count);
    Core::ElapsedTimer m_elapsed_timer;

    FileOperation m_operation;
    RefPtr<Core::Notifier> m_notifier;
    OwnPtr<Core::InputBufferedFile> m_helper_pipe;
};
}
