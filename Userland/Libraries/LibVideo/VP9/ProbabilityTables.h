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
#include <AK/Vector.h>

namespace Video::VP9 {

typedef u8 pareto_table_t[128][8];
typedef u8 kf_partition_probs_t[PARTITION_CONTEXTS][PARTITION_TYPES - 1];
typedef u8 kf_y_mode_probs_t[INTRA_MODES][INTRA_MODES][INTRA_MODES - 1];
typedef u8 kf_uv_mode_prob_t[INTRA_MODES][INTRA_MODES - 1];
typedef u8 partition_probs_t[PARTITION_CONTEXTS][PARTITION_TYPES - 1];
typedef u8 y_mode_probs_t[BLOCK_SIZE_GROUPS][INTRA_MODES - 1];
typedef u8 uv_mode_probs_t[INTRA_MODES][INTRA_MODES - 1];
typedef u8 skip_prob_t[SKIP_CONTEXTS];
typedef u8 is_inter_prob_t[IS_INTER_CONTEXTS];
typedef u8 comp_mode_prob_t[COMP_MODE_CONTEXTS];
typedef u8 comp_ref_prob_t[REF_CONTEXTS];
typedef u8 single_ref_prob_t[REF_CONTEXTS][2];
typedef u8 mv_sign_prob_t[2];
typedef u8 mv_bits_prob_t[2][MV_OFFSET_BITS];
typedef u8 mv_class0_bit_prob_t[2];
typedef u8 tx_probs_t[TX_SIZES][TX_SIZE_CONTEXTS][TX_SIZES - 1];
typedef u8 inter_mode_probs_t[INTER_MODE_CONTEXTS][INTER_MODES - 1];
typedef u8 interp_filter_probs_t[INTERP_FILTER_CONTEXTS][SWITCHABLE_FILTERS - 1];
typedef u8 mv_joint_probs_t[3];
typedef u8 mv_class_probs_t[2][MV_CLASSES - 1];
typedef u8 mv_class0_fr_probs_t[2][CLASS0_SIZE][3];
typedef u8 mv_class0_hp_prob_t[2];
typedef u8 mv_fr_probs_t[2][3];
typedef u8 mv_hp_prob_t[2];
typedef u8 coef_probs_t[TX_SIZES][BLOCK_TYPES][REF_TYPES][COEF_BANDS][PREV_COEF_CONTEXTS][UNCONSTRAINED_NODES];

class ProbabilityTables final {
public:
    void save_probs(size_t index);
    void reset_probs();
    void load_probs(size_t index);
    void load_probs2(size_t index);

    const pareto_table_t& pareto_table() const;
    const kf_partition_probs_t& kf_partition_probs() const;
    const kf_y_mode_probs_t& kf_y_mode_probs() const;
    const kf_uv_mode_prob_t& kf_uv_mode_prob() const;

    partition_probs_t& partition_probs() { return m_current_probability_table.partition_probs; };
    y_mode_probs_t& y_mode_probs() { return m_current_probability_table.y_mode_probs; };
    uv_mode_probs_t& uv_mode_probs() { return m_current_probability_table.uv_mode_probs; };
    skip_prob_t& skip_prob() { return m_current_probability_table.skip_prob; };
    is_inter_prob_t& is_inter_prob() { return m_current_probability_table.is_inter_prob; };
    comp_mode_prob_t& comp_mode_prob() { return m_current_probability_table.comp_mode_prob; };
    comp_ref_prob_t& comp_ref_prob() { return m_current_probability_table.comp_ref_prob; };
    single_ref_prob_t& single_ref_prob() { return m_current_probability_table.single_ref_prob; };
    mv_sign_prob_t& mv_sign_prob() { return m_current_probability_table.mv_sign_prob; };
    mv_bits_prob_t& mv_bits_prob() { return m_current_probability_table.mv_bits_prob; };
    mv_class0_bit_prob_t& mv_class0_bit_prob() { return m_current_probability_table.mv_class0_bit_prob; };
    tx_probs_t& tx_probs() { return m_current_probability_table.tx_probs; };
    inter_mode_probs_t& inter_mode_probs() { return m_current_probability_table.inter_mode_probs; };
    interp_filter_probs_t& interp_filter_probs() { return m_current_probability_table.interp_filter_probs; };
    mv_joint_probs_t& mv_joint_probs() { return m_current_probability_table.mv_joint_probs; };
    mv_class_probs_t& mv_class_probs() { return m_current_probability_table.mv_class_probs; };
    mv_class0_fr_probs_t& mv_class0_fr_probs() { return m_current_probability_table.mv_class0_fr_probs; };
    mv_class0_hp_prob_t& mv_class0_hp_prob() { return m_current_probability_table.mv_class0_hp_prob; };
    mv_fr_probs_t& mv_fr_probs() { return m_current_probability_table.mv_fr_probs; };
    mv_hp_prob_t& mv_hp_prob() { return m_current_probability_table.mv_hp_prob; };
    coef_probs_t& coef_probs() { return m_current_probability_table.coef_probs; };

private:
    struct ProbabilityTable {
        partition_probs_t partition_probs;
        y_mode_probs_t y_mode_probs;
        uv_mode_probs_t uv_mode_probs;
        skip_prob_t skip_prob;
        is_inter_prob_t is_inter_prob;
        comp_mode_prob_t comp_mode_prob;
        comp_ref_prob_t comp_ref_prob;
        single_ref_prob_t single_ref_prob;
        mv_sign_prob_t mv_sign_prob;
        mv_bits_prob_t mv_bits_prob;
        mv_class0_bit_prob_t mv_class0_bit_prob;
        tx_probs_t tx_probs;
        inter_mode_probs_t inter_mode_probs;
        interp_filter_probs_t interp_filter_probs;
        mv_joint_probs_t mv_joint_probs;
        mv_class_probs_t mv_class_probs;
        mv_class0_fr_probs_t mv_class0_fr_probs;
        mv_class0_hp_prob_t mv_class0_hp_prob;
        mv_fr_probs_t mv_fr_probs;
        mv_hp_prob_t mv_hp_prob;
        coef_probs_t coef_probs;
    };

    Vector<ProbabilityTable, 4> m_saved_probability_tables;
    ProbabilityTable m_current_probability_table;
};

}
