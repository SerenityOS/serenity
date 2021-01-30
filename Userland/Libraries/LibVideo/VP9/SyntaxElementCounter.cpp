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

#include "SyntaxElementCounter.h"

namespace Video::VP9 {

void SyntaxElementCounter::clear_counts()
{
    __builtin_memset(counts_intra_mode, 0, BLOCK_SIZE_GROUPS * INTRA_MODES);
    __builtin_memset(counts_uv_mode, 0, INTRA_MODES * INTRA_MODES);
    __builtin_memset(counts_partition, 0, PARTITION_CONTEXTS * PARTITION_TYPES);
    __builtin_memset(counts_interp_filter, 0, INTERP_FILTER_CONTEXTS * SWITCHABLE_FILTERS);
    __builtin_memset(counts_inter_mode, 0, INTER_MODE_CONTEXTS * INTER_MODES);
    __builtin_memset(counts_tx_size, 0, TX_SIZES * TX_SIZE_CONTEXTS * TX_SIZES);
    __builtin_memset(counts_is_inter, 0, IS_INTER_CONTEXTS * 2);
    __builtin_memset(counts_comp_mode, 0, COMP_MODE_CONTEXTS * 2);
    __builtin_memset(counts_single_ref, 0, REF_CONTEXTS * 2 * 2);
    __builtin_memset(counts_comp_ref, 0, REF_CONTEXTS * 2);
    __builtin_memset(counts_skip, 0, SKIP_CONTEXTS * 2);
    __builtin_memset(counts_mv_joint, 0, MV_JOINTS);
    __builtin_memset(counts_mv_sign, 0, 2 * 2);
    __builtin_memset(counts_mv_class, 0, 2 * MV_CLASSES);
    __builtin_memset(counts_mv_class0_bit, 0, 2 * CLASS0_SIZE);
    __builtin_memset(counts_mv_class0_fr, 0, 2 * CLASS0_SIZE * MV_FR_SIZE);
    __builtin_memset(counts_mv_class0_hp, 0, 2 * 2);
    __builtin_memset(counts_mv_bits, 0, 2 * MV_OFFSET_BITS * 2);
    __builtin_memset(counts_mv_fr, 0, 2 * MV_FR_SIZE);
    __builtin_memset(counts_mv_hp, 0, 2 * 2);
    __builtin_memset(counts_token, 0, TX_SIZES * BLOCK_TYPES * REF_TYPES * COEF_BANDS * PREV_COEF_CONTEXTS * UNCONSTRAINED_NODES);
    __builtin_memset(counts_more_coefs, 0, TX_SIZES * BLOCK_TYPES * REF_TYPES * COEF_BANDS * PREV_COEF_CONTEXTS * 2);
}

}
