/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>

namespace Gfx {

struct QuantizationTable {
    Array<u8, 64> table {};
    u8 id {};
};

// K.1 - Quantization tables for luminance and chrominance components

// clang-format off
constexpr static QuantizationTable s_default_luminance_quantization_table {
    .table = {
        16, 11, 10, 16, 124, 140, 151, 161,
        12, 12, 14, 19, 126, 158, 160, 155,
        14, 13, 16, 24, 140, 157, 169, 156,
        14, 17, 22, 29, 151, 187, 180, 162,
        18, 22, 37, 56, 168, 109, 103, 177,
        24, 35, 55, 64, 181, 104, 113, 192,
        49, 64, 78, 87, 103, 121, 120, 101,
        72, 92, 95, 98, 112, 100, 103, 199,
    },
    .id = 0,
};

constexpr static QuantizationTable s_default_chrominance_quantization_table {
    .table = {
        17, 18, 24, 47, 99, 99, 99, 99,
        18, 21, 26, 66, 99, 99, 99, 99,
        24, 26, 56, 99, 99, 99, 99, 99,
        47, 66, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
    },
    .id = 1,
};

constexpr static QuantizationTable s_dummy_quantization_table {
    .table = {
        1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1,
    },
    .id = 1,
};

// clang-format on

struct OutputHuffmanTable {
    struct Symbol {
        u8 input_byte {};
        u8 code_length {};
        u16 word {};
    };

    Symbol from_input_byte(u8 input_byte) const
    {
        for (auto symbol : table) {
            if (symbol.input_byte == input_byte)
                return symbol;
        }
        VERIFY_NOT_REACHED();
    }

