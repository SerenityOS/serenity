/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <FileSystemAccessServer/ConnectionFromClient.h>
#include <LibCore/DeprecatedFile.h>
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

RefPtr<GUI::Window> ConnectionFromClient::create_dummy_child_window(i32 window_server_client_id, i32 parent_window_id)
{
    auto window = GUI::Window::construct();
    window->set_opacity(0);
    window->set_frameless(true);
    window->set_window_mode(GUI::WindowMode::Passive);
    auto rect = GUI::ConnectionToWindowServer::the().get_window_rect_from_client(window_server_client_id, parent_window_id);
    window->set_rect(rect);
    window->show();
    GUI::ConnectionToWindowServer::the().set_window_parent_from_client(window_server_client_id, parent_window_id, window->window_id());

    return window;
}

void ConnectionFromClient::request_file_handler(i32 request_id, i32 window_server_client_id, i32 parent_window_id, DeprecatedString const& path, Core::File::OpenMode requested_access, ShouldPrompt prompt)
{
    VERIFY(path.starts_with("/"sv));

    bool approved = false;
    auto maybe_permissions = m_approved_files.get(path);

    auto relevant_permissions = requested_access & (Core::File::OpenMode::Read | Core::File::OpenMode::Write);
    VERIFY(relevant_permissions != Core::File::OpenMode::NotOpen);

    if (maybe_permissions.has_value())
        approved = has_flag(maybe_permissions.value(), relevant_permissions);

    if (!approved) {
        DeprecatedString access_string;

        if (has_flag(requested_access, Core::File::OpenMode::ReadWrite))
            access_string = "read and write";
        else if (has_flag(requested_access, Core::File::OpenMode::Read))
            access_string = "read from";
        else if (has_flag(requested_access, Core::File::OpenMode::Write))
            access_string = "write to";

        auto pid = this->socket().peer_pid().release_value_but_fixme_should_propagate_errors();
        auto exe_link = LexicalPath("/proc").append(DeprecatedString::number(pid)).append("exe"sv).string();
        auto exe_path = Core::DeprecatedFile::real_path_for(exe_link);

        auto main_window = create_dummy_child_window(window_server_client_id, parent_window_id);

        if (prompt == ShouldPrompt::Yes) {
            auto exe_name = LexicalPath::basename(exe_path);
            auto result = GUI::MessageBox::show(main_window, DeprecatedString::formatted("Allow {} ({}) to {} \"{}\"?", exe_name, pid, access_string, path), "File Permissions Requested"sv, GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNo);
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
            async_handle_prompt_end(request_id, 0, IPC::File(*file.release_value(), IPC::File::CloseAfterSending), path);
        }
    } else {
        async_handle_prompt_end(request_id, -1, Optional<IPC::File> {}, path);
    }
}

void ConnectionFromClient::request_file_read_only_approved(i32 request_id, i32 window_server_client_id, i32 parent_window_id, DeprecatedString const& path)
{
    request_file_handler(request_id, window_server_client_id, parent_window_id, path, Core::File::OpenMode::Read, ShouldPrompt::No);
}

void ConnectionFromClient::request_file(i32 request_id, i32 window_server_client_id, i32 parent_window_id, DeprecatedString const& path, Core::File::OpenMode requested_access)
{
    request_file_handler(request_id, window_server_client_id, parent_window_id, path, requested_access, ShouldPrompt::Yes);
}

void ConnectionFromClient::prompt_open_file(i32 request_id, i32 window_server_client_id, i32 parent_window_id, DeprecatedString const& window_title, DeprecatedString const& path_to_view, Core::File::OpenMode requested_access, Optional<Vector<GUI::FileTypeFilter>> const& allowed_file_types)
{
    auto relevant_permissions = requested_access & (Core::File::OpenMode::Read | Core::File::OpenMode::Write);
    VERIFY(relevant_permissions != Core::File::OpenMode::NotOpen);

    auto main_window = create_dummy_child_window(window_server_client_id, parent_window_id);

    auto user_picked_file = GUI::FilePicker::get_open_filepath(main_window, window_title, path_to_view, false, GUI::Dialog::ScreenPosition::CenterWithinParent, allowed_file_types);

    prompt_helper(request_id, user_picked_file, requested_access);
}

void ConnectionFromClient::prompt_save_file(i32 request_id, i32 window_server_client_id, i32 parent_window_id, DeprecatedString const& name, DeprecatedString const& ext, DeprecatedString const& path_to_view, Core::File::OpenMode requested_access)
{
    auto relevant_permissions = requested_access & (Core::File::OpenMode::Read | Core::File::OpenMode::Write);
    VERIFY(relevant_permissions != Core::File::OpenMode::NotOpen);

    auto main_window = create_dummy_child_window(window_server_client_id, parent_window_id);

    auto user_picked_file = GUI::FilePicker::get_save_filepath(main_window, name, ext, path_to_view);

    prompt_helper(request_id, user_picked_file, requested_access);
}

void ConnectionFromClient::prompt_helper(i32 request_id, Optional<DeprecatedString> const& user_picked_file, Core::File::OpenMode requested_access)
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

            async_handle_prompt_end(request_id, 0, IPC::File(*file.release_value(), IPC::File::CloseAfterSending), user_picked_file);
        }
    } else {
        async_handle_prompt_end(request_id, ECANCELED, Optional<IPC::File> {}, Optional<DeprecatedString> {});
    }
}

Messages::FileSystemAccessServer::ExposeWindowServerClientIdResponse ConnectionFromClient::expose_window_server_client_id()
{
    return GUI::ConnectionToWindowServer::the().expose_client_id();
}

}
