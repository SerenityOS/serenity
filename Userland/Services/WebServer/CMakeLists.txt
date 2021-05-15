set(SOURCES
    Client.cpp
    main.cpp
)

serenity_bin(WebServer)
target_link_libraries(WebServer LibCore LibHTTP)
