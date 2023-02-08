serenity_component(
    Welcome
    TARGETS Welcome
    DEPENDS Help WebContent
)

stringify_gml(WelcomeWindow.gml WelcomeWindowGML.h welcome_window_gml)

set(SOURCES
    WelcomeWidget.cpp
    main.cpp
)

set(GENERATED_SOURCES
    WelcomeWindowGML.h
)

serenity_app(Welcome ICON app-welcome)
target_link_libraries(Welcome PRIVATE LibConfig LibCore LibGfx LibGUI LibWebView LibWeb LibMain)
