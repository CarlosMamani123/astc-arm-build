package com.smms.assistance.infrastructure.client;

import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Name;

@GraphQLClientApi(configKey = "auth-service")
public interface AuthClient {

    @Query("getUser")
    UserOutput getUser(@Name("id") String id);
}
