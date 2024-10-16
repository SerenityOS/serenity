/*
 * Copyright (c) 2024, Ben Jilks <benjyjilks@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BinarySearch.h>
#include <AK/Error.h>
#include <AK/Utf8View.h>
#include <LibTextCodec/Decoder.h>
#include <LibTextCodec/Encoder.h>
#include <LibTextCodec/LookupTables.h>

namespace TextCodec {

namespace {
UTF8Encoder s_utf8_encoder;
GB18030Encoder s_gb18030_encoder;
GB18030Encoder s_gbk_encoder(GB18030Encoder::IsGBK::Yes);
Big5Encoder s_big5_encoder;
EUCJPEncoder s_euc_jp_encoder;
EUCKREncoder s_euc_kr_encoder;
}

Optional<Encoder&> encoder_for_exact_name(StringView encoding)
{
    if (encoding.equals_ignoring_ascii_case("utf-8"sv))
        return s_utf8_encoder;
    if (encoding.equals_ignoring_ascii_case("big5"sv))
        return s_big5_encoder;
    if (encoding.equals_ignoring_ascii_case("euc-jp"sv))
        return s_euc_jp_encoder;
    if (encoding.equals_ignoring_ascii_case("euc-kr"sv))
        return s_euc_kr_encoder;
    if (encoding.equals_ignoring_ascii_case("gb18030"sv))
        return s_gb18030_encoder;
    if (encoding.equals_ignoring_ascii_case("gbk"sv))
        return s_gbk_encoder;
    dbgln("TextCodec: No encoder implemented for encoding '{}'", encoding);
    return {};
}

Optional<Encoder&> encoder_for(StringView label)
{
    auto encoding = get_standardized_encoding(label);
    return encoding.has_value() ? encoder_for_exact_name(encoding.value()) : Optional<Encoder&> {};
}

// https://encoding.spec.whatwg.org/#utf-8-encoder
ErrorOr<void> UTF8Encoder::process(Utf8View input, Function<ErrorOr<void>(u8)> on_byte)
{
    ReadonlyBytes bytes { input.bytes(), input.byte_length() };
    for (auto byte : bytes)
        TRY(on_byte(byte));
    return {};
}

// https://encoding.spec.whatwg.org/#euc-jp-encoder
ErrorOr<void> EUCJPEncoder::process(Utf8View input, Function<ErrorOr<void>(u8)> on_byte)
{
    for (auto item : input) {
        // 1. If code point is end-of-queue, return finished.

        // 2. If code point is an ASCII code point, return a byte whose value is code point.
        if (is_ascii(item)) {
            TRY(on_byte(static_cast<u8>(item)));
            continue;
        }

        // 3. If code point is U+00A5, return byte 0x5C.
        if (item == 0x00A5) {
            TRY(on_byte(static_cast<u8>(0x5C)));
            continue;
        }

        // 4. If code point is U+203E, return byte 0x7E.
        if (item == 0x203E) {
            TRY(on_byte(static_cast<u8>(0x7E)));
            continue;
        }

        // 5. If code point is in the range U+FF61 to U+FF9F, inclusive, return two bytes whose values are 0x8E and code point − 0xFF61 + 0xA1.
        if (item >= 0xFF61 && item <= 0xFF9F) {
            TRY(on_byte(0x8E));
            TRY(on_byte(static_cast<u8>(item - 0xFF61 + 0xA1)));
            continue;
        }

        // 6. If code point is U+2212, set it to U+FF0D.
        if (item == 0x2212)
            item = 0xFF0D;

        // 7. Let pointer be the index pointer for code point in index jis0208.
        auto pointer = code_point_jis0208_index(item);

        // 8. If pointer is null, return error with code point.
        if (!pointer.has_value()) {
            // TODO: Report error.
            continue;
        }

        // 9. Let lead be pointer / 94 + 0xA1.
        auto lead = *pointer / 94 + 0xA1;

        // 10. Let trail be pointer % 94 + 0xA1.
        auto trail = *pointer % 94 + 0xA1;

        // 11. Return two bytes whose values are lead and trail.
        TRY(on_byte(static_cast<u8>(lead)));
        TRY(on_byte(static_cast<u8>(trail)));
    }

    return {};
}

// https://encoding.spec.whatwg.org/#euc-kr-encoder
ErrorOr<void> EUCKREncoder::process(Utf8View input, Function<ErrorOr<void>(u8)> on_byte)
{
    for (u32 item : input) {
        // 1. If code point is end-of-queue, return finished.

        // 2. If code point is an ASCII code point, return a byte whose value is code point.
        if (is_ascii(item)) {
            TRY(on_byte(static_cast<u8>(item)));
            continue;
        }

        // 3. Let pointer be the index pointer for code point in index EUC-KR.
        auto pointer = code_point_euc_kr_index(item);

        // 4. If pointer is null, return error with code point.
        if (!pointer.has_value()) {
            // TODO: Report error.
            continue;
        }

        // 5. Let lead be pointer / 190 + 0x81.
        auto lead = *pointer / 190 + 0x81;

        // 6. Let trail be pointer % 190 + 0x41.
        auto trail = *pointer % 190 + 0x41;

        // 7. Return two bytes whose values are lead and trail.
        TRY(on_byte(static_cast<u8>(lead)));
        TRY(on_byte(static_cast<u8>(trail)));
    }

    return {};
}

// https://encoding.spec.whatwg.org/#index-big5-pointer
static Optional<u32> index_big5_pointer(u32 code_point)
{
    // 1. Let index be index Big5 excluding all entries whose pointer is less than (0xA1 - 0x81) × 157.
    auto start_index = (0xA1 - 0x81) * 157 - s_big5_index_first_pointer;

    // 2. If code point is U+2550, U+255E, U+2561, U+256A, U+5341, or U+5345, return the last pointer
    //    corresponding to code point in index.
    if (Array<u32, 6> { 0x2550, 0x255E, 0x2561, 0x256A, 0x5341, 0x5345 }.contains_slow(code_point)) {
        for (u32 i = s_big5_index.size() - 1; i >= start_index; --i) {
            if (s_big5_index[i] == code_point) {
                return s_big5_index_first_pointer + i;
            }
        }
        return {};
    }

    // 3. Return the index pointer for code point in index.
    for (u32 i = start_index; i < s_big5_index.size(); ++i) {
        if (s_big5_index[i] == code_point) {
            return s_big5_index_first_pointer + i;
        }
    }
    return {};
}

// https://encoding.spec.whatwg.org/#big5-encoder
ErrorOr<void> Big5Encoder::process(Utf8View input, Function<ErrorOr<void>(u8)> on_byte)
{
    for (u32 item : input) {
        // 1. If code point is end-of-queue, return finished.

        // 2. If code point is an ASCII code point, return a byte whose value is code point.
        if (is_ascii(item)) {
            TRY(on_byte(static_cast<u8>(item)));
            continue;
        }

        // 3. Let pointer be the index Big5 pointer for code point.
        auto pointer = index_big5_pointer(item);

        // 4. If pointer is null, return error with code point.
        if (!pointer.has_value()) {
            // TODO: Report error.
            continue;
        }

        // 5. Let lead be pointer / 157 + 0x81.
        auto lead = *pointer / 157 + 0x81;

        // 6. Let trail be pointer % 157.
        auto trail = *pointer % 157;

        // 7. Let offset be 0x40 if trail is less than 0x3F, otherwise 0x62.
        auto offset = 0x62;
        if (trail < 0x3f)
            offset = 0x40;

        // 8. Return two bytes whose values are lead and trail + offset.
        TRY(on_byte(static_cast<u8>(lead)));
        TRY(on_byte(static_cast<u8>(trail + offset)));
    }

    return {};
}

// https://encoding.spec.whatwg.org/#index-gb18030-ranges-pointer
static u32 index_gb18030_ranges_pointer(u32 code_point)
{
    // 1. If code point is U+E7C7, return pointer 7457.
    if (code_point == 0xe7c7)
        return 7457;

    // 2. Let offset be the last code point in index gb18030 ranges that is less than
    //    or equal to code point and let pointer offset be its corresponding pointer.
    size_t last_index;
    binary_search(s_gb18030_ranges, code_point, &last_index, [](auto const code_point, auto const& entry) {
        return code_point - entry.code_point;
    });
    auto offset = s_gb18030_ranges[last_index].code_point;
    auto pointer_offset = s_gb18030_ranges[last_index].pointer;

    // 3. Return a pointer whose value is pointer offset + code point − offset.
    return pointer_offset + code_point - offset;
}

GB18030Encoder::GB18030Encoder(IsGBK is_gbk)
    : m_is_gbk(is_gbk)
{
}

// https://encoding.spec.whatwg.org/#gb18030-encoder
ErrorOr<void> GB18030Encoder::process(Utf8View input, Function<ErrorOr<void>(u8)> on_byte)
{
    bool gbk = (m_is_gbk == IsGBK::Yes);

    for (u32 item : input) {
        // 1. If code point is end-of-queue, return finished.

        // 2. If code point is an ASCII code point, return a byte whose value is code point.
        if (is_ascii(item)) {
            TRY(on_byte(static_cast<u8>(item)));
            continue;
        }

        // 3. If code point is U+E5E5, return error with code point.
        if (item == 0xE5E5) {
            // TODO: Report error.
            continue;
        }

        // 4. If is GBK is true and code point is U+20AC, return byte 0x80.
        if (gbk && item == 0x20AC) {
            TRY(on_byte(0x80));
            continue;
        }

        // 5. Let pointer be the index pointer for code point in index gb18030.
        auto pointer = code_point_gb18030_index(item);

        // 6. If pointer is non-null, then:
        if (pointer.has_value()) {
            // 1. Let lead be pointer / 190 + 0x81.
            auto lead = *pointer / 190 + 0x81;

            // 2. Let trail be pointer % 190.
            auto trail = *pointer % 190;

            // 3. Let offset be 0x40 if trail is less than 0x3F, otherwise 0x41.
            auto offset = 0x41;
            if (trail < 0x3f)
                offset = 0x40;

            // 4. Return two bytes whose values are lead and trail + offset.
            TRY(on_byte(static_cast<u8>(lead)));
            TRY(on_byte(static_cast<u8>(trail + offset)));
            continue;
        }

        // 7. If is GBK is true, return error with code point.
        if (gbk) {
            // TODO: Report error.
            continue;
        }

        // 8. Set pointer to the index gb18030 ranges pointer for code point.
        pointer = index_gb18030_ranges_pointer(item);

        // 9. Let byte1 be pointer / (10 × 126 × 10).
        auto byte1 = *pointer / (10 * 126 * 10);

        // 10. Set pointer to pointer % (10 × 126 × 10).
        pointer = *pointer % (10 * 126 * 10);

        // 11. Let byte2 be pointer / (10 × 126).
        auto byte2 = *pointer / (10 * 126);

        // 12. Set pointer to pointer % (10 × 126).
        pointer = *pointer % (10 * 126);

        // 13. Let byte3 be pointer / 10.
        auto byte3 = *pointer / 10;

        // 14. Let byte4 be pointer % 10.
        auto byte4 = *pointer % 10;

        // 15. Return four bytes whose values are byte1 + 0x81, byte2 + 0x30, byte3 + 0x81, byte4 + 0x30.
        TRY(on_byte(static_cast<u8>(byte1 + 0x81)));
        TRY(on_byte(static_cast<u8>(byte2 + 0x30)));
        TRY(on_byte(static_cast<u8>(byte3 + 0x81)));
        TRY(on_byte(static_cast<u8>(byte4 + 0x30)));
    }

    return {};
}

}
