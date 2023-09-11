/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Controller.h"
#include <AK/Optional.h>
#include <AK/Vector.h>
#include <Kernel/Arch/Delay.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Devices/Audio/IntelHDA/Codec.h>
#include <Kernel/Devices/Audio/IntelHDA/InterruptHandler.h>
#include <Kernel/Devices/Audio/IntelHDA/Stream.h>
#include <Kernel/Devices/Audio/IntelHDA/Timing.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel::Audio::IntelHDA {

UNMAP_AFTER_INIT ErrorOr<bool> Controller::probe(PCI::DeviceIdentifier const& device_identifier)
{
    VERIFY(device_identifier.class_code() == PCI::ClassID::Multimedia);
    return device_identifier.subclass_code() == PCI::Multimedia::SubclassID::HDACompatible;
}

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<AudioController>> Controller::create(PCI::DeviceIdentifier const& pci_device_identifier)
{
    auto controller_io_window = TRY(IOWindow::create_for_pci_device_bar(pci_device_identifier, PCI::HeaderType0BaseRegister::BAR0));
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Controller(pci_device_identifier, move(controller_io_window))));
}

UNMAP_AFTER_INIT Controller::Controller(PCI::DeviceIdentifier const& pci_device_identifier, NonnullOwnPtr<IOWindow> controller_io_window)
    : PCI::Device(const_cast<PCI::DeviceIdentifier&>(pci_device_identifier))
    , m_controller_io_window(move(controller_io_window))
{
}

UNMAP_AFTER_INIT ErrorOr<void> Controller::initialize(Badge<AudioManagement>)
{
    // Enable DMA and interrupts
    PCI::enable_bus_mastering(device_identifier());
    m_interrupt_handler = TRY(InterruptHandler::create(*this));

    // 3.3.3, 3.3.4: Controller version
    auto version_minor = m_controller_io_window->read8(ControllerRegister::VersionMinor);
    auto version_major = m_controller_io_window->read8(ControllerRegister::VersionMajor);
    dmesgln_pci(*this, "Intel High Definition Audio specification v{}.{}", version_major, version_minor);
    if (version_major != 1 || version_minor != 0)
        return ENOTSUP;

    // 3.3.2: Read capabilities
    u16 capabilities = m_controller_io_window->read16(ControllerRegister::GlobalCapabilities);
    dbgln_if(INTEL_HDA_DEBUG, "Controller capabilities:");
    m_number_of_output_streams = capabilities >> 12;
    m_number_of_input_streams = (capabilities >> 8) & 0xf;
    m_number_of_bidirectional_streams = (capabilities >> 3) & 0x1f;
    bool is_64_bit_addressing_supported = (capabilities & 0x1) > 0;
    dbgln_if(INTEL_HDA_DEBUG, "├ Number of output streams: {}", m_number_of_output_streams);
    dbgln_if(INTEL_HDA_DEBUG, "├ Number of input streams: {}", m_number_of_input_streams);
    dbgln_if(INTEL_HDA_DEBUG, "├ Number of bidirectional streams: {}", m_number_of_bidirectional_streams);
    dbgln_if(INTEL_HDA_DEBUG, "└ 64-bit addressing supported: {}", is_64_bit_addressing_supported ? "yes" : "no");
    if (m_number_of_output_streams == 0)
        return ENOTSUP;
    if (!is_64_bit_addressing_supported && sizeof(FlatPtr) == 8)
        return ENOTSUP;

    // Reset the controller
    TRY(reset());

    // Register CORB and RIRB
    auto command_io_window = TRY(m_controller_io_window->create_from_io_window_with_offset(ControllerRegister::CommandOutboundRingBufferOffset));
    m_command_buffer = TRY(CommandOutboundRingBuffer::create("IntelHDA CORB"sv, move(command_io_window)));
    TRY(m_command_buffer->register_with_controller());

    auto response_io_window = TRY(m_controller_io_window->create_from_io_window_with_offset(ControllerRegister::ResponseInboundRingBufferOffset));
    m_response_buffer = TRY(ResponseInboundRingBuffer::create("IntelHDA RIRB"sv, move(response_io_window)));
    TRY(m_response_buffer->register_with_controller());

    dbgln_if(INTEL_HDA_DEBUG, "CORB ({} entries) and RIRB ({} entries) registered", m_command_buffer->capacity(), m_response_buffer->capacity());

    // Initialize all codecs
    // 3.3.9: State Change Status
    u16 state_change_status = m_controller_io_window->read16(ControllerRegister::StateChangeStatus);
    for (u8 codec_address = 0; codec_address < 14; ++codec_address) {
        if ((state_change_status & (1 << codec_address)) > 0) {
            dmesgln_pci(*this, "Found codec on address #{}", codec_address);
            TRY(initialize_codec(codec_address));
        }
    }

    auto result = configure_output_route();
    if (result.is_error()) {
        dmesgln_pci(*this, "Failed to set up an output audio channel: {}", result.error());
        return result.release_error();
    }

    m_audio_channel = TRY(AudioChannel::create(*this, fixed_audio_channel_index));
    return {};
}

