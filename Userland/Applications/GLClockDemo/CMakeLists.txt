serenity_component(
        GLClockDemo
        RECOMMENDED
        TARGETS GLClockDemo
)

compile_gml(GLClockDemoWindow.gml GLClockDemoWindow.cpp)

set(SOURCES
        main.cpp
        GLClockDemoWindow.cpp
)

serenity_app(GLClockDemo ICON app-3d-file-viewer)
target_link_libraries(GLClockDemo PRIVATE LibCore LibGfx LibGUI LibGL LibMain )
