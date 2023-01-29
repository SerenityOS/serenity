/*
 * Copyright (c) 2022, cflip <cflip@cflip.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/RefCounted.h>

namespace GL {

// FIXME: For now, this is basically just a wrapper around ByteBuffer, but in
//        the future buffer data should be stored in LibGPU.
class Buffer : public RefCounted<Buffer> {
public:
    ErrorOr<void> set_data(void const*, size_t);
    void replace_data(void const*, size_t offset, size_t size);

    size_t size();
    void* data();
    void* offset_data(size_t);

private:
    ByteBuffer m_data;
};

}
