set(SOURCES
    Line.cpp
    Terminal.cpp
    TerminalWidget.cpp
    EscapeSequenceParser.cpp
)

set(GENERATED_SOURCES
    EscapeSequenceStateMachine.h
)

generate_state_machine(StateMachine.txt EscapeSequenceStateMachine.h)
serenity_lib(LibVT vt)
target_link_libraries(LibVT PRIVATE LibCore LibGUI LibGfx LibDesktop LibConfig LibURL)
