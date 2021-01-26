package io.izzel.kether.common.actions;

import io.izzel.kether.common.api.persistent.KetherCompleters;
import io.izzel.kether.common.api.QuestAction;
import io.izzel.kether.common.api.QuestActionParser;
import io.izzel.kether.common.api.QuestContext;
import io.izzel.kether.common.loader.InferType;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

final class SetAction extends QuestAction<Void> {

    private final String key;
    private final InferType value;

    public SetAction(String key, InferType value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public CompletableFuture<Void> process(QuestContext.Frame frame) {
        return value.process(frame).thenAccept(e -> {
            if (Objects.equals(key, "null")) {
                frame.variables().remove(key);
            } else {
                frame.variables().set(key, e);
            }
        });
    }

    @Override
    public String toString() {
        return "SetAction{" +
            "key='" + key + '\'' +
            ", value='" + value + '\'' +
            '}';
    }

    public static QuestActionParser parser() {
        return QuestActionParser.of(
            resolver -> new SetAction(resolver.nextToken(), resolver.expect("to").nextInferType()),
            KetherCompleters.seq(
                KetherCompleters.consume(),
                KetherCompleters.consume()
            )
        );
    }
}
