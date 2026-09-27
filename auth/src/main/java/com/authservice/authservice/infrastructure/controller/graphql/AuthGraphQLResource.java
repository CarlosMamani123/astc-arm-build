package com.authservice.authservice.infrastructure.controller.graphql;

import com.authservice.authservice.application.in.LoginUseCase;
import com.authservice.authservice.infrastructure.repository.RoleRepository;
import com.authservice.authservice.infrastructure.controller.graphql.dto.RoleOutput;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.graphql.*;

import java.util.List;
import java.util.stream.Collectors;

@GraphQLApi
@ApplicationScoped
public class AuthGraphQLResource {

    @Inject
    LoginUseCase loginUseCase;

    @Inject
    RoleRepository roleRepository;

    @Query
    public String ping() {
        return "auth-service running carlospruea";
    }

    @Mutation
    public String login(String email, String password) {
        return loginUseCase.execute(email, password);
    }

    @Query
    public List<RoleOutput> listRoles() {
        return roleRepository.listAll().stream()
                .map(role -> {
                    RoleOutput output = new RoleOutput();
                    output.setId(role.getId());
                    output.setCode(role.getCode());
                    output.setName(role.getName());
                    return output;
                })
                .collect(Collectors.toList());
    }
}