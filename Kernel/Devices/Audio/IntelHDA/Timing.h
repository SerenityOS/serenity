/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Function.h>

namespace Kernel::Audio::IntelHDA {

// We define an arbitrary controller timeout of 300ms for most actions.
constexpr size_t controller_timeout_in_microseconds = 300'000;

consteval u32 frame_delay_in_microseconds(u32 frames)
{
    // NOTE: the link operates at this _fixed_ frequency and is independent of the streams' rates.
    constexpr u32 link_frame_frequency_hz = 48'000;

    // 2.2: Streams and Channels
    // A new frame starts exactly every 20.83 μs, corresponding to the common 48-kHz sample rate.
    VERIFY(frames <= 4294);
    return frames * 1'000'000u / link_frame_frequency_hz + 1u;
}

ErrorOr<void> wait_until(size_t delay_in_microseconds, size_t timeout_in_microseconds, Function<ErrorOr<bool>()> condition);

}
