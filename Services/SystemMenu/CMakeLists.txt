set(SOURCES
    main.cpp
    PowerDialog.cpp
)

serenity_bin(SystemMenu)
target_link_libraries(SystemMenu LibGUI)
