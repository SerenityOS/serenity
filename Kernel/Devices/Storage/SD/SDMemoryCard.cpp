/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Storage/SD/Commands.h>
#include <Kernel/Devices/Storage/SD/SDHostController.h>
#include <Kernel/Devices/Storage/SD/SDMemoryCard.h>

namespace Kernel {

SDMemoryCard::SDMemoryCard(SDHostController& sdhc, StorageDevice::LUNAddress lun_address, u32 hardware_relative_controller_id, u32 block_len, u64 capacity_in_blocks, u32 relative_card_address, SD::OperatingConditionRegister ocr, SD::CardIdentificationRegister cid, SD::SDConfigurationRegister scr)
    : StorageDevice(lun_address, hardware_relative_controller_id, block_len, capacity_in_blocks)
    , m_sdhc(sdhc)
    , m_relative_card_address(relative_card_address)
    , m_ocr(ocr)
    , m_cid(cid)
    , m_scr(scr)
{
}

void SDMemoryCard::start_request(AsyncBlockDeviceRequest& request)
{
    // FIXME: Make this asynchronous
    MutexLocker locker(m_lock);

    VERIFY(request.block_size() == block_size());

    auto buffer = request.buffer();
    u32 block_address = request.block_index();
    if (card_addressing_mode() == CardAddressingMode::ByteAddressing) {
        block_address *= block_size();
    }

    if (request.request_type() == AsyncBlockDeviceRequest::RequestType::Write) {
        if (m_sdhc.write_block({}, block_address, request.block_count(), buffer).is_error()) {
            request.complete(AsyncDeviceRequest::Failure);
            return;
        }
    } else {
        if (m_sdhc.read_block({}, block_address, request.block_count(), buffer).is_error()) {
            request.complete(AsyncDeviceRequest::Failure);
            return;
        }
    }

    request.complete(AsyncDeviceRequest::Success);
}

}
