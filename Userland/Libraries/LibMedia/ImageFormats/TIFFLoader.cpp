/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TIFFLoader.h"
#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/String.h>
#include <LibCompress/LZWDecoder.h>
#include <LibMedia/ImageFormats/TIFFMetadata.h>

namespace Media {

namespace TIFF {

class TIFFLoadingContext {
public:
    enum class State {
        NotDecoded = 0,
        Error,
        HeaderDecoded,
        FrameDecoded,
    };

    TIFFLoadingContext(NonnullOwnPtr<FixedMemoryStream> stream)
        : m_stream(move(stream))
    {
    }

    ErrorOr<void> decode_image_header()
    {
        TRY(read_image_file_header());
        TRY(read_next_image_file_directory());

        m_state = State::HeaderDecoded;
        return {};
    }

    ErrorOr<void> decode_frame()
    {
        auto maybe_error = decode_frame_impl();

        if (maybe_error.is_error()) {
            m_state = State::Error;
            return maybe_error.release_error();
        }

        return {};
    }

    Gfx::IntSize size() const
    {
        return { *m_metadata.image_width(), *m_metadata.image_height() };
    }

    State state() const
    {
        return m_state;
    }

    RefPtr<Gfx::Bitmap> bitmap() const
    {
        return m_bitmap;
    }

private:
    enum class ByteOrder {
        LittleEndian,
        BigEndian,
    };

    template<typename ByteReader>
    ErrorOr<void> loop_over_pixels(ByteReader&& byte_reader, Function<ErrorOr<void>(u32)> initializer = {})
    {
        auto const strips_offset = *m_metadata.strip_offsets();
        auto const strip_byte_counts = *m_metadata.strip_byte_counts();

        for (u32 strip_index = 0; strip_index < strips_offset.size(); ++strip_index) {
            TRY(m_stream->seek(strips_offset[strip_index]));
            if (initializer)
                TRY(initializer(strip_byte_counts[strip_index]));
            for (u32 row = 0; row < *m_metadata.rows_per_strip(); row++) {
                auto const scanline = row + *m_metadata.rows_per_strip() * strip_index;
                if (scanline >= *m_metadata.image_height())
                    break;

                Optional<Color> last_color {};

                for (u32 column = 0; column < *m_metadata.image_width(); ++column) {
                    auto color = Color { TRY(byte_reader()), TRY(byte_reader()), TRY(byte_reader()) };

                    if (m_metadata.predictor() == Predictor::HorizontalDifferencing && last_color.has_value()) {
                        color.set_red(last_color->red() + color.red());
                        color.set_green(last_color->green() + color.green());
                        color.set_blue(last_color->blue() + color.blue());
                    }

                    last_color = color;
                    m_bitmap->set_pixel(column, scanline, color);
                }
            }
        }

        return {};
    }

    ErrorOr<void> decode_frame_impl()
    {
        m_bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, size()));

        switch (*m_metadata.compression()) {
        case Compression::NoCompression:
            TRY(loop_over_pixels([this]() { return read_value<u8>(); }));
            break;
        case Compression::LZW: {
            ByteBuffer decoded_bytes {};
            u32 read_head {};

            auto initializer = [&](u32 bytes) -> ErrorOr<void> {
                decoded_bytes = TRY(Compress::LZWDecoder<BigEndianInputBitStream>::decode_all(TRY(m_stream->read_in_place<u8 const>(bytes)), 8, -1));
                read_head = 0;
                return {};
            };

            auto read_lzw_byte = [&]() -> ErrorOr<u8> {
                if (read_head < decoded_bytes.size())
                    return decoded_bytes[read_head++];
                return Error::from_string_literal("TIFFImageDecoderPlugin: Reached end of LZW stream");
            };

            TRY(loop_over_pixels([read_lzw_byte = move(read_lzw_byte)]() { return read_lzw_byte(); }, move(initializer)));
            break;
        }
        case Compression::PackBits: {
            // Section 9: PackBits Compression
            Optional<i8> n;
            Optional<u8> saved_byte;

            auto read_packed_byte = [&]() -> ErrorOr<u8> {
                while (true) {
                    if (!n.has_value())
                        n = TRY(read_value<i8>());

                    if (n.value() >= 0 && !saved_byte.has_value()) {
                        n.value() = n.value() - 1;
                        if (n.value() == -1)
                            n.clear();

                        return read_value<u8>();
                    }

                    if (n.value() == -128) {
                        n.clear();
                        continue;
                    }

                    if (!saved_byte.has_value())
                        saved_byte = TRY(read_value<u8>());

                    n.value() = n.value() + 1;

                    auto const byte_backup = *saved_byte;

                    if (n == 1) {
                        saved_byte.clear();
                        n.clear();
                    }

                    return byte_backup;
                }
            };

            TRY(loop_over_pixels(move(read_packed_byte)));
            break;
        }
        default:
            return Error::from_string_literal("This compression type is not supported yet :^)");
        }

