serenity_component(
    VirGLDemo
    TARGETS VirGLDemo
)

set(SOURCES
    CommandBufferBuilder.cpp
    VirGLDemo.cpp
    Widget.cpp
)

serenity_app(VirGLDemo ICON app-cube)
target_link_libraries(VirGLDemo LibMain LibGUI)
