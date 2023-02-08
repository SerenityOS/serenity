serenity_component(
    MailSettings
    RECOMMENDED
    TARGETS MailSettings
)

stringify_gml(MailSettingsWidget.gml MailSettingsWidgetGML.h mail_settings_widget_gml)

set(SOURCES
    main.cpp
    MailSettingsWidget.cpp
)

set(GENERATED_SOURCES
    MailSettingsWidgetGML.h
)

serenity_app(MailSettings ICON app-mail)
target_link_libraries(MailSettings PRIVATE LibConfig LibCore LibGfx LibGUI LibMain)
