/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Types.h>
#include <Kernel/Bus/USB/xHCI/DataStructures.h>
#include <Kernel/Library/StdLib.h>

namespace Kernel::USB::xHCI {

inline void set_address(u32 volatile (&destination)[2], void* address)
{
    destination[0] = bit_cast<u64>(address) & 0xFFFF'FFFF;
    destination[1] = bit_cast<u64>(address) >> 32;
}

inline size_t device_context_size(size_t endpoints, bool context_size)
{
    if (context_size)
        return sizeof(DeviceContext64) + sizeof(EndpointContext64) * (endpoints * 2 + 1);
    return sizeof(DeviceContext) + sizeof(EndpointContext) * (endpoints * 2 + 1);
}

inline ErrorOr<Span<TransferRequestBlock>> allocate_trb_ring(size_t size, bool link_back)
{
    VERIFY(size % sizeof(TransferRequestBlock) == 0);
    // FIXME: Allow allocating split Transfer Rings
    if (size > 64 * KiB)
        return ENOTSUP;

    auto count = size / sizeof(TransferRequestBlock);
    auto* ring = new (std::align_val_t(64), nothrow) TransferRequestBlock[count];
    if (ring == nullptr)
        return ENOMEM;
    // TRB Rings may be larger than a Page, however they shall not cross a 64K byte
    // boundary. Refer to section 4.11.5.1 for more information on TRB Rings and page
    // boundaries.
    // FIXME: With a more dedicated allocation strategy we might avoid hitting this a bit more
    // FIXME: Make use of LinkTRBs in this case, if they are allowed
    if ((bit_cast<FlatPtr>(ring) & (64 * KiB)) != ((bit_cast<FlatPtr>(ring) + size) & (64 * KiB)))
        return ENOMEM;

    memset(ring, 0, size);

    auto span = Span<TransferRequestBlock> { ring, count };

    if (link_back)
        span.last() = TransferRequestBlock::link_trb(ring);

    return span;
}

}
