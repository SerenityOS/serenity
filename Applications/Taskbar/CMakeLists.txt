set(SOURCES
    main.cpp
    TaskbarButton.cpp
    TaskbarWindow.cpp
    WindowList.cpp
)

serenity_app(Taskbar)
target_link_libraries(Taskbar LibGUI)
