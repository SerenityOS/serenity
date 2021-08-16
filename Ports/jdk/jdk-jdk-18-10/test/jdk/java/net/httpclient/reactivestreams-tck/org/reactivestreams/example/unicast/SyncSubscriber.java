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

/**
 * SyncSubscriber is an implementation of Reactive Streams `Subscriber`,
 * it runs synchronously (on the Publisher's thread) and requests one element
 * at a time and invokes a user-defined method to process each element.
 *
 * NOTE: The code below uses a lot of try-catches to show the reader where exceptions can be expected, and where they are forbidden.
 */
public abstract class SyncSubscriber<T> implements Subscriber<T> {
  private Subscription subscription; // Obeying rule 3.1, we make this private!
  private boolean done = false;

  @Override public void onSubscribe(final Subscription s) {
    // As per rule 2.13, we need to throw a `java.lang.NullPointerException` if the `Subscription` is `null`
    if (s == null) throw null;

    if (subscription != null) { // If someone has made a mistake and added this Subscriber multiple times, let's handle it gracefully
      try {
        s.cancel(); // Cancel the additional subscription
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

  @Override public void onNext(final T element) {
    if (subscription == null) { // Technically this check is not needed, since we are expecting Publishers to conform to the spec
      (new IllegalStateException("Publisher violated the Reactive Streams rule 1.09 signalling onNext prior to onSubscribe.")).printStackTrace(System.err);
    } else {
      // As per rule 2.13, we need to throw a `java.lang.NullPointerException` if the `element` is `null`
      if (element == null) throw null;

      if (!done) { // If we aren't already done
        try {
          if (whenNext(element)) {
            try {
              subscription.request(1); // Our Subscriber is unbuffered and modest, it requests one element at a time
            } catch (final Throwable t) {
              // Subscription.request is not allowed to throw according to rule 3.16
              (new IllegalStateException(subscription + " violated the Reactive Streams rule 3.16 by throwing an exception from request.", t)).printStackTrace(System.err);
            }
          } else {
            done();
          }
        } catch (final Throwable t) {
          done();
          try {
            onError(t);
          } catch (final Throwable t2) {
            //Subscriber.onError is not allowed to throw an exception, according to rule 2.13
            (new IllegalStateException(this + " violated the Reactive Streams rule 2.13 by throwing an exception from onError.", t2)).printStackTrace(System.err);
          }
        }
      }
    }
  }

  // Showcases a convenience method to idempotently marking the Subscriber as "done", so we don't want to process more elements
  // herefor we also need to cancel our `Subscription`.
  private void done() {
    //On this line we could add a guard against `!done`, but since rule 3.7 says that `Subscription.cancel()` is idempotent, we don't need to.
    done = true; // If we `whenNext` throws an exception, let's consider ourselves done (not accepting more elements)
    try {
      subscription.cancel(); // Cancel the subscription
    } catch(final Throwable t) {
      //Subscription.cancel is not allowed to throw an exception, according to rule 3.15
      (new IllegalStateException(subscription + " violated the Reactive Streams rule 3.15 by throwing an exception from cancel.", t)).printStackTrace(System.err);
    }
  }

  // This method is left as an exercise to the reader/extension point
  // Returns whether more elements are desired or not, and if no more elements are desired
  protected abstract boolean whenNext(final T element);

  @Override public void onError(final Throwable t) {
    if (subscription == null) { // Technically this check is not needed, since we are expecting Publishers to conform to the spec
      (new IllegalStateException("Publisher violated the Reactive Streams rule 1.09 signalling onError prior to onSubscribe.")).printStackTrace(System.err);
    } else {
      // As per rule 2.13, we need to throw a `java.lang.NullPointerException` if the `Throwable` is `null`
      if (t == null) throw null;
      // Here we are not allowed to call any methods on the `Subscription` or the `Publisher`, as per rule 2.3
      // And anyway, the `Subscription` is considered to be cancelled if this method gets called, as per rule 2.4
    }
  }

  @Override public void onComplete() {
    if (subscription == null) { // Technically this check is not needed, since we are expecting Publishers to conform to the spec
      (new IllegalStateException("Publisher violated the Reactive Streams rule 1.09 signalling onComplete prior to onSubscribe.")).printStackTrace(System.err);
    } else {
      // Here we are not allowed to call any methods on the `Subscription` or the `Publisher`, as per rule 2.3
      // And anyway, the `Subscription` is considered to be cancelled if this method gets called, as per rule 2.4
    }
  }
}
