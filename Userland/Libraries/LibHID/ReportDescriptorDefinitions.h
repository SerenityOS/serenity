/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>

namespace HID {

// https://www.usb.org/document-library/device-class-definition-hid-111

// 6.2.2 Report Descriptor

enum class ItemType : u8 {
    Main = 0,
    Global = 1,
    Local = 2,
    Reserved = 3,
};

// 5.3 Generic Item Format, 6.2.2.2 Short Items, 6.2.2.3 Long Items

static constexpr u8 TAG_LONG_ITEM = 0b1111;

struct [[gnu::packed]] ItemHeader {
    u8 size : 2;
    ItemType type : 2;
    u8 tag : 4;

    u8 real_size() const { return size == 3 ? 4 : size; }
};
static_assert(AssertSize<ItemHeader, 1>());

// 6.2.2.4 Main Items

enum class MainItemTag : u8 {
    Input = 0b1000,
    Output = 0b1001,
    Feature = 0b1011,
    Collection = 0b1010,
    EndCollection = 0b1100,
};

// 6.2.2.5 Input, Output, and Feature Items

struct [[gnu::packed]] InputItemData {
    u8 constant : 1;
    u8 variable : 1;
    u8 relative : 1;
    u8 wrap : 1;
    u8 nonlinear : 1;
    u8 no_preferred_state : 1;
    u8 has_null_state : 1;
    u8 : 1;
    u8 buffered_bytes : 1;
};
static_assert(AssertSize<InputItemData, 2>());

struct [[gnu::packed]] OutputItemData {
    u8 constant : 1;
    u8 variable : 1;
    u8 relative : 1;
    u8 wrap : 1;
    u8 nonlinear : 1;
    u8 no_preferred_state : 1;
    u8 has_null_state : 1;
    u8 volatile_ : 1;
    u8 buffered_bytes : 1;
};
static_assert(AssertSize<OutputItemData, 2>());

struct [[gnu::packed]] FeatureItemData {
    u8 constant : 1;
    u8 variable : 1;
    u8 relative : 1;
    u8 wrap : 1;
    u8 nonlinear : 1;
    u8 no_preferred_state : 1;
    u8 has_null_state : 1;
    u8 volatile_ : 1;
    u8 buffered_bytes : 1;
};
static_assert(AssertSize<FeatureItemData, 2>());

// 6.2.2.6 Collection, End Collection Items

enum class CollectionType : u8 {
    Physical = 0x00,
    Application = 0x01,
    Logical = 0x02,
    Report = 0x03,
    NamedArray = 0x04,
    UsageSwitch = 0x05,
    UsageModifier = 0x06,
};

// 6.2.2.7 Global Items

enum class GlobalItemTag : u8 {
    UsagePage = 0b0000,
    LogicalMinimum = 0b0001,
    LogicalMaximum = 0b0010,
    PhysicalMinimum = 0b0011,
    PhysicalMaximum = 0b0100,
    UnitExponent = 0b0101,
    Unit = 0b0110,
    ReportSize = 0b0111,
    ReportID = 0b1000,
    ReportCount = 0b1001,
    Push = 0b1010,
    Pop = 0b1011,
};

// 6.2.2.8 Local Items

enum class LocalItemTag : u8 {
    Usage = 0b0000,
    UsageMinimum = 0b0001,
    UsageMaximum = 0b0010,
    DesignatorIndex = 0b0011,
    DesignatorMinimum = 0b0100,
    DesignatorMaximum = 0b0101,
    StringIndex = 0b0111,
    StringMinimum = 0b1000,
    StringMaximum = 0b1001,
    Delimiter = 0b1010,
};

}

template<>
class AK::Traits<::HID::ItemHeader> : public DefaultTraits<::HID::ItemHeader> {
public:
    static constexpr bool is_trivially_serializable() { return true; }
};
