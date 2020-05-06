set(SOURCES
    DisassemblyModel.cpp
    main.cpp
    Profile.cpp
    ProfileModel.cpp
    ProfileTimelineWidget.cpp
)

serenity_bin(ProfileViewer)
target_link_libraries(ProfileViewer LibGUI LibX86)
