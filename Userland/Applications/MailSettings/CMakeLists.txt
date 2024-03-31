serenity_component(
    MailSettings
    RECOMMENDED
    TARGETS MailSettings
)

compile_gml(MailSettingsWidget.gml MailSettingsWidgetGML.cpp)

set(SOURCES
    main.cpp
    MailSettingsWidget.cpp
    MailSettingsWidgetGML.cpp
)

serenity_app(MailSettings ICON app-mail)
target_link_libraries(MailSettings PRIVATE LibConfig LibCore LibGfx LibGUI LibMain)
