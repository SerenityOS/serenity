/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Decoder.h"
#include "Utilities.h"

namespace Video::VP9 {

Decoder::Decoder()
    : m_parser(make<Parser>(*this))
{
}

bool Decoder::decode_frame(ByteBuffer const& frame_data)
{
    SAFE_CALL(m_parser->parse_frame(frame_data));
    // TODO:
    //  - #2
    //  - #3
    //  - #4
    SAFE_CALL(update_reference_frames());
    return true;
}

void Decoder::dump_frame_info()
{
    m_parser->dump_info();
}

bool Decoder::predict_intra(size_t, u32, u32, bool, bool, bool, TXSize, u32)
{
    // TODO: Implement
    return true;
}

bool Decoder::predict_inter(size_t, u32, u32, u32, u32, u32)
{
    // TODO: Implement
    return true;
}

bool Decoder::reconstruct(size_t, u32, u32, TXSize)
{
    // TODO: Implement
    return true;
}

bool Decoder::update_reference_frames()
{
    for (auto i = 0; i < NUM_REF_FRAMES; i++) {
        dbgln("updating frame {}? {}", i, (m_parser->m_refresh_frame_flags & (1 << i)) == 1);
        if ((m_parser->m_refresh_frame_flags & (1 << i)) != 1)
            continue;
        m_parser->m_ref_frame_width[i] = m_parser->m_frame_width;
        m_parser->m_ref_frame_height[i] = m_parser->m_frame_height;
        // TODO: 1.3-1.7
    }
    // TODO: 2.1-2.2
    return true;
}

}
