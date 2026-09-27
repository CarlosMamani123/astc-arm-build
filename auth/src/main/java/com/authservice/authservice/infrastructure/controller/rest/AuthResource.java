/*package com.authservice.authservice.infrastructure.controller.rest;

import com.authservice.authservice.application.in.LoginUseCase;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("/auth")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    LoginUseCase loginUseCase;

    public static class LoginRequest {
        public String email;
        public String password;
    }

    // =========================
    // LOGIN
    // =========================
    @POST
    @Path("/login")
    public Response login(LoginRequest req) {

        System.out.println("🔐 [AUTH] Login request: " + req.email);

        String token = loginUseCase.execute(req.email, req.password);

        System.out.println("🪪 [AUTH] JWT generated: " + token);

        return Response.ok()
                .entity("{\"ok\":true}")
                .header("Set-Cookie",
                        "auth_token=" + token +
                        "; Path=/" +
                        "; HttpOnly" +
                        "; Secure" +
                        "; SameSite=None"
                )
                .build();
    }

    // =========================
    // ME (USER LOGUEADO)
    // =========================
    @GET
    @Path("/me")
    public Response me(@Context HttpHeaders headers) {

        Cookie cookie = headers.getCookies().get("auth_token");

        if (cookie == null) {
            return Response.status(401)
                    .entity("{\"error\":\"No token found\"}")
                    .build();
        }

        String token = cookie.getValue();

        System.out.println("🍪 [ME] Token recibido");

        try {
            var claims = JWTParser.parse(token);

            String role = claims.getClaim("role");
            String email = claims.getClaim("email");

            return Response.ok()
                    .entity("""
                    {
                        "role": "%s",
                        "email": "%s"
                    }
                    """.formatted(role, email))
                    .build();

        } catch (Exception e) {
            return Response.status(401)
                    .entity("{\"error\":\"Invalid token\"}")
                    .build();
        }
    }
}*/