/*
 * Copyright (c) 2023, Gregory Bertilson <Zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Reader.h"
#include "JPEG2000Boxes.h"
#include <AK/Function.h>

namespace Gfx::ISOBMFF {

ErrorOr<Reader> Reader::create(MaybeOwned<SeekableStream> stream)
{
    size_t size = TRY(stream->size());
    return Reader(make<BoxStream>(move(stream), size));
}

ErrorOr<Reader> Reader::create(MaybeOwned<BoxStream> stream)
{
    return Reader(move(stream));
}

ErrorOr<BoxList> Reader::read_entire_file()
{
    BoxList top_level_boxes;

    while (!m_box_stream->is_eof()) {
        auto box_header = TRY(read_box_header(*m_box_stream));
        BoxStream box_stream { MaybeOwned<Stream> { *m_box_stream }, static_cast<size_t>(box_header.contents_size) };

        switch (box_header.type) {
        case BoxType::FileTypeBox:
            TRY(top_level_boxes.try_append(TRY(FileTypeBox::create_from_stream(box_stream))));
            break;
        case BoxType::JPEG2000HeaderBox:
            TRY(top_level_boxes.try_append(TRY(JPEG2000HeaderBox::create_from_stream(box_stream))));
            break;
        case BoxType::JPEG2000SignatureBox:
            TRY(top_level_boxes.try_append(TRY(JPEG2000SignatureBox::create_from_stream(box_stream))));
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
