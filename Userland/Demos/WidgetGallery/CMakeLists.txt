compile_gml(DemoWizardPage1.gml DemoWizardPage1GML.h demo_wizard_page_1_gml)
compile_gml(DemoWizardPage2.gml DemoWizardPage2GML.h demo_wizard_page_2_gml)

set(SOURCES
    main.cpp
    DemoWizardDialog.cpp
    DemoWizardPage1GML.h
    DemoWizardPage2GML.h
)

serenity_app(WidgetGallery ICON app-widget-gallery)
target_link_libraries(WidgetGallery LibGUI)
