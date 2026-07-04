/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Net/Cadence/GEM.h>

namespace Kernel {

// https://docs.amd.com/r/en-US/ug1087-zynq-ultrascale-registers/GEM-Module
struct CadenceGEMNetworkAdapter::Registers {
    enum class NetworkControl : u32 {
        EnableReceive = 1u << 2,
        EnableTransmit = 1u << 3,
        EnableManagementPort = 1u << 4,
        ClearStatisticsRegisters = 1u << 5,
        StartTransmission = 1u << 9,
    };
    NetworkControl network_control;

    enum class NetworkConfiguration : u32 {
        Speed = 1u << 0,
        FullDuplex = 1u << 1,
        DiscardNonVLANFrames = 1u << 2,
        JumboFrames = 1u << 3,
        CopyAllFrames = 1u << 4,
        NoBroadcast = 1u << 5,
        MulticastHashEnable = 1u << 6,
        UnicastHashEnable = 1u << 7,
        Receive1536ByteFrames = 1u << 8,
        ExternAddressMatchEnable = 1u << 9,
        GigabitModeEnable = 1u << 10,
        PCSSelect = 1u << 11,
        RetryTest = 1u << 12,
        PauseEnable = 1u << 13,

        ReceiveBufferOffsetMask = (1u << 15) | (1u << 14),
        ReceiveBufferOffset0 = 0b00u << 14,
        ReceiveBufferOffset1 = 0b01u << 14,
        ReceiveBufferOffset2 = 0b10u << 14,
        ReceiveBufferOffset3 = 0b11u << 14,

        LengthFieldErrorFrameDiscard = 1u << 16,
        FCSRemove = 1u << 17,

        MDCClockDivisionMask = (1u << 20) | (1u << 19) | (1u << 18),
        MDCClockDivision8 = 0b000u << 18,
        MDCClockDivision16 = 0b001u << 18,
        MDCClockDivision32 = 0b010u << 18,
        MDCClockDivision48 = 0b011u << 18,
        MDCClockDivision64 = 0b100u << 18,
        MDCClockDivision96 = 0b101u << 18,
        MDCClockDivision128 = 0b110u << 18,
        MDCClockDivision224 = 0b111u << 18,

        DataBusWidthMask = (1u << 22) | (1u << 21),
        DataBusWidth32 = 0b00u << 21,
        DataBusWidth64 = 0b01u << 21,

        DisableCopyOfPauseFrames = 1u << 23,
        ReceiveChecksumOffloadEnable = 1u << 24,
        EnableHalfDuplexRX = 1u << 25,
        IgnoreRXFCS = 1u << 26,
        SGMIIModeEnable = 1u << 27,
        IPGStretchEnable = 1u << 28,
        ReceiveBadPreamble = 1u << 29,
        IgnoreIPGRX_ER = 1u << 30,
        UniDirectionEnable = 1u << 31,
    };

    static constexpr size_t NETWORK_CONFIGURATION_MDC_DIVISION_MASK = 0b111 << 18;
    static constexpr size_t NETWORK_CONFIGURATION_MDC_DIVISION_OFFSET = 18;

    NetworkConfiguration network_configuration;

    enum class NetworkStatus : u32 {
        ManagementLogicIdle = 1u << 2,
    };
    NetworkStatus network_status;
    u32 _;

    enum class DMAConfiguration {
        AddressBusWidth64 = 1u << 30,
    };

    static constexpr size_t DMA_CONFIGURATION_RECEIVE_BUFFER_SIZE_MASK = 0xff << 16;
    static constexpr size_t DMA_CONFIGURATION_RECEIVE_BUFFER_SIZE_OFFSET = 16;
    static constexpr size_t DMA_CONFIGURATION_64_BIT_DMA_OFFSET = 30;
    static constexpr size_t DMA_CONFIGURATION_CHECKSUM_OFFLOAD_ENABLE_OFFSET = 11;
    static constexpr size_t DMA_CONFIGURATION_TRANSMIT_PACKET_BUFFER_MEMORY_SIZE_OFFSET = 10;
    static constexpr size_t DMA_CONFIGURATION_RECEIVE_PACKET_BUFFER_MEMORY_SIZE_OFFSET = 8;
    static constexpr size_t DMA_CONFIGURATION_SWAP_PACKET_BYTES_ENABLE_OFFSET = 7;
    static constexpr size_t DMA_CONFIGURATION_AHB_FIXED_BURST_LENGTH_OFFSET = 0;

