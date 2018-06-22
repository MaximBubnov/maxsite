package com.max.controller;

import com.max.domain.User;
import com.max.domain.dto.CaptchaResponseDto;
import com.max.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

@Controller
public class RegistrationController {

    //урл для каптчи (берется на сайте гугла, после регистрации самой каптчи (внизу перед табличкой с ключом)
    //и так же к урлу параметры (берутся как раз из этой таблички)
    private static final String CAPTCH_URL = "https://www.google.com/recaptcha/api/siteverify?secret=%s&response=%s";

    @Autowired
    private UserService userService;

    //секретные ключ для каптчи (берется на сайте гугла)
    @Value("${recaptcha.secret}")
    private String secret;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/registration")
    public String registration() {
        return "registration";
    }

    //регистрация нового пользователя
    @PostMapping("/registration")
    public String addUser(
            @RequestParam("password2") String passwordConfirm, // передаем 2 пароль
            @RequestParam("g-recaptcha-response") String captchaResponse, //передаем каптчу
            @Valid User user,
            BindingResult bindingResult,
            Model model) {

        //наш урл по которому нужно будет перейти
        String url = String.format(CAPTCH_URL, secret, captchaResponse);
        //и передаем ему параметры
        CaptchaResponseDto responseDto = restTemplate.postForObject(url, Collections.emptyList(), CaptchaResponseDto.class);

        //если не успешно все прошло, то говорим проверить каптчу
        if(!responseDto.isSuccess()) {
            model.addAttribute("captchaError", "Fill captcha");
        }

        //если пассворд2 (для проверки) не пустой
        boolean isConfirmEpty = StringUtils.isEmpty(passwordConfirm);
        if(!isConfirmEpty) {
            model.addAttribute("password2", "Password confirmation cannot be empty");
        }
        //сходяться ли два пароля которые ввел пользователь при регистрации
        if(user.getPassword() != null && !user.getPassword().equals(passwordConfirm)) {
            model.addAttribute("passwordError", "Password are different");
        }

        //есть ли у нас ошибки валидации
        if(isConfirmEpty || bindingResult.hasErrors() || !responseDto.isSuccess()) {
            Map<String, String> errors = ControllerUtils.getErrors(bindingResult);
            model.mergeAttributes(errors);

            return "registration";
        }

        //идем в сервси там есть метод по добавления пользователя - ретернет тру или фалс
        //если не смоги добавить пользователя
        if(!userService.addUser(user)) {
            model.addAttribute("usernameError", "User Exists!");
            return "registration";
        }

        return "redirect:/login";
    }

    //подтверждение регистрации
    @GetMapping("/activate/{code}")
    public String activate(Model model, @PathVariable String code) {
        boolean isActive = userService.activateUser(code);

        if(isActive) {
            model.addAttribute("messageType", "success");
            model.addAttribute("message", "User successfully activated");
        } else {
            model.addAttribute("messageType", "danger");
            model.addAttribute("message", "Activation code is not found");
        }
        return "login";
    }

}
