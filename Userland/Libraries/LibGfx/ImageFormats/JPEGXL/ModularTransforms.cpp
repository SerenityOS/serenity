/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ModularTransforms.h"
#include <LibGfx/ImageFormats/JPEGXL/Common.h>

namespace Gfx::JPEGXL {

// H.6.2.1 - Parameters
ErrorOr<SqueezeParams> read_squeeze_params(LittleEndianInputBitStream& stream)
{
    SqueezeParams squeeze_params;

    squeeze_params.horizontal = TRY(stream.read_bit());
    squeeze_params.in_place = TRY(stream.read_bit());
    squeeze_params.begin_c = U32(TRY(stream.read_bits(3)), 8 + TRY(stream.read_bits(6)), 72 + TRY(stream.read_bits(10)), 1096 + TRY(stream.read_bits(13)));
    squeeze_params.num_c = U32(1, 2, 3, 4 + TRY(stream.read_bits(4)));

    return squeeze_params;
}

// H.6.1 - General
ErrorOr<TransformInfo> read_transform_info(LittleEndianInputBitStream& stream)
{
    TransformInfo transform_info;

    transform_info.tr = static_cast<TransformInfo::TransformId>(TRY(stream.read_bits(2)));

    if (transform_info.tr != TransformInfo::TransformId::kSqueeze) {
        transform_info.begin_c = U32(
            TRY(stream.read_bits(3)),
            8 + TRY(stream.read_bits(3)),
            72 + TRY(stream.read_bits(10)),
            1096 + TRY(stream.read_bits(13)));
    }

    if (transform_info.tr == TransformInfo::TransformId::kRCT) {
        transform_info.rct_type = U32(
            6,
            TRY(stream.read_bits(2)),
            2 + TRY(stream.read_bits(4)),
            10 + TRY(stream.read_bits(6)));
    }

    if (transform_info.tr == TransformInfo::TransformId::kPalette) {
        transform_info.num_c = U32(1, 3, 4, 1 + TRY(stream.read_bits(13)));
        transform_info.nb_colours = U32(TRY(stream.read_bits(8)), 256 + TRY(stream.read_bits(10)), 1280 + TRY(stream.read_bits(12)), 5376 + TRY(stream.read_bits(16)));
        transform_info.nb_deltas = U32(0, 1 + TRY(stream.read_bits(8)), 257 + TRY(stream.read_bits(10)), 1281 + TRY(stream.read_bits(16)));
        transform_info.d_pred = TRY(stream.read_bits(4));
    }

    if (transform_info.tr == TransformInfo::TransformId::kSqueeze) {
        auto const num_sq = U32(0, 1 + TRY(stream.read_bits(4)), 9 + TRY(stream.read_bits(6)), 41 + TRY(stream.read_bits(8)));
        TRY(transform_info.sp.try_resize(num_sq));
        for (u32 i = 0; i < num_sq; ++i)
            transform_info.sp[i] = TRY(read_squeeze_params(stream));
    }

    return transform_info;
}

// H.6.3 - RCT (reversible colour transform)
static void apply_rct(Span<Channel> channels, TransformInfo const& transformation)
{
    for (u32 y {}; y < channels[transformation.begin_c].height(); y++) {
        for (u32 x {}; x < channels[transformation.begin_c].width(); x++) {

            auto a = channels[transformation.begin_c + 0].get(x, y);
            auto b = channels[transformation.begin_c + 1].get(x, y);
            auto c = channels[transformation.begin_c + 2].get(x, y);

            i32 d {};
            i32 e {};
            i32 f {};

            auto const permutation = transformation.rct_type / 7;
            auto const type = transformation.rct_type % 7;
            if (type == 6) { // YCgCo
                auto const tmp = a - (c >> 1);
                e = c + tmp;
                f = tmp - (b >> 1);
                d = f + b;
            } else {
                if (type & 1)
                    c = c + a;
                if ((type >> 1) == 1)
                    b = b + a;
                if ((type >> 1) == 2)
                    b = b + ((a + c) >> 1);
                d = a;
                e = b;
                f = c;
            }

            Array<i32, 3> v {};
            v[permutation % 3] = d;
            v[(permutation + 1 + (permutation / 3)) % 3] = e;
            v[(permutation + 2 - (permutation / 3)) % 3] = f;

            channels[transformation.begin_c + 0].set(x, y, v[0]);
            channels[transformation.begin_c + 1].set(x, y, v[1]);
            channels[transformation.begin_c + 2].set(x, y, v[2]);
        }
    }
}

// H.6.4 - Palette
static constexpr i16 kDeltaPalette[72][3] = {
    { 0, 0, 0 }, { 4, 4, 4 }, { 11, 0, 0 }, { 0, 0, -13 }, { 0, -12, 0 }, { -10, -10, -10 },
    { -18, -18, -18 }, { -27, -27, -27 }, { -18, -18, 0 }, { 0, 0, -32 }, { -32, 0, 0 }, { -37, -37, -37 },
    { 0, -32, -32 }, { 24, 24, 45 }, { 50, 50, 50 }, { -45, -24, -24 }, { -24, -45, -45 }, { 0, -24, -24 },
    { -34, -34, 0 }, { -24, 0, -24 }, { -45, -45, -24 }, { 64, 64, 64 }, { -32, 0, -32 }, { 0, -32, 0 },
    { -32, 0, 32 }, { -24, -45, -24 }, { 45, 24, 45 }, { 24, -24, -45 }, { -45, -24, 24 }, { 80, 80, 80 },
    { 64, 0, 0 }, { 0, 0, -64 }, { 0, -64, -64 }, { -24, -24, 45 }, { 96, 96, 96 }, { 64, 64, 0 },
    { 45, -24, -24 }, { 34, -34, 0 }, { 112, 112, 112 }, { 24, -45, -45 }, { 45, 45, -24 }, { 0, -32, 32 },
    { 24, -24, 45 }, { 0, 96, 96 }, { 45, -24, 24 }, { 24, -45, -24 }, { -24, -45, 24 }, { 0, -64, 0 },
    { 96, 0, 0 }, { 128, 128, 128 }, { 64, 0, 64 }, { 144, 144, 144 }, { 96, 96, 0 }, { -36, -36, 36 },
    { 45, -24, -45 }, { 45, -45, -24 }, { 0, 0, -96 }, { 0, 128, 128 }, { 0, 96, 0 }, { 45, 24, -45 },
    { -128, 0, 0 }, { 24, -45, 24 }, { -45, 24, -45 }, { 64, 0, -64 }, { 64, -64, -64 }, { 96, 0, 96 },
    { 45, -45, 24 }, { 24, 45, -45 }, { 64, 64, -64 }, { 128, 128, 0 }, { 0, 0, -128 }, { -24, 45, -45 }
};

// H.6.4 - Palette
static ErrorOr<void> apply_palette(Vector<Channel>& channel,
    TransformInfo const& tr,
    u32 bitdepth,
    WPHeader const& wp_params)
{
    auto first = tr.begin_c + 1;
    auto last = tr.begin_c + tr.num_c;
    for (u32 i = first + 1; i <= last; i++)
        channel.insert(i, TRY(channel[first].copy()));
    for (u32 c = 0; c < tr.num_c; c++) {
        auto self_correcting_data = TRY(SelfCorrectingData::create(wp_params, channel[first].width()));

        for (u32 y = 0; y < channel[first].height(); y++) {
            for (u32 x = 0; x < channel[first].width(); x++) {
                i32 index = channel[first + c].get(x, y);
                auto is_delta = index < static_cast<i64>(tr.nb_deltas);
                i32 value {};
                if (index >= 0 && index < static_cast<i64>(tr.nb_colours)) {
                    value = channel[0].get(index, c);
                } else if (index >= static_cast<i64>(tr.nb_colours)) {
                    index -= tr.nb_colours;
                    if (index < 64) {
                        value = ((index >> (2 * c)) % 4) * ((1 << bitdepth) - 1) / 4
                            + (1 << max(0, bitdepth - 3));
                    } else {
                        index -= 64;
                        for (u32 i = 0; i < c; i++)
                            index = index / 5;
                        value = (index % 5) * ((1 << bitdepth) - 1) / 4;
                    }
                } else if (c < 3) {
                    index = (-index - 1) % 143;
                    value = kDeltaPalette[(index + 1) >> 1][c];
                    if ((index & 1) == 0)
                        value = -value;
                    if (bitdepth > 8)
                        value <<= min(bitdepth, 24) - 8;
                } else {
                    value = 0;
                }
                channel[first + c].set(x, y, value);
                if (is_delta) {
                    auto const original = channel[first + c].get(x, y);
                    auto const neighborhood = retrieve_neighborhood(channel[first + c], x, y);
                    auto const self_prediction = self_correcting_data.compute_predictions(neighborhood, x);
                    auto const pred = prediction(neighborhood, self_prediction.prediction, tr.d_pred);
                    channel[first + c].set(x, y, original + pred);
                }
            }
        }
    }
    channel.remove(0);
    return {};
}

// H.6.2.2 - Horizontal inverse squeeze step
static i32 tendency(i32 A, i32 B, i32 C)
{
    if (A >= B && B >= C) {
        auto X = (4 * A - 3 * C - B + 6) / 12;
        if (X - (X & 1) > 2 * (A - B))
            X = 2 * (A - B) + 1;
        if (X + (X & 1) > 2 * (B - C))
            X = 2 * (B - C);
        return X;
    } else if (A <= B && B <= C) {
        auto X = (4 * A - 3 * C - B - 6) / 12;
        if (X + (X & 1) < 2 * (A - B))
            X = 2 * (A - B) - 1;
        if (X - (X & 1) < 2 * (B - C))
            X = 2 * (B - C);
        return X;
    }
    return 0;
}

// H.6.2.2 - Horizontal inverse squeeze step
static ErrorOr<void> horiz_isqueeze(Channel const& input_1, Channel const& input_2, Channel& output)
{
    // "This step takes two input channels of sizes W1 × H and W2 × H"
    if (input_1.height() != input_2.height())
        return Error::from_string_literal("JPEGXLLoader: Invalid size when undoing squeeze transform");
    auto h = input_1.height();
    auto w1 = input_1.width();
    auto w2 = input_2.width();

    // "Either W1 == W2 or W1 == W2 + 1."
    if (w1 != w2 && w1 != w2 + 1)
        return Error::from_string_literal("JPEGXLLoader: Invalid size when undoing squeeze transform");

    // "output channel of size (W1 + W2) × H."
    if ((w1 + w2) != output.width() || h != output.height())
        return Error::from_string_literal("JPEGXLLoader: Invalid size when undoing squeeze transform");

    for (u32 y = 0; y < h; y++) {
        for (u32 x = 0; x < w2; x++) {
            auto avg = input_1.get(x, y);
            auto residu = input_2.get(x, y);
            auto next_avg = (x + 1 < w1 ? input_1.get(x + 1, y) : avg);
            auto left = (x > 0 ? output.get((x << 1) - 1, y) : avg);
            auto diff = residu + tendency(left, avg, next_avg);
            auto first = avg + diff / 2;
            output.set(2 * x, y, first);
            output.set(2 * x + 1, y, first - diff);
        }
        if (w1 > w2)
            output.set(2 * w2, y, input_1.get(w2, y));
    }
    return {};
}

// H.6.2.3 -  Vertical inverse squeeze step
static ErrorOr<void> vert_isqueeze(Channel const& input_1, Channel const& input_2, Channel& output)
{
    // "This step takes two input channels of sizes W × H1 and W × H2"
    if (input_1.width() != input_2.width())
        return Error::from_string_literal("JPEGXLLoader: Invalid size when undoing squeeze transform");
    auto w = input_1.width();
    auto h1 = input_1.height();
    auto h2 = input_2.height();

    // "Either H1 == H2 or H1 == H2 + 1."
    if (h1 != h2 && h1 != h2 + 1)
        return Error::from_string_literal("JPEGXLLoader: Invalid size when undoing squeeze transform");

    // "output channel of size W × (H1 + H2)."
    if ((h1 + h2) != output.height() || w != output.width())
        return Error::from_string_literal("JPEGXLLoader: Invalid size when undoing squeeze transform");

    for (u32 y = 0; y < h2; y++) {
        for (u32 x = 0; x < w; x++) {
            auto avg = input_1.get(x, y);
            auto residu = input_2.get(x, y);
            auto next_avg = (y + 1 < h1 ? input_1.get(x, y + 1) : avg);
            auto top = (y > 0 ? output.get(x, (y << 1) - 1) : avg);
            auto diff = residu + tendency(top, avg, next_avg);
            auto first = avg + diff / 2;
            output.set(x, 2 * y, first);
            output.set(x, 2 * y + 1, first - diff);
        }
    }
    if (h1 > h2) {
        for (u32 x = 0; x < w; x++)
            output.set(x, 2 * h2, input_1.get(x, h2));
    }
    return {};
}

// H.6.2 - Squeeze
static ErrorOr<void> apply_squeeze(
    Vector<Channel>& channel,
    TransformInfo const& transformation)
{
    auto const& sp = transformation.sp;
    for (i64 i = sp.size() - 1; i >= 0; i--) {
        auto begin = transformation.sp[i].begin_c;
        auto end = begin + transformation.sp[i].num_c - 1;

        auto r = sp[i].in_place ? end + 1 : channel.size() + begin - end - 1;
        for (u32 c = begin; c <= end; c++) {
            Optional<Channel> output;
            if (sp[i].horizontal) {
                output = TRY(channel[c].copy(IntSize(channel[c].width() + channel[r].width(), channel[c].height())));
                TRY(horiz_isqueeze(channel[c], channel[r], *output));
            } else {
                output = TRY(channel[c].copy(IntSize(channel[c].width(), channel[c].height() + channel[r].height())));
                TRY(vert_isqueeze(channel[c], channel[r], *output));
            }
            channel[c] = output.release_value();
            /* Remove the channel with index r */
            channel.remove(r);
        }
    }
    return {};
}

ErrorOr<void> apply_transformation(
    Vector<Channel>& channels,
    TransformInfo const& transformation,
    u32 bit_depth,
    WPHeader const& wp_header)
{
    switch (transformation.tr) {
    case TransformInfo::TransformId::kRCT:
        apply_rct(channels, transformation);
        break;
    case TransformInfo::TransformId::kPalette:
        return apply_palette(channels, transformation, bit_depth, wp_header);
    case TransformInfo::TransformId::kSqueeze:
        return apply_squeeze(channels, transformation);
    default:
        VERIFY_NOT_REACHED();
    }
    return {};
}

}
