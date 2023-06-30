/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Queue.h"
#include "SampleFormats.h"
#include <AK/ByteBuffer.h>
#include <AK/Types.h>
#include <AK/Variant.h>
#include <LibCrypto/Checksum/CRC16.h>
#include <LibCrypto/Checksum/CRC8.h>

namespace Audio {

// These are not the actual values stored in the file! They are marker constants instead, only used temporarily in the decoder.
// 11.22.3. INTERCHANNEL SAMPLE BLOCK SIZE
#define FLAC_BLOCKSIZE_AT_END_OF_HEADER_8 0xffffffff
#define FLAC_BLOCKSIZE_AT_END_OF_HEADER_16 0xfffffffe
// 11.22.4. SAMPLE RATE
#define FLAC_SAMPLERATE_AT_END_OF_HEADER_8 0xffffffff
#define FLAC_SAMPLERATE_AT_END_OF_HEADER_16 0xfffffffe
#define FLAC_SAMPLERATE_AT_END_OF_HEADER_16X10 0xfffffffd

// 11.22.11. FRAME CRC
// The polynomial used here is known as CRC-8-CCITT.
static constexpr u8 flac_polynomial = 0x07;
using FlacFrameHeaderCRC = Crypto::Checksum::CRC8<flac_polynomial>;

// 11.23. FRAME_FOOTER
// The polynomial used here is known as CRC-16-IBM.
static constexpr u16 ibm_polynomial = 0xA001;
using IBMCRC = Crypto::Checksum::CRC16<ibm_polynomial>;

// 11.8 BLOCK_TYPE (7 bits)
enum class FlacMetadataBlockType : u8 {
    STREAMINFO = 0,     // Important data about the audio format
    PADDING = 1,        // Non-data block to be ignored
    APPLICATION = 2,    // Ignored
    SEEKTABLE = 3,      // Seeking info, maybe to be used later
    VORBIS_COMMENT = 4, // Ignored
    CUESHEET = 5,       // Ignored
    PICTURE = 6,        // Ignored
    INVALID = 127,      // Error
};

// 11.22.5. CHANNEL ASSIGNMENT
enum class FlacFrameChannelType : u8 {
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

// 11.25.1. SUBFRAME TYPE
enum class FlacSubframeType : u8 {
    Constant = 0,
    Verbatim = 1,
    Fixed = 0b001000,
    LPC = 0b100000,
    // others are reserved
};

// 11.30.1. RESIDUAL_CODING_METHOD
enum class FlacResidualMode : u8 {
    Rice4Bit = 0,
    Rice5Bit = 1,
};

// 11.6. METADATA_BLOCK
struct FlacRawMetadataBlock {
    bool is_last_block;
    FlacMetadataBlockType type;
    u32 length; // 24 bits
    ByteBuffer data;
};

// 11.22. FRAME_HEADER
struct FlacFrameHeader {
    u32 sample_count;
    u32 sample_rate;
    FlacFrameChannelType channels;
    u8 bit_depth;
    u8 checksum;
};

// 11.25. SUBFRAME_HEADER
struct FlacSubframeHeader {
    FlacSubframeType type;
    // order for fixed and LPC subframes
    u8 order;
    u8 wasted_bits_per_sample;
    u8 bits_per_sample;
};

}
