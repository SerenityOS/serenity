serenity_component(
    Welcome
    TARGETS Welcome
    DEPENDS Help WebContent
)

compile_gml(WelcomeWindow.gml WelcomeWindowGML.cpp)

set(SOURCES
    WelcomeWindowGML.cpp
    WelcomeWidget.cpp
    main.cpp
)

serenity_app(Welcome ICON app-welcome)
target_link_libraries(Welcome PRIVATE LibConfig LibCore LibGfx LibGUI LibWebView LibWeb LibMain LibURL)
