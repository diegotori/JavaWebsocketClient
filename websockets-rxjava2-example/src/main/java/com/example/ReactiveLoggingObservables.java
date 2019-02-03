package com.example;

import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * Created by son_g on 11/12/2017.
 */

public class ReactiveLoggingObservables {

    @Nonnull
    public static Observer<Object> logging(@Nonnull final Logger logger, @Nonnull final String tag) {
        return new LoggerObserver<>(logger, tag);
    }

    @Nonnull
    public static Observer<Object> loggingOnlyError(@Nonnull final Logger logger, @Nonnull final String tag) {
        return new ErrorLoggerObserver<>(logger, tag);
    }

    @Nonnull
    public static <T> ObservableOperator<T, T> loggingLift(@Nonnull final Logger logger, @Nonnull
    final String tag) {
        return new ObservableOperator<T, T>() {
            @Override
            public Observer<? super T> apply(final Observer<? super T> observer) throws Exception {
                return new LoggerObserver<T>(logger, tag) {
                    @Override
                    public void onSubscribe(Disposable d) {
                        super.onSubscribe(d);
                        observer.onSubscribe(d);
                    }

                    @Override
                    public void onComplete() {
                        super.onComplete();
                        observer.onComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        super.onError(e);
                        observer.onError(e);
                    }

                    @Override
                    public void onNext(T o) {
                        super.onNext(o);
                        observer.onNext(o);
                    }
                };
            }
        };
    }

    @Nonnull
    public static <T> ObservableOperator<T, T> loggingOnlyErrorLift(@Nonnull final Logger logger,
            @Nonnull final String tag) {
        return new ObservableOperator<T, T>() {
            @Override
            public Observer<? super T> apply(final Observer<? super T> observer) throws Exception {
                return new ErrorLoggerObserver<T>(logger, tag) {
                    @Override
                    public void onSubscribe(Disposable d) {
                        super.onSubscribe(d);
                        observer.onSubscribe(d);
                    }

                    @Override
                    public void onComplete() {
                        super.onComplete();
                        observer.onComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        super.onError(e);
                        observer.onError(e);
                    }

                    @Override
                    public void onNext(T o) {
                        super.onNext(o);
                        observer.onNext(o);
                    }
                };
            }
        };
    }

    static class LoggerObserver<T> extends BaseLoggerObserver<T> {

        protected LoggerObserver(Logger logger, String tag) {
            super(logger, tag);
        }

        @Override
        public void onComplete() {
            logger.log(Level.INFO, tag + " - onCompleted");
        }

        @Override
        public void onError(Throwable e) {
            logger.log(Level.SEVERE, tag + " - onError", e);
        }

        @Override
        public void onNext(T o) {
            logger.log(Level.INFO, tag + " - onNext: {0}", o == null ? "null" : o.toString());
        }
    }

    static class ErrorLoggerObserver<T> extends BaseLoggerObserver<T> {
        protected ErrorLoggerObserver(Logger logger, String tag) {
            super(logger, tag);
        }

        @Override
        public void onError(Throwable e) {
            logger.log(Level.SEVERE, tag + " - onError", e);
        }
    }

    static abstract class BaseLoggerObserver<T> implements Observer<T> {
        protected final Logger logger;
        protected final String tag;

        protected BaseLoggerObserver(Logger logger, String tag) {
            this.logger = logger;
            this.tag = tag;
        }

        @Override
        public void onSubscribe(Disposable d) {}

        @Override
        public void onNext(T object) {}

        @Override
        public void onError(Throwable e) {}

        @Override
        public void onComplete() {}
    }
}
