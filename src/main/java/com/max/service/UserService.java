package com.max.service;

import com.max.domain.Role;
import com.max.domain.User;
import com.max.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService{
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private MailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo.findByUsername(username);

        if(user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return user;
    }

    //добавление нового пользователя
    public boolean addUser(User user) {
        User userFromDb = userRepo.findByUsername(user.getUsername());

        //если уже есть такой пользователь
        if(userFromDb != null) {
            return false;
        }

        //если нет
        user.setActive(true);
        user.setRoles(Collections.singleton(Role.USER));
        user.setActivationCode(UUID.randomUUID().toString());
        //зашифруем пароль
        //user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepo.save(user);

        //если у него есть емаил. то отправим ему сообщение
        sendMessage(user);

        return true;
    }

    //отправка сообщения с активационным ключом
    private void sendMessage(User user) {
        if(!StringUtils.isEmpty(user.getEmail())) {

            String message = String.format(
                    "Hello, %s! \n" +
                            "Welcome to Max-site. Please, visit next link: http://localhost:8080/activate/%s",
                    user.getUsername(),
                    user.getActivationCode()
            );
            mailSender.send(user.getEmail(), "Activation code", message);
        }
    }

    //пользователь активирован или нет
    public boolean activateUser(String code) {
        //возвращаем пользователя по определенному коду
        User user = userRepo.findByActivationCode(code);

        if(user == null) {
            return false;
        }
        //если все ок - код = 0
        user.setActivationCode(null);
        userRepo.save(user);

        return true;
    }

    //получение всех юзеров
    public List<User> findAll() {
        return userRepo.findAll();
    }

    //сохранение юзера
    public void saveUser(User user, String username, Map<String, String> form) {
        user.setUsername(username);

        Set<String> roles = Arrays.stream(Role.values())
                .map(Role::name)
                .collect(Collectors.toSet());

        user.getRoles().clear();

        for (String key : form.keySet()) {
            if(roles.contains(key)) {
                user.getRoles().add(Role.valueOf(key));
            }
        }
        userRepo.save(user);
    }

    //изменение профиля юзера
    public void updateProfile(User user, String password, String email) {
        //получаем емаил изначальные
        String userEmail = user.getEmail();

        //изменен ли емаил
        boolean isEmailChanged = ((email != null && !email.equals(userEmail)) ||
                (userEmail!=null && !userEmail.equals(email)));

        //если да
        if(isEmailChanged) {
            user.setEmail(email); // то устанавливаем новый
            //если он установил новый емаил
            if(!StringUtils.isEmpty(email)) {
                user.setActivationCode(UUID.randomUUID().toString()); // генерируем ему новый код
            }
        }

        //если юзер установил новый пароль
        if(!StringUtils.isEmpty(password)) {
            user.setPassword(password); // устанавливаем его юзеру
        }

        //после всего этого сохраняем пользователя в базе данных
        userRepo.save(user);

        //и отправляем ему активайшен код, только тогда когда емаил был изменен
        if (isEmailChanged) {
            sendMessage(user);
        }
    }

    //подписаться на пользователя
    public void subscribe(User currentUser, User user) {
        user.getSubscribers().add(currentUser);

        userRepo.save(user);
    }

    //отписаться от пользователя
    public void unsubscribe(User currentUser, User user) {
        user.getSubscribers().remove(currentUser);

        userRepo.save(user);
    }
}
