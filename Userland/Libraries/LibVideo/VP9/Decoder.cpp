/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Decoder.h"

namespace Video::VP9 {

Decoder::Decoder()
    : m_parser(make<Parser>(*this))
{
}

bool Decoder::decode_frame(ByteBuffer const& frame_data)
{
    return m_parser->parse_frame(frame_data);
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

}
