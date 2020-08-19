set(SOURCES
    AddEventDialog.cpp
    main.cpp
)

serenity_bin(Calendar)
target_link_libraries(Calendar LibGUI)
