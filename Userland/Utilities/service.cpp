/*
 * Copyright (c) 2021, Maciej Zygmanowski <sppmacd@pm.me>
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

#include <AK/Format.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibIPC/ServerConnection.h>
#include <SystemServer/ClientConnection.h>
#include <SystemServer/ServiceManagementClientEndpoint.h>
#include <SystemServer/ServiceManagementServerEndpoint.h>

class Client final
    : public IPC::ServerConnection<ServiceManagementClientEndpoint, ServiceManagementServerEndpoint>
    , public ServiceManagementClientEndpoint {
    C_OBJECT(Client);

public:
    virtual void handshake() override;

private:
    Client();

    virtual void handle(const Messages::ServiceManagementClient::Dummy&) override;
};

Client::Client()
    : IPC::ServerConnection<ServiceManagementClientEndpoint, ServiceManagementServerEndpoint>(*this, "/tmp/portal/system")
{
    handshake();
}

void Client::handshake()
{
    send_sync<Messages::ServiceManagementServer::Greet>();
}

void Client::handle(const Messages::ServiceManagementClient::Dummy&)
{
}

int main(int argc, char** argv)
{
    const char* service_name = nullptr;
    const char* command = nullptr;
    bool print_all = false;

    Core::ArgsParser parser;
    parser.add_positional_argument(service_name, "Service to manage", "service", Core::ArgsParser::Required::No);
    parser.add_positional_argument(command, "Command", "command", Core::ArgsParser::Required::No);
    parser.add_option(print_all, "Print all services", "all", 'a');
    if (!parser.parse(argc, argv))
        return 1;

    Core::EventLoop loop;
    auto connection = Client::construct();

    if (print_all) {
        auto all_services = connection->send_sync<Messages::ServiceManagementServer::ServiceList>();

        for (auto& service : all_services->services()) {
            outln(" [ {} ] {}", service[0], service.substring(1));
        }
        return 0;

    } else if (!service_name || !command) {
        parser.print_usage(stdout, argv[0]);
        return 1;
    }

    // Handle commands
    auto command_string = StringView(command);
    if (command_string == "status"sv) {
        auto status = connection->send_sync<Messages::ServiceManagementServer::ServiceStatus>(service_name);
        auto status_json = JsonParser(status->status()).parse().value().as_object();

        auto enabled = status_json.get_ptr("enabled");
        if (!enabled) {
            warnln("Invalid service: {}", service_name);
            return 1;
        }
        outln("Service '{}'", service_name);
        outln(" - Enabled: {}", enabled->to_bool());

        auto pid = status_json.get_ptr("pid");
        if (pid && pid->to_uint() != 0) {
            outln(" - Active: yes, PID {}", pid->to_uint());
        } else {
            outln(" - Active: no");
        }

        auto lazy = status_json.get("lazy").to_bool();
        outln(" - Lazy: {}", lazy ? "yes" : "no");
        auto multi_instance = status_json.get("multi_instance").to_bool();
        outln(" - Multi instance: {}", multi_instance ? "yes" : "no");
        auto accept_socket_connections = status_json.get("accept_socket_connections").to_bool();
        outln(" - Accepts socket connection: {}", accept_socket_connections ? "yes" : "no");
        return 0;
    } else if (command_string == "enable"sv) {
        connection->send_sync<Messages::ServiceManagementServer::ServiceSetEnabled>(service_name, true);
        return 0;
    } else if (command_string == "disable"sv) {
        connection->send_sync<Messages::ServiceManagementServer::ServiceSetEnabled>(service_name, false);
        return 0;
    } else if (command_string == "start"sv) {
        connection->send_sync<Messages::ServiceManagementServer::ServiceStart>(service_name);
        return 0;
    } else if (command_string == "stop"sv) {
        connection->send_sync<Messages::ServiceManagementServer::ServiceStop>(service_name);
        return 0;
    }

    warnln("Valid commands are 'status', 'enable', 'disable', 'start', 'stop', given {}", command_string);
    return 1;
}
