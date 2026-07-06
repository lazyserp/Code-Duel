package com.codeduel.codeduel.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codeduel.codeduel.auth.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User,UUID>{

    public Optional<User> findByEmail(String email);
    public Optional<User> findByUsername(String username);

    public boolean existsByUsername(String username);
    public boolean existsByEmail(String email);
    
    public List<User> findByUsernameIn(Collection<String> username);
    
}
