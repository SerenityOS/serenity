serenity_component(
    SpiceAgent
    TARGETS SpiceAgent
)

set(SOURCES
    main.cpp
    Message.cpp
    SpiceAgent.cpp
    ConnectionToClipboardServer.cpp
)

serenity_bin(SpiceAgent)
target_link_libraries(SpiceAgent PRIVATE LibGfx LibCore LibIPC LibMain)
add_dependencies(SpiceAgent Clipboard)
