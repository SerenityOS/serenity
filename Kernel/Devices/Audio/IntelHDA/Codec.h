/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <Kernel/Devices/Audio/IntelHDA/Format.h>
#include <Kernel/Library/KString.h>

namespace Kernel::Audio::IntelHDA {

class Codec;
class Controller;
class RootNode;

// 7.3.3: Controls
enum CodecControlVerb : u16 {
    GetParameter = 0xf00,
    GetConnectionSelectControl = 0xf01,
    SetConnectionSelectControl = 0x701,
    GetConnectionListEntry = 0xf02,
    GetAmplifierGainMute = 0xb,
    SetAmplifierGainMute = 0x3,
    GetConverterFormat = 0xa,
    SetConverterFormat = 0x2,
    SetPowerState = 0x705,
    GetConverterStreamChannel = 0xf06,
    SetConverterStreamChannel = 0x706,
    SetPinWidgetControl = 0x707,
    GetConfigurationDefault = 0xf1c,
};

// 7.3.4.8: Supported Stream Formats, figure 88
enum StreamFormatFlag : u8 {
    PCM = 1u << 0,
    Float32 = 1u << 1,
    AC3 = 1u << 2,
};
AK_ENUM_BITWISE_OPERATORS(StreamFormatFlag);

class Node : public RefCounted<Node> {
public:
    enum class NodeType {
        Root,
        FunctionGroup,
        Widget,
    };

    // 7.3.3.10: Power State, table 83
    enum class PowerState : u8 {
        D0 = 0b000,
        D1 = 0b001,
        D2 = 0b010,
        D3 = 0b011,
        D3Cold = 0b100,
    };

    // 7.3.4: Parameters
    enum class GetParameterId : u8 {
        VendorID = 0x00,
        RevisionID = 0x02,
        SubordinateNodeCount = 0x04,
        FunctionGroupType = 0x05,
        AudioFunctionGroupCapabilities = 0x08,
        AudioWidgetCapabilities = 0x09,
        SupportedPCMSizeRates = 0x0a,
        SupportedStreamFormats = 0x0b,
        PinCapabilities = 0x0c,
        InputAmplifierCapabilities = 0x0d,
        ConnectionListLength = 0x0e,
        SupportedPowerStates = 0x0f,
        ProcessingCapabilities = 0x10,
        GPIOCount = 0x11,
        OutputAmplifierCapabilities = 0x12,
        VolumeKnobCapabilities = 0x13,
    };

    virtual ~Node() = default;

    template<typename T, class... Args>
    static ErrorOr<NonnullRefPtr<T>> create(Args&&... args)
    requires(IsBaseOf<Node, T>)
    {
        auto node = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) T(forward<Args>(args)...)));
        TRY(node->initialize());
        return node;
    }

    virtual Codec& codec()
    {
        VERIFY(m_parent_node);
        return m_parent_node->codec();
    }

    NodeType node_type() const { return m_node_type; }
    RefPtr<Node> parent_node() const { return m_parent_node; }
    u8 node_id() const { return m_node_id; }

    ErrorOr<u32> command(CodecControlVerb, u16 payload);
    ErrorOr<u32> parameter(GetParameterId);
    ErrorOr<void> set_power_state(PowerState);
    virtual ErrorOr<NonnullOwnPtr<KString>> to_string() = 0;

protected:
    Node(NodeType node_type, RefPtr<Node> parent_node, u8 node_id)
        : m_node_type(node_type)
        , m_parent_node(parent_node)
        , m_node_id(node_id)
    {
    }

    virtual ErrorOr<void> initialize();

    NodeType m_node_type;
    RefPtr<Node> m_parent_node;
    u8 m_node_id;
};

template<typename T>
class NodeWithChildren : public Node {
    friend class Node;

public:
    Vector<NonnullRefPtr<T>> child_nodes() { return m_child_nodes; }
    void for_each_child_node(Function<void(T const&, bool)> callback) const;

protected:
    NodeWithChildren(NodeType node_type, RefPtr<Node> parent_node, u8 node_id)
        : Node(node_type, parent_node, node_id)
    {
    }