UNMAP_AFTER_INIT ErrorOr<void> Controller::initialize_codec(u8 codec_address)
{
    auto codec = TRY(Codec::create(*this, codec_address));

    auto root_node = TRY(Node::create<RootNode>(codec));
    if constexpr (INTEL_HDA_DEBUG)
        root_node->debug_dump();
    codec->set_root_node(root_node);

    TRY(m_codecs.try_append(codec));

    return {};
}

ErrorOr<u32> Controller::send_command(u8 codec_address, u8 node_id, CodecControlVerb verb, u16 payload)
{
    // Construct command
    // 7.3: If the most significant 4 bits of 12-bits verb are 0xf or 0x7, extended mode is selected
    u32 command_value = codec_address << 28 | (node_id << 20);
    if (((verb & 0x700) > 0) || ((verb & 0xf00) > 0))
        command_value |= ((verb & 0xfff) << 8) | (payload & 0xff);
    else
        command_value |= ((verb & 0xf) << 16) | payload;

    dbgln_if(INTEL_HDA_DEBUG, "Controller::{}: codec {} node {} verb {:#x} payload {:#b}",
        __FUNCTION__, codec_address, node_id, to_underlying(verb), payload);
    TRY(m_command_buffer->write_value(command_value));

    // Read response
    Optional<u64> full_response;
    TRY(wait_until(frame_delay_in_microseconds(1), controller_timeout_in_microseconds, [&]() -> ErrorOr<bool> {
        full_response = TRY(m_response_buffer->read_value());
        return full_response.has_value();
    }));
    u32 response = full_response.value() & 0xffffffffu;
    dbgln_if(INTEL_HDA_DEBUG, "Controller::{}: response {:#032b}", __FUNCTION__, response);
    return response;
}

