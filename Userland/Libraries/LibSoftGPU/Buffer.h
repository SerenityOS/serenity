/*
 * Copyright (c) 2022, cflip <cflip@cflip.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/RefCounted.h>
#include <LibGPU/Buffer.h>

namespace SoftGPU {

class Buffer final : public GPU::Buffer {
public:
    virtual ErrorOr<void> set_data(void const*, size_t) override;
    virtual void replace_data(void const*, size_t offset, size_t size) override;

    virtual size_t size() const override;
    virtual void* data() override;
    virtual void* offset_data(size_t) override;

private:
    ByteBuffer m_data;
};

}
