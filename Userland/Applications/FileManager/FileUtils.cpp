/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileUtils.h"
#include "FileOperationProgressWidget.h"
#include <AK/LexicalPath.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibGUI/MessageBox.h>
#include <unistd.h>

namespace FileManager {

HashTable<NonnullRefPtr<GUI::Window>> file_operation_windows;

void delete_paths(Vector<String> const& paths, bool should_confirm, GUI::Window* parent_window)
{
    String message;
    if (paths.size() == 1) {
        message = String::formatted("Are you sure you want to delete {}?", LexicalPath::basename(paths[0]));
    } else {
        message = String::formatted("Are you sure you want to delete {} files?", paths.size());
    }

    if (should_confirm) {
        auto result = GUI::MessageBox::show(parent_window,
            message,
            "Confirm deletion",
            GUI::MessageBox::Type::Warning,
            GUI::MessageBox::InputType::OKCancel);
        if (result == GUI::MessageBox::ExecResult::Cancel)
            return;
    }

    if (run_file_operation(FileOperation::Delete, paths, {}, parent_window).is_error())
        _exit(1);
}

ErrorOr<void> run_file_operation(FileOperation operation, Vector<String> const& sources, String const& destination, GUI::Window* parent_window)
{
    auto pipe_fds = TRY(Core::System::pipe2(0));

    pid_t child_pid = TRY(Core::System::fork());

    if (!child_pid) {
        TRY(Core::System::close(pipe_fds[0]));
        TRY(Core::System::dup2(pipe_fds[1], STDOUT_FILENO));

        Vector<StringView> file_operation_args;
        file_operation_args.append("/bin/FileOperation");

        switch (operation) {
        case FileOperation::Copy:
            file_operation_args.append("Copy");
            break;
        case FileOperation::Move:
            file_operation_args.append("Move");
            break;
        case FileOperation::Delete:
            file_operation_args.append("Delete");
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        for (auto& source : sources)
            file_operation_args.append(source.view());

        if (operation != FileOperation::Delete)
            file_operation_args.append(destination.view());

        TRY(Core::System::exec(file_operation_args.first(), file_operation_args, Core::System::SearchInPath::Yes));
        VERIFY_NOT_REACHED();
    } else {
        TRY(Core::System::close(pipe_fds[1]));
    }

    auto window = TRY(GUI::Window::try_create());
    TRY(file_operation_windows.try_set(window));

    switch (operation) {
    case FileOperation::Copy:
        window->set_title("Copying Files...");
        break;
    case FileOperation::Move:
        window->set_title("Moving Files...");
        break;
    case FileOperation::Delete:
        window->set_title("Deleting Files...");
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    auto pipe_input_file = TRY(Core::Stream::File::adopt_fd(pipe_fds[0], Core::Stream::OpenMode::Read));
    auto buffered_pipe = TRY(Core::Stream::BufferedFile::create(move(pipe_input_file)));

    (void)TRY(window->try_set_main_widget<FileOperationProgressWidget>(operation, move(buffered_pipe), pipe_fds[0]));
    window->resize(320, 190);
    if (parent_window)
        window->center_within(*parent_window);
    window->show();

    return {};
}

}
