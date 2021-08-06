/*
 * Copyright (c) 2021, Peter Elliott <pelliott@ualberta.ca>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Protocol.h"

namespace X {

static void pad(ByteBuffer& buffer, size_t n)
{
    buffer.resize(buffer.size() + n);
}

template<>
void serialize(ByteBuffer& buffer, ConnectionSetupSuccess::Format const& format)
{
    serialize(buffer, format.depth);
    serialize(buffer, format.bits_per_pixel);
    serialize(buffer, format.scanline_pad);

    pad(buffer, 5);
}

template<>
void serialize(ByteBuffer& buffer, ConnectionSetupSuccess::VisualType const& visual)
{
    serialize(buffer, visual.visual_id);
    serialize(buffer, visual.clas);
    serialize(buffer, visual.bits_per_rgb_value);
    serialize(buffer, visual.colormap_entries);
    serialize(buffer, visual.red_mask);
    serialize(buffer, visual.green_mask);
    serialize(buffer, visual.blue_mask);

    pad(buffer, 4);
}

template<>
size_t wire_sizeof(ConnectionSetupSuccess::VisualType const&)
{
    return 24;
}

template<>
void serialize(ByteBuffer& buffer, ConnectionSetupSuccess::Depth const& depth)
{
    serialize(buffer, depth.depth);
    pad(buffer, 1);
    serialize<Card16>(buffer, depth.visuals.size());
    pad(buffer, 4);
    serialize(buffer, depth.visuals);
}

template<>
size_t wire_sizeof(ConnectionSetupSuccess::Depth const& depth)
{
    return 8 + wire_sizeof(depth.visuals);
}

template<>
void serialize(ByteBuffer& buffer, ConnectionSetupSuccess::Screen const& screen)
{
    serialize(buffer, screen.root);
    serialize(buffer, screen.default_colormap);
    serialize(buffer, screen.white_pixel);
    serialize(buffer, screen.black_pixel);
    serialize(buffer, screen.current_input_masks);
    serialize(buffer, screen.width_in_pixels);
    serialize(buffer, screen.height_in_pixels);
    serialize(buffer, screen.width_in_millimeters);
    serialize(buffer, screen.height_in_millimeters);
    serialize(buffer, screen.min_installed_maps);
    serialize(buffer, screen.max_installed_maps);
    serialize(buffer, screen.root_visual);
    serialize(buffer, screen.backing_stores);
    serialize(buffer, screen.save_unders);
    serialize(buffer, screen.root_depth);
    serialize<Card8>(buffer, screen.allowed_depths.size());
    serialize(buffer, screen.allowed_depths);
}

template<>
size_t wire_sizeof(ConnectionSetupSuccess::Screen const& screen)
{
    return 40 + wire_sizeof(screen.allowed_depths);
}

template<>
void serialize(ByteBuffer& buffer, ConnectionSetupSuccess const& setup)
{
    serialize(buffer, Success::Success);
    pad(buffer, 1);

    serialize(buffer, setup.protocol_major_version);
    serialize(buffer, setup.protocol_minor_version);

    // This is essentialy the remaining length of the packet.
    auto bytes_left = 8 + 2 * setup.pixmap_formats.size() + (aligned(4, setup.vendor.size()) + wire_sizeof(setup.roots)) / 4;
    buffer.ensure_capacity(buffer.size() + bytes_left);
    serialize<Card16>(buffer, bytes_left);

    serialize(buffer, setup.release_number);
    serialize(buffer, setup.resource_id_base);
    serialize(buffer, setup.resource_id_mask);
    serialize(buffer, setup.motion_buffer_size);
    serialize<Card16>(buffer, setup.vendor.size());
    serialize(buffer, setup.maximum_request_length);
    serialize<Card8>(buffer, setup.roots.size());
    serialize<Card8>(buffer, setup.pixmap_formats.size());
    serialize(buffer, setup.image_byte_order);
    serialize(buffer, setup.bitmap_bit_order);
    serialize(buffer, setup.bitmap_scanline_unit);
    serialize(buffer, setup.bitmap_scanline_pad);
    serialize(buffer, setup.min_keycode);
    serialize(buffer, setup.max_keycode);

    pad(buffer, 4);

    serialize(buffer, setup.vendor);
    pad(buffer, align(4, setup.vendor.size()));
    serialize(buffer, setup.pixmap_formats);
    serialize(buffer, setup.roots);
}

}
