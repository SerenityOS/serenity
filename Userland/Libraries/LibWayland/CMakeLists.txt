set(SOURCES
    Connection.cpp
    Message.cpp
)

compile_wayland_protocol(wayland.xml wayland)

set(GENERATED_SOURCES
    wayland-protocol
)

serenity_lib(LibWayland wayland)
target_link_libraries(LibWayland PRIVATE LibCore)


set(SOURCES
    Testing.cpp
)
set(GENERATED_SOURCES
)
serenity_bin(WaylandTest waylandtest)
target_link_libraries(WaylandTest PRIVATE LibWayland LibMain LibCore)
