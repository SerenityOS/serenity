/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibDeviceTree/DeviceTree.h>
#include <LibDeviceTree/FlattenedDeviceTree.h>

TEST_CASE(basic_functionality)
{
    auto fdt_file = TRY_OR_FAIL(Core::File::open("/usr/Tests/LibDeviceTree/dtb.dtb"sv, Core::File::OpenMode::Read));
    auto fdt = TRY_OR_FAIL(fdt_file->read_until_eof());

    auto device_tree = TRY_OR_FAIL(DeviceTree::DeviceTree::parse(fdt));

    auto boot_args = device_tree->resolve_property("/chosen/bootargs"sv);
    EXPECT(boot_args.has_value());
    EXPECT_EQ(boot_args->as_string(), "hello root=nvme0:1:0 serial_debug"sv);

    EXPECT(device_tree->phandle(1));
    auto device_type = device_tree->phandle(1)->get_property("device_type"sv);
    EXPECT(device_type.has_value());
    EXPECT_EQ(device_type->as_string(), "cpu"sv);
}
