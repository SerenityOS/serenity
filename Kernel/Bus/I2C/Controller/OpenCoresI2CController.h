/*
 * Copyright (c) 2024, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/I2C/Controller/Controller.h>
#include <Kernel/Firmware/DeviceTree/Device.h>
#include <Kernel/Memory/MemoryManager.h>

// https://opencores.org/websvn/filedetails?repname=i2c&path=%2Fi2c%2Ftags%2Frel_1%2Fdoc%2Fi2c_specs.pdf

namespace Kernel::I2C {

class OpenCoresI2CController final : public I2C::Controller {
public:
    static ErrorOr<NonnullOwnPtr<OpenCoresI2CController>> create(DeviceTree::Device const&);
    ~OpenCoresI2CController() override = default;

    ErrorOr<void> do_transfers(Span<Transfer>) override;

private:
    OpenCoresI2CController(NonnullOwnPtr<Memory::Region> register_region, VirtualAddress register_address, u32 register_shift, u32 register_io_width);

    NonnullOwnPtr<Memory::Region> m_register_region;
    VirtualAddress m_register_address;

    u32 m_register_shift;
    u32 m_register_io_width;

    // "Please note that all reserved bits are read as zeros. To ensure forward compatibility, they should be written as zeros."

    // 3.1 Registers list
    enum class RegisterOffset {
        // PRERlo, "Clock Prescale register lo-byte (RW)"
        PrescaleLow = 0x00,

        // PRERhi, "Clock Prescale register hi-byte (RW)"
        PrescaleHigh = 0x01,

        // CTR, "Control register (RW)"
        Control = 0x02,

        // TXR, "Transmit register (W)"
        Transmit = 0x03,

        // RXR, "Receive register (R)"
        Receive = 0x03,

        // CR, "Command register (W)"
        Command = 0x04,

        // SR, "Status register (R)"
        Status = 0x04,
    };

    static constexpr size_t REGISTER_COUNT = 5;

    // 3.2.2 Control register
    enum class ControlRegisterFlags {
        // "EN, I2C core enable bit.
        //  When set to ‘1’, the core is enabled.
        //  When set to ‘0’, the core is disabled."
        Enable = 1 << 7, // RW

        // "IEN, I 2C core interrupt enable bit.
        //  When set to ‘1’, interrupt is enabled.
        //  When set to ‘0’, interrupt is disabled."
        InterruptEnable = 1 << 6, // RW
    };
    AK_ENUM_BITWISE_FRIEND_OPERATORS(ControlRegisterFlags);

    // 3.2.5 Command register
    enum class CommandRegisterFlags {
        // "STA, generate (repeated) start condition"
        GenerateStartCondition = 1 << 7, // W

        // "STO, generate stop condition"
        GenerateStopCondition = 1 << 6, // W

        // "RD, read from slave"
        ReadFromSlave = 1 << 5, // W

        // "WR, write to slave"
        WriteToSlave = 1 << 4, // W

        // "ACK, when a receiver, sent ACK (ACK = ‘0’) or NACK (ACK = ‘1’)"
        NACK = 1 << 3, // W

        // "IACK, Interrupt acknowledge. When set, clears a pending interrupt."
        ClearPendingInterrupt = 1 << 0, // W
    };
    AK_ENUM_BITWISE_FRIEND_OPERATORS(CommandRegisterFlags);

    // 3.2.6 Status register
    enum class StatusRegisterFlags {
        // "RxACK, Received acknowledge from slave.
        //  This flag represents acknowledge from the addressed slave.
        //  ‘1’ = No acknowledge received
        //  ‘0’ = Acknowledge received"
        NoACKReceived = 1 << 7, // R

        // "Busy, I2C bus busy
        //  ‘1’ after START signal detected
        //  ‘0’ after STOP signal detected"
        BusBusy = 1 << 6, // R

        // "AL, Arbitration lost
        //  This bit is set when the core lost arbitration. Arbitration is lost when:
        //  • a STOP signal is detected, but non requested
        //  • The master drives SDA high, but SDA is low.
        //  See bus-arbitration section for more information."
        ArbitrationLost = 1 << 5, // R

        // "TIP, Transfer in progress.
        //  ‘1’ when transferring data
        //  ‘0’ when transfer complete"
        TransferInProgress = 1 << 1, // R

        // "IF, Interrupt Flag. This bit is set when an interrupt is pending, which
        //  will cause a processor interrupt request if the IEN bit is set.
        //  The Interrupt Flag is set when:
        //  • one byte transfer has been completed
        //  • arbitration is lost"
        InterruptPending = 1 << 0, // R
    };
    AK_ENUM_BITWISE_FRIEND_OPERATORS(StatusRegisterFlags);

    void write_reg(RegisterOffset reg, u8 value)
    {
        auto offset = to_underlying(reg) << m_register_shift;

        // FIXME: What is the endianness for reg-io-width > 1?
        switch (m_register_io_width) {
        case 1:
            *reinterpret_cast<u8 volatile*>(m_register_address.get() + offset) = value;
            break;
        case 2:
            VERIFY(offset % 2 == 0);
            *reinterpret_cast<u16 volatile*>(m_register_address.get() + offset) = value;
            break;
        case 4:
            VERIFY(offset % 4 == 0);
            *reinterpret_cast<u32 volatile*>(m_register_address.get() + offset) = value;
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    u8 read_reg(RegisterOffset reg)
    {
        auto offset = to_underlying(reg) << m_register_shift;

        // FIXME: What is the endianness for reg-io-width > 1?
        switch (m_register_io_width) {
        case 1:
            return *reinterpret_cast<u8 volatile*>(m_register_address.get() + offset);
        case 2:
            VERIFY(offset % 2 == 0);
            return *reinterpret_cast<u16 volatile*>(m_register_address.get() + offset);
        case 4:
            VERIFY(offset % 4 == 0);
            return *reinterpret_cast<u32 volatile*>(m_register_address.get() + offset);
        default:
            VERIFY_NOT_REACHED();
        }
    }
};

}
