set(SOURCES
    ClipboardHistoryModel.cpp
    main.cpp
)

serenity_app(ClipboardHistory.Applet ICON edit-copy)
target_link_libraries(ClipboardHistory.Applet LibGUI LibCore LibGfx)
