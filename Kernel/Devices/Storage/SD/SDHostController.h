/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/Result.h>
#include <AK/Types.h>
#include <Kernel/Devices/Storage/SD/Commands.h>
#include <Kernel/Devices/Storage/SD/Registers.h>
#include <Kernel/Devices/Storage/SD/SDMemoryCard.h>
#include <Kernel/Locking/Mutex.h>

namespace Kernel {

class SDHostController : public StorageController {
public:
    SDHostController();
    ErrorOr<void> initialize();

    virtual ~SDHostController() = default;

    virtual LockRefPtr<StorageDevice> device(u32 index) const override;
    virtual size_t devices_count() const override { return m_card ? 1 : 0; }
    virtual void complete_current_request(AsyncDeviceRequest::RequestResult) override;

    ErrorOr<void> read_block(Badge<SDMemoryCard>, u32 block_address, u32 block_count, UserOrKernelBuffer out);
    ErrorOr<void> write_block(Badge<SDMemoryCard>, u32 block_address, u32 block_count, UserOrKernelBuffer in);

    void try_enable_dma();

protected:
    virtual SD::HostControlRegisterMap volatile* get_register_map_base_address() = 0;

private:
    ErrorOr<NonnullRefPtr<SDMemoryCard>> try_initialize_inserted_card();

    bool is_card_inserted() const
    {
        return m_registers->present_state.card_inserted;
    }

    SD::HostVersion host_version() { return m_registers->slot_interrupt_status_and_version.specification_version_number; }

    ErrorOr<void> reset_host_controller();

    SD::Command last_sent_command()
    {
        SD::Command command {};
        command.raw = m_registers->transfer_mode_and_command;
        return command;
    }
    bool currently_active_command_uses_transfer_complete_interrupt();

    ErrorOr<u32> calculate_sd_clock_divisor(u32 sd_clock_frequency, u32 frequency);
    bool is_sd_clock_enabled();
    ErrorOr<void> sd_clock_supply(u32 frequency);
    ErrorOr<void> sd_clock_stop();
    ErrorOr<void> sd_clock_frequency_change(u32 frequency);
    ErrorOr<u32> retrieve_sd_clock_frequency();

    struct Response {
        u32 response[4];
    };
    ErrorOr<void> issue_command(SD::Command const&, u32 argument);
    ErrorOr<Response> wait_for_response();

    bool card_status_contains_errors(SD::Command const&, u32);

    bool retry_with_timeout(Function<bool()>, i64 delay_between_tries = 100);

    enum class DataTransferType {
        Read,
        Write
    };
    enum class OperatingMode {
        PIO,
        ADMA2_32,
        ADMA2_64
    };

    ErrorOr<void> transaction_control_with_data_transfer_using_the_dat_line_without_dma(SD::Command const&, u32 argument, u32 block_count, u32 block_size, UserOrKernelBuffer, DataTransferType data_transfer_type);
    ErrorOr<void> transfer_blocks_adma2(u32 block_address, u32 block_count, UserOrKernelBuffer, SD::DataTransferDirection);
    ErrorOr<SD::SDConfigurationRegister> retrieve_sd_configuration_register(u32 relative_card_address);

    u32 make_adma_descriptor_table(u32 block_count);

    volatile SD::HostControlRegisterMap* m_registers;
    RefPtr<SDMemoryCard> m_card { nullptr };

    u32 m_hardware_relative_controller_id { 0 };
    OperatingMode m_mode { OperatingMode::PIO };
    Mutex m_lock { "SDHostController"sv };

    // For ADMA2
    // One page of descriptor tables with 16 bit lengths can address writes of
    // Up to 4 MiB ADMA2_32
    // Up to 2 MiB ADMA2_64
    // To not over allocate we use a buffer of just 16 pages
    // FIXME: Investigate the average usage and adjust this
    constexpr static size_t dma_rw_buffer_size = 16 * PAGE_SIZE;
    constexpr static size_t dma_region_size = PAGE_SIZE + dma_rw_buffer_size;
    OwnPtr<Memory::Region> m_dma_region;
};

}
