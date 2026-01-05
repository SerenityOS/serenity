/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Boxes.h"

namespace Gfx::ISOBMFF {

// 18181-2 Information technology — JPEG XL image coding system
// Part 2: File format

// 9.1  JPEG XL Signature box (JXL␣)
struct JPEGXLSignatureBox final : public Box {
    BOX_SUBTYPE(JPEGXLSignatureBox);

    u32 signature { 0 };
};

// 9.3  Level box (jxll)
struct JPEGXLLevelBox final : public Box {
    BOX_SUBTYPE(JPEGXLLevelBox);

    u8 level { 0 };
};

// 9.9  JPEG XL Codestream box (jxlc)
struct JPEGXLCodestreamBox final : public Box {
    BOX_SUBTYPE(JPEGXLCodestreamBox);

    Vector<u8> codestream;
};

// 9.10 - JPEG XL Partial Codestream box (jxlp)
struct JPEGXLPartialCodestreamBox final : public Box {
    BOX_SUBTYPE(JPEGXLPartialCodestreamBox);

    u32 index() const { return part_index & 0x7FFF'FFFF; }
    bool is_last() const { return (part_index & 0x8000'0000) != 0; }

    u32 part_index { 0 };
    Vector<u8> codestream;
};

}
