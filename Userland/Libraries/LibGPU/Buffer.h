/*
 * Copyright (c) 2022, cflip <cflip@cflip.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>

namespace GPU {

class Buffer : public RefCounted<Buffer> {
public:
    virtual ~Buffer() { }

    virtual ErrorOr<void> set_data(void const*, size_t) = 0;
    virtual void replace_data(void const*, size_t offset, size_t size) = 0;

    virtual size_t size() const = 0;
    virtual void* data() = 0;
    virtual void* offset_data(size_t) = 0;
};

}
