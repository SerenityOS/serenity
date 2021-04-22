/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>

namespace Gfx {

class Bitmap;

class BMPWriter {
public:
    BMPWriter() = default;

    ByteBuffer dump(const RefPtr<Bitmap>);

    enum class Compression : u32 {
        RGB = 0,
    };

    inline void set_compression(Compression compression) { m_compression = compression; }

private:
    Compression m_compression { Compression::RGB };
};

}
