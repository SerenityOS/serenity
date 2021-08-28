serenity_component(
    MailSettings
    RECOMMENDED
    TARGETS MailSettings
)

compile_gml(MailSettingsWindow.gml MailSettingsWindowGML.h mail_settings_window_gml)

set(SOURCES
    main.cpp
    MailSettingsWindow.cpp
    MailSettingsWindow.h
    MailSettingsWindowGML.h
)

serenity_app(MailSettings ICON app-mail)
target_link_libraries(MailSettings LibConfig LibGUI)
