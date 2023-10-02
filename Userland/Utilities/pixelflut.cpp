/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/NonnullRefPtr.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Socket.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Size.h>
#include <LibImageDecoderClient/Client.h>
#include <LibMain/Main.h>

constexpr StringView get_command = "SIZE\n"sv;

// Client for the Pixelflut protocol
// https://github.com/defnull/pixelflut#pixelflut-protocol
class Client : public RefCounted<Client> {
public:
    static ErrorOr<NonnullRefPtr<Client>> create(StringView image_path, StringView server, size_t x, size_t y);

    ErrorOr<void> run();

private:
    Client(NonnullOwnPtr<Core::BufferedTCPSocket>, NonnullRefPtr<Gfx::Bitmap>, Gfx::IntSize, Gfx::IntPoint);

    ErrorOr<void> send_current_pixel();
    void next_pixel();

    NonnullOwnPtr<Core::BufferedTCPSocket> m_socket;

    NonnullRefPtr<Gfx::Bitmap> const m_image;
    Gfx::IntSize const m_canvas_size;
    Gfx::IntPoint const m_image_offset;

    Gfx::IntPoint m_current_point { 0, 0 };
};

ErrorOr<NonnullRefPtr<Client>> Client::create(StringView image_path, StringView server, size_t x, size_t y)
{
    // Extract hostname and port and connect to server.
    auto parts = server.split_view(':');
    auto maybe_port = parts.take_last().to_uint();
    if (!maybe_port.has_value())
        return Error::from_string_view("Invalid port number"sv);

    auto port = maybe_port.release_value();
    auto hostname = TRY(String::join(':', parts));

    auto socket = TRY(Core::BufferedTCPSocket::create(TRY(Core::TCPSocket::connect(hostname.to_deprecated_string(), port))));

    // Ask the server for the canvas size.
    TRY(socket->write_until_depleted(get_command.bytes()));
    auto buffer = TRY(ByteBuffer::create_zeroed(KiB));
    auto size_line = TRY(socket->read_line(buffer));
    if (!size_line.starts_with("SIZE"sv))
        return Error::from_string_view("Server didn't return size correctly"sv);

    auto size_parts = size_line.split_view(' ');
    auto maybe_width = size_parts[1].to_uint();
    auto maybe_height = size_parts[2].to_uint();
    if (!maybe_width.has_value() || !maybe_height.has_value())
        return Error::from_string_view("Width or height invalid"sv);

    auto width = maybe_width.release_value();
    auto height = maybe_height.release_value();
    Gfx::IntSize canvas_size = { width, height };

    // Read input image.
    auto image_file = TRY(Core::File::open(image_path, Core::File::OpenMode::Read));
    auto image_decoder = TRY(ImageDecoderClient::Client::try_create());
    ScopeGuard guard = [&] { image_decoder->shutdown(); };

    auto byte_buffer = TRY(image_file->read_until_eof(16 * KiB));
    auto maybe_image = image_decoder->decode_image(byte_buffer);

    if (!maybe_image.has_value())
        return Error::from_string_view("Image could not be read"sv);

    auto image = maybe_image->frames.take_first().bitmap;

    // Make sure to not draw out of bounds; some servers will disconnect us for that!
    if (image->width() > canvas_size.width()) {
        auto fitting_scale = static_cast<float>(canvas_size.width()) / image->width();
        image = TRY(image->scaled(fitting_scale, fitting_scale));
    }

    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Client(move(socket), move(image), canvas_size, Gfx::IntPoint { x, y })));
}

Client::Client(NonnullOwnPtr<Core::BufferedTCPSocket> socket, NonnullRefPtr<Gfx::Bitmap> image, Gfx::IntSize canvas_size, Gfx::IntPoint image_offset)
    : m_socket(move(socket))
    , m_image(move(image))
    , m_canvas_size(canvas_size)
    , m_image_offset(image_offset)
{
    outln("Connected to server, image {}, canvas size {}", m_image, m_canvas_size);
}

ErrorOr<void> Client::run()
{
    while (true) {
        TRY(send_current_pixel());
        next_pixel();
    }
}

ErrorOr<void> Client::send_current_pixel()
{
    auto color = m_image->get_pixel(m_current_point);
    if (color.alpha() == 0)
        return {};
    auto hex = color.to_deprecated_string();
    // Pixelflut requires hex colors without leading hash.
    auto hex_without_hash = hex.substring(1);

    // PX <x> <y> <hex color>
    TRY(m_socket->write_formatted("PX {} {} {}\n", m_current_point.x() + m_image_offset.x(), m_current_point.y() + m_image_offset.y(), hex_without_hash));
    return {};
}

void Client::next_pixel()
{
    m_current_point.set_x(m_current_point.x() + 1);
    if (m_current_point.x() >= m_image->width()) {
        m_current_point.set_x(0);
        m_current_point.set_y(m_current_point.y() + 1);
        if (m_current_point.y() >= m_image->height()) {
            m_current_point.set_x(0);
            m_current_point.set_y(0);
        }
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::EventLoop loop;

    StringView image_path;
    size_t x;
    size_t y;
    StringView server;

    Core::ArgsParser args_parser;
    args_parser.add_option(image_path, "Image to send to server", "image", 'i', "IMAGE");
    args_parser.add_option(x, "Target x coordinate of the image on the server", "x", 'x', "X");
    args_parser.add_option(y, "Target y coordinate of the image on the server", "y", 'y', "Y");
    args_parser.add_positional_argument(server, "Pixelflut server (hostname:port)", "server");
    args_parser.parse(arguments);

    if (image_path.is_empty()) {
        warnln("Error: -i argument is required");
        return 1;
    }

    auto client = TRY(Client::create(image_path, server, x, y));

    TRY(client->run());

    return 0;
}
