/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <Kernel/Devices/CharacterDevice.h>

namespace Kernel {

class MasterPTY;

class PTYMultiplexer final : public CharacterDevice {
public:
    PTYMultiplexer();
    virtual ~PTYMultiplexer() override;

    static void initialize();
    static PTYMultiplexer& the();

    // ^CharacterDevice
    virtual ErrorOr<NonnullRefPtr<OpenFileDescription>> open(int options) override;
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override { return 0; }
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override { return 0; }
    virtual bool can_read(OpenFileDescription const&, u64) const override { return true; }
    virtual bool can_write(OpenFileDescription const&, u64) const override { return true; }

    void notify_master_destroyed(Badge<MasterPTY>, unsigned index);

private:
    // ^CharacterDevice
    virtual StringView class_name() const override { return "PTYMultiplexer"sv; }

    static constexpr size_t max_pty_pairs = 64;
    SpinlockProtected<Vector<unsigned, max_pty_pairs>, LockRank::None> m_freelist {};
};

}
