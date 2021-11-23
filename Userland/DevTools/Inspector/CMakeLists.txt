serenity_component(
    Inspector
    RECOMMENDED
    TARGETS Inspector
)

set(SOURCES
    main.cpp
    RemoteObject.cpp
    RemoteObjectGraphModel.cpp
    RemoteObjectPropertyModel.cpp
    RemoteProcess.cpp
)

serenity_app(Inspector ICON app-inspector)
target_link_libraries(Inspector LibDesktop LibGUI LibMain)
