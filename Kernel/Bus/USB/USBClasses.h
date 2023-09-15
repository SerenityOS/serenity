/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::USB {

// https://www.usb.org/defined-class-codes
static constexpr u8 USB_CLASS_DEVICE = 0x00;
static constexpr u8 USB_CLASS_AUDIO = 0x01;
static constexpr u8 USB_CLASS_COMMUNICATIONS_AND_CDC_CONTROL = 0x02;
static constexpr u8 USB_CLASS_HID = 0x03;
static constexpr u8 USB_CLASS_PHYSICAL = 0x05;
static constexpr u8 USB_CLASS_IMAGE = 0x06;
static constexpr u8 USB_CLASS_PRINTER = 0x07;
static constexpr u8 USB_CLASS_MASS_STORAGE = 0x08;
static constexpr u8 USB_CLASS_HUB = 0x09;
static constexpr u8 USB_CLASS_CDC_DATE = 0x0A;
static constexpr u8 USB_CLASS_SMART_CARD = 0x0B;
static constexpr u8 USB_CLASS_CONTENT_SECURITY = 0x0D;
static constexpr u8 USB_CLASS_VIDEO = 0x0E;
static constexpr u8 USB_CLASS_PERSONAL_HEALTHCARE = 0x0F;
static constexpr u8 USB_CLASS_AUDIO_VIDEO = 0x10;
static constexpr u8 USB_CLASS_BILLBOARD = 0x11;
static constexpr u8 USB_CLASS_TYPE_C_BRIDGE = 0x12;
static constexpr u8 USB_CLASS_DIAGNOSTIC = 0xDC;
static constexpr u8 USB_CLASS_WIRELESS_CONTROLLER = 0xE0;
static constexpr u8 USB_CLASS_MISCELLANEOUS = 0xEF;
static constexpr u8 USB_CLASS_APPLICATION_SPECIFIC = 0xFE;
static constexpr u8 USB_CLASS_VENDOR_SPECIFIC = 0xFF;

}
