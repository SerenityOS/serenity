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

package org.reactivestreams.example.unicast;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AsyncSubscriber is an implementation of Reactive Streams `Subscriber`,
 * it runs asynchronously (on an Executor), requests one element
 * at a time, and invokes a user-defined method to process each element.
 *
 * NOTE: The code below uses a lot of try-catches to show the reader where exceptions can be expected, and where they are forbidden.
 */
public abstract class AsyncSubscriber<T> implements Subscriber<T>, Runnable {

  // Signal represents the asynchronous protocol between the Publisher and Subscriber
  private static interface Signal {}

  private enum OnComplete implements Signal { Instance; }

  private static class OnError implements Signal {
    public final Throwable error;
    public OnError(final Throwable error) { this.error = error; }
  }

  private static class OnNext<T> implements Signal {
    public final T next;
    public OnNext(final T next) { this.next = next; }
  }

  private static class OnSubscribe implements Signal {
    public final Subscription subscription;
    public OnSubscribe(final Subscription subscription) { this.subscription = subscription; }
  }

  private Subscription subscription; // Obeying rule 3.1, we make this private!
  private boolean done; // It's useful to keep track of whether this Subscriber is done or not
  private final Executor executor; // This is the Executor we'll use to be asynchronous, obeying rule 2.2

  // Only one constructor, and it's only accessible for the subclasses
  protected AsyncSubscriber(Executor executor) {
    if (executor == null) throw null;
    this.executor = executor;
  }

  // Showcases a convenience method to idempotently marking the Subscriber as "done", so we don't want to process more elements
  // herefor we also need to cancel our `Subscription`.
  private final void done() {
    //On this line we could add a guard against `!done`, but since rule 3.7 says that `Subscription.cancel()` is idempotent, we don't need to.
    done = true; // If `whenNext` throws an exception, let's consider ourselves done (not accepting more elements)
    if (subscription != null) { // If we are bailing out before we got a `Subscription` there's little need for cancelling it.
      try {
        subscription.cancel(); // Cancel the subscription
      } catch(final Throwable t) {
        //Subscription.cancel is not allowed to throw an exception, according to rule 3.15
        (new IllegalStateException(subscription + " violated the Reactive Streams rule 3.15 by throwing an exception from cancel.", t)).printStackTrace(System.err);
      }
    }
  }

  // This method is invoked when the OnNext signals arrive
  // Returns whether more elements are desired or not, and if no more elements are desired,
  // for convenience.
  protected abstract boolean whenNext(final T element);

  // This method is invoked when the OnComplete signal arrives
  // override this method to implement your own custom onComplete logic.
  protected void whenComplete() { }

  // This method is invoked if the OnError signal arrives
  // override this method to implement your own custom onError logic.
  protected void whenError(Throwable error) { }

  private final void handleOnSubscribe(final Subscription s) {
    if (s == null) {
      // Getting a null `Subscription` here is not valid so lets just ignore it.
    } else if (subscription != null) { // If someone has made a mistake and added this Subscriber multiple times, let's handle it gracefully
      try {
        s.cancel(); // Cancel the additional subscription to follow rule 2.5
      } catch(final Throwable t) {
        //Subscription.cancel is not allowed to throw an exception, according to rule 3.15
        (new IllegalStateException(s + " violated the Reactive Streams rule 3.15 by throwing an exception from cancel.", t)).printStackTrace(System.err);
      }
    } else {
      // We have to assign it locally before we use it, if we want to be a synchronous `Subscriber`
      // Because according to rule 3.10, the Subscription is allowed to call `onNext` synchronously from within `request`
      subscription = s;
      try {
        // If we want elements, according to rule 2.1 we need to call `request`
        // And, according to rule 3.2 we are allowed to call this synchronously from within the `onSubscribe` method
        s.request(1); // Our Subscriber is unbuffered and modest, it requests one element at a time
      } catch(final Throwable t) {
        // Subscription.request is not allowed to throw according to rule 3.16
        (new IllegalStateException(s + " violated the Reactive Streams rule 3.16 by throwing an exception from request.", t)).printStackTrace(System.err);
      }
    }
  }

  private final void handleOnNext(final T element) {
    if (!done) { // If we aren't already done
      if(subscription == null) { // Technically this check is not needed, since we are expecting Publishers to conform to the spec
        // Check for spec violation of 2.1 and 1.09
        (new IllegalStateException("Someone violated the Reactive Streams rule 1.09 and 2.1 by signalling OnNext before `Subscription.request`. (no Subscription)")).printStackTrace(System.err);
      } else {
        try {
          if (whenNext(element)) {
            try {
              subscription.request(1); // Our Subscriber is unbuffered and modest, it requests one element at a time
            } catch(final Throwable t) {
              // Subscription.request is not allowed to throw according to rule 3.16
              (new IllegalStateException(subscription + " violated the Reactive Streams rule 3.16 by throwing an exception from request.", t)).printStackTrace(System.err);
            }
          } else {
            done(); // This is legal according to rule 2.6
          }
        } catch(final Throwable t) {
          done();
          try {
            onError(t);
          } catch(final Throwable t2) {
            //Subscriber.onError is not allowed to throw an exception, according to rule 2.13
            (new IllegalStateException(this + " violated the Reactive Streams rule 2.13 by throwing an exception from onError.", t2)).printStackTrace(System.err);
          }
        }
      }
    }
  }

