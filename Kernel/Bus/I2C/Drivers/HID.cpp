/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BuiltinWrappers.h>
#include <AK/ByteReader.h>
#include <AK/MemoryStream.h>
#include <Kernel/Bus/I2C/Controller/Controller.h>
#include <Kernel/Bus/I2C/Drivers/HID.h>
#include <Kernel/Devices/Input/HID/Device.h>
#include <Kernel/Devices/Input/HID/TransportInterface.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>
#include <Kernel/Interrupts/IRQHandler.h>

namespace Kernel::I2C {

class TransportInterface final
    : public HID::TransportInterface
    , public IRQHandler {
public:
    static ErrorOr<NonnullOwnPtr<TransportInterface>> create(I2C::Controller& i2c_controller, I2C::Address i2c_address, HIDDescriptor const& hid_descriptor, size_t interrupt_number)
    {
        auto input_report_buffer = TRY(ByteBuffer::create_zeroed(hid_descriptor.max_input_length));
        return TRY(adopt_nonnull_own_or_enomem(new (nothrow) TransportInterface(i2c_controller, i2c_address, hid_descriptor.input_register, interrupt_number, move(input_report_buffer))));
    }

    virtual ErrorOr<void> start_receiving_input_reports(InputReportCallback&& callback) override
    {
        m_input_report_callback = move(callback);

        enable_irq();

        return {};
    }

    ErrorOr<void> handle_input_report()
    {
        FixedMemoryStream input_report { m_input_report_buffer.bytes() };

        auto total_length = TRY(input_report.read_value<LittleEndian<u16>>());
        if (total_length < sizeof(LittleEndian<u16>))
            return EINVAL;

        auto report = TRY(input_report.read_in_place<u8>(total_length - sizeof(LittleEndian<u16>)));

        m_input_report_callback(report);

        return {};
    }

    virtual bool handle_irq() override
    {
        // FIXME: Don't do the I²C transfers in the interrupt handler while interrupts are disabled. I²C transfers are slow.
        //        Instead, do this in a thread or WorkQueue. If the interrupt is level triggered, this requires marking the
        //        interrupt as non-pending and temporarily disabling interrupts until we read the input report,
        //        as the device will otherwise continue interrupting us until we have read the input report completely.
        //        Ideally, we should have some generic abstraction for this.

        VERIFY(m_input_report_callback);

        // Read the input report from the Input Register.

        auto input_register_index = LittleEndian<u16> { m_input_register_index };

        auto read_input_report = to_array<I2C::Transfer>({
            I2C::WriteTransfer {
                .target_address = m_i2c_address,
                .data_to_write = input_register_index.bytes(),
            },
            I2C::ReadTransfer {
                .target_address = m_i2c_address,
                .data_read = m_input_report_buffer,
            },
        });

        if (auto result = m_i2c_controller.do_transfers(read_input_report); result.is_error()) {
            dbgln("Failed to receive HID input report: {}", result.error());
            return false;
        }

        if (auto result = handle_input_report(); result.is_error()) {
            dbgln("Failed to parse HID input report: {}", result.error());
            return false;
        }

        return true;
    }

private:
    TransportInterface(I2C::Controller& i2c_controller, I2C::Address i2c_address, u16 input_register_index, size_t interrupt_number, ByteBuffer input_report_buffer)
        : IRQHandler(interrupt_number)
        , m_i2c_controller(i2c_controller)
        , m_i2c_address(i2c_address)
        , m_input_register_index(input_register_index)
        , m_input_report_buffer(move(input_report_buffer))
    {
    }

    I2C::Controller& m_i2c_controller;
    I2C::Address m_i2c_address;
    u16 m_input_register_index;
    InputReportCallback m_input_report_callback;

    ByteBuffer m_input_report_buffer;
};

static constinit Array const compatibles_array = {
    "hid-over-i2c"sv,
};

DEVICETREE_DRIVER(HIDOverI2CDriver, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/input/hid-over-i2c.yaml
ErrorOr<void> HIDOverI2CDriver::probe(DeviceTree::Device const& device, StringView) const
{
    auto raw_i2c_address = TRY(TRY(TRY(device.node().reg()).entry(0)).bus_address().as_flatptr());
    if (count_required_bits(raw_i2c_address) > 10) // I²C addresses are 7-bit or 10-bit.
        return EINVAL;
    I2C::Address i2c_address = raw_i2c_address;

    auto& i2c_controller = *TRY(DeviceTree::Management::i2c_controller_for(device.node()));

    auto interrupt_number = TRY(device.get_interrupt_number(0));

    // Retrieve the HID descriptor.

    auto maybe_hid_descriptor_address = device.node().get_property("hid-descr-addr"sv);
    if (!maybe_hid_descriptor_address.has_value() || maybe_hid_descriptor_address->size() != sizeof(u32))
        return EINVAL;

    auto hid_descriptor_address = LittleEndian<u16> { static_cast<u16>(maybe_hid_descriptor_address.release_value().as<u32>()) };
    HIDDescriptor hid_descriptor;

    auto get_hid_descriptor = to_array<I2C::Transfer>({
        I2C::WriteTransfer {
            .target_address = i2c_address,
            .data_to_write = hid_descriptor_address.bytes(),
        },
        I2C::ReadTransfer {
            .target_address = i2c_address,
            .data_read = Bytes { &hid_descriptor, sizeof(hid_descriptor) },
        },
    });

    if (auto result = i2c_controller.do_transfers(get_hid_descriptor); result.is_error()) {
        dbgln("Failed to receive HID descriptor: {}", result.error());
        return EIO;
    }

    // Retrieve the HID report descriptor.

    auto hid_report_descriptor = TRY(ByteBuffer::create_uninitialized(hid_descriptor.report_descriptor_length));

    auto get_hid_report_descriptor = to_array<I2C::Transfer>({
        I2C::WriteTransfer {
            .target_address = i2c_address,
            .data_to_write = hid_descriptor.report_descriptor_register.bytes(),
        },
        I2C::ReadTransfer {
            .target_address = i2c_address,
            .data_read = hid_report_descriptor.bytes(),
        },
    });

    if (auto result = i2c_controller.do_transfers(get_hid_report_descriptor); result.is_error()) {
        dbgln("Failed to receive HID report descriptor: {}", result.error());
        return EIO;
    }

    ::HID::ReportDescriptorParser report_descriptor_parser { hid_report_descriptor };
    auto parsed_descriptor = TRY(report_descriptor_parser.parse());

    auto transport_interface = TRY(TransportInterface::create(i2c_controller, i2c_address, hid_descriptor, interrupt_number));
    auto hid_device = TRY(HID::Device::create(move(transport_interface), move(parsed_descriptor)));

    (void)hid_device.leak_ptr();

    return {};
}

}
