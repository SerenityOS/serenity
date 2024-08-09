/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Locking/SpinlockProtected.h>

namespace Kernel {

struct FUSEInstance {
    OpenFileDescription const* fd = nullptr;
    NonnullOwnPtr<KBuffer> pending_request;
    NonnullOwnPtr<KBuffer> response;
    bool buffer_ready = false;
    bool response_ready = false;
    bool expecting_header = true;
};

class FUSEDevice final : public CharacterDevice {
    friend class Device;

public:
    static NonnullRefPtr<FUSEDevice> must_create();
    virtual ~FUSEDevice() override;

    ErrorOr<void> initialize_instance(OpenFileDescription const&);
    ErrorOr<NonnullOwnPtr<KBuffer>> send_request_and_wait_for_a_reply(OpenFileDescription const&, Bytes);
    void shutdown_for_description(OpenFileDescription const&);

private:
    FUSEDevice();

    // ^Device
    virtual bool is_openable_by_jailed_processes() const override { return false; }

    // ^CharacterDevice
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override;
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual bool can_write(OpenFileDescription const&, u64) const override;
    virtual StringView class_name() const override { return "FUSEDevice"sv; }

    SpinlockProtected<HashMap<OpenFileDescription const*, Vector<FUSEInstance>>, LockRank::None> m_instances;
    SpinlockProtected<Vector<OpenFileDescription const*>, LockRank::None> m_closing_instances;
};

}
