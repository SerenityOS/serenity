/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Locking/Mutex.h>

namespace Kernel {

class MasterPTY;

class PTYMultiplexer final : public CharacterDevice {
    AK_MAKE_ETERNAL
public:
    PTYMultiplexer();
    virtual ~PTYMultiplexer() override;

    static void initialize();
    static PTYMultiplexer& the();

    // ^CharacterDevice
    virtual KResultOr<NonnullRefPtr<OpenFileDescription>> open(int options) override;
    virtual KResultOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override { return 0; }
    virtual KResultOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return 0; }
    virtual bool can_read(const OpenFileDescription&, size_t) const override { return true; }
    virtual bool can_write(const OpenFileDescription&, size_t) const override { return true; }

    void notify_master_destroyed(Badge<MasterPTY>, unsigned index);

private:
    // ^CharacterDevice
    virtual StringView class_name() const override { return "PTYMultiplexer"; }

    static constexpr size_t max_pty_pairs = 64;
    MutexProtected<Vector<unsigned, max_pty_pairs>> m_freelist;
};

}
