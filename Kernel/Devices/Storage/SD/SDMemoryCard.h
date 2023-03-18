/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Result.h>
#include <AK/Types.h>
#include <Kernel/Devices/Storage/SD/Registers.h>
#include <Kernel/Devices/Storage/StorageDevice.h>
#include <Kernel/Locking/Mutex.h>

namespace Kernel {

class SDHostController;

class SDMemoryCard : public StorageDevice {
public:
    SDMemoryCard(SDHostController& sdhc, StorageDevice::LUNAddress, u32 hardware_relative_controller_id, u32 block_len, u64 capacity_in_blocks, u32 relative_card_address, SD::OperatingConditionRegister ocr, SD::CardIdentificationRegister cid, SD::SDConfigurationRegister scr);

    // ^StorageDevice
    virtual CommandSet command_set() const override { return CommandSet::SD; }

    // ^BlockDevice
    virtual void start_request(AsyncBlockDeviceRequest&) override;

private:
    enum class CardAddressingMode {
        ByteAddressing,
        BlockAddressing
    };
    CardAddressingMode card_addressing_mode() const
    {
        return m_ocr.card_capacity_status ? CardAddressingMode::BlockAddressing : CardAddressingMode::ByteAddressing;
    }

    Mutex m_lock { "SDMemoryCard"sv };
    SDHostController& m_sdhc;

    u32 m_relative_card_address;
    SD::OperatingConditionRegister m_ocr;
    SD::CardIdentificationRegister m_cid;
    SD::SDConfigurationRegister m_scr;
};

}
