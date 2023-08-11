/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <Kernel/Net/LoopbackAdapter.h>

namespace Kernel {

static bool s_loopback_initialized = false;

ErrorOr<NonnullRefPtr<LoopbackAdapter>> LoopbackAdapter::try_create()
{
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) LoopbackAdapter("loop"sv)));
}

LoopbackAdapter::LoopbackAdapter(StringView interface_name)
    : NetworkAdapter(interface_name)
{
    VERIFY(!s_loopback_initialized);
    s_loopback_initialized = true;
    set_mtu(65536);
    set_mac_address({ 19, 85, 2, 9, 0x55, 0xaa });
}

LoopbackAdapter::~LoopbackAdapter() = default;

void LoopbackAdapter::send_raw(ReadonlyBytes payload)
{
    dbgln_if(LOOPBACK_DEBUG, "LoopbackAdapter: Sending {} byte(s) to myself.", payload.size());
    did_receive(payload);
}

}
