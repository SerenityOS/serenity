serenity_component(
    Presenter
    RECOMMENDED
    TARGETS Presenter
    DEPENDS FileSystemAccessServer
)


set(SOURCES
    main.cpp
    Presentation.cpp
    PresenterWidget.cpp
    Slide.cpp
    SlideObject.cpp
)
serenity_app(Presenter ICON app-presenter)
target_link_libraries(Presenter PRIVATE LibWebView LibGUI LibGfx LibFileSystemAccessClient LibCore LibDesktop LibMain LibURL)