    ErrorOr<void> initialize() override;

private:
    ErrorOr<void> populate_child_nodes();

    Vector<NonnullRefPtr<T>> m_child_nodes {};
};

class WidgetNode final : public Node {
    friend class Node;

public:
    static constexpr NodeType Type = NodeType::Widget;

    // 7.3.4.6: Audio Widget Capabilities, figure 86
    enum WidgetCapabilityFlag : u32 {
        InputAmpPresent = 1u << 1,
        OutputAmpPresent = 1u << 2,
        AmpParamOverride = 1u << 3,
        FormatOverride = 1u << 4,
        ConnectionListPresent = 1u << 8,
        PowerControlSupported = 1u << 10,
    };

    // 7.3.4.6: Audio Widget Capabilities, table 138
    enum WidgetType : u8 {
        AudioOutput = 0x0,
        AudioInput = 0x1,
        AudioMixer = 0x2,
        AudioSelector = 0x3,
        PinComplex = 0x4,
        Power = 0x5,
        VolumeKnob = 0x6,
        BeepGenerator = 0x7,
        VendorDefined = 0xf,
    };

    // 7.3.4.9: Pin Capabilities, figure 89
    enum PinCapabilityFlag : u32 {
        OutputCapable = 1u << 4,
        InputCapable = 1u << 5,
    };

    // 7.3.4.10: Amplifier Capabilities
    struct AmplifierCapabilities {
        bool muting_supported;
        u8 step_size;
        u8 number_of_steps;
        u8 offset;
    };

    // 7.3.3.7: Amplifier Gain/Mute Set Payload
    struct SetAmplifierGainMute {
        bool set_left { true };
        bool set_right { true };
        u8 connection_index { 0 };
        bool mute;
        u8 gain;
    };

    // 7.3.3.13: Pin Widget Control
    struct PinControl {
        bool low_impedance_amplifier_enabled { true };
        bool output_enabled { false };
        bool input_enabled { false };
        u8 voltage_reference_enable { 0 };
    };

    // 7.3.3.31: Configuration Default, table 109
    enum class PinPortConnectivity : u8 {
        Jack = 0b00,
        NoConnection = 0b01,
        FixedFunction = 0b10,
        JackAndFixedFunction = 0b11,
    };

    // 7.3.3.31: Configuration Default, table 110 (rows)
    enum class PinGrossLocation : u8 {
        ExternalOnPrimaryChassis = 0b00,
        Internal = 0b01,
        SeparateChassis = 0b10,
        Other = 0b11,
    };

    // 7.3.3.31: Configuration Default, table 110 (columns)
    enum class PinGeometricLocation : u8 {
        NotApplicable = 0x0,
        Rear = 0x1,
        Front = 0x2,
        Left = 0x3,
        Right = 0x4,
        Top = 0x5,
        Bottom = 0x6,
        Special1 = 0x7,
        Special2 = 0x8,
        Special3 = 0x9,
    };

    // 7.3.3.31: Configuration Default, table 111
    enum class PinDefaultDevice : u8 {
        LineOut = 0x0,
        Speaker = 0x1,
        HPOut = 0x2,
        CD = 0x3,
        SPDIFOut = 0x4,
        DigitalOtherOut = 0x5,
        ModemLineSide = 0x6,
        ModemHandsetSide = 0x7,
        LineIn = 0x8,
        AUX = 0x9,
        MicIn = 0xa,
        Telephony = 0xb,
        SPDIFIn = 0xc,
        DigitalOtherIn = 0xd,
        Reserved = 0xe,
        Other = 0xf,
    };

    // 7.3.3.31: Configuration Default, table 112
    enum class PinConnectionType : u8 {
        Unknown = 0x0,
        EighthStereoMono = 0x1,
        FourthStereoMono = 0x2,
        ATAPIInternal = 0x3,
        RCA = 0x4,
        Optical = 0x5,
        OtherDigital = 0x6,
        OtherAnalog = 0x7,
        MultichannelAnalog = 0x8,
        XLRProfessional = 0x9,
        RJ11 = 0xa,
        Combination = 0xb,
        Other = 0xf,
    };

