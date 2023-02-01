/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/File.h>
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

DeprecatedResult Client::try_request_file_read_only_approved_deprecated(GUI::Window* parent_window, DeprecatedString const& path)
{
    auto const id = get_new_id();
    m_promises.set(id, PromiseAndWindow { { Core::Promise<DeprecatedResult>::construct() }, parent_window });

    auto parent_window_server_client_id = GUI::ConnectionToWindowServer::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();
    auto parent_window_id = parent_window->window_id();

    GUI::ConnectionToWindowServer::the().add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::ConnectionToWindowServer::the().remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    if (path.starts_with('/')) {
        async_request_file_read_only_approved(id, parent_window_server_client_id, parent_window_id, path);
    } else {
        auto full_path = LexicalPath::join(Core::File::current_working_directory(), path).string();
        async_request_file_read_only_approved(id, parent_window_server_client_id, parent_window_id, full_path);
    }

    return handle_promise<DeprecatedResult>(id);
}

Result Client::request_file_read_only_approved(GUI::Window* parent_window, DeprecatedString const& path)
{
    auto const id = get_new_id();
    m_promises.set(id, PromiseAndWindow { { Core::Promise<Result>::construct() }, parent_window });

    auto parent_window_server_client_id = GUI::ConnectionToWindowServer::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();
    auto parent_window_id = parent_window->window_id();

    GUI::ConnectionToWindowServer::the().add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::ConnectionToWindowServer::the().remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    if (path.starts_with('/')) {
        async_request_file_read_only_approved(id, parent_window_server_client_id, parent_window_id, path);
    } else {
        auto full_path = LexicalPath::join(Core::File::current_working_directory(), path).string();
        async_request_file_read_only_approved(id, parent_window_server_client_id, parent_window_id, full_path);
    }

    return handle_promise<Result>(id);
}

static Core::Stream::OpenMode to_stream_open_mode(Core::OpenMode open_mode)
{
    Core::Stream::OpenMode result {};
    if ((open_mode & Core::OpenMode::ReadOnly) == Core::OpenMode::ReadOnly)
        result |= Core::Stream::OpenMode::Read;
    if ((open_mode & Core::OpenMode::WriteOnly) == Core::OpenMode::WriteOnly)
        result |= Core::Stream::OpenMode::Write;
    if ((open_mode & Core::OpenMode::ReadWrite) == Core::OpenMode::ReadWrite)
        result |= Core::Stream::OpenMode::ReadWrite;
    if ((open_mode & Core::OpenMode::Append) == Core::OpenMode::Append)
        result |= Core::Stream::OpenMode::Append;
    if ((open_mode & Core::OpenMode::Truncate) == Core::OpenMode::Truncate)
        result |= Core::Stream::OpenMode::Truncate;
    if ((open_mode & Core::OpenMode::MustBeNew) == Core::OpenMode::MustBeNew)
        result |= Core::Stream::OpenMode::MustBeNew;
    if ((open_mode & Core::OpenMode::KeepOnExec) == Core::OpenMode::KeepOnExec)
        result |= Core::Stream::OpenMode::KeepOnExec;

    return result;
}

DeprecatedResult Client::try_request_file_deprecated(GUI::Window* parent_window, DeprecatedString const& path, Core::OpenMode deprecated_mode)
{
    auto const id = get_new_id();
    m_promises.set(id, PromiseAndWindow { { Core::Promise<DeprecatedResult>::construct() }, parent_window });

    auto parent_window_server_client_id = GUI::ConnectionToWindowServer::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();
    auto parent_window_id = parent_window->window_id();

    GUI::ConnectionToWindowServer::the().add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::ConnectionToWindowServer::the().remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    auto const mode = to_stream_open_mode(deprecated_mode);

    if (path.starts_with('/')) {
        async_request_file(id, parent_window_server_client_id, parent_window_id, path, mode);
    } else {
        auto full_path = LexicalPath::join(Core::File::current_working_directory(), path).string();
        async_request_file(id, parent_window_server_client_id, parent_window_id, full_path, mode);
    }

    return handle_promise<DeprecatedResult>(id);
}

Result Client::request_file(GUI::Window* parent_window, DeprecatedString const& path, Core::Stream::OpenMode mode)
{
    auto const id = get_new_id();
    m_promises.set(id, PromiseAndWindow { { Core::Promise<Result>::construct() }, parent_window });

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
        auto full_path = LexicalPath::join(Core::File::current_working_directory(), path).string();
        async_request_file(id, parent_window_server_client_id, parent_window_id, full_path, mode);
    }

    return handle_promise<Result>(id);
}

DeprecatedResult Client::try_open_file_deprecated(GUI::Window* parent_window, DeprecatedString const& window_title, StringView path, Core::OpenMode deprecated_requested_access)
{
    auto const id = get_new_id();
    m_promises.set(id, PromiseAndWindow { { Core::Promise<DeprecatedResult>::construct() }, parent_window });

    auto parent_window_server_client_id = GUI::ConnectionToWindowServer::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();
    auto parent_window_id = parent_window->window_id();

    GUI::ConnectionToWindowServer::the().add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::ConnectionToWindowServer::the().remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    auto const requested_access = to_stream_open_mode(deprecated_requested_access);

    async_prompt_open_file(id, parent_window_server_client_id, parent_window_id, window_title, path, requested_access);

    return handle_promise<DeprecatedResult>(id);
}

