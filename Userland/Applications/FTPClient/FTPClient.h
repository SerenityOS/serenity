/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "FTPClient.h"
#include <LibCore/TCPSocket.h>

class FTPClient final : public Core::Object {
    C_OBJECT(FTPClient)
public:
    void run();

private:
    String drain_socket();
    void quit();
    void send(String);

    RefPtr<Core::TCPSocket> m_socket;
};
