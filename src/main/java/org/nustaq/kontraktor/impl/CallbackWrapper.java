package org.nustaq.kontraktor.impl;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.util.Log;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Copyright (c) 2014, Ruediger Moeller. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Date: 03.01.14
 * Time: 18:46
 */

/**
 * ..
 */
public class CallbackWrapper<T> implements IPromise<T>, Serializable {

    static Method receiveRes;

    static {
        try {
            receiveRes = Callback.class.getMethod("complete", new Class[]{Object.class,Object.class});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    final Actor targetActor;
    Callback<T> realCallback;

    public CallbackWrapper(Actor targetQ, Callback<T> realFuture) {
        this.realCallback = realFuture;
        this.targetActor = targetQ;
    }

    @Override
    public void complete(T result, Object error) {
        if ( realCallback == null ) {
            return;
        }
        if ( targetActor == null ) {
            // call came from outside the actor world => use current thread => blocking the callback blocks actor, dont't !
            try {
                receiveRes.invoke(realCallback, result, error);
            } catch (Exception e) {
                Log.Warn( this, e, "" );
            }
        } else {
            CallEntry ce = new CallEntry( realCallback, receiveRes, new Object[]{result,error}, Actor.sender.get(), targetActor, true);
            targetActor.__scheduler.put2QueuePolling(targetActor.__cbQueue, true, ce, targetActor);
        }
    }

    @Override
    public IPromise<T> then(Runnable result) {
        if (realCallback instanceof IPromise == false)
            throw new RuntimeException("this is an error.");
        else
            return ((IPromise)realCallback).then(result);
    }

    @Override
    public IPromise<T> thenAnd(Supplier<IPromise<T>> result) {
        if (realCallback instanceof IPromise == false)
            throw new RuntimeException("this is an error.");
        else
            return ((IPromise)realCallback).thenAnd(result);
    }

    @Override
    public IPromise then(Callback<T> result) {
        if (realCallback instanceof IPromise == false)
            throw new RuntimeException("this is an error.");
        else
            return ((IPromise)realCallback).then(result);
    }

    @Override
    public IPromise<T> onResult(Consumer<T> resultHandler) {
        if (realCallback instanceof IPromise == false)
            throw new RuntimeException("this is an error.");
        else
            return ((IPromise)realCallback).onResult(resultHandler);
    }

    @Override
    public IPromise<T> onError(Consumer errorHandler) {
        if (realCallback instanceof IPromise == false)
            throw new RuntimeException("this is an error.");
        else
            return ((IPromise)realCallback).onError(errorHandler);
    }

    @Override
    public IPromise<T> onTimeout(Consumer timeoutHandler) {
        if (realCallback instanceof IPromise == false)
            throw new RuntimeException("this is an error.");
        else
            return ((IPromise)realCallback).onTimeout(timeoutHandler);
    }

    @Override
    public <OUT> IPromise<OUT> thenAnd(Function<T, IPromise<OUT>> function) {
        if (realCallback instanceof IPromise == false)
            throw new RuntimeException("this is an error.");
        else
            return ((IPromise)realCallback).thenAnd(function);
    }

    @Override
    public <OUT> IPromise<OUT> then(Consumer<T> function) {
        if (realCallback instanceof IPromise == false)
            throw new RuntimeException("this is an error.");
        else
            return ((IPromise)realCallback).then(function);
    }

    @Override
    public <OUT> IPromise<OUT> catchError(Function<Object, IPromise<OUT>> function) {
        if (realCallback instanceof IPromise == false)
            throw new RuntimeException("this is an error.");
        else
            return ((IPromise)realCallback).catchError(function);
    }

    @Override
    public <OUT> IPromise<OUT> catchError(Consumer<Object> function) {
        if (realCallback instanceof IPromise == false)
            throw new RuntimeException("this is an error.");
        else
            return ((IPromise)realCallback).catchError(function);
    }

    @Override
    public T get() {
        if (realCallback instanceof IPromise == false)
            return null;
        else
            return (T) ((IPromise)realCallback).get();
    }

    @Override
    public T await() {
        if (realCallback instanceof IPromise == false)
            return null;
        else
            return ((IPromise<T>)realCallback).await();
    }

    @Override
    public IPromise<T> awaitPromise() {
        if (realCallback instanceof IPromise == false)
            return null;
        else
            return ((IPromise<T>)realCallback).awaitPromise();
    }

    @Override
    public Object getError() {
        if (realCallback instanceof IPromise == false)
            return null;
        else
            return (T) ((IPromise)realCallback).getError();
    }

    @Override
    public IPromise timeoutIn(long millis) {
        if (realCallback instanceof IPromise == false)
            throw new RuntimeException( "currently supported for futures only" );
        ((IPromise)realCallback).timeoutIn(millis);
        return this;
    }

    @Override
    public boolean isSettled() {
        if (realCallback instanceof IPromise == false)
            throw new RuntimeException( "currently supported for futures only" );
        else
            return ((IPromise)realCallback).isSettled();
    }
}
