package com.backoffice.backoffice.infrastructure.controller.graphql.dto;
import org.eclipse.microprofile.graphql.Input;
@Input
public class DeleteUserInput {

    private String id;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}