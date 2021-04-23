/*
 * Copyright (c) 2021, Maciej Zygmanowski <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/LocalServer.h>
#include <SystemServer/ServiceManagement.h>

namespace SystemServer {

ServiceManagement& ServiceManagement::the()
{
    static ServiceManagement management;
    return management;
}

void ServiceManagement::initialize()
{
    auto config = Core::ConfigFile::get_for_system("SystemServer");

    // Read our config and instantiate services.
    // This takes care of setting up sockets.
    for (auto name : config->groups()) {
        auto service = Service::construct(*config, name);
        m_services.append(service);
    }

    // Setup IPC socket for service configuration.
    m_server = Core::LocalServer::construct();

    m_server->on_ready_to_accept = [&] {
        auto client_socket = m_server->accept();
        if (!client_socket) {
            dbgln("SystemServer: accept failed");
            return;
        }
        dbgln("SystemServer: new config client!!!");

        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;

        IPC::new_client_connection<SystemServer::ClientConnection>(client_socket.release_nonnull(), client_id);
    };

    if (!m_server->listen("/tmp/portal/system")) {
        VERIFY_NOT_REACHED();
    }

    activate_all_services();
}

RefPtr<Service> ServiceManagement::find_service_by_name(const StringView& name)
{
    for (auto& service : m_services) {
        if (service.name() == name)
            return service;
    }
    return nullptr;
}

void ServiceManagement::activate_all_services()
{
    // After we've set them all up, activate them!
    size_t activated_service_count = 0;
    for (auto& service : m_services)
        if (service.is_enabled()) {
            service.activate();
            activated_service_count++;
        }

    dbgln("Activated {} of {} services :^)", activated_service_count, m_services.size());
}

}
