/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/URL.h>
#include <LibCore/Proxy.h>
#include <RequestServer/Forward.h>

namespace RequestServer {

class Protocol {
public:
    virtual ~Protocol();

    String const& name() const { return m_name; }
    virtual OwnPtr<Request> start_request(ConnectionFromClient&, String const& method, const URL&, HashMap<String, String> const& headers, ReadonlyBytes body, Core::ProxyData proxy_data = {}) = 0;

    static Protocol* find_by_name(String const&);

protected:
    explicit Protocol(String const& name);
    struct Pipe {
        int read_fd { -1 };
        int write_fd { -1 };
    };
    static ErrorOr<Pipe> get_pipe_for_request();

private:
    String m_name;
};

}
