set(SOURCES
    main.cpp
    WebContentView.cpp
    WebContentClient.cpp
)

set(GENERATED_SOURCES
    ../../Services/WebContent/WebContentClientEndpoint.h
    ../../Services/WebContent/WebContentServerEndpoint.h
)

serenity_bin(WebView)
target_link_libraries(WebView LibGUI)