    Vector<Symbol, 16> table {};
    u8 id {};
};

static OutputHuffmanTable s_default_dc_luminance_huffman_table {
    .table = {
        { 0, 2, 0b00 },
        { 1, 3, 0b010 },
        { 2, 3, 0b011 },
        { 3, 3, 0b100 },
        { 4, 3, 0b101 },
        { 5, 3, 0b110 },
        { 6, 4, 0b1110 },
        { 7, 5, 0b11110 },
        { 8, 6, 0b111110 },
        { 9, 7, 0b1111110 },
        { 10, 8, 0b11111110 },
        { 11, 9, 0b111111110 },
    },
    .id = (0 << 4) | 0,
};

static OutputHuffmanTable s_default_dc_chrominance_huffman_table {
    .table = {
        { 0, 2, 0b00 },
        { 1, 2, 0b01 },
        { 2, 2, 0b10 },
        { 3, 3, 0b110 },
        { 4, 4, 0b1110 },
        { 5, 5, 0b11110 },
        { 6, 6, 0b111110 },
        { 7, 7, 0b1111110 },
        { 8, 8, 0b11111110 },
        { 9, 9, 0b111111110 },
        { 10, 10, 0b1111111110 },
        { 11, 11, 0b11111111110 },
    },
    .id = (0 << 4) | 1,
};

static OutputHuffmanTable s_default_ac_luminance_huffman_table {
    .table = {
        { 0x01, 2, 0b00 },
        { 0x02, 2, 0b01 },
        { 0x03, 3, 0b100 },
        { 0x00, 4, 0b1010 },
        { 0x04, 4, 0b1011 },
        { 0x11, 4, 0b1100 },
        { 0x05, 5, 0b11010 },
        { 0x12, 5, 0b11011 },
        { 0x21, 5, 0b11100 },
        { 0x31, 6, 0b111010 },
        { 0x41, 6, 0b111011 },
        { 0x06, 7, 0b1111000 },
        { 0x13, 7, 0b1111001 },
        { 0x51, 7, 0b1111010 },
        { 0x61, 7, 0b1111011 },
        { 0x07, 8, 0b11111000 },
        { 0x22, 8, 0b11111001 },
        { 0x71, 8, 0b11111010 },
        { 0x14, 9, 0b111110110 },
        { 0x32, 9, 0b111110111 },
        { 0x81, 9, 0b111111000 },
        { 0x91, 9, 0b111111001 },
        { 0xA1, 9, 0b111111010 },
        { 0x08, 10, 0b1111110110 },
        { 0x23, 10, 0b1111110111 },
        { 0x42, 10, 0b1111111000 },
        { 0xB1, 10, 0b1111111001 },
        { 0xC1, 10, 0b1111111010 },
        { 0x15, 11, 0b11111110110 },
        { 0x52, 11, 0b11111110111 },
        { 0xD1, 11, 0b11111111000 },
        { 0xF0, 11, 0b11111111001 },
        { 0x24, 12, 0b111111110100 },
        { 0x33, 12, 0b111111110101 },
        { 0x62, 12, 0b111111110110 },
        { 0x72, 12, 0b111111110111 },
        { 0x82, 15, 0b111111111000000 },
        { 0x09, 16, 0b1111111110000010 },
        { 0x0A, 16, 0b1111111110000011 },
        { 0x16, 16, 0b1111111110000100 },
        { 0x17, 16, 0b1111111110000101 },
        { 0x18, 16, 0b1111111110000110 },
        { 0x19, 16, 0b1111111110000111 },
        { 0x1A, 16, 0b1111111110001000 },
        { 0x25, 16, 0b1111111110001001 },
        { 0x26, 16, 0b1111111110001010 },
        { 0x27, 16, 0b1111111110001011 },
        { 0x28, 16, 0b1111111110001100 },
        { 0x29, 16, 0b1111111110001101 },
        { 0x2A, 16, 0b1111111110001110 },
        { 0x34, 16, 0b1111111110001111 },
        { 0x35, 16, 0b1111111110010000 },
        { 0x36, 16, 0b1111111110010001 },
        { 0x37, 16, 0b1111111110010010 },
        { 0x38, 16, 0b1111111110010011 },
        { 0x39, 16, 0b1111111110010100 },
        { 0x3A, 16, 0b1111111110010101 },
        { 0x43, 16, 0b1111111110010110 },
        { 0x44, 16, 0b1111111110010111 },
        { 0x45, 16, 0b1111111110011000 },
        { 0x46, 16, 0b1111111110011001 },
        { 0x47, 16, 0b1111111110011010 },
        { 0x48, 16, 0b1111111110011011 },
        { 0x49, 16, 0b1111111110011100 },
        { 0x4A, 16, 0b1111111110011101 },
        { 0x53, 16, 0b1111111110011110 },
        { 0x54, 16, 0b1111111110011111 },
        { 0x55, 16, 0b1111111110100000 },
        { 0x56, 16, 0b1111111110100001 },
        { 0x57, 16, 0b1111111110100010 },
        { 0x58, 16, 0b1111111110100011 },
        { 0x59, 16, 0b1111111110100100 },
        { 0x5A, 16, 0b1111111110100101 },
        { 0x63, 16, 0b1111111110100110 },
        { 0x64, 16, 0b1111111110100111 },
        { 0x65, 16, 0b1111111110101000 },
        { 0x66, 16, 0b1111111110101001 },
        { 0x67, 16, 0b1111111110101010 },
        { 0x68, 16, 0b1111111110101011 },
        { 0x69, 16, 0b1111111110101100 },
        { 0x6A, 16, 0b1111111110101101 },
        { 0x73, 16, 0b1111111110101110 },
        { 0x74, 16, 0b1111111110101111 },
        { 0x75, 16, 0b1111111110110000 },
        { 0x76, 16, 0b1111111110110001 },
        { 0x77, 16, 0b1111111110110010 },
        { 0x78, 16, 0b1111111110110011 },
        { 0x79, 16, 0b1111111110110100 },
        { 0x7A, 16, 0b1111111110110101 },
        { 0x83, 16, 0b1111111110110110 },
        { 0x84, 16, 0b1111111110110111 },
        { 0x85, 16, 0b1111111110111000 },
        { 0x86, 16, 0b1111111110111001 },
        { 0x87, 16, 0b1111111110111010 },
        { 0x88, 16, 0b1111111110111011 },
        { 0x89, 16, 0b1111111110111100 },
        { 0x8A, 16, 0b1111111110111101 },
        { 0x92, 16, 0b1111111110111110 },
        { 0x93, 16, 0b1111111110111111 },
        { 0x94, 16, 0b1111111111000000 },
        { 0x95, 16, 0b1111111111000001 },
        { 0x96, 16, 0b1111111111000010 },
        { 0x97, 16, 0b1111111111000011 },
        { 0x98, 16, 0b1111111111000100 },
        { 0x99, 16, 0b1111111111000101 },
        { 0x9A, 16, 0b1111111111000110 },
        { 0xA2, 16, 0b1111111111000111 },
        { 0xA3, 16, 0b1111111111001000 },
        { 0xA4, 16, 0b1111111111001001 },
        { 0xA5, 16, 0b1111111111001010 },
        { 0xA6, 16, 0b1111111111001011 },
        { 0xA7, 16, 0b1111111111001100 },
        { 0xA8, 16, 0b1111111111001101 },
        { 0xA9, 16, 0b1111111111001110 },
        { 0xAA, 16, 0b1111111111001111 },
        { 0xB2, 16, 0b1111111111010000 },
        { 0xB3, 16, 0b1111111111010001 },
        { 0xB4, 16, 0b1111111111010010 },
        { 0xB5, 16, 0b1111111111010011 },
        { 0xB6, 16, 0b1111111111010100 },
        { 0xB7, 16, 0b1111111111010101 },
        { 0xB8, 16, 0b1111111111010110 },
        { 0xB9, 16, 0b1111111111010111 },
        { 0xBA, 16, 0b1111111111011000 },
        { 0xC2, 16, 0b1111111111011001 },
        { 0xC3, 16, 0b1111111111011010 },
        { 0xC4, 16, 0b1111111111011011 },
        { 0xC5, 16, 0b1111111111011100 },
        { 0xC6, 16, 0b1111111111011101 },
        { 0xC7, 16, 0b1111111111011110 },
        { 0xC8, 16, 0b1111111111011111 },
        { 0xC9, 16, 0b1111111111100000 },
        { 0xCA, 16, 0b1111111111100001 },
        { 0xD2, 16, 0b1111111111100010 },
        { 0xD3, 16, 0b1111111111100011 },
        { 0xD4, 16, 0b1111111111100100 },
        { 0xD5, 16, 0b1111111111100101 },
        { 0xD6, 16, 0b1111111111100110 },
        { 0xD7, 16, 0b1111111111100111 },
        { 0xD8, 16, 0b1111111111101000 },
        { 0xD9, 16, 0b1111111111101001 },
        { 0xDA, 16, 0b1111111111101010 },
        { 0xE1, 16, 0b1111111111101011 },
        { 0xE2, 16, 0b1111111111101100 },
        { 0xE3, 16, 0b1111111111101101 },
        { 0xE4, 16, 0b1111111111101110 },
        { 0xE5, 16, 0b1111111111101111 },
        { 0xE6, 16, 0b1111111111110000 },
        { 0xE7, 16, 0b1111111111110001 },
        { 0xE8, 16, 0b1111111111110010 },
        { 0xE9, 16, 0b1111111111110011 },
        { 0xEA, 16, 0b1111111111110100 },
        { 0xF1, 16, 0b1111111111110101 },
        { 0xF2, 16, 0b1111111111110110 },
        { 0xF3, 16, 0b1111111111110111 },
        { 0xF4, 16, 0b1111111111111000 },
        { 0xF5, 16, 0b1111111111111001 },
        { 0xF6, 16, 0b1111111111111010 },
        { 0xF7, 16, 0b1111111111111011 },
        { 0xF8, 16, 0b1111111111111100 },
        { 0xF9, 16, 0b1111111111111101 },
        { 0xFA, 16, 0b1111111111111110 },
    },
    .id = (1 << 4) | 0,
};

static OutputHuffmanTable s_default_ac_chrominance_huffman_table {
    .table = {
        { 0x00, 2, 0b00 },
        { 0x01, 2, 0b01 },
        { 0x02, 3, 0b100 },
        { 0x03, 4, 0b1010 },
        { 0x11, 4, 0b1011 },
        { 0x04, 5, 0b11000 },
        { 0x05, 5, 0b11001 },
        { 0x21, 5, 0b11010 },
        { 0x31, 5, 0b11011 },
        { 0x06, 6, 0b111000 },
        { 0x12, 6, 0b111001 },
        { 0x41, 6, 0b111010 },
        { 0x51, 6, 0b111011 },
        { 0x07, 7, 0b1111000 },
        { 0x61, 7, 0b1111001 },
        { 0x71, 7, 0b1111010 },
        { 0x13, 8, 0b11110110 },
        { 0x22, 8, 0b11110111 },
        { 0x32, 8, 0b11111000 },
        { 0x81, 8, 0b11111001 },
        { 0x08, 9, 0b111110100 },
        { 0x14, 9, 0b111110101 },
        { 0x42, 9, 0b111110110 },
        { 0x91, 9, 0b111110111 },
        { 0xA1, 9, 0b111111000 },
        { 0xB1, 9, 0b111111001 },
        { 0xC1, 9, 0b111111010 },
        { 0x09, 10, 0b1111110110 },
        { 0x23, 10, 0b1111110111 },
        { 0x33, 10, 0b1111111000 },
        { 0x52, 10, 0b1111111001 },
        { 0xF0, 10, 0b1111111010 },
        { 0x15, 11, 0b11111110110 },
        { 0x62, 11, 0b11111110111 },
        { 0x72, 11, 0b11111111000 },
        { 0xD1, 11, 0b11111111001 },
        { 0x0A, 12, 0b111111110100 },
        { 0x16, 12, 0b111111110101 },
        { 0x24, 12, 0b111111110110 },
        { 0x34, 12, 0b111111110111 },
        { 0xE1, 14, 0b11111111100000 },
        { 0x25, 15, 0b111111111000010 },
        { 0xF1, 15, 0b111111111000011 },
        { 0x17, 16, 0b1111111110001000 },
        { 0x18, 16, 0b1111111110001001 },
        { 0x19, 16, 0b1111111110001010 },
        { 0x1A, 16, 0b1111111110001011 },
        { 0x26, 16, 0b1111111110001100 },
        { 0x27, 16, 0b1111111110001101 },
        { 0x28, 16, 0b1111111110001110 },
        { 0x29, 16, 0b1111111110001111 },
        { 0x2A, 16, 0b1111111110010000 },
        { 0x35, 16, 0b1111111110010001 },
        { 0x36, 16, 0b1111111110010010 },
        { 0x37, 16, 0b1111111110010011 },
        { 0x38, 16, 0b1111111110010100 },
        { 0x39, 16, 0b1111111110010101 },
        { 0x3A, 16, 0b1111111110010110 },
        { 0x43, 16, 0b1111111110010111 },
        { 0x44, 16, 0b1111111110011000 },
        { 0x45, 16, 0b1111111110011001 },
        { 0x46, 16, 0b1111111110011010 },
        { 0x47, 16, 0b1111111110011011 },
        { 0x48, 16, 0b1111111110011100 },
        { 0x49, 16, 0b1111111110011101 },
        { 0x4A, 16, 0b1111111110011110 },
        { 0x53, 16, 0b1111111110011111 },
        { 0x54, 16, 0b1111111110100000 },
        { 0x55, 16, 0b1111111110100001 },
        { 0x56, 16, 0b1111111110100010 },
        { 0x57, 16, 0b1111111110100011 },
        { 0x58, 16, 0b1111111110100100 },
        { 0x59, 16, 0b1111111110100101 },
        { 0x5A, 16, 0b1111111110100110 },
        { 0x63, 16, 0b1111111110100111 },
        { 0x64, 16, 0b1111111110101000 },
        { 0x65, 16, 0b1111111110101001 },
        { 0x66, 16, 0b1111111110101010 },
        { 0x67, 16, 0b1111111110101011 },
        { 0x68, 16, 0b1111111110101100 },
        { 0x69, 16, 0b1111111110101101 },
        { 0x6A, 16, 0b1111111110101110 },
        { 0x73, 16, 0b1111111110101111 },
        { 0x74, 16, 0b1111111110110000 },
        { 0x75, 16, 0b1111111110110001 },
        { 0x76, 16, 0b1111111110110010 },
        { 0x77, 16, 0b1111111110110011 },
        { 0x78, 16, 0b1111111110110100 },
        { 0x79, 16, 0b1111111110110101 },
        { 0x7A, 16, 0b1111111110110110 },
        { 0x82, 16, 0b1111111110110111 },
        { 0x83, 16, 0b1111111110111000 },
        { 0x84, 16, 0b1111111110111001 },
        { 0x85, 16, 0b1111111110111010 },
        { 0x86, 16, 0b1111111110111011 },
        { 0x87, 16, 0b1111111110111100 },
        { 0x88, 16, 0b1111111110111101 },
        { 0x89, 16, 0b1111111110111110 },
        { 0x8A, 16, 0b1111111110111111 },
        { 0x92, 16, 0b1111111111000000 },
        { 0x93, 16, 0b1111111111000001 },
        { 0x94, 16, 0b1111111111000010 },
        { 0x95, 16, 0b1111111111000011 },
        { 0x96, 16, 0b1111111111000100 },
        { 0x97, 16, 0b1111111111000101 },
        { 0x98, 16, 0b1111111111000110 },
        { 0x99, 16, 0b1111111111000111 },
        { 0x9A, 16, 0b1111111111001000 },
        { 0xA2, 16, 0b1111111111001001 },
        { 0xA3, 16, 0b1111111111001010 },
        { 0xA4, 16, 0b1111111111001011 },
        { 0xA5, 16, 0b1111111111001100 },
        { 0xA6, 16, 0b1111111111001101 },
        { 0xA7, 16, 0b1111111111001110 },
        { 0xA8, 16, 0b1111111111001111 },
        { 0xA9, 16, 0b1111111111010000 },
        { 0xAA, 16, 0b1111111111010001 },
        { 0xB2, 16, 0b1111111111010010 },
        { 0xB3, 16, 0b1111111111010011 },
        { 0xB4, 16, 0b1111111111010100 },
        { 0xB5, 16, 0b1111111111010101 },
        { 0xB6, 16, 0b1111111111010110 },
        { 0xB7, 16, 0b1111111111010111 },
        { 0xB8, 16, 0b1111111111011000 },
        { 0xB9, 16, 0b1111111111011001 },
        { 0xBA, 16, 0b1111111111011010 },
        { 0xC2, 16, 0b1111111111011011 },
        { 0xC3, 16, 0b1111111111011100 },
        { 0xC4, 16, 0b1111111111011101 },
        { 0xC5, 16, 0b1111111111011110 },
        { 0xC6, 16, 0b1111111111011111 },
        { 0xC7, 16, 0b1111111111100000 },
        { 0xC8, 16, 0b1111111111100001 },
        { 0xC9, 16, 0b1111111111100010 },
        { 0xCA, 16, 0b1111111111100011 },
        { 0xD2, 16, 0b1111111111100100 },
        { 0xD3, 16, 0b1111111111100101 },
        { 0xD4, 16, 0b1111111111100110 },
        { 0xD5, 16, 0b1111111111100111 },
        { 0xD6, 16, 0b1111111111101000 },
        { 0xD7, 16, 0b1111111111101001 },
        { 0xD8, 16, 0b1111111111101010 },
        { 0xD9, 16, 0b1111111111101011 },
        { 0xDA, 16, 0b1111111111101100 },
        { 0xE2, 16, 0b1111111111101101 },
        { 0xE3, 16, 0b1111111111101110 },
        { 0xE4, 16, 0b1111111111101111 },
        { 0xE5, 16, 0b1111111111110000 },
        { 0xE6, 16, 0b1111111111110001 },
        { 0xE7, 16, 0b1111111111110010 },
        { 0xE8, 16, 0b1111111111110011 },
        { 0xE9, 16, 0b1111111111110100 },
        { 0xEA, 16, 0b1111111111110101 },
        { 0xF2, 16, 0b1111111111110110 },
        { 0xF3, 16, 0b1111111111110111 },
        { 0xF4, 16, 0b1111111111111000 },
        { 0xF5, 16, 0b1111111111111001 },
        { 0xF6, 16, 0b1111111111111010 },
        { 0xF7, 16, 0b1111111111111011 },
        { 0xF8, 16, 0b1111111111111100 },
        { 0xF9, 16, 0b1111111111111101 },
        { 0xFA, 16, 0b1111111111111110 },
    },
    .id = (1 << 4) | 1,
};

}
