/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Kernel::VirtIO {

enum class InitializationState {
    Unknown,
    OutOfMemory,
    Failed,
    OK
};

class [[nodiscard]] InitializationResult {
public:
    InitializationResult(InitializationState state)
        : m_state(state)
    {
    }
    InitializationResult() = default;

    [[nodiscard]] InitializationState error() const
    {
        VERIFY(is_error());
        return m_state;
    }

    [[nodiscard]] bool is_success() const { return m_state == InitializationState::OK; }
    [[nodiscard]] bool is_error() const { return !is_success(); }

    bool operator==(InitializationState state) const { return m_state == state; }
    bool operator!=(InitializationState state) const { return m_state != state; }

    // NOTE: These are here to make InitializationResult usable with TRY()
    InitializationResult release_error() { return *this; }
    void release_value() { }

private:
    InitializationState m_state { InitializationState::OK };
};

}