        return {};
    }

    template<typename T>
    ErrorOr<T> read_value()
    {
        if (m_byte_order == ByteOrder::LittleEndian)
            return TRY(m_stream->read_value<LittleEndian<T>>());
        if (m_byte_order == ByteOrder::BigEndian)
            return TRY(m_stream->read_value<BigEndian<T>>());
        VERIFY_NOT_REACHED();
    }

    ErrorOr<void> read_next_idf_offset()
    {
        auto const next_block_position = TRY(read_value<u32>());

        if (next_block_position != 0)
            m_next_ifd = Optional<u32> { next_block_position };
        else
            m_next_ifd = OptionalNone {};
        dbgln_if(TIFF_DEBUG, "Setting image file directory pointer to {}", m_next_ifd);
        return {};
    }

    ErrorOr<void> read_image_file_header()
    {
        // Section 2: TIFF Structure - Image File Header

        auto const byte_order = TRY(m_stream->read_value<u16>());

        switch (byte_order) {
        case 0x4949:
            m_byte_order = ByteOrder::LittleEndian;
            break;
        case 0x4D4D:
            m_byte_order = ByteOrder::BigEndian;
            break;
        default:
            return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid byte order");
        }

        auto const magic_number = TRY(read_value<u16>());

        if (magic_number != 42)
            return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid magic number");

        TRY(read_next_idf_offset());

        return {};
    }

    ErrorOr<void> read_next_image_file_directory()
    {
        // Section 2: TIFF Structure - Image File Directory

        if (!m_next_ifd.has_value())
            return Error::from_string_literal("TIFFImageDecoderPlugin: Missing an Image File Directory");

        TRY(m_stream->seek(m_next_ifd.value()));

        auto const number_of_field = TRY(read_value<u16>());

        for (u16 i = 0; i < number_of_field; ++i)
            TRY(read_tag());

        TRY(read_next_idf_offset());
        return {};
    }

    ErrorOr<Type> read_type()
    {
        switch (TRY(read_value<u16>())) {
        case to_underlying(Type::Byte):
            return Type::Byte;
        case to_underlying(Type::ASCII):
            return Type::ASCII;
        case to_underlying(Type::UnsignedShort):
            return Type::UnsignedShort;
        case to_underlying(Type::UnsignedLong):
            return Type::UnsignedLong;
        case to_underlying(Type::UnsignedRational):
            return Type::UnsignedRational;
        case to_underlying(Type::Undefined):
            return Type::Undefined;
        case to_underlying(Type::SignedLong):
            return Type::SignedLong;
        case to_underlying(Type::SignedRational):
            return Type::SignedRational;
        case to_underlying(Type::UTF8):
            return Type::UTF8;
        default:
            return Error::from_string_literal("TIFFImageDecoderPlugin: Unknown type");
        }
    }

