/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibDeviceTree/DeviceTree.h>
#include <LibDeviceTree/FlattenedDeviceTree.h>

TEST_CASE(address_translation)
{
    auto fdt_file = TRY_OR_FAIL(Core::File::open("/usr/Tests/LibDeviceTree/address-translation.dtb"sv, Core::File::OpenMode::Read));
    auto fdt = TRY_OR_FAIL(fdt_file->read_until_eof());

    auto device_tree = TRY_OR_FAIL(DeviceTree::DeviceTree::parse(fdt));

    {
        auto const* usb = device_tree->resolve_node("/soc/usb@a0010000"sv);
        EXPECT(usb != nullptr);

        auto usb_reg_entry_0 = TRY_OR_FAIL(TRY_OR_FAIL(usb->reg()).entry(0));
        EXPECT_EQ(TRY_OR_FAIL(usb_reg_entry_0.bus_address().as_flatptr()), 0xa001'0000uz);
        EXPECT_EQ(TRY_OR_FAIL(usb_reg_entry_0.length().as_size_t()), 0x10'0000uz);
        EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(usb_reg_entry_0.resolve_root_address()).as_flatptr()), 0xfe'd001'0000uz);
    }

    {
        auto const* leds = device_tree->resolve_node("/soc/some-bus@b0000000/leds@200100000"sv);
        EXPECT(leds != nullptr);

        auto const* leds_parent = leds->parent();
        EXPECT(leds_parent != nullptr);

        auto leds_parent_ranges = TRY_OR_FAIL(leds_parent->ranges());

        auto leds_reg_entry_0 = TRY_OR_FAIL(TRY_OR_FAIL(leds->reg()).entry(0));
        EXPECT_EQ(TRY_OR_FAIL(leds_reg_entry_0.bus_address().as_flatptr()), 0x2'0010'0000uz);
        EXPECT_EQ(TRY_OR_FAIL(leds_reg_entry_0.length().as_size_t()), 0x1000uz);
        EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(leds_parent_ranges.translate_child_bus_address_to_parent_bus_address(leds_reg_entry_0.bus_address())).as_flatptr()), 0xb010'0000uz);

        EXPECT_EQ(TRY_OR_FAIL(TRY_OR_FAIL(leds_reg_entry_0.resolve_root_address()).as_flatptr()), 0xfe'e010'0000uz);
    }
}
