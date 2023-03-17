/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/DeprecatedFile.h>
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
        auto full_path = LexicalPath::join(Core::DeprecatedFile::current_working_directory(), path).string();
        async_request_file_read_only_approved(id, parent_window_server_client_id, parent_window_id, full_path);
    }

    return handle_promise(id);
}

Result Client::request_file(GUI::Window* parent_window, DeprecatedString const& path, Core::File::OpenMode mode)
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
        auto full_path = LexicalPath::join(Core::DeprecatedFile::current_working_directory(), path).string();
        async_request_file(id, parent_window_server_client_id, parent_window_id, full_path, mode);
    }

    return handle_promise(id);
}

Result Client::open_file(GUI::Window* parent_window, DeprecatedString const& window_title, StringView path, Core::File::OpenMode requested_access, Optional<Vector<GUI::FileTypeFilter>> const& allowed_file_types)
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

    async_prompt_open_file(id, parent_window_server_client_id, parent_window_id, window_title, path, requested_access, allowed_file_types);

    return handle_promise(id);
}

Result Client::save_file(GUI::Window* parent_window, DeprecatedString const& name, DeprecatedString const ext, Core::File::OpenMode requested_access)
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

    return handle_promise(id);
}

void Client::handle_prompt_end(i32 request_id, i32 error, Optional<IPC::File> const& ipc_file, Optional<DeprecatedString> const& chosen_file)
{
    auto potential_data = m_promises.get(request_id);
    VERIFY(potential_data.has_value());
    auto& request_data = potential_data.value();

    if (error != 0) {
        // We don't want to show an error message for non-existent files since some applications may want
        // to handle it as opening a new, named file.
        if (error != -1 && error != ENOENT)
            GUI::MessageBox::show_error(request_data.parent_window, DeprecatedString::formatted("Opening \"{}\" failed: {}", *chosen_file, strerror(error)));
        request_data.promise->resolve(Error::from_errno(error)).release_value_but_fixme_should_propagate_errors();
        return;
    }

    if (FileSystem::is_device(ipc_file->fd())) {
        GUI::MessageBox::show_error(request_data.parent_window, DeprecatedString::formatted("Opening \"{}\" failed: Cannot open device files", *chosen_file));
        request_data.promise->resolve(Error::from_string_literal("Cannot open device files")).release_value_but_fixme_should_propagate_errors();
        return;
    }

    if (FileSystem::is_directory(ipc_file->fd())) {
        GUI::MessageBox::show_error(request_data.parent_window, DeprecatedString::formatted("Opening \"{}\" failed: Cannot open directory", *chosen_file));
        request_data.promise->resolve(Error::from_errno(EISDIR)).release_value_but_fixme_should_propagate_errors();
        return;
    }

    auto file_or_error = [&]() -> ErrorOr<File> {
        auto stream = TRY(Core::File::adopt_fd(ipc_file->take_fd(), Core::File::OpenMode::ReadWrite));
        auto filename = TRY(String::from_deprecated_string(*chosen_file));
        return File({}, move(stream), filename);
    }();
    if (file_or_error.is_error()) {
        request_data.promise->resolve(file_or_error.release_error()).release_value_but_fixme_should_propagate_errors();
        return;
    }

    request_data.promise->resolve(file_or_error.release_value()).release_value_but_fixme_should_propagate_errors();
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
