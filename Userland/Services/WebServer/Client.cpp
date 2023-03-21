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
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <LibCore/DateTime.h>
#include <LibCore/DeprecatedFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibCore/MimeData.h>
#include <LibFileSystem/FileSystem.h>
#include <LibHTTP/HttpRequest.h>
#include <LibHTTP/HttpResponse.h>
#include <WebServer/Client.h>
#include <WebServer/Configuration.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

namespace WebServer {

Client::Client(NonnullOwnPtr<Core::BufferedTCPSocket> socket, Core::Object* parent)
    : Core::Object(parent)
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
        StringBuilder builder;

        auto maybe_buffer = ByteBuffer::create_uninitialized(m_socket->buffer_size());
        if (maybe_buffer.is_error()) {
            warnln("Could not create buffer for client: {}", maybe_buffer.error());
            die();
            return;
        }

        auto buffer = maybe_buffer.release_value();
        for (;;) {
            auto maybe_can_read = m_socket->can_read_without_blocking();
            if (maybe_can_read.is_error()) {
                warnln("Failed to get the blocking status for the socket: {}", maybe_can_read.error());
                die();
                return;
            }

            if (!maybe_can_read.value())
                break;

            auto maybe_bytes_read = m_socket->read_until_any_of(buffer, Array { "\r"sv, "\n"sv, "\r\n"sv });
            if (maybe_bytes_read.is_error()) {
                warnln("Failed to read a line from the request: {}", maybe_bytes_read.error());
                die();
                return;
            }

            if (m_socket->is_eof()) {
                die();
                break;
            }

            builder.append(StringView { maybe_bytes_read.value() });
            builder.append("\r\n"sv);
        }

        auto request = builder.to_byte_buffer().release_value_but_fixme_should_propagate_errors();
        dbgln_if(WEBSERVER_DEBUG, "Got raw request: '{}'", DeprecatedString::copy(request));

        auto maybe_did_handle = handle_request(request);
        if (maybe_did_handle.is_error()) {
            warnln("Failed to handle the request: {}", maybe_did_handle.error());
        }

        die();
    };
}

ErrorOr<bool> Client::handle_request(ReadonlyBytes raw_request)
{
    auto request_or_error = HTTP::HttpRequest::from_raw_request(raw_request);
    if (!request_or_error.has_value())
        return false;
    auto& request = request_or_error.value();
    auto resource_decoded = URL::percent_decode(request.resource());

    if constexpr (WEBSERVER_DEBUG) {
        dbgln("Got HTTP request: {} {}", request.method_name(), request.resource());
        for (auto& header : request.headers()) {
            dbgln("    {} => {}", header.name, header.value);
        }
    }

    if (request.method() != HTTP::HttpRequest::Method::GET) {
        TRY(send_error_response(501, request));
        return false;
    }

    // Check for credentials if they are required
    if (Configuration::the().credentials().has_value()) {
        bool has_authenticated = verify_credentials(request.headers());
        if (!has_authenticated) {
            auto const basic_auth_header = TRY("WWW-Authenticate: Basic realm=\"WebServer\", charset=\"UTF-8\""_string);
            Vector<String> headers {};
            TRY(headers.try_append(basic_auth_header));
            TRY(send_error_response(401, request, move(headers)));
            return false;
        }
    }

    auto requested_path = TRY(String::from_deprecated_string(LexicalPath::join("/"sv, resource_decoded).string()));
    dbgln_if(WEBSERVER_DEBUG, "Canonical requested path: '{}'", requested_path);

    StringBuilder path_builder;
    path_builder.append(Configuration::the().document_root_path());
    path_builder.append(requested_path);
    auto real_path = TRY(path_builder.to_string());

    if (FileSystem::is_directory(real_path.bytes_as_string_view())) {
        if (!resource_decoded.ends_with('/')) {
            StringBuilder red;

            red.append(requested_path);
            red.append("/"sv);

            TRY(send_redirect(red.to_deprecated_string(), request));
            return true;
        }

        StringBuilder index_html_path_builder;
        index_html_path_builder.append(real_path);
        index_html_path_builder.append("/index.html"sv);
        auto index_html_path = TRY(index_html_path_builder.to_string());
        if (!FileSystem::exists(index_html_path)) {
            TRY(handle_directory_listing(requested_path, real_path, request));
            return true;
        }
        real_path = index_html_path;
    }

    auto file = Core::DeprecatedFile::construct(real_path.bytes_as_string_view());
    if (!file->open(Core::OpenMode::ReadOnly)) {
        TRY(send_error_response(404, request));
        return false;
    }

    if (file->is_device()) {
        TRY(send_error_response(403, request));
        return false;
    }

    auto stream = TRY(Core::File::open(real_path.bytes_as_string_view(), Core::File::OpenMode::Read));

    auto const info = ContentInfo {
        .type = TRY(String::from_utf8(Core::guess_mime_type_based_on_filename(real_path.bytes_as_string_view()))),
        .length = TRY(FileSystem::size(real_path.bytes_as_string_view()))
    };
    TRY(send_response(*stream, request, move(info)));
    return true;
}

