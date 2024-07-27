/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtraDetails.h>
#include <AK/StringView.h>
#include <AK/Types.h>

namespace Kernel::USB::HID {

// https://www.usb.org/sites/default/files/hid1_11.pdf

// 4.2 Subclass
enum class SubclassCode : u8 {
    BootProtocol = 0x01,
};

// Appendix B.2 Protocol 2 (Mouse)
struct [[gnu::packed]] MouseBootProtocolPacket {
    u8 buttons;
    i8 x;
    i8 y;
    i8 z;
    i8 reserved1;
    i8 reserved2;
};

static_assert(AssertSize<MouseBootProtocolPacket, 6>());

constexpr StringView subclass_string(SubclassCode code)
{
    switch (code) {
    case SubclassCode::BootProtocol:
        return "Boot Protocol"sv;
    }

    return "Reserved"sv;
}

// 4.3 Protocols
enum class InterfaceProtocol : u8 {
    Mouse = 0x02,
};

}
