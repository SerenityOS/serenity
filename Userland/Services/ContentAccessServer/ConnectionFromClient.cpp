/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 * Copyright (c) 2021-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumberFormat.h>
#include <ContentAccessServer/ConnectionFromClient.h>
#include <LibCore/File.h>
#include <LibCore/Stream.h>
#include <LibGUI/Application.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Progressbar.h>
#include <LibProtocol/Request.h>

namespace ContentAccessServer {

static HashMap<int, NonnullRefPtr<ConnectionFromClient>> s_connections;

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
    : IPC::ConnectionFromClient<ContentAccessClientEndpoint, ContentAccessServerEndpoint>(*this, move(socket), 1)
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

ConnectionFromClient::ProgressObject ConnectionFromClient::create_download_progress_window(i32 window_server_client_id, i32 parent_window_id)
{
    auto main_window = create_dummy_child_window(window_server_client_id, parent_window_id);
    auto dialog = GUI::Window::construct(main_window);
    dialog->set_title("Downloading Remote File..."sv);
    dialog->set_rect(Gfx::IntRect { 0, 0, 400, 100 });
    dialog->center_within(*main_window);
    auto frame = MUST(dialog->set_main_widget<GUI::Widget>());
    MUST(frame->load_from_gml(R"gmlgmlgml(
        @GUI::Widget {
            layout: @GUI::VerticalBoxLayout {
                margins: [20]
            }

            @GUI::Label {
                name: "label"
                text_alignment: "TopLeft"
                fixed_height: 32
            }

            @GUI::Progressbar {
                name: "progressbar"
                fixed_height: 28
            }
        }
    )gmlgmlgml"sv));
    frame->set_fill_with_background_color(true);

    auto& progressbar = *frame->find_descendant_of_type_named<GUI::Progressbar>("progressbar");
    auto& label = *frame->find_descendant_of_type_named<GUI::Label>("label");

    dialog->show();

    return {
        .main_window = main_window.release_nonnull(),
        .dialog = move(dialog),
        .update = [&progressbar, &label](AK::URL const& url, u32 current, Optional<u32> total) {
            if (!total.has_value()) {
                label.set_text(DeprecatedString::formatted("Downloading from {}", url));
                progressbar.set_visible(false);
                return;
            }

            label.set_text(DeprecatedString::formatted("Downloading {} from {}", human_readable_size(*total), url));
            progressbar.set_range(0, *total);
            progressbar.set_value(current);
            progressbar.set_format(GUI::Progressbar::Percentage);
        },
    };
}

void ConnectionFromClient::request_url_handler(i32 request_id, i32 window_server_client_id, i32 parent_window_id, URL const& url, ShouldPrompt prompt)
{
    bool approved = false;
    auto maybe_permissions = m_approved_files.get(url);

    if (maybe_permissions.has_value())
        approved = has_flag(maybe_permissions.value(), Core::Stream::OpenMode::Read);

    if (!approved) {
        StringView access_string = "read from"sv;

        auto pid = this->socket().peer_pid().release_value_but_fixme_should_propagate_errors();
        auto exe_link = LexicalPath("/proc").append(DeprecatedString::number(pid)).append("exe"sv).string();
        auto exe_path = Core::File::real_path_for(exe_link);

        auto main_window = create_dummy_child_window(window_server_client_id, parent_window_id);

        if (prompt == ShouldPrompt::Yes) {
            auto exe_name = LexicalPath::basename(exe_path);
            auto result = GUI::MessageBox::show(
                main_window,
                DeprecatedString::formatted("Allow {} ({}) to {} \"{}\"?", exe_name, pid, access_string, url),
                "Remote Access Permissions Requested"sv,
                GUI::MessageBox::Type::Warning,
                GUI::MessageBox::InputType::YesNo);
            approved = result == GUI::MessageBox::ExecResult::Yes;
        } else {
            approved = true;
        }

        if (approved) {
            auto new_permissions = Core::Stream::OpenMode::Read;

            if (maybe_permissions.has_value())
                new_permissions |= maybe_permissions.value();

            m_approved_files.set(url, new_permissions);
        }
    }

    if (!approved) {
        async_handle_prompt_end(request_id, EPERM, Optional<IPC::File> {}, url);
        return;
    }

    if (url.scheme() == "file"sv) {
        auto file = Core::Stream::File::open(url.path(), Core::Stream::OpenMode::Read);
        if (file.is_error()) {
            dbgln("ContentAccessServer: Couldn't open {}, error {}", url.path(), file.error());
            async_handle_prompt_end(request_id, file.error().code(), Optional<IPC::File> {}, url);
        } else {
            async_handle_prompt_end(request_id, 0, IPC::File(*file.release_value(), IPC::File::CloseAfterSending), url);
        }

        return;
    }

    if (!ensure_request_client()) {
        async_handle_prompt_end(request_id, ENOTCONN, Optional<IPC::File> {}, url);
        return;
    }

    char name_template[] = "/tmp/url-request.XXXXXX";
    auto fd = mkstemp(name_template);
    if (fd < 0) {
        async_handle_prompt_end(request_id, errno, Optional<IPC::File> {}, url);
        return;
    }
    ScopeGuard unlink_temp_file = [&] {
        unlink(name_template);
    };

    auto file_stream_result = Core::Stream::File::adopt_fd(fd, Core::Stream::OpenMode::ReadWrite);
    if (file_stream_result.is_error()) {
        async_handle_prompt_end(request_id, -2, Optional<IPC::File> {}, url);
        return;
    }

    auto& unbuffered_file_stream = *file_stream_result.value();
    auto buffered_stream_result = Core::Stream::BufferedFile::create(file_stream_result.release_value(), 4 * MiB);
    if (buffered_stream_result.is_error()) {
        async_handle_prompt_end(request_id, buffered_stream_result.error().code(), Optional<IPC::File> {}, url);
        return;
    }

    auto file_stream = buffered_stream_result.release_value();

    auto request = m_request_client->start_request("GET"sv, url);
    if (!request) {
        async_handle_prompt_end(request_id, ENETRESET, Optional<IPC::File> {}, url);
        return;
    }

    auto& request_object = *request;

    m_active_requests.set(url, request.release_nonnull());

    auto& stream = *file_stream;
    IPC::File file { unbuffered_file_stream, IPC::File::CloseAfterSending };

    auto progress = create_download_progress_window(window_server_client_id, parent_window_id);
    request_object.on_finish = [=, this, file = move(file), file_stream = move(file_stream)](bool success, auto) mutable {
        if (success) {
            lseek(file.fd(), 0, SEEK_SET);
            async_handle_prompt_end(request_id, 0, move(file), url);
        } else {
            async_handle_prompt_end(request_id, EBADF, Optional<IPC::File> {}, url);
        }

        deferred_invoke([&] { m_active_requests.remove(url); });
    };

    request_object.on_progress = [=, progress = move(progress)](Optional<u32> total, u32 current) {
        Core::EventLoop::current().pump(Core::EventLoop::WaitMode::PollForEvents);
        progress.update(url, current, total);
    };

    request_object.stream_into(stream);
}

bool ConnectionFromClient::ensure_request_client()
{
    if (m_request_client)
        return true;

    auto result = Protocol::RequestClient::try_create();
    if (result.is_error()) {
        dbgln("Failed to create a RequestClient, subsequent URL access requests will likely fail");
        return false;
    }

    m_request_client = result.release_value();
    return true;
}

void ConnectionFromClient::request_url_read_only(i32 request_id, i32 window_server_client_id, i32 parent_window_id, URL const& url, bool should_prompt)
{
    request_url_handler(request_id, window_server_client_id, parent_window_id, url, should_prompt ? ShouldPrompt::Yes : ShouldPrompt::No);
}

void ConnectionFromClient::prompt_open_file(i32 request_id, i32 window_server_client_id, i32 parent_window_id, DeprecatedString const& window_title, DeprecatedString const& path_to_view, Core::Stream::OpenMode requested_access)
{
    auto relevant_permissions = requested_access & (Core::Stream::OpenMode::Read | Core::Stream::OpenMode::Write);
    VERIFY(relevant_permissions != Core::Stream::OpenMode::NotOpen);

    auto main_window = create_dummy_child_window(window_server_client_id, parent_window_id);
    auto file_path = GUI::FilePicker::get_open_filepath(main_window, window_title, path_to_view);
    if (!file_path.has_value())
        return async_handle_prompt_end(request_id, -1, Optional<IPC::File> {}, Optional<URL> {});

    auto user_picked_url = URL::create_with_file_scheme(file_path.release_value());
    prompt_helper(request_id, user_picked_url, requested_access);
}

void ConnectionFromClient::prompt_helper(i32 request_id, URL const& user_picked_url, Core::Stream::OpenMode requested_access)
{
    if (user_picked_url.scheme() == "file"sv) {
        VERIFY(user_picked_url.path().starts_with("/"sv));
        auto file = Core::Stream::File::open(user_picked_url.path(), requested_access);

        if (file.is_error()) {
            dbgln("ContentAccessServer: Couldn't open {}, error {}", user_picked_url.path(), file.error());
            return async_handle_prompt_end(request_id, file.error().code(), Optional<IPC::File> {}, user_picked_url);
        }

        auto maybe_permissions = m_approved_files.get(user_picked_url);
        auto new_permissions = requested_access & (Core::Stream::OpenMode::Read | Core::Stream::OpenMode::Write);
        if (maybe_permissions.has_value())
            new_permissions |= maybe_permissions.value();
        m_approved_files.set(user_picked_url, new_permissions);

        return async_handle_prompt_end(request_id, 0, IPC::File(*file.release_value(), IPC::File::CloseAfterSending), user_picked_url);
    }

    if (has_flag(requested_access, Core::Stream::OpenMode::Write)) {
        dbgln("ContentAccessServer: Attempted to access a remote URL ({}), can't do that here", user_picked_url);
        return async_handle_prompt_end(request_id, ENOTSUP, Optional<IPC::File> {}, user_picked_url);
    }

    // FIXME: Implement this path after prompt_open_file() can actually hand us a remote URL.
    TODO();
}

Messages::ContentAccessServer::ExposeWindowServerClientIdResponse ConnectionFromClient::expose_window_server_client_id()
{
    return GUI::ConnectionToWindowServer::the().expose_client_id();
}

}
