package com.backoffice.backoffice.infrastructure.controller.graphql.dto;
import org.eclipse.microprofile.graphql.Input;
@Input
public class ListUsersFilterInput {

    private String username;
    private String email;
    private String roleId;

    private Integer page;
    private Integer size;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}