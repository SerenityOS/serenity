/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::USB {

// Setup descriptor bit definitions
static constexpr u8 BM_REQUEST_HOST_TO_DEVICE = (0 << 7);
static constexpr u8 BM_REQUEST_DEVICE_TO_HOST = (1 << 7);
static constexpr u8 BM_REQUEST_TYPE_STANDARD = (0 << 5);
static constexpr u8 BM_REQUEST_TYPE_CLASS = (1 << 5);
static constexpr u8 BM_REQUEST_TYPE_VENDOR = (2 << 5);
static constexpr u8 BM_REQUEST_TYPE_RESERVED = (3 << 5);
static constexpr u8 BM_REQUEST_RECIPEINT_DEVICE = (0 << 0);
static constexpr u8 BM_REQUEST_RECIPIENT_INTERFACE = (1 << 0);
static constexpr u8 BM_REQUEST_RECIPIENT_ENDPOINT = (2 << 0);
static constexpr u8 BM_REQUEST_RECIPIENT_OTHER = (3 << 0);

//
// This is also known as the "setup" packet. It's attached to the
// first TD in the chain and is the first piece of data sent to the
// USB device over the bus.
// https://beyondlogic.org/usbnutshell/usb6.shtml#StandardEndpointRequests
//
struct USBRequestData {
    u8 request_type;
    u8 request;
    u16 value;
    u16 index;
    u16 length;
};

}
