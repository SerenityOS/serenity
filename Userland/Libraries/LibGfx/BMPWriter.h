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

    enum class Compression : u32 {
        BI_RGB = 0,
        BI_BITFIELDS = 3,
    };

    enum class DibHeader : u32 {
        Info = 40,
        V3 = 56,
        V4 = 108,
    };

    ByteBuffer dump(const RefPtr<Bitmap>, DibHeader dib_header = DibHeader::V4);

    inline void set_compression(Compression compression) { m_compression = compression; }

private:
    Compression m_compression { Compression::BI_BITFIELDS };
    int m_bytes_per_pixel { 4 };
    bool m_include_alpha_channel { true };
};

}
