set(SOURCES
    Line.cpp
    Terminal.cpp
    TerminalWidget.cpp
)

serenity_lib(LibVT vt)
target_link_libraries(LibVT ${LIBRARY_LIBC} LibCore LibGUI LibGfx LibDesktop)