UNMAP_AFTER_INIT ErrorOr<void> Controller::configure_output_route()
{
    Vector<NonnullRefPtr<WidgetNode>> queued_nodes;
    Vector<WidgetNode*> visited_nodes;
    HashMap<WidgetNode*, WidgetNode*> parents;

    auto create_output_path = [&](RefPtr<WidgetNode> found_node) -> ErrorOr<NonnullOwnPtr<OutputPath>> {
        // Reconstruct path by traversing parent nodes
        Vector<NonnullRefPtr<WidgetNode>> path;
        auto path_node = found_node;
        while (path_node) {
            TRY(path.try_append(*path_node));
            path_node = parents.get(path_node).value_or(nullptr);
        }
        path.reverse();

        // Create output stream
        constexpr u8 output_stream_index = 0;
        constexpr u8 output_stream_number = 1;
        u64 output_stream_offset = ControllerRegister::StreamsOffset
            + m_number_of_input_streams * 0x20
            + output_stream_index * 0x20;
        auto stream_io_window = TRY(m_controller_io_window->create_from_io_window_with_offset(output_stream_offset));
        auto output_stream = TRY(OutputStream::create(move(stream_io_window), output_stream_number));

        // Create output path
        auto output_path = TRY(OutputPath::create(move(path), move(output_stream)));
        TRY(output_path->activate());

        // Enable controller and stream interrupts for this output stream
        auto interrupt_control = m_controller_io_window->read32(ControllerRegister::InterruptControl);
        interrupt_control |= InterruptControlFlag::GlobalInterruptEnable;
        interrupt_control |= 1u << (m_number_of_input_streams + output_stream_index);
        m_controller_io_window->write32(ControllerRegister::InterruptControl, interrupt_control);

        return output_path;
    };

    for (auto codec : m_codecs) {
        // Start off by finding all candidate pin complexes
        auto pin_widgets = TRY(codec->nodes_matching<WidgetNode>([](NonnullRefPtr<WidgetNode> node) {
            // Find pin complexes that support output.
            if (node->widget_type() != WidgetNode::WidgetType::PinComplex
                || !node->pin_complex_output_supported())
                return false;

            // Only consider pin complexes that have:
            // - a physical connection (jack or fixed function)
            // - and a default device that is line out, speakers or headphones.
            auto configuration_default = node->pin_configuration_default();
            auto port_connectivity = configuration_default.port_connectivity;
            auto default_device = configuration_default.default_device;

            bool is_physically_connected = port_connectivity == WidgetNode::PinPortConnectivity::Jack
                || port_connectivity == WidgetNode::PinPortConnectivity::FixedFunction
                || port_connectivity == WidgetNode::PinPortConnectivity::JackAndFixedFunction;
            bool is_output_device = default_device == WidgetNode::PinDefaultDevice::LineOut
                || default_device == WidgetNode::PinDefaultDevice::Speaker
                || default_device == WidgetNode::PinDefaultDevice::HPOut;

            return is_physically_connected && is_output_device;
        }));

        // Perform a breadth-first search to find a path to an audio output widget
        for (auto pin_widget : pin_widgets) {
            VERIFY(queued_nodes.is_empty() && visited_nodes.is_empty() && parents.is_empty());

            TRY(queued_nodes.try_append(pin_widget));
            Optional<NonnullRefPtr<WidgetNode>> found_node = {};
            while (!queued_nodes.is_empty()) {
                auto current_node = queued_nodes.take_first();
                if (current_node->widget_type() == WidgetNode::AudioOutput) {
                    found_node = current_node;
                    break;
                }

                TRY(visited_nodes.try_append(current_node.ptr()));
                for (u8 connection_node_id : current_node->connection_list()) {
                    auto connection_node = codec->node_by_node_id(connection_node_id);
                    if (!connection_node.has_value() || connection_node.value()->node_type() != Node::NodeType::Widget) {
                        dmesgln_pci(*this, "Warning: connection node {} does not exist or is the wrong type", connection_node_id);
                        continue;
                    }

                    auto connection_widget = NonnullRefPtr<WidgetNode> { *reinterpret_cast<WidgetNode*>(connection_node.release_value()) };
                    if (visited_nodes.contains_slow(connection_widget))
                        continue;

                    TRY(queued_nodes.try_append(connection_widget));
                    TRY(parents.try_set(connection_widget, current_node.ptr()));
                }
            }

            if (found_node.has_value()) {
                m_output_path = TRY(create_output_path(found_node.release_value()));
                break;
            }

            queued_nodes.clear_with_capacity();
            visited_nodes.clear_with_capacity();
            parents.clear_with_capacity();
        }

        if (m_output_path)
            break;
    }

    if (!m_output_path) {
        dmesgln_pci(*this, "Failed to find an audio output path");
        return ENODEV;
    }

    // We are ready to go!
    dmesgln_pci(*this, "Successfully configured an audio output path");
    dbgln_if(INTEL_HDA_DEBUG, "{}", TRY(m_output_path->to_string()));

    return {};
}

