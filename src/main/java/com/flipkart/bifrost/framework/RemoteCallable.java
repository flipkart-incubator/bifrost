package com.flipkart.bifrost.framework;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.flipkart.bifrost.HttpCallCommand;

@JsonTypeInfo(use= JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = HttpCallCommand.class, name = "http")})
public abstract class RemoteCallable<T> {
    @JsonIgnore
    private final String type;

    protected RemoteCallable(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public abstract T call() throws Exception;

}
