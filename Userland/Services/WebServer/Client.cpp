/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Client.h"
#include <AK/Base64.h>
#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <AK/MappedFile.h>
#include <AK/MemoryStream.h>
#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <LibCore/DateTime.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/FileStream.h>
#include <LibCore/MimeData.h>
#include <LibHTTP/HttpRequest.h>
#include <inttypes.h>
#include <stdio.h>
#include <sys/stat.h>
#include <time.h>
#include <unistd.h>

namespace WebServer {

Client::Client(NonnullRefPtr<Core::TCPSocket> socket, const String& root, Core::Object* parent)
    : Core::Object(parent)
    , m_socket(socket)
    , m_root_path(root)
{
}

void Client::die()
{
    remove_from_parent();
}

void Client::start()
{
    m_socket->on_ready_to_read = [this] {
        StringBuilder builder;
        for (;;) {
            auto line = m_socket->read_line();
            if (line.is_empty())
                break;
            builder.append(line);
            builder.append("\r\n");
        }

        auto request = builder.to_byte_buffer();
        dbgln_if(WEBSERVER_DEBUG, "Got raw request: '{}'", String::copy(request));
        handle_request(request);
        die();
    };
}

void Client::handle_request(ReadonlyBytes raw_request)
{
    auto request_or_error = HTTP::HttpRequest::from_raw_request(raw_request);
    if (!request_or_error.has_value())
        return;
    auto& request = request_or_error.value();

    if constexpr (WEBSERVER_DEBUG) {
        dbgln("Got HTTP request: {} {}", request.method_name(), request.resource());
        for (auto& header : request.headers()) {
            dbgln("    {} => {}", header.name, header.value);
        }
    }

    if (request.method() != HTTP::HttpRequest::Method::GET) {
        send_error_response(403, "Forbidden!", request);
        return;
    }

    auto requested_path = LexicalPath::join("/", request.resource()).string();
    dbgln_if(WEBSERVER_DEBUG, "Canonical requested path: '{}'", requested_path);

    StringBuilder path_builder;
    path_builder.append(m_root_path);
    path_builder.append('/');
    path_builder.append(requested_path);
    auto real_path = path_builder.to_string();

    if (Core::File::is_directory(real_path)) {

        if (!request.resource().ends_with("/")) {
            StringBuilder red;

            red.append(requested_path);
            red.append("/");

            send_redirect(red.to_string(), request);
            return;
        }

        StringBuilder index_html_path_builder;
        index_html_path_builder.append(real_path);
        index_html_path_builder.append("/index.html");
        auto index_html_path = index_html_path_builder.to_string();
        if (!Core::File::exists(index_html_path)) {
            handle_directory_listing(requested_path, real_path, request);
            return;
        }
        real_path = index_html_path;
    }

    auto file = Core::File::construct(real_path);
    if (!file->open(Core::OpenMode::ReadOnly)) {
        send_error_response(404, "Not found!", request);
        return;
    }

    Core::InputFileStream stream { file };

    send_response(stream, request, Core::guess_mime_type_based_on_filename(real_path));
}

void Client::send_response(InputStream& response, const HTTP::HttpRequest& request, const String& content_type)
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

    m_socket->write(builder.to_string());
    log_response(200, request);

    char buffer[PAGE_SIZE];
    do {
        auto size = response.read({ buffer, sizeof(buffer) });
        if (response.unreliable_eof() && size == 0)
            break;

        m_socket->write({ buffer, size });
    } while (true);
}

void Client::send_redirect(StringView redirect_path, const HTTP::HttpRequest& request)
{
    StringBuilder builder;
    builder.append("HTTP/1.0 301 Moved Permanently\r\n");
    builder.append("Location: ");
    builder.append(redirect_path);
    builder.append("\r\n");
    builder.append("\r\n");

    m_socket->write(builder.to_string());

    log_response(301, request);
}

static String folder_image_data()
{
    static String cache;
    if (cache.is_empty()) {
        auto file_or_error = MappedFile::map("/res/icons/16x16/filetype-folder.png");
        VERIFY(!file_or_error.is_error());
        cache = encode_base64(file_or_error.value()->bytes());
    }
    return cache;
}

static String file_image_data()
{
    static String cache;
    if (cache.is_empty()) {
        auto file_or_error = MappedFile::map("/res/icons/16x16/filetype-unknown.png");
        VERIFY(!file_or_error.is_error());
        cache = encode_base64(file_or_error.value()->bytes());
    }
    return cache;
}

void Client::handle_directory_listing(const String& requested_path, const String& real_path, const HTTP::HttpRequest& request)
{
    StringBuilder builder;

    builder.append("<!DOCTYPE html>\n");
    builder.append("<html>\n");
    builder.append("<head><title>Index of ");
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
    while (dt.has_next()) {
        auto name = dt.next_path();

        StringBuilder path_builder;
        path_builder.append(real_path);
        path_builder.append('/');
        path_builder.append(name);
        struct stat st;
        memset(&st, 0, sizeof(st));
        int rc = stat(path_builder.to_string().characters(), &st);
        if (rc < 0) {
            perror("stat");
        }

        bool is_directory = S_ISDIR(st.st_mode) || name.is_one_of(".", "..");

        builder.append("<tr>");
        builder.appendff("<td><div class=\"{}\"></div></td>", is_directory ? "folder" : "file");
        builder.append("<td><a href=\"");
        builder.append(URL::percent_encode(name));
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
    send_response(stream, request, "text/html");
}

void Client::send_error_response(unsigned code, const StringView& message, const HTTP::HttpRequest& request)
{
    StringBuilder builder;
    builder.appendff("HTTP/1.0 {} ", code);
    builder.append(message);
    builder.append("\r\n\r\n");
    builder.append("<!DOCTYPE html><html><body><h1>");
    builder.appendff("{} ", code);
    builder.append(message);
    builder.append("</h1></body></html>");
    m_socket->write(builder.to_string());

    log_response(code, request);
}

void Client::log_response(unsigned code, const HTTP::HttpRequest& request)
{
    printf("%s :: %03u :: %s %s\n",
        Core::DateTime::now().to_string().characters(),
        code,
        request.method_name().characters(),
        request.resource().characters());
}

}