  // Here it is important that we do not violate 2.2 and 2.3 by calling methods on the `Subscription` or `Publisher`
  private void handleOnComplete() {
    if (subscription == null) { // Technically this check is not needed, since we are expecting Publishers to conform to the spec
      // Publisher is not allowed to signal onComplete before onSubscribe according to rule 1.09
      (new IllegalStateException("Publisher violated the Reactive Streams rule 1.09 signalling onComplete prior to onSubscribe.")).printStackTrace(System.err);
    } else {
      done = true; // Obey rule 2.4
      whenComplete();
    }
  }

  // Here it is important that we do not violate 2.2 and 2.3 by calling methods on the `Subscription` or `Publisher`
  private void handleOnError(final Throwable error) {
    if (subscription == null) { // Technically this check is not needed, since we are expecting Publishers to conform to the spec
      // Publisher is not allowed to signal onError before onSubscribe according to rule 1.09
      (new IllegalStateException("Publisher violated the Reactive Streams rule 1.09 signalling onError prior to onSubscribe.")).printStackTrace(System.err);
    } else {
      done = true; // Obey rule 2.4
      whenError(error);
    }
  }

  // We implement the OnX methods on `Subscriber` to send Signals that we will process asycnhronously, but only one at a time

  @Override public final void onSubscribe(final Subscription s) {
    // As per rule 2.13, we need to throw a `java.lang.NullPointerException` if the `Subscription` is `null`
    if (s == null) throw null;

    signal(new OnSubscribe(s));
  }

  @Override public final void onNext(final T element) {
    // As per rule 2.13, we need to throw a `java.lang.NullPointerException` if the `element` is `null`
    if (element == null) throw null;

    signal(new OnNext<T>(element));
  }

  @Override public final void onError(final Throwable t) {
    // As per rule 2.13, we need to throw a `java.lang.NullPointerException` if the `Throwable` is `null`
    if (t == null) throw null;

    signal(new OnError(t));
  }

  @Override public final void onComplete() {
     signal(OnComplete.Instance);
  }

  // This `ConcurrentLinkedQueue` will track signals that are sent to this `Subscriber`, like `OnComplete` and `OnNext` ,
  // and obeying rule 2.11
  private final ConcurrentLinkedQueue<Signal> inboundSignals = new ConcurrentLinkedQueue<Signal>();

  // We are using this `AtomicBoolean` to make sure that this `Subscriber` doesn't run concurrently with itself,
  // obeying rule 2.7 and 2.11
  private final AtomicBoolean on = new AtomicBoolean(false);

   @SuppressWarnings("unchecked")
   @Override public final void run() {
    if(on.get()) { // establishes a happens-before relationship with the end of the previous run
      try {
        final Signal s = inboundSignals.poll(); // We take a signal off the queue
        if (!done) { // If we're done, we shouldn't process any more signals, obeying rule 2.8
          // Below we simply unpack the `Signal`s and invoke the corresponding methods
          if (s instanceof OnNext<?>)
            handleOnNext(((OnNext<T>)s).next);
          else if (s instanceof OnSubscribe)
            handleOnSubscribe(((OnSubscribe)s).subscription);
          else if (s instanceof OnError) // We are always able to handle OnError, obeying rule 2.10
            handleOnError(((OnError)s).error);
          else if (s == OnComplete.Instance) // We are always able to handle OnComplete, obeying rule 2.9
            handleOnComplete();
        }
      } finally {
        on.set(false); // establishes a happens-before relationship with the beginning of the next run
        if(!inboundSignals.isEmpty()) // If we still have signals to process
          tryScheduleToExecute(); // Then we try to schedule ourselves to execute again
      }
    }
  }

  // What `signal` does is that it sends signals to the `Subscription` asynchronously
  private void signal(final Signal signal) {
    if (inboundSignals.offer(signal)) // No need to null-check here as ConcurrentLinkedQueue does this for us
      tryScheduleToExecute(); // Then we try to schedule it for execution, if it isn't already
  }

  // This method makes sure that this `Subscriber` is only executing on one Thread at a time
  private final void tryScheduleToExecute() {
    if(on.compareAndSet(false, true)) {
      try {
        executor.execute(this);
      } catch(Throwable t) { // If we can't run on the `Executor`, we need to fail gracefully and not violate rule 2.13
        if (!done) {
          try {
            done(); // First of all, this failure is not recoverable, so we need to cancel our subscription
          } finally {
            inboundSignals.clear(); // We're not going to need these anymore
            // This subscription is cancelled by now, but letting the Subscriber become schedulable again means
            // that we can drain the inboundSignals queue if anything arrives after clearing
            on.set(false);
          }
        }
      }
    }
  }
}
