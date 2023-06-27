/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/FixedArray.h>
#include <LibAudio/Sample.h>

namespace Audio {

// Downmixes any number of channels to stereo, under the assumption that standard channel layout is followed:
// 1 channel = mono
// 2 channels = stereo (left, right)
// 3 channels = left, right, center
// 4 channels = front left/right, back left/right
// 5 channels = front left/right, center, back left/right
// 6 channels = front left/right, center, LFE, back left/right
// 7 channels = front left/right, center, LFE, back center, side left/right
// 8 channels = front left/right, center, LFE, back left/right, side left/right
template<ArrayLike<float> ChannelType, ArrayLike<ChannelType> InputType>
ErrorOr<FixedArray<Sample>> downmix_surround_to_stereo(InputType input)
{
    if (input.size() == 0)
        return Error::from_string_view("Cannot resample from 0 channels"sv);

    auto channel_count = input.size();
    auto sample_count = input[0].size();

    FixedArray<Sample> output = TRY(FixedArray<Sample>::create(sample_count));

    // FIXME: We could figure out a better way to mix the channels, possibly spatially, but for now:
    //        - Center and LFE channels are added to both left and right.
    //        - All left channels are added together on the left, all right channels are added together on the right.
    switch (channel_count) {
    case 1:
        for (auto i = 0u; i < sample_count; ++i)
            output[i] = Sample { input[0][i] };
        break;
    case 2:
        for (auto i = 0u; i < sample_count; ++i)
            output[i] = Sample { input[0][i], input[1][i] };
        break;
    case 3:
        for (auto i = 0u; i < sample_count; ++i)
            output[i] = Sample { input[0][i] + input[2][i],
                input[1][i] + input[2][i] };
        break;
    case 4:
        for (auto i = 0u; i < sample_count; ++i)
            output[i] = Sample { input[0][i] + input[2][i],
                input[1][i] + input[3][i] };
        break;
    case 5:
        for (auto i = 0u; i < sample_count; ++i)
            output[i] = Sample { input[0][i] + input[3][i] + input[2][i],
                input[1][i] + input[4][i] + input[2][i] };
        break;
    case 6:
        for (auto i = 0u; i < sample_count; ++i) {
            output[i] = Sample { input[0][i] + input[4][i] + input[2][i] + input[3][i],
                input[1][i] + input[5][i] + input[2][i] + input[3][i] };
        }
        break;
    case 7:
        for (auto i = 0u; i < sample_count; ++i) {
            output[i] = Sample { input[0][i] + input[5][i] + input[2][i] + input[3][i] + input[4][i],
                input[1][i] + input[6][i] + input[2][i] + input[3][i] + input[4][i] };
        }
        break;
    case 8:
        for (auto i = 0u; i < sample_count; ++i) {
            output[i] = Sample { input[0][i] + input[4][i] + input[6][i] + input[2][i] + input[3][i],
                input[1][i] + input[5][i] + input[7][i] + input[2][i] + input[3][i] };
        }
        break;
    default:
        return Error::from_string_view("Invalid number of channels greater than 8"sv);
    }

    return output;
}

}
