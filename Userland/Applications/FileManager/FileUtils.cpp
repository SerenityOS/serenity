/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileUtils.h"
#include "FileOperationProgressWidget.h"
#include <AK/LexicalPath.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibGUI/MessageBox.h>
#include <sys/stat.h>
#include <unistd.h>

namespace FileManager {

HashTable<NonnullRefPtr<GUI::Window>> file_operation_windows;

void delete_path(String const& path, GUI::Window* parent_window)
{
    struct stat st;
    if (lstat(path.characters(), &st)) {
        GUI::MessageBox::show(parent_window,
            String::formatted("lstat({}) failed: {}", path, strerror(errno)),
            "Delete failed",
            GUI::MessageBox::Type::Error);
    }

    bool is_directory = S_ISDIR(st.st_mode);
    auto result = Core::File::remove(path, Core::File::RecursionMode::Allowed, false);

    if (result.is_error()) {
        auto& error = result.error();
        if (is_directory) {
            GUI::MessageBox::show(parent_window,
                String::formatted("Failed to delete directory \"{}\": {}", error.file, error.error_code),
                "Delete failed",
                GUI::MessageBox::Type::Error);
        } else {
            GUI::MessageBox::show(parent_window,
                String::formatted("Failed to delete file \"{}\": {}", error.file, error.error_code),
                "Delete failed",
                GUI::MessageBox::Type::Error);
        }
    }
}

void delete_paths(Vector<String> const& paths, bool should_confirm, GUI::Window* parent_window)
{
    String message;
    if (paths.size() == 1) {
        message = String::formatted("Really delete {}?", LexicalPath::basename(paths[0]));
    } else {
        message = String::formatted("Really delete {} files?", paths.size());
    }

    if (should_confirm) {
        auto result = GUI::MessageBox::show(parent_window,
            message,
            "Confirm deletion",
            GUI::MessageBox::Type::Warning,
            GUI::MessageBox::InputType::OKCancel);
        if (result == GUI::MessageBox::ExecCancel)
            return;
    }

    for (auto& path : paths) {
        delete_path(path, parent_window);
    }
}

void run_file_operation(FileOperation operation, Vector<String> const& sources, String const& destination, GUI::Window* parent_window)
{
    int pipe_fds[2];
    if (pipe(pipe_fds) < 0) {
        perror("pipe");
        VERIFY_NOT_REACHED();
    }

    pid_t child_pid = fork();
    if (child_pid < 0) {
        perror("fork");
        VERIFY_NOT_REACHED();
    }

    if (!child_pid) {
        if (close(pipe_fds[0]) < 0) {
            perror("close");
            _exit(1);
        }
        if (dup2(pipe_fds[1], STDOUT_FILENO) < 0) {
            perror("dup2");
            _exit(1);
        }

        Vector<char const*> file_operation_args;
        file_operation_args.append("/bin/FileOperation");

        switch (operation) {
        case FileOperation::Copy:
            file_operation_args.append("Copy");
            break;
        case FileOperation::Cut:
            file_operation_args.append("Move");
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        for (auto& source : sources)
            file_operation_args.append(source.characters());

        file_operation_args.append(destination.characters());
        file_operation_args.append(nullptr);

        if (execvp(file_operation_args.first(), const_cast<char**>(file_operation_args.data())) < 0) {
            perror("execvp");
            _exit(1);
        }
        VERIFY_NOT_REACHED();
    } else {
        if (close(pipe_fds[1]) < 0) {
            perror("close");
            _exit(1);
        }
    }

    auto window = GUI::Window::construct();
    file_operation_windows.set(window);

    auto pipe_input_file = Core::File::construct();
    pipe_input_file->open(pipe_fds[0], Core::OpenMode::ReadOnly, Core::File::ShouldCloseFileDescriptor::Yes);

    window->set_title("Copying Files...");
    window->set_main_widget<FileOperationProgressWidget>(pipe_input_file);
    window->resize(320, 190);
    if (parent_window)
        window->center_within(*parent_window);
    window->show();
}

}
