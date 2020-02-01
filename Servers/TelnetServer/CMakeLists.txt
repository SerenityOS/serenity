set(SOURCES
    main.cpp
    Client.cpp
    Parser.cpp
)

serenity_app(TelnetServer)
target_link_libraries(TelnetServer LibCore)
