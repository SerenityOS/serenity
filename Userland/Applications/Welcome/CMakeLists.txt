serenity_component(
    Welcome
    TARGETS Welcome
)

compile_gml(WelcomeWindow.gml WelcomeWindowGML.h welcome_window_gml)

set(SOURCES
    WelcomeWindowGML.h
    WelcomeWidget.cpp
    WelcomeWidget.h
    main.cpp
)

serenity_app(Welcome ICON app-welcome)
target_link_libraries(Welcome LibGUI LibWeb LibMain)
