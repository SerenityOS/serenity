set(SOURCES
    main.cpp
    ShutdownDialog.cpp
)

serenity_bin(SystemMenu)
target_link_libraries(SystemMenu LibGUI)
