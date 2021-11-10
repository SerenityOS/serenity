/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// FIXME: LibIPC Decoder and Encoder are sensitive to include order here
// clang-format off
#include <LibGUI/WindowServerConnection.h>
// clang-format on
#include <AK/LexicalPath.h>
#include <LibCore/File.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/Window.h>

namespace FileSystemAccessClient {

static RefPtr<Client> s_the = nullptr;

Client& Client::the()
{
    if (!s_the || !s_the->is_open())
        s_the = Client::construct();
    return *s_the;
}

Result Client::request_file_read_only_approved(i32 parent_window_id, String const& path)
{
    m_promise = Core::Promise<Result>::construct();
    auto parent_window_server_client_id = GUI::WindowServerConnection::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();

    GUI::WindowServerConnection::the().async_add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::WindowServerConnection::the().async_remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    if (path.starts_with('/')) {
        async_request_file_read_only_approved(parent_window_server_client_id, parent_window_id, path);
    } else {
        auto full_path = LexicalPath::join(Core::File::current_working_directory(), path).string();
        async_request_file_read_only_approved(parent_window_server_client_id, parent_window_id, full_path);
    }

    return m_promise->await();
}

Result Client::request_file(i32 parent_window_id, String const& path, Core::OpenMode mode)
{
    m_promise = Core::Promise<Result>::construct();
    auto parent_window_server_client_id = GUI::WindowServerConnection::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();

    GUI::WindowServerConnection::the().async_add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::WindowServerConnection::the().async_remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    if (path.starts_with('/')) {
        async_request_file(parent_window_server_client_id, parent_window_id, path, mode);
    } else {
        auto full_path = LexicalPath::join(Core::File::current_working_directory(), path).string();
        async_request_file(parent_window_server_client_id, parent_window_id, full_path, mode);
    }

    return m_promise->await();
}

Result Client::open_file(i32 parent_window_id, String const& window_title, StringView path)
{
    m_promise = Core::Promise<Result>::construct();
    auto parent_window_server_client_id = GUI::WindowServerConnection::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();

    GUI::WindowServerConnection::the().async_add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::WindowServerConnection::the().async_remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    async_prompt_open_file(parent_window_server_client_id, parent_window_id, window_title, path, Core::OpenMode::ReadOnly);

    return m_promise->await();
}

Result Client::save_file(i32 parent_window_id, String const& name, String const ext)
{
    m_promise = Core::Promise<Result>::construct();
    auto parent_window_server_client_id = GUI::WindowServerConnection::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();

    GUI::WindowServerConnection::the().async_add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::WindowServerConnection::the().async_remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    async_prompt_save_file(parent_window_server_client_id, parent_window_id, name.is_null() ? "Untitled" : name, ext.is_null() ? "txt" : ext, Core::StandardPaths::home_directory(), Core::OpenMode::Truncate | Core::OpenMode::WriteOnly);

    return m_promise->await();
}

void Client::handle_prompt_end(i32 error, Optional<IPC::File> const& fd, Optional<String> const& chosen_file)
{
    VERIFY(m_promise);

    if (error == 0) {
        m_promise->resolve({ error, fd->take_fd(), *chosen_file });
    } else {
        m_promise->resolve({ error, {}, chosen_file });
    }
}

void Client::die()
{
    if (m_promise)
        handle_prompt_end(ECONNRESET, {}, "");
}

}
