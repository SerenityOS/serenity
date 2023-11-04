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

namespace Gfx {

class TIFFLoadingContext {
public:
    enum class State {
        NotDecoded = 0,
        Error,
        HeaderDecoded,
        FrameDecoded,
    };

    template<OneOf<u32, i32> x32>
    struct Rational {
        using Type = x32;
        x32 numerator;
        x32 denominator;
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

    IntSize size() const
    {
        return m_size;
    }

    State state() const
    {
        return m_state;
    }

    RefPtr<Bitmap> bitmap() const
    {
        return m_bitmap;
    }

private:
    enum class ByteOrder {
        LittleEndian,
        BigEndian,
    };

    enum class Type {
        Byte = 1,
        ASCII = 2,
        UnsignedShort = 3,
        UnsignedLong = 4,
        UnsignedRational = 5,
        Undefined = 7,
        SignedLong = 9,
        SignedRational = 10,
        Float = 11,
        Double = 12,
        UTF8 = 129,
    };

    using Value = Variant<u8, String, u16, u32, Rational<u32>, i32, Rational<i32>>;

    // This enum is progessively defined across sections but summarized in:
    // Appendix A: TIFF Tags Sorted by Number
    enum class Compression {
        NoCompression = 1,
        CCITT = 2,
        Group3Fax = 3,
        Group4Fax = 4,
        LZW = 5,
        JPEG = 6,
        PackBits = 32773,
    };

    enum class Predictor {
        None = 1,
        HorizontalDifferencing = 2,
    };

