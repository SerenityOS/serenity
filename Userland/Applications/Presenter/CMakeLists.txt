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
serenity_app(Presenter ICON app-display-settings)
target_link_libraries(Presenter PRIVATE LibWebView LibGUI LibGfx LibFileSystemAccessClient LibCore LibMain)
