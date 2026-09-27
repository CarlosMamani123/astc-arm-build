package com.authservice.authservice.infrastructure.controller.graphql;

import com.authservice.authservice.application.service.UserService;
import com.authservice.authservice.infrastructure.controller.graphql.dto.*;

import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.*;

import java.util.List;

import io.smallrye.common.annotation.Blocking;

@GraphQLApi
@Blocking
public class UserController {

    @Inject
    UserService userService;

    // =========================
    // CREATE USER
    // =========================
    @Mutation
    public UserOutput createUser(@Name("input") CreateUserRequest input) throws Exception {
        return userService.create(input);
    }

    // =========================
    // LIST USERS
    // =========================
    @Query
    public List<UserOutput> listUsers() {
        return userService.listAll();
    }

    // =========================
    // GET USER BY ID
    // =========================
    @Query("getUser")
    public UserOutput getUser(@Name("id") String id) {
        return userService.getById(id);
    }

    // =========================
    // DELETE USER
    // =========================
    @Mutation
    public Boolean deleteUser(@Name("id") String id) {
        return userService.delete(id);
    }

    // =========================
    // EDIT USER (FIXED)
    // =========================
    @Mutation
    public UserOutput editUser(
            @Name("id") String id,
            @Name("input") EditUserInput input) throws Exception {

        return userService.update(id, input);
    }
}