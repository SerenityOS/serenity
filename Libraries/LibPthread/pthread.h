#pragma once

#include <sched.h>
#include <stdint.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

typedef int pthread_t;
typedef void* pthread_key_t;
typedef void* pthread_once_t;
typedef uint32_t pthread_mutex_t;
typedef void* pthread_attr_t;
typedef void* pthread_mutexattr_t;
typedef void* pthread_cond_t;
typedef void* pthread_spinlock_t;
typedef void* pthread_condattr_t;

int pthread_create(pthread_t*, pthread_attr_t*, void* (*)(void*), void*);
void pthread_exit(void*);
int pthread_kill(pthread_t, int);
void pthread_cleanup_push(void (*)(void*), void*);
void pthread_cleanup_pop(int);
int pthread_join(pthread_t, void**);
int pthread_mutex_lock(pthread_mutex_t*);
int pthread_mutex_trylock(pthread_mutex_t* mutex);
int pthread_mutex_unlock(pthread_mutex_t*);
int pthread_mutex_init(pthread_mutex_t*, const pthread_mutexattr_t*);
int pthread_mutex_destroy(pthread_mutex_t*);
int pthread_attr_init(pthread_attr_t*);
int pthread_attr_destroy(pthread_attr_t*);

int pthread_once(pthread_once_t*, void (*)(void));
#define PTHREAD_ONCE_INIT 0
void* pthread_getspecific(pthread_key_t key);
int pthread_setspecific(pthread_key_t key, const void* value);

#define PTHREAD_MUTEX_NORMAL 0
#define PTHREAD_MUTEX_RECURSIVE 1
#define PTHREAD_MUTEX_INITIALIZER 0
#define PTHREAD_COND_INITIALIZER 0

int pthread_key_create(pthread_key_t* key, void (*destructor)(void*));
int pthread_key_delete(pthread_key_t key);
int pthread_cond_broadcast(pthread_cond_t*);
int pthread_cond_init(pthread_cond_t*, const pthread_condattr_t*);
int pthread_cond_signal(pthread_cond_t*);
int pthread_cond_wait(pthread_cond_t*, pthread_mutex_t*);
int pthread_condattr_destroy(pthread_condattr_t*);
int pthread_cancel(pthread_t);
void pthread_cleanup_push(void (*)(void*), void*);
void pthread_cleanup_pop(int);
int pthread_cond_broadcast(pthread_cond_t*);
int pthread_cond_destroy(pthread_cond_t*);
int pthread_cond_timedwait(pthread_cond_t*, pthread_mutex_t*, const struct timespec*);
int pthread_cond_wait(pthread_cond_t*, pthread_mutex_t*);
int pthread_condattr_destroy(pthread_condattr_t*);

void pthread_testcancel(void);

int pthread_spin_destroy(pthread_spinlock_t*);
int pthread_spin_init(pthread_spinlock_t*, int);
int pthread_spin_lock(pthread_spinlock_t*);
int pthread_spin_trylock(pthread_spinlock_t*);
int pthread_spin_unlock(pthread_spinlock_t*);
int pthread_cond_destroy(pthread_cond_t*);
pthread_t pthread_self(void);
int pthread_detach(pthread_t);
int pthread_equal(pthread_t, pthread_t);
void pthread_exit(void*);
int pthread_mutexattr_init(pthread_mutexattr_t*);
int pthread_mutexattr_settype(pthread_mutexattr_t*, int);
int pthread_mutexattr_destroy(pthread_mutexattr_t*);

__END_DECLS
