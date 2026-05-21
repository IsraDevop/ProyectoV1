package com.yala.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserRegisteredEvent extends ApplicationEvent {
    private final Long userId;
    private final String email;
    private final String name;

    public UserRegisteredEvent(Object source, Long userId, String email, String name) {
        super(source);
        this.userId = userId;
        this.email = email;
        this.name = name;
    }
}
