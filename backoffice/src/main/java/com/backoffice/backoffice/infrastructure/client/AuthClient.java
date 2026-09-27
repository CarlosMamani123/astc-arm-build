package com.backoffice.backoffice.infrastructure.client;

import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;

import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Name;

import java.util.List;

import com.backoffice.backoffice.infrastructure.controller.graphql.dto.*;

@GraphQLClientApi(configKey = "auth-service")
public interface AuthClient {

    // =====================
    // MUTATIONS (ESCRITURA)
    // =====================
    @Mutation
    CreateUserResponse createUser(@Name("input") CreateUserInput input);

    @Mutation
    UserOutput editUser(
        @Name("id") String id,
        @Name("input") EditUserInput input
    );

    @Mutation
    Boolean deleteUser(@Name("id") String id);

    // =====================
    // QUERIES (LECTURA)
    // =====================
    @Query("getUser")
    UserOutput getUser(@Name("id") String id);

    @Query
    List<UserOutput> listUsers();
}