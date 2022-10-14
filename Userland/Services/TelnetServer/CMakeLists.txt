serenity_component(
    TelnetServer
    TARGETS TelnetServer
)

set(SOURCES
    Client.cpp
    main.cpp
    Parser.cpp
)

serenity_bin(TelnetServer)
target_link_libraries(TelnetServer PRIVATE LibCore LibMain)
