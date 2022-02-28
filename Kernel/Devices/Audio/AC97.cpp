/*
 * Copyright (c) 2021-2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Audio/AC97.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Memory/AnonymousVMObject.h>

namespace Kernel {

static constexpr int buffer_descriptor_list_max_entries = 32;

static constexpr u16 pcm_default_sample_rate = 44100;
static constexpr u16 pcm_fixed_sample_rate = 48000;

// Valid output range - with double-rate enabled, sample rate can go up to 96kHZ
static constexpr u16 pcm_sample_rate_minimum = 8000;
static constexpr u16 pcm_sample_rate_maximum = 48000;

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<AC97>> AC97::try_create(PCI::DeviceIdentifier const& pci_device_identifier)
{
    auto ac97 = adopt_nonnull_ref_or_enomem(new (nothrow) AC97(pci_device_identifier));
    if (!ac97.is_error())
        TRY(ac97.value()->initialize());
    return ac97;
}

UNMAP_AFTER_INIT AC97::AC97(PCI::DeviceIdentifier const& pci_device_identifier)
    : PCI::Device(pci_device_identifier.address())
    , IRQHandler(pci_device_identifier.interrupt_line().value())
    , m_io_mixer_base(PCI::get_BAR0(pci_address()) & ~1)
    , m_io_bus_base(PCI::get_BAR1(pci_address()) & ~1)
    , m_pcm_out_channel(channel("PCMOut"sv, NativeAudioBusChannel::PCMOutChannel))
{
}

UNMAP_AFTER_INIT AC97::~AC97()
{
}

bool AC97::handle_irq(RegisterState const&)
{
    auto pcm_out_status_register = m_pcm_out_channel.reg(AC97Channel::Register::Status);
    auto pcm_out_status = pcm_out_status_register.in<u16>();
    dbgln_if(AC97_DEBUG, "AC97 @ {}: interrupt received - status: {:#05b}", pci_address(), pcm_out_status);

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
    pcm_out_status_register.out(pcm_out_status);

    if (is_dma_halted) {
        VERIFY(current_equals_last_valid);
        m_pcm_out_channel.handle_dma_stopped();
    }

    if (!m_irq_queue.is_empty())
        m_irq_queue.wake_all();

    return true;
}

UNMAP_AFTER_INIT ErrorOr<void> AC97::initialize()
{
    dbgln_if(AC97_DEBUG, "AC97 @ {}: mixer base: {:#04x}", pci_address(), m_io_mixer_base.get());
    dbgln_if(AC97_DEBUG, "AC97 @ {}: bus base: {:#04x}", pci_address(), m_io_bus_base.get());

    enable_pin_based_interrupts();
    PCI::enable_bus_mastering(pci_address());

    // Bus cold reset, enable interrupts
    auto control = m_io_bus_base.offset(NativeAudioBusRegister::GlobalControl).in<u32>();
    control |= GlobalControlFlag::GPIInterruptEnable;
    control |= GlobalControlFlag::AC97ColdReset;
    m_io_bus_base.offset(NativeAudioBusRegister::GlobalControl).out(control);

    // Reset mixer
    m_io_mixer_base.offset(NativeAudioMixerRegister::Reset).out<u16>(1);

    // Read out AC'97 codec revision
    auto extended_audio_id = m_io_mixer_base.offset(NativeAudioMixerRegister::ExtendedAudioID).in<u16>();
    m_codec_revision = static_cast<AC97Revision>(((extended_audio_id & ExtendedAudioMask::Revision) >> 10) & 0b11);
    dbgln_if(AC97_DEBUG, "AC97 @ {}: codec revision {:#02b}", pci_address(), to_underlying(m_codec_revision));
    if (m_codec_revision == AC97Revision::Reserved)
        return ENOTSUP;

    // Enable variable and double rate PCM audio if supported
    auto extended_audio_status_control_register = m_io_mixer_base.offset(NativeAudioMixerRegister::ExtendedAudioStatusControl);
    auto extended_audio_status = extended_audio_status_control_register.in<u16>();
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
    extended_audio_status_control_register.out(extended_audio_status);

    TRY(set_pcm_output_sample_rate(m_variable_rate_pcm_supported ? pcm_default_sample_rate : pcm_fixed_sample_rate));

    // Left and right volume of 0 means attenuation of 0 dB
    set_master_output_volume(0, 0, Muted::No);
    set_pcm_output_volume(0, 0, Muted::No);

    m_pcm_out_channel.reset();
    enable_irq();
    return {};
}

void AC97::set_master_output_volume(u8 left_channel, u8 right_channel, Muted mute)
{
    u16 volume_value = ((right_channel & 63) << 0)
        | ((left_channel & 63) << 8)
        | ((mute == Muted::Yes ? 1 : 0) << 15);
    m_io_mixer_base.offset(NativeAudioMixerRegister::SetMasterOutputVolume).out(volume_value);
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

    auto pcm_front_dac_rate_register = m_io_mixer_base.offset(NativeAudioMixerRegister::PCMFrontDACRate);
    pcm_front_dac_rate_register.out<u16>(shifted_sample_rate);
    m_sample_rate = static_cast<u32>(pcm_front_dac_rate_register.in<u16>()) << double_rate_shift;

    dbgln("AC97 @ {}: PCM front DAC rate set to {} Hz", pci_address(), m_sample_rate);

    // Setting the sample rate stops a running DMA engine, so restart it
    if (m_pcm_out_channel.dma_running())
        m_pcm_out_channel.start_dma();

    return {};
}

void AC97::set_pcm_output_volume(u8 left_channel, u8 right_channel, Muted mute)
{
    u16 volume_value = ((right_channel & 31) << 0)
        | ((left_channel & 31) << 8)
        | ((mute == Muted::Yes ? 1 : 0) << 15);
    m_io_mixer_base.offset(NativeAudioMixerRegister::SetPCMOutputVolume).out(volume_value);
}

RefPtr<AudioChannel> AC97::audio_channel(u32 index) const
{
    if (index == 0)
        return m_audio_channel;
    return {};
}

void AC97::detect_hardware_audio_channels(Badge<AudioManagement>)
{
    m_audio_channel = AudioChannel::must_create(*this, 0);
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

    if (!m_output_buffer)
        m_output_buffer = TRY(MM.allocate_dma_buffer_pages(m_output_buffer_page_count * PAGE_SIZE, "AC97 Output buffer"sv, Memory::Region::Access::Write));

    if (!m_buffer_descriptor_list) {
        size_t buffer_descriptor_list_size = buffer_descriptor_list_max_entries * sizeof(BufferDescriptorListEntry);
        buffer_descriptor_list_size = TRY(Memory::page_round_up(buffer_descriptor_list_size));
        m_buffer_descriptor_list = TRY(MM.allocate_dma_buffer_pages(buffer_descriptor_list_size, "AC97 Buffer Descriptor List"sv, Memory::Region::Access::Write));
    }

    auto remaining = length;
    size_t offset = 0;
    while (remaining > 0) {
        TRY(write_single_buffer(data, offset, min(remaining, PAGE_SIZE)));
        offset += PAGE_SIZE;
        remaining -= PAGE_SIZE;
    }

    return length;
}

ErrorOr<void> AC97::write_single_buffer(UserOrKernelBuffer const& data, size_t offset, size_t length)
{
    VERIFY(length <= PAGE_SIZE);

    // Block until we can write into an unused buffer
    cli();
    do {
        auto pcm_out_status = m_pcm_out_channel.reg(AC97Channel::Register::Status).in<u16>();
        auto current_index = m_pcm_out_channel.reg(AC97Channel::Register::CurrentIndexValue).in<u8>();
        int last_valid_index = m_pcm_out_channel.reg(AC97Channel::Register::LastValidIndex).in<u8>();

        auto head_distance = last_valid_index - current_index;
        if (head_distance < 0)
            head_distance += buffer_descriptor_list_max_entries;
        if (m_pcm_out_channel.dma_running())
            ++head_distance;

        // Current index has _passed_ last valid index - move our list index up
        if (head_distance > m_output_buffer_page_count) {
            m_buffer_descriptor_list_index = current_index + 1;
            break;
        }

        // There is room for our data
        if (head_distance < m_output_buffer_page_count)
            break;

        dbgln_if(AC97_DEBUG, "AC97 @ {}: waiting on interrupt - status: {:#05b} CI: {} LVI: {}", pci_address(), pcm_out_status, current_index, last_valid_index);
        m_irq_queue.wait_forever("AC97"sv);
    } while (m_pcm_out_channel.dma_running());
    sti();

    // Copy data from userspace into one of our buffers
    TRY(data.read(m_output_buffer->vaddr_from_page_index(m_output_buffer_page_index).as_ptr(), offset, length));

    // Write the next entry to the buffer descriptor list
    u16 number_of_samples = length / sizeof(u16);
    auto list_entries = reinterpret_cast<BufferDescriptorListEntry*>(m_buffer_descriptor_list->vaddr().get());
    auto list_entry = &list_entries[m_buffer_descriptor_list_index];
    list_entry->buffer_pointer = static_cast<u32>(m_output_buffer->physical_page(m_output_buffer_page_index)->paddr().get());
    list_entry->control_and_length = number_of_samples | BufferDescriptorListEntryFlags::InterruptOnCompletion;

    auto buffer_address = static_cast<u32>(m_buffer_descriptor_list->physical_page(0)->paddr().get());
    m_pcm_out_channel.set_last_valid_index(buffer_address, m_buffer_descriptor_list_index);

    if (!m_pcm_out_channel.dma_running())
        m_pcm_out_channel.start_dma();

    m_output_buffer_page_index = (m_output_buffer_page_index + 1) % m_output_buffer_page_count;
    m_buffer_descriptor_list_index = (m_buffer_descriptor_list_index + 1) % buffer_descriptor_list_max_entries;

    return {};
}

void AC97::AC97Channel::handle_dma_stopped()
{
    dbgln_if(AC97_DEBUG, "AC97 @ {}: channel {}: DMA engine has stopped", m_device.pci_address(), name());
    VERIFY(m_dma_running);
    m_dma_running = false;
}

void AC97::AC97Channel::reset()
{
    dbgln_if(AC97_DEBUG, "AC97 @ {}: channel {}: resetting", m_device.pci_address(), name());

    auto control_register = reg(Register::Control);
    control_register.out(AudioControlRegisterFlag::ResetRegisters);

    while ((control_register.in<u8>() & AudioControlRegisterFlag::ResetRegisters) > 0)
        IO::delay(50);

    m_dma_running = false;
}

void AC97::AC97Channel::set_last_valid_index(u32 buffer_address, u8 last_valid_index)
{
    dbgln_if(AC97_DEBUG, "AC97 @ {}: channel {}: setting buffer address: {:#x} LVI: {}", m_device.pci_address(), name(), buffer_address, last_valid_index);

    reg(Register::BufferDescriptorListBaseAddress).out(buffer_address);
    reg(Register::LastValidIndex).out(last_valid_index);
}

void AC97::AC97Channel::start_dma()
{
    dbgln_if(AC97_DEBUG, "AC97 @ {}: channel {}: starting DMA engine", m_device.pci_address(), name());

    auto control_register = reg(Register::Control);
    auto control = control_register.in<u8>();
    control |= AudioControlRegisterFlag::RunPauseBusMaster;
    control |= AudioControlRegisterFlag::FIFOErrorInterruptEnable;
    control |= AudioControlRegisterFlag::InterruptOnCompletionEnable;
    control_register.out(control);

    m_dma_running = true;
}

}