    // 7.3.3.31: Configuration Default, table 113
    enum class PinColor : u8 {
        Unknown = 0x0,
        Black = 0x1,
        Grey = 0x2,
        Blue = 0x3,
        Green = 0x4,
        Red = 0x5,
        Orange = 0x6,
        Yellow = 0x7,
        Purple = 0x8,
        Pink = 0x9,
        White = 0xe,
        Other = 0xf,
    };

    // 7.3.3.31: Configuration Default, table 114
    enum class PinMiscFlag : u8 {
        JackDetectOverride = 1u << 0,
    };

    // 7.3.3.31: Configuration Default, figure 74
    struct PinConfigurationDefault {
        PinPortConnectivity port_connectivity;
        PinGrossLocation gross_location;
        PinGeometricLocation geometric_location;
        PinDefaultDevice default_device;
        PinConnectionType connection_type;
        PinColor color;
        PinMiscFlag misc;
        u8 default_association;
        u8 sequence;
    };

    WidgetType widget_type() const { return m_widget_type; }
    StringView widget_type_name() const;

    u8 channel_count() const { return m_channel_count; }
    bool power_control_supported() const { return m_power_control_supported; }
    bool connection_list_present() const { return m_connection_list_present; }
    bool format_override() const { return m_format_override; }
    bool amp_param_override() const { return m_amp_param_override; }
    bool output_amp_present() const { return m_output_amp_present; }
    bool input_amp_present() const { return m_input_amp_present; }
    u8 selected_stream() const { return m_selected_stream; }
    u8 selected_channel() const { return m_selected_channel; }
    Span<u8 const> supported_pcm_sizes() const { return m_supported_pcm_sizes.span(); }
    Span<u32 const> supported_pcm_rates() const { return m_supported_pcm_rates.span(); }
    StreamFormatFlag supported_stream_formats() const { return m_supported_stream_formats; }
    AmplifierCapabilities output_amp_capabilities() const { return m_output_amp_capabilities; }
    AmplifierCapabilities input_amp_capabilities() const { return m_input_amp_capabilities; }
    bool pin_complex_input_supported() const { return m_pin_complex_input_supported; }
    bool pin_complex_output_supported() const { return m_pin_complex_output_supported; }
    Span<u8 const> connection_list() const { return m_connection_list.span(); }
    u8 connection_selected_node_id() const { return m_connection_list[m_connection_index]; }

    PinConfigurationDefault pin_configuration_default() const { return m_pin_configuration_default; }
    StringView pin_color_name() const;
    StringView pin_connection_type_name() const;
    StringView pin_default_device_name() const;
    StringView pin_gross_location_name() const;
    StringView pin_geometric_location_name() const;
    StringView pin_port_connectivity_name() const;

    ErrorOr<NonnullOwnPtr<KString>> to_string() override;
    void debug_dump(StringView, bool) const;
    ErrorOr<void> set_amplifier_gain_mute(SetAmplifierGainMute);
    ErrorOr<void> set_connection_select(u8);
    ErrorOr<void> set_converter_stream_and_channel(u8 stream_index, u8 channel_index);
    ErrorOr<void> set_pin_control(PinControl);
    bool supports_stream() const;
    bool supports_connection_select_control() const;

    ErrorOr<FormatParameters> get_converter_format();
    ErrorOr<void> set_converter_format(FormatParameters);

protected:
    ErrorOr<void> initialize() override;

private:
    WidgetNode(NonnullRefPtr<Node> parent_node, u8 node_id)
        : Node(NodeType::Widget, parent_node, node_id)
    {
    }

    ErrorOr<void> populate_supported_pcm_size_rates();
    ErrorOr<void> populate_supported_stream_formats();
    ErrorOr<void> populate_connection_list();
    ErrorOr<void> populate_pin_configuration_default();