    DMAConfiguration dma_configuration;

    enum class TransmitStatus : u32 {
    };
    TransmitStatus transmit_status;

    u32 receive_buffer_queue_base_address;
    u32 transmit_buffer_queue_base_address;

    enum class ReceiveStatus : u32 {
    };
    ReceiveStatus receive_status;

    enum class Interrupt : u32 {
        ReceiveComplete = 1 << 1,
        TransmitComplete = 1 << 7,
    };

    Interrupt interrupt_status;
    Interrupt interrupt_enable;
    Interrupt interrupt_disable;
    Interrupt interrupt_mask_status;

    enum class PhyMaintenanceOperation {
        // For IEEE 802.3 Clause 22
        Clause22Write = 0b01,
        Clause22Read = 0b10,

        // For IEEE 802.3 Clause 45
        Clause45Address = 0b00,
        Clause45Write = 0b01,
        Clause45ReadPostIncrement = 0b10,
        Clause45Read = 0b11,
    };
    static constexpr size_t PHY_MAINTENANCE_WRITE_OR_READ_DATA_OFFSET = 0;
    static constexpr size_t PHY_MAINTENANCE_WRITE_OR_READ_DATA_MASK = 0xffff;

    static constexpr size_t PHY_MAINTENANCE_WRITE_10_OFFSET = 16;
    static constexpr size_t PHY_MAINTENANCE_REGISTER_ADDRESS_OFFSET = 18;
    static constexpr size_t PHY_MAINTENANCE_PHY_ADDRESS_OFFSET = 23;
    static constexpr size_t PHY_MAINTENANCE_OPERATION_OFFSET = 28;
    static constexpr size_t PHY_MAINTENANCE_CLAUSE_22_FRAME_OFFSET = 30;
    static constexpr size_t PHY_MAINTENANCE_WRITE_0_OFFSET = 31;

    u32 phy_maintenance;

    u32 receive_pause_quantum;
    u32 transmit_pause_quantum;

    u8 _[0x80 - (0x3c + 4)];

    u32 hash_register_bottom;
    u32 hash_register_top;
    u32 specific_address_1_bottom;
    u32 specific_address_1_top;
    u32 specific_address_2_bottom;
    u32 specific_address_2_top;
    u32 specific_address_3_bottom;
    u32 specific_address_3_top;
    u32 specific_address_4_bottom;
    u32 specific_address_4_top;

    u8 _[0x440 - (0xa4 + 4)];

    u32 transmit_buffer_queue_1_base_address;
    u32 _[15];
    u32 receive_buffer_queue_1_base_address;

    u8 _[0x4c8 - (0x480 + 4)];

    u32 transmit_buffer_queue_base_address_high;
    u32 _;
    u32 _;
    u32 receive_buffer_queue_base_address_high;
};
static_assert(AssertSize<CadenceGEMNetworkAdapter::Registers, 0x4d8>());

AK_ENUM_BITWISE_OPERATORS(CadenceGEMNetworkAdapter::Registers::NetworkControl);
AK_ENUM_BITWISE_OPERATORS(CadenceGEMNetworkAdapter::Registers::NetworkConfiguration);
AK_ENUM_BITWISE_OPERATORS(CadenceGEMNetworkAdapter::Registers::NetworkStatus);
AK_ENUM_BITWISE_OPERATORS(CadenceGEMNetworkAdapter::Registers::DMAConfiguration);
AK_ENUM_BITWISE_OPERATORS(CadenceGEMNetworkAdapter::Registers::TransmitStatus);
AK_ENUM_BITWISE_OPERATORS(CadenceGEMNetworkAdapter::Registers::ReceiveStatus);
AK_ENUM_BITWISE_OPERATORS(CadenceGEMNetworkAdapter::Registers::Interrupt);

}
