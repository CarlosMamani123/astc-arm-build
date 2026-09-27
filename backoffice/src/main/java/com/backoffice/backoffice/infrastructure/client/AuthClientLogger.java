package com.backoffice.backoffice.infrastructure.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import com.backoffice.backoffice.infrastructure.controller.graphql.dto.*;

import java.util.List;

@ApplicationScoped
public class AuthClientLogger {

    private static final Logger LOG = Logger.getLogger(AuthClientLogger.class);

    @Inject
    AuthClient authClient;

    public CreateUserResponse createUser(CreateUserInput input) {

        LOG.info("🚀 [AUTH CLIENT] createUser REQUEST");
        LOG.info("📥 INPUT: " + inputToString(input));

        try {
            CreateUserResponse response = authClient.createUser(input);

            LOG.info("📤 [AUTH CLIENT] RESPONSE: " + response);
            return response;

        } catch (Exception e) {
            LOG.error("💥 [AUTH CLIENT] ERROR calling createUser", e);
            throw e;
        }
    }

    public List<UserOutput> listUsers() {

        LOG.info("🚀 [AUTH CLIENT] listUsers REQUEST");

        try {
            List<UserOutput> response = authClient.listUsers();

            LOG.info("📤 [AUTH CLIENT] RESPONSE SIZE: " + (response != null ? response.size() : 0));
            return response;

        } catch (Exception e) {
            LOG.error("💥 [AUTH CLIENT] ERROR listUsers", e);
            throw e;
        }
    }

    public UserOutput getUser(String id) {

        LOG.info("🚀 [AUTH CLIENT] getUser REQUEST: " + id);

        try {
            UserOutput response = authClient.getUser(id);

            LOG.info("📤 RESPONSE: " + response);
            return response;

        } catch (Exception e) {
            LOG.error("💥 ERROR getUser", e);
            throw e;
        }
    }

    public UserOutput editUser(String id, EditUserInput input) {

        LOG.info("🚀 [AUTH CLIENT] editUser REQUEST");
        LOG.info("ID: " + id);
        LOG.info("INPUT: " + input);

        try {
            UserOutput response = authClient.editUser(id, input);

            LOG.info("📤 RESPONSE: " + response);
            return response;

        } catch (Exception e) {
            LOG.error("💥 ERROR editUser", e);
            throw e;
        }
    }

    public Boolean deleteUser(String id) {

        LOG.info("🚀 [AUTH CLIENT] deleteUser REQUEST: " + id);

        try {
            Boolean response = authClient.deleteUser(id);

            LOG.info("📤 RESPONSE: " + response);
            return response;

        } catch (Exception e) {
            LOG.error("💥 ERROR deleteUser", e);
            throw e;
        }
    }

    private String inputToString(CreateUserInput i) {
        if (i == null) return "NULL";

        return "username=" + i.getUsername()
                + ", email=" + i.getEmail()
                + ", roleId=" + i.getRoleId();
    }
}