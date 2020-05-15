set(SOURCES
    ClipboardHistoryModel.cpp
    IconWidget.cpp
    main.cpp
)

serenity_bin(ClipboardHistory.MenuApplet)
target_link_libraries(ClipboardHistory.MenuApplet LibGUI LibCore LibGfx)
