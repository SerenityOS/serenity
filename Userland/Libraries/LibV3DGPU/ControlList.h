/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Buffer.h"
#include "Definitions.h"
#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/Span.h>
#include <AK/Types.h>

namespace V3DGPU {

class ControlList {
public:
    template<typename T>
    void append(T const& packet)
    {
        m_data.try_append(&packet, sizeof(packet)).release_value_but_fixme_should_propagate_errors();
    }

    void clear()
    {
        m_data.clear();
    }

    ReadonlyBytes data() const
    {
        return m_data.bytes();
    }

    ErrorOr<void> copy_to_gpu_buffer()
    {
        if (!m_buffer.has_value() || m_buffer->size() < m_data.size()) {
            auto buffer_size = align_up_to(m_data.size(), V3D_PAGE_SIZE);

            m_buffer = TRY(Buffer::create(buffer_size, "LibV3DGPU: Control List"sv));
        }

        m_data.bytes().copy_to(m_buffer->data());

        return {};
    }

    Optional<Buffer const&> buffer() const
    {
        VERIFY(m_buffer.has_value());
        return *m_buffer;
    }

private:
    AK::Detail::ByteBuffer<0> m_data;
    Optional<Buffer> m_buffer;
};

}
