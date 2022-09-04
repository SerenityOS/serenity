/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/FixedArray.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibUSBDB/Database.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool print_verbose = false;
    bool flag_show_numerical = false;
    Core::ArgsParser args;
    args.set_general_help("List USB devices.");
    args.add_option(print_verbose, "Print all device descriptors", "verbose", 'v');
    args.add_option(flag_show_numerical, "Show numerical IDs", "numerical", 'n');
    args.parse(arguments);

    if (!flag_show_numerical)
        TRY(Core::System::unveil("/res/usb.ids", "r"));
    TRY(Core::System::pledge("stdio rpath"));
    TRY(Core::System::unveil("/sys/bus/usb", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    Core::DirIterator usb_devices("/sys/bus/usb", Core::DirIterator::SkipDots);

    RefPtr<USBDB::Database> usb_db;
    if (!flag_show_numerical) {
        usb_db = USBDB::Database::open();
        if (!usb_db) {
            warnln("Failed to open usb.ids");
        }
    }

    while (usb_devices.has_next()) {
        auto full_path = LexicalPath(usb_devices.next_full_path());

        auto proc_usb_device = Core::File::construct(full_path.string());
        if (!proc_usb_device->open(Core::OpenMode::ReadOnly)) {
            warnln("Failed to open {}: {}", proc_usb_device->name(), proc_usb_device->error_string());
            continue;
        }

        auto contents = proc_usb_device->read_all();
        auto json_or_error = JsonValue::from_string(contents);
        if (json_or_error.is_error()) {
            warnln("Failed to decode JSON: {}", json_or_error.error());
            continue;
        }
        auto json = json_or_error.release_value();

        json.as_array().for_each([usb_db, print_verbose](auto& value) {
            auto& device_descriptor = value.as_object();

            auto device_address = device_descriptor.get("device_address"sv).to_u32();
            auto vendor_id = device_descriptor.get("vendor_id"sv).to_u32();
            auto product_id = device_descriptor.get("product_id"sv).to_u32();

            if (usb_db) {
                StringView vendor_string = usb_db->get_vendor(vendor_id);
                StringView device_string = usb_db->get_device(vendor_id, product_id);
                if (device_string.is_empty())
                    device_string = "Unknown Device"sv;

                outln("Device {}: ID {:04x}:{:04x} {} {}", device_address, vendor_id, product_id, vendor_string, device_string);
            } else {
                outln("Device {}: ID {:04x}:{:04x}", device_address, vendor_id, product_id);
            }

            if (print_verbose) {
                outln("Device Descriptor");
                outln("  bLength            {}", device_descriptor.get("length"sv).to_u32());
                outln("  bDescriptorType    {}", device_descriptor.get("descriptor_type"sv).to_u32());
                outln("  bcdUSB             {}", device_descriptor.get("usb_spec_compliance_bcd"sv).to_u32());
                outln("  bDeviceClass       {}", device_descriptor.get("device_class"sv).to_u32());
                outln("  bDeviceSubClass    {}", device_descriptor.get("device_sub_class"sv).to_u32());
                outln("  bDeviceProtocol    {}", device_descriptor.get("device_protocol"sv).to_u32());
                outln("  bMaxPacketSize     {}", device_descriptor.get("max_packet_size"sv).to_u32());
                if (usb_db) {
                    StringView vendor_string = usb_db->get_vendor(vendor_id);
                    StringView device_string = usb_db->get_device(vendor_id, product_id);
                    outln("  idVendor           0x{:04x} {}", device_descriptor.get("vendor_id"sv).to_u32(), vendor_string);
                    outln("  idProduct          0x{:04x} {}", device_descriptor.get("product_id"sv).to_u32(), device_string);
                } else {
                    outln("  idVendor           0x{:04x}", device_descriptor.get("vendor_id"sv).to_u32());
                    outln("  idProduct          0x{:04x}", device_descriptor.get("product_id"sv).to_u32());
                }
                outln("  bcdDevice          {}", device_descriptor.get("device_release_bcd"sv).to_u32());
                outln("  iManufacturer      {}", device_descriptor.get("manufacturer_id_descriptor_index"sv).to_u32());
                outln("  iProduct           {}", device_descriptor.get("product_string_descriptor_index"sv).to_u32());
                outln("  iSerial            {}", device_descriptor.get("serial_number_descriptor_index"sv).to_u32());
                outln("  bNumConfigurations {}", device_descriptor.get("num_configurations"sv).to_u32());

                auto const& configuration_descriptors = value.as_object().get("configurations"sv);
                configuration_descriptors.as_array().for_each([&](auto& config_value) {
                    auto const& configuration_descriptor = config_value.as_object();
                    outln("  Configuration Descriptor:");
                    outln("    bLength          {}", configuration_descriptor.get("length"sv).as_u32());
                    outln("    bDescriptorType  {}", configuration_descriptor.get("descriptor_type"sv).as_u32());
                    outln("    wTotalLength     {}", configuration_descriptor.get("total_length"sv).as_u32());
                    outln("    bNumInterfaces   {}", configuration_descriptor.get("number_of_interfaces"sv).as_u32());
                    outln("    bmAttributes     0x{:02x}", configuration_descriptor.get("attributes_bitmap"sv).as_u32());
                    outln("    MaxPower         {}mA", configuration_descriptor.get("max_power"sv).as_u32() * 2u);

                    auto const& interface_descriptors = config_value.as_object().get("interfaces"sv);
                    interface_descriptors.as_array().for_each([&](auto& interface_value) {
                        auto const& interface_descriptor = interface_value.as_object();
                        auto const interface_class_code = interface_descriptor.get("interface_class_code"sv).to_u32();
                        auto const interface_subclass_code = interface_descriptor.get("interface_sub_class_code"sv).to_u32();
                        auto const interface_protocol_code = interface_descriptor.get("interface_protocol"sv).to_u32();

                        outln("    Interface Descriptor:");
                        outln("      bLength            {}", interface_descriptor.get("length"sv).to_u32());
                        outln("      bDescriptorType    {}", interface_descriptor.get("descriptor_type"sv).to_u32());
                        outln("      bInterfaceNumber   {}", interface_descriptor.get("interface_number"sv).to_u32());
                        outln("      bAlternateSetting  {}", interface_descriptor.get("alternate_setting"sv).to_u32());
                        outln("      bNumEndpoints      {}", interface_descriptor.get("num_endpoints"sv).to_u32());
                        if (usb_db) {
                            auto const interface_class = usb_db->get_class(interface_class_code);
                            auto const interface_subclass = usb_db->get_subclass(interface_class_code, interface_subclass_code);
                            auto const interface_protocol = usb_db->get_protocol(interface_class_code, interface_subclass_code, interface_protocol_code);
                            outln("      bInterfaceClass    {} {}", interface_class_code, interface_class);
                            outln("      bInterfaceSubClass {} {}", interface_subclass_code, interface_subclass);
                            outln("      bInterfaceProtocol {} {}", interface_protocol_code, interface_protocol);
                        } else {
                            outln("      bInterfaceClass    {}", interface_class_code);
                            outln("      bInterfaceSubClass {}", interface_subclass_code);
                            outln("      bInterfaceProtocol {}", interface_protocol_code);
                        }
                        outln("      iInterface         {}", interface_descriptor.get("interface_string_desc_index"sv).to_u32());

                        auto const& endpoint_descriptors = interface_value.as_object().get("endpoints"sv);
                        endpoint_descriptors.as_array().for_each([&](auto& endpoint_value) {
                            auto const& endpoint_descriptor = endpoint_value.as_object();
                            auto const endpoint_address = endpoint_descriptor.get("endpoint_address"sv).to_u32();
                            outln("      Endpoint Descriptor:");
                            outln("        bLength            {}", endpoint_descriptor.get("length"sv).to_u32());
                            outln("        bDescriptorType    {}", endpoint_descriptor.get("descriptor_type"sv).to_u32());
                            outln("        bEndpointAddress   0x{:02x} EP {} {}", endpoint_address, (endpoint_address & 0xFu), ((endpoint_address & 0x80u) ? "IN" : "OUT"));
                            outln("        bmAttributes       0x{:02x}", endpoint_descriptor.get("attribute_bitmap"sv).to_u32());
                            outln("        wMaxPacketSize     0x{:04x}", endpoint_descriptor.get("max_packet_size"sv).to_u32());
                            outln("        bInterval          {}", endpoint_descriptor.get("polling_interval"sv).to_u32());
                        });
                    });
                });
            }
        });
    }

    return 0;
}
