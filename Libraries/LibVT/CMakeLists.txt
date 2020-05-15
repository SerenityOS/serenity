set(SOURCES
    Line.cpp
    Terminal.cpp
    TerminalWidget.cpp
)

serenity_lib(LibVT vt)
target_link_libraries(LibVT LibC LibCore LibGUI LibGfx LibDesktop)
