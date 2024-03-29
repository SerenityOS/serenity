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
    auto make_top_level_box = [](BoxType type, BoxStream& stream) -> ErrorOr<Optional<NonnullOwnPtr<Box>>> {
        switch (type) {
        case BoxType::FileTypeBox:
            return TRY(FileTypeBox::create_from_stream(stream));
        case BoxType::JPEG2000ContiguousCodestreamBox:
            return TRY(JPEG2000ContiguousCodestreamBox::create_from_stream(stream));
        case BoxType::JPEG2000HeaderBox:
            return TRY(JPEG2000HeaderBox::create_from_stream(stream));
        case BoxType::JPEG2000SignatureBox:
            return TRY(JPEG2000SignatureBox::create_from_stream(stream));
        case BoxType::JPEG2000UUIDInfoBox:
            return TRY(JPEG2000UUIDInfoBox::create_from_stream(stream));
        case BoxType::UserExtensionBox:
            return TRY(UserExtensionBox::create_from_stream(stream));
        default:
            return OptionalNone {};
        }
    };
    return read_entire_file((ErrorOr<Optional<NonnullOwnPtr<Box>>>(*)(BoxType, BoxStream&))(make_top_level_box));
}

ErrorOr<BoxList> Reader::read_entire_file(BoxCallback box_factory)
{
    BoxList top_level_boxes;

    while (!m_box_stream->is_eof()) {
        auto box_header = TRY(read_box_header(*m_box_stream));
        BoxStream box_stream { MaybeOwned<Stream> { *m_box_stream }, static_cast<size_t>(box_header.contents_size) };

        auto maybe_box = TRY(box_factory(box_header.type, box_stream));
        if (maybe_box.has_value()) {
            TRY(top_level_boxes.try_append(move(maybe_box.value())));
        } else {
            TRY(top_level_boxes.try_append(TRY(UnknownBox::create_from_stream(box_header.type, box_stream))));
        }

        if (!box_stream.is_eof()) {
            dbgln("Reader did not consume all data for box type {}, {} bytes remaining", RIFF::ChunkID::from_number(to_underlying(box_header.type)), box_stream.remaining());
            return Error::from_string_literal("Reader did not consume all data");
        }
    }
    return top_level_boxes;
}

}
