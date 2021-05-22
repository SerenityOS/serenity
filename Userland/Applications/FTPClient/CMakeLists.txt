serenity_component(
    FTPClient
    RECOMMENDED
    TARGETS FTPClient
)

set(SOURCES
    FTPClient.cpp
    main.cpp
)

serenity_app(FTPClient ICON app-irc-client)
target_link_libraries(FTPClient LibWeb)
