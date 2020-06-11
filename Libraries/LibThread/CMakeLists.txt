set(SOURCES
	BackgroundAction.cpp
	ConditionVariable.cpp
	LockGuard.cpp
	Mutex.cpp
	Thread.cpp
	UniqueLock.cpp
)

serenity_lib(LibThread thread)
target_link_libraries(LibThread LibC LibCore LibPthread)
