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
import org.reactivestreams.tck.TestEnvironment.BlackholeSubscriberWithSubscriptionSupport;
import org.reactivestreams.tck.TestEnvironment.Latch;
import org.reactivestreams.tck.TestEnvironment.ManualSubscriber;
import org.reactivestreams.tck.TestEnvironment.ManualSubscriberWithSubscriptionSupport;
import org.reactivestreams.tck.flow.support.Function;
import org.reactivestreams.tck.flow.support.Optional;
import org.reactivestreams.tck.flow.support.PublisherVerificationRules;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.Override;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Provides tests for verifying {@code Publisher} specification rules.
 *
 * @see org.reactivestreams.Publisher
 */
public abstract class PublisherVerification<T> implements PublisherVerificationRules {

  private static final String PUBLISHER_REFERENCE_GC_TIMEOUT_MILLIS_ENV = "PUBLISHER_REFERENCE_GC_TIMEOUT_MILLIS";
  private static final long DEFAULT_PUBLISHER_REFERENCE_GC_TIMEOUT_MILLIS = 300L;

  private final TestEnvironment env;

  /**
   * The amount of time after which a cancelled Subscriber reference should be dropped.
   * See Rule 3.13 for details.
   */
  private final long publisherReferenceGCTimeoutMillis;

  /**
   * Constructs a new verification class using the given env and configuration.
   *
   * @param publisherReferenceGCTimeoutMillis used to determine after how much time a reference to a Subscriber should be already dropped by the Publisher.
   */
  public PublisherVerification(TestEnvironment env, long publisherReferenceGCTimeoutMillis) {
    this.env = env;
    this.publisherReferenceGCTimeoutMillis = publisherReferenceGCTimeoutMillis;
  }

  /**
   * Constructs a new verification class using the given env and configuration.
   *
   * The value for {@code publisherReferenceGCTimeoutMillis} will be obtained by using {@link PublisherVerification#envPublisherReferenceGCTimeoutMillis()}.
   */
  public PublisherVerification(TestEnvironment env) {
    this.env = env;
    this.publisherReferenceGCTimeoutMillis = envPublisherReferenceGCTimeoutMillis();
  }

  /**
   * Tries to parse the env variable {@code PUBLISHER_REFERENCE_GC_TIMEOUT_MILLIS} as long and returns the value if present,
   * OR its default value ({@link PublisherVerification#DEFAULT_PUBLISHER_REFERENCE_GC_TIMEOUT_MILLIS}).
   *
   * This value is used to determine after how much time a reference to a Subscriber should be already dropped by the Publisher.
   *
   * @throws java.lang.IllegalArgumentException when unable to parse the env variable
   */
  public static long envPublisherReferenceGCTimeoutMillis() {
    final String envMillis = System.getenv(PUBLISHER_REFERENCE_GC_TIMEOUT_MILLIS_ENV);
    if (envMillis == null) return DEFAULT_PUBLISHER_REFERENCE_GC_TIMEOUT_MILLIS;
    else try {
      return Long.parseLong(envMillis);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(String.format("Unable to parse %s env value [%s] as long!", PUBLISHER_REFERENCE_GC_TIMEOUT_MILLIS_ENV, envMillis), ex);
    }
  }

  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a Publisher for a stream with exactly the given number of elements.
   * If `elements` is `Long.MAX_VALUE` the produced stream must be infinite.
   */
  public abstract Publisher<T> createPublisher(long elements);

  /**
   * By implementing this method, additional TCK tests concerning a "failed" publishers will be run.
   *
   * The expected behaviour of the {@link Publisher} returned by this method is hand out a subscription,
   * followed by signalling {@code onError} on it, as specified by Rule 1.9.
   *
   * If you ignore these additional tests, return {@code null} from this method.
   */
  public abstract Publisher<T> createFailedPublisher();


  /**
   * Override and return lower value if your Publisher is only able to produce a known number of elements.
   * For example, if it is designed to return at-most-one element, return {@code 1} from this method.
   *
   * Defaults to {@code Long.MAX_VALUE - 1}, meaning that the Publisher can be produce a huge but NOT an unbounded number of elements.
   *
   * To mark your Publisher will *never* signal an {@code onComplete} override this method and return {@code Long.MAX_VALUE},
   * which will result in *skipping all tests which require an onComplete to be triggered* (!).
   */
  public long maxElementsFromPublisher() {
    return Long.MAX_VALUE - 1;
  }

  /**
   * Override and return {@code true} in order to skip executing tests marked as {@code Stochastic}.
   * Stochastic in this case means that the Rule is impossible or infeasible to deterministically verify—
   * usually this means that this test case can yield false positives ("be green") even if for some case,
   * the given implementation may violate the tested behaviour.
   */
  public boolean skipStochasticTests() {
    return false;
  }

  /**
   * In order to verify rule 3.3 of the reactive streams spec, this number will be used to check if a
   * {@code Subscription} actually solves the "unbounded recursion" problem by not allowing the number of
   * recursive calls to exceed the number returned by this method.
   *
   * @see <a href="https://github.com/reactive-streams/reactive-streams-jvm#3.3">reactive streams spec, rule 3.3</a>
   * @see PublisherVerification#required_spec303_mustNotAllowUnboundedRecursion()
   */
  public long boundedDepthOfOnNextAndRequestRecursion() {
    return 1;
  }

  ////////////////////// TEST ENV CLEANUP /////////////////////////////////////

  @BeforeMethod
  public void setUp() throws Exception {
    env.clearAsyncErrors();
  }

