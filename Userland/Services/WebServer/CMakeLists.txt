serenity_component(
    WebServer
    TARGETS WebServer
)

set(SOURCES
    Client.cpp
    Configuration.cpp
    main.cpp
)

serenity_bin(WebServer)
target_link_libraries(WebServer PRIVATE LibCore LibFileSystem LibHTTP LibMain LibURL)
