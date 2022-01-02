/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "QOIWriter.h"
#include "QOICommon.h"
#include <AK/ByteBuffer.h>
#include <AK/Math.h>
#include <AK/MemoryStream.h>
#include <AK/TypedTransfer.h>
#include <LibGfx/Bitmap.h>

namespace Gfx {

template<typename Stream = ByteBuffer>
class QOIWriterState : public QOIState {
public:
    ErrorOr<void> start(u32 width, u32 height, bool has_alpha_channel)
    {
        if (m_pixels_to_encode != INVALID_PIXELS_TO_ENCODE)
            return Error::from_string_literal("Trying to start an already initialized QOI writer"sv);

        m_pixels_to_encode = static_cast<u64>(width) * static_cast<u64>(height);
        VERIFY(m_pixels_to_encode != INVALID_PIXELS_TO_ENCODE);

        // 5 is the worse case scenario where we need to encode QOI_OP_RGBA for all pixels
        const u64 pixels_to_encode_size = m_pixels_to_encode * 5;
        TRY(m_stream.try_ensure_capacity(sizeof(QOIHeader) + pixels_to_encode_size + sizeof(QOI_END_MARKER)));

        TRY(try_encode_qoi_header(width, height, has_alpha_channel));
        return {};
    }

    ErrorOr<void> encode(Color const pixel)
    {
        if (m_pixels_to_encode == INVALID_PIXELS_TO_ENCODE)
            return Error::from_string_literal("Trying to encode using a not initialized QOI writer"sv);
        if (m_pixels_to_encode == 0)
            return Error::from_string_literal("Trying to encode using a QOI writer that should be finished instead"sv);
        --m_pixels_to_encode;

        const i8 dr = pixel.red() - previous_pixel().red();
        const i8 dg = pixel.green() - previous_pixel().green();
        const i8 db = pixel.blue() - previous_pixel().blue();
        const i8 da = pixel.alpha() - previous_pixel().alpha();

        false
            || TRY(try_encode_qoi_op_run(dr, dg, db, da))
            || TRY(try_encode_qoi_op_index(pixel))
            || TRY(try_encode_qoi_op_diff(dr, dg, db, da))
            || TRY(try_encode_qoi_op_luma(dr, dg, db, da))
            || TRY(try_encode_qoi_op_rgb(pixel))
            || TRY(try_encode_qoi_op_rgba(pixel));
        set_previous_pixel(pixel);
        return {};
    }

    ErrorOr<Stream> finish()
    {
        if (m_pixels_to_encode == INVALID_PIXELS_TO_ENCODE)
            return Error::from_string_literal("Trying to finish a not initialized QOI writer"sv);
        if (m_pixels_to_encode != 0)
            return Error::from_string_literal("Prematurely trying to finish a QOI writer"sv);

        m_pixels_to_encode = INVALID_PIXELS_TO_ENCODE;
        TRY(try_encode_qoi_end_marker());
        return m_stream;
    }

private:
    ErrorOr<void> try_encode_qoi_header(u32 width, u32 height, bool has_alpha_channel)
    {
        QOIHeader header {
            {},
            AK::convert_between_host_and_big_endian(width),
            AK::convert_between_host_and_big_endian(height),
            static_cast<u8>(has_alpha_channel ? 4 : 3),
            0,
        };
        QOI_MAGIC.span().copy_to(header.magic);
        return m_stream.try_append(ReadonlyBytes { &header, sizeof(header) });
    }

    ErrorOr<bool> try_encode_qoi_op_run(i8 dr, i8 dg, i8 db, i8 da)
    {
        const bool is_equal = dr == 0 && dg == 0 && db == 0 && da == 0;

        if (is_equal) {
            ++m_run;
            if (m_run < QOI_RUN_MAX && m_pixels_to_encode > 0)
                return true;
        } else if (m_run == 0) {
            return false;
        }

        VERIFY(qoi_is_valid_run(m_run));
        // The run-length is stored with a bias of -1.
        ByteArray<1> bytes {
            static_cast<u8>(QOI_OP_RUN | (m_run - 1))
        };
        TRY(m_stream.try_append(bytes));
        m_run = 0;
        return is_equal;
    }

    ErrorOr<bool> try_encode_qoi_op_index(Color pixel)
    {
        auto index = index_position(pixel);
        VERIFY(AK::is_in_bounds<u8>(index, 0, 63));
        if (previously_seen_pixel(index) != pixel)
            return false;

        ByteArray<1> bytes {
            static_cast<u8>(QOI_OP_INDEX | index)
        };
        TRY(m_stream.try_append(bytes));
        return true;
    }

    ErrorOr<bool> try_encode_qoi_op_diff(i8 dr, i8 dg, i8 db, i8 da)
    {
        if (!AK::is_in_bounds<i8>(dr, -2, 1)
            || !AK::is_in_bounds<i8>(dg, -2, 1)
            || !AK::is_in_bounds<i8>(db, -2, 1)
            || da != 0)
            return false;

        ByteArray<1> bytes {
            static_cast<u8>(QOI_OP_DIFF | (dr + 2) << 4 | (dg + 2) << 2 | (db + 2))
        };
        TRY(m_stream.try_append(bytes));
        return true;
    }

    ErrorOr<bool> try_encode_qoi_op_luma(i8 dr, i8 dg, i8 db, i8 da)
    {
        const i8 dr_dg = dr - dg;
        const i8 db_dg = db - dg;

        if (!AK::is_in_bounds<i8>(dg, -32, 31)
            || !AK::is_in_bounds<i8>(dr_dg, -8, 7)
            || !AK::is_in_bounds<i8>(db_dg, -8, 7)
            || da != 0)
            return false;

        ByteArray<2> bytes {
            static_cast<u8>(QOI_OP_LUMA | (dg + 32)),
            static_cast<u8>((dr_dg + 8) << 4 | (db_dg + 8))
        };
        TRY(m_stream.try_append(bytes));
        return true;
    }

    ErrorOr<bool> try_encode_qoi_op_rgb(Color pixel)
    {
        if (pixel.alpha() != previous_pixel().alpha())
            return false;

        ByteArray<4> bytes {
            QOI_OP_RGB,
            pixel.red(),
            pixel.green(),
            pixel.blue()
        };
        TRY(m_stream.try_append(bytes));
        return true;
    }

    ErrorOr<bool> try_encode_qoi_op_rgba(Color pixel)
    {
        ByteArray<5> bytes {
            QOI_OP_RGBA,
            pixel.red(),
            pixel.green(),
            pixel.blue(),
            pixel.alpha()
        };
        TRY(m_stream.try_append(bytes));
        return true;
    }

    ErrorOr<void> try_encode_qoi_end_marker()
    {
        return m_stream.try_append(QOI_END_MARKER);
    }

private:
    static constexpr u64 INVALID_PIXELS_TO_ENCODE = NumericLimits<u64>::max();
    u64 m_pixels_to_encode { INVALID_PIXELS_TO_ENCODE };
    u8 m_run { 0 };
    Stream m_stream;
};

ErrorOr<ByteBuffer> QOIWriter::encode(Gfx::Bitmap const& bitmap)
{
    const u32 width = static_cast<u32>(bitmap.width());
    const u32 height = static_cast<u32>(bitmap.height());

    QOIWriterState<> state;
    TRY(state.start(width, height, bitmap.has_alpha_channel()));
    for (u32 y = 0; y < height; ++y) {
        for (u32 x = 0; x < width; ++x)
            TRY(state.encode(bitmap.get_pixel(x, y)));
    }
    return TRY(state.finish());
}

}
