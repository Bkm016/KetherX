package io.izzel.kether.common.loader.types;

import io.izzel.kether.common.loader.ArgType;
import io.izzel.kether.common.util.Coerce;

import java.time.Duration;

public enum ArgTypes {

    INT(new AnyType<>(Coerce::asInteger, "integer")),

    LONG(new AnyType<>(Coerce::asLong, "long")),

    DOUBLE(new AnyType<>(Coerce::asDouble, "double")),

    BOOLEAN(new AnyType<>(Coerce::asBoolean, "boolean")),

    DURATION(new DurationType());

    ArgType<?> type;

    ArgTypes(ArgType<?> type) {
        this.type = type;
    }

    public ArgType<?> getType() {
        return type;
    }
}
