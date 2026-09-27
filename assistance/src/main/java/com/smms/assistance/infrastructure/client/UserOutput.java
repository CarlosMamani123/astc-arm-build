package com.smms.assistance.infrastructure.client;

import java.util.UUID;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class UserOutput {
    public UUID id;
    public String username;
    public String email;
    public String firstName;
    public String lastName;
    public String avatarUrl;
}