ErrorOr<void> Controller::reset()
{
    // 3.3.7: "Controller Reset (CRST): Writing a 0 to this bit causes the High Definition Audio
    //         controller to transition to the Reset state."
    u32 global_control = m_controller_io_window->read32(ControllerRegister::GlobalControl);
    global_control &= ~GlobalControlFlag::ControllerReset;
    global_control &= ~GlobalControlFlag::AcceptUnsolicitedResponseEnable;
    m_controller_io_window->write32(ControllerRegister::GlobalControl, global_control);

    // 3.3.7: "After the hardware has completed sequencing into the reset state, it will report
    //         a 0 in this bit. Software must read a 0 from this bit to verify that the
    //         controller is in reset."
    TRY(wait_until(frame_delay_in_microseconds(1), controller_timeout_in_microseconds, [&]() {
        global_control = m_controller_io_window->read32(ControllerRegister::GlobalControl);
        return (global_control & GlobalControlFlag::ControllerReset) == 0;
    }));

    // 3.3.7: "Writing a 1 to this bit causes the controller to exit its Reset state and
    //         de-assert the link RESET# signal. Software is responsible for
    //         setting/clearing this bit such that the minimum link RESET# signal assertion
    //         pulse width specification is met (see Section 5.5)."
    microseconds_delay(100);
    global_control |= GlobalControlFlag::ControllerReset;
    m_controller_io_window->write32(ControllerRegister::GlobalControl, global_control);

    // 3.3.7: "When the controller hardware is ready to begin operation, it will report a 1 in
    //         this bit. Software must read a 1 from this bit before accessing any controller
    //         registers."
    TRY(wait_until(frame_delay_in_microseconds(1), controller_timeout_in_microseconds, [&]() {
        global_control = m_controller_io_window->read32(ControllerRegister::GlobalControl);
        return (global_control & GlobalControlFlag::ControllerReset) > 0;
    }));

    // 4.3 Codec Discovery:
    // "The software must wait at least 521 us (25 frames) after reading CRST as a 1 before
    // assuming that codecs have all made status change requests and have been registered
    // by the controller."
    microseconds_delay(frame_delay_in_microseconds(25));

    dbgln_if(INTEL_HDA_DEBUG, "Controller reset");
    return {};
}

ErrorOr<bool> Controller::handle_interrupt(Badge<InterruptHandler>)
{
    // Check if any interrupt status bit is set
    auto interrupt_status = m_controller_io_window->read32(ControllerRegister::InterruptStatus);
    if ((interrupt_status & InterruptStatusFlag::GlobalInterruptStatus) == 0)
        return false;

    // FIXME: Actually look at interrupt_status and iterate over streams as soon as
    //        we support multiple streams.
    if (m_output_path)
        TRY(m_output_path->output_stream().handle_interrupt({}));

    return true;
}

RefPtr<AudioChannel> Controller::audio_channel(u32 index) const
{
    if (index != fixed_audio_channel_index)
        return {};
    return m_audio_channel;
}

ErrorOr<size_t> Controller::write(size_t channel_index, UserOrKernelBuffer const& data, size_t length)
{
    if (channel_index != fixed_audio_channel_index || !m_output_path)
        return ENODEV;
    return m_output_path->output_stream().write(data, length);
}

ErrorOr<void> Controller::set_pcm_output_sample_rate(size_t channel_index, u32 samples_per_second_rate)
{
    if (channel_index != fixed_audio_channel_index || !m_output_path)
        return ENODEV;

    TRY(m_output_path->set_format({
        .sample_rate = samples_per_second_rate,
        .pcm_bits = OutputPath::fixed_pcm_bits,
        .number_of_channels = OutputPath::fixed_channel_count,
    }));
    dmesgln_pci(*this, "Set output channel #{} PCM rate: {} Hz", channel_index, samples_per_second_rate);
    return {};
}

ErrorOr<u32> Controller::get_pcm_output_sample_rate(size_t channel_index)
{
    if (channel_index != fixed_audio_channel_index || !m_output_path)
        return ENODEV;

    return m_output_path->output_stream().sample_rate();
}

ErrorOr<void> wait_until(size_t delay_in_microseconds, size_t timeout_in_microseconds, Function<ErrorOr<bool>()> condition)
{
    auto const timeout = Duration::from_microseconds(static_cast<i64>(timeout_in_microseconds));
    auto const& time_management = TimeManagement::the();
    auto start = time_management.monotonic_time(TimePrecision::Precise);
    while (!TRY(condition())) {
        microseconds_delay(delay_in_microseconds);
        if (time_management.monotonic_time(TimePrecision::Precise) - start >= timeout)
            return ETIMEDOUT;
    }
    return {};
}

}
