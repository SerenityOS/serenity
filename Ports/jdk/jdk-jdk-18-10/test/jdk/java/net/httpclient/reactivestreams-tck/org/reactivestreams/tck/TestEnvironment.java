/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.reactivestreams.tck;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.flow.support.Optional;
import org.reactivestreams.tck.flow.support.SubscriberBufferOverflowException;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class TestEnvironment {
  public static final int TEST_BUFFER_SIZE = 16;

  private static final String DEFAULT_TIMEOUT_MILLIS_ENV = "DEFAULT_TIMEOUT_MILLIS";
  private static final long DEFAULT_TIMEOUT_MILLIS = 100;

  private static final String DEFAULT_NO_SIGNALS_TIMEOUT_MILLIS_ENV = "DEFAULT_NO_SIGNALS_TIMEOUT_MILLIS";
  private static final String DEFAULT_POLL_TIMEOUT_MILLIS_ENV = "DEFAULT_POLL_TIMEOUT_MILLIS_ENV";

  private final long defaultTimeoutMillis;
  private final long defaultPollTimeoutMillis;
  private final long defaultNoSignalsTimeoutMillis;
  private final boolean printlnDebug;

  private CopyOnWriteArrayList<Throwable> asyncErrors = new CopyOnWriteArrayList<Throwable>();

  /**
   * Tests must specify the timeout for expected outcome of asynchronous
   * interactions. Longer timeout does not invalidate the correctness of
   * the implementation, but can in some cases result in longer time to
   * run the tests.
   * @param defaultTimeoutMillis default timeout to be used in all expect* methods
   * @param defaultNoSignalsTimeoutMillis default timeout to be used when no further signals are expected anymore
   * @param defaultPollTimeoutMillis default amount of time to poll for events if {@code defaultTimeoutMillis} isn't
    *                                preempted by an asynchronous event.
   * @param printlnDebug         if true, signals such as OnNext / Request / OnComplete etc will be printed to standard output,
   */
  public TestEnvironment(long defaultTimeoutMillis, long defaultNoSignalsTimeoutMillis, long defaultPollTimeoutMillis,
                         boolean printlnDebug) {
    this.defaultTimeoutMillis = defaultTimeoutMillis;
    this.defaultPollTimeoutMillis = defaultPollTimeoutMillis;
    this.defaultNoSignalsTimeoutMillis = defaultNoSignalsTimeoutMillis;
    this.printlnDebug = printlnDebug;
  }

  /**
   * Tests must specify the timeout for expected outcome of asynchronous
   * interactions. Longer timeout does not invalidate the correctness of
   * the implementation, but can in some cases result in longer time to
   * run the tests.
   * @param defaultTimeoutMillis default timeout to be used in all expect* methods
   * @param defaultNoSignalsTimeoutMillis default timeout to be used when no further signals are expected anymore
   * @param printlnDebug         if true, signals such as OnNext / Request / OnComplete etc will be printed to standard output,
   */
  public TestEnvironment(long defaultTimeoutMillis, long defaultNoSignalsTimeoutMillis, boolean printlnDebug) {
    this(defaultTimeoutMillis, defaultNoSignalsTimeoutMillis, defaultTimeoutMillis, printlnDebug);
  }

  /**
   * Tests must specify the timeout for expected outcome of asynchronous
   * interactions. Longer timeout does not invalidate the correctness of
   * the implementation, but can in some cases result in longer time to
   * run the tests.
   *
   * @param defaultTimeoutMillis default timeout to be used in all expect* methods
   * @param defaultNoSignalsTimeoutMillis default timeout to be used when no further signals are expected anymore
   * @param defaultPollTimeoutMillis default amount of time to poll for events if {@code defaultTimeoutMillis} isn't
   *                                 preempted by an asynchronous event.
   */
  public TestEnvironment(long defaultTimeoutMillis, long defaultNoSignalsTimeoutMillis, long defaultPollTimeoutMillis) {
      this(defaultTimeoutMillis, defaultNoSignalsTimeoutMillis, defaultPollTimeoutMillis, false);
  }

  /**
   * Tests must specify the timeout for expected outcome of asynchronous
   * interactions. Longer timeout does not invalidate the correctness of
   * the implementation, but can in some cases result in longer time to
   * run the tests.
   *
   * @param defaultTimeoutMillis default timeout to be used in all expect* methods
   * @param defaultNoSignalsTimeoutMillis default timeout to be used when no further signals are expected anymore
   */
  public TestEnvironment(long defaultTimeoutMillis, long defaultNoSignalsTimeoutMillis) {
    this(defaultTimeoutMillis, defaultTimeoutMillis, defaultNoSignalsTimeoutMillis);
  }

  /**
   * Tests must specify the timeout for expected outcome of asynchronous
   * interactions. Longer timeout does not invalidate the correctness of
   * the implementation, but can in some cases result in longer time to
   * run the tests.
   *
   * @param defaultTimeoutMillis default timeout to be used in all expect* methods
   */
  public TestEnvironment(long defaultTimeoutMillis) {
    this(defaultTimeoutMillis, defaultTimeoutMillis, defaultTimeoutMillis);
  }

  /**
   * Tests must specify the timeout for expected outcome of asynchronous
   * interactions. Longer timeout does not invalidate the correctness of
   * the implementation, but can in some cases result in longer time to
   * run the tests.
   *
   * The default timeout for all expect* methods will be obtained by either the env variable {@code DEFAULT_TIMEOUT_MILLIS}
   * or the default value ({@link TestEnvironment#DEFAULT_TIMEOUT_MILLIS}) will be used.
   *
   * @param printlnDebug if true, signals such as OnNext / Request / OnComplete etc will be printed to standard output,
   *                     often helpful to pinpoint simple race conditions etc.
   */
  public TestEnvironment(boolean printlnDebug) {
    this(envDefaultTimeoutMillis(), envDefaultNoSignalsTimeoutMillis(), envDefaultPollTimeoutMillis(), printlnDebug);
  }

  /**
   * Tests must specify the timeout for expected outcome of asynchronous
   * interactions. Longer timeout does not invalidate the correctness of
   * the implementation, but can in some cases result in longer time to
   * run the tests.
   *
   * The default timeout for all expect* methods will be obtained by either the env variable {@code DEFAULT_TIMEOUT_MILLIS}
   * or the default value ({@link TestEnvironment#DEFAULT_TIMEOUT_MILLIS}) will be used.
   */
  public TestEnvironment() {
    this(envDefaultTimeoutMillis(), envDefaultNoSignalsTimeoutMillis());
  }

  /** This timeout is used when waiting for a signal to arrive. */
  public long defaultTimeoutMillis() {
    return defaultTimeoutMillis;
  }

  /**
   * This timeout is used when asserting that no further signals are emitted.
   * Note that this timeout default
   */
  public long defaultNoSignalsTimeoutMillis() {
    return defaultNoSignalsTimeoutMillis;
  }

  /**
   * The default amount of time to poll for events if {@code defaultTimeoutMillis} isn't preempted by an asynchronous
   * event.
   */
  public long defaultPollTimeoutMillis() {
    return defaultPollTimeoutMillis;
  }

  /**
   * Tries to parse the env variable {@code DEFAULT_TIMEOUT_MILLIS} as long and returns the value if present OR its default value.
   *
   * @throws java.lang.IllegalArgumentException when unable to parse the env variable
   */
  public static long envDefaultTimeoutMillis() {
    final String envMillis = System.getenv(DEFAULT_TIMEOUT_MILLIS_ENV);
    if (envMillis == null) return DEFAULT_TIMEOUT_MILLIS;
    else try {
      return Long.parseLong(envMillis);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(String.format("Unable to parse %s env value [%s] as long!", DEFAULT_TIMEOUT_MILLIS_ENV, envMillis), ex);
    }
  }

  /**
   * Tries to parse the env variable {@code DEFAULT_NO_SIGNALS_TIMEOUT_MILLIS} as long and returns the value if present OR its default value.
   *
   * @throws java.lang.IllegalArgumentException when unable to parse the env variable
   */
  public static long envDefaultNoSignalsTimeoutMillis() {
    final String envMillis = System.getenv(DEFAULT_NO_SIGNALS_TIMEOUT_MILLIS_ENV);
    if (envMillis == null) return envDefaultTimeoutMillis();
    else try {
      return Long.parseLong(envMillis);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(String.format("Unable to parse %s env value [%s] as long!", DEFAULT_NO_SIGNALS_TIMEOUT_MILLIS_ENV, envMillis), ex);
    }
  }

  /**
   * Tries to parse the env variable {@code DEFAULT_POLL_TIMEOUT_MILLIS_ENV} as long and returns the value if present OR its default value.
   *
   * @throws java.lang.IllegalArgumentException when unable to parse the env variable
   */
  public static long envDefaultPollTimeoutMillis() {
    final String envMillis = System.getenv(DEFAULT_POLL_TIMEOUT_MILLIS_ENV);
    if (envMillis == null) return envDefaultTimeoutMillis();
    else try {
      return Long.parseLong(envMillis);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(String.format("Unable to parse %s env value [%s] as long!", DEFAULT_POLL_TIMEOUT_MILLIS_ENV, envMillis), ex);
    }
  }

  /**
   * To flop means to "fail asynchronously", either by onErroring or by failing some TCK check triggered asynchronously.
   * This method does *NOT* fail the test - it's up to inspections of the error to fail the test if required.
   *
   * Use {@code env.verifyNoAsyncErrorsNoDelay()} at the end of your TCK tests to verify there no flops called during it's execution.
   * To check investigate asyncErrors more closely you can use {@code expectError} methods or collect the error directly
   * from the environment using {@code env.dropAsyncError()}.
   *
   * To clear asyncErrors you can call {@link org.reactivestreams.tck.TestEnvironment#clearAsyncErrors()}
   */
  public void flop(String msg) {
    try {
      fail(msg);
    } catch (Throwable t) {
      asyncErrors.add(t);
    }
  }

  /**
   * To flop means to "fail asynchronously", either by onErroring or by failing some TCK check triggered asynchronously.
   * This method does *NOT* fail the test - it's up to inspections of the error to fail the test if required.
   *
   * This overload keeps the passed in throwable as the asyncError, instead of creating an AssertionError for this.
   *
   * Use {@code env.verifyNoAsyncErrorsNoDelay()} at the end of your TCK tests to verify there no flops called during it's execution.
   * To check investigate asyncErrors more closely you can use {@code expectError} methods or collect the error directly
   * from the environment using {@code env.dropAsyncError()}.
   *
   * To clear asyncErrors you can call {@link org.reactivestreams.tck.TestEnvironment#clearAsyncErrors()}
   */
  public void flop(Throwable thr, String msg) {
    try {
      fail(msg, thr);
    } catch (Throwable t) {
      asyncErrors.add(thr);
    }
  }

  /**
   * To flop means to "fail asynchronously", either by onErroring or by failing some TCK check triggered asynchronously.
   * This method does *NOT* fail the test - it's up to inspections of the error to fail the test if required.
   *
   * This overload keeps the passed in throwable as the asyncError, instead of creating an AssertionError for this.
   *
   * Use {@code env.verifyNoAsyncErrorsNoDelay()} at the end of your TCK tests to verify there no flops called during it's execution.
   * To check investigate asyncErrors more closely you can use {@code expectError} methods or collect the error directly
   * from the environment using {@code env.dropAsyncError()}.
   *
   * To clear asyncErrors you can call {@link org.reactivestreams.tck.TestEnvironment#clearAsyncErrors()}
   */
  public void flop(Throwable thr) {
    try {
      fail(thr.getMessage(), thr);
    } catch (Throwable t) {
      asyncErrors.add(thr);
    }
  }

  /**
   * To flop means to "fail asynchronously", either by onErroring or by failing some TCK check triggered asynchronously.
   *
   * This method DOES fail the test right away (it tries to, by throwing an AssertionException),
   * in such it is different from {@link org.reactivestreams.tck.TestEnvironment#flop} which only records the error.
   *
   * Use {@code env.verifyNoAsyncErrorsNoDelay()} at the end of your TCK tests to verify there no flops called during it's execution.
   * To check investigate asyncErrors more closely you can use {@code expectError} methods or collect the error directly
   * from the environment using {@code env.dropAsyncError()}.
   *
   * To clear asyncErrors you can call {@link org.reactivestreams.tck.TestEnvironment#clearAsyncErrors()}
   */
  public <T> T flopAndFail(String msg) {
    try {
      fail(msg);
    } catch (Throwable t) {
      asyncErrors.add(t);
      fail(msg, t);
    }
    return null; // unreachable, the previous block will always exit by throwing
  }



  public <T> void subscribe(Publisher<T> pub, TestSubscriber<T> sub) throws InterruptedException {
    subscribe(pub, sub, defaultTimeoutMillis);
  }

  public <T> void subscribe(Publisher<T> pub, TestSubscriber<T> sub, long timeoutMillis) throws InterruptedException {
    pub.subscribe(sub);
    sub.subscription.expectCompletion(timeoutMillis, String.format("Could not subscribe %s to Publisher %s", sub, pub));
    verifyNoAsyncErrorsNoDelay();
  }

  public <T> ManualSubscriber<T> newBlackholeSubscriber(Publisher<T> pub) throws InterruptedException {
    ManualSubscriberWithSubscriptionSupport<T> sub = new BlackholeSubscriberWithSubscriptionSupport<T>(this);
    subscribe(pub, sub, defaultTimeoutMillis());
    return sub;
  }

  public <T> ManualSubscriber<T> newManualSubscriber(Publisher<T> pub) throws InterruptedException {
    return newManualSubscriber(pub, defaultTimeoutMillis());
  }

  public <T> ManualSubscriber<T> newManualSubscriber(Publisher<T> pub, long timeoutMillis) throws InterruptedException {
    ManualSubscriberWithSubscriptionSupport<T> sub = new ManualSubscriberWithSubscriptionSupport<T>(this);
    subscribe(pub, sub, timeoutMillis);
    return sub;
  }

  public void clearAsyncErrors() {
    asyncErrors.clear();
  }

  public Throwable dropAsyncError() {
    try {
      return asyncErrors.remove(0);
    } catch (IndexOutOfBoundsException ex) {
      return null;
    }
  }

  /**
   * Waits for {@link TestEnvironment#defaultNoSignalsTimeoutMillis()} and then verifies that no asynchronous errors
   * were signalled pior to, or during that time (by calling {@code flop()}).
   */
  public void verifyNoAsyncErrors() {
    verifyNoAsyncErrors(defaultNoSignalsTimeoutMillis());
  }

  /**
   * This version of {@code verifyNoAsyncErrors} should be used when errors still could be signalled
   * asynchronously during {@link TestEnvironment#defaultTimeoutMillis()} time.
   * <p></p>
   * It will immediatly check if any async errors were signaled (using {@link TestEnvironment#flop(String)},
   * and if no errors encountered wait for another default timeout as the errors may yet be signalled.
   * The initial check is performed in order to fail-fast in case of an already failed test.
   */
  public void verifyNoAsyncErrors(long delay) {
    try {
      verifyNoAsyncErrorsNoDelay();

      Thread.sleep(delay);
      verifyNoAsyncErrorsNoDelay();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Verifies that no asynchronous errors were signalled pior to calling this method (by calling {@code flop()}).
   * This version of verifyNoAsyncError <b>does not wait before checking for asynchronous errors</b>, and is to be used
   * for example in tight loops etc.
   */
  public void verifyNoAsyncErrorsNoDelay() {
    for (Throwable e : asyncErrors) {
      if (e instanceof AssertionError) {
        throw (AssertionError) e;
      } else {
        fail(String.format("Async error during test execution: %s", e.getMessage()), e);
      }
    }
  }

  /** If {@code TestEnvironment#printlnDebug} is true, print debug message to std out. */
  public void debug(String msg) {
    if (debugEnabled()) {
      System.out.printf("[TCK-DEBUG] %s%n", msg);
    }
  }

  public final boolean debugEnabled() {
    return printlnDebug;
  }

  /**
   * Looks for given {@code method} method in stack trace.
   * Can be used to answer questions like "was this method called from onComplete?".
   *
   * @return the caller's StackTraceElement at which he the looked for method was found in the call stack, EMPTY otherwise
   */
  public Optional<StackTraceElement> findCallerMethodInStackTrace(String method) {
    final Throwable thr = new Throwable(); // gets the stacktrace

    for (StackTraceElement stackElement : thr.getStackTrace()) {
      if (stackElement.getMethodName().equals(method)) {
        return Optional.of(stackElement);
      }
    }
    return Optional.empty();
  }

  // ---- classes ----

  /**
   * {@link Subscriber} implementation which can be steered by test code and asserted on.
   */
  public static class ManualSubscriber<T> extends TestSubscriber<T> {
    Receptacle<T> received;

    public ManualSubscriber(TestEnvironment env) {
      super(env);
      received = new Receptacle<T>(this.env);
    }

    @Override
    public void onNext(T element) {
      try {
        received.add(element);
      } catch (IllegalStateException ex) {
          // error message refinement
          throw new SubscriberBufferOverflowException(
            String.format("Received more than bufferSize (%d) onNext signals. " +
                            "The Publisher probably emited more signals than expected!",
                          received.QUEUE_SIZE), ex);
      }
    }

    @Override
    public void onComplete() {
      received.complete();
    }

    public void request(long elements) {
      subscription.value().request(elements);
    }

    public T requestNextElement() throws InterruptedException {
      return requestNextElement(env.defaultTimeoutMillis());
    }

    public T requestNextElement(long timeoutMillis) throws InterruptedException {
      return requestNextElement(timeoutMillis, "Did not receive expected element");
    }

    public T requestNextElement(String errorMsg) throws InterruptedException {
      return requestNextElement(env.defaultTimeoutMillis(), errorMsg);
    }

    public T requestNextElement(long timeoutMillis, String errorMsg) throws InterruptedException {
      request(1);
      return nextElement(timeoutMillis, errorMsg);
    }

    public Optional<T> requestNextElementOrEndOfStream() throws InterruptedException {
      return requestNextElementOrEndOfStream(env.defaultTimeoutMillis(), "Did not receive expected stream completion");
    }

    public Optional<T> requestNextElementOrEndOfStream(String errorMsg) throws InterruptedException {
      return requestNextElementOrEndOfStream(env.defaultTimeoutMillis(), errorMsg);
    }

    public Optional<T> requestNextElementOrEndOfStream(long timeoutMillis) throws InterruptedException {
      return requestNextElementOrEndOfStream(timeoutMillis, "Did not receive expected stream completion");
    }

    public Optional<T> requestNextElementOrEndOfStream(long timeoutMillis, String errorMsg) throws InterruptedException {
      request(1);
      return nextElementOrEndOfStream(timeoutMillis, errorMsg);
    }

    public void requestEndOfStream() throws InterruptedException {
      requestEndOfStream(env.defaultTimeoutMillis(), "Did not receive expected stream completion");
    }

    public void requestEndOfStream(long timeoutMillis) throws InterruptedException {
      requestEndOfStream(timeoutMillis, "Did not receive expected stream completion");
    }

    public void requestEndOfStream(String errorMsg) throws InterruptedException {
      requestEndOfStream(env.defaultTimeoutMillis(), errorMsg);
    }

    public void requestEndOfStream(long timeoutMillis, String errorMsg) throws InterruptedException {
      request(1);
      expectCompletion(timeoutMillis, errorMsg);
    }

    public List<T> requestNextElements(long elements) throws InterruptedException {
      request(elements);
      return nextElements(elements, env.defaultTimeoutMillis());
    }

    public List<T> requestNextElements(long elements, long timeoutMillis) throws InterruptedException {
      request(elements);
      return nextElements(elements, timeoutMillis, String.format("Did not receive %d expected elements", elements));
    }

    public List<T> requestNextElements(long elements, long timeoutMillis, String errorMsg) throws InterruptedException {
      request(elements);
      return nextElements(elements, timeoutMillis, errorMsg);
    }

    public T nextElement() throws InterruptedException {
      return nextElement(env.defaultTimeoutMillis());
    }

    public T nextElement(long timeoutMillis) throws InterruptedException {
      return nextElement(timeoutMillis, "Did not receive expected element");
    }

    public T nextElement(String errorMsg) throws InterruptedException {
      return nextElement(env.defaultTimeoutMillis(), errorMsg);
    }

    public T nextElement(long timeoutMillis, String errorMsg) throws InterruptedException {
      return received.next(timeoutMillis, errorMsg);
    }

    public Optional<T> nextElementOrEndOfStream() throws InterruptedException {
      return nextElementOrEndOfStream(env.defaultTimeoutMillis(), "Did not receive expected stream completion");
    }

    public Optional<T> nextElementOrEndOfStream(long timeoutMillis) throws InterruptedException {
      return nextElementOrEndOfStream(timeoutMillis, "Did not receive expected stream completion");
    }

    public Optional<T> nextElementOrEndOfStream(long timeoutMillis, String errorMsg) throws InterruptedException {
      return received.nextOrEndOfStream(timeoutMillis, errorMsg);
    }

    public List<T> nextElements(long elements) throws InterruptedException {
      return nextElements(elements, env.defaultTimeoutMillis(), "Did not receive expected element or completion");
    }

    public List<T> nextElements(long elements, String errorMsg) throws InterruptedException {
      return nextElements(elements, env.defaultTimeoutMillis(), errorMsg);
    }

    public List<T> nextElements(long elements, long timeoutMillis) throws InterruptedException {
      return nextElements(elements, timeoutMillis, "Did not receive expected element or completion");
    }

    public List<T> nextElements(long elements, long timeoutMillis, String errorMsg) throws InterruptedException {
      return received.nextN(elements, timeoutMillis, errorMsg);
    }

    public void expectNext(T expected) throws InterruptedException {
      expectNext(expected, env.defaultTimeoutMillis());
    }

    public void expectNext(T expected, long timeoutMillis) throws InterruptedException {
      T received = nextElement(timeoutMillis, "Did not receive expected element on downstream");
      if (!received.equals(expected)) {
        env.flop(String.format("Expected element %s on downstream but received %s", expected, received));
      }
    }

    public void expectCompletion() throws InterruptedException {
      expectCompletion(env.defaultTimeoutMillis(), "Did not receive expected stream completion");
    }

    public void expectCompletion(long timeoutMillis) throws InterruptedException {
      expectCompletion(timeoutMillis, "Did not receive expected stream completion");
    }

    public void expectCompletion(String errorMsg) throws InterruptedException {
      expectCompletion(env.defaultTimeoutMillis(), errorMsg);
    }

    public void expectCompletion(long timeoutMillis, String errorMsg) throws InterruptedException {
      received.expectCompletion(timeoutMillis, errorMsg);
    }

    public <E extends Throwable> void expectErrorWithMessage(Class<E> expected, String requiredMessagePart) throws Exception {
      expectErrorWithMessage(expected, Collections.singletonList(requiredMessagePart), env.defaultTimeoutMillis(), env.defaultPollTimeoutMillis());
    }
    public <E extends Throwable> void expectErrorWithMessage(Class<E> expected, List<String> requiredMessagePartAlternatives) throws Exception {
      expectErrorWithMessage(expected, requiredMessagePartAlternatives, env.defaultTimeoutMillis(), env.defaultPollTimeoutMillis());
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public <E extends Throwable> void expectErrorWithMessage(Class<E> expected, String requiredMessagePart, long timeoutMillis) throws Exception {
      expectErrorWithMessage(expected, Collections.singletonList(requiredMessagePart), timeoutMillis);
    }

    public <E extends Throwable> void expectErrorWithMessage(Class<E> expected, List<String> requiredMessagePartAlternatives, long timeoutMillis) throws Exception {
      expectErrorWithMessage(expected, requiredMessagePartAlternatives, timeoutMillis, timeoutMillis);
    }

    public <E extends Throwable> void expectErrorWithMessage(Class<E> expected, List<String> requiredMessagePartAlternatives,
                                                             long totalTimeoutMillis, long pollTimeoutMillis) throws Exception {
      final E err = expectError(expected, totalTimeoutMillis, pollTimeoutMillis);
      final String message = err.getMessage();

      boolean contains = false;
      for (String requiredMessagePart : requiredMessagePartAlternatives)
        if (message.contains(requiredMessagePart)) contains = true; // not short-circuting loop, it is expected to
      assertTrue(contains,
              String.format("Got expected exception [%s] but missing message part [%s], was: %s",
                      err.getClass(), "anyOf: " + requiredMessagePartAlternatives, err.getMessage()));
    }

    public <E extends Throwable> E expectError(Class<E> expected) throws Exception {
      return expectError(expected, env.defaultTimeoutMillis());
    }

    public <E extends Throwable> E expectError(Class<E> expected, long timeoutMillis) throws Exception {
      return expectError(expected, timeoutMillis, env.defaultPollTimeoutMillis());
    }

    public <E extends Throwable> E expectError(Class<E> expected, String errorMsg) throws Exception {
      return expectError(expected, env.defaultTimeoutMillis(), errorMsg);
    }

    public <E extends Throwable> E expectError(Class<E> expected, long timeoutMillis, String errorMsg) throws Exception {
      return expectError(expected, timeoutMillis, env.defaultPollTimeoutMillis(), errorMsg);
    }

    public <E extends Throwable> E expectError(Class<E> expected, long totalTimeoutMillis, long pollTimeoutMillis) throws Exception {
      return expectError(expected, totalTimeoutMillis, pollTimeoutMillis, String.format("Expected onError(%s)", expected.getName()));
    }

    public <E extends Throwable> E expectError(Class<E> expected, long totalTimeoutMillis, long pollTimeoutMillis,
                                               String errorMsg) throws Exception {
      return received.expectError(expected, totalTimeoutMillis, pollTimeoutMillis, errorMsg);
    }

    public void expectNone() throws InterruptedException {
      expectNone(env.defaultNoSignalsTimeoutMillis());
    }

    public void expectNone(String errMsgPrefix) throws InterruptedException {
      expectNone(env.defaultNoSignalsTimeoutMillis(), errMsgPrefix);
    }

    public void expectNone(long withinMillis) throws InterruptedException {
      expectNone(withinMillis, "Did not expect an element but got element");
    }

    public void expectNone(long withinMillis, String errMsgPrefix) throws InterruptedException {
      received.expectNone(withinMillis, errMsgPrefix);
    }

  }

  public static class ManualSubscriberWithSubscriptionSupport<T> extends ManualSubscriber<T> {

    public ManualSubscriberWithSubscriptionSupport(TestEnvironment env) {
      super(env);
    }

    @Override
    public void onNext(T element) {
      if (env.debugEnabled()) {
        env.debug(String.format("%s::onNext(%s)", this, element));
      }
      if (subscription.isCompleted()) {
        super.onNext(element);
      } else {
        env.flop(String.format("Subscriber::onNext(%s) called before Subscriber::onSubscribe", element));
      }
    }

    @Override
    public void onComplete() {
      if (env.debugEnabled()) {
        env.debug(this + "::onComplete()");
      }
      if (subscription.isCompleted()) {
        super.onComplete();
      } else {
        env.flop("Subscriber::onComplete() called before Subscriber::onSubscribe");
      }
    }

    @Override
    public void onSubscribe(Subscription s) {
      if (env.debugEnabled()) {
        env.debug(String.format("%s::onSubscribe(%s)", this, s));
      }
      if (!subscription.isCompleted()) {
        subscription.complete(s);
      } else {
        env.flop("Subscriber::onSubscribe called on an already-subscribed Subscriber");
      }
    }

    @Override
    public void onError(Throwable cause) {
      if (env.debugEnabled()) {
        env.debug(String.format("%s::onError(%s)", this, cause));
      }
      if (subscription.isCompleted()) {
        super.onError(cause);
      } else {
        env.flop(cause, String.format("Subscriber::onError(%s) called before Subscriber::onSubscribe", cause));
      }
    }
  }

  /**
   * Similar to {@link org.reactivestreams.tck.TestEnvironment.ManualSubscriberWithSubscriptionSupport}
   * but does not accumulate values signalled via <code>onNext</code>, thus it can not be used to assert
   * values signalled to this subscriber. Instead it may be used to quickly drain a given publisher.
   */
  public static class BlackholeSubscriberWithSubscriptionSupport<T>
    extends ManualSubscriberWithSubscriptionSupport<T> {

    public BlackholeSubscriberWithSubscriptionSupport(TestEnvironment env) {
      super(env);
    }

    @Override
    public void onNext(T element) {
      if (env.debugEnabled()) {
        env.debug(String.format("%s::onNext(%s)", this, element));
      }
      if (!subscription.isCompleted()) {
        env.flop(String.format("Subscriber::onNext(%s) called before Subscriber::onSubscribe", element));
      }
    }

    @Override
    public T nextElement(long timeoutMillis, String errorMsg) throws InterruptedException {
      throw new RuntimeException("Can not expect elements from BlackholeSubscriber, use ManualSubscriber instead!");
    }

    @Override
    public List<T> nextElements(long elements, long timeoutMillis, String errorMsg) throws InterruptedException {
      throw new RuntimeException("Can not expect elements from BlackholeSubscriber, use ManualSubscriber instead!");
    }
  }

  public static class TestSubscriber<T> implements Subscriber<T> {
    final Promise<Subscription> subscription;

    protected final TestEnvironment env;

    public TestSubscriber(TestEnvironment env) {
      this.env = env;
      subscription = new Promise<Subscription>(env);
    }

    @Override
    public void onError(Throwable cause) {
      env.flop(cause, String.format("Unexpected Subscriber::onError(%s)", cause));
    }

    @Override
    public void onComplete() {
      env.flop("Unexpected Subscriber::onComplete()");
    }

    @Override
    public void onNext(T element) {
      env.flop(String.format("Unexpected Subscriber::onNext(%s)", element));
    }

    @Override
    public void onSubscribe(Subscription subscription) {
      env.flop(String.format("Unexpected Subscriber::onSubscribe(%s)", subscription));
    }

    public void cancel() {
      if (subscription.isCompleted()) {
        subscription.value().cancel();
      } else {
        env.flop("Cannot cancel a subscription before having received it");
      }
    }
  }

  public static class ManualPublisher<T> implements Publisher<T> {
    protected final TestEnvironment env;

    protected long pendingDemand = 0L;
    protected Promise<Subscriber<? super T>> subscriber;

    protected final Receptacle<Long> requests;

    protected final Latch cancelled;

    public ManualPublisher(TestEnvironment env) {
      this.env = env;
      requests = new Receptacle<Long>(env);
      cancelled = new Latch(env);
      subscriber = new Promise<Subscriber<? super T>>(this.env);
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
      if (!subscriber.isCompleted()) {
        subscriber.completeImmediatly(s);

        Subscription subs = new Subscription() {
          @Override
          public void request(long elements) {
            requests.add(elements);
          }

          @Override
          public void cancel() {
            cancelled.close();
          }
        };
        s.onSubscribe(subs);

      } else {
        env.flop("TestPublisher doesn't support more than one Subscriber");
      }
    }

    public void sendNext(T element) {
      if (subscriber.isCompleted()) {
        subscriber.value().onNext(element);
      } else {
        env.flop("Cannot sendNext before having a Subscriber");
      }
    }

    public void sendCompletion() {
      if (subscriber.isCompleted()) {
        subscriber.value().onComplete();
      } else {
        env.flop("Cannot sendCompletion before having a Subscriber");
      }
    }

    public void sendError(Throwable cause) {
      if (subscriber.isCompleted()) {
        subscriber.value().onError(cause);
      } else {
        env.flop("Cannot sendError before having a Subscriber");
      }
    }

    public long expectRequest() throws InterruptedException {
      return expectRequest(env.defaultTimeoutMillis());
    }

    public long expectRequest(long timeoutMillis) throws InterruptedException {
      long requested = requests.next(timeoutMillis, "Did not receive expected `request` call");
      if (requested <= 0) {
        return env.<Long>flopAndFail(String.format("Requests cannot be zero or negative but received request(%s)", requested));
      } else {
        pendingDemand += requested;
        return requested;
      }
    }


    public long expectRequest(long timeoutMillis, String errorMessageAddendum) throws InterruptedException {
      long requested = requests.next(timeoutMillis, String.format("Did not receive expected `request` call. %s", errorMessageAddendum));
      if (requested <= 0) {
        return env.<Long>flopAndFail(String.format("Requests cannot be zero or negative but received request(%s)", requested));
      } else {
        pendingDemand += requested;
        return requested;
      }
    }

    public void expectExactRequest(long expected) throws InterruptedException {
      expectExactRequest(expected, env.defaultTimeoutMillis());
    }

    public void expectExactRequest(long expected, long timeoutMillis) throws InterruptedException {
      long requested = expectRequest(timeoutMillis);
      if (requested != expected) {
        env.flop(String.format("Received `request(%d)` on upstream but expected `request(%d)`", requested, expected));
      }
      pendingDemand += requested;
    }

    public void expectNoRequest() throws InterruptedException {
      expectNoRequest(env.defaultTimeoutMillis());
    }

    public void expectNoRequest(long timeoutMillis) throws InterruptedException {
      requests.expectNone(timeoutMillis, "Received an unexpected call to: request: ");
    }

    public void expectCancelling() throws InterruptedException {
      expectCancelling(env.defaultTimeoutMillis());
    }

    public void expectCancelling(long timeoutMillis) throws InterruptedException {
      cancelled.expectClose(timeoutMillis, "Did not receive expected cancelling of upstream subscription");
    }

    public boolean isCancelled() throws InterruptedException {
      return cancelled.isClosed();
    }
  }

  /**
   * Like a CountDownLatch, but resettable and with some convenience methods
   */
  public static class Latch {
    private final TestEnvironment env;
    volatile private CountDownLatch countDownLatch = new CountDownLatch(1);

    public Latch(TestEnvironment env) {
      this.env = env;
    }

    public void reOpen() {
      countDownLatch = new CountDownLatch(1);
    }

    public boolean isClosed() {
      return countDownLatch.getCount() == 0;
    }

    public void close() {
      countDownLatch.countDown();
    }

    public void assertClosed(String openErrorMsg) {
      if (!isClosed()) {
        env.flop(new ExpectedClosedLatchException(openErrorMsg));
      }
    }

    public void assertOpen(String closedErrorMsg) {
      if (isClosed()) {
        env.flop(new ExpectedOpenLatchException(closedErrorMsg));
      }
    }

    public void expectClose(String notClosedErrorMsg) throws InterruptedException {
      expectClose(env.defaultTimeoutMillis(), notClosedErrorMsg);
    }

    public void expectClose(long timeoutMillis, String notClosedErrorMsg) throws InterruptedException {
      countDownLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
      if (countDownLatch.getCount() > 0) {
        env.flop(String.format("%s within %d ms", notClosedErrorMsg, timeoutMillis));
      }
    }

    static final class ExpectedOpenLatchException extends RuntimeException {
      public ExpectedOpenLatchException(String message) {
        super(message);
      }
    }

    static final class ExpectedClosedLatchException extends RuntimeException {
      public ExpectedClosedLatchException(String message) {
        super(message);
      }
    }

  }

  // simple promise for *one* value, which cannot be reset
  public static class Promise<T> {
    private final TestEnvironment env;

    public static <T> Promise<T> completed(TestEnvironment env, T value) {
      Promise<T> promise = new Promise<T>(env);
      promise.completeImmediatly(value);
      return promise;
    }

    public Promise(TestEnvironment env) {
      this.env = env;
    }

    private ArrayBlockingQueue<T> abq = new ArrayBlockingQueue<T>(1);
    private AtomicReference<T> _value = new AtomicReference<T>();

    public T value() {
      final T value = _value.get();
      if (value != null) {
        return value;
      } else {
        env.flop("Cannot access promise value before completion");
        return null;
      }
    }

    public boolean isCompleted() {
      return _value.get() != null;
    }

    /**
     * Allows using expectCompletion to await for completion of the value and complete it _then_
     */
    public void complete(T value) {
      if (_value.compareAndSet(null, value)) {
        // we add the value to the queue such to wake up any expectCompletion which was triggered before complete() was called
        abq.add(value);
      } else {
        env.flop(String.format("Cannot complete a promise more than once! Present value: %s, attempted to set: %s", _value.get(), value));
      }
    }

    /**
     * Same as complete.
     *
     * Keeping this method for binary compatibility.
     */
    public void completeImmediatly(T value) {
      complete(value);
    }

    public void expectCompletion(long timeoutMillis, String errorMsg) throws InterruptedException {
      if (!isCompleted()) {
        T val = abq.poll(timeoutMillis, TimeUnit.MILLISECONDS);

        if (val == null) {
          env.flop(String.format("%s within %d ms", errorMsg, timeoutMillis));
        }
      }
    }
  }

  // a "Promise" for multiple values, which also supports "end-of-stream reached"
  public static class Receptacle<T> {
    final int QUEUE_SIZE = 2 * TEST_BUFFER_SIZE;
    private final TestEnvironment env;

    private final ArrayBlockingQueue<Optional<T>> abq = new ArrayBlockingQueue<Optional<T>>(QUEUE_SIZE);

    private final Latch completedLatch;

    Receptacle(TestEnvironment env) {
      this.env = env;
      this.completedLatch = new Latch(env);
    }

    public void add(T value) {
      completedLatch.assertOpen(String.format("Unexpected element %s received after stream completed", value));

      abq.add(Optional.of(value));
    }

    public void complete() {
      completedLatch.assertOpen("Unexpected additional complete signal received!");
      completedLatch.close();

      abq.add(Optional.<T>empty());
    }

    public T next(long timeoutMillis, String errorMsg) throws InterruptedException {
      Optional<T> value = abq.poll(timeoutMillis, TimeUnit.MILLISECONDS);

      if (value == null) {
        return env.flopAndFail(String.format("%s within %d ms", errorMsg, timeoutMillis));
      } else if (value.isDefined()) {
        return value.get();
      } else {
        return env.flopAndFail("Expected element but got end-of-stream");
      }
    }

    public Optional<T> nextOrEndOfStream(long timeoutMillis, String errorMsg) throws InterruptedException {
      Optional<T> value = abq.poll(timeoutMillis, TimeUnit.MILLISECONDS);

      if (value == null) {
        env.flop(String.format("%s within %d ms", errorMsg, timeoutMillis));
        return Optional.empty();
      }

      return value;
    }

    /**
     * @param timeoutMillis total timeout time for awaiting all {@code elements} number of elements
     */
    public List<T> nextN(long elements, long timeoutMillis, String errorMsg) throws InterruptedException {
      List<T> result = new LinkedList<T>();
      long remaining = elements;
      long deadline = System.currentTimeMillis() + timeoutMillis;
      while (remaining > 0) {
        long remainingMillis = deadline - System.currentTimeMillis();

        result.add(next(remainingMillis, errorMsg));
        remaining--;
      }

      return result;
    }


    public void expectCompletion(long timeoutMillis, String errorMsg) throws InterruptedException {
      Optional<T> value = abq.poll(timeoutMillis, TimeUnit.MILLISECONDS);

      if (value == null) {
        env.flop(String.format("%s within %d ms", errorMsg, timeoutMillis));
      } else if (value.isDefined()) {
        env.flop(String.format("Expected end-of-stream but got element [%s]", value.get()));
      } // else, ok
    }

    /**
     * @deprecated Deprecated in favor of {@link #expectError(Class, long, long, String)}.
     */
    @Deprecated
    public <E extends Throwable> E expectError(Class<E> clazz, long timeoutMillis, String errorMsg) throws Exception {
      return expectError(clazz, timeoutMillis, timeoutMillis, errorMsg);
    }

    @SuppressWarnings("unchecked")
    final <E extends Throwable> E expectError(Class<E> clazz, final long totalTimeoutMillis,
                                              long pollTimeoutMillis,
                                              String errorMsg) throws Exception {
      long totalTimeoutRemainingNs = MILLISECONDS.toNanos(totalTimeoutMillis);
      long timeStampANs = System.nanoTime();
      long timeStampBNs;

      for (;;) {
        Thread.sleep(Math.min(pollTimeoutMillis, NANOSECONDS.toMillis(totalTimeoutRemainingNs)));

        if (env.asyncErrors.isEmpty()) {
          timeStampBNs = System.nanoTime();
          totalTimeoutRemainingNs =- timeStampBNs - timeStampANs;
          timeStampANs = timeStampBNs;

          if (totalTimeoutRemainingNs <= 0) {
            return env.flopAndFail(String.format("%s within %d ms", errorMsg, totalTimeoutMillis));
          }
        } else {
          // ok, there was an expected error
          Throwable thrown = env.asyncErrors.remove(0);

          if (clazz.isInstance(thrown)) {
            return (E) thrown;
          } else {

            return env.flopAndFail(String.format("%s within %d ms; Got %s but expected %s",
                    errorMsg, totalTimeoutMillis, thrown.getClass().getCanonicalName(), clazz.getCanonicalName()));
          }
        }
      }
    }

    public void expectNone(long withinMillis, String errorMsgPrefix) throws InterruptedException {
      Thread.sleep(withinMillis);
      Optional<T> value = abq.poll();

      if (value == null) {
        // ok
      } else if (value.isDefined()) {
        env.flop(String.format("%s [%s]", errorMsgPrefix, value.get()));
      } else {
        env.flop("Expected no element but got end-of-stream");
      }
    }
  }
}