Result Client::open_file(GUI::Window* parent_window, DeprecatedString const& window_title, StringView path, Core::Stream::OpenMode requested_access)
{
    auto const id = get_new_id();
    m_promises.set(id, PromiseAndWindow { { Core::Promise<Result>::construct() }, parent_window });

    auto parent_window_server_client_id = GUI::ConnectionToWindowServer::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();
    auto parent_window_id = parent_window->window_id();

    GUI::ConnectionToWindowServer::the().add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::ConnectionToWindowServer::the().remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    async_prompt_open_file(id, parent_window_server_client_id, parent_window_id, window_title, path, requested_access);

    return handle_promise<Result>(id);
}

DeprecatedResult Client::try_save_file_deprecated(GUI::Window* parent_window, DeprecatedString const& name, DeprecatedString const ext, Core::OpenMode deprecated_requested_access)
{
    auto const id = get_new_id();
    m_promises.set(id, PromiseAndWindow { { Core::Promise<DeprecatedResult>::construct() }, parent_window });

    auto parent_window_server_client_id = GUI::ConnectionToWindowServer::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();
    auto parent_window_id = parent_window->window_id();

    GUI::ConnectionToWindowServer::the().add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::ConnectionToWindowServer::the().remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    auto const requested_access = to_stream_open_mode(deprecated_requested_access);

    async_prompt_save_file(id, parent_window_server_client_id, parent_window_id, name.is_null() ? "Untitled" : name, ext.is_null() ? "txt" : ext, Core::StandardPaths::home_directory(), requested_access);

    return handle_promise<DeprecatedResult>(id);
}

Result Client::save_file(GUI::Window* parent_window, DeprecatedString const& name, DeprecatedString const ext, Core::Stream::OpenMode requested_access)
{
    auto const id = get_new_id();
    m_promises.set(id, PromiseAndWindow { { Core::Promise<Result>::construct() }, parent_window });

    auto parent_window_server_client_id = GUI::ConnectionToWindowServer::the().expose_client_id();
    auto child_window_server_client_id = expose_window_server_client_id();
    auto parent_window_id = parent_window->window_id();

    GUI::ConnectionToWindowServer::the().add_window_stealing_for_client(child_window_server_client_id, parent_window_id);

    ScopeGuard guard([parent_window_id, child_window_server_client_id] {
        GUI::ConnectionToWindowServer::the().remove_window_stealing_for_client(child_window_server_client_id, parent_window_id);
    });

    async_prompt_save_file(id, parent_window_server_client_id, parent_window_id, name.is_null() ? "Untitled" : name, ext.is_null() ? "txt" : ext, Core::StandardPaths::home_directory(), requested_access);

    return handle_promise<Result>(id);
}

void Client::handle_prompt_end(i32 request_id, i32 error, Optional<IPC::File> const& ipc_file, Optional<DeprecatedString> const& chosen_file)
{
    auto potential_data = m_promises.get(request_id);
    VERIFY(potential_data.has_value());
    auto& request_data = potential_data.value();

    auto const resolve_any_promise = [&promise = request_data.promise](Error&& error) {
        if (promise.has<PromiseType<DeprecatedResult>>()) {
            promise.get<PromiseType<DeprecatedResult>>()->resolve(move(error));
            return;
        }
        promise.get<PromiseType<Result>>()->resolve(move(error));
    };

    if (error != 0) {
        // We don't want to show an error message for non-existent files since some applications may want
        // to handle it as opening a new, named file.
        if (error != -1 && error != ENOENT)
            GUI::MessageBox::show_error(request_data.parent_window, DeprecatedString::formatted("Opening \"{}\" failed: {}", *chosen_file, strerror(error)));
        resolve_any_promise(Error::from_errno(error));
        return;
    }

    if (Core::File::is_device(ipc_file->fd())) {
        GUI::MessageBox::show_error(request_data.parent_window, DeprecatedString::formatted("Opening \"{}\" failed: Cannot open device files", *chosen_file));
        resolve_any_promise(Error::from_string_literal("Cannot open device files"));
        return;
    }

    if (Core::File::is_directory(ipc_file->fd())) {
        GUI::MessageBox::show_error(request_data.parent_window, DeprecatedString::formatted("Opening \"{}\" failed: Cannot open directory", *chosen_file));
        resolve_any_promise(Error::from_errno(EISDIR));
        return;
    }

    if (request_data.promise.has<PromiseType<DeprecatedResult>>()) {
        auto file = Core::File::construct();
        auto fd = ipc_file->take_fd();
        file->open(fd, Core::OpenMode::ReadWrite, Core::File::ShouldCloseFileDescriptor::Yes);
        file->set_filename(*chosen_file);

        request_data.promise.get<PromiseType<DeprecatedResult>>()->resolve(file);
        return;
    }

    auto file_or_error = [&]() -> ErrorOr<File> {
        auto stream = TRY(Core::Stream::File::adopt_fd(ipc_file->take_fd(), Core::Stream::OpenMode::ReadWrite));
        auto filename = TRY(String::from_deprecated_string(*chosen_file));
        return File({}, move(stream), filename);
    }();
    if (file_or_error.is_error()) {
        resolve_any_promise(file_or_error.release_error());
        return;
    }

    request_data.promise.get<PromiseType<Result>>()->resolve(file_or_error.release_value());
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

template<typename AnyResult>
AnyResult Client::handle_promise(int id)
{
    auto result = m_promises.get(id)->promise.get<PromiseType<AnyResult>>()->await();
    m_promises.remove(id);
    return result;
}

}