    WidgetType m_widget_type;
    u8 m_channel_count;
    bool m_power_control_supported;
    bool m_connection_list_present;
    bool m_format_override;
    bool m_amp_param_override;
    bool m_output_amp_present;
    bool m_input_amp_present;
    u8 m_selected_stream;
    u8 m_selected_channel;
    Vector<u8> m_supported_pcm_sizes {};
    Vector<u32> m_supported_pcm_rates {};
    StreamFormatFlag m_supported_stream_formats { 0 };
    AmplifierCapabilities m_output_amp_capabilities;
    AmplifierCapabilities m_input_amp_capabilities;
    bool m_pin_complex_input_supported;
    bool m_pin_complex_output_supported;
    PinConfigurationDefault m_pin_configuration_default;
    Vector<u8> m_connection_list {};
    u8 m_connection_index;
};

class FunctionGroupNode final : public NodeWithChildren<WidgetNode> {
    friend class Node;

public:
    static constexpr NodeType Type = NodeType::FunctionGroup;

    // 7.3.4.4: Function Group Type
    enum class FunctionGroupType {
        AudioFunctionGroup,
        ModemFunctionGroup,
        VendorFunctionGroup,
        Reserved,
    };

    ErrorOr<NonnullOwnPtr<KString>> to_string() override;
    void debug_dump(bool) const;

    FunctionGroupType function_group_type() const { return m_function_group_type; }
    StringView function_group_type_name() const;

protected:
    ErrorOr<void> initialize() override;

private:
    FunctionGroupNode(NonnullRefPtr<Node> parent_node, u8 node_id)
        : NodeWithChildren<WidgetNode>(NodeType::FunctionGroup, parent_node, node_id)
    {
    }

    FunctionGroupType m_function_group_type;
};

class RootNode final : public NodeWithChildren<FunctionGroupNode> {
    friend class Node;

public:
    static constexpr NodeType Type = NodeType::Root;

    Codec& codec() override { return m_codec; }

    u16 vendor_id() const { return m_vendor_id; }
    u16 device_id() const { return m_device_id; }
    u8 major_revision() const { return m_major_revision; }
    u8 minor_revision() const { return m_minor_revision; }

    ErrorOr<NonnullOwnPtr<KString>> to_string() override;
    void debug_dump() const;

protected:
    ErrorOr<void> initialize() override;

private:
    RootNode(Codec& codec)
        : NodeWithChildren<FunctionGroupNode>(NodeType::Root, {}, 0)
        , m_codec(codec)
    {
    }

    Codec& m_codec;
    u16 m_vendor_id;
    u16 m_device_id;
    u8 m_major_revision;
    u8 m_minor_revision;
};

class Codec : public RefCounted<Codec> {
public:
    static ErrorOr<NonnullRefPtr<Codec>> create(Controller& controller, u8 codec_address)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) Codec(controller, codec_address));
    }

    Controller& controller() const { return m_controller; }
    u8 codec_address() const { return m_codec_address; }

    RefPtr<RootNode> root_node() const { return m_root_node; }
    void set_root_node(NonnullRefPtr<RootNode> root_node) { m_root_node = root_node; }

    ErrorOr<void> register_node(NonnullRefPtr<Node> node);
    Optional<Node*> node_by_node_id(u8 node_id) { return m_nodes_by_node_id.get(node_id); }

    template<typename T, typename TPredicate>
    ErrorOr<Vector<NonnullRefPtr<T>>> nodes_matching(TPredicate predicate)
    {
        Vector<NonnullRefPtr<T>> results;
        for (auto node_entry : m_nodes_by_node_id) {
            if (node_entry.value->node_type() != T::Type)
                continue;
            auto node = NonnullRefPtr<T> { *reinterpret_cast<WidgetNode*>(node_entry.value.ptr()) };
            if (predicate(node))
                TRY(results.try_append(node));
        }
        return results;
    }

private:
    Codec(Controller& controller, u8 codec_address)
        : m_controller(controller)
        , m_codec_address(codec_address)
    {
    }

    Controller& m_controller;
    u8 m_codec_address;
    RefPtr<RootNode> m_root_node;
    HashMap<u8, NonnullRefPtr<Node>> m_nodes_by_node_id;
};

}
