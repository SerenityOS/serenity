/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibContentAccessClient/Client.h>
#include <LibCore/File.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>

namespace ContentAccessClient {

static RefPtr<Client> s_the = nullptr;

Client& Client::the()
{
    if (!s_the || !s_the->is_open())
        s_the = Client::try_create().release_value_but_fixme_should_propagate_errors();
    return *s_the;
}

Result Client::request_url_read_only_approve_local(GUI::Window* parent_window, URL const& url)
{
    return request_url_impl(parent_window, url, url.scheme() == "file"sv);
}

Result Client::request_url_read_only(GUI::Window* parent_window, URL const& url)
{
    return request_url_impl(parent_window, url, false);
}

Result Client::request_url_impl(GUI::Window* parent_window, URL const& url, bool skip_prompt)
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

    URL effective_url = url;
    if (url.scheme() == "file"sv && !url.host().starts_with('/'))
        effective_url.set_paths(LexicalPath::join(Core::File::current_working_directory(), url.path()).parts());

    async_request_url_read_only(id, parent_window_server_client_id, parent_window_id, effective_url, !skip_prompt);

    return handle_promise(id);
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

    return handle_promise(id);
}

void Client::handle_prompt_end(i32 request_id, i32 error, Optional<IPC::File> const& ipc_file, Optional<URL> const& chosen_url)
{
    auto potential_data = m_promises.get(request_id);
    VERIFY(potential_data.has_value());
    auto& request_data = potential_data.value();

    if (error != 0) {
        // We don't want to show an error message for non-existent files since some applications may want
        // to handle it as opening a new, named file.
        if (error != -1 && error != ENOENT)
            GUI::MessageBox::show_error(request_data.parent_window, DeprecatedString::formatted("Opening \"{}\" failed: {}", *chosen_url, strerror(error)));
        request_data.promise->resolve(Error::from_errno(error));
        return;
    }

    if (Core::File::is_device(ipc_file->fd())) {
        GUI::MessageBox::show_error(request_data.parent_window, DeprecatedString::formatted("Opening \"{}\" failed: Cannot open device files", *chosen_url));
        request_data.promise->resolve(Error::from_string_literal("Cannot open device files"));
        return;
    }

    if (Core::File::is_directory(ipc_file->fd())) {
        GUI::MessageBox::show_error(request_data.parent_window, DeprecatedString::formatted("Opening \"{}\" failed: Cannot open directory", *chosen_url));
        request_data.promise->resolve(Error::from_errno(EISDIR));
        return;
    }

    auto file_or_error = [&]() -> ErrorOr<File> {
        auto stream = TRY(Core::Stream::File::adopt_fd(ipc_file->take_fd(), Core::Stream::OpenMode::ReadWrite));
        auto filename = chosen_url->scheme() == "file"sv ? TRY(String::from_utf8(chosen_url->path())) : String();
        return File(move(stream), move(filename));
    }();

    if (file_or_error.is_error()) {
        request_data.promise->resolve(file_or_error.release_error());
        return;
    }

    request_data.promise->resolve(file_or_error.release_value());
}

void Client::die()
{
    for (auto const& entry : m_promises)
        handle_prompt_end(entry.key, ECONNRESET, {}, URL {});
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
    auto result = m_promises.get(id)->promise->await();
    m_promises.remove(id);
    return result;
}

}
