set(SOURCES
    main.cpp
    Profile.cpp
    ProfileModel.cpp
    ProfileTimelineWidget.cpp
)

serenity_app(ProfileViewer)
target_link_libraries(ProfileViewer LibGUI)
