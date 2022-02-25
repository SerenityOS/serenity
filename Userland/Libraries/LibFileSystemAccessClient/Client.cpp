/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// FIXME: LibIPC Decoder and Encoder are sensitive to include order here
// clang-format off
#include <LibGUI/ConnectionToWindowServer.h>
// clang-format on
#include <AK/LexicalPath.h>
#include <LibCore/File.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>

namespace FileSystemAccessClient {

static RefPtr<Client> s_the = nullptr;

Client& Client::the()
{
    if (!s_the || !s_the->is_open())
        s_the = Client::try_create().release_value_but_fixme_should_propagate_errors();
    return *s_the;
}

Result Client::try_request_file_read_only_approved(GUI::Window* parent_window, String const& path)
{
    m_promise = Core::Promise<Result>::construct();
    m_parent_window = parent_window;

    auto parent_window_server_client_id = GUI::ConnectionToWindowServer::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();
    auto parent_window_id = parent_window->window_id();

    GUI::ConnectionToWindowServer::the().async_add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::ConnectionToWindowServer::the().async_remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    if (path.starts_with('/')) {
        async_request_file_read_only_approved(parent_window_server_client_id, parent_window_id, path);
    } else {
        auto full_path = LexicalPath::join(Core::File::current_working_directory(), path).string();
        async_request_file_read_only_approved(parent_window_server_client_id, parent_window_id, full_path);
    }

    return m_promise->await();
}

Result Client::try_request_file(GUI::Window* parent_window, String const& path, Core::OpenMode mode)
{
    m_promise = Core::Promise<Result>::construct();
    m_parent_window = parent_window;

    auto parent_window_server_client_id = GUI::ConnectionToWindowServer::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();
    auto parent_window_id = parent_window->window_id();

    GUI::ConnectionToWindowServer::the().async_add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::ConnectionToWindowServer::the().async_remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    if (path.starts_with('/')) {
        async_request_file(parent_window_server_client_id, parent_window_id, path, mode);
    } else {
        auto full_path = LexicalPath::join(Core::File::current_working_directory(), path).string();
        async_request_file(parent_window_server_client_id, parent_window_id, full_path, mode);
    }

    return m_promise->await();
}

Result Client::try_open_file(GUI::Window* parent_window, String const& window_title, StringView path, Core::OpenMode requested_access)
{
    m_promise = Core::Promise<Result>::construct();
    m_parent_window = parent_window;

    auto parent_window_server_client_id = GUI::ConnectionToWindowServer::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();
    auto parent_window_id = parent_window->window_id();

    GUI::ConnectionToWindowServer::the().async_add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::ConnectionToWindowServer::the().async_remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    async_prompt_open_file(parent_window_server_client_id, parent_window_id, window_title, path, requested_access);

    return m_promise->await();
}

Result Client::try_save_file(GUI::Window* parent_window, String const& name, String const ext, Core::OpenMode requested_access)
{
    m_promise = Core::Promise<Result>::construct();
    m_parent_window = parent_window;

    auto parent_window_server_client_id = GUI::ConnectionToWindowServer::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();
    auto parent_window_id = parent_window->window_id();

    GUI::ConnectionToWindowServer::the().async_add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::ConnectionToWindowServer::the().async_remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    async_prompt_save_file(parent_window_server_client_id, parent_window_id, name.is_null() ? "Untitled" : name, ext.is_null() ? "txt" : ext, Core::StandardPaths::home_directory(), requested_access);

    return m_promise->await();
}

void Client::handle_prompt_end(i32 error, Optional<IPC::File> const& ipc_file, Optional<String> const& chosen_file)
{
    VERIFY(m_promise);
    if (error != 0) {
        // We don't want to show an error message for non-existent files since some applications may want
        // to handle it as opening a new, named file.
        if (error != -1 && error != ENOENT)
            GUI::MessageBox::show_error(m_parent_window, String::formatted("Opening \"{}\" failed: {}", *chosen_file, strerror(error)));
        m_promise->resolve(Error::from_errno(error));
        return;
    }

    auto file = Core::File::construct();
    auto fd = ipc_file->take_fd();
    if (!file->open(fd, Core::OpenMode::ReadWrite, Core::File::ShouldCloseFileDescriptor::Yes) && file->error() != ENOENT) {
        GUI::MessageBox::show_error(m_parent_window, String::formatted("Opening \"{}\" failed: {}", *chosen_file, strerror(error)));
        m_promise->resolve(Error::from_errno(file->error()));
        return;
    }

    if (file->is_device()) {
        GUI::MessageBox::show_error(m_parent_window, String::formatted("Opening \"{}\" failed: Cannot open device files", *chosen_file));
        m_promise->resolve(Error::from_string_literal("Cannot open device files"sv));
        return;
    }

    if (file->is_directory()) {
        GUI::MessageBox::show_error(m_parent_window, String::formatted("Opening \"{}\" failed: Cannot open directory", *chosen_file));
        m_promise->resolve(Error::from_string_literal("Cannot open directory"sv));
        return;
    }

    file->set_filename(move(*chosen_file));
    m_promise->resolve(file);
}

void Client::die()
{
    if (m_promise)
        handle_prompt_end(ECONNRESET, {}, "");
}

}
