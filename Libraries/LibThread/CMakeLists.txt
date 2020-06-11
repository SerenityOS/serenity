set(SOURCES
		BackgroundAction.cpp
		Thread.cpp
		Mutex.cpp
		UniqueLock.cpp
		LockGuard.cpp
		ConditionVariable.cpp
		)

serenity_lib(LibThread thread)
target_link_libraries(LibThread LibC LibCore LibPthread)
