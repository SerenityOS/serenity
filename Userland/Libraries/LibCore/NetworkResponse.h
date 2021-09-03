/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/ByteBuffer.h>
#include <YAK/RefCounted.h>

namespace Core {

class NetworkResponse : public RefCounted<NetworkResponse> {
public:
    virtual ~NetworkResponse();

    bool is_error() const { return m_error; }

protected:
    explicit NetworkResponse();

    bool m_error { false };
};

}
