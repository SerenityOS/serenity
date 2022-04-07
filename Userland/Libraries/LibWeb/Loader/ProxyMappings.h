/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibCore/Proxy.h>

namespace Web {

class ProxyMappings {
public:
    static ProxyMappings& the();

    Core::ProxyData proxy_for_url(AK::URL const&) const;
    void set_mappings(Vector<String> proxies, OrderedHashMap<String, size_t> mappings);

private:
    ProxyMappings() = default;
    ~ProxyMappings() = default;

    Vector<String> m_proxies;
    OrderedHashMap<String, size_t> m_mappings;
};

}
