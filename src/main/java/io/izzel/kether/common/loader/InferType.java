package io.izzel.kether.common.loader;

import com.google.common.collect.Lists;
import io.izzel.kether.common.api.Quest;
import io.izzel.kether.common.api.QuestAction;
import io.izzel.kether.common.api.QuestContext;
import io.izzel.kether.common.api.data.SimpleQuest;
import io.izzel.kether.common.loader.types.ArgTypes;
import io.izzel.kether.common.util.Coerce;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * kether
 * io.izzel.kether.common.loader.MultipleType
 *
 * @author sky
 * @since 2021/1/20 12:46 下午
 */
public class InferType {

    private final Type type;
    private final Object value;

    public InferType(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<Object> process(QuestContext.Frame frame) {
        switch (type) {
            case LIST:
                List<InferType> inferTypeList = (List<InferType>) this.value;
                CompletableFuture<Object> future = CompletableFuture.completedFuture(null);
                for (InferType inferType : inferTypeList) {
                    future = inferType.process(frame);
                }
                return future;
            case BLOCK:
                String name = "inner_" + System.currentTimeMillis();
                QuestContext.Frame newFrame = frame.newFrame(name);
                newFrame.setNext((Quest.Block) value);
                frame.addClosable(newFrame);
                return newFrame.run();
            case ACTION:
                return frame.newFrame((QuestAction<?>) value).run();
            case VARIABLE:
                return CompletableFuture.completedFuture(frame.variables().get(value.toString()).orElse(null));
            default:
                return CompletableFuture.completedFuture(value);
        }
    }

    public Type getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public enum Type {

        TOKEN, INT, LONG, DOUBLE, BOOLEAN, ACTION, BLOCK, LIST, VARIABLE;

        public static Type fromArgTypes(ArgTypes argTypes) {
            try {
                return valueOf(argTypes.name());
            } catch (Exception ignored) {
                return TOKEN;
            }
        }
    }
}
