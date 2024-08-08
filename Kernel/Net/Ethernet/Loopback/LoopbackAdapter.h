/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Net/NetworkAdapter.h>

namespace Kernel {

class LoopbackAdapter final : public NetworkAdapter {
private:
    LoopbackAdapter(StringView);

public:
    static ErrorOr<NonnullRefPtr<LoopbackAdapter>> try_create();
    virtual ~LoopbackAdapter() override;

    virtual ErrorOr<void> initialize(Badge<NetworkingManagement>) override { VERIFY_NOT_REACHED(); }

    virtual void send_raw(ReadonlyBytes) override;
    virtual StringView class_name() const override { return "LoopbackAdapter"sv; }
    virtual Type adapter_type() const override { return Type::Loopback; }
    virtual bool link_up() override { return true; }
    virtual bool link_full_duplex() override { return true; }
    virtual int link_speed() override { return 1000; }
};

}