  ////////////////////// TEST SETUP VERIFICATION //////////////////////////////

  @Override @Test
  public void required_createPublisher1MustProduceAStreamOfExactly1Element() throws Throwable {
    activePublisherTest(1, true, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws InterruptedException {
        ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        assertTrue(requestNextElementOrEndOfStream(pub, sub).isDefined(), String.format("Publisher %s produced no elements", pub));
        sub.requestEndOfStream();
      }

      Optional<T> requestNextElementOrEndOfStream(Publisher<T> pub, ManualSubscriber<T> sub) throws InterruptedException {
        return sub.requestNextElementOrEndOfStream(String.format("Timeout while waiting for next element from Publisher %s", pub));
      }

    });
  }

  @Override @Test
  public void required_createPublisher3MustProduceAStreamOfExactly3Elements() throws Throwable {
    activePublisherTest(3, true, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws InterruptedException {
        ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        assertTrue(requestNextElementOrEndOfStream(pub, sub).isDefined(), String.format("Publisher %s produced no elements", pub));
        assertTrue(requestNextElementOrEndOfStream(pub, sub).isDefined(), String.format("Publisher %s produced only 1 element", pub));
        assertTrue(requestNextElementOrEndOfStream(pub, sub).isDefined(), String.format("Publisher %s produced only 2 elements", pub));
        sub.requestEndOfStream();
      }

      Optional<T> requestNextElementOrEndOfStream(Publisher<T> pub, ManualSubscriber<T> sub) throws InterruptedException {
        return sub.requestNextElementOrEndOfStream(String.format("Timeout while waiting for next element from Publisher %s", pub));
      }

    });
  }

  @Override @Test
  public void required_validate_maxElementsFromPublisher() throws Exception {
    assertTrue(maxElementsFromPublisher() >= 0, "maxElementsFromPublisher MUST return a number >= 0");
  }

  @Override @Test
  public void required_validate_boundedDepthOfOnNextAndRequestRecursion() throws Exception {
    assertTrue(boundedDepthOfOnNextAndRequestRecursion() >= 1, "boundedDepthOfOnNextAndRequestRecursion must return a number >= 1");
  }


  ////////////////////// SPEC RULE VERIFICATION ///////////////////////////////

  @Override @Test
  public void required_spec101_subscriptionRequestMustResultInTheCorrectNumberOfProducedElements() throws Throwable {
    activePublisherTest(5, false, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws InterruptedException {

        ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        try {
            sub.expectNone(String.format("Publisher %s produced value before the first `request`: ", pub));
            sub.request(1);
            sub.nextElement(String.format("Publisher %s produced no element after first `request`", pub));
            sub.expectNone(String.format("Publisher %s produced unrequested: ", pub));

            sub.request(1);
            sub.request(2);
            sub.nextElements(3, env.defaultTimeoutMillis(), String.format("Publisher %s produced less than 3 elements after two respective `request` calls", pub));

            sub.expectNone(String.format("Publisher %sproduced unrequested ", pub));
        } finally {
            sub.cancel();
        }
      }
    });
  }

  @Override @Test
  public void required_spec102_maySignalLessThanRequestedAndTerminateSubscription() throws Throwable {
    final int elements = 3;
    final int requested = 10;

    activePublisherTest(elements, true, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        final ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        sub.request(requested);
        sub.nextElements(elements);
        sub.expectCompletion();
      }
    });
  }

  @Override @Test
  public void stochastic_spec103_mustSignalOnMethodsSequentially() throws Throwable {
    final int iterations = 100;
    final int elements = 10;

    stochasticTest(iterations, new Function<Integer, Void>() {
      @Override
      public Void apply(final Integer runNumber) throws Throwable {
        activePublisherTest(elements, true, new PublisherTestRun<T>() {
          @Override
          public void run(Publisher<T> pub) throws Throwable {
            final Latch completionLatch = new Latch(env);

            final AtomicInteger gotElements = new AtomicInteger(0);
            pub.subscribe(new Subscriber<T>() {
              private Subscription subs;

              private ConcurrentAccessBarrier concurrentAccessBarrier = new ConcurrentAccessBarrier();

              /**
               * Concept wise very similar to a {@link org.reactivestreams.tck.TestEnvironment.Latch}, serves to protect
               * a critical section from concurrent access, with the added benefit of Thread tracking and same-thread-access awareness.
               *
               * Since a <i>Synchronous</i> Publisher may choose to synchronously (using the same {@link Thread}) call
               * {@code onNext} directly from either {@code subscribe} or {@code request} a plain Latch is not enough
               * to verify concurrent access safety - one needs to track if the caller is not still using the calling thread
               * to enter subsequent critical sections ("nesting" them effectively).
               */
              final class ConcurrentAccessBarrier {
                private AtomicReference<Thread> currentlySignallingThread = new AtomicReference<Thread>(null);
                private volatile String previousSignal = null;

                public void enterSignal(String signalName) {
                  if((!currentlySignallingThread.compareAndSet(null, Thread.currentThread())) && !isSynchronousSignal()) {
                    env.flop(String.format(
                      "Illegal concurrent access detected (entering critical section)! " +
                        "%s emited %s signal, before %s finished its %s signal.",
                        Thread.currentThread(), signalName, currentlySignallingThread.get(), previousSignal));
                  }
                  this.previousSignal = signalName;
                }

                public void leaveSignal(String signalName) {
                  currentlySignallingThread.set(null);
                  this.previousSignal = signalName;
                }

                private boolean isSynchronousSignal() {
                  return (previousSignal != null) && Thread.currentThread().equals(currentlySignallingThread.get());
                }

              }

              @Override
              public void onSubscribe(Subscription s) {
                final String signal = "onSubscribe()";
                concurrentAccessBarrier.enterSignal(signal);

                subs = s;
                subs.request(1);

                concurrentAccessBarrier.leaveSignal(signal);
              }

              @Override
              public void onNext(T ignore) {
                final String signal = String.format("onNext(%s)", ignore);
                concurrentAccessBarrier.enterSignal(signal);

                if (gotElements.incrementAndGet() <= elements) // requesting one more than we know are in the stream (some Publishers need this)
                  subs.request(1);

                concurrentAccessBarrier.leaveSignal(signal);
              }

              @Override
              public void onError(Throwable t) {
                final String signal = String.format("onError(%s)", t.getMessage());
                concurrentAccessBarrier.enterSignal(signal);

                // ignore value

                concurrentAccessBarrier.leaveSignal(signal);
              }

              @Override
              public void onComplete() {
                final String signal = "onComplete()";
                concurrentAccessBarrier.enterSignal(signal);

                // entering for completeness

                concurrentAccessBarrier.leaveSignal(signal);
                completionLatch.close();
              }
            });

            completionLatch.expectClose(
              elements * env.defaultTimeoutMillis(),
              String.format("Failed in iteration %d of %d. Expected completion signal after signalling %d elements (signalled %d), yet did not receive it",
                            runNumber, iterations, elements, gotElements.get()));
          }
        });
        return null;
      }
    });
  }

  @Override @Test
  public void optional_spec104_mustSignalOnErrorWhenFails() throws Throwable {
    try {
      whenHasErrorPublisherTest(new PublisherTestRun<T>() {
        @Override
        public void run(final Publisher<T> pub) throws InterruptedException {
          final Latch onErrorlatch = new Latch(env);
          final Latch onSubscribeLatch = new Latch(env);
          pub.subscribe(new TestEnvironment.TestSubscriber<T>(env) {
            @Override
            public void onSubscribe(Subscription subs) {
              onSubscribeLatch.assertOpen("Only one onSubscribe call expected");
              onSubscribeLatch.close();
            }
            @Override
            public void onError(Throwable cause) {
              onSubscribeLatch.assertClosed("onSubscribe should be called prior to onError always");
              onErrorlatch.assertOpen(String.format("Error-state Publisher %s called `onError` twice on new Subscriber", pub));
              onErrorlatch.close();
            }
          });

          onSubscribeLatch.expectClose("Should have received onSubscribe");
          onErrorlatch.expectClose(String.format("Error-state Publisher %s did not call `onError` on new Subscriber", pub));

          env.verifyNoAsyncErrors();
          }
      });
    } catch (SkipException se) {
      throw se;
    } catch (Throwable ex) {
      // we also want to catch AssertionErrors and anything the publisher may have thrown inside subscribe
      // which was wrong of him - he should have signalled on error using onError
      throw new RuntimeException(String.format("Publisher threw exception (%s) instead of signalling error via onError!", ex.getMessage()), ex);
    }
  }

  @Override @Test
  public void required_spec105_mustSignalOnCompleteWhenFiniteStreamTerminates() throws Throwable {
    activePublisherTest(3, true, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        sub.requestNextElement();
        sub.requestNextElement();
        sub.requestNextElement();
        sub.requestEndOfStream();
        sub.expectNone();
      }
    });
  }

  @Override @Test
  public void optional_spec105_emptyStreamMustTerminateBySignallingOnComplete() throws Throwable {
    optionalActivePublisherTest(0, true, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        sub.request(1);
        sub.expectCompletion();
        sub.expectNone();
      }
    });
  }

  @Override @Test
  public void untested_spec106_mustConsiderSubscriptionCancelledAfterOnErrorOrOnCompleteHasBeenCalled() throws Throwable {
    notVerified(); // not really testable without more control over the Publisher
  }

  @Override @Test
  public void required_spec107_mustNotEmitFurtherSignalsOnceOnCompleteHasBeenSignalled() throws Throwable {
    activePublisherTest(1, true, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        sub.request(10);
        sub.nextElement();
        sub.expectCompletion();

        sub.request(10);
        sub.expectNone();
      }
    });
  }

  @Override @Test
  public void untested_spec107_mustNotEmitFurtherSignalsOnceOnErrorHasBeenSignalled() throws Throwable {
    notVerified(); // can we meaningfully test this, without more control over the publisher?
  }

  @Override @Test
  public void untested_spec108_possiblyCanceledSubscriptionShouldNotReceiveOnErrorOrOnCompleteSignals() throws Throwable {
    notVerified(); // can we meaningfully test this?
  }

  @Override @Test
  public void untested_spec109_subscribeShouldNotThrowNonFatalThrowable() throws Throwable {
    notVerified(); // can we meaningfully test this?
  }

  @Override @Test
  public void required_spec109_subscribeThrowNPEOnNullSubscriber() throws Throwable {
    activePublisherTest(0, false, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        try {
            pub.subscribe(null);
            env.flop("Publisher did not throw a NullPointerException when given a null Subscribe in subscribe");
        } catch (NullPointerException ignored) {
          // valid behaviour
        }
        env.verifyNoAsyncErrorsNoDelay();
      }
    });
  }

  @Override @Test
  public void required_spec109_mustIssueOnSubscribeForNonNullSubscriber() throws Throwable {
    activePublisherTest(0, false, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        final Latch onSubscribeLatch = new Latch(env);
        final AtomicReference<Subscription> cancel = new AtomicReference<Subscription>();
        try {
          pub.subscribe(new Subscriber<T>() {
            @Override
            public void onError(Throwable cause) {
              onSubscribeLatch.assertClosed("onSubscribe should be called prior to onError always");
            }

            @Override
            public void onSubscribe(Subscription subs) {
              cancel.set(subs);
              onSubscribeLatch.assertOpen("Only one onSubscribe call expected");
              onSubscribeLatch.close();
            }

            @Override
            public void onNext(T elem) {
              onSubscribeLatch.assertClosed("onSubscribe should be called prior to onNext always");
            }

            @Override
            public void onComplete() {
              onSubscribeLatch.assertClosed("onSubscribe should be called prior to onComplete always");
            }
          });
          onSubscribeLatch.expectClose("Should have received onSubscribe");
          env.verifyNoAsyncErrorsNoDelay();
        } finally {
          Subscription s = cancel.getAndSet(null);
          if (s != null) {
            s.cancel();
          }
        }
      }
    });
  }

  @Override @Test
  public void required_spec109_mayRejectCallsToSubscribeIfPublisherIsUnableOrUnwillingToServeThemRejectionMustTriggerOnErrorAfterOnSubscribe() throws Throwable {
    whenHasErrorPublisherTest(new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        final Latch onErrorLatch = new Latch(env);
        final Latch onSubscribeLatch = new Latch(env);
        ManualSubscriberWithSubscriptionSupport<T> sub = new ManualSubscriberWithSubscriptionSupport<T>(env) {
          @Override
          public void onError(Throwable cause) {
            onSubscribeLatch.assertClosed("onSubscribe should be called prior to onError always");
            onErrorLatch.assertOpen("Only one onError call expected");
            onErrorLatch.close();
          }

          @Override
          public void onSubscribe(Subscription subs) {
            onSubscribeLatch.assertOpen("Only one onSubscribe call expected");
            onSubscribeLatch.close();
          }
        };
        pub.subscribe(sub);
        onSubscribeLatch.expectClose("Should have received onSubscribe");
        onErrorLatch.expectClose("Should have received onError");

        env.verifyNoAsyncErrorsNoDelay();
      }
    });
  }

  @Override @Test
  public void untested_spec110_rejectASubscriptionRequestIfTheSameSubscriberSubscribesTwice() throws Throwable {
    notVerified(); // can we meaningfully test this?
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams-jvm#1.11
  @Override @Test
  public void optional_spec111_maySupportMultiSubscribe() throws Throwable {
    optionalActivePublisherTest(1, false, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        ManualSubscriber<T> sub1 = env.newManualSubscriber(pub);
        ManualSubscriber<T> sub2 = env.newManualSubscriber(pub);

        try {
          env.verifyNoAsyncErrors();
        } finally {
          try {
            sub1.cancel();
          } finally {
            sub2.cancel();
          }
        }
      }
    });
  }

  @Override @Test
  public void optional_spec111_registeredSubscribersMustReceiveOnNextOrOnCompleteSignals() throws Throwable {
    optionalActivePublisherTest(1, false, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        ManualSubscriber<T> sub1 = env.newManualSubscriber(pub);
        ManualSubscriber<T> sub2 = env.newManualSubscriber(pub);
        // Since we're testing the case when the Publisher DOES support the optional multi-subscribers scenario,
        // and decides if it handles them uni-cast or multi-cast, we don't know which subscriber will receive an
        // onNext (and optional onComplete) signal(s) and which just onComplete signal.
        // Plus, even if subscription assumed to be unicast, it's implementation choice, which one will be signalled
        // with onNext.
        sub1.requestNextElementOrEndOfStream();
        sub2.requestNextElementOrEndOfStream();
        try {
            env.verifyNoAsyncErrors();
        } finally {
            try {
                sub1.cancel();
            } finally {
                sub2.cancel();
            }
        }
      }
    });
  }

  @Override @Test
  public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne() throws Throwable {
    optionalActivePublisherTest(5, true, new PublisherTestRun<T>() { // This test is skipped if the publisher is unbounded (never sends onComplete)
      @Override
      public void run(Publisher<T> pub) throws InterruptedException {
        ManualSubscriber<T> sub1 = env.newManualSubscriber(pub);
        ManualSubscriber<T> sub2 = env.newManualSubscriber(pub);
        ManualSubscriber<T> sub3 = env.newManualSubscriber(pub);

        sub1.request(1);
        T x1 = sub1.nextElement(String.format("Publisher %s did not produce the requested 1 element on 1st subscriber", pub));
        sub2.request(2);
        List<T> y1 = sub2.nextElements(2, String.format("Publisher %s did not produce the requested 2 elements on 2nd subscriber", pub));
        sub1.request(1);
        T x2 = sub1.nextElement(String.format("Publisher %s did not produce the requested 1 element on 1st subscriber", pub));
        sub3.request(3);
        List<T> z1 = sub3.nextElements(3, String.format("Publisher %s did not produce the requested 3 elements on 3rd subscriber", pub));
        sub3.request(1);
        T z2 = sub3.nextElement(String.format("Publisher %s did not produce the requested 1 element on 3rd subscriber", pub));
        sub3.request(1);
        T z3 = sub3.nextElement(String.format("Publisher %s did not produce the requested 1 element on 3rd subscriber", pub));
        sub3.requestEndOfStream(String.format("Publisher %s did not complete the stream as expected on 3rd subscriber", pub));
        sub2.request(3);
        List<T> y2 = sub2.nextElements(3, String.format("Publisher %s did not produce the requested 3 elements on 2nd subscriber", pub));
        sub2.requestEndOfStream(String.format("Publisher %s did not complete the stream as expected on 2nd subscriber", pub));
        sub1.request(2);
        List<T> x3 = sub1.nextElements(2, String.format("Publisher %s did not produce the requested 2 elements on 1st subscriber", pub));
        sub1.request(1);
        T x4 = sub1.nextElement(String.format("Publisher %s did not produce the requested 1 element on 1st subscriber", pub));
        sub1.requestEndOfStream(String.format("Publisher %s did not complete the stream as expected on 1st subscriber", pub));

        @SuppressWarnings("unchecked")
        List<T> r = new ArrayList<T>(Arrays.asList(x1, x2));
        r.addAll(x3);
        r.addAll(Collections.singleton(x4));

        List<T> check1 = new ArrayList<T>(y1);
        check1.addAll(y2);

        //noinspection unchecked
        List<T> check2 = new ArrayList<T>(z1);
        check2.add(z2);
        check2.add(z3);

        assertEquals(r, check1, String.format("Publisher %s did not produce the same element sequence for subscribers 1 and 2", pub));
        assertEquals(r, check2, String.format("Publisher %s did not produce the same element sequence for subscribers 1 and 3", pub));
      }
    });
  }

  @Override @Test
  public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront() throws Throwable {
    optionalActivePublisherTest(3, false, new PublisherTestRun<T>() { // This test is skipped if the publisher cannot produce enough elements
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        ManualSubscriber<T> sub1 = env.newManualSubscriber(pub);
        ManualSubscriber<T> sub2 = env.newManualSubscriber(pub);
        ManualSubscriber<T> sub3 = env.newManualSubscriber(pub);

        List<T> received1 = new ArrayList<T>();
        List<T> received2 = new ArrayList<T>();
        List<T> received3 = new ArrayList<T>();

        // if the publisher must touch it's source to notice it's been drained, the OnComplete won't come until we ask for more than it actually contains...
        // edgy edge case?
        sub1.request(4);
        sub2.request(4);
        sub3.request(4);

        received1.addAll(sub1.nextElements(3));
        received2.addAll(sub2.nextElements(3));
        received3.addAll(sub3.nextElements(3));

        // NOTE: can't check completion, the Publisher may not be able to signal it
        //       a similar test *with* completion checking is implemented

        assertEquals(received1, received2, String.format("Expected elements to be signaled in the same sequence to 1st and 2nd subscribers"));
        assertEquals(received2, received3, String.format("Expected elements to be signaled in the same sequence to 2nd and 3rd subscribers"));
      }
    });
  }

  @Override @Test
  public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfrontAndCompleteAsExpected() throws Throwable {
    optionalActivePublisherTest(3, true, new PublisherTestRun<T>() { // This test is skipped if the publisher is unbounded (never sends onComplete)
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        ManualSubscriber<T> sub1 = env.newManualSubscriber(pub);
        ManualSubscriber<T> sub2 = env.newManualSubscriber(pub);
        ManualSubscriber<T> sub3 = env.newManualSubscriber(pub);

        List<T> received1 = new ArrayList<T>();
        List<T> received2 = new ArrayList<T>();
        List<T> received3 = new ArrayList<T>();

        // if the publisher must touch it's source to notice it's been drained, the OnComplete won't come until we ask for more than it actually contains...
        // edgy edge case?
        sub1.request(4);
        sub2.request(4);
        sub3.request(4);

        received1.addAll(sub1.nextElements(3));
        received2.addAll(sub2.nextElements(3));
        received3.addAll(sub3.nextElements(3));

        sub1.expectCompletion();
        sub2.expectCompletion();
        sub3.expectCompletion();

        assertEquals(received1, received2, String.format("Expected elements to be signaled in the same sequence to 1st and 2nd subscribers"));
        assertEquals(received2, received3, String.format("Expected elements to be signaled in the same sequence to 2nd and 3rd subscribers"));
      }
    });
  }

  ///////////////////// SUBSCRIPTION TESTS //////////////////////////////////

  @Override @Test
  public void required_spec302_mustAllowSynchronousRequestCallsFromOnNextAndOnSubscribe() throws Throwable {
    activePublisherTest(6, false, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        ManualSubscriber<T> sub = new ManualSubscriber<T>(env) {
          @Override
          public void onSubscribe(Subscription subs) {
            this.subscription.completeImmediatly(subs);

            subs.request(1);
            subs.request(1);
            subs.request(1);
          }

          @Override
          public void onNext(T element) {
            Subscription subs = this.subscription.value();
            subs.request(1);
          }
        };

        env.subscribe(pub, sub);

        env.verifyNoAsyncErrors();
      }
    });
  }

  @Override @Test
  public void required_spec303_mustNotAllowUnboundedRecursion() throws Throwable {
    final long oneMoreThanBoundedLimit = boundedDepthOfOnNextAndRequestRecursion() + 1;

    activePublisherTest(oneMoreThanBoundedLimit, false, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        final ThreadLocal<Long> stackDepthCounter = new ThreadLocal<Long>() {
          @Override
          protected Long initialValue() {
            return 0L;
          }
        };

        final Latch runCompleted = new Latch(env);

        final ManualSubscriber<T> sub = new ManualSubscriberWithSubscriptionSupport<T>(env) {
          // counts the number of signals received, used to break out from possibly infinite request/onNext loops
          long signalsReceived = 0L;

          @Override
          public void onNext(T element) {
            // NOT calling super.onNext as this test only cares about stack depths, not the actual values of elements
            // which also simplifies this test as we do not have to drain the test buffer, which would otherwise be in danger of overflowing

            signalsReceived += 1;
            stackDepthCounter.set(stackDepthCounter.get() + 1);
            if (env.debugEnabled()) {
              env.debug(String.format("%s(recursion depth: %d)::onNext(%s)", this, stackDepthCounter.get(), element));
            }

            final long callsUntilNow = stackDepthCounter.get();
            if (callsUntilNow > boundedDepthOfOnNextAndRequestRecursion()) {
              env.flop(String.format("Got %d onNext calls within thread: %s, yet expected recursive bound was %d",
                                     callsUntilNow, Thread.currentThread(), boundedDepthOfOnNextAndRequestRecursion()));

              // stop the recursive call chain
              runCompleted.close();
              return;
            } else if (signalsReceived >= oneMoreThanBoundedLimit) {
              // since max number of signals reached, and recursion depth not exceeded, we judge this as a success and
              // stop the recursive call chain
              runCompleted.close();
              return;
            }

            // request more right away, the Publisher must break the recursion
            subscription.value().request(1);

            stackDepthCounter.set(stackDepthCounter.get() - 1);
          }

          @Override
          public void onComplete() {
            super.onComplete();
            runCompleted.close();
          }

          @Override
          public void onError(Throwable cause) {
            super.onError(cause);
            runCompleted.close();
          }
        };

        try {
          env.subscribe(pub, sub);

          sub.request(1); // kick-off the `request -> onNext -> request -> onNext -> ...`

          final String msg = String.format("Unable to validate call stack depth safety, " +
                                               "awaited at-most %s signals (`maxOnNextSignalsInRecursionTest()`) or completion",
                                           oneMoreThanBoundedLimit);
          runCompleted.expectClose(env.defaultTimeoutMillis(), msg);
          env.verifyNoAsyncErrorsNoDelay();
        } finally {
          // since the request/onNext recursive calls may keep the publisher running "forever",
          // we MUST cancel it manually before exiting this test case
          sub.cancel();
        }
      }
    });
  }

  @Override @Test
  public void untested_spec304_requestShouldNotPerformHeavyComputations() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  @Override @Test
  public void untested_spec305_cancelMustNotSynchronouslyPerformHeavyComputation() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  @Override @Test
  public void required_spec306_afterSubscriptionIsCancelledRequestMustBeNops() throws Throwable {
    activePublisherTest(3, false, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {

        // override ManualSubscriberWithSubscriptionSupport#cancel because by default a ManualSubscriber will drop the
        // subscription once it's cancelled (as expected).
        // In this test however it must keep the cancelled Subscription and keep issuing `request(long)` to it.
        ManualSubscriber<T> sub = new ManualSubscriberWithSubscriptionSupport<T>(env) {
          @Override
          public void cancel() {
            if (subscription.isCompleted()) {
              subscription.value().cancel();
            } else {
              env.flop("Cannot cancel a subscription before having received it");
            }
          }
        };

        env.subscribe(pub, sub);

        sub.cancel();
        sub.request(1);
        sub.request(1);
        sub.request(1);

        sub.expectNone();
        env.verifyNoAsyncErrorsNoDelay();
      }
    });
  }

  @Override @Test
  public void required_spec307_afterSubscriptionIsCancelledAdditionalCancelationsMustBeNops() throws Throwable {
    activePublisherTest(1, false, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        final ManualSubscriber<T> sub = env.newManualSubscriber(pub);

        // leak the Subscription
        final Subscription subs = sub.subscription.value();

        subs.cancel();
        subs.cancel();
        subs.cancel();

        sub.expectNone();
        env.verifyNoAsyncErrorsNoDelay();
      }
    });
  }

  @Override @Test
  public void required_spec309_requestZeroMustSignalIllegalArgumentException() throws Throwable {
    activePublisherTest(10, false, new PublisherTestRun<T>() {
      @Override public void run(Publisher<T> pub) throws Throwable {
        final ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        sub.request(0);
        sub.expectError(IllegalArgumentException.class);
      }
    });
  }

  @Override @Test
  public void required_spec309_requestNegativeNumberMustSignalIllegalArgumentException() throws Throwable {
    activePublisherTest(10, false, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        final ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        final Random r = new Random();
        sub.request(-r.nextInt(Integer.MAX_VALUE) - 1);
        // we do require implementations to mention the rule number at the very least, or mentioning that the non-negative request is the problem
        sub.expectError(IllegalArgumentException.class);
      }
    });
  }

  @Override @Test
  public void optional_spec309_requestNegativeNumberMaySignalIllegalArgumentExceptionWithSpecificMessage() throws Throwable {
    optionalActivePublisherTest(10, false, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        final ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        final Random r = new Random();
        sub.request(-r.nextInt(Integer.MAX_VALUE) - 1);
        // we do require implementations to mention the rule number at the very least, or mentioning that the non-negative request is the problem
        sub.expectErrorWithMessage(IllegalArgumentException.class, Arrays.asList("3.9", "non-positive subscription request", "negative subscription request"));
      }
    });
  }

  @Override @Test
  public void required_spec312_cancelMustMakeThePublisherToEventuallyStopSignaling() throws Throwable {
    // the publisher is able to signal more elements than the subscriber will be requesting in total
    final int publisherElements = 20;

    final int demand1 = 10;
    final int demand2 = 5;
    final int totalDemand = demand1 + demand2;

    activePublisherTest(publisherElements, false, new PublisherTestRun<T>() {
      @Override @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
      public void run(Publisher<T> pub) throws Throwable {
        final ManualSubscriber<T> sub = env.newManualSubscriber(pub);

        sub.request(demand1);
        sub.request(demand2);

        /*
          NOTE: The order of the nextElement/cancel calls below is very important (!)

          If this ordering was reversed, given an asynchronous publisher,
          the following scenario would be *legal* and would break this test:

          > AsyncPublisher receives request(10) - it does not emit data right away, it's asynchronous
          > AsyncPublisher receives request(5) - demand is now 15
          ! AsyncPublisher didn't emit any onNext yet (!)
          > AsyncPublisher receives cancel() - handles it right away, by "stopping itself" for example
          ! cancel was handled hefore the AsyncPublisher ever got the chance to emit data
          ! the subscriber ends up never receiving even one element - the test is stuck (and fails, even on valid Publisher)

          Which is why we must first expect an element, and then cancel, once the producing is "running".
         */
        sub.nextElement();
        sub.cancel();

        int onNextsSignalled = 1;

        boolean stillBeingSignalled;
        do {
          // put asyncError if onNext signal received
          sub.expectNone();
          Throwable error = env.dropAsyncError();

          if (error == null) {
            stillBeingSignalled = false;
          } else {
            onNextsSignalled += 1;
            stillBeingSignalled = true;
          }

          // if the Publisher tries to emit more elements than was requested (and/or ignores cancellation) this will throw
          assertTrue(onNextsSignalled <= totalDemand,
                     String.format("Publisher signalled [%d] elements, which is more than the signalled demand: %d",
                                   onNextsSignalled, totalDemand));

        } while (stillBeingSignalled);
      }
    });

    env.verifyNoAsyncErrorsNoDelay();
  }

  @Override @Test
  public void required_spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber() throws Throwable {
    final ReferenceQueue<ManualSubscriber<T>> queue = new ReferenceQueue<ManualSubscriber<T>>();

    final Function<Publisher<T>, WeakReference<ManualSubscriber<T>>> run = new Function<Publisher<T>, WeakReference<ManualSubscriber<T>>>() {
      @Override
      public WeakReference<ManualSubscriber<T>> apply(Publisher<T> pub) throws Exception {
        final ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        final WeakReference<ManualSubscriber<T>> ref = new WeakReference<ManualSubscriber<T>>(sub, queue);

        sub.request(1);
        sub.nextElement();
        sub.cancel();

        return ref;
      }
    };

    activePublisherTest(3, false, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        final WeakReference<ManualSubscriber<T>> ref = run.apply(pub);

        // cancel may be run asynchronously so we add a sleep before running the GC
        // to "resolve" the race
        Thread.sleep(publisherReferenceGCTimeoutMillis);
        System.gc();

        if (!ref.equals(queue.remove(100))) {
          env.flop(String.format("Publisher %s did not drop reference to test subscriber after subscription cancellation", pub));
        }

        env.verifyNoAsyncErrorsNoDelay();
      }
    });
  }

  @Override @Test
  public void required_spec317_mustSupportAPendingElementCountUpToLongMaxValue() throws Throwable {
    final int totalElements = 3;

    activePublisherTest(totalElements, true, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        sub.request(Long.MAX_VALUE);

        sub.nextElements(totalElements);
        sub.expectCompletion();

        env.verifyNoAsyncErrorsNoDelay();
      }
    });
  }

  @Override @Test
  public void required_spec317_mustSupportACumulativePendingElementCountUpToLongMaxValue() throws Throwable {
    final int totalElements = 3;

    activePublisherTest(totalElements, true, new PublisherTestRun<T>() {
      @Override
      public void run(Publisher<T> pub) throws Throwable {
        final ManualSubscriber<T> sub = env.newManualSubscriber(pub);
        sub.request(Long.MAX_VALUE / 2); // pending = Long.MAX_VALUE / 2
        sub.request(Long.MAX_VALUE / 2); // pending = Long.MAX_VALUE - 1
        sub.request(1); // pending = Long.MAX_VALUE

        sub.nextElements(totalElements);
        sub.expectCompletion();

        try {
          env.verifyNoAsyncErrorsNoDelay();
        } finally {
          sub.cancel();
        }

      }
    });
  }

  @Override @Test
  public void required_spec317_mustNotSignalOnErrorWhenPendingAboveLongMaxValue() throws Throwable {
    activePublisherTest(Integer.MAX_VALUE, false, new PublisherTestRun<T>() {
      @Override public void run(Publisher<T> pub) throws Throwable {
        final ManualSubscriberWithSubscriptionSupport<T> sub = new BlackholeSubscriberWithSubscriptionSupport<T>(env) {
           // arbitrarily set limit on nuber of request calls signalled, we expect overflow after already 2 calls,
           // so 10 is relatively high and safe even if arbitrarily chosen
          int callsCounter = 10;

          @Override
          public void onNext(T element) {
            if (env.debugEnabled()) {
              env.debug(String.format("%s::onNext(%s)", this, element));
            }
            if (subscription.isCompleted()) {
              if (callsCounter > 0) {
                subscription.value().request(Long.MAX_VALUE - 1);
                callsCounter--;
              } else {
                  subscription.value().cancel();
              }
            } else {
              env.flop(String.format("Subscriber::onNext(%s) called before Subscriber::onSubscribe", element));
            }
          }
        };
        env.subscribe(pub, sub, env.defaultTimeoutMillis());

        // eventually triggers `onNext`, which will then trigger up to `callsCounter` times `request(Long.MAX_VALUE - 1)`
        // we're pretty sure to overflow from those
        sub.request(1);

        // no onError should be signalled
        try {
          env.verifyNoAsyncErrors();
        } finally {
          sub.cancel();
        }
      }
    });
  }

  ///////////////////// ADDITIONAL "COROLLARY" TESTS ////////////////////////

  ///////////////////// TEST INFRASTRUCTURE /////////////////////////////////

  public interface PublisherTestRun<T> {
    public void run(Publisher<T> pub) throws Throwable;
  }

  /**
   * Test for feature that SHOULD/MUST be implemented, using a live publisher.
   *
   * @param elements the number of elements the Publisher under test  must be able to emit to run this test
   * @param completionSignalRequired true if an {@code onComplete} signal is required by this test to run.
   *                                 If the tested Publisher is unable to signal completion, tests requireing onComplete signals will be skipped.
   *                                 To signal if your Publisher is able to signal completion see {@link PublisherVerification#maxElementsFromPublisher()}.
   */
  public void activePublisherTest(long elements, boolean completionSignalRequired, PublisherTestRun<T> body) throws Throwable {
    if (elements > maxElementsFromPublisher()) {
      throw new SkipException(String.format("Unable to run this test, as required elements nr: %d is higher than supported by given producer: %d", elements, maxElementsFromPublisher()));
    } else if (completionSignalRequired && maxElementsFromPublisher() == Long.MAX_VALUE) {
      throw new SkipException("Unable to run this test, as it requires an onComplete signal, " +
                                "which this Publisher is unable to provide (as signalled by returning Long.MAX_VALUE from `maxElementsFromPublisher()`)");
    } else {
      Publisher<T> pub = createPublisher(elements);
      body.run(pub);
      env.verifyNoAsyncErrorsNoDelay();
    }
  }

  /**
   * Test for feature that MAY be implemented. This test will be marked as SKIPPED if it fails.
   *
   * @param elements the number of elements the Publisher under test  must be able to emit to run this test
   * @param completionSignalRequired true if an {@code onComplete} signal is required by this test to run.
   *                                 If the tested Publisher is unable to signal completion, tests requireing onComplete signals will be skipped.
   *                                 To signal if your Publisher is able to signal completion see {@link PublisherVerification#maxElementsFromPublisher()}.
   */
  public void optionalActivePublisherTest(long elements, boolean completionSignalRequired, PublisherTestRun<T> body) throws Throwable {
    if (elements > maxElementsFromPublisher()) {
      throw new SkipException(String.format("Unable to run this test, as required elements nr: %d is higher than supported by given producer: %d", elements, maxElementsFromPublisher()));
    } else if (completionSignalRequired && maxElementsFromPublisher() == Long.MAX_VALUE) {
      throw new SkipException("Unable to run this test, as it requires an onComplete signal, " +
                                "which this Publisher is unable to provide (as signalled by returning Long.MAX_VALUE from `maxElementsFromPublisher()`)");
    } else {

      final Publisher<T> pub = createPublisher(elements);
      final String skipMessage = "Skipped because tested publisher does NOT implement this OPTIONAL requirement.";

      try {
        potentiallyPendingTest(pub, body);
      } catch (Exception ex) {
        notVerified(skipMessage);
      } catch (AssertionError ex) {
        notVerified(skipMessage + " Reason for skipping was: " + ex.getMessage());
      }
    }
  }

  public static final String SKIPPING_NO_ERROR_PUBLISHER_AVAILABLE =
    "Skipping because no error state Publisher provided, and the test requires it. " +
          "Please implement PublisherVerification#createFailedPublisher to run this test.";

  public static final String SKIPPING_OPTIONAL_TEST_FAILED =
    "Skipping, because provided Publisher does not pass this *additional* verification.";
  /**
   * Additional test for Publisher in error state
   */
  public void whenHasErrorPublisherTest(PublisherTestRun<T> body) throws Throwable {
    potentiallyPendingTest(createFailedPublisher(), body, SKIPPING_NO_ERROR_PUBLISHER_AVAILABLE);
  }

  public void potentiallyPendingTest(Publisher<T> pub, PublisherTestRun<T> body) throws Throwable {
    potentiallyPendingTest(pub, body, SKIPPING_OPTIONAL_TEST_FAILED);
  }

  public void potentiallyPendingTest(Publisher<T> pub, PublisherTestRun<T> body, String message) throws Throwable {
    if (pub != null) {
      body.run(pub);
    } else {
      throw new SkipException(message);
    }
  }

  /**
   * Executes a given test body {@code n} times.
   * All the test runs must pass in order for the stochastic test to pass.
   */
  public void stochasticTest(int n, Function<Integer, Void> body) throws Throwable {
    if (skipStochasticTests()) {
      notVerified("Skipping @Stochastic test because `skipStochasticTests()` returned `true`!");
    }

    for (int i = 0; i < n; i++) {
      body.apply(i);
    }
  }

  public void notVerified() {
    throw new SkipException("Not verified by this TCK.");
  }

  /**
   * Return this value from {@link PublisherVerification#maxElementsFromPublisher()} to mark that the given {@link org.reactivestreams.Publisher},
   * is not able to signal completion. For example it is strictly a time-bound or unbounded source of data.
   *
   * <b>Returning this value from {@link PublisherVerification#maxElementsFromPublisher()} will result in skipping all TCK tests which require onComplete signals!</b>
   */
  public long publisherUnableToSignalOnComplete() {
    return Long.MAX_VALUE;
  }

  public void notVerified(String message) {
    throw new SkipException(message);
  }

}
