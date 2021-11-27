serenity_component(
    MailSettings
    RECOMMENDED
    TARGETS MailSettings
)

compile_gml(MailSettingsWidget.gml MailSettingsWidgetGML.h mail_settings_widget_gml)

set(SOURCES
    main.cpp
    MailSettingsWidget.cpp
    MailSettingsWidget.h
    MailSettingsWidgetGML.h
)

serenity_app(MailSettings ICON app-mail)
target_link_libraries(MailSettings LibConfig LibGUI LibMain)
