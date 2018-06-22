package com.max.controller;

import com.max.domain.Role;
import com.max.domain.User;
import com.max.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/user") //это делается, чтобы у каждого метода не приписывать этот маппинг
public class UserController {
    @Autowired
    private UserService userService;

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping
    public String userList(Model model) {
        model.addAttribute("users", userService.findAll());
        return "userList";
    }
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("{user}")  //это означает, что помимо нашего главного мапинга(написан сверху), будет еще и этот идентификатор
    public String userEditForm(@PathVariable User user, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        return "userEdit";
    }
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public String userSave(
            @RequestParam String username,
            @RequestParam Map<String, String> form,
            @RequestParam("userId") User user)
    {
        userService.saveUser(user, username, form);
        return "redirect:/user";
    }

    //мапинг для профиля (просмотра)
    @GetMapping("profile")
    public String getProfile(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());

        return "profile";
    }

    //изменение профиля
    @PostMapping("profile")
    public String updateProfile(
            @AuthenticationPrincipal User user,
            @RequestParam String password,
            @RequestParam String email) {

        userService.updateProfile(user, password, email);

        return "redirect:/user/profile";
    }

    //подписка
    @GetMapping("subscribe/${user}")
    public String subscribe(
            @AuthenticationPrincipal User currentUser,
            @PathVariable User user) {

        userService.subscribe(currentUser, user);
        return "redirect:/user-messages/" + user.getId();
    }
    //отписка
    @GetMapping("unsubscribe/${user}")
    public String unsubscribe(
            @AuthenticationPrincipal User currentUser,
            @PathVariable User user) {

        userService.unsubscribe(currentUser, user);
        return "redirect:/user-messages/" + user.getId();
    }

    //выведем страницу с подспиками (кто - на кого)
    @GetMapping("{type}/${user}/list")
    public String userListSub(
            Model model,
            @PathVariable User user,
            @PathVariable String type
            ) {

        model.addAttribute("userChannel", user);
        model.addAttribute("type", type);

        //список (подписчиков / подписок)
        if("subscriptions".equals(type)) {
            model.addAttribute("users", user.getSubscriptions());
        }else {
            model.addAttribute("users", user.getSubscribers());

        }
        return "subscriptions";
    }
}
