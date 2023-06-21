/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>

namespace DSP {

// For now, this cannot be optimal as clang doesn't know underlying type specifications.
enum EnvelopeState {
    Off,
    Attack,
    Decay,
    Sustain,
    Release,
};

struct Envelope {
    constexpr Envelope() = default;
    constexpr Envelope(double envelope)
        : envelope(envelope)
    {
    }

    constexpr bool is_attack() const { return 0 <= envelope && envelope < 1; }
    constexpr double attack() const { return clamp(envelope, 0, 1); }
    constexpr void set_attack(double offset) { envelope = offset; }
    static constexpr Envelope from_attack(double attack) { return Envelope(attack); }

    constexpr bool is_decay() const { return 1 <= envelope && envelope < 2; }
    constexpr double decay() const { return clamp(envelope, 1, 2) - 1; }
    constexpr void set_decay(double offset) { envelope = 1 + offset; }
    static constexpr Envelope from_decay(double decay) { return Envelope(decay + 1); }

    constexpr bool is_sustain() const { return 2 <= envelope && envelope < 3; }
    constexpr double sustain() const { return clamp(envelope, 2, 3) - 2; }
    constexpr void set_sustain(double offset) { envelope = 2 + offset; }
    static constexpr Envelope from_sustain(double decay) { return Envelope(decay + 2); }

    constexpr bool is_release() const { return 3 <= envelope && envelope < 4; }
    constexpr double release() const { return clamp(envelope, 3, 4) - 3; }
    constexpr void set_release(double offset) { envelope = 3 + offset; }
    static constexpr Envelope from_release(double decay) { return Envelope(decay + 3); }

    constexpr bool is_active() const { return 0 <= envelope && envelope < 4; }

    constexpr void reset() { envelope = -1; }

    constexpr operator EnvelopeState() const
    {
        if (!is_active())
            return EnvelopeState::Off;
        if (is_attack())
            return EnvelopeState::Attack;
        if (is_decay())
            return EnvelopeState::Decay;
        if (is_sustain())
            return EnvelopeState::Sustain;
        if (is_release())
            return EnvelopeState::Release;
        VERIFY_NOT_REACHED();
    }

    double envelope { -1 };
};

}
