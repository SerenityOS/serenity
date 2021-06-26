/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Parser.h"
#include <AK/ByteBuffer.h>

namespace Video::VP9 {

class Decoder {
    friend class Parser;

public:
    Decoder();
    bool decode_frame(ByteBuffer const&);
    void dump_frame_info();

private:
    /* (8.5) Prediction Processes */
    bool predict_intra(size_t plane, u32 x, u32 y, bool have_left, bool have_above, bool not_on_right, TXSize tx_size, u32 block_index);
    bool predict_inter(size_t plane, u32 x, u32 y, u32 w, u32 h, u32 block_index);

    /* (8.6) Reconstruction and Dequantization */
    bool reconstruct(size_t plane, u32 x, u32 y, TXSize size);

    NonnullOwnPtr<Parser> m_parser;
};

}
