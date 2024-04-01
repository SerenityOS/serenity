#include <LibMain/Main.h>
#include <LibWayland/Connection.h>
#include <LibWayland/wayland-protocol.h>

ErrorOr<int>
serenity_main(Main::Arguments)
{
    auto connection = TRY(Wayland::Connection::open());

    auto& display = connection->get_display();
    display.on_error = [](RefPtr<Wayland::Object>, uint32_t code, ByteString string) {
        warnln("Have Error: {}, {}", code, string);
    };

    auto registry = display.get_registry();

    registry->on_global = [&](uint32_t name, ByteString interface, uint32_t) {
        warnln("name: {}", interface);
        if (interface == Wayland::Shm::name()) {
            auto a = registry->bind<Wayland::Shm>(name);
        }
    };

    size_t idx = 10;

    while (idx > 0) {
        TRY(connection->write());
        ErrorOr<void> result;
        do {
            result = connection->read();
            if (!result.is_error()) {
                break;
            }
            auto error = result.release_error();
            if (error.code() == EAGAIN) {
                continue;
            }
            return error;
        } while (true);

        idx--;
    }

    return 0;
}
