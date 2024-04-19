serenity_component(
    Screenshot
    RECOMMENDED
    TARGETS Screenshot
)

compile_gml(Screenshot.gml ScreenshotGML.cpp)

set(SOURCES
    ScreenshotGML.cpp
    MainWindow.cpp
    main.cpp
)

serenity_app(Screenshot ICON app-screenshot)
target_link_libraries(Screenshot PRIVATE LibCore LibConfig LibGfx LibGUI LibMain)
