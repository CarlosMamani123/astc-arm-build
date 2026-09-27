package com.authservice.authservice.application.service;

import com.authservice.authservice.domain.entity.UserEntity;
import com.authservice.authservice.infrastructure.controller.graphql.dto.CreateUserRequest;
import com.authservice.authservice.infrastructure.controller.graphql.dto.UserOutput;
import com.authservice.authservice.infrastructure.repository.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import com.authservice.authservice.infrastructure.controller.graphql.dto.EditUserInput;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class UserService {

	@Inject
	UserRepository userRepository;

	@Inject
	com.authservice.authservice.infrastructure.repository.RoleRepository roleRepository;

	// =========================
	// CREATE
	// =========================
	@Transactional
	public UserOutput create(CreateUserRequest input) throws Exception {
		UUID roleUuid = resolveRoleId(input.roleId);
		if (roleRepository.findById(roleUuid) == null) {
			throw new IllegalArgumentException("El rol especificado no existe.");
		}

		UserEntity user = new UserEntity();
		if (input.id != null && !input.id.isBlank()) {
			user.setId(UUID.fromString(input.id));
		}

		String username = input.username;
		if (username == null || username.trim().isEmpty()) {
			if (input.email != null && input.email.contains("@")) {
				username = input.email.split("@")[0];
			} else {
				username = "user_" + UUID.randomUUID().toString().substring(0, 8);
			}
		}

		if (input.email != null && userRepository.existsByEmail(input.email)) {
			throw new org.eclipse.microprofile.graphql.GraphQLException("El correo electrónico ya se encuentra registrado en el sistema.");
		}
		if (userRepository.existsByUsername(username)) {
			throw new org.eclipse.microprofile.graphql.GraphQLException("El nombre de usuario ya se encuentra registrado en el sistema.");
		}

		user.setUsername(username);
		user.setEmail(input.email);
		user.setPasswordHash(hash(input.password));
		user.setRoleId(roleUuid);

		user.setFirstName(input.firstName);
		user.setLastName(input.lastName);
		user.setPhone(input.phone);
		user.setAvatarUrl(input.avatarUrl);
		user.setCountry(input.country != null && !input.country.trim().isEmpty() ? input.country : "Peru");
		
		try {
			userRepository.persistAndFlush(user);
		} catch (Exception e) {
			handleConstraintViolation(e);
			throw e;
		}

		return map(user);
	}

	// =========================
	// LIST ALL USERS (Native: 1 query LEFT JOIN)
	// =========================
	public List<UserOutput> listAll() {
		return userRepository.findAllWithRoleNative();
	}

	// =========================
	// GET BY ID (Native: 1 query LEFT JOIN)
	// =========================
	public UserOutput getById(String id) {
		return userRepository.findByIdWithRoleNative(UUID.fromString(id));
	}

	// =========================
	// DELETE (Native: 1 query DELETE)
	// =========================
	@Transactional
	public Boolean delete(String id) {
		return userRepository.deleteByIdNative(UUID.fromString(id));
	}

	// =========================
	// UPDATE
	// =========================
	@Transactional
	public UserOutput update(String id, EditUserInput input) throws Exception {

		UserEntity user = userRepository.findById(UUID.fromString(id));

		if (user == null) return null;

		if (input.username != null && !input.username.trim().isEmpty()) {
			if (!input.username.trim().equalsIgnoreCase(user.getUsername()) && userRepository.existsByUsername(input.username)) {
				throw new org.eclipse.microprofile.graphql.GraphQLException("El nombre de usuario ya se encuentra registrado en el sistema.");
			}
			user.setUsername(input.username);
		} else if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
			String fallbackUser = (input.email != null && input.email.contains("@")) ? input.email.split("@")[0]
					: (user.getEmail() != null && user.getEmail().contains("@")) ? user.getEmail().split("@")[0]
					: "user_" + UUID.randomUUID().toString().substring(0, 8);
			user.setUsername(fallbackUser);
		}
		if (input.email != null && !input.email.trim().isEmpty()) {
			if (!input.email.trim().equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(input.email)) {
				throw new org.eclipse.microprofile.graphql.GraphQLException("El correo electrónico ya se encuentra registrado en el sistema.");
			}
			user.setEmail(input.email);
		}
		if (input.roleId != null && !input.roleId.trim().isEmpty()) {
			user.setRoleId(resolveRoleId(input.roleId));
		}

		if (input.firstName != null) {
			user.setFirstName(input.firstName);
		}
		if (input.lastName != null) {
			user.setLastName(input.lastName);
		}
		if (input.phone != null) {
			user.setPhone(input.phone);
		}
		if (input.avatarUrl != null && !input.avatarUrl.trim().isEmpty()) {
			user.setAvatarUrl(input.avatarUrl);
		}
		if (input.country != null && !input.country.trim().isEmpty()) {
			user.setCountry(input.country);
		}

		if (input.password != null && !input.password.isEmpty()) {
			user.setPasswordHash(hash(input.password));
		}

		try {
			userRepository.persistAndFlush(user);
		} catch (Exception e) {
			handleConstraintViolation(e);
			throw e;
		}

		// Return with role via native LEFT JOIN (avoids separate role fetch)
		return userRepository.findByIdWithRoleAfterUpdate(user.getId());
	}

	private void handleConstraintViolation(Exception e) throws Exception {
		Throwable cause = e;
		while (cause != null) {
			String msg = cause.getMessage();
			if (msg != null) {
				if (msg.contains("users_email_key")) {
					throw new org.eclipse.microprofile.graphql.GraphQLException("El correo electrónico ya se encuentra registrado en el sistema.");
				}
				if (msg.contains("users_username_key")) {
					throw new org.eclipse.microprofile.graphql.GraphQLException("El nombre de usuario ya se encuentra registrado en el sistema.");
				}
			}
			if (cause instanceof org.hibernate.exception.ConstraintViolationException) {
				org.hibernate.exception.ConstraintViolationException cve = (org.hibernate.exception.ConstraintViolationException) cause;
				String constraint = cve.getConstraintName();
				if ("users_email_key".equals(constraint)) {
					throw new org.eclipse.microprofile.graphql.GraphQLException("El correo electrónico ya se encuentra registrado en el sistema.");
				}
				if ("users_username_key".equals(constraint)) {
					throw new org.eclipse.microprofile.graphql.GraphQLException("El nombre de usuario ya se encuentra registrado en el sistema.");
				}
			}
			cause = cause.getCause();
		}
	}

	// =========================
	// MAPPER
	// =========================
	private UserOutput map(UserEntity user) {
		UserOutput out = mapWithoutRole(user);

		if (user.getRoleId() != null) {
			var role = roleRepository.findById(user.getRoleId());
			if (role != null) {
				out.setRoleCode(role.getCode());
				out.setRoleName(role.getName());
			}
		}

		return out;
	}

	private UserOutput mapWithoutRole(UserEntity user) {
		UserOutput out = new UserOutput();

		out.setId(user.getId());
		out.setUsername(user.getUsername());
		out.setEmail(user.getEmail());
		out.setFirstName(user.getFirstName());
		out.setLastName(user.getLastName());
		out.setFullName(user.getFullName());
		out.setPhone(user.getPhone());
		out.setAvatarUrl(user.getAvatarUrl());
		out.setRoleId(user.getRoleId());
		out.setCountry(user.getCountry());

		return out;
	}

	private String hash(String password) {
		return BCrypt.hashpw(password, BCrypt.gensalt());
	}

	private UUID resolveRoleId(String roleId) {
		if (roleId == null || roleId.isBlank()) {
			throw new IllegalArgumentException("roleId es obligatorio.");
		}
		try {
			return UUID.fromString(roleId);
		} catch (IllegalArgumentException ignored) {
			return roleRepository.findByCode(roleId)
				.map(role -> role.getId())
				.orElseThrow(() -> new IllegalArgumentException("El rol especificado no existe."));
		}
	}
}

