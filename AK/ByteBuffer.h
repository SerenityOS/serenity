/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Span.h>
#include <AK/Vector.h>

namespace AK {

template<>
struct Traits<ByteBuffer> : public GenericTraits<ByteBuffer> {
    static unsigned hash(ByteBuffer const& byte_buffer)
    {
        return Traits<ReadonlyBytes>::hash(byte_buffer.span());
    }
};

}
