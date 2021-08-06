serenity_component(
    XServer
    TARGETS XServer
)

set(SOURCES
    Client.cpp
    main.cpp
    Protocol.cpp
    ScreenLayout.cpp
)

serenity_bin(XServer)
target_link_libraries(XServer LibCore LibGfx LibIPC)
