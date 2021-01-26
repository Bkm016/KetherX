package io.izzel.kether.common.loader;

import com.google.common.annotations.Beta;
import io.izzel.kether.common.api.Quest;
import io.izzel.kether.common.api.QuestAction;
import io.izzel.kether.common.api.data.ContextString;
import io.izzel.kether.common.loader.types.ArgTypes;
import io.izzel.kether.common.util.LocalizedException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public interface QuestReader {

    char peek();

    char peek(int n);

    int getIndex();

    int getMark();

    void setIndex(int index);

    boolean hasNext();

    String nextToken();

    ContextString nextString();

    void mark();

    void reset();

    <T> QuestAction<T> nextAction();

    @Beta
    <T> List<QuestAction<T>> nextList();

    @Beta
    Quest.Block nextBlock();

    @Beta
    InferType nextInferType();

    @Beta
    List<InferType> nextInferList();

    default QuestReader expect(String value) {
        String element = nextToken();
        if (!element.equals(value)) {
            throw LocalizedException.of("not-match", value, element);
        }
        return this;
    }

    default int nextInt() {
        return next(ArgTypes.INT);
    }

    default long nextLong() {
        return next(ArgTypes.LONG);
    }

    default double nextDouble() {
        return next(ArgTypes.DOUBLE);
    }

    default boolean nextBoolean() {
        return next(ArgTypes.BOOLEAN);
    }

    default Duration nextDuration() {
        return next(ArgTypes.DURATION);
    }

    boolean flag(String name);

    <T> Optional<T> optionalFlag(String name, ArgType<T> flagType);

    default <T> T flag(String name, ArgType<T> flagType, Supplier<T> defaultValue) {
        return optionalFlag(name, flagType).orElseGet(defaultValue);
    }

    default <T> T next(ArgType<T> argType) throws LocalizedException {
        return (T) argType.read(this);
    }

    @SuppressWarnings("unchecked")
    default <T> T next(ArgTypes argType) throws LocalizedException {
        return (T) argType.getType().read(this);
    }
}
