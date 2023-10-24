/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibProtocol/RequestClient.h>

namespace Geolocation {

class Client : public Core::EventReceiver {
    C_OBJECT(Client);

public:
    static Client& the();

    struct Position {
        double latitude;
        double longitude;
        double accuracy;
        Optional<String> city;
        Optional<String> region;
        Optional<String> country;
        UnixDateTime timestamp;
    };
    void get_current_position(Function<void(Optional<Position>)> callback);

private:
    Client();

    RefPtr<Protocol::RequestClient> m_request_client;
    Vector<RefPtr<Protocol::Request>> m_active_requests;
};

}
