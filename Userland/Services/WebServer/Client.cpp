/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
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
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/FileStream.h>
#include <LibCore/MappedFile.h>
#include <LibCore/MimeData.h>
#include <LibHTTP/HttpRequest.h>
#include <LibHTTP/HttpResponse.h>
#include <WebServer/Client.h>
#include <WebServer/Configuration.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

namespace WebServer {

Client::Client(NonnullOwnPtr<Core::Stream::BufferedTCPSocket> socket, Core::Object* parent)
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

            auto maybe_nread = m_socket->read_until_any_of(buffer, Array { "\r"sv, "\n"sv, "\r\n"sv });
            if (maybe_nread.is_error()) {
                warnln("Failed to read a line from the request: {}", maybe_nread.error());
                die();
                return;
            }

            if (m_socket->is_eof()) {
                die();
                break;
            }

            builder.append(StringView { buffer.data(), maybe_nread.value() });
            builder.append("\r\n");
        }

        auto request = builder.to_byte_buffer();
        dbgln_if(WEBSERVER_DEBUG, "Got raw request: '{}'", String::copy(request));

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
            TRY(send_error_response(401, request, { "WWW-Authenticate: Basic realm=\"WebServer\", charset=\"UTF-8\"" }));
            return false;
        }
    }

    auto requested_path = LexicalPath::join("/", request.resource()).string();
    dbgln_if(WEBSERVER_DEBUG, "Canonical requested path: '{}'", requested_path);

    StringBuilder path_builder;
    path_builder.append(Configuration::the().root_path());
    path_builder.append(requested_path);
    auto real_path = path_builder.to_string();

    if (Core::File::is_directory(real_path)) {

        if (!request.resource().ends_with("/")) {
            StringBuilder red;

            red.append(requested_path);
            red.append("/");

            TRY(send_redirect(red.to_string(), request));
            return true;
        }

        StringBuilder index_html_path_builder;
        index_html_path_builder.append(real_path);
        index_html_path_builder.append("/index.html");
        auto index_html_path = index_html_path_builder.to_string();
        if (!Core::File::exists(index_html_path)) {
            TRY(handle_directory_listing(requested_path, real_path, request));
            return true;
        }
        real_path = index_html_path;
    }

    auto file = Core::File::construct(real_path);
    if (!file->open(Core::OpenMode::ReadOnly)) {
        TRY(send_error_response(404, request));
        return false;
    }

    if (file->is_device()) {
        TRY(send_error_response(403, request));
        return false;
    }

    Core::InputFileStream stream { file };

    TRY(send_response(stream, request, Core::guess_mime_type_based_on_filename(real_path)));
    return true;
}

ErrorOr<void> Client::send_response(InputStream& response, HTTP::HttpRequest const& request, String const& content_type)
{
    StringBuilder builder;
    builder.append("HTTP/1.0 200 OK\r\n");
    builder.append("Server: WebServer (SerenityOS)\r\n");
    builder.append("X-Frame-Options: SAMEORIGIN\r\n");
    builder.append("X-Content-Type-Options: nosniff\r\n");
    builder.append("Pragma: no-cache\r\n");
    builder.append("Content-Type: ");
    builder.append(content_type);
    builder.append("\r\n");
    builder.append("\r\n");

    auto builder_contents = builder.to_byte_buffer();
    TRY(m_socket->write(builder_contents));
    log_response(200, request);

    char buffer[PAGE_SIZE];
    do {
        auto size = response.read({ buffer, sizeof(buffer) });
        if (response.unreliable_eof() && size == 0)
            break;

        ReadonlyBytes write_buffer { buffer, size };
        while (!write_buffer.is_empty()) {
            auto nwritten = TRY(m_socket->write(write_buffer));

            if (nwritten == 0) {
                dbgln("EEEEEE got 0 bytes written!");
            }

            write_buffer = write_buffer.slice(nwritten);
        }
    } while (true);

    return {};
}

ErrorOr<void> Client::send_redirect(StringView redirect_path, HTTP::HttpRequest const& request)
{
    StringBuilder builder;
    builder.append("HTTP/1.0 301 Moved Permanently\r\n");
    builder.append("Location: ");
    builder.append(redirect_path);
    builder.append("\r\n");
    builder.append("\r\n");

    auto builder_contents = builder.to_byte_buffer();
    TRY(m_socket->write(builder_contents));

    log_response(301, request);
    return {};
}

static String folder_image_data()
{
    static String cache;
    if (cache.is_empty()) {
        auto file = Core::MappedFile::map("/res/icons/16x16/filetype-folder.png").release_value_but_fixme_should_propagate_errors();
        cache = encode_base64(file->bytes());
    }
    return cache;
}

