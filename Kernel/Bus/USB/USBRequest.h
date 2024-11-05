/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::USB {

//
// bmRequestType fields
//
// As per Section 9.3 of the USB 2.0 Specification.
// Note that while some of these values are zero, there are here for convenience.
// This is because it makes reading the request type easier to read when constructing a USB request.
//
static constexpr u8 USB_REQUEST_TRANSFER_DIRECTION_DEVICE_TO_HOST = 0x80;
static constexpr u8 USB_REQUEST_TRANSFER_DIRECTION_HOST_TO_DEVICE = 0x00;
static constexpr u8 USB_REQUEST_TYPE_STANDARD = 0x00;
static constexpr u8 USB_REQUEST_TYPE_CLASS = 0x20;
static constexpr u8 USB_REQUEST_TYPE_VENDOR = 0x40;
static constexpr u8 USB_REQUEST_RECIPIENT_DEVICE = 0x00;
static constexpr u8 USB_REQUEST_RECIPIENT_INTERFACE = 0x01;
static constexpr u8 USB_REQUEST_RECIPIENT_ENDPOINT = 0x02;
static constexpr u8 USB_REQUEST_RECIPIENT_OTHER = 0x03;

//
// Standard USB request types
//
// These are found in Section 9.4 of the USB Spec
//
static constexpr u8 USB_REQUEST_GET_STATUS = 0x00;
static constexpr u8 USB_REQUEST_CLEAR_FEATURE = 0x01;
static constexpr u8 USB_REQUEST_SET_FEATURE = 0x03;
static constexpr u8 USB_REQUEST_SET_ADDRESS = 0x05;
static constexpr u8 USB_REQUEST_GET_DESCRIPTOR = 0x06;
static constexpr u8 USB_REQUEST_SET_DESCRIPTOR = 0x07;
static constexpr u8 USB_REQUEST_GET_CONFIGURATION = 0x08;
static constexpr u8 USB_REQUEST_SET_CONFIGURATION = 0x09;
static constexpr u8 USB_REQUEST_SET_INTERFACE = 0x0B;

// Table 9-6
static constexpr u16 USB_FEATURE_DEVICE_REMOTE_WAKEUP = 1;
static constexpr u16 USB_FEATURE_ENDPOINT_HALT = 0;
static constexpr u16 USB_FEATURE_TEST_MODE = 2;

// Figure 9-6
static constexpr u16 USB_ENDPOINT_STATUS_HALT = 1 << 0;

}
