/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/StdLibExtras.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Panic.h>
#include <Kernel/Storage/SD/Commands.h>
#include <Kernel/Storage/SD/SDHostController.h>
#include <Kernel/Storage/StorageManagement.h>
#include <Kernel/Time/TimeManagement.h>
#if ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/RPi/SDHostController.h>
#endif

namespace Kernel {

// Relevant Specifications:
// * (SDHC): SD Host Controller Simplified Specification (https://www.sdcard.org/downloads/pls/)
// * (PLSS) Physical Layer Simplified Specification (https://www.sdcard.org/downloads/pls/)
// * (BCM2835) BCM2835 ARM Peripherals (https://www.raspberrypi.org/app/uploads/2012/02/BCM2835-ARM-Peripherals.pdf)

static void delay(i64 nanoseconds)
{
    auto start = TimeManagement::the().monotonic_time().to_nanoseconds();
    auto end = start + nanoseconds;
    while (TimeManagement::the().monotonic_time().to_nanoseconds() < end)
        Processor::pause();
}

constexpr u32 max_supported_sdsc_frequency = 25000000;

// In "m_registers->host_configuration_1"
// In sub-register "Clock Control"
constexpr u32 internal_clock_enable = 1 << 0;
constexpr u32 internal_clock_stable = 1 << 1;
constexpr u32 sd_clock_enable = 1 << 2;

// In sub-register "Software Reset"
constexpr u32 software_reset_for_all = 0x01000000;

// In Interrupt Status Register
const u32 command_complete = 1 << 0;
const u32 transfer_complete = 1 << 1;
const u32 buffer_write_ready = 1 << 4;
const u32 buffer_read_ready = 1 << 5;

// PLSS 5.1: all voltage windows
constexpr u32 acmd41_voltage = 0x00ff8000;
// PLSS 4.2.3.1: All voltage windows, XPC = 1, SDHC = 1
constexpr u32 acmd41_arg = 0x50ff8000;

constexpr u32 block_len = 512;

SDHostController::SDHostController()
    : StorageController(StorageManagement::generate_relative_sd_controller_id({}))
{
}

ErrorOr<void> SDHostController::reset() { return ENOTIMPL; }

ErrorOr<void> SDHostController::shutdown() { return ENOTIMPL; }

void SDHostController::complete_current_request(AsyncDeviceRequest::RequestResult)
{
    VERIFY_NOT_REACHED();
}

ErrorOr<void> SDHostController::initialize()
{
    m_registers = get_register_map_base_address();
    if (!m_registers)
        return EIO;

    if (host_version() != SD::HostVersion::Version3 && host_version() != SD::HostVersion::Version2)
        return ENOTSUP;

    TRY(reset_host_controller());

    m_registers->interrupt_status_enable = 0xffffffff;

    auto card_or_error = try_initialize_inserted_card();
    if (card_or_error.is_error() && card_or_error.error().code() != ENODEV) {
        dmesgln("SDHostController: Failed to initialize inserted card: {}", card_or_error.error());
    } else if (!card_or_error.is_error()) {
        m_card = card_or_error.release_value();
    }

    return {};
}

ErrorOr<NonnullLockRefPtr<SDMemoryCard>> SDHostController::try_initialize_inserted_card()
{
    if (!is_card_inserted())
        return ENODEV;

    // PLSS 4.2: "Card Identification Mode"
    // "After power-on ...the cards are initialized with ... 400KHz clock frequency."

    // NOTE: The SDHC might already have been initialized (e.g. by the bootloader), let's reset it to a known configuration
    if (is_sd_clock_enabled())
        sd_clock_stop();
    TRY(sd_clock_supply(400000));

    // PLSS 4.2.3: "Card Initialization and Identification Process"
    // Also see Figure 4-2 in the PLSS spec for a flowchart of the initialization process.
    // Note that the steps correspond to the steps in the flowchart, although I made up the numbering and text

    // 1. Send CMD0 (GO_IDLE_STATE) to the card
    TRY(issue_command(SD::Commands::go_idle_state, 0));
    TRY(wait_for_response());

    // 2. Send CMD8 (SEND_IF_COND) to the card
    // SD interface condition: 7:0 = check pattern, 11:8 = supply voltage
    //      0x1aa: check pattern = 10101010, supply voltage = 1 => 2.7-3.6V
    const u32 voltage_window = 0x1aa;
    TRY(issue_command(SD::Commands::send_if_cond, voltage_window));
    auto interface_condition_response = wait_for_response();

    // 3. If the card does not respond to CMD8 it means that (Ver2.00 or later
    // SD Memory Card(voltage mismatch) or Ver1.X SD Memory Card or not SD
    // Memory Card)
    if (interface_condition_response.is_error()) {
        // TODO: This is supposed to be the "No Response" branch of the
        // flowchart in Figure 4-2 of the PLSS spec
        return ENOTSUP;
    }

    // 4. If the card responds to CMD8, but it's not a valid response then the
    // card is not usable
    if (interface_condition_response.value().response[0] != voltage_window) {
        // FIXME: We should probably try again with a lower voltage window
        return ENODEV;
    }

    // 5. Send ACMD41 (SEND_OP_COND) with HCS=1 to the card, repeat this until the card is ready or timeout
    SD::OperatingConditionRegister ocr = {};
    bool card_is_usable = true;
    if (!retry_with_timeout([&]() {
            if (issue_command(SD::Commands::app_cmd, 0).is_error() || wait_for_response().is_error())
                return false;

            if (issue_command(SD::Commands::app_send_op_cond, acmd41_arg).is_error())
                return false;

            if (auto acmd41_response = wait_for_response();
                !acmd41_response.is_error()) {

                // 20. Check if the card supports the voltage windows we requested and SDHC
                u32 response = acmd41_response.value().response[0];
                if ((response & acmd41_voltage) != acmd41_voltage) {
                    card_is_usable = false;
                    return false;
                }

                ocr.raw = acmd41_response.value().response[0];
            }

            return ocr.card_power_up_status == 1;
        })) {
        return card_is_usable ? EIO : ENODEV;
    }

    // 6. If you requested to switch to 1.8V, and the card accepts, execute a voltage switch sequence
    //    (we didn't ask it)

    // 7. Send CMD2 (ALL_SEND_CID) to the card
    TRY(issue_command(SD::Commands::all_send_cid, 0));
    auto all_send_cid_response = TRY(wait_for_response());
    auto cid = bit_cast<SD::CardIdentificationRegister>(all_send_cid_response.response);

    // 8. Send CMD3 (SEND_RELATIVE_ADDR) to the card
    TRY(issue_command(SD::Commands::send_relative_addr, 0));
    auto send_relative_addr_response = TRY(wait_for_response());
    u32 rca = send_relative_addr_response.response[0]; // FIXME: Might need to clear some bits here

    // Extra steps:

    TRY(issue_command(SD::Commands::send_csd, rca));
    auto send_csd_response = TRY(wait_for_response());
    auto csd = bit_cast<SD::CardSpecificDataRegister>(send_csd_response.response);

    u32 block_count = (csd.device_size + 1) * (1 << (csd.device_size_multiplier + 2));
    u32 block_size = (1 << csd.max_read_data_block_length);
    u64 capacity = static_cast<u64>(block_count) * block_size;
    u64 card_capacity_in_blocks = capacity / block_len;

    // TODO: Do high speed initialisation, if supported
    TRY(sd_clock_frequency_change(max_supported_sdsc_frequency));

    TRY(issue_command(SD::Commands::select_card, rca));
    TRY(wait_for_response());

    // Set block length to 512 if the card is SDSC.
    // All other models only support 512 byte blocks so they don't need to be explicitly told
    if (!ocr.card_capacity_status) {
        TRY(issue_command(SD::Commands::set_block_len, block_len));
        TRY(wait_for_response());
    }

    auto scr = TRY(retrieve_sd_configuration_register(rca));

    TRY(issue_command(SD::Commands::app_cmd, rca));
    TRY(wait_for_response());
    TRY(issue_command(SD::Commands::app_set_bus_width, 0x2)); // 0b00=1 bit bus, 0b10=4 bit bus
    TRY(wait_for_response());

    return TRY(DeviceManagement::try_create_device<SDMemoryCard>(
        *this,
        StorageDevice::LUNAddress { controller_id(), 0, 0 },
        hardware_relative_controller_id(), block_len,
        card_capacity_in_blocks, rca, ocr, cid, scr));
}

bool SDHostController::retry_with_timeout(Function<bool()> f, i64 delay_between_tries)
{
    int timeout = 1000;
    bool success = false;
    while (!success && timeout > 0) {
        success = f();
        if (!success)
            delay(delay_between_tries);
        timeout--;
    }
    return timeout > 0;
}

ErrorOr<void> SDHostController::issue_command(SD::Command const& cmd, u32 argument)
{
    // SDHC 3.7.1: "Transaction Control without Data Transfer Using DAT Line"
    constexpr u32 command_inhibit = 1 << 1;

    // 1. Check Command Inhibit (CMD) in the Present State register.
    //    Repeat this step until Command Inhibit (CMD) is 0.
    //    That is, when Command Inhibit (CMD) is 1, the Host Driver
    //    shall not issue an SD Command.
    if (!retry_with_timeout(
            [&]() { return !(m_registers->present_state & command_inhibit); })) {
        return EIO;
    }

    // 2. If the Host Driver issues an SD Command using DAT lines
    //    including busy signal, go to step (3).
    //    If without using DAT lines including busy signal, go to step (5).
    // 3. If the Host Driver is issuing an abort command, go to step (5). In the
    //    case of non-abort command, go to step (4).
    if (cmd.requires_dat_line() && cmd.type != SD::CommandType::Abort) {

        // 4. Check Command Inhibit (DAT) in the Present State register. Repeat
        // this step until Command Inhibit (DAT) is set to 0.
        constexpr u32 data_inhibit = 1 << 2;
        if (!retry_with_timeout([&]() { return !(m_registers->present_state & data_inhibit); })) {
            return EIO;
        }
    }

    // 5. Set registers as described in Table 1-2 except Command register.
    m_registers->argument_1 = argument;

    // 6. Set the Command register.
    m_registers->transfer_mode_and_command = cmd.raw;

    // 7. Perform Command Completion Sequence in accordance with 3.7.1.2.
    // Done in wait_for_response()

    return {};
}

ErrorOr<SDHostController::Response> SDHostController::wait_for_response()
{
    // SDHC 3.7.1.2 The Sequence to Finalize a Command

    // 1. Wait for the Command Complete Interrupt. If the Command Complete
    // Interrupt has occurred, go to step (2).
    if (!retry_with_timeout(
            [&]() { return m_registers->interrupt_status & command_complete; })) {
        return EIO;
    }

    // 2. Write 1 to Command Complete in the Normal Interrupt Status register to clear this bit
    m_registers->interrupt_status = command_complete;

    // 3. Read the Response register(s) to get the response.
    // NOTE: We read fewer bits than ResponseType because the missing bits are only
    //       relevant for the physical layer, and the device filters them before they
    //       reach us
    Response r = {};
    auto cmd = last_sent_command();
    switch (cmd.response_type) {
    case SD::ResponseType::NoResponse:
        break;
    case SD::ResponseType::ResponseOf136Bits:
        r.response[0] = m_registers->response_0;
        r.response[1] = m_registers->response_1;
        r.response[2] = m_registers->response_2;
        r.response[3] = m_registers->response_3;
        break;
    case SD::ResponseType::ResponseOf48Bits:
        r.response[0] = m_registers->response_0;
        break;
    case SD::ResponseType::ResponseOf48BitsWithBusy:
        // FIXME: Idk what to do here
        break;
    }

    // 4. Judge whether the command uses the Transfer Complete Interrupt or not.
    //    If it uses Transfer Complete, go to step (5). If not, go to step (7).
    if (last_sent_command().uses_transfer_complete_interrupt())
        TODO();

    // 7. Check for errors in Response Data. If there is no error, go to step (8). If there is an error, go to step (9).
    if (cmd.response_type != SD::ResponseType::ResponseOf136Bits) {
        if (card_status_contains_errors(cmd, r.response[0])) {
            return EIO;
        }
    }

    // NOTE: Steps 7, 8 and 9 consist of checking the response for errors, which
    // are specific to each command therefore those steps are not fully implemented
    // here.

    return { r };
}

bool SDHostController::is_sd_clock_enabled()
{
    return m_registers->host_configuration_1 & sd_clock_enable;
}

ErrorOr<u32> SDHostController::calculate_sd_clock_divisor(u32 sd_clock_frequency, u32 frequency)
{
    // SDHC 2.2.14: "Clock Control Register"

    // (1) 10-bit Divisor Mode
    // This mode is supported by the Host Controller Version 1.00 and 2.00.
    // The frequency is not programmed directly; rather this register holds the divisor of
    // the Base Clock Frequency For SD Clock in the Capabilities register. Only
    // the following settings are allowed.
    //
    //     +-----+---------------------------+
    //     | 80h | base clock divided by 256 |
    //     | 40h | base clock divided by 128 |
    //     | 20h | base clock divided by 64  |
    //     | 10h | base clock divided by 32  |
    //     | 08h | base clock divided by 16  |
    //     | 04h | base clock divided by 8   |
    //     | 02h | base clock divided by 4   |
    //     | 01h | base clock divided by 2   |
    //     | 00h | Base clock (10MHz-63MHz)  |
    //     +-----+---------------------------+
    //
    if (host_version() == SD::HostVersion::Version2 || host_version() == SD::HostVersion::Version1) {
        for (u32 divisor = 1; divisor <= 256; divisor *= 2) {
            if (sd_clock_frequency / divisor <= frequency)
                return divisor >> 1;
        }

        dmesgln("SDHostController: Could not find a suitable divisor for the requested frequency");
        return ENOTSUP;
    }

    // (2) 10-bit Divided Clock Mode
    // Host Controller Version 3.00 supports this mandatory mode instead of the
    // 8-bit Divided Clock Mode. The length of divider is extended to 10 bits and all
    // divider values shall be supported.
    //
    //     +------+-------------------------------+
    //     | 3FFh | 1/2046 Divided Clock          |
    //     | .... | ............................. |
    //     |  N   | 1/2N Divided Clock (Duty 50%) |
    //     | .... | ............................. |
    //     | 002h | 1/4 Divided Clock             |
    //     | 001h | 1/2 Divided Clock             |
    //     | 000h | Base Clock (10MHz-255MHz)     |
    //     +------+-------------------------------+
    //
    if (host_version() == SD::HostVersion::Version3) {
        if (frequency == sd_clock_frequency)
            return 0;

        auto divisor = AK::ceil_div(sd_clock_frequency, 2 * frequency);
        if (divisor > 0x3ff) {
            dmesgln("SDHostController: Cannot represent the divisor for the requested frequency");
            return ENOTSUP;
        }

        return divisor;
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<void> SDHostController::sd_clock_supply(u32 frequency)
{
    // SDHC 3.2.1: "SD Clock Supply Sequence"
    // The *Clock Control* register is in the lower 16 bits of *Host Configuration 1*
    VERIFY((m_registers->host_configuration_1 & sd_clock_enable) == 0);

    // 1. Find out the divisor to determine the SD Clock Frequency
    const u32 sd_clock_frequency = TRY(retrieve_sd_clock_frequency());
    u32 divisor = TRY(calculate_sd_clock_divisor(sd_clock_frequency, frequency));

    // 2. Set Internal Clock Enable and SDCLK Frequency Select in the Clock Control register
    const u32 eight_lower_bits_of_sdclk_frequency_select = (divisor & 0xff) << 8;
    u32 sdclk_frequency_select = eight_lower_bits_of_sdclk_frequency_select;
    if (host_version() == SD::HostVersion::Version3) {
        const u32 two_upper_bits_of_sdclk_frequency_select = (divisor >> 8 & 0x3) << 6;
        sdclk_frequency_select |= two_upper_bits_of_sdclk_frequency_select;
    }
    m_registers->host_configuration_1 = m_registers->host_configuration_1 | internal_clock_enable | sdclk_frequency_select;

    // 3. Check Internal Clock Stable in the Clock Control register until it is 1
    if (!retry_with_timeout([&] { return m_registers->host_configuration_1 & internal_clock_stable; })) {
        return EIO;
    }

    // 4. Set SD Clock Enable in the Clock Control register to 1
    m_registers->host_configuration_1 = m_registers->host_configuration_1 | sd_clock_enable;

    return {};
}

void SDHostController::sd_clock_stop()
{
    // SDHC 3.2.2: "SD Clock Stop Sequence"

    // 1. Set SD Clock Enable in the Clock Control register to 0
    m_registers->host_configuration_1 = m_registers->host_configuration_1 & ~sd_clock_enable;
}

ErrorOr<void> SDHostController::sd_clock_frequency_change(u32 new_frequency)
{
    // SDHC 3.2.3: "SD Clock Frequency Change Sequence"

    // 1. Execute the SD Clock Stop Sequence
    sd_clock_stop();

    // 2. Execute the SD Clock Supply Sequence
    return sd_clock_supply(new_frequency);
}

ErrorOr<void> SDHostController::reset_host_controller()
{
    m_registers->host_configuration_0 = 0;
    m_registers->host_configuration_1 = m_registers->host_configuration_1 | software_reset_for_all;
    if (!retry_with_timeout(
            [&] {
                return (m_registers->host_configuration_1 & software_reset_for_all) == 0;
            })) {
        return EIO;
    }

    return {};
}

ErrorOr<void> SDHostController::transaction_control_with_data_transfer_using_the_dat_line_without_dma(
    SD::Command const& command,
    u32 argument,
    u32 block_count,
    u32 block_size,
    UserOrKernelBuffer buf,
    DataTransferType data_transfer_type)
{
    // SDHC 3.7.2: "Transaction Control with Data Transfer Using DAT Line (without DMA)"

    // 1. Set the value corresponding to the executed data byte length of one block to Block Size register.
    // 2. Set the value corresponding to the executed data block count to Block Count register in accordance with Table 2-8.
    m_registers->block_size_and_block_count = (block_count << 16) | block_size;

    // 3. Set the argument value to Argument 1 register.
    m_registers->argument_1 = argument;

    // 4. Set the value to the Transfer Mode register. The host driver
    // determines Multi / Single Block
    //    Select, Block Count Enable, Data Transfer Direction, Auto CMD12 Enable
    //    and DMA Enable. Multi / Single Block Select and Block Count Enable are
    //    determined according to Table 2-8. (NOTE: We assume `cmd` already has
    //    the correct flags set)
    // 5. Set the value to Command register.
    m_registers->transfer_mode_and_command = command.raw;

    // 6. Then, wait for the Command Complete Interrupt.
    if (!retry_with_timeout(
            [&]() { return m_registers->interrupt_status & command_complete; })) {
        return EIO;
    }

    // 7. Write 1 to the Command Complete in the Normal Interrupt Status
    // register for clearing this bit.
    m_registers->interrupt_status = command_complete;

    // 8. Read Response register and get necessary information of the issued
    // command
    //    (FIXME: Return the value for better error handling)

    // 9. In the case where this sequence is for write to a card, go to step
    // (10).
    //    In case of read from a card, go to step (14).
    if (data_transfer_type == DataTransferType::Write) {

        for (u32 i = 0; i < block_count; i++) {
            // 10. Then wait for Buffer Write Ready Interrupt.
            if (!retry_with_timeout(
                    [&]() {
                        return m_registers->interrupt_status & buffer_write_ready;
                    })) {
                return EIO;
            }

            // 11. Write 1 to the Buffer Write Ready in the Normal Interrupt Status register for clearing this bit.
            m_registers->interrupt_status = buffer_write_ready;

            // 12. Write block data (in according to the number of bytes specified at the step (1)) to Buffer Data Port register.
            u32 temp;
            for (u32 j = 0; j < block_size / sizeof(u32); j++) {
                TRY(buf.read(&temp, i * block_size + sizeof(u32) * j, sizeof(u32)));
                m_registers->buffer_data_port = temp;
            }

            // 13. Repeat until all blocks are sent and then go to step (18).
        }
    } else {
        for (u32 i = 0; i < block_count; i++) {
            // 14. Then wait for the Buffer Read Ready Interrupt.
            if (!retry_with_timeout([&]() { return m_registers->interrupt_status & buffer_read_ready; })) {
                return EIO;
            }

            // 15. Write 1 to the Buffer Read Ready in the Normal Interrupt Status
            // register for clearing this bit.
            m_registers->interrupt_status = buffer_read_ready;

            // 16. Read block data (in according to the number of bytes specified at
            // the step (1)) from the Buffer Data Port register
            u32 temp;
            for (u32 j = 0; j < block_size / sizeof(u32); j++) {
                temp = m_registers->buffer_data_port;
                TRY(buf.write(&temp, i * block_size + sizeof(u32) * j, sizeof(u32)));
            }

            // 17. Repeat until all blocks are received and then go to step (18).
        }
    }

    // 18. If this sequence is for Single or Multiple Block Transfer, go to step
    // (19). In case of Infinite Block Transfer, go to step (21)

    // 19. Wait for Transfer Complete Interrupt.
    if (!retry_with_timeout(
            [&]() { return m_registers->interrupt_status & transfer_complete; })) {
        return EIO;
    }

    // 20. Write 1 to the Transfer Complete in the Normal Interrupt Status
    // register for clearing this bit
    m_registers->interrupt_status = transfer_complete;

    return {};
}

ErrorOr<void> SDHostController::read_block(Badge<SDMemoryCard>, u32 block_address, u32 block_count, UserOrKernelBuffer out)
{
    VERIFY(is_card_inserted());

    if (block_count > 1) {
        return transaction_control_with_data_transfer_using_the_dat_line_without_dma(
            SD::Commands::read_multiple_block,
            block_address,
            block_count,
            block_len,
            out,
            DataTransferType::Read);
    }

    return transaction_control_with_data_transfer_using_the_dat_line_without_dma(
        SD::Commands::read_single_block,
        block_address,
        block_count,
        block_len,
        out,
        DataTransferType::Read);
}

ErrorOr<void> SDHostController::write_block(Badge<SDMemoryCard>, u32 block_address, u32 block_count, UserOrKernelBuffer in)
{
    VERIFY(is_card_inserted());

    if (block_count > 1) {
        return transaction_control_with_data_transfer_using_the_dat_line_without_dma(
            SD::Commands::write_multiple_block,
            block_address,
            block_count,
            block_len,
            in,
            DataTransferType::Write);
    }
    return transaction_control_with_data_transfer_using_the_dat_line_without_dma(
        SD::Commands::write_single_block,
        block_address,
        block_count,
        block_len,
        in,
        DataTransferType::Write);
}

ErrorOr<SD::SDConfigurationRegister> SDHostController::retrieve_sd_configuration_register(u32 relative_card_address)
{
    SD::SDConfigurationRegister scr;

    TRY(issue_command(SD::Commands::app_cmd, relative_card_address));
    TRY(wait_for_response());
    TRY(transaction_control_with_data_transfer_using_the_dat_line_without_dma(
        SD::Commands::app_send_scr,
        0, 1, 8,
        UserOrKernelBuffer::for_kernel_buffer(scr.raw), DataTransferType::Read));

    return scr;
}

ErrorOr<u32> SDHostController::retrieve_sd_clock_frequency()
{
    const i64 one_mhz = 1'000'000;
    return { m_registers->capabilities.base_clock_frequency * one_mhz };
}

// PLSS Table 4-43 : Card Status Field/Command
bool SDHostController::card_status_contains_errors(SD::Command const& command, u32 resp)
{
    SD::CardStatus status;
    // PLSS 4.9.5 R6
    if (command.index == SD::CommandIndex::SendRelativeAddr) {
        status.raw = (resp & 0x1fff) | ((resp & 0x2000) << 6) | ((resp & 0x4000) << 8) | ((resp & 0x8000) << 8);
    } else {
        status.raw = resp;
    }

    bool common_errors = status.error || status.cc_error || status.card_ecc_failed || status.illegal_command || status.com_crc_error || status.lock_unlock_failed || status.card_is_locked || status.wp_violation || status.erase_param || status.csd_overwrite;

    bool contains_errors = false;
    switch (command.index) {
    case SD::CommandIndex::SendRelativeAddr:
        if (status.error || status.illegal_command || status.com_crc_error) {
            contains_errors = true;
        }
        break;
    case SD::CommandIndex::SelectCard:
        if (common_errors) {
            contains_errors = true;
        }
        break;
    case SD::CommandIndex::SetBlockLen:
        if (common_errors || status.block_len_error) {
            contains_errors = true;
        }
        break;
    case SD::CommandIndex::ReadSingleBlock:
    case SD::CommandIndex::ReadMultipleBlock:
        if (common_errors || status.address_error || status.out_of_range) {
            contains_errors = true;
        }
        break;
    case SD::CommandIndex::WriteSingleBlock:
    case SD::CommandIndex::WriteMultipleBlock:
        if (common_errors || status.block_len_error || status.address_error || status.out_of_range) {
            contains_errors = true;
        }
        break;
    case SD::CommandIndex::AppSendScr:
        if (common_errors) {
            contains_errors = true;
        }
        break;
    case SD::CommandIndex::AppCmd:
        if (common_errors) {
            contains_errors = true;
        }
        break;
    default:
        break;
    }

    return contains_errors;
}

}
