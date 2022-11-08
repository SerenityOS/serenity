serenity_component(
    WebDriver
    TARGETS WebDriver
    DEPENDS WebContent
)

set(SOURCES
    BrowserConnection.cpp
    Client.cpp
    Session.cpp
    TimeoutsConfiguration.cpp
    WebContentConnection.cpp
    main.cpp
)

set(GENERATED_SOURCES
    ../../Applications/Browser/WebDriverSessionClientEndpoint.h
    ../../Applications/Browser/WebDriverSessionServerEndpoint.h
    ../../Services/WebContent/WebDriverClientEndpoint.h
    ../../Services/WebContent/WebDriverServerEndpoint.h
)

serenity_bin(WebDriver)
target_link_libraries(WebDriver PRIVATE LibCore LibHTTP LibMain LibIPC LibWeb LibGfx)
