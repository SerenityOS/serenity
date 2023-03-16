/*
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/String.h>
#include <LibWebView/ViewImplementation.h>

namespace WebView {

WebContentClient& ViewImplementation::client()
{
    VERIFY(m_client_state.client);
    return *m_client_state.client;
}

WebContentClient const& ViewImplementation::client() const
{
    VERIFY(m_client_state.client);
    return *m_client_state.client;
}

void ViewImplementation::load(AK::URL const& url)
{
    m_url = url;
    client().async_load_url(url);
}

void ViewImplementation::load_html(StringView html, AK::URL const& url)
{
    m_url = url;
    client().async_load_html(html, url);
}

void ViewImplementation::load_empty_document()
{
    load_html(""sv, {});
}

void ViewImplementation::zoom_in()
{
    if (m_zoom_level >= ZOOM_MAX_LEVEL)
        return;
    m_zoom_level += ZOOM_STEP;
    update_zoom();
}

void ViewImplementation::zoom_out()
{
    if (m_zoom_level <= ZOOM_MIN_LEVEL)
        return;
    m_zoom_level -= ZOOM_STEP;
    update_zoom();
}

void ViewImplementation::reset_zoom()
{
    m_zoom_level = 1.0f;
    update_zoom();
}

void ViewImplementation::set_preferred_color_scheme(Web::CSS::PreferredColorScheme color_scheme)
{
    client().async_set_preferred_color_scheme(color_scheme);
}

DeprecatedString ViewImplementation::selected_text()
{
    return client().get_selected_text();
}

void ViewImplementation::select_all()
{
    client().async_select_all();
}

void ViewImplementation::get_source()
{
    client().async_get_source();
}

void ViewImplementation::inspect_dom_tree()
{
    client().async_inspect_dom_tree();
}

void ViewImplementation::inspect_accessibility_tree()
{
    client().async_inspect_accessibility_tree();
}

ErrorOr<ViewImplementation::DOMNodeProperties> ViewImplementation::inspect_dom_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement> pseudo_element)
{
    auto response = client().inspect_dom_node(node_id, pseudo_element);
    if (!response.has_style())
        return Error::from_string_view("Inspected node returned no style"sv);
    return DOMNodeProperties {
        .computed_style_json = TRY(String::from_deprecated_string(response.take_computed_style())),
        .resolved_style_json = TRY(String::from_deprecated_string(response.take_resolved_style())),
        .custom_properties_json = TRY(String::from_deprecated_string(response.take_custom_properties())),
        .node_box_sizing_json = TRY(String::from_deprecated_string(response.take_node_box_sizing())),
    };
}

void ViewImplementation::clear_inspected_dom_node()
{
    client().inspect_dom_node(0, {});
}

i32 ViewImplementation::get_hovered_node_id()
{
    return client().get_hovered_node_id();
}

void ViewImplementation::debug_request(DeprecatedString const& request, DeprecatedString const& argument)
{
    client().async_debug_request(request, argument);
}

void ViewImplementation::run_javascript(StringView js_source)
{
    client().async_run_javascript(js_source);
}

#if !defined(AK_OS_SERENITY)

ErrorOr<NonnullRefPtr<WebView::WebContentClient>> ViewImplementation::launch_web_content_process(ReadonlySpan<String> candidate_web_content_paths)
{
    int socket_fds[2] {};
    TRY(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, socket_fds));

    int ui_fd = socket_fds[0];
    int wc_fd = socket_fds[1];

    int fd_passing_socket_fds[2] {};
    TRY(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, fd_passing_socket_fds));

    int ui_fd_passing_fd = fd_passing_socket_fds[0];
    int wc_fd_passing_fd = fd_passing_socket_fds[1];

    if (auto child_pid = TRY(Core::System::fork()); child_pid == 0) {
        TRY(Core::System::close(ui_fd_passing_fd));
        TRY(Core::System::close(ui_fd));

        auto takeover_string = TRY(String::formatted("WebContent:{}", wc_fd));
        TRY(Core::System::setenv("SOCKET_TAKEOVER"sv, takeover_string, true));

        auto webcontent_fd_passing_socket_string = TRY(String::number(wc_fd_passing_fd));

        auto arguments = Array {
            "WebContent"sv,
            "--webcontent-fd-passing-socket"sv,
            webcontent_fd_passing_socket_string
        };

        ErrorOr<void> result;
        for (auto const& path : candidate_web_content_paths) {
            result = Core::System::exec(path, arguments, Core::System::SearchInPath::Yes);
            if (!result.is_error())
                break;
        }

        if (result.is_error())
            warnln("Could not launch any of {}: {}", candidate_web_content_paths, result.error());
        VERIFY_NOT_REACHED();
    }

    TRY(Core::System::close(wc_fd_passing_fd));
    TRY(Core::System::close(wc_fd));

    auto socket = TRY(Core::LocalSocket::adopt_fd(ui_fd));
    TRY(socket->set_blocking(true));

    auto new_client = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) WebView::WebContentClient(move(socket), *this)));
    new_client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(ui_fd_passing_fd)));

    return new_client;
}

#endif

}
