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
// Section 9.1.1: Block Size Bits
#define FLAC_BLOCKSIZE_AT_END_OF_HEADER_8 0xffffffff
#define FLAC_BLOCKSIZE_AT_END_OF_HEADER_16 0xfffffffe
// Section 9.1.2: Sample Rate Bits
#define FLAC_SAMPLERATE_AT_END_OF_HEADER_8 0xffffffff
#define FLAC_SAMPLERATE_AT_END_OF_HEADER_16 0xfffffffe
#define FLAC_SAMPLERATE_AT_END_OF_HEADER_16X10 0xfffffffd

constexpr StringView flac_magic = "fLaC"sv;

// Section 9.1.8: Frame Header CRC
// The polynomial used here is known as CRC-8-CCITT.
static constexpr u8 flac_polynomial = 0x07;
using FlacFrameHeaderCRC = Crypto::Checksum::CRC8<flac_polynomial>;

// Section 9.3: Frame Footer
// The polynomial used here is known as CRC-16-IBM.
static constexpr u16 ibm_polynomial = 0xA001;
using IBMCRC = Crypto::Checksum::CRC16<ibm_polynomial>;

static constexpr size_t flac_seekpoint_size = (64 + 64 + 16) / 8;

// Section 8.1: Metadata Block Header
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

// Section 9.1.3: Channels Bits
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

// Section 9.2.1: Subframe Header
enum class FlacSubframeType : u8 {
    Constant = 0,
    Verbatim = 1,
    Fixed = 0b001000,
    LPC = 0b100000,
    // others are reserved
};

// Section 9.2.7: Coded Residual
enum class FlacResidualMode : u8 {
    Rice4Bit = 0,
    Rice5Bit = 1,
};

// Section 8: File-Level Metadata
struct FlacRawMetadataBlock {
    bool is_last_block;
    FlacMetadataBlockType type;
    u32 length; // 24 bits
    ByteBuffer data;

    ErrorOr<void> write_to_stream(Stream&) const;
};

enum class BlockingStrategy : u8 {
    Fixed = 0,
    Variable = 1,
};

// Block sample count can be stored in one of 5 ways.
enum class BlockSizeCategory : u8 {
    Reserved = 0b0000,
    S192 = 0b0001,
    // The formula for these four is 144 * (2^x), and it appears to be an MP3 compatibility feature.
    S576 = 0b0010,
    S1152 = 0b0011,
    S2304 = 0b0100,
    S4608 = 0b0101,
    // Actual size is stored later on.
    Uncommon8Bits = 0b0110,
    Uncommon16Bits = 0b0111,
    // Formula 2^x.
    S256 = 0b1000,
    S512 = 0b1001,
    S1024 = 0b1010,
    S2048 = 0b1011,
    S4096 = 0b1100,
    S8192 = 0b1101,
    S16384 = 0b1110,
    S32768 = 0b1111,
};

// Section 9.1: Frame Header
struct FlacFrameHeader {
    u32 sample_rate;
    // Referred to as “block size” in the specification.
    u16 sample_count;
    // If blocking strategy is fixed, this encodes the frame index instead of the sample index.
    u32 sample_or_frame_index;
    BlockingStrategy blocking_strategy;
    FlacFrameChannelType channels;
    u8 bit_depth;
    u8 checksum;

    ErrorOr<void> write_to_stream(Stream&) const;
};

// Section 9.2: Subframes
struct FlacSubframeHeader {
    FlacSubframeType type;
    // order for fixed and LPC subframes
    u8 order;
    u8 wasted_bits_per_sample;
    u8 bits_per_sample;
};

enum class FlacFixedLPC : size_t {
    Zero = 0,
    One = 1,
    Two = 2,
    Three = 3,
    Four = 4,
};

struct FlacLPCEncodedSubframe {
    Vector<i64> warm_up_samples;
    Variant<Vector<i64>, FlacFixedLPC> coefficients;
    Vector<i64> residuals;
    size_t residual_cost_bits;
    // If we’re only using one Rice partition, this is the optimal order to use.
    u8 single_partition_optimal_order;
};

}
