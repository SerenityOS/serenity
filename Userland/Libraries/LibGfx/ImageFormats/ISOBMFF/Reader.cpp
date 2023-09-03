/*
 * Copyright (c) 2023, Gregory Bertilson <Zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Reader.h"

namespace Gfx::ISOBMFF {

ErrorOr<Reader> Reader::create(MaybeOwned<SeekableStream> stream)
{
    return Reader(move(stream));
}

ErrorOr<BoxList> Reader::read_entire_file()
{
    BoxList top_level_boxes;

    while (!m_stream->is_eof()) {
        auto box_header = TRY(read_box_header(*m_stream));
        BoxStream box_stream { *m_stream, static_cast<size_t>(box_header.contents_size) };

        switch (box_header.type) {
        case BoxType::FileTypeBox:
            TRY(top_level_boxes.try_append(TRY(FileTypeBox::create_from_stream(box_stream))));
            break;
        default:
            TRY(top_level_boxes.try_append(TRY(UnknownBox::create_from_stream(box_header.type, box_stream))));
            break;
        }

        if (!box_stream.is_eof())
            return Error::from_string_literal("Reader did not consume all data");
    }
    return top_level_boxes;
}

}
