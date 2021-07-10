/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <FileSystemAccessServer/ClientConnection.h>
#include <LibCore/File.h>
#include <LibCore/IODevice.h>
#include <LibGUI/Application.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/MessageBox.h>

namespace FileSystemAccessServer {

static HashMap<int, NonnullRefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> socket, int client_id)
    : IPC::ClientConnection<FileSystemAccessClientEndpoint, FileSystemAccessServerEndpoint>(*this, move(socket), client_id)
{
    s_connections.set(client_id, *this);
}

ClientConnection::~ClientConnection()
{
}

void ClientConnection::die()
{
    s_connections.remove(client_id());
    GUI::Application::the()->quit();
    exit(0);
}

Messages::FileSystemAccessServer::RequestFileResponse ClientConnection::request_file(String const& path, Core::OpenMode const& requested_access)
{
    VERIFY(path.starts_with("/"sv));

    bool approved = false;
    auto maybe_permissions = m_approved_files.get(path);

    auto relevant_permissions = requested_access & (Core::OpenMode::ReadOnly | Core::OpenMode::WriteOnly);
    VERIFY(relevant_permissions != Core::OpenMode::NotOpen);

    if (maybe_permissions.has_value())
        approved = has_flag(maybe_permissions.value(), relevant_permissions);

    if (!approved) {
        StringBuilder builder;
        if (has_flag(requested_access, Core::OpenMode::ReadOnly))
            builder.append('r');

        if (has_flag(requested_access, Core::OpenMode::WriteOnly))
            builder.append('w');

        auto access_string = builder.to_string();

        auto pid = this->socket().peer_pid();
        auto exe_link = LexicalPath("/proc").append(String::number(pid)).append("exe").string();
        auto exe_path = Core::File::real_path_for(exe_link);
        auto exe_name = LexicalPath::basename(exe_path);

        auto result = GUI::MessageBox::show(nullptr, String::formatted("Give {} ({}) \"{}\" access to \"{}\"?", exe_name, pid, access_string, path), "File Permissions Requested", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNo);

        approved = result == GUI::MessageBox::ExecYes;

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

            return { errno, Optional<IPC::File> {} };
        }

        return { 0, IPC::File(file.value()->leak_fd(), IPC::File::CloseAfterSending) };
    }

    return { -1, Optional<IPC::File> {} };
}

Messages::FileSystemAccessServer::PromptOpenFileResponse ClientConnection::prompt_open_file(String const& path_to_view, Core::OpenMode const& requested_access)
{
    auto relevant_permissions = requested_access & (Core::OpenMode::ReadOnly | Core::OpenMode::WriteOnly);
    VERIFY(relevant_permissions != Core::OpenMode::NotOpen);

    auto main_window = GUI::Window::construct();
    auto user_picked_file = GUI::FilePicker::get_open_filepath(main_window, "Select file", path_to_view);

    return prompt_helper<Messages::FileSystemAccessServer::PromptOpenFileResponse>(user_picked_file, requested_access);
}

Messages::FileSystemAccessServer::PromptSaveFileResponse ClientConnection::prompt_save_file(String const& name, String const& ext, String const& path_to_view, Core::OpenMode const& requested_access)
{
    auto relevant_permissions = requested_access & (Core::OpenMode::ReadOnly | Core::OpenMode::WriteOnly);
    VERIFY(relevant_permissions != Core::OpenMode::NotOpen);

    auto main_window = GUI::Window::construct();
    auto user_picked_file = GUI::FilePicker::get_save_filepath(main_window, name, ext, path_to_view);

    return prompt_helper<Messages::FileSystemAccessServer::PromptSaveFileResponse>(user_picked_file, requested_access);
}

template<typename T>
T ClientConnection::prompt_helper(Optional<String> const& user_picked_file, Core::OpenMode const& requested_access)
{
    if (user_picked_file.has_value()) {
        VERIFY(user_picked_file->starts_with("/"sv));
        auto file = Core::File::open(user_picked_file.value(), requested_access);

        if (file.is_error()) {
            dbgln("FileSystemAccessServer: Couldn't open {}, error {}", user_picked_file.value(), file.error());

            return { errno, Optional<IPC::File> {}, user_picked_file.value() };
        }

        auto maybe_permissions = m_approved_files.get(user_picked_file.value());
        auto new_permissions = requested_access & (Core::OpenMode::ReadOnly | Core::OpenMode::WriteOnly);
        if (maybe_permissions.has_value())
            new_permissions |= maybe_permissions.value();

        m_approved_files.set(user_picked_file.value(), new_permissions);

        return { 0, IPC::File(file.value()->leak_fd(), IPC::File::CloseAfterSending), user_picked_file.value() };
    }

    return { -1, Optional<IPC::File> {}, Optional<String> {} };
}

}
