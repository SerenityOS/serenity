/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/NonnullRefPtr.h>
#include <AK/QuickSelect.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Socket.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Size.h>
#include <LibImageDecoderClient/Client.h>
#include <LibMain/Main.h>

constexpr StringView get_command = "SIZE\n"sv;

// Which strategies to use for flooding the image to the server.
enum class FloodStrategy {
    // Send image row by row, like a CRT scanline.
    Scanline,
    // Send random pixels.
    Random,
    // Send image column by column.
    Columns,
};

static Optional<FloodStrategy> parse_strategy(StringView name)
{
    if (name.equals_ignoring_ascii_case("scanline"sv))
        return FloodStrategy::Scanline;
    if (name.equals_ignoring_ascii_case("random"sv))
        return FloodStrategy::Random;
    if (name.equals_ignoring_ascii_case("columns"sv))
        return FloodStrategy::Columns;
    return {};
}

// Client for the Pixelflut protocol
// https://github.com/defnull/pixelflut#pixelflut-protocol
class Client : public RefCounted<Client> {
public:
    static ErrorOr<NonnullRefPtr<Client>> create(StringView image_path, StringView server, size_t x, size_t y, FloodStrategy strategy);

    ErrorOr<void> run();

private:
    Client(NonnullOwnPtr<Core::BufferedTCPSocket>, NonnullRefPtr<Gfx::Bitmap>, Gfx::IntSize, Gfx::IntPoint, FloodStrategy);

    ErrorOr<void> send_current_pixel();
    void next_scanline_pixel();
    void next_random_pixel();
    void next_column_pixel();

    NonnullOwnPtr<Core::BufferedTCPSocket> m_socket;

    NonnullRefPtr<Gfx::Bitmap> const m_image;
    Gfx::IntSize const m_canvas_size;
    Gfx::IntPoint const m_image_offset;

    Gfx::IntPoint m_current_point { 0, 0 };
    FloodStrategy m_strategy;
};

ErrorOr<NonnullRefPtr<Client>> Client::create(StringView image_path, StringView server, size_t x, size_t y, FloodStrategy strategy)
{
    // Extract hostname and port and connect to server.
    auto parts = server.split_view(':');
    auto maybe_port = parts.take_last().to_number<unsigned>();
    if (!maybe_port.has_value())
        return Error::from_string_view("Invalid port number"sv);

    auto port = maybe_port.release_value();
    auto hostname = TRY(String::join(':', parts));

    auto socket = TRY(Core::BufferedTCPSocket::create(TRY(Core::TCPSocket::connect(hostname.to_byte_string(), port))));

    // Ask the server for the canvas size.
    TRY(socket->write_until_depleted(get_command.bytes()));
    auto buffer = TRY(ByteBuffer::create_zeroed(KiB));
    auto size_line = TRY(socket->read_line(buffer));
    if (!size_line.starts_with("SIZE"sv))
        return Error::from_string_view("Server didn't return size correctly"sv);

    auto size_parts = size_line.split_view(' ');
    auto maybe_width = size_parts[1].to_number<unsigned>();
    auto maybe_height = size_parts[2].to_number<unsigned>();
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
    auto image_promise = image_decoder->decode_image(byte_buffer, {}, {});
    auto image_result = TRY(image_promise->await());

    auto image = image_result.frames.take_first().bitmap;

    // Make sure to not draw out of bounds; some servers will disconnect us for that!
    if (image->width() > canvas_size.width()) {
        auto fitting_scale = static_cast<float>(canvas_size.width()) / image->width();
        image = TRY(image->scaled(fitting_scale, fitting_scale));
    }

    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Client(move(socket), move(image), canvas_size, Gfx::IntPoint { x, y }, strategy)));
}

Client::Client(NonnullOwnPtr<Core::BufferedTCPSocket> socket, NonnullRefPtr<Gfx::Bitmap> image, Gfx::IntSize canvas_size, Gfx::IntPoint image_offset, FloodStrategy strategy)
    : m_socket(move(socket))
    , m_image(move(image))
    , m_canvas_size(canvas_size)
    , m_image_offset(image_offset)
    , m_strategy(strategy)
{
    outln("Connected to server, image {}, canvas size {}", m_image, m_canvas_size);
}

ErrorOr<void> Client::run()
{
    Function<void(void)> next_pixel = [this] { this->next_scanline_pixel(); };
    if (m_strategy == FloodStrategy::Columns)
        next_pixel = [this] { this->next_column_pixel(); };
    else if (m_strategy == FloodStrategy::Random)
        next_pixel = [this] { this->next_random_pixel(); };

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
    auto hex = color.to_byte_string();
    // Pixelflut requires hex colors without leading hash.
    auto hex_without_hash = hex.substring(1);

    // PX <x> <y> <hex color>
    while (true) {
        auto result = m_socket->write_formatted("PX {} {} {}\n", m_current_point.x() + m_image_offset.x(), m_current_point.y() + m_image_offset.y(), hex_without_hash);
        // Very contested servers will cause frequent EAGAIN errors.
        if (!result.is_error() || result.error().code() != EAGAIN)
            break;
    }
    return {};
}

void Client::next_scanline_pixel()
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

void Client::next_column_pixel()
{
    m_current_point.set_y(m_current_point.y() + 1);
    if (m_current_point.y() >= m_image->height()) {
        m_current_point.set_y(0);
        m_current_point.set_x(m_current_point.x() + 1);
        if (m_current_point.x() >= m_image->width()) {
            m_current_point.set_x(0);
            m_current_point.set_y(0);
        }
    }
}

void Client::next_random_pixel()
{
    m_current_point = {
        AK::random_int(0, m_image->width() - 1),
        AK::random_int(0, m_image->height() - 1),
    };
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::EventLoop loop;

    StringView image_path;
    size_t x;
    size_t y;
    StringView server;
    StringView strategy_string { "scanline"sv };

    Core::ArgsParser args_parser;
    args_parser.add_option(image_path, "Image to send to server", "image", 'i', "IMAGE");
    args_parser.add_option(x, "Target x coordinate of the image on the server", "x", 'x', "X");
    args_parser.add_option(y, "Target y coordinate of the image on the server", "y", 'y', "Y");
    args_parser.add_option(strategy_string, "Pixel flooding strategy (scanline, random or column; default: scanline)", "strategy", 'm', "STRATEGY");
    args_parser.add_positional_argument(server, "Pixelflut server (hostname:port)", "server");
    args_parser.parse(arguments);

    if (image_path.is_empty()) {
        warnln("Error: -i argument is required");
        return 1;
    }

    auto maybe_strategy = parse_strategy(strategy_string);
    if (!maybe_strategy.has_value()) {
        warnln("Error: Strategy {} invalid", strategy_string);
        return 1;
    }

    auto client = TRY(Client::create(image_path, server, x, y, maybe_strategy.release_value()));

    TRY(client->run());

    return 0;
}
