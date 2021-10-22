serenity_component(
    SystemServer
    REQUIRED
    TARGETS SystemServer
)

set(SOURCES
    main.cpp
    Service.cpp
    SystemServer.cpp
)

serenity_bin(SystemServer)
target_link_libraries(SystemServer LibCore)
