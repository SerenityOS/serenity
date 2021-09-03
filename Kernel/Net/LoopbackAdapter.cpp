/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/Singleton.h>
#include <Kernel/Net/LoopbackAdapter.h>

namespace Kernel {

static bool s_loopback_initialized = false;

RefPtr<LoopbackAdapter> LoopbackAdapter::try_create()
{
    return adopt_ref_if_nonnull(new LoopbackAdapter());
}

LoopbackAdapter::LoopbackAdapter()
{
    VERIFY(!s_loopback_initialized);
    s_loopback_initialized = true;
    set_loopback_name();
    set_mtu(65536);
    set_mac_address({ 19, 85, 2, 9, 0x55, 0xaa });
}

LoopbackAdapter::~LoopbackAdapter()
{
}

void LoopbackAdapter::send_raw(ReadonlyBytes payload)
{
    dbgln("LoopbackAdapter: Sending {} byte(s) to myself.", payload.size());
    did_receive(payload);
}

}
