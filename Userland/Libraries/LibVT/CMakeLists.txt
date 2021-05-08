# FIXME: this assumes that EscapeSequenceStateMachine.h has been
# already generated when the kernel was built. This will probably
# mess builds up later on.
set(SOURCES
    Line.cpp
    Terminal.cpp
    TerminalWidget.cpp
    EscapeSequenceParser.cpp
)

serenity_lib(LibVT vt)
target_link_libraries(LibVT LibC LibCore LibGUI LibGfx LibDesktop)