    template<typename ByteReader>
    ErrorOr<void> loop_over_pixels(ByteReader&& byte_reader, Function<ErrorOr<void>(u32)> initializer = {})
    {
        for (u32 strip_index = 0; strip_index < m_strip_offsets.size(); ++strip_index) {
            TRY(m_stream->seek(m_strip_offsets[strip_index]));
            if (initializer)
                TRY(initializer(m_strip_bytes_count[strip_index]));
            for (u32 row = 0; row < m_rows_per_strip; row++) {
                auto const scanline = row + m_rows_per_strip * strip_index;
                if (scanline >= static_cast<u32>(m_size.height()))
                    break;

                Optional<Color> last_color {};

                for (u32 column = 0; column < static_cast<u32>(m_size.width()); ++column) {
                    auto color = Color { TRY(byte_reader()), TRY(byte_reader()), TRY(byte_reader()) };

                    if (m_predictor == Predictor::HorizontalDifferencing && last_color.has_value()) {
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
        m_bitmap = TRY(Bitmap::create(BitmapFormat::BGRA8888, m_size));

        switch (m_compression) {
        case Compression::NoCompression:
            TRY(loop_over_pixels([this]() { return read_value<u8>(); }));
            break;
        case Compression::LZW: {
            Vector<u8> result_buffer {};
            Optional<Compress::LZWDecoder<BigEndianInputBitStream>> decoder {};

            u16 clear_code {};
            u16 end_of_information_code {};

            auto initializer = [&](u32 bytes) -> ErrorOr<void> {
                auto strip_stream = make<FixedMemoryStream>(TRY(m_stream->read_in_place<u8 const>(bytes)));
                auto lzw_stream = make<BigEndianInputBitStream>(MaybeOwned<Stream>(move(strip_stream)));
                decoder = Compress::LZWDecoder { MaybeOwned<BigEndianInputBitStream> { move(lzw_stream) }, 8, -1 };

                clear_code = decoder->add_control_code();
                end_of_information_code = decoder->add_control_code();

                return {};
            };

            auto read_lzw_byte = [&]() -> ErrorOr<u8> {
                while (true) {
                    if (!result_buffer.is_empty())
                        return result_buffer.take_first();

                    auto const code = TRY(decoder->next_code());

                    if (code == clear_code) {
                        decoder->reset();
                        continue;
                    }

                    if (code == end_of_information_code)
                        return Error::from_string_literal("TIFFImageDecoderPlugin: Reached end of LZW stream");

                    result_buffer = decoder->get_output();
                }
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
                    result.empend(TRY(read_value<T>()));
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

        auto const tiff_value = TRY(([=, this]() -> ErrorOr<Vector<Value>> {
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

        TRY(handle_tag(tag, type, count, tiff_value));

        return {};
    }

    ErrorOr<void> handle_tag(u16 tag, Type type, u32 count, Vector<Value> const& value)
    {
        // FIXME: Make that easy to extend
        switch (tag) {
        case 256:
            // ImageWidth
            if ((type != Type::UnsignedShort && type != Type::UnsignedLong) || count != 1)
                return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 256");

            value[0].visit(
                [this]<OneOf<u16, u32> T>(T const& width) {
                    m_size.set_width(width);
                },
                [&](auto const&) {
                    VERIFY_NOT_REACHED();
                });
            break;

        case 257:
            // ImageLength
            if ((type != Type::UnsignedShort && type != Type::UnsignedLong) || count != 1)
                return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 257");

            value[0].visit(
                [this]<OneOf<u16, u32> T>(T const& width) {
                    m_size.set_height(width);
                },
                [&](auto const&) {
                    VERIFY_NOT_REACHED();
                });
            break;

        case 258:
            // BitsPerSample
            if (type != Type::UnsignedShort || count != 3)
                return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 258");

            for (u8 i = 0; i < m_bits_per_sample.size(); ++i) {
                value[i].visit(
                    [this, i](u16 const& bits_per_sample) {
                        m_bits_per_sample[i] = bits_per_sample;
                    },
                    [&](auto const&) {
                        VERIFY_NOT_REACHED();
                    });
            }
            break;

        case 259:
            // Compression
            if (type != Type::UnsignedShort || count != 1)
                return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 259");

            TRY(value[0].visit(
                [this](u16 const& compression) -> ErrorOr<void> {
                    if (compression > 6 && compression != to_underlying(Compression::PackBits))
                        return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid compression value");

                    m_compression = static_cast<Compression>(compression);
                    return {};
                },
                [&](auto const&) -> ErrorOr<void> {
                    VERIFY_NOT_REACHED();
                }));
            break;

        case 273:
            // StripOffsets
            if (type != Type::UnsignedShort && type != Type::UnsignedLong)
                return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 273");

            TRY(m_strip_offsets.try_ensure_capacity(count));
            for (u32 i = 0; i < count; ++i) {
                value[i].visit(
                    [this]<OneOf<u16, u32> T>(T const& offset) {
                        m_strip_offsets.append(offset);
                    },
                    [&](auto const&) {
                        VERIFY_NOT_REACHED();
                    });
            }
            break;

        case 277:
            // SamplesPerPixel
            if (type != Type::UnsignedShort || count != 1)
                return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 277");
            TRY(value[0].visit(
                [](u16 const& samples_per_pixels) -> ErrorOr<void> {
                    if (samples_per_pixels != 3)
                        return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 277");
                    return {};
                },
                [&](auto const&) -> ErrorOr<void> {
                    VERIFY_NOT_REACHED();
                }));
            break;

        case 278:
            // RowsPerStrip
            if ((type != Type::UnsignedShort && type != Type::UnsignedLong) || count != 1)
                return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 278");

            value[0].visit(
                [this]<OneOf<u16, u32> T>(T const& rows_per_strip) {
                    m_rows_per_strip = rows_per_strip;
                },
                [&](auto const&) {
                    VERIFY_NOT_REACHED();
                });
            break;

        case 279:
            // StripByteCounts
            if (type != Type::UnsignedShort && type != Type::UnsignedLong)
                return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 279");

            TRY(m_strip_bytes_count.try_ensure_capacity(count));
            for (u32 i = 0; i < count; ++i) {
                value[i].visit(
                    [this]<OneOf<u16, u32> T>(T const& offset) {
                        m_strip_bytes_count.append(offset);
                    },
                    [&](auto const&) {
                        VERIFY_NOT_REACHED();
                    });
            }
            break;
        case 317:
            // Predictor
            if (type != Type::UnsignedShort || count != 1)
                return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid tag 317");

            TRY(value[0].visit(
                [this](u16 const& predictor) -> ErrorOr<void> {
                    if (predictor != 1 && predictor != 2)
                        return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid predictor value");

                    m_predictor = static_cast<Predictor>(predictor);
                    return {};
                },
                [&](auto const&) -> ErrorOr<void> {
                    VERIFY_NOT_REACHED();
                }));
            break;
        default:
            dbgln_if(TIFF_DEBUG, "Unknown tag: {}", tag);
        }

        return {};
    }

    NonnullOwnPtr<FixedMemoryStream> m_stream;
    IntSize m_size {};
    State m_state {};
    RefPtr<Bitmap> m_bitmap {};

    ByteOrder m_byte_order {};
    Optional<u32> m_next_ifd {};

    Array<u16, 3> m_bits_per_sample {};
    Compression m_compression {};
    Predictor m_predictor {};
    Vector<u32> m_strip_offsets {};
    u32 m_rows_per_strip {};
    Vector<u32> m_strip_bytes_count {};
};

TIFFImageDecoderPlugin::TIFFImageDecoderPlugin(NonnullOwnPtr<FixedMemoryStream> stream)
{
    m_context = make<TIFFLoadingContext>(move(stream));
}

bool TIFFImageDecoderPlugin::sniff(ReadonlyBytes bytes)
{
    if (bytes.size() < 4)
        return false;
    bool const valid_little_endian = bytes[0] == 0x49 && bytes[1] == 0x49 && bytes[2] == 0x2A && bytes[3] == 0x00;
    bool const valid_big_endian = bytes[0] == 0x4D && bytes[1] == 0x4D && bytes[2] == 0x00 && bytes[3] == 0x2A;
    return valid_little_endian || valid_big_endian;
}

IntSize TIFFImageDecoderPlugin::size()
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

ErrorOr<ImageFrameDescriptor> TIFFImageDecoderPlugin::frame(size_t index, Optional<IntSize>)
{
    if (index > 0)
        return Error::from_string_literal("TIFFImageDecoderPlugin: Invalid frame index");

    if (m_context->state() == TIFFLoadingContext::State::Error)
        return Error::from_string_literal("TIFFImageDecoderPlugin: Decoding failed");

    if (m_context->state() < TIFFLoadingContext::State::FrameDecoded)
        TRY(m_context->decode_frame());

    return ImageFrameDescriptor { m_context->bitmap(), 0 };
}
}

template<typename T>
struct AK::Formatter<Gfx::TIFFLoadingContext::Rational<T>> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::TIFFLoadingContext::Rational<T> value)
    {
        return Formatter<FormatString>::format(builder, "{} ({}/{})"sv, static_cast<double>(value.numerator) / value.denominator, value.numerator, value.denominator);
    }
};
