/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Codec.h"
#include <AK/Array.h>
#include <Kernel/Devices/Audio/IntelHDA/Controller.h>

namespace Kernel::Audio::IntelHDA {

// 7.3.4.7: Supported PCM Size, Rates
struct BitRateEncoding {
    u8 flag;
    u8 bit_rate;
};
static constexpr Array<BitRateEncoding, 5> bit_rate_encodings { {
    // clang-format off
    { 0x1,  8 },
    { 0x2,  16 },
    { 0x4,  20 },
    { 0x8,  24 },
    { 0x10, 32 },
    // clang-format on
} };

struct SampleRateEncoding {
    u16 flag;
    u32 sample_rate;
};
static constexpr Array<SampleRateEncoding, 12> sample_rate_encodings { {
    // clang-format off
    { 0x1,     8'000 },
    { 0x2,    11'025 },
    { 0x4,    16'000 },
    { 0x8,    22'050 },
    { 0x10,   32'000 },
    { 0x20,   44'100 },
    { 0x40,   48'000 },
    { 0x80,   88'200 },
    { 0x100,  96'000 },
    { 0x200, 176'400 },
    { 0x400, 192'000 },
    { 0x800, 384'000 },
    // clang-format on
} };

ErrorOr<void> Codec::register_node(NonnullRefPtr<Node> node)
{
    auto set_result = TRY(m_nodes_by_node_id.try_set(node->node_id(), node));
    VERIFY(set_result == HashSetResult::InsertedNewEntry);
    return {};
}

ErrorOr<void> Node::initialize()
{
    return codec().register_node(*this);
}

ErrorOr<u32> Node::command(CodecControlVerb verb, u16 payload)
{
    auto& node_codec = codec();
    return node_codec.controller().send_command(node_codec.codec_address(), m_node_id, verb, payload);
}

ErrorOr<u32> Node::parameter(GetParameterId get_parameter_id)
{
    return command(CodecControlVerb::GetParameter, to_underlying(get_parameter_id));
}

ErrorOr<void> Node::set_power_state(PowerState power_state)
{
    // 7.3.3.10: Power State
    TRY(command(CodecControlVerb::SetPowerState, to_underlying(power_state)));
    return {};
}

template<typename T>
void NodeWithChildren<T>::for_each_child_node(Function<void(T const&, bool)> callback) const
{
    auto number_of_child_nodes = m_child_nodes.size();
    for (size_t child_index = 0; child_index < number_of_child_nodes; ++child_index)
        callback(m_child_nodes[child_index], child_index == number_of_child_nodes - 1);
}

template<typename T>
ErrorOr<void> NodeWithChildren<T>::initialize()
{
    TRY(Node::initialize());
    return populate_child_nodes();
}

template<typename T>
ErrorOr<void> NodeWithChildren<T>::populate_child_nodes()
{
    VERIFY(m_child_nodes.is_empty());

    // 7.3.4.3: Subordinate Node Count
    auto subordinate_node_count = TRY(parameter(GetParameterId::SubordinateNodeCount));
    u8 starting_node_number = (subordinate_node_count >> 16) & 0xff;
    u8 total_number_of_nodes = subordinate_node_count & 0xff;
    TRY(m_child_nodes.try_ensure_capacity(total_number_of_nodes));
    for (int subnode_index = 0; subnode_index < total_number_of_nodes; ++subnode_index)
        m_child_nodes.unchecked_append(TRY(Node::create<T>(*this, starting_node_number + subnode_index)));
    return {};
}

StringView WidgetNode::widget_type_name() const
{
    switch (m_widget_type) {
    case WidgetType::AudioInput:
        return "Audio Input"sv;
    case WidgetType::AudioMixer:
        return "Audio Mixer"sv;
    case WidgetType::AudioOutput:
        return "Audio Output"sv;
    case WidgetType::AudioSelector:
        return "Audio Selector"sv;
    case WidgetType::BeepGenerator:
        return "Beep Generator"sv;
    case WidgetType::PinComplex:
        return "Pin Complex"sv;
    case WidgetType::Power:
        return "Power"sv;
    case WidgetType::VendorDefined:
        return "Vendor Defined"sv;
    case WidgetType::VolumeKnob:
        return "Volume Knob"sv;
    }
    return "Reserved"sv;
}

ErrorOr<void> WidgetNode::initialize()
{
    TRY(Node::initialize());

    // 7.3.4.6: Audio Widget Capabilities
    auto widget_capabilities = TRY(parameter(GetParameterId::AudioWidgetCapabilities));
    m_widget_type = static_cast<WidgetType>((widget_capabilities >> 20) & 0xf);
    m_channel_count = (((widget_capabilities >> 15) & 0xe) | (widget_capabilities & 0x1)) + 1;
    m_power_control_supported = (widget_capabilities & WidgetCapabilityFlag::PowerControlSupported) > 0;
    m_connection_list_present = (widget_capabilities & WidgetCapabilityFlag::ConnectionListPresent) > 0;
    m_format_override = (widget_capabilities & WidgetCapabilityFlag::FormatOverride) > 0;
    m_amp_param_override = (widget_capabilities & WidgetCapabilityFlag::AmpParamOverride) > 0;
    m_output_amp_present = (widget_capabilities & WidgetCapabilityFlag::OutputAmpPresent) > 0;
    m_input_amp_present = (widget_capabilities & WidgetCapabilityFlag::InputAmpPresent) > 0;

    if (supports_stream()) {
        // 7.3.3.11: Converter Stream, Channel
        auto stream_channel = TRY(command(CodecControlVerb::GetConverterStreamChannel, 0));
        m_selected_stream = (stream_channel >> 4) & 0xf;
        m_selected_channel = stream_channel & 0xf;

        TRY(populate_supported_pcm_size_rates());
        TRY(populate_supported_stream_formats());
    }

    // 7.3.4.10: Amplifier Capabilities
    auto read_amp_capabilities = [](Node& node, GetParameterId type) -> ErrorOr<AmplifierCapabilities> {
        auto capabilities = TRY(node.parameter(type));
        return AmplifierCapabilities {
            .muting_supported = ((capabilities >> 31) & 0x1) > 0,
            .step_size = static_cast<u8>((capabilities >> 16) & 0x7f),
            .number_of_steps = static_cast<u8>(((capabilities >> 8) & 0x7f) + 1),
            .offset = static_cast<u8>(capabilities & 0x7f),
        };
    };
    Node& amp_params_node = amp_param_override() ? *this : *parent_node();
    if (output_amp_present())
        m_output_amp_capabilities = TRY(read_amp_capabilities(amp_params_node, GetParameterId::OutputAmplifierCapabilities));
    if (input_amp_present())
        m_input_amp_capabilities = TRY(read_amp_capabilities(amp_params_node, GetParameterId::InputAmplifierCapabilities));

    if (widget_type() == WidgetType::PinComplex) {
        // 7.3.4.9: Pin Capabilities
        auto pin_capabilities = TRY(parameter(GetParameterId::PinCapabilities));
        m_pin_complex_input_supported = (pin_capabilities & PinCapabilityFlag::InputCapable) > 0;
        m_pin_complex_output_supported = (pin_capabilities & PinCapabilityFlag::OutputCapable) > 0;

        TRY(populate_pin_configuration_default());
    }

    // Connection list
    if (connection_list_present())
        TRY(populate_connection_list());

    return {};
}

ErrorOr<NonnullOwnPtr<KString>> WidgetNode::to_string()
{
    StringBuilder builder;
    TRY(builder.try_appendff("WidgetNode(node_id={}, type={})", node_id(), widget_type_name()));
    return KString::try_create(builder.string_view());
}

void WidgetNode::debug_dump(StringView group_spine, bool is_last) const
{
    dbgln("{} {} Widget (node #{}):", group_spine, is_last ? "└"sv : "├"sv, node_id());
    auto spine = is_last ? " "sv : "│"sv;
    dbgln("{} {} ├ Type: {} ({:#x})", group_spine, spine, widget_type_name(), to_underlying(widget_type()));
    dbgln("{} {} ├ Channel count: {}", group_spine, spine, channel_count());
    dbgln("{} {} ├ Power control supported: {}", group_spine, spine, m_power_control_supported ? "yes"sv : "no"sv);

    if (supports_stream()) {
        dbgln("{} {} ├ Selected stream: {}", group_spine, spine, selected_stream());
        if (channel_count() == 1)
            dbgln("{} {} ├ Selected channel: {}", group_spine, spine, selected_channel());
        else
            dbgln("{} {} ├ Selected channels: {}-{}", group_spine, spine, selected_channel(), selected_channel() + channel_count() - 1);

        dbgln("{} {} ├ Format override: {}", group_spine, spine, format_override() ? "yes"sv : "no"sv);
        dbgln("{} {} ├ Supported PCM bit sizes:", group_spine, spine);
        for (auto supported_size : supported_pcm_sizes())
            dbgln("{} {} │ • {}", group_spine, spine, supported_size);

        dbgln("{} {} ├ Supported PCM rates:", group_spine, spine);
        for (auto supported_rate : supported_pcm_rates())
            dbgln("{} {} │ • {}Hz", group_spine, spine, supported_rate);

        dbgln("{} {} ├ Supported stream formats:", group_spine, spine);
        if (has_flag(supported_stream_formats(), StreamFormatFlag::PCM))
            dbgln("{} {} │ • PCM", group_spine, spine);
        if (has_flag(supported_stream_formats(), StreamFormatFlag::Float32))
            dbgln("{} {} │ • Float32", group_spine, spine);
        if (has_flag(supported_stream_formats(), StreamFormatFlag::AC3))
            dbgln("{} {} │ • AC3", group_spine, spine);
    }

    dbgln("{} {} ├ Amplifier parameters override: {}", group_spine, spine, amp_param_override() ? "yes"sv : "no"sv);
    dbgln("{} {} ├ Output amplifier present: {}", group_spine, spine, output_amp_present() ? "yes"sv : "no"sv);
    if (output_amp_present()) {
        auto amp_capabilities = output_amp_capabilities();
        dbgln("{} {} │ ├ Muting supported: {}", group_spine, spine, amp_capabilities.muting_supported ? "yes"sv : "no"sv);
        dbgln("{} {} │ ├ Step size: {}", group_spine, spine, amp_capabilities.step_size);
        dbgln("{} {} │ ├ Number of steps: {}", group_spine, spine, amp_capabilities.number_of_steps);
        dbgln("{} {} │ └ Offset: {}", group_spine, spine, amp_capabilities.offset);
    }

    dbgln("{} {} ├ Input amplifier present: {}", group_spine, spine, input_amp_present() ? "yes"sv : "no"sv);
    if (input_amp_present()) {
        auto amp_capabilities = input_amp_capabilities();
        dbgln("{} {} │ ├ Muting supported: {}", group_spine, spine, amp_capabilities.muting_supported ? "yes"sv : "no"sv);
        dbgln("{} {} │ ├ Step size: {}", group_spine, spine, amp_capabilities.step_size);
        dbgln("{} {} │ ├ Number of steps: {}", group_spine, spine, amp_capabilities.number_of_steps);
        dbgln("{} {} │ └ Offset: {}", group_spine, spine, amp_capabilities.offset);
    }

    if (widget_type() == WidgetType::PinComplex) {
        dbgln("{} {} ├ Pin complex input supported: {}", group_spine, spine, pin_complex_input_supported());
        dbgln("{} {} ├ Pin complex output supported: {}", group_spine, spine, pin_complex_output_supported());
        dbgln("{} {} ├ Pin configuration default:", group_spine, spine);
        dbgln("{} {} │ ├ Sequence: {}", group_spine, spine, m_pin_configuration_default.sequence);
        dbgln("{} {} │ ├ Default association: {}", group_spine, spine, m_pin_configuration_default.default_association);
        dbgln("{} {} │ ├ Jack detect override: {}", group_spine, spine,
            ((static_cast<u8>(m_pin_configuration_default.misc) & to_underlying(PinMiscFlag::JackDetectOverride)) > 0) ? "yes"sv : "no"sv);
        dbgln("{} {} │ ├ Color: {}", group_spine, spine, pin_color_name());
        dbgln("{} {} │ ├ Connection type: {}", group_spine, spine, pin_connection_type_name());
        dbgln("{} {} │ ├ Default device: {}", group_spine, spine, pin_default_device_name());
        dbgln("{} {} │ ├ Location: {}, {}", group_spine, spine, pin_gross_location_name(), pin_geometric_location_name());
        dbgln("{} {} │ └ Port connectivity: {}", group_spine, spine, pin_port_connectivity_name());
    }

    dbgln("{} {} └ Connection list:{}", group_spine, spine, connection_list_present() ? ""sv : " absent"sv);
    if (connection_list_present()) {
        auto selected_node_id = connection_selected_node_id();
        auto all_active = !supports_connection_select_control();
        for (auto connection_entry : connection_list()) {
            dbgln("{} {}   • Node #{}{}", group_spine, spine, connection_entry,
                all_active || connection_entry == selected_node_id ? " (active)"sv : ""sv);
        }
    }
}

ErrorOr<void> WidgetNode::set_amplifier_gain_mute(SetAmplifierGainMute settings)
{
    // 7.3.3.7: Amplifier Gain/Mute
    VERIFY(input_amp_present() || output_amp_present());
    u16 set_amp_gain_payload = ((output_amp_present() ? 1 : 0) << 15)
        | ((input_amp_present() ? 1 : 0) << 15)
        | ((settings.set_left ? 1 : 0) << 13)
        | ((settings.set_right ? 1 : 0) << 12)
        | ((settings.connection_index & 0xf) << 8)
        | ((settings.mute ? 1 : 0) << 7)
        | (settings.gain & 0x7f);
    TRY(command(CodecControlVerb::SetAmplifierGainMute, set_amp_gain_payload));
    return {};
}

ErrorOr<void> WidgetNode::set_connection_select(u8 connection_index)
{
    // 7.3.3.2: Connection Select Control
    VERIFY(connection_list_present());
    VERIFY(connection_index < connection_list().size());
    TRY(command(CodecControlVerb::SetConnectionSelectControl, connection_index));
    return {};
}

ErrorOr<void> WidgetNode::set_converter_stream_and_channel(u8 stream_index, u8 channel_index)
{
    // 7.3.3.11: Converter Stream, Channel
    VERIFY(widget_type() == WidgetType::AudioInput || widget_type() == WidgetType::AudioOutput);
    u16 stream_channel_payload = ((stream_index & 0xf) << 4) | (channel_index & 0xf);
    TRY(command(CodecControlVerb::SetConverterStreamChannel, stream_channel_payload));
    return {};
}

ErrorOr<void> WidgetNode::set_pin_control(PinControl pin_control)
{
    // 7.3.3.13: Pin Widget Control
    VERIFY(widget_type() == WidgetType::PinComplex);
    VERIFY(!pin_control.output_enabled || pin_complex_output_supported());
    VERIFY(!pin_control.input_enabled || pin_complex_input_supported());

    u8 payload = ((pin_control.low_impedance_amplifier_enabled ? 1 : 0) << 7)
        | ((pin_control.output_enabled ? 1 : 0) << 6)
        | ((pin_control.input_enabled ? 1 : 0) << 5)
        | (pin_control.voltage_reference_enable & 0x7);
    TRY(command(CodecControlVerb::SetPinWidgetControl, payload));
    return {};
}

bool WidgetNode::supports_stream() const
{
    return widget_type() == WidgetType::AudioInput
        || widget_type() == WidgetType::AudioOutput;
}

bool WidgetNode::supports_connection_select_control() const
{
    return widget_type() == WidgetType::AudioInput
        || widget_type() == WidgetType::AudioSelector
        || widget_type() == WidgetType::PinComplex;
}

ErrorOr<FormatParameters> WidgetNode::get_converter_format()
{
    // 7.3.3.8: Converter Format
    VERIFY(widget_type() == WidgetType::AudioInput || widget_type() == WidgetType::AudioOutput);
    u16 format = TRY(command(CodecControlVerb::GetConverterFormat, 0)) & 0xffffu;
    return decode_format(format);
}

ErrorOr<void> WidgetNode::set_converter_format(FormatParameters format)
{
    // 7.3.3.8: Converter Format
    VERIFY(widget_type() == WidgetType::AudioInput || widget_type() == WidgetType::AudioOutput);
    u16 format_payload = TRY(encode_format(format));
    TRY(command(CodecControlVerb::SetConverterFormat, format_payload));
    return {};
}

ErrorOr<void> WidgetNode::populate_supported_pcm_size_rates()
{
    VERIFY(m_supported_pcm_sizes.is_empty() && m_supported_pcm_rates.is_empty());

    // 7.3.4.7: Supported PCM Size, Rates
    Node& stream_support_node = format_override() ? *this : *parent_node();
    auto supported_pcm_size_and_rates = TRY(stream_support_node.parameter(GetParameterId::SupportedPCMSizeRates));

    auto pcm_sizes = (supported_pcm_size_and_rates >> 16) & 0x1f;
    TRY(m_supported_pcm_sizes.try_ensure_capacity(popcount(pcm_sizes)));
    for (auto bit_rate_encoding : bit_rate_encodings) {
        if ((pcm_sizes & bit_rate_encoding.flag) > 0)
            m_supported_pcm_sizes.unchecked_append(bit_rate_encoding.bit_rate);
    }

    auto pcm_rates = supported_pcm_size_and_rates & 0x7ff;
    TRY(m_supported_pcm_rates.try_ensure_capacity(popcount(pcm_rates)));
    for (auto sample_rate_encoding : sample_rate_encodings) {
        if ((pcm_rates & sample_rate_encoding.flag) > 0)
            m_supported_pcm_rates.unchecked_append(sample_rate_encoding.sample_rate);
    }

    return {};
}

ErrorOr<void> WidgetNode::populate_supported_stream_formats()
{
    VERIFY(m_supported_stream_formats == 0);

    // 7.3.4.8: Supported Stream Formats
    Node& stream_support_node = format_override() ? *this : *parent_node();
    auto supported_stream_formats = TRY(stream_support_node.parameter(GetParameterId::SupportedStreamFormats));

    if ((supported_stream_formats & 0x1) > 0)
        m_supported_stream_formats |= StreamFormatFlag::PCM;
    if ((supported_stream_formats & 0x2) > 0)
        m_supported_stream_formats |= StreamFormatFlag::Float32;
    if ((supported_stream_formats & 0x4) > 0)
        m_supported_stream_formats |= StreamFormatFlag::AC3;

    return {};
}

ErrorOr<void> WidgetNode::populate_connection_list()
{
    VERIFY(connection_list_present());
    VERIFY(m_connection_list.is_empty());

    // 7.3.4.11: Connection List Length
    auto connection_list_length_info = TRY(parameter(GetParameterId::ConnectionListLength));
    bool long_form = (connection_list_length_info >> 7) & 0x1;
    u8 connection_list_length = connection_list_length_info & 0x7f;
    u8 entries_per_request = long_form ? 2 : 4;

    // 7.3.3.3: Get Connection List Entry
    for (u8 entry_offset = 0; entry_offset < connection_list_length; entry_offset += entries_per_request) {
        auto entries = TRY(command(CodecControlVerb::GetConnectionListEntry, entry_offset));
        for (u8 entry_index = 0; entry_index < min(entries_per_request, static_cast<int>(connection_list_length) - entry_offset); ++entry_index) {
            u16 entry = entries & (long_form ? 0xffff : 0xff);
            TRY(m_connection_list.try_append(entry));
            entries >>= (32 / entries_per_request);
        }
    }

    // 7.1.3: Widget Interconnection Rules
    //         "Connection_List_Length = 1 means there is only one (hard-wired) input possible and,
    //          therefore, there is no Connection_Selector field. The actual connection is read
    //          from the Connection List as usual."
    if (connection_list_length == 1) {
        m_connection_index = 0;
    } else {
        // 7.3.3.2: Connection Select Control
        auto connection_selection_control = TRY(command(CodecControlVerb::GetConnectionSelectControl, 0));
        m_connection_index = connection_selection_control & 0xff;
    }

    return {};
}

ErrorOr<void> WidgetNode::populate_pin_configuration_default()
{
    VERIFY(widget_type() == WidgetType::PinComplex);

    u32 configuration_default = TRY(command(CodecControlVerb::GetConfigurationDefault, 0));
    m_pin_configuration_default.sequence = configuration_default & 0xf;
    m_pin_configuration_default.default_association = (configuration_default >> 4) & 0xf;
    m_pin_configuration_default.misc = static_cast<PinMiscFlag>((configuration_default >> 8) & 0xf);
    m_pin_configuration_default.color = static_cast<PinColor>((configuration_default >> 12) & 0xf);
    m_pin_configuration_default.connection_type = static_cast<PinConnectionType>((configuration_default >> 16) & 0xf);
    m_pin_configuration_default.default_device = static_cast<PinDefaultDevice>((configuration_default >> 20) & 0xf);
    m_pin_configuration_default.geometric_location = static_cast<PinGeometricLocation>((configuration_default >> 24) & 0xf);
    m_pin_configuration_default.gross_location = static_cast<PinGrossLocation>((configuration_default >> 28) & 0x3);
    m_pin_configuration_default.port_connectivity = static_cast<PinPortConnectivity>(configuration_default >> 30);

    return {};
}

StringView WidgetNode::pin_color_name() const
{
    auto underlying_color = to_underlying(m_pin_configuration_default.color);
    if (underlying_color >= 0xa && underlying_color <= 0xd)
        return "Reserved"sv;

    switch (m_pin_configuration_default.color) {
    case PinColor::Unknown:
        return "Unknown"sv;
    case PinColor::Black:
        return "Black"sv;
    case PinColor::Grey:
        return "Grey"sv;
    case PinColor::Blue:
        return "Blue"sv;
    case PinColor::Green:
        return "Green"sv;
    case PinColor::Red:
        return "Red"sv;
    case PinColor::Orange:
        return "Orange"sv;
    case PinColor::Yellow:
        return "Yellow"sv;
    case PinColor::Purple:
        return "Purple"sv;
    case PinColor::Pink:
        return "Pink"sv;
    case PinColor::White:
        return "White"sv;
    case PinColor::Other:
        return "Other"sv;
    }
    VERIFY_NOT_REACHED();
}

StringView WidgetNode::pin_connection_type_name() const
{
    switch (m_pin_configuration_default.connection_type) {
    case PinConnectionType::Unknown:
        return "Unknown"sv;
    case PinConnectionType::EighthStereoMono:
        return "1/8\" Stereo/Mono"sv;
    case PinConnectionType::FourthStereoMono:
        return "1/4\" Stereo/Mono"sv;
    case PinConnectionType::ATAPIInternal:
        return "ATAPI Internal"sv;
    case PinConnectionType::RCA:
        return "RCA"sv;
    case PinConnectionType::Optical:
        return "Optical"sv;
    case PinConnectionType::OtherDigital:
        return "Other Digital"sv;
    case PinConnectionType::OtherAnalog:
        return "Other Analog"sv;
    case PinConnectionType::MultichannelAnalog:
        return "Multichannel Analog"sv;
    case PinConnectionType::XLRProfessional:
        return "XLR / Professional"sv;
    case PinConnectionType::RJ11:
        return "RJ-11 (Modem)"sv;
    case PinConnectionType::Combination:
        return "Combination"sv;
    case PinConnectionType::Other:
        return "Other"sv;
    }
    VERIFY_NOT_REACHED();
}

StringView WidgetNode::pin_default_device_name() const
{
    switch (m_pin_configuration_default.default_device) {
    case PinDefaultDevice::LineOut:
        return "Line Out"sv;
    case PinDefaultDevice::Speaker:
        return "Speaker"sv;
    case PinDefaultDevice::HPOut:
        return "Headphones"sv;
    case PinDefaultDevice::CD:
        return "CD"sv;
    case PinDefaultDevice::SPDIFOut:
        return "S/PDIF Out"sv;
    case PinDefaultDevice::DigitalOtherOut:
        return "Digital Other Out"sv;
    case PinDefaultDevice::ModemLineSide:
        return "Modem Line Side"sv;
    case PinDefaultDevice::ModemHandsetSide:
        return "Modem Handset Side"sv;
    case PinDefaultDevice::LineIn:
        return "Line In"sv;
    case PinDefaultDevice::AUX:
        return "AUX"sv;
    case PinDefaultDevice::MicIn:
        return "Mic In"sv;
    case PinDefaultDevice::Telephony:
        return "Telephony"sv;
    case PinDefaultDevice::SPDIFIn:
        return "S/PDIF In"sv;
    case PinDefaultDevice::DigitalOtherIn:
        return "Digital Other In"sv;
    case PinDefaultDevice::Reserved:
        return "Reserved"sv;
    case PinDefaultDevice::Other:
        return "Other"sv;
    }
    VERIFY_NOT_REACHED();
}

StringView WidgetNode::pin_gross_location_name() const
{
    switch (m_pin_configuration_default.gross_location) {
    case PinGrossLocation::ExternalOnPrimaryChassis:
        return "External on Primary Chassis"sv;
    case PinGrossLocation::Internal:
        return "Internal"sv;
    case PinGrossLocation::SeparateChassis:
        return "Separate Chassis"sv;
    case PinGrossLocation::Other:
        return "Other"sv;
    }
    VERIFY_NOT_REACHED();
}

StringView WidgetNode::pin_geometric_location_name() const
{
    // 7.3.3.31: Configuration Default - special cases
    if (m_pin_configuration_default.geometric_location == PinGeometricLocation::Special1) {
        if (m_pin_configuration_default.gross_location == PinGrossLocation::ExternalOnPrimaryChassis)
            return "Rear Panel"sv;
        if (m_pin_configuration_default.gross_location == PinGrossLocation::Internal)
            return "Riser"sv;
        if (m_pin_configuration_default.gross_location == PinGrossLocation::Other)
            return "Mobile Lid (Inside)"sv;
    } else if (m_pin_configuration_default.geometric_location == PinGeometricLocation::Special2) {
        if (m_pin_configuration_default.gross_location == PinGrossLocation::ExternalOnPrimaryChassis)
            return "Drive Bay"sv;
        if (m_pin_configuration_default.gross_location == PinGrossLocation::Internal)
            return "Digital Display"sv;
        if (m_pin_configuration_default.gross_location == PinGrossLocation::Other)
            return "Mobile Lid (Outside)"sv;
    } else if (m_pin_configuration_default.geometric_location == PinGeometricLocation::Special3) {
        if (m_pin_configuration_default.gross_location == PinGrossLocation::Internal)
            return "ATAPI"sv;
    }

    switch (m_pin_configuration_default.geometric_location) {
    case PinGeometricLocation::NotApplicable:
        return "N/A"sv;
    case PinGeometricLocation::Rear:
        return "Rear"sv;
    case PinGeometricLocation::Front:
        return "Front"sv;
    case PinGeometricLocation::Left:
        return "Left"sv;
    case PinGeometricLocation::Right:
        return "Right"sv;
    case PinGeometricLocation::Top:
        return "Top"sv;
    case PinGeometricLocation::Bottom:
        return "Bottom"sv;
    case PinGeometricLocation::Special1:
    case PinGeometricLocation::Special2:
    case PinGeometricLocation::Special3:
        return "Special"sv;
    }
    VERIFY_NOT_REACHED();
}

StringView WidgetNode::pin_port_connectivity_name() const
{
    switch (m_pin_configuration_default.port_connectivity) {
    case PinPortConnectivity::Jack:
        return "Jack"sv;
    case PinPortConnectivity::NoConnection:
        return "No Physical Connection"sv;
    case PinPortConnectivity::FixedFunction:
        return "Fixed Function Device"sv;
    case PinPortConnectivity::JackAndFixedFunction:
        return "Jack and Fixed Function Device"sv;
    }
    VERIFY_NOT_REACHED();
}

StringView FunctionGroupNode::function_group_type_name() const
{
    switch (m_function_group_type) {
    case FunctionGroupType::AudioFunctionGroup:
        return "Audio Function Group"sv;
    case FunctionGroupType::ModemFunctionGroup:
        return "Modem Function Group"sv;
    case FunctionGroupType::VendorFunctionGroup:
        return "Vendor Function Group"sv;
    case FunctionGroupType::Reserved:
        return "Reserved"sv;
    }
    VERIFY_NOT_REACHED();
}

ErrorOr<void> FunctionGroupNode::initialize()
{
    TRY(NodeWithChildren<WidgetNode>::initialize());

    // 7.3.4.4: Function Group Type
    auto function_group_type = TRY(parameter(GetParameterId::FunctionGroupType));
    if (function_group_type == 0x1)
        m_function_group_type = FunctionGroupType::AudioFunctionGroup;
    else if (function_group_type == 0x2)
        m_function_group_type = FunctionGroupType::ModemFunctionGroup;
    else if (function_group_type >= 0x80)
        m_function_group_type = FunctionGroupType::VendorFunctionGroup;
    else
        m_function_group_type = FunctionGroupType::Reserved;

    return {};
}

ErrorOr<NonnullOwnPtr<KString>> FunctionGroupNode::to_string()
{
    StringBuilder builder;
    TRY(builder.try_appendff("FunctionGroupNode(node_id={})", node_id()));
    return KString::try_create(builder.string_view());
}

void FunctionGroupNode::debug_dump(bool is_last) const
{
    dbgln("{} Function group (node #{}):", is_last ? "└"sv : "├"sv, node_id());
    auto spine = is_last ? " "sv : "│"sv;
    dbgln("{} ├ Function group type: {} ({:#x})", spine, function_group_type_name(), to_underlying(function_group_type()));

    for_each_child_node([&spine](WidgetNode const& widget_node, bool is_last) -> void {
        widget_node.debug_dump(spine, is_last);
    });
}

ErrorOr<void> RootNode::initialize()
{
    TRY(NodeWithChildren<FunctionGroupNode>::initialize());

    // 7.3.4.1: Vendor ID
    auto vendor_id_response = TRY(parameter(GetParameterId::VendorID));
    m_vendor_id = (vendor_id_response >> 16) & 0xffff;
    m_device_id = vendor_id_response & 0xffff;

    // 7.3.4.2: Revision ID
    auto revision_id_response = TRY(parameter(GetParameterId::RevisionID));
    m_major_revision = (revision_id_response >> 20) & 0xf;
    m_minor_revision = (revision_id_response >> 16) & 0xf;
    if (m_major_revision != 1 || m_minor_revision != 0)
        return ENOTSUP;

    return {};
}

ErrorOr<NonnullOwnPtr<KString>> RootNode::to_string()
{
    StringBuilder builder;
    TRY(builder.try_appendff("RootNode(node_id={})", node_id()));
    return KString::try_create(builder.string_view());
}

void RootNode::debug_dump() const
{
    dbgln("Root (node #{}):", node_id());
    dbgln("├ Codec vendor: {:#04x}, device: {:#04x}", vendor_id(), device_id());
    dbgln("├ Codec HDA compatibility: {}.{}", major_revision(), minor_revision());

    for_each_child_node([](FunctionGroupNode const& fg_node, bool is_last) -> void {
        fg_node.debug_dump(is_last);
    });
}

}
