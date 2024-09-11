# Coroutines and asynchronous streams

## Asynchronous resources

`AK::AsyncResource` class represents a generic resource (e. g. POSIX file descriptor, AsyncStream, HTTP response body) with a failible and/or asynchronous destructor. As an example of such destructor, an asynchronous socket might want to wait for all outstanding transfers to complete and want to notify the calling code if the server acknowledged all buffered writes.

When working with AsyncResource, the only thing you should pay attention to is not leaving the resource open when you are done with it. This is important both for consistency and for not signaling spurious end-of-data errors to the other party. This close/reset hygiene is not hard to achieve in practice: every AsyncResource is automatically reset during destruction and some resources reset on any error returned by any of their interfaces.

The above means that the following snippet is correct:

```cpp
Coroutine<ErrorOr<void>> do_very_meaningful_work()
{
    Core::AsyncTCPSocket socket = make_me_a_socket();
    // Core::AsyncTCPSocket is AK::AsyncStream (which is itself an AsyncResource, obviously), which
    // exhibits reset-on-error behavior.
    auto object = CO_TRY(co_await socket.read_object<VeryImportantObject>());
    auto response = CO_TRY(process_object(object));
    CO_TRY(co_await socket.write(response));
    CO_TRY(co_await socket.close());
}
```

and will send a TCP RST if any of the functions (including `process_object`) fails and will close the socket gracefully if everything goes well.

Of course, automatic-reset-on-destruction does not always get rid of explicit resets. If asynchronous resource is not local to a fallible function, you will have to call reset manually in case of errors, i. e.

```cpp
Coroutine<ErrorOr<void>> do_very_meaningful_work_second_time(AsyncStream& stream)
{
    auto object = CO_TRY(co_await stream.read_object<VeryImportantObject>());
    auto response_or_error = process_object(object);
    if (response_or_error.is_error()) {
        stream.reset();
        co_return response_or_error.release_error();
    }
    CO_TRY(co_await stream.write(response_or_error.release_value()));
}
```

It might be tempting to just do nothing with a stream in this situation. However, this will leave the stream in an unknown state if the function fails, which would necessitate `stream->is_open()` checks later on in the caller (because doing pretty much anything with a closed stream asserts).

As mentioned in the previous paragraph, in addition to `close` and `reset` methods, resources have an `is_open` method which checks if the resource has already been closed or reset (either explicitly or implicitly by a failed method). `is_open` state cannot change by itself with time (i. e. if a server sends RST when nobody is listening the socket)&mdash;one has to call a method on a resource for `open` state to change.

## Input streams

`AK::AsyncInputStream` is a base class for all asynchronous input streams.

Every AsyncInputStream is effectively buffered by design. This means that all input streams have an internal read buffer to which data is read and references to which are returned from `peek` and `read` in the form of ReadonlyBytes.

Typical workflow with AsyncInputStream consists of a number of peeks (until the caller finds what they are looking for in the stream) followed by a read that removes just-peeked object from the buffer.

The following example reads an object of an unknown size from the stream:

```cpp
Coroutine<ErrorOr<VeryImportantObject>> read_some_very_important_object(AsyncInputStream& stream)
{
    size_t byte_size;
    while (true) {
        auto bytes = CO_TRY(co_await stream.peek());
        Optional<size_t> maybe_size = figure_out_size_from_prefix(bytes);
        if (maybe_size.has_value()) {
            // Yay! We read enough to figure out how long the object is.
            byte_size = maybe_size.value();
            break;
        }
    }

    auto bytes = CO_TRY(co_await stream.read(byte_size));
    auto object_or_error = parse_object_from_data(bytes);
    if (object_or_error.is_error()) {
        stream.reset();
        co_return object_or_error.release_error();
    }
    co_return object_or_error.release_value();
}
```

Of course, if we know size upfront, reading stuff from a stream is as easy as `stream->read(size)`.

> [!IMPORTANT]
> Note that you should never peek more than is absolutely necessary.

