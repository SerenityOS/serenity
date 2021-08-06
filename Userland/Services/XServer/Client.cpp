/*
 * Copyright (c) 2021, Peter Elliott <pelliott@ualberta.ca>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Client.h"

namespace X {

static constexpr Window root_window = 0x01000000;
static constexpr ColorMap true_colormap = 0x02000000;

static size_t s_clients = 0;

Client::Client(NonnullRefPtr<Core::Socket> socket, Core::Object* parent)
    : IPC::ServerConnection<WindowClientEndpoint, WindowServerEndpoint>(*this, "/tmp/portal/window", parent)
    , m_socket(socket)

{
    ++s_clients;
}

void Client::die()
{
    deferred_invoke([this](auto& object) {
        NonnullRefPtr protector { object };
        remove_from_parent();
        --s_clients;
        if (s_clients == 0)
            exit(0);
    });
}

void Client::do_handshake()
{
    m_socket->on_ready_to_read = [this] {
        auto endianness_byte = m_socket->read(sizeof(ByteOrderByte));
        if (endianness_byte.size() != sizeof(ByteOrderByte)) {
            die();
            return;
        }
        auto endianness = deserialize<ByteOrderByte>(endianness_byte, 0);

        // FIXME: Support variable endianness.
        if (endianness != ByteOrderByte::Little) {
            dbgln("Invalid Byte Order: '{}'", to_underlying(endianness));
            die();
            return;
        }

        auto setup = read_connection_setup();
        if (!setup.has_value()) {
            die();
            return;
        }

        if (setup.value().protocol_major_version != 11) {
            dbgln("Invalid X protocol version: {}.{} (Seriously? what year is it?)",
                setup.value().protocol_major_version,
                setup.value().protocol_minor_version);
            die();
            return;
        }

        if (!write_connection_success()) {
            dbgln("failed to write connection setup: {}", strerror(errno));
            die();
            return;
        }

        die();
    };
}

Optional<ConnectionSetup> Client::read_connection_setup()
{
    // For some reason X11 doesn't send the connection setup as a regular
    // message with a length, so we need to handle this one specially.
    auto buffer = m_socket->read(11);
    if (buffer.size() != 11)
        return {};

    ConnectionSetup setup;

    setup.protocol_major_version = deserialize<Card16>(buffer, 1);
    setup.protocol_minor_version = deserialize<Card16>(buffer, 3);

    auto auth_name_len = deserialize<Card16>(buffer, 5);
    auto auth_data_len = deserialize<Card16>(buffer, 7);

    auto auth_name_buffer = m_socket->read(aligned(4, auth_name_len));
    if (auth_name_buffer.size() < auth_name_len)
        return {};

    setup.authorization_protocol_name = String8::deserialize_n(auth_name_buffer, 0, auth_name_len);

    auto auth_data_buffer = m_socket->read(aligned(4, auth_data_len));
    if (auth_data_buffer.size() < auth_data_len)
        return {};

    setup.authorization_protocol_data = String8::deserialize_n(auth_data_buffer, 0, auth_data_len);

    return setup;
}

bool Client::write_connection_success()
{
    ConnectionSetupSuccess setup;

    setup.protocol_major_version = 11;
    setup.protocol_minor_version = 0;
    setup.vendor = "SerenityOS XServer" Xs8;
    setup.release_number = 0;
    setup.resource_id_base = 0;
    setup.resource_id_mask = 0x00ffffff;
    setup.image_byte_order = ConnectionSetupSuccess::ByteOrder::LSBFirst;
    setup.bitmap_scanline_unit = 32;
    setup.bitmap_scanline_pad = 32;
    setup.bitmap_bit_order = ConnectionSetupSuccess::BitmapBitOrder::LeastSignificant;
    setup.pixmap_formats = { { 32, 32, 0 } };

    auto screen_layout = get_screen_layout();
    Vector<ConnectionSetupSuccess::Screen> screens;
    for (auto& screen : screen_layout.screens) {
        screens.append(ConnectionSetupSuccess::Screen {
            .root = root_window,
            .width_in_pixels = static_cast<Card16>(screen.resolution.width()),
            .height_in_pixels = static_cast<Card16>(screen.resolution.height()),
            // FIXME: Calculate this somehow.
            .width_in_millimeters = 500,
            .height_in_millimeters = 500,
            .allowed_depths = {
                ConnectionSetupSuccess::Depth {
                    32,
                    { ConnectionSetupSuccess::VisualType {
                        .visual_id = 0,
                        .clas = ConnectionSetupSuccess::VisualType::Class::TrueColor,
                        .red_mask = 0x000000ff,
                        .green_mask = 0x0000ff00,
                        .blue_mask = 0x00ff0000,
                        .bits_per_rgb_value = 32,
                        .colormap_entries = 256,
                    } } } },
            .root_depth = 0,
            .root_visual = 0,
            .default_colormap = true_colormap,
            .white_pixel = 0x00ffffff,
            .black_pixel = 0x00000000,
            .min_installed_maps = 1,
            .max_installed_maps = 1,
            .backing_stores = ConnectionSetupSuccess::Screen::BackingStores::Always,
            .save_unders = true,
            .current_input_masks = SetOf<Event>(),
        });
    }
    setup.roots = ListOf<ConnectionSetupSuccess::Screen>(screens);
    setup.motion_buffer_size = 0;
    setup.maximum_request_length = NumericLimits<Card16>::max();
    setup.min_keycode = 8;
    setup.max_keycode = 255;

    // Serialize and write the message.
    auto buffer = serialize(setup);
    return m_socket->write(buffer);
}

void Client::fast_greet(Vector<Gfx::IntRect> const&, u32, u32, u32, Core::AnonymousBuffer const&, String const&, String const&, i32) { }
void Client::paint(i32, Gfx::IntSize const&, Vector<Gfx::IntRect> const&) { }
void Client::mouse_move(i32, Gfx::IntPoint const&, u32, u32, u32, i32, bool, Vector<String> const&) { }
void Client::mouse_down(i32, Gfx::IntPoint const&, u32, u32, u32, i32) { }
void Client::mouse_double_click(i32, Gfx::IntPoint const&, u32, u32, u32, i32) { }
void Client::mouse_up(i32, Gfx::IntPoint const&, u32, u32, u32, i32) { }
void Client::mouse_wheel(i32, Gfx::IntPoint const&, u32, u32, u32, i32) { }
void Client::window_entered(i32) { }
void Client::window_left(i32) { }
void Client::key_down(i32, u32, u32, u32, u32) { }
void Client::key_up(i32, u32, u32, u32, u32) { }
void Client::window_activated(i32) { }
void Client::window_deactivated(i32) { }
void Client::window_input_entered(i32) { }
void Client::window_input_left(i32) { }
void Client::window_close_request(i32) { }
void Client::window_resized(i32, Gfx::IntRect const&) { }
void Client::menu_item_activated(i32, u32) { }
void Client::menu_item_entered(i32, u32) { }
void Client::menu_item_left(i32, u32) { }
void Client::menu_visibility_did_change(i32, bool) { }
void Client::screen_rects_changed(Vector<Gfx::IntRect> const&, u32, u32, u32) { }
void Client::set_wallpaper_finished(bool) { }
void Client::drag_dropped(i32, Gfx::IntPoint const&, String const&, HashMap<String, ByteBuffer> const&) { }
void Client::drag_accepted() { }
void Client::drag_cancelled() { }
void Client::update_system_theme(Core::AnonymousBuffer const&) { }
void Client::update_system_fonts(String const&, String const&) { }
void Client::window_state_changed(i32, bool, bool) { }
void Client::display_link_notification() { }
void Client::ping() { }

}