    static constexpr u8 size_of_type(Type type)
    {
        switch (type) {
        case Type::Byte:
            return 1;
        case Type::ASCII:
            return 1;
        case Type::UnsignedShort:
            return 2;
        case Type::UnsignedLong:
            return 4;
        case Type::UnsignedRational:
            return 8;
        case Type::Undefined:
            return 1;
        case Type::SignedLong:
            return 4;
        case Type::SignedRational:
            return 8;
        case Type::Float:
            return 4;
        case Type::Double:
            return 8;
        case Type::UTF8:
            return 1;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ErrorOr<Vector<Value, 1>> read_tiff_value(Type type, u32 count, u32 offset)
    {
        auto const old_offset = TRY(m_stream->tell());
        ScopeGuard reset_offset { [this, old_offset]() { MUST(m_stream->seek(old_offset)); } };

        TRY(m_stream->seek(offset));

        if (size_of_type(type) * count > m_stream->remaining())
            return Error::from_string_literal("TIFFImageDecoderPlugin: Tag size claims to be bigger that remaining bytes");

        auto const read_every_values = [this, count]<typename T>() -> ErrorOr<Vector<Value>> {
            Vector<Value, 1> result {};
            TRY(result.try_ensure_capacity(count));
            if constexpr (IsSpecializationOf<T, Rational>) {
                for (u32 i = 0; i < count; ++i)
                    result.empend(T { TRY(read_value<typename T::Type>()), TRY(read_value<typename T::Type>()) });
            } else {
                for (u32 i = 0; i < count; ++i)
                    result.empend(typename TypePromoter<T>::Type(TRY(read_value<T>())));
            }
            return result;
        };

        switch (type) {
        case Type::Byte:
        case Type::Undefined:
            return read_every_values.template operator()<u8>();
        case Type::ASCII:
        case Type::UTF8: {
            Vector<Value, 1> result;
            auto string_data = TRY(ByteBuffer::create_uninitialized(count));
            TRY(m_stream->read_until_filled(string_data));
            result.empend(TRY(String::from_utf8(StringView { string_data.bytes() })));
            return result;
        }
        case Type::UnsignedShort:
            return read_every_values.template operator()<u16>();
        case Type::UnsignedLong:
            return read_every_values.template operator()<u32>();
        case Type::UnsignedRational:
            return read_every_values.template operator()<Rational<u32>>();
        case Type::SignedLong:
            return read_every_values.template operator()<i32>();
            ;
        case Type::SignedRational:
            return read_every_values.template operator()<Rational<i32>>();
        default:
            VERIFY_NOT_REACHED();
        }
    }

    ErrorOr<void> read_tag()
    {
        auto const tag = TRY(read_value<u16>());
        auto const type = TRY(read_type());
        auto const count = TRY(read_value<u32>());

        Checked<u32> checked_size = size_of_type(type);
        checked_size *= count;

        if (checked_size.has_overflow())
            return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag with too large data");

        auto tiff_value = TRY(([=, this]() -> ErrorOr<Vector<Value>> {
            if (checked_size.value() <= 4) {
                auto value = TRY(read_tiff_value(type, count, TRY(m_stream->tell())));
                TRY(m_stream->discard(4));
                return value;
            }
            auto const offset = TRY(read_value<u32>());
            return read_tiff_value(type, count, offset);
        }()));

        if constexpr (TIFF_DEBUG) {
            if (tiff_value.size() == 1) {
                tiff_value[0].visit(
                    [&](auto const& value) {
                        dbgln("Read tag({}), type({}): {}", tag, to_underlying(type), value);
                    });
            } else {
                dbg("Read tag({}), type({}): [", tag, to_underlying(type));
                for (u32 i = 0; i < tiff_value.size(); ++i) {
                    tiff_value[i].visit(
                        [&](auto const& value) {
                            dbg("{}", value);
                        });
                    if (i != tiff_value.size() - 1)
                        dbg(", ");
                }
                dbgln("]");
            }
        }

        TRY(handle_tag(m_metadata, tag, type, count, move(tiff_value)));

        return {};
    }

    NonnullOwnPtr<FixedMemoryStream> m_stream;
    State m_state {};
    RefPtr<Gfx::Bitmap> m_bitmap {};

    ByteOrder m_byte_order {};
    Optional<u32> m_next_ifd {};

    Metadata m_metadata {};
};

}

TIFFImageDecoderPlugin::TIFFImageDecoderPlugin(NonnullOwnPtr<FixedMemoryStream> stream)
{
    m_context = make<TIFF::TIFFLoadingContext>(move(stream));
}

bool TIFFImageDecoderPlugin::sniff(ReadonlyBytes bytes)
{
    if (bytes.size() < 4)
        return false;
    bool const valid_little_endian = bytes[0] == 0x49 && bytes[1] == 0x49 && bytes[2] == 0x2A && bytes[3] == 0x00;
    bool const valid_big_endian = bytes[0] == 0x4D && bytes[1] == 0x4D && bytes[2] == 0x00 && bytes[3] == 0x2A;
    return valid_little_endian || valid_big_endian;
}

Gfx::IntSize TIFFImageDecoderPlugin::size()
{
    return m_context->size();
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> TIFFImageDecoderPlugin::create(ReadonlyBytes data)
{
    auto stream = TRY(try_make<FixedMemoryStream>(data));
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) TIFFImageDecoderPlugin(move(stream))));
    TRY(plugin->m_context->decode_image_header());
    return plugin;
}

ErrorOr<ImageFrameDescriptor> TIFFImageDecoderPlugin::frame(size_t index, Optional<Gfx::IntSize>)
{
    if (index > 0)
        return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid frame index");

    if (m_context->state() == TIFF::TIFFLoadingContext::State::Error)
        return Error::from_string_literal("TIFFImageDecoderPlugin: Decoding failed");

    if (m_context->state() < TIFF::TIFFLoadingContext::State::FrameDecoded)
        TRY(m_context->decode_frame());

    return ImageFrameDescriptor { m_context->bitmap(), 0 };
}
}

template<typename T>
struct AK::Formatter<Media::TIFF::Rational<T>> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Media::TIFF::Rational<T> value)
    {
        return Formatter<FormatString>::format(builder, "{} ({}/{})"sv, static_cast<double>(value.numerator) / value.denominator, value.numerator, value.denominator);
    }
};
