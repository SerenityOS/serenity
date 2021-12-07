/*
 * Copyright (c) 2021, Arne Elster <arne@elster.li>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/EnumBits.h>
#include <AK/FixedArray.h>

namespace Audio::MP3 {

enum class Mode {
    Stereo = 0,
    JointStereo = 1,
    DualChannel = 2,
    SingleChannel = 3,
};

enum class ModeExtension {
    Stereo = 0,
    IntensityStereo = 1,
    MsStereo = 2,
};
AK_ENUM_BITWISE_OPERATORS(ModeExtension)

enum class Emphasis {
    None = 0,
    Microseconds_50_15 = 1,
    Reserved = 2,
    CCITT_J17 = 3,
};

enum class BlockType {
    Normal = 0,
    Start = 1,
    Short = 2,
    End = 3,
};

struct Header {
    i32 id;
    i32 layer;
    bool protection_bit;
    i32 bitrate;
    i32 samplerate;
    bool padding_bit;
    bool private_bit;
    Mode mode;
    ModeExtension mode_extension;
    bool copyright_bit;
    bool original_bit;
    Emphasis emphasis;
    u16 crc16;
    size_t frame_size;
    size_t slot_count;

    size_t channel_count() const { return mode == Mode::SingleChannel ? 1 : 2; }
};

struct Granule {
    Array<double, 576> samples;
    Array<Array<double, 18>, 32> filter_bank_input;
    Array<Array<double, 32>, 18> pcm;
    u32 part_2_3_length;
    u32 big_values;
    u32 global_gain;
    u32 scalefac_compress;
    bool window_switching_flag;
    BlockType block_type;
    bool mixed_block_flag;
    Array<int, 3> table_select;
    Array<int, 3> sub_block_gain;
    u32 region0_count;
    u32 region1_count;
    bool preflag;
    bool scalefac_scale;
    bool count1table_select;
};

struct Channel {
    Array<Granule, 2> granules;
    Array<int, 39> scale_factors;
    Array<int, 4> scale_factor_selection_info;
};

struct MP3Frame {
    Header header;
    FixedArray<Channel> channels;
    off_t main_data_begin;
    u32 private_bits;

    MP3Frame(Header header)
        : header(header)
        , channels(FixedArray<Channel>::must_create_but_fixme_should_propagate_errors(header.channel_count()))
    {
    }
};

}
