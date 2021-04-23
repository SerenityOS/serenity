/*
 * Copyright (c) 2021, Maciej Zygmanowski <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/Singleton.h>
#include <LibCore/ConfigFile.h>
#include <LibIPC/ClientConnection.h>
#include <SystemServer/ClientConnection.h>
#include <SystemServer/Service.h>

namespace SystemServer {

class ServiceManagement {
public:
    static ServiceManagement& the();

    void initialize();

    RefPtr<Service> find_service_by_name(const StringView& name);
    const NonnullRefPtrVector<Service>& services() const { return m_services; }

protected:
    void activate_all_services();

    NonnullRefPtrVector<Service> m_services;
    RefPtr<Core::LocalServer> m_server;
};

}
