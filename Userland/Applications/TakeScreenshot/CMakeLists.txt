serenity_component(
    TakeScreenshot
    RECOMMENDED
    TARGETS TakeScreenshot
)

compile_gml(TakeScreenshot.gml TakeScreenshotGML.cpp)

set(SOURCES
    TakeScreenshotGML.cpp
    MainWindow.cpp
    main.cpp
)

serenity_app(TakeScreenshot ICON app-take-screenshot)
target_link_libraries(TakeScreenshot PRIVATE LibCore LibGfx LibGUI LibMain)
