/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "Constants.h"
#include <AK/Types.h>

namespace Video::VP9 {

enum class SyntaxElementType {
    Partition,
    DefaultIntraMode,
    DefaultUVMode,
    IntraMode,
    SubIntraMode,
    UVMode,
    SegmentID,
    Skip,
    SegIDPredicted,
    IsInter,
    CompMode,
    CompRef,
    SingleRefP1,
    SingleRefP2,
    MVSign,
    MVClass0Bit,
    MVBit,
    TXSize,
    InterMode,
    InterpFilter,
    MVJoint,
    MVClass,
    MVClass0FR,
    MVClass0HP,
    MVFR,
    MVHP,
    Token,
    MoreCoefs,
};

class SyntaxElementCounter final {
public:
    void clear_counts();

    u8 m_counts_intra_mode[BLOCK_SIZE_GROUPS][INTRA_MODES];
    u8 m_counts_uv_mode[INTRA_MODES][INTRA_MODES];
    u8 m_counts_partition[PARTITION_CONTEXTS][PARTITION_TYPES];
    u8 m_counts_interp_filter[INTERP_FILTER_CONTEXTS][SWITCHABLE_FILTERS];
    u8 m_counts_inter_mode[INTER_MODE_CONTEXTS][INTER_MODES];
    u8 m_counts_tx_size[TX_SIZES][TX_SIZE_CONTEXTS][TX_SIZES];
    u8 m_counts_is_inter[IS_INTER_CONTEXTS][2];
    u8 m_counts_comp_mode[COMP_MODE_CONTEXTS][2];
    u8 m_counts_single_ref[REF_CONTEXTS][2][2];
    u8 m_counts_comp_ref[REF_CONTEXTS][2];
    u8 m_counts_skip[SKIP_CONTEXTS][2];
    u8 m_counts_mv_joint[MV_JOINTS];
    u8 m_counts_mv_sign[2][2];
    u8 m_counts_mv_class[2][MV_CLASSES];
    u8 m_counts_mv_class0_bit[2][CLASS0_SIZE];
    u8 m_counts_mv_class0_fr[2][CLASS0_SIZE][MV_FR_SIZE];
    u8 m_counts_mv_class0_hp[2][2];
    u8 m_counts_mv_bits[2][MV_OFFSET_BITS][2];
    u8 m_counts_mv_fr[2][MV_FR_SIZE];
    u8 m_counts_mv_hp[2][2];
    u8 m_counts_token[TX_SIZES][BLOCK_TYPES][REF_TYPES][COEF_BANDS][PREV_COEF_CONTEXTS][UNCONSTRAINED_NODES];
    u8 m_counts_more_coefs[TX_SIZES][BLOCK_TYPES][REF_TYPES][COEF_BANDS][PREV_COEF_CONTEXTS][2];
};

}
