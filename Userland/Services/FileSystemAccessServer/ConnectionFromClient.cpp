/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <FileSystemAccessServer/ConnectionFromClient.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Application.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/MessageBox.h>

namespace FileSystemAccessServer {

static HashMap<int, NonnullRefPtr<ConnectionFromClient>> s_connections;

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket> socket)
    : IPC::ConnectionFromClient<FileSystemAccessClientEndpoint, FileSystemAccessServerEndpoint>(*this, move(socket), 1)
{
    s_connections.set(1, *this);
}

void ConnectionFromClient::die()
{
    s_connections.remove(client_id());
    GUI::Application::the()->quit();
}

void ConnectionFromClient::request_file_handler(i32 request_id, i32 window_server_client_id, i32 parent_window_id, ByteString const& path, Core::File::OpenMode requested_access, ShouldPrompt prompt)
{
    VERIFY(path.starts_with("/"sv));

    bool approved = false;
    auto maybe_permissions = m_approved_files.get(path);

    auto relevant_permissions = requested_access & (Core::File::OpenMode::Read | Core::File::OpenMode::Write);
    VERIFY(relevant_permissions != Core::File::OpenMode::NotOpen);

    if (maybe_permissions.has_value())
        approved = has_flag(maybe_permissions.value(), relevant_permissions);

    if (!approved) {
        ByteString access_string;

        if (has_flag(requested_access, Core::File::OpenMode::ReadWrite))
            access_string = "read and write";
        else if (has_flag(requested_access, Core::File::OpenMode::Read))
            access_string = "read from";
        else if (has_flag(requested_access, Core::File::OpenMode::Write))
            access_string = "write to";

        auto pid = this->socket().peer_pid().release_value_but_fixme_should_propagate_errors();
        auto exe_link = LexicalPath("/proc").append(ByteString::number(pid)).append("exe"sv).string();
        auto exe_path = FileSystem::real_path(exe_link).release_value_but_fixme_should_propagate_errors();

        if (prompt == ShouldPrompt::Yes) {
            VERIFY(window_server_client_id != -1 && parent_window_id != -1);
            auto exe_name = LexicalPath::basename(exe_path);
            auto text = String::formatted("Allow {} ({}) to {} \"{}\"?", exe_name, pid, access_string, path).release_value_but_fixme_should_propagate_errors();
            auto result = GUI::MessageBox::try_show({}, window_server_client_id, parent_window_id, text, "File Permissions Requested"sv).release_value_but_fixme_should_propagate_errors();
            approved = result == GUI::MessageBox::ExecResult::Yes;
        } else {
            approved = true;
        }

        if (approved) {
            auto new_permissions = relevant_permissions;

            if (maybe_permissions.has_value())
                new_permissions |= maybe_permissions.value();

            m_approved_files.set(path, new_permissions);
        }
    }

    if (approved) {
        auto file = Core::File::open(path, requested_access);

        if (file.is_error()) {
            dbgln("FileSystemAccessServer: Couldn't open {}, error {}", path, file.error());
            async_handle_prompt_end(request_id, file.error().code(), Optional<IPC::File> {}, path);
        } else {
            async_handle_prompt_end(request_id, 0, IPC::File::adopt_file(file.release_value()), path);
        }
    } else {
        async_handle_prompt_end(request_id, EPERM, Optional<IPC::File> {}, path);
    }
}

void ConnectionFromClient::request_file_read_only_approved(i32 request_id, ByteString const& path)
{
    request_file_handler(request_id, -1, -1, path, Core::File::OpenMode::Read, ShouldPrompt::No);
}

void ConnectionFromClient::request_file(i32 request_id, i32 window_server_client_id, i32 parent_window_id, ByteString const& path, Core::File::OpenMode requested_access)
{
    request_file_handler(request_id, window_server_client_id, parent_window_id, path, requested_access, ShouldPrompt::Yes);
}

void ConnectionFromClient::prompt_open_file(i32 request_id, i32 window_server_client_id, i32 parent_window_id, ByteString const& window_title, ByteString const& path_to_view, Core::File::OpenMode requested_access, Optional<Vector<GUI::FileTypeFilter>> const& allowed_file_types)
{
    auto relevant_permissions = requested_access & (Core::File::OpenMode::Read | Core::File::OpenMode::Write);
    VERIFY(relevant_permissions != Core::File::OpenMode::NotOpen);

    auto user_picked_file = GUI::FilePicker::get_filepath({}, window_server_client_id, parent_window_id, GUI::FilePicker::Mode::Open, window_title, {}, path_to_view, allowed_file_types).release_value_but_fixme_should_propagate_errors();
    auto user_picked_file_but_fixme_should_use_string = user_picked_file.has_value() ? user_picked_file.release_value().to_byte_string() : Optional<ByteString> {};

    prompt_helper(request_id, user_picked_file_but_fixme_should_use_string, requested_access);
}

void ConnectionFromClient::prompt_save_file(i32 request_id, i32 window_server_client_id, i32 parent_window_id, ByteString const& name, ByteString const& ext, ByteString const& path_to_view, Core::File::OpenMode requested_access)
{
    auto relevant_permissions = requested_access & (Core::File::OpenMode::Read | Core::File::OpenMode::Write);
    VERIFY(relevant_permissions != Core::File::OpenMode::NotOpen);

    auto basename = String::formatted("{}.{}", name, ext).release_value_but_fixme_should_propagate_errors();
    auto user_picked_file = GUI::FilePicker::get_filepath({}, window_server_client_id, parent_window_id, GUI::FilePicker::Mode::Save, {}, basename, path_to_view).release_value_but_fixme_should_propagate_errors();
    auto user_picked_file_but_fixme_should_use_string = user_picked_file.has_value() ? user_picked_file.release_value().to_byte_string() : Optional<ByteString> {};

    prompt_helper(request_id, user_picked_file_but_fixme_should_use_string, requested_access);
}

void ConnectionFromClient::prompt_helper(i32 request_id, Optional<ByteString> const& user_picked_file, Core::File::OpenMode requested_access)
{
    if (user_picked_file.has_value()) {
        VERIFY(user_picked_file->starts_with("/"sv));
        auto file = Core::File::open(user_picked_file.value(), requested_access);

        if (file.is_error()) {
            dbgln("FileSystemAccessServer: Couldn't open {}, error {}", user_picked_file.value(), file.error());
            async_handle_prompt_end(request_id, file.error().code(), Optional<IPC::File> {}, user_picked_file);
        } else {
            auto maybe_permissions = m_approved_files.get(user_picked_file.value());
            auto new_permissions = requested_access & (Core::File::OpenMode::Read | Core::File::OpenMode::Write);
            if (maybe_permissions.has_value())
                new_permissions |= maybe_permissions.value();

            m_approved_files.set(user_picked_file.value(), new_permissions);

            async_handle_prompt_end(request_id, 0, IPC::File::adopt_file(file.release_value()), user_picked_file);
        }
    } else {
        async_handle_prompt_end(request_id, ECANCELED, Optional<IPC::File> {}, Optional<ByteString> {});
    }
}

Messages::FileSystemAccessServer::ExposeWindowServerClientIdResponse ConnectionFromClient::expose_window_server_client_id()
{
    return GUI::ConnectionToWindowServer::the().expose_client_id();
}

}
