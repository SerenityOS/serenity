/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <Kernel/Devices/Audio/Channel.h>
#include <Kernel/Devices/Audio/IntelHDA/Stream.h>
#include <Kernel/Library/KString.h>

namespace Kernel::Audio::IntelHDA {

class WidgetNode;

class OutputPath {
public:
    static constexpr u8 fixed_pcm_bits = 16;
    static constexpr u8 fixed_channel_count = 2;

    static ErrorOr<NonnullOwnPtr<OutputPath>> create(Vector<NonnullRefPtr<WidgetNode>> widget_path, NonnullOwnPtr<OutputStream> output_stream)
    {
        return adopt_nonnull_own_or_enomem(new (nothrow) OutputPath(move(widget_path), move(output_stream)));
    }

    OutputStream& output_stream() { return *m_output_stream; }

    ErrorOr<void> activate()
    {
        // Power on the function group and all widgets that support it
        auto output_widget = get<WidgetNode::WidgetType::AudioOutput>();
        auto group = output_widget->parent_node();
        TRY(group->set_power_state(Node::PowerState::D0));
        for (auto& widget : m_widget_path) {
            if (widget->power_control_supported())
                TRY(widget->set_power_state(Node::PowerState::D0));
        }

        // Link the audio output widget to the output stream number and first channel
        TRY(output_widget->set_converter_stream_and_channel(m_output_stream->stream_number(), OutputStream::fixed_channel));

        // Set full volume for all output amplifiers in the path
        for (auto& widget : m_widget_path) {
            if (!widget->output_amp_present())
                continue;

            // NOTE: setting gain to the offset means 0dB attenuation / 100% volume
            TRY(widget->set_amplifier_gain_mute({
                .mute = false,
                .gain = widget->output_amp_capabilities().offset,
            }));
        }

        // Walk through pairs of widgets and connect them to each other
        for (size_t i = 0; i < m_widget_path.size() - 1; ++i) {
            auto left_widget = m_widget_path[i];
            auto right_widget = m_widget_path[i + 1];

            VERIFY(left_widget->connection_list_present());
            if (left_widget->connection_list().size() == 1) {
                // If there is only one possible connection, it is fixed and we cannot change it.
                VERIFY(left_widget->connection_selected_node_id() == right_widget->node_id());
            } else {
                // Find the index of the right widget node id in the connection list
                size_t connection_index = 0;
                for (auto connection_node_id : left_widget->connection_list()) {
                    if (connection_node_id == right_widget->node_id())
                        break;
                    ++connection_index;
                }
                VERIFY(connection_index < left_widget->connection_list().size());

                // Select this index
                TRY(left_widget->set_connection_select(connection_index));
            }
        }

        // Enable pin complex output
        auto pin_widget = get<WidgetNode::WidgetType::PinComplex>();
        TRY(pin_widget->set_pin_control({ .output_enabled = true }));

        // Finally, retrieve the active converter format for the output widget and set the same for our output stream
        auto converter_format = TRY(output_widget->get_converter_format());
        TRY(set_format(converter_format));
        return {};
    }

    ErrorOr<void> set_format(FormatParameters format)
    {
        // FIXME: support other PCM bit sizes and channel counts
        format.pcm_bits = fixed_pcm_bits;
        format.number_of_channels = fixed_channel_count;

        // 7.3.3.8: Converter Format
        // "The Converter Format control determines the format the converter will use. This must match the
        // format programmed into the Stream Descriptor on the controller so that the data format being
        // transmitted on the link matches what is expected by the consumer of the data."
        auto output_widget = get<WidgetNode::WidgetType::AudioOutput>();
        if (!output_widget->supported_pcm_rates().contains_slow(format.sample_rate)
            || !output_widget->supported_pcm_sizes().contains_slow(format.pcm_bits)
            || format.number_of_channels > output_widget->channel_count())
            return ENOTSUP;

        TRY(m_output_stream->set_format(format));
        TRY(output_widget->set_converter_format(format));
        return {};
    }

    ErrorOr<NonnullOwnPtr<KString>> to_string()
    {
        StringBuilder builder;
        TRY(builder.try_append("OutputPath: ["sv));
        for (size_t i = 0; i < m_widget_path.size(); ++i) {
            auto widget = m_widget_path[i];
            TRY(builder.try_append(TRY(widget->to_string())->view()));
            if (i < m_widget_path.size() - 1)
                TRY(builder.try_append(" → "sv));
        }
        TRY(builder.try_append(']'));
        return KString::try_create(builder.string_view());
    }

private:
    OutputPath(Vector<NonnullRefPtr<WidgetNode>> widget_path, NonnullOwnPtr<OutputStream> output_stream)
        : m_widget_path(move(widget_path))
        , m_output_stream(move(output_stream))
    {
    }

    template<WidgetNode::WidgetType T>
    NonnullRefPtr<WidgetNode> get()
    {
        for (auto& widget : m_widget_path) {
            if (widget->widget_type() == T)
                return widget;
        }
        VERIFY_NOT_REACHED();
    }

    Vector<NonnullRefPtr<WidgetNode>> m_widget_path;
    NonnullOwnPtr<OutputStream> m_output_stream;
};

}
