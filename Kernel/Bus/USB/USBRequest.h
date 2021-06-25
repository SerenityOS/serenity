/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

//
// USB Request directions
//
// As per Section 9.4 of the USB Specification, it is noted that Requeset Types that
// Device to Host have bit 7 of `bmRequestType` set. These are here as a convenience,
// as we construct the request at the call-site to make reading transfers easier.
//
static constexpr u8 USB_DEVICE_REQUEST_DEVICE_TO_HOST = 0x80;
static constexpr u8 USB_DEVICE_REQUEST_HOST_TO_DEVICE = 0x00;
static constexpr u8 USB_INTERFACE_REQUEST_DEVICE_TO_HOST = 0x81;
static constexpr u8 USB_INTERFACE_REQUEST_HOST_TO_DEVICE = 0x01;
static constexpr u8 USB_ENDPOINT_REQUEST_DEVICE_TO_HOST = 0x82;
static constexpr u8 USB_ENDPOINT_REQUEST_HOST_TO_DEVICE = 0x02;

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