static String file_image_data()
{
    static String cache;
    if (cache.is_empty()) {
        auto file = Core::MappedFile::map("/res/icons/16x16/filetype-unknown.png").release_value_but_fixme_should_propagate_errors();
        cache = encode_base64(file->bytes());
    }
    return cache;
}

ErrorOr<void> Client::handle_directory_listing(String const& requested_path, String const& real_path, HTTP::HttpRequest const& request)
{
    StringBuilder builder;

    builder.append("<!DOCTYPE html>\n");
    builder.append("<html>\n");
    builder.append("<head><meta charset=\"utf-8\">\n");
    builder.append("<title>Index of ");
    builder.append(escape_html_entities(requested_path));
    builder.append("</title><style>\n");
    builder.append(".folder { width: 16px; height: 16px; background-image: url('data:image/png;base64,");
    builder.append(folder_image_data());
    builder.append("'); }\n");
    builder.append(".file { width: 16px; height: 16px; background-image: url('data:image/png;base64,");
    builder.append(file_image_data());
    builder.append("'); }\n");
    builder.append("</style></head><body>\n");
    builder.append("<h1>Index of ");
    builder.append(escape_html_entities(requested_path));
    builder.append("</h1>\n");
    builder.append("<hr>\n");
    builder.append("<code><table>\n");

    Core::DirIterator dt(real_path);
    Vector<String> names;
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
            path_builder.append(".");
        else
            path_builder.append(name);

        struct stat st;
        memset(&st, 0, sizeof(st));
        int rc = stat(path_builder.to_string().characters(), &st);
        if (rc < 0) {
            perror("stat");
        }

        bool is_directory = S_ISDIR(st.st_mode);

        builder.append("<tr>");
        builder.appendff("<td><div class=\"{}\"></div></td>", is_directory ? "folder" : "file");
        builder.append("<td><a href=\"");
        builder.append(URL::percent_encode(name));
        // NOTE: For directories, we append a slash so we don't always hit the redirect case,
        //       which adds a slash anyways.
        if (is_directory)
            builder.append('/');
        builder.append("\">");
        builder.append(escape_html_entities(name));
        builder.append("</a></td><td>&nbsp;</td>");

        builder.appendff("<td>{:10}</td><td>&nbsp;</td>", st.st_size);
        builder.append("<td>");
        builder.append(Core::DateTime::from_timestamp(st.st_mtime).to_string());
        builder.append("</td>");
        builder.append("</tr>\n");
    }

    builder.append("</table></code>\n");
    builder.append("<hr>\n");
    builder.append("<i>Generated by WebServer (SerenityOS)</i>\n");
    builder.append("</body>\n");
    builder.append("</html>\n");

    auto response = builder.to_string();
    InputMemoryStream stream { response.bytes() };
    return send_response(stream, request, "text/html");
}

ErrorOr<void> Client::send_error_response(unsigned code, HTTP::HttpRequest const& request, Vector<String> const& headers)
{
    auto reason_phrase = HTTP::HttpResponse::reason_phrase_for_code(code);
    StringBuilder builder;
    builder.appendff("HTTP/1.0 {} ", code);
    builder.append(reason_phrase);
    builder.append("\r\n");

    for (auto& header : headers) {
        builder.append(header);
        builder.append("\r\n");
    }
    builder.append("Content-Type: text/html; charset=UTF-8\r\n");

    builder.append("\r\n");
    builder.append("<!DOCTYPE html><html><body><h1>");
    builder.appendff("{} ", code);
    builder.append(reason_phrase);
    builder.append("</h1></body></html>");

    auto builder_contents = builder.to_byte_buffer();
    TRY(m_socket->write(builder_contents));

    log_response(code, request);
    return {};
}

void Client::log_response(unsigned code, HTTP::HttpRequest const& request)
{
    outln("{} :: {:03d} :: {} {}", Core::DateTime::now().to_string(), code, request.method_name(), request.resource());
}

bool Client::verify_credentials(Vector<HTTP::HttpRequest::Header> const& headers)
{
    VERIFY(Configuration::the().credentials().has_value());
    auto& configured_credentials = Configuration::the().credentials().value();
    for (auto& header : headers) {
        if (header.name.equals_ignoring_case("Authorization")) {
            auto provided_credentials = HTTP::HttpRequest::parse_http_basic_authentication_header(header.value);
            if (provided_credentials.has_value() && configured_credentials.username == provided_credentials->username && configured_credentials.password == provided_credentials->password)
                return true;
        }
    }
    return false;
}

}
