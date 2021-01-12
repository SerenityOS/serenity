set(SOURCES
    main.cpp
)

serenity_bin(Debugger)
target_link_libraries(Debugger LibCore LibDebug LibX86 LibLine)