More formally, you should ensure the following: at the time of `read`, let $(s_1, s_2, ..., s_n)$ be a sequence of lengths of ReadonlyBytes views returned from `peek` or `peek_or_eof` since the last read (or since the creation of the stream, if there were no previous reads). If $n \le 1$, then any length is allowed. Otherwise, if $n > 1$, $s_{n - 1}$ MUST NOT be greater than `bytes` parameter. Moreover, if the stream data does not have sub-byte structure and EOF has not been reached, `bytes` SHOULD be greater than $s_{n - 1}$. While the said condition might seem arbitrary, violation of it almost always indicates a bug in the caller's code. The asynchronous streams framework doesn't guarantee linear asymptotic runtime complexity in the case of length condition violation.

It is not hard to fulfill the condition in practice. For example, in the `read_some_very_important_object` example, the condition just requires `figure_out_size_from_prefix` to figure out size if the object is already fully inside `bytes` parameter.

Note that `read` always returns exactly `bytes` bytes. If `bytes` is larger than $s_n$ or $n$ is 0, `read` will return more data than previously peeked (by reading the stream, obviously).

If your use-case doesn't require the knowledge of EOF location (i. e. you know how much to read upfront without relying on EOF), then just use `peek`, `read`, and `close` in the end (if you own the stream, of course). `peek` or `read` will reset the stream and error out if input ended prematurely because of EOF. Likewise, `close` will error out if the data is left after the stream supposedly should have been fully read. EIO is returned for the unexpected stream end and EBUSY&mdash;for not reading the whole stream.

For the EOF-aware applications, AsyncInputStream provides `peek_or_eof`. In contrast to `peek`, `peek_or_eof` returns an additional flag indicating if EOF has been reached. Note that the EOF detection is similar to POSIX streams: first, `peek_or_eof` returns data up to EOF without `is_eof` flag being set, and then, on the next call, it returns the same data with `is_eof` flag set.

As an example, let us abuse the said functionality of `peek_or_eof` to implement a completely reliable `is_eof` for an asynchronous stream:

```cpp
Coroutine<ErrorOr<bool>> accurate_is_eof(AsyncInputStream& stream) {
    auto [_, is_eof] = CO_TRY(co_await stream.peek_or_eof());
    must_sync(stream.read(0));
    co_return is_eof;
}
```

You might notice a seemingly useless read of 0 bytes here. Why do we need it? Well, let us remove it and consider the case when we have a stream with an empty buffer and a single byte left overall. `peek_or_eof` will return that byte without `is_eof` flag set, so `accurate_is_eof` returns false. Next, someone tries to peek that byte with a plain `peek` that will in turn call `peek_or_eof`, which now _will_ set the `is_eof` flag. The flag will then be checked by `peek` and the stream will error out with EIO.

Therefore,

> [!IMPORTANT]
> If you peek something from a stream, you should read it.

What follows is a more formal specification of `peek`, `peek_or_eof`, and `read` behavior. The implementation of them might seem simple but it has a lot of conceptual complexity packed into it.

Each `peek_or_eof` call is classified as either reading or non-reading peek. Reading peeks always read new data, while non-reading peeks only do so when there's no data available in the buffer. In the former case, non-reading peek is called a no-op peek and, in the later case, peek is called a promoted peek. Peek occurring after another peek is always a reading peek while a peek occurring just after `read` call is non-reading. Note that this gives rise to the aforementioned length condition: peek, in some way, is always a request for new unseen data, and an unnecessary peek can, for example, unintentionally read EOF and error out.

Promoted and reading peeks read data from the underlying stream and check if EOF has been encountered (i. e. read returned 0 new bytes). If it was not, peek adds read data to the buffer. Finally, regardless of its type, peek returns a view to the buffer.

Calling `peek` when the EOF has been reached as well as calling `read` that tries to read past EOF is a protocol violation and results in EIO error. Note that EIO (just like any other error) causes the stream to reset, so if one attempts to continue reading from the stream, it will assert. Next, calling read operations concurrently asserts since it is a logic error. And finally, calling `peek`, `peek_or_eof`, or `read` on a stream that isn't open is a logic error as well and asserts too.
