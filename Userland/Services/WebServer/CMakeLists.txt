set(SOURCES
    Client.cpp
    Configuration.cpp
    main.cpp
)

serenity_bin(WebServer)
target_link_libraries(WebServer LibCore LibHTTP)
