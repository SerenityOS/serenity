serenity_component(
    WebDriver
    TARGETS WebDriver
)

set(SOURCES
    Client.cpp
    Session.cpp
    main.cpp
)

set(GENERATED_SOURCES
    ../../Application/Browser/WebDriverSessionClientEndpoint.h
    ../../Application/Browser/WebDriverSessionServerEndpoint.h
)

serenity_bin(WebDriver)
target_link_libraries(WebDriver LibCore LibHTTP LibMain LibIPC)
