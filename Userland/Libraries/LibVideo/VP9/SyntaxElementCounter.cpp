/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SyntaxElementCounter.h"

namespace Video::VP9 {

void SyntaxElementCounter::clear_counts()
{
    __builtin_memset(m_counts_intra_mode, 0, BLOCK_SIZE_GROUPS * INTRA_MODES);
    __builtin_memset(m_counts_uv_mode, 0, INTRA_MODES * INTRA_MODES);
    __builtin_memset(m_counts_partition, 0, PARTITION_CONTEXTS * PARTITION_TYPES);
    __builtin_memset(m_counts_interp_filter, 0, INTERP_FILTER_CONTEXTS * SWITCHABLE_FILTERS);
    __builtin_memset(m_counts_inter_mode, 0, INTER_MODE_CONTEXTS * INTER_MODES);
    __builtin_memset(m_counts_tx_size, 0, TX_SIZES * TX_SIZE_CONTEXTS * TX_SIZES);
    __builtin_memset(m_counts_is_inter, 0, IS_INTER_CONTEXTS * 2);
    __builtin_memset(m_counts_comp_mode, 0, COMP_MODE_CONTEXTS * 2);
    __builtin_memset(m_counts_single_ref, 0, REF_CONTEXTS * 2 * 2);
    __builtin_memset(m_counts_comp_ref, 0, REF_CONTEXTS * 2);
    __builtin_memset(m_counts_skip, 0, SKIP_CONTEXTS * 2);
    __builtin_memset(m_counts_mv_joint, 0, MV_JOINTS);
    __builtin_memset(m_counts_mv_sign, 0, 2 * 2);
    __builtin_memset(m_counts_mv_class, 0, 2 * MV_CLASSES);
    __builtin_memset(m_counts_mv_class0_bit, 0, 2 * CLASS0_SIZE);
    __builtin_memset(m_counts_mv_class0_fr, 0, 2 * CLASS0_SIZE * MV_FR_SIZE);
    __builtin_memset(m_counts_mv_class0_hp, 0, 2 * 2);
    __builtin_memset(m_counts_mv_bits, 0, 2 * MV_OFFSET_BITS * 2);
    __builtin_memset(m_counts_mv_fr, 0, 2 * MV_FR_SIZE);
    __builtin_memset(m_counts_mv_hp, 0, 2 * 2);
    __builtin_memset(m_counts_token, 0, TX_SIZES * BLOCK_TYPES * REF_TYPES * COEF_BANDS * PREV_COEF_CONTEXTS * UNCONSTRAINED_NODES);
    __builtin_memset(m_counts_more_coefs, 0, TX_SIZES * BLOCK_TYPES * REF_TYPES * COEF_BANDS * PREV_COEF_CONTEXTS * 2);
}

}
