serenity_component(
    SpiceAgent
    TARGETS SpiceAgent
)

set(SOURCES
    main.cpp
    FileTransferOperation.cpp
    Message.cpp
    SpiceAgent.cpp
)

serenity_bin(SpiceAgent)
target_link_libraries(SpiceAgent PRIVATE LibCore LibFileSystem LibGfx LibGUI LibMain LibURL)
add_dependencies(SpiceAgent Clipboard)
