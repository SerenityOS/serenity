/*
 * Copyright (c) 2021-2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Arch/Delay.h>
#include <Kernel/Devices/Audio/AC97/AC97.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Memory/AnonymousVMObject.h>

namespace Kernel {

static constexpr int buffer_descriptor_list_max_entries = 32;

static constexpr u16 pcm_fixed_sample_rate = 48000;

// Valid output range - with double-rate enabled, sample rate can go up to 96kHZ
static constexpr u16 pcm_sample_rate_minimum = 8000;
static constexpr u16 pcm_sample_rate_maximum = 48000;

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<AudioController>> AC97::create(PCI::DeviceIdentifier const& pci_device_identifier)
{
    auto mixer_io_window = TRY(IOWindow::create_for_pci_device_bar(pci_device_identifier, PCI::HeaderType0BaseRegister::BAR0));
    auto bus_io_window = TRY(IOWindow::create_for_pci_device_bar(pci_device_identifier, PCI::HeaderType0BaseRegister::BAR1));

    auto pcm_out_channel_io_window = TRY(bus_io_window->create_from_io_window_with_offset(NativeAudioBusChannel::PCMOutChannel));
    auto pcm_out_channel = TRY(AC97Channel::create_with_parent_pci_device(pci_device_identifier.address(), "PCMOut"sv, move(pcm_out_channel_io_window)));

    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) AC97(pci_device_identifier, move(pcm_out_channel), move(mixer_io_window), move(bus_io_window))));
}

UNMAP_AFTER_INIT ErrorOr<bool> AC97::probe(PCI::DeviceIdentifier const& device_identifier)
{
    VERIFY(device_identifier.class_code() == PCI::ClassID::Multimedia);

    // TODO: Check pci ids.

    if (PCI::get_BAR_space_size(device_identifier, PCI::HeaderType0BaseRegister::BAR0) <= NativeAudioMixerRegister::MaxUsedMixerOffset)
        return Error::from_errno(EIO);

    // BAR registers are 32-bit. So if BAR0 is 64-bit then
    // it occupies BAR0 and BAR1 and hence BAR1 isn't present on its own.
    u64 pci_bar0_value = PCI::get_BAR(device_identifier, PCI::HeaderType0BaseRegister::BAR0);
    if (PCI::get_BAR_space_type(pci_bar0_value) == PCI::BARSpaceType::Memory64BitSpace)
        return Error::from_errno(EIO);

    if (PCI::get_BAR_space_size(device_identifier, PCI::HeaderType0BaseRegister::BAR1) <= NativeAudioBusRegister::MaxUsedBusOffset)
        return Error::from_errno(EIO);

    return device_identifier.subclass_code() == PCI::Multimedia::SubclassID::Audio;
}

UNMAP_AFTER_INIT AC97::AC97(PCI::DeviceIdentifier const& pci_device_identifier, NonnullOwnPtr<AC97Channel> pcm_out_channel, NonnullOwnPtr<IOWindow> mixer_io_window, NonnullOwnPtr<IOWindow> bus_io_window)
    : PCI::Device(const_cast<PCI::DeviceIdentifier&>(pci_device_identifier))
    , IRQHandler(pci_device_identifier.interrupt_line().value())
    , m_mixer_io_window(move(mixer_io_window))
    , m_bus_io_window(move(bus_io_window))
    , m_pcm_out_channel(move(pcm_out_channel))
{
}

UNMAP_AFTER_INIT AC97::~AC97() = default;

bool AC97::handle_irq()
{
    auto pcm_out_status = m_pcm_out_channel->io_window().read16(AC97Channel::Register::Status);
    dbgln_if(AC97_DEBUG, "AC97 @ {}: interrupt received - status: {:#05b}", device_identifier().address(), pcm_out_status);

    bool is_dma_halted = (pcm_out_status & AudioStatusRegisterFlag::DMAControllerHalted) > 0;
    bool current_equals_last_valid = (pcm_out_status & AudioStatusRegisterFlag::CurrentEqualsLastValid) > 0;
    bool is_completion_interrupt = (pcm_out_status & AudioStatusRegisterFlag::BufferCompletionInterruptStatus) > 0;
    bool is_fifo_error = (pcm_out_status & AudioStatusRegisterFlag::FIFOError) > 0;
    VERIFY(!is_fifo_error);

    // If there is no buffer completion, we're not going to do anything
    if (!is_completion_interrupt)
        return false;

    // On interrupt, we need to reset PCM interrupt flags by setting their bits
    pcm_out_status = AudioStatusRegisterFlag::LastValidBufferCompletionInterrupt
        | AudioStatusRegisterFlag::BufferCompletionInterruptStatus
        | AudioStatusRegisterFlag::FIFOError;
    m_pcm_out_channel->io_window().write16(AC97Channel::Register::Status, pcm_out_status);

    if (is_dma_halted) {
        VERIFY(current_equals_last_valid);
        m_pcm_out_channel->handle_dma_stopped();
    }

    if (!m_irq_queue.is_empty())
        m_irq_queue.wake_all();

    return true;
}

UNMAP_AFTER_INIT ErrorOr<void> AC97::initialize(Badge<AudioManagement>)
{
    dbgln_if(AC97_DEBUG, "AC97 @ {}: mixer base: {:#04x}", device_identifier().address(), m_mixer_io_window);
    dbgln_if(AC97_DEBUG, "AC97 @ {}: bus base: {:#04x}", device_identifier().address(), m_bus_io_window);

    // Read out AC'97 codec revision and vendor
    auto extended_audio_id = m_mixer_io_window->read16(NativeAudioMixerRegister::ExtendedAudioID);
    m_codec_revision = static_cast<AC97Revision>(((extended_audio_id & ExtendedAudioMask::Revision) >> 10) & 0b11);
    dbgln_if(AC97_DEBUG, "AC97 @ {}: codec revision {:#02b}", device_identifier().address(), to_underlying(m_codec_revision));
    if (m_codec_revision == AC97Revision::Reserved)
        return ENOTSUP;

    // Report vendor / device ID
    u32 vendor_id = m_mixer_io_window->read16(NativeAudioMixerRegister::VendorID1) << 16 | m_mixer_io_window->read16(NativeAudioMixerRegister::VendorID2);
    dmesgln_pci(*this, "Vendor ID: {:#8x}", vendor_id);

    // Bus cold reset, enable interrupts
    enable_pin_based_interrupts();
    PCI::enable_bus_mastering(device_identifier());
    auto control = m_bus_io_window->read32(NativeAudioBusRegister::GlobalControl);
    control |= GlobalControlFlag::GPIInterruptEnable;
    control |= GlobalControlFlag::AC97ColdReset;
    m_bus_io_window->write32(NativeAudioBusRegister::GlobalControl, control);

    // Reset mixer
    m_mixer_io_window->write16(NativeAudioMixerRegister::Reset, 1);

    // Enable variable and double rate PCM audio if supported
    auto extended_audio_status = m_mixer_io_window->read16(NativeAudioMixerRegister::ExtendedAudioStatusControl);
    if ((extended_audio_id & ExtendedAudioMask::VariableRatePCMAudio) > 0) {
        extended_audio_status |= ExtendedAudioStatusControlFlag::VariableRateAudio;
        m_variable_rate_pcm_supported = true;
    }
    if (!m_variable_rate_pcm_supported) {
        extended_audio_status &= ~ExtendedAudioStatusControlFlag::DoubleRateAudio;
    } else if ((extended_audio_id & ExtendedAudioMask::DoubleRatePCMAudio) > 0) {
        extended_audio_status |= ExtendedAudioStatusControlFlag::DoubleRateAudio;
        m_double_rate_pcm_enabled = true;
    }
    m_mixer_io_window->write16(NativeAudioMixerRegister::ExtendedAudioStatusControl, extended_audio_status);

    // Get the device's current sample rate
    m_sample_rate = read_pcm_output_sample_rate();

    // Left and right volume of 0 means attenuation of 0 dB
    set_master_output_volume(0, 0, Muted::No);
    set_pcm_output_volume(0, 0, Muted::No);

    m_pcm_out_channel->reset();
    enable_irq();

    m_audio_channel = TRY(AudioChannel::create(*this, 0));
    return {};
}

void AC97::set_master_output_volume(u8 left_channel, u8 right_channel, Muted mute)
{
    u16 volume_value = ((right_channel & 63) << 0)
        | ((left_channel & 63) << 8)
        | ((mute == Muted::Yes ? 1 : 0) << 15);
    m_mixer_io_window->write16(NativeAudioMixerRegister::SetMasterOutputVolume, volume_value);
}

u32 AC97::read_pcm_output_sample_rate()
{
    auto const double_rate_shift = m_double_rate_pcm_enabled ? 1 : 0;
    return static_cast<u32>(m_mixer_io_window->read16(NativeAudioMixerRegister::PCMFrontDACRate)) << double_rate_shift;
}

ErrorOr<void> AC97::set_pcm_output_sample_rate(u32 sample_rate)
{
    if (m_sample_rate == sample_rate)
        return {};

    auto const double_rate_shift = m_double_rate_pcm_enabled ? 1 : 0;
    auto shifted_sample_rate = sample_rate >> double_rate_shift;
    if (!m_variable_rate_pcm_supported && shifted_sample_rate != pcm_fixed_sample_rate)
        return ENOTSUP;
    if (shifted_sample_rate < pcm_sample_rate_minimum || shifted_sample_rate > pcm_sample_rate_maximum)
        return ENOTSUP;

    m_mixer_io_window->write16(NativeAudioMixerRegister::PCMFrontDACRate, shifted_sample_rate);
    m_sample_rate = read_pcm_output_sample_rate();

    dmesgln_pci(*this, "PCM front DAC rate set to {} Hz", m_sample_rate);

    // Setting the sample rate stops a running DMA engine, so restart it
    if (m_pcm_out_channel->dma_running())
        m_pcm_out_channel->start_dma();

    return {};
}

void AC97::set_pcm_output_volume(u8 left_channel, u8 right_channel, Muted mute)
{
    u16 volume_value = ((right_channel & 31) << 0)
        | ((left_channel & 31) << 8)
        | ((mute == Muted::Yes ? 1 : 0) << 15);
    m_mixer_io_window->write16(NativeAudioMixerRegister::SetPCMOutputVolume, volume_value);
}

RefPtr<AudioChannel> AC97::audio_channel(u32 index) const
{
    if (index == 0)
        return m_audio_channel;
    return {};
}

ErrorOr<void> AC97::set_pcm_output_sample_rate(size_t channel_index, u32 samples_per_second_rate)
{
    if (channel_index != 0)
        return ENODEV;
    TRY(set_pcm_output_sample_rate(samples_per_second_rate));
    return {};
}

ErrorOr<u32> AC97::get_pcm_output_sample_rate(size_t channel_index)
{
    if (channel_index != 0)
        return Error::from_errno(ENODEV);
    return m_sample_rate;
}

ErrorOr<size_t> AC97::write(size_t channel_index, UserOrKernelBuffer const& data, size_t length)
{
    if (channel_index != 0)
        return Error::from_errno(ENODEV);

    if (!m_output_buffer) {
        // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
        m_output_buffer = TRY(MM.allocate_dma_buffer_pages(m_output_buffer_page_count * PAGE_SIZE, "AC97 Output buffer"sv, Memory::Region::Access::Write, Memory::MemoryType::IO));
    }

    if (!m_buffer_descriptor_list) {
        size_t buffer_descriptor_list_size = buffer_descriptor_list_max_entries * sizeof(BufferDescriptorListEntry);
        buffer_descriptor_list_size = TRY(Memory::page_round_up(buffer_descriptor_list_size));
        // FIXME: Synchronize DMA buffer accesses correctly and set the MemoryType to NonCacheable.
        m_buffer_descriptor_list = TRY(MM.allocate_dma_buffer_pages(buffer_descriptor_list_size, "AC97 Buffer Descriptor List"sv, Memory::Region::Access::Write, Memory::MemoryType::IO));
    }

    Checked<size_t> remaining = length;
    size_t offset = 0;
    while (remaining > static_cast<size_t>(0)) {
        TRY(write_single_buffer(data, offset, min(remaining.value(), PAGE_SIZE)));
        offset += PAGE_SIZE;
        remaining.saturating_sub(PAGE_SIZE);
    }

    return length;
}

ErrorOr<void> AC97::write_single_buffer(UserOrKernelBuffer const& data, size_t offset, size_t length)
{
    VERIFY(length <= PAGE_SIZE);

    {
        // Block until we can write into an unused buffer
        InterruptDisabler disabler;
        do {
            auto pcm_out_status = m_pcm_out_channel->io_window().read16(AC97Channel::Register::Status);
            auto current_index = m_pcm_out_channel->io_window().read8(AC97Channel::Register::CurrentIndexValue);
            int last_valid_index = m_pcm_out_channel->io_window().read8(AC97Channel::Register::LastValidIndex);

            auto head_distance = last_valid_index - current_index;
            if (head_distance < 0)
                head_distance += buffer_descriptor_list_max_entries;
            if (m_pcm_out_channel->dma_running())
                ++head_distance;

            // Current index has _passed_ last valid index - move our list index up
            if (head_distance > m_output_buffer_page_count) {
                m_buffer_descriptor_list_index = current_index + 1;
                break;
            }

            // There is room for our data
            if (head_distance < m_output_buffer_page_count)
                break;

            dbgln_if(AC97_DEBUG, "AC97 @ {}: waiting on interrupt - status: {:#05b} CI: {} LVI: {}", device_identifier().address(), pcm_out_status, current_index, last_valid_index);
            m_irq_queue.wait_forever("AC97"sv);
        } while (m_pcm_out_channel->dma_running());
    }
    // Copy data from userspace into one of our buffers
    TRY(data.read(m_output_buffer->vaddr_from_page_index(m_output_buffer_page_index).as_ptr(), offset, length));

    // Write the next entry to the buffer descriptor list
    u16 number_of_samples = length / sizeof(u16);
    auto list_entries = reinterpret_cast<BufferDescriptorListEntry*>(m_buffer_descriptor_list->vaddr().get());
    auto list_entry = &list_entries[m_buffer_descriptor_list_index];
    list_entry->buffer_pointer = static_cast<u32>(m_output_buffer->physical_page(m_output_buffer_page_index)->paddr().get());
    list_entry->control_and_length = number_of_samples | BufferDescriptorListEntryFlags::InterruptOnCompletion;

    auto buffer_address = static_cast<u32>(m_buffer_descriptor_list->physical_page(0)->paddr().get());
    m_pcm_out_channel->set_last_valid_index(buffer_address, m_buffer_descriptor_list_index);

    if (!m_pcm_out_channel->dma_running())
        m_pcm_out_channel->start_dma();

    m_output_buffer_page_index = (m_output_buffer_page_index + 1) % m_output_buffer_page_count;
    m_buffer_descriptor_list_index = (m_buffer_descriptor_list_index + 1) % buffer_descriptor_list_max_entries;

    return {};
}

ErrorOr<NonnullOwnPtr<AC97::AC97Channel>> AC97::AC97Channel::create_with_parent_pci_device(PCI::Address pci_device_address, StringView name, NonnullOwnPtr<IOWindow> channel_io_base)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) AC97::AC97Channel(pci_device_address, name, move(channel_io_base)));
}

void AC97::AC97Channel::handle_dma_stopped()
{
    dbgln_if(AC97_DEBUG, "AC97 @ {}: channel {}: DMA engine has stopped", m_device_pci_address, name());
    m_dma_running.with([this](auto& dma_running) {
        // NOTE: QEMU might send spurious interrupts while we're not running, so we don't want to panic here.
        if (!dma_running)
            dbgln("AC97 @ {}: received DMA interrupt while it wasn't running", m_device_pci_address);
        dma_running = false;
    });
}

void AC97::AC97Channel::reset()
{
    dbgln_if(AC97_DEBUG, "AC97 @ {}: channel {}: resetting", m_device_pci_address, name());

    m_channel_io_window->write8(Register::Control, AudioControlRegisterFlag::ResetRegisters);

    while ((m_channel_io_window->read8(Register::Control) & AudioControlRegisterFlag::ResetRegisters) > 0)
        microseconds_delay(50);

    m_dma_running.with([](auto& dma_running) {
        dma_running = false;
    });
}

void AC97::AC97Channel::set_last_valid_index(u32 buffer_address, u8 last_valid_index)
{
    dbgln_if(AC97_DEBUG, "AC97 @ {}: channel {}: setting buffer address: {:#x} LVI: {}", m_device_pci_address, name(), buffer_address, last_valid_index);

    m_channel_io_window->write32(Register::BufferDescriptorListBaseAddress, buffer_address);
    m_channel_io_window->write8(Register::LastValidIndex, last_valid_index);
}

void AC97::AC97Channel::start_dma()
{
    dbgln_if(AC97_DEBUG, "AC97 @ {}: channel {}: starting DMA engine", m_device_pci_address, name());

    auto control = m_channel_io_window->read8(Register::Control);
    control |= AudioControlRegisterFlag::RunPauseBusMaster;
    control |= AudioControlRegisterFlag::FIFOErrorInterruptEnable;
    control |= AudioControlRegisterFlag::InterruptOnCompletionEnable;
    m_channel_io_window->write8(Register::Control, control);

    m_dma_running.with([](auto& dma_running) {
        dma_running = true;
    });
}

}
