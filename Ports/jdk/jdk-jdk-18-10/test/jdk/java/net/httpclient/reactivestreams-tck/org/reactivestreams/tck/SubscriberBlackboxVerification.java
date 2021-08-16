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
import org.reactivestreams.tck.TestEnvironment.ManualPublisher;
import org.reactivestreams.tck.TestEnvironment.ManualSubscriber;
import org.reactivestreams.tck.flow.support.Optional;
import org.reactivestreams.tck.flow.support.SubscriberBlackboxVerificationRules;
import org.reactivestreams.tck.flow.support.TestException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.reactivestreams.tck.SubscriberWhiteboxVerification.BlackboxSubscriberProxy;
import static org.testng.Assert.assertTrue;

/**
 * Provides tests for verifying {@link org.reactivestreams.Subscriber} and {@link org.reactivestreams.Subscription}
 * specification rules, without any modifications to the tested implementation (also known as "Black Box" testing).
 *
 * This verification is NOT able to check many of the rules of the spec, and if you want more
 * verification of your implementation you'll have to implement {@code org.reactivestreams.tck.SubscriberWhiteboxVerification}
 * instead.
 *
 * @see org.reactivestreams.Subscriber
 * @see org.reactivestreams.Subscription
 */
public abstract class SubscriberBlackboxVerification<T> extends WithHelperPublisher<T>
  implements SubscriberBlackboxVerificationRules {

  protected final TestEnvironment env;

  protected SubscriberBlackboxVerification(TestEnvironment env) {
    this.env = env;
  }

  // USER API

  /**
   * This is the main method you must implement in your test incarnation.
   * It must create a new {@link org.reactivestreams.Subscriber} instance to be subjected to the testing logic.
   */
  public abstract Subscriber<T> createSubscriber();

  /**
   * Override this method if the Subscriber implementation you are verifying
   * needs an external signal before it signals demand to its Publisher.
   *
   * By default this method does nothing.
   */
  public void triggerRequest(final Subscriber<? super T> subscriber) {
    // this method is intentionally left blank
  }

  // ENV SETUP

  /**
   * Executor service used by the default provided asynchronous Publisher.
   * @see #createHelperPublisher(long)
   */
  private ExecutorService publisherExecutor;
  @BeforeClass public void startPublisherExecutorService() { publisherExecutor = Executors.newFixedThreadPool(4); }
  @AfterClass public void shutdownPublisherExecutorService() { if (publisherExecutor != null) publisherExecutor.shutdown(); }
  @Override public ExecutorService publisherExecutorService() { return publisherExecutor; }

  ////////////////////// TEST ENV CLEANUP /////////////////////////////////////

  @BeforeMethod
  public void setUp() throws Exception {
    env.clearAsyncErrors();
  }

  ////////////////////// SPEC RULE VERIFICATION ///////////////////////////////

  @Override @Test
  public void required_spec201_blackbox_mustSignalDemandViaSubscriptionRequest() throws Throwable {
    blackboxSubscriberTest(new BlackboxTestStageTestRun() {
      @Override
      public void run(BlackboxTestStage stage) throws InterruptedException {
        triggerRequest(stage.subProxy().sub());
        final long requested = stage.expectRequest();// assuming subscriber wants to consume elements...
        final long signalsToEmit = Math.min(requested, 512); // protecting against Subscriber which sends ridiculous large demand

        // should cope with up to requested number of elements
        for (int i = 0; i < signalsToEmit && sampleIsCancelled(stage, i, 10); i++)
          stage.signalNext();

        // we complete after `signalsToEmit` (which can be less than `requested`),
        // which is legal under https://github.com/reactive-streams/reactive-streams-jvm#1.2
        stage.sendCompletion();
      }

      /**
       * In order to allow some "skid" and not check state on each iteration,
       * only check {@code stage.isCancelled} every {@code checkInterval}'th iteration.
       */
      private boolean sampleIsCancelled(BlackboxTestStage stage, int i, int checkInterval) throws InterruptedException {
        if (i % checkInterval == 0) return stage.isCancelled();
        else return false;
      }
    });
  }

  @Override @Test
  public void untested_spec202_blackbox_shouldAsynchronouslyDispatch() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  @Override @Test
  public void required_spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnComplete() throws Throwable {
    blackboxSubscriberWithoutSetupTest(new BlackboxTestStageTestRun() {
      @Override
      public void run(BlackboxTestStage stage) throws Throwable {
        final Subscription subs = new Subscription() {
          @Override
          public void request(long n) {
            final Optional<StackTraceElement> onCompleteStackTraceElement = env.findCallerMethodInStackTrace("onComplete");
            if (onCompleteStackTraceElement.isDefined()) {
              final StackTraceElement stackElem = onCompleteStackTraceElement.get();
              env.flop(String.format("Subscription::request MUST NOT be called from Subscriber::onComplete (Rule 2.3)! (Caller: %s::%s line %d)",
                                     stackElem.getClassName(), stackElem.getMethodName(), stackElem.getLineNumber()));
            }
          }

          @Override
          public void cancel() {
            final Optional<StackTraceElement> onCompleteStackElement = env.findCallerMethodInStackTrace("onComplete");
            if (onCompleteStackElement.isDefined()) {
              final StackTraceElement stackElem = onCompleteStackElement.get();
              env.flop(String.format("Subscription::cancel MUST NOT be called from Subscriber::onComplete (Rule 2.3)! (Caller: %s::%s line %d)",
                                     stackElem.getClassName(), stackElem.getMethodName(), stackElem.getLineNumber()));
            }
          }
        };

        final Subscriber<T> sub = createSubscriber();
        sub.onSubscribe(subs);
        sub.onComplete();

        env.verifyNoAsyncErrorsNoDelay();
      }
    });
  }

  @Override @Test
  public void required_spec203_blackbox_mustNotCallMethodsOnSubscriptionOrPublisherInOnError() throws Throwable {
    blackboxSubscriberWithoutSetupTest(new BlackboxTestStageTestRun() {
      @Override
      public void run(BlackboxTestStage stage) throws Throwable {
        final Subscription subs = new Subscription() {
          @Override
          public void request(long n) {
            Throwable thr = new Throwable();
            for (StackTraceElement stackElem : thr.getStackTrace()) {
              if (stackElem.getMethodName().equals("onError")) {
                env.flop(String.format("Subscription::request MUST NOT be called from Subscriber::onError (Rule 2.3)! (Caller: %s::%s line %d)",
                                       stackElem.getClassName(), stackElem.getMethodName(), stackElem.getLineNumber()));
              }
            }
          }

          @Override
          public void cancel() {
            Throwable thr = new Throwable();
            for (StackTraceElement stackElem : thr.getStackTrace()) {
              if (stackElem.getMethodName().equals("onError")) {
                env.flop(String.format("Subscription::cancel MUST NOT be called from Subscriber::onError (Rule 2.3)! (Caller: %s::%s line %d)",
                                       stackElem.getClassName(), stackElem.getMethodName(), stackElem.getLineNumber()));
              }
            }
          }
        };

        final Subscriber<T> sub = createSubscriber();
        sub.onSubscribe(subs);
        sub.onError(new TestException());

        env.verifyNoAsyncErrorsNoDelay();
      }
    });
  }

  @Override @Test
  public void untested_spec204_blackbox_mustConsiderTheSubscriptionAsCancelledInAfterRecievingOnCompleteOrOnError() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  @Override @Test
  public void required_spec205_blackbox_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal() throws Exception {
    new BlackboxTestStage(env) {{
      // try to subscribe another time, if the subscriber calls `probe.registerOnSubscribe` the test will fail
      final TestEnvironment.Latch secondSubscriptionCancelled = new TestEnvironment.Latch(env);
      sub().onSubscribe(
          new Subscription() {
            @Override
            public void request(long elements) {
              env.flop(String.format("Subscriber %s illegally called `subscription.request(%s)`!", sub(), elements));
            }

            @Override
            public void cancel() {
              secondSubscriptionCancelled.close();
            }

            @Override
            public String toString() {
              return "SecondSubscription(should get cancelled)";
            }
          });

      secondSubscriptionCancelled.expectClose("Expected SecondSubscription given to subscriber to be cancelled, but `Subscription.cancel()` was not called.");
      env.verifyNoAsyncErrorsNoDelay();
      sendCompletion(); // we're done, complete the subscriber under test
    }};
  }

  @Override @Test
  public void untested_spec206_blackbox_mustCallSubscriptionCancelIfItIsNoLongerValid() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  @Override @Test
  public void untested_spec207_blackbox_mustEnsureAllCallsOnItsSubscriptionTakePlaceFromTheSameThreadOrTakeCareOfSynchronization() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
    // the same thread part of the clause can be verified but that is not very useful, or is it?
  }

  @Override @Test
  public void untested_spec208_blackbox_mustBePreparedToReceiveOnNextSignalsAfterHavingCalledSubscriptionCancel() throws Throwable {
    notVerified(); // cannot be meaningfully tested as black box, or can it?
  }

  @Override @Test
  public void required_spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithPrecedingRequestCall() throws Throwable {
    blackboxSubscriberTest(new BlackboxTestStageTestRun() {
      @Override @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
      public void run(BlackboxTestStage stage) throws Throwable {
        triggerRequest(stage.subProxy().sub());
        final long notUsed = stage.expectRequest(); // received request signal
        stage.sub().onComplete();
        stage.subProxy().expectCompletion();
      }
    });
  }

  @Override @Test
  public void required_spec209_blackbox_mustBePreparedToReceiveAnOnCompleteSignalWithoutPrecedingRequestCall() throws Throwable {
    blackboxSubscriberTest(new BlackboxTestStageTestRun() {
      @Override @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
      public void run(BlackboxTestStage stage) throws Throwable {
        final Subscriber<? super T> sub = stage.sub();
        sub.onComplete();
        stage.subProxy().expectCompletion();
      }
    });
  }

  @Override @Test
  public void required_spec210_blackbox_mustBePreparedToReceiveAnOnErrorSignalWithPrecedingRequestCall() throws Throwable {
    blackboxSubscriberTest(new BlackboxTestStageTestRun() {
      @Override @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
      public void run(BlackboxTestStage stage) throws Throwable {
        triggerRequest(stage.subProxy().sub());
        final long notUsed = stage.expectRequest(); // received request signal
        stage.sub().onError(new TestException()); // in response to that, we fail
        stage.subProxy().expectError(Throwable.class);
      }
    });
  }

  // Verifies rule: https://github.com/reactive-streams/reactive-streams-jvm#2.10
  @Override @Test
  public void required_spec210_blackbox_mustBePreparedToReceiveAnOnErrorSignalWithoutPrecedingRequestCall() throws Throwable {
    blackboxSubscriberTest(new BlackboxTestStageTestRun() {
      @Override @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
      public void run(BlackboxTestStage stage) throws Throwable {

        stage.sub().onError(new TestException());
        stage.subProxy().expectError(Throwable.class);
      }
    });
  }

  @Override @Test
  public void untested_spec211_blackbox_mustMakeSureThatAllCallsOnItsMethodsHappenBeforeTheProcessingOfTheRespectiveEvents() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  @Override @Test
  public void untested_spec212_blackbox_mustNotCallOnSubscribeMoreThanOnceBasedOnObjectEquality() throws Throwable {
    notVerified(); // cannot be meaningfully tested as black box, or can it?
  }

  @Override @Test
  public void untested_spec213_blackbox_failingOnSignalInvocation() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  @Override @Test
  public void required_spec213_blackbox_onSubscribe_mustThrowNullPointerExceptionWhenParametersAreNull() throws Throwable {
    blackboxSubscriberWithoutSetupTest(new BlackboxTestStageTestRun() {
      @Override
      public void run(BlackboxTestStage stage) throws Throwable {

        {
          final Subscriber<T> sub = createSubscriber();
          boolean gotNPE = false;
          try {
            sub.onSubscribe(null);
          } catch(final NullPointerException expected) {
            gotNPE = true;
          }
          assertTrue(gotNPE, "onSubscribe(null) did not throw NullPointerException");
        }

        env.verifyNoAsyncErrorsNoDelay();
      }
    });
  }

  @Override @Test
  public void required_spec213_blackbox_onNext_mustThrowNullPointerExceptionWhenParametersAreNull() throws Throwable {
    blackboxSubscriberWithoutSetupTest(new BlackboxTestStageTestRun() {
      @Override
      public void run(BlackboxTestStage stage) throws Throwable {
        final Subscription subscription = new Subscription() {
          @Override public void request(final long elements) {}
          @Override public void cancel() {}
        };

        {
          final Subscriber<T> sub = createSubscriber();
          boolean gotNPE = false;
          sub.onSubscribe(subscription);
          try {
            sub.onNext(null);
          } catch(final NullPointerException expected) {
            gotNPE = true;
          }
          assertTrue(gotNPE, "onNext(null) did not throw NullPointerException");
        }

        env.verifyNoAsyncErrorsNoDelay();
      }
    });
  }

  @Override @Test
  public void required_spec213_blackbox_onError_mustThrowNullPointerExceptionWhenParametersAreNull() throws Throwable {
    blackboxSubscriberWithoutSetupTest(new BlackboxTestStageTestRun() {
      @Override
      public void run(BlackboxTestStage stage) throws Throwable {
        final Subscription subscription = new Subscription() {
          @Override public void request(final long elements) {}
          @Override public void cancel() {}
        };

        {
          final Subscriber<T> sub = createSubscriber();
          boolean gotNPE = false;
          sub.onSubscribe(subscription);
          try {
            sub.onError(null);
          } catch(final NullPointerException expected) {
            gotNPE = true;
          }
          assertTrue(gotNPE, "onError(null) did not throw NullPointerException");
        }

        env.verifyNoAsyncErrorsNoDelay();
      }
    });
  }

  ////////////////////// SUBSCRIPTION SPEC RULE VERIFICATION //////////////////

  @Override @Test
  public void untested_spec301_blackbox_mustNotBeCalledOutsideSubscriberContext() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  @Override @Test
  public void untested_spec308_blackbox_requestMustRegisterGivenNumberElementsToBeProduced() throws Throwable {
    notVerified(); // cannot be meaningfully tested as black box, or can it?
  }

  @Override @Test
  public void untested_spec310_blackbox_requestMaySynchronouslyCallOnNextOnSubscriber() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  @Override @Test
  public void untested_spec311_blackbox_requestMaySynchronouslyCallOnCompleteOrOnError() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  @Override @Test
  public void untested_spec314_blackbox_cancelMayCauseThePublisherToShutdownIfNoOtherSubscriptionExists() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  @Override @Test
  public void untested_spec315_blackbox_cancelMustNotThrowExceptionAndMustSignalOnError() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  @Override @Test
  public void untested_spec316_blackbox_requestMustNotThrowExceptionAndMustOnErrorTheSubscriber() throws Exception {
    notVerified(); // cannot be meaningfully tested, or can it?
  }

  /////////////////////// ADDITIONAL "COROLLARY" TESTS ////////////////////////

  /////////////////////// TEST INFRASTRUCTURE /////////////////////////////////

  abstract class BlackboxTestStageTestRun {
    public abstract void run(BlackboxTestStage stage) throws Throwable;
  }

  public void blackboxSubscriberTest(BlackboxTestStageTestRun body) throws Throwable {
    BlackboxTestStage stage = new BlackboxTestStage(env, true);
    body.run(stage);
  }

  public void blackboxSubscriberWithoutSetupTest(BlackboxTestStageTestRun body) throws Throwable {
    BlackboxTestStage stage = new BlackboxTestStage(env, false);
    body.run(stage);
  }

  public class BlackboxTestStage extends ManualPublisher<T> {
    public Publisher<T> pub;
    public ManualSubscriber<T> tees; // gives us access to an infinite stream of T values

    public T lastT = null;
    private Optional<BlackboxSubscriberProxy<T>> subProxy = Optional.empty();

    public BlackboxTestStage(TestEnvironment env) throws InterruptedException {
      this(env, true);
    }

    public BlackboxTestStage(TestEnvironment env, boolean runDefaultInit) throws InterruptedException {
      super(env);
      if (runDefaultInit) {
        pub = this.createHelperPublisher(Long.MAX_VALUE);
        tees = env.newManualSubscriber(pub);
        Subscriber<T> sub = createSubscriber();
        subProxy = Optional.of(createBlackboxSubscriberProxy(env, sub));
        subscribe(subProxy.get());
      }
    }

    public Subscriber<? super T> sub() {
      return subscriber.value();
    }

    /**
     * Proxy for the {@link #sub()} {@code Subscriber}, providing certain assertions on methods being called on the Subscriber.
     */
    public BlackboxSubscriberProxy<T> subProxy() {
      return subProxy.get();
    }

    public Publisher<T> createHelperPublisher(long elements) {
      return SubscriberBlackboxVerification.this.createHelperPublisher(elements);
    }

    public BlackboxSubscriberProxy<T> createBlackboxSubscriberProxy(TestEnvironment env, Subscriber<T> sub) {
      return new BlackboxSubscriberProxy<T>(env, sub);
    }

    public T signalNext() throws InterruptedException {
      T element = nextT();
      sendNext(element);
      return element;
    }

    public T nextT() throws InterruptedException {
      lastT = tees.requestNextElement();
      return lastT;
    }

  }

  public void notVerified() {
    throw new SkipException("Not verified using this TCK.");
  }
}
