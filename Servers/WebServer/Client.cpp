/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Client.h"
#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/DateTime.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/HttpRequest.h>
#include <stdio.h>
#include <sys/stat.h>
#include <time.h>
#include <unistd.h>

namespace WebServer {

Client::Client(NonnullRefPtr<Core::TCPSocket> socket, Core::Object* parent)
    : Core::Object(parent)
    , m_socket(socket)
{
}

void Client::die()
{
    remove_from_parent();
}

void Client::start()
{
    m_socket->on_ready_to_read = [this] {
        auto raw_request = m_socket->read_all();
        if (raw_request.is_null()) {
            die();
            return;
        }

        dbg() << "Got raw request: '" << String::copy(raw_request) << "'";

        handle_request(move(raw_request));
        die();
    };
}

void Client::handle_request(ByteBuffer raw_request)
{
    auto request_or_error = Core::HttpRequest::from_raw_request(raw_request);
    if (!request_or_error.has_value())
        return;
    auto& request = request_or_error.value();

    dbg() << "Got HTTP request: " << request.method_name() << " " << request.resource();
    for (auto& header : request.headers()) {
        dbg() << "    " << header.name << " => " << header.value;
    }

    if (request.method() != Core::HttpRequest::Method::GET) {
        send_error_response(403, "Forbidden, bro!", request);
        return;
    }

    auto requested_path = canonicalized_path(request.resource());
    dbg() << "Canonical requested path: '" << requested_path << "'";

    StringBuilder path_builder;
    path_builder.append("/www/");
    path_builder.append(requested_path);
    auto real_path = path_builder.to_string();

    if (Core::File::is_directory(real_path)) {
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
    if (!file->open(Core::File::ReadOnly)) {
        send_error_response(404, "Not found, bro!", request);
        return;
    }

    send_response(file->read_all(), request);
}

void Client::send_response(StringView response, const Core::HttpRequest& request)
{
    StringBuilder builder;
    builder.append("HTTP/1.0 200 OK\r\n");
    builder.append("Server: WebServer (SerenityOS)\r\n");
    builder.append("Content-Type: text/html\r\n");
    builder.append("\r\n");

    m_socket->write(builder.to_string());
    m_socket->write(response);

    log_response(200, request);
}

void Client::handle_directory_listing(const String& requested_path, const String& real_path, const Core::HttpRequest& request)
{
    StringBuilder builder;

    builder.append("<!DOCTYPE html>\n");
    builder.append("<html>\n");
    builder.append("<head><title>Index of ");
    builder.append(escape_html_entities(requested_path));
    builder.append("</title></head>\n");
    builder.append("<body>\n");
    builder.append("<h1>Index of ");
    builder.append(escape_html_entities(requested_path));
    builder.append("</h1>\n");
    builder.append("<hr>\n");
    builder.append("<pre>\n");

    Core::DirIterator dt(real_path);
    while (dt.has_next()) {
        auto name = dt.next_path();
        builder.append("<a href=\"");
        // FIXME: urlencode
        builder.append(name);
        builder.append("\">");
        builder.append(escape_html_entities(name));
        builder.append("</a>");
        for (size_t i = 0; i < (40 - name.length()); ++i)
            builder.append(' ');

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
        builder.appendf("  %10d", st.st_size);
        builder.appendf("  ");
        builder.append(Core::DateTime::from_timestamp(st.st_mtime).to_string());
        builder.append("\n");
    }

    builder.append("</pre>\n");
    builder.append("<hr>\n");
    builder.append("<i>Generated by WebServer (SerenityOS)</i>\n");
    builder.append("</body>\n");
    builder.append("</html>\n");

    send_response(builder.to_string(), request);
}

void Client::send_error_response(unsigned code, const StringView& message, const Core::HttpRequest& request)
{
    StringBuilder builder;
    builder.appendf("HTTP/1.0 %u ", code);
    builder.append(message);
    builder.append("\r\n\r\n");
    builder.append("<!DOCTYPE html><html><body><h1>");
    builder.appendf("%u ", code);
    builder.append(message);
    builder.append("</h1></body></html>");
    m_socket->write(builder.to_string());

    log_response(code, request);
}

void Client::log_response(unsigned code, const Core::HttpRequest& request)
{
    printf("%s :: %03u :: %s %s\n",
        Core::DateTime::now().to_string().characters(),
        code,
        request.method_name().characters(),
        request.resource().characters());
}

}
