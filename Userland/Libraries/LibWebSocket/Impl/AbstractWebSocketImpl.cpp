/*
 * Copyright (c) 2021, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWebSocket/Impl/AbstractWebSocketImpl.h>

namespace WebSocket {

AbstractWebSocketImpl::AbstractWebSocketImpl(Core::Object* parent)
    : Object(parent)
{
}

AbstractWebSocketImpl::~AbstractWebSocketImpl()
{
}

}
