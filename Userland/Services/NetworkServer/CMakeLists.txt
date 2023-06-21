serenity_component(
    NetworkServer
    REQUIRED
    TARGETS NetworkServer
)

set(SOURCES
    main.cpp
)

serenity_bin(NetworkServer)
target_link_libraries(NetworkServer PRIVATE LibCore LibConfig LibMain)
