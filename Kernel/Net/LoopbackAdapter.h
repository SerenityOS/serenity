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
    LoopbackAdapter(NonnullOwnPtr<KString>);

public:
    static RefPtr<LoopbackAdapter> try_create();
    virtual ~LoopbackAdapter() override;

    virtual void send_raw(ReadonlyBytes) override;
    virtual StringView class_name() const override { return "LoopbackAdapter"sv; }
    virtual bool link_up() override { return true; }
    virtual bool link_full_duplex() override { return true; }
    virtual int link_speed() override { return 1000; }
};

}
