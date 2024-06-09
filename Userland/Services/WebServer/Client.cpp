/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 * Copyright (c) 2022, Thomas Keppler <serenity@tkeppler.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <AK/MemoryStream.h>
#include <AK/NumberFormat.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibCore/DateTime.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibCore/MimeData.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibHTTP/HttpRequest.h>
#include <LibHTTP/HttpResponse.h>
#include <LibURL/URL.h>
#include <WebServer/Client.h>
#include <WebServer/Configuration.h>
#include <stdio.h>
#include <unistd.h>

namespace WebServer {

Client::Client(NonnullOwnPtr<Core::BufferedTCPSocket> socket, Core::EventReceiver* parent)
    : Core::EventReceiver(parent)
    , m_socket(move(socket))
{
}

void Client::die()
{
    m_socket->close();
    deferred_invoke([this] { remove_from_parent(); });
}

void Client::start()
{
    m_socket->on_ready_to_read = [this] {
        if (auto result = on_ready_to_read(); result.is_error()) {
            result.error().visit(
                [](AK::Error const& error) {
                    warnln("Internal error: {}", error);
                },
                [](HTTP::HttpRequest::ParseError const& error) {
                    warnln("HTTP request parsing error: {}", HTTP::HttpRequest::parse_error_to_string(error));
                });

            die();
        }
    };
}

ErrorOr<void, Client::WrappedError> Client::on_ready_to_read()
{
    // FIXME: Mostly copied from LibWeb/WebDriver/Client.cpp. As noted there, this should be move the LibHTTP and made spec compliant.
    auto buffer = TRY(ByteBuffer::create_uninitialized(m_socket->buffer_size()));

    for (;;) {
        if (!TRY(m_socket->can_read_without_blocking()))
            break;

        auto data = TRY(m_socket->read_some(buffer));
        TRY(m_remaining_request.try_append(StringView { data }));

        if (m_socket->is_eof())
            break;
    }

    if (m_remaining_request.is_empty())
        return {};

    auto request = TRY(m_remaining_request.to_byte_buffer());
    dbgln_if(WEBSERVER_DEBUG, "Got raw request: '{}'", ByteString::copy(request));

    auto maybe_parsed_request = HTTP::HttpRequest::from_raw_request(TRY(m_remaining_request.to_byte_buffer()));
    if (maybe_parsed_request.is_error()) {
        if (maybe_parsed_request.error() == HTTP::HttpRequest::ParseError::RequestIncomplete) {
            // If request is not complete we need to wait for more data to arrive
            return {};
        }
        return maybe_parsed_request.error();
    }

    m_remaining_request.clear();

    TRY(handle_request(maybe_parsed_request.value()));

    return {};
}

ErrorOr<bool> Client::handle_request(HTTP::HttpRequest const& request)
{
    auto resource_decoded = URL::percent_decode(request.resource());

    if constexpr (WEBSERVER_DEBUG) {
        dbgln("Got HTTP request: {} {}", request.method_name(), request.resource());
        for (auto& header : request.headers().headers()) {
            dbgln("    {} => {}", header.name, header.value);
        }
    }

    if (request.method() != HTTP::HttpRequest::Method::GET) {
        TRY(send_error_response(501, request));
        return false;
    }

    // Check for credentials if they are required
    if (Configuration::the().credentials().has_value()) {
        bool has_authenticated = verify_credentials(request.headers().headers());
        if (!has_authenticated) {
            auto const basic_auth_header = "WWW-Authenticate: Basic realm=\"WebServer\", charset=\"UTF-8\""_string;
            Vector<String> headers {};
            TRY(headers.try_append(basic_auth_header));
            TRY(send_error_response(401, request, move(headers)));
            return false;
        }
    }

    auto requested_path = TRY(String::from_byte_string(LexicalPath::join("/"sv, resource_decoded).string()));
    dbgln_if(WEBSERVER_DEBUG, "Canonical requested path: '{}'", requested_path);

    auto real_path = TRY(String::formatted("{}{}", Configuration::the().document_root_path(), requested_path));

    if (FileSystem::is_directory(real_path.bytes_as_string_view())) {
        if (!resource_decoded.ends_with('/')) {
            TRY(send_redirect(TRY(String::formatted("{}/", requested_path)), request));
            return true;
        }

        auto index_html_path = TRY(String::formatted("{}/index.html", real_path));
        if (!FileSystem::exists(index_html_path)) {
            auto is_searchable_or_error = Core::System::access(real_path.bytes_as_string_view(), X_OK);
            if (is_searchable_or_error.is_error()) {
                TRY(send_error_response(403, request));
                return false;
            }

            TRY(handle_directory_listing(requested_path, real_path, request));
            return true;
        }
        real_path = index_html_path;
    }

    if (!FileSystem::exists(real_path.bytes_as_string_view())) {
        TRY(send_error_response(404, request));
        return false;
    }

    auto is_readable_or_error = Core::System::access(real_path.bytes_as_string_view(), R_OK);
    if (is_readable_or_error.is_error()) {
        TRY(send_error_response(403, request));
        return false;
    }

    if (FileSystem::is_device(real_path.bytes_as_string_view())) {
        TRY(send_error_response(403, request));
        return false;
    }

    auto stream = TRY(Core::File::open(real_path.bytes_as_string_view(), Core::File::OpenMode::Read));

    auto const info = ContentInfo {
        .type = TRY(String::from_utf8(Core::guess_mime_type_based_on_filename(real_path.bytes_as_string_view()))),
        .length = static_cast<u64>(TRY(FileSystem::size_from_stat(real_path.bytes_as_string_view())))
    };
    TRY(send_response(*stream, request, move(info)));
    return true;
}

ErrorOr<void> Client::send_response(Stream& response, HTTP::HttpRequest const& request, ContentInfo content_info)
{
    StringBuilder builder;
    TRY(builder.try_append("HTTP/1.0 200 OK\r\n"sv));
    TRY(builder.try_append("Server: WebServer (SerenityOS)\r\n"sv));
    TRY(builder.try_append("X-Frame-Options: SAMEORIGIN\r\n"sv));
    TRY(builder.try_append("X-Content-Type-Options: nosniff\r\n"sv));
    TRY(builder.try_append("Pragma: no-cache\r\n"sv));
    if (content_info.type == "text/plain")
        TRY(builder.try_appendff("Content-Type: {}; charset=utf-8\r\n", content_info.type));
    else
        TRY(builder.try_appendff("Content-Type: {}\r\n", content_info.type));
    TRY(builder.try_appendff("Content-Length: {}\r\n", content_info.length));
    TRY(builder.try_append("\r\n"sv));

    auto builder_contents = TRY(builder.to_byte_buffer());
    TRY(m_socket->write_until_depleted(builder_contents));
    log_response(200, request);

    char buffer[PAGE_SIZE];
    do {
        auto size = TRY(response.read_some({ buffer, sizeof(buffer) })).size();
        if (response.is_eof() && size == 0)
            break;

        ReadonlyBytes write_buffer { buffer, size };
        while (!write_buffer.is_empty()) {
            auto nwritten = TRY(m_socket->write_some(write_buffer));

            if (nwritten == 0) {
                dbgln("EEEEEE got 0 bytes written!");
            }

            write_buffer = write_buffer.slice(nwritten);
        }
    } while (true);

    auto keep_alive = false;
    if (auto it = request.headers().headers().find_if([](auto& header) { return header.name.equals_ignoring_ascii_case("Connection"sv); }); !it.is_end()) {
        if (it->value.trim_whitespace().equals_ignoring_ascii_case("keep-alive"sv))
            keep_alive = true;
    }
    if (!keep_alive)
        m_socket->close();

    return {};
}

ErrorOr<void> Client::send_redirect(StringView redirect_path, HTTP::HttpRequest const& request)
{
    StringBuilder builder;
    TRY(builder.try_append("HTTP/1.0 301 Moved Permanently\r\n"sv));
    TRY(builder.try_append("Location: "sv));
    TRY(builder.try_append(redirect_path));
    TRY(builder.try_append("\r\n"sv));
    TRY(builder.try_append("\r\n"sv));

    auto builder_contents = TRY(builder.to_byte_buffer());
    TRY(m_socket->write_until_depleted(builder_contents));

    log_response(301, request);
    return {};
}

static ByteString folder_image_data()
{
    static ByteString cache;
    if (cache.is_empty()) {
        auto file = Core::MappedFile::map("/res/icons/16x16/filetype-folder.png"sv).release_value_but_fixme_should_propagate_errors();
        // FIXME: change to TRY() and make method fallible
        cache = MUST(encode_base64(file->bytes())).to_byte_string();
    }
    return cache;
}

static ByteString file_image_data()
{
    static ByteString cache;
    if (cache.is_empty()) {
        auto file = Core::MappedFile::map("/res/icons/16x16/filetype-unknown.png"sv).release_value_but_fixme_should_propagate_errors();
        // FIXME: change to TRY() and make method fallible
        cache = MUST(encode_base64(file->bytes())).to_byte_string();
    }
    return cache;
}

ErrorOr<void> Client::handle_directory_listing(String const& requested_path, String const& real_path, HTTP::HttpRequest const& request)
{
    StringBuilder builder;

    TRY(builder.try_append("<!DOCTYPE html>\n"sv));
    TRY(builder.try_append("<html>\n"sv));
    TRY(builder.try_append("<head><meta charset=\"utf-8\">\n"sv));
    TRY(builder.try_append("<title>Index of "sv));
    TRY(builder.try_append(escape_html_entities(requested_path)));
    TRY(builder.try_append("</title><style>\n"sv));
    TRY(builder.try_append(".folder { width: 16px; height: 16px; background-image: url('data:image/png;base64,"sv));
    TRY(builder.try_append(folder_image_data()));
    TRY(builder.try_append("'); }\n"sv));
    TRY(builder.try_append(".file { width: 16px; height: 16px; background-image: url('data:image/png;base64,"sv));
    TRY(builder.try_append(file_image_data()));
    TRY(builder.try_append("'); }\n"sv));
    TRY(builder.try_append("</style></head><body>\n"sv));
    TRY(builder.try_append("<h1>Index of "sv));
    TRY(builder.try_append(escape_html_entities(requested_path)));
    TRY(builder.try_append("</h1>\n"sv));
    TRY(builder.try_append("<hr>\n"sv));
    TRY(builder.try_append("<code><table>\n"sv));

    Core::DirIterator dt(real_path.bytes_as_string_view());
    Vector<ByteString> names;
    while (dt.has_next())
        TRY(names.try_append(dt.next_path()));
    quick_sort(names);

    for (auto& name : names) {
        StringBuilder path_builder;
        TRY(path_builder.try_append(real_path));
        TRY(path_builder.try_append('/'));
        // NOTE: In the root directory of the webserver, ".." should be equal to ".", since we don't want
        //       the user to see e.g. the size of the parent directory (and it isn't unveiled, so stat fails).
        if (requested_path == "/" && name == "..")
            TRY(path_builder.try_append("."sv));
        else
            TRY(path_builder.try_append(name));

        auto st_or_error = Core::System::stat(path_builder.string_view());
        if (st_or_error.is_error()) {
            warnln("Skipping file: '{}'. {}", path_builder.string_view(), strerror(st_or_error.error().code()));
            continue;
        }

        auto st = st_or_error.release_value();

        bool is_directory = S_ISDIR(st.st_mode);

        TRY(builder.try_append("<tr>"sv));
        TRY(builder.try_appendff("<td><div class=\"{}\"></div></td>", is_directory ? "folder" : "file"));
        TRY(builder.try_append("<td><a href=\"./"sv));
        TRY(builder.try_append(URL::percent_encode(name)));
        // NOTE: For directories, we append a slash so we don't always hit the redirect case,
        //       which adds a slash anyways.
        if (is_directory)
            TRY(builder.try_append('/'));
        TRY(builder.try_append("\">"sv));
        TRY(builder.try_append(escape_html_entities(name)));
        TRY(builder.try_append("</a></td><td>&nbsp;</td>"sv));

        TRY(builder.try_appendff("<td>{:10}</td><td>&nbsp;</td>", is_directory ? "-"_string : human_readable_size(st.st_size)));
        TRY(builder.try_append("<td>"sv));
        TRY(builder.try_append(TRY(Core::DateTime::from_timestamp(st.st_mtime).to_string())));
        TRY(builder.try_append("</td>"sv));
        TRY(builder.try_append("</tr>\n"sv));
    }

    TRY(builder.try_append("</table></code>\n"sv));
    TRY(builder.try_append("<hr>\n"sv));
    TRY(builder.try_append("<i>Generated by WebServer (SerenityOS)</i>\n"sv));
    TRY(builder.try_append("</body>\n"sv));
    TRY(builder.try_append("</html>\n"sv));

    auto response = builder.to_byte_string();
    FixedMemoryStream stream { response.bytes() };
    return send_response(stream, request, { .type = "text/html"_string, .length = response.length() });
}

ErrorOr<void> Client::send_error_response(unsigned code, HTTP::HttpRequest const& request, Vector<String> const& headers)
{
    auto reason_phrase = HTTP::HttpResponse::reason_phrase_for_code(code);

    StringBuilder content_builder;
    TRY(content_builder.try_append("<!DOCTYPE html><html><body><h1>"sv));
    TRY(content_builder.try_appendff("{} ", code));
    TRY(content_builder.try_append(reason_phrase));
    TRY(content_builder.try_append("</h1></body></html>"sv));

    StringBuilder header_builder;
    TRY(header_builder.try_appendff("HTTP/1.0 {} ", code));
    TRY(header_builder.try_append(reason_phrase));
    TRY(header_builder.try_append("\r\n"sv));

    for (auto& header : headers) {
        TRY(header_builder.try_append(header));
        TRY(header_builder.try_append("\r\n"sv));
    }
    TRY(header_builder.try_append("Content-Type: text/html; charset=UTF-8\r\n"sv));
    TRY(header_builder.try_appendff("Content-Length: {}\r\n", content_builder.length()));
    TRY(header_builder.try_append("\r\n"sv));
    TRY(m_socket->write_until_depleted(TRY(header_builder.to_byte_buffer())));
    TRY(m_socket->write_until_depleted(TRY(content_builder.to_byte_buffer())));

    log_response(code, request);
    return {};
}

void Client::log_response(unsigned code, HTTP::HttpRequest const& request)
{
    outln("{} :: {:03d} :: {} {}", Core::DateTime::now().to_byte_string(), code, request.method_name(), request.url().serialize().substring(1));
}

bool Client::verify_credentials(Vector<HTTP::Header> const& headers)
{
    VERIFY(Configuration::the().credentials().has_value());
    auto& configured_credentials = Configuration::the().credentials().value();
    for (auto& header : headers) {
        if (header.name.equals_ignoring_ascii_case("Authorization"sv)) {
            auto provided_credentials = HTTP::HttpRequest::parse_http_basic_authentication_header(header.value);
            if (provided_credentials.has_value() && configured_credentials.username == provided_credentials->username && configured_credentials.password == provided_credentials->password)
                return true;
        }
    }
    return false;
}

}
