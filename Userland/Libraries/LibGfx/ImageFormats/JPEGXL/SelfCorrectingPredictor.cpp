/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SelfCorrectingPredictor.h"
#include <AK/BitStream.h>
#include <AK/Math.h>

namespace Gfx::JPEGXL {

// H.5.1 - General
ErrorOr<WPHeader> read_self_correcting_predictor(LittleEndianInputBitStream& stream)
{
    WPHeader self_correcting_predictor {};

    bool const default_wp = TRY(stream.read_bit());

    if (!default_wp) {
        self_correcting_predictor.wp_p1 = TRY(stream.read_bits(5));
        self_correcting_predictor.wp_p2 = TRY(stream.read_bits(5));
        self_correcting_predictor.wp_p3a = TRY(stream.read_bits(5));
        self_correcting_predictor.wp_p3b = TRY(stream.read_bits(5));
        self_correcting_predictor.wp_p3c = TRY(stream.read_bits(5));
        self_correcting_predictor.wp_p3d = TRY(stream.read_bits(5));
        self_correcting_predictor.wp_p3e = TRY(stream.read_bits(5));
        self_correcting_predictor.wp_w = {
            TRY(stream.read_bits<u8>(4)),
            TRY(stream.read_bits<u8>(4)),
            TRY(stream.read_bits<u8>(4)),
            TRY(stream.read_bits<u8>(4)),
        };
    }

    return self_correcting_predictor;
}

// H.5.1 - General
void SelfCorrectingData::compute_errors(u32 x, i32 true_value)
{
    auto& current_predictions = m_next_row[x];

    current_predictions.true_err = current_predictions.prediction - (true_value << 3);

    for (u8 i = 0; i < 4; ++i)
        current_predictions.err[i] = (abs(current_predictions.subpred[i] - (true_value << 3)) + 3) >> 3;
}

// H.5.2 - Prediction
SelfCorrectingData::Predictions SelfCorrectingData::compute_predictions(Gfx::JPEGXL::Neighborhood const& neighborhood, u32 x)
{
    {
        auto& current_predictions = m_next_row[x];

        auto const N3 = neighborhood.N << 3;
        auto const NW3 = neighborhood.NW << 3;
        auto const NE3 = neighborhood.NE << 3;
        auto const W3 = neighborhood.W << 3;
        auto const NN3 = neighborhood.NN << 3;

        auto const predictions_W = predictions_for(x, Direction::West);
        auto const predictions_N = predictions_for(x, Direction::North);
        auto const predictions_NE = predictions_for(x, Direction::NorthEast);
        auto const predictions_NW = predictions_for(x, Direction::NorthWest);
        auto const predictions_WW = predictions_for(x, Direction::WestWest);

        current_predictions.subpred[0] = W3 + NE3 - N3;
        current_predictions.subpred[1] = N3 - (((predictions_W.true_err + predictions_N.true_err + predictions_NE.true_err) * wp_params.wp_p1) >> 5);
        current_predictions.subpred[2] = W3 - (((predictions_W.true_err + predictions_N.true_err + predictions_NW.true_err) * wp_params.wp_p2) >> 5);
        current_predictions.subpred[3] = N3 - ((predictions_NW.true_err * wp_params.wp_p3a + predictions_N.true_err * wp_params.wp_p3b + predictions_NE.true_err * wp_params.wp_p3c + (NN3 - N3) * wp_params.wp_p3d + (NW3 - W3) * wp_params.wp_p3e) >> 5);

        auto const error2weight = [](i32 err_sum, u8 maxweight) -> i32 {
            i32 shift = floor(log2(err_sum + 1)) - 5;
            if (shift < 0)
                shift = 0;
            return 4 + ((static_cast<u64>(maxweight) * ((1 << 24) / ((err_sum >> shift) + 1))) >> shift);
        };

        Array<i32, 4> weight {};
        for (u8 i = 0; i < weight.size(); ++i) {
            auto err_sum = predictions_N.err[i] + predictions_W.err[i] + predictions_NW.err[i] + predictions_WW.err[i] + predictions_NE.err[i];
            if (x == m_width - 1)
                err_sum += predictions_W.err[i];
            weight[i] = error2weight(err_sum, wp_params.wp_w[i]);
        }

        auto sum_weights = weight[0] + weight[1] + weight[2] + weight[3];
        i32 const log_weight = floor(log2(sum_weights)) + 1;
        for (u8 i = 0; i < 4; i++)
            weight[i] = weight[i] >> (log_weight - 5);
        sum_weights = weight[0] + weight[1] + weight[2] + weight[3];

        auto s = (sum_weights >> 1) - 1;
        for (u8 i = 0; i < 4; i++)
            s += current_predictions.subpred[i] * weight[i];

        current_predictions.prediction = static_cast<u64>(s) * ((1 << 24) / sum_weights) >> 24;
        // if true_err_N, true_err_W and true_err_NW don't have the same sign
        if (((predictions_N.true_err ^ predictions_W.true_err) | (predictions_N.true_err ^ predictions_NW.true_err)) <= 0) {
            current_predictions.prediction = clamp(current_predictions.prediction, min(W3, min(N3, NE3)), max(W3, max(N3, NE3)));
        }

        auto& max_error = current_predictions.max_error;
        max_error = predictions_W.true_err;
        if (abs(predictions_N.true_err) > abs(max_error))
            max_error = predictions_N.true_err;
        if (abs(predictions_NW.true_err) > abs(max_error))
            max_error = predictions_NW.true_err;
        if (abs(predictions_NE.true_err) > abs(max_error))
            max_error = predictions_NE.true_err;

        return current_predictions;
    }
}

SelfCorrectingData::Predictions SelfCorrectingData::predictions_for(u32 x, Gfx::JPEGXL::SelfCorrectingData::Direction direction) const
{
    {
        // H.5.2 - Prediction
        auto const north = [&]() {
            return m_current_row_index < 1 ? Predictions {} : m_current_row[x];
        };

        switch (direction) {
        case Direction::North:
            return north();
        case Direction::NorthWest:
            return x < 1 ? north() : m_current_row[x - 1];
        case Direction::NorthEast:
            return x + 1 >= m_current_row.size() ? north() : m_current_row[x + 1];
        case Direction::West:
            return x < 1 ? Predictions {} : m_next_row[x - 1];
        case Direction::NorthNorth:
            return m_current_row_index < 2 ? Predictions {} : m_previous[x];
        case Direction::WestWest:
            return x < 2 ? Predictions {} : m_next_row[x - 2];
        }
        VERIFY_NOT_REACHED();
    }
}

Neighborhood retrieve_neighborhood(Channel const& channel, u32 x, u32 y)
{
    i32 const W = x > 0 ? channel.get(x - 1, y) : (y > 0 ? channel.get(x, y - 1) : 0);
    i32 const N = y > 0 ? channel.get(x, y - 1) : W;
    i32 const NW = x > 0 && y > 0 ? channel.get(x - 1, y - 1) : W;
    i32 const NE = x + 1 < channel.width() && y > 0 ? channel.get(x + 1, y - 1) : N;
    i32 const NN = y > 1 ? channel.get(x, y - 2) : N;
    i32 const WW = x > 1 ? channel.get(x - 2, y) : W;
    i32 const NEE = x + 2 < channel.width() && y > 0 ? channel.get(x + 2, y - 1) : NE;

    Neighborhood const neighborhood {
        .N = N,
        .NW = NW,
        .NE = NE,
        .W = W,
        .NN = NN,
        .WW = WW,
        .NEE = NEE,
    };

    return neighborhood;
}

// Table H.3 — Modular predictors
i32 prediction(Neighborhood const& neighborhood, i32 self_correcting, u32 predictor)
{
    switch (predictor) {
    case 0:
        return 0;
    case 1:
        return neighborhood.W;
    case 2:
        return neighborhood.N;
    case 3:
        return (neighborhood.W + neighborhood.N) / 2;
    case 4:
        return abs(neighborhood.N - neighborhood.NW) < abs(neighborhood.W - neighborhood.NW) ? neighborhood.W : neighborhood.N;
    case 5:
        return clamp(neighborhood.W + neighborhood.N - neighborhood.NW, min(neighborhood.W, neighborhood.N), max(neighborhood.W, neighborhood.N));
    case 6:
        return (self_correcting + 3) >> 3;
    case 7:
        return neighborhood.NE;
    case 8:
        return neighborhood.NW;
    case 9:
        return neighborhood.WW;
    case 10:
        return (neighborhood.W + neighborhood.NW) / 2;
    case 11:
        return (neighborhood.N + neighborhood.NW) / 2;
    case 12:
        return (neighborhood.N + neighborhood.NE) / 2;
    case 13:
        return (6 * neighborhood.N - 2 * neighborhood.NN + 7 * neighborhood.W + neighborhood.WW + neighborhood.NEE + 3 * neighborhood.NE + 8) / 16;
    }
    VERIFY_NOT_REACHED();
}

}
