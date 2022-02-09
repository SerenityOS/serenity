serenity_component(
    Debugger
    TARGETS Debugger
)

set(SOURCES
    main.cpp
)

serenity_bin(Debugger)
target_link_libraries(Debugger LibCore LibDebug LibLine LibMain LibX86)
