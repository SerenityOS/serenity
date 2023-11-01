/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FileUtils.h"
#include "FileOperationProgressWidget.h"
#include <AK/LexicalPath.h>
#include <LibCore/MimeData.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Action.h>
#include <LibGUI/Event.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <functional>
#include <unistd.h>

namespace FileManager {

HashTable<NonnullRefPtr<GUI::Window>> file_operation_windows;

void delete_paths(Vector<DeprecatedString> const& paths, bool should_confirm, GUI::Window* parent_window)
{
    DeprecatedString message;
    if (paths.size() == 1) {
        message = DeprecatedString::formatted("Are you sure you want to delete \"{}\"?", LexicalPath::basename(paths[0]));
    } else {
        message = DeprecatedString::formatted("Are you sure you want to delete {} files?", paths.size());
    }

    if (should_confirm) {
        auto result = GUI::MessageBox::show(parent_window,
            message,
            "Confirm Deletion"sv,
            GUI::MessageBox::Type::Warning,
            GUI::MessageBox::InputType::OKCancel);
        if (result == GUI::MessageBox::ExecResult::Cancel)
            return;
    }

    if (run_file_operation(FileOperation::Delete, paths, {}, parent_window).is_error())
        _exit(1);
}

ErrorOr<void> run_file_operation(FileOperation operation, Vector<DeprecatedString> const& sources, DeprecatedString const& destination, GUI::Window* parent_window)
{
    auto pipe_fds = TRY(Core::System::pipe2(0));

    pid_t child_pid = TRY(Core::System::fork());

    if (!child_pid) {
        TRY(Core::System::close(pipe_fds[0]));
        TRY(Core::System::dup2(pipe_fds[1], STDOUT_FILENO));

        Vector<StringView> file_operation_args;
        file_operation_args.append("/bin/FileOperation"sv);

        switch (operation) {
        case FileOperation::Copy:
            file_operation_args.append("Copy"sv);
            break;
        case FileOperation::Move:
            file_operation_args.append("Move"sv);
            break;
        case FileOperation::Delete:
            file_operation_args.append("Delete"sv);
            break;
        case FileOperation::Link:
            GUI::MessageBox::show(parent_window, "This feature is not implemented yet"sv, "Not Implemented"sv);
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

    auto window = GUI::Window::construct();
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

    auto pipe_input_file = TRY(Core::File::adopt_fd(pipe_fds[0], Core::File::OpenMode::Read));
    auto buffered_pipe = TRY(Core::InputBufferedFile::create(move(pipe_input_file)));

    (void)window->set_main_widget<FileOperationProgressWidget>(operation, move(buffered_pipe), pipe_fds[0]);
    window->resize(320, 190);
    if (parent_window)
        window->center_within(*parent_window);
    window->show();

    return {};
}

ErrorOr<bool> handle_drop(GUI::DropEvent const& event, DeprecatedString const& destination, GUI::Window* window)
{

    bool has_accepted_drop = false;

    if (!event.mime_data().has_urls())
        return has_accepted_drop;

    auto const urls = event.mime_data().urls();
    if (urls.is_empty()) {
        dbgln("No files to drop");
        return has_accepted_drop;
    }

    auto const target = LexicalPath::canonicalized_path(destination);

    if (!FileSystem::is_directory(target))
        return has_accepted_drop;

    Vector<DeprecatedString> sources;

    for (auto const& url_to_copy : urls) {
        auto file_path = url_to_copy.serialize_path();
        if (!url_to_copy.is_valid() || file_path == target)
            continue;
        auto new_path = DeprecatedString::formatted("{}/{}", target, LexicalPath::basename(file_path));
        if (file_path == new_path)
            continue;

        if (FileSystem::exists(new_path.view())) {
            auto result = GUI::MessageBox::create(window, ""sv, ""sv);
        }

        sources.append(file_path);
        has_accepted_drop = true;
    }

    if (!sources.is_empty()) {
        auto ctx_menu = GUI::Menu::construct("Item Drop Menu"_string);
        auto move_action = GUI::Action::create(urls.size() > 1 ? "Move Items" : "Move Item", [&](auto&) {
            MUST(run_file_operation(FileOperation::Move, sources, target, window));
        });
        auto link_action = GUI::Action::create(urls.size() > 1 ? "Link Items" : "Link Item", [&](auto&) {
            MUST(run_file_operation(FileOperation::Link, sources, target, window));
        });
        auto copy_action = GUI::Action::create(urls.size() > 1 ? "Copy Items" : "Copy Item", [&](auto&) {
            MUST(run_file_operation(FileOperation::Copy, sources, target, window));
        });
        auto cancel_action = GUI::Action::create("Cancel", [&](auto&) {
            has_accepted_drop = false;
        });
        ctx_menu->add_action(move_action);
        ctx_menu->add_action(copy_action);
        ctx_menu->add_action(link_action);
        ctx_menu->add_separator();
        ctx_menu->add_action(cancel_action);
        ctx_menu->popup(event.position(), cancel_action);
    }

    return has_accepted_drop;
}

}
