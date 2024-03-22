/*
 * Copyright (c) 2023, Gregory Bertilson <Zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/MaybeOwned.h>
#include <AK/Stream.h>

#include "Boxes.h"

namespace Gfx::ISOBMFF {

class Reader {
public:
    static ErrorOr<Reader> create(MaybeOwned<SeekableStream> stream);

    ErrorOr<BoxList> read_entire_file();

    ErrorOr<BrandIdentifier> get_major_brand();
    ErrorOr<Vector<BrandIdentifier>> get_minor_brands();

private:
    Reader(MaybeOwned<SeekableStream> stream, size_t size)
        : m_stream(move(stream))
        , m_box_stream(*m_stream, size)
    {
    }

    ErrorOr<void> parse_initial_data();

    MaybeOwned<SeekableStream> m_stream;
    BoxStream m_box_stream;
};

}
