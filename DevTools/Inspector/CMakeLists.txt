set(SOURCES
    main.cpp
    RemoteObject.cpp
    RemoteObjectGraphModel.cpp
    RemoteObjectPropertyModel.cpp
    RemoteProcess.cpp
)

serenity_app(Inspector)
target_link_libraries(Inspector LibGUI)
