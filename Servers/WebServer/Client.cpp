#include "Client.h"
#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <LibCore/HttpRequest.h>
#include <stdio.h>
#include <time.h>

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

    FileSystemPath canonical_path(request.resource());
    dbg() << "Requested canonical path: '" << canonical_path.string() << "'";

    StringBuilder path_builder;
    path_builder.append("/www/");
    path_builder.append(canonical_path.string());

    if (Core::File::is_directory(path_builder.to_string()))
        path_builder.append("/index.html");

    auto file = Core::File::construct(path_builder.to_string());
    if (!file->open(Core::File::ReadOnly)) {
        send_error_response(404, "Not found, bro!", request);
        return;
    }

    StringBuilder builder;
    builder.append("HTTP/1.0 200 OK\r\n");
    builder.append("Server: WebServer (SerenityOS)\r\n");
    builder.append("Content-Type: text/html\r\n");
    builder.append("\r\n");

    m_socket->write(builder.to_string());
    m_socket->write(file->read_all());

    log_response(200, request);
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
    printf("%lld :: %03u :: %s %s\n", time(nullptr), code, request.method_name().characters(), request.resource().characters());
}

}
