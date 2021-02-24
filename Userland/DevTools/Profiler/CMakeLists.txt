set(SOURCES
    DisassemblyModel.cpp
    main.cpp
    Profile.cpp
    ProfileModel.cpp
    ProfileTimelineWidget.cpp
)

serenity_app(Profiler ICON app-profiler)
target_link_libraries(Profiler LibGUI LibDesktop LibX86 LibCoreDump)
