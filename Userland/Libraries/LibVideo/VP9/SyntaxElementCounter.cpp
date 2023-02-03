/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SyntaxElementCounter.h"

namespace Video::VP9 {

void SyntaxElementCounter::clear_counts()
{
    __builtin_memset(m_counts_intra_mode, 0, sizeof(m_counts_intra_mode));
    __builtin_memset(m_counts_uv_mode, 0, sizeof(m_counts_uv_mode));
    __builtin_memset(m_counts_partition, 0, sizeof(m_counts_partition));
    __builtin_memset(m_counts_interp_filter, 0, sizeof(m_counts_interp_filter));
    __builtin_memset(m_counts_inter_mode, 0, sizeof(m_counts_inter_mode));
    __builtin_memset(m_counts_tx_size, 0, sizeof(m_counts_tx_size));
    __builtin_memset(m_counts_is_inter, 0, sizeof(m_counts_is_inter));
    __builtin_memset(m_counts_comp_mode, 0, sizeof(m_counts_comp_mode));
    __builtin_memset(m_counts_single_ref, 0, sizeof(m_counts_single_ref));
    __builtin_memset(m_counts_comp_ref, 0, sizeof(m_counts_comp_ref));
    __builtin_memset(m_counts_skip, 0, sizeof(m_counts_skip));
    __builtin_memset(m_counts_mv_joint, 0, sizeof(m_counts_mv_joint));
    __builtin_memset(m_counts_mv_sign, 0, sizeof(m_counts_mv_sign));
    __builtin_memset(m_counts_mv_class, 0, sizeof(m_counts_mv_class));
    __builtin_memset(m_counts_mv_class0_bit, 0, sizeof(m_counts_mv_class0_bit));
    __builtin_memset(m_counts_mv_class0_fr, 0, sizeof(m_counts_mv_class0_fr));
    __builtin_memset(m_counts_mv_class0_hp, 0, sizeof(m_counts_mv_class0_hp));
    __builtin_memset(m_counts_mv_bits, 0, sizeof(m_counts_mv_bits));
    __builtin_memset(m_counts_mv_fr, 0, sizeof(m_counts_mv_fr));
    __builtin_memset(m_counts_mv_hp, 0, sizeof(m_counts_mv_hp));
    __builtin_memset(m_counts_token, 0, sizeof(m_counts_token));
    __builtin_memset(m_counts_more_coefs, 0, sizeof(m_counts_more_coefs));
}

}