ErrorOr<void> Client::send_response(Stream& response, HTTP::HttpRequest const& request, ContentInfo content_info)
{
    StringBuilder builder;
    builder.append("HTTP/1.0 200 OK\r\n"sv);
    builder.append("Server: WebServer (SerenityOS)\r\n"sv);
    builder.append("X-Frame-Options: SAMEORIGIN\r\n"sv);
    builder.append("X-Content-Type-Options: nosniff\r\n"sv);
    builder.append("Pragma: no-cache\r\n"sv);
    if (content_info.type == "text/plain")
        builder.appendff("Content-Type: {}; charset=utf-8\r\n", content_info.type);
    else
        builder.appendff("Content-Type: {}\r\n", content_info.type);
    builder.appendff("Content-Length: {}\r\n", content_info.length);
    builder.append("\r\n"sv);

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
    if (auto it = request.headers().find_if([](auto& header) { return header.name.equals_ignoring_ascii_case("Connection"sv); }); !it.is_end()) {
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
    builder.append("HTTP/1.0 301 Moved Permanently\r\n"sv);
    builder.append("Location: "sv);
    builder.append(redirect_path);
    builder.append("\r\n"sv);
    builder.append("\r\n"sv);

    auto builder_contents = TRY(builder.to_byte_buffer());
    TRY(m_socket->write_until_depleted(builder_contents));

    log_response(301, request);
    return {};
}

static DeprecatedString folder_image_data()
{
    static DeprecatedString cache;
    if (cache.is_empty()) {
        auto file = Core::MappedFile::map("/res/icons/16x16/filetype-folder.png"sv).release_value_but_fixme_should_propagate_errors();
        // FIXME: change to TRY() and make method fallible
        cache = MUST(encode_base64(file->bytes())).to_deprecated_string();
    }
    return cache;
}

static DeprecatedString file_image_data()
{
    static DeprecatedString cache;
    if (cache.is_empty()) {
        auto file = Core::MappedFile::map("/res/icons/16x16/filetype-unknown.png"sv).release_value_but_fixme_should_propagate_errors();
        // FIXME: change to TRY() and make method fallible
        cache = MUST(encode_base64(file->bytes())).to_deprecated_string();
    }
    return cache;
}

ErrorOr<void> Client::handle_directory_listing(String const& requested_path, String const& real_path, HTTP::HttpRequest const& request)
{
    StringBuilder builder;

    builder.append("<!DOCTYPE html>\n"sv);
    builder.append("<html>\n"sv);
    builder.append("<head><meta charset=\"utf-8\">\n"sv);
    builder.append("<title>Index of "sv);
    builder.append(escape_html_entities(requested_path));
    builder.append("</title><style>\n"sv);
    builder.append(".folder { width: 16px; height: 16px; background-image: url('data:image/png;base64,"sv);
    builder.append(folder_image_data());
    builder.append("'); }\n"sv);
    builder.append(".file { width: 16px; height: 16px; background-image: url('data:image/png;base64,"sv);
    builder.append(file_image_data());
    builder.append("'); }\n"sv);
    builder.append("</style></head><body>\n"sv);
    builder.append("<h1>Index of "sv);
    builder.append(escape_html_entities(requested_path));
    builder.append("</h1>\n"sv);
    builder.append("<hr>\n"sv);
    builder.append("<code><table>\n"sv);

    Core::DirIterator dt(real_path.bytes_as_string_view());
    Vector<DeprecatedString> names;
    while (dt.has_next())
        names.append(dt.next_path());
    quick_sort(names);

    for (auto& name : names) {
        StringBuilder path_builder;
        path_builder.append(real_path);
        path_builder.append('/');
        // NOTE: In the root directory of the webserver, ".." should be equal to ".", since we don't want
        //       the user to see e.g. the size of the parent directory (and it isn't unveiled, so stat fails).
        if (requested_path == "/" && name == "..")
            path_builder.append("."sv);
        else
            path_builder.append(name);

        struct stat st;
        memset(&st, 0, sizeof(st));
        int rc = stat(path_builder.to_deprecated_string().characters(), &st);
        if (rc < 0) {
            perror("stat");
        }

        bool is_directory = S_ISDIR(st.st_mode);

        builder.append("<tr>"sv);
        builder.appendff("<td><div class=\"{}\"></div></td>", is_directory ? "folder" : "file");
        builder.append("<td><a href=\""sv);
        builder.append(URL::percent_encode(name));
        // NOTE: For directories, we append a slash so we don't always hit the redirect case,
        //       which adds a slash anyways.
        if (is_directory)
            builder.append('/');
        builder.append("\">"sv);
        builder.append(escape_html_entities(name));
        builder.append("</a></td><td>&nbsp;</td>"sv);

        builder.appendff("<td>{:10}</td><td>&nbsp;</td>", st.st_size);
        builder.append("<td>"sv);
        builder.append(Core::DateTime::from_timestamp(st.st_mtime).to_deprecated_string());
        builder.append("</td>"sv);
        builder.append("</tr>\n"sv);
    }

    builder.append("</table></code>\n"sv);
    builder.append("<hr>\n"sv);
    builder.append("<i>Generated by WebServer (SerenityOS)</i>\n"sv);
    builder.append("</body>\n"sv);
    builder.append("</html>\n"sv);

    auto response = builder.to_deprecated_string();
    FixedMemoryStream stream { response.bytes() };
    return send_response(stream, request, { .type = TRY("text/html"_string), .length = response.length() });
}

ErrorOr<void> Client::send_error_response(unsigned code, HTTP::HttpRequest const& request, Vector<String> const& headers)
{
    auto reason_phrase = HTTP::HttpResponse::reason_phrase_for_code(code);

    StringBuilder content_builder;
    content_builder.append("<!DOCTYPE html><html><body><h1>"sv);
    content_builder.appendff("{} ", code);
    content_builder.append(reason_phrase);
    content_builder.append("</h1></body></html>"sv);

    StringBuilder header_builder;
    header_builder.appendff("HTTP/1.0 {} ", code);
    header_builder.append(reason_phrase);
    header_builder.append("\r\n"sv);

    for (auto& header : headers) {
        header_builder.append(header);
        header_builder.append("\r\n"sv);
    }
    header_builder.append("Content-Type: text/html; charset=UTF-8\r\n"sv);
    header_builder.appendff("Content-Length: {}\r\n", content_builder.length());
    header_builder.append("\r\n"sv);
    TRY(m_socket->write_until_depleted(TRY(header_builder.to_byte_buffer())));
    TRY(m_socket->write_until_depleted(TRY(content_builder.to_byte_buffer())));

    log_response(code, request);
    return {};
}

void Client::log_response(unsigned code, HTTP::HttpRequest const& request)
{
    outln("{} :: {:03d} :: {} {}", Core::DateTime::now().to_deprecated_string(), code, request.method_name(), request.url().serialize().substring(1));
}

bool Client::verify_credentials(Vector<HTTP::HttpRequest::Header> const& headers)
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
