serenity_component(
    WebDriver
    TARGETS WebDriver
)

set(SOURCES
    BrowserConnection.cpp
    Client.cpp
    Session.cpp
    main.cpp
)

set(GENERATED_SOURCES
    ../../Applications/Browser/WebDriverSessionClientEndpoint.h
    ../../Applications/Browser/WebDriverSessionServerEndpoint.h
)

serenity_bin(WebDriver)
target_link_libraries(WebDriver LibCore LibHTTP LibMain LibIPC)
