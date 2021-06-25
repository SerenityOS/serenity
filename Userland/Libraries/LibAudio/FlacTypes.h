/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Buffer.h"
#include <AK/ByteBuffer.h>
#include <AK/Types.h>
#include <AK/Variant.h>

namespace Audio {

// Temporary constants for header blocksize/sample rate spec
#define FLAC_BLOCKSIZE_AT_END_OF_HEADER_8 0xffffffff
#define FLAC_BLOCKSIZE_AT_END_OF_HEADER_16 0xfffffffe
#define FLAC_SAMPLERATE_AT_END_OF_HEADER_8 0xffffffff
#define FLAC_SAMPLERATE_AT_END_OF_HEADER_16 0xfffffffe
#define FLAC_SAMPLERATE_AT_END_OF_HEADER_16X10 0xfffffffd

// Metadata block type, 7 bits.
enum FlacMetadataBlockType : u8 {
    STREAMINFO = 0,     // Important data about the audio format
    PADDING = 1,        // Non-data block to be ignored
    APPLICATION = 2,    // Ignored
    SEEKTABLE = 3,      // Seeking info, maybe to be used later
    VORBIS_COMMENT = 4, // Ignored
    CUESHEET = 5,       // Ignored
    PICTURE = 6,        // Ignored
    INVALID = 127,      // Error
};

// follows FLAC codes
enum FlacFrameChannelType : u8 {
    Mono = 0,
    Stereo = 1,
    StereoCenter = 2,    // left, right, center
    Surround4p0 = 3,     // front left/right, back left/right
    Surround5p0 = 4,     // front left/right, center, back left/right
    Surround5p1 = 5,     // front left/right, center, LFE, back left/right
    Surround6p1 = 6,     // front left/right, center, LFE, back center, side left/right
    Surround7p1 = 7,     // front left/right, center, LFE, back left/right, side left/right
    LeftSideStereo = 8,  // channel coupling: left and difference
    RightSideStereo = 9, // channel coupling: difference and right
    MidSideStereo = 10,  // channel coupling: center and difference
    // others are reserved
};

// follows FLAC codes
enum FlacSubframeType : u8 {
    Constant = 0,
    Verbatim = 1,
    Fixed = 0b001000,
    LPC = 0b100000,
    // others are reserved
};

// follows FLAC codes
enum FlacResidualMode : u8 {
    Rice4Bit = 0,
    Rice5Bit = 1,
};

// Simple wrapper around any kind of metadata block
struct FlacRawMetadataBlock {
    bool is_last_block;
    FlacMetadataBlockType type;
    u32 length; // 24 bits
    ByteBuffer data;
};

// An abstract, parsed and validated FLAC frame
struct FlacFrameHeader {
    u32 sample_count;
    u32 sample_rate;
    FlacFrameChannelType channels;
    PcmSampleFormat bit_depth;
};

struct FlacSubframeHeader {
    FlacSubframeType type;
    // order for fixed and LPC subframes
    u8 order;
    u8 wasted_bits_per_sample;
    u8 bits_per_sample;
};

}
