package io.izzel.kether.common.actions;

import io.izzel.kether.common.api.persistent.KetherCompleters;
import io.izzel.kether.common.api.QuestAction;
import io.izzel.kether.common.api.QuestActionParser;
import io.izzel.kether.common.api.QuestContext;

import java.util.concurrent.CompletableFuture;

public class GetAction extends QuestAction<Object> {

    private final String key;

    public GetAction(String key) {
        this.key = key;
    }

    @Override
    public CompletableFuture<Object> process(QuestContext.Frame frame) {
        return CompletableFuture.completedFuture(frame.variables().get(key).orElse(null));
    }

    public static QuestActionParser parser() {
        return QuestActionParser.of(
            resolver -> new GetAction(resolver.nextToken()),
            KetherCompleters.consume()
        );
    }
}
