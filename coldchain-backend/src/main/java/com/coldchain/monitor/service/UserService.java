package com.coldchain.monitor.service;

import com.coldchain.monitor.dto.Dtos.*;
import com.coldchain.monitor.entity.*;
import com.coldchain.monitor.exception.ApiException;
import com.coldchain.monitor.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repo, PasswordEncoder encoder) { this.repo = repo; this.encoder = encoder; }

    private User find(Long id) { return repo.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found.")); }

    private void checkUnique(String username, String email, Long selfId) {
        repo.findByUsernameIgnoreCase(username.trim()).filter(u -> !u.id.equals(selfId))
            .ifPresent(u -> { throw new ApiException(HttpStatus.CONFLICT, "This username is already in use."); });
        repo.findByEmailIgnoreCase(email.trim()).filter(u -> !u.id.equals(selfId))
            .ifPresent(u -> { throw new ApiException(HttpStatus.CONFLICT, "This email is already associated with another account."); });
    }

    public List<UserResponse> list() { return repo.findAll(Sort.by("id")).stream().map(UserResponse::of).toList(); }
    public UserResponse get(Long id) { return UserResponse.of(find(id)); }

    @Transactional
    public UserResponse create(CreateUserRequest r) {
        checkUnique(r.username(), r.email(), null);
        User u = new User();
        u.name = r.name().trim();
        u.username = r.username().trim().toLowerCase();
        u.email = r.email().trim().toLowerCase();
        u.password = encoder.encode(r.password());
        u.role = r.role();
        u.status = r.status() == null ? Status.ACTIVE : r.status();
        return UserResponse.of(repo.save(u));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest r, User me) {
        User u = find(id);
        checkUnique(r.username(), r.email(), id);
        if (u.id.equals(me.id)) {
            if (r.status() == Status.INACTIVE) throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot deactivate the account you are currently using.");
            if (r.role() != u.role) throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot change the role of the account you are currently using.");
        }
        u.name = r.name().trim();
        u.username = r.username().trim().toLowerCase();
        u.email = r.email().trim().toLowerCase();
        u.role = r.role();
        u.status = r.status();
        return UserResponse.of(repo.save(u));
    }

    @Transactional
    public void delete(Long id, User me) {
        User u = find(id);
        if (u.id.equals(me.id)) throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot delete the account you are currently using.");
        repo.delete(u);
    }

    @Transactional
    public UserResponse setStatus(Long id, Status status, User me) {
        User u = find(id);
        if (status == Status.INACTIVE && u.id.equals(me.id)) throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot deactivate the account you are currently using.");
        u.status = status;
        return UserResponse.of(repo.save(u));
    }
}
