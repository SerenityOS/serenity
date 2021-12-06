/*
 * Copyright (c) 2021, Arne Elster <arne@elster.li>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/HashMap.h>
#include <AK/Math.h>

namespace Audio::MP3::Tables {

// ISO/IEC 11172-3 (2.4.2.3)
Array<int, 4> LayerNumberLookup { -1, 3, 2, 1 };

// ISO/IEC 11172-3 (2.4.2.3)
Array<Array<int, 16>, 3> BitratesPerLayerLookup { {
    { 0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, -1 }, // Layer I
    { 0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, -1 },    // Layer II
    { 0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, -1 }      // Layer III
} };

// ISO/IEC 11172-3 (2.4.2.3)
Array<int, 4> SampleratesLookup { { 44100, 48000, 32000, -1 } };

// ISO/IEC 11172-3 (2.4.2.7)
Array<int, 16> ScalefacCompressSlen1 { { 0, 0, 0, 0, 3, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4 } };
Array<int, 16> ScalefacCompressSlen2 { { 0, 1, 2, 3, 0, 1, 2, 3, 1, 2, 3, 1, 2, 3, 2, 3 } };

// ISO/IEC 11172-3 (Table B.6)
Array<int, 22> Pretab { { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 3, 3, 3, 2, 0 } };

// ISO/IEC 11172-3 (Table B.9)
Array<double, 8> AliasReductionCoefficients {
    -0.6,
    -0.535,
    -0.33,
    -0.185,
    -0.095,
    -0.041,
    -0.0142,
    -0.0032
};

// This is using the cs[i] formula taken from ISO/IEC 11172-3 (below Table B.9)
Array<double, 8> AliasReductionCs { {
    1.0 / AK::sqrt(1 + AK::pow(AliasReductionCoefficients[0], 2.0)),
    1.0 / AK::sqrt(1 + AK::pow(AliasReductionCoefficients[1], 2.0)),
    1.0 / AK::sqrt(1 + AK::pow(AliasReductionCoefficients[2], 2.0)),
    1.0 / AK::sqrt(1 + AK::pow(AliasReductionCoefficients[3], 2.0)),
    1.0 / AK::sqrt(1 + AK::pow(AliasReductionCoefficients[4], 2.0)),
    1.0 / AK::sqrt(1 + AK::pow(AliasReductionCoefficients[5], 2.0)),
    1.0 / AK::sqrt(1 + AK::pow(AliasReductionCoefficients[6], 2.0)),
    1.0 / AK::sqrt(1 + AK::pow(AliasReductionCoefficients[7], 2.0)),
} };

// This is using the ca[i] formula taken from ISO/IEC 11172-3 (below Table B.9)
Array<double, 8> AliasReductionCa { {
    AliasReductionCoefficients[0] / AK::sqrt(1 + AK::pow(AliasReductionCoefficients[0], 2.0)),
    AliasReductionCoefficients[1] / AK::sqrt(1 + AK::pow(AliasReductionCoefficients[1], 2.0)),
    AliasReductionCoefficients[2] / AK::sqrt(1 + AK::pow(AliasReductionCoefficients[2], 2.0)),
    AliasReductionCoefficients[3] / AK::sqrt(1 + AK::pow(AliasReductionCoefficients[3], 2.0)),
    AliasReductionCoefficients[4] / AK::sqrt(1 + AK::pow(AliasReductionCoefficients[4], 2.0)),
    AliasReductionCoefficients[5] / AK::sqrt(1 + AK::pow(AliasReductionCoefficients[5], 2.0)),
    AliasReductionCoefficients[6] / AK::sqrt(1 + AK::pow(AliasReductionCoefficients[6], 2.0)),
    AliasReductionCoefficients[7] / AK::sqrt(1 + AK::pow(AliasReductionCoefficients[7], 2.0)),
} };

struct ScaleFactorBand {
    size_t width;
    size_t start;
    size_t end;
};

template<auto sizes, size_t offset = 0>
constexpr auto MakeShortScaleFactorBandArray()
{
    constexpr size_t N = sizes.size();
    Array<ScaleFactorBand, 3 * N> result {};
    size_t start = offset;

    for (size_t i = 0; i < N; i++) {
        result[3 * i + 0] = { sizes[i], start, start + sizes[i] - 1 };
        start += sizes[i];
        result[3 * i + 1] = { sizes[i], start, start + sizes[i] - 1 };
        start += sizes[i];
        result[3 * i + 2] = { sizes[i], start, start + sizes[i] - 1 };
        start += sizes[i];
    }

    return result;
}

template<auto sizes>
constexpr auto MakeLongScaleFactorBandArray()
{
    constexpr size_t N = sizes.size();
    Array<ScaleFactorBand, N> result {};
    size_t start = 0;

    for (size_t i = 0; i < N; i++) {
        result[i] = { sizes[i], start, start + sizes[i] - 1 };
        start += sizes[i];
    }

    return result;
}

template<auto sizes_long, auto sizes_short>
constexpr auto MakeMixedScaleFactorBandArray()
{
    constexpr size_t N = sizes_long.size() + sizes_short.size() * 3 + 1;
    Array<ScaleFactorBand, N> result {};

    constexpr auto long_bands = MakeLongScaleFactorBandArray<sizes_long>();
    constexpr auto short_bands = MakeShortScaleFactorBandArray<sizes_short, long_bands.back().end + 1>();

    for (size_t i = 0; i < long_bands.size(); i++) {
        result[i] = long_bands[i];
    }

    for (size_t i = 0; i < short_bands.size(); i++) {
        result[i + long_bands.size()] = short_bands[i];
    }

    for (size_t i = long_bands.size() + short_bands.size(); i < N; i++) {
        result[i] = { 0, 576, 576 };
    }

    return result;
}

// ISO/IEC 11172-3 (Table B.8)
HashMap<int, Array<ScaleFactorBand, 39>> ScaleFactorBandsShort = {
    { 32000,
        MakeShortScaleFactorBandArray<Array<size_t, 13> { 4, 4, 4, 4, 6, 8, 12, 16, 20, 26, 34, 42, 12 }>() },
    { 44100,
        MakeShortScaleFactorBandArray<Array<size_t, 13> { 4, 4, 4, 4, 6, 8, 10, 12, 14, 18, 22, 30, 56 }>() },
    { 48000,
        MakeShortScaleFactorBandArray<Array<size_t, 13> { 4, 4, 4, 4, 6, 6, 10, 12, 14, 16, 20, 26, 66 }>() },
};

// ISO/IEC 11172-3 (Table B.8)
HashMap<int, Array<ScaleFactorBand, 39>> ScaleFactorBandsMixed = {
    { 32000,
        MakeMixedScaleFactorBandArray<Array<size_t, 8> { 4, 4, 4, 4, 4, 4, 6, 6 }, Array<size_t, 10> { 4, 6, 8, 12, 16, 20, 26, 34, 42, 12 }>() },
    { 44100,
        MakeMixedScaleFactorBandArray<Array<size_t, 8> { 4, 4, 4, 4, 4, 4, 6, 6 }, Array<size_t, 10> { 4, 6, 8, 10, 12, 14, 18, 22, 30, 56 }>() },
    { 48000,
        MakeMixedScaleFactorBandArray<Array<size_t, 8> { 4, 4, 4, 4, 4, 4, 6, 6 }, Array<size_t, 10> { 4, 6, 6, 10, 12, 14, 16, 20, 26, 66 }>() },
};

// ISO/IEC 11172-3 (Table B.8)
HashMap<int, Array<ScaleFactorBand, 23>> ScaleFactorBandsLong = {
    { 32000,
        MakeLongScaleFactorBandArray<Array<size_t, 23> { 4, 4, 4, 4, 4, 4, 6, 6, 8, 10, 12, 16, 20, 24, 30, 38, 46, 56, 68, 84, 102, 25, 0 }>() },
    { 44100,
        MakeLongScaleFactorBandArray<Array<size_t, 23> { 4, 4, 4, 4, 4, 4, 6, 6, 8, 8, 10, 12, 16, 20, 24, 28, 34, 42, 50, 54, 76, 158, 0 }>() },
    { 48000,
        MakeLongScaleFactorBandArray<Array<size_t, 23> { 4, 4, 4, 4, 4, 4, 6, 6, 6, 8, 10, 12, 16, 18, 22, 28, 34, 40, 46, 54, 54, 192, 0 }>() },
};

// ISO/IEC 11172-3 (2.4.3.4.10.3 a)
Array<double, 36> WindowBlockType0 { {
    AK::sin(AK::Pi<double> / 36 * (0 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (1 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (2 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (3 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (4 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (5 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (6 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (7 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (8 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (9 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (10 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (11 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (12 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (13 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (14 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (15 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (16 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (17 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (18 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (19 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (20 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (21 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (22 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (23 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (24 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (25 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (26 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (27 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (28 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (29 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (30 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (31 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (32 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (33 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (34 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (35 + 0.5)),
} };

// ISO/IEC 11172-3 (2.4.3.4.10.3 b)
AK::Array<double, 36> WindowBlockType1 { { AK::sin(AK::Pi<double> / 36 * (0 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (1 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (2 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (3 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (4 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (5 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (6 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (7 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (8 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (9 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (10 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (11 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (12 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (13 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (14 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (15 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (16 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (17 + 0.5)),
    1,
    1,
    1,
    1,
    1,
    1,
    AK::sin(AK::Pi<double> / 12 * (24 - 18 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (25 - 18 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (26 - 18 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (27 - 18 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (28 - 18 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (29 - 18 + 0.5)),
    0,
    0,
    0,
    0,
    0,
    0 } };

// ISO/IEC 11172-3 (2.4.3.4.10.3 d)
AK::Array<double, 36> WindowBlockType2 { {
    AK::sin(AK::Pi<double> / 12 * (0 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (1 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (2 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (3 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (4 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (5 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (6 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (7 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (8 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (9 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (10 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (11 + 0.5)),

    AK::sin(AK::Pi<double> / 12 * (0 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (1 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (2 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (3 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (4 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (5 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (6 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (7 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (8 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (9 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (10 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (11 + 0.5)),

    AK::sin(AK::Pi<double> / 12 * (0 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (1 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (2 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (3 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (4 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (5 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (6 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (7 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (8 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (9 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (10 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (11 + 0.5)),
} };

// ISO/IEC 11172-3 (2.4.3.4.10.3 c)
AK::Array<double, 36> WindowBlockType3 { { 0,
    0,
    0,
    0,
    0,
    0,
    AK::sin(AK::Pi<double> / 12 * (6 - 6 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (7 - 6 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (8 - 6 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (9 - 6 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (10 - 6 + 0.5)),
    AK::sin(AK::Pi<double> / 12 * (11 - 6 + 0.5)),
    1,
    1,
    1,
    1,
    1,
    1,
    AK::sin(AK::Pi<double> / 36 * (18 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (19 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (20 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (21 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (22 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (23 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (24 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (25 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (26 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (27 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (28 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (29 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (30 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (31 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (32 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (33 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (34 + 0.5)),
    AK::sin(AK::Pi<double> / 36 * (35 + 0.5)) } };

// ISO/IEC 11172-3 (Table B.3)
AK::Array<double, 512> WindowSynthesis {
    0.000000000, -0.000015259, -0.000015259, -0.000015259, -0.000015259, -0.000015259, -0.000015259, -0.000030518,
    -0.000030518, -0.000030518, -0.000030518, -0.000045776, -0.000045776, -0.000061035, -0.000061035, -0.000076294,
    -0.000076294, -0.000091553, -0.000106812, -0.000106812, -0.000122070, -0.000137329, -0.000152588, -0.000167847,
    -0.000198364, -0.000213623, -0.000244141, -0.000259399, -0.000289917, -0.000320435, -0.000366211, -0.000396729,
    -0.000442505, -0.000473022, -0.000534058, -0.000579834, -0.000625610, -0.000686646, -0.000747681, -0.000808716,
    -0.000885010, -0.000961304, -0.001037598, -0.001113892, -0.001205444, -0.001296997, -0.001388550, -0.001480103,
    -0.001586914, -0.001693726, -0.001785278, -0.001907349, -0.002014160, -0.002120972, -0.002243042, -0.002349854,
    -0.002456665, -0.002578735, -0.002685547, -0.002792358, -0.002899170, -0.002990723, -0.003082275, -0.003173828,
    0.003250122, 0.003326416, 0.003387451, 0.003433228, 0.003463745, 0.003479004, 0.003479004, 0.003463745,
    0.003417969, 0.003372192, 0.003280640, 0.003173828, 0.003051758, 0.002883911, 0.002700806, 0.002487183,
    0.002227783, 0.001937866, 0.001617432, 0.001266479, 0.000869751, 0.000442505, -0.000030518, -0.000549316,
    -0.001098633, -0.001693726, -0.002334595, -0.003005981, -0.003723145, -0.004486084, -0.005294800, -0.006118774,
    -0.007003784, -0.007919312, -0.008865356, -0.009841919, -0.010848999, -0.011886597, -0.012939453, -0.014022827,
    -0.015121460, -0.016235352, -0.017349243, -0.018463135, -0.019577026, -0.020690918, -0.021789551, -0.022857666,
    -0.023910522, -0.024932861, -0.025909424, -0.026840210, -0.027725220, -0.028533936, -0.029281616, -0.029937744,
    -0.030532837, -0.031005859, -0.031387329, -0.031661987, -0.031814575, -0.031845093, -0.031738281, -0.031478882,
    0.031082153, 0.030517578, 0.029785156, 0.028884888, 0.027801514, 0.026535034, 0.025085449, 0.023422241,
    0.021575928, 0.019531250, 0.017257690, 0.014801025, 0.012115479, 0.009231567, 0.006134033, 0.002822876,
    -0.000686646, -0.004394531, -0.008316040, -0.012420654, -0.016708374, -0.021179199, -0.025817871, -0.030609131,
    -0.035552979, -0.040634155, -0.045837402, -0.051132202, -0.056533813, -0.061996460, -0.067520142, -0.073059082,
    -0.078628540, -0.084182739, -0.089706421, -0.095169067, -0.100540161, -0.105819702, -0.110946655, -0.115921021,
    -0.120697021, -0.125259399, -0.129562378, -0.133590698, -0.137298584, -0.140670776, -0.143676758, -0.146255493,
    -0.148422241, -0.150115967, -0.151306152, -0.151962280, -0.152069092, -0.151596069, -0.150497437, -0.148773193,
    -0.146362305, -0.143264771, -0.139450073, -0.134887695, -0.129577637, -0.123474121, -0.116577148, -0.108856201,
    0.100311279, 0.090927124, 0.080688477, 0.069595337, 0.057617187, 0.044784546, 0.031082153, 0.016510010,
    0.001068115, -0.015228271, -0.032379150, -0.050354004, -0.069168091, -0.088775635, -0.109161377, -0.130310059,
    -0.152206421, -0.174789429, -0.198059082, -0.221984863, -0.246505737, -0.271591187, -0.297210693, -0.323318481,
    -0.349868774, -0.376800537, -0.404083252, -0.431655884, -0.459472656, -0.487472534, -0.515609741, -0.543823242,
    -0.572036743, -0.600219727, -0.628295898, -0.656219482, -0.683914185, -0.711318970, -0.738372803, -0.765029907,
    -0.791213989, -0.816864014, -0.841949463, -0.866363525, -0.890090942, -0.913055420, -0.935195923, -0.956481934,
    -0.976852417, -0.996246338, -1.014617920, -1.031936646, -1.048156738, -1.063217163, -1.077117920, -1.089782715,
    -1.101211548, -1.111373901, -1.120223999, -1.127746582, -1.133926392, -1.138763428, -1.142211914, -1.144287109,
    1.144989014, 1.144287109, 1.142211914, 1.138763428, 1.133926392, 1.127746582, 1.120223999, 1.111373901,
    1.101211548, 1.089782715, 1.077117920, 1.063217163, 1.048156738, 1.031936646, 1.014617920, 0.996246338,
    0.976852417, 0.956481934, 0.935195923, 0.913055420, 0.890090942, 0.866363525, 0.841949463, 0.816864014,
    0.791213989, 0.765029907, 0.738372803, 0.711318970, 0.683914185, 0.656219482, 0.628295898, 0.600219727,
    0.572036743, 0.543823242, 0.515609741, 0.487472534, 0.459472656, 0.431655884, 0.404083252, 0.376800537,
    0.349868774, 0.323318481, 0.297210693, 0.271591187, 0.246505737, 0.221984863, 0.198059082, 0.174789429,
    0.152206421, 0.130310059, 0.109161377, 0.088775635, 0.069168091, 0.050354004, 0.032379150, 0.015228271,
    -0.001068115, -0.016510010, -0.031082153, -0.044784546, -0.057617187, -0.069595337, -0.080688477, -0.090927124,
    0.100311279, 0.108856201, 0.116577148, 0.123474121, 0.129577637, 0.134887695, 0.139450073, 0.143264771,
    0.146362305, 0.148773193, 0.150497437, 0.151596069, 0.152069092, 0.151962280, 0.151306152, 0.150115967,
    0.148422241, 0.146255493, 0.143676758, 0.140670776, 0.137298584, 0.133590698, 0.129562378, 0.125259399,
    0.120697021, 0.115921021, 0.110946655, 0.105819702, 0.100540161, 0.095169067, 0.089706421, 0.084182739,
    0.078628540, 0.073059082, 0.067520142, 0.061996460, 0.056533813, 0.051132202, 0.045837402, 0.040634155,
    0.035552979, 0.030609131, 0.025817871, 0.021179199, 0.016708374, 0.012420654, 0.008316040, 0.004394531,
    0.000686646, -0.002822876, -0.006134033, -0.009231567, -0.012115479, -0.014801025, -0.017257690, -0.019531250,
    -0.021575928, -0.023422241, -0.025085449, -0.026535034, -0.027801514, -0.028884888, -0.029785156, -0.030517578,
    0.031082153, 0.031478882, 0.031738281, 0.031845093, 0.031814575, 0.031661987, 0.031387329, 0.031005859,
    0.030532837, 0.029937744, 0.029281616, 0.028533936, 0.027725220, 0.026840210, 0.025909424, 0.024932861,
    0.023910522, 0.022857666, 0.021789551, 0.020690918, 0.019577026, 0.018463135, 0.017349243, 0.016235352,
    0.015121460, 0.014022827, 0.012939453, 0.011886597, 0.010848999, 0.009841919, 0.008865356, 0.007919312,
    0.007003784, 0.006118774, 0.005294800, 0.004486084, 0.003723145, 0.003005981, 0.002334595, 0.001693726,
    0.001098633, 0.000549316, 0.000030518, -0.000442505, -0.000869751, -0.001266479, -0.001617432, -0.001937866,
    -0.002227783, -0.002487183, -0.002700806, -0.002883911, -0.003051758, -0.003173828, -0.003280640, -0.003372192,
    -0.003417969, -0.003463745, -0.003479004, -0.003479004, -0.003463745, -0.003433228, -0.003387451, -0.003326416,
    0.003250122, 0.003173828, 0.003082275, 0.002990723, 0.002899170, 0.002792358, 0.002685547, 0.002578735,
    0.002456665, 0.002349854, 0.002243042, 0.002120972, 0.002014160, 0.001907349, 0.001785278, 0.001693726,
    0.001586914, 0.001480103, 0.001388550, 0.001296997, 0.001205444, 0.001113892, 0.001037598, 0.000961304,
    0.000885010, 0.000808716, 0.000747681, 0.000686646, 0.000625610, 0.000579834, 0.000534058, 0.000473022,
    0.000442505, 0.000396729, 0.000366211, 0.000320435, 0.000289917, 0.000259399, 0.000244141, 0.000213623,
    0.000198364, 0.000167847, 0.000152588, 0.000137329, 0.000122070, 0.000106812, 0.000106812, 0.000091553,
    0.000076294, 0.000076294, 0.000061035, 0.000061035, 0.000045776, 0.000045776, 0.000030518, 0.000030518,
    0.000030518, 0.000030518, 0.000015259, 0.000015259, 0.000015259, 0.000015259, 0.000015259, 0.000015259
};

}
