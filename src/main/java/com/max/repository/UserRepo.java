package com.max.repository;

import com.max.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User, Long> {

    User findByUsername(String name);

    //возвращаем пользователя по определенному коду
    User findByActivationCode(String code);
}
