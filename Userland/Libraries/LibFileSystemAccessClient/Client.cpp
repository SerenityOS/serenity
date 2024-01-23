/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibFileSystem/FileSystem.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/ConnectionToWindowServer.h>
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

Result Client::request_file_read_only_approved(GUI::Window* parent_window, ByteString const& path)
{
    auto const id = get_new_id();
    m_promises.set(id, RequestData { { Core::Promise<Result>::construct() }, parent_window, Core::File::OpenMode::Read });

    if (path.starts_with('/')) {
        async_request_file_read_only_approved(id, path);
    } else {
        auto full_path = LexicalPath::join(TRY(FileSystem::current_working_directory()), path).string();
        async_request_file_read_only_approved(id, full_path);
    }

    return handle_promise(id);
}

Result Client::request_file(GUI::Window* parent_window, ByteString const& path, Core::File::OpenMode mode)
{
    auto const id = get_new_id();
    m_promises.set(id, RequestData { { Core::Promise<Result>::construct() }, parent_window, mode });

    auto parent_window_server_client_id = GUI::ConnectionToWindowServer::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();
    auto parent_window_id = parent_window->window_id();

    GUI::ConnectionToWindowServer::the().add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::ConnectionToWindowServer::the().remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    if (path.starts_with('/')) {
        async_request_file(id, parent_window_server_client_id, parent_window_id, path, mode);
    } else {
        auto full_path = LexicalPath::join(TRY(FileSystem::current_working_directory()), path).string();
        async_request_file(id, parent_window_server_client_id, parent_window_id, full_path, mode);
    }

    return handle_promise(id);
}

Result Client::open_file(GUI::Window* parent_window, OpenFileOptions const& options)
{
    auto const id = get_new_id();
    m_promises.set(id, RequestData { { Core::Promise<Result>::construct() }, parent_window, options.requested_access });

    auto parent_window_server_client_id = GUI::ConnectionToWindowServer::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();
    auto parent_window_id = parent_window->window_id();

    GUI::ConnectionToWindowServer::the().add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::ConnectionToWindowServer::the().remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    async_prompt_open_file(id, parent_window_server_client_id, parent_window_id, options.window_title, options.path, options.requested_access, options.allowed_file_types);

    return handle_promise(id);
}

Result Client::save_file(GUI::Window* parent_window, ByteString const& name, ByteString const ext, Core::File::OpenMode requested_access)
{
    auto const id = get_new_id();
    m_promises.set(id, RequestData { { Core::Promise<Result>::construct() }, parent_window, requested_access });

    auto parent_window_server_client_id = GUI::ConnectionToWindowServer::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();
    auto parent_window_id = parent_window->window_id();

    GUI::ConnectionToWindowServer::the().add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::ConnectionToWindowServer::the().remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    async_prompt_save_file(id, parent_window_server_client_id, parent_window_id, name.is_empty() ? "Untitled" : name, ext.is_empty() ? "txt" : ext, Core::StandardPaths::home_directory(), requested_access);

    return handle_promise(id);
}

void Client::handle_prompt_end(i32 request_id, i32 error, Optional<IPC::File> const& ipc_file, Optional<ByteString> const& chosen_file)
{
    auto potential_data = m_promises.get(request_id);
    VERIFY(potential_data.has_value());
    auto& request_data = potential_data.value();

    auto action = "Requesting"sv;
    if (has_flag(request_data.mode, Core::File::OpenMode::Read))
        action = "Opening"sv;
    else if (has_flag(request_data.mode, Core::File::OpenMode::Write))
        action = "Saving"sv;

    if (ipc_file.has_value()) {
        if (FileSystem::is_device(ipc_file->fd()))
            error = is_silencing_devices() ? ESUCCESS : EINVAL;
        else if (FileSystem::is_directory(ipc_file->fd()))
            error = is_silencing_directories() ? ESUCCESS : EISDIR;
    }

    switch (error) {
    case ESUCCESS:
    case ECANCELED:
        break;
    case ENOENT:
        if (is_silencing_nonexistent_entries())
            break;
        [[fallthrough]];
    default:
        ErrorOr<String> maybe_message = String {};
        if (error == ECONNRESET)
            maybe_message = String::formatted("FileSystemAccessClient: {}", Error::from_errno(error));
        else
            maybe_message = String::formatted("{} \"{}\" failed: {}", action, *chosen_file, Error::from_errno(error));
        if (!maybe_message.is_error())
            (void)GUI::MessageBox::try_show_error(request_data.parent_window, maybe_message.release_value());
    }

    if (error != ESUCCESS)
        return (void)request_data.promise->resolve(Error::from_errno(error));

    auto file_or_error = [&]() -> ErrorOr<File> {
        auto stream = TRY(Core::File::adopt_fd(ipc_file->take_fd(), Core::File::OpenMode::ReadWrite));
        return File({}, move(stream), *chosen_file);
    }();
    if (file_or_error.is_error()) {
        auto maybe_message = String::formatted("{} \"{}\" failed: {}", action, *chosen_file, file_or_error.error());
        if (!maybe_message.is_error())
            (void)GUI::MessageBox::try_show_error(request_data.parent_window, maybe_message.release_value());
        return (void)request_data.promise->resolve(file_or_error.release_error());
    }

    (void)request_data.promise->resolve(file_or_error.release_value());
}

void Client::die()
{
    for (auto const& entry : m_promises)
        handle_prompt_end(entry.key, ECONNRESET, {}, "");
}

int Client::get_new_id()
{
    auto const new_id = m_last_id++;
    // Note: This verify shouldn't fail, and we should provide a valid ID
    // But we probably have more issues if this test fails.
    VERIFY(!m_promises.contains(new_id));
    return new_id;
}

Result Client::handle_promise(int id)
{
    auto result = TRY(m_promises.get(id)->promise->await());
    m_promises.remove(id);
    return result;
}

}
