/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Transport.h"

ByteBuffer RawTransport::receive(int max_size, RefPtr<Core::Socket> socket)
{
    return socket->receive(max_size);
}

bool RawTransport::send(ReadonlyBytes data, RefPtr<Core::Socket> socket)
{
    return socket->send(data);
}

void RawTransport::init([[maybe_unused]] ReadonlyBytes input, [[maybe_unused]] RefPtr<Core::Socket> socket)
{
}
