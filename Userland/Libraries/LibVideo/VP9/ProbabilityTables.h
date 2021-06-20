/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Symbols.h"
#include <AK/Types.h>
#include <AK/Vector.h>

namespace Video::VP9 {

typedef u8 ParetoTable[128][8];
typedef u8 KfPartitionProbs[PARTITION_CONTEXTS][PARTITION_TYPES - 1];
typedef u8 KfYModeProbs[INTRA_MODES][INTRA_MODES][INTRA_MODES - 1];
typedef u8 KfUVModeProbs[INTRA_MODES][INTRA_MODES - 1];
typedef u8 PartitionProbs[PARTITION_CONTEXTS][PARTITION_TYPES - 1];
typedef u8 YModeProbs[BLOCK_SIZE_GROUPS][INTRA_MODES - 1];
typedef u8 UVModeProbs[INTRA_MODES][INTRA_MODES - 1];
typedef u8 SkipProb[SKIP_CONTEXTS];
typedef u8 IsInterProb[IS_INTER_CONTEXTS];
typedef u8 CompModeProb[COMP_MODE_CONTEXTS];
typedef u8 CompRefProb[REF_CONTEXTS];
typedef u8 SingleRefProb[REF_CONTEXTS][2];
typedef u8 MvSignProb[2];
typedef u8 MvBitsProb[2][MV_OFFSET_BITS];
typedef u8 MvClass0BitProb[2];
typedef u8 TxProbs[TX_SIZES][TX_SIZE_CONTEXTS][TX_SIZES - 1];
typedef u8 InterModeProbs[INTER_MODE_CONTEXTS][INTER_MODES - 1];
typedef u8 InterpFilterProbs[INTERP_FILTER_CONTEXTS][SWITCHABLE_FILTERS - 1];
typedef u8 MvJointProbs[3];
typedef u8 MvClassProbs[2][MV_CLASSES - 1];
typedef u8 MvClass0FrProbs[2][CLASS0_SIZE][3];
typedef u8 MvClass0HpProbs[2];
typedef u8 MvFrProbs[2][3];
typedef u8 MvHpProb[2];
typedef u8 CoefProbs[TX_SIZES][BLOCK_TYPES][REF_TYPES][COEF_BANDS][PREV_COEF_CONTEXTS][UNCONSTRAINED_NODES];

class ProbabilityTables final {
public:
    void save_probs(size_t index);
    void reset_probs();
    void load_probs(size_t index);
    void load_probs2(size_t index);

    ParetoTable const& pareto_table() const;
    KfPartitionProbs const& kf_partition_probs() const;
    KfYModeProbs const& kf_y_mode_probs() const;
    KfUVModeProbs const& kf_uv_mode_prob() const;

    PartitionProbs& partition_probs() { return m_current_probability_table.partition_probs; };
    YModeProbs& y_mode_probs() { return m_current_probability_table.y_mode_probs; };
    UVModeProbs& uv_mode_probs() { return m_current_probability_table.uv_mode_probs; };
    SkipProb& skip_prob() { return m_current_probability_table.skip_prob; };
    IsInterProb& is_inter_prob() { return m_current_probability_table.is_inter_prob; };
    CompModeProb& comp_mode_prob() { return m_current_probability_table.comp_mode_prob; };
    CompRefProb& comp_ref_prob() { return m_current_probability_table.comp_ref_prob; };
    SingleRefProb& single_ref_prob() { return m_current_probability_table.single_ref_prob; };
    MvSignProb& mv_sign_prob() { return m_current_probability_table.mv_sign_prob; };
    MvBitsProb& mv_bits_prob() { return m_current_probability_table.mv_bits_prob; };
    MvClass0BitProb& mv_class0_bit_prob() { return m_current_probability_table.mv_class0_bit_prob; };
    TxProbs& tx_probs() { return m_current_probability_table.tx_probs; };
    InterModeProbs& inter_mode_probs() { return m_current_probability_table.inter_mode_probs; };
    InterpFilterProbs& interp_filter_probs() { return m_current_probability_table.interp_filter_probs; };
    MvJointProbs& mv_joint_probs() { return m_current_probability_table.mv_joint_probs; };
    MvClassProbs& mv_class_probs() { return m_current_probability_table.mv_class_probs; };
    MvClass0FrProbs& mv_class0_fr_probs() { return m_current_probability_table.mv_class0_fr_probs; };
    MvClass0HpProbs& mv_class0_hp_prob() { return m_current_probability_table.mv_class0_hp_prob; };
    MvFrProbs& mv_fr_probs() { return m_current_probability_table.mv_fr_probs; };
    MvHpProb& mv_hp_prob() { return m_current_probability_table.mv_hp_prob; };
    CoefProbs& coef_probs() { return m_current_probability_table.coef_probs; };

private:
    struct ProbabilityTable {
        PartitionProbs partition_probs;
        YModeProbs y_mode_probs;
        UVModeProbs uv_mode_probs;
        SkipProb skip_prob;
        IsInterProb is_inter_prob;
        CompModeProb comp_mode_prob;
        CompRefProb comp_ref_prob;
        SingleRefProb single_ref_prob;
        MvSignProb mv_sign_prob;
        MvBitsProb mv_bits_prob;
        MvClass0BitProb mv_class0_bit_prob;
        TxProbs tx_probs;
        InterModeProbs inter_mode_probs;
        InterpFilterProbs interp_filter_probs;
        MvJointProbs mv_joint_probs;
        MvClassProbs mv_class_probs;
        MvClass0FrProbs mv_class0_fr_probs;
        MvClass0HpProbs mv_class0_hp_prob;
        MvFrProbs mv_fr_probs;
        MvHpProb mv_hp_prob;
        CoefProbs coef_probs;
    };

    Vector<ProbabilityTable, 4> m_saved_probability_tables;
    ProbabilityTable m_current_probability_table;
};

}
